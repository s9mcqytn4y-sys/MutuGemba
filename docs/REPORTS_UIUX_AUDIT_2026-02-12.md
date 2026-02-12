# Audit Laporan UI/UX - 2026-02-12

## Flow Audit
1. Laporan Harian:
- Alur inti sudah benar: pilih line -> pilih tanggal -> preview dokumen -> full document.
- Read-only behavior sesuai scope MVP.
- Kelemahan sebelumnya: visual header dokumen masih placeholder logo, tabel belum optimal untuk scan cepat.

2. Laporan Bulanan:
- Alur inti benar: pilih bulan + line -> filter -> print/export.
- Kelemahan sebelumnya: identitas dokumen kurang kuat, beberapa alignment tabel terasa padat.

## Perbaikan yang diterapkan
1. Header dokumen harian dan bulanan kini memakai logo mark nyata (bukan placeholder text).
2. Tabel harian ditingkatkan:
- Tambah kolom `No`.
- Wrap dan ellipsis lebih baik pada kolom part number / part name.
- Alignment angka tetap center agar mudah audit.
3. Format part number di dokumen bulanan diperjelas (`partNumber (uniqCode)`).

## Rekomendasi lanjutan
1. Tambah nomor revisi dokumen dan cap timestamp cetak/export.
2. Tambah opsi sort di preview harian (NG tertinggi / rasio tertinggi).
3. Tambah indikator quality status per baris bulanan (warna ringan, tetap printable).
4. Pisahkan mode layar vs mode print agar tata letak PDF lebih ketat (A4-first layout).
