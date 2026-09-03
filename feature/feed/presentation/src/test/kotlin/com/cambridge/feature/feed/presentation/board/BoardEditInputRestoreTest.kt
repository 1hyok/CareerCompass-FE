package com.cambridge.feature.feed.presentation.board

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.cambridge.core.domain.testing.FakeBoardRepository
import com.cambridge.core.model.board.Board
import com.cambridge.core.model.board.BoardUpdate
import com.cambridge.feature.feed.domain.usecase.DeleteBoardUseCase
import com.cambridge.feature.feed.domain.usecase.GetBoardsUseCase
import com.cambridge.feature.feed.domain.usecase.RetryBoardUseCase
import com.cambridge.feature.feed.domain.usecase.ToggleBoardActiveUseCase
import com.cambridge.feature.feed.domain.usecase.UpdateBoardUseCase
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.board
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.cambridge.core.model.board.BoardType as DomainBoardType

/**
 * 프로세스 사망 뒤 게시판 **수정** 시트의 입력이 살아 돌아오는지 — 이슈 #156.
 *
 * ### 왜 재구성이 아니라 이 방식인가
 * Robolectric 의 화면 재구성(회전·테마 변경)으로는 이 결함이 잡히지 않는다. 재구성은 ViewModel 을 **살려 두므로**
 * 저장이 하나도 없어도 상태가 그대로 남는다. 여기서는 진짜 경로를 태운다 — [SavedStateHandle] 을 `Bundle` 로
 * 저장하고, 그 번들을 [Parcel] 에 마샬링했다가 되읽어(안드로이드가 프로세스 경계를 넘길 때 하는 그대로) 새
 * ViewModel 을 세운다(#137 의 `FeedInputRestoreTest` 와 같은 방식이다).
 *
 * ### 여기서 지키는 것은 「복원된다」가 아니라 「서버를 밀어내지 않는다」
 * 수정 시트는 서버에 이미 있는 게시판을 고치는 자리다. 그래서 이 파일의 절반은 **복원되지 않아야 할 것**을
 * 못 박는다 — 손대지 않은 필드는 그 사이 바뀐 서버 값으로 서고, 살아난 초안으로 저장해도 `PATCH` 에는 사용자가
 * 실제로 바꾼 필드만 실린다. 무엇을 왜 그렇게 정했는지는 [BoardEditInputDraft] 의 KDoc 에 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoardEditInputRestoreTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    @Test
    fun `수정 시트에서 고치던 값은 프로세스가 죽어도 남고 다시 열면 그 자리에 있다`() {
        val handle = SavedStateHandle()
        val repository = repository()
        val before = viewModel(repository, handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))
        before.onEditEvent(BoardEditEvent.TypeSelected(BoardType.Contest))
        before.onEditEvent(BoardEditEvent.CycleSelected(BoardCollectCycle.Weekly))

        val after = viewModel(repository, handle.acrossProcessDeath())

        // 프로세스 사망은 사용자가 의도한 이동이 아니다 — 돌아온 화면에 시트가 떠 있지 않다.
        assertNull(after.state.value.editDraft)

        after.onEvent(BoardListEvent.BoardSelected("1"))

        val draft = checkNotNull(after.state.value.editDraft)
        assertEquals("건국대 장학 공지", draft.name)
        assertEquals(BoardType.Contest, draft.type)
        assertEquals(BoardCollectCycle.Weekly, draft.cycle)
        assertFalse(draft.isSaving)
    }

    /** 핵심 판정 — 초안은 「폼의 값」이 아니라 「바꾼 필드」다. 나머지 자리는 서버가 이긴다. */
    @Test
    fun `건드리지 않은 필드는 그 사이 바뀐 서버 값으로 선다`() {
        val handle = SavedStateHandle()
        val repository = repository()
        val before = viewModel(repository, handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))

        // 죽어 있는 사이 서버에서 이름·유형·주기가 모두 바뀌었다.
        repository.boards[0] = board(id = 1, type = DomainBoardType.Recruit, cycleHours = 12).copy(name = "서버가 바꾼 이름")
        val after = viewModel(repository, handle.acrossProcessDeath())
        after.onEvent(BoardListEvent.BoardSelected("1"))

        val draft = checkNotNull(after.state.value.editDraft)
        // 사용자가 실제로 바꾼 이름만 초안에서 오고,
        assertEquals("건국대 장학 공지", draft.name)
        // 손대지 않은 유형·주기는 낡은 초안이 아니라 새 서버 값이다.
        assertEquals(BoardType.Employment, draft.type)
        assertEquals(BoardCollectCycle.TwiceDaily, draft.cycle)
        // 「바뀐 필드」 판정의 기준도 낡은 스냅샷이 아니라 새 서버 값이다.
        assertEquals("서버가 바꾼 이름", draft.board.name)
    }

    /** 위 판정이 요청에서도 지켜지는가 — 살아난 초안이 서버 값을 되돌려 놓지 않는다. */
    @Test
    fun `살아난 초안으로 저장해도 바꾼 필드만 실려 나간다`() {
        val handle = SavedStateHandle()
        val repository = repository()
        val before = viewModel(repository, handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))

        repository.boards[0] = board(id = 1, type = DomainBoardType.Recruit, cycleHours = 12).copy(name = "서버가 바꾼 이름")
        val after = viewModel(repository, handle.acrossProcessDeath())
        after.onEvent(BoardListEvent.BoardSelected("1"))
        after.onEditEvent(BoardEditEvent.SaveClicked)

        assertEquals(listOf(1L to BoardUpdate(name = "건국대 장학 공지")), repository.updates.toList())
        // 유형·주기는 요청에 없었으므로 서버가 그 사이 바꾼 값이 그대로 남는다.
        assertEquals(DomainBoardType.Recruit, repository.boards[0].type)
        assertEquals(12, repository.boards[0].cycleHours)
    }

    @Test
    fun `다른 게시판을 열면 남의 초안이 따라붙지 않는다`() {
        val handle = SavedStateHandle()
        val repository = repository()
        val before = viewModel(repository, handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))
        before.onEditEvent(BoardEditEvent.CycleSelected(BoardCollectCycle.Weekly))

        val after = viewModel(repository, handle.acrossProcessDeath())
        after.onEvent(BoardListEvent.BoardSelected("2"))

        val draft = checkNotNull(after.state.value.editDraft)
        assertEquals("게시판 2", draft.name)
        assertEquals(BoardCollectCycle.Daily, draft.cycle)
    }

    /**
     * 상한을 넘긴 이름은 초안을 남기지 않는다 — 잘린 이름은 다른 이름이고, 되살아난 그 값으로 저장하면
     * 사용자가 모르는 사이 게시판 이름이 반토막 난다. 같은 시트에서 고친 유형은 그대로 살아 온다.
     */
    @Test
    fun `한도를 넘게 붙여 넣은 이름은 초안을 남기지 않는다`() {
        val handle = SavedStateHandle()
        val repository = repository()
        val before = viewModel(repository, handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged(OVERSIZED_NAME))
        before.onEditEvent(BoardEditEvent.TypeSelected(BoardType.Contest))

        val after = viewModel(repository, handle.acrossProcessDeath())
        after.onEvent(BoardListEvent.BoardSelected("1"))

        val draft = checkNotNull(after.state.value.editDraft)
        assertEquals("게시판 1", draft.name)
        assertEquals(BoardType.Contest, draft.type)
    }

    @Test
    fun `시트를 닫으면 초안도 함께 버린다`() {
        val handle = SavedStateHandle()
        val repository = repository()
        val before = viewModel(repository, handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))
        before.onEditEvent(BoardEditEvent.DismissClicked)

        val restored = handle.acrossProcessDeath()

        assertNull(BoardEditInputDraft(restored).restoredEdit())
        val after = viewModel(repository, restored)
        after.onEvent(BoardListEvent.BoardSelected("1"))
        assertEquals("게시판 1", checkNotNull(after.state.value.editDraft).name)
    }

    @Test
    fun `저장에 성공하면 초안이 남지 않는다`() {
        val handle = SavedStateHandle()
        val before = viewModel(repository(), handle)
        before.onEvent(BoardListEvent.BoardSelected("1"))
        before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))
        before.onEditEvent(BoardEditEvent.SaveClicked)

        assertNull(BoardEditInputDraft(handle.acrossProcessDeath()).restoredEdit())
    }

    /** 살아난 프로세스에는 그 요청이 없다 — 되살리면 영원히 도는 저장 버튼이 된다. */
    @Test
    fun `저장 중에 죽어도 되살아난 시트는 저장 중이 아니다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = repository().apply { onUpdate = { id, update -> gate.await().let { Result.success(updated(id, update)) } } }
            val handle = SavedStateHandle()
            val before = viewModel(repository, handle)
            before.onEvent(BoardListEvent.BoardSelected("1"))
            before.onEditEvent(BoardEditEvent.NameChanged("건국대 장학 공지"))
            before.onEditEvent(BoardEditEvent.SaveClicked)
            check(checkNotNull(before.state.value.editDraft).isSaving)

            val after = viewModel(repository, handle.acrossProcessDeath())
            after.onEvent(BoardListEvent.BoardSelected("1"))

            val draft = checkNotNull(after.state.value.editDraft)
            assertEquals("건국대 장학 공지", draft.name)
            assertFalse(draft.isSaving)
            gate.complete(Unit)
        }

    /** 낡거나 망가진 번들이 화면 계약을 깨뜨리거나 엉뚱한 값을 세우지 않는다. */
    @Test
    fun `모르는 이름이 든 초안은 버린다`() {
        val handle =
            SavedStateHandle(
                mapOf(
                    "board.edit.draft.boardId" to 1L,
                    "board.edit.draft.name" to "",
                    "board.edit.draft.type" to "사라진유형",
                    "board.edit.draft.cycle" to "사라진주기",
                ),
            )
        val restored = handle.acrossProcessDeath()

        assertNull(BoardEditInputDraft(restored).restoredEdit())
        val viewModel = viewModel(repository(), restored)
        viewModel.onEvent(BoardListEvent.BoardSelected("1"))

        val draft = checkNotNull(viewModel.state.value.editDraft)
        assertEquals("게시판 1", draft.name)
        assertEquals(BoardType.Scholarship, draft.type)
        assertEquals(BoardCollectCycle.Daily, draft.cycle)
    }

    // ---- 조립 ----

    /**
     * 안드로이드가 프로세스를 죽였다 되살릴 때 하는 그대로 — 핸들을 `Bundle` 로 저장하고 [Parcel] 에 마샬링했다가
     * 되읽는다. 번들에 담기지 못하는 값을 저장하면 여기서 드러난다.
     *
     * `FeedInputRestoreTest` 에도 같은 헬퍼가 있다. 옮겨 쓰지 않고 여기 두는 이유는 그 파일이 피드 쪽 이슈와
     * 함께 움직이기 때문이다 — 게시판 초안 테스트가 남의 작업에 묶이지 않게 한다.
     */
    private fun SavedStateHandle.acrossProcessDeath(): SavedStateHandle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedStateProvider().saveState())
            parcel.setDataPosition(0)
            SavedStateHandle.createHandle(parcel.readBundle(javaClass.classLoader), null)
        } finally {
            parcel.recycle()
        }
    }

    private fun repository() = FakeBoardRepository(initial = listOf(board(id = 1), board(id = 2)))

    private fun FakeBoardRepository.updated(
        id: Long,
        update: BoardUpdate,
    ): Board {
        val current = boards.first { it.id == id }
        return current.copy(
            name = update.name ?: current.name,
            type = update.type ?: current.type,
            cycleHours = update.cycleHours ?: current.cycleHours,
        )
    }

    private fun viewModel(
        repository: FakeBoardRepository,
        savedStateHandle: SavedStateHandle,
    ): BoardListViewModel =
        BoardListViewModel(
            getBoards = GetBoardsUseCase(repository),
            toggleBoardActive = ToggleBoardActiveUseCase(repository),
            retryBoard = RetryBoardUseCase(repository),
            deleteBoard = DeleteBoardUseCase(repository),
            updateBoard = UpdateBoardUseCase(repository),
            errorReporter = reporter,
            clock = FIXED_CLOCK,
            savedStateHandle = savedStateHandle,
        )

    private companion object {
        /** 초안 상한(200자)을 한 글자 넘긴 이름 — 사람이 지은 게시판 이름이 아니라 잘못 붙여 넣은 글이다. */
        val OVERSIZED_NAME = "가".repeat(201)
    }
}
