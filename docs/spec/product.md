# Spesifikasi Produk MutuGemba

Dokumen ini merangkum identitas aplikasi, target pengguna, prinsip UX, dan batasan arsitektur agar pengembangan konsisten.

## Identitas Aplikasi
- Nama: MutuGemba
- Tagline: QC TPS Harian: Inspeksi, Abnormal, QCPC, dan Analitik Ringkas
- Target pengguna: Operator / Inspector / Leader / QC Engineer (awam komputer)

## Platform & Mode Operasi
- Desktop (Windows 10/11) adalah prioritas MVP.
- Android (HP/Tablet) menyusul tanpa rewrite besar dengan berbagi modul `core-*`.
- Offline-only, internal perusahaan.
- Data disimpan lokal: SQLite single file + folder lampiran.
- Backup/restore 1 klik untuk operasional lapangan tanpa cloud.

## Prinsip UX untuk User Awam
- Bahasa Indonesia sederhana (contoh: "Cacat", "Parameter Proses", "Tiket Abnormal", "Tindakan Penahanan").
- Wizard input 3 langkah: Konteks -> Input -> Simpan.
- Tombol besar, minim menu, nyaman dipakai satu tangan.
- Default nilai otomatis (tanggal/shift, user, line terakhir).
- Riwayat checksheet bersifat read-only agar data final terjaga.

## Arsitektur (Clean + Modular)
- Modul:
  - `app-desktop` (UI Compose Desktop)
  - `core-domain` (entity + aturan TPS, pure Kotlin)
  - `core-usecase` (interactors)
  - `core-data` (SQLite + file lampiran + migrations)
  - `core-analytics` (Pareto/Trend/SPC engine)
- Disiplin: UI tidak boleh akses DB langsung; hanya via UseCase.

## Library yang Disarankan (Opsional)
- UI lintas platform: Compose Multiplatform/Compose Desktop.
- DB type-safe + migrations: SQLDelight.
- DI ringan: manual container (tanpa framework) atau Koin jika dibutuhkan.

## Kualitas Kode & CI
- Format/lint: ktlint-gradle.
- Static analysis: detekt.
- CI: `gradle/actions/setup-gradle` + `actions/setup-java` (JDK 17).
- Konvensi kode: mengikuti Kotlin coding conventions.
