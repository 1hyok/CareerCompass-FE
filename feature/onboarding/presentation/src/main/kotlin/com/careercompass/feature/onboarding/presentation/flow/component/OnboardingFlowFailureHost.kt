package com.careercompass.feature.onboarding.presentation.flow.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.flow.OnboardingFailureReason
import com.careercompass.feature.onboarding.presentation.flow.toMessage
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingErrorCard

/**
 * 단계 화면 위에 흐름 실패 배너를 띄운다.
 *
 * 단계 화면 계약에는 오류 슬롯이 없어(필드 오류만 있다) 화면을 덮는 배너로 보여준다. 하단 액션 버튼을 가리지
 * 않도록 푸터 높이만큼 띄운다.
 */
@Composable
internal fun OnboardingFlowFailureHost(
    failure: OnboardingFailureReason?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (failure != null) {
            OnboardingErrorCard(
                message = failure.toMessage(),
                onDismissClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            start = CareerCompassTheme.spacing.large,
                            end = CareerCompassTheme.spacing.large,
                            bottom = FOOTER_CLEARANCE,
                        ),
            )
        }
    }
}

/** 단계 푸터(52dp 버튼 + 상하 여백)를 가리지 않는 높이. */
private val FOOTER_CLEARANCE = 88.dp
