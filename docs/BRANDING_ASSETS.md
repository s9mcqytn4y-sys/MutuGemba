# Branding & Static Assets

Dokumen ini menjelaskan aset identitas aplikasi yang diambil dari dokumen resmi:

- Sumber: `C:\Users\Acer\Downloads\General Compro PT. Primaraya 2022 email.pdf`
- Tujuan: memperkuat identitas resmi PT. Primaraya Graha Nusantara di aplikasi MutuGemba.

## Aset yang digunakan

Semua aset disimpan di `app-desktop/src/main/resources/branding/`:

- `pt_prima_logo.png`: logo lockup (ikon + nama perusahaan)
- `pt_prima_mark.png`: logo mark untuk elemen UI ringkas (header/sidebar)
- `app_icon_512.png`: ikon aplikasi untuk window runtime
- `app_icon.ico`: ikon native distribution Windows

## Pipeline ekstraksi

Aset diekstrak dari PDF menggunakan `PyMuPDF` + `Pillow`, lalu dituning agar:

1. background abu-abu/transparan dibersihkan,
2. lockup dan mark dipotong ke area relevan,
3. icon Windows (`.ico`) digenerate multi-size.

Folder `branding/source_pdf/` dipakai sebagai workbench lokal saat ekstraksi dan tidak wajib di-commit.

## Palet tema (Industrial Modern)

Token warna disesuaikan dengan logo perusahaan:

- `BrandBlue`: `#2E3192`
- `BrandBlueDark`: `#1F246D`
- `BrandRed`: `#ED1C24`
- `BrandRedDark`: `#B4141A`

Warna netral dibuat lebih industrial (abu kebiruan) agar dashboard dan form terlihat resmi.

## Catatan penggunaan

- Aset branding hanya untuk konteks aplikasi internal MutuGemba.
- Jika PDF sumber diperbarui, regenerasi aset pada folder branding agar identitas tetap sinkron.
