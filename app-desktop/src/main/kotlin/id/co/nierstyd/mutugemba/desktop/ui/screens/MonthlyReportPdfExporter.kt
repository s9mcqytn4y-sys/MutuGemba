@file:Suppress("LongMethod", "LongParameterList", "MaxLineLength")

package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import id.co.nierstyd.mutugemba.domain.MonthlyReportRow
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

data class MonthlyReportPrintMeta(
    val companyName: String,
    val departmentName: String,
    val customerName: String,
    val documentTitle: String,
)

object MonthlyReportPdfExporter {
    private val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

    fun export(
        document: MonthlyReportDocument,
        meta: MonthlyReportPrintMeta,
        outputPath: Path,
        manualHolidays: Set<LocalDate>,
        colorProfile: ReportColorProfile = ReportColorProfile.EXPORT,
    ): Path {
        Files.createDirectories(outputPath.parent)
        val pdf = PDDocument()
        val palette = paletteFor(colorProfile)
        val pageSpecs = buildPageSpecs(document, manualHolidays)
        pageSpecs.forEach { spec ->
            val page = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
            pdf.addPage(page)
            PDPageContentStream(pdf, page).use { content ->
                renderPage(
                    content = content,
                    page = page,
                    document = document,
                    meta = meta,
                    spec = spec,
                    palette = palette,
                )
            }
        }
        pdf.save(outputPath.toFile())
        pdf.close()
        return outputPath
    }

    private data class PageSpec(
        val days: List<LocalDate>,
        val rows: List<MonthlyReportRow>,
        val manualHolidays: Set<LocalDate>,
    )

    private fun buildPageSpecs(
        document: MonthlyReportDocument,
        manualHolidays: Set<LocalDate>,
    ): List<PageSpec> {
        val pagedRows = paginateRowsForPage(document.rows)
        if (pagedRows.isEmpty()) {
            return listOf(
                PageSpec(
                    days = document.days,
                    rows = emptyList(),
                    manualHolidays = manualHolidays,
                ),
            )
        }
        return pagedRows.map { chunk ->
            PageSpec(
                days = document.days,
                rows = chunk,
                manualHolidays = manualHolidays,
            )
        }
    }

    private fun paginateRowsForPage(rows: List<MonthlyReportRow>): List<List<MonthlyReportRow>> {
        if (rows.isEmpty()) return emptyList()
        val availableHeight = tableBodyHeightBudget()
        val rowHeight = 18f
        val subtotalHeight = 16f
        val groupedRows =
            rows
                .groupBy { it.partId }
                .toList()
                .sortedBy { (_, partRows) -> partRows.firstOrNull()?.partNumber ?: "" }

        val pages = mutableListOf<MutableList<MonthlyReportRow>>()
        var currentPage = mutableListOf<MonthlyReportRow>()
        var usedHeight = 0f

        groupedRows.forEach { (_, partRows) ->
            val (nextPage, nextUsedHeight) =
                appendPartRows(
                    currentPage = currentPage,
                    usedHeight = usedHeight,
                    pages = pages,
                    partRows = partRows,
                    availableHeight = availableHeight,
                    rowHeight = rowHeight,
                    subtotalHeight = subtotalHeight,
                )
            currentPage = nextPage
            usedHeight = nextUsedHeight
        }
        if (currentPage.isNotEmpty()) {
            pages += currentPage
        }
        return pages
    }

    private fun appendPartRows(
        currentPage: MutableList<MonthlyReportRow>,
        usedHeight: Float,
        pages: MutableList<MutableList<MonthlyReportRow>>,
        partRows: List<MonthlyReportRow>,
        availableHeight: Float,
        rowHeight: Float,
        subtotalHeight: Float,
    ): Pair<MutableList<MonthlyReportRow>, Float> {
        var mutablePage = currentPage
        var mutableUsedHeight = usedHeight
        val partBlockHeight = partRows.size * rowHeight + subtotalHeight
        if (partBlockHeight <= availableHeight) {
            val (nextPage, nextUsedHeight) =
                appendChunk(
                    currentPage = mutablePage,
                    usedHeight = mutableUsedHeight,
                    pages = pages,
                    chunkRows = partRows,
                    chunkHeight = partBlockHeight,
                    availableHeight = availableHeight,
                )
            mutablePage = nextPage
            mutableUsedHeight = nextUsedHeight
            return mutablePage to mutableUsedHeight
        }

        val chunkSize = ((availableHeight - subtotalHeight) / rowHeight).toInt().coerceAtLeast(1)
        partRows.chunked(chunkSize).forEach { partChunk ->
            val chunkHeight = partChunk.size * rowHeight + subtotalHeight
            val (nextPage, nextUsedHeight) =
                appendChunk(
                    currentPage = mutablePage,
                    usedHeight = mutableUsedHeight,
                    pages = pages,
                    chunkRows = partChunk,
                    chunkHeight = chunkHeight,
                    availableHeight = availableHeight,
                )
            mutablePage = nextPage
            mutableUsedHeight = nextUsedHeight
        }
        return mutablePage to mutableUsedHeight
    }

    private fun appendChunk(
        currentPage: MutableList<MonthlyReportRow>,
        usedHeight: Float,
        pages: MutableList<MutableList<MonthlyReportRow>>,
        chunkRows: List<MonthlyReportRow>,
        chunkHeight: Float,
        availableHeight: Float,
    ): Pair<MutableList<MonthlyReportRow>, Float> {
        var mutablePage = currentPage
        var mutableUsedHeight = usedHeight
        if (mutablePage.isNotEmpty() && mutableUsedHeight + chunkHeight > availableHeight) {
            pages += mutablePage
            mutablePage = mutableListOf()
            mutableUsedHeight = 0f
        }
        mutablePage.addAll(chunkRows)
        mutableUsedHeight += chunkHeight
        return mutablePage to mutableUsedHeight
    }

    private fun tableBodyHeightBudget(): Float {
        val contentHeight = pageContentHeight()
        val headerHeight = 64f
        val metaHeight = 32f
        val tableHeaderHeight = 36f
        val footerHeight = 70f
        return (contentHeight - headerHeight - metaHeight - tableHeaderHeight - footerHeight - 8f).coerceAtLeast(34f)
    }

    private fun renderPage(
        content: PDPageContentStream,
        page: PDPage,
        document: MonthlyReportDocument,
        meta: MonthlyReportPrintMeta,
        spec: PageSpec,
        palette: ReportPalette,
    ) {
        val marginLeft = mmToPoint(14f)
        val marginTop = mmToPoint(16f)
        val marginRight = mmToPoint(14f)
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height
        val contentWidth = pageWidth - marginLeft - marginRight
        var cursorY = pageHeight - marginTop

        cursorY = drawHeader(content, marginLeft, cursorY, contentWidth, meta, document)
        cursorY = drawMeta(content, marginLeft, cursorY, contentWidth, meta, document)
        cursorY =
            drawTable(
                content = content,
                left = marginLeft,
                top = cursorY,
                width = contentWidth,
                document = document,
                spec = spec,
                palette = palette,
            )
        drawSignature(content, marginLeft, cursorY, contentWidth, palette)
    }

    private fun drawHeader(
        content: PDPageContentStream,
        left: Float,
        top: Float,
        width: Float,
        meta: MonthlyReportPrintMeta,
        document: MonthlyReportDocument,
    ): Float {
        val headerHeight = 64f
        val right = left + width
        val title = meta.documentTitle
        val company = meta.companyName
        val dept = meta.departmentName

        drawText(content, company, left, top - 18f, fontBold, 11f, width = width * 0.4f)
        drawText(content, dept, left, top - 32f, fontRegular, 9f, width = width * 0.4f)
        drawText(
            content,
            title,
            left + width * 0.4f,
            top - 20f,
            fontBold,
            13f,
            width = width * 0.2f,
            alignCenter = true,
        )

        val docNoLabel = AppStrings.ReportsMonthly.DocumentNoLabel
        val docNo = document.header.documentNumber
        drawText(content, docNoLabel, right - 160f, top - 18f, fontRegular, 8f, width = 160f, alignRight = true)
        drawText(content, docNo, right - 160f, top - 30f, fontBold, 10f, width = 160f, alignRight = true)

        return top - headerHeight
    }

    private fun drawMeta(
        content: PDPageContentStream,
        left: Float,
        top: Float,
        width: Float,
        meta: MonthlyReportPrintMeta,
        document: MonthlyReportDocument,
    ): Float {
        val metaHeight = 32f
        val right = left + width
        val monthLabel = document.header.month.toString()
        drawText(content, AppStrings.ReportsMonthly.DocumentMonthLabel, left, top - 14f, fontRegular, 8f, width = 80f)
        drawText(content, monthLabel, left, top - 26f, fontBold, 9f, width = 80f)

        drawText(
            content,
            AppStrings.ReportsMonthly.DocumentCustomerLabel,
            left + 90f,
            top - 14f,
            fontRegular,
            8f,
            width = 120f,
        )
        drawText(content, meta.customerName, left + 90f, top - 26f, fontBold, 9f, width = 120f)

        drawText(
            content,
            AppStrings.ReportsMonthly.DocumentPicLabel,
            right - 180f,
            top - 14f,
            fontRegular,
            8f,
            width = 80f,
            alignRight = true,
        )
        drawText(
            content,
            document.header.picName,
            right - 180f,
            top - 26f,
            fontBold,
            9f,
            width = 80f,
            alignRight = true,
        )

        drawText(
            content,
            AppStrings.ReportsMonthly.DocumentLineLabel,
            right - 80f,
            top - 14f,
            fontRegular,
            8f,
            width = 80f,
            alignRight = true,
        )
        drawText(
            content,
            document.header.lineName,
            right - 80f,
            top - 26f,
            fontBold,
            9f,
            width = 80f,
            alignRight = true,
        )

        return top - metaHeight
    }

    private fun drawTable(
        content: PDPageContentStream,
        left: Float,
        top: Float,
        width: Float,
        document: MonthlyReportDocument,
        spec: PageSpec,
        palette: ReportPalette,
    ): Float {
        val headerHeight = 18f
        val subHeaderHeight = 18f
        val rowHeight = 18f
        val subtotalHeight = 16f
        val totalHeight = 18f

        val noWidth = 26f
        val sketchWidth = 78f
        val partWidth = 124f
        val problemWidth = 182f
        val totalWidth = 38f
        val dayCount = spec.days.size.coerceAtLeast(1)
        val fixedWidth = noWidth + sketchWidth + partWidth + problemWidth + totalWidth
        val dayWidth = ((width - fixedWidth) / dayCount).coerceAtLeast(9.2f)

        var cursorY = top
        val headerBackground = palette.header
        val subtotalBackground = palette.subtotal

        val leftHeaderHeight = headerHeight + subHeaderHeight
        drawCell(
            content,
            left,
            cursorY,
            noWidth,
            leftHeaderHeight,
            "No",
            headerBackground,
            fontBold,
            7.5f,
            alignCenter = true,
        )
        drawCell(
            content,
            left + noWidth,
            cursorY,
            sketchWidth,
            leftHeaderHeight,
            AppStrings.ReportsMonthly.TableSketch,
            headerBackground,
            fontBold,
            7.5f,
            alignCenter = true,
        )
        drawCell(
            content,
            left + noWidth + sketchWidth,
            cursorY,
            partWidth,
            leftHeaderHeight,
            AppStrings.ReportsMonthly.TablePartNumber,
            headerBackground,
            fontBold,
            7.5f,
            alignCenter = true,
        )
        drawCell(
            content,
            left + noWidth + sketchWidth + partWidth,
            cursorY,
            problemWidth,
            leftHeaderHeight,
            AppStrings.ReportsMonthly.TableProblemItem,
            headerBackground,
            fontBold,
            7.5f,
            alignCenter = true,
        )

        val rightStart = left + noWidth + sketchWidth + partWidth + problemWidth
        val dayWidthTotal = dayCount * dayWidth

        drawCell(
            content,
            rightStart,
            cursorY,
            dayWidthTotal,
            headerHeight,
            AppStrings.ReportsMonthly.TableDates,
            headerBackground,
            fontBold,
            7.5f,
            alignCenter = true,
        )
        drawCell(
            content,
            rightStart + dayWidthTotal,
            cursorY,
            totalWidth,
            headerHeight,
            AppStrings.ReportsMonthly.TableTotalNg,
            headerBackground,
            fontBold,
            7.5f,
            alignCenter = true,
        )

        cursorY -= headerHeight

        spec.days.forEachIndexed { index, date ->
            val isHoliday = isHoliday(date, spec.manualHolidays)
            val dayBackground = if (isHoliday) Color(255, 244, 229) else headerBackground
            drawCell(
                content,
                rightStart + (index * dayWidth),
                cursorY,
                dayWidth,
                subHeaderHeight,
                date.dayOfMonth.toString(),
                dayBackground,
                fontBold,
                7f,
                alignCenter = true,
            )
        }

        drawCell(
            content,
            rightStart + dayWidthTotal,
            cursorY,
            totalWidth,
            subHeaderHeight,
            AppStrings.ReportsMonthly.TableTotalNg,
            headerBackground,
            fontBold,
            7f,
            alignCenter = true,
        )

        cursorY -= subHeaderHeight

        val groupedRows =
            spec.rows
                .groupBy { it.partId }
                .toList()
                .sortedBy { (_, rows) -> rows.firstOrNull()?.partNumber ?: "" }
        groupedRows.forEachIndexed { partIndex, (_, partRows) ->
            val partDayTotals = MutableList(dayCount) { 0 }
            var partTotal = 0
            val rowBackground = if (partIndex % 2 == 0) Color.WHITE else palette.stripe
            val sketchHeight = rowHeight * partRows.size
            val sample = partRows.firstOrNull()

            drawCell(
                content,
                left,
                cursorY,
                noWidth,
                sketchHeight,
                (partIndex + 1).toString(),
                rowBackground,
                fontRegular,
                7f,
                alignCenter = true,
            )

            drawCell(
                content,
                left + noWidth,
                cursorY,
                sketchWidth,
                sketchHeight,
                sample?.uniqCode ?: "-",
                rowBackground,
                fontRegular,
                7f,
                alignCenter = true,
            )

            partRows.forEachIndexed { rowIndex, row ->
                val partLabel = if (rowIndex == 0) "${row.partNumber}(${row.uniqCode})" else ""
                drawCell(
                    content,
                    left + noWidth + sketchWidth,
                    cursorY,
                    partWidth,
                    rowHeight,
                    partLabel,
                    rowBackground,
                    fontRegular,
                    7.5f,
                )
                drawCell(
                    content,
                    left + noWidth + sketchWidth + partWidth,
                    cursorY,
                    problemWidth,
                    rowHeight,
                    formatProblemItems(row.problemItems),
                    rowBackground,
                    fontRegular,
                    7.5f,
                )

                row.dayValues.take(dayCount).forEachIndexed { i, value ->
                    partDayTotals[i] += value
                    drawCell(
                        content,
                        rightStart + (i * dayWidth),
                        cursorY,
                        dayWidth,
                        rowHeight,
                        value.toString(),
                        rowBackground,
                        fontRegular,
                        7f,
                        alignCenter = true,
                    )
                }
                partTotal += row.totalDefect
                drawCell(
                    content,
                    rightStart + dayWidthTotal,
                    cursorY,
                    totalWidth,
                    rowHeight,
                    row.totalDefect.toString(),
                    rowBackground,
                    fontRegular,
                    7f,
                    alignCenter = true,
                )

                cursorY -= rowHeight
            }

            drawCell(
                content,
                left,
                cursorY,
                noWidth + sketchWidth + partWidth,
                subtotalHeight,
                "",
                subtotalBackground,
                fontRegular,
                7f,
            )
            drawCell(
                content,
                left + noWidth + sketchWidth + partWidth,
                cursorY,
                problemWidth,
                subtotalHeight,
                AppStrings.ReportsMonthly.TableSubtotal,
                subtotalBackground,
                fontBold,
                7f,
            )
            partDayTotals.forEachIndexed { i, value ->
                drawCell(
                    content,
                    rightStart + (i * dayWidth),
                    cursorY,
                    dayWidth,
                    subtotalHeight,
                    value.toString(),
                    subtotalBackground,
                    fontBold,
                    7f,
                    alignCenter = true,
                )
            }
            drawCell(
                content,
                rightStart + dayWidthTotal,
                cursorY,
                totalWidth,
                subtotalHeight,
                partTotal.toString(),
                subtotalBackground,
                fontBold,
                7f,
                alignCenter = true,
            )

            cursorY -= subtotalHeight
        }

        val totalLabelWidth = sketchWidth + partWidth + problemWidth
        drawCell(
            content,
            left,
            cursorY,
            noWidth + totalLabelWidth,
            totalHeight,
            AppStrings.ReportsMonthly.TableGrandTotal,
            palette.total,
            fontBold,
            7.5f,
        )
        document.totals.dayTotals.take(dayCount).forEachIndexed { i, value ->
            drawCell(
                content,
                rightStart + (i * dayWidth),
                cursorY,
                dayWidth,
                totalHeight,
                value.toString(),
                subtotalBackground,
                fontBold,
                7f,
                alignCenter = true,
            )
        }
        drawCell(
            content,
            rightStart + dayWidthTotal,
            cursorY,
            totalWidth,
            totalHeight,
            document.totals.totalDefect.toString(),
            subtotalBackground,
            fontBold,
            7f,
            alignCenter = true,
        )

        cursorY -= totalHeight
        return cursorY
    }

    private fun drawSignature(
        content: PDPageContentStream,
        left: Float,
        top: Float,
        width: Float,
        palette: ReportPalette,
    ) {
        val boxWidth = 110f
        val boxHeight = 46f
        val gap = 16f
        val labels =
            listOf(
                AppStrings.Reports.DocumentSignaturePrepared,
                AppStrings.Reports.DocumentSignatureChecked,
                AppStrings.Reports.DocumentSignatureApproved,
            )
        val startX = left + width - (boxWidth * labels.size) - (gap * (labels.size - 1))
        var x = startX
        val y = top - 64f
        labels.forEach { label ->
            drawText(content, label, x, y + boxHeight + 12f, fontRegular, 8f, width = boxWidth, alignCenter = true)
            drawCell(
                content,
                x,
                y + boxHeight,
                boxWidth,
                boxHeight,
                "",
                palette.stripe,
                fontRegular,
                7f,
                palette.border,
            )
            drawText(
                content,
                AppStrings.Reports.DocumentSignatureName,
                x,
                y - 4f,
                fontRegular,
                7f,
                width = boxWidth,
                alignCenter = true,
            )
            x += boxWidth + gap
        }
    }

    private fun drawCell(
        content: PDPageContentStream,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        text: String,
        background: Color,
        font: org.apache.pdfbox.pdmodel.font.PDFont,
        fontSize: Float,
        border: Color = Color(200, 200, 200),
        alignCenter: Boolean = false,
        alignRight: Boolean = false,
    ) {
        val bottom = top - height
        content.setNonStrokingColor(background)
        content.addRect(left, bottom, width, height)
        content.fill()
        content.setStrokingColor(border)
        content.addRect(left, bottom, width, height)
        content.stroke()

        if (text.isNotBlank()) {
            val clipped = fitText(text, font, fontSize, width - 4f)
            val textWidth = font.getStringWidth(clipped) / 1000f * fontSize
            val textX =
                when {
                    alignRight -> left + width - textWidth - 2f
                    alignCenter -> left + (width - textWidth) / 2f
                    else -> left + 2f
                }
            val textY = bottom + (height - fontSize) / 2f + 2f
            drawText(content, clipped, textX, textY, font, fontSize)
        }
    }

    private fun drawText(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        font: org.apache.pdfbox.pdmodel.font.PDFont,
        fontSize: Float,
        width: Float? = null,
        alignCenter: Boolean = false,
        alignRight: Boolean = false,
    ) {
        val clipped = if (width != null) fitText(text, font, fontSize, width) else text
        val textWidth = font.getStringWidth(clipped) / 1000f * fontSize
        val textX =
            when {
                alignRight && width != null -> x + width - textWidth
                alignCenter && width != null -> x + (width - textWidth) / 2f
                else -> x
            }
        content.beginText()
        content.setFont(font, fontSize)
        content.newLineAtOffset(textX, y)
        content.showText(clipped)
        content.endText()
    }

    private fun fitText(
        text: String,
        font: org.apache.pdfbox.pdmodel.font.PDFont,
        fontSize: Float,
        maxWidth: Float,
    ): String {
        var current = text
        val max = maxWidth.coerceAtLeast(4f)
        val ellipsis = "..."
        while (current.isNotEmpty() && font.getStringWidth(current) / 1000f * fontSize > max) {
            current = current.dropLast(1)
        }
        if (current.length < text.length) {
            var trimmed = current
            while (trimmed.isNotEmpty() &&
                font.getStringWidth(trimmed + ellipsis) / 1000f * fontSize > max
            ) {
                trimmed = trimmed.dropLast(1)
            }
            return if (trimmed.isEmpty()) "" else trimmed + ellipsis
        }
        return current
    }

    private fun pageContentHeight(): Float {
        val pageHeight = PDRectangle.A4.width
        val margin = mmToPoint(16f) * 2
        return pageHeight - margin
    }

    private fun mmToPoint(mm: Float): Float = mm * 72f / 25.4f

    private fun isHoliday(
        date: LocalDate,
        manualHolidays: Set<LocalDate>,
    ): Boolean {
        val weekend = date.dayOfWeek.value >= 6
        return weekend || manualHolidays.contains(date)
    }

    private fun formatProblemItems(items: List<String>): String {
        if (items.isEmpty()) return "-"
        val normalized =
            items
                .flatMap(DefectNameSanitizer::expandProblemItems)
                .ifEmpty { items.map(DefectNameSanitizer::canonicalKey) }
                .filter { it.isNotBlank() }
                .distinct()
        if (normalized.isEmpty()) return "-"
        if (normalized.size <= 2) return normalized.joinToString(", ")
        val head = normalized.take(2).joinToString(", ")
        return "$head +${normalized.size - 2}"
    }
}
