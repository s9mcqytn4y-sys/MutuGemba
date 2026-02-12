# Audit Gradle - 2026-02-12

## Ringkasan
- Build toolchain valid untuk JVM Desktop: Gradle `8.7`, JDK `17`, Kotlin plugin `2.1.20`, Compose plugin `1.8.0`.
- Struktur multi-module sudah sehat: `app-desktop`, `core-domain`, `core-usecase`, `core-data`, `core-analytics`.
- Version catalog (`gradle/libs.versions.toml`) sudah dipakai konsisten.

## Temuan
1. `build` gagal bukan karena compile, tapi karena rule `detekt` lama yang belum dibaseline ulang.
2. Root build script masih menyertakan repository Compose dev (`maven.pkg.jetbrains.space/public/p/compose/dev`) walau versi stabil dipakai.
3. Belum ada optimasi `configuration-cache` / `build-cache` yang eksplisit di `gradle.properties`.
4. Task verifikasi harian belum dibedakan antara fast-check dan full-quality gate (sudah ditambah di `.vscode/tasks.json`).

## Rekomendasi
1. Short-term: gunakan `./gradlew ktlintCheck :app-desktop:compileKotlin test` untuk health check rutin.
2. Medium-term: refactor hotspot detekt (long method/cyclomatic) lalu refresh baseline agar `./gradlew build` kembali hijau.
3. Medium-term: evaluasi apakah repository Compose dev masih diperlukan; jika tidak, hapus untuk mengurangi risiko dependency drift.
4. Medium-term: aktifkan cache tuning di `gradle.properties` setelah validasi CI (`org.gradle.caching=true`, `org.gradle.configuration-cache=true`).

## Command referensi
- Run app: `./gradlew :app-desktop:run`
- Fast verify: `./gradlew ktlintCheck :app-desktop:compileKotlin test`
- Full verify: `./gradlew clean ktlintCheck detektAll test :app-desktop:build`
