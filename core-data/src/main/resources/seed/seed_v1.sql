-- Seed master data MutuGemba
-- Fokus: Line, Shift, Part, Jenis Cacat, CTQ Parameter

INSERT OR IGNORE INTO master_line(id, name, is_active) VALUES (1, 'Line A', 1);
INSERT OR IGNORE INTO master_line(id, name, is_active) VALUES (2, 'Line B', 1);
INSERT OR IGNORE INTO master_line(id, name, is_active) VALUES (3, 'Line C', 1);

INSERT OR IGNORE INTO master_shift(id, code, name, start_time, end_time, is_active)
VALUES (1, 'S1', 'Shift 1 (Pagi)', '07:00', '15:00', 1);
INSERT OR IGNORE INTO master_shift(id, code, name, start_time, end_time, is_active)
VALUES (2, 'S2', 'Shift 2 (Sore)', '15:00', '23:00', 1);
INSERT OR IGNORE INTO master_shift(id, code, name, start_time, end_time, is_active)
VALUES (3, 'S3', 'Shift 3 (Malam)', '23:00', '07:00', 1);

INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, is_active)
VALUES (1, 'PN-1001', 'Model-A', 'Housing Assy', 'HG-A-001', 'Aluminium', NULL, 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, is_active)
VALUES (2, 'PN-2002', 'Model-B', 'Bracket Support', 'BR-B-002', 'Steel', NULL, 1);
INSERT OR IGNORE INTO master_part(id, part_number, model, name, uniq_code, material, picture_path, is_active)
VALUES (3, 'PN-3003', 'Model-C', 'Cover Plate', 'CP-C-003', 'ABS', NULL, 1);

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

INSERT OR IGNORE INTO master_ctq_parameter(id, code, name, unit, lower_limit, upper_limit, target_value, is_active)
VALUES (1, 'CTQ-001', 'Diameter Luar', 'mm', 9.90, 10.10, 10.00, 1);
INSERT OR IGNORE INTO master_ctq_parameter(id, code, name, unit, lower_limit, upper_limit, target_value, is_active)
VALUES (2, 'CTQ-002', 'Panjang Total', 'mm', 49.80, 50.20, 50.00, 1);
INSERT OR IGNORE INTO master_ctq_parameter(id, code, name, unit, lower_limit, upper_limit, target_value, is_active)
VALUES (3, 'CTQ-003', 'Berat Produk', 'gram', 95.00, 105.00, 100.00, 1);
INSERT OR IGNORE INTO master_ctq_parameter(id, code, name, unit, lower_limit, upper_limit, target_value, is_active)
VALUES (4, 'CTQ-004', 'Kekerasan', 'HRC', 28.00, 34.00, 31.00, 1);
