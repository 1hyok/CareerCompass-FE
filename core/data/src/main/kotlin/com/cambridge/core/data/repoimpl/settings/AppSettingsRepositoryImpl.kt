package com.cambridge.core.data.repoimpl.settings

import com.cambridge.core.datastore.DeviceDataSource
import com.cambridge.core.domain.settings.AppSettingsRepository
import com.cambridge.core.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 저장소는 문자열만 알고, 뜻은 여기서 붙인다 — `core:datastore` 는 `core:model` 을 모른다.
 *
 * 읽기가 관대한 이유: 앱 버전이 내려가거나 값이 손상돼 모르는 문자열이 들어와도
 * [ThemeMode.fromStorageValue] 가 [ThemeMode.System] 으로 떨어뜨린다. 설정 하나 때문에 앱이 열리지 않는 것이
 * 훨씬 나쁘다.
 */
internal class AppSettingsRepositoryImpl
    @Inject
    constructor(
        private val deviceDataSource: DeviceDataSource,
    ) : AppSettingsRepository {
        override val themeMode: Flow<ThemeMode> = deviceDataSource.themeMode.map(ThemeMode::fromStorageValue)

        override suspend fun setThemeMode(mode: ThemeMode) {
            deviceDataSource.setThemeMode(mode.storageValue)
        }
    }
