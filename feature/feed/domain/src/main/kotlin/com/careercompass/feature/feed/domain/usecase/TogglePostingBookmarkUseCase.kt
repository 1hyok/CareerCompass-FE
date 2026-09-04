package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.repository.PostingRepository
import javax.inject.Inject

/** 북마크를 뒤집는다 — `POST`/`DELETE /postings/{id}/bookmark`. 성공 시 새 북마크 값을 돌려준다. */
public class TogglePostingBookmarkUseCase
    @Inject
    constructor(
        private val postingRepository: PostingRepository,
    ) {
        public suspend operator fun invoke(
            id: Long,
            currentlyBookmarked: Boolean,
        ): Result<Boolean> {
            val next = !currentlyBookmarked
            return postingRepository.setBookmarked(id, next).map { next }
        }
    }
