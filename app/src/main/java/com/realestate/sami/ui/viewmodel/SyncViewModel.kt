package com.realestate.sami.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.realestate.sami.sync.AccessTokenResult
import com.realestate.sami.sync.DrivePickerActivity
import com.realestate.sami.sync.GoogleAuthManager
import com.realestate.sami.sync.JoinTeamResult
import com.realestate.sami.sync.SyncManager
import com.realestate.sami.sync.SyncPreferences
import com.realestate.sami.sync.SyncResult
import com.realestate.sami.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR }

data class SyncUiState(
    val account: GoogleSignInAccount? = null,
    val status: SyncStatus = SyncStatus.IDLE,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null,
    val pendingConsentIntent: Intent? = null,
    /** شناسه‌ی پوشه‌ی تیمی فعلی (بعد از اولین sync موفق یا join دستی) — برای نمایش/اشتراک‌گذاری با بقیه اعضا. */
    val teamFolderId: String? = null,
    /** برچسب دلخواه تیم که فقط در UI نمایش داده می‌شود (مستقل از نام واقعی پوشه‌ی Drive). */
    val teamDisplayName: String = "",
    val joinTeamMessage: String? = null,
    val isJoiningTeam: Boolean = false,
    /** وقتی روشنه، sync دوره‌ای پس‌زمینه فقط روی Wi-Fi اجرا می‌شود. */
    val autoSyncWifiOnly: Boolean = false,
    /** وقتی مقدار داره، صفحه باید DrivePickerActivity رو با این Intent باز کنه (یک‌بار‌مصرف). */
    val pickerLaunchIntent: Intent? = null,
    /** پیام خطای مخصوص Picker — درست کنار دکمه‌ی «انتخاب پوشه» نمایش داده می‌شه، نه در کارت بالای صفحه؛
     *  چون این خطاها (کلید تنظیم‌نشده، توکن نگرفتن) دقیقاً محل کلیک کاربر رو نشونه می‌گیرن. */
    val pickerErrorMessage: String? = null
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: GoogleAuthManager,
    private val syncManager: SyncManager,
    private val syncPrefs: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SyncUiState(
            account = authManager.getSignedInAccount(context),
            lastSyncedAt = syncPrefs.lastSyncedAt,
            teamFolderId = syncPrefs.teamFolderId,
            teamDisplayName = syncPrefs.teamDisplayName,
            autoSyncWifiOnly = syncPrefs.autoSyncWifiOnly
        )
    )
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun getSignInIntent(): Intent = authManager.getSignInIntent(context)

    fun onSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
            _uiState.value = _uiState.value.copy(account = account, errorMessage = null)
            if (account != null) syncNow()
        } catch (e: ApiException) {
            // کد ۱۰ (DEVELOPER_ERROR) یعنی SHA-1 امضای این بیلد در Google Cloud Console ثبت نشده
            _uiState.value = _uiState.value.copy(
                errorMessage = "ورود ناموفق (کد ${e.statusCode}): ${e.message}"
            )
        }
    }

    fun onSignInError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun signOut() {
        authManager.signOut(context) {
            _uiState.value = SyncUiState(
                teamFolderId = syncPrefs.teamFolderId,
                teamDisplayName = syncPrefs.teamDisplayName,
                autoSyncWifiOnly = syncPrefs.autoSyncWifiOnly
            )
        }
    }

    /** بعد از این‌که کاربر consent Drive رو تایید کرد (از activity result)، دوباره sync رو اجرا کن. */
    fun onConsentGranted() {
        _uiState.value = _uiState.value.copy(pendingConsentIntent = null)
        syncNow()
    }

    fun syncNow() {
        val account = _uiState.value.account ?: return
        _uiState.value = _uiState.value.copy(status = SyncStatus.SYNCING, errorMessage = null)
        viewModelScope.launch {
            when (val result = syncManager.syncNow(account)) {
                is SyncResult.Success -> _uiState.value = _uiState.value.copy(
                    status = SyncStatus.SUCCESS,
                    lastSyncedAt = result.syncedAt,
                    teamFolderId = syncPrefs.teamFolderId
                )
                is SyncResult.ConsentRequired -> _uiState.value = _uiState.value.copy(
                    status = SyncStatus.IDLE,
                    pendingConsentIntent = result.intent
                )
                is SyncResult.Failure -> _uiState.value = _uiState.value.copy(
                    status = SyncStatus.ERROR,
                    errorMessage = result.message
                )
            }
        }
    }

    /**
     * به یک پوشه‌ی تیمی موجود (که یک همکار ساخته و Share کرده) با شناسه‌ی Drive می‌پیوندد،
     * تا به‌جای ساخت یه پوشه‌ی جدا و جدید، مستقیم به همون داده‌ی تیمی وصل بشه.
     * بعد از پیوستن موفق، یه sync کامل خودکار اجرا می‌شه.
     *
     * منبع folderId می‌تونه یا ورودی دستی کاربر باشه یا نتیجه‌ی [onFolderPicked] از Google Picker —
     * منطق پیوستن برای هر دو یکسانه.
     */
    fun joinTeamFolder(folderId: String) {
        val account = _uiState.value.account ?: return
        _uiState.value = _uiState.value.copy(isJoiningTeam = true, joinTeamMessage = null)
        viewModelScope.launch {
            when (val result = syncManager.joinTeamFolder(account, folderId)) {
                is JoinTeamResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isJoiningTeam = false,
                        teamFolderId = syncPrefs.teamFolderId,
                        teamDisplayName = syncPrefs.teamDisplayName,
                        joinTeamMessage = "به پوشه‌ی «${result.folderName}» وصل شدی"
                    )
                    syncNow()
                }
                is JoinTeamResult.Failure -> _uiState.value = _uiState.value.copy(
                    isJoiningTeam = false,
                    joinTeamMessage = result.message
                )
            }
        }
    }

    fun clearJoinTeamMessage() {
        _uiState.value = _uiState.value.copy(joinTeamMessage = null)
    }

    /**
     * توکن دسترسی فعلی رو می‌گیره و Intent مربوط به DrivePickerActivity رو در state می‌ذاره تا
     * صفحه (SyncSettingsScreen) اون رو با یک ActivityResultLauncher باز کنه. اگه هنوز consent Drive
     * داده نشده، مثل بقیه‌ی جاها pendingConsentIntent ست می‌شه.
     */
    fun requestFolderPicker() {
        val account = _uiState.value.account ?: return
        _uiState.value = _uiState.value.copy(pickerErrorMessage = null)
        viewModelScope.launch {
            when (val tokenResult = authManager.getAccessToken(context, account)) {
                is AccessTokenResult.Success -> _uiState.value = _uiState.value.copy(
                    pickerLaunchIntent = Intent(context, DrivePickerActivity::class.java)
                        .putExtra(DrivePickerActivity.EXTRA_ACCESS_TOKEN, tokenResult.token)
                )
                is AccessTokenResult.ConsentRequired -> _uiState.value = _uiState.value.copy(
                    pendingConsentIntent = tokenResult.intent
                )
                is AccessTokenResult.Failure -> _uiState.value = _uiState.value.copy(
                    pickerErrorMessage = tokenResult.message
                )
            }
        }
    }

    /** بعد از این‌که صفحه Intent رو لانچ کرد، یک‌بار‌مصرف بودنش رو با پاک کردن از state تضمین کن. */
    fun clearPickerLaunchIntent() {
        _uiState.value = _uiState.value.copy(pickerLaunchIntent = null)
    }

    /**
     * نتیجه‌ی DrivePickerActivity وقتی RESULT_OK نبوده. اگه [reason] مقدار داشته باشه (یعنی کاربر
     * خودش لغو نکرده، بلکه Picker اصلاً به‌خاطر یه مشکل تنظیمات باز نشده)، پیام مناسب رو نشون بده؛
     * وگرنه (لغو دستی خود کاربر از داخل Picker) هیچ پیامی لازم نیست.
     */
    fun onPickerCancelled(reason: String?) {
        if (reason == DrivePickerActivity.REASON_CONFIG_MISSING) {
            _uiState.value = _uiState.value.copy(
                pickerErrorMessage = context.getString(com.realestate.sami.R.string.sync_picker_config_missing)
            )
        }
    }

    fun clearPickerErrorMessage() {
        _uiState.value = _uiState.value.copy(pickerErrorMessage = null)
    }

    /** نتیجه‌ی DrivePickerActivity؛ اگه کاربر پوشه‌ای انتخاب کرده باشه (لغو نکرده باشه)، بهش می‌پیونده. */
    fun onFolderPicked(folderId: String?) {
        if (!folderId.isNullOrBlank()) joinTeamFolder(folderId)
    }

    /** تغییر برچسب دلخواه تیم؛ فقط محلی است، هیچ درخواستی به Drive نمی‌فرستد. */
    fun setTeamDisplayName(name: String) {
        syncPrefs.teamDisplayName = name
        _uiState.value = _uiState.value.copy(teamDisplayName = name)
    }

    /**
     * قطع اتصال این دستگاه از تیم فعلی: شناسه‌ی پوشه، شناسه‌های کش‌شده‌ی فایل‌ها و برچسب تیم پاک
     * می‌شوند تا بشه بی‌دردسر (بدون باقی‌ماندن state تیم قبلی که باعث تکرار/فورک داده می‌شه) به یک
     * تیم دیگه پیوست یا یک تیم تازه ساخت. داده‌های محلی (ملک/متقاضی ثبت‌شده روی همین دستگاه) پاک
     * نمی‌شوند و همچنان قابل‌استفاده‌اند.
     */
    fun leaveTeam() {
        syncPrefs.clearTeamState()
        _uiState.value = _uiState.value.copy(
            teamFolderId = null,
            teamDisplayName = "",
            lastSyncedAt = null,
            joinTeamMessage = null
        )
    }

    /** تغییر تنظیم «sync خودکار فقط با Wi-Fi»؛ ذخیره می‌شود و کار دوره‌ای فوراً با محدودیت شبکه‌ی جدید دوباره زمان‌بندی می‌شود. */
    fun setAutoSyncWifiOnly(enabled: Boolean) {
        syncPrefs.autoSyncWifiOnly = enabled
        _uiState.value = _uiState.value.copy(autoSyncWifiOnly = enabled)
        SyncWorker.schedulePeriodic(context, wifiOnly = enabled, policy = ExistingPeriodicWorkPolicy.REPLACE)
    }
}
