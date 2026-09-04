package com.careercompass.core.common.reporting

import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.error.CoreDataFailure
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ProtocolException
import java.net.SocketException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException

/** 리포팅 속성 키 — 전송 계층에서 끝난 실패의 원인 갈래. 값은 [TransportFailureKind.key] 다. */
public const val ERROR_REPORT_KEY_TRANSPORT: String = "transport_failure"

/**
 * 전송 계층에서 끝난 실패의 원인 갈래 — **예외 타입만** 보고 가른다.
 *
 * 문구 매칭에 기대지 않는다. OkHttp·JDK 의 메시지는 버전마다 바뀌고, 게시판 URL 감지처럼 사용자가
 * 임의 주소를 넣는 경로에서는 그 문구에 host 가 실린다 — 판정 근거로 삼으면 콘솔로 새어 나간다.
 */
public enum class TransportFailureKind(
    public val key: String,
) {
    /**
     * 사용자 환경의 일시적 전송 실패 — DNS 실패·타임아웃·연결 거부.
     *
     * 지하철에서 앱을 켠 횟수만큼 쌓이므로 원칙적으로 기록하지 않는다. 다만 (원인, 단계) 조합마다
     * 세션 첫 건은 남긴다 — 통째로 지우면 「이 API 만 타임아웃한다」 같은 신호까지 함께 사라진다.
     */
    Transient("transient"),

    /**
     * 설정·프로토콜 결함 — cleartext 차단·TLS 회귀·잘리거나 깨진 응답.
     *
     * 사용자 환경이 아니라 우리가 고칠 결함이므로 접지 않고 그대로 기록한다.
     */
    Defect("defect"),
}

/**
 * 전송 계층에서 끝난 실패면 그 갈래를, 아니면 `null` 을 돌려준다.
 *
 * [CoreDataFailure.NetworkUnavailable] 은 data 계층이 `IOException` 을 **전부** 접어 만든 값이라 그
 * 자체로는 원인을 말해 주지 않는다. 안에 든 원본 예외를 꺼내야 cleartext 차단·TLS 회귀가 「사용자가
 * 오프라인」과 구분된다.
 */
public fun Throwable.transportFailureKind(): TransportFailureKind? = transportFailureCause()?.failureKind()

/**
 * 단계 속성을 붙여 흡수된 실패를 기록한다 — **기록 여부 판정은 이 함수 한 곳에만 둔다.**
 *
 * 기능별 리포팅 헬퍼(피드·온보딩)는 속성 키와 단계 값만 다르고 규칙은 같다. 규칙을 각자 들고 있으면
 * 한쪽만 고쳐진 채로 갈라지므로, 여기로 모아 [TransportFailureKind] 판정과 세션 중복 제거를 공유한다.
 *
 * @param stageKey 단계를 담을 속성 키(예: `feed_stage`).
 * @param stage 단계 값. 세션 지문의 한 축이라, 같은 원인이라도 단계가 다르면 따로 샘플링된다.
 * @param attributes 단계 외에 이 실패에만 붙는 속성. 개인정보·자격증명은 넣지 않는다.
 */
public fun ErrorReporter.recordStagedFailure(
    stageKey: String,
    stage: String,
    throwable: Throwable,
    attributes: Map<String, String> = emptyMap(),
) {
    if (throwable.isAnnouncedOutcome()) return

    val cause = throwable.transportFailureCause()
    val kind = cause?.failureKind()
    if (cause != null && kind == TransportFailureKind.Transient) {
        if (!isFirstInSession("${cause.javaClass.name}@$stage")) return
    }

    recordFailure(
        throwable = throwable,
        attributes =
            buildMap {
                put(stageKey, stage)
                putAll(attributes)
                kind?.let { put(ERROR_REPORT_KEY_TRANSPORT, it.key) }
            },
    )
}

/**
 * 화면이 사유를 그대로 안내하는, 우리가 고칠 것이 아닌 결말.
 *
 * 503 `LLM_UNAVAILABLE` 은 서버가 스스로 알린 계획된 상태고, 인증 취소는 사용자의 의도된 행동이다.
 * 코루틴 취소는 [ErrorReporter.recordFailure] 가 이미 거른다.
 */
private fun Throwable.isAnnouncedOutcome(): Boolean =
    this is CoreDataFailure.ServiceUnavailable || this is CoreAuthFailure.UserCancelledAuth

/** 전송 실패의 원본 예외. 도메인 사유로 접힌 것은 풀고, 접히지 않은 `IOException` 은 그대로 쓴다. */
private fun Throwable.transportFailureCause(): IOException? =
    when (this) {
        is CoreDataFailure.NetworkUnavailable -> transportCause
        is CoreAuthFailure.NetworkUnavailable -> transportCause
        is IOException -> this
        else -> null
    }

/**
 * 첫 갈래는 설정·프로토콜 계열이다: cleartext 차단([UnknownServiceException])·인증서와 핀닝·TLS 회귀·
 * 잘리거나 깨진 응답. 둘째 갈래는 전송이 상대에 닿지 못한 경우로, `SocketTimeoutException` 은
 * [InterruptedIOException] 이, `ConnectException`·`NoRouteToHostException` 은 [SocketException] 이 잡는다.
 * 정체를 모르는 나머지 `IOException` 은 결함 쪽에 둔다 — 조용히 사라지는 쪽이 잡음보다 비싸다.
 */
private fun IOException.failureKind(): TransportFailureKind =
    when (this) {
        is SSLException, is UnknownServiceException, is ProtocolException, is EOFException -> TransportFailureKind.Defect
        is UnknownHostException, is InterruptedIOException, is SocketException -> TransportFailureKind.Transient
        else -> TransportFailureKind.Defect
    }

/**
 * 세션 표본 지문 — 리포터 인스턴스별로 센다.
 *
 * 앱에는 [ErrorReporter] 구현이 `@Singleton` 하나뿐이라 이 집합의 수명이 곧 프로세스 수명이고,
 * 리포터를 새로 만드는 테스트는 별도 장치 없이 격리된다. 키를 약참조로 잡아 테스트가 만든 리포터가
 * 프로세스에 쌓이지 않게 한다.
 */
private val sessionSamples = WeakHashMap<ErrorReporter, MutableSet<String>>()

/** [fingerprint] 가 이 세션에서 처음이면 등록하고 `true`. 재시도 폭주가 표본을 독점하지 못하게 한다. */
private fun ErrorReporter.isFirstInSession(fingerprint: String): Boolean {
    val samples =
        synchronized(sessionSamples) {
            sessionSamples.getOrPut(this) { ConcurrentHashMap.newKeySet<String>() }
        }
    return samples.add(fingerprint)
}
