package com.cambridge.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale shared by CareerCompass layouts and components. */
@Immutable
public class CareerCompassSpacing internal constructor(
    public val xxSmall: Dp,
    public val xSmall: Dp,
    public val small: Dp,
    public val medium: Dp,
    public val large: Dp,
    public val xLarge: Dp,
    public val xxLarge: Dp,
)

internal val defaultCareerCompassSpacing =
    CareerCompassSpacing(
        xxSmall = 4.dp,
        xSmall = 6.dp,
        small = 8.dp,
        medium = 12.dp,
        large = 16.dp,
        xLarge = 20.dp,
        xxLarge = 24.dp,
    )

internal val LocalCareerCompassSpacing = staticCompositionLocalOf { defaultCareerCompassSpacing }
