package com.careercompass.feature.onboarding.domain.testing

import com.careercompass.feature.onboarding.domain.model.OnboardingProgress
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import com.careercompass.feature.onboarding.domain.repository.OnboardingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [OnboardingProgressRepository] fake 정본. 기본은 메모리에 진행 상태를 저장하고 호출을 기록한다.
 *
 * 특정 실패는 `onX` 로 갈아끼운다. 호출 금지 경계가 필요한 테스트는 [strict] 로 시작해 실제로 쓰는 경로만 연다.
 */
public class FakeOnboardingProgressRepository(
    initial: OnboardingProgress = OnboardingProgress.NotStarted,
    public var onSave: (suspend (OnboardingStep) -> Result<Unit>)? = null,
    public var onMarkCompleted: (suspend () -> Result<Unit>)? = null,
    public var onClear: (suspend () -> Result<Unit>)? = null,
) : OnboardingProgressRepository {
    public val progressState: MutableStateFlow<OnboardingProgress> = MutableStateFlow(initial)
    public val savedSteps: CopyOnWriteArrayList<OnboardingStep> = CopyOnWriteArrayList()
    private val markCompletedCounter = AtomicInteger()
    private val clearCounter = AtomicInteger()

    public val markCompletedCalls: Int get() = markCompletedCounter.get()
    public val clearCalls: Int get() = clearCounter.get()

    override val progress: Flow<OnboardingProgress> get() = progressState

    override suspend fun save(step: OnboardingStep): Result<Unit> {
        savedSteps += step
        onSave?.let { return it(step) }
        progressState.value = OnboardingProgress.InProgress(step)
        return Result.success(Unit)
    }

    override suspend fun markCompleted(): Result<Unit> {
        markCompletedCounter.incrementAndGet()
        onMarkCompleted?.let { return it() }
        progressState.value = OnboardingProgress.Completed
        return Result.success(Unit)
    }

    override suspend fun clear(): Result<Unit> {
        clearCounter.incrementAndGet()
        onClear?.let { return it() }
        progressState.value = OnboardingProgress.NotStarted
        return Result.success(Unit)
    }

    public companion object {
        /** 모든 경로를 닫고, 테스트가 쓰는 `onX` 만 명시적으로 연다. */
        public fun strict(initial: OnboardingProgress = OnboardingProgress.NotStarted): FakeOnboardingProgressRepository =
            FakeOnboardingProgressRepository(
                initial = initial,
                onSave = { unexpectedCall("OnboardingProgressRepository.save") },
                onMarkCompleted = { unexpectedCall("OnboardingProgressRepository.markCompleted") },
                onClear = { unexpectedCall("OnboardingProgressRepository.clear") },
            )

        private fun unexpectedCall(method: String): Nothing = error("$method 는 이 시나리오에서 호출되면 안 됨")
    }
}
