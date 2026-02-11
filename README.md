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
- Root data runtime: `~/.mutugemba/data/`
- Database SQLite: `~/.mutugemba/data/mutugemba.db`
- Asset image hash-store: `~/.mutugemba/data/assets_store/images/sha256/<2-char>/<sha256>.png`
- Lampiran: `~/.mutugemba/data/attachments/`
- Preferensi: `~/.mutugemba/data/settings.properties`
- Ekspor PDF: `~/.mutugemba/data/exports/`
- Backup: `~/.mutugemba/data/backup/backup-YYYYMMDD-HHmmss/`

Source of truth part management:
- `data/part_assets/extracted/mappings/mapping.json`
- `data/part_assets/extracted/reports/defect_screening.json`

Aplikasi bootstrap dari folder extracted di atas dan tidak bergantung ZIP runtime.

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
