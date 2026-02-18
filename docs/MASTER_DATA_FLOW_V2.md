# Master Data Flow v2

Dokumen ini menjadi style guide tekstual dan alur data untuk sistem part/bahan/Jenis NG.

## Istilah Resmi (Bahasa Indonesia)
- `Part`: komponen hasil proses line produksi.
- `Bahan`: material penyusun part.
- `Pemasok`: supplier bahan.
- `Jenis NG`: daftar jenis ketidaksesuaian.
- `Asal NG`: kategori `material` atau `proses`.
- `Checksheet`: dokumen input inspeksi harian.
- `Sub-total`: jumlah per kelompok part.
- `Total NG`: jumlah NG agregat.

## Alur Data Utama
1. `Administrasi Master`
   - Input/ubah data `Part`, `Bahan`, `Pemasok`, `Jenis NG`.
   - Mapping `Part -> Layer Bahan`.
   - Mapping `Part -> Jenis NG`.
2. `Input Inspeksi`
   - QC memilih line dan part yang aktif.
   - Hanya `Part` yang tidak ditandai `kecualikan checksheet` yang tampil.
   - Part line `press` dengan bahan `strap` dikecualikan dari checksheet.
3. `Laporan Harian`
   - Ringkasan total periksa/NG/OK per tanggal.
4. `Laporan Bulanan`
   - Akumulasi harian menjadi dokumen bulanan.
5. `Dashboard`
   - Menampilkan KPI, pareto, dan tren dari data harian/bulanan.

## Kontrol Integrasi
- Semua operasi master data wajib lewat repository `PartMasterRepository`.
- Operasi batch assignment bahan/Jenis NG wajib transaksional.
- Skema database v12 memiliki self-healing untuk tabel master tambahan:
  - `supplier`
  - `defect_catalog`
  - `part_configuration`
  - `part_defect_catalog`
  - `part_recycle_source`

