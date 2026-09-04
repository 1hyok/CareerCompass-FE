package com.cambridge.feature.onboarding.presentation.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.cambridge.feature.onboarding.presentation.R
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * Circular "CC" brand mark shared by the login and biometric login screens.
 *
 * [contentDescription] names the mark for assistive technology. Pass null when the app name is
 * already rendered as adjacent text so the glyph stays decorative instead of being read twice.
 * The glyph keeps its visual size under large font scales because the circle is fixed.
 */
@Composable
internal fun OnboardingBrandMark(
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    require(contentDescription == null || contentDescription.isNotBlank()) {
        "contentDescription must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors
    val fontScale = LocalDensity.current.fontScale
    val glyphSize = size.value * BRAND_GLYPH_RATIO / fontScale
    val semanticsModifier =
        if (contentDescription == null) {
            Modifier.clearAndSetSemantics {}
        } else {
            Modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
        }

    Box(
        modifier =
            modifier
                .size(size)
                .background(color = colors.primary, shape = CircleShape)
                .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_brand_mark),
            modifier = Modifier.clearAndSetSemantics {},
            color = colors.onAction,
            fontSize = glyphSize.sp,
            lineHeight = (glyphSize * BRAND_GLYPH_LINE_HEIGHT_RATIO).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val BRAND_GLYPH_RATIO: Float = 0.36f

private const val BRAND_GLYPH_LINE_HEIGHT_RATIO: Float = 1.25f
