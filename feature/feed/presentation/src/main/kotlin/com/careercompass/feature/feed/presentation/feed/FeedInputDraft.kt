package com.careercompass.feature.feed.presentation.feed

import androidx.lifecycle.SavedStateHandle
import com.careercompass.core.model.posting.PostingSort
import com.careercompass.core.model.posting.PostingType
import com.careercompass.feature.feed.domain.model.FeedQuery
import com.careercompass.feature.feed.presentation.FeedListingCategory
import com.careercompass.feature.feed.presentation.feedfilter.FeedDeadlineRange
import com.careercompass.feature.feed.presentation.shared.util.toDomainDeadlineFilter
import com.careercompass.feature.feed.presentation.shared.util.toUiDeadlineFilter
import com.careercompass.feature.feed.presentation.shared.util.toUiDeadlineRange
import java.time.LocalDate
import com.careercompass.feature.feed.domain.model.FeedDeadlineFilter as DomainDeadlineFilter
import com.careercompass.feature.feed.presentation.feedfilter.FeedDeadlineFilter as UiDeadlineFilter

/**
 * 프로세스 사망을 건너 살아남는 피드의 **조회 조건** — 이슈 #137(온보딩 쪽 #133 의 나머지 절반).
 *
 * 검색하다 알림을 확인하고 돌아오면 검색 전 목록으로 되돌아가 있었다. 안드로이드는 백그라운드 앱을 언제든
 * 죽이고, [FeedViewModel] 의 상태는 여태 메모리에만 있었다.
 *
 * ### 왜 [SavedStateHandle] 인가
 * 검색어·필터·정렬은 전부 [FeedViewModel] 이 들고 있고(목록 조회를 부르는 근거이기도 하다), 화면은 그
 * 상태를 그리기만 한다. 상태의 주인이 ViewModel 이므로 저장도 ViewModel 의 저장소가 맡는다 — 화면 쪽에
 * `rememberSaveable` 을 두면 같은 값이 두 곳에 살면서 어느 쪽이 조회의 정본인지 흐려진다.
 *
 * ### 저장 시점 — 상태 흐름 하나를 구독하되, 저장 대상이 바뀔 때만 쓴다
 * 입력을 바꾸는 자리가 여럿(검색 입력·칩·시트 이벤트 10여 개·정렬)이라 각자 저장하게 두면 언젠가 한 곳이
 * 빠지고, 빠진 자리는 프로세스가 죽어야 드러난다. 그래서 #133 처럼 상태 흐름을 구독한다. 다만 온보딩과 달리
 * 피드 상태는 입력과 무관하게 쉴 새 없이 바뀐다(페이지 누적·새로고침·북마크 토글·프로필 수신) — 그대로
 * 구독하면 목록이 흔들릴 때마다 열 개 남짓한 키를 다시 쓴다. [Input] 으로 저장 대상만 뽑아
 * `distinctUntilChanged` 를 걸어, 실제로 조건이 바뀐 순간에만 쓴다.
 *
 * ### 복원하지 않는 것
 * - **목록·페이징 결과([FeedViewState.postings]·`nextCursor`)**: 서버가 준 사실의 사본이고, 그 사이 공고가
 *   늘거나 마감됐을 수 있다. 되살리면 낡은 목록 위에 다음 페이지가 이어 붙는다. 복원한 조건은 목록을 **다시
 *   조회할 근거**일 뿐이라, `init` 의 `load()` 가 그 조건으로 첫 페이지를 새로 읽는다.
 * - **오프라인 스냅샷과 그 표시**: 스냅샷은 저장소에 따로 있고 실패했을 때 다시 읽는다. 살아난 프로세스는
 *   아직 아무것도 조회하지 않았으므로 오프라인이라고 말할 근거가 없다.
 * - **시트·정렬 메뉴가 열려 있었는지**: 프로세스 사망은 사용자가 의도한 이동이 아니다. 돌아왔는데 시트가 떠
 *   있으면 마지막으로 본 화면과 달라 놀란다. 대신 **다시 열면** 고르던 값이 그대로 있다([restoredFilterDraft]).
 * - **`message`·`pendingNavigation`·`sessionEnded` 같은 단발 신호**: 이미 지나간 사건이다. 죽기 직전의 실패
 *   안내를 새 프로세스가 다시 띄우면 방금 아무 일도 안 한 사용자에게 원인 없는 경고가 된다.
 * - **게시판 목록·오늘 신규 개수·프로필**: 서버·저장소에서 `init` 이 다시 읽는다.
 *
 * ### 검색어는 「입력창의 글자」 하나만 저장한다
 * 검색은 [FeedViewModel.SEARCH_DEBOUNCE_MS] 뒤에 조회에 반영되므로, 입력창([FeedViewState.searchInput])과
 * 조회 조건([FeedQuery.searchQuery])이 잠시 어긋난다. 복원할 때는 입력창의 글자를 조회 조건으로도 쓴다 —
 * 사용자가 친 글자가 곧 의도였고, 죽지 않았다면 300ms 뒤에 그렇게 됐을 값이다.
 *
 * 저장 값은 [SavedStateHandle] 이 그대로 `Bundle` 로 나가므로 문자열·불린·정수·`LongArray`·[ArrayList] 만
 * 쓴다. 읽을 때는 계약(허용된 최소 점수, 아는 유형·정렬 이름, 만들 수 있는 마감일 범위)을 다시 통과시킨다 —
 * 낡거나 망가진 번들이 [FeedQuery] 의 `require` 를 깨뜨려 앱을 죽이지 않게.
 */
internal class FeedInputDraft(
    private val handle: SavedStateHandle,
) {
    /** 저장 대상만 뽑은 값 — 이것이 바뀔 때만 [save] 한다. */
    data class Input(
        val searchInput: String,
        val query: FeedQuery,
        val filterDraft: FeedFilterDraft?,
    )

    /** 초안을 담은 시작 상태. 목록은 비어 있고 `init` 의 조회가 이 조건으로 첫 페이지를 읽는다. */
    fun restoredState(): FeedViewState {
        val searchInput = handle.get<String>(KEY_SEARCH).orEmpty()
        return FeedViewState(searchInput = searchInput, query = restoredQuery(searchInput))
    }

    /**
     * 죽기 전 필터 시트가 열려 있었다면 그때 고르던 값, 아니면 null.
     *
     * 시트를 **닫는** 두 가지 이유(적용·취소)는 모두 초안을 버려야 하므로, [save] 가 시트가 닫힌 상태를 보면
     * 저장해 둔 초안도 함께 지운다. 그래서 이 값은 ViewModel 이 시작할 때 한 번 읽어 두고 써야 한다.
     */
    fun restoredFilterDraft(): FeedFilterDraft? {
        if (KEY_DRAFT_CATEGORY !in handle) return null
        return FeedFilterDraft(
            category = restoredEnum<FeedListingCategory>(KEY_DRAFT_CATEGORY) ?: FeedListingCategory.All,
            boardIds = handle.get<LongArray>(KEY_DRAFT_BOARD_IDS)?.toSet().orEmpty(),
            deadline = restoredEnum<UiDeadlineFilter>(KEY_DRAFT_DEADLINE) ?: UiDeadlineFilter.All,
            // 편집 중인 범위는 뒤집혀 있어도 그대로 되살린다 — 시트가 「적용」을 막고 이유를 보여 주는 값이다.
            deadlineRange = restoredRange(KEY_DRAFT_DEADLINE_START, KEY_DRAFT_DEADLINE_END),
            minScore = restoredMinScore(KEY_DRAFT_MIN_SCORE),
            unreadOnly = handle.get<Boolean>(KEY_DRAFT_UNREAD_ONLY) == true,
        )
    }

    fun save(input: Input) {
        saveSearch(input.searchInput)
        saveQuery(input.query)
        input.filterDraft?.let(::saveFilterDraft) ?: clearFilterDraft()
    }

    /**
     * 검색어 상한을 넘으면 저장을 건너뛴다.
     *
     * 앞부분만 남기는 쪽은 고르지 않았다 — 잘린 검색어는 **다른 조회**이고, 살아 돌아온 목록이 사용자가
     * 마지막으로 본 것과 다른데 그 사실이 화면에 드러나지 않는다. 지금은 「원래도 없던 것」으로 남는다.
     */
    private fun saveSearch(searchInput: String) {
        if (searchInput.length <= MAX_SEARCH_CHARS) {
            handle[KEY_SEARCH] = searchInput
        } else {
            handle.remove<String>(KEY_SEARCH)
        }
    }

    private fun saveQuery(query: FeedQuery) {
        handle[KEY_QUERY_TYPES] = ArrayList(query.types.map(PostingType::name))
        handle[KEY_QUERY_BOARD_IDS] = query.boardIds.toLongArray()
        handle[KEY_QUERY_SORT] = query.sort.name
        handle[KEY_QUERY_UNREAD_ONLY] = query.unreadOnly
        putIntOrRemove(KEY_QUERY_MIN_SCORE, query.minScore)
        // 마감일은 시트 선택지 이름으로 적는다 — 도메인 `sealed` 를 직접 적으면 두 곳에 이름 표가 생긴다.
        handle[KEY_QUERY_DEADLINE] = query.deadline.toUiDeadlineFilter().name
        val range = query.deadline.toUiDeadlineRange()
        putDateOrRemove(KEY_QUERY_DEADLINE_START, range.start)
        putDateOrRemove(KEY_QUERY_DEADLINE_END, range.end)
    }

    private fun saveFilterDraft(draft: FeedFilterDraft) {
        handle[KEY_DRAFT_CATEGORY] = draft.category.name
        handle[KEY_DRAFT_BOARD_IDS] = draft.boardIds.toLongArray()
        handle[KEY_DRAFT_DEADLINE] = draft.deadline.name
        handle[KEY_DRAFT_UNREAD_ONLY] = draft.unreadOnly
        putIntOrRemove(KEY_DRAFT_MIN_SCORE, draft.minScore)
        putDateOrRemove(KEY_DRAFT_DEADLINE_START, draft.deadlineRange.start)
        putDateOrRemove(KEY_DRAFT_DEADLINE_END, draft.deadlineRange.end)
        // 날짜 선택기가 열려 있었는지(`editing`)는 남기지 않는다 — 시트 자체를 다시 열지 않는 것과 같은 이유다.
    }

    private fun clearFilterDraft() {
        handle.remove<String>(KEY_DRAFT_CATEGORY)
        handle.remove<LongArray>(KEY_DRAFT_BOARD_IDS)
        handle.remove<String>(KEY_DRAFT_DEADLINE)
        handle.remove<Boolean>(KEY_DRAFT_UNREAD_ONLY)
        handle.remove<Int>(KEY_DRAFT_MIN_SCORE)
        handle.remove<Long>(KEY_DRAFT_DEADLINE_START)
        handle.remove<Long>(KEY_DRAFT_DEADLINE_END)
    }

    /** 아는 유형·정렬 이름만 살린다. 모르는 이름은 서버·앱 버전이 어긋난 흔적이므로 조용히 버린다. */
    private fun restoredQuery(searchInput: String): FeedQuery =
        FeedQuery(
            types =
                handle
                    .get<ArrayList<String>>(KEY_QUERY_TYPES)
                    .orEmpty()
                    .mapNotNull { name -> PostingType.entries.firstOrNull { it.name == name } }
                    .toSet(),
            boardIds = handle.get<LongArray>(KEY_QUERY_BOARD_IDS)?.toSet().orEmpty(),
            deadline = restoredQueryDeadline(),
            minScore = restoredMinScore(KEY_QUERY_MIN_SCORE),
            unreadOnly = handle.get<Boolean>(KEY_QUERY_UNREAD_ONLY) == true,
            sort = restoredEnum<PostingSort>(KEY_QUERY_SORT) ?: PostingSort.CollectedDesc,
            searchQuery = searchInput,
        )

    /**
     * 걸려 있던 마감일 조건. 만들 수 없는 범위(양 끝이 다 비었거나 뒤집힌)는 「전체」로 되돌린다 —
     * 도메인 값이 그 조합을 거절하므로([DomainDeadlineFilter.Range]), 그대로 넘기면 살아나는 순간 앱이 죽는다.
     */
    private fun restoredQueryDeadline(): DomainDeadlineFilter {
        val selection = restoredEnum<UiDeadlineFilter>(KEY_QUERY_DEADLINE) ?: return DomainDeadlineFilter.All
        val range = restoredRange(KEY_QUERY_DEADLINE_START, KEY_QUERY_DEADLINE_END)
        return selection.toDomainDeadlineFilter(range) ?: DomainDeadlineFilter.All
    }

    private fun restoredRange(
        startKey: String,
        endKey: String,
    ): FeedDeadlineRange = FeedDeadlineRange(start = restoredDate(startKey), end = restoredDate(endKey))

    /** 상한 밖의 숫자가 들어와도 날짜로 만들지 않는다 — 망가진 번들이 예외로 앱을 죽이지 않게. */
    private fun restoredDate(key: String): LocalDate? = handle.get<Long>(key)?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }

    /**
     * 선택지([FeedQuery.ALLOWED_MIN_SCORES]) 밖의 값은 「전체」로 본다.
     *
     * **여기가 이 화면에서 가장 깨지기 쉬운 자리다.** 선택지는 줄어들 수 있는데(이슈 #200 이 70 을 뺐다)
     * 저장된 번들은 그 전에 쓰였을 수 있다. 그대로 넘기면 [FeedQuery] 의 `require` 가 살아나는 프로세스를
     * 그 자리에서 죽인다 — 사용자에게는 「앱을 켜면 죽는다」로 보인다.
     *
     * 사라진 70 을 가까운 60 으로 접지 않는 이유 — 그건 사용자가 고르지 않은 조건을 앱이 대신 고르는 것이다.
     * 「전체」는 조건이 없어졌다는 사실 그대로이고, 시트를 열면 「전체」가 선택돼 있어 무엇이 걸려 있는지
     * 화면이 정직하게 말한다.
     */
    private fun restoredMinScore(key: String): Int? = handle.get<Int>(key)?.takeIf { it in FeedQuery.ALLOWED_MIN_SCORES }

    private inline fun <reified T : Enum<T>> restoredEnum(key: String): T? =
        handle.get<String>(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

    private fun putIntOrRemove(
        key: String,
        value: Int?,
    ) {
        if (value == null) handle.remove<Int>(key) else handle[key] = value
    }

    private fun putDateOrRemove(
        key: String,
        value: LocalDate?,
    ) {
        if (value == null) handle.remove<Long>(key) else handle[key] = value.toEpochDay()
    }

    private companion object {
        /**
         * 초안으로 남기는 검색어의 상한(문자).
         *
         * 검색은 공고 **제목**의 부분 문자열 조회다([FeedQuery.filterClientSide]) — 제목보다 긴 검색어는 어떤
         * 공고와도 맞지 않으므로 사람이 검색하려던 글자가 아니라 잘못 붙여 넣은 본문이다. 200자면 가장 긴
         * 공고 제목도 넉넉히 담고, 저장 상태 전체가 통과해야 하는 Binder 트랜잭션(약 1MB)에 부담이 없다.
         */
        const val MAX_SEARCH_CHARS = 200

        // 키는 저장 계약이다 — 바꾸면 그 순간 떠 있던 앱의 초안이 복원되지 않는다.
        const val KEY_SEARCH = "feed.draft.searchInput"
        const val KEY_QUERY_TYPES = "feed.draft.query.types"
        const val KEY_QUERY_BOARD_IDS = "feed.draft.query.boardIds"
        const val KEY_QUERY_DEADLINE = "feed.draft.query.deadline"
        const val KEY_QUERY_DEADLINE_START = "feed.draft.query.deadline.start"
        const val KEY_QUERY_DEADLINE_END = "feed.draft.query.deadline.end"
        const val KEY_QUERY_MIN_SCORE = "feed.draft.query.minScore"
        const val KEY_QUERY_UNREAD_ONLY = "feed.draft.query.unreadOnly"
        const val KEY_QUERY_SORT = "feed.draft.query.sort"
        const val KEY_DRAFT_CATEGORY = "feed.draft.filter.category"
        const val KEY_DRAFT_BOARD_IDS = "feed.draft.filter.boardIds"
        const val KEY_DRAFT_DEADLINE = "feed.draft.filter.deadline"
        const val KEY_DRAFT_DEADLINE_START = "feed.draft.filter.deadline.start"
        const val KEY_DRAFT_DEADLINE_END = "feed.draft.filter.deadline.end"
        const val KEY_DRAFT_MIN_SCORE = "feed.draft.filter.minScore"
        const val KEY_DRAFT_UNREAD_ONLY = "feed.draft.filter.unreadOnly"
    }
}
