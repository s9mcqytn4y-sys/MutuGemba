@file:Suppress("LongMethod", "LongParameterList", "TooManyFunctions", "MaxLineLength")

package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.desktop.ui.util.NumberFormats
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.floor

data class DailyReportPrintMeta(
    val companyName: String,
    val departmentName: String,
)

object DailyReportPdfExporter {
    private val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

    fun export(
        detail: DailyChecksheetDetail,
        meta: DailyReportPrintMeta,
        outputPath: Path,
        colorProfile: ReportColorProfile = ReportColorProfile.EXPORT,
    ): Path {
        Files.createDirectories(outputPath.parent)
        val pdf = PDDocument()
        val palette = paletteFor(colorProfile)
        val pageEntries = paginateEntries(detail.entries)
        pageEntries.forEachIndexed { pageIndex, entries ->
            val page = PDPage(PDRectangle.A4)
            pdf.addPage(page)
            PDPageContentStream(pdf, page).use { content ->
                drawPage(
                    content = content,
                    page = page,
                    detail = detail,
                    entries = entries,
                    meta = meta,
                    pageIndex = pageIndex,
                    pageCount = pageEntries.size,
                    palette = palette,
                )
            }
        }
        pdf.save(outputPath.toFile())
        pdf.close()
        return outputPath
    }

    private fun paginateEntries(entries: List<ChecksheetEntry>): List<List<ChecksheetEntry>> {
        if (entries.isEmpty()) return listOf(emptyList())
        val capacity = rowsPerPage().coerceAtLeast(1)
        return entries.chunked(capacity)
    }

    private fun rowsPerPage(): Int {
        val contentHeight = pageHeight() - mmToPt(24f) - mmToPt(18f)
        val staticHeights = 64f + 38f + 18f + 22f + 52f
        val rowHeight = 18f
        return floor((contentHeight - staticHeights) / rowHeight).toInt().coerceAtLeast(8)
    }

    @Suppress("LongMethod")
    private fun drawPage(
        content: PDPageContentStream,
        page: PDPage,
        detail: DailyChecksheetDetail,
        entries: List<ChecksheetEntry>,
        meta: DailyReportPrintMeta,
        pageIndex: Int,
        pageCount: Int,
        palette: ReportPalette,
    ) {
        val marginLeft = mmToPt(14f)
        val marginRight = mmToPt(14f)
        val marginTop = mmToPt(16f)
        val width = page.mediaBox.width - marginLeft - marginRight
        var y = page.mediaBox.height - marginTop

        drawText(content, meta.companyName, marginLeft, y - 14f, fontBold, 11f)
        drawText(content, meta.departmentName, marginLeft, y - 28f, fontRegular, 9f)
        drawText(
            content,
            AppStrings.Reports.DocumentHeaderTitle,
            marginLeft,
            y - 18f,
            fontBold,
            13f,
            width = width,
            alignCenter = true,
        )
        drawText(
            content,
            AppStrings.Reports.DocumentNumberLabel,
            marginLeft + width - 170f,
            y - 14f,
            fontRegular,
            8f,
            width = 170f,
            alignRight = true,
        )
        drawText(
            content,
            detail.docNumber,
            marginLeft + width - 170f,
            y - 28f,
            fontBold,
            10f,
            width = 170f,
            alignRight = true,
        )
        y -= 44f

        drawMetaRow(
            content = content,
            x = marginLeft,
            y = y,
            width = width,
            label = AppStrings.Reports.DocumentMetaDate,
            value = DateTimeFormats.formatDate(detail.date),
            label2 = AppStrings.Reports.DocumentMetaLine,
            value2 = detail.lineName,
            label3 = AppStrings.Reports.DocumentMetaShift,
            value3 = detail.shiftName,
            label4 = AppStrings.Reports.DocumentMetaPic,
            value4 = detail.picName,
            palette = palette,
        )
        y -= 36f

        val statWidth = (width - 3 * 6f) / 4f
        val ratio = if (detail.totalCheck > 0) detail.totalDefect.toDouble() / detail.totalCheck else 0.0
        drawStatCell(
            content,
            marginLeft,
            y,
            statWidth,
            AppStrings.Reports.DocumentTableTotalCheck,
            detail.totalCheck.toString(),
            palette,
        )
        drawStatCell(
            content,
            marginLeft + statWidth + 6f,
            y,
            statWidth,
            AppStrings.Reports.DocumentTableTotalNg,
            detail.totalDefect.toString(),
            palette,
        )
        drawStatCell(
            content,
            marginLeft + (statWidth + 6f) * 2,
            y,
            statWidth,
            AppStrings.Reports.DocumentTableTotalOk,
            detail.totalOk.toString(),
            palette,
        )
        drawStatCell(
            content,
            marginLeft + (statWidth + 6f) * 3,
            y,
            statWidth,
            AppStrings.Reports.DocumentTableNgRatio,
            NumberFormats.formatPercent(ratio),
            palette,
        )
        y -= 58f

        val noWidth = 24f
        val uniqWidth = 72f
        val numberWidth = 96f
        val nameWidth = 138f
        val checkWidth = 56f
        val ngWidth = 48f
        val okWidth = 48f
        val ratioWidth = width - (noWidth + uniqWidth + numberWidth + nameWidth + checkWidth + ngWidth + okWidth)
        val headerHeight = 18f
        val rowHeight = 18f
        var x = marginLeft
        drawCell(
            content,
            x,
            y,
            noWidth,
            headerHeight,
            "No",
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += noWidth
        drawCell(
            content,
            x,
            y,
            uniqWidth,
            headerHeight,
            AppStrings.Reports.DocumentTablePartUniq,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += uniqWidth
        drawCell(
            content,
            x,
            y,
            numberWidth,
            headerHeight,
            AppStrings.Reports.DocumentTablePartNumber,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += numberWidth
        drawCell(
            content,
            x,
            y,
            nameWidth,
            headerHeight,
            AppStrings.Reports.DocumentTablePartName,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += nameWidth
        drawCell(
            content,
            x,
            y,
            checkWidth,
            headerHeight,
            AppStrings.Reports.DocumentTableTotalCheck,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += checkWidth
        drawCell(
            content,
            x,
            y,
            ngWidth,
            headerHeight,
            AppStrings.Reports.DocumentTableTotalNg,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += ngWidth
        drawCell(
            content,
            x,
            y,
            okWidth,
            headerHeight,
            AppStrings.Reports.DocumentTableTotalOk,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        x += okWidth
        drawCell(
            content,
            x,
            y,
            ratioWidth,
            headerHeight,
            AppStrings.Reports.DocumentTableNgRatio,
            palette.header,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        y -= headerHeight

        entries.forEachIndexed { index, entry ->
            val rowColor = if (index % 2 == 0) java.awt.Color.WHITE else palette.stripe
            val entryRatio = if (entry.totalCheck > 0) entry.totalDefect.toDouble() / entry.totalCheck else 0.0
            var cx = marginLeft
            drawCell(
                content,
                cx,
                y,
                noWidth,
                rowHeight,
                (index + 1 + pageIndex * rowsPerPage()).toString(),
                rowColor,
                fontRegular,
                7.5f,
                palette.border,
                alignCenter = true,
            )
            cx += noWidth
            drawCell(content, cx, y, uniqWidth, rowHeight, entry.uniqCode, rowColor, fontRegular, 7.5f, palette.border)
            cx += uniqWidth
            drawCell(
                content,
                cx,
                y,
                numberWidth,
                rowHeight,
                entry.partNumber,
                rowColor,
                fontRegular,
                7.5f,
                palette.border,
            )
            cx += numberWidth
            drawCell(content, cx, y, nameWidth, rowHeight, entry.partName, rowColor, fontRegular, 7.5f, palette.border)
            cx += nameWidth
            drawCell(
                content,
                cx,
                y,
                checkWidth,
                rowHeight,
                entry.totalCheck.toString(),
                rowColor,
                fontRegular,
                7.5f,
                palette.border,
                alignCenter = true,
            )
            cx += checkWidth
            drawCell(
                content,
                cx,
                y,
                ngWidth,
                rowHeight,
                entry.totalDefect.toString(),
                rowColor,
                fontRegular,
                7.5f,
                palette.border,
                alignCenter = true,
            )
            cx += ngWidth
            drawCell(
                content,
                cx,
                y,
                okWidth,
                rowHeight,
                (entry.totalCheck - entry.totalDefect).coerceAtLeast(0).toString(),
                rowColor,
                fontRegular,
                7.5f,
                palette.border,
                alignCenter = true,
            )
            cx += okWidth
            drawCell(
                content,
                cx,
                y,
                ratioWidth,
                rowHeight,
                NumberFormats.formatPercent(entryRatio),
                rowColor,
                fontRegular,
                7.5f,
                palette.border,
                alignCenter = true,
            )
            y -= rowHeight
        }

        drawCell(
            content,
            marginLeft,
            y,
            noWidth + uniqWidth + numberWidth + nameWidth,
            18f,
            AppStrings.Reports.DocumentTableTotal,
            palette.total,
            fontBold,
            7.5f,
            palette.border,
        )
        var totalX = marginLeft + noWidth + uniqWidth + numberWidth + nameWidth
        drawCell(
            content,
            totalX,
            y,
            checkWidth,
            18f,
            detail.totalCheck.toString(),
            palette.total,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        totalX += checkWidth
        drawCell(
            content,
            totalX,
            y,
            ngWidth,
            18f,
            detail.totalDefect.toString(),
            palette.total,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        totalX += ngWidth
        drawCell(
            content,
            totalX,
            y,
            okWidth,
            18f,
            detail.totalOk.toString(),
            palette.total,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        totalX += okWidth
        drawCell(
            content,
            totalX,
            y,
            ratioWidth,
            18f,
            NumberFormats.formatPercent(ratio),
            palette.total,
            fontBold,
            7.5f,
            palette.border,
            alignCenter = true,
        )
        y -= 24f

        val footer = "Halaman ${pageIndex + 1}/$pageCount"
        drawText(content, footer, marginLeft, y, fontRegular, 7.5f, width = width, alignRight = true)
    }

    private fun drawMetaRow(
        content: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        label: String,
        value: String,
        label2: String,
        value2: String,
        label3: String,
        value3: String,
        label4: String,
        value4: String,
        palette: ReportPalette,
    ) {
        val colWidth = width / 4f
        repeat(4) { index ->
            val left = x + colWidth * index
            drawCell(content, left, y, colWidth, 34f, "", palette.header, fontRegular, 8f, palette.border)
        }
        drawText(content, label, x + 4f, y - 11f, fontRegular, 7.5f)
        drawText(content, value, x + 4f, y - 24f, fontBold, 9f)
        drawText(content, label2, x + colWidth + 4f, y - 11f, fontRegular, 7.5f)
        drawText(content, value2, x + colWidth + 4f, y - 24f, fontBold, 9f)
        drawText(content, label3, x + colWidth * 2 + 4f, y - 11f, fontRegular, 7.5f)
        drawText(content, value3, x + colWidth * 2 + 4f, y - 24f, fontBold, 9f)
        drawText(content, label4, x + colWidth * 3 + 4f, y - 11f, fontRegular, 7.5f)
        drawText(content, value4, x + colWidth * 3 + 4f, y - 24f, fontBold, 9f)
    }

    private fun drawStatCell(
        content: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        label: String,
        value: String,
        palette: ReportPalette,
    ) {
        drawCell(content, x, y, width, 52f, "", palette.subtotal, fontRegular, 7f, palette.border)
        drawText(content, label, x + 4f, y - 14f, fontRegular, 7.5f)
        drawText(content, value, x + 4f, y - 34f, fontBold, 12f)
    }

    private fun drawCell(
        content: PDPageContentStream,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        text: String,
        background: java.awt.Color,
        font: org.apache.pdfbox.pdmodel.font.PDFont,
        fontSize: Float,
        border: java.awt.Color,
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
        if (text.isBlank()) return
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
            while (trimmed.isNotEmpty() && font.getStringWidth(trimmed + ellipsis) / 1000f * fontSize > max) {
                trimmed = trimmed.dropLast(1)
            }
            return if (trimmed.isBlank()) "" else "$trimmed$ellipsis"
        }
        return current
    }

    private fun pageHeight(): Float = PDRectangle.A4.height

    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f
}
