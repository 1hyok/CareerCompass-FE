package com.cambridge.feature.onboarding.presentation.shared.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.careercompass.core.ui.theme.CareerCompassTheme

/** 피커·시트 본문을 감싸는 모달 시트 — 온보딩 단계 피커와 지문 등록 제안이 함께 쓴다. 본문 컴포저블은 stateless 로 따로 테스트한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnboardingSheetHost(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CareerCompassTheme.colors.surface,
        contentColor = CareerCompassTheme.colors.onSurface,
        content = content,
    )
}
