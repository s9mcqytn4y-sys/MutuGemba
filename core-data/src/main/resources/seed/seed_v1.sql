-- Seed master data MutuGemba
-- Fokus: Line, Shift, Part, dan Jenis NG.

INSERT OR IGNORE INTO master_line(id, code, name, is_active)
VALUES (1, 'PRESS', 'Press', 1);
INSERT OR IGNORE INTO master_line(id, code, name, is_active)
VALUES (2, 'SEWING', 'Sewing', 1);

INSERT OR IGNORE INTO master_shift(id, code, name, start_time, end_time, is_active)
VALUES (1, 'S1', 'Shift 1 (08:00-17:00 WIB)', '08:00', '17:00', 1);

INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (1, 'PN-1001', 'Press-A', 'Housing Assy', 'HG-A-001', 'Aluminium', NULL, 'PRESS', 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (2, 'PN-1002', 'Press-B', 'Bracket Base', 'BR-B-002', 'Steel', NULL, 'PRESS', 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (3, 'PN-1003', 'Press-C', 'Cover Plate', 'CP-C-003', 'ABS', NULL, 'PRESS', 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (4, 'PN-1004', 'Press-D', 'Arm Link', 'AL-D-004', 'Carbon Steel', NULL, 'PRESS', 1);

INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (5, 'PN-2001', 'Sew-A', 'Strap Holder', 'SH-A-005', 'Fabric', NULL, 'SEWING', 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (6, 'PN-2002', 'Sew-B', 'Bracket Support', 'BS-B-006', 'Steel', NULL, 'SEWING', 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (7, 'PN-2003', 'Sew-C', 'Cushion Pad', 'CP-C-007', 'Foam', NULL, 'SEWING', 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, line_code, is_active)
VALUES (8, 'PN-2004', 'Sew-D', 'Label Cover', 'LC-D-008', 'Fabric', NULL, 'SEWING', 1);

INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (1, 'DF-001', 'Goresan', 'Permukaan', 'NORMAL', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (2, 'DF-002', 'Retak', 'Struktur', 'KRITIS', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (3, 'DF-003', 'Dimensi Tidak Sesuai', 'Dimensi', 'KRITIS', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (4, 'DF-004', 'Burr / Serabut', 'Finishing', 'NORMAL', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (5, 'DF-005', 'Kotoran / Kontaminasi', 'Kebersihan', 'NORMAL', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (6, 'DF-006', 'Karat', 'Permukaan', 'KRITIS', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (7, 'DF-007', 'Jahitan Loncat', 'Jahitan', 'NORMAL', 1);
INSERT OR IGNORE INTO master_defect_type(id, code, name, category, severity, is_active)
VALUES (8, 'DF-008', 'Lubang', 'Material', 'KRITIS', 1);

-- Tidak ada seed untuk data inspeksi agar database selalu kosong saat awal.

INSERT OR IGNORE INTO app_user(
  id,
  name,
  password_hash,
  password_salt,
  employee_id,
  full_name,
  position,
  department,
  line_code,
  role,
  is_active,
  photo_path,
  created_at
)
VALUES (
  1,
  'admin',
  'dev_hash',
  'dev_salt',
  'QC-001',
  'Admin QC',
  'QC Inspector',
  'Quality Assurance',
  'PRESS',
  'ADMIN',
  1,
  NULL,
  '2026-02-01T08:00:00'
);
