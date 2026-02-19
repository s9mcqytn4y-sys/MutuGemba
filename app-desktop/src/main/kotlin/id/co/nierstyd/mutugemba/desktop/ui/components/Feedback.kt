package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun FeedbackHost(
    feedback: UserFeedback?,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    dense: Boolean = false,
    autoDismissMillis: Long? = null,
    autoDismissTypes: Set<FeedbackType> = setOf(FeedbackType.INFO, FeedbackType.SUCCESS),
    onFeedbackShown: (suspend () -> Unit)? = null,
) {
    LaunchedEffect(feedback?.type, feedback?.message) {
        val currentFeedback = feedback ?: return@LaunchedEffect
        onFeedbackShown?.invoke()
        if (onDismiss != null && autoDismissMillis != null && currentFeedback.type in autoDismissTypes) {
            delay(autoDismissMillis)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = feedback != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
    ) {
        feedback?.let {
            StatusBanner(
                feedback = it,
                modifier = modifier,
                onDismiss = onDismiss,
                dense = dense,
            )
        }
    }
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun StatusBanner(
    feedback: UserFeedback,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    dense: Boolean = false,
) {
    val receivedAt =
        remember(feedback.type, feedback.message) {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        }
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector =
                            when (feedback.type) {
                                FeedbackType.INFO -> AppIcons.Assignment
                                FeedbackType.WARNING -> AppIcons.Abnormal
                                FeedbackType.ERROR -> AppIcons.ErrorOutline
                                FeedbackType.SUCCESS -> AppIcons.CheckCircle
                            },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
                if (onDismiss != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = receivedAt,
                            style = MaterialTheme.typography.caption,
                            color = accent.copy(alpha = 0.9f),
                        )
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
                } else {
                    Text(
                        text = receivedAt,
                        style = MaterialTheme.typography.caption,
                        color = accent.copy(alpha = 0.9f),
                    )
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
