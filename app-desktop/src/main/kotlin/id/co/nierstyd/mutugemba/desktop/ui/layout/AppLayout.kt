package id.co.nierstyd.mutugemba.desktop.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.components.SidebarMenu
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun AppLayout(
    routes: List<AppRoute>,
    currentRoute: AppRoute,
    onRouteSelected: (AppRoute) -> Unit,
    headerContent: @Composable () -> Unit,
    footerContent: @Composable () -> Unit,
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
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .weight(1f),
        ) {
            headerContent()
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(Spacing.lg)
                        .verticalScroll(rememberScrollState()),
            ) {
                MaterialTheme {
                    content()
                }
            }
            footerContent()
        }
    }
}
