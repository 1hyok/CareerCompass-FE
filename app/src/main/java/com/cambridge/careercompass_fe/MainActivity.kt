package com.cambridge.careercompass_fe

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
        splashScreen.setKeepOnScreenCondition { viewModel.launch.value == null }

        setContent {
            CareerCompassTheme {
                val launch by viewModel.launch.collectAsStateWithLifecycle()
                launch?.let { current ->
                    // 세션 종료(로그아웃·만료)마다 revision 이 올라 NavHost 를 새로 만든다 — 같은 컨트롤러의
                    // startDestination 만 바꾸면 이전 백스택이 남고, 목적지 값만 키로 쓰면 같은 값일 때 아무
                    // 일도 일어나지 않는다. 프로세스 재생성 시에는 시작 목적지부터 다시 시작한다(백스택 미복원).
                    key(current.revision) {
                        AppNavigation(
                            startDestination = current.destination,
                            onSessionEnded = viewModel::refresh,
                            onExitRequest = ::finish,
                        )
                    }
                }
            }
        }
    }
}
