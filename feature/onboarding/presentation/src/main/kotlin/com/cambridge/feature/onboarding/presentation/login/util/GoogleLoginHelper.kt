package com.cambridge.feature.onboarding.presentation.login.util

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.cambridge.core.domain.error.CoreAuthFailure
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Credential Manager 로 Google ID 토큰을 얻는다.
 *
 * 1차는 [GetGoogleIdOption](기기에 로그인된 계정 목록). 계정이 하나도 없어 [NoCredentialException] 이 나면
 * [GetSignInWithGoogleOption](Google 로그인 버튼 흐름)으로 한 번 더 시도한다. 취소는
 * [CoreAuthFailure.UserCancelledAuth] 로 번역한다.
 *
 * [serverClientId] 는 `BuildConfig.GOOGLE_WEB_CLIENT_ID` 다 — 비어 있으면 SDK 를 부르지 않고 설정 오류로 끝낸다.
 */
internal object GoogleLoginHelper {
    suspend fun requestGoogleIdToken(
        activity: Activity,
        serverClientId: String,
    ): Result<String> {
        if (serverClientId.isBlank()) {
            return Result.failure(IllegalStateException("GOOGLE_WEB_CLIENT_ID is not configured"))
        }
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()
        val first = requestIdToken(credentialManager, activity, googleIdOption)
        if (first.exceptionOrNull() !is NoCredentialException) return first

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        return requestIdToken(credentialManager, activity, signInOption)
    }

    private suspend fun requestIdToken(
        credentialManager: CredentialManager,
        activity: Activity,
        option: CredentialOption,
    ): Result<String> =
        try {
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(CoreAuthFailure.UserCancelledAuth())
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(e)
        }
}
