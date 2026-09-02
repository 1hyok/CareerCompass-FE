package com.cambridge.feature.onboarding.presentation.flow

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
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.experience.MAX_EXPERIENCE_CARDS
import com.cambridge.core.model.user.MAX_JOB_INTERESTS
import com.cambridge.core.model.user.MAX_PROFILE_TAGS
import com.cambridge.core.model.user.MIN_GRADUATION_YEAR
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.onboarding.domain.model.JobOptionCatalog
import com.cambridge.feature.onboarding.domain.model.OnboardingProgress
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.model.SchoolCatalog
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
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

/**
 * 온보딩 Step 1~4 와 완료 화면의 상태를 한 그래프 스코프에서 소유한다 — 기능 스펙 F1-2.
 *
 * - 재개: `init` 에서 [ResolveOnboardingEntryUseCase] 로 시작 단계를 정하고 프로필 값을 프리필한다(F1-1).
 * - 단계 저장은 각 use case 가 서버 저장과 진행 기록을 함께 처리하고, 성공하면 [OnboardingDestination.Step] 을 낸다.
 * - 실패 사유는 [OnboardingFailureReason] 으로 두고 계측은 [ErrorReporter] 로 남긴다. 문구·플랫폼 의존은 Entry 몫이다.
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingFlowState())
        public val uiState: StateFlow<OnboardingFlowState> = _uiState.asStateFlow()

        private var nextLocalDocumentId = 1

        init {
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
                    _uiState.update { state ->
                        state.copy(
                            step1 = state.step1.copy(school = event.school, schoolError = null),
                            schoolPicker = null,
                        )
                    }
                }

                SchoolPickerEvent.Dismissed -> {
                    _uiState.update { it.copy(schoolPicker = null) }
                }
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
                    schoolError = if (form.school.isBlank()) OnboardingFieldError.Required else null,
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
                    school = validated.school.trim(),
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
                    updateExperienceEditor {
                        if (isEditing) {
                            this
                        } else {
                            copy(type = event.type, startDateError = null, endDateError = null, primaryError = null, secondaryError = null)
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
            val validated = validateExperienceEditor(editor)
            if (validated.hasErrors) {
                _uiState.update { it.copy(experienceEditor = validated) }
                return
            }
            val editingId = validated.experienceId
            val original =
                editingId?.let { id ->
                    _uiState.value.step3.experiences
                        .firstOrNull { it.id == id }
                }
            val edited = validated.toDraft()
            val draft = original?.let(edited::preserving) ?: edited
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

        /** Entry 가 파일 선택기에서 읽어 만든 [UploadFile]. 라벨은 파일명을 그대로 쓴다. */
        public fun onFileSelected(file: UploadFile) {
            enqueueUpload(label = file.fileName, file = file)
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

        private fun openDirectInput() {
            if (!_uiState.value.isInputEnabled) return
            _uiState.update { it.copy(directInput = DirectInputState()) }
        }

        public fun onDirectInputEvent(event: DirectInputEvent) {
            when (event) {
                is DirectInputEvent.LabelChanged -> updateDirectInput { copy(label = event.value, labelError = null) }
                is DirectInputEvent.ContentChanged -> updateDirectInput { copy(content = event.value, contentError = null) }
                DirectInputEvent.Submitted -> submitDirectInput()
                DirectInputEvent.Dismissed -> _uiState.update { it.copy(directInput = null) }
            }
        }

        private fun submitDirectInput() {
            val input = _uiState.value.directInput ?: return
            val label = input.label.trim()
            val validated =
                input.copy(
                    labelError =
                        when {
                            label.isEmpty() -> {
                                OnboardingFieldError.Required
                            }

                            label.length > DirectInputState.MAX_LABEL_LENGTH -> {
                                OnboardingFieldError.TooLong(
                                    DirectInputState.MAX_LABEL_LENGTH,
                                )
                            }

                            else -> {
                                null
                            }
                        },
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
    return copy(selectedJobCodes = codes, interestTags = tags)
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
 * 빠른 입력 5필드로 표현되지 않는 값은 원본에서 그대로 물려받는다.
 *
 * 시트는 유형별 공통 5필드만 받으므로, 다른 화면에서 채운 기술 태그·링크·요약이 수정 저장에 쓸려 나가면 안 된다.
 * 수정 중에는 유형이 잠겨 있어 원본과 상세 타입이 항상 같다.
 */
private fun ExperienceDraft.preserving(original: Experience): ExperienceDraft {
    val edited = details
    val originalDetails = original.details
    val merged =
        when {
            edited is ExperienceDetails.Project && originalDetails is ExperienceDetails.Project -> {
                edited.copy(techs = originalDetails.techs, link = originalDetails.link)
            }

            edited is ExperienceDetails.Intern && originalDetails is ExperienceDetails.Intern -> {
                edited.copy(summary = originalDetails.summary)
            }

            edited is ExperienceDetails.Activity && originalDetails is ExperienceDetails.Activity -> {
                edited.copy(role = originalDetails.role)
            }

            else -> {
                edited
            }
        }
    return copy(details = merged)
}

/** 등록된 카드를 빠른 입력 시트의 값으로 되돌린다 — [ExperienceEditorState.toDraft] 의 역방향이다. */
internal fun Experience.toEditorState(): ExperienceEditorState {
    val details = details
    val start =
        when (details) {
            is ExperienceDetails.Certificate -> startDate?.toEditorYearMonth() ?: details.acquiredYearMonth?.replace('-', '.') ?: ""
            is ExperienceDetails.Award -> startDate?.toEditorYearMonth() ?: details.year?.let { "%04d.01".format(it) } ?: ""
            else -> startDate?.toEditorYearMonth() ?: ""
        }
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
    return ExperienceEditorState(
        experienceId = id,
        type = type,
        title = title,
        startDate = start,
        endDate = if (ExperienceEditorRules.hasEndDate(type)) endDate?.toEditorYearMonth().orEmpty() else "",
        primary = primary.orEmpty(),
        secondary = secondary.orEmpty(),
    )
}

private fun LocalDate.toEditorYearMonth(): String = "%04d.%02d".format(year, monthValue)

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
    get() = listOfNotNull(titleError, startDateError, endDateError, primaryError, secondaryError).isNotEmpty()

/** 시트 입력을 [ExperienceEditorRules] 로 검증해 필드 오류를 채운 사본을 돌려준다. */
internal fun validateExperienceEditor(editor: ExperienceEditorState): ExperienceEditorState {
    val type = editor.type
    val start = ExperienceEditorRules.parseYearMonth(editor.startDate)
    val end = ExperienceEditorRules.parseYearMonth(editor.endDate)
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
            start == null -> OnboardingFieldError.InvalidFormat
            else -> null
        }
    val endDateError =
        when {
            !ExperienceEditorRules.hasEndDate(type) || editor.endDate.isBlank() -> null
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
    return editor.copy(
        titleError = titleError,
        startDateError = startDateError,
        endDateError = endDateError,
        primaryError = primaryError,
        secondaryError = secondaryError,
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

/** 검증을 통과한 시트 입력을 [ExperienceDraft] 로 옮긴다 — 유형별 필드 의미는 [ExperienceEditorRules] 표를 따른다. */
internal fun ExperienceEditorState.toDraft(): ExperienceDraft {
    val trimmedTitle = title.trim()
    val start = ExperienceEditorRules.parseYearMonth(startDate)
    val end = if (ExperienceEditorRules.hasEndDate(type)) ExperienceEditorRules.parseYearMonth(endDate) else null
    val primaryText = primary.trim().ifEmpty { null }
    val secondaryText = secondary.trim().ifEmpty { null }
    val details =
        when (type) {
            ExperienceType.Project -> {
                ExperienceDetails.Project(role = primaryText, techs = emptyList(), summary = secondaryText, link = null)
            }

            ExperienceType.Award -> {
                ExperienceDetails.Award(
                    contestName = trimmedTitle,
                    rank = requireNotNull(primaryText) { "award rank is required" },
                    year = start?.year,
                    organizer = secondaryText,
                )
            }

            ExperienceType.Intern -> {
                ExperienceDetails.Intern(
                    company = requireNotNull(primaryText) { "intern company is required" },
                    role = requireNotNull(secondaryText) { "intern role is required" },
                    summary = null,
                )
            }

            ExperienceType.Activity -> {
                ExperienceDetails.Activity(
                    organization = requireNotNull(primaryText) { "activity organization is required" },
                    role = null,
                    summary = secondaryText,
                )
            }

            ExperienceType.Certificate -> {
                ExperienceDetails.Certificate(
                    issuer = primaryText,
                    acquiredYearMonth = start?.let { "%04d-%02d".format(it.year, it.monthValue) },
                )
            }
        }
    return ExperienceDraft(title = trimmedTitle, startDate = start, endDate = end, details = details)
}
