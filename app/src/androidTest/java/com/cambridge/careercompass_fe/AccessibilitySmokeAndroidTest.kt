package com.cambridge.careercompass_fe

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.cambridge.careercompass_fe.test.FailureArtifactRule
import com.cambridge.feature.feed.presentation.FeedContentState
import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedScreen
import com.cambridge.feature.feed.presentation.FeedSortUiModel
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import com.cambridge.feature.feed.presentation.FeedUiState
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.OnboardingApplicationDocument
import com.careercompass.feature.onboarding.presentation.OnboardingApplicationDocumentFormat
import com.careercompass.feature.onboarding.presentation.OnboardingApplicationDocumentStatus
import com.careercompass.feature.onboarding.presentation.OnboardingExperience
import com.careercompass.feature.onboarding.presentation.OnboardingExperienceType
import com.careercompass.feature.onboarding.presentation.OnboardingJobOption
import com.careercompass.feature.onboarding.presentation.OnboardingStep1Screen
import com.careercompass.feature.onboarding.presentation.OnboardingStep1UiState
import com.careercompass.feature.onboarding.presentation.OnboardingStep2Screen
import com.careercompass.feature.onboarding.presentation.OnboardingStep2UiState
import com.careercompass.feature.onboarding.presentation.OnboardingStep3Screen
import com.careercompass.feature.onboarding.presentation.OnboardingStep3UiState
import com.careercompass.feature.onboarding.presentation.OnboardingStep4Screen
import com.careercompass.feature.onboarding.presentation.OnboardingStep4UiState
import com.careercompass.feature.onboarding.presentation.login.LoginScreen
import com.careercompass.feature.onboarding.presentation.login.LoginUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** API 34 Accessibility Test Framework로 주요 화면의 고정된 실제 semantics를 검사한다. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
@OptIn(ExperimentalTestApi::class)
class AccessibilitySmokeAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot(useUnmergedTree = true).captureToImage().asAndroidBitmap()
        }

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    /** 앱 시작 화면(세션 없음) — 저장소 정책 테스트의 selected 계획 픽스처가 이 이름을 참조한다. */
    @Test
    fun welcomeAndLogin_haveNoAutomatedAccessibilityErrors() {
        renderAndCheck {
            LoginScreen(state = LoginUiState(), onEvent = {})
        }
    }

    @Test
    fun onboardingStep1_hasNoAutomatedAccessibilityErrors() {
        renderAndCheck {
            OnboardingStep1Screen(state = onboardingStep1State, onEvent = {})
        }
    }

    @Test
    fun onboardingStep2_hasNoAutomatedAccessibilityErrors() {
        renderAndCheck {
            OnboardingStep2Screen(state = onboardingStep2State, onEvent = {})
        }
    }

    @Test
    fun onboardingStep3_hasNoAutomatedAccessibilityErrors() {
        renderAndCheck {
            OnboardingStep3Screen(state = onboardingStep3State, onEvent = {})
        }
    }

    @Test
    fun onboardingStep4_hasNoAutomatedAccessibilityErrors() {
        renderAndCheck {
            OnboardingStep4Screen(state = onboardingStep4State, onEvent = {})
        }
    }

    @Test
    fun feed_hasNoAutomatedAccessibilityErrors() {
        renderAndCheck {
            FeedScreen(state = feedState, onEvent = {})
        }
    }

    private fun renderAndCheck(content: @Composable () -> Unit) {
        composeRule.setContent {
            CareerCompassTheme {
                content()
            }
        }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private companion object {
        val onboardingStep1State =
            OnboardingStep1UiState(
                name = "정일혁",
                school = "건국대학교",
                major = "컴퓨터공학부",
                gradePointAverage = "3.87",
                graduationDate = "2027.02",
            )

        val onboardingStep2State =
            OnboardingStep2UiState(
                jobOptions =
                    listOf(
                        OnboardingJobOption(id = "backend", label = "백엔드 개발"),
                        OnboardingJobOption(id = "frontend", label = "프론트엔드"),
                        OnboardingJobOption(id = "data", label = "데이터 분석"),
                    ),
                selectedJobIds = setOf("backend"),
                interestTags = listOf("AI", "스타트업"),
            )

        val onboardingStep3State =
            OnboardingStep3UiState(
                experienceTypes =
                    listOf(
                        OnboardingExperienceType(id = "project", label = "프로젝트"),
                        OnboardingExperienceType(id = "award", label = "수상"),
                    ),
                selectedExperienceTypeId = "project",
                experiences =
                    listOf(
                        OnboardingExperience(
                            id = "career-compass",
                            typeId = "project",
                            title = "CareerCompass - 졸업 프로젝트",
                            period = "2025.09 — 진행 중",
                            role = "프론트엔드",
                            tags = listOf("Android", "Kotlin", "Compose"),
                        ),
                    ),
            )

        val onboardingStep4State =
            OnboardingStep4UiState(
                uploadedDocuments =
                    listOf(
                        OnboardingApplicationDocument(
                            id = "application-1",
                            fileName = "2024 카카오 인턴 자소서.pdf",
                            format = OnboardingApplicationDocumentFormat.PDF,
                            fileSizeBytes = 512L * 1024L,
                            status =
                                OnboardingApplicationDocumentStatus.Completed(
                                    classifiedItemCount = 4,
                                ),
                        ),
                    ),
            )

        val feedState =
            FeedUiState(
                userName = "일혁",
                newListingCount = 12,
                searchQuery = "",
                filters =
                    listOf(
                        FeedFilterUiModel(FeedListingCategory.All, "전체"),
                        FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
                        FeedFilterUiModel(FeedListingCategory.Scholarship, "장학금"),
                    ),
                selectedFilter = FeedListingCategory.All,
                selectedSort = FeedSortUiModel(id = "fit", label = "적합도 높은순"),
                totalListingCount = 2,
                content =
                    FeedContentState.Loaded(
                        listOf(
                            FeedListingUiModel(
                                id = "listing-1",
                                title = "2026 카카오 SW 인턴십 모집",
                                category = FeedListingCategory.Employment,
                                categoryLabel = "채용",
                                sourceLabel = "공식 채용",
                                suitability = FeedSuitabilityState.Scored(88),
                                deadlineLabel = "D-7",
                                isDeadlineUrgent = false,
                                collectedAtLabel = "오늘 수집",
                                isNew = true,
                                isRead = false,
                                isBookmarked = false,
                            ),
                            // 읽은 카드도 함께 그린다 — 「읽음」 배지와 흐려진 제목이 대비 기준을
                            // 넘는지는 읽지 않은 카드만으로는 검사되지 않는다(#140).
                            FeedListingUiModel(
                                id = "listing-2",
                                title = "네이버 부스트캠프 9기 모집",
                                category = FeedListingCategory.Employment,
                                categoryLabel = "채용",
                                sourceLabel = "네이버 채용",
                                suitability = FeedSuitabilityState.Scored(76),
                                deadlineLabel = "D-14",
                                isDeadlineUrgent = false,
                                collectedAtLabel = "수집 3일 전",
                                isNew = false,
                                isRead = true,
                                isBookmarked = false,
                            ),
                        ),
                    ),
            )
    }
}
