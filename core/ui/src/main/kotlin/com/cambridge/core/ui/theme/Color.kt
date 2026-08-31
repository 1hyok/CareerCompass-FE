package com.cambridge.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Brand50 = Color(0xFFECFDF5)
private val Brand100 = Color(0xFFD1FAE5)
private val Brand300 = Color(0xFF6EE7B7)
private val Brand500 = Color(0xFF10B981)
private val Brand600 = Color(0xFF059669)
private val Brand700 = Color(0xFF047857)
private val Brand900 = Color(0xFF064E3B)

private val Neutral0 = Color(0xFFFFFFFF)
private val Neutral100 = Color(0xFFF5F5F5)
private val Neutral200 = Color(0xFFE5E5E5)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral600 = Color(0xFF525252)
private val Neutral800 = Color(0xFF262626)
private val Neutral950 = Color(0xFF0A0A0A)
private val SubtleSurface = Color(0xFFFAFAFA)
private val MutedContent = Color(0xFF737373)

private val SemanticSuccess = Color(0xFF10B981)
private val SemanticWarning = Color(0xFFF59E0B)
private val SemanticError = Color(0xFFEF4444)
private val SemanticInfo = Color(0xFF3B82F6)

private val WarningContainer = Color(0xFFFEF3C7)
private val OnWarningContainer = Color(0xFFB45309)
private val ErrorContainer = Color(0xFFFEE2E2)
private val OnErrorContainer = Color(0xFFB91C1C)
private val InfoContainer = Color(0xFFDBEAFE)
private val OnInfoContainer = Color(0xFF1D4ED8)

/**
 * Semantic color roles for CareerCompass UI.
 *
 * Components should consume these roles instead of depending on palette values directly.
 */
@Immutable
public class CareerCompassColors internal constructor(
    public val primary: Color,
    public val onPrimary: Color,
    public val primaryContainer: Color,
    public val onPrimaryContainer: Color,
    public val primaryEmphasis: Color,
    public val actionPrimary: Color,
    public val actionDanger: Color,
    public val onAction: Color,
    public val surface: Color,
    public val onSurface: Color,
    public val subtleSurface: Color,
    public val surfaceVariant: Color,
    public val onSurfaceVariant: Color,
    public val mutedContent: Color,
    public val inverseSurface: Color,
    public val inverseOnSurface: Color,
    public val outline: Color,
    public val outlineStrong: Color,
    public val subtleOutline: Color,
    public val interactiveOutline: Color,
    public val disabledContainer: Color,
    public val disabledContent: Color,
    public val success: Color,
    public val onSuccess: Color,
    public val successContainer: Color,
    public val onSuccessContainer: Color,
    public val warning: Color,
    public val onWarning: Color,
    public val warningContainer: Color,
    public val onWarningContainer: Color,
    public val error: Color,
    public val onError: Color,
    public val errorContainer: Color,
    public val onErrorContainer: Color,
    public val info: Color,
    public val onInfo: Color,
    public val infoContainer: Color,
    public val onInfoContainer: Color,
)

internal val lightCareerCompassColors =
    CareerCompassColors(
        primary = Brand500,
        onPrimary = Neutral950,
        primaryContainer = Brand50,
        onPrimaryContainer = Brand900,
        primaryEmphasis = Brand600,
        actionPrimary = Brand700,
        actionDanger = OnErrorContainer,
        onAction = Neutral0,
        surface = Neutral0,
        onSurface = Neutral950,
        subtleSurface = SubtleSurface,
        surfaceVariant = Neutral100,
        onSurfaceVariant = Neutral600,
        mutedContent = MutedContent,
        inverseSurface = Neutral950,
        inverseOnSurface = Neutral0,
        outline = MutedContent,
        outlineStrong = Neutral600,
        subtleOutline = Neutral200,
        interactiveOutline = MutedContent,
        disabledContainer = Neutral100,
        disabledContent = Neutral400,
        success = SemanticSuccess,
        onSuccess = Neutral950,
        successContainer = Brand50,
        onSuccessContainer = Brand700,
        warning = SemanticWarning,
        onWarning = Neutral950,
        warningContainer = WarningContainer,
        onWarningContainer = OnWarningContainer,
        error = SemanticError,
        onError = Neutral950,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        info = SemanticInfo,
        onInfo = Neutral950,
        infoContainer = InfoContainer,
        onInfoContainer = OnInfoContainer,
    )

internal val LocalCareerCompassColors = staticCompositionLocalOf { lightCareerCompassColors }
