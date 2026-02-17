# MutuGemba - Instruksi Proyek untuk Codex

## Tujuan
MutuGemba adalah aplikasi QC TPS **offline** untuk user awam (target awal: **Desktop Windows**, Android menyusul).
UI Bahasa Indonesia, sederhana, cepat, dan aman. Fokus pada stabilitas, data integrity, dan workflow QC yang jelas.

## Aturan Emas
- Jangan melebar dari MVP tanpa instruksi eksplisit.
- Default: perubahan kecil, aman, dan mudah direview (small diffs).
- Jangan tambah dependensi besar kecuali benar-benar perlu.
- Tidak ada cloud / sync / login / API rumit (offline first).
- Domain tidak bergantung ke UI/DB. UI hanya memanggil UseCase.
- SQLite single file. Attachment di folder (bukan BLOB di DB).

## MVP Scope
1) Input Inspeksi:
   - Mode Cacat (Defect)
   - Mode Parameter Proses / Measurement (CTQ)
2) Tiket Abnormal (Jidoka):
   - DETECT → CONTAIN → CLOSE
3) QCPC:
   - Versi (Draft/Approved/Obsolete) + cetak PDF
4) Analitik:
   - Pareto + Trend (yang lain menyusul)
5) Backup/Restore 1 klik

## Struktur Modul
- app-desktop (Compose Desktop)
- core-domain (entities & rules)
- core-usecase (interactors)
- core-data (SQLite repositories + migrations + attachment store)
- core-analytics (pareto/trend engine)

## Konvensi Arsitektur
- core-domain:
  - Pure Kotlin, tanpa dependensi UI/DB.
  - Validasi rule & invariants di sini semaksimal mungkin.
- core-usecase:
  - Orkestrasi flow: validasi → repository → analytics → output model.
  - Tidak ada Compose/SQL di sini.
- core-data:
  - Implementasi repository (SQLite), migrations, attachment store.
  - Semua I/O & side effects di sini.
- app-desktop:
  - Compose UI, state management, navigation.
  - Tidak ada rule bisnis “baru” di UI (harus lewat usecase).

## Perintah Run & Verify
- Jalankan desktop:
  - `./gradlew :app-desktop:run`
- Build desktop:
  - `./gradlew :app-desktop:build`
- Lint/format:
  - `./gradlew ktlintFormat`
  - `./gradlew ktlintCheck detektAll`
- Test:
  - `./gradlew test`
- Verifikasi cepat (mirip CI):
  - `./gradlew ktlintCheck :app-desktop:compileKotlin test`

Jika membuat perubahan yang menyentuh core-*:
- Tambahkan/ubah unit test (logic non-UI) dan jalankan `./gradlew test`.

## Data Dummy
Jika fitur butuh contoh data:
- Sediakan dummy minimal (mis. 3–10 record) dan jelaskan cara mengaktifkan/menjalankannya.
- Dummy harus bisa dihapus/di-reset tanpa merusak user data.

## Aturan Implementasi Penting
- Error handling harus jelas dan user-friendly (Bahasa Indonesia).
- Jangan menulis data yang “setengah jadi”: gunakan transaksi bila perlu.
- Migration harus idempotent & kompatibel untuk upgrade versi.
- Jangan lakukan kerja berat di UI thread (Compose).

## Output yang Diharapkan dari Codex (wajib setiap PR/patch)
1) Ringkasan singkat apa yang berubah.
2) Daftar file yang diubah / ditambah.
3) Cara verifikasi (perintah Gradle yang harus dijalankan).
4) Jika ada perubahan perilaku: jelaskan sebelum/sesudah + dampaknya.

## Batasan
- Jangan mengubah struktur modul besar-besaran kecuali diminta.
- Jangan menambah fitur baru di luar MVP Scope.
- Jangan menambah network call / telemetry.
