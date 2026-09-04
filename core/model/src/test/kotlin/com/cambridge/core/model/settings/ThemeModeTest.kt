package com.cambridge.core.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `시스템 따름은 기기 설정을 그대로 따른다`() {
        assertTrue(ThemeMode.System.resolveDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.System.resolveDark(systemInDarkTheme = false))
    }

    @Test
    fun `고른 값은 기기 설정을 무시한다`() {
        assertFalse(ThemeMode.Light.resolveDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.Dark.resolveDark(systemInDarkTheme = false))
    }

    @Test
    fun `저장 문자열은 왕복해도 같은 값이다`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorageValue(mode.storageValue))
        }
    }

    @Test
    fun `모르는 값과 없는 값은 시스템 따름으로 떨어진다`() {
        // 앱 버전이 내려가거나 저장이 손상돼도 설정 하나 때문에 앱이 열리지 않으면 안 된다.
        assertEquals(ThemeMode.System, ThemeMode.fromStorageValue(null))
        assertEquals(ThemeMode.System, ThemeMode.fromStorageValue(""))
        assertEquals(ThemeMode.System, ThemeMode.fromStorageValue("sepia"))
        assertEquals(ThemeMode.System, ThemeMode.fromStorageValue("SYSTEM"))
    }

    @Test
    fun `저장 문자열은 상수 이름과 따로 간다`() {
        // 이 값이 바뀌면 이미 저장된 설정이 읽히지 않는다. 이름을 바꿔도 여기는 그대로여야 한다.
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.storageValue })
    }
}
