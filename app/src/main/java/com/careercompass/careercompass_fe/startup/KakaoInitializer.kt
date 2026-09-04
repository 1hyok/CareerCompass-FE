package com.careercompass.careercompass_fe.startup

import android.content.Context
import androidx.startup.Initializer
import com.careercompass.careercompass_fe.BuildConfig
import com.kakao.sdk.common.KakaoSdk

/**
 * 카카오 SDK 초기화. `Application.onCreate()` 를 초기화 코드로 채우지 않고 App Startup Initializer 단위로 둔다.
 *
 * 키가 비어 있으면(로컬 `KAKAO_NATIVE_APP_KEY` 미기재·CI 스텁) 초기화를 건너뛴다 — 카카오 로그인 버튼은
 * SDK 미초기화 예외로 실패하고 그 실패는 로그인 화면이 안내한다. release 빌드는 build-logic 가드가 빈 키를 막는다.
 */
public class KakaoInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val key = BuildConfig.KAKAO_NATIVE_APP_KEY
        if (key.isBlank()) return
        KakaoSdk.init(context.applicationContext, key)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
