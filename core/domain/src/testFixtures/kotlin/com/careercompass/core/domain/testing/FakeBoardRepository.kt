package com.careercompass.core.domain.testing

import com.careercompass.core.domain.repository.BoardRepository
import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardDetection
import com.careercompass.core.model.board.BoardDetectionStatus
import com.careercompass.core.model.board.BoardPreviewItem
import com.careercompass.core.model.board.BoardRegistration
import com.careercompass.core.model.board.BoardStatus
import com.careercompass.core.model.board.BoardUpdate
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** [BoardRepository] fake 정본. 기본 감지는 성공 미리보기 1건을 돌려준다. */
public class FakeBoardRepository(
    initial: List<Board> = emptyList(),
    public var detection: BoardDetection =
        BoardDetection(
            status = BoardDetectionStatus.Success,
            preview = listOf(BoardPreviewItem(title = "미리보기 게시글", url = "https://example.com/1", date = null)),
            hasDateSelector = true,
        ),
    public var onDetect: (suspend (String) -> Result<BoardDetection>)? = null,
    public var onRegister: (suspend (BoardRegistration) -> Result<Board>)? = null,
    public var onGetBoards: (suspend () -> Result<List<Board>>)? = null,
    public var onUpdate: (suspend (Long, BoardUpdate) -> Result<Board>)? = null,
    public var onDelete: (suspend (Long) -> Result<Unit>)? = null,
    public var onRetry: (suspend (Long) -> Result<Unit>)? = null,
) : BoardRepository {
    public val boards: CopyOnWriteArrayList<Board> = CopyOnWriteArrayList(initial)
    public val detectedUrls: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()
    public val registrations: CopyOnWriteArrayList<BoardRegistration> = CopyOnWriteArrayList()
    public val updates: CopyOnWriteArrayList<Pair<Long, BoardUpdate>> = CopyOnWriteArrayList()
    public val retries: CopyOnWriteArrayList<Long> = CopyOnWriteArrayList()
    private val nextId = AtomicLong((initial.maxOfOrNull { it.id } ?: 0L) + 1)

    override suspend fun detect(url: String): Result<BoardDetection> {
        detectedUrls += url
        onDetect?.let { return it(url) }
        return Result.success(detection)
    }

    override suspend fun register(registration: BoardRegistration): Result<Board> {
        registrations += registration
        onRegister?.let { return it(registration) }
        val board =
            Board(
                id = nextId.getAndIncrement(),
                url = registration.url,
                name = registration.name,
                type = registration.type,
                cycleHours = registration.cycleHours,
                isActive = true,
                status = BoardStatus.Active,
                failCount = 0,
                lastCollectedAt = null,
            )
        boards += board
        return Result.success(board)
    }

    override suspend fun getBoards(): Result<List<Board>> {
        onGetBoards?.let { return it() }
        return Result.success(boards.toList())
    }

    override suspend fun update(
        id: Long,
        update: BoardUpdate,
    ): Result<Board> {
        updates += id to update
        onUpdate?.let { return it(id, update) }
        val index = boards.indexOfFirst { it.id == id }
        if (index < 0) return Result.failure(NoSuchElementException("board $id"))
        val current = boards[index]
        val updated =
            current.copy(
                name = update.name ?: current.name,
                type = update.type ?: current.type,
                cycleHours = update.cycleHours ?: current.cycleHours,
                isActive = update.isActive ?: current.isActive,
            )
        boards[index] = updated
        return Result.success(updated)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        onDelete?.let { return it(id) }
        return if (boards.removeIf { it.id == id }) Result.success(Unit) else Result.failure(NoSuchElementException("board $id"))
    }

    override suspend fun retry(id: Long): Result<Unit> {
        retries += id
        onRetry?.let { return it(id) }
        val index = boards.indexOfFirst { it.id == id }
        if (index < 0) return Result.failure(NoSuchElementException("board $id"))
        boards[index] = boards[index].copy(status = BoardStatus.Active, failCount = 0)
        return Result.success(Unit)
    }

    public companion object {
        public fun strict(): FakeBoardRepository =
            FakeBoardRepository(
                onDetect = { unexpectedCall("BoardRepository.detect") },
                onRegister = { unexpectedCall("BoardRepository.register") },
                onGetBoards = { unexpectedCall("BoardRepository.getBoards") },
                onUpdate = { _, _ -> unexpectedCall("BoardRepository.update") },
                onDelete = { unexpectedCall("BoardRepository.delete") },
                onRetry = { unexpectedCall("BoardRepository.retry") },
            )
    }
}
