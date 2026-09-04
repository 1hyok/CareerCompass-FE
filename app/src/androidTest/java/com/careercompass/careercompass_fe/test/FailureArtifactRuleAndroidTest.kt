package com.careercompass.careercompass_fe.test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.io.PlatformTestStorageRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.File

@RunWith(AndroidJUnit4::class)
class FailureArtifactRuleAndroidTest {
    @Test
    fun failedStatement_writesSanitizedPngAndPreservesOriginalFailure() {
        val sourceBitmap =
            Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.MAGENTA)
            }
        val originalFailure = IllegalStateException("original test failure")
        val description =
            Description.createTestDescription(
                "../Failure/Artifact Rule",
                "writes:png?",
            )
        val failingStatement =
            object : Statement() {
                override fun evaluate(): Unit = throw originalFailure
            }

        val thrown =
            runCatching {
                FailureArtifactRule { sourceBitmap }
                    .apply(failingStatement, description)
                    .evaluate()
            }.exceptionOrNull()

        assertSame(originalFailure, thrown)

        val artifactPath =
            "failure-artifacts/.._Failure_Artifact_Rule_writes_png__api${Build.VERSION.SDK_INT}.png"
        val artifactUri = PlatformTestStorageRegistry.getInstance().getOutputFileUri(artifactPath)
        val artifactFile = File(requireNotNull(artifactUri.path))
        assertTrue("Expected failure artifact at $artifactUri", artifactFile.isFile)
        assertTrue("Failure artifact must not be empty", artifactFile.length() > 0L)

        val decoded = BitmapFactory.decodeFile(artifactFile.absolutePath)
        assertNotNull("Failure artifact must be a decodable PNG", decoded)
        assertEquals(2, decoded.width)
        assertEquals(3, decoded.height)
    }
}
