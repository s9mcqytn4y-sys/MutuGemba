# ADR 0001: Arsitektur Awal MutuGemba

Tanggal: 2026-02-05

## Keputusan
- Menggunakan Kotlin + Gradle multi-module untuk menjaga pemisahan domain, use case, data, analytics, dan UI.
- UI desktop memakai Compose Desktop untuk MVP Windows.
- Penyimpanan lokal memakai SQLite single file, dengan attachment di folder terpisah.
- Mengikuti Clean Architecture: domain tidak bergantung ke UI/DB, UI hanya memanggil use case.

## Alasan
- Sederhana, offline, dan mudah dirawat.
- Memudahkan pengujian logic non-UI dan pengembangan bertahap.
