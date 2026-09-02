package com.cambridge.feature.feed.domain.testing

import com.cambridge.feature.feed.domain.model.FeedSnapshot
import com.cambridge.feature.feed.domain.repository.FeedSnapshotRepository
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [FeedSnapshotRepository] fake 정본 — 메모리에 스냅샷 하나를 든다.
 *
 * `onX` 훅이 있으면 기본 동작 대신 그 결과를 돌려준다(실패·지연 시나리오). 호출 기록은 [saved]·[loadCount]·
 * [clearCount] 로 검증한다.
 */
public class FakeFeedSnapshotRepository(
    initial: FeedSnapshot? = null,
    public var onSave: (suspend (FeedSnapshot) -> Result<Unit>)? = null,
    public var onLoad: (suspend () -> Result<FeedSnapshot?>)? = null,
    public var onClear: (suspend () -> Result<Unit>)? = null,
) : FeedSnapshotRepository {
    public var snapshot: FeedSnapshot? = initial

    /** [save] 로 들어온 스냅샷 순서대로. 훅이 가로챈 호출도 기록한다. */
    public val saved: CopyOnWriteArrayList<FeedSnapshot> = CopyOnWriteArrayList()
    public var loadCount: Int = 0
        private set
    public var clearCount: Int = 0
        private set

    override suspend fun save(snapshot: FeedSnapshot): Result<Unit> {
        saved += snapshot
        onSave?.let { return it(snapshot) }
        this.snapshot = snapshot
        return Result.success(Unit)
    }

    override suspend fun load(): Result<FeedSnapshot?> {
        loadCount += 1
        onLoad?.let { return it() }
        return Result.success(snapshot)
    }

    override suspend fun clear(): Result<Unit> {
        clearCount += 1
        onClear?.let { return it() }
        snapshot = null
        return Result.success(Unit)
    }
}
