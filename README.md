# MutuGemba

Tagline: QC TPS Harian: Inspeksi, Abnormal, QCPC, dan Analitik Ringkas.

Aplikasi QC TPS offline untuk desktop Windows. MVP ini berfokus pada clean architecture, UI sederhana, dan operasional lokal.
Fitur laporan bulanan tersedia dalam format dokumen A4 landscape (read-only) dengan opsi ekspor PDF.

Dokumen spesifikasi produk: `docs/spec/product.md`.

## Prasyarat
- JDK 17

## Cara Run
```
./gradlew :app-desktop:run
```

## Testing
```
./gradlew test
```

## Data & Backup Lokal
- Database SQLite: `data/mutugemba.db`
- Lampiran: `data/attachments/`
- Preferensi: `data/settings.properties`
- Ekspor PDF: `data/exports/`
- Backup: `data/backup/backup-YYYYMMDD-HHmmss/`

Backup/restore tersedia dari menu Pengaturan. Restore akan memuat ulang aplikasi agar data terbaru terbaca.

## Format
```
./gradlew formatAll
```

## Lint
```
./gradlew lintAll
```
Lint menjalankan ktlint dan detekt.

## Build Paket Desktop
```
./gradlew :app-desktop:packageDistribution
```
Alias ini menjalankan `packageDistributionForCurrentOS`.
