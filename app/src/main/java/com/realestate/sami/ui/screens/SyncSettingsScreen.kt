package com.realestate.sami.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.sync.DrivePickerActivity
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.viewmodel.SyncStatus
import com.realestate.sami.ui.viewmodel.SyncViewModel
import com.realestate.sami.util.toPersianDateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(viewModel: SyncViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSignInResult(result.data)
        } else {
            viewModel.onSignInError("ورود لغو شد")
        }
    }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onConsentGranted()
        }
    }

    // Google Picker (انتخاب پوشه‌ی تیمی از Drive) رو باز می‌کنه؛ نتیجه شناسه/نام پوشه‌ی انتخاب‌شده است.
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val folderId = result.data?.getStringExtra(DrivePickerActivity.EXTRA_RESULT_FOLDER_ID)
            viewModel.onFolderPicked(folderId)
        } else {
            val reason = result.data?.getStringExtra(DrivePickerActivity.EXTRA_CANCEL_REASON)
            viewModel.onPickerCancelled(reason)
        }
    }

    // وقتی SyncManager بگه نیاز به consent هست، خودکار Intent مربوطه رو باز کن
    LaunchedEffect(state.pendingConsentIntent) {
        state.pendingConsentIntent?.let { consentLauncher.launch(it) }
    }

    // وقتی ViewModel یک Intent برای Picker آماده کرده، بازش کن و بعد از state پاکش کن (یک‌بار‌مصرف)
    LaunchedEffect(state.pickerLaunchIntent) {
        state.pickerLaunchIntent?.let {
            pickerLauncher.launch(it)
            viewModel.clearPickerLaunchIntent()
        }
    }

    val context = LocalContext.current
    var joinFolderIdInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Groups, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.sync_description), style = MaterialTheme.typography.bodyMedium)
                }
            }

            SectionCard {
                if (state.account == null) {
                    Button(
                        onClick = { signInLauncher.launch(viewModel.getSignInIntent()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.sync_sign_in_button))
                    }
                } else {
                    Text(
                        stringResource(R.string.sync_signed_in_as, state.account?.email.orEmpty()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = state.status != SyncStatus.SYNCING,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.status == SyncStatus.SYNCING) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sync_in_progress))
                        } else {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sync_now_button))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.lastSyncedAt?.let {
                            stringResource(R.string.sync_last_synced, it.toPersianDateString())
                        } ?: stringResource(R.string.sync_never_synced),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.status == SyncStatus.SUCCESS) {
                        Text(
                            stringResource(R.string.sync_success),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    state.errorMessage?.let { msg ->
                        Text(
                            stringResource(R.string.sync_error_prefix, msg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.sync_sign_out_button))
                    }
                }
            }

            if (state.account != null) {
                SectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.sync_auto_wifi_only_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.sync_auto_wifi_only_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = state.autoSyncWifiOnly,
                            onCheckedChange = { viewModel.setAutoSyncWifiOnly(it) }
                        )
                    }
                }
            }

            if (state.account != null) {
                SectionCard {
                    Text(
                        stringResource(R.string.sync_team_folder_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))

                    if (state.teamFolderId != null) {
                        // این عضو تیم قبلاً یه پوشه‌ی تیمی داره (چه خودش ساخته چه بهش پیوسته) —
                        // با این دکمه می‌تونه شناسه/لینکش رو برای بقیه‌ی اعضا بفرسته تا اونا هم با
                        // «پیوستن به تیم موجود» دقیقاً به همین پوشه وصل بشن، نه اینکه یکی جدا بسازن.
                        Text(
                            stringResource(R.string.sync_team_folder_has_one),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        // این نام روی خودِ Drive تغییر می‌کند (نه فقط لوکال داخل اپ) — تا وقتی چند
                        // پوشه‌ی هم‌نام هست (یکی خودِ این دستگاه ساخته، یکی از یک عضو دیگر Share
                        // شده)، در خودِ Drive و Picker هم قابل‌تشخیص بمونن.
                        var teamNameInput by remember(state.teamFolderId) { mutableStateOf(state.teamDisplayName) }
                        OutlinedTextField(
                            value = teamNameInput,
                            onValueChange = { teamNameInput = it },
                            label = { Text(stringResource(R.string.sync_team_display_name_label)) },
                            singleLine = true,
                            enabled = !state.isRenamingTeamFolder,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (state.isRenamingTeamFolder) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    TextButton(
                                        onClick = { viewModel.setTeamDisplayName(teamNameInput) },
                                        enabled = teamNameInput.isNotBlank() && teamNameInput != state.teamDisplayName
                                    ) { Text(stringResource(R.string.sync_team_display_name_save)) }
                                }
                            }
                        )
                        state.renameTeamFolderMessage?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val folderId = state.teamFolderId.orEmpty()
                                val link = "https://drive.google.com/drive/folders/$folderId"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, link)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sync_team_folder_share_button))
                        }
                        Spacer(Modifier.height(8.dp))

                        var showLeaveConfirm by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showLeaveConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.sync_leave_team_button))
                        }
                        if (showLeaveConfirm) {
                            AlertDialog(
                                onDismissRequest = { showLeaveConfirm = false },
                                title = { Text(stringResource(R.string.sync_leave_team_confirm_title)) },
                                text = { Text(stringResource(R.string.sync_leave_team_confirm_message)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.leaveTeam()
                                        showLeaveConfirm = false
                                    }) { Text(stringResource(R.string.sync_leave_team_confirm_button)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showLeaveConfirm = false }) {
                                        Text(stringResource(R.string.sync_cancel_button))
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // روش اصلی و توصیه‌شده برای پیوستن: Google Picker. چون با اسکوپ drive.file،
                    // فقط انتخاب صریح از طریق Picker است که به اپ دسترسی واقعی به پوشه‌ی Share‌شده می‌ده.
                    Text(
                        stringResource(R.string.sync_team_folder_picker_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.requestFolderPicker() },
                        enabled = !state.isJoiningTeam,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sync_team_folder_picker_button))
                    }
                    state.pickerErrorMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.sync_error_prefix, msg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.sync_team_folder_join_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinFolderIdInput,
                        onValueChange = { joinFolderIdInput = it },
                        placeholder = { Text(stringResource(R.string.sync_team_folder_id_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isJoiningTeam
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.joinTeamFolder(joinFolderIdInput) },
                        enabled = !state.isJoiningTeam && joinFolderIdInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isJoiningTeam) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sync_team_folder_join_button))
                        }
                    }

                    state.joinTeamMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
