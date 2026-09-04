package com.cambridge.feature.onboarding.presentation.pastapplication

import androidx.compose.runtime.Immutable
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import com.careercompass.core.model.application.UploadFile

/**
 * 파일을 고른 뒤 업로드 직전에 라벨을 확인·수정하는 시트 상태 — F1-4 「각 지원서에 사용자가 직접 라벨 부여」.
 *
 * 서버에 라벨 수정 엔드포인트가 없어(API_SPEC §4 는 업로드 요청 필드로만 받는다) 올리기 전에 받아야 한다.
 * 올릴 파일을 상태가 함께 들어, 시트가 열려 있는 동안 라벨과 대상 파일이 어긋날 수 없게 한다 — 목록의 문서가
 * 재시도용 원본을 드는 것과 같은 방식이다.
 *
 * @property file 확인이 끝나면 이 파일을 [label] 로 올린다. 시트는 이름만 보여 준다.
 * @property label 기본값은 확장자를 뺀 파일명이라, 그대로 두면 파일명을 라벨로 쓰던 이전과 같은 결과가 된다.
 */
@Immutable
public data class UploadLabelState(
    public val file: UploadFile,
    public val label: String,
    public val labelError: OnboardingFieldError? = null,
) {
    /** 공백만 남은 라벨은 규칙상 거부되므로 버튼부터 잠근다. 길이 초과는 눌렀을 때 사유로 알린다. */
    public val isSubmitEnabled: Boolean
        get() = label.isNotBlank()

    public val fileName: String
        get() = file.fileName
}

/** User intentions emitted by [UploadLabelSheet]. */
public sealed interface UploadLabelEvent {
    public data class LabelChanged(
        public val value: String,
    ) : UploadLabelEvent

    public data object Submitted : UploadLabelEvent

    /** 시트를 닫는다 — 고른 파일은 올리지 않는다. */
    public data object Dismissed : UploadLabelEvent
}
