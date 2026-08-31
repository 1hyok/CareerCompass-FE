package com.cambridge.careercompass_fe.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// 화면이 붙기 전까지의 시작 목적지. feature presentation 모듈이 채워지면
// 각 모듈의 navigation 확장으로 옮기고 이 자리표시자는 지운다.
private const val PLACEHOLDER_ROUTE = "placeholder"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    MaterialTheme {
        Surface {
            NavHost(navController = navController, startDestination = PLACEHOLDER_ROUTE) {
                composable(PLACEHOLDER_ROUTE) { Text(text = "CareerCompass") }
            }
        }
    }
}
