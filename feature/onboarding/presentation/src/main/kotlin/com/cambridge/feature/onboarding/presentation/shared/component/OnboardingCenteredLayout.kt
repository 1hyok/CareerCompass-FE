package com.cambridge.feature.onboarding.presentation.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.isFinite
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * Full-screen layout shared by the auth and completion screens: an optional top section, a
 * vertically centered body and actions pinned to the bottom edge.
 *
 * The body absorbs the leftover height so the actions sit at the bottom on tall screens, while
 * the whole column scrolls when large font scales make the content taller than the viewport.
 * [topContent] null means no top section is reserved.
 */
@Composable
internal fun OnboardingCenteredLayout(
    topContent: (@Composable ColumnScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    centerContent: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val fillViewportModifier =
            if (maxHeight.isFinite) {
                Modifier.heightIn(min = maxHeight)
            } else {
                Modifier
            }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .then(fillViewportModifier)
                    .padding(horizontal = spacing.xxLarge),
        ) {
            if (topContent != null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xxLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = topContent,
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = spacing.xxLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = centerContent,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.xxLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                content = bottomContent,
            )
        }
    }
}
