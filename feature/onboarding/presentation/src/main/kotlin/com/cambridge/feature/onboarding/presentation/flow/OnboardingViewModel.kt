package com.cambridge.feature.onboarding.presentation.flow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.model.application.MAX_PAST_APPLICATIONS
import com.cambridge.core.model.application.MAX_PAST_APPLICATION_FILE_BYTES
import com.cambridge.core.model.application.PastApplication
import com.cambridge.core.model.application.PastApplicationCategory
import com.cambridge.core.model.application.PastApplicationFileFormat
import com.cambridge.core.model.application.PastApplicationItem
import com.cambridge.core.model.application.UploadFile
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperiencePoint
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.experience.MAX_EXPERIENCE_CARDS
import com.cambridge.core.model.experience.MAX_EXPERIENCE_LINK_LENGTH
import com.cambridge.core.model.experience.MAX_EXPERIENCE_TECH_TAGS
import com.cambridge.core.model.experience.MAX_EXPERIENCE_TECH_TAG_LENGTH
import com.cambridge.core.model.experience.isAllowedExperienceLink
import com.cambridge.core.model.user.MAX_JOB_INTERESTS
import com.cambridge.core.model.user.MAX_PROFILE_TAGS
import com.cambridge.core.model.user.MIN_GRADUATION_YEAR
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.onboarding.domain.model.JobOptionCatalog
import com.cambridge.feature.onboarding.domain.model.OnboardingProgress
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.model.SchoolCatalog
import com.cambridge.feature.onboarding.domain.model.SchoolNameRules
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
import com.cambridge.feature.onboarding.presentation.basicinfo.GraduationPickerState
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolDirectInputState
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerEvent
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerState
import com.cambridge.feature.onboarding.presentation.complete.OnboardingCompleteEvent
import com.cambridge.feature.onboarding.presentation.experience.ExperienceDeleteEvent
import com.cambridge.feature.onboarding.presentation.experience.ExperienceDeleteState
import com.cambridge.feature.onboarding.presentation.experience.ExperienceEditorRules
import com.cambridge.feature.onboarding.presentation.experience.ExperienceEditorState
import com.cambridge.feature.onboarding.presentation.experience.ExperienceQuickAddEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.DirectInputEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.DirectInputState
import com.cambridge.feature.onboarding.presentation.pastapplication.PastApplicationItemCategoryEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.PastApplicationItemCategoryState
import com.cambridge.feature.onboarding.presentation.pastapplication.UploadLabelEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.UploadLabelState
import com.cambridge.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.cambridge.feature.onboarding.presentation.reporting.recordOnboardingFailure
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.time.Year
import javax.inject.Inject

/**
 * 온보딩 Step 1~4 와 완료 화면의 상태를 한 그래프 스코프에서 소유한다 — 기능 스펙 F1-2.
 *
 * - 재개: `init` 에서 [ResolveOnboardingEntryUseCase] 로 시작 단계를 정하고 프로필 값을 프리필한다(F1-1).
 * - 단계 저장은 각 use case 가 서버 저장과 진행 기록을 함께 처리하고, 성공하면 [OnboardingDestination.Step] 을 낸다.
 * - 실패 사유는 [OnboardingFailureReason] 으로 두고 계측은 [ErrorReporter] 로 남긴다. 문구·플랫폼 의존은 Entry 몫이다.
 * - 입력 초안은 [OnboardingInputDraft] 가 [SavedStateHandle] 에 남긴다 — 프로세스가 죽어도 친 글자가 남는다(#133).
 *   무엇을 남기고 무엇을 버리는지, 서버 값과의 우선순위가 어떻게 되는지는 그 클래스의 KDoc 에 있다.
 */
@HiltViewModel
public class OnboardingViewModel
    @Inject
    constructor(
        private val resolveOnboardingEntry: ResolveOnboardingEntryUseCase,
        private val saveBasicInfo: SaveBasicInfoUseCase,
        private val saveJobPreferences: SaveJobPreferencesUseCase,
        private val getOnboardingExperiences: GetOnboardingExperiencesUseCase,
        private val addExperience: AddExperienceUseCase,
        private val updateExperience: UpdateExperienceUseCase,
        private val deleteExperience: DeleteExperienceUseCase,
        private val proceedToPastApplication: ProceedToPastApplicationUseCase,
        private val getOnboardingPastApplications: GetOnboardingPastApplicationsUseCase,
        private val uploadPastApplication: UploadPastApplicationUseCase,
        private val deletePastApplication: DeletePastApplicationUseCase,
        private val updatePastApplicationItemCategory: UpdatePastApplicationItemCategoryUseCase,
        private val completeOnboarding: CompleteOnboardingUseCase,
        private val errorReporter: ErrorReporter,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val draft = OnboardingInputDraft(savedStateHandle)

        // 초안이 시작값이고, 서버 프리필이 그 위에 덮인다 — 우선순위는 서버 > 초안 > 빈 값이다.
        private val _uiState = MutableStateFlow(draft.restoredState())
        public val uiState: StateFlow<OnboardingFlowState> = _uiState.asStateFlow()

        private var nextLocalDocumentId = 1

        init {
            viewModelScope.launch { _uiState.collect(draft::save) }
            viewModelScope.launch { resolveEntry() }
        }

        // ---- 진입·재개 ----

        private suspend fun resolveEntry() {
            val entry = resolveOnboardingEntry()
            entry.profileRefreshFailure?.let { errorReporter.recordOnboardingFailure(OnboardingFailureStage.ResolveEntry, it) }
            val profile = entry.profile
            _uiState.update {
                it.copy(
                    isResolvingEntry = false,
                    userName = profile?.name,
                    step1 = it.step1.prefill(profile),
                    step2 = it.step2.prefill(profile),
                )
            }
            when (val progress = entry.progress) {
                OnboardingProgress.Completed -> {
                    navigateTo(OnboardingDestination.Feed)
                }

                is OnboardingProgress.InProgress -> {
                    if (progress.step != OnboardingStep.BasicInfo) navigateTo(OnboardingDestination.Step(progress.step))
                    prepareStep(progress.step)
                }

                OnboardingProgress.NotStarted -> {
                    Unit
                }
            }
        }

        private fun moveToStep(step: OnboardingStep) {
            navigateTo(OnboardingDestination.Step(step))
            prepareStep(step)
        }

        private fun prepareStep(step: OnboardingStep) {
            when (step) {
                OnboardingStep.BasicInfo, OnboardingStep.JobPreference -> Unit
                OnboardingStep.Experience -> loadExperiences()
                OnboardingStep.PastApplication -> loadPastApplications()
            }
        }

        // ---- Step 1 ----

        public fun onStep1Event(event: OnboardingStep1Event) {
            when (event) {
                is OnboardingStep1Event.NameChanged -> {
                    updateStep1 {
                        copy(
                            name = event.value,
                            nameError =
                                OnboardingStep1Rules.validateText(
                                    event.value,
                                    OnboardingStep1Rules.MAX_NAME_LENGTH,
                                    requireValue = false,
                                ),
                        )
                    }
                }

                is OnboardingStep1Event.MajorChanged -> {
                    updateStep1 {
                        copy(
                            major = event.value,
                            majorError =
                                OnboardingStep1Rules.validateText(
                                    event.value,
                                    OnboardingStep1Rules.MAX_MAJOR_LENGTH,
                                    requireValue = false,
                                ),
                        )
                    }
                }

                is OnboardingStep1Event.GradePointAverageChanged -> {
                    updateStep1 {
                        copy(
                            gradePointAverage = event.value,
                            gradePointAverageError = OnboardingStep1Rules.validateGradePointAverage(event.value),
                        )
                    }
                }

                OnboardingStep1Event.SchoolPickerClicked -> {
                    openSchoolPicker()
                }

                OnboardingStep1Event.GraduationDatePickerClicked -> {
                    openGraduationPicker()
                }

                OnboardingStep1Event.BackClicked -> {
                    Unit
                }

                OnboardingStep1Event.NextClicked -> {
                    submitStep1()
                }
            }
        }

        public fun onSchoolPickerEvent(event: SchoolPickerEvent) {
            when (event) {
                is SchoolPickerEvent.QueryChanged -> {
                    _uiState.update { state ->
                        state.copy(schoolPicker = SchoolPickerState(query = event.value, results = SchoolCatalog.search(event.value)))
                    }
                }

                is SchoolPickerEvent.SchoolSelected -> {
                    _uiState.update { state -> state.withSchool(event.school) }
                }

                SchoolPickerEvent.DirectInputRequested -> {
                    updateSchoolPicker { copy(directInput = SchoolDirectInputState(value = SchoolNameRules.normalize(query))) }
                }

                is SchoolPickerEvent.DirectInputChanged -> {
                    updateSchoolPicker {
                        copy(
                            directInput =
                                SchoolDirectInputState(
                                    value = event.value,
                                    error = OnboardingStep1Rules.validateSchool(event.value, requireValue = false),
                                ),
                        )
                    }
                }

                SchoolPickerEvent.DirectInputConfirmed -> {
                    confirmSchoolDirectInput()
                }

                SchoolPickerEvent.DirectInputCancelled -> {
                    updateSchoolPicker { copy(directInput = null) }
                }

                SchoolPickerEvent.Dismissed -> {
                    _uiState.update { it.copy(schoolPicker = null) }
                }
            }
        }

        /**
         * 직접 입력한 학교를 확정한다 — 목록 선택과 같은 자리로 들어간다.
         *
         * 확정 시점에만 필수 여부를 따진다. 입력 도중에 「필수 입력이에요」 를 띄우면 첫 글자를 치기도
         * 전에 빨간 칸이 된다.
         */
        private fun confirmSchoolDirectInput() {
            _uiState.update { state ->
                val input = state.schoolPicker?.directInput ?: return@update state
                val error = OnboardingStep1Rules.validateSchool(input.value, requireValue = true)
                if (error != null) {
                    state.copy(schoolPicker = state.schoolPicker.copy(directInput = input.copy(error = error)))
                } else {
                    state.withSchool(input.value)
                }
            }
        }

        /** 학교를 정하고 시트를 닫는다. 목록 값·직접 입력값 모두 같은 규칙으로 다듬어 담는다. */
        private fun OnboardingFlowState.withSchool(school: String): OnboardingFlowState =
            copy(
                step1 = step1.copy(school = SchoolNameRules.normalize(school), schoolError = null),
                schoolPicker = null,
            )

        private inline fun updateSchoolPicker(transform: SchoolPickerState.() -> SchoolPickerState) {
            _uiState.update { state ->
                val picker = state.schoolPicker ?: return@update state
                state.copy(schoolPicker = picker.transform())
            }
        }

        public fun onGraduationPickerEvent(event: GraduationDatePickerEvent) {
            when (event) {
                is GraduationDatePickerEvent.YearSelected -> {
                    updateGraduationPicker { copy(selectedYear = event.year) }
                }

                is GraduationDatePickerEvent.MonthSelected -> {
                    updateGraduationPicker { copy(selectedMonth = event.month) }
                }

                GraduationDatePickerEvent.Confirmed -> {
                    _uiState.update { state ->
                        val picker = state.graduationPicker ?: return@update state
                        state.copy(
                            step1 =
                                state.step1.copy(
                                    graduationDate = formatGraduationDate(picker.selectedYear, picker.selectedMonth),
                                    graduationDateError = null,
                                ),
                            graduationPicker = null,
                        )
                    }
                }

                GraduationDatePickerEvent.Dismissed -> {
                    _uiState.update { it.copy(graduationPicker = null) }
                }
            }
        }

        private fun openSchoolPicker() {
            if (!_uiState.value.isInputEnabled) return
            _uiState.update { it.copy(schoolPicker = SchoolPickerState(query = "", results = SchoolCatalog.search(""))) }
        }

        private fun openGraduationPicker() {
            if (!_uiState.value.isInputEnabled) return
            val currentYear = Year.now().value
            val years = (MIN_GRADUATION_YEAR..currentYear + GRADUATION_YEARS_AHEAD).toList()
            val typedYear = OnboardingStep1Rules.parseGraduationYear(_uiState.value.step1.graduationDate)
            val selectedYear = typedYear?.takeIf { it in years } ?: currentYear
            val selectedMonth = parseGraduationMonth(_uiState.value.step1.graduationDate) ?: DEFAULT_GRADUATION_MONTH
            _uiState.update {
                it.copy(graduationPicker = GraduationPickerState(years = years, selectedYear = selectedYear, selectedMonth = selectedMonth))
            }
        }

        private fun submitStep1() {
            val state = _uiState.value
            if (!state.isInputEnabled) return
            val form = state.step1
            val validated =
                form.copy(
                    nameError = OnboardingStep1Rules.validateText(form.name, OnboardingStep1Rules.MAX_NAME_LENGTH, requireValue = true),
                    schoolError = OnboardingStep1Rules.validateSchool(form.school, requireValue = true),
                    majorError = OnboardingStep1Rules.validateText(form.major, OnboardingStep1Rules.MAX_MAJOR_LENGTH, requireValue = true),
                    gradePointAverageError = OnboardingStep1Rules.validateGradePointAverage(form.gradePointAverage),
                    graduationDateError = OnboardingStep1Rules.validateGraduationDate(form.graduationDate),
                )
            if (validated.hasErrors) {
                _uiState.update { it.copy(step1 = validated) }
                return
            }
            _uiState.update { it.copy(step1 = validated, isSubmitting = true, failure = null) }
            viewModelScope.launch {
                saveBasicInfo(
                    name = validated.name.trim(),
                    school = SchoolNameRules.normalize(validated.school),
                    department = validated.major.trim(),
                    gpa = OnboardingStep1Rules.parseGradePointAverage(validated.gradePointAverage),
                    gradYear = OnboardingStep1Rules.parseGraduationYear(validated.graduationDate),
                ).onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, userName = validated.name.trim()) }
                    moveToStep(OnboardingStep.JobPreference)
                }.onFailure { throwable -> fail(OnboardingFailureStage.SaveBasicInfo, throwable) }
            }
        }

        // ---- Step 2 ----

        public fun onStep2Event(event: OnboardingStep2Event) {
            when (event) {
                is OnboardingStep2Event.JobSelectionToggled -> toggleJob(event.jobId)
                is OnboardingStep2Event.InterestInputChanged -> updateStep2 { copy(interestInput = event.value) }
                OnboardingStep2Event.InterestTagSubmitted -> submitInterestTag()
                is OnboardingStep2Event.InterestTagRemoved -> updateStep2 { copy(interestTags = interestTags - event.tag) }
                OnboardingStep2Event.BackClicked -> Unit
                OnboardingStep2Event.NextClicked -> submitStep2()
            }
        }

        private fun toggleJob(code: String) {
            if (!JobOptionCatalog.contains(code)) return
            updateStep2 {
                when {
                    code in selectedJobCodes -> copy(selectedJobCodes = selectedJobCodes - code)
                    selectedJobCodes.size < MAX_JOB_INTERESTS -> copy(selectedJobCodes = selectedJobCodes + code)
                    else -> this
                }
            }
        }

        private fun submitInterestTag() {
            val form = _uiState.value.step2
            val tag = normalizeInterestTag(form.interestInput)
            if (tag.isEmpty()) return
            if (tag in form.interestTags) {
                updateStep2 { copy(interestInput = "") }
                return
            }
            if (form.interestTags.size >= MAX_PROFILE_TAGS) {
                _uiState.update { it.copy(failure = OnboardingFailureReason.LimitExceeded) }
                return
            }
            updateStep2 { copy(interestInput = "", interestTags = interestTags + tag) }
        }

        private fun submitStep2() {
            val state = _uiState.value
            val form = state.step2
            if (!state.isInputEnabled || form.selectedJobCodes.isEmpty() || form.interestTags.isEmpty()) return
            _uiState.update { it.copy(isSubmitting = true, failure = null) }
            viewModelScope.launch {
                saveJobPreferences(jobCodes = form.selectedJobCodes, tags = form.interestTags)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        moveToStep(OnboardingStep.Experience)
                    }.onFailure { throwable -> fail(OnboardingFailureStage.SaveJobPreferences, throwable) }
            }
        }

        // ---- Step 3 ----

        public fun onStep3Event(event: OnboardingStep3Event) {
            when (event) {
                is OnboardingStep3Event.ExperienceTypeSelected -> {
                    ExperienceType.fromWireValue(event.typeId)?.let { type -> updateStep3 { copy(selectedType = type) } }
                }

                is OnboardingStep3Event.ExperienceSelected -> {
                    openExperienceEditor(event.experienceId)
                }

                is OnboardingStep3Event.ExperienceDeleteClicked -> {
                    askExperienceDeletion(event.experienceId)
                }

                OnboardingStep3Event.AddExperienceClicked -> {
                    openExperienceEditor()
                }

                OnboardingStep3Event.BackClicked -> {
                    Unit
                }

                OnboardingStep3Event.NextClicked -> {
                    submitStep3()
                }
            }
        }

        private fun loadExperiences() {
            if (_uiState.value.step3.isLoaded) return
            viewModelScope.launch {
                getOnboardingExperiences()
                    .onSuccess { experiences -> updateStep3 { copy(experiences = experiences, isLoaded = true) } }
                    .onFailure { throwable -> report(OnboardingFailureStage.LoadExperiences, throwable) }
            }
        }

        /** 신규 등록. 상한(F1-3, 30개)에 닿았으면 열지 않고 사유만 알린다 — 하나를 지우면 다시 열린다. */
        private fun openExperienceEditor() {
            val state = _uiState.value
            if (!state.isInputEnabled) return
            if (state.step3.experiences.size >= MAX_EXPERIENCE_CARDS) {
                _uiState.update { it.copy(failure = OnboardingFailureReason.LimitExceeded) }
                return
            }
            _uiState.update { it.copy(experienceEditor = ExperienceEditorState(type = state.step3.selectedType)) }
        }

        /** 기존 카드 수정. 시트를 그 카드의 값으로 채우고 유형은 잠근다. */
        private fun openExperienceEditor(experienceId: String) {
            val state = _uiState.value
            if (!state.isInputEnabled) return
            val experience = state.step3.experiences.firstOrNull { it.id.toString() == experienceId } ?: return
            _uiState.update { it.copy(experienceEditor = experience.toEditorState()) }
        }

        private fun askExperienceDeletion(experienceId: String) {
            val state = _uiState.value
            if (!state.isInputEnabled) return
            val experience = state.step3.experiences.firstOrNull { it.id.toString() == experienceId } ?: return
            _uiState.update { it.copy(experienceDelete = ExperienceDeleteState(experienceId = experience.id, title = experience.title)) }
        }

        public fun onExperienceDeleteEvent(event: ExperienceDeleteEvent) {
            when (event) {
                ExperienceDeleteEvent.Confirmed -> confirmExperienceDeletion()
                ExperienceDeleteEvent.Dismissed -> _uiState.update { it.copy(experienceDelete = null) }
            }
        }

        /**
         * 삭제를 낙관적으로 반영한다 — 다이얼로그를 닫으면서 목록에서 먼저 뺀다.
         *
         * 실패하면 원래 자리에 되돌리고 사유를 알린다. 자리를 기억하는 이유는 목록이 최신 등록순이라
         * 맨 뒤에 붙이면 순서가 흐트러지기 때문이다.
         */
        private fun confirmExperienceDeletion() {
            val pending = _uiState.value.experienceDelete ?: return
            val index =
                _uiState.value.step3.experiences
                    .indexOfFirst { it.id == pending.experienceId }
            if (index < 0) {
                _uiState.update { it.copy(experienceDelete = null) }
                return
            }
            val removed = _uiState.value.step3.experiences[index]
            _uiState.update { it.copy(experienceDelete = null) }
            updateStep3 { copy(experiences = experiences.filterNot { it.id == removed.id }) }
            viewModelScope.launch {
                deleteExperience(removed.id)
                    .onFailure { throwable ->
                        report(OnboardingFailureStage.DeleteExperience, throwable)
                        updateStep3 { restore(removed, index) }
                        _uiState.update { it.copy(failure = throwable.toOnboardingFailureReason()) }
                    }
            }
        }

        public fun onExperienceEditorEvent(event: ExperienceQuickAddEvent) {
            when (event) {
                is ExperienceQuickAddEvent.TypeSelected -> {
                    // 수정 중에는 유형을 바꾸지 않는다 — 유형마다 필드 의미가 달라 채운 값이 다른 뜻으로 저장된다.
                    //
                    // 신규 등록에서 유형을 바꿔도 이미 친 값은 **지우지 않는다**. 새 유형이 쓰지 않는 값은
                    // `toDraft()` 가 표를 보고 읽지 않아 저장에서 빠지므로, 잘못 누른 칩을 되돌린 사용자만
                    // 이득을 본다. 오류 표시만 새 유형 기준으로 다시 계산하도록 비운다.
                    updateExperienceEditor {
                        if (isEditing) {
                            this
                        } else {
                            copy(
                                type = event.type,
                                startDateError = null,
                                endDateError = null,
                                primaryError = null,
                                secondaryError = null,
                                techInputError = null,
                                linkError = null,
                                detailError = null,
                            )
                        }
                    }
                }

                is ExperienceQuickAddEvent.TitleChanged -> {
                    updateExperienceEditor { copy(title = event.value, titleError = null) }
                }

                is ExperienceQuickAddEvent.StartDateChanged -> {
                    updateExperienceEditor { copy(startDate = event.value, startDateError = null) }
                }

                is ExperienceQuickAddEvent.EndDateChanged -> {
                    updateExperienceEditor { copy(endDate = event.value, endDateError = null) }
                }

                is ExperienceQuickAddEvent.PrimaryChanged -> {
                    updateExperienceEditor { copy(primary = event.value, primaryError = null) }
                }

                is ExperienceQuickAddEvent.SecondaryChanged -> {
                    updateExperienceEditor { copy(secondary = event.value, secondaryError = null) }
                }

                is ExperienceQuickAddEvent.TechInputChanged -> {
                    updateExperienceEditor { copy(techInput = event.value, techInputError = null) }
                }

                ExperienceQuickAddEvent.TechTagSubmitted -> {
                    updateExperienceEditor { withTechTagCommitted() }
                }

                is ExperienceQuickAddEvent.TechTagRemoved -> {
                    updateExperienceEditor { copy(techs = techs.filterNot { it == event.tag }, techInputError = null) }
                }

                is ExperienceQuickAddEvent.LinkChanged -> {
                    updateExperienceEditor { copy(link = event.value, linkError = null) }
                }

                is ExperienceQuickAddEvent.DetailChanged -> {
                    updateExperienceEditor { copy(detail = event.value, detailError = null) }
                }

                ExperienceQuickAddEvent.DetailSectionToggled -> {
                    updateExperienceEditor { copy(isDetailExpanded = !isDetailExpanded) }
                }

                ExperienceQuickAddEvent.Submitted -> {
                    submitExperience()
                }

                ExperienceQuickAddEvent.Dismissed -> {
                    _uiState.update { it.copy(experienceEditor = null) }
                }
            }
        }

        private fun submitExperience() {
            val editor = _uiState.value.experienceEditor ?: return
            if (editor.isSubmitting) return
            // 입력칸에 남은 기술 이름을 먼저 태그로 확정한다 — 「Kotlin」을 치고 완료 대신 바로 추가하기를 누른
            // 사용자가 그 글자를 조용히 잃지 않게.
            val validated = validateExperienceEditor(editor.withTechTagCommitted())
            if (validated.hasErrors) {
                _uiState.update { it.copy(experienceEditor = validated) }
                return
            }
            val editingId = validated.experienceId
            val draft = validated.toDraft()
            _uiState.update { it.copy(experienceEditor = validated.copy(isSubmitting = true), failure = null) }
            val stage = if (editingId == null) OnboardingFailureStage.AddExperience else OnboardingFailureStage.UpdateExperience
            viewModelScope.launch {
                val result = if (editingId == null) addExperience(draft) else updateExperience(editingId, draft)
                result
                    .onSuccess { saved ->
                        _uiState.update { state ->
                            state.copy(
                                step3 = state.step3.upsert(saved, isNew = editingId == null),
                                experienceEditor = null,
                            )
                        }
                    }.onFailure { throwable ->
                        report(stage, throwable)
                        _uiState.update { state ->
                            state.copy(
                                experienceEditor = state.experienceEditor?.copy(isSubmitting = false),
                                failure = throwable.toOnboardingFailureReason(),
                            )
                        }
                    }
            }
        }

        private fun submitStep3() {
            if (!_uiState.value.isInputEnabled) return
            _uiState.update { it.copy(isSubmitting = true, failure = null) }
            viewModelScope.launch {
                proceedToPastApplication()
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        moveToStep(OnboardingStep.PastApplication)
                    }.onFailure { throwable -> fail(OnboardingFailureStage.ProceedToPastApplication, throwable) }
            }
        }

        // ---- Step 4 ----

        public fun onStep4Event(event: OnboardingStep4Event) {
            when (event) {
                // 파일 선택기는 Entry 가 연다 — 결과는 onFileSelected / onFileSelectionFailed 로 들어온다.
                OnboardingStep4Event.UploadClicked -> Unit

                OnboardingStep4Event.DirectInputClicked -> openDirectInput()

                is OnboardingStep4Event.DocumentMenuClicked -> deleteDocument(event.documentId)

                is OnboardingStep4Event.DocumentRetryClicked -> retryUpload(event.documentId)

                is OnboardingStep4Event.DocumentExpandToggled -> toggleDocumentItems(event.documentId)

                is OnboardingStep4Event.ItemCategoryClicked -> openItemCategoryPicker(event.documentId, event.itemId)

                OnboardingStep4Event.BackClicked -> Unit

                OnboardingStep4Event.SkipClicked -> finishOnboarding()

                OnboardingStep4Event.CompleteClicked -> finishOnboarding()
            }
        }

        private fun loadPastApplications() {
            if (_uiState.value.step4.isLoaded) return
            viewModelScope.launch {
                getOnboardingPastApplications()
                    .onSuccess { applications ->
                        _uiState.update { state ->
                            val remote = applications.take(MAX_PAST_APPLICATIONS).map(::toRemoteDocument)
                            val local = state.step4.documents.filter { it.remoteId == null }
                            state.copy(
                                step4 = OnboardingStep4FormState(documents = (remote + local).take(MAX_PAST_APPLICATIONS), isLoaded = true),
                            )
                        }
                    }.onFailure { throwable -> report(OnboardingFailureStage.LoadPastApplications, throwable) }
            }
        }

        /**
         * Entry 가 파일 선택기에서 읽어 만든 [UploadFile].
         *
         * 바로 올리지 않고 라벨 시트를 먼저 연다 — 서버에 라벨 수정 엔드포인트가 없어 이때가 사용자가 이름을
         * 정할 수 있는 유일한 시점이다(F1-4). 상한은 시트를 열기 전에 본다: 어차피 못 올릴 파일에 이름을
         * 붙이게 두지 않는다.
         */
        public fun onFileSelected(file: UploadFile) {
            if (!_uiState.value.isInputEnabled) return
            if (_uiState.value.step4.documents.size >= MAX_PAST_APPLICATIONS) {
                _uiState.update { it.copy(failure = OnboardingFailureReason.LimitExceeded) }
                return
            }
            val label = draft.restoredUploadLabel(PastApplicationLabelRules.defaultLabelFor(file.fileName))
            _uiState.update { it.copy(uploadLabel = UploadLabelState(file = file, label = label)) }
        }

        public fun onUploadLabelEvent(event: UploadLabelEvent) {
            when (event) {
                is UploadLabelEvent.LabelChanged -> {
                    updateUploadLabel { copy(label = event.value, labelError = null) }
                }

                UploadLabelEvent.Submitted -> {
                    submitUploadLabel()
                }

                // 취소는 고른 파일을 버린다 — 목록에도, 초안에도 흔적을 남기지 않는다.
                UploadLabelEvent.Dismissed -> {
                    draft.clearUploadLabel()
                    _uiState.update { it.copy(uploadLabel = null) }
                }
            }
        }

        private fun submitUploadLabel() {
            val sheet = _uiState.value.uploadLabel ?: return
            val labelError = PastApplicationLabelRules.validate(sheet.label)
            if (labelError != null) {
                _uiState.update { it.copy(uploadLabel = sheet.copy(labelError = labelError)) }
                return
            }
            draft.clearUploadLabel()
            _uiState.update { it.copy(uploadLabel = null) }
            enqueueUpload(label = PastApplicationLabelRules.normalize(sheet.label), file = sheet.file)
        }

        /** 파일을 [UploadFile] 로 만들지 못했다(지원하지 않는 형식·크기 초과·읽기 실패). */
        public fun onFileSelectionFailed(
            reason: OnboardingFailureReason,
            cause: Throwable,
        ) {
            report(OnboardingFailureStage.UploadPastApplication, cause)
            _uiState.update { it.copy(failure = reason) }
        }

        private fun enqueueUpload(
            label: String,
            file: UploadFile,
        ) {
            val documents = _uiState.value.step4.documents
            if (documents.size >= MAX_PAST_APPLICATIONS) {
                _uiState.update { it.copy(failure = OnboardingFailureReason.LimitExceeded) }
                return
            }
            val document =
                OnboardingUploadDocument(
                    id = "local-${nextLocalDocumentId++}",
                    remoteId = null,
                    label = label,
                    sizeBytes = file.sizeBytes,
                    status = OnboardingUploadStatus.Processing,
                    file = file,
                )
            updateStep4 { copy(documents = documents + document) }
            upload(document)
        }

        private fun upload(document: OnboardingUploadDocument) {
            val file = document.file ?: return
            viewModelScope.launch {
                uploadPastApplication(file = file, label = document.label)
                    .onSuccess { application ->
                        val stillListed =
                            _uiState.value.step4.documents
                                .any { it.id == document.id }
                        if (!stillListed) {
                            // 업로드 중 사용자가 지운 문서 — 서버에 남은 사본을 best-effort 로 정리한다.
                            deletePastApplication(application.id)
                            return@onSuccess
                        }
                        replaceDocument(document.id) {
                            copy(remoteId = application.id, status = OnboardingUploadStatus.Completed(application.items))
                        }
                    }.onFailure { throwable ->
                        report(OnboardingFailureStage.UploadPastApplication, throwable)
                        replaceDocument(document.id) { copy(status = OnboardingUploadStatus.Failed(throwable.toOnboardingFailureReason())) }
                    }
            }
        }

        private fun retryUpload(documentId: String) {
            val document =
                _uiState.value.step4.documents
                    .firstOrNull { it.id == documentId } ?: return
            if (document.status !is OnboardingUploadStatus.Failed || document.file == null) return
            replaceDocument(documentId) { copy(status = OnboardingUploadStatus.Processing) }
            upload(document)
        }

        private fun deleteDocument(documentId: String) {
            val document =
                _uiState.value.step4.documents
                    .firstOrNull { it.id == documentId } ?: return
            val remoteId = document.remoteId
            if (remoteId == null) {
                updateStep4 { removeDocument(documentId) }
                return
            }
            viewModelScope.launch {
                deletePastApplication(remoteId)
                    .onSuccess { updateStep4 { removeDocument(documentId) } }
                    .onFailure { throwable ->
                        report(OnboardingFailureStage.DeletePastApplication, throwable)
                        _uiState.update { it.copy(failure = throwable.toOnboardingFailureReason()) }
                    }
            }
        }

        /** 분류 항목 목록 펼침/접기. 한 번에 하나만 펼쳐 아래 액션이 멀리 밀리지 않게 한다. */
        private fun toggleDocumentItems(documentId: String) {
            val document =
                _uiState.value.step4.documents
                    .firstOrNull { it.id == documentId } ?: return
            val items = (document.status as? OnboardingUploadStatus.Completed)?.items.orEmpty()
            if (items.isEmpty()) return
            updateStep4 { copy(expandedDocumentId = documentId.takeIf { it != expandedDocumentId }) }
        }

        private fun openItemCategoryPicker(
            documentId: String,
            itemId: Long,
        ) {
            if (!_uiState.value.isInputEnabled) return
            val item = findItem(documentId, itemId) ?: return
            _uiState.update {
                it.copy(
                    itemCategoryPicker =
                        PastApplicationItemCategoryState(
                            documentId = documentId,
                            itemId = itemId,
                            contentPreview = item.content,
                            selected = item.category,
                        ),
                )
            }
        }

        public fun onItemCategoryPickerEvent(event: PastApplicationItemCategoryEvent) {
            when (event) {
                is PastApplicationItemCategoryEvent.CategorySelected -> submitItemCategory(event.category)
                PastApplicationItemCategoryEvent.Dismissed -> _uiState.update { it.copy(itemCategoryPicker = null) }
            }
        }

        /**
         * 분류 조정을 낙관적으로 반영한다 — 목록에 바로 새 분류를 그리고 시트를 닫는다.
         *
         * 실패하면 조정 전 항목으로 되돌리고 사유를 알린다. 성공하면 서버가 돌려준 항목으로 다시 맞춘다
         * (서버가 `confident` 를 어떻게 판정하는지는 서버 몫이다).
         */
        private fun submitItemCategory(category: PastApplicationCategory) {
            val picker = _uiState.value.itemCategoryPicker ?: return
            _uiState.update { it.copy(itemCategoryPicker = null) }
            val document =
                _uiState.value.step4.documents
                    .firstOrNull { it.id == picker.documentId } ?: return
            val remoteId = document.remoteId ?: return
            val previous = findItem(picker.documentId, picker.itemId) ?: return
            if (previous.category == category) return
            replaceItem(picker.documentId, previous.copy(category = category, confident = true))
            viewModelScope.launch {
                updatePastApplicationItemCategory(applicationId = remoteId, itemId = picker.itemId, category = category)
                    .onSuccess { updated -> replaceItem(picker.documentId, updated) }
                    .onFailure { throwable ->
                        report(OnboardingFailureStage.UpdatePastApplicationItemCategory, throwable)
                        replaceItem(picker.documentId, previous)
                        _uiState.update { it.copy(failure = throwable.toOnboardingFailureReason()) }
                    }
            }
        }

        private fun findItem(
            documentId: String,
            itemId: Long,
        ): PastApplicationItem? {
            val status =
                _uiState.value.step4.documents
                    .firstOrNull { it.id == documentId }
                    ?.status
            return (status as? OnboardingUploadStatus.Completed)?.items?.firstOrNull { it.id == itemId }
        }

        /** 프로세스가 죽어 닫힌 시트는 저절로 다시 열지 않는다 — 대신 다시 열면 쓰던 글이 그대로 있다(#133). */
        private fun openDirectInput() {
            if (!_uiState.value.isInputEnabled) return
            _uiState.update { it.copy(directInput = draft.restoredDirectInput()) }
        }

        public fun onDirectInputEvent(event: DirectInputEvent) {
            when (event) {
                is DirectInputEvent.LabelChanged -> {
                    updateDirectInput { copy(label = event.value, labelError = null) }
                }

                is DirectInputEvent.ContentChanged -> {
                    updateDirectInput { copy(content = event.value, contentError = null) }
                }

                DirectInputEvent.Submitted -> {
                    submitDirectInput()
                }

                // 취소는 쓰던 글을 버리겠다는 뜻이다 — 초안도 함께 지워야 다음에 열었을 때 되살아나지 않는다.
                DirectInputEvent.Dismissed -> {
                    draft.clearDirectInput()
                    _uiState.update { it.copy(directInput = null) }
                }
            }
        }

        private fun submitDirectInput() {
            val input = _uiState.value.directInput ?: return
            val label = PastApplicationLabelRules.normalize(input.label)
            val validated =
                input.copy(
                    labelError = PastApplicationLabelRules.validate(input.label),
                    contentError = if (input.content.isBlank()) OnboardingFieldError.Required else null,
                )
            if (validated.labelError != null || validated.contentError != null) {
                _uiState.update { it.copy(directInput = validated) }
                return
            }
            val bytes = input.content.toByteArray(Charsets.UTF_8)
            if (bytes.size > MAX_PAST_APPLICATION_FILE_BYTES) {
                _uiState.update { it.copy(failure = OnboardingFailureReason.FileTooLarge) }
                return
            }
            val file =
                UploadFile(
                    fileName = "${label.replace('/', ' ')}.${PastApplicationFileFormat.Txt.extension}",
                    sizeBytes = bytes.size.toLong(),
                ) { ByteArrayInputStream(bytes) }
            draft.clearDirectInput()
            _uiState.update { it.copy(directInput = null) }
            enqueueUpload(label = label, file = file)
        }

        private fun finishOnboarding() {
            if (!_uiState.value.isInputEnabled) return
            _uiState.update { it.copy(isSubmitting = true, failure = null) }
            viewModelScope.launch {
                completeOnboarding()
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        navigateTo(OnboardingDestination.Complete)
                    }.onFailure { throwable -> fail(OnboardingFailureStage.Complete, throwable) }
            }
        }

        // ---- 완료 ----

        public fun onCompleteEvent(event: OnboardingCompleteEvent) {
            when (event) {
                OnboardingCompleteEvent.ViewFeedClicked -> navigateTo(OnboardingDestination.Feed)
                OnboardingCompleteEvent.RegisterBoardClicked -> navigateTo(OnboardingDestination.BoardRegister)
            }
        }

        // ---- 단발 신호 소비 ----

        public fun onNavigationConsumed() {
            _uiState.update { it.copy(pendingNavigation = null) }
        }

        public fun onFailureConsumed() {
            _uiState.update { it.copy(failure = null) }
        }

        // ---- 내부 도우미 ----

        private fun navigateTo(destination: OnboardingDestination) {
            _uiState.update { it.copy(pendingNavigation = destination) }
        }

        private fun fail(
            stage: OnboardingFailureStage,
            throwable: Throwable,
        ) {
            report(stage, throwable)
            _uiState.update { it.copy(isSubmitting = false, failure = throwable.toOnboardingFailureReason()) }
        }

        private fun report(
            stage: OnboardingFailureStage,
            throwable: Throwable,
        ) {
            errorReporter.recordOnboardingFailure(stage, throwable)
        }

        private inline fun updateStep1(transform: OnboardingStep1FormState.() -> OnboardingStep1FormState) {
            _uiState.update { it.copy(step1 = it.step1.transform()) }
        }

        private inline fun updateStep2(transform: OnboardingStep2FormState.() -> OnboardingStep2FormState) {
            _uiState.update { it.copy(step2 = it.step2.transform()) }
        }

        private inline fun updateStep3(transform: OnboardingStep3FormState.() -> OnboardingStep3FormState) {
            _uiState.update { it.copy(step3 = it.step3.transform()) }
        }

        private inline fun updateStep4(transform: OnboardingStep4FormState.() -> OnboardingStep4FormState) {
            _uiState.update { it.copy(step4 = it.step4.transform()) }
        }

        private inline fun updateGraduationPicker(transform: GraduationPickerState.() -> GraduationPickerState) {
            _uiState.update { it.copy(graduationPicker = it.graduationPicker?.transform()) }
        }

        private inline fun updateExperienceEditor(transform: ExperienceEditorState.() -> ExperienceEditorState) {
            _uiState.update { it.copy(experienceEditor = it.experienceEditor?.transform()) }
        }

        private inline fun updateDirectInput(transform: DirectInputState.() -> DirectInputState) {
            _uiState.update { it.copy(directInput = it.directInput?.transform()) }
        }

        private inline fun updateUploadLabel(transform: UploadLabelState.() -> UploadLabelState) {
            _uiState.update { it.copy(uploadLabel = it.uploadLabel?.transform()) }
        }

        private inline fun replaceDocument(
            documentId: String,
            transform: OnboardingUploadDocument.() -> OnboardingUploadDocument,
        ) {
            updateStep4 { copy(documents = documents.map { if (it.id == documentId) it.transform() else it }) }
        }

        /** 분류가 끝난 문서의 항목 하나만 갈아 끼운다 — 다른 문서·항목은 그대로 둔다. */
        private fun replaceItem(
            documentId: String,
            item: PastApplicationItem,
        ) {
            replaceDocument(documentId) {
                val status = status as? OnboardingUploadStatus.Completed ?: return@replaceDocument this
                copy(status = OnboardingUploadStatus.Completed(status.items.map { if (it.id == item.id) item else it }))
            }
        }

        private companion object {
            const val GRADUATION_YEARS_AHEAD = 6

            /** 국내 대학 졸업은 대개 2월이라 피커의 기본 월로 둔다. */
            const val DEFAULT_GRADUATION_MONTH = 2
        }
    }

private fun OnboardingStep1FormState.prefill(profile: UserProfile?): OnboardingStep1FormState {
    if (profile == null) return this
    return copy(
        name = profile.name ?: name,
        school = profile.school ?: school,
        major = profile.department ?: major,
        gradePointAverage = profile.gpa?.toString() ?: gradePointAverage,
        graduationDate = profile.gradYear?.toString() ?: graduationDate,
    )
}

/**
 * 서버가 아는 값이 이긴다. 서버가 아직 모르는 목록(이 Step 의 「다음」을 누르기 전이다)에서만 살아난 초안이 남는다
 * — Step 1 의 `?:` 와 같은 규칙이다. 빈 목록으로 덮으면 프로세스가 죽기 직전에 고른 직무·태그가 그대로 지워진다.
 *
 * 등록 전의 태그 입력칸(`interestInput`)은 서버에 대응하는 값이 없어 언제나 초안이 남는다.
 */
private fun OnboardingStep2FormState.prefill(profile: UserProfile?): OnboardingStep2FormState {
    if (profile == null) return this
    val codes =
        profile.jobInterests
            .sortedBy { it.priority }
            .map { it.code }
            .filter(JobOptionCatalog::contains)
            .distinct()
            .take(MAX_JOB_INTERESTS)
    val tags = profile.tags.distinct().take(MAX_PROFILE_TAGS)
    return copy(
        selectedJobCodes = codes.ifEmpty { selectedJobCodes },
        interestTags = tags.ifEmpty { interestTags },
    )
}

/** 새 카드는 맨 앞(최신 등록순), 수정한 카드는 있던 자리에 그대로 둔다. */
private fun OnboardingStep3FormState.upsert(
    saved: Experience,
    isNew: Boolean,
): OnboardingStep3FormState =
    if (isNew) {
        copy(selectedType = saved.type, experiences = listOf(saved) + experiences.filterNot { it.id == saved.id })
    } else {
        copy(experiences = experiences.map { if (it.id == saved.id) saved else it })
    }

/** 삭제가 실패한 카드를 원래 자리에 되돌린다. 그 사이 목록이 짧아졌으면 끝에 붙인다. */
private fun OnboardingStep3FormState.restore(
    experience: Experience,
    index: Int,
): OnboardingStep3FormState =
    if (experiences.any { it.id == experience.id }) {
        this
    } else {
        copy(experiences = experiences.toMutableList().apply { add(index.coerceAtMost(size), experience) })
    }

/**
 * 등록된 카드를 시트의 값으로 되돌린다 — [toDraft] 의 역방향이다.
 *
 * ### 「시트가 모르는 필드를 지우지 않는다」를 이제 무엇이 지키는가 (#139)
 * 예전 시트는 공통 5필드만 받아, 수정 저장이 기술 태그·링크·요약을 지우지 않도록 원본에서 물려받는
 * `preserving()` 를 따로 뒀다. #139 로 시트가 `ExperienceDetails` 의 **전 필드**를 받게 되면서 물려받을
 * 대상이 사라졌고, 손으로 쓴 유형별 물려받기는 그대로 두면 새 필드가 생겼을 때 조용히 낡는 죽은 코드가 된다.
 * 그래서 물려받기를 걷어내고 계약을 **이 함수와 [toDraft] 의 왕복이 무손실**이라는 한 가지로 좁혔다 —
 * `OnboardingViewModelTest` 의 왕복 테스트가 다섯 유형 전부에 대해 그 등식을 고정한다.
 *
 * 상세 값이 하나라도 있으면 [ExperienceEditorState.isDetailExpanded] 를 켜서 펼친 채로 연다. 접힌 채 열면
 * 사용자는 그 값이 사라졌다고 읽는다.
 *
 * ### 시점 칸은 그 카드가 아는 정밀도 그대로 연다 (#166 · #207)
 * 연도만 있는 수상 카드를 「2025.01」로 열면, 사용자가 준 적 없는 1월이 화면에 뜨고 저장에 실린다.
 * 이제 시점의 정밀도는 카드가 값으로 들고 있으므로(`ExperiencePoint`), 유형별로 어느 필드를 먼저 볼지
 * 따지지 않고 **그 값이 아는 만큼** 그린다 — 연이면 「2025」, 연월 이상이면 「2025.06」.
 *
 * ### 칸이 담지 못하는 일(day)은 원본째로 들고 간다 (#171)
 * 시점 칸은 `YYYY.MM` 이라 `2025-06-15` 를 「2025.06」으로밖에 못 그린다. 그 글만 들고 저장하면 사용자가
 * 손대지도 않은 일이 1일로 깎이므로, 원본 시점을 [ExperienceEditorState.startDateOrigin]·
 * [ExperienceEditorState.endDateOrigin] 에 함께 실어 [toDraft] 가 되돌릴 수 있게 한다.
 */
internal fun Experience.toEditorState(): ExperienceEditorState {
    val details = details
    val start = startPoint?.toEditorText().orEmpty()
    val primary =
        when (details) {
            is ExperienceDetails.Project -> details.role
            is ExperienceDetails.Award -> details.rank
            is ExperienceDetails.Intern -> details.company
            is ExperienceDetails.Activity -> details.organization
            is ExperienceDetails.Certificate -> details.issuer
        }
    val secondary =
        when (details) {
            is ExperienceDetails.Project -> details.summary
            is ExperienceDetails.Award -> details.organizer
            is ExperienceDetails.Intern -> details.role
            is ExperienceDetails.Activity -> details.summary
            is ExperienceDetails.Certificate -> null
        }
    val detail =
        when (details) {
            is ExperienceDetails.Intern -> details.summary
            is ExperienceDetails.Activity -> details.role
            else -> null
        }
    // 서버에 상한을 넘는 태그가 있어도(다른 클라이언트가 만들었을 수 있다) 여기서 자르지 않는다 — 자르면
    // 제목만 고치려던 사용자가 자기도 모르게 태그를 잃는다. 상한은 「새로 더할 때」만 건다.
    val techs = (details as? ExperienceDetails.Project)?.techs.orEmpty()
    val link = (details as? ExperienceDetails.Project)?.link.orEmpty()
    return ExperienceEditorState(
        experienceId = id,
        type = type,
        title = title,
        startDate = start,
        endDate = endPoint?.toEditorText().orEmpty(),
        startDateOrigin = startPoint,
        endDateOrigin = endPoint,
        primary = primary.orEmpty(),
        secondary = secondary.orEmpty(),
        techs = techs,
        link = link,
        detail = detail.orEmpty(),
        isDetailExpanded = techs.isNotEmpty() || link.isNotEmpty() || !detail.isNullOrEmpty(),
    )
}

/**
 * 시점을 그 정밀도가 담기는 칸 글로 옮긴다 — 연 정밀도는 `2025`, 그보다 자세하면 `2025.06`.
 *
 * 연도만 아는 카드를 「2025.01」로 열면 사용자가 준 적 없는 1월이 화면에 뜨고 그대로 저장에 실린다(#166).
 * 반대로 `2025-06-15` 를 「2025.06」으로 여는 것은 칸이 일을 담지 못해서일 뿐이고, 잃은 일은
 * [ExperienceEditorState.startDateOrigin] 이 들고 있다가 되돌린다(#171).
 */
private fun ExperiencePoint.toEditorText(): String =
    when (this) {
        is ExperiencePoint.Year -> "%04d".format(year)
        is ExperiencePoint.WithMonth -> "%04d.%02d".format(year, month)
    }

/** 문서를 목록에서 빼면서 펼침 상태도 함께 정리한다 — 목록에 없는 문서를 펼친 채로 두면 상태 불변식이 깨진다. */
private fun OnboardingStep4FormState.removeDocument(documentId: String): OnboardingStep4FormState =
    copy(
        documents = documents.filterNot { it.id == documentId },
        expandedDocumentId = expandedDocumentId?.takeIf { it != documentId },
    )

private fun toRemoteDocument(application: PastApplication): OnboardingUploadDocument =
    OnboardingUploadDocument(
        id = "remote-${application.id}",
        remoteId = application.id,
        label = application.label,
        sizeBytes = null,
        status = OnboardingUploadStatus.Completed(application.items),
        file = null,
    )

internal fun formatGraduationDate(
    year: Int,
    month: Int,
): String = "%04d.%02d".format(year, month)

private fun parseGraduationMonth(value: String): Int? =
    value.trim().substringAfter('.', missingDelimiterValue = "").toIntOrNull()?.takeIf {
        it in
            1..12
    }

private val ExperienceEditorState.hasErrors: Boolean
    get() =
        listOfNotNull(
            titleError,
            startDateError,
            endDateError,
            primaryError,
            secondaryError,
            techInputError,
            linkError,
            detailError,
        ).isNotEmpty()

/**
 * 입력칸에 남은 글자를 기술 태그로 확정한다. 규칙에 걸리면 태그 대신 필드 오류를 남긴다.
 *
 * 중복은 **대소문자를 무시하고** 거른다 — `kotlin` 과 `Kotlin` 은 사람에게 같은 기술이고, 카드에 둘 다 뜨면
 * 오히려 잘못 입력한 것처럼 보인다. 먼저 친 표기를 남긴다. 상한을 넘으면 오류만 남기고 입력칸은 비우지
 * 않는다 — 태그 하나를 지우고 다시 완료를 누르면 그대로 들어간다.
 */
internal fun ExperienceEditorState.withTechTagCommitted(): ExperienceEditorState {
    if (!ExperienceEditorRules.hasTechTags(type)) return this
    val tag = ExperienceEditorRules.normalizeTechTag(techInput)
    return when {
        tag.isEmpty() -> {
            copy(techInput = "", techInputError = null)
        }

        tag.length > MAX_EXPERIENCE_TECH_TAG_LENGTH -> {
            copy(techInputError = OnboardingFieldError.TooLong(MAX_EXPERIENCE_TECH_TAG_LENGTH))
        }

        techs.any { it.equals(tag, ignoreCase = true) } -> {
            copy(techInput = "", techInputError = null)
        }

        techs.size >= MAX_EXPERIENCE_TECH_TAGS -> {
            copy(techInputError = OnboardingFieldError.OutOfRange)
        }

        else -> {
            copy(techInput = "", techs = techs + tag, techInputError = null)
        }
    }
}

/** 시트 입력을 [ExperienceEditorRules] 로 검증해 필드 오류를 채운 사본을 돌려준다. */
internal fun validateExperienceEditor(editor: ExperienceEditorState): ExperienceEditorState {
    val type = editor.type
    val start = ExperienceEditorRules.parseYearMonthPoint(editor.startDate)
    val end = ExperienceEditorRules.parseYearMonthPoint(editor.endDate)
    val titleError =
        when {
            editor.title.isBlank() -> {
                OnboardingFieldError.Required
            }

            editor.title.trim().length > ExperienceEditorRules.MAX_TITLE_LENGTH -> {
                OnboardingFieldError.TooLong(
                    ExperienceEditorRules.MAX_TITLE_LENGTH,
                )
            }

            else -> {
                null
            }
        }
    val startDateError =
        when {
            editor.startDate.isBlank() -> if (ExperienceEditorRules.isStartDateRequired(type)) OnboardingFieldError.Required else null

            // 받는 형식은 유형마다 다르다 — 수상은 연도(`YYYY`)다.
            !ExperienceEditorRules.isValidDateInput(type, editor.startDate) -> OnboardingFieldError.InvalidFormat

            else -> null
        }
    val endDateError =
        when {
            !ExperienceEditorRules.hasPeriod(type) || editor.endDate.isBlank() -> null
            end == null -> OnboardingFieldError.InvalidFormat
            start != null && end.isBefore(start) -> OnboardingFieldError.OutOfRange
            else -> null
        }
    val primaryError = validateOptionalText(editor.primary, required = ExperienceEditorRules.isPrimaryRequired(type))
    val secondaryError =
        if (ExperienceEditorRules.hasSecondary(type)) {
            validateOptionalText(editor.secondary, required = ExperienceEditorRules.isSecondaryRequired(type))
        } else {
            null
        }
    // 상세는 전부 선택 입력이다 — 비어 있는 것은 오류가 아니고, 그 유형이 안 쓰는 값은 아예 보지 않는다.
    val linkError =
        when {
            !ExperienceEditorRules.hasLink(type) || editor.link.isBlank() -> {
                null
            }

            editor.link.trim().length > MAX_EXPERIENCE_LINK_LENGTH -> {
                OnboardingFieldError.TooLong(MAX_EXPERIENCE_LINK_LENGTH)
            }

            !isAllowedExperienceLink(editor.link) -> {
                OnboardingFieldError.InvalidFormat
            }

            else -> {
                null
            }
        }
    val detailError =
        if (ExperienceEditorRules.hasDetail(type)) validateOptionalText(editor.detail, required = false) else null
    return editor.copy(
        titleError = titleError,
        startDateError = startDateError,
        endDateError = endDateError,
        primaryError = primaryError,
        secondaryError = secondaryError,
        linkError = linkError,
        detailError = detailError,
        // 접힌 영역의 오류로 제출이 막히면 사용자에게는 「버튼이 안 먹는다」로만 보인다 — 오류가 나면 펼친다.
        isDetailExpanded =
            editor.isDetailExpanded || editor.techInputError != null || linkError != null || detailError != null,
    )
}

private fun validateOptionalText(
    value: String,
    required: Boolean,
): OnboardingFieldError? =
    when {
        value.isBlank() -> if (required) OnboardingFieldError.Required else null
        value.trim().length > ExperienceEditorRules.MAX_TEXT_LENGTH -> OnboardingFieldError.TooLong(ExperienceEditorRules.MAX_TEXT_LENGTH)
        else -> null
    }

/**
 * 검증을 통과한 시트 입력을 [ExperienceDraft] 로 옮긴다 — 유형별 필드 의미는 [ExperienceEditorRules] 표를 따른다.
 *
 * 표에 없는 값은 **읽지 않는다**. 유형을 바꿔도 시트는 이전 유형에 친 글을 지우지 않으므로(칩을 잘못 눌렀다
 * 돌아온 사용자를 위해), 새 유형이 쓰지 않는 값이 서버로 새지 않는 것은 여기서 보장한다.
 *
 * ### 없는 값을 만들지 않는다 (#166 · #207)
 * 시트를 열었다 저장만 해도 없던 값이 생기면 안 된다. 걸리는 곳은 **시점 한 칸이 유형마다 다른 정밀도로 가는**
 * 수상·자격증이었다 — 연도 `2025` 를 `2025-01-01` 로, 취득 연월 `2025-06` 을 `2025-06-01` 로 넓혀 실었다.
 * 이제 넓히는 길이 아예 없다 — 수상 칸은 [ExperienceEditorRules.parseYearPoint] 로 연 정밀도 시점이 되고,
 * 모델이 그보다 자세한 시점을 수상 카드에 담지 않는다(`ExperienceType.maxPointPrecision`).
 *
 * 반대 방향인 좁히기(`YYYY.MM` → 연도)는 그대로 한다 — 예전 카드가 남긴 일자에서 연도를 읽는 것은 새 정보를
 * 만들지 않는다.
 *
 * ### 있던 값도 바꾸지 않는다 (#171)
 * #166 이 막은 것은 「없던 값이 생긴다」였고, 남아 있던 것은 그 반대편인 **「있던 값이 바뀐다」**였다 — 시점 칸이
 * 월 정밀도라 `2025-06-15` 짜리 카드를 열었다 저장만 해도 시작일이 `2025-06-01` 로 깎였다. 그래서 사용자가 그
 * 칸의 달을 바꾸지 않았으면 원본 시점을 되돌린다([ExperienceEditorRules.resolvePoint]). 이 칸은 일을 표현할
 * 수단이 없으므로, 달이 같다는 것은 「일에 대해 아무 말도 하지 않았다」는 뜻이다.
 *
 * ### 지켜 낸 일과 새로 친 달이 어긋나는 경우
 * 6월 20일 시작을 그대로 두고 종료만 6월로 당기면 「6월 20일 ~ 6월」이 된다. 예전에는 이것을 거꾸로 된 기간으로
 * 보고 지켜 낸 일을 버렸지만, 지금은 모델이 **두 시점을 더 굵은 쪽 정밀도로 견주므로**(`ExperiencePoint.isBefore`)
 * 그대로 성립한다 — 종료가 말한 것은 달까지뿐이라 20일보다 앞선다고 단정할 근거가 없다.
 */
internal fun ExperienceEditorState.toDraft(): ExperienceDraft {
    val trimmedTitle = title.trim()
    val start = resolveStartPoint()
    val end = if (ExperienceEditorRules.hasPeriod(type)) ExperienceEditorRules.resolvePoint(endDate, endDateOrigin) else null
    val primaryText = primary.trim().ifEmpty { null }
    val secondaryText = secondary.trim().ifEmpty { null }
    val linkText = if (ExperienceEditorRules.hasLink(type)) link.trim().ifEmpty { null } else null
    val detailText = if (ExperienceEditorRules.hasDetail(type)) detail.trim().ifEmpty { null } else null
    // 모델이 「공백 없음·중복 없음」을 require 로 지킨다 — 여기서 한 번 더 거른다.
    val techTags =
        if (ExperienceEditorRules.hasTechTags(type)) {
            techs.map(String::trim).filter(String::isNotEmpty).distinctBy(String::lowercase)
        } else {
            emptyList()
        }
    val details =
        when (type) {
            ExperienceType.Project -> {
                ExperienceDetails.Project(role = primaryText, techs = techTags, summary = secondaryText, link = linkText)
            }

            ExperienceType.Award -> {
                ExperienceDetails.Award(
                    contestName = trimmedTitle,
                    rank = requireNotNull(primaryText) { "award rank is required" },
                    organizer = secondaryText,
                )
            }

            ExperienceType.Intern -> {
                ExperienceDetails.Intern(
                    company = requireNotNull(primaryText) { "intern company is required" },
                    role = requireNotNull(secondaryText) { "intern role is required" },
                    summary = detailText,
                )
            }

            ExperienceType.Activity -> {
                ExperienceDetails.Activity(
                    organization = requireNotNull(primaryText) { "activity organization is required" },
                    role = detailText,
                    summary = secondaryText,
                )
            }

            ExperienceType.Certificate -> {
                ExperienceDetails.Certificate(issuer = primaryText)
            }
        }
    return ExperienceDraft(
        title = trimmedTitle,
        startPoint = start,
        endPoint = end,
        details = details,
    )
}

/**
 * 시점 칸의 글을 그 유형이 담을 수 있는 정밀도의 시점으로 읽는다.
 *
 * 수상만 칸 형식이 `YYYY` 라 따로 읽는다 — 나머지는 `YYYY.MM` 이고, 칸이 담지 못하는 정밀도는 원본에서
 * 되돌린다([ExperienceEditorRules.resolvePoint]).
 */
private fun ExperienceEditorState.resolveStartPoint(): ExperiencePoint? =
    if (type == ExperienceType.Award) {
        ExperienceEditorRules.parseYearPoint(startDate)
    } else {
        ExperienceEditorRules.resolvePoint(startDate, startDateOrigin)
    }
