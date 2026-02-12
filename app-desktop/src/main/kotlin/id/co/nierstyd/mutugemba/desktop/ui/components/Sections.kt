package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Sizing
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning

@Composable
fun HeaderBar(
    title: String,
    subtitle: String,
    demoMode: Boolean = false,
    dummyData: Boolean = false,
) {
    val badges =
        buildList {
            add(
                HeaderBadgeSpec(
                    text = AppStrings.App.HeaderBadge,
                    backgroundColor = StatusInfo,
                    contentColor = NeutralSurface,
                ),
            )
            if (demoMode) {
                add(
                    HeaderBadgeSpec(
                        text = AppStrings.App.DemoMode,
                        backgroundColor = StatusWarning,
                        contentColor = NeutralSurface,
                    ),
                )
            }
            if (dummyData) {
                add(
                    HeaderBadgeSpec(
                        text = AppStrings.App.DummyData,
                        backgroundColor = NeutralBorder,
                        contentColor = NeutralTextMuted,
                    ),
                )
            } else {
                add(
                    HeaderBadgeSpec(
                        text = AppStrings.App.Offline,
                        backgroundColor = NeutralBorder,
                        contentColor = NeutralTextMuted,
                    ),
                )
            }
            add(
                HeaderBadgeSpec(
                    text = AppStrings.App.BuildVersion,
                    backgroundColor = NeutralLight,
                    contentColor = NeutralTextMuted,
                ),
            )
        }
    Surface(
        color = NeutralSurface,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Sizing.headerHeight),
        elevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Image(
                        painter = painterResource("branding/pt_prima_mark.png"),
                        contentDescription = "Logo PT Primaraya",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h5,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.body2,
                            color = NeutralTextMuted,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppStrings.App.CompanyMotto,
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                    badges.forEach { badge ->
                        AppBadge(
                            text = badge.text,
                            backgroundColor = badge.backgroundColor,
                            contentColor = badge.contentColor,
                        )
                    }
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NeutralBorder),
            ) {}
        }
    }
}

@Composable
fun FooterBar(
    statusText: String,
    hintText: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Sizing.footerHeight)
                .background(NeutralSurface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NeutralBorder),
        ) {}
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Text(
                text = hintText,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md),
    ) {
        Text(text = title, style = MaterialTheme.typography.h4)
        Text(text = subtitle, style = MaterialTheme.typography.body1)
    }
}

private data class HeaderBadgeSpec(
    val text: String,
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)

@Composable
fun WizardStepIndicator(
    currentStep: Int,
    labels: List<String>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        labels.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isActive = stepNumber == currentStep
            WizardStepChip(
                number = stepNumber,
                label = label,
                active = isActive,
            )
        }
    }
}

@Composable
private fun WizardStepChip(
    number: Int,
    label: String,
    active: Boolean,
) {
    val background = if (active) MaterialTheme.colors.primary else NeutralSurface
    val contentColor = if (active) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface

    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (active) MaterialTheme.colors.onPrimary else NeutralBorder,
            elevation = 0.dp,
        ) {
            Text(
                text = number.toString(),
                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                style =
                    MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                    ),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.body2.copy(color = contentColor),
        )
    }
}
