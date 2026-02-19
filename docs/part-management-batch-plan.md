# Part Management Refactor - Batch Plan

Tanggal audit: 19 Februari 2026

## Ringkasan masalah saat ini
- Halaman `Katalog & Master Part` masih menggabungkan dua concern besar dalam satu screen: `catalog/gallery` dan `master CRUD`.
- UX detail part masih berdampingan (split pane), belum flow navigasi fokus `thumbnail -> halaman detail`.
- CRUD master sudah cukup lengkap tetapi feedback field-level, validasi lintas entitas, dan alur konfirmasi belum seragam.
- Sumber data `part/material/supplier/defect` sudah saling terhubung di repository, namun belum divisualkan sebagai graph dampak material -> defect -> part -> supplier.

## Audit data model & distribusi saat ini
- DB lokal (`SqliteDatabase.ensureSupplementalSchema`) sudah memiliki:
  - `supplier` dengan unique normalized name (`supplier_name_norm`).
  - `material` terhubung ke supplier (`supplier_id`) + index supplier.
  - `defect_catalog` dengan origin (`material/process`) + unique normalized name.
  - `material_defect_catalog` (many-to-many material-defect).
  - `part_defect_catalog` (part-defect, optional `material_id`, origin type).
  - `part_configuration` dan `part_recycle_source`.
- Constraint & index penting sudah ada:
  - PK/FK pada tabel relasi.
  - Index untuk origin, line_code, material/supplier relation.
- Distribusi usecase:
  - `PartMasterRepository` sudah menjadi source-of-truth untuk master entitas.
  - `PartMappingScreen` mengonsumsi semua usecase master + catalog dalam satu composable besar.

## Target arsitektur UI yang dituju
- Halaman dipisah secara navigasi:
  1) `Katalog Part` (discover + search + filter + card + gallery).
  2) `Administrasi Master` (part/material/supplier/defect CRUD).
- Flow detail:
  - Klik kartu part di katalog -> navigasi ke detail part (single focus layout).
- Reusable global:
  - Field-level validation feedback.
  - Form row, section card, accordion header, dropdown, confirmation pattern.

## Batch implementasi

### Batch 1 (SELESAI pada commit terkait)
Tujuan:
- Pisah menu operasional menjadi `Katalog Part` dan `Administrasi Master` di sidebar.
- Pertahankan backend/usecase existing agar risiko rendah.

Implementasi:
- Tambah route baru `PartCatalog` dan `PartMaster`.
- `PartMappingScreen` ditambah mode tampilan:
  - `CATALOG_ONLY`
  - `MASTER_ONLY`
  - `ALL` (fallback backward compatibility).
- Routing app:
  - `PartCatalog -> PartMappingScreen(CATALOG_ONLY)`
  - `PartMaster -> PartMappingScreen(MASTER_ONLY)`

Validasi:
- Build aplikasi desktop harus hijau.

---

### Batch 2 (UI split screen internal + detail page)
Tujuan:
- Pisahkan composable besar `PartMappingScreen` menjadi:
  - `PartCatalogScreen`
  - `PartDetailScreen`
  - `PartMasterAdminScreen`
- Flow navigasi thumbnail -> detail menjadi fokus.

Teknis:
- Tambah state navigasi internal `selectedPartId` + mode `LIST/DETAIL`.
- Reusable card katalog (thumbnail, status badge, metadata ringkas).
- Detail layout biodata dengan section:
  - Identitas part
  - Layer material
  - Jenis NG (material/process)
  - Supplier impact summary

Validasi:
- Snapshot UI list/detail.
- UX keyboard (search & enter) tetap berjalan.

---

### Batch 3 (CRUD hardening + field validation)
Tujuan:
- Validasi/sanitasi per field dengan in-field feedback.
- Prevent duplikasi pada key domain penting.

Teknis:
- Domain validation helper:
  - `uniqNo`, `partNumber`, nama material, nama supplier, nama defect.
- Standardized error mapping:
  - Repository error -> user-friendly message + field binding.
- Konfirmasi patch per field (biodata style):
  - edit field -> klik simpan -> dialog -> persist -> feedback.

Validasi:
- Unit test usecase save/update/delete.
- Test duplicate & sanitization path.

---

### Batch 4 (material-defect-part impact graph)
Tujuan:
- Menyesuaikan catatan bisnis bahwa NG terutama berasal dari material/proses.
- Menampilkan dampak material terhadap part dan supplier secara eksplisit.

Teknis:
- Query agregasi:
  - Material -> defects
  - Material -> impacted parts
  - Supplier -> impacted defects/parts
- UI impact panel di detail part dan admin defect/material.
- Label khusus saat defect name sama pada material berbeda.

Validasi:
- SQL query perf check (index usage).
- Golden-path UI checks.

---

### Batch 5 (image repository prep + fast load pipeline)
Tujuan:
- Menyiapkan fondasi pengelolaan image untuk patch lanjutan upload/replace.

Teknis:
- Repository API image:
  - resolve active ref
  - cache metadata
  - quick thumbnail load
- Background prefetch thumbnail katalog.
- Error fallback image placeholder seragam.

Validasi:
- Load-time benchmark sederhana.
- Memory-safe decode path.

---

### Batch 6 (migration hardening + seeder/faker + normalization)
Tujuan:
- Menyiapkan migrasi global aman dan data quality gate.

Teknis:
- Tambah migration guard:
  - re-normalize historical names
  - backfill missing norms
- Seeder/faker untuk QA scenario:
  - part/material/supplier/defect graph dataset.
- Constraint review:
  - unique composite tambahan bila diperlukan untuk mapping tertentu.

Validasi:
- Migration idempotency test.
- Seed load test + rollback test.

## Risiko & mitigasi
- Risiko: perubahan screen besar memicu regression state/UI.
  - Mitigasi: bertahap batch, compile check setiap batch, commit per batch.
- Risiko: validasi baru memblokir data lama.
  - Mitigasi: normalization/backfill dulu sebelum rule ketat diaktifkan.
- Risiko: query impact berat.
  - Mitigasi: index review dan paginasi/filter default.

## Definition of Done per batch
- Refactor sesuai batch scope.
- `:app-desktop:build` hijau.
- Commit per batch dengan pesan jelas.
- Push ke `origin/main`.
- Report perubahan + dampak perilaku.
