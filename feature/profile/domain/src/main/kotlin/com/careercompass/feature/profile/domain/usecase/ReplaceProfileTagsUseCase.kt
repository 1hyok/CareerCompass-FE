package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.model.user.MAX_PROFILE_TAGS
import javax.inject.Inject

/**
 * 관심 분야 태그를 통째로 바꾼다 — `PUT /users/me/tags` (전체 교체).
 *
 * 자유 입력이라 앞뒤 공백이 섞여 들어온다. 저장 전에 다듬어 「AI」와 「AI 」가 서로 다른 태그로 남지
 * 않게 한다. 개수·중복 확인은 다듬은 뒤의 값으로 한다.
 */
public class ReplaceProfileTagsUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
    ) {
        public suspend operator fun invoke(tags: List<String>): Result<Unit> {
            val trimmed = tags.map(String::trim)
            require(trimmed.size in 1..MAX_PROFILE_TAGS) { "tags must be 1..$MAX_PROFILE_TAGS" }
            require(trimmed.all(String::isNotBlank)) { "tags must not be blank" }
            require(trimmed.distinct().size == trimmed.size) { "tags must be unique" }

            return userProfileRepository.replaceTags(trimmed)
        }
    }
