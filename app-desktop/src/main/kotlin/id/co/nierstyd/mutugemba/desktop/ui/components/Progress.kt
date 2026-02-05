package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning

enum class MilestoneStatus {
    DONE,
    ACTIVE,
    PENDING,
}

data class MilestoneItem(
    val title: String,
    val subtitle: String,
    val status: MilestoneStatus,
)

@Composable
fun MilestonePanel(
    title: String,
    items: List<MilestoneItem>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NeutralSurface,
        elevation = 0.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            items.forEach { item ->
                MilestoneRow(item)
            }
        }
    }
}

@Composable
private fun MilestoneRow(item: MilestoneItem) {
    val indicatorColor =
        when (item.status) {
            MilestoneStatus.DONE -> StatusSuccess
            MilestoneStatus.ACTIVE -> StatusWarning
            MilestoneStatus.PENDING -> NeutralBorder
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(indicatorColor),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(text = item.title, style = MaterialTheme.typography.body1)
            Text(text = item.subtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NeutralSurface,
        elevation = 0.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
        ) {
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            Spacer(modifier = Modifier.height(Spacing.xs))
            content()
        }
    }
}
