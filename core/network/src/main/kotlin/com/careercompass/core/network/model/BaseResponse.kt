package com.careercompass.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 공통 응답 봉투. 정본은 API_SPEC v0.1 의 `{ "ok": ..., "data": ... }` 규약이다.
 *
 * 제네릭 [T] 는 `data` 필드의 페이로드 타입 — `data` 없는 엔드포인트는 `BaseResponse<Unit>`.
 */
@Serializable
data class BaseResponse<T>(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("data")
    val data: T? = null,
    @SerialName("error")
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String? = null,
    @SerialName("field")
    val field: String? = null,
)

/**
 * 서버가 응답을 마쳤지만 HTTP 또는 공통 응답 봉투 계약이 실패했을 때 throw 된다.
 *
 * 전송 계층 실패인 `IOException` 과 타입 계층을 공유하지 않는다 — 서버 응답을 받은 뒤의 내용 실패다.
 *
 * @property code 봉투 `error.code`(API_SPEC v0.1 §9). HTTP 실패 본문에 봉투가 없으면 `HTTP_<status>`.
 * @property status HTTP 상태. 봉투가 `ok=false` 로 왔지만 HTTP 200 이었던 경우 등 상태를 모르면 null.
 * @property field 서버가 지목한 검증 실패 필드.
 */
class ApiException(
    val code: String,
    val serverMessage: String?,
    fallbackMessage: String,
    val status: Int? = null,
    val field: String? = null,
) : RuntimeException(serverMessage?.takeIf(String::isNotBlank) ?: fallbackMessage)

fun <T : Any> BaseResponse<T>.requireData(): T {
    throwIfEnvelopeFailed(fallbackMessage = "알 수 없는 서버 에러가 발생했습니다.")
    return data ?: throw ApiException(
        code = "EMPTY_DATA",
        serverMessage = null,
        fallbackMessage = "성공했으나 데이터가 비어있습니다.",
    )
}

fun BaseResponse<*>.requireOk() {
    throwIfEnvelopeFailed(fallbackMessage = "요청에 실패했습니다.")
}

private fun BaseResponse<*>.throwIfEnvelopeFailed(fallbackMessage: String) {
    if (!ok) {
        throw ApiException(
            code = error?.code ?: "UNKNOWN",
            serverMessage = error?.message,
            fallbackMessage = fallbackMessage,
            field = error?.field,
        )
    }
}
