package com.cambridge.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 라이트·다크 팔레트의 역할 대비를 검사한다. 대비는 WCAG 2.x 상대 휘도 공식으로 직접 계산한다 —
 * 본문·컨테이너 쌍은 4.5:1, 라벨·강조·테두리처럼 큰 글자나 비텍스트 요소는 3:1 이 기준이다.
 */
public class CareerCompassColorsTest {
    @Test
    public fun darkPalette_flipsSurfaceRolesAgainstLight() {
        val light = lightCareerCompassColors.roles()
        val dark = darkCareerCompassColors.roles()

        listOf("surface", "onSurface", "subtleSurface", "inverseSurface").forEach { role ->
            assertNotEquals("$role 은 라이트와 다크에서 달라야 한다", light.getValue(role), dark.getValue(role))
        }
        assertTrue(
            "다크의 inverseSurface 는 surface 보다 밝아야 한다",
            darkCareerCompassColors.inverseSurface.relativeLuminance() >
                darkCareerCompassColors.surface.relativeLuminance(),
        )
        assertTrue(
            "라이트의 inverseSurface 는 surface 보다 어두워야 한다",
            lightCareerCompassColors.inverseSurface.relativeLuminance() <
                lightCareerCompassColors.surface.relativeLuminance(),
        )
    }

    @Test
    public fun everyRole_isOpaqueInBothPalettes() {
        listOf("라이트" to lightCareerCompassColors, "다크" to darkCareerCompassColors).forEach { (name, palette) ->
            palette.roles().forEach { (role, color) ->
                assertEquals("$name $role 은 불투명해야 한다", 1f, color.alpha)
            }
        }
    }

    @Test
    public fun lightPalette_keepsContrastOnEveryPair() {
        assertContrast("라이트", lightCareerCompassColors)
    }

    @Test
    public fun darkPalette_keepsContrastOnEveryPair() {
        assertContrast("다크", darkCareerCompassColors)
    }

    @Test
    public fun darkPalette_keepsDisabledPairReadable() {
        // 라이트의 비활성 쌍(2.3:1)은 시안 값을 그대로 둔다. 다크는 바탕이 어두워 비활성 글자가 묻히기 쉬우므로
        // 비활성 컨테이너 위에서도 3:1 을 지키는지 따로 본다.
        val ratio = contrastRatio(darkCareerCompassColors.disabledContainer, darkCareerCompassColors.disabledContent)
        assertTrue("다크 disabledContent on disabledContainer = ${"%.2f".format(ratio)}:1", ratio >= LABEL_MINIMUM_RATIO)
    }

    private fun assertContrast(
        name: String,
        palette: CareerCompassColors,
    ) {
        val failures =
            CONTRAST_PAIRS.mapNotNull { pair ->
                val (background, foreground) = pair.select(palette)
                val ratio = contrastRatio(background, foreground)
                if (ratio < pair.minimumRatio) {
                    "${pair.label}: ${"%.2f".format(ratio)}:1 < ${pair.minimumRatio}:1"
                } else {
                    null
                }
            }
        assertTrue("$name 팔레트 대비 미달\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    private class ContrastPair(
        val label: String,
        val minimumRatio: Double,
        val select: (CareerCompassColors) -> Pair<Color, Color>,
    )

    private companion object {
        const val TEXT_MINIMUM_RATIO = 4.5
        const val LABEL_MINIMUM_RATIO = 3.0

        val CONTRAST_PAIRS =
            listOf(
                ContrastPair("onSurface on surface", TEXT_MINIMUM_RATIO) { it.surface to it.onSurface },
                ContrastPair("onSurface on subtleSurface", TEXT_MINIMUM_RATIO) { it.subtleSurface to it.onSurface },
                ContrastPair("onSurfaceVariant on surface", TEXT_MINIMUM_RATIO) { it.surface to it.onSurfaceVariant },
                ContrastPair("onSurfaceVariant on surfaceVariant", TEXT_MINIMUM_RATIO) {
                    it.surfaceVariant to it.onSurfaceVariant
                },
                ContrastPair("onPrimary on primary", TEXT_MINIMUM_RATIO) { it.primary to it.onPrimary },
                ContrastPair("onPrimaryContainer on primaryContainer", TEXT_MINIMUM_RATIO) {
                    it.primaryContainer to it.onPrimaryContainer
                },
                ContrastPair("onAction on actionPrimary", TEXT_MINIMUM_RATIO) { it.actionPrimary to it.onAction },
                ContrastPair("onAction on actionDanger", TEXT_MINIMUM_RATIO) { it.actionDanger to it.onAction },
                ContrastPair("inverseOnSurface on inverseSurface", TEXT_MINIMUM_RATIO) {
                    it.inverseSurface to it.inverseOnSurface
                },
                ContrastPair("onSuccess on success", TEXT_MINIMUM_RATIO) { it.success to it.onSuccess },
                ContrastPair("onSuccessContainer on successContainer", TEXT_MINIMUM_RATIO) {
                    it.successContainer to it.onSuccessContainer
                },
                ContrastPair("onWarning on warning", TEXT_MINIMUM_RATIO) { it.warning to it.onWarning },
                ContrastPair("onWarningContainer on warningContainer", TEXT_MINIMUM_RATIO) {
                    it.warningContainer to it.onWarningContainer
                },
                ContrastPair("onError on error", TEXT_MINIMUM_RATIO) { it.error to it.onError },
                ContrastPair("onErrorContainer on errorContainer", TEXT_MINIMUM_RATIO) {
                    it.errorContainer to it.onErrorContainer
                },
                ContrastPair("onInfo on info", TEXT_MINIMUM_RATIO) { it.info to it.onInfo },
                ContrastPair("onInfoContainer on infoContainer", TEXT_MINIMUM_RATIO) {
                    it.infoContainer to it.onInfoContainer
                },
                // 라벨·강조·테두리 — 큰 글자 또는 비텍스트 기준 3:1.
                ContrastPair("mutedContent on surface", LABEL_MINIMUM_RATIO) { it.surface to it.mutedContent },
                ContrastPair("mutedContent on subtleSurface", LABEL_MINIMUM_RATIO) { it.subtleSurface to it.mutedContent },
                ContrastPair("primaryEmphasis on surface", LABEL_MINIMUM_RATIO) { it.surface to it.primaryEmphasis },
                ContrastPair("actionDanger on surface", LABEL_MINIMUM_RATIO) { it.surface to it.actionDanger },
                ContrastPair("interactiveOutline on surface", LABEL_MINIMUM_RATIO) { it.surface to it.interactiveOutline },
                ContrastPair("outline on surface", LABEL_MINIMUM_RATIO) { it.surface to it.outline },
            )

        fun contrastRatio(
            first: Color,
            second: Color,
        ): Double {
            val lighter = max(first.relativeLuminance(), second.relativeLuminance())
            val darker = min(first.relativeLuminance(), second.relativeLuminance())
            return (lighter + 0.05) / (darker + 0.05)
        }

        /** WCAG 2.x relative luminance — sRGB 채널을 선형화한 뒤 가중 합산한다. */
        fun Color.relativeLuminance(): Double {
            fun linear(channel: Float): Double {
                val value = channel.toDouble()
                return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
        }

        /** 38개 역할 전부를 이름과 함께 늘어놓는다 — 새 역할이 생기면 여기에도 추가한다. */
        fun CareerCompassColors.roles(): Map<String, Color> =
            mapOf(
                "primary" to primary,
                "onPrimary" to onPrimary,
                "primaryContainer" to primaryContainer,
                "onPrimaryContainer" to onPrimaryContainer,
                "primaryEmphasis" to primaryEmphasis,
                "actionPrimary" to actionPrimary,
                "actionDanger" to actionDanger,
                "onAction" to onAction,
                "surface" to surface,
                "onSurface" to onSurface,
                "subtleSurface" to subtleSurface,
                "surfaceVariant" to surfaceVariant,
                "onSurfaceVariant" to onSurfaceVariant,
                "mutedContent" to mutedContent,
                "inverseSurface" to inverseSurface,
                "inverseOnSurface" to inverseOnSurface,
                "outline" to outline,
                "outlineStrong" to outlineStrong,
                "subtleOutline" to subtleOutline,
                "interactiveOutline" to interactiveOutline,
                "disabledContainer" to disabledContainer,
                "disabledContent" to disabledContent,
                "success" to success,
                "onSuccess" to onSuccess,
                "successContainer" to successContainer,
                "onSuccessContainer" to onSuccessContainer,
                "warning" to warning,
                "onWarning" to onWarning,
                "warningContainer" to warningContainer,
                "onWarningContainer" to onWarningContainer,
                "error" to error,
                "onError" to onError,
                "errorContainer" to errorContainer,
                "onErrorContainer" to onErrorContainer,
                "info" to info,
                "onInfo" to onInfo,
                "infoContainer" to infoContainer,
                "onInfoContainer" to onInfoContainer,
            )
    }
}
