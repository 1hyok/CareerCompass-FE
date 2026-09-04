package com.careercompass.careercompass_fe.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.careercompass.careercompass_fe.MainActivity

/**
 * 디버그 빌드 전용 진입점. 런처 아이콘을 늘리지 않으려고 LAUNCHER intent-filter 는 두지 않는다.
 *
 * `adb shell am start -n com.careercompass.careercompass_fe/.debug.DebugSettingsActivity`
 *
 * 화면별 바로가기는 feature 모듈이 붙는 대로 여기에 추가한다.
 */
class DebugSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.systemBarsPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "CareerCompass DEV", style = MaterialTheme.typography.titleLarge)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startActivity(Intent(this@DebugSettingsActivity, MainActivity::class.java)) },
                        ) {
                            Text(text = "앱 시작 화면 열기")
                        }
                    }
                }
            }
        }
    }
}
