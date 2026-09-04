package com.careercompass.core.network.di

import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.model.auth.TokenBundle
import com.careercompass.core.network.interceptor.AuthInterceptor
import com.careercompass.core.network.interceptor.TokenAuthenticator
import com.careercompass.core.network.support.LocalHttpServer
import com.careercompass.core.network.token.AccessTokenExpiryTracker
import com.careercompass.core.network.token.TokenReissuer
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 재발급 클라이언트가 메인 클라이언트와 OkHttp Dispatcher 를 공유하면, 같은 호스트의 비동기 호출이 호스트 동시
 * 한도(5)만큼 재발급을 기다리는 동안 재발급 호출이 대기열에 갇혀 영구 교착한다. 한도보다 많은 호출을 동시에
 * 넣어 전부 끝나는지, 그리고 재발급이 정확히 한 번만 나가는지(single-flight) 실제 소켓 경계에서 확인한다.
 *
 * 호스트 한도만큼의 호출이 인증 인터셉터에 **동시에** 들어가도록 앞단 게이트로 모아 둔다 — 그렇지 않으면 첫
 * 스레드가 포화 전에 재발급을 끝내 교착 조건이 우연히 비켜 간다. 교착이 재발하면 완료 대기 시한에 걸려 실패한다.
 */
class RefreshClientDispatcherIsolationTest {
    private object NoopReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private val server =
        LocalHttpServer { recorded ->
            when (recorded.path) {
                REFRESH_PATH -> {
                    LocalHttpServer.Reply(200, """{"ok":true}""")
                }

                else -> {
                    if (recorded.authorization == "Bearer $NEW_TOKEN") {
                        LocalHttpServer.Reply(200, """{"ok":true}""")
                    } else {
                        LocalHttpServer.Reply(401, """{"ok":false}""")
                    }
                }
            }
        }

    private val hostLimit = NetworkModule.provideBaseOkHttpClient().dispatcher.maxRequestsPerHost
    private val gate = CountDownLatch(hostLimit)
    private val base =
        NetworkModule
            .provideBaseOkHttpClient()
            .newBuilder()
            .addInterceptor(
                Interceptor { chain ->
                    gate.countDown()
                    gate.await(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    chain.proceed(chain.request())
                },
            ).build()
    private val logging = NetworkModule.provideLoggingInterceptor()
    private val refreshClient = NetworkModule.provideRefreshOkHttpClient(base, logging)
    private val tracker = AccessTokenExpiryTracker { 0L }
    private val repository =
        FakeAuthRepository(accessToken = OLD_TOKEN, refreshToken = "refresh").apply {
            onRotateToken = {
                // Retrofit suspend 호출과 같은 비동기(enqueue) 경로로 재발급 엔드포인트를 친다.
                refreshClient.codeOf("${server.baseUrl}$REFRESH_PATH")
                accessToken = NEW_TOKEN
                refreshToken = "refresh-2"
                Result.success(TokenBundle(NEW_TOKEN, "refresh-2", expiresInSeconds = 3600))
            }
        }
    private val mainClient: OkHttpClient =
        run {
            val lazy = dagger.Lazy<AuthRepository> { repository }
            val reissuer = TokenReissuer(lazy, tracker, NoopReporter)
            NetworkModule.provideMainOkHttpClient(
                baseClient = base,
                loggingInterceptor = logging,
                authInterceptor = AuthInterceptor(lazy, tracker, reissuer),
                tokenAuthenticator = TokenAuthenticator(lazy, reissuer, NoopReporter),
            )
        }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `호스트 동시 한도를 넘는 401 재시도가 한 번의 재발급으로 전부 끝난다`() {
        assertTrue(CONCURRENT_CALLS > hostLimit)

        val codes = fireConcurrently(CONCURRENT_CALLS)

        assertEquals(List(CONCURRENT_CALLS) { 200 }, codes)
        assertEquals(1, server.requests.count { it.path == REFRESH_PATH })
        assertEquals(1, repository.rotateTokenCalls)
    }

    @Test
    fun `호스트 동시 한도를 넘는 선제 재발급이 한 번의 재발급으로 전부 끝난다`() {
        assertTrue(CONCURRENT_CALLS > hostLimit)
        tracker.record(expiresInSeconds = 10)

        val codes = fireConcurrently(CONCURRENT_CALLS)

        assertEquals(List(CONCURRENT_CALLS) { 200 }, codes)
        assertEquals(1, server.requests.count { it.path == REFRESH_PATH })
        assertEquals(1, repository.rotateTokenCalls)
    }

    private fun fireConcurrently(count: Int): List<Int> {
        val latch = CountDownLatch(count)
        val codes = ConcurrentLinkedQueue<Int>()
        repeat(count) {
            mainClient.newCall(Request.Builder().url("${server.baseUrl}$API_PATH").build()).enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        codes += -1
                        latch.countDown()
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use { codes += it.code }
                        latch.countDown()
                    }
                },
            )
        }
        assertTrue(
            "호출 ${count}건이 ${COMPLETION_TIMEOUT_SECONDS}초 안에 끝나지 않았다 — 재발급 교착",
            latch.await(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS),
        )
        return codes.toList()
    }

    private suspend fun OkHttpClient.codeOf(url: String): Int =
        suspendCancellableCoroutine { continuation ->
            newCall(Request.Builder().url(url).build()).enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use { continuation.resume(it.code) }
                    }
                },
            )
        }

    private companion object {
        const val OLD_TOKEN = "old"
        const val NEW_TOKEN = "new"
        const val API_PATH = "/api/v1/users/me"
        const val REFRESH_PATH = "/api/v1/auth/refresh"

        /** OkHttp Dispatcher 의 호스트당 기본 동시 한도(5)보다 많아야 교착 조건이 성립한다. */
        const val CONCURRENT_CALLS = 8
        const val COMPLETION_TIMEOUT_SECONDS = 20L
    }
}
