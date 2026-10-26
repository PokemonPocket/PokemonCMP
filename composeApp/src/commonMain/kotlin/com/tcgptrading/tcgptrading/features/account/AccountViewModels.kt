package com.tcgptrading.tcgptrading.features.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.runApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Keeps only digits, at most 16. */
fun friendIdDigits(input: String): String = input.filter { it.isDigit() }.take(16)

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------

data class ProfileState(
    val email: String = "",
    val friendIdInput: String = "",
    val savedFriendId: String = "",
    val savingId: Boolean = false,
    /** Result line under the Friend ID field; error = true shows it in red. */
    val idMessage: String? = null,
    val idError: Boolean = false,
    val emailsOn: Boolean = true,
    val savingEmails: Boolean = false,
    val deleting: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    private val api: TradingApi,
    private val session: Session,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            session.me.collect { me ->
                if (me != null) {
                    _state.update {
                        val id = me.friendId.orEmpty().filter { c -> c.isDigit() }
                        it.copy(
                            email = me.email,
                            savedFriendId = id,
                            // Don't overwrite what the user is typing
                            friendIdInput = if (it.friendIdInput.isEmpty() || it.friendIdInput == it.savedFriendId) id else it.friendIdInput,
                            emailsOn = me.acceptNotifications != false,
                        )
                    }
                }
            }
        }
        viewModelScope.launch { session.refresh() }
    }

    fun setFriendIdInput(value: String) =
        _state.update { it.copy(friendIdInput = friendIdDigits(value), idMessage = null, idError = false) }

    fun saveFriendId() {
        val id = _state.value.friendIdInput
        if (id.length != 16 || _state.value.savingId) return
        viewModelScope.launch {
            _state.update { it.copy(savingId = true, idMessage = null) }
            runApi { api.setFriendId(id) }
                .onSuccess { ok ->
                    if (ok) {
                        session.update { p -> p.copy(friendId = id) }
                        _state.update { it.copy(savingId = false, savedFriendId = id, idMessage = "Friend ID saved.", idError = false) }
                    } else {
                        _state.update { it.copy(savingId = false, idMessage = "That Friend ID is already used by another account.", idError = true) }
                    }
                }
                .onFailure { e -> _state.update { it.copy(savingId = false, idMessage = e.message, idError = true) } }
        }
    }

    fun setEmails(on: Boolean) {
        val before = _state.value.emailsOn
        _state.update { it.copy(emailsOn = on, savingEmails = true, error = null) }
        viewModelScope.launch {
            runApi { api.setEmailNotifications(on) }
                .onSuccess {
                    session.update { p -> p.copy(acceptNotifications = on) }
                    _state.update { it.copy(savingEmails = false) }
                }
                .onFailure { e -> _state.update { it.copy(emailsOn = before, savingEmails = false, error = e.message) } }
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        if (_state.value.deleting) return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true, error = null) }
            runApi { api.deleteAccount() }
                .onSuccess { _state.update { it.copy(deleting = false) }; onDone() }
                .onFailure { e -> _state.update { it.copy(deleting = false, error = e.message) } }
        }
    }
}

// ---------------------------------------------------------------------------
// Contact
// ---------------------------------------------------------------------------

val CONTACT_TOPICS = listOf("A player or a trade", "Something is broken", "An idea for the site", "My account")

data class ContactState(
    val topic: String = CONTACT_TOPICS.first(),
    val subject: String = "",
    val message: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)

class ContactViewModel(
    private val api: TradingApi,
    private val session: Session,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactState())
    val state: StateFlow<ContactState> = _state.asStateFlow()

    fun setTopic(value: String) = _state.update { it.copy(topic = value) }
    fun setSubject(value: String) = _state.update { it.copy(subject = value.take(120), error = null) }
    fun setMessage(value: String) = _state.update { it.copy(message = value.take(4000), error = null) }

    fun send() {
        val s = _state.value
        if (s.sending) return
        if (s.message.isBlank()) {
            _state.update { it.copy(error = "Write a message first.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            runApi { api.contact(s.topic, s.subject.trim(), s.message.trim(), session.me.value?.email?.substringBefore('@')) }
                .onSuccess { _state.update { it.copy(sending = false, sent = true) } }
                .onFailure { e -> _state.update { it.copy(sending = false, error = e.message) } }
        }
    }

    fun reset() = _state.update { ContactState() }
}
