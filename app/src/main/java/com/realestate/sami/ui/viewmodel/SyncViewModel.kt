package com.realestate.sami.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.realestate.sami.sync.GoogleAuthManager
import com.realestate.sami.sync.JoinTeamResult
import com.realestate.sami.sync.SyncManager
import com.realestate.sami.sync.SyncPreferences
import com.realestate.sami.sync.SyncResult
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
    val joinTeamMessage: String? = null,
    val isJoiningTeam: Boolean = false
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
            teamFolderId = syncPrefs.teamFolderId
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
            _uiState.value = SyncUiState(teamFolderId = syncPrefs.teamFolderId)
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
}
