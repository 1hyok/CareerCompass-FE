package com.careercompass.feature.onboarding.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.careercompass.feature.onboarding.data.support.InMemoryPreferencesDataStore
import com.careercompass.feature.onboarding.domain.model.OnboardingProgress
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OnboardingProgressRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val repository = OnboardingProgressRepositoryImpl(dataStore)

    @Test
    fun `저장 전에는 NotStarted 다`() =
        runTest {
            assertEquals(OnboardingProgress.NotStarted, repository.progress.first())
        }

    @Test
    fun `단계를 저장하면 같은 단계로 복원된다`() =
        runTest {
            assertTrue(repository.save(OnboardingStep.Experience).isSuccess)

            assertEquals(OnboardingProgress.InProgress(OnboardingStep.Experience), repository.progress.first())
            assertEquals(
                OnboardingProgress.InProgress(OnboardingStep.Experience),
                OnboardingProgressRepositoryImpl(dataStore).progress.first(),
            )
        }

    @Test
    fun `완료 기록은 단계 값보다 우선한다`() =
        runTest {
            repository.save(OnboardingStep.PastApplication)

            assertTrue(repository.markCompleted().isSuccess)

            assertEquals(OnboardingProgress.Completed, repository.progress.first())
        }

    @Test
    fun `완료 뒤 단계를 다시 저장하면 진행 중으로 돌아간다`() =
        runTest {
            repository.markCompleted()

            repository.save(OnboardingStep.BasicInfo)

            assertEquals(OnboardingProgress.InProgress(OnboardingStep.BasicInfo), repository.progress.first())
        }

    @Test
    fun `clear 는 단계와 완료 기록을 모두 지운다`() =
        runTest {
            repository.save(OnboardingStep.JobPreference)
            repository.markCompleted()

            assertTrue(repository.clear().isSuccess)

            assertEquals(OnboardingProgress.NotStarted, repository.progress.first())
            assertTrue(dataStore.snapshot().asMap().isEmpty())
        }

    @Test
    fun `알 수 없는 단계 이름은 NotStarted 로 읽는다`() =
        runTest {
            val corrupted = InMemoryPreferencesDataStore(mutablePreferencesOf(stringPreferencesKey("step") to "Legacy"))

            assertEquals(OnboardingProgress.NotStarted, OnboardingProgressRepositoryImpl(corrupted).progress.first())
        }

    @Test
    fun `쓰기 실패는 Result 실패로 돌려준다`() =
        runTest {
            dataStore.failOnWrite = true

            val result = repository.save(OnboardingStep.Experience)

            assertTrue(result.exceptionOrNull() is IOException)
            assertEquals(OnboardingProgress.NotStarted, repository.progress.first())
        }
}
