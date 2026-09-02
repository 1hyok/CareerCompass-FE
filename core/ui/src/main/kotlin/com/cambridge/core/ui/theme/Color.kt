package com.cambridge.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Brand50 = Color(0xFFECFDF5)
private val Brand100 = Color(0xFFD1FAE5)
private val Brand300 = Color(0xFF6EE7B7)
private val Brand400 = Color(0xFF34D399)
private val Brand500 = Color(0xFF10B981)
private val Brand600 = Color(0xFF059669)
private val Brand700 = Color(0xFF047857)
private val Brand900 = Color(0xFF064E3B)

private val Neutral0 = Color(0xFFFFFFFF)
private val Neutral50 = Color(0xFFFAFAFA)
private val Neutral100 = Color(0xFFF5F5F5)
private val Neutral200 = Color(0xFFE5E5E5)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral500 = Color(0xFF737373)
private val Neutral600 = Color(0xFF525252)
private val Neutral700 = Color(0xFF404040)
private val Neutral800 = Color(0xFF262626)
private val Neutral900 = Color(0xFF171717)
private val Neutral950 = Color(0xFF0A0A0A)

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

// 다크 시맨틱 컨테이너 — 라이트의 100/700 쌍을 900/300 쌍으로 뒤집는다(Figma 08 Redline · Color Tokens).
private val DarkWarningContainer = Color(0xFF78350F)
private val DarkOnWarningContainer = Color(0xFFFCD34D)
private val DarkErrorContainer = Color(0xFF7F1D1D)
private val DarkOnErrorContainer = Color(0xFFFCA5A5)
private val DarkInfoContainer = Color(0xFF1E3A8A)
private val DarkOnInfoContainer = Color(0xFF93C5FD)

/**
 * Semantic color roles for CareerCompass UI.
 *
 * Components should consume these roles instead of depending on palette values directly.
 * 라이트·다크 팔레트는 역할의 의미를 공유한다 — `surface` 는 카드 배경, `subtleSurface` 는 화면 바탕,
 * `inverseSurface` 는 현재 테마의 반대 톤(라이트에서 검정, 다크에서 밝은 회색)이다.
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
        subtleSurface = Neutral50,
        surfaceVariant = Neutral100,
        onSurfaceVariant = Neutral600,
        mutedContent = Neutral500,
        inverseSurface = Neutral950,
        inverseOnSurface = Neutral0,
        outline = Neutral500,
        outlineStrong = Neutral600,
        subtleOutline = Neutral200,
        interactiveOutline = Neutral500,
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

/**
 * Figma `07 · Dark Mode` 팔레트. 화면 바탕은 neutral950, 카드는 neutral900, 액센트는 brand500 을 그대로 쓰고
 * 버튼은 brand500 배경에 검정 글자다. 보조 글자는 neutral400, 비활성 글자는 neutral500 으로 두어
 * 비활성 컨테이너(neutral800) 위에서도 3:1 이상 읽힌다.
 */
internal val darkCareerCompassColors =
    CareerCompassColors(
        primary = Brand500,
        onPrimary = Neutral950,
        primaryContainer = Brand900,
        onPrimaryContainer = Brand300,
        primaryEmphasis = Brand400,
        actionPrimary = Brand500,
        actionDanger = SemanticError,
        onAction = Neutral950,
        surface = Neutral900,
        onSurface = Neutral100,
        subtleSurface = Neutral950,
        surfaceVariant = Neutral800,
        onSurfaceVariant = Neutral300,
        mutedContent = Neutral400,
        inverseSurface = Neutral100,
        inverseOnSurface = Neutral950,
        outline = Neutral500,
        outlineStrong = Neutral400,
        subtleOutline = Neutral700,
        interactiveOutline = Neutral500,
        disabledContainer = Neutral800,
        disabledContent = Neutral500,
        success = SemanticSuccess,
        onSuccess = Neutral950,
        successContainer = Brand900,
        onSuccessContainer = Brand300,
        warning = SemanticWarning,
        onWarning = Neutral950,
        warningContainer = DarkWarningContainer,
        onWarningContainer = DarkOnWarningContainer,
        error = SemanticError,
        onError = Neutral950,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        info = SemanticInfo,
        onInfo = Neutral950,
        infoContainer = DarkInfoContainer,
        onInfoContainer = DarkOnInfoContainer,
    )

internal val LocalCareerCompassColors = staticCompositionLocalOf { lightCareerCompassColors }
