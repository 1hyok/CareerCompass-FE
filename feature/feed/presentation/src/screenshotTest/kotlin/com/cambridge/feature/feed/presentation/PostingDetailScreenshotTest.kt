package com.cambridge.feature.feed.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailContentState
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailScreen
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailUiModel
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailUiState
import com.cambridge.feature.feed.presentation.postingdetail.PostingFormQuestionUiModel
import com.cambridge.feature.feed.presentation.postingdetail.PostingSuitabilityState
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityAxisUiModel
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityUiModel
import com.cambridge.feature.feed.presentation.postingdetail.component.SimilarPostingCard
import com.careercompass.core.model.posting.SuitabilityLabel
import com.careercompass.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Posting detail employment", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailEmploymentPreview() {
    PostingDetailPreviewSurface(
        state = PostingDetailUiState(PostingDetailContentState.Loaded(employmentPostingPreview())),
    )
}

@PreviewTest
@Preview(name = "Posting detail employment - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailEmploymentDarkPreview() {
    PostingDetailPreviewSurface(
        state = PostingDetailUiState(PostingDetailContentState.Loaded(employmentPostingPreview())),
        darkTheme = true,
    )
}

/**
 * 긴 한국어 제목·점수 칩·축 목록이 한 화면에 있는 최악의 경우.
 *
 * 충족 배지가 막대와 같은 줄에서 잘리지 않는지도 여기서 못 박는다. 「미충족」 은 한 글자 더 길지만
 * 배지가 고정 폭을 먼저 가져가고 막대(`weight`)가 그만큼 줄어드는 구조라 같은 줄이 더 넓어질 뿐이다 —
 * 2.0 배율에서 첫 축을 미충족으로 바꿔 렌더해 실측으로 확인했다(#141). 고정 `heightDp` 탓에
 * 미충족 축이 접히는 자리 아래라 골든에는 담기지 않는다.
 */
@PreviewTest
@Preview(name = "Posting detail employment - Large font", widthDp = 360, heightDp = 772, fontScale = LARGE_FONT_SCALE)
@Composable
public fun PostingDetailEmploymentLargeFontPreview() {
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

/** 총점은 왔는데 축 분해가 비어 온 경우 — 「모름」 은 0점 축 4개가 아니라 한 줄 안내로 그린다. */
@PreviewTest
@Preview(name = "Posting detail breakdown unavailable", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailBreakdownUnavailablePreview() {
    val posting = employmentPostingPreview()
    val ready = posting.suitability as PostingSuitabilityState.Ready
    PostingDetailPreviewSurface(
        state =
            PostingDetailUiState(
                PostingDetailContentState.Loaded(
                    posting.copy(
                        suitability = PostingSuitabilityState.Ready(ready.suitability.copy(breakdown = emptyList())),
                    ),
                ),
            ),
    )
}

/**
 * 게이지 색 구간이 F3-2 레이블 구간과 같은지 눈으로 확인하는 자리 — 이슈 #200.
 *
 * 「매우 적합」(88점)·「적합」(64점)은 위쪽 preview 들이 이미 덮으므로, 새로 갈라진 아래 두 구간만 더한다.
 * 색이 갈려도 배지 글자가 함께 갈리는지도 이 골든이 본다.
 */
@PreviewTest
@Preview(name = "Posting detail score band neutral", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailScoreBandNeutralPreview() {
    PostingDetailPreviewSurface(state = scoreBandPreviewState(score = 48, levelLabel = "보통", level = SuitabilityLabel.Neutral))
}

@PreviewTest
@Preview(name = "Posting detail score band low", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailScoreBandLowPreview() {
    PostingDetailPreviewSurface(state = scoreBandPreviewState(score = 24, levelLabel = "낮음", level = SuitabilityLabel.Low))
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
                        suitability = PostingSuitabilityState.Analyzing(isAutoRecheckExhausted = false),
                    ),
                ),
            ),
    )
}

/** 자동 재조회를 다 쓴 뒤(#221) — 진행 표시를 거두고 「다시 확인」을 연다. 실패도 성공도 약속하지 않는 문구다. */
@PreviewTest
@Preview(name = "Posting detail analysis pending", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailAnalysisPendingPreview() {
    PostingDetailPreviewSurface(
        state =
            PostingDetailUiState(
                PostingDetailContentState.Loaded(
                    employmentPostingPreview().copy(
                        suitability = PostingSuitabilityState.Analyzing(isAutoRecheckExhausted = true),
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
                PostingDetailContentState.Error(
                    title = "공고를 불러오지 못했어요",
                    description = "잠시 후 다시 시도해 주세요",
                    isRetryable = true,
                ),
            ),
    )
}

@PreviewTest
@Preview(name = "Posting detail maintenance", widthDp = 360, heightDp = 772)
@Composable
public fun PostingDetailMaintenancePreview() {
    // 서버 점검(503) — 한 줄 오류 문구가 아니라 점검 안내다. 상세는 스냅샷이 없어 오프라인 경로가 없다.
    PostingDetailPreviewSurface(state = PostingDetailUiState(PostingDetailContentState.Maintenance))
}

/**
 * 유사 공고 카드 — 읽은 것과 읽지 않은 것을 나란히 둔다(#165).
 *
 * 화면 골든이 아니라 **부품 골든**이다. 상세 화면 골든은 단말 높이(772dp)를 그대로 두는데 유사 공고
 * 줄은 그 프레임보다 한참 아래라, 이 카드는 지금까지 어느 골든에도 담기지 않았다. 캔버스를 카드가
 * 들어갈 만큼만 잡아 「✓ 읽음」 배지가 실제로 그려지는지, 읽지 않은 카드와 무엇이 다른지를 못 박는다.
 */
@PreviewTest
@Preview(name = "Similar posting cards", widthDp = 360, heightDp = 300)
@Composable
public fun SimilarPostingCardsPreview() {
    SimilarPostingCardsPreviewSurface()
}

/**
 * 같은 카드의 fontScale 2.0 — 메타 줄이 **접히는지** 확인하는 자리다.
 *
 * 마감일과 「✓ 읽음」 배지는 `FlowRow` 라 폭이 모자라면 두 줄로 나뉜다. 잘려서 사라지는 것이 아니라
 * 카드가 세로로 길어지기만 하는지, 체크 표시가 문구를 따라 함께 커지는지(sp 기준)를 여기서 본다.
 * 캔버스 높이를 키우는 것은 부품 골든이라 허용된다 — `docs/testing/screenshot.md` 「캔버스 높이」.
 */
@PreviewTest
@Preview(name = "Similar posting cards - Large font", widthDp = 360, heightDp = 480, fontScale = LARGE_FONT_SCALE)
@Composable
public fun SimilarPostingCardsLargeFontPreview() {
    SimilarPostingCardsPreviewSurface()
}

@Composable
private fun SimilarPostingCardsPreviewSurface() {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimilarPostingCard(listing = similarPostingPreview(isRead = true), onClick = {})
                SimilarPostingCard(listing = similarPostingPreview(isRead = false), onClick = {})
            }
        }
    }
}

private fun similarPostingPreview(isRead: Boolean): FeedListingUiModel =
    FeedListingUiModel(
        id = if (isRead) "boostcamp" else "kakao-tech",
        title = if (isRead) "네이버 부스트캠프 9기 모집" else "카카오테크 캠퍼스 3기 모집",
        category = FeedListingCategory.Employment,
        categoryLabel = "채용",
        sourceLabel = "네이버 채용",
        suitability = FeedSuitabilityState.Scored(76),
        // 읽은 쪽에 가장 긴 마감 문구를 물린다 — 마감일과 배지가 한 줄에서 가장 넓게 서는 경우다.
        deadlineLabel = if (isRead) "오늘 마감" else "D-14",
        isDeadlineUrgent = isRead,
        collectedAtLabel = "수집 3일 전",
        isNew = false,
        isRead = isRead,
        isBookmarked = false,
    )

@Composable
private fun PostingDetailPreviewSurface(
    state: PostingDetailUiState,
    darkTheme: Boolean = false,
) {
    CareerCompassTheme(darkTheme = darkTheme) {
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
                    // 총점은 축 가중합 그대로다: 95*.4 + 88*.3 + 78*.2 + 50*.1 = 85.
                    score = 85,
                    levelLabel = "매우 적합",
                    level = SuitabilityLabel.VerySuitable,
                    breakdown =
                        listOf(
                            SuitabilityAxisUiModel(label = "분야 유사도", score = 95, weightLabel = "40%"),
                            SuitabilityAxisUiModel(label = "자격 조건 충족도", score = 88, weightLabel = "30%"),
                            SuitabilityAxisUiModel(label = "우대 조건 매칭", score = 78, weightLabel = "20%"),
                            SuitabilityAxisUiModel(label = "경쟁 강도(역점)", score = 50, weightLabel = "10%"),
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
                    suitability = FeedSuitabilityState.Scored(76),
                    deadlineLabel = "D-14",
                    isDeadlineUrgent = false,
                    collectedAtLabel = "수집 3일 전",
                    isNew = false,
                    isRead = true,
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
                    level = SuitabilityLabel.Suitable,
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
                    suitability = FeedSuitabilityState.Scored(58),
                    deadlineLabel = "D-21",
                    isDeadlineUrgent = false,
                    collectedAtLabel = "오늘 수집",
                    isNew = true,
                    isRead = false,
                    isBookmarked = false,
                ),
            ),
        canCreateDraft = false,
    )

private fun scoreBandPreviewState(
    score: Int,
    levelLabel: String,
    level: SuitabilityLabel,
): PostingDetailUiState {
    val posting = contestPostingPreview()
    val ready = posting.suitability as PostingSuitabilityState.Ready
    return PostingDetailUiState(
        PostingDetailContentState.Loaded(
            posting.copy(
                suitability =
                    PostingSuitabilityState.Ready(
                        ready.suitability.copy(score = score, levelLabel = levelLabel, level = level),
                    ),
            ),
        ),
    )
}
