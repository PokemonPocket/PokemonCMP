package com.tcgptrading.tcgptrading.features.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcgptrading.tcgptrading.core.data.network.ApiConfig
import com.tcgptrading.tcgptrading.core.presentation.components.ConfirmDialog
import com.tcgptrading.tcgptrading.core.presentation.components.Panel
import com.tcgptrading.tcgptrading.core.presentation.components.ScreenHeader
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TcgChip
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.main.presentation.LocalShowTutorial
import com.tcgptrading.tcgptrading.main.presentation.LocalSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ContactViewModel)
}

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------

@Composable
fun ProfileScreen(onContact: () -> Unit, onSignedOut: () -> Unit) {
    val vm = koinViewModel<ProfileViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val showTutorial = LocalShowTutorial.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            "Profile",
            subtitle = if (state.email.isNotBlank()) "Signed in as ${state.email}" else null,
        )

        // Friend ID
        Section("Friend ID") {
            Text(
                "Players you trade with use it to add you in Pokémon TCG Pocket. Only they see it, after an offer is accepted.",
                style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
            )
            FriendIdField(
                value = state.friendIdInput,
                onValueChange = vm::setFriendIdInput,
                error = state.idMessage?.takeIf { state.idError },
                hint = state.idMessage?.takeIf { !state.idError },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TcgButton(
                    if (copied) "Copied" else "Copy",
                    style = TcgButtonStyle.Secondary,
                    enabled = state.savedFriendId.isNotEmpty(),
                    onClick = {
                        clipboard.setText(AnnotatedString(state.savedFriendId))
                        copied = true
                        scope.launch { delay(2000); copied = false }
                    },
                )
                TcgButton(
                    "Save", style = TcgButtonStyle.Dark, loading = state.savingId,
                    enabled = state.friendIdInput.length == 16 && state.friendIdInput != state.savedFriendId,
                    onClick = vm::saveFriendId,
                )
            }
        }

        // Emails
        Section("Emails") {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .toggleable(value = state.emailsOn, enabled = !state.savingEmails, role = Role.Switch, onValueChange = vm::setEmails),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Email me about my trades", style = MaterialTheme.typography.bodyLarge, color = TcgColors.Ink)
                    Text("For example when your offer is accepted.", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
                Switch(
                    checked = state.emailsOn, onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = TcgColors.Ink, checkedThumbColor = TcgColors.Surface),
                )
            }
        }

        // Help
        Section("Help") {
            Text("See the short tour of how wants, offers and trades work.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TcgButton("Show how it works", onClick = showTutorial, style = TcgButtonStyle.Secondary)
                TcgButton("Contact us", onClick = onContact, style = TcgButtonStyle.Secondary)
                TcgButton("Open the website", onClick = { uriHandler.openUri(ApiConfig.WEBSITE_URL) }, style = TcgButtonStyle.Ghost)
                TcgButton("Privacy policy", onClick = { uriHandler.openUri(ApiConfig.WEBSITE_URL + "/privacy") }, style = TcgButtonStyle.Ghost)
            }
        }

        // Account
        Section("Account") {
            Text(
                "Deleting your account removes your wants, offers and chats. Trades in progress are cancelled. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TcgButton("Sign out", onClick = onSignedOut, style = TcgButtonStyle.Secondary)
                TcgButton("Delete account", onClick = { confirmDelete = true }, style = TcgButtonStyle.Danger, loading = state.deleting)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "Your wants, offers and chats are removed, and trades in progress are cancelled. This cannot be undone.",
            confirmText = "Delete account",
            dismissText = "Keep my account",
            danger = true,
            onConfirm = {
                confirmDelete = false
                vm.deleteAccount(onDone = onSignedOut)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Panel(Modifier.padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TcgColors.Ink)
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Contact
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactScreen(onBack: () -> Unit) {
    val vm = koinViewModel<ContactViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TcgColors.Ink)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("Contact us", subtitle = "Report a player, a bug or an idea. We reply by email, usually within two days.")

            if (state.sent) {
                Panel(Modifier.padding(horizontal = 16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Message sent", style = MaterialTheme.typography.titleLarge, color = TcgColors.Ink)
                        Text("Thanks. We'll reply to your email address.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TcgButton("Back to profile", onClick = onBack, style = TcgButtonStyle.Dark)
                            TcgButton("Send another", onClick = vm::reset, style = TcgButtonStyle.Secondary)
                        }
                    }
                }
                return@Column
            }

            Panel(Modifier.padding(horizontal = 16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("What is it about?", style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CONTACT_TOPICS.forEach { topic ->
                            TcgChip(topic, selected = state.topic == topic, onClick = { vm.setTopic(topic) })
                        }
                    }
                    ContactField("Subject", state.subject, vm::setSubject, singleLine = true, placeholder = "Short summary")
                    ContactField(
                        "Message", state.message, vm::setMessage, singleLine = false,
                        placeholder = "What happened, and which player or card was involved",
                    )
                    state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TcgColors.RedInk) }
                    TcgButton(
                        "Send message", onClick = vm::send, loading = state.sending, large = true,
                        enabled = state.message.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Panel(Modifier.padding(horizontal = 16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Quick answers", style = MaterialTheme.typography.titleLarge, color = TcgColors.Ink)
                    Faq("Why only cards of the same rarity?", "Pokémon TCG Pocket only allows trades between cards of matching rarity, so offers follow the same rule.")
                    Faq("When does an accepted trade expire?", "Three days after it is accepted. Mark it as traded once the cards are swapped in the game.")
                    Faq("Who can see my Friend ID?", "Only the player in a trade you accepted, or whose offer you accepted.")
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}

@Composable
private fun ContactField(label: String, value: String, onChange: (String) -> Unit, singleLine: Boolean, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.heightIn(min = 140.dp)),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TcgColors.Faint) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = TcgColors.Surface, unfocusedContainerColor = TcgColors.Surface,
            focusedBorderColor = TcgColors.Ink, unfocusedBorderColor = TcgColors.Line, cursorColor = TcgColors.Ink,
            focusedLabelColor = TcgColors.Ink,
        ),
    )
}

@Composable
private fun Faq(question: String, answer: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = open, role = Role.Button, onValueChange = { open = it }),
    ) {
        Text(question, style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink, modifier = Modifier.padding(vertical = 8.dp))
        if (open) Text(answer, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted, modifier = Modifier.padding(bottom = 8.dp))
    }
}
