package com.careercompass.core.model.experience

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 태그·링크 상한이 **모델의 불변식**이라는 계약 (#208).
 *
 * 예전에는 이 규칙이 온보딩 시트 계약에만 있어 모델이 태그 100개짜리 카드를 만들 수 있었다. 입력 경로가
 * 늘면 상한이 한 벌씩 늘고, 두 벌은 반드시 어긋난다.
 */
class ExperienceLimitsTest {
    private fun project(
        techs: List<String> = emptyList(),
        link: String? = null,
    ) = ExperienceDetails.Project(role = null, techs = techs, summary = null, link = link)

    @Test
    fun `기술 태그는 개수 상한을 넘을 수 없다`() {
        val tags = List(MAX_EXPERIENCE_TECH_TAGS + 1) { "tech$it" }

        assertThrows(IllegalArgumentException::class.java) { project(techs = tags) }
        // 상한만큼은 그대로 들어간다 — 상한은 「넘지 않는다」이지 「줄인다」가 아니다.
        assertTrue(project(techs = tags.dropLast(1)).techs.size == MAX_EXPERIENCE_TECH_TAGS)
    }

    @Test
    fun `기술 태그 한 개의 길이도 상한이 있다`() {
        assertThrows(IllegalArgumentException::class.java) {
            project(techs = listOf("a".repeat(MAX_EXPERIENCE_TECH_TAG_LENGTH + 1)))
        }
    }

    @Test
    fun `링크는 http 와 https 절대 주소만 받는다`() {
        // 서버가 받아 다시 사용자에게 보여 주고 눌리는 값이라 스킴을 좁힌다.
        assertTrue(isAllowedExperienceLink("https://github.com/Team-CareerCompass/CareerCompass-FE"))
        assertTrue(isAllowedExperienceLink(" http://example.com "))
        assertFalse(isAllowedExperienceLink("javascript:alert(1)"))
        assertFalse(isAllowedExperienceLink("ftp://example.com"))
        assertFalse(isAllowedExperienceLink("github.com/foo"))
        assertFalse(isAllowedExperienceLink("https://"))
        assertFalse(isAllowedExperienceLink(""))
        assertFalse(isAllowedExperienceLink("https://example.com/" + "a".repeat(MAX_EXPERIENCE_LINK_LENGTH)))
    }

    @Test
    fun `카드는 그 판정을 통과하지 못하는 링크를 가질 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) { project(link = "javascript:alert(1)") }
        assertThrows(IllegalArgumentException::class.java) {
            project(link = "https://example.com/" + "a".repeat(MAX_EXPERIENCE_LINK_LENGTH))
        }
    }
}
