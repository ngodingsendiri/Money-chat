# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - Fix v1.1.0

### Fixed
- **BUG-01**: Snackbar 'Urungkan' action now works correctly — was opening 'Kelola Anggota' due to SnackbarHost overlapping TopAppBar when keyboard visible. Fixed by increasing top padding from 8dp to 72dp to clear TopAppBar + status bar.
- **K1**: Parse nominal ribuan bertitik bertingkat (1.500.000, 15.000.000, etc.) — was returning null due to multiple dots in thousand separators. Fixed by normalizing all dots as thousand separators before parsing.
- **BUG-02**: Tab navigation (Chat ↔ Rekap) now works when keyboard is visible — was blocked because keyboard covered bottom nav. Fixed by hiding keyboard on tab click and adding ImeAction.Done + keyboardActions to input fields.

### Added
- None yet

### Changed
- None yet

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