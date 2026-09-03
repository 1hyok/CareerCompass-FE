package com.cambridge.feature.feed.presentation.board

import androidx.lifecycle.SavedStateHandle
import com.cambridge.feature.feed.presentation.shared.util.toCollectCycle
import com.cambridge.feature.feed.presentation.shared.util.toUiBoardType

/**
 * 프로세스 사망을 건너 살아남는 게시판 **수정** 시트의 입력 — 이슈 #156(#137 이 범위 밖으로 남긴 것).
 *
 * ### 서버 값과의 우선순위 — **필드 단위로 서버가 이긴다**
 * 이 시트는 서버에 이미 있는 게시판을 고치는 자리다. 초안을 통째로 되살리면 그 사이 서버에서 바뀐 값 위에
 * 옛 입력이 덮인다. 그래서 여기서 남기는 것은 **폼의 값이 아니라 사용자가 실제로 바꾼 필드**뿐이다
 * ([BoardEditDraft.toUpdate] 가 만드는 부분 수정과 같은 것). 시트를 다시 열 때 바탕은 **그 순간 목록에
 * 있는 서버 값**이고([BoardEditDraft.from]), 그 위에 이 diff 만 덮는다 — 손대지 않은 필드는 언제나 서버
 * 값으로 선다.
 *
 * ### 왜 「알아채고 물어보기」가 아닌가 — API_SPEC v0.1 §5 를 먼저 봤다
 * 서버 값이 그 사이 바뀐 것을 **클라이언트가 알아챌 방법이 없다.**
 * - `GET /boards` 항목(`BoardDto`)에 `updatedAt`·`version` 류가 없다. `lastCollectedAt` 은 마지막 **수집**
 *   시각이라 이름·유형·주기가 언제 바뀌었는지 말해 주지 않는다.
 * - `PATCH /boards/{id}` 는 조건부 요청이 아니다 — `ETag`/`If-Match` 를 주고받지 않으므로 「내가 본 값이
 *   아직 그대로일 때만 바꿔라」를 서버에 부탁할 수 없고, 충돌은 거절되지 않고 그냥 덮인다.
 * - 단건 조회(`GET /boards/{id}`)도 없어 시트를 열 때 그 게시판만 다시 확인할 수도 없다.
 *
 * 알아챌 수 없으니 「충돌이 났다」고 물어보는 선택지는 애초에 없다. 남는 것은 **덮어쓸 수 있는 면적을
 * 구조적으로 줄이는 것**뿐이고, 그게 초안을 diff 로만 남기는 이유다. 사용자가 이름 한 칸만 고쳤다면 살아난
 * 뒤에도 `PATCH` 에 실리는 것은 이름 하나다.
 *
 * ### #133 이 Step 3 경험 카드에서 복원을 접은 것과 무엇이 다른가
 * 그쪽 시트는 5필드만 받고 **나머지를 원본에서 물려받아 통째로 다시 제출**한다 — 초안을 되살리면 손대지도
 * 않은 필드가 옛 값으로 서버에 실려 나간다. 게시판 수정은 반대다. `BoardUpdateRequestDto` 가 null 인 필드를
 * 직렬화하지 않아 **바꾼 필드만** 나간다. 「서버 값과 충돌하지 않는다」를 계약 수준에서 지킬 수 있으므로
 * 같은 결론(복원하지 않는다)에 묶이지 않는다.
 *
 * ### 복원하지 않는 것
 * - **시트가 열려 있었는지**: 프로세스 사망은 사용자가 의도한 이동이 아니다. 돌아왔는데 시트가 떠 있으면
 *   마지막으로 본 화면과 다르다. 대신 그 게시판을 **다시 누르면** 고치던 값이 그 자리에 있다 —
 *   피드 필터 시트와 같은 처분이다(`FeedInputDraft`).
 * - **원본 게시판([BoardEditDraft.board])**: 그건 서버가 그때 돌려준 사실이다. 되살리면 목록이 방금 다시
 *   읽어 온 값을 낡은 스냅샷이 밀어내고, 「바뀐 필드」 판정의 기준까지 과거로 되돌아간다.
 * - **`isSaving`**: 살아난 프로세스에는 그 요청이 없다. 되살리면 영원히 도는 저장 버튼이 된다.
 *
 * 저장 값은 [SavedStateHandle] 이 그대로 `Bundle` 로 나가므로 `Long` 과 문자열만 쓴다(유형·주기는 enum
 * 이름). 읽을 때는 모르는 이름을 버려, 낡은 번들이 화면 계약을 깨뜨리지 않게 한다.
 */
internal class BoardEditInputDraft(
    private val handle: SavedStateHandle,
) {
    /**
     * 되살린 「사용자가 바꾼 필드」. null 인 필드는 **손대지 않았다**는 뜻이라 서버 값이 그대로 선다.
     *
     * [boardId] 를 함께 들고 다니는 이유는 하나다 — 다른 게시판을 열었을 때 남의 초안이 따라붙지 않게.
     */
    data class Edited(
        val boardId: Long,
        val name: String? = null,
        val type: BoardType? = null,
        val cycle: BoardCollectCycle? = null,
    ) {
        val isEmpty: Boolean get() = name == null && type == null && cycle == null

        /** 서버 값으로 세운 [base] 위에 바꾼 필드만 덮는다. 게시판이 다르면 남의 초안이므로 그대로 둔다. */
        fun applyTo(base: BoardEditDraft): BoardEditDraft =
            if (boardId != base.board.id) {
                base
            } else {
                base.copy(name = name ?: base.name, type = type ?: base.type, cycle = cycle ?: base.cycle)
            }
    }

    /** 남아 있는 초안. 바꾼 필드가 하나도 없으면 초안이 아니다. */
    fun restoredEdit(): Edited? {
        val boardId = handle.get<Long>(KEY_BOARD_ID) ?: return null
        return Edited(
            boardId = boardId,
            name = handle.get<String>(KEY_NAME)?.takeIf { it.isNotBlank() },
            type = restoredEnum<BoardType>(KEY_TYPE),
            cycle = restoredEnum<BoardCollectCycle>(KEY_CYCLE),
        ).takeIf { !it.isEmpty }
    }

    /**
     * 지금 시트가 들고 있는 diff 를 남긴다. 원본과 다른 것이 없으면 남길 초안도 없다.
     *
     * 저장 대상을 [BoardEditDraft.toUpdate] 에서 그대로 뽑는다. 「무엇이 바뀐 필드인가」의 판정이 요청을
     * 만드는 자리와 초안을 남기는 자리에서 갈라지면, 살아난 초안이 서버에 보내는 것과 달라진다.
     *
     * 남는 초안은 **하나뿐**이다 — 시트는 한 번에 하나만 열리고, 다른 게시판을 열면 그쪽 diff 가 이 자리를
     * 차지한다. 어느 게시판의 것인지 [KEY_BOARD_ID] 로 함께 적어 두므로 남의 초안이 따라붙지는 않는다.
     */
    fun save(draft: BoardEditDraft) {
        val update = draft.toUpdate()
        if (update.isEmpty) {
            clear()
            return
        }
        handle[KEY_BOARD_ID] = draft.board.id
        putBoundedOrRemove(KEY_NAME, update.name, MAX_NAME_CHARS)
        putEnumOrRemove(KEY_TYPE, update.type?.toUiBoardType())
        putEnumOrRemove(KEY_CYCLE, update.cycleHours?.toCollectCycle())
    }

    /**
     * 초안을 버린다. 시트가 닫히는 이유는 취소·저장 둘뿐이고 둘 다 버려야 할 초안이라, 닫힘을 여기서
     * 추측하지 않고 호출부가 명시한다(온보딩 시트 초안과 같은 처분 — `OnboardingInputDraft`).
     */
    fun clear() {
        handle.remove<Long>(KEY_BOARD_ID)
        handle.remove<String>(KEY_NAME)
        handle.remove<String>(KEY_TYPE)
        handle.remove<String>(KEY_CYCLE)
    }

    /**
     * 상한을 넘는 이름은 저장하지 않는다.
     *
     * 이름은 게시판 목록에서 한 줄로 보이는 값이라 실제로는 수십 자다(등록 폼도 같은 상한을 쓴다 —
     * [BoardRegisterInputDraft]). 200자를 넘겼다면 이름 칸에 잘못 붙여 넣은 글이고, 저장 상태는 Binder
     * 트랜잭션(약 1MB)을 통과해야 한다. 앞부분만 남기는 쪽은 고르지 않았다 — **잘린 이름은 다른 이름**이고,
     * 되살아난 그 값으로 저장을 누르면 사용자가 모르는 사이 게시판 이름이 반토막 난다. 지금은 그 필드만
     * 「원래도 없던 것」으로 남고, 서버 이름이 그대로 선다.
     */
    private fun putBoundedOrRemove(
        key: String,
        value: String?,
        maxChars: Int,
    ) {
        if (value != null && value.length <= maxChars) handle[key] = value else handle.remove<String>(key)
    }

    private fun <T : Enum<T>> putEnumOrRemove(
        key: String,
        value: T?,
    ) {
        value?.let { handle[key] = it.name } ?: handle.remove<String>(key)
    }

    private inline fun <reified T : Enum<T>> restoredEnum(key: String): T? =
        handle.get<String>(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

    private companion object {
        /** 초안으로 남기는 게시판 이름의 상한(문자) — 등록 폼과 같은 근거·같은 값이다. */
        const val MAX_NAME_CHARS = 200

        // 키는 저장 계약이다 — 바꾸면 그 순간 떠 있던 앱의 초안이 복원되지 않는다.
        const val KEY_BOARD_ID = "board.edit.draft.boardId"
        const val KEY_NAME = "board.edit.draft.name"
        const val KEY_TYPE = "board.edit.draft.type"
        const val KEY_CYCLE = "board.edit.draft.cycle"
    }
}
