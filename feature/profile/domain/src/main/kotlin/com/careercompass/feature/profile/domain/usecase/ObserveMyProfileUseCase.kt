package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.model.user.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 마지막으로 받아 둔 내 프로필을 흘려보낸다 — API_SPEC v0.1 §2.
 *
 * 마이 홈은 이 값으로 **먼저 그리고** [RefreshMyProfileUseCase] 로 갱신한다. 서버 왕복을 기다렸다
 * 그리면 탭을 열 때마다 빈 화면이 한 번 스친다. 아직 한 번도 받은 적 없으면 `null` 이다.
 */
public class ObserveMyProfileUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
    ) {
        public operator fun invoke(): Flow<UserProfile?> = userProfileRepository.profile
    }
