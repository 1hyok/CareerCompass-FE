package com.cambridge.feature.onboarding.presentation.flow

import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeExperienceRepository
import com.cambridge.core.domain.testing.FakePastApplicationRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.application.MAX_PAST_APPLICATIONS
import com.cambridge.core.model.application.PastApplication
import com.cambridge.core.model.application.PastApplicationCategory
import com.cambridge.core.model.application.PastApplicationItem
import com.cambridge.core.model.application.UploadFile
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.experience.MAX_EXPERIENCE_CARDS
import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.model.user.UserProfileUpdate
import com.cambridge.feature.onboarding.domain.model.OnboardingProgress
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.model.SchoolCatalog
import com.cambridge.feature.onboarding.domain.testing.FakeOnboardingProgressRepository
import com.cambridge.feature.onboarding.domain.usecase.AddExperienceUseCase
import com.cambridge.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.cambridge.feature.onboarding.domain.usecase.DeleteExperienceUseCase
import com.cambridge.feature.onboarding.domain.usecase.DeletePastApplicationUseCase
import com.cambridge.feature.onboarding.domain.usecase.GetOnboardingExperiencesUseCase
import com.cambridge.feature.onboarding.domain.usecase.GetOnboardingPastApplicationsUseCase
import com.cambridge.feature.onboarding.domain.usecase.ProceedToPastApplicationUseCase
import com.cambridge.feature.onboarding.domain.usecase.ResolveOnboardingEntryUseCase
import com.cambridge.feature.onboarding.domain.usecase.SaveBasicInfoUseCase
import com.cambridge.feature.onboarding.domain.usecase.SaveJobPreferencesUseCase
import com.cambridge.feature.onboarding.domain.usecase.UpdateExperienceUseCase
import com.cambridge.feature.onboarding.domain.usecase.UpdatePastApplicationItemCategoryUseCase
import com.cambridge.feature.onboarding.domain.usecase.UploadPastApplicationUseCase
import com.cambridge.feature.onboarding.presentation.OnboardingStep1Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep2Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep3Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep4Event
import com.cambridge.feature.onboarding.presentation.basicinfo.GraduationDatePickerEvent
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerEvent
import com.cambridge.feature.onboarding.presentation.complete.OnboardingCompleteEvent
import com.cambridge.feature.onboarding.presentation.experience.ExperienceDeleteEvent
import com.cambridge.feature.onboarding.presentation.experience.ExperienceQuickAddEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.DirectInputEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.PastApplicationItemCategoryEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.UploadLabelEvent
import com.cambridge.feature.onboarding.presentation.reporting.RecordingErrorReporter
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDate
import java.time.Year

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val userProfileRepository = FakeUserProfileRepository(initialProfile = sampleProfile())
    private val progressRepository = FakeOnboardingProgressRepository()
    private val experienceRepository = FakeExperienceRepository()
    private val pastApplicationRepository = FakePastApplicationRepository()
    private val reporter = RecordingErrorReporter()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        OnboardingViewModel(
            resolveOnboardingEntry = ResolveOnboardingEntryUseCase(userProfileRepository, progressRepository),
            saveBasicInfo = SaveBasicInfoUseCase(userProfileRepository, progressRepository),
            saveJobPreferences = SaveJobPreferencesUseCase(userProfileRepository, progressRepository),
            getOnboardingExperiences = GetOnboardingExperiencesUseCase(experienceRepository),
            addExperience = AddExperienceUseCase(experienceRepository),
            updateExperience = UpdateExperienceUseCase(experienceRepository),
            deleteExperience = DeleteExperienceUseCase(experienceRepository),
            proceedToPastApplication = ProceedToPastApplicationUseCase(progressRepository),
            getOnboardingPastApplications = GetOnboardingPastApplicationsUseCase(pastApplicationRepository),
            uploadPastApplication = UploadPastApplicationUseCase(pastApplicationRepository),
            deletePastApplication = DeletePastApplicationUseCase(pastApplicationRepository),
            updatePastApplicationItemCategory = UpdatePastApplicationItemCategoryUseCase(pastApplicationRepository),
            completeOnboarding = CompleteOnboardingUseCase(progressRepository, userProfileRepository),
            errorReporter = reporter,
        )

    // ---- 진입·재개 ----

    @Test
    fun `기록이 없으면 첫 단계에 머물고 프로필로 프리필한다`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isResolvingEntry)
        assertTrue(state.isInputEnabled)
        assertNull(state.pendingNavigation)
        assertEquals("정일혁", state.userName)
        assertEquals("정일혁", state.step1.name)
        assertEquals("건국대학교", state.step1.school)
        assertEquals("컴퓨터공학부", state.step1.major)
        assertEquals("3.87", state.step1.gradePointAverage)
        assertEquals("2027", state.step1.graduationDate)
        assertEquals(listOf("backend", "frontend"), state.step2.selectedJobCodes)
        assertEquals(listOf("AI", "스타트업"), state.step2.interestTags)
    }

    @Test
    fun `중단된 단계가 있으면 그 단계로 이동하고 목록을 미리 읽는다`() {
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        experienceRepository.experiences += sampleExperience(id = 5L)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(OnboardingDestination.Step(OnboardingStep.Experience), state.pendingNavigation)
        assertTrue(state.step3.isLoaded)
        assertEquals(listOf(5L), state.step3.experiences.map(Experience::id))
    }

    @Test
    fun `이미 완료한 사용자는 피드로 보낸다`() {
        userProfileRepository.profileState.value = sampleProfile(onboardingDone = true)

        val viewModel = createViewModel()

        assertEquals(OnboardingDestination.Feed, viewModel.uiState.value.pendingNavigation)
    }

    @Test
    fun `프로필 갱신 실패는 진입을 막지 않고 기록만 한다`() {
        userProfileRepository.onRefreshProfile = { Result.failure(IOException("offline")) }

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isResolvingEntry)
        assertEquals("정일혁", viewModel.uiState.value.step1.name)
        assertEquals(listOf("resolve_entry"), reporter.stages())
        assertNull(viewModel.uiState.value.failure)
    }

    // ---- Step 1 ----

    @Test
    fun `입력 중 검증은 길이와 형식만 본다`() {
        val viewModel = createViewModel()

        viewModel.onStep1Event(OnboardingStep1Event.NameChanged("가".repeat(21)))
        viewModel.onStep1Event(OnboardingStep1Event.MajorChanged(""))
        viewModel.onStep1Event(OnboardingStep1Event.GradePointAverageChanged("abc"))

        val form = viewModel.uiState.value.step1
        assertEquals(OnboardingFieldError.TooLong(20), form.nameError)
        assertNull(form.majorError)
        assertEquals(OnboardingFieldError.InvalidFormat, form.gradePointAverageError)

        viewModel.onStep1Event(OnboardingStep1Event.GradePointAverageChanged("4.6"))
        assertEquals(OnboardingFieldError.OutOfRange, viewModel.uiState.value.step1.gradePointAverageError)
    }

    @Test
    fun `다음을 누르면 필수 값을 검사하고 저장하지 않는다`() {
        userProfileRepository.profileState.value = null
        val viewModel = createViewModel()

        viewModel.onStep1Event(OnboardingStep1Event.NameChanged("정일혁"))
        viewModel.onStep1Event(OnboardingStep1Event.NextClicked)

        val form = viewModel.uiState.value.step1
        assertEquals(OnboardingFieldError.Required, form.schoolError)
        assertEquals(OnboardingFieldError.Required, form.majorError)
        assertNull(form.nameError)
        assertTrue(userProfileRepository.updates.isEmpty())
        assertNull(viewModel.uiState.value.pendingNavigation)
    }

    @Test
    fun `기본 정보 저장은 프로필 수정 후 Step 2 로 이동한다`() {
        val viewModel = createViewModel()
        viewModel.onStep1Event(OnboardingStep1Event.NameChanged(" 정일혁 "))
        viewModel.onStep1Event(OnboardingStep1Event.GradePointAverageChanged("3.5"))
        viewModel.onSchoolPickerEvent(SchoolPickerEvent.SchoolSelected("연세대학교"))

        viewModel.onStep1Event(OnboardingStep1Event.NextClicked)

        assertEquals(
            listOf(UserProfileUpdate(name = "정일혁", school = "연세대학교", department = "컴퓨터공학부", gpa = 3.5, gradYear = 2027)),
            userProfileRepository.updates,
        )
        assertEquals(listOf(OnboardingStep.JobPreference), progressRepository.savedSteps)
        val state = viewModel.uiState.value
        assertEquals(OnboardingDestination.Step(OnboardingStep.JobPreference), state.pendingNavigation)
        assertFalse(state.isSubmitting)
        assertEquals("정일혁", state.userName)
    }

    @Test
    fun `저장 실패는 사유를 표시하고 단계를 기록한다`() {
        userProfileRepository.onUpdateProfile = { Result.failure(CoreDataFailure.NetworkUnavailable(IOException("offline"))) }
        val viewModel = createViewModel()

        viewModel.onStep1Event(OnboardingStep1Event.NextClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingFailureReason.Network, state.failure)
        assertFalse(state.isSubmitting)
        assertNull(state.pendingNavigation)
        assertEquals(listOf("save_basic_info"), reporter.stages())
        assertTrue(progressRepository.savedSteps.isEmpty())

        viewModel.onFailureConsumed()
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `저장 중에는 입력을 잠근다`() {
        val gate = CompletableDeferred<Unit>()
        userProfileRepository.onUpdateProfile = { update ->
            gate.await()
            Result.success(sampleProfile().copy(name = update.name))
        }
        val viewModel = createViewModel()

        viewModel.onStep1Event(OnboardingStep1Event.NextClicked)
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertFalse(viewModel.uiState.value.isInputEnabled)

        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `학교 피커는 검색어로 목록을 거르고 선택하면 닫힌다`() {
        val viewModel = createViewModel()

        viewModel.onStep1Event(OnboardingStep1Event.SchoolPickerClicked)
        assertEquals(
            SchoolCatalog.schools,
            viewModel.uiState.value.schoolPicker
                ?.results,
        )

        viewModel.onSchoolPickerEvent(SchoolPickerEvent.QueryChanged("건국"))
        assertEquals(
            listOf("건국대학교"),
            viewModel.uiState.value.schoolPicker
                ?.results,
        )

        viewModel.onSchoolPickerEvent(SchoolPickerEvent.SchoolSelected("건국대학교"))
        assertNull(viewModel.uiState.value.schoolPicker)
        assertEquals("건국대학교", viewModel.uiState.value.step1.school)

        viewModel.onStep1Event(OnboardingStep1Event.SchoolPickerClicked)
        viewModel.onSchoolPickerEvent(SchoolPickerEvent.Dismissed)
        assertNull(viewModel.uiState.value.schoolPicker)
    }

    @Test
    fun `졸업 피커는 입력값을 기본으로 열고 확정하면 YYYY점MM 을 채운다`() {
        val viewModel = createViewModel()

        viewModel.onStep1Event(OnboardingStep1Event.GraduationDatePickerClicked)
        val picker = viewModel.uiState.value.graduationPicker
        assertNotNull(picker)
        assertEquals(2027, picker!!.selectedYear)
        assertEquals(2, picker.selectedMonth)
        assertEquals(2000, picker.years.first())
        assertTrue(picker.years.contains(Year.now().value))

        viewModel.onGraduationPickerEvent(GraduationDatePickerEvent.YearSelected(2026))
        viewModel.onGraduationPickerEvent(GraduationDatePickerEvent.MonthSelected(8))
        viewModel.onGraduationPickerEvent(GraduationDatePickerEvent.Confirmed)

        assertNull(viewModel.uiState.value.graduationPicker)
        assertEquals("2026.08", viewModel.uiState.value.step1.graduationDate)
    }

    // ---- Step 2 ----

    @Test
    fun `직무는 세 개까지만 고르고 다시 누르면 해제된다`() {
        val viewModel = createViewModel()

        viewModel.onStep2Event(OnboardingStep2Event.JobSelectionToggled("mobile"))
        viewModel.onStep2Event(OnboardingStep2Event.JobSelectionToggled("qa"))
        assertEquals(listOf("backend", "frontend", "mobile"), viewModel.uiState.value.step2.selectedJobCodes)

        viewModel.onStep2Event(OnboardingStep2Event.JobSelectionToggled("backend"))
        assertEquals(listOf("frontend", "mobile"), viewModel.uiState.value.step2.selectedJobCodes)

        viewModel.onStep2Event(OnboardingStep2Event.JobSelectionToggled("unknown"))
        assertEquals(listOf("frontend", "mobile"), viewModel.uiState.value.step2.selectedJobCodes)
    }

    @Test
    fun `태그는 제출 시 정규화해 추가하고 중복과 공백은 거부한다`() {
        val viewModel = createViewModel()

        viewModel.onStep2Event(OnboardingStep2Event.InterestInputChanged(" #환경 "))
        viewModel.onStep2Event(OnboardingStep2Event.InterestTagSubmitted)
        assertEquals(listOf("AI", "스타트업", "환경"), viewModel.uiState.value.step2.interestTags)
        assertEquals("", viewModel.uiState.value.step2.interestInput)

        viewModel.onStep2Event(OnboardingStep2Event.InterestInputChanged("AI"))
        viewModel.onStep2Event(OnboardingStep2Event.InterestTagSubmitted)
        assertEquals(listOf("AI", "스타트업", "환경"), viewModel.uiState.value.step2.interestTags)

        viewModel.onStep2Event(OnboardingStep2Event.InterestInputChanged("   "))
        viewModel.onStep2Event(OnboardingStep2Event.InterestTagSubmitted)
        assertEquals(3, viewModel.uiState.value.step2.interestTags.size)

        viewModel.onStep2Event(OnboardingStep2Event.InterestTagRemoved("AI"))
        assertEquals(listOf("스타트업", "환경"), viewModel.uiState.value.step2.interestTags)
    }

    @Test
    fun `태그가 다섯 개면 더 추가할 수 없다고 알린다`() {
        val viewModel = createViewModel()
        listOf("환경", "교육", "핀테크").forEach { tag ->
            viewModel.onStep2Event(OnboardingStep2Event.InterestInputChanged(tag))
            viewModel.onStep2Event(OnboardingStep2Event.InterestTagSubmitted)
        }
        assertEquals(5, viewModel.uiState.value.step2.interestTags.size)

        viewModel.onStep2Event(OnboardingStep2Event.InterestInputChanged("여섯"))
        viewModel.onStep2Event(OnboardingStep2Event.InterestTagSubmitted)

        assertEquals(5, viewModel.uiState.value.step2.interestTags.size)
        assertEquals(OnboardingFailureReason.LimitExceeded, viewModel.uiState.value.failure)
    }

    @Test
    fun `직무 선호 저장은 우선순위 순서로 보내고 Step 3 로 이동한다`() {
        val viewModel = createViewModel()
        viewModel.onStep2Event(OnboardingStep2Event.JobSelectionToggled("backend"))
        viewModel.onStep2Event(OnboardingStep2Event.JobSelectionToggled("backend"))

        viewModel.onStep2Event(OnboardingStep2Event.NextClicked)

        assertEquals(
            listOf(JobInterest("frontend", 1), JobInterest("backend", 2)),
            userProfileRepository.replacedJobInterests.single(),
        )
        assertEquals(listOf("AI", "스타트업"), userProfileRepository.replacedTags.single())
        assertEquals(listOf(OnboardingStep.Experience), progressRepository.savedSteps)
        assertEquals(OnboardingDestination.Step(OnboardingStep.Experience), viewModel.uiState.value.pendingNavigation)
        assertTrue(viewModel.uiState.value.step3.isLoaded)
    }

    @Test
    fun `직무 선호 저장 실패는 이동하지 않는다`() {
        userProfileRepository.onReplaceTags = { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", IOException("500"))) }
        val viewModel = createViewModel()

        viewModel.onStep2Event(OnboardingStep2Event.NextClicked)

        assertEquals(OnboardingFailureReason.Server, viewModel.uiState.value.failure)
        assertNull(viewModel.uiState.value.pendingNavigation)
        assertEquals(listOf("save_job_preferences"), reporter.stages())
    }

    // ---- Step 3 ----

    @Test
    fun `경험 추가 시트는 선택한 유형으로 열리고 검증 실패를 필드에 돌려준다`() {
        val viewModel = createViewModel()
        viewModel.onStep3Event(OnboardingStep3Event.ExperienceTypeSelected("intern"))

        viewModel.onStep3Event(OnboardingStep3Event.AddExperienceClicked)
        assertEquals(
            ExperienceType.Intern,
            viewModel.uiState.value.experienceEditor
                ?.type,
        )

        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TitleChanged("카카오 인턴"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Submitted)

        val editor = viewModel.uiState.value.experienceEditor
        assertNotNull(editor)
        assertEquals(OnboardingFieldError.Required, editor!!.startDateError)
        assertEquals(OnboardingFieldError.Required, editor.primaryError)
        assertEquals(OnboardingFieldError.Required, editor.secondaryError)
        assertTrue(experienceRepository.createdDrafts.isEmpty())

        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.StartDateChanged("2025.13"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Submitted)
        assertEquals(
            OnboardingFieldError.InvalidFormat,
            viewModel.uiState.value.experienceEditor
                ?.startDateError,
        )
    }

    @Test
    fun `경험을 등록하면 목록 맨 앞에 넣고 시트를 닫는다`() {
        val viewModel = createViewModel()
        experienceRepository.experiences += sampleExperience(id = 1L)
        viewModel.onStep3Event(OnboardingStep3Event.AddExperienceClicked)
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TypeSelected(ExperienceType.Intern))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TitleChanged("카카오 인턴"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.StartDateChanged("2025.01"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.EndDateChanged("2025.02"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.PrimaryChanged("카카오"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.SecondaryChanged("안드로이드 개발"))

        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Submitted)

        val draft = experienceRepository.createdDrafts.single()
        assertEquals("카카오 인턴", draft.title)
        assertEquals(LocalDate.of(2025, 1, 1), draft.startDate)
        assertEquals(LocalDate.of(2025, 2, 1), draft.endDate)
        assertEquals(ExperienceDetails.Intern(company = "카카오", role = "안드로이드 개발", summary = null), draft.details)
        val state = viewModel.uiState.value
        assertNull(state.experienceEditor)
        assertEquals(ExperienceType.Intern, state.step3.selectedType)
        assertEquals(
            "카카오 인턴",
            state.step3.experiences
                .first()
                .title,
        )
    }

    @Test
    fun `경험 등록 실패는 시트를 열어 둔 채 사유를 알린다`() {
        experienceRepository.onCreateExperience = { Result.failure(CoreDataFailure.LimitExceeded("LIMIT_EXCEEDED", IOException("422"))) }
        val viewModel = createViewModel()
        viewModel.onStep3Event(OnboardingStep3Event.AddExperienceClicked)
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TypeSelected(ExperienceType.Award))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TitleChanged("공모전"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.PrimaryChanged("대상"))

        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Submitted)

        val state = viewModel.uiState.value
        assertNotNull(state.experienceEditor)
        assertFalse(state.experienceEditor!!.isSubmitting)
        assertEquals(OnboardingFailureReason.LimitExceeded, state.failure)
        assertEquals(listOf("add_experience"), reporter.stages())
    }

    @Test
    fun `카드를 누르면 기존 값이 채워진 수정 시트가 열리고 유형은 바뀌지 않는다`() {
        experienceRepository.experiences += internExperience(id = 3L)
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.ExperienceSelected("3"))

        val editor = viewModel.uiState.value.experienceEditor
        assertNotNull(editor)
        assertEquals(3L, editor!!.experienceId)
        assertTrue(editor.isEditing)
        assertEquals(ExperienceType.Intern, editor.type)
        assertEquals("카카오 인턴", editor.title)
        assertEquals("2025.01", editor.startDate)
        assertEquals("2025.02", editor.endDate)
        assertEquals("카카오", editor.primary)
        assertEquals("안드로이드 개발", editor.secondary)

        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TypeSelected(ExperienceType.Award))

        assertEquals(
            ExperienceType.Intern,
            viewModel.uiState.value.experienceEditor
                ?.type,
        )
    }

    @Test
    fun `수정 저장은 그 카드만 갱신하고 시트에 없는 값은 보존한다`() {
        experienceRepository.experiences += sampleExperience(id = 1L)
        experienceRepository.experiences += sampleExperience(id = 2L)
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.ExperienceSelected("2"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.TitleChanged("CareerCompass 리뉴얼"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Submitted)

        val state = viewModel.uiState.value
        assertNull(state.experienceEditor)
        assertEquals(listOf(1L, 2L), state.step3.experiences.map(Experience::id))
        val updated = state.step3.experiences.first { it.id == 2L }
        assertEquals("CareerCompass 리뉴얼", updated.title)
        // 빠른 입력 시트에 없는 기술 태그는 수정 저장에 쓸려 나가지 않는다.
        assertEquals(listOf("Kotlin"), (updated.details as ExperienceDetails.Project).techs)
        assertTrue(experienceRepository.createdDrafts.isEmpty())
    }

    @Test
    fun `수정 실패는 시트를 열어 둔 채 사유를 알린다`() {
        experienceRepository.experiences += sampleExperience(id = 1L)
        experienceRepository.onUpdateExperience = { _, _ -> Result.failure(IOException("offline")) }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.ExperienceSelected("1"))
        viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Submitted)

        val state = viewModel.uiState.value
        assertNotNull(state.experienceEditor)
        assertFalse(state.experienceEditor!!.isSubmitting)
        assertEquals(OnboardingFailureReason.Network, state.failure)
        assertEquals(listOf("update_experience"), reporter.stages())
        assertEquals(
            "CareerCompass",
            state.step3.experiences
                .single()
                .title,
        )
    }

    @Test
    fun `삭제는 확인 다이얼로그를 거쳐 목록에서 뺀다`() {
        experienceRepository.experiences += sampleExperience(id = 1L)
        experienceRepository.experiences += sampleExperience(id = 2L)
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.ExperienceDeleteClicked("1"))
        val pending = viewModel.uiState.value.experienceDelete
        assertNotNull(pending)
        assertEquals(1L, pending!!.experienceId)
        assertEquals("CareerCompass", pending.title)

        viewModel.onExperienceDeleteEvent(ExperienceDeleteEvent.Dismissed)
        assertNull(viewModel.uiState.value.experienceDelete)
        assertEquals(2, viewModel.uiState.value.step3.experiences.size)

        viewModel.onStep3Event(OnboardingStep3Event.ExperienceDeleteClicked("1"))
        viewModel.onExperienceDeleteEvent(ExperienceDeleteEvent.Confirmed)

        val state = viewModel.uiState.value
        assertNull(state.experienceDelete)
        assertEquals(listOf(2L), state.step3.experiences.map(Experience::id))
        assertTrue(experienceRepository.experiences.none { it.id == 1L })
    }

    @Test
    fun `삭제 실패는 카드를 원래 자리에 되돌리고 사유를 알린다`() {
        experienceRepository.experiences += sampleExperience(id = 1L)
        experienceRepository.experiences += sampleExperience(id = 2L)
        experienceRepository.experiences += sampleExperience(id = 3L)
        experienceRepository.onDeleteExperience = {
            Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", IOException("500")))
        }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.ExperienceDeleteClicked("2"))
        viewModel.onExperienceDeleteEvent(ExperienceDeleteEvent.Confirmed)

        val state = viewModel.uiState.value
        assertEquals(listOf(1L, 2L, 3L), state.step3.experiences.map(Experience::id))
        assertEquals(OnboardingFailureReason.Server, state.failure)
        assertEquals(listOf("delete_experience"), reporter.stages())
    }

    @Test
    fun `상한에 닿으면 추가를 막고 하나를 지우면 다시 열린다`() {
        repeat(MAX_EXPERIENCE_CARDS) { index -> experienceRepository.experiences += sampleExperience(id = index + 1L) }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.Experience)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.AddExperienceClicked)

        assertNull(viewModel.uiState.value.experienceEditor)
        assertEquals(OnboardingFailureReason.LimitExceeded, viewModel.uiState.value.failure)

        viewModel.onFailureConsumed()
        viewModel.onStep3Event(OnboardingStep3Event.ExperienceDeleteClicked("1"))
        viewModel.onExperienceDeleteEvent(ExperienceDeleteEvent.Confirmed)
        viewModel.onStep3Event(OnboardingStep3Event.AddExperienceClicked)

        assertEquals(MAX_EXPERIENCE_CARDS - 1, viewModel.uiState.value.step3.experiences.size)
        assertNotNull(viewModel.uiState.value.experienceEditor)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `Step 3 다음은 진행 기록만 바꾸고 Step 4 로 이동하며 목록을 미리 읽는다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 3)
        val viewModel = createViewModel()

        viewModel.onStep3Event(OnboardingStep3Event.NextClicked)

        assertEquals(listOf(OnboardingStep.PastApplication), progressRepository.savedSteps)
        val state = viewModel.uiState.value
        assertEquals(OnboardingDestination.Step(OnboardingStep.PastApplication), state.pendingNavigation)
        assertTrue(state.step4.isLoaded)
        val document = state.step4.documents.single()
        assertEquals("remote-9", document.id)
        assertEquals(9L, document.remoteId)
        assertEquals(3, (document.status as OnboardingUploadStatus.Completed).classifiedItemCount)
        assertNull(document.file)
    }

    // ---- Step 4 ----

    @Test
    fun `파일 업로드는 처리 중을 거쳐 완료로 바뀐다`() {
        val gate = CompletableDeferred<Unit>()
        pastApplicationRepository.onUpload = { _, label ->
            gate.await()
            Result.success(samplePastApplication(id = 11L, itemCount = 4, label = label))
        }
        val viewModel = createViewModel()

        viewModel.confirmUpload(uploadFile("resume.pdf"))

        val processing =
            viewModel.uiState.value.step4.documents
                .single()
        assertEquals(OnboardingUploadStatus.Processing, processing.status)
        // 기본 라벨을 그대로 확인하면 확장자만 빠진다.
        assertEquals("resume", processing.label)

        gate.complete(Unit)
        val completed =
            viewModel.uiState.value.step4.documents
                .single()
        assertEquals(4, (completed.status as OnboardingUploadStatus.Completed).classifiedItemCount)
        assertEquals(11L, completed.remoteId)
        assertEquals("resume", pastApplicationRepository.uploads.single().second)
    }

    @Test
    fun `업로드 실패는 실패 상태로 두고 재시도는 같은 파일을 다시 올린다`() {
        pastApplicationRepository.onUpload = { _, _ -> Result.failure(CoreDataFailure.NetworkUnavailable(IOException("offline"))) }
        val viewModel = createViewModel()
        val file = uploadFile("resume.docx")

        viewModel.confirmUpload(file, label = "2024 카카오 인턴 자소서")
        val failed =
            viewModel.uiState.value.step4.documents
                .single()
        assertEquals(OnboardingUploadStatus.Failed(OnboardingFailureReason.Network), failed.status)
        assertEquals(listOf("upload_past_application"), reporter.stages())

        pastApplicationRepository.onUpload = null
        viewModel.onStep4Event(OnboardingStep4Event.DocumentRetryClicked(failed.id))

        val retried =
            viewModel.uiState.value.step4.documents
                .single()
        assertEquals(0, (retried.status as OnboardingUploadStatus.Completed).classifiedItemCount)
        assertEquals(2, pastApplicationRepository.uploads.size)
        assertTrue(pastApplicationRepository.uploads.all { it.first === file })
        // 재시도는 파일명이 아니라 사용자가 정한 라벨을 그대로 다시 보낸다.
        assertTrue(pastApplicationRepository.uploads.all { it.second == "2024 카카오 인턴 자소서" })
    }

    @Test
    fun `메뉴는 서버 문서를 삭제하고 실패한 로컬 문서는 목록에서만 뺀다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 1)
        pastApplicationRepository.onUpload = { _, _ -> Result.failure(IOException("offline")) }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()
        viewModel.confirmUpload(uploadFile("resume.txt"))
        val localId =
            viewModel.uiState.value.step4.documents
                .first { it.remoteId == null }
                .id

        viewModel.onStep4Event(OnboardingStep4Event.DocumentMenuClicked("remote-9"))
        viewModel.onStep4Event(OnboardingStep4Event.DocumentMenuClicked(localId))

        assertTrue(
            viewModel.uiState.value.step4.documents
                .isEmpty(),
        )
        assertTrue(pastApplicationRepository.applications.none { it.id == 9L })
    }

    @Test
    fun `서버 문서 삭제 실패는 목록을 유지하고 사유를 알린다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 1)
        pastApplicationRepository.onDelete = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", IOException("401"))) }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.DocumentMenuClicked("remote-9"))

        assertEquals(1, viewModel.uiState.value.step4.documents.size)
        assertEquals(OnboardingFailureReason.SessionExpired, viewModel.uiState.value.failure)
        assertEquals(listOf("delete_past_application"), reporter.stages())
    }

    @Test
    fun `파일 선택 실패는 사유를 표시하고 기록한다`() {
        val viewModel = createViewModel()

        viewModel.onFileSelectionFailed(OnboardingFailureReason.UnsupportedFile, IllegalArgumentException("png"))

        assertEquals(OnboardingFailureReason.UnsupportedFile, viewModel.uiState.value.failure)
        assertEquals(listOf("upload_past_application"), reporter.stages())
        assertTrue(
            viewModel.uiState.value.step4.documents
                .isEmpty(),
        )
    }

    @Test
    fun `파일을 고르면 올리기 전에 라벨 시트를 열고 기본 라벨은 확장자를 뺀 파일명이다`() {
        val viewModel = createViewModel()

        viewModel.onFileSelected(uploadFile("이력서_최종_v3(2).pdf"))

        val sheet = viewModel.uiState.value.uploadLabel
        assertNotNull(sheet)
        assertEquals("이력서_최종_v3(2)", sheet?.label)
        assertEquals("이력서_최종_v3(2).pdf", sheet?.fileName)
        // 확인하기 전에는 올리지도, 목록에 넣지도 않는다.
        assertTrue(pastApplicationRepository.uploads.isEmpty())
        assertTrue(
            viewModel.uiState.value.step4.documents
                .isEmpty(),
        )
    }

    @Test
    fun `라벨을 고쳐 확인하면 파일명 대신 그 라벨로 올린다`() {
        val viewModel = createViewModel()
        viewModel.onFileSelected(uploadFile("resume.pdf"))

        viewModel.onUploadLabelEvent(UploadLabelEvent.LabelChanged(" 2024 카카오 인턴 자소서 "))
        viewModel.onUploadLabelEvent(UploadLabelEvent.Submitted)

        assertNull(viewModel.uiState.value.uploadLabel)
        val (file, label) = pastApplicationRepository.uploads.single()
        assertEquals("2024 카카오 인턴 자소서", label)
        assertEquals("resume.pdf", file.fileName)
        assertEquals(
            "2024 카카오 인턴 자소서",
            viewModel.uiState.value.step4.documents
                .single()
                .label,
        )
    }

    @Test
    fun `업로드 라벨은 직접 입력과 같은 규칙으로 거른다`() {
        val viewModel = createViewModel()
        viewModel.onFileSelected(uploadFile("resume.pdf"))

        viewModel.onUploadLabelEvent(UploadLabelEvent.LabelChanged("   "))
        viewModel.onUploadLabelEvent(UploadLabelEvent.Submitted)
        assertEquals(
            OnboardingFieldError.Required,
            viewModel.uiState.value.uploadLabel
                ?.labelError,
        )

        viewModel.onUploadLabelEvent(UploadLabelEvent.LabelChanged("가".repeat(51)))
        viewModel.onUploadLabelEvent(UploadLabelEvent.Submitted)
        assertEquals(
            OnboardingFieldError.TooLong(50),
            viewModel.uiState.value.uploadLabel
                ?.labelError,
        )
        assertTrue(pastApplicationRepository.uploads.isEmpty())
    }

    @Test
    fun `라벨 시트를 취소하면 고른 파일을 올리지 않는다`() {
        val viewModel = createViewModel()
        viewModel.onFileSelected(uploadFile("resume.pdf"))

        viewModel.onUploadLabelEvent(UploadLabelEvent.Dismissed)

        assertNull(viewModel.uiState.value.uploadLabel)
        assertTrue(pastApplicationRepository.uploads.isEmpty())
        assertTrue(
            viewModel.uiState.value.step4.documents
                .isEmpty(),
        )
    }

    @Test
    fun `문서 상한이 차면 라벨 시트를 열지 않고 사유만 알린다`() {
        repeat(MAX_PAST_APPLICATIONS) { index ->
            pastApplicationRepository.applications += samplePastApplication(id = index + 1L, itemCount = 0)
        }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onFileSelected(uploadFile("resume.pdf"))

        assertNull(viewModel.uiState.value.uploadLabel)
        assertEquals(OnboardingFailureReason.LimitExceeded, viewModel.uiState.value.failure)
        assertEquals(MAX_PAST_APPLICATIONS, viewModel.uiState.value.step4.documents.size)
    }

    @Test
    fun `직접 입력은 라벨과 본문을 검증한 뒤 TXT 로 업로드한다`() {
        val viewModel = createViewModel()
        viewModel.onStep4Event(OnboardingStep4Event.DirectInputClicked)
        assertNotNull(viewModel.uiState.value.directInput)

        viewModel.onDirectInputEvent(DirectInputEvent.Submitted)
        assertEquals(
            OnboardingFieldError.Required,
            viewModel.uiState.value.directInput
                ?.labelError,
        )
        assertEquals(
            OnboardingFieldError.Required,
            viewModel.uiState.value.directInput
                ?.contentError,
        )

        viewModel.onDirectInputEvent(DirectInputEvent.LabelChanged(" 2024 카카오 인턴 자소서 "))
        viewModel.onDirectInputEvent(DirectInputEvent.ContentChanged("지원 동기는 ..."))
        viewModel.onDirectInputEvent(DirectInputEvent.Submitted)

        assertNull(viewModel.uiState.value.directInput)
        val (file, label) = pastApplicationRepository.uploads.single()
        assertEquals("2024 카카오 인턴 자소서", label)
        assertEquals("2024 카카오 인턴 자소서.txt", file.fileName)
        assertEquals("지원 동기는 ...", file.openStream().readBytes().toString(Charsets.UTF_8))
        val document =
            viewModel.uiState.value.step4.documents
                .single()
        assertEquals("2024 카카오 인턴 자소서", document.label)
        assertEquals(0, (document.status as OnboardingUploadStatus.Completed).classifiedItemCount)
    }

    @Test
    fun `분류 항목은 펼쳤다 접을 수 있고 문서를 지우면 펼침도 함께 사라진다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 2)
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.DocumentExpandToggled("remote-9"))
        assertEquals("remote-9", viewModel.uiState.value.step4.expandedDocumentId)

        viewModel.onStep4Event(OnboardingStep4Event.DocumentExpandToggled("remote-9"))
        assertNull(viewModel.uiState.value.step4.expandedDocumentId)

        viewModel.onStep4Event(OnboardingStep4Event.DocumentExpandToggled("remote-9"))
        viewModel.onStep4Event(OnboardingStep4Event.DocumentMenuClicked("remote-9"))

        assertTrue(
            viewModel.uiState.value.step4.documents
                .isEmpty(),
        )
        assertNull(viewModel.uiState.value.step4.expandedDocumentId)
    }

    @Test
    fun `항목이 없는 문서는 펼치지 않는다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 0)
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.DocumentExpandToggled("remote-9"))

        assertNull(viewModel.uiState.value.step4.expandedDocumentId)
    }

    @Test
    fun `분류 조정은 낙관적으로 반영하고 성공하면 서버 항목으로 맞춘다`() {
        val gate = CompletableDeferred<Unit>()
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 2, confident = false)
        pastApplicationRepository.onUpdateItemCategory = { _, itemId, category ->
            gate.await()
            Result.success(PastApplicationItem(id = itemId, category = category, content = "서버 본문", confident = true))
        }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.ItemCategoryClicked("remote-9", 1L))
        val picker = viewModel.uiState.value.itemCategoryPicker
        assertNotNull(picker)
        assertEquals(1L, picker!!.itemId)
        assertEquals(PastApplicationCategory.Other, picker.selected)
        assertEquals("내용 0", picker.contentPreview)

        viewModel.onItemCategoryPickerEvent(PastApplicationItemCategoryEvent.CategorySelected(PastApplicationCategory.Motivation))

        assertNull(viewModel.uiState.value.itemCategoryPicker)
        val optimistic = items(viewModel, "remote-9")
        assertEquals(PastApplicationCategory.Motivation, optimistic.first().category)
        assertEquals("내용 0", optimistic.first().content)
        assertEquals(PastApplicationCategory.Other, optimistic.last().category)

        gate.complete(Unit)

        val saved = items(viewModel, "remote-9")
        assertEquals("서버 본문", saved.first().content)
        assertTrue(saved.first().confident)
        assertFalse(saved.last().confident)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `분류 조정 실패는 이전 분류로 되돌리고 사유를 알린다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 1, confident = false)
        pastApplicationRepository.onUpdateItemCategory = { _, _, _ ->
            Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", IOException("500")))
        }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.ItemCategoryClicked("remote-9", 1L))
        viewModel.onItemCategoryPickerEvent(PastApplicationItemCategoryEvent.CategorySelected(PastApplicationCategory.Growth))

        val reverted = items(viewModel, "remote-9").single()
        assertEquals(PastApplicationCategory.Other, reverted.category)
        assertFalse(reverted.confident)
        assertEquals(OnboardingFailureReason.Server, viewModel.uiState.value.failure)
        assertEquals(listOf("update_past_application_item_category"), reporter.stages())
    }

    @Test
    fun `같은 분류를 다시 고르면 요청하지 않는다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 1)
        pastApplicationRepository.onUpdateItemCategory = { _, _, _ -> error("요청하면 안 된다") }
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.ItemCategoryClicked("remote-9", 1L))
        viewModel.onItemCategoryPickerEvent(PastApplicationItemCategoryEvent.CategorySelected(PastApplicationCategory.Other))

        assertNull(viewModel.uiState.value.itemCategoryPicker)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `분류 조정 중에도 건너뛰기로 온보딩을 끝낼 수 있다`() {
        pastApplicationRepository.applications += samplePastApplication(id = 9L, itemCount = 1, confident = false)
        progressRepository.progressState.value = OnboardingProgress.InProgress(OnboardingStep.PastApplication)
        val viewModel = createViewModel()
        viewModel.onStep4Event(OnboardingStep4Event.DocumentExpandToggled("remote-9"))

        viewModel.onStep4Event(OnboardingStep4Event.SkipClicked)

        assertEquals(OnboardingDestination.Complete, viewModel.uiState.value.pendingNavigation)
    }

    @Test
    fun `완료와 건너뛰기는 완료 기록 후 완료 화면으로 보낸다`() {
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.SkipClicked)

        assertEquals(1, progressRepository.markCompletedCalls)
        assertEquals(OnboardingProgress.Completed, progressRepository.progressState.value)
        assertEquals(OnboardingDestination.Complete, viewModel.uiState.value.pendingNavigation)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `완료 기록 실패는 사유를 알린다`() {
        progressRepository.onMarkCompleted = { Result.failure(IOException("disk")) }
        val viewModel = createViewModel()

        viewModel.onStep4Event(OnboardingStep4Event.CompleteClicked)

        assertEquals(OnboardingFailureReason.Network, viewModel.uiState.value.failure)
        assertNull(viewModel.uiState.value.pendingNavigation)
        assertEquals(listOf("complete"), reporter.stages())
    }

    // ---- 완료 ----

    @Test
    fun `완료 화면 액션은 피드 또는 게시판 등록으로 보낸다`() {
        val viewModel = createViewModel()

        viewModel.onCompleteEvent(OnboardingCompleteEvent.ViewFeedClicked)
        assertEquals(OnboardingDestination.Feed, viewModel.uiState.value.pendingNavigation)
        viewModel.onNavigationConsumed()
        assertNull(viewModel.uiState.value.pendingNavigation)

        viewModel.onCompleteEvent(OnboardingCompleteEvent.RegisterBoardClicked)
        assertEquals(OnboardingDestination.BoardRegister, viewModel.uiState.value.pendingNavigation)
    }

    private fun sampleProfile(onboardingDone: Boolean = false) =
        UserProfile(
            id = 1L,
            name = "정일혁",
            school = "건국대학교",
            department = "컴퓨터공학부",
            gpa = 3.87,
            gradYear = 2027,
            jobInterests = listOf(JobInterest("frontend", 2), JobInterest("backend", 1)),
            tags = listOf("AI", "스타트업"),
            onboardingDone = onboardingDone,
            completion = 40,
        )

    private fun sampleExperience(id: Long) =
        Experience(
            id = id,
            title = "CareerCompass",
            startDate = LocalDate.of(2025, 9, 1),
            endDate = null,
            details = ExperienceDetails.Project(role = "안드로이드", techs = listOf("Kotlin"), summary = null, link = null),
            createdAt = null,
        )

    private fun internExperience(id: Long) =
        Experience(
            id = id,
            title = "카카오 인턴",
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 2, 1),
            details = ExperienceDetails.Intern(company = "카카오", role = "안드로이드 개발", summary = "요약"),
            createdAt = null,
        )

    private fun samplePastApplication(
        id: Long,
        itemCount: Int,
        label: String = "지원서 $id",
        confident: Boolean = true,
    ) = PastApplication(
        id = id,
        label = label,
        items =
            List(itemCount) { index ->
                PastApplicationItem(
                    id = index + 1L,
                    category = PastApplicationCategory.Other,
                    content = "내용 $index",
                    confident = confident,
                )
            },
        createdAt = null,
    )

    private fun items(
        viewModel: OnboardingViewModel,
        documentId: String,
    ): List<PastApplicationItem> {
        val status =
            viewModel.uiState.value.step4.documents
                .first { it.id == documentId }
                .status
        return (status as OnboardingUploadStatus.Completed).items
    }

    private fun uploadFile(fileName: String) = UploadFile(fileName = fileName, sizeBytes = 16L) { ByteArrayInputStream(ByteArray(16)) }

    /** 파일 선택 → 라벨 시트 확인까지. [label] 을 주지 않으면 기본 라벨(확장자를 뺀 파일명)을 그대로 쓴다. */
    private fun OnboardingViewModel.confirmUpload(
        file: UploadFile,
        label: String? = null,
    ) {
        onFileSelected(file)
        if (label != null) onUploadLabelEvent(UploadLabelEvent.LabelChanged(label))
        onUploadLabelEvent(UploadLabelEvent.Submitted)
    }
}
