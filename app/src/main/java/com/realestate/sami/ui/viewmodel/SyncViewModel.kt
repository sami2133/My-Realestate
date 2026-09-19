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
import com.realestate.sami.sync.ImagesFolderResult
import com.realestate.sami.sync.JoinTeamResult
import com.realestate.sami.sync.RenameTeamFolderResult
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
    val isRenamingTeamFolder: Boolean = false,
    val renameTeamFolderMessage: String? = null,
    val joinTeamMessage: String? = null,
    val isJoiningTeam: Boolean = false,
    /** وقتی روشنه، sync دوره‌ای پس‌زمینه فقط روی Wi-Fi اجرا می‌شود. */
    val autoSyncWifiOnly: Boolean = false,
    /** وقتی مقدار داره، صفحه باید DrivePickerActivity رو با این Intent باز کنه (یک‌بار‌مصرف).
     *  هم برای انتخاب پوشه‌ی تیمی استفاده می‌شه، هم برای «دریافت تصاویر تیم» (با mode متفاوت). */
    val pickerLaunchIntent: Intent? = null,
    /** خطای مخصوص دکمه‌ی «دریافت تصاویر تیم» (مثلاً هنوز به هیچ تیمی وصل نیستی). */
    val fetchImagesErrorMessage: String? = null,
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
    /** مرحله‌ای که Picker چندانتخابیِ در حال اجرا برایش باز شده — چون خروجی خامِ DrivePickerActivity
     *  (یک آرایه‌ی id) به‌تنهایی نمی‌گه این انتخاب برای «فایل‌های سطح‌بالای پوشه‌ی تیمی» بوده،
     *  «زیرپوشه‌ی images»، یا صرفاً دکمه‌ی مستقل «دریافت تصاویر تیم» — و هرکدوم قدم بعدی متفاوتی دارن. */
    private enum class FilesPickerStep { STANDALONE_IMAGES, JOIN_TOP_LEVEL_FILES, JOIN_IMAGES }
    /** null یعنی «الان وسط هیچ زنجیره‌ی Picker چندفایلی‌ای نیستیم» — یعنی نتیجه‌ی در‌راه، مربوط به
     *  همون Picker تک‌انتخابیِ ساده‌ی انتخاب پوشه‌ست (چه join بار اول باشه، چه از قبل عضو تیم بودن). */
    private var pendingFilesPickerStep: FilesPickerStep? = null

    /**
     * به یک پوشه‌ی تیمی موجود (که یک همکار ساخته و Share کرده) با شناسه‌ی Drive می‌پیوندد،
     * تا به‌جای ساخت یه پوشه‌ی جدا و جدید، مستقیم به همون داده‌ی تیمی وصل بشه.
     *
     * بعد از پیوستنِ خودِ پوشه، **مستقیم syncNow صدا زده نمی‌شه** — چون با اسکوپ drive.file، صرفِ
     * انتخاب پوشه (نه فایل‌های داخلش) هیچ مجوزی به فایل‌های JSON از‌قبل‌موجودی که یک همکار روی
     * دستگاه دیگه ساخته نمی‌ده؛ اگه اینجا syncNow بزنیم، دقیقاً همون باگ اولیه تکرار می‌شه: چون
     * resolveExistingFile چیزی پیدا نمی‌کنه، یک نسخه‌ی موازی و خالی از فایل‌ها ساخته می‌شه. برای
     * همین به‌جاش [requestJoinFilesPicker] صدا زده می‌شه تا کاربر صریحاً همون فایل‌ها رو انتخاب کنه.
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
                        joinTeamMessage = "به پوشه‌ی «${result.folderName}» وصل شدی — در صفحه‌ی بعد، فایل‌ها و عکس‌های موجودش رو انتخاب کن تا این دستگاه هم بتونه بخونتشون"
                    )
                    requestJoinFilesPicker(folderId)
                }
                is JoinTeamResult.Failure -> _uiState.value = _uiState.value.copy(
                    isJoiningTeam = false,
                    joinTeamMessage = result.message
                )
            }
        }
    }

    /**
     * قدم اول زنجیره‌ی بعد از Join: Picker چندانتخابی مستقیماً روی خودِ پوشه‌ی تیمی (نه یک
     * زیرپوشه) باز می‌کنه تا کاربر سه فایل JSON مشترک (properties/clients/contact-logs) رو
     * صریحاً انتخاب کنه. همین انتخاب صریح، طبق قانون drive.file، مجوز خواندن هرکدوم رو برای
     * همیشه به این دستگاه می‌ده — مستقل از این‌که محتواشون بعداً توسط عضو دیگری عوض بشه یا نه.
     */
    private fun requestJoinFilesPicker(folderId: String) {
        val account = _uiState.value.account ?: return
        pendingFilesPickerStep = FilesPickerStep.JOIN_TOP_LEVEL_FILES
        viewModelScope.launch {
            when (val tokenResult = authManager.getAccessToken(context, account)) {
                is AccessTokenResult.Success -> _uiState.value = _uiState.value.copy(
                    pickerLaunchIntent = Intent(context, DrivePickerActivity::class.java)
                        .putExtra(DrivePickerActivity.EXTRA_ACCESS_TOKEN, tokenResult.token)
                        .putExtra(DrivePickerActivity.EXTRA_MODE, DrivePickerActivity.MODE_FILES)
                        .putExtra(DrivePickerActivity.EXTRA_PARENT_FOLDER_ID, folderId)
                )
                is AccessTokenResult.ConsentRequired -> _uiState.value = _uiState.value.copy(pendingConsentIntent = tokenResult.intent)
                is AccessTokenResult.Failure -> {
                    _uiState.value = _uiState.value.copy(joinTeamMessage = tokenResult.message)
                    pendingFilesPickerStep = null
                    syncNow() // حتی اگه این قدم شکست خورد، بی‌خیالش نشو — بازم یه sync عادی امتحان کن
                }
            }
        }
    }

    /**
     * قدم دوم زنجیره: همون کار، ولی روی زیرپوشه‌ی «images» (نه خودِ پوشه‌ی تیمی) — برای عکس‌هایی
     * که قبل از پیوستن این دستگاه، توسط بقیه‌ی اعضا آپلود شده بودن. اگه این تیم کاملاً تازه‌ست و
     * هنوز هیچ‌کس sync نکرده (پس زیرپوشه‌ی images هم وجود نداره)، Failure رو نادیده می‌گیریم و
     * مستقیم syncNow می‌زنیم — چیزی برای گرفتن مجوزش نیست.
     */
    private fun requestJoinImagesPicker() {
        val account = _uiState.value.account ?: run { pendingFilesPickerStep = null; return syncNow() }
        pendingFilesPickerStep = FilesPickerStep.JOIN_IMAGES
        viewModelScope.launch {
            when (val result = syncManager.ensureImagesFolderId(account)) {
                is ImagesFolderResult.Success -> _uiState.value = _uiState.value.copy(
                    pickerLaunchIntent = Intent(context, DrivePickerActivity::class.java)
                        .putExtra(DrivePickerActivity.EXTRA_ACCESS_TOKEN, result.token)
                        .putExtra(DrivePickerActivity.EXTRA_MODE, DrivePickerActivity.MODE_FILES)
                        .putExtra(DrivePickerActivity.EXTRA_PARENT_FOLDER_ID, result.folderId)
                )
                is ImagesFolderResult.ConsentRequired -> _uiState.value = _uiState.value.copy(pendingConsentIntent = result.intent)
                is ImagesFolderResult.Failure -> {
                    pendingFilesPickerStep = null
                    syncNow()
                }
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
        // این همیشه همون Picker تک‌انتخابیِ ساده‌ست (نه یکی از قدم‌های زنجیره‌ی بعد از join)؛
        // اگه یه چرخه‌ی قبلی به هر دلیلی pendingFilesPickerStep رو ناقص جا گذاشته باشه، اینجا
        // ریست می‌شه تا لغوِ این Picker به‌اشتباه یه قدم اضافه از زنجیره‌ی قبلی رو ادامه نده.
        pendingFilesPickerStep = null
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
     *
     * اگه این لغو مربوط به یکی از قدم‌های Picker چندفایلیِ زنجیره‌ی بعد از join بوده (نه لغوِ همون
     * انتخاب اولیه‌ی خودِ پوشه)، کاربر نباید وسط راه بمونه — با قدم بعدی زنجیره (یا در نهایت یک
     * sync عادی) ادامه بده؛ [pendingFilesPickerStep] دقیقاً همینو تشخیص می‌ده (null یعنی این لغو
     * مربوط به همون انتخاب پوشه‌ست، نه یکی از قدم‌های چندفایلی).
     */
    fun onPickerCancelled(reason: String?) {
        if (reason == DrivePickerActivity.REASON_CONFIG_MISSING) {
            _uiState.value = _uiState.value.copy(
                pickerErrorMessage = context.getString(com.realestate.sami.R.string.sync_picker_config_missing)
            )
            return
        }
        val step = pendingFilesPickerStep
        pendingFilesPickerStep = null
        when (step) {
            FilesPickerStep.JOIN_TOP_LEVEL_FILES -> requestJoinImagesPicker()
            FilesPickerStep.JOIN_IMAGES, FilesPickerStep.STANDALONE_IMAGES -> syncNow()
            null -> Unit // لغوِ خودِ انتخاب پوشه (قبل از هر joinی) — کار دیگه‌ای لازم نیست
        }
    }

    fun clearPickerErrorMessage() {
        _uiState.value = _uiState.value.copy(pickerErrorMessage = null)
    }

    /** نتیجه‌ی DrivePickerActivity؛ اگه کاربر پوشه‌ای انتخاب کرده باشه (لغو نکرده باشه)، بهش می‌پیونده. */
    fun onFolderPicked(folderId: String?) {
        if (!folderId.isNullOrBlank()) joinTeamFolder(folderId)
    }

    /**
     * نام واقعیِ پوشه‌ی تیمی را روی خودِ Google Drive تغییر می‌دهد (نه فقط یک برچسب لوکال داخل اپ) —
     * تا وقتی چند پوشه‌ی هم‌نام هست (یکی خودش ساخته، یکی Share‌شده از یک عضو دیگر)، در خودِ
     * Drive/Picker هم قابل‌تشخیص بمونن، نه فقط داخل لیست تیم‌های همین اپ.
     */
    fun setTeamDisplayName(name: String) {
        val account = _uiState.value.account ?: return
        _uiState.value = _uiState.value.copy(isRenamingTeamFolder = true, renameTeamFolderMessage = null)
        viewModelScope.launch {
            when (val result = syncManager.renameTeamFolder(account, name)) {
                is RenameTeamFolderResult.Success -> _uiState.value = _uiState.value.copy(
                    isRenamingTeamFolder = false,
                    teamDisplayName = result.newName,
                    renameTeamFolderMessage = null
                )
                is RenameTeamFolderResult.Failure -> _uiState.value = _uiState.value.copy(
                    isRenamingTeamFolder = false,
                    renameTeamFolderMessage = result.message
                )
            }
        }
    }

    fun clearRenameTeamFolderMessage() {
        _uiState.value = _uiState.value.copy(renameTeamFolderMessage = null)
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
            isRenamingTeamFolder = false,
            renameTeamFolderMessage = null,
            lastSyncedAt = null,
            joinTeamMessage = null
        )
    }

    /**
     * صفحه‌ی جزئیات ملک این رو صدا می‌زنه وقتی کاربر روی یک عکسِ «هنوز دانلود نشده» بزنه.
     * چون درست‌کردن این محدودیت (اسکوپ drive.file) نیاز به مجوز جداگونه‌ی هر فایل داره، اول
     * باید شناسه‌ی پوشه‌ی images رو گرفت (بدون sync کامل)، و بعد Picker چندانتخابی رو رویش باز کرد.
     */
    fun requestTeamImagesPicker() {
        val account = _uiState.value.account ?: return
        _uiState.value = _uiState.value.copy(fetchImagesErrorMessage = null)
        pendingFilesPickerStep = FilesPickerStep.STANDALONE_IMAGES
        viewModelScope.launch {
            when (val result = syncManager.ensureImagesFolderId(account)) {
                is ImagesFolderResult.Success -> _uiState.value = _uiState.value.copy(
                    pickerLaunchIntent = Intent(context, DrivePickerActivity::class.java)
                        .putExtra(DrivePickerActivity.EXTRA_ACCESS_TOKEN, result.token)
                        .putExtra(DrivePickerActivity.EXTRA_MODE, DrivePickerActivity.MODE_FILES)
                        .putExtra(DrivePickerActivity.EXTRA_PARENT_FOLDER_ID, result.folderId)
                )
                is ImagesFolderResult.ConsentRequired -> _uiState.value = _uiState.value.copy(
                    pendingConsentIntent = result.intent
                )
                is ImagesFolderResult.Failure -> _uiState.value = _uiState.value.copy(
                    fetchImagesErrorMessage = result.message
                )
            }
        }
    }

    /**
     * بعد از این‌که کاربر در هر کدوم از Pickerهای چندانتخابی (فایل‌های سطح‌بالا بعد از join،
     * زیرپوشه‌ی images بعد از join، یا دکمه‌ی مستقل «دریافت تصاویر تیم») چیزی انتخاب کرد یا لغو
     * کرد — فرقی نداره کدوم، چون خودِ idهای انتخاب‌شده لازم نیست جایی استفاده شن؛ صرفِ عبور از
     * Picker مجوز drive.file رو داده. تنها فرق مسیرها اینه که قدم *بعدی* چیه:
     * فایل‌های سطح‌بالا → برو سراغ عکس‌ها؛ عکس‌ها (چه در زنجیره‌ی join، چه دکمه‌ی مستقل) → یک sync عادی.
     */
    fun onFilesPicked() {
        val step = pendingFilesPickerStep
        pendingFilesPickerStep = null
        when (step) {
            FilesPickerStep.JOIN_TOP_LEVEL_FILES -> requestJoinImagesPicker()
            FilesPickerStep.JOIN_IMAGES, FilesPickerStep.STANDALONE_IMAGES, null -> syncNow()
        }
    }

    fun clearFetchImagesErrorMessage() {
        _uiState.value = _uiState.value.copy(fetchImagesErrorMessage = null)
    }

    /** تغییر تنظیم «sync خودکار فقط با Wi-Fi»؛ ذخیره می‌شود و کار دوره‌ای فوراً با محدودیت شبکه‌ی جدید دوباره زمان‌بندی می‌شود. */
    fun setAutoSyncWifiOnly(enabled: Boolean) {
        syncPrefs.autoSyncWifiOnly = enabled
        _uiState.value = _uiState.value.copy(autoSyncWifiOnly = enabled)
        SyncWorker.schedulePeriodic(context, wifiOnly = enabled, policy = ExistingPeriodicWorkPolicy.REPLACE)
    }
}
