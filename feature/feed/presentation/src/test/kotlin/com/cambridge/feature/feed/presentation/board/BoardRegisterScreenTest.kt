package com.cambridge.feature.feed.presentation.board

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoardRegisterScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idleWithBlankUrl_disablesDetectAndHidesForm() {
        composeRule.setRegisterContent(state = sampleState(url = ""))

        composeRule.onNodeWithText("AI가 사이트 구조를 자동 분석").assertIsDisplayed()
        detectButton().assertIsNotEnabled()
        composeRule.onAllNodesWithText("등록하기").assertCountEquals(0)
        composeRule.onAllNodesWithText("게시판 이름 *").assertCountEquals(0)
    }

    @Test
    fun urlInput_emitsUrlChangedAndEnablesDetect() {
        val events = mutableListOf<BoardRegisterEvent>()
        composeRule.setRegisterContent(state = sampleState(), onEvent = events::add)

        composeRule.onNodeWithContentDescription("게시판 URL *").performTextReplacement(TYPED_URL)
        detectButton().assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    BoardRegisterEvent.UrlChanged(TYPED_URL),
                    BoardRegisterEvent.DetectClicked,
                    BoardRegisterEvent.BackClicked,
                ),
                events,
            )
        }
    }

    @Test
    fun urlError_isShownInline() {
        composeRule.setRegisterContent(state = sampleState(urlError = "올바른 URL 형식이 아니에요"))

        composeRule.onNodeWithText("올바른 URL 형식이 아니에요").assertIsDisplayed()
    }

    @Test
    fun detecting_showsProgressAndDisablesDetect() {
        composeRule.setRegisterContent(state = sampleState(detection = BoardDetectionState.Detecting))

        composeRule.onNodeWithText("게시글 구조를 분석하고 있어요").assertIsDisplayed()
        composeRule.onNodeWithText("사이트 응답 속도에 따라 1분 넘게 걸릴 수 있어요").assertIsDisplayed()
        detectButton().assertIsNotEnabled()
    }

    @Test
    fun timedOut_showsWaitingCopyDistinctFromDetectionFailureAndRetries() {
        val events = mutableListOf<BoardRegisterEvent>()
        composeRule.setRegisterContent(
            state = sampleState(detection = BoardDetectionState.TimedOut),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("분석이 오래 걸려 멈췄어요").assertIsDisplayed()
        // 감지 실패 문구가 함께 뜨면 사용자가 사이트가 지원 안 된다고 읽는다.
        composeRule.onNodeWithText("게시글 구조를 찾지 못했어요. 목록 페이지 주소인지 확인해 주세요").assertDoesNotExist()
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(BoardRegisterEvent.DetectClicked), events)
        }
    }

    /**
     * 점검은 **재시도를 권하지 않는** 실패다 — 서버가 돌아와야 답이 달라진다(#144 의 규칙).
     *
     * 「구조를 분석하지 못했어요」로도, 타임아웃 문구로도 새면 안 된다. 앞의 둘은 「네 게시판·네 사이트가
     * 문제」라는 뜻이라 사용자가 멀쩡한 URL 을 의심하게 된다.
     */
    @Test
    fun maintenance_showsServerNoticeWithoutRetryButton() {
        composeRule.setRegisterContent(state = sampleState(detection = BoardDetectionState.Maintenance))

        composeRule.onNodeWithText("서비스가 잠시 점검 중이에요").assertIsDisplayed()
        composeRule.onAllNodesWithText("구조를 분석하지 못했어요. 잠시 후 다시 시도해 주세요").assertCountEquals(0)
        composeRule.onAllNodesWithText("분석이 오래 걸려 멈췄어요").assertCountEquals(0)
        // 눌러도 아무 일 없는 버튼을 만들지 않는다.
        composeRule.onAllNodesWithText("다시 시도").assertCountEquals(0)
        // 그래도 막다른 길은 아니다 — 위의 「구조 분석하기」가 그대로 살아 있다.
        detectButton().assertIsEnabled()
    }

    /** 주소를 고치면 답이 갈리는 실패 — 여기서는 다시 감지할 길을 그 자리에 준다. */
    @Test
    fun failed_showsReasonAndRetryReEmitsDetect() {
        val events = mutableListOf<BoardRegisterEvent>()
        composeRule.setRegisterContent(
            state = sampleState(detection = BoardDetectionState.Failed(BoardDetectionFailure.Failed)),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("게시글 구조를 찾지 못했어요. 목록 페이지 주소인지 확인해 주세요").assertIsDisplayed()
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(BoardRegisterEvent.DetectClicked), events)
        }
    }

    /**
     * 사이트 쪽 사정으로 막힌 실패에는 재시도를 주지 않는다(#204).
     *
     * 몇 번을 다시 보내도 같은 답이 오므로 버튼은 사용자를 같은 상자로 돌려보낼 뿐이다. 그래도 막다른
     * 길이 아니다 — URL 입력란과 「구조 분석하기」가 실패 상자 위에 그대로 남아 주소를 고칠 길을 연다.
     * 나머지 사유의 판정은 `BoardDetectionFailure.isRetryable` 을 도는 테스트가 지킨다.
     */
    @Test
    fun failedWithHopelessReason_hidesRetryButKeepsDetectPath() {
        composeRule.setRegisterContent(
            state = sampleState(detection = BoardDetectionState.Failed(BoardDetectionFailure.LoginRequired)),
        )

        composeRule.onNodeWithText("로그인이 필요한 게시판은 지원하지 않습니다").assertIsDisplayed()
        composeRule.onAllNodesWithText("다시 시도").assertCountEquals(0)
        detectButton().assertIsEnabled()
    }

    @Test
    fun success_showsPreviewAndFormWithRegisterDisabledUntilComplete() {
        composeRule.setRegisterContent(state = sampleState(detection = sampleSuccess()))

        composeRule.onNodeWithText("감지 성공 · 최근 게시글 2개").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2026 SW 인턴 모집 안내").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2026-05-10").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("게시판 이름 *").performScrollTo().assertIsDisplayed()
        composeRule
            .onNode(hasText("1일 1회") and hasStateDescription("선택됨"))
            .performScrollTo()
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        registerButton().assertIsNotEnabled()
    }

    @Test
    fun successWithoutDate_showsParsingWarning() {
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection =
                        BoardDetectionState.Success(
                            preview = listOf(samplePreviewItem(dateLabel = null)),
                            dateDetected = false,
                        ),
                ),
        )

        composeRule
            .onNodeWithText("날짜를 찾지 못해 마감일 파싱 정확도가 낮을 수 있어요")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun completedForm_enablesRegisterAndEmitsFormIntents() {
        val events = mutableListOf<BoardRegisterEvent>()
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection = sampleSuccess(),
                    name = "건국대 공지사항",
                    type = BoardType.Employment,
                ),
            onEvent = events::add,
        )

        composeRule
            .onNodeWithContentDescription("게시판 이름 *")
            .performScrollTo()
            .performTextReplacement("건국대 학교 공지")
        composeRule
            .onNode(hasText("장학금") and hasStateDescription("선택 안 됨"))
            .performScrollTo()
            .performClick()
        composeRule
            .onNode(hasText("주 1회") and hasStateDescription("선택 안 됨"))
            .performScrollTo()
            .performClick()
        registerButton().assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    BoardRegisterEvent.NameChanged("건국대 학교 공지"),
                    BoardRegisterEvent.TypeSelected(BoardType.Scholarship),
                    BoardRegisterEvent.CycleSelected(BoardCollectCycle.Weekly),
                    BoardRegisterEvent.RegisterClicked,
                ),
                events,
            )
        }
    }

    @Test
    fun submitting_disablesEveryAction() {
        val events = mutableListOf<BoardRegisterEvent>()
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection = sampleSuccess(),
                    name = "건국대 공지사항",
                    type = BoardType.Employment,
                    isSubmitting = true,
                ),
            onEvent = events::add,
        )

        detectButton().assertIsNotEnabled().performClick()
        registerButton().assertIsNotEnabled().performClick()
        composeRule
            .onNode(hasText("장학금") and hasStateDescription("선택 안 됨"))
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(emptyList<BoardRegisterEvent>(), events)
        }
    }

    @Test
    fun submitting_showsProgressAboveTheButtonItWasStartedFrom() {
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection = sampleSuccess(),
                    name = "건국대 공지사항",
                    type = BoardType.Employment,
                    isSubmitting = true,
                ),
        )

        // 스크롤 없이 보여야 한다 — performScrollTo 를 쓰면 「본문 어딘가에 있다」만 확인하게 된다.
        composeRule.onNodeWithText("게시판을 등록하고 있어요").assertIsDisplayed()
        composeRule.onNodeWithText("끝나면 게시판 목록으로 돌아가요").assertIsDisplayed()
    }

    @Test
    fun notSubmitting_hidesSubmitProgress() {
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection = sampleSuccess(),
                    name = "건국대 공지사항",
                    type = BoardType.Employment,
                ),
        )

        composeRule.onNodeWithText("게시판을 등록하고 있어요").assertDoesNotExist()
    }

    @Test
    fun largeFontScale_keepsSubmitProgressAndButtonVisible() {
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection = sampleSuccess(),
                    name = "건국대 공지사항",
                    type = BoardType.Employment,
                    isSubmitting = true,
                ),
            fontScale = 2f,
        )

        composeRule.onNodeWithText("게시판을 등록하고 있어요").assertIsDisplayed()
        registerButton().assertIsDisplayed()
    }

    @Test
    fun largeFontScale_keepsRegisterButtonVisible() {
        composeRule.setRegisterContent(
            state =
                sampleState(
                    detection = sampleSuccess(),
                    name = "건국대 공지사항",
                    type = BoardType.Employment,
                ),
            fontScale = 2f,
        )

        registerButton().assertIsDisplayed()
    }

    private fun detectButton() = composeRule.onNode(hasText("구조 분석하기") and hasClickAction())

    private fun registerButton() = composeRule.onNode(hasText("등록하기") and hasClickAction())
}

private fun ComposeContentTestRule.setRegisterContent(
    state: BoardRegisterUiState,
    onEvent: (BoardRegisterEvent) -> Unit = {},
    fontScale: Float = 1f,
) {
    setContent {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(currentDensity.density, fontScale),
        ) {
            CareerCompassTheme {
                BoardRegisterScreen(state = state, onEvent = onEvent)
            }
        }
    }
}

private fun sampleState(
    url: String = SAMPLE_URL,
    urlError: String? = null,
    detection: BoardDetectionState = BoardDetectionState.Idle,
    name: String = "",
    type: BoardType? = null,
    isSubmitting: Boolean = false,
): BoardRegisterUiState =
    BoardRegisterUiState(
        url = url,
        urlError = urlError,
        detection = detection,
        name = name,
        type = type,
        cycle = BoardCollectCycle.Daily,
        isSubmitting = isSubmitting,
    )

private fun sampleSuccess(): BoardDetectionState.Success =
    BoardDetectionState.Success(
        preview =
            listOf(
                samplePreviewItem(),
                samplePreviewItem(title = "1학기 우수학생 장학금 추가 모집", dateLabel = "2026-05-08"),
            ),
        dateDetected = true,
    )

private fun samplePreviewItem(
    title: String = "2026 SW 인턴 모집 안내",
    dateLabel: String? = "2026-05-10",
): BoardPreviewItemUiModel =
    BoardPreviewItemUiModel(
        title = title,
        url = "$SAMPLE_URL/1",
        dateLabel = dateLabel,
    )

private const val SAMPLE_URL = "https://konkuk.ac.kr/board/notice"
private const val TYPED_URL = "https://konkuk.ac.kr/board/scholarship"
