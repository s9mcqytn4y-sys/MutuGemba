package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    val background =
        when (feedback.type) {
            FeedbackType.INFO -> StatusInfo
            FeedbackType.WARNING -> StatusWarning
            FeedbackType.ERROR -> StatusError
            FeedbackType.SUCCESS -> StatusSuccess
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text = feedback.message,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onPrimary,
        )
    }
}
