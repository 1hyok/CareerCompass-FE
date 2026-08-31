package com.cambridge.careercompass_fe.test

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.test.platform.io.PlatformTestStorageRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.BufferedOutputStream
import java.io.IOException

class FailureArtifactRule(
    private val captureBitmap: () -> Bitmap,
) : TestWatcher() {
    override fun failed(
        e: Throwable,
        description: Description,
    ) {
        runCatching {
            val artifactPath = "failure-artifacts/${description.artifactFileName()}"
            val bitmap = captureBitmap()

            BufferedOutputStream(
                PlatformTestStorageRegistry.getInstance().openOutputFile(artifactPath),
            ).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                    throw IOException("Bitmap refused to encode as PNG")
                }
            }
        }.onFailure { artifactFailure ->
            Log.e(
                TAG,
                "Could not capture failure artifact for ${description.displayName} " +
                    "after ${e.javaClass.simpleName}",
                artifactFailure,
            )
        }
    }

    private fun Description.artifactFileName(): String {
        val rawName = "${className.orEmpty()}_${methodName.orEmpty()}_api${Build.VERSION.SDK_INT}.png"
        return rawName.replace(UNSAFE_FILE_NAME_CHARACTER, "_")
    }

    private companion object {
        const val TAG = "FailureArtifactRule"
        const val PNG_QUALITY = 100
        val UNSAFE_FILE_NAME_CHARACTER = Regex("[^A-Za-z0-9._-]")
    }
}
