package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    val background =
        when (feedback.type) {
            FeedbackType.INFO -> StatusInfo
            FeedbackType.WARNING -> StatusWarning
            FeedbackType.ERROR -> StatusError
            FeedbackType.SUCCESS -> StatusSuccess
        }

    val verticalPadding = if (dense) Spacing.xs else Spacing.sm
    val messageStyle = if (dense) MaterialTheme.typography.caption else MaterialTheme.typography.body2
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .padding(horizontal = Spacing.md, vertical = verticalPadding),
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
                color = MaterialTheme.colors.onPrimary,
            )
            if (onDismiss != null) {
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = AppStrings.Common.Close,
                        tint = MaterialTheme.colors.onPrimary,
                    )
                }
            }
        }
        Text(
            text = feedback.message,
            style = messageStyle,
            color = MaterialTheme.colors.onPrimary,
        )
    }
}
