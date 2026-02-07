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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CalendarMonth
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
    var reportsExpanded by remember { mutableStateOf(true) }
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
                if (route == AppRoute.Reports) {
                    val reportSelected = currentRoute == AppRoute.Reports || currentRoute == AppRoute.ReportsMonthly
                    SidebarSectionHeader(
                        label = route.label,
                        icon = Icons.Filled.Report,
                        selected = reportSelected,
                        expanded = reportsExpanded,
                        onClick = { reportsExpanded = !reportsExpanded },
                    )
                    if (reportsExpanded) {
                        SidebarSubItem(
                            label = "Laporan Harian",
                            icon = Icons.AutoMirrored.Filled.FactCheck,
                            selected = currentRoute == AppRoute.Reports,
                            onClick = { onRouteSelected(AppRoute.Reports) },
                        )
                        SidebarSubItem(
                            label = "Laporan Bulanan",
                            icon = Icons.Filled.CalendarMonth,
                            selected = currentRoute == AppRoute.ReportsMonthly,
                            onClick = { onRouteSelected(AppRoute.ReportsMonthly) },
                        )
                    }
                } else if (route != AppRoute.ReportsMonthly) {
                    SidebarItem(
                        label = route.label,
                        icon = when (route) {
                            AppRoute.Home -> Icons.Filled.Home
                            AppRoute.Inspection -> Icons.AutoMirrored.Filled.FactCheck
                            AppRoute.Abnormal -> Icons.Filled.Warning
                            AppRoute.Settings -> Icons.Filled.Settings
                            else -> Icons.Filled.Report
                        },
                        selected = route == currentRoute,
                        onClick = { onRouteSelected(route) },
                    )
                }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Icon(imageVector = icon, contentDescription = null, tint = textColor)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.subtitle1,
        )
    }
}

@Composable
private fun SidebarSectionHeader(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.08f) else NeutralSurface
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Icon(imageVector = icon, contentDescription = null, tint = textColor)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.subtitle1,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = textColor,
        )
    }
}

@Composable
private fun SidebarSubItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Sizing.sidebarItemHeight - 10.dp)
                .background(if (selected) MaterialTheme.colors.primary.copy(alpha = 0.06f) else NeutralSurface)
                .clickable(onClick = onClick)
                .padding(start = Spacing.lg, end = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.body2,
        )
    }
}
