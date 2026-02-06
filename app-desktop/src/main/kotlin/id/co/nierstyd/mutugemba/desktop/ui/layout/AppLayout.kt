package id.co.nierstyd.mutugemba.desktop.ui.layout

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.components.SidebarMenu
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
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
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .weight(1f),
        ) {
            headerContent()
            val contentModifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(Spacing.lg)
            val scrollModifier =
                if (scrollableContent) {
                    contentModifier.verticalScroll(rememberScrollState())
                } else {
                    contentModifier
                }
            Box(
                modifier =
                scrollModifier,
                contentAlignment = Alignment.TopCenter,
            ) {
                MaterialTheme {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = Sizing.contentMaxWidth)) {
                        content()
                    }
                }
            }
            footerContent()
        }
    }
}
