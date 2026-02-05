package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.CompactNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.InfoCard
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Part

@Composable
internal fun DefectTableContent(
    state: InputStepState,
    actions: InputStepActions,
) {
    val totalOkLabel = state.totalCheck?.let { state.totalOk.toString() } ?: "-"
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppNumberField(
                spec =
                    FieldSpec(
                        label = "Total Check",
                        placeholder = "0",
                        helperText = "Jumlah pemeriksaan hari ini.",
                    ),
                value = state.totalCheckInput,
                onValueChange = actions.onTotalCheckChanged,
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                title = "Total NG",
                value = state.totalDefectQuantity.toString(),
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                title = "Total OK",
                value = totalOkLabel,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PartSummaryCard(part = state.selectedPart, modifier = Modifier.width(220.dp))
            DefectTableGrid(
                defectTypes = state.defectTypes,
                timeSlots = state.timeSlots,
                values = state.defectSlotValues,
                onValueChange = actions.onDefectSlotChanged,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    InfoCard(title = title, modifier = modifier) {
        Text(value, color = NeutralText)
    }
}

@Composable
private fun PartSummaryCard(
    part: Part?,
    modifier: Modifier = Modifier,
) {
    InfoCard(title = "Part Image", modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .size(140.dp)
                    .background(NeutralLight)
                    .border(1.dp, NeutralBorder),
            contentAlignment = Alignment.Center,
        ) {
            Text("Part\nImage", color = NeutralTextMuted)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("UNIQ Part: ${part?.uniqCode ?: "-"}")
        Text("Part Number: ${part?.partNumber ?: "-"}")
        Text("Nama: ${part?.name ?: "-"}")
    }
}

@Composable
private fun DefectTableGrid(
    defectTypes: List<DefectType>,
    timeSlots: List<InspectionTimeSlot>,
    values: Map<DefectSlotKey, String>,
    onValueChange: (Long, InspectionTimeSlot, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun slotValue(
        defectId: Long,
        slot: InspectionTimeSlot,
    ): String = values[DefectSlotKey(defectId, slot)] ?: ""

    fun slotQuantity(
        defectId: Long,
        slot: InspectionTimeSlot,
    ): Int = slotValue(defectId, slot).toIntOrNull() ?: 0

    fun rowTotal(defectId: Long): Int = timeSlots.sumOf { slotQuantity(defectId, it) }

    fun columnTotal(slot: InspectionTimeSlot): Int = defectTypes.sumOf { defect -> slotQuantity(defect.id, slot) }

    val totalNg = defectTypes.sumOf { defect -> rowTotal(defect.id) }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TableHeaderCell(text = "Jenis NG", weight = 1.4f)
            timeSlots.forEach { slot ->
                TableHeaderCell(text = slot.label, weight = 1f)
            }
            TableHeaderCell(text = "Total", weight = 0.7f)
        }

        defectTypes.forEach { defect ->
            Row(modifier = Modifier.fillMaxWidth()) {
                TableCell(
                    text = "${defect.code} - ${defect.name}",
                    weight = 1.4f,
                )
                timeSlots.forEach { slot ->
                    TableInputCell(
                        value = slotValue(defect.id, slot),
                        onValueChange = { input -> onValueChange(defect.id, slot, input) },
                        weight = 1f,
                    )
                }
                TableCell(text = rowTotal(defect.id).toString(), weight = 0.7f, alignCenter = true)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TableFooterCell(text = "Sub-total", weight = 1.4f)
            timeSlots.forEach { slot ->
                TableFooterCell(text = columnTotal(slot).toString(), weight = 1f, alignCenter = true)
            }
            TableFooterCell(text = totalNg.toString(), weight = 0.7f, alignCenter = true)
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(
    text: String,
    weight: Float,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = NeutralText)
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralSurface)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text, color = NeutralText)
    }
}

@Composable
private fun RowScope.TableInputCell(
    value: String,
    onValueChange: (String) -> Unit,
    weight: Float,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralSurface)
                .padding(Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        CompactNumberField(
            value = value,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun RowScope.TableFooterCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text, color = NeutralText)
    }
}
