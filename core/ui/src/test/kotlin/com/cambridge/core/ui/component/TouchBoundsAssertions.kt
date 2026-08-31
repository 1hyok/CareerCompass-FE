package com.cambridge.core.ui.component

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertTrue

internal fun SemanticsNodeInteraction.assertTouchHeightIsAtLeast(expectedMinHeight: Dp): SemanticsNodeInteraction {
    val semanticsNode = fetchSemanticsNode("Failed to retrieve touch bounds for the node.")
    val actualHeight =
        with(semanticsNode.layoutInfo.density) {
            semanticsNode.touchBoundsInRoot.height.toDp()
        }

    assertTrue(
        "Actual touch height is $actualHeight, expected at least $expectedMinHeight",
        actualHeight >= expectedMinHeight,
    )
    return this
}
