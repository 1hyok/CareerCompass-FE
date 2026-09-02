package com.cambridge.feature.onboarding.presentation.basicinfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.component.CareerCompassTextField
import com.cambridge.core.ui.component.CareerCompassTextFieldSize
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.R

/**
 * 학교 검색·선택 시트의 본문 — Step 1 「학교」 필드가 연다.
 *
 * 시트 컨테이너(`ModalBottomSheet`)는 호스트가 감싼다. 이 컴포저블은 stateless 로 [state] 만 그리고
 * [onEvent] 로 의도를 전달한다.
 */
@Composable
public fun SchoolPickerSheet(
    state: SchoolPickerState,
    onEvent: (SchoolPickerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.large, vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.onboarding_school_picker_title),
            modifier = Modifier.semantics { heading() },
            color = colors.onSurface,
            style = CareerCompassTheme.typography.headline4,
        )
        CareerCompassTextField(
            value = state.query,
            onValueChange = { onEvent(SchoolPickerEvent.QueryChanged(it)) },
            label = stringResource(R.string.onboarding_school_picker_search_label),
            placeholder = stringResource(R.string.onboarding_school_picker_search_placeholder),
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        if (state.results.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_school_picker_empty),
                modifier = Modifier.padding(vertical = spacing.large),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = SCHOOL_LIST_MAX_HEIGHT),
            ) {
                items(items = state.results, key = { it }) { school ->
                    SchoolRow(
                        school = school,
                        onClick = { onEvent(SchoolPickerEvent.SchoolSelected(school)) },
                    )
                    HorizontalDivider(color = colors.subtleOutline)
                }
            }
        }
    }
}

@Composable
private fun SchoolRow(
    school: String,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val description = stringResource(R.string.onboarding_school_picker_item_description, school)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SCHOOL_ROW_MIN_HEIGHT)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    role = Role.Button
                }.padding(horizontal = spacing.xSmall),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = school,
            color = colors.onSurface,
            style = CareerCompassTheme.typography.bodyLarge,
        )
    }
}

private val SCHOOL_LIST_MAX_HEIGHT = 360.dp

private val SCHOOL_ROW_MIN_HEIGHT = 48.dp
