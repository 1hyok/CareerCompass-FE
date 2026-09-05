package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.PastApplicationRepository
import com.careercompass.core.model.application.MAX_PAST_APPLICATIONS
import com.careercompass.core.model.application.PastApplication
import com.careercompass.core.model.application.UploadFile
import com.careercompass.feature.profile.domain.error.ProfileFailure
import javax.inject.Inject

/**
 * 과거 지원서를 올린다 — `POST /past-applications/upload` (multipart). 텍스트 추출·AI 분류까지 끝난
 * 결과가 돌아온다.
 *
 * 보내기 전에 목록을 세어 상한([MAX_PAST_APPLICATIONS], F1-4)에 닿았으면 요청 없이
 * [ProfileFailure.PastApplicationLimitReached] 로 실패한다. 여기서 상한을 먼저 보는 값어치는 경험
 * 카드보다 크다 — 파일은 최대 10MB 라, 서버가 거절할 요청에 사용자의 데이터와 대기 시간을 통째로
 * 쓰게 된다.
 *
 * 파일 형식(PDF/DOCX/TXT)과 크기 상한은 [UploadFile] 이 만들어질 때 이미 확정된다. 재시도는 같은
 * [UploadFile] 로 다시 호출한다 — [UploadFile.openStream] 이 호출마다 새 스트림을 연다.
 */
public class UploadPastApplicationUseCase
    @Inject
    constructor(
        private val pastApplicationRepository: PastApplicationRepository,
    ) {
        public suspend operator fun invoke(
            file: UploadFile,
            label: String,
        ): Result<PastApplication> {
            require(label.isNotBlank()) { "label must not be blank" }
            val applications =
                pastApplicationRepository
                    .getPastApplications()
                    .getOrElse { return Result.failure(it) }
            if (applications.size >= MAX_PAST_APPLICATIONS) {
                return Result.failure(ProfileFailure.PastApplicationLimitReached(MAX_PAST_APPLICATIONS))
            }
            return pastApplicationRepository.upload(file = file, label = label.trim())
        }
    }
