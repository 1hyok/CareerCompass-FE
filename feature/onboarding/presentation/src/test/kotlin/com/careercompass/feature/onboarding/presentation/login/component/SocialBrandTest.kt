package com.careercompass.feature.onboarding.presentation.login.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 브랜드 마크·색이 가이드에서 벗어나지 않는지.
 *
 * 값이 상수와 같은지 보는 검사라 얼핏 동어반복 같지만, 여기서 막으려는 것은 「값이 틀리는 것」이 아니라
 * **「우리 것처럼 손대는 것」** 이다. 소셜 버튼은 테마 토큰을 쓰는 다른 버튼들 사이에 섞여 있어, 색을
 * 토큰으로 바꾸거나 컬러 G 를 단색 아이콘으로 「정리」하려는 리팩터가 자연스러워 보인다. 둘 다 가이드
 * 위반이고, 심사에서야 드러난다. 그래서 값을 가이드 원문과 함께 여기에 고정한다.
 *
 * - 카카오: https://developers.kakao.com/docs/latest/ko/kakaologin/design-guide
 * - 구글: https://developers.google.com/identity/branding-guidelines
 */
public class SocialBrandTest {
    @Test
    public fun kakaoColors_matchDesignGuide() {
        // 「컨테이너 #FEE500 / 심볼 #000000 / 레이블 #000000 85%」
        assertEquals(Color(0xFFFEE500), KakaoBrand.Container)
        assertEquals(Color(0xFF000000), KakaoBrand.Symbol)
        assertEquals(Color(0xD9000000), KakaoBrand.Label)
    }

    @Test
    public fun googleColors_matchBrandingGuidelines() {
        // light theme: fill #FFFFFF · stroke #747775 · text #1F1F1F
        assertEquals(Color(0xFFFFFFFF), GoogleBrand.LightContainer)
        assertEquals(Color(0xFF747775), GoogleBrand.LightStroke)
        assertEquals(Color(0xFF1F1F1F), GoogleBrand.LightLabel)

        // dark theme: fill #131314 · stroke #8E918F · text #E3E3E3
        assertEquals(Color(0xFF131314), GoogleBrand.DarkContainer)
        assertEquals(Color(0xFF8E918F), GoogleBrand.DarkStroke)
        assertEquals(Color(0xFFE3E3E3), GoogleBrand.DarkLabel)
    }

    /**
     * 「you can't change the size or color of the Google 'G' logo. It must be the standard color
     * version」 — 네 색이 모두 살아 있어야 표준 컬러 G 다. 단색으로 바꾸면 이 검사가 먼저 깨진다.
     */
    @Test
    public fun googleLogo_keepsAllFourStandardColors() {
        val fills = GoogleGLogo.root.solidFills()

        assertEquals(
            listOf(
                Color(0xFF4285F4),
                Color(0xFF34A853),
                Color(0xFFFBBC05),
                Color(0xFFEA4335),
            ),
            fills,
        )
    }

    /** 마크는 통째로 tint 되면 안 되므로 경로마다 자기 색을 들고 있어야 한다. */
    @Test
    public fun googleLogo_paintsEveryPathItself() {
        val paths = GoogleGLogo.root.paths()

        assertEquals(4, paths.size)
        assertEquals(emptyList<VectorPath>(), paths.filter { it.fill !is SolidColor })
    }

    private fun VectorGroup.paths(): List<VectorPath> =
        flatMap { node: VectorNode ->
            when (node) {
                is VectorPath -> listOf(node)
                is VectorGroup -> node.paths()
            }
        }

    private fun VectorGroup.solidFills(): List<Color> = paths().mapNotNull { (it.fill as? SolidColor)?.value }
}
