package com.cambridge.core.data.failure

import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.network.model.ApiException
import java.io.IOException

/**
 * `ApiException`(서버 봉투 실패)·`IOException`(전송 실패)을 도메인 사유로 옮긴다 — API_SPEC v0.1 §9.
 *
 * 사유가 확인된 실패만 치환하고 나머지는 원본 그대로 두어 소비처가 일반 문구로 내려앉는다. 취소는 다시 보지
 * 않는다 — 호출부가 전부 `runCatchingCancellable` 이라 `CancellationException` 이 [Result] 에 담기지 않는다.
 */
internal fun <T> Result<T>.mapDataFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> Result.failure(exception.toDataFailure())
        is IOException -> Result.failure(CoreDataFailure.NetworkUnavailable(exception))
        else -> this
    }

/** 인증 API 전용 — 소셜 로그인 거절과 전송 실패만 인증 사유로 갈고, 나머지는 데이터 사유와 같다. */
internal fun <T> Result<T>.mapAuthFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_AUTH_REQUIRED, CODE_AUTH_INVALID -> Result.failure(CoreAuthFailure.SocialLoginRejected(exception))
                else -> Result.failure(exception.toDataFailure())
            }
        }

        is IOException -> {
            Result.failure(CoreAuthFailure.NetworkUnavailable(exception))
        }

        else -> {
            this
        }
    }

internal fun ApiException.toDataFailure(): Throwable =
    when (code) {
        CODE_INVALID_INPUT -> {
            CoreDataFailure.InvalidInput(code, field, this)
        }

        CODE_AUTH_REQUIRED, CODE_AUTH_INVALID -> {
            CoreDataFailure.Unauthorized(code, this)
        }

        CODE_PERMISSION_DENIED -> {
            CoreDataFailure.Forbidden(code, this)
        }

        CODE_RESOURCE_NOT_FOUND, CODE_POSTING_NOT_FOUND -> {
            CoreDataFailure.NotFound(code, this)
        }

        CODE_DUPLICATE_BOARD -> {
            CoreDataFailure.DuplicateBoard(code, this)
        }

        CODE_LIMIT_EXCEEDED -> {
            CoreDataFailure.LimitExceeded(code, this)
        }

        CODE_PROFILE_INCOMPLETE -> {
            CoreDataFailure.ProfileIncomplete(code, this)
        }

        CODE_PARSING_FAILED -> {
            CoreDataFailure.ParsingFailed(code, this)
        }

        CODE_BOARD_BLOCKED -> {
            CoreDataFailure.BoardBlocked(code, this)
        }

        CODE_RATE_LIMITED -> {
            CoreDataFailure.RateLimited(code, this)
        }

        CODE_LLM_UNAVAILABLE -> {
            CoreDataFailure.ServiceUnavailable(code, this)
        }

        CODE_INTERNAL_ERROR -> {
            CoreDataFailure.ServerError(code, this)
        }

        else -> {
            when (status) {
                401 -> CoreDataFailure.Unauthorized(code, this)
                403 -> CoreDataFailure.Forbidden(code, this)
                404 -> CoreDataFailure.NotFound(code, this)
                429 -> CoreDataFailure.RateLimited(code, this)
                503 -> CoreDataFailure.ServiceUnavailable(code, this)
                in 500..599 -> CoreDataFailure.ServerError(code, this)
                else -> this
            }
        }
    }

private const val CODE_INVALID_INPUT = "INVALID_INPUT"
private const val CODE_AUTH_REQUIRED = "AUTH_REQUIRED"
private const val CODE_AUTH_INVALID = "AUTH_INVALID"
private const val CODE_PERMISSION_DENIED = "PERMISSION_DENIED"
private const val CODE_RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND"
private const val CODE_POSTING_NOT_FOUND = "POSTING_NOT_FOUND"
private const val CODE_DUPLICATE_BOARD = "DUPLICATE_BOARD"
private const val CODE_LIMIT_EXCEEDED = "LIMIT_EXCEEDED"
private const val CODE_PROFILE_INCOMPLETE = "PROFILE_INCOMPLETE"
private const val CODE_PARSING_FAILED = "PARSING_FAILED"
private const val CODE_BOARD_BLOCKED = "BOARD_BLOCKED"
private const val CODE_RATE_LIMITED = "RATE_LIMITED"
private const val CODE_LLM_UNAVAILABLE = "LLM_UNAVAILABLE"
private const val CODE_INTERNAL_ERROR = "INTERNAL_ERROR"
