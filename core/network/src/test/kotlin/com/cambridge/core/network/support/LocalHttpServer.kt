package com.cambridge.core.network.support

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * JDK 내장 [HttpServer] 로 띄우는 로컬 HTTP 서버 — 외부 의존성·Docker 없이 OkHttp 를 실제 소켓까지 통과시킨다.
 *
 * 요청을 병렬로 받아야 하는 동시성 테스트용이라 cached thread pool 을 실행기로 쓴다.
 */
internal class LocalHttpServer(
    private val route: (Recorded) -> Reply,
) : AutoCloseable {
    internal data class Recorded(
        val path: String,
        val authorization: String?,
    )

    internal data class Reply(
        val code: Int,
        val body: String = "",
    )

    private val server: HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            executor = Executors.newCachedThreadPool()
            createContext("/") { exchange ->
                val recorded = Recorded(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"))
                requests += recorded
                val reply = route(recorded)
                val bytes = reply.body.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(reply.code, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    val requests: CopyOnWriteArrayList<Recorded> = CopyOnWriteArrayList()

    val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    override fun close() {
        server.stop(0)
    }
}
