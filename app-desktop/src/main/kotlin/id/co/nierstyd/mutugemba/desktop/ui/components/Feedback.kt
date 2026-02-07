package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback

@Composable
fun StatusBanner(
    feedback: UserFeedback,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    dense: Boolean = false,
) {
    val accent =
        when (feedback.type) {
            FeedbackType.INFO -> StatusInfo
            FeedbackType.WARNING -> StatusWarning
            FeedbackType.ERROR -> StatusError
            FeedbackType.SUCCESS -> StatusSuccess
        }

    val background = accent.copy(alpha = if (dense) 0.12f else 0.16f)
    val borderColor = accent.copy(alpha = 0.35f)
    val verticalPadding = if (dense) 6.dp else Spacing.sm
    val messageStyle = if (dense) MaterialTheme.typography.caption else MaterialTheme.typography.body2
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .border(1.dp, borderColor, MaterialTheme.shapes.small),
    ) {
        Box(
            modifier =
                Modifier
                    .width(4.dp)
                    .background(accent),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
        val label =
            when (feedback.type) {
                FeedbackType.INFO -> AppStrings.Common.Info
                FeedbackType.WARNING -> AppStrings.Common.Warning
                FeedbackType.ERROR -> AppStrings.Common.Error
                FeedbackType.SUCCESS -> AppStrings.Common.Success
            }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
            if (onDismiss != null) {
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = AppStrings.Common.Close,
                        tint = accent,
                    )
                }
            }
        }
        Text(
            text = feedback.message,
            style = messageStyle,
            color = MaterialTheme.colors.onSurface,
        )
        }
    }
}
