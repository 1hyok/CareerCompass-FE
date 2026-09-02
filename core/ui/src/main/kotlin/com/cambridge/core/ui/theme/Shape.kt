package com.cambridge.core.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Shape roles shared by CareerCompass components. */
@Immutable
public class CareerCompassShapes internal constructor(
    public val control: CornerBasedShape,
    public val largeControl: CornerBasedShape,
    public val card: CornerBasedShape,
    public val pill: CornerBasedShape,
)

internal val defaultCareerCompassShapes =
    CareerCompassShapes(
        control = RoundedCornerShape(10.dp),
        largeControl = RoundedCornerShape(12.dp),
        card = RoundedCornerShape(16.dp),
        pill = RoundedCornerShape(percent = 50),
    )

internal val LocalCareerCompassShapes = staticCompositionLocalOf { defaultCareerCompassShapes }
