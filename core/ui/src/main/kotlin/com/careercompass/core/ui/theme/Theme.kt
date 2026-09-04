package com.careercompass.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Entry point for reading CareerCompass design-system values. */
public object CareerCompassTheme {
    public val colors: CareerCompassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCareerCompassColors.current

    public val typography: CareerCompassTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCareerCompassTypography.current

    public val shapes: CareerCompassShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalCareerCompassShapes.current

    public val spacing: CareerCompassSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalCareerCompassSpacing.current
}

/**
 * Applies CareerCompass tokens and keeps Material 3 components on the same theme.
 *
 * [darkTheme] 이 true 면 [darkCareerCompassColors] 와 `darkColorScheme` 을, 아니면 라이트 쌍을 제공한다.
 * 기본값은 시스템 설정을 따르므로 기존 `CareerCompassTheme { ... }` 호출은 그대로 동작한다.
 */
@Composable
public fun CareerCompassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkCareerCompassColors else lightCareerCompassColors
    val typography = defaultCareerCompassTypography
    val shapes = defaultCareerCompassShapes
    val spacing = defaultCareerCompassSpacing

    CompositionLocalProvider(
        LocalCareerCompassColors provides colors,
        LocalCareerCompassTypography provides typography,
        LocalCareerCompassShapes provides shapes,
        LocalCareerCompassSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme(colors, darkTheme),
            typography =
                Typography(
                    displayLarge = typography.displayLarge,
                    displayMedium = typography.headline1,
                    displaySmall = typography.headline2,
                    headlineLarge = typography.headline1,
                    headlineMedium = typography.headline2,
                    headlineSmall = typography.headline2,
                    titleLarge = typography.headline2,
                    titleMedium = typography.headline4,
                    titleSmall = typography.labelMedium,
                    bodyLarge = typography.bodyLarge,
                    bodyMedium = typography.bodyMedium,
                    bodySmall = typography.caption,
                    labelLarge = typography.labelMedium,
                    labelMedium = typography.labelMedium,
                    labelSmall = typography.caption,
                ),
            shapes =
                Shapes(
                    extraSmall = shapes.control,
                    small = shapes.control,
                    medium = shapes.control,
                    large = shapes.control,
                    extraLarge = shapes.control,
                ),
            content = content,
        )
    }
}

/**
 * 역할 토큰을 Material 3 슬롯에 같은 규칙으로 매핑한다. 매핑하지 않는 슬롯(tertiary·surfaceContainer 계열 등)은
 * 라이트/다크 기본 스킴의 값을 그대로 두어 테마 밝기와 어긋나지 않게 한다.
 */
private fun materialColorScheme(
    colors: CareerCompassColors,
    darkTheme: Boolean,
): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.actionPrimary,
        onPrimary = colors.onAction,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.primary,
        onSecondary = colors.onPrimary,
        secondaryContainer = colors.primaryContainer,
        onSecondaryContainer = colors.onPrimaryContainer,
        background = colors.surface,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onSurfaceVariant,
        inverseSurface = colors.inverseSurface,
        inverseOnSurface = colors.inverseOnSurface,
        outline = colors.outline,
        outlineVariant = colors.outlineStrong,
        error = colors.actionDanger,
        onError = colors.onAction,
        errorContainer = colors.errorContainer,
        onErrorContainer = colors.onErrorContainer,
    )
}
