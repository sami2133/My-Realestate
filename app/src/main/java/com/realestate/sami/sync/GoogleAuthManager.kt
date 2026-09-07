package com.realestate.sami.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** نتیجه‌ی تلاش برای گرفتن access token برای Drive. */
sealed class AccessTokenResult {
    data class Success(val token: String) : AccessTokenResult()

    /** کاربر باید یک بار دیگر رضایت (consent) بده — این Intent را با startActivityForResult باز کن. */
    data class ConsentRequired(val intent: Intent) : AccessTokenResult()
    data class Failure(val message: String) : AccessTokenResult()
}

/**
 * مدیریت ورود با گوگل و گرفتن access token با دسترسی Drive.
 * از GoogleSignInClient کلاسیک استفاده می‌کند تا نیازی به google-services.json/Firebase نباشد.
 */
@Singleton
class GoogleAuthManager @Inject constructor() {

    private fun signInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveConstants.DRIVE_SCOPE))
            .build()

    fun getClient(context: Context): GoogleSignInClient =
        GoogleSignIn.getClient(context, signInOptions())

    fun getSignInIntent(context: Context): Intent = getClient(context).signInIntent

    /** آخرین حسابی که وارد شده و دسترسی Drive را هم تایید کرده (اگر باشد). */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val hasDriveScope = GoogleSignIn.hasPermissions(account, Scope(DriveConstants.DRIVE_SCOPE))
        return if (hasDriveScope) account else null
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        getClient(context).signOut().addOnCompleteListener { onComplete() }
    }

    /**
     * تلاش برای گرفتن access token. اگر کاربر هنوز رضایت Drive رو نداده،
     * ConsentRequired برمی‌گرده که باید Intent داخلش رو با activity result launcher باز کنی.
     */
    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): AccessTokenResult =
        withContext(Dispatchers.IO) {
            val androidAccount: Account = account.account
                ?: return@withContext AccessTokenResult.Failure("حساب گوگل معتبر نیست، دوباره وارد شو")
            try {
                val token = GoogleAuthUtil.getToken(context, androidAccount, DriveConstants.DRIVE_SCOPE_OAUTH)
                AccessTokenResult.Success(token)
            } catch (e: UserRecoverableAuthException) {
                val recoveryIntent = e.intent
                if (recoveryIntent != null) {
                    AccessTokenResult.ConsentRequired(recoveryIntent)
                } else {
                    AccessTokenResult.Failure("نیاز به تایید مجدد دسترسی به Drive - لطفاً دوباره امتحان کن")
                }
            } catch (e: Exception) {
                AccessTokenResult.Failure(e.message ?: "خطا در دریافت مجوز دسترسی به Drive")
            }
        }
}
