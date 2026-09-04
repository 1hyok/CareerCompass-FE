package com.careercompass.feature.feed.presentation.board

import androidx.lifecycle.SavedStateHandle

/**
 * 프로세스 사망을 건너 살아남는 게시판 등록 폼 — 이슈 #137(온보딩 쪽 #133 의 나머지 절반).
 *
 * 이 화면은 **다른 앱을 반드시 다녀오는** 흐름이다. 게시판 주소를 브라우저에서 복사해 오기 때문이다. 안드로이드는
 * 그 사이 앱을 언제든 죽이고, 돌아오면 주소·이름·유형·주기가 통째로 비어 있었다.
 *
 * ### 왜 [SavedStateHandle] 인가
 * 등록 폼의 값은 전부 [BoardRegisterViewModel] 이 들고 있고 화면은 그리기만 한다(감지·등록 요청을 만드는
 * 근거도 이 값이다). 상태의 주인이 ViewModel 이므로 저장도 ViewModel 의 저장소가 맡는다.
 *
 * ### 복원하지 않는 것
 * - **구조 감지 결과([BoardDetectionState])와 감지에 쓴 URL**: 서버가 그때 돌려준 사실이다. 되살리면 그 사이
 *   바뀐 게시판을 옛 감지 결과로 등록하게 되고, [BoardDetectionState.Detecting] 은 새 프로세스에 존재하지도
 *   않는 요청을 기다리는 상태가 된다. 주소가 그대로 남아 있으니 「구조 감지」 한 번이면 다시 선다.
 * - **URL 입력 오류([BoardUrlError])**: 검증은 사실이 아니라 그 순간의 피드백이다. 되살리면 아직 손대지도
 *   않은 폼이 빨갛게 떠 있고, 감지·등록을 누르면 어차피 다시 계산한다.
 * - **`isSubmitting`**: 살아난 프로세스에는 그 요청이 없다. 되살리면 영원히 도는 버튼이 된다.
 * - **`message`·`isBackRequested`·`sessionEnded` 같은 단발 신호**: 이미 지나간 사건이다. 죽기 직전의 실패
 *   안내를 새 프로세스가 다시 띄우면 방금 아무 일도 안 한 사용자에게 원인 없는 경고가 된다.
 *
 * 저장 값은 [SavedStateHandle] 이 그대로 `Bundle` 로 나가므로 문자열만 쓴다(유형·주기는 enum 이름). 읽을 때는
 * 모르는 이름을 버려, 낡은 번들이 화면 계약을 깨뜨리지 않게 한다.
 */
internal class BoardRegisterInputDraft(
    private val handle: SavedStateHandle,
) {
    /** 저장 대상만 뽑은 값 — 이것이 바뀔 때만 [save] 한다(감지 진행 상태는 저장 대상이 아니다). */
    data class Input(
        val url: String,
        val name: String,
        val type: BoardType?,
        val cycle: BoardCollectCycle,
    )

    /** 초안을 담은 시작 상태. 감지는 [BoardDetectionState.Idle] 에서 다시 시작한다. */
    fun restoredState(): BoardRegisterViewState =
        BoardRegisterViewState(
            url = handle.get<String>(KEY_URL).orEmpty(),
            name = handle.get<String>(KEY_NAME).orEmpty(),
            type = restoredEnum<BoardType>(KEY_TYPE),
            cycle = restoredEnum<BoardCollectCycle>(KEY_CYCLE) ?: BoardCollectCycle.Daily,
        )

    fun save(input: Input) {
        putBoundedOrRemove(KEY_URL, input.url, MAX_URL_CHARS)
        putBoundedOrRemove(KEY_NAME, input.name, MAX_NAME_CHARS)
        input.type?.let { handle[KEY_TYPE] = it.name } ?: handle.remove<String>(KEY_TYPE)
        handle[KEY_CYCLE] = input.cycle.name
    }

    /**
     * 상한을 넘는 입력은 저장하지 않는다.
     *
     * 저장 상태는 Binder 트랜잭션(약 1MB)을 통과한다 — 브라우저에서 통째로 붙여 넣은 글이 그 한도를 위협하면
     * 저장을 건너뛴다. 앞부분만 남기는 쪽은 고르지 않았다: **잘린 주소는 다른 주소**이고, 되살아난 값으로
     * 감지를 누르면 사용자는 자기가 복사한 주소가 바뀐 줄도 모른 채 「이 사이트는 지원되지 않는다」를 본다.
     * 지금은 저장을 못 지키는 대신 「원래도 없던 것」으로 남는다.
     */
    private fun putBoundedOrRemove(
        key: String,
        value: String,
        maxChars: Int,
    ) {
        if (value.length <= maxChars) handle[key] = value else handle.remove<String>(key)
    }

    private inline fun <reified T : Enum<T>> restoredEnum(key: String): T? =
        handle.get<String>(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

    private companion object {
        /**
         * 초안으로 남기는 게시판 주소의 상한(문자).
         *
         * 주소창·서버·프록시가 실제로 다루는 URL 길이의 통념적 상한(약 2KB)에 맞춘다. 게시판 주소는 보통
         * 100자 안팎이라 여기 걸릴 일이 없고, 걸린다면 주소가 아니라 페이지 본문을 붙여 넣은 경우다.
         */
        const val MAX_URL_CHARS = 2_048

        /**
         * 초안으로 남기는 게시판 이름의 상한(문자).
         *
         * 이름은 게시판 목록에서 한 줄로 보이는 값이라 실제로는 수십 자다. 200자를 넘겼다면 이름 칸에 잘못
         * 붙여 넣은 글이고, 그런 값을 되살려 등록시키면 목록이 깨진다.
         */
        const val MAX_NAME_CHARS = 200

        // 키는 저장 계약이다 — 바꾸면 그 순간 떠 있던 앱의 초안이 복원되지 않는다.
        const val KEY_URL = "board.register.draft.url"
        const val KEY_NAME = "board.register.draft.name"
        const val KEY_TYPE = "board.register.draft.type"
        const val KEY_CYCLE = "board.register.draft.cycle"
    }
}
