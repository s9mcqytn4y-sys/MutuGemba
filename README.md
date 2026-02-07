# MutuGemba

Tagline: QC TPS Harian: Inspeksi, Abnormal, QCPC, dan Analitik Ringkas.

Aplikasi QC TPS offline untuk desktop Windows. MVP ini berfokus pada clean architecture, UI sederhana, dan operasional lokal.

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
