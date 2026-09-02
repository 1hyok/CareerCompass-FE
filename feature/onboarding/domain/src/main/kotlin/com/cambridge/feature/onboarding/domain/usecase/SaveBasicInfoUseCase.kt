package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.model.user.UserProfileUpdate
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.repository.OnboardingProgressRepository
import javax.inject.Inject

/**
 * Step 1 기본 정보를 서버에 저장하고 진행 상태를 Step 2 로 옮긴다 — 기능 스펙 F1-2 Step 1.
 *
 * 입력 검증(글자 수·범위)은 화면이 끝낸 뒤 호출한다. 여기서는 모델 불변식(빈 값·학점 범위·졸업 연도 하한)만
 * `require` 로 다시 확인한다.
 */
public class SaveBasicInfoUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
        private val progressRepository: OnboardingProgressRepository,
    ) {
        public suspend operator fun invoke(
            name: String,
            school: String,
            department: String,
            gpa: Double?,
            gradYear: Int?,
        ): Result<Unit> {
            require(name.isNotBlank()) { "name must not be blank" }
            require(school.isNotBlank()) { "school must not be blank" }
            require(department.isNotBlank()) { "department must not be blank" }
            val update =
                UserProfileUpdate(
                    name = name.trim(),
                    school = school.trim(),
                    department = department.trim(),
                    gpa = gpa,
                    gradYear = gradYear,
                )
            userProfileRepository.updateProfile(update).getOrElse { return Result.failure(it) }
            return progressRepository.save(OnboardingStep.JobPreference)
        }
    }
