# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - Fix v1.1.0

### Fixed
- **BUG-01**: Snackbar 'Urungkan' action now works correctly — was opening 'Kelola Anggota' due to SnackbarHost overlapping TopAppBar when keyboard visible. Fixed by increasing top padding from 8dp to 72dp to clear TopAppBar + status bar.
- **K1**: Parse nominal ribuan bertitik bertingkat (1.500.000, 15.000.000, etc.) — was returning null due to multiple dots in thousand separators. Fixed by normalizing all dots as thousand separators before parsing.
- **BUG-02**: Tab navigation (Chat ↔ Rekap) now works when keyboard is visible — was blocked because keyboard covered bottom nav. Fixed by hiding keyboard on tab click and adding ImeAction.Done + keyboardActions to input fields.
- **K2**: Parsing transportasi (bensin, taxi, ojek, grab, gojek, tol, parkir, "isi") kini tercatat sebagai Pengeluaran kategori Transportasi — was missed entirely because `isExpenseTrigger` tidak punya keyword transportasi. Plus mapping kategori untuk nasi/market/belanja/taxi.
- **BUG-07**: Typo tagline login "Nyatat" → "Mencatat"; versi app disinkronkan ke r1.1.0 (versionCode 24) di layar login & Settings.
- **BUG-04**: Field nama di onboarding kini punya tombol clear cepat (trailing icon) — user tidak perlu menghapus nama prefilled dari Google manual karakter per karakter.

### Fixed
- **BUG-08**: Sinkronisasi Firestore "Gagal sinkron" — penyebabnya `get(...).exists` pada subcollection `members` dari dalam aturan selalu mengembalikan `null` (rules menolak langsun semua baca/tulis walau member valid). Diperbaiki dengan mengganti ke fungsi `exists()` (atomik, tanpa perlu baca resource) sehingga `isMember`/`isOwner` evaluasi benar. Terverifikasi live: atur → "Tersinkron".

### Added
- **TASK-2.1**: Cross-device chat↔transaksi lookup via `sourceMessageCloudId` (tap badge finansial dari pesan tersinkron di perangkat lain kini membuka transaksi yang benar); `DataExporter` mempertahankan relasi lintas-perangkat pada JSON (pending-op retry & backup).
- **TASK-2.2**: Perkuat Firestore Security Rules — validasi skema `messages`/`transactions` (`cloudId` = docId, `amount` angka positif & di bawah 1e12, tipe valid), join-request anti-duplikat per UID; tambah `firebase.json`.
- **TASK-2.3**: BUG-03 — layar konfirmasi PIN kini benar-benar scrollable (`fillMaxSize` + `verticalScroll`, tetap ter-center saat konten pendek).

### Changed
- **TASK-2.1**: Unit test `sourceMessageCloudId` round-trip & merge lintas perangkat (+4).

---

## [r1.0.3] - 2026-08-06

### Added
- Backup & restore Google Drive (v1.2.9)
- Export rekap CSV (v1.2.9)
- Workspace bersama + kelola anggota + persetujuan owner (FASE 4, v1.4.0)
- Sinkronisasi realtime Firestore (FASE 4, v1.4.0)

### Fixed
- Google Sign-In di release build
- Redesign layar login (logo + nama + tagline)
- Sembunyikan navigasi saat login
- Dialog update tersedia di semua build