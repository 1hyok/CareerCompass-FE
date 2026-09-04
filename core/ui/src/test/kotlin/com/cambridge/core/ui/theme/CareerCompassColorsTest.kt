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
 * 라이트·다크 팔레트의 역할 대비를 검사한다. 대비는 WCAG 2.1 상대 휘도 공식으로 직접 계산한다 —
 * 본문·컨테이너 쌍은 4.5:1(1.4.3), 큰 글자·아이콘·경계·그래픽은 3:1(1.4.3·1.4.11)이 기준이다.
 *
 * 표는 [CONTRAST_PAIRS] 한 곳이고 라이트·다크가 같은 표를 돈다. 각 행의 `usage` 는 **실제로 그 조합이
 * 그려지는 자리**다 — 쓰이지 않는 조합을 채워 통과율을 만들지 않는다. 표에 없는 새 역할이 생기면
 * [everyForegroundRole_appearsInTheContrastTable] 가 먼저 깨진다.
 *
 * 측정값과 면제 근거는 `docs/convention/color-contrast.md` 가 정본이다.
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
        PALETTES.forEach { (name, palette) ->
            palette.roles().forEach { (role, color) ->
                assertEquals("$name $role 은 불투명해야 한다", 1f, color.alpha)
            }
        }
    }

    @Test
    public fun lightPalette_keepsContrastOnEveryUsedPair() {
        assertContrast("라이트", lightCareerCompassColors, Vision.Normal)
    }

    @Test
    public fun darkPalette_keepsContrastOnEveryUsedPair() {
        assertContrast("다크", darkCareerCompassColors, Vision.Normal)
    }

    /**
     * 전색맹(achromatopsia)은 색을 휘도로만 본다. WCAG 대비도 휘도만 쓰므로 통과해야 정상이지만,
     * 「색조 차이에 기대어 통과한 조합이 없다」는 것을 표 전체에 대해 못 박아 둔다.
     */
    @Test
    public fun contrastSurvivesTotalColorBlindness() {
        PALETTES.forEach { (name, palette) ->
            assertContrast(name, palette, Vision.Achromatopsia)
        }
    }

    /**
     * 적록색맹 변환에서 성공·경고·오류 액센트가 서로 무너지는지 잰다.
     *
     * 이 검사는 팔레트를 고치라는 뜻이 아니다 — 세 액센트는 색조로만 갈리므로 어떤 값을 골라도
     * 적록에서는 붙는다. **그러니 배지·상태는 글자를 함께 내보내야 한다**는 근거를 수치로 고정한다.
     * 이 테스트가 깨졌다면 색으로도 갈리게 됐다는 뜻이니 문서의 판단 근거를 다시 쓴다.
     */
    @Test
    public fun redGreenSimulation_collapsesSemanticAccents() {
        PALETTES.forEach { (name, palette) ->
            val roles = palette.roles()
            val warning = roles.getValue("warning")
            val error = roles.getValue("error")
            val ratio = contrastRatio(Vision.Deuteranopia.simulate(warning), Vision.Deuteranopia.simulate(error))
            assertTrue(
                "$name warning↔error 은 적록색맹에서 ${"%.2f".format(ratio)}:1 로 붙는다 — 배지 문구가 필요하다",
                ratio < INDISTINGUISHABLE_RATIO,
            )
        }
    }

    /**
     * 전경 역할이 표에서 빠지는 것을 막는다. `onX` 는 `x` 컨테이너 위에 놓인다는 이름 규칙을 그대로
     * 검사에 옮기고, 규칙 밖 전경([EXTRA_FOREGROUND_ROLES])은 이름을 적어 둔다. 역할을 새로 더하면
     * 이 테스트가 먼저 깨져서 표를 채우게 된다.
     */
    @Test
    public fun everyForegroundRole_appearsInTheContrastTable() {
        val roleNames = lightCareerCompassColors.roles().keys
        val foregrounds = roleNames.filter { it.startsWith("on") } + EXTRA_FOREGROUND_ROLES
        val covered = CONTRAST_PAIRS.map { it.foreground }.toSet() + EXEMPT_PAIRS.map { it.foreground }.toSet()

        val missing = foregrounds.filterNot { it in covered }
        assertTrue("대비 표에 없는 전경 역할: $missing", missing.isEmpty())

        val unknown =
            CONTRAST_PAIRS
                .flatMap { listOf(it.foreground, it.background) }
                .filterNot { it in roleNames }
        assertTrue("표가 없는 역할을 가리킨다: ${unknown.distinct()}", unknown.isEmpty())
    }

    /**
     * `onX` → `x` 이름 규칙으로 짝을 끌어낼 수 있는 역할은 전부 표에 그 짝 그대로 들어 있어야 한다.
     * 표를 손으로 적다가 `onWarningContainer` 를 `warning` 위에 얹는 식의 어긋남을 막는다.
     */
    @Test
    public fun namedOnRoles_pairWithTheirOwnContainer() {
        val roleNames = lightCareerCompassColors.roles().keys
        val naturalPairs =
            roleNames
                .filter { it.startsWith("on") }
                .mapNotNull { onRole ->
                    val container = onRole.removePrefix("on").replaceFirstChar(Char::lowercaseChar)
                    if (container in roleNames) onRole to container else null
                }
        val table = CONTRAST_PAIRS.map { it.foreground to it.background }.toSet()

        val missing = naturalPairs.filterNot { it in table }
        assertTrue("이름 규칙으로 나오는 짝이 표에 없다: $missing", missing.isEmpty())
    }

    @Test
    public fun darkPalette_keepsDisabledPairReadable() {
        // 비활성 컴포넌트는 WCAG 1.4.3·1.4.11 의 예외라 표에서 면제다(라이트 2.31:1). 다만 다크는 바탕이
        // 어두워 비활성 글자가 아예 묻히기 쉬우므로 3:1 은 지키는지 따로 본다.
        val ratio = contrastRatio(darkCareerCompassColors.disabledContainer, darkCareerCompassColors.disabledContent)
        assertTrue("다크 disabledContent on disabledContainer = ${"%.2f".format(ratio)}:1", ratio >= NON_TEXT_MINIMUM_RATIO)
    }

    private fun assertContrast(
        name: String,
        palette: CareerCompassColors,
        vision: Vision,
    ) {
        val roles = palette.roles()
        val failures =
            CONTRAST_PAIRS.mapNotNull { pair ->
                val foreground = vision.simulate(roles.getValue(pair.foreground))
                val background = vision.simulate(roles.getValue(pair.background))
                val ratio = contrastRatio(foreground, background)
                if (ratio < pair.minimumRatio) {
                    "${pair.foreground} on ${pair.background}: ${"%.2f".format(ratio)}:1 " +
                        "< ${pair.minimumRatio}:1 (${pair.usage})"
                } else {
                    null
                }
            }
        assertTrue("$name 팔레트(${vision.label}) 대비 미달\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    /**
     * 표의 한 행. [foreground]·[background] 는 [roles] 의 키이고, [usage] 는 그 조합이 실제로 그려지는 자리다.
     */
    private class ContrastPair(
        val foreground: String,
        val background: String,
        val minimumRatio: Double,
        val usage: String,
    )

    /** 색각 이상 시뮬레이션. 선형 sRGB 에서 Viénot–Brettel–Mollon(1999) 행렬을 적용한다. */
    private enum class Vision(
        val label: String,
    ) {
        Normal("정상"),
        Deuteranopia("적록(녹색맹)"),
        Achromatopsia("전색맹"),
        ;

        fun simulate(color: Color): Color {
            val r = linear(color.red)
            val g = linear(color.green)
            val b = linear(color.blue)
            return when (this) {
                Normal -> {
                    color
                }

                Deuteranopia -> {
                    fromLinear(
                        r,
                        DEUTAN_G_FROM_R * r + DEUTAN_G_FROM_B * b,
                        b,
                    )
                }

                Achromatopsia -> {
                    val y = RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b
                    fromLinear(y, y, y)
                }
            }
        }
    }

    private companion object {
        const val TEXT_MINIMUM_RATIO = 4.5
        const val NON_TEXT_MINIMUM_RATIO = 3.0

        /** 이 아래로 붙으면 「색만으로는 갈리지 않는다」고 본다. */
        const val INDISTINGUISHABLE_RATIO = 1.5

        const val RED_WEIGHT = 0.2126
        const val GREEN_WEIGHT = 0.7152
        const val BLUE_WEIGHT = 0.0722
        const val DEUTAN_G_FROM_R = 0.9513092
        const val DEUTAN_G_FROM_B = 0.04866992

        val PALETTES =
            listOf(
                "라이트" to lightCareerCompassColors,
                "다크" to darkCareerCompassColors,
            )

        /** `onX` 규칙을 따르지 않는 전경 역할. 표에 있어야 하는 것은 같다. */
        val EXTRA_FOREGROUND_ROLES =
            listOf(
                "inverseOnSurface",
                "disabledContent",
                "mutedContent",
                "primaryEmphasis",
                "primary",
                "actionPrimary",
                "actionDanger",
                "error",
                "success",
                "outline",
                "outlineStrong",
                "subtleOutline",
                "interactiveOutline",
                "inverseSurface",
            )

        /**
         * 실제로 그려지는 전경/배경 조합. 본문 4.5:1, 큰 글자(≥18.66sp Bold, ≥24sp)·아이콘·경계·그래픽 3:1.
         *
         * `usage` 의 파일·줄은 조합을 찾은 자리다 — 줄 번호는 흔들릴 수 있으니 근거는 파일과 문장으로 읽는다.
         */
        val CONTRAST_PAIRS =
            listOf(
                // ---- 이름 규칙 그대로의 on*/* 짝 ----
                ContrastPair("onPrimary", "primary", TEXT_MINIMUM_RATIO, "필터 개수 배지 10sp Bold · FeedScreen"),
                ContrastPair(
                    "onPrimaryContainer",
                    "primaryContainer",
                    TEXT_MINIMUM_RATIO,
                    "프로필 배너 본문 · FeedScreen",
                ),
                ContrastPair("onSurface", "surface", TEXT_MINIMUM_RATIO, "카드·시트 본문"),
                ContrastPair(
                    "onSurfaceVariant",
                    "surfaceVariant",
                    TEXT_MINIMUM_RATIO,
                    "Neutral 배지·읽음 배지 · CareerCompassBadge, FeedReadBadge",
                ),
                ContrastPair("onSuccess", "success", TEXT_MINIMUM_RATIO, "역할 계약 — 현재 채움 사용처 없음"),
                ContrastPair(
                    "onSuccessContainer",
                    "successContainer",
                    TEXT_MINIMUM_RATIO,
                    "Brand 배지 · CareerCompassBadge",
                ),
                ContrastPair("onWarning", "warning", TEXT_MINIMUM_RATIO, "역할 계약 — 현재 채움 사용처 없음"),
                ContrastPair(
                    "onWarningContainer",
                    "warningContainer",
                    TEXT_MINIMUM_RATIO,
                    "오프라인 배너·검토 필요 배지 · FeedScreen, OnboardingStep4Screen",
                ),
                ContrastPair("onError", "error", TEXT_MINIMUM_RATIO, "역할 계약 — 현재 채움 사용처 없음"),
                ContrastPair(
                    "onErrorContainer",
                    "errorContainer",
                    TEXT_MINIMUM_RATIO,
                    "오류 카드 · OnboardingErrorCard, BoardListScreen",
                ),
                ContrastPair("onInfo", "info", TEXT_MINIMUM_RATIO, "역할 계약 — 현재 채움 사용처 없음"),
                ContrastPair("onInfoContainer", "infoContainer", TEXT_MINIMUM_RATIO, "Info 배지 · CareerCompassBadge"),
                // ---- 이름 규칙 밖의 짝 ----
                ContrastPair(
                    "inverseOnSurface",
                    "inverseSurface",
                    TEXT_MINIMUM_RATIO,
                    "선택된 태그·Dark 버튼 · CareerCompassTag, CareerCompassButton",
                ),
                ContrastPair("onAction", "actionPrimary", TEXT_MINIMUM_RATIO, "Primary 버튼 16sp · CareerCompassButton"),
                ContrastPair("onAction", "actionDanger", TEXT_MINIMUM_RATIO, "Danger 버튼 · CareerCompassButton"),
                ContrastPair(
                    "onAction",
                    "primary",
                    TEXT_MINIMUM_RATIO,
                    "브랜드 마크 글리프·완료 체크 · OnboardingBrandMark, OnboardingCompleteScreen",
                ),
                // ---- 화면 바탕·카드 위의 전경 ----
                ContrastPair("onSurface", "subtleSurface", TEXT_MINIMUM_RATIO, "화면 제목·본문 · 모든 화면 루트 · 상태 화면 제목 · CareerCompassStateView"),
                ContrastPair("onSurface", "surfaceVariant", TEXT_MINIMUM_RATIO, "적합도 칩 Mid 점수 · CareerCompassScoreChip"),
                ContrastPair(
                    "onSurface",
                    "primaryContainer",
                    TEXT_MINIMUM_RATIO,
                    "생체인증 원 안 이모지(색 미지정 상속) · BiometricLoginScreen",
                ),
                ContrastPair(
                    "onSurface",
                    "successContainer",
                    TEXT_MINIMUM_RATIO,
                    "강점 코멘트 본문 · PostingDetailScreen",
                ),
                ContrastPair(
                    "onSurface",
                    "warningContainer",
                    TEXT_MINIMUM_RATIO,
                    "약점 코멘트 본문 · PostingDetailScreen",
                ),
                ContrastPair("onSurfaceVariant", "surface", TEXT_MINIMUM_RATIO, "읽은 공고 제목 · FeedScreen"),
                ContrastPair("onSurfaceVariant", "subtleSurface", TEXT_MINIMUM_RATIO, "화면 보조 문구 · FeedScreen"),
                ContrastPair(
                    "onSurfaceVariant",
                    "primaryContainer",
                    TEXT_MINIMUM_RATIO,
                    "적합도 칩 High 라벨 · CareerCompassScoreChip",
                ),
                ContrastPair("mutedContent", "surface", TEXT_MINIMUM_RATIO, "카드 메타 12sp SemiBold · FeedScreen"),
                ContrastPair(
                    "mutedContent",
                    "subtleSurface",
                    TEXT_MINIMUM_RATIO,
                    "적합도 자리표시 칩 11sp · FeedSuitabilityChip · 상태 화면 본문(실패 포함) · CareerCompassStateView",
                ),
                ContrastPair(
                    "primaryEmphasis",
                    "surface",
                    TEXT_MINIMUM_RATIO,
                    "정렬 선택 문구·북마크 아이콘 · FeedSortMenuContent, FeedScreen",
                ),
                ContrastPair(
                    "primaryEmphasis",
                    "subtleSurface",
                    TEXT_MINIMUM_RATIO,
                    "더보기 스피너·온보딩 진행 세그먼트 · FeedScreen, OnboardingStepScaffold",
                ),
                ContrastPair(
                    "primaryEmphasis",
                    "primaryContainer",
                    TEXT_MINIMUM_RATIO,
                    "프로필 배너 행동 문구 11sp · FeedScreen",
                ),
                ContrastPair("primary", "surface", TEXT_MINIMUM_RATIO, "분석 축 점수 14sp Bold · SuitabilityBreakdownRow"),
                ContrastPair(
                    "primary",
                    "subtleSurface",
                    NON_TEXT_MINIMUM_RATIO,
                    "브랜드 마크 원 · OnboardingBrandMark",
                ),
                ContrastPair(
                    "actionPrimary",
                    "subtleSurface",
                    TEXT_MINIMUM_RATIO,
                    "스텝 카운터 11sp SemiBold · OnboardingStepScaffold",
                ),
                ContrastPair("actionDanger", "surface", TEXT_MINIMUM_RATIO, "마감 임박 12sp SemiBold · FeedScreen"),
                ContrastPair("error", "surface", TEXT_MINIMUM_RATIO, "상세 마감 12sp SemiBold · PostingDetailScreen"),
                ContrastPair("error", "subtleSurface", TEXT_MINIMUM_RATIO, "기간 입력 오류 문구 · FeedDeadlineRangeEditor"),
                ContrastPair(
                    "onSuccessContainer",
                    "primaryContainer",
                    TEXT_MINIMUM_RATIO,
                    "적합도 칩 High 점수 13sp Bold · CareerCompassScoreChip",
                ),
                ContrastPair("onWarningContainer", "surface", TEXT_MINIMUM_RATIO, "수집 주기 안내 · BoardRegisterScreen"),
                // ---- 비텍스트(경계·그래픽) 3:1 ----
                ContrastPair(
                    "interactiveOutline",
                    "surface",
                    NON_TEXT_MINIMUM_RATIO,
                    "입력 테두리 · CareerCompassTextField, DirectInputSheet",
                ),
                ContrastPair(
                    "interactiveOutline",
                    "subtleSurface",
                    NON_TEXT_MINIMUM_RATIO,
                    "검색·필터 버튼 테두리 · FeedScreen",
                ),
                ContrastPair(
                    "outline",
                    "subtleSurface",
                    NON_TEXT_MINIMUM_RATIO,
                    "온보딩 진행바 미완료 구간 · OnboardingStepScaffold",
                ),
                ContrastPair("outlineStrong", "surface", NON_TEXT_MINIMUM_RATIO, "M3 outlineVariant 매핑 · Theme"),
                ContrastPair(
                    "primary",
                    "surfaceVariant",
                    NON_TEXT_MINIMUM_RATIO,
                    "적합도 게이지 채움 vs 트랙 · SuitabilityGauge",
                ),
                ContrastPair(
                    "primary",
                    "subtleOutline",
                    NON_TEXT_MINIMUM_RATIO,
                    "분석 진행 표시 vs 트랙 · CareerCompassStateView",
                ),
                ContrastPair(
                    "inverseSurface",
                    "surfaceVariant",
                    NON_TEXT_MINIMUM_RATIO,
                    "미충족 막대 vs 트랙 · SuitabilityBreakdownRow",
                ),
                ContrastPair(
                    "inverseSurface",
                    "subtleSurface",
                    NON_TEXT_MINIMUM_RATIO,
                    "선택된 경험 유형 pill · OnboardingStep3Screen",
                ),
            )

        /**
         * 재지 않는 것이 아니라 **기준을 적용하지 않는** 조합. 근거는 `docs/convention/color-contrast.md` 에
         * 측정값과 함께 적혀 있다. 여기 있는 전경 역할은 표 커버리지 검사를 통과한 것으로 본다.
         */
        val EXEMPT_PAIRS =
            listOf(
                ContrastPair("disabledContent", "disabledContainer", 0.0, "비활성 — WCAG 1.4.3·1.4.11 예외"),
                ContrastPair("disabledContent", "surface", 0.0, "비활성 — 카드는 surface 인 채 내용만 흐려진다"),
                ContrastPair("subtleOutline", "surface", 0.0, "장식 구분선·카드 테두리"),
                ContrastPair("subtleOutline", "subtleSurface", 0.0, "장식 구분선"),
                ContrastPair("success", "surface", 0.0, "「오늘 수집」 6dp 장식 점 — 같은 뜻을 문구가 진다"),
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
        fun Color.relativeLuminance(): Double = RED_WEIGHT * linear(red) + GREEN_WEIGHT * linear(green) + BLUE_WEIGHT * linear(blue)

        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        fun fromLinear(
            red: Double,
            green: Double,
            blue: Double,
        ): Color =
            Color(
                red = encode(red),
                green = encode(green),
                blue = encode(blue),
            )

        private fun encode(value: Double): Float {
            val clamped = value.coerceIn(0.0, 1.0)
            val encoded = if (clamped <= 0.0031308) clamped * 12.92 else 1.055 * clamped.pow(1 / 2.4) - 0.055
            return encoded.coerceIn(0.0, 1.0).toFloat()
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
