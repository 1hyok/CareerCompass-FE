package com.cambridge.core.data.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * 서버 `data` 객체(API_SPEC v0.1 §3)의 자유 필드를 읽는 공통 도구.
 *
 * 유형별 `data` 는 스키마가 계약에 없어 자유 객체로 받는다. 그래서 「없음·null·빈 문자열」을 전부 「값이
 * 없다」로 같게 다뤄, 서버가 셋 중 무엇으로 비워 보내도 앱의 동작이 갈리지 않게 한다.
 */
internal fun JsonObject.stringOrNull(key: String): String? =
    (this[key]?.takeUnless { it is JsonNull } as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

internal fun JsonObject.requireString(key: String): String = stringOrNull(key) ?: throw IllegalStateException("경험 데이터에 '$key' 가 없습니다.")

internal fun JsonObject.intOrNull(key: String): Int? = (this[key]?.takeUnless { it is JsonNull } as? JsonPrimitive)?.intOrNull

internal fun JsonObject.stringsOrEmpty(key: String): List<String> =
    (this[key]?.takeUnless { it is JsonNull } as? JsonArray)
        ?.jsonArray
        ?.map { it.jsonPrimitive.content }
        ?.filter(String::isNotBlank)
        ?.distinct()
        .orEmpty()
