# 🔬 Laporan Audit Menyeluruh — Nyachat

> Audit membaca & menganalisis **seluruh codebase** tanpa mengubah kode.
> Fokus: arsitektur, kualitas kode, bug, performa, keamanan, state, DB, API,
> error handling, UI logic, dan edge case.
> Tanggal: 2026-08-06 · Versi audit: `r1.0.3` (versionCode 23)

---

## Ringkasan Eksekutif

Nyachat adalah aplikasi Android (Kotlin + Compose M3) yang secara keseluruhan **tersusun baik**: MVVM + repository, offline-first, BYOK, dokumentasi internal rapi, dan testing (unit + snapshot) yang lebih baik dari rata-rata. Tidak ditemukan **error kompilasi** (analisis statis clean).

Namun audit menemukan **1 bug logika nyata yang berdampak langsung pada pencatatan transaksi**, beberapa isu keamanan, banyak file monolitik, dependensi yang tertinggal cukup jauh, dan sederet edge case.

### Distribusi temuan

| Prioritas | Jumlah | Ringkas |
|---|---|---|
| 🔴 Kritis | 1 | Gagal parse nominal ribuan bertitik → transaksi hilang |
| 🟠 Tinggi | 3 | Model pribadi hilang di cloud, keystore publik, file raksasa |
| 🟡 Sedang | 12 | Dependensi usang, listener bocor, relasi lintas-perangkat, dsb. |
| 🟢 Rendah | 13 | Log menyesatkan, edge case heuristik, indikator AI, dsb. |

> ⚠️ **Catatan koreksi**: nama model `gemini-3.5-flash` **valid (GA)** — bukan bug.

---

## 🔴 KRITIS

### K1 — Gagal parse nominal ribuan bertitik bertingkat ("1.500.000")
- **File**: `data/remote/GeminiService.kt` → `extractAmountFromText` / `toRupiah`
- **Penyebab**:
  ```kotlin
  val rawNum = numStr.replace(",", ".").toDoubleOrNull() ?: return null
  ```
  Untuk input `"1.500.000"`: `replace(",",".")` menghasilkan `"1.500.000"` → mengandung **dua titik** → `toDoubleOrNull()` mengembalikan `null`. Nominal dengan pemisah ribuan bertitik yang memiliki **≥ 2 titik** (nilai ≥ 1 juta) tidak bisa diparse.
- **Dampak**: Pesan chat seperti *"gaji masuk 1.500.000"* atau *"beli motor 15.000.000"* **tidak terdeteksi sebagai transaksi** pada jalur heuristik offline. (Pada jalur AI, model mungkin masih benar — tapi fallback offline & kasus tanpa API key pasti gagal.) Ini *default path* bagi banyak pengguna.
- **Rekomendasi**:
  - Normalisasi pemisah ribuan **sebelum** konversi: strip semua `.` (titik ribuan) lalu konversi, sambil perlakukan `,` sebagai desimal hanya bila diikuti 1–2 angka — atau pakai pendekatan regex yang lebih cermat memisahkan ribuan vs desimal.
  - Tambahkan unit test: `extractAmountFromText("gaji 1.500.000") == 1_500_000.0`, `("15.000.000")`, `("1.200")`, `("2,5jt")`.

---

## 🟠 TINGGI

### T1 — Relasi `chatMessageId` memakai id lokal → tidak sinkron lintas perangkat
- **File**: `FinanceRepository.kt`, `MainActivity.kt` (`onOpenTransaction`), `ChatScreen.kt`
- **Penyebab**: `FinancialTransaction.chatMessageId` menyimpan **id lokal Room** dari pesan asal. Saat transaksi disinkronkan ke Firestore, nilai ini ikut dikirim; perangkat lain punya id lokal yang berbeda → relasi tidak bermakna lintas perangkat.
- **Dampak**: Fitur **"ketuk badge keuangan di chat → buka/edit transaksi"** (`transactions.find { it.chatMessageId == msg.id }`) **gagal di perangkat kedua** — muncul snackbar "transaksi tidak ditemukan". Konsistensi 2-arah chat↔rekap putus di lingkungan multi-device (fitur utama FASE 4).
- **Rekomendasi**: Sinkronkan relasi berbasis `cloudId` pesan (mis. simpan `sourceMessageCloudId`) atau hapus relasi lintas-perangkat dan resolusi lewat pencocokan lain. Buat test untuk skenario dua perangkat.

### T2 — `debug.keystore` di-commit ke repo publik (risiko penandatanganan)
- **File**: `repo/debug.keystore`, `app/build.gradle.kts`, `README.md`
- **Penyebab**: Keystore debug (SHA-1 `B5:9D:30…03:B1`) di-commit supaya SHA-1 stabil untuk Google Sign-In.
- **Dampak / risiko**: Siapa pun dapat menandatangani APK debug dengan SHA-1 yang sudah terdaftar di Firebase — Firebase memperlakukan APK itu sebagai app "resmi". Menyentuh pengguna yang menginstal APK debug dari sumber tak tepercaya.
- **Rekomendasi**: Aktifkan **Firebase App Check** (sudah disebut di README sebagai mitigasi), pertimbangkan menandatangani debug dengan kunci per-developer + mendaftarkan SHA-1-nya, dan edukasi pengguna agar hanya mengunduh dari GitHub Releases resmi.

### T3 — File raksasa: `MainActivity` (1474), `ChatScreen` (1400), `RekapScreen` (1441 baris)
- **File**: `MainActivity.kt` (onCreate ~1150 baris), `ui/screens/ChatScreen.kt`, `ui/screens/RekapScreen.kt`
- **Penyebab**: UI shell & screen terakumulasi menjadi satu composable monolitik.
- **Dampak**: Sulit dirawat, risiko regresi, kesulitan testing UI terisolasi, dan beban komposisi tinggi (lihat P2).
- **Rekomendasi**: Refactor bertahap — ekstrak state-holder per layar, pisahkan dialogs (sudah ada beberapa), pertimbangkan Navigation Compose untuk memisahkan tujuan layar.

---

## 🟡 SEDANG

### M1 — Dependensi tertinggal cukup jauh (version lookup terverifikasi)
| Dependency | Terpasang | Terbaru |
|---|---|---|
| `compose-bom` | 2024.09.00 | **2026.06.01** |
| `lifecycle-runtime-ktx` | 2.8.7 | **2.11.0** |
| `room-runtime` | 2.7.0 | **2.8.4** |
| `okhttp` | 4.10.0 | **5.4.0** (major bump) |
| `activity-compose` | 1.10.1 | **1.13.0** |
| `robolectric` | 4.16.1 | 4.16.1 (sama) |

- **Dampak**: Melewatkan perbaikan bug, performa, dan fitur (termasuk perbaikan keamanan). Upgrade `okhttp` 4→5 bersifat breaking & perlu audit perubahan API.
- **Rekomendasi**: Rencanakan upgrade bertahap per-modul diikuti `gradle_build` + `unitTests` + `verifyRoborazziDebug`.

### M2 — Listener keanggotaan tidak di-pause saat background
- **File**: `data/remote/MembershipManager.kt` (`start`)
- **Penyebab**: `FirestoreSyncManager` punya pause/resume, tapi `membersListener` & `joinRequestsListener` di `MembershipManager` tetap aktif di background.
- **Dampak**: Boros kuota/baterai & memicu rekomposisi daftar anggota di background.
- **Rekomendasi**: Terapkan pola pause/resume yang sama (mis. dari `LifecycleResumeEffect` di MainActivity).

### M3 — Tidak ada proteksi skema di Firestore rules
- **File**: `firestore.rules`
- **Penyebab**: Rules `messages`/`transactions` hanya memeriksa `isMember(familyId)`; tidak memvalidasi field/wajib kunci `cloudId == docId`, tipe, atau rentang `amount`.
- **Dampak**: Anggota (yang sah) bisa menulis dokumen tak valid/invalid ke koleksi bersama → merusak data orang lain, atau mengirim `amount` negatif/NaN. `nonNullMap` (klien) tidak menyelamatkan jika klien lain mem-bypass.
- **Rekomendasi**: Tambahkan validasi baca-tulis di rules: `request.resource.data.cloudId == docId`, `amount is number && amount > 0 && amount < <MAX>`, dsb.

### M4 — Relasi LWW berbasis jam perangkat (`editedAt`)
- **File**: `FirestoreSyncManager.kt` (`effectiveSortTime`, `upsertMessage/upsertTransaction`)
- **Penyebab**: Konflik resolved dengan membandingkan `editedAt`/`timestamp` yang berasal dari `System.currentTimeMillis()` tiap perangkat.
- **Dampak**: Jika jam perangkat tidak sinkron, edit "lebih baru" bisa dikalahkan edit "lebih lama".
- **Rekomendasi**: Pertimbangkan `FieldValue.serverTimestamp()` + `lastUpdateByUid` untuk tie-break deterministik, atau dokumentasikan keterbatasannya secara eksplisit.

### M5 — Auto-backup harian dilewati saat enkripsi aktif
- **File**: `data/backup/DriveBackupController.kt` (`silentBackup`)
- **Penyebab**: `if (getEncryptionEnabled()) return false`.
- **Dampak**: Pengguna yang mengaktifkan enkripsi **tidak pernah** mendapat backup otomatis 24 jam (hanya manual).
- **Rekomendasi**: Tampilkan keterangan / alternatif (mis. backup terenkripsi otomatis dengan kunci yang disimpan aman di Keystore, atau peringatan eksplisit).

### M6 — Tidak ada batas laju (rate-limit) di sisi server untuk PIN/join
- **File**: `firestore.rules` + `PinAttemptLimiter` (sisi klien)
- **Penyebab**: `PinAttemptLimiter` murni sisi klien dan mudah di-bypass; rules tidak membatasi frekuensi `joinRequests` per akun/UID.
- **Dampak**: Potensi brute-force enumerasi/penebakan PIN (8 digit) atau spam join request.
- **Rekomendasi**: Batasi jumlah dokumen joinRequest per UID atau per jangka waktu di rules (mis. `request.time`), dan pertahankan lockout sisi klien sebagai lapisan UX.

### M7 — Error/kegagalan AI & fallback tidak diindikasikan ke pengguna
- **File**: `data/remote/GeminiService.kt`, `ui/screens/ChatScreen.kt`
- **Penyebab**: Saat AI gagal, `offlineHeuristicParse`/balasan offline dipakai diam-diam; tidak ada penanda "mode offline/heuristik".
- **Dampak**: Pengguna tidak tahu transaksi diproses oleh mesin sederhana (bisa keliru) vs AI penuh. Membingungkan saat hasil suboptimal.
- **Rekomendasi**: Tampilkan indikator kecil ("diproses offline") pada bubble hasil parse, atau pada pengaturan status AI aktif.

### M8 — `buildChatRows` & `onOpenTransaction` linear scan
- **File**: `ui/screens/ChatScreen.kt`, `MainActivity.kt`
- **Penyebab**: `buildChatRows` dipanggil tiap komposisi; `transactions.find { it.chatMessageId == msg.id }` linear.
- **Dampak**: Untuk riwayat besar (ribuan item), komposisi & tap bisa melambat (jank).
- **Rekomendasi**: `remember(transactions) { … }` grouping, dan index transaksi per `chatMessageId` (Map) via `derivedStateOf`.

### M9 — `clearLocalData` (ganti workspace) menghapus lampiran fisik
- **File**: `ui/MainViewModel.kt` (`clearLocalData` → `ImageFileUtil.deleteAllAttachments`)
- **Penyebab**: Lampiran disimpan di satu folder global `filesDir/attachments`, dihapus total saat pindah workspace.
- **Dampak**: Kembali ke workspace lama → foto nota lama **rusak** (path tetap tersimpan di DB, file hilang).
- **Rekomendasi**: Namespace folder lampiran per workspace, atau hapus hanya lampiran milik data yang ikut dihapus.

### M10 — Log menyesatkan di kaskade AI
- **File**: `data/remote/GeminiService.kt`
- **Penyebab**: Cabang OpenRouter **dan** Gemini keduanya menulis `Log.w(TAG, "OpenRouter/parsing gagal…")`.
- **Dampak**: Menghambat diagnosa jalur mana yang gagal.
- **Rekomendasi**: Bedakan label log OpenRouter vs Gemini.

### M11 — Parameter `recentContext` tidak pernah dipakai
- **File**: `GeminiService.kt` (`parseChatMessage`) / `FinanceAiService.kt`
- **Penyebab**: `recentContext` diterima sebagai argumen namun tidak disisipkan ke `buildParsePrompt`/`buildReceiptPrompt`.
- **Dampak**: Parameter mati; konteks riwayat tidak memanfaatkan konteks untuk akurasi parse.
- **Rekomendasi**: Gunakan `recentContext` di prompt, atau hapus parameter.

### M12 — Belum ada migration test untuk Room (7 migrasi)
- **File**: `data/local/AppDatabase.kt` + `app/src/test`
- **Penyebab**: `exportSchema=true` ada, tapi tidak ada test `MigrationTestHelper` untuk memverifikasi v1→v8.
- **Dampak**: Migrasi yang salah (mis. v7→v8 yang destruktif) baru terdeteksi di produksi.
- **Rekomendasi**: Tambahkan migration test dengan skema historis (`app/schemas`) untuk tiap jalan migrasi.

---

## 🟢 RENDAH

### L1 — Migrasi v7→v8 destruktif (dedupe permanen)
- `AppDatabase.kt` MIGRATION_7_8: `DELETE … WHERE id NOT IN (SELECT MAX(id) … GROUP BY cloudId)` menghapus baris duplikat **secara permanen** saat upgrade. Disengaja untuk integritas index unik, tapi membutikan data duplikat hilang. Rekomendasi: jalankan backup sebelum upgrade / pastikan hanya menghapus duplikat sungguhan.

### L2 — False-positive heuristik untuk angka polos kecil
- `GeminiService.kt` `offlineHeuristicParse`: `"makan 2 kucing"` → amount polos `2` → dianggap `Rp 2.000` + trigger "makan" → tercatat. Heuristik perlu lebih konservatif untuk angka polos < 3 digit tanpa satuan.

### L3 — `sendMessage` timestamp pesan vs transaksi berbeda
- `FinanceRepository.kt`: `initialMsg.timestamp` dan `trans.timestamp` masing-masing `System.currentTimeMillis()` terpisah → bisa lintas tanggal/detik berbeda antara chat & rekap. Rekomendasi: gunakan satu sumber waktu.

### L4 — `AddTransactionDialog` default `loggedBy = "Bendahara"`
- Saat `initialLoggedBy` null. Bisa menampilkan label yang tidak relevan. Rekomendasi: default lebih netral / konfigurasi.

### L5 — `ImageFileUtil.encodeBase64` mem-buffer file penuh
- Terbatas karena foto di-downscale (1600px, JPEG 85) & PDF tidak dikirim ke AI. Pertahankan batas ukuran & pertimbangkan stream.

### L6 — Quick suggestions memanggil AI (sampai 6 model) di background
- Sudah dimitigasi debounce 3s + cooldown 15 menit. Pertimbangkan menonaktifkan saat tidak ada key AI (langsung fallback offline).

### L7 — PIN disalin ke clipboard
- Tercatat sekaligus fitur (mudah dibagikan), tapi clipboard dapat dibaca app lain (API < 32). Pertimbangkan `ClipDescription` privacy/`setPrimaryClip` dengan label.

### L8 — `pruneOldBackups` mengabaikan error per file
- `DriveBackupManager.pruneOldBackups` men-skip error tanpa laporan; backup lama bisa menumpuk. Rekomendasi: agregasi & log error.

### L9 — Backup JSON (plaintext) di appDataFolder Drive
- Hanya terenkripsi jika user mengaktifkan enkripsi. Pertimbangkan enkripsi default-on.

### L10 — `package.json` Node hanya untuk lint rules (`npm run lint:rules`)
- Bentrok dengan AGENTS.md (kesan proyek Node.js). Jelas perannya (eslint firestore.rules).

### L11 — Pemilihan versi CI via grep regex rapuh
- `build-apk.yml` menggrep `appVersion` dari teks build.gradle. Rekomendasi: set `-Papp.version` eksplisit / Gradle property.

### L12 — `MembershipManager.stop()` dipanggil dua kali (sync + langsung)
- Tidak berbahaya (idempoten), hanya duplikasi pemanggilan. Bisa dirapikan.

### L13 — `installApk` hanya tersedia di debug (by design) — pastikan verifikasi manual alur update release (buka browser ke GitHub release).

---

## Verifikasi / Catatan Lain

- **Error statis**: `analyze_file` pada MainActivity, ChatScreen, RekapScreen, GeminiService, FirestoreSyncManager, FinanceRepository → **tidak ada error** (warnings timeout, bukan error).
- **Model AI**: `gemini-3.5-flash` terverifikasi **valid (GA)** per dokumentasi Google.
- **Gradle props**: `org.gradle.caching=true`, `configuration-cache=true`, `parallel=true`, `jvmargs=-Xmx4g` → build config sudah baik.

---

## Prioritas Penyelesaian yang Disarankan

1. **Segera (K1)** — perbaiki `toRupiah`/`extractAmountFromText` agar nominal ribuan bertitik bertingkat terdeteksi + unit test.
2. **Rencana 1 rilis (T1, M1)** — sinkronkan relasi chat↔transaksi lintas perangkat; upgrade dependensi bertahap.
3. **Keamanan (T2, M3, M6)** — App Check, validasi skema di rules, batas laju join.
4. **Kualitas (T3, M2, M8, M12)** — refactor file raksasa, pause listener, optimasi UI, migration test.
5. **Sisa** sesuai prioritas rendah.

---

*Laporan dibuat sebagai bagian audit menyeluruh; **belum ada perubahan kode** yang dilakukan.*
