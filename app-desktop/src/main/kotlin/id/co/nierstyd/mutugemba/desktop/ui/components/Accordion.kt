package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder

@Composable
fun AppAccordionIndicator(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colors.primary,
    size: Dp = 24.dp,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
    ) {
        Icon(
            imageVector = AppIcons.ExpandMore,
            contentDescription = null,
            tint = accent,
            modifier =
                Modifier
                    .size(size)
                    .padding(2.dp)
                    .graphicsLayer { rotationZ = rotation },
        )
    }
}
