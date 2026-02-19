package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.model.PartDetail

@Composable
internal fun PartDetailScreen(
    selectedUniqNo: String?,
    detail: PartDetail?,
    detailBitmap: ImageBitmap?,
    detailLoading: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Biodata Part",
                    style = MaterialTheme.typography.h6,
                )
                Text(
                    text = "UNIQ ${selectedUniqNo ?: "-"}",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            SecondaryButton(text = "Kembali ke Katalog", onClick = onBack)
        }

        when {
            detailLoading -> {
                Text(
                    "Memuat detail part...",
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }

            detail == null -> {
                Text(
                    AppStrings.PartMapping.EmptyDetail,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }

            else -> {
                PartDetailContent(detail = detail, bitmap = detailBitmap)
            }
        }
    }
}
