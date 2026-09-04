package com.careercompass.core.model.settings

/**
 * 화면을 어느 테마로 그릴지 — 이 **기기에서** 고른 값이다(계정 설정이 아니다).
 *
 * [System] 이 기본이고, 저장된 값이 없거나 알 수 없는 값이면 여기로 떨어진다. 시스템을 따르는 것과 라이트를
 * 고른 것은 **다르다** — 앞은 기기 설정이 바뀌면 함께 바뀌고, 뒤는 기기가 어두워져도 밝게 남는다. 그래서
 * 「다크 아님」을 Boolean 하나로 담지 않고 값 셋을 둔다.
 *
 * [storageValue] 는 저장소에 남는 문자열이다. enum 이름을 그대로 쓰면 나중에 상수 이름을 바꿀 때 이미 저장된
 * 값이 읽히지 않으므로 따로 둔다.
 */
public enum class ThemeMode(
    public val storageValue: String,
) {
    /** 기기의 다크 모드 설정을 따른다. */
    System("system"),

    /** 기기 설정과 무관하게 밝게 그린다. */
    Light("light"),

    /** 기기 설정과 무관하게 어둡게 그린다. */
    Dark("dark"),
    ;

    /**
     * 이 모드에서 화면을 어둡게 그리는가 — [systemInDarkTheme] 은 지금 기기 설정이 다크인지다.
     *
     * 판정을 화면이 아니라 여기 두는 이유: 「시스템 따름」의 뜻이 한 곳에만 있어야 새 화면이 다르게 해석하지
     * 않는다. Compose 를 모르는 순수 함수라 유닛 테스트로 세 값을 전부 돌 수 있다.
     */
    public fun resolveDark(systemInDarkTheme: Boolean): Boolean =
        when (this) {
            System -> systemInDarkTheme
            Light -> false
            Dark -> true
        }

    public companion object {
        /** 저장된 문자열을 값으로 되읽는다. 모르는 값·null 은 [System] 으로 떨어진다 — 설정 하나 때문에 앱이 열리지 않으면 안 된다. */
        public fun fromStorageValue(value: String?): ThemeMode = entries.firstOrNull { it.storageValue == value } ?: System
    }
}
