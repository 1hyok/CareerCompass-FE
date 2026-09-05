package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.model.user.UserProfile
import com.careercompass.core.model.user.UserProfileUpdate
import javax.inject.Inject

/**
 * 기본 정보(이름·학교·학과·학점·졸업 연도)를 고친다 — `PATCH /users/me`.
 *
 * 온보딩 Step 1 은 다섯 칸을 한꺼번에 받지만(`SaveBasicInfoUseCase`) 마이 탭 편집은 **부분 수정**이다.
 * 그래서 값 다섯 개가 아니라 [UserProfileUpdate] 를 그대로 받는다 — 「고치지 않은 칸」과 「비운 칸」이
 * 같은 `null` 로 뭉개지지 않게 하는 것은 그 타입의 일이고, 여기서 값 목록으로 풀면 두 벌이 된다.
 *
 * 빈 수정을 막지 않는다 — 리포지토리가 요청 없이 현재 프로필을 돌려준다. 저장 버튼이 눌린 이상 화면은
 * 어느 쪽이든 같은 결과(최신 프로필)를 받아야 한다.
 */
public class UpdateBasicInfoUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
    ) {
        public suspend operator fun invoke(update: UserProfileUpdate): Result<UserProfile> = userProfileRepository.updateProfile(update)
    }
