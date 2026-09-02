package com.cambridge.careercompass_fe

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.careercompass_fe.navigation.AppNavigation
import com.cambridge.careercompass_fe.session.MainViewModel
import com.cambridge.core.ui.theme.CareerCompassTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * [FragmentActivity] 인 이유 — 지문 로그인의 `androidx.biometric.BiometricPrompt` 가 FragmentActivity 를 요구한다.
 */
@AndroidEntryPoint
public class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 시작 목적지가 확정될 때까지(세션·프로필 확인) 시스템 스플래시를 유지한다.
        splashScreen.setKeepOnScreenCondition { viewModel.startDestination.value == null }

        setContent {
            CareerCompassTheme {
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
                startDestination?.let { destination ->
                    AppNavigation(startDestination = destination, onSessionEnded = viewModel::refresh)
                }
            }
        }
    }
}
