package com.cambridge.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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

/** Applies CareerCompass tokens and keeps Material 3 components on the same theme. */
@Composable
public fun CareerCompassTheme(content: @Composable () -> Unit) {
    val colors = lightCareerCompassColors
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
            colorScheme =
                lightColorScheme(
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
                ),
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
