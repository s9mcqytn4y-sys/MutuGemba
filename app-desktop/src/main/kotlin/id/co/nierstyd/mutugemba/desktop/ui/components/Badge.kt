package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun AppBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
        modifier =
            modifier.border(
                1.dp,
                NeutralBorder.copy(alpha = 0.6f),
                MaterialTheme.shapes.small,
            ),
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.caption.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                ),
            color = contentColor,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
        )
    }
}
