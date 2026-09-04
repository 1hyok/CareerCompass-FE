package com.careercompass.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.postingraw.PostingRawScreen
import com.careercompass.feature.feed.presentation.postingraw.PostingRawUiState

@PreviewTest
@Preview(name = "Posting raw", widthDp = 360, heightDp = 772)
@Composable
public fun PostingRawPreview() {
    PostingRawPreviewSurface(state = postingRawPreviewState())
}

@PreviewTest
@Preview(name = "Posting raw without link", widthDp = 360, heightDp = 772)
@Composable
public fun PostingRawWithoutLinkPreview() {
    PostingRawPreviewSurface(state = postingRawPreviewState().copy(originalUrl = null))
}

@Composable
private fun PostingRawPreviewSurface(state: PostingRawUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            PostingRawScreen(state = state, onEvent = {})
        }
    }
}

private fun postingRawPreviewState(): PostingRawUiState =
    PostingRawUiState(
        title = "2026 카카오 SW 인턴십 (백엔드) 모집",
        sourceLabel = "카카오 채용팀 · 2026.05.10 게시",
        originalUrl = "https://careers.kakao.com/jobs/1",
        rawContent =
            """
            [모집 분야]
            Server (Java/Kotlin) 백엔드 개발 인턴십 — 검색·메시징·결제 도메인

            [자격 요건]
            • 2년제 이상 4년제 대학 재학생 (전공 무관)
            • 졸업까지 2학기 이상 남은 자
            • 풀타임 근무 가능한 자
            • 자료구조·알고리즘 기본 지식

            [우대 사항]
            • Java 또는 Kotlin 기반 백엔드 프로젝트 경험
            • Spring Framework 및 RDB 사용 경험
            • 분산 시스템·메시지큐·캐시 등에 대한 이해
            • 공개된 오픈소스 기여 경험
            """.trimIndent(),
    )
