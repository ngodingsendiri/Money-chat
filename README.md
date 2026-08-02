# 💬 Money Chat — Pencatatan Keuangan via Chat + AI

[![Build APK](https://github.com/ngodingsendiri/Money-chat/actions/workflows/build-apk.yml/badge.svg)](https://github.com/ngodingsendiri/Money-chat/actions/workflows/build-apk.yml)
![Versi](https://img.shields.io/badge/versi-1.0.1-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-blue)

**Money Chat** adalah aplikasi Android pencatat keuangan keluarga/kelompok yang berbasis **percakapan chat** (seperti WhatsApp) — cukup ketik pesan biasa seperti *"beli kopi 20rb"* atau *"gaji masuk 5 juta"*, dan AI otomatis mencatatnya sebagai transaksi. Dilengkapi rekap visual, analisis AI finansial, dan mode gelap.

> 🎯 **Filosofi app**: *offline-first*, tanpa server sendiri. Data tersimpan di perangkat. AI jalan pakai **key API milik pengguna** (BYOK) — server tidak perlu menyediakan API key.

---

## 📥 Download APK

| Versi | File | Link |
|---|---|---|
| **v1.0.1** (terbaru) | `MoneyChat-v1.0.1-debug.apk` | **⬇️ [Download dari GitHub Releases](https://github.com/ngodingsendiri/Money-chat/releases/latest)** |
| Semua versi | — | [Daftar Release](https://github.com/ngodingsendiri/Money-chat/releases) |
| Build mentah (artifact) | `MoneyChat-v1.0.1-debug` (zip) | [Actions → Build APK](https://github.com/ngodingsendiri/Money-chat/actions/workflows/build-apk.yml) |

**Cara install:**
1. Unduh APK dari link di atas (via HP langsung atau kirim ke HP).
2. Buka file APK → Android akan minta izin **"Instal dari sumber tidak dikenal"** → izinkan.
3. Selesai! Buka aplikasi, login, dan mulai catat.

> ℹ️ **APK debug vs release**: APK debug sudah ditandatangani & bisa langsung diinstall. APK **release** (tanda tangan produksi) otomatis muncul kalau secrets keystore disiapkan di repo (lihat [Release Signing](#release-signing-opsional)).

---

## ✨ Fitur Utama

- 💬 **Pencatatan lewat chat** — ketik pesan biasa, AI mendeteksi transaksi & jumlahnya otomatis
- 🤖 **AI 3 lapis tanpa server**:
  1. **OpenRouter** (BYOK) — model gratis dengan **rotasi otomatis** saat kena rate-limit/kuota habis
  2. **Google Gemini** (BYOK) — pakai key dari akun Google sendiri
  3. **Mesin offline** — heuristik lokal kalau keduanya tidak ada
- 📊 **Rekap visual** — saldo total, diagram donat kategori, progress bar alokasi pengeluaran
- 🧾 **Analisis AI finansial** — evaluasi kesehatan arus kas + rekomendasi strategis
- ⚡ **Saran cepat** — chip rekomendasi transaksi berdasarkan kebiasaan pengguna
- 🔄 **Workspace bersama (PIN)** — beberapa perangkat bisa saling terhubung via PIN unik
- 👥 **Peran anggota** — Bendahara / Anggota / Ketua (atau Suami / Istri)
- 🌙 **Mode gelap** — nyala/mati manual dari menu pengaturan
- 🔑 **BYOK (Bring Your Own Key)** — tempel API key sendiri di *Pengaturan → Kunci API*, tersimpan lokal di perangkat

---

## 🔑 Setup API AI

Aplikasi **tidak butuh API key apa pun untuk dijalankan** — semua fitur AI punya fallback ke mesin offline. Tapi supaya AI beneran cerdas, isi salah satu (atau keduanya):

| Provider | Cara dapat key | Di mana set di app |
|---|---|---|
| **OpenRouter** (disarankan) | Gratis di [openrouter.ai/keys](https://openrouter.ai/keys) — format `sk-or-v1-...` | Pengaturan (⋮) → **Kunci OpenRouter API** |
| **Google Gemini** | Gratis di [aistudio.google.com/apikey](https://aistudio.google.com/apikey) — format `AIza...` | Pengaturan (⋮) → **Kunci Gemini API** |

**Urutan prioritas AI:** OpenRouter (user) → Gemini (user) → key bawaan app (jika dibakar saat build) → mesin offline.

> 💡 **Buat developer**: key bawaan app bisa dibakar lewat env saat build —
> - `OPENROUTER_API_KEY` → `BuildConfig.OPENROUTER_API_KEY`
> - `GEMINI_API_KEY` (via Secrets Gradle Plugin / `.env`) → `BuildConfig.GEMINI_API_KEY`

---

## 🛠️ Build dari Source

### Prasyarat
- JDK 17
- Android SDK (compileSdk 36)

### Langkah
```bash
# 1. Siapkan key AI (opsional — fallback ke offline kalau kosong)
export OPENROUTER_API_KEY="sk-or-v1-..."   # opsional

# 2. Build APK debug
./gradlew :app:assembleDebug

# 3. Hasilnya ada di:
#    app/build/outputs/apk/debug/app-debug.apk
```

### Release signing (opsional)
```bash
export KEYSTORE_PATH=/path/ke/keystore.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

---

## 🤖 GitHub Actions (CI)

Setiap push ke `main` atau tag `v*` otomatis menjalankan workflow **Build APK**:
1. Generate debug keystore
2. Build APK debug → upload sebagai **artifact** `MoneyChat-vX.Y.Z-debug`
3. Jika secrets keystore tersedia → build + upload APK **release** (bertanda tangan)
4. Jika push **tag** → buat **GitHub Release** dengan APK terpasang (link permanen)

### Secrets yang dipakai
| Secret | Fungsi |
|---|---|
| `OPENROUTER_API_KEY` | Key OpenRouter bawaan app (dibakar ke APK saat build) |
| `KEYSTORE_BASE64` | Isi file keystore `.jks` dalam base64 (untuk APK release) |
| `KEYSTORE_PASSWORD` | Password keystore |
| `KEY_PASSWORD` | Password key (alias `upload`) |

---

## 🏗️ Arsitektur

```
┌─ HP Android (app Money Chat) ─────────────────────────┐
│  Chat + AI → OpenRouter cloud (openrouter.ai)  ← key user  │
│           → Google Gemini cloud (generativelanguage)  ← key user  │
│  Data utama → Room (SQLite) di perangkat  ← offline-first │
│  Sync opsional → Firebase Firestore (butuh google-services.json) │
└────────────────────────────────────────────────────────┘
```

- **Tanpa server sendiri** — semua berjalan di perangkat + cloud AI pihak ketiga
- **Database**: Room/SQLite (transaksi & chat tersimpan lokal)
- **Sync cloud**: `FirestoreSyncManager` sudah dikoding (opsional, belum aktif)

---

## 🧱 Tech Stack

- **Kotlin + Jetpack Compose (Material 3)**
- Room, OkHttp, Retrofit, Moshi, kotlinx.coroutines
- Firebase (Auth, Firestore, App Check) — opsional
- Gradle 9.3.1 · AGP 9.1.1 · Kotlin 2.x

---

## 🗺️ Roadmap

- [ ] Aktifkan sinkronisasi Firebase Firestore antar perangkat
- [ ] APK release bertanda tangan di CI (siap upload Play Store)
- [ ] Backup & restore data
- [ ] Grafik bulanan & notifikasi pengingat

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE) *(jika ada)* — silakan kembangkan, fork, dan bagikan.

---

Dibuat dengan ❤️ oleh [@ngodingsendiri](https://github.com/ngodingsendiri) — **Money Chat v1.0.1**
