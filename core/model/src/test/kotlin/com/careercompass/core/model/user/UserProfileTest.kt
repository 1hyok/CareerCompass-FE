package com.careercompass.core.model.user

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {
    @Test
    fun `학점 범위와 완성도 범위를 벗어나면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) { profile(gpa = 4.6) }
        assertThrows(IllegalArgumentException::class.java) { profile(completion = 101) }
    }

    @Test
    fun `희망 직무 코드와 태그는 중복될 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            profile(jobInterests = listOf(JobInterest("backend", 1), JobInterest("backend", 2)))
        }
        assertThrows(IllegalArgumentException::class.java) { profile(tags = listOf("AI", "AI")) }
    }

    @Test
    fun `빈 수정 요청을 구분한다`() {
        assertTrue(UserProfileUpdate().isEmpty)
        assertTrue(!UserProfileUpdate(name = "정일혁").isEmpty)
    }

    private fun profile(
        gpa: Double? = 3.87,
        completion: Int = 78,
        jobInterests: List<JobInterest> = listOf(JobInterest("backend", 1)),
        tags: List<String> = listOf("AI"),
    ) = UserProfile(
        id = 1,
        name = "정일혁",
        school = "건국대학교",
        department = "컴퓨터공학부",
        gpa = gpa,
        gradYear = 2027,
        jobInterests = jobInterests,
        tags = tags,
        onboardingDone = true,
        completion = completion,
    )
}
