package com.cambridge.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class CareerCompassThemeTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun darkTheme_providesDarkColorsAndMatchingMaterialScheme() {
        val (colors, scheme) = capture(darkTheme = true)

        assertSame(darkCareerCompassColors, colors)
        assertSchemeFollows(darkCareerCompassColors, scheme)
    }

    @Test
    public fun lightTheme_providesLightColorsAndMatchingMaterialScheme() {
        val (colors, scheme) = capture(darkTheme = false)

        assertSame(lightCareerCompassColors, colors)
        assertSchemeFollows(lightCareerCompassColors, scheme)
    }

    @Test
    @Config(qualifiers = "night")
    public fun defaultTheme_followsSystemNightMode() {
        var colors: CareerCompassColors? = null

        composeRule.setContent {
            CareerCompassTheme {
                colors = CareerCompassTheme.colors
            }
        }

        assertSame(darkCareerCompassColors, colors)
    }

    @Test
    public fun defaultTheme_staysLightWithoutNightQualifier() {
        var colors: CareerCompassColors? = null

        composeRule.setContent {
            CareerCompassTheme {
                colors = CareerCompassTheme.colors
            }
        }

        assertSame(lightCareerCompassColors, colors)
    }

    private fun capture(darkTheme: Boolean): Pair<CareerCompassColors?, ColorScheme?> {
        var colors: CareerCompassColors? = null
        var scheme: ColorScheme? = null

        composeRule.setContent {
            CareerCompassTheme(darkTheme = darkTheme) {
                colors = CareerCompassTheme.colors
                scheme = MaterialTheme.colorScheme
            }
        }

        return colors to scheme
    }

    private fun assertSchemeFollows(
        expected: CareerCompassColors,
        scheme: ColorScheme?,
    ) {
        requireNotNull(scheme)
        assertEquals(expected.surface, scheme.surface)
        assertEquals(expected.onSurface, scheme.onSurface)
        assertEquals(expected.surface, scheme.background)
        assertEquals(expected.actionPrimary, scheme.primary)
        assertEquals(expected.onAction, scheme.onPrimary)
        assertEquals(expected.actionDanger, scheme.error)
        assertEquals(expected.inverseSurface, scheme.inverseSurface)
    }
}
