package com.cambridge.core.ui.failure

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.cambridge.core.ui.R
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.model.application.MAX_PAST_APPLICATIONS
import com.careercompass.core.model.board.MAX_BOARDS
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS

/**
 * 실패가 사용자에게 어떻게 보일지 정한 표의 한 줄 — **문구의 정본은 여기 하나다.**
 *
 * API_SPEC v0.1 §9 의 코드는 개발자용 설명만 달고 온다. 그것을 그대로 띄우면 사용자는 무슨 말인지
 * 모르고, 화면마다 각자 문구를 지으면 같은 사실을 서로 다르게 말하게 된다(#204). 그래서 「무슨 일이
 * 일어났는가([titleRes]·[descriptionRes])」와 「지금 무엇을 할 수 있는가([action])」를 한 표에 모은다.
 *
 * 문자열은 해석하지 않고 `@StringRes` 로 들고 있는다 — 이 계약은 `Resources` 없이도 만들어지고
 * 비교되며, 폰트 배율·다크 모드처럼 컴포지션이 정하는 것을 미리 굳히지 않는다.
 *
 * @property descriptionArgs [descriptionRes] 가 서식 인자를 받을 때만 채워진다(상한 개수 등).
 */
@Immutable
public data class FailureDisplay(
    @get:StringRes public val titleRes: Int,
    @get:StringRes public val descriptionRes: Int,
    public val action: FailureAction,
    public val descriptionArgs: List<Any> = emptyList(),
) {
    /**
     * **같은 요청을 그대로 다시 보내 답이 갈릴 여지가 있는가** — 재시도 버튼을 그릴지의 유일한 근거다.
     *
     * 상한 초과·중복·차단처럼 조건이 그대로면 답도 그대로인 실패에 재시도를 주면, 사용자는 버튼이 있으니
     * 누르고 같은 실패를 다시 만난다. 그 자리에는 [action] 이 실제로 상황을 바꿀 길을 대신 연다.
     */
    public val isRetryable: Boolean get() = action == FailureAction.Retry
}

/**
 * 실패 앞에서 사용자가 할 수 있는 일 — 버튼 하나로 옮겨지는 것만 둔다.
 *
 * 화면은 이 값을 보고 **어떤 버튼을 붙일지**를 정하고, 눌렀을 때 어디로 보낼지는 화면이 채운다.
 * 목적지(프로필 화면·게시판 목록·로그인)는 모듈마다 다르고 core 가 알 수 있는 것이 아니다.
 */
public enum class FailureAction(
    @get:StringRes public val labelRes: Int?,
) {
    /** 할 수 있는 일이 없다. 버튼을 주지 않는다 — 그래서 [labelRes] 도 없다. */
    None(null),

    /** 시간이 지나면 답이 갈린다. 같은 요청을 다시 보낸다. */
    Retry(R.string.core_ui_state_retry),

    /** 프로필을 채우러 보낸다. */
    CompleteProfile(R.string.core_ui_failure_action_complete_profile),

    /** 상한에 닿았다. 지우러 보낸다. */
    FreeUpSpace(R.string.core_ui_failure_action_free_up_space),

    /** 세션이 끝났다. 다시 로그인시킨다. */
    SignIn(R.string.core_ui_failure_action_sign_in),
}

/**
 * 실패의 갈래 — 표의 행 이름이다. API_SPEC §9 의 14종을 **사용자가 읽을 문장 단위**로 접는다.
 *
 * 코드와 1:1 이 아니다. `AUTH_REQUIRED` 와 `AUTH_INVALID` 는 사용자가 할 일이 「다시 로그인」으로
 * 같고, `RESOURCE_NOT_FOUND` 와 `POSTING_NOT_FOUND` 도 「없어졌다」는 같은 사실이다 — 어느 자원이
 * 없어졌는지는 [FailureSurface] 가 말한다. `INTERNAL_ERROR` 와 정체를 모르는 실패는 사용자에게 같은
 * 말이므로 [Unexpected] 하나로 묶는다. 관측용 분류는 이 접힘과 무관하게 원본을 다시 읽는다
 * (`core:common` 의 `StagedFailureReporting`, #117).
 *
 * 코드가 아예 없는 실패(전송 계층)는 [NoConnection] 과 [Timeout] 으로 갈린다 —
 * [CoreDataFailure.NetworkUnavailable.isTimeout] 의 판정을 그대로 존중한다(#134).
 */
public enum class FailureKind {
    NoConnection,
    Timeout,
    InvalidInput,
    AuthExpired,
    PermissionDenied,
    NotFound,
    DuplicateBoard,
    LimitExceeded,
    ProfileIncomplete,
    ParsingFailed,
    BoardBlocked,
    RateLimited,
    ServiceUnavailable,
    Unexpected,
}

/**
 * 문구를 고르는 화면 문맥 — **넘기지 않아도 되는 값이다.**
 *
 * 같은 코드라도 무엇을 하다 만났는지에 따라 사용자가 읽어야 할 문장이 다르다. 404 는 공고일 때와
 * 게시판일 때 가리키는 것이 다르고, 상한 초과는 무엇이 몇 개까지인지를 말해야 쓸모가 있다.
 * 문맥을 모르는 자리(공통 다이얼로그·아직 옮기지 않은 화면)는 [Unspecified] 로 두면 어느 화면에
 * 붙어도 어긋나지 않는 문구가 나간다 — 틀린 명사를 말하느니 명사를 말하지 않는다.
 */
public enum class FailureSurface {
    Unspecified,
    Posting,
    Board,
    Application,
    ExperienceCard,
}

/**
 * 실패를 표의 행으로 옮긴다. 사유를 확인하지 못한 실패는 [FailureKind.Unexpected] 로 내려앉는다.
 *
 * `when` 이 [CoreDataFailure] 의 모든 갈래를 덮으므로, 서버가 코드를 늘려 도메인 사유가 하나 생기면
 * **컴파일이 먼저 막는다** — 표에 행을 빠뜨린 채 배포되는 길이 없다.
 */
public fun Throwable.toFailureKind(): FailureKind =
    when (this) {
        is CoreDataFailure -> dataFailureKind()
        else -> FailureKind.Unexpected
    }

private fun CoreDataFailure.dataFailureKind(): FailureKind =
    when (this) {
        is CoreDataFailure.NetworkUnavailable -> if (isTimeout) FailureKind.Timeout else FailureKind.NoConnection
        is CoreDataFailure.InvalidInput -> FailureKind.InvalidInput
        is CoreDataFailure.Unauthorized -> FailureKind.AuthExpired
        is CoreDataFailure.Forbidden -> FailureKind.PermissionDenied
        is CoreDataFailure.NotFound -> FailureKind.NotFound
        is CoreDataFailure.DuplicateBoard -> FailureKind.DuplicateBoard
        is CoreDataFailure.LimitExceeded -> FailureKind.LimitExceeded
        is CoreDataFailure.ProfileIncomplete -> FailureKind.ProfileIncomplete
        is CoreDataFailure.ParsingFailed -> FailureKind.ParsingFailed
        is CoreDataFailure.BoardBlocked -> FailureKind.BoardBlocked
        is CoreDataFailure.RateLimited -> FailureKind.RateLimited
        is CoreDataFailure.ServiceUnavailable -> FailureKind.ServiceUnavailable
        is CoreDataFailure.ServerError -> FailureKind.Unexpected
    }

/** 실패를 곧장 표시 계약으로. [FailureKind.display] 의 지름길이다. */
public fun Throwable.toFailureDisplay(
    surface: FailureSurface = FailureSurface.Unspecified,
    itemLimit: Int? = null,
): FailureDisplay = toFailureKind().display(surface, itemLimit)

/**
 * 표를 읽는다.
 *
 * @param surface 화면 문맥. 넘기지 않으면 어느 화면에 붙어도 어긋나지 않는 기본 문구가 나간다.
 * @param itemLimit 상한 초과 문구에 실을 개수. 넘기지 않으면 [surface] 의 상한을 쓴다 — 도메인이
 *  자기 상한을 들고 오는 경우(`FeedFailure.BoardLimitReached.limit`)에만 넘긴다.
 */
public fun FailureKind.display(
    surface: FailureSurface = FailureSurface.Unspecified,
    itemLimit: Int? = null,
): FailureDisplay =
    when (this) {
        FailureKind.NoConnection -> {
            FailureDisplay(
                titleRes = R.string.core_ui_state_network_title,
                descriptionRes = R.string.core_ui_state_network_description,
                action = FailureAction.Retry,
            )
        }

        FailureKind.Timeout -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_timeout_title,
                descriptionRes = R.string.core_ui_failure_timeout_description,
                action = FailureAction.Retry,
            )
        }

        // 같은 값을 그대로 다시 보내면 서버는 같은 400 을 돌려준다. 고칠 자리는 입력란이지 버튼이 아니다.
        FailureKind.InvalidInput -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_invalid_input_title,
                descriptionRes = R.string.core_ui_failure_invalid_input_description,
                action = FailureAction.None,
            )
        }

        FailureKind.AuthExpired -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_auth_expired_title,
                descriptionRes = R.string.core_ui_failure_auth_expired_description,
                action = FailureAction.SignIn,
            )
        }

        FailureKind.PermissionDenied -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_permission_denied_title,
                descriptionRes = R.string.core_ui_failure_permission_denied_description,
                action = FailureAction.None,
            )
        }

        FailureKind.NotFound -> {
            notFoundDisplay(surface)
        }

        FailureKind.DuplicateBoard -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_duplicate_board_title,
                descriptionRes = R.string.core_ui_failure_duplicate_board_description,
                action = FailureAction.None,
            )
        }

        FailureKind.LimitExceeded -> {
            limitExceededDisplay(surface, itemLimit)
        }

        FailureKind.ProfileIncomplete -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_profile_incomplete_title,
                descriptionRes = R.string.core_ui_failure_profile_incomplete_description,
                action = FailureAction.CompleteProfile,
            )
        }

        // 같은 공고를 다시 요청해도 같은 원문을 같은 파서가 읽는다. 바뀌는 것이 없다.
        FailureKind.ParsingFailed -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_parsing_failed_title,
                descriptionRes = R.string.core_ui_failure_parsing_failed_description,
                action = FailureAction.None,
            )
        }

        FailureKind.BoardBlocked -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_board_blocked_title,
                descriptionRes = R.string.core_ui_failure_board_blocked_description,
                action = FailureAction.None,
            )
        }

        FailureKind.RateLimited -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_rate_limited_title,
                descriptionRes = R.string.core_ui_failure_rate_limited_description,
                action = FailureAction.Retry,
            )
        }

        FailureKind.ServiceUnavailable -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_service_unavailable_title,
                descriptionRes = R.string.core_ui_failure_service_unavailable_description,
                action = FailureAction.Retry,
            )
        }

        FailureKind.Unexpected -> {
            unexpectedDisplay(surface)
        }
    }

/** 404 — 없어진 것이 무엇인지는 문맥만 안다. 문맥이 없으면 명사를 말하지 않는다. */
private fun notFoundDisplay(surface: FailureSurface): FailureDisplay =
    when (surface) {
        FailureSurface.Posting -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_not_found_posting_title,
                descriptionRes = R.string.core_ui_failure_not_found_posting_description,
                action = FailureAction.None,
            )
        }

        FailureSurface.Board -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_not_found_board_title,
                descriptionRes = R.string.core_ui_failure_not_found_board_description,
                action = FailureAction.None,
            )
        }

        FailureSurface.Unspecified,
        FailureSurface.Application,
        FailureSurface.ExperienceCard,
        -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_not_found_title,
                descriptionRes = R.string.core_ui_failure_not_found_description,
                action = FailureAction.None,
            )
        }
    }

/**
 * 422 `LIMIT_EXCEEDED` — 상한은 화면마다 다르다(게시판 20 · 경험 카드 30 · 지원서 10).
 *
 * 문맥을 모르면 개수를 말하지 않는다. 틀린 숫자를 말하는 것보다 안 말하는 쪽이 낫다.
 */
private fun limitExceededDisplay(
    surface: FailureSurface,
    itemLimit: Int?,
): FailureDisplay {
    val limit = itemLimit ?: surface.defaultItemLimit()
    return when {
        surface == FailureSurface.Board && limit != null -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_limit_exceeded_board_title,
                descriptionRes = R.string.core_ui_failure_limit_exceeded_board_description,
                action = FailureAction.FreeUpSpace,
                descriptionArgs = listOf(limit),
            )
        }

        surface == FailureSurface.ExperienceCard && limit != null -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_limit_exceeded_experience_title,
                descriptionRes = R.string.core_ui_failure_limit_exceeded_experience_description,
                action = FailureAction.FreeUpSpace,
                descriptionArgs = listOf(limit),
            )
        }

        surface == FailureSurface.Application && limit != null -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_limit_exceeded_application_title,
                descriptionRes = R.string.core_ui_failure_limit_exceeded_application_description,
                action = FailureAction.FreeUpSpace,
                descriptionArgs = listOf(limit),
            )
        }

        else -> {
            FailureDisplay(
                titleRes = R.string.core_ui_failure_limit_exceeded_title,
                descriptionRes = R.string.core_ui_failure_limit_exceeded_description,
                action = FailureAction.FreeUpSpace,
            )
        }
    }
}

/** 500·5xx 와 정체불명 — 본문은 「잠시 후 다시」로 같고, 무엇을 못 했는지만 문맥이 채운다. */
private fun unexpectedDisplay(surface: FailureSurface): FailureDisplay {
    val titleRes =
        when (surface) {
            FailureSurface.Unspecified -> R.string.core_ui_failure_unexpected_title
            FailureSurface.Posting -> R.string.core_ui_failure_unexpected_posting_title
            FailureSurface.Board -> R.string.core_ui_failure_unexpected_board_title
            FailureSurface.Application -> R.string.core_ui_failure_unexpected_application_title
            FailureSurface.ExperienceCard -> R.string.core_ui_failure_unexpected_experience_title
        }
    return FailureDisplay(
        titleRes = titleRes,
        descriptionRes = R.string.core_ui_failure_unexpected_description,
        action = FailureAction.Retry,
    )
}

/** 화면이 상한을 따로 넘기지 않았을 때 쓰는 기본값 — 상한의 정본은 `core:model` 이다. */
private fun FailureSurface.defaultItemLimit(): Int? =
    when (this) {
        FailureSurface.Board -> MAX_BOARDS
        FailureSurface.ExperienceCard -> MAX_EXPERIENCE_CARDS
        FailureSurface.Application -> MAX_PAST_APPLICATIONS
        FailureSurface.Unspecified, FailureSurface.Posting -> null
    }

@Composable
public fun FailureDisplay.title(): String = stringResource(titleRes)

@Composable
public fun FailureDisplay.description(): String = stringResource(descriptionRes, *descriptionArgs.toTypedArray())

/** 행동이 없으면 `null` — 화면은 이 값이 `null` 인지로 버튼을 그릴지 정한다. */
@Composable
public fun FailureDisplay.actionLabel(): String? = action.labelRes?.let { stringResource(it) }

/**
 * 제목과 본문을 한 문장으로 잇는다 — 스낵바처럼 **한 줄만 허용되는 자리**를 위한 것이다.
 *
 * 본문만 띄우면 무슨 일이 일어났는지가 빠지고, 제목만 띄우면 무엇을 하라는지가 빠진다.
 */
@Composable
public fun FailureDisplay.sentence(): String = stringResource(R.string.core_ui_failure_sentence, title(), description())

public fun FailureDisplay.title(resources: Resources): String = resources.getString(titleRes)

public fun FailureDisplay.description(resources: Resources): String = resources.getString(descriptionRes, *descriptionArgs.toTypedArray())

/** [sentence] 의 `Resources` 판 — ViewModel 상태를 문구로 옮기는 매핑 함수용. */
public fun FailureDisplay.sentence(resources: Resources): String =
    resources.getString(R.string.core_ui_failure_sentence, title(resources), description(resources))
