package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.model.user.JobInterest
import com.careercompass.core.model.user.MAX_JOB_INTERESTS
import javax.inject.Inject

/**
 * 희망 직무를 통째로 바꾼다 — `PUT /users/me/job-interests` (전체 교체).
 *
 * [jobCodes] 의 **순서가 곧 우선순위**다(첫 번째가 `priority = 1`). 화면이 고른 순서를 그대로 넘기면
 * 되고, 우선순위 숫자를 화면이 만들지 않는다 — 두 곳에서 번호를 매기면 반드시 어긋난다.
 *
 * 개수·중복은 화면이 이미 막은 뒤라 여기서는 `require` 로 다시 확인만 한다. 사용자에게 보일 실패가
 * 아니라 배선 결함이다.
 */
public class ReplaceJobInterestsUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
    ) {
        public suspend operator fun invoke(jobCodes: List<String>): Result<Unit> {
            require(jobCodes.size in 1..MAX_JOB_INTERESTS) { "jobCodes must be 1..$MAX_JOB_INTERESTS" }
            require(jobCodes.all(String::isNotBlank)) { "jobCodes must not be blank" }
            require(jobCodes.distinct().size == jobCodes.size) { "jobCodes must be unique" }

            val interests = jobCodes.mapIndexed { index, code -> JobInterest(code = code, priority = index + 1) }
            return userProfileRepository.replaceJobInterests(interests)
        }
    }
