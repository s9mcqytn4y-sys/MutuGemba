package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.Sizing
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun SidebarMenu(
    routes: List<AppRoute>,
    currentRoute: AppRoute,
    onRouteSelected: (AppRoute) -> Unit,
) {
    Surface(
        color = NeutralSurface,
        modifier =
            Modifier
                .width(Sizing.sidebarWidth)
                .fillMaxHeight(),
        elevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "MutuGemba",
                    style = MaterialTheme.typography.h6,
                )
                Text(
                    text = "QC TPS Harian",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary,
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NeutralBorder)
                        .height(1.dp),
            ) {}
            routes.forEach { route ->
                SidebarItem(
                    label = route.label,
                    selected = route == currentRoute,
                    onClick = { onRouteSelected(route) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NeutralBorder)
                        .height(1.dp),
            ) {}
            Text(
                text = "Offline",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else NeutralSurface
    val textColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Sizing.sidebarItemHeight)
                .background(background, MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(
                        if (selected) MaterialTheme.colors.primary else NeutralSurface,
                        MaterialTheme.shapes.small,
                    ),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.subtitle1,
        )
    }
}
