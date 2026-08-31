package com.cambridge.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.R

private val InterFontFamily =
    FontFamily(
        Font(R.font.inter_variable, weight = FontWeight.Normal),
        Font(R.font.inter_variable, weight = FontWeight.Medium),
        Font(R.font.inter_variable, weight = FontWeight.SemiBold),
        Font(R.font.inter_variable, weight = FontWeight.Bold),
    )

/** Typography roles defined by the CareerCompass design system. */
@Immutable
public class CareerCompassTypography internal constructor(
    public val displayLarge: TextStyle,
    public val headline1: TextStyle,
    public val headline2: TextStyle,
    public val headline4: TextStyle,
    public val bodyLarge: TextStyle,
    public val bodyMedium: TextStyle,
    public val labelMedium: TextStyle,
    public val caption: TextStyle,
)

internal val defaultCareerCompassTypography =
    CareerCompassTypography(
        displayLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            ),
        headline1 =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            ),
        headline2 =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        headline4 =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            ),
        caption =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
    )

internal val LocalCareerCompassTypography =
    staticCompositionLocalOf { defaultCareerCompassTypography }
