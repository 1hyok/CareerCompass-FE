package com.cambridge.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * CareerCompass 기능 아이콘 정본.
 *
 * 화면들이 아이콘을 문자열 리소스의 글리프·이모지(`←` · `✕` · `🔔` …)로 그려 왔다. 그 방식은 셋을 깨뜨린다 —
 * 컬러 이모지는 `tint` 로 물들지 않아 다크 모드에서 라이트와 같은 색으로 남고, OEM 폰트마다 모양·여백이
 * 달라 디자인이 기기별로 흔들리고, 스크린샷 baseline 이 렌더 환경의 폰트에 묶인다. 여기 있는 [ImageVector]
 * 들은 폰트를 타지 않고 `Icon(tint = …)`/`LocalContentColor` 로 테마 색을 그대로 받는다.
 *
 * ### 쓰는 법
 * 아이콘 자체는 `contentDescription = null` 로 두고 접근성 이름은 감싸는 버튼이 갖는다. 아이콘에 이름을
 * 달면 버튼 이름과 이중으로 읽힌다.
 *
 * ### 왜 직접 그리는가
 * `material-icons-extended` 는 수천 개를 끌고 오는데 이 앱이 쓰는 기능 아이콘은 열 몇 개다. 메서드 수와
 * APK 크기를 그만큼 내주고 살 이유가 없다. 디자인 정본인 Figma 10 Brand & Icons 에서 벡터를 내려받을 수
 * 없어(MCP 미인증) 모양은 Material Symbols(outlined) 의 일반형을 따랐다.
 *
 * ### 모양 규약
 * 24×24 뷰포트에 2f 굵기 스트로크, 둥근 캡·조인이다. 채움이 필요한 것(`Bookmark` · `Bullet` ·
 * `MoreHorizontal`)만 예외로 채운다. 새 아이콘을 더할 때도 같은 굵기를 쓴다 — 굵기가 섞이면 나란히 놓았을
 * 때 한쪽만 두꺼워 보인다.
 */
public object CareerCompassIcons {
    /** 뒤로 가기. RTL 로케일에서 자동으로 좌우가 뒤집힌다. */
    public val ArrowBack: ImageVector by lazy {
        strokeIcon(name = "ArrowBack", autoMirror = true) {
            moveTo(20f, 12f)
            lineTo(4.5f, 12f)
            moveTo(11.5f, 19f)
            lineTo(4.5f, 12f)
            lineTo(11.5f, 5f)
        }
    }

    /** 닫기 · 삭제. */
    public val Close: ImageVector by lazy {
        strokeIcon(name = "Close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    /** 선택 완료 표시. */
    public val Check: ImageVector by lazy {
        strokeIcon(name = "Check") {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 6.5f)
        }
    }

    /** 검색. */
    public val Search: ImageVector by lazy {
        strokeIcon(name = "Search") {
            circle(centerX = 10.5f, centerY = 10.5f, radius = 5.5f)
            moveTo(14.6f, 14.6f)
            lineTo(19.5f, 19.5f)
        }
    }

    /** 필터(깔때기). */
    public val Filter: ImageVector by lazy {
        strokeIcon(name = "Filter") {
            moveTo(3.5f, 5f)
            lineTo(20.5f, 5f)
            lineTo(14f, 12.5f)
            lineTo(14f, 19.5f)
            lineTo(10f, 17f)
            lineTo(10f, 12.5f)
            close()
        }
    }

    /** 펼치기 · 드롭다운 열기. 정렬 트리거처럼 「눌러서 목록을 연다」를 뜻하는 자리에 쓴다. */
    public val ExpandMore: ImageVector by lazy {
        strokeIcon(name = "ExpandMore") {
            moveTo(6f, 9.5f)
            lineTo(12f, 15.5f)
            lineTo(18f, 9.5f)
        }
    }

    /** 접기. */
    public val ExpandLess: ImageVector by lazy {
        strokeIcon(name = "ExpandLess") {
            moveTo(6f, 14.5f)
            lineTo(12f, 8.5f)
            lineTo(18f, 14.5f)
        }
    }

    /** 공유. */
    public val Share: ImageVector by lazy {
        strokeIcon(name = "Share") {
            circle(centerX = 18f, centerY = 5.5f, radius = 2.5f)
            circle(centerX = 6f, centerY = 12f, radius = 2.5f)
            circle(centerX = 18f, centerY = 18.5f, radius = 2.5f)
            moveTo(8.2f, 10.8f)
            lineTo(15.8f, 6.7f)
            moveTo(8.2f, 13.2f)
            lineTo(15.8f, 17.3f)
        }
    }

    /** 저장하지 않은 북마크. */
    public val BookmarkBorder: ImageVector by lazy {
        strokeIcon(name = "BookmarkBorder") { bookmark() }
    }

    /** 저장한 북마크. 테두리형과 같은 외곽선을 채워 나란히 놓아도 크기가 흔들리지 않는다. */
    public val Bookmark: ImageVector by lazy {
        filledStrokeIcon(name = "Bookmark") { bookmark() }
    }

    /** 알림(종). */
    public val Notifications: ImageVector by lazy {
        strokeIcon(name = "Notifications") {
            moveTo(6f, 16.5f)
            lineTo(6f, 11f)
            curveTo(6f, 7.7f, 8.7f, 5f, 12f, 5f)
            curveTo(15.3f, 5f, 18f, 7.7f, 18f, 11f)
            lineTo(18f, 16.5f)
            lineTo(19.5f, 18f)
            lineTo(4.5f, 18f)
            close()
            moveTo(9.7f, 18.8f)
            curveTo(9.7f, 20.1f, 10.7f, 21.1f, 12f, 21.1f)
            curveTo(13.3f, 21.1f, 14.3f, 20.1f, 14.3f, 18.8f)
        }
    }

    /** 항목 추가. */
    public val Add: ImageVector by lazy {
        strokeIcon(name = "Add") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    /** 편집. */
    public val Edit: ImageVector by lazy {
        strokeIcon(name = "Edit") {
            moveTo(4f, 20f)
            lineTo(4f, 16.2f)
            lineTo(15.6f, 4.6f)
            lineTo(19.4f, 8.4f)
            lineTo(7.8f, 20f)
            close()
            moveTo(13.7f, 6.5f)
            lineTo(17.5f, 10.3f)
        }
    }

    /** 더 보기(가로 점 세 개). */
    public val MoreHorizontal: ImageVector by lazy {
        filledIcon(name = "MoreHorizontal") {
            circle(centerX = 5.5f, centerY = 12f, radius = 1.8f)
            circle(centerX = 12f, centerY = 12f, radius = 1.8f)
            circle(centerX = 18.5f, centerY = 12f, radius = 1.8f)
        }
    }

    /** 목록 글머리. */
    public val Bullet: ImageVector by lazy {
        filledIcon(name = "Bullet") {
            circle(centerX = 12f, centerY = 12f, radius = 3f)
        }
    }
}

private const val ICON_SIZE_DP: Float = 24f

private const val ICON_STROKE_WIDTH: Float = 2f

/** 원을 4분할 베지어로 근사할 때의 제어점 비율. */
private const val CIRCLE_CONTROL_RATIO: Float = 0.5523f

/** 북마크 리본. 테두리형과 채움형이 같은 외곽선을 공유하도록 한 곳에 둔다. */
private fun PathBuilder.bookmark() {
    moveTo(6f, 4.5f)
    lineTo(18f, 4.5f)
    lineTo(18f, 20f)
    lineTo(12f, 16.3f)
    lineTo(6f, 20f)
    close()
}

/**
 * 원 하나를 그린다.
 *
 * [PathBuilder] 에 원 프리미티브가 없어 4분할 베지어로 근사한다. `arcTo` 는 반원 경계에서
 * `isMoreThanHalf` 판정이 갈려 렌더가 흔들리는 자리라 쓰지 않는다.
 */
private fun PathBuilder.circle(
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    val control = radius * CIRCLE_CONTROL_RATIO

    moveTo(centerX, centerY - radius)
    curveTo(
        centerX + control,
        centerY - radius,
        centerX + radius,
        centerY - control,
        centerX + radius,
        centerY,
    )
    curveTo(
        centerX + radius,
        centerY + control,
        centerX + control,
        centerY + radius,
        centerX,
        centerY + radius,
    )
    curveTo(
        centerX - control,
        centerY + radius,
        centerX - radius,
        centerY + control,
        centerX - radius,
        centerY,
    )
    curveTo(
        centerX - radius,
        centerY - control,
        centerX - control,
        centerY - radius,
        centerX,
        centerY - radius,
    )
    close()
}

private fun iconBuilder(
    name: String,
    autoMirror: Boolean,
): ImageVector.Builder =
    ImageVector.Builder(
        name = "CareerCompass.$name",
        defaultWidth = ICON_SIZE_DP.dp,
        defaultHeight = ICON_SIZE_DP.dp,
        viewportWidth = ICON_SIZE_DP,
        viewportHeight = ICON_SIZE_DP,
        autoMirror = autoMirror,
    )

/**
 * 선으로만 그리는 아이콘.
 *
 * 경로 색을 검정으로 두는 것은 Material 아이콘과 같은 관례다 — `Icon` 이 `ColorFilter.tint` 로 통째로
 * 덮으므로 실제 색은 호출부의 tint·[androidx.compose.material3.LocalContentColor] 가 정한다.
 */
private fun strokeIcon(
    name: String,
    autoMirror: Boolean = false,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector =
    iconBuilder(name = name, autoMirror = autoMirror)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        ).build()

/** 면으로만 그리는 아이콘. 점처럼 작아서 선으로 그리면 형태가 뭉개지는 것들에 쓴다. */
private fun filledIcon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector =
    iconBuilder(name = name, autoMirror = false)
        .path(
            fill = SolidColor(Color.Black),
            pathBuilder = pathBuilder,
        ).build()

/** 채우면서 같은 굵기의 테두리도 두르는 아이콘. 테두리형과 채움형이 짝인 아이콘의 채움 쪽이다. */
private fun filledStrokeIcon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector =
    iconBuilder(name = name, autoMirror = false)
        .path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        ).build()
