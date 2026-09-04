package com.careercompass.feature.feed.domain.repository

import com.careercompass.feature.feed.domain.model.FeedSnapshot

/**
 * 피드 스냅샷([FeedSnapshot])의 로컬 저장 계약 — 네트워크가 끊겼을 때 「오프라인 모드로 보기」가 읽는 목록.
 *
 * 세션에 귀속된다 — 로그아웃하면 함께 비워진다(다른 계정의 공고가 남아 보이면 안 된다). 저장 형식이 바뀌어
 * 읽을 수 없는 기록은 「스냅샷 없음」으로 본다 — 오래된 목록보다 빈 오류 화면이 낫다.
 */
public interface FeedSnapshotRepository {
    /** 마지막 스냅샷을 덮어쓴다. */
    public suspend fun save(snapshot: FeedSnapshot): Result<Unit>

    /** 저장된 스냅샷. 없거나 읽을 수 없으면 `null`. */
    public suspend fun load(): Result<FeedSnapshot?>

    /** 스냅샷을 지운다. */
    public suspend fun clear(): Result<Unit>
}
