package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.PostingRepository
import com.cambridge.core.model.posting.PostingDetail
import javax.inject.Inject

/**
 * 공고 상세를 열고 읽음 처리한다 — 기능 스펙 F2-3 「읽음: 상세 페이지 1회 이상 진입」.
 *
 * 읽음 처리는 best-effort 다. 실패해도 상세는 성공으로 돌려주고, 이미 읽은 공고는 요청하지 않는다.
 * 돌려주는 상세의 `isRead` 는 서버에 반영된 상태를 따른다(읽음 처리 성공 시에만 `true`).
 */
public class OpenPostingDetailUseCase
    @Inject
    constructor(
        private val postingRepository: PostingRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<PostingDetail> {
            val detail = postingRepository.getPostingDetail(id).getOrElse { return Result.failure(it) }
            if (detail.isRead) return Result.success(detail)
            val marked = postingRepository.markRead(id).isSuccess
            return Result.success(if (marked) detail.copy(isRead = true) else detail)
        }
    }
