package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.model.user.UserProfile
import javax.inject.Inject

/**
 * 내 프로필을 서버에서 다시 읽는다 — `GET /users/me`.
 *
 * 성공하면 [ObserveMyProfileUseCase] 가 내보내는 값도 함께 갱신된다(리포지토리 계약). 실패는
 * [CoreDataFailure][com.careercompass.core.domain.error.CoreDataFailure] 그대로 흘려보낸다 — 캐시로
 * 이미 그려 둔 화면을 어떻게 할지는 화면이 정한다.
 */
public class RefreshMyProfileUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
    ) {
        public suspend operator fun invoke(): Result<UserProfile> = userProfileRepository.refreshProfile()
    }
