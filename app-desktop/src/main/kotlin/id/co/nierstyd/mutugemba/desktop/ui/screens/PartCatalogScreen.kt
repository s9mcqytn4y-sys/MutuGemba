@file:Suppress("LongParameterList", "LongMethod")

package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.FeedbackHost
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.model.PartListItem
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.time.YearMonth

@Composable
internal fun PartCatalogScreen(
    period: YearMonth,
    parts: List<PartListItem>,
    filteredParts: List<PartListItem>,
    selectedUniqNo: String?,
    selectedPartLabel: String,
    assetLoadedCount: Int,
    thumbnailLoading: Boolean,
    thumbnailMap: Map<String, ImageBitmap?>,
    partsLoading: Boolean,
    loadError: String?,
    catalogQuery: String,
    catalogSort: String,
    onCatalogQueryChange: (String) -> Unit,
    onCatalogSortChange: (String) -> Unit,
    onDismissError: () -> Unit,
    onSelectPart: (PartListItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Periode QA",
                value = "${period.monthValue.toString().padStart(2, '0')}-${period.year}",
                hint = "Sumber analitik NG bulanan",
            )
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Total Part Aktif",
                value = parts.size.toString(),
                hint = "Part tersedia untuk line produksi",
            )
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Aset Gambar",
                value = "$assetLoadedCount/${parts.size}",
                hint = if (thumbnailLoading) "Memuat thumbnail..." else "Sinkron dari asset hash store",
            )
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Part Terpilih",
                value = selectedPartLabel,
                hint = "Klik kartu part untuk buka detail penuh",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppTextField(
                spec =
                    FieldSpec(
                        label = "Cari katalog part",
                        placeholder = "Cari UNIQ / part number / nama part",
                    ),
                value = catalogQuery,
                onValueChange = onCatalogQueryChange,
                modifier = Modifier.weight(2f),
                singleLine = true,
            )
            AppTextField(
                spec = FieldSpec(label = "Urutkan (uniq / part_number / ng_desc)"),
                value = catalogSort,
                onValueChange = onCatalogSortChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        loadError?.let { message ->
            FeedbackHost(
                feedback = UserFeedback(FeedbackType.ERROR, "Gagal memuat part: $message"),
                onDismiss = onDismissError,
                dense = true,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = NeutralSurface,
            border = BorderStroke(1.dp, NeutralBorder),
            shape = MaterialTheme.shapes.medium,
            elevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "${AppStrings.PartMapping.PartListTitle} (${filteredParts.size})",
                    style = MaterialTheme.typography.subtitle1,
                )
                Text(
                    text = "Pilih 1 part untuk melihat biodata detail dan aksi CRUD.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )

                when {
                    partsLoading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            repeat(10) {
                                SkeletonBlock(width = 420.dp, height = 72.dp, color = NeutralLight)
                            }
                        }
                    }

                    filteredParts.isEmpty() -> {
                        Text(
                            text = AppStrings.PartMapping.EmptyParts,
                            style = MaterialTheme.typography.body2,
                            color = NeutralTextMuted,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(filteredParts, key = { it.partId }) { item ->
                                PartCard(
                                    item = item,
                                    thumbnail = thumbnailMap[item.uniqNo],
                                    thumbnailLoading = thumbnailLoading && !thumbnailMap.containsKey(item.uniqNo),
                                    selected = item.uniqNo == selectedUniqNo,
                                    onClick = { onSelectPart(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
