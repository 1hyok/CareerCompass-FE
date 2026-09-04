package com.careercompass.core.domain.testing

import com.careercompass.core.domain.settings.AppSettingsRepository
import com.careercompass.core.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** [AppSettingsRepository] fake 정본. 고른 값을 메모리에 담고 즉시 흘려보낸다. */
public class FakeAppSettingsRepository(
    initialThemeMode: ThemeMode = ThemeMode.System,
) : AppSettingsRepository {
    public val themeModeState: MutableStateFlow<ThemeMode> = MutableStateFlow(initialThemeMode)

    override val themeMode: Flow<ThemeMode> get() = themeModeState

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModeState.value = mode
    }
}
