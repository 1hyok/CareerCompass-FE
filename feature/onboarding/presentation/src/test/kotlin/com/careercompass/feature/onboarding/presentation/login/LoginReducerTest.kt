package com.careercompass.feature.onboarding.presentation.login

import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.domain.usecase.auth.SocialLoginUseCase
import com.careercompass.feature.onboarding.presentation.reporting.RecordingErrorReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 로그인의 **순수 전이** (#245).
 *
 * 코루틴 하네스가 없다 — 여기서 보는 Intent 는 저장소를 부르지 않고 `dispatch` → `reduce` 만 지난다.
 * SDK·서버를 타는 비동기 갈래는 [LoginViewModelTest] 가 본다.
 */
class LoginReducerTest {
    private val authRepository = FakeAuthRepository()
    private val reporter = RecordingErrorReporter()

    private fun viewModel() = LoginViewModel(SocialLoginUseCase(authRepository), reporter)

    @Test
    fun `소비 Intent 는 그 신호만 되돌린다`() {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.ConsumeFailure)
        viewModel.onIntent(LoginIntent.ConsumeNavigation)

        val state = viewModel.uiState.value
        assertNull(state.failure)
        assertNull(state.pendingNavigation)
        assertFalse(state.isLoading)
        assertTrue(authRepository.socialLoginCalls.isEmpty())
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `진행 중인 시도가 없으면 호스트 이탈은 아무것도 바꾸지 않는다`() {
        val viewModel = viewModel()
        val before = viewModel.uiState.value

        viewModel.onIntent(LoginIntent.DetachLoginHost)

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `같은 상태와 같은 입력은 같은 결과다`() {
        val first = viewModel()
        val second = viewModel()

        first.onIntent(LoginIntent.ConsumeFailure)
        second.onIntent(LoginIntent.ConsumeFailure)

        assertEquals(first.uiState.value, second.uiState.value)
        assertEquals(LoginUiState(), first.uiState.value)
    }
}
