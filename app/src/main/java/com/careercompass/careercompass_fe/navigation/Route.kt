package com.careercompass.careercompass_fe.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 앱 셸이 소유하는 루트 Navigation 3 백스택의 키.
 *
 * 피처 그래프는 각자의 로컬 스택을 갖고([Onboarding] · [Feed] 가 그 host 다), 여기에는 그 host 와 다른 담당 모듈이
 * 진입점을 제공하기 전까지 셸이 대신 그리는 자리표시자만 둔다. `@Serializable` 은 프로세스 재생성 뒤 루트 스택을
 * 복원하는 데 쓰인다(#260).
 */
@Serializable
public sealed interface Route : NavKey {
    /** 온보딩 host — 로그인·지문·Step 1~4·완료를 담은 로컬 스택. 시작 화면은 셸이 고른다. */
    @Serializable
    public data object Onboarding : Route

    /** 피드 host — 하단 탭 「피드」이자 메인 루트. 홈·상세·원문·게시판을 담은 로컬 스택. */
    @Serializable
    public data object Feed : Route

    /** 하단 탭 「분석」(For You·커리어 로드맵·강점 Export) — foryou 모듈 몫. */
    @Serializable
    public data object AnalysisTab : Route

    /** 하단 탭 「지원서」(AI 초안·에디터·이력) — editor 모듈 몫. */
    @Serializable
    public data object ApplicationsTab : Route

    /**
     * 하단 탭 「마이」(프로필·경험 카드·과거 자소서·알림 설정) — profile 모듈 몫.
     *
     * 인수 전까지 셸이 세션 카드와 로그아웃만 그린다([MyTabPlaceholderScreen]) — 그것 말고는 세션을 끝낼 방법이 없다.
     */
    @Serializable
    public data object MyTab : Route

    /** 피드 헤더의 알림 — notification 모듈 몫. */
    @Serializable
    public data object NotificationsPlaceholder : Route
}
