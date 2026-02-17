# Operasional MutuGemba (Offline)

Dokumen ini merangkum cara menjalankan aplikasi, lokasi data, dan prosedur pemeliharaan dasar untuk operasional harian.

## Menjalankan Aplikasi
```
./gradlew :app-desktop:run
```

## Lokasi Data Lokal
- Root data runtime: `~/.mutugemba/data/`
- Database SQLite: `~/.mutugemba/data/mutugemba.db`
- Lampiran: `~/.mutugemba/data/attachments/`
- Preferensi: `~/.mutugemba/data/settings.properties`
- Ekspor PDF: `~/.mutugemba/data/exports/`
- Backup: `~/.mutugemba/data/backup/backup-YYYYMMDD-HHmmss/`
- Asset hash-store: `~/.mutugemba/data/assets_store/images/sha256/<2-char>/<sha256>.png`

## Backup & Restore
- Backup/restore dilakukan dari menu **Pengaturan**.
- Backup akan menyalin database, preferensi, lampiran, dan asset hash-store ke folder backup.
- Restore akan mengambil backup terbaru dan menimpa data aktif.
- Setelah restore, aplikasi akan dimuat ulang agar data terbaru terbaca.

## Reset Data
- Reset akan menghapus data inspeksi dan mengembalikan master data awal.
- Proses ini tidak bisa dibatalkan.

## Logging
- Runtime log ditulis ke stdout melalui `slf4j-simple`.
- Gunakan terminal/launcher untuk menangkap log saat troubleshooting.

## Ekspor Laporan Bulanan
- Ekspor/print dilakukan dari layar **Laporan Bulanan** (read-only).
- File PDF tersimpan di `~/.mutugemba/data/exports/` dengan format nama `MonthlyReport-<LINE>-YYYY-MM.pdf`.

## Upgrade & Migrasi
- Skema database dimigrasikan otomatis saat startup dari resource schema lokal (`core-data`).
- Pastikan direktori `~/.mutugemba/data/` tetap tersedia saat upgrade versi.
