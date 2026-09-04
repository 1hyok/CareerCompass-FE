package com.cambridge.core.domain.settings

import com.cambridge.core.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * 이 **기기**에 남는 앱 설정 — 계정이 아니라 기기 수명(DEVICE 스코프)이다.
 *
 * 화면 테마가 계정 설정이 아닌 이유는 두 가지다. 로그인 전(스플래시·로그인 화면)에도 이미 적용돼 있어야 하고,
 * 「이 기기에서 어떻게 보이는가」는 다른 기기로 들고 갈 값이 아니다. 그래서 로그아웃해도 남는다.
 */
public interface AppSettingsRepository {
    /** 고른 화면 테마. 고른 적이 없거나 저장된 값을 읽지 못하면 [ThemeMode.System]. */
    public val themeMode: Flow<ThemeMode>

    /** 화면 테마를 고른다. */
    public suspend fun setThemeMode(mode: ThemeMode)
}
