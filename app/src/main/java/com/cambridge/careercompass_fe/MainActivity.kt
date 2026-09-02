package com.cambridge.careercompass_fe

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.careercompass_fe.navigation.AppDeepLinkParser
import com.cambridge.careercompass_fe.navigation.AppNavigation
import com.cambridge.careercompass_fe.session.MainViewModel
import com.cambridge.core.ui.theme.CareerCompassTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * [FragmentActivity] 인 이유 — 지문 로그인의 `androidx.biometric.BiometricPrompt` 가 FragmentActivity 를 요구한다.
 *
 * 딥링크(`careercompass://postings/{id}`, 계약은 `navigation/AppDeepLink.kt`)는 intent 를 파서에 넘겨 [MainViewModel] 에
 * 싣기만 한다 — 어디로 언제 이동할지는 인증 게이트를 아는 [AppNavigation] 이 정한다.
 */
@AndroidEntryPoint
public class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 회전 등 재생성에서는 ViewModel 이 같은 intent 의 딥링크를 이미 갖고 있거나 소비했다 — 두 번 싣지 않는다.
        if (savedInstanceState == null) viewModel.onDeepLink(AppDeepLinkParser.parse(intent?.data))

        // 시작 목적지가 확정될 때까지(세션·프로필 확인) 시스템 스플래시를 유지한다.
        splashScreen.setKeepOnScreenCondition { viewModel.launch.value == null }

        setContent {
            CareerCompassTheme {
                val launch by viewModel.launch.collectAsStateWithLifecycle()
                val pendingDeepLink by viewModel.pendingDeepLink.collectAsStateWithLifecycle()
                launch?.let { current ->
                    // 세션 종료(로그아웃·만료)마다 revision 이 올라 NavHost 를 새로 만든다 — 같은 컨트롤러의
                    // startDestination 만 바꾸면 이전 백스택이 남고, 목적지 값만 키로 쓰면 같은 값일 때 아무
                    // 일도 일어나지 않는다. 프로세스 재생성 시에는 시작 목적지부터 다시 시작한다(백스택 미복원).
                    key(current.revision) {
                        AppNavigation(
                            startDestination = current.destination,
                            pendingDeepLink = pendingDeepLink,
                            onDeepLinkConsumed = viewModel::consumeDeepLink,
                            onSessionEnded = viewModel::refresh,
                            onExitRequest = ::finish,
                        )
                    }
                }
            }
        }
    }

    /** 이미 떠 있는 인스턴스로 들어온 딥링크 — 보내는 쪽이 `FLAG_ACTIVITY_SINGLE_TOP` 을 붙였을 때만 여기로 온다. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.onDeepLink(AppDeepLinkParser.parse(intent.data))
    }
}
