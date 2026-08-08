# 🔍 Audit Proyek Nyachat

Laporan audit komprehensif proyek **Nyachat** — aplikasi Android pencatat keuangan keluarga/kelompok berbasis **percakapan chat + AI**.

> Tanggal audit: 2026-08-06 · Bahasa: Indonesia

---

## 1. Ringkasan Proyek

**Nyachat** adalah aplikasi Android (Kotlin + Jetpack Compose, Material 3) yang memungkinkan keluarga/kelompok mencatat keuangan lewat obrolan seperti WhatsApp. Pengguna cukup mengetik pesan biasa (*"beli kopi 20rb"*) dan AI otomatis mendeteksi transaksi + nominalnya.

| Atribut | Nilai |
|---|---|
| ApplicationId | `com.startupmini.nyachat` |
| Versi | `r1.0.3` (versionCode 23) |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |
| Build | Gradle 9.3.1 · AGP 9.1.1 · Kotlin 2.2.10 · KSP 2.3.6 |
| Filsafat | **Offline-first** — data lokal Room, AI BYOK, sync Firestore opsional |

**Fitur utama:**
- 💬 Pencatatan via chat (parse transaksi + AI vision untuk foto nota)
- 🤖 AI 3 lapis tanpa server: OpenRouter (BYOK) → Gemini (BYOK) → mesin heuristik offline
- 📊 Rekap visual: saldo, donut chart kategori, progress bar, insight mingguan
- 📤 Export CSV, ☁️ backup/restore Google Drive (terenkripsi)
- 🔗 Workspace bersama via PIN + keanggotaan berbasis akun Google
- 🔄 Sinkronisasi realtime Firestore dengan antrian retry offline
- 🌙 Mode gelap, 🔑 BYOK (Bring Your Own Key)

---

## 2. Struktur Kode & Arsitektur

```
app/src/main/java/com/startupmini/nyachat/
├── MainActivity.kt          (1474 baris — wiring utama + UI shell)
├── Constants.kt             (satu sumber kebenaran konstanta/pref/field)
├── data/
│   ├── local/               Room DB, DAO, SecureStorage, AvatarStore
│   ├── remote/              Gemini, OpenRouter, FirestoreSync, Membership, UpdateChecker
│   ├── repository/          FinanceRepository (logika bisnis utama)
│   ├── analytics/           FinancialInsights, MonthlyAnalytics, WeeklyInsights (lokal, tanpa AI)
│   └── backup/              DataExporter, BackupCrypto, DriveBackup{Controller,Manager}
└── ui/
    ├── MainViewModel.kt     (StateFlow; jembatan UI ↔ repository)
    ├── screens/             ChatScreen, RekapScreen, PinConnectScreen, SettingsSheet, ...
    ├── theme/               Color, Theme, Type, SemanticColors
    └── util/                AvatarImage, DateLabels
```

### Pola arsitektur
- **MVVM** — `MainViewModel` (AndroidViewModel) meng-expose `StateFlow`, UI Compose mengumpulkan via `collectAsStateWithLifecycle`.
- **Repository pattern** — `FinanceRepository` memegang logika: persist lokal (Room), sync cloud, dan parsing AI.
- **Dekomposisi P3-1** — AI dipisah ke `FinanceAiService` (injectable, bisa di-mock di test), memutus ketergantungan repository ke `GeminiService` langsung.
- **Obyek singleton service** — `GeminiService`, `OpenRouterService`, `FirestoreSyncManager`, `MembershipManager` semuanya `object` (statis). Ini menyederhanakan wiring tapi menyimpan state global.

---

## 3. Lapisan Data (Room)

**Database:** `keuangan_pasutri_db`, **versi 8** dengan 7 migrasi bertahap, `exportSchema = true` → skema ke `app/schemas`.

| Tabel / Entity | Kolom penting |
|---|---|
| `ChatMessage` | sender, messageText, timestamp, isFinancial, detectedAmount/Category/Type, imagePath, filePath/fileName, replyTo*, editedAt, **cloudId (unik)** |
| `FinancialTransaction` | type (PEMASUKAN/PENGELUARAN), category, amount, description, loggedBy, timestamp, editedAt, chatMessageId, **cloudId (unik)** |
| `PendingOp` | opType + payload (JSON) — antrian ops cloud yang gagal (retry offline) |

**Sejarah migrasi (v1→v8):**
1. **v1→v2:** index timestamp chat_messages
2. **v2→v3:** kolom `cloudId` + index unik (basis sync)
3. **v3→v4:** `imagePath` (foto nota)
4. **v4→v5:** reply, filePath/fileName, editedAt
5. **v5→v6:** tabel `pending_ops`
6. **v6→v7:** `editedAt` di transaksi (resolusi konflik LWW)
7. **v7→v8:** index unik cloudId + index timestamp transaksi (dengan dedupe data lama)

`OnConflictStrategy.REPLACE` pada insert — penting agar merge dari snapshot Firestore konvergen (bukan crash).

---

## 4. Keamanan Penyimpanan

`SecureStorage` menggantikan `EncryptedSharedPreferences` (deprecated sejak security-crypto 1.1.0).
- **Master key AES-256-GCM** disimpan di Android Keystore (`money_chat_master_key`).
- Hanya **3 field rahasia** yang terenkripsi: 2 API key + PIN (format `Base64(IV || ciphertext)`).
- Field non-rahasia (dark mode, role, name, timestamp) di SharedPreferences biasa.
- Async wrapper (`putSecretAsync`) agar dekripsi Keystore tidak memblokir komposisi (P2-14).
- ⚠️ Catatan: data lama dari EncryptedSharedPreferences tidak bisa dibaca lagi setelah migrasi (user perlu input ulang PIN & key sekali).

---

## 5. AI 3 Lapis (BYOK, Tanpa Server)

Urutan prioritas sesuai README: **OpenRouter (user) → Gemini (user) → mesin offline.**

### GeminiService (714 baris)
- `parseChatMessage` → transaksi; kaskade ke OpenRouter lalu heuristik offline.
- Prompt builder (`buildParsePrompt`, `buildReceiptPrompt` untuk vision foto nota).
- `extractAmountFromText` + `toRupiah` + `offlineHeuristicParse` (regex `NUMBER_UNIT_PATTERN`).
- Laporan audit & analisis bulanan punya **fallback offline** (`buildOfflineAuditReport`, `buildOfflineMonthlyReport`) via `FinancialInsightsEngine`.
- BYOK: `GeminiService.userApiKey` di-set dari SecureStorage.

### OpenRouterService
- **Model gratis dengan rotasi otomatis** (`FREE_MODELS`) — kalau satu model kena 429/402/404, lanjut ke model berikutnya, berakhir di router virtual `openrouter/free`.
- Dukungan vision: foto nota dikirim sebagai `data:image/jpeg;base64,...`.

### Mesin offline
- `FinanceAiService` & heuristik lokal menjamin app tetap berfungsi penuh tanpa API key.

---

## 6. Sinkronisasi Firestore (`FirestoreSyncManager`, 735 baris)

- Login Google wajib (rules memaksa `sign_in_provider == 'google.com'`).
- Data disimpan perkeluarga: `families/{PIN}/messages|transactions|members|joinRequests`.
- **Offline-first**: Room adalah sumber kebenaran; sinkronisasi meng-*upsert* ke lokal.
- **Antrian pending ops** (`PendingOp`) untuk tulis yang gagal, pakai **exponential backoff** (`MIN/MAX_RETRY_DELAY`) + drain saat app hidup.
- **Resolusi konflik LWW** berbasis `editedAt` (last-writer-by-time).
- **Snapshot listener realtime** dipause saat background (`LifecycleResumeEffect`), resume saat foreground.
- Dedupe double-inject (lokal + snapshot) lewat `dedupeByCloudId`.
- Status sync: `SYNCED/SYNCING/OFFLINE/ERROR` via `StateFlow`.

**Keamanan rules (`firestore.rules`):**
- Semua akses butuh login Google.
- `families` read dibatasi member/owner (perbaikan penting — mencegah oracle enumerasi PIN).
- `members`/`joinRequests`/`messages`/`transactions` diatur granular.
- Pembuat workspace punya bootstrap untuk dokumen member-nya sendiri.

---

## 7. Keanggotaan Workspace (`MembershipManager`)

- **Owner** membuat workspace (PIN 8 digit), mensetujui/menolak join request, mengubah peran/label, menghapus anggota.
- **Member** bergabung via PIN → join request → menunggu keputusan owner (ada timeout).
- Status: `FAMILY_NOT_FOUND / MEMBER / PENDING / NOT_REQUESTED / FAILED / TIMED_OUT`.
- **Re-check saat resume** (ON_RESUME) — menangani kasus di-kick/ditolak di device lain.
- Role di app: `owner` / `member` (label: Suami/Istri/Bendahara).

---

## 8. Backup & Restore Drive

- **`DataExporter`** — format CSV rekap + JSON backup (dengan `formatVersion`).
- **`BackupCrypto`** — enkripsi backup (password/passphrase).
- **`DriveBackupController`** (P4-4, 15KB) + **`DriveBackupManager`** — logika backup/restore Drive diekstrak dari MainActivity; controller me-*wire* dependency + state.
- **Auto backup 24 jam** seperti WhatsApp; **menyisakan 5 backup terbaru**.
- Restore mengembalikan chat + transaksi & menyinkronkan lintas perangkat.
- Pembersihan backup lintas keluarga (`deleteAbsentFromBackup`) saat restore.

---

## 9. UI Screens

| Screen | Peran |
|---|---|
| **MainActivity.onCreate** (UI shell) | Login gate, tab navigasi, wiring semua state, snackbar, dialogs |
| **PinConnectScreen** | Google Sign-In + PIN workspace (join/buat) |
| **ChatScreen** (1400 baris) | Chat, bubble, lampiran, reply, quick suggestions; `buildChatRows` dikelompok per hari |
| **RekapScreen** (1441 baris) | Balance banner, donut chart, insight AI card, daftar transaksi |
| **ManageMembersScreen** | Kelola anggota + join requests |
| **SettingsSheet** | Pengaturan: mode gelap, API key, export, backup, logout |
| **AddTransactionDialog / AiReportDialog** | Input manual + laporan AI |
| **MembershipGateScreen** | Gerbang menunggu persetujuan owner |

Detail desain diyakini rapi: radius sudut terstandar (`Constants.Ui.CORNER_*`), kategori & role di `Constants`, tidak ada rad meme adab.

---

## 10. Testing (Robolectric + Roborazzi)

Unit test cukup kaya (17 file) mencakup:
- Analitik: `FinancialInsights`, `MonthlyAnalytics`, `WeeklyInsights`
- Backup: `BackupCrypto`, `DataExporter`, `DriveBackupController`, `PendingOpSerialization`
- Remote: `FirestoreSyncManagerConflictTest`, `GeminiServiceHeuristicParse`, `GitHubUpdateChecker`
- Repository: `FinanceRepositoryTest`, `FinanceRepositoryBadgeTest`
- UI logic: `AmountFormatter`, `**AppSnapshotTest (Roborazzi)**`, `MembershipGateLogic`, `PinAttemptLimiter`, `DateLabels`

Perintah:
- Unit test: `./gradlew :app:testDebugUnitTest`
- Snapshot verify: `./gradlew :app:verifyRoborazziDebug`
- Rekam baseline: `./gradlew :app:recordRoborazziDebug`
- Lint: `./gradlew :app:lintDebug`

---

## 11. CI/CD (GitHub Actions)

Workflow `build-apk.yml` (push `main` / tag `r*`):
1. JDK 21 (wajib untuk compileSdk 36 + Robolectric)
2. Cache Gradle → **unit test** → **lint** → **snapshot Roborazzi** → **lint firestore.rules** (`npm ci && npm run lint:rules`)
3. Build + upload APK debug (artifact)
4. Jika secrets keystore (`KEYSTORE_BASE64`, dll.): build APK & AAB release
5. Saat tag: buat GitHub Release dengan file APK/AAB

**Catatan jilid sintaks:** `debug.keystore` DI-COMMIT ke repo agar SHA-1 debug stabil → Google Sign-In tidak ditolak Firebase. Risiko keamanan terukur (lihat §12).

---

## 12. Temuan & Perhatian (Observasi Audit)

### ✅ Kekuatan
- Arsitktur bersih & terdokumentasi baik (komentar audit P1–P4 literal di kode).
- Offline-first murni + fallback AI bertingkat — sangat resilien.
- Migrasi Room bertahap + `exportSchema` untuk review.
- Keamanan: BYOK murni (tanpa key dibakar), Keystore untuk rahasia, rules Firestore diperketat.
- Testing (unit + snapshot UI) + CI gate berkualitas sebelum build.

### ⚠️ Poin yang perlu diperhatikan
1. **`MainActivity.kt` (1474 baris) + `ChatScreen` (1400) + `RekapScreen` (1441) sangat besar** — UI shell & screen bisa dipecah (composable terpisah, state holder). Kandidat refactor ke Navigation Compose / multi-file.
2. **Singleton `object` untuk service** (`GeminiService`, `FirestoreSyncManager`, `MembershipManager`) menyimpan state global statis — menyulitkan unit test murni & bisa bocor antar sesi login. `FirestoreSyncManager` & `MembershipManager` sudah di-test tapi makin besar (735 & 429 baris).
3. **`debug.keystore` publik** — risiko keamanan terukur (README §"Risiko debug.keystore publik (P1)"): siapa pun bisa menandatangani APK debug dengan SHA-1 terdaftar. Mitigasi yang disebut: Firebase App Check + edukasi download dari release resmi.
4. **Versi dependensi tua/tidak terkini**:
   - `composeBom = 2024.09.00` (lama; perlu naik ke versi lebih baru)
   - `lifecycle 2.8.7`, `activityCompose 1.10.1`, `room 2.7.0` — masih terekspektasi tapi bisa diperbarui
   - `okhttp 4.10.0`, `robolectric 4.16.1`, `roborazzi 1.59.0`
5. **`evaluate` label**: `AppSnapshotTest` disediakan; pastikan baseline PNG di-commit terbaru saat UI berubah.
6. **Gradle properties**: perlu dicek flag performa (`org.gradle.caching`, `configuration-cache`) — belum terlihat dari baca singkat.
7. **`ROLE` minSdk 24 + targetSdk 36** — pastikan edge-to-edge (`enableEdgeToEdge`) sudah menangani inset di semua screen (perlu verifikasi visual saat deploy).
8. **AGENTS.md di context mengacu Node.js/Azure** — itu file proyek lain; untuk proyek ini yang relevan README.md & docs/. Perlu disesuaikan agar tidak menyesatkan agent.

---

## 13. Kesimpulan

Nyachat adalah proyek Android berarsitektur baik, **offline-first**, dengan fokus keamanan & testing yang serius. Produk siap pakai (r1.0.3) dengan pipeline CI lengkap menuju Google Play. Area yang paling menjanjikan untuk peningkatan berikutnya: **pemecahan file raksasa (MainActivity/ChatScreen/RekapScreen), modernisasi dependensi Compose/lifecycle, dan penguatan isolasi service singleton**.
