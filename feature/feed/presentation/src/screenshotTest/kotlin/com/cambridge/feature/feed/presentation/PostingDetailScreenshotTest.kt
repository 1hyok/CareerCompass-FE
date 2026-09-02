package com.cambridge.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailContentState
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailScreen
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailUiModel
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailUiState
import com.cambridge.feature.feed.presentation.postingdetail.PostingFormQuestionUiModel
import com.cambridge.feature.feed.presentation.postingdetail.PostingSuitabilityState
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityAxisUiModel
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityUiModel

@PreviewTest
@Preview(name = "Posting detail employment", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailEmploymentPreview() {
    PostingDetailPreviewSurface(
        state = PostingDetailUiState(PostingDetailContentState.Loaded(employmentPostingPreview())),
    )
}

@PreviewTest
@Preview(name = "Posting detail contest", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailContestPreview() {
    PostingDetailPreviewSurface(
        state = PostingDetailUiState(PostingDetailContentState.Loaded(contestPostingPreview())),
    )
}

@PreviewTest
@Preview(name = "Posting detail profile incomplete", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailProfileIncompletePreview() {
    PostingDetailPreviewSurface(
        state =
            PostingDetailUiState(
                PostingDetailContentState.Loaded(
                    employmentPostingPreview().copy(
                        suitability = PostingSuitabilityState.ProfileIncomplete,
                    ),
                ),
            ),
    )
}

@PreviewTest
@Preview(name = "Posting detail analyzing", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailAnalyzingPreview() {
    PostingDetailPreviewSurface(
        state =
            PostingDetailUiState(
                PostingDetailContentState.Loaded(
                    employmentPostingPreview().copy(
                        suitability = PostingSuitabilityState.Analyzing,
                    ),
                ),
            ),
    )
}

@PreviewTest
@Preview(name = "Posting detail loading", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailLoadingPreview() {
    PostingDetailPreviewSurface(state = PostingDetailUiState(PostingDetailContentState.Loading))
}

@PreviewTest
@Preview(name = "Posting detail error", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailErrorPreview() {
    PostingDetailPreviewSurface(
        state =
            PostingDetailUiState(
                PostingDetailContentState.Error(message = "공고를 불러오지 못했어요. 네트워크 연결을 확인해 주세요"),
            ),
    )
}

@Composable
private fun PostingDetailPreviewSurface(state: PostingDetailUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            PostingDetailScreen(state = state, onEvent = {})
        }
    }
}

private fun employmentPostingPreview(): PostingDetailUiModel =
    PostingDetailUiModel(
        id = "kakao",
        title = "2026 카카오 SW 인턴십 (백엔드)",
        category = FeedListingCategory.Employment,
        categoryLabel = "채용",
        sourceLabel = "공식 채용",
        collectedAtLabel = "2시간 전",
        deadlineLabel = "2026.05.25",
        isDeadlineUrgent = false,
        isBookmarked = false,
        suitability =
            PostingSuitabilityState.Ready(
                SuitabilityUiModel(
                    score = 88,
                    levelLabel = "매우 적합",
                    level = CareerCompassScoreLevel.High,
                    breakdown =
                        listOf(
                            SuitabilityAxisUiModel(label = "분야 유사도", score = 95, weightLabel = "40%"),
                            SuitabilityAxisUiModel(label = "자격 조건 충족도", score = 88, weightLabel = "30%"),
                            SuitabilityAxisUiModel(label = "우대 조건 매칭", score = 78, weightLabel = "20%"),
                            SuitabilityAxisUiModel(label = "경쟁 강도(역점)", score = 80, weightLabel = "10%"),
                        ),
                    strengthComment = "\"학교 도서관 좌석 알리미\" 프로젝트의 Spring-JPA 경험이 우대 조건과 일치합니다",
                    weaknessComment = "이 공고는 어학 성적을 요구하나 해당 정보가 프로필에 없습니다",
                ),
            ),
        keywords = listOf("Kotlin", "Spring", "JPA", "백엔드", "인턴십"),
        qualifications = listOf("2년제 이상 4년제 대학 재학생 (전공 무관)", "졸업까지 2학기 이상 남은 자"),
        preferences = listOf("Java 또는 Kotlin 기반 백엔드 프로젝트 경험", "Spring Framework 및 RDB 사용 경험"),
        formQuestions =
            listOf(
                PostingFormQuestionUiModel(order = 1, question = "지원 동기를 작성해 주세요", maxCharsLabel = "최대 1,000자"),
                PostingFormQuestionUiModel(order = 2, question = "가장 몰입했던 프로젝트 경험", maxCharsLabel = "최대 1,500자"),
            ),
        similarPostings =
            listOf(
                FeedListingUiModel(
                    id = "boostcamp",
                    title = "네이버 부스트캠프 9기 모집",
                    category = FeedListingCategory.Employment,
                    categoryLabel = "채용",
                    sourceLabel = "네이버 채용",
                    suitabilityScore = 76,
                    deadlineLabel = "D-14",
                    isDeadlineUrgent = false,
                    isNew = false,
                    isBookmarked = true,
                ),
            ),
        canCreateDraft = true,
    )

private fun contestPostingPreview(): PostingDetailUiModel =
    PostingDetailUiModel(
        id = "contest",
        title = "제 15회 대학생 SW 공모전",
        category = FeedListingCategory.Contest,
        categoryLabel = "공모전",
        sourceLabel = "공식 사이트",
        collectedAtLabel = "1일 전",
        deadlineLabel = "2026.06.20",
        isDeadlineUrgent = false,
        isBookmarked = true,
        suitability =
            PostingSuitabilityState.Ready(
                SuitabilityUiModel(
                    score = 64,
                    levelLabel = "적합",
                    level = CareerCompassScoreLevel.Mid,
                    breakdown =
                        listOf(
                            SuitabilityAxisUiModel(label = "분야 유사도", score = 72, weightLabel = "40%"),
                            SuitabilityAxisUiModel(label = "자격 조건 충족도", score = 60, weightLabel = "30%"),
                            SuitabilityAxisUiModel(label = "우대 조건 매칭", score = 55, weightLabel = "20%"),
                            SuitabilityAxisUiModel(label = "경쟁 강도(역점)", score = 58, weightLabel = "10%"),
                        ),
                    strengthComment = null,
                    weaknessComment = "팀 단위 공모전 수상 경험이 프로필에 없습니다",
                ),
            ),
        keywords = listOf("공모전", "SW", "팀 프로젝트"),
        qualifications = listOf("전국 대학(원)생"),
        preferences = emptyList(),
        formQuestions = emptyList(),
        similarPostings =
            listOf(
                FeedListingUiModel(
                    id = "campus-contest",
                    title = "교내 창업 아이디어 경진대회",
                    category = FeedListingCategory.Contest,
                    categoryLabel = "공모전",
                    sourceLabel = "학교 게시판",
                    suitabilityScore = 58,
                    deadlineLabel = "D-21",
                    isDeadlineUrgent = false,
                    isNew = true,
                    isBookmarked = false,
                ),
            ),
        canCreateDraft = false,
    )
