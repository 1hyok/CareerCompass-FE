package com.cambridge.core.network.di

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.network.interceptor.AuthInterceptor
import com.cambridge.core.network.interceptor.TokenAuthenticator
import com.cambridge.core.network.token.AccessTokenExpiryTracker
import com.cambridge.core.network.token.TokenReissuer
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class NetworkModuleTest {
    private object NoopReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private val base = NetworkModule.provideBaseOkHttpClient()
    private val logging = NetworkModule.provideLoggingInterceptor()

    @Test
    fun `재발급 클라이언트는 Dispatcher 를 분리하고 ConnectionPool 은 공유한다`() {
        val refresh = NetworkModule.provideRefreshOkHttpClient(base, logging)

        assertNotSame(base.dispatcher, refresh.dispatcher)
        assertSame(base.connectionPool, refresh.connectionPool)
    }

    @Test
    fun `메인 클라이언트는 뿌리의 Dispatcher 를 그대로 쓴다`() {
        val lazy = dagger.Lazy<AuthRepository> { FakeAuthRepository() }
        val tracker = AccessTokenExpiryTracker { 0L }
        val reissuer = TokenReissuer(lazy, tracker, NoopReporter)
        val main =
            NetworkModule.provideMainOkHttpClient(
                baseClient = base,
                loggingInterceptor = logging,
                authInterceptor = AuthInterceptor(lazy, tracker, reissuer),
                tokenAuthenticator = TokenAuthenticator(lazy, reissuer, NoopReporter),
            )

        assertSame(base.dispatcher, main.dispatcher)
    }
}
