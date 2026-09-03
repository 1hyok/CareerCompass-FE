package com.cambridge.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
public class CareerCompassTopAppBarTest {
    @get:Rule
    public val composeRule = createComposeRule()

    /**
     * 상단바 높이는 56dp 하한을 지킨다.
     *
     * 종전에는 «정확히 56dp» 를 못 박았다. 고정 높이를 걷어내면서(#122 — 배율 2.0 에서 부제가
     * 가로로 반 잘렸다) 계약이 «56dp 이상» 으로 바뀌었고, 기본 배율에서 실제로 56dp 인지는
     * screenshot 골든(`Top app bar - Subtitle`)이 지킨다. 여기서 dp 를 다시 못 박지 않는 이유는
     * 측정 환경이다 — Robolectric 은 실제 폰트 파일 없이 대체 글꼴로 텍스트를 재서 한 줄을 24dp
     * 대신 35dp 로 잡는다. 그 숫자를 기준으로 삼으면 실제 단말과 무관한 값을 지키게 된다.
     */
    @Test
    public fun topAppBar_rendersTitleSubtitleAndActionsAtBaselineHeight() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTopAppBar(
                    title = "지원서 분석",
                    onBackClick = {},
                    modifier = Modifier.testTag(TOP_APP_BAR_TAG),
                    subtitle = "최근 업데이트",
                    actions = { Text("저장") },
                )
            }
        }

        composeRule
            .onNodeWithTag(TOP_APP_BAR_TAG)
            .assertHeightIsAtLeast(56.dp)
        composeRule
            .onNodeWithText("지원서 분석")
            .assertTextEquals("지원서 분석")
        composeRule
            .onNodeWithText("최근 업데이트")
            .assertTextEquals("최근 업데이트")
        composeRule
            .onNodeWithText("저장")
            .assertTextEquals("저장")
    }

    @Test
    public fun nullBackClick_hidesBackControl() {
        setTopAppBar(onBackClick = null)

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .assertDoesNotExist()
    }

    @Test
    public fun backControl_hasButtonSemanticsAndInvokesCallback() {
        var clickCount = 0
        setTopAppBar(onBackClick = { clickCount += 1 })

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            ).assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .assertTouchHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }

    @Test
    public fun blankTitle_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setTopAppBar(title = " \t\n")
            }

        assertEquals("title must not be blank", error.message)
    }

    @Test
    public fun blankSubtitle_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setTopAppBar(subtitle = " \t\n")
            }

        assertEquals("subtitle must be null or non-blank", error.message)
    }

    private fun setTopAppBar(
        title: String = "지원서 분석",
        onBackClick: (() -> Unit)? = {},
        subtitle: String? = null,
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTopAppBar(
                    title = title,
                    onBackClick = onBackClick,
                    subtitle = subtitle,
                )
            }
        }
    }

    private companion object {
        const val TOP_APP_BAR_TAG = "career_compass_top_app_bar"
    }
}
