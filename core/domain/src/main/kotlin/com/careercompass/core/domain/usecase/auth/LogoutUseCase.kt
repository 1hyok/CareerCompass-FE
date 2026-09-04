package com.careercompass.core.domain.usecase.auth

import com.careercompass.core.domain.repository.AuthRepository
import javax.inject.Inject

/** 서버 로그아웃(best-effort) 후 로컬 세션을 정리한다. 네트워크가 실패해도 사용자는 로그아웃 상태로 간다. */
public class LogoutUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        public suspend operator fun invoke(): Result<Unit> = authRepository.logout()
    }
