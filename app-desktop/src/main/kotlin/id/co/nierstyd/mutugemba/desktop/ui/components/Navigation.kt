package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.resources.classpathPainterResource
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
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Image(
                    painter = classpathPainterResource("branding/pt_prima_mark.png"),
                    contentDescription = "Logo PT Primaraya",
                    modifier = Modifier.size(26.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = AppStrings.App.Name,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = AppStrings.App.CompanyName,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NeutralBorder)
                        .height(1.dp),
            ) {}
            SidebarSectionLabel("Operasional")
            routes
                .filter { route ->
                    route == AppRoute.Home ||
                        route == AppRoute.PartMapping ||
                        route == AppRoute.Inspection ||
                        route == AppRoute.Abnormal
                }.forEach { route ->
                    SidebarItem(
                        label = route.label,
                        icon =
                            when (route) {
                                AppRoute.Home -> AppIcons.Home
                                AppRoute.PartMapping -> AppIcons.Inventory
                                AppRoute.Inspection -> AppIcons.Inspection
                                AppRoute.Abnormal -> AppIcons.Abnormal
                                else -> AppIcons.Reports
                            },
                        selected = route == currentRoute,
                        onClick = { onRouteSelected(route) },
                    )
                }

            SidebarSectionLabel("Laporan")
            val reportSelected = currentRoute == AppRoute.Reports || currentRoute == AppRoute.ReportsMonthly
            SidebarSectionHeader(
                label = AppStrings.Navigation.Reports,
                icon = AppIcons.Reports,
                selected = reportSelected,
                expanded = reportsExpanded,
                onClick = { reportsExpanded = !reportsExpanded },
            )
            if (reportsExpanded) {
                SidebarSubItem(
                    label = AppStrings.Navigation.ReportsDaily,
                    icon = AppIcons.ReportsDaily,
                    selected = currentRoute == AppRoute.Reports,
                    onClick = { onRouteSelected(AppRoute.Reports) },
                )
                SidebarSubItem(
                    label = AppStrings.Navigation.ReportsMonthly,
                    icon = AppIcons.ReportsMonthly,
                    selected = currentRoute == AppRoute.ReportsMonthly,
                    onClick = { onRouteSelected(AppRoute.ReportsMonthly) },
                )
            }

            SidebarSectionLabel("Sistem")
            SidebarItem(
                label = AppRoute.Settings.label,
                icon = AppIcons.Settings,
                selected = currentRoute == AppRoute.Settings,
                onClick = { onRouteSelected(AppRoute.Settings) },
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NeutralBorder)
                        .height(1.dp),
            ) {}
            Text(
                text = AppStrings.App.Offline,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Text(
                text = currentRoute.navHint(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.primary.copy(alpha = 0.75f),
    )
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
                    .width(3.dp)
                    .height(18.dp)
                    .background(
                        if (selected) MaterialTheme.colors.primary else NeutralSurface,
                        MaterialTheme.shapes.small,
                    ),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
                        .width(3.dp)
                        .height(18.dp)
                        .background(
                            if (selected) MaterialTheme.colors.primary else NeutralSurface,
                            MaterialTheme.shapes.small,
                        ),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
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
        Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun AppRoute.navHint(): String =
    when (this) {
        AppRoute.Home -> "Ringkasan KPI harian, pareto, dan trend QC."
        AppRoute.PartMapping -> "Cari part aktif, cek gambar, dan telusuri kebutuhan model."
        AppRoute.Inspection -> "Input checksheet harian untuk line produksi."
        AppRoute.Abnormal -> "Tindak lanjut tiket abnormal: detect, contain, close."
        AppRoute.Reports -> "Review dokumen checksheet harian dan status tanggal."
        AppRoute.ReportsMonthly -> "Ringkasan bulanan dan cetak PDF laporan."
        AppRoute.Settings -> "Atur aturan input, backup/restore, dan maintenance."
    }
