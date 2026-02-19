@file:Suppress("ConstPropertyName")

package id.co.nierstyd.mutugemba.desktop.ui.resources

import java.time.LocalDate
import java.time.YearMonth

object AppStrings {
    object App {
        const val Name = "MutuGemba"
        const val WindowTitle = "MutuGemba - PT. Primaraya QC TPS v1.2.0"
        const val IdentityTagline = "Platform QC TPS Offline - Industrial Modern"
        const val BuildVersion = "v1.2.0"
        const val HeaderBadge = "QC TPS Harian"
        const val CompanyName = "PT. Primaraya Graha Nusantara"
        const val DepartmentName = "Quality Assurance Dept."
        const val CompanyMotto = "RESPONSIBLE | COMMIT | TRUSTED"
        const val CompanyAddress = "Jl. Raya Bekasi KM.17 No.5, Jatinegara Kaum, Pulogadung, Jakarta Timur 13250"
        const val CompanyPhone = "+62-21-4788 3012"
        const val CompanyEmail = "primaraya.granusa@yahoo.co.id"
        const val CustomerName = "Customer"
        const val Offline = "Offline"
        const val OfflineStatus = "Offline - Lokal"
        const val StatusOffline = "Status: Offline - Lokal"
        const val ReadOnly = "Baca Saja"
        const val Auto = "Auto"
        const val Logo = "LOGO"
        const val DemoMode = "Mode Demo"
        const val DummyData = "Data Dummy"

        fun footerPage(label: String): String = "Halaman: $label"
    }

    object Navigation {
        const val Home = "Beranda"
        const val PartMapping = "Katalog & Master Part"
        const val Inspection = "Input Inspeksi"
        const val Abnormal = "Tiket Abnormal"
        const val Reports = "Laporan"
        const val ReportsDaily = "Laporan Harian"
        const val ReportsMonthly = "Laporan Bulanan"
        const val Settings = "Pengaturan"
    }

    object Common {
        const val Date = "Tanggal"
        const val Line = "Line"
        const val Shift = "Shift"
        const val Pic = "PIC"
        const val TotalCheck = "Total Periksa"
        const val TotalNg = "Total NG"
        const val TotalOk = "Total OK"
        const val NgRatio = "Rasio NG"
        const val PartRecorded = "Part Tercatat"
        const val LineActive = "Line Aktif"
        const val Input = "Input"
        const val Detail = "Detail"
        const val Select = "Pilih"
        const val Info = "Info"
        const val Warning = "Peringatan"
        const val Error = "Gagal"
        const val Success = "Sukses"
        const val Close = "Tutup"
        const val Placeholder = "-"
        const val Zero = "0"

        fun lastInput(label: String?): String = "Input terakhir: ${label ?: Placeholder}"

        fun lineCoverage(
            active: Int,
            total: Int,
        ): String = "$active dari $total line tercatat hari ini."

        fun dateRange(
            start: Int,
            end: Int,
        ): String = "Tanggal $start-$end"

        fun monthlyTitle(month: YearMonth): String = "Bulan $month"
    }

    object Actions {
        const val StartInspection = "Mulai Input Inspeksi"
        const val ResetInput = "Reset Riwayat Input"
        const val ResetData = "Reset Data"
        const val Reset = "Reset"
        const val Cancel = "Batal"
        const val ConfirmSave = "Konfirmasi & Simpan"
        const val SaveNow = "Simpan Sekarang"
        const val ClearAll = "Bersihkan Semua"
        const val Create = "Buat"
        const val Continue = "Lanjut"
        const val Backup = "Backup Data"
        const val Restore = "Pulihkan Data"
        const val PrintDocument = "Cetak Dokumen"
        const val ExportPdf = "Ekspor PDF"
        const val ViewFullDocument = "Lihat Dokumen Lengkap"
        const val CurrentMonth = "Bulan Ini"
        const val Previous = "Sebelumnya"
        const val Next = "Berikutnya"
        const val PreviousMonth = "Bulan Sebelumnya"
        const val NextMonth = "Bulan Berikutnya"
        const val UseManualSelection = "Gunakan Pilihan Manual"
        const val RunLoadSimulation = "Simulasi Beban Data"
        const val ClearCache = "Bersihkan Cache"
    }

    object Home {
        const val Title = "Beranda"
        const val Subtitle = "Dashboard hasil input checksheet harian."
        const val AnalyticsTitle = "Sorotan Analitik QC"
        const val AnalyticsSubtitle = "Pareto & trend NG untuk evaluasi bulanan."
        const val OpsTitle = "Operasional Hari Ini"
        const val OpsSubtitle = "Status input dan aktivitas QC."
        const val SummaryTitle = "Ringkasan Checksheet Hari Ini"
        const val SummarySubtitle = "Data terakumulasi sepanjang shift berjalan."
        const val SummaryLastInput = "Input terakhir"
        const val MetricChecksheet = "Checksheet Masuk"
        const val MetricTotalNg = "Total NG"
        const val MetricNgRatio = "Rasio NG"
        const val MetricParts = "Part Tercatat"
        const val MetricLineCoverage = "Line Terisi"
        const val MetricTotalCheck = "Total Periksa"
        const val DailySummaryTitle = "Ringkasan Harian"
        const val DailySummarySubtitle = "Ringkas data input harian untuk evaluasi cepat."
        const val DailyLineCoverage = "Cakupan Line"
        const val ActivityTitle = "Aktivitas QC Hari Ini"
        const val ActivitySubtitle = "Pantau aktivitas input QC untuk evaluasi cepat."
        const val ActivityMostActive = "Line Aktif"
        const val ActivityMostPart = "Part Terbanyak"
        const val ActionTitle = "Aksi Cepat"
        const val ActionSubtitle = "Mulai input inspeksi atau kosongkan data simulasi."
        const val LineStatusTitle = "Status Input Hari Ini"
        const val LineStatusNotInput = "Belum Diinput"
        const val LineStatusDone = "Sudah Diinput"

        fun summaryDateLabel(dateLabel: String): String = "Tanggal $dateLabel - Data terakumulasi sepanjang shift."

        fun lineStatusSubtitle(date: String): String = "Ringkasan checksheet per line ($date)."

        fun lineStatusLastInput(time: String): String = "Input Terakhir $time"

        fun partsRecorded(total: Int): String = "$total part tercatat"

        fun docsRecorded(total: Int): String = "$total dokumen harian"

        fun lineRecorded(
            active: Int,
            total: Int,
        ): String = "$active/$total"

        fun ratioPercent(percent: String): String = percent

        const val ResetDialogTitle = "Reset Riwayat Input"
        const val ResetDialogMessage = "Semua data inspeksi akan dihapus. Lanjutkan?"
        const val ResetSuccess = "Riwayat input berhasil direset."
        const val ResetFailed = "Reset riwayat gagal. Coba ulangi."
    }

    object Inspection {
        const val Title = "Input Inspeksi Harian"
        const val Subtitle = "Masukkan data checksheet harian secara cepat dan terstruktur."
        const val IntroTitle = "Panduan Singkat"
        const val IntroStep1 = "Pilih line produksi dan tanggal kerja aktual."
        const val IntroStep2 = "Isi hanya part yang benar-benar diproduksi, wajib isi total check."
        const val IntroStep3 = "Verifikasi ringkasan OK/NG lalu simpan final."
        const val ContextTitle = "Konteks Inspeksi"
        const val ContextSubtitle = "Pastikan tanggal dan shift sesuai kondisi produksi hari ini."
        const val ContextBanner =
            "Input checksheet hanya untuk hari ini. Riwayat sebelumnya bersifat final dan tidak bisa diubah."
        const val LineTitle = "Line Produksi"
        const val LineSubtitle = "Pilih area produksi yang akan diinput hari ini."
        const val PartTitle = "Checksheet Per Part"
        const val PartSubtitle = "Isi part yang diproduksi hari ini. Part yang tidak diproduksi boleh dibiarkan kosong."
        const val SummaryTitle = "Ringkasan Checksheet"
        const val SummarySubtitle = "Terisi otomatis dari input yang sudah dimasukkan."
        const val SummaryAuto = "Auto"
        const val SummaryOk = "OK"
        const val SummaryNg = "NG"
        const val DefaultShiftLabel = "Shift 1 (08:00-17:00 WIB)"
        const val EmptyPartTitle = "Belum ada part untuk line ini."
        const val EmptyPartSubtitle = "Periksa master part di pengaturan atau pilih line lain."
        const val ConfirmTitle = "Konfirmasi Simpan"
        const val ConfirmSubtitle = "Pastikan data sudah benar. Setelah disimpan, data tidak bisa diubah."
        const val ConfirmPartTitle = "Part Terisi"
        const val ConfirmFinal = "FINAL"
        const val ConfirmFinalHint = "Data yang disimpan akan dianggap final."
        const val HeaderDate = "Tanggal"
        const val HeaderPic = "PIC"
        const val HeaderShift = "Shift Aktif"
        const val TotalCheckLabel = "Total Periksa"
        const val TotalCheckPlaceholder = "Contoh: 120"
        const val CheckLabel = "Periksa"
        const val TotalNgLabel = "Total NG"
        const val TotalOkLabel = "Total OK"
        const val NgRatioLabel = "Rasio NG"
        const val PartFilledLabel = "Part Terisi"
        const val TotalNgHint = "Total NG"
        const val TotalOkHint = "Total OK"
        const val DuplicateAllowed = "Input Ulang Line Diizinkan"
        const val DuplicateBlocked = "Input Ulang Line Diblokir"
        const val DuplicateHintOn = "Line yang sama boleh diinput lebih dari sekali di hari yang sama."
        const val DuplicateHintOff = "Jika line sudah tersimpan hari ini, line tersebut tidak bisa diinput ulang."
        const val MasterDataHintTitle = "INFO"
        const val MasterDataHint = "Part, material, dan item defect divalidasi dari data real (mapping + Daily NG)."
        const val LineHintDefault = "Part akan muncul otomatis sesuai line."

        fun lineHintQc(lineName: String): String = "Line QC otomatis: $lineName (dikunci dari Pengaturan)."

        fun totalCheckHelper(valid: Boolean): String =
            if (valid) {
                "Jumlah pemeriksaan hari ini untuk part ini."
            } else {
                "Total periksa harus lebih besar atau sama dengan total NG."
            }

        fun partHeaderLabel(
            partNumber: String,
            uniqCode: String,
        ): String = "$partNumber - UNIQ $uniqCode"

        fun partStatus(label: String): String = label

        fun partTotals(
            totalNg: Int,
            totalOk: Int,
        ): String = "NG $totalNg - OK $totalOk"

        fun toggleDetail(expanded: Boolean): String = if (expanded) "Sembunyikan" else "Buka"

        fun partSummaryItem(
            partNumber: String,
            partName: String,
        ): String = "$partNumber - $partName"

        fun summaryRatioTitle(
            left: String,
            right: String,
        ): String = "$left vs $right"

        fun partInputError(): String = "Periksa total periksa yang kurang dari total NG."

        fun partRequired(): String = "Isi minimal satu part sebelum disimpan."

        fun lineRequired(): String = "Pilih line produksi terlebih dahulu."

        const val SearchPartLabel = "Cari Part"
        const val SearchPartPlaceholder = "Cari UNIQ / part number / nama / material"
        const val SearchPartHint =
            "Pencarian pintar: cocokkan kata kunci pada UNIQ, nama part, nomor part, dan material."

        const val CustomDefectTitle = "Tambah Jenis NG"
        const val CustomDefectLabel = "Jenis NG Tambahan"
        const val CustomDefectPlaceholder = "Contoh: DIMENSI TIDAK SIMETRIS"
        const val CustomDefectAddButton = "Tambah"
        const val CustomDefectEmpty = "Isi nama Jenis NG terlebih dahulu."

        fun customDefectHint(lineName: String): String = "Jenis NG baru akan diterapkan untuk line $lineName."

        fun customDefectAdded(name: String): String = "Jenis NG '$name' berhasil ditambahkan."

        fun failedPartLine(
            partNumber: String,
            partName: String,
            reason: String,
        ): String = "- $partNumber $partName : $reason"

        fun partialSaveFailed(details: String): String = "Sebagian data gagal disimpan:\n$details"

        const val ErrorLineRequired = "Pilih line produksi terlebih dahulu."
        const val ErrorPartRequired = "Isi minimal satu part sebelum disimpan."
        const val ErrorTotalCheckInvalid = "Periksa total periksa yang kurang dari total NG."
        const val PartStatusComplete = "Lengkap"
        const val PartStatusWarning = "Perlu Cek"
        const val PartStatusError = "Perbaiki"
        const val PartStatusPartial = "Sebagian"
        const val PartStatusNotProduced = "Belum Produksi"
        const val PartImagePlaceholder = "Foto\nPart"
        const val PartImageDescription = "Foto Part"
        const val TableDefectType = "Jenis NG"
        const val TableTotal = "Total"
        const val TableSubtotal = "Sub-total"
        const val SummaryRatioOk = "OK"
        const val SummaryRatioNg = "NG"
        const val ConfirmChartTitle = "Komposisi OK vs NG"
    }

    object Reports {
        const val Title = "Laporan Checksheet Harian"
        const val Subtitle = "Riwayat dan dokumen checksheet harian untuk evaluasi QC."
        const val HistoryTitle = "Riwayat Checksheet Bulan Ini"
        const val HistorySubtitle = "Riwayat bersifat read-only."
        const val HistoryHint =
            "Ganti tanggal di bar di atas untuk melihat riwayat. Tanggal masa depan tidak bisa dibuka."
        const val HistoryInfo = "Data checksheet harian bersifat final. QC dapat menandai hari libur secara manual."
        const val LineHistoryLabel = "Pilih Line (Riwayat)"
        const val LineHistoryHint = "Riwayat hanya menampilkan dokumen yang sudah tersimpan."
        const val LegendFilled = "Sudah Input"
        const val LegendEmpty = "Belum Diinput"
        const val LegendWeekend = "Akhir Pekan/Libur"
        const val LegendToday = "Hari ini"
        const val LegendSelected = "Tanggal terpilih"
        const val LegendManualHoliday = "Libur manual"
        const val ToggleHoliday = "Tandai Libur"
        const val ToggleHolidayOff = "Batalkan Libur"
        const val NoInput = "Belum ada input"
        const val HolidayLabel = "Libur"
        const val LineCountHint = "Angka kecil di tanggal menunjukkan jumlah input per line."
        const val DocumentPreviewTitle = "Pratinjau Dokumen Ringkas"
        const val PreviewBeforePrint = "Pratinjau Ringkas"
        const val DocumentFull = "Dokumen Lengkap"
        const val DocumentCountLabel = "Menampilkan"
        const val DocumentHeaderTitle = "CHECKSHEET HARIAN"
        const val DocumentNumberLabel = "Nomor Dokumen"
        const val DocumentTotalsTitle = "Ringkasan Statistik Harian"
        const val DocumentTotalsPart = "Part dengan NG terbanyak"
        const val DocumentTotalsRatio = "Rasio NG part tertinggi"
        const val DocumentTotalsOverall = "Rasio NG total hari ini"
        const val DocumentTotalsDefect = "Jenis NG terbanyak"
        const val DocumentMetaDate = "Tanggal"
        const val DocumentMetaLine = "Line"
        const val DocumentMetaShift = "Shift"
        const val DocumentMetaPic = "PIC"
        const val DocumentTablePartUniq = "Part UNIQ"
        const val DocumentTablePartNumber = "Part Number"
        const val DocumentTablePartName = "Part Name"
        const val DocumentTableTotalCheck = "Total Periksa"
        const val DocumentTableTotalNg = "Total NG"
        const val DocumentTableTotalOk = "Total OK"
        const val DocumentTableNgRatio = "Rasio NG"
        const val DocumentTableTotal = "Total"
        const val DocumentSignaturePrepared = "Prepared"
        const val DocumentSignatureChecked = "Checked"
        const val DocumentSignatureApproved = "Approved"
        const val DocumentSignatureName = "Nama & TTD"
        const val DocumentEmptyTitle = "Belum ada data checksheet"
        const val DocumentEmptySubtitle = "Pilih tanggal yang memiliki input untuk melihat dokumen."
        const val DocumentLoading = "Memuat data checksheet..."
        const val DailyDocumentTitle = "Dokumen Checksheet Harian"
        const val DailyDocumentSubtitle = "Tampilan dokumen untuk tanggal yang dipilih."
        const val MonthRangeLabel = "Tanggal"

        fun historyMonthLabel(label: String): String = "Bulan $label - Riwayat bersifat read-only."

        fun monthlyRangeLabel(
            start: Int,
            end: Int,
        ): String = "Tanggal $start-$end"

        fun pageLabel(
            current: Int,
            total: Int,
        ): String = "Halaman $current / $total"

        fun filledCount(
            filled: Int,
            total: Int,
        ): String = "Terisi $filled/$total"

        fun holidayCount(total: Int): String = "Libur $total"

        fun previewCount(
            shown: Int,
            total: Int,
        ): String = "Menampilkan $shown dari $total part."

        fun dailyDocumentLabel(dateLabel: String): String = "Rincian dokumen untuk $dateLabel."

        fun documentNotFound(dateLabel: String): String = "Tidak ditemukan data checksheet untuk $dateLabel."

        fun ngCheckTooltip(
            totalDefect: Int,
            totalCheck: Int,
        ): String = "NG $totalDefect - Periksa $totalCheck"

        fun ngCountLabel(totalDefect: Int): String = "NG $totalDefect"

        fun tooltipHoliday(dateLabel: String): String = "Libur manual: $dateLabel"

        fun tooltipWeekend(dateLabel: String): String = "Weekend: $dateLabel"

        fun tooltipStored(dateLabel: String): String = "Ada data: $dateLabel"

        fun tooltipEmpty(dateLabel: String): String = "Belum ada data: $dateLabel"

        fun ratioLabel(ratio: String): String = ratio
    }

    object ReportsMonthly {
        const val Title = "Laporan Bulanan"
        const val Subtitle = "Dokumen bulanan berbasis checksheet harian (read-only)."
        const val InfoSync = "Data disinkronkan otomatis dari checksheet harian. Semua perubahan mengikuti data harian."
        const val InfoHoliday = "Akhir pekan terdeteksi otomatis. QC dapat menandai hari libur manual."
        const val DocumentTitlePress = "Laporan Bulanan Press"
        const val DocumentTitleSewing = "Laporan Bulanan Sewing"
        const val DocumentNoLabel = "Nomor Dokumen"
        const val DocumentMonthLabel = "Bulan"
        const val DocumentCustomerLabel = "Pelanggan"
        const val DocumentPicLabel = "PIC"
        const val DocumentLineLabel = "Line"
        const val DocumentMultiplePic = "PIC Lebih dari 1"
        const val TableSketch = "Sketch"
        const val TablePartNumber = "Part Number"
        const val TableProblemItem = "Problem Item"
        const val TableDates = "Tanggal"
        const val TableTotals = "Total"
        const val TableSubtotal = "Subtotal"
        const val TableGrandTotal = "Total Keseluruhan"
        const val TableTotalNg = "Total NG"
        const val RangePrefix = "Part"
        const val Of = "dari"
        const val CompactMode = "Mode Ringkas Part"
        const val ShowAllParts = "Tampilkan Semua Part"
        const val PreviousPartPage = "Part Sebelumnya"
        const val NextPartPage = "Part Berikutnya"
        const val PagePrefix = "Halaman"
        const val FilterAllDates = "Semua Tanggal"
        const val FilterInputDates = "Terisi"
        const val FilterHolidayDates = "Libur"
        const val FilterEmptyDates = "Belum Diinput"
        const val OpenAllColumns = "Buka Semua Kolom"
        const val NoProblemLabel = "Tanpa Keterangan"
        const val LegendInput = "Terisi"
        const val LegendHoliday = "Akhir Pekan/Libur"
        const val LegendEmpty = "Belum Diinput"
        const val LegendGrandTotal = "Total Keseluruhan"
        const val LegendSubtotal = "Subtotal"
        const val MetricActivePart = "Part Aktif"
        const val MetricActiveDay = "Hari Aktif"
        const val EmptyTitle = "Belum ada data bulanan"
        const val EmptySubtitle = "Data akan muncul setelah checksheet harian tersimpan."
        const val ExportSuccess = "PDF laporan bulanan berhasil dibuat."
        const val ExportFailed = "Gagal membuat PDF laporan bulanan."
        const val PrintSuccess = "Dokumen siap dicetak."
        const val PrintFailed = "Gagal memicu proses cetak."
    }

    object Abnormal {
        const val Title = "Tiket Abnormal"
        const val Subtitle = "Alur DETECT -> CONTAIN -> CLOSE."
        const val Description = "Gunakan tiket abnormal untuk mencatat isu kualitas yang perlu tindakan cepat."
        const val CreateDummy = "Buat Tiket Dummy"
        const val Note = "Catatan: halaman ini masih dummy untuk MVP."
        const val DialogTitle = "Tiket Abnormal"
        const val DialogMessage = "Buat tiket abnormal dummy?"
    }

    object Settings {
        const val Title = "Pengaturan"
        const val Subtitle = "Atur aturan inspeksi dan preferensi aplikasi."
        const val RulesTitle = "Aturan Inspeksi"
        const val RulesSubtitle = "Aturan standar untuk checksheet harian."
        const val RulesBadge = "ATURAN"
        const val RulesDescription = "Saat aturan aktif, line yang sudah tersimpan hari ini tidak bisa diinput ulang."
        const val SimTitle = "Simulasi QC"
        const val SimSubtitle = "Pengaturan simulasi untuk QA dan pengembangan."
        const val LineQcLabel = "Line QC Aktif"
        const val LineQcHint = "Input inspeksi akan auto-select line ini (tetap bisa diubah)."
        const val LineManualHint = "Line QC dikembalikan ke mode manual."
        const val DemoModeTitle = "Mode Demo"
        const val DemoModeSubtitle = "Aktifkan untuk tampilan demo dan simulasi presentasi."
        const val DummyDataTitle = "Gunakan Data Dummy"
        const val DummyDataSubtitle = "Aktifkan untuk menampilkan data contoh di dashboard."
        const val DuplicateTitle = "Izinkan Input Ulang per Line di Hari yang Sama"
        const val DuplicateSubtitle = "Saat nonaktif, line yang sudah tersimpan hari ini akan terkunci."
        const val MaintenanceTitle = "Pemeliharaan Data"
        const val MaintenanceSubtitle = "Reset, backup, dan restore data lokal."
        const val MaintenanceBody =
            "Gunakan tombol di bawah untuk mengosongkan data inspeksi dan mengisi ulang data awal."
        const val BackupHint = "Backup akan menyimpan database, settings, lampiran, dan assets image."
        const val RestoreHint = "Restore akan menggunakan backup terbaru (aplikasi perlu dibuka ulang)."
        const val DevToolsHint =
            "Dev tools: simulasi beban data untuk uji performa dan bersihkan cache runtime."
        const val LoadSimulationBusy = "Menjalankan simulasi beban data..."
        const val CacheBusy = "Membersihkan cache aplikasi..."
        const val ResetDialogTitle = "Reset Data"
        const val ResetDialogMessage = "Semua data inspeksi akan dihapus dan diisi ulang dengan data awal. Lanjutkan?"
        const val BackupSuccess = "Backup berhasil dibuat."
        const val RestoreSuccess = "Restore selesai. Aplikasi perlu dimuat ulang."
    }

    object PartMapping {
        const val Title = "Katalog & Administrasi Part"
        const val Subtitle = "Katalog part produksi, galeri part, dan administrasi data master."
        const val Filters = "Filter Part"
        const val SearchLabel = "Cari UNIQ / part number / nama"
        const val LineLabel = "Line"
        const val ModelLabel = "Model"
        const val MonthLabel = "Periode QA (YYYY-MM)"
        const val PartListTitle = "Daftar Part"
        const val DetailTitle = "Detail Part"
        const val DashboardTitle = "Dashboard Defect"
        const val EmptyParts = "Belum ada part. Pastikan data tersedia di data/part_assets/extracted."
        const val EmptyDetail = "Pilih salah satu part untuk melihat detail."
        const val TopDefects = "Top Defects per Model"
        const val Heatmap = "Heatmap Tanggal vs Defect"
        const val ImportBannerOffline = "Offline mode: data dibaca dari SQLite lokal + asset hash store."
    }

    object Login {
        const val Title = "Selamat Datang"
        const val Subtitle = "Masuk untuk melanjutkan ke MutuGemba QC TPS."
        const val CardTitle = "Login"
        const val NameLabel = "Nama"
        const val NamePlaceholder = "Masukkan nama user"
        const val PasswordLabel = "Password"
        const val PasswordPlaceholder = "Masukkan password"
        const val Action = "Masuk"
        const val Tip = "Tip: gunakan akun resmi QC untuk akses yang aman."
    }

    object Analytics {
        const val InsightTitle = "Ringkasan Bulan Ini"
        const val InsightSubtitle = "Ringkasan checksheet harian."
        const val ParetoTitle = "Pareto NG Bulan Ini"
        const val ParetoSubtitle = "Kontribusi NG terbesar & kumulatif."
        const val TrendTitle = "Trend NG Harian"
        const val TrendSubtitle = "Pergerakan NG harian untuk monitoring stabilitas proses."
        const val TopProblemTitle = "Problem Item Teratas"
        const val TopProblemSubtitle = "Part dengan NG tertinggi bulan ini."
        const val LineCompareTitle = "Perbandingan Line"
        const val LineCompareSubtitle = "Total NG dan rasio NG per line."
        const val TopDominant = "Top 3 NG Dominan"
        const val NoData = "Belum ada data NG bulan ini."
        const val All = "Semua"
        const val DaysFilled = "Hari Terisi"
        const val Documents = "Dokumen"
        const val EmptyDays = "Hari Kosong"
        const val NgPerDoc = "NG/Dokumen"
        const val NgPerDay = "NG per Hari"
        const val NgRatio = "Rasio NG"

        fun partRecorded(total: Int): String = "Part tercatat: $total"

        fun monthLabel(label: String): String = "$label - Ringkasan checksheet harian."

        fun paretoSubtitle(label: String): String = "Kontribusi NG terbesar & kumulatif ($label)."

        fun topDominantItem(
            label: String,
            count: Int,
        ): String = "$label ($count)"

        fun valueWithPercent(
            value: Int,
            percent: String,
        ): String = "$value ($percent)"

        fun cumulativeLabel(percent: String): String = "Kumulatif $percent"

        const val TrendFootnote = "Tanggal di bawah grafik menunjukkan hari ke-1 s/d akhir bulan. Titik = NG harian."
    }

    object DefectSeverity {
        const val Normal = "Normal"
        const val Critical = "Kritis"
    }

    object Feedback {
        const val BackupSuccess = "Backup berhasil dibuat."
        const val BackupFailed = "Backup gagal. Coba ulangi."
        const val RestoreFailed = "Restore gagal. Periksa file backup."
        const val ResetSuccess = "Data berhasil direset. Master data telah dipulihkan."
        const val ResetFailed = "Reset data gagal. Coba ulangi."
        const val DummyEnabled = "Data dummy diaktifkan."
        const val DummyDisabled = "Data dummy dimatikan."
        const val DemoEnabled = "Mode demo diaktifkan."
        const val DemoDisabled = "Mode demo dimatikan."
        const val DuplicateAllowed = "Input ulang line di hari yang sama diizinkan."
        const val DuplicateBlocked = "Input ulang line di hari yang sama diblokir."

        fun simulationInserted(inserted: Int): String = "Simulasi selesai. $inserted record inspeksi ditambahkan."

        const val SimulationFailed = "Simulasi gagal dijalankan."
        const val CacheCleared = "Cache runtime berhasil dibersihkan."
        const val CacheClearFailed = "Gagal membersihkan cache runtime."

        fun lineQcUpdated(name: String): String = "Line QC diubah ke $name."

        const val lineQcManual = "Line QC dikembalikan ke mode manual."
    }

    object DateLabels {
        fun dayLabel(date: LocalDate): String = date.toString()
    }
}
