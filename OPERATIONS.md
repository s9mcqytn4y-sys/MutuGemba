# Operasional MutuGemba (Offline)

Dokumen ini merangkum cara menjalankan aplikasi, lokasi data, dan prosedur pemeliharaan dasar untuk operasional harian.

## Menjalankan Aplikasi
```
./gradlew :app-desktop:run
```

## Lokasi Data Lokal
- Database SQLite: `data/mutugemba.db`
- Lampiran: `data/attachments/`
- Preferensi: `data/settings.properties`
- Ekspor PDF: `data/exports/`
- Backup: `data/backup/backup-YYYYMMDD-HHmmss/`

Direktori `data/` berada di root project (working directory aplikasi).

## Backup & Restore
- Backup/restore dilakukan dari menu **Pengaturan**.
- Backup akan menyalin database, preferensi, dan lampiran ke `data/backup/`.
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
- File PDF tersimpan di `data/exports/` dengan format nama `MonthlyReport-<LINE>-YYYY-MM.pdf`.

## Upgrade & Migrasi
- Skema database dimigrasikan otomatis menggunakan SQLDelight (`core-data`).
- Pastikan folder `data/` tetap tersedia saat upgrade versi.
