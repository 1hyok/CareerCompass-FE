package com.careercompass.feature.foryou.data

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.network.dto.ForYouFeedDto
import com.careercompass.core.network.dto.ForYouPickDto
import com.careercompass.core.network.dto.ForYouTopPickDto
import com.careercompass.core.network.model.ApiException
import com.careercompass.core.network.model.BaseResponse
import com.careercompass.core.network.service.ForYouApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ForYouRepositoryImplTest {
    private class FakeForYouApi(
        private val response: BaseResponse<ForYouFeedDto>? = null,
        private val throws: Throwable? = null,
    ) : ForYouApiService {
        var callCount: Int = 0
            private set

        override suspend fun getForYouFeed(): BaseResponse<ForYouFeedDto> {
            callCount += 1
            throws?.let { throw it }
            return checkNotNull(response)
        }
    }

    @Test
    fun `추천을 도메인 모델로 옮겨 돌려준다`() =
        runTest {
            val api =
                FakeForYouApi(
                    response =
                        BaseResponse(
                            ok = true,
                            data =
                                ForYouFeedDto(
                                    topPick = ForYouTopPickDto(postingId = 101, reason = listOf("전공 적합")),
                                    byStrength = listOf(ForYouPickDto(postingId = 102, reason = "Kotlin 경험")),
                                    byGap = emptyList(),
                                ),
                        ),
                )

            val recommendations = ForYouRepositoryImpl(api).getRecommendations().getOrThrow()

            assertEquals(101L, recommendations.topPick?.postingId)
            assertEquals(listOf("Kotlin 경험"), recommendations.byStrength.single().reasons)
            assertEquals(1, api.callCount)
        }

    @Test
    fun `프로필이 모자라 422 로 막히면 화면이 읽는 사유로 접힌다`() =
        runTest {
            val api =
                FakeForYouApi(
                    throws =
                        ApiException(
                            code = "PROFILE_INCOMPLETE",
                            serverMessage = "프로필을 먼저 채워 주세요",
                            fallbackMessage = "요청에 실패했습니다.",
                            status = 422,
                        ),
                )

            val failure = ForYouRepositoryImpl(api).getRecommendations().exceptionOrNull()

            assertTrue("실제 사유: $failure", failure is CoreDataFailure.ProfileIncomplete)
            assertEquals("PROFILE_INCOMPLETE", (failure as CoreDataFailure).code)
        }

    @Test
    fun `봉투가 ok false 로 와도 같은 사유로 접힌다`() =
        runTest {
            val api =
                FakeForYouApi(
                    response =
                        BaseResponse(
                            ok = false,
                            data = null,
                            error =
                                com.careercompass.core.network.model.ApiErrorDto(
                                    code = "PROFILE_INCOMPLETE",
                                    message = "프로필을 먼저 채워 주세요",
                                ),
                        ),
                )

            val failure = ForYouRepositoryImpl(api).getRecommendations().exceptionOrNull()

            assertTrue("실제 사유: $failure", failure is CoreDataFailure.ProfileIncomplete)
        }

    @Test
    fun `전송 실패는 네트워크 사유로 접히고 원인을 잃지 않는다`() =
        runTest {
            val transport = IOException("offline")

            val failure = ForYouRepositoryImpl(FakeForYouApi(throws = transport)).getRecommendations().exceptionOrNull()

            assertTrue("실제 사유: $failure", failure is CoreDataFailure.NetworkUnavailable)
            assertSame(transport, (failure as CoreDataFailure.NetworkUnavailable).transportCause)
        }
}
