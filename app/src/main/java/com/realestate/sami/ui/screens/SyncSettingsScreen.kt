package com.realestate.sami.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
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

    // وقتی SyncManager بگه نیاز به consent هست، خودکار Intent مربوطه رو باز کن
    LaunchedEffect(state.pendingConsentIntent) {
        state.pendingConsentIntent?.let { consentLauncher.launch(it) }
    }

    val context = LocalContext.current
    var joinFolderIdInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.sync_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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
