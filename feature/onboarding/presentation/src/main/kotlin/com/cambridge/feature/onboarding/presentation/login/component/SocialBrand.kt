package com.cambridge.feature.onboarding.presentation.login.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cambridge.feature.onboarding.presentation.R
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.kakao.sdk.v2.user.R as KakaoSdkR

/**
 * 카카오·구글 로그인 버튼의 브랜드 마크와 고정 색.
 *
 * ### 왜 `CareerCompassIcons` 에 넣지 않는가
 * 저기 있는 것은 우리 디자인 시스템의 기능 아이콘이라 테마 색을 받아 물든다. 여기 있는 것은 **남의
 * 상표**다. 상표는 색을 바꾸는 순간 상표가 아니게 되므로 같은 서랍에 둘 수 없다. 그래서 쓰는 자리
 * (로그인 버튼) 옆에, 가이드가 준 형태와 색 그대로 둔다.
 *
 * ### 색이 테마 토큰이 아닌 이유
 * 두 가이드 모두 버튼 색을 명시하고 그 밖의 색을 금지한다. 그래서 여기 값은 전부 고정이다. 다크
 * 모드에서도 **가이드가 스스로 정의한 변형만** 쓴다 — 구글은 가이드에 dark theme 한 쌍이 있어 그것을
 * 쓰고, 카카오는 다크 변형을 정의하지 않아 두 테마에서 같은 노랑을 쓴다. 가이드에 없는 변형을 우리가
 * 만들어 내지는 않는다.
 *
 * ### 마크 자산의 출처
 * - 카카오 심볼: 이미 의존성에 있는 카카오 SDK(`com.kakao.sdk:v2-user`)의 공식 드로어블을 그대로
 *   참조한다. 가이드가 「심볼의 형태, 비율, 색상은 변경할 수 없습니다」 라고 못박으므로, 우리가 다시
 *   그리는 것보다 SDK 가 배포하는 원본을 쓰는 편이 어긋날 여지가 없다.
 * - 구글 G: SDK(`play-services-base`)는 18dp 래스터(`googleg_standard_color_18.png`)만 배포해
 *   이슈가 요구하는 벡터가 없다. 그래서 가이드의 표준 컬러 G 경로를 벡터로 넣고, 배포된 래스터와
 *   렌더를 대조해 형태가 같은지 확인했다.
 *
 * 가이드 원문
 * - 카카오: https://developers.kakao.com/docs/latest/ko/kakaologin/design-guide
 * - 구글: https://developers.google.com/identity/branding-guidelines
 */
internal object KakaoBrand {
    /**
     * 컨테이너 `#FEE500`.
     *
     * 가이드가 다크 변형을 정의하지 않으므로 라이트·다크 모두 이 값이다. 테마를 따라 어둡게 바꾸면
     * 「위의 색상 규정에 벗어난 색상을 적용해서는 안 됩니다」 에 걸린다.
     */
    val Container: Color = Color(0xFFFEE500)

    /** 심볼 `#000000`. 가이드가 형태·비율과 함께 색도 변경 불가로 규정한다. */
    val Symbol: Color = Color(0xFF000000)

    /** 레이블 `#000000` 85%. */
    val Label: Color = Color(0xD9000000)
}

/**
 * 「Sign in with Google」 버튼의 고정 색.
 *
 * 가이드가 light·dark·neutral 세 테마를 주는데, 우리 화면은 라이트·다크 두 갈래뿐이라 앞의 두 쌍만
 * 쓴다. 색은 채움·테두리·글자가 한 벌이라 테마별로 셋을 함께 바꾼다.
 */
internal object GoogleBrand {
    /** light theme 채움 `#FFFFFF`. */
    val LightContainer: Color = Color(0xFFFFFFFF)

    /** light theme 테두리 `#747775`. */
    val LightStroke: Color = Color(0xFF747775)

    /** light theme 글자 `#1F1F1F`. */
    val LightLabel: Color = Color(0xFF1F1F1F)

    /** dark theme 채움 `#131314`. */
    val DarkContainer: Color = Color(0xFF131314)

    /** dark theme 테두리 `#8E918F`. */
    val DarkStroke: Color = Color(0xFF8E918F)

    /** dark theme 글자 `#E3E3E3`. */
    val DarkLabel: Color = Color(0xFFE3E3E3)

    /**
     * 비활성 G 의 회색 `#8D8D8D`.
     *
     * 우리가 고른 회색이 아니라 구글이 `googleg_disabled_color_18` 로 배포하는 값이다 — 그 자산의
     * 픽셀에서 그대로 읽었다. 컬러 G 를 임의로 흐리는 대신 구글이 비활성용으로 인정한 색을 쓴다.
     */
    val DisabledLogo: Color = Color(0xFF8D8D8D)
}

/**
 * 구글 표준 컬러 「G」.
 *
 * 경로는 가이드의 표준 컬러 G 를 48×48 뷰포트로 옮긴 것이고, 색 넷은 구글이 배포하는
 * `googleg_standard_color_18.png` 의 픽셀에서 읽은 값이다(`#EA4335` · `#34A853` · `#4285F4` ·
 * `#FBBC05`). 「you can't change the size or color of the Google 'G' logo」 이므로 경로와 색은
 * 손대지 않는다 — [CareerCompassIcons][com.careercompass.core.ui.icon.CareerCompassIcons] 의 24×24 ·
 * 2f 스트로크 규약이 여기 적용되지 않는 것도 같은 이유다.
 *
 * 뷰포트 48 안에서 글자가 차지하는 폭은 43.12 라, [GOOGLE_MARK_SIZE] 20dp 로 그리면 눈에 보이는
 * 마크가 가이드의 18dp 가 된다.
 */
internal val GoogleGLogo: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Brand.GoogleG",
            defaultWidth = GOOGLE_LOGO_VIEWPORT.dp,
            defaultHeight = GOOGLE_LOGO_VIEWPORT.dp,
            viewportWidth = GOOGLE_LOGO_VIEWPORT,
            viewportHeight = GOOGLE_LOGO_VIEWPORT,
        ).brandPath(
            color = Color(0xFF4285F4),
            pathData =
                "M45.12,24.5c0,-1.56 -0.14,-3.06 -0.4,-4.5H24v8.51h11.84c-0.51,2.75 -2.06,5.08 " +
                    "-4.39,6.64v5.52h7.11c4.16,-3.83 6.56,-9.47 6.56,-16.17z",
        ).brandPath(
            color = Color(0xFF34A853),
            pathData =
                "M24,46c5.94,0 10.92,-1.97 14.56,-5.33l-7.11,-5.52c-1.97,1.32 -4.49,2.1 -7.45,2.1 " +
                    "-5.73,0 -10.58,-3.87 -12.31,-9.07H4.34v5.7C7.96,41.07 15.4,46 24,46z",
        ).brandPath(
            color = Color(0xFFFBBC05),
            pathData =
                "M11.69,28.18C11.25,26.86 11,25.45 11,24s0.25,-2.86 0.69,-4.18v-5.7H4.34C2.85,17.09 " +
                    "2,20.45 2,24s0.85,6.91 2.34,9.88l7.35,-5.7z",
        ).brandPath(
            color = Color(0xFFEA4335),
            pathData =
                "M24,10.75c3.23,0 6.13,1.11 8.41,3.29l6.31,-6.31C34.91,4.18 29.93,2 24,2 15.4,2 " +
                    "7.96,6.93 4.34,14.12l7.35,5.7c1.73,-5.2 6.58,-9.07 12.31,-9.07z",
        ).build()
}

/**
 * 카카오 로그인 버튼.
 *
 * 문구는 가이드의 국문 완성형 「카카오 로그인」 이다. 접근성 이름은 이 문구 하나이고, 심볼은
 * `contentDescription = null` 로 두어 이름이 이중으로 읽히지 않게 한다.
 */
@Composable
internal fun KakaoLoginButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SocialLoginButton(
        text = stringResource(R.string.onboarding_login_kakao),
        onClick = onClick,
        containerColor = KakaoBrand.Container,
        contentColor = KakaoBrand.Label,
        border = null,
        enabled = enabled,
        modifier = modifier,
        leadingMark = { KakaoMark(enabled = enabled) },
    )
}

/**
 * 「Google 계정으로 로그인」 버튼.
 *
 * 문구는 구글이 권하는 「Sign in with Google」 의 한국어 정본이다 — 우리가 옮긴 것이 아니라
 * `play-services-base` 의 `values-ko` 가 `common_signin_button_text_long` 으로 배포하는 문자열이다.
 *
 * 테마 밝기는 [isSystemInDarkTheme] 로 읽는다. `CareerCompassTheme` 은 다크 여부를 밖으로 내주지
 * 않고 기본값이 곧 이 함수라, 앱에 수동 테마 전환이 들어오기 전까지 둘은 같은 값이다. 전환이 생기면
 * 여기도 테마에서 읽도록 함께 고쳐야 한다.
 */
@Composable
internal fun GoogleLoginButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()

    SocialLoginButton(
        text = stringResource(R.string.onboarding_login_google),
        onClick = onClick,
        containerColor = if (dark) GoogleBrand.DarkContainer else GoogleBrand.LightContainer,
        contentColor = if (dark) GoogleBrand.DarkLabel else GoogleBrand.LightLabel,
        border =
            BorderStroke(
                width = GOOGLE_BORDER_WIDTH,
                color = if (dark) GoogleBrand.DarkStroke else GoogleBrand.LightStroke,
            ),
        enabled = enabled,
        modifier = modifier,
        leadingMark = { GoogleMark(enabled = enabled) },
    )
}

/** 카카오 말풍선 심볼. SDK 가 배포하는 원본을 그대로 그리고 색만 가이드 값으로 맞춘다. */
@Composable
private fun KakaoMark(enabled: Boolean) {
    Icon(
        painter = painterResource(KakaoSdkR.drawable.icon_talk_login),
        contentDescription = null,
        modifier = Modifier.size(KAKAO_MARK_SIZE),
        tint = if (enabled) KakaoBrand.Symbol else CareerCompassTheme.colors.disabledContent,
    )
}

/** 구글 표준 컬러 G. 비활성일 때만 구글이 인정한 회색으로 덮는다. */
@Composable
private fun GoogleMark(enabled: Boolean) {
    Image(
        imageVector = GoogleGLogo,
        contentDescription = null,
        modifier = Modifier.size(GOOGLE_MARK_SIZE),
        colorFilter = if (enabled) null else ColorFilter.tint(GoogleBrand.DisabledLogo),
    )
}

private fun ImageVector.Builder.brandPath(
    color: Color,
    pathData: String,
): ImageVector.Builder =
    addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(color),
    )

/** 구글 G 의 뷰포트 한 변. 가이드가 배포하는 경로가 이 좌표계로 그려져 있다. */
private const val GOOGLE_LOGO_VIEWPORT: Float = 48f

/** 구글 G 를 그리는 상자. 뷰포트 여백까지 감안해 눈에 보이는 마크가 가이드의 18dp 가 되는 값이다. */
private val GOOGLE_MARK_SIZE: Dp = 20.dp

/** 구글 버튼 테두리 굵기. */
private val GOOGLE_BORDER_WIDTH: Dp = 1.dp

/**
 * 카카오 심볼을 그리는 상자.
 *
 * 원본 드로어블은 19×20 뷰포트 안에 말풍선을 13.68×13.66 으로 담고 나머지를 여백으로 둔다. 그래서
 * 상자를 18dp 로 잡으면 눈에 보이는 말풍선은 12dp 남짓이 되어, 옆에 선 18dp 구글 G 보다 눈에 띄게
 * 작아진다(실측: 11.8dp 대 18.3dp). 여백까지 감안해 26dp 로 잡으면 말풍선이 약 17.8dp 로 그려져 둘이
 * 같은 크기로 보인다. 상자만 키우는 것이라 「심볼의 형태, 비율」 은 그대로다.
 */
private val KAKAO_MARK_SIZE: Dp = 26.dp
