package id.co.nierstyd.mutugemba.desktop.ui.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.components.SidebarMenu
import id.co.nierstyd.mutugemba.desktop.ui.theme.BackgroundBottom
import id.co.nierstyd.mutugemba.desktop.ui.theme.BackgroundGlow
import id.co.nierstyd.mutugemba.desktop.ui.theme.BackgroundGrid
import id.co.nierstyd.mutugemba.desktop.ui.theme.BackgroundTop
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.Sizing
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun AppLayout(
    routes: List<AppRoute>,
    currentRoute: AppRoute,
    onRouteSelected: (AppRoute) -> Unit,
    headerContent: @Composable () -> Unit,
    footerContent: @Composable () -> Unit,
    scrollableContent: Boolean = true,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        SidebarMenu(
            routes = routes,
            currentRoute = currentRoute,
            onRouteSelected = onRouteSelected,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(NeutralBorder),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .weight(1f),
        ) {
            ManufacturingBackground(modifier = Modifier.fillMaxSize())
            Column(modifier = Modifier.fillMaxSize()) {
                headerContent()
                val contentModifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(Spacing.md)
                val scrollModifier =
                    if (scrollableContent) {
                        contentModifier.verticalScroll(rememberScrollState())
                    } else {
                        contentModifier
                    }
                Box(
                    modifier = scrollModifier,
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .widthIn(max = Sizing.contentMaxWidth),
                        shape = MaterialTheme.shapes.large,
                        color = NeutralLight.copy(alpha = 0.38f),
                        border = BorderStroke(1.dp, NeutralBorder.copy(alpha = 0.95f)),
                        elevation = 0.dp,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(Spacing.sm),
                        ) {
                            content()
                        }
                    }
                }
                footerContent()
            }
        }
    }
}

@Composable
private fun ManufacturingBackground(modifier: Modifier = Modifier) {
    val gradient = Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom))
    Box(modifier = modifier.background(gradient)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val grid = 72.dp.toPx()
            val stroke = 1.dp.toPx()
            val gridColor = BackgroundGrid.copy(alpha = 0.6f)
            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                x += grid
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
                y += grid
            }
            drawCircle(
                color = BackgroundGlow.copy(alpha = 0.35f),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width * 0.85f, size.height * 0.15f),
            )
            drawCircle(
                color = NeutralLight.copy(alpha = 0.5f),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.15f, size.height * 0.9f),
            )
        }
    }
}
