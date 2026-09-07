package com.realestate.sami.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.realestate.sami.sync.GoogleAuthManager
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
    val pendingConsentIntent: Intent? = null
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
            lastSyncedAt = syncPrefs.lastSyncedAt
        )
    )
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun getSignInIntent(): Intent = authManager.getSignInIntent(context)

    fun onSignInResult(data: Intent?) {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
        _uiState.value = _uiState.value.copy(account = account, errorMessage = null)
        if (account != null) syncNow()
    }

    fun onSignInError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun signOut() {
        authManager.signOut(context) {
            _uiState.value = SyncUiState()
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
                    lastSyncedAt = result.syncedAt
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
}
