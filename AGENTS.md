# MutuGemba - Instruksi Proyek untuk Codex

## Ringkasan
MutuGemba adalah aplikasi QC TPS OFFLINE (desktop Windows dulu, Android nanti). UI Bahasa Indonesia, sederhana, cepat, dan aman untuk user awam.

## MVP Scope (jangan melebar)
1) Input Inspeksi:
   - Mode Cacat (Defect)
   - Mode Parameter Proses / Measurement (CTQ)
2) Tiket Abnormal (Jidoka):
   - DETECT -> CONTAIN -> CLOSE
3) QCPC:
   - Versi (Draft/Approved/Obsolete) + cetak PDF
4) Analitik:
   - Pareto + Trend (yang lain menyusul)
5) Backup/Restore 1 klik

## Prinsip
- No cloud, no API rumit, no fitur muluk-muluk.
- Domain tidak bergantung ke UI/DB.
- UI hanya memanggil UseCase.
- SQLite single file, attachments di folder.

## Struktur Modul
- app-desktop (Compose Desktop)
- core-domain (entities & rules)
- core-usecase (interactors)
- core-data (SQLite repositories + migrations + attachment store)
- core-analytics (pareto/trend engine)

## Output yang diharapkan dari Codex
- Kerjakan bertahap dan sebut file yang diubah.
- Sertakan cara run: ./gradlew :app-desktop:run
- Sertakan data dummy minimal.
- Tambahkan unit test untuk logic non-UI jika relevan.
