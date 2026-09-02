package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.domain.model.SchoolCatalog
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerSheet
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerState

@PreviewTest
@Preview(name = "School picker default", widthDp = 360, heightDp = 800)
@Composable
public fun SchoolPickerDefaultPreview() {
    SchoolPickerPreviewHost(state = SchoolPickerState(results = SchoolCatalog.search("")))
}

@PreviewTest
@Preview(name = "School picker filtered", widthDp = 360, heightDp = 800)
@Composable
public fun SchoolPickerFilteredPreview() {
    SchoolPickerPreviewHost(state = SchoolPickerState(query = "건국", results = SchoolCatalog.search("건국")))
}

@PreviewTest
@Preview(name = "School picker empty", widthDp = 360, heightDp = 800)
@Composable
public fun SchoolPickerEmptyPreview() {
    SchoolPickerPreviewHost(state = SchoolPickerState(query = "없는 학교", results = emptyList()))
}

@Composable
private fun SchoolPickerPreviewHost(state: SchoolPickerState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.surface) {
            SchoolPickerSheet(state = state, onEvent = {})
        }
    }
}
