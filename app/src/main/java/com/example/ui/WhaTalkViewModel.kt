package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.BlockedUser
import com.example.data.CallLog
import com.example.data.ChatMessage
import com.example.data.Community
import com.example.data.Contact
import com.example.data.Conversation
import com.example.data.StatusPost
import com.example.data.UserProfile
import com.example.data.UserSettings
import com.example.data.WhaTalkRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WhaTalkViewModel(
  private val repository: WhaTalkRepository,
  val currentUserId: String,
) : ViewModel() {

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _profileLoaded = MutableStateFlow(false)
  val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

  val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe user profile", e)
      _profileLoaded.value = true
      emit(null)
    }
    .combine(MutableStateFlow(Unit)) { profile, _ ->
      _profileLoaded.value = true
      profile
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val userSettings: StateFlow<UserSettings> = repository.observeUserSettings(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe user settings", e)
      emit(UserSettings(userId = currentUserId))
    }
    .combine(MutableStateFlow(Unit)) { settings, _ ->
      settings ?: UserSettings(userId = currentUserId)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserSettings(userId = currentUserId))

  val contacts: StateFlow<List<Contact>> = repository.observeContacts(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe contacts", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val conversations: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe conversations", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val statuses: StateFlow<List<StatusPost>> = repository.observeActiveStatuses(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe statuses", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val communities: StateFlow<List<Community>> = repository.observeCommunities(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe communities", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val calls: StateFlow<List<CallLog>> = repository.observeCalls(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe calls", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val blockedUsers: StateFlow<List<BlockedUser>> = repository.observeBlockedUsers(currentUserId)
    .catch { e ->
      Log.w(TAG, "Failed to observe blocked users", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  // Active Conversation & Paginated Messages
  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  private val _messageLimit = MutableStateFlow(50L)
  val messageLimit: StateFlow<Long> = _messageLimit.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeMessages: StateFlow<List<ChatMessage>> =
    combine(_activeConversationId, _messageLimit) { convId, limit -> convId to limit }
      .flatMapLatest { (convId, limit) ->
        if (convId.isNullOrBlank()) {
          flowOf(emptyList())
        } else {
          repository.observeMessages(convId, currentUserId, limit)
            .catch { e ->
              Log.w(TAG, "Failed to observe messages for $convId", e)
              emit(emptyList())
            }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  // Search results for adding contacts
  private val _searchedUsers = MutableStateFlow<List<UserProfile>>(emptyList())
  val searchedUsers: StateFlow<List<UserProfile>> = _searchedUsers.asStateFlow()

  private val _isSearchingUsers = MutableStateFlow(false)
  val isSearchingUsers: StateFlow<Boolean> = _isSearchingUsers.asStateFlow()

  // Active Call state
  private val _activeCallId = MutableStateFlow<String?>(null)
  val activeCallId: StateFlow<String?> = _activeCallId.asStateFlow()

  fun showMessage(msg: String?) {
    _statusBannerMessage.value = msg
  }

  fun openConversation(conversationId: String?) {
    _activeConversationId.value = conversationId
    _messageLimit.value = 50L
  }

  fun loadOlderMessages() {
    _messageLimit.value += 40L
  }

  fun completeOrUpdateProfile(
    displayName: String,
    username: String,
    bio: String,
    phone: String,
    avatarData: String,
    email: String,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
      val availableRes = repository.isUsernameAvailable(cleanUsername, currentUserId)
      if (availableRes.getOrDefault(true) == false) {
        _statusBannerMessage.value = "Username @$cleanUsername sudah digunakan pengguna lain. Pilih username lain."
        _isSaving.value = false
        return@launch
      }

      val saveRes = repository.saveUserProfile(
        displayName = displayName,
        username = cleanUsername,
        bio = bio,
        avatarData = avatarData,
        isOnline = true,
        lastSeenText = "Online",
      )
      if (saveRes.isSuccess) {
        val currentSettings = userSettings.value
        repository.saveUserSettings(
          currentSettings.copy(
            userId = currentUserId,
            phoneNumber = phone.ifBlank { currentSettings.phoneNumber },
            email = email.ifBlank { currentSettings.email },
          )
        )
        _statusBannerMessage.value = "Profil WhaTalk berhasil disimpan."
      } else {
        _statusBannerMessage.value =
          "Gagal menyimpan profil: ${saveRes.exceptionOrNull()?.localizedMessage ?: "Periksa koneksi"}"
      }
      _isSaving.value = false
    }
  }

  fun updateSettings(newSettings: UserSettings) {
    viewModelScope.launch {
      val res = repository.saveUserSettings(newSettings)
      if (res.isSuccess) {
        _statusBannerMessage.value = "Pengaturan disimpan."
      } else {
        _statusBannerMessage.value = "Gagal menyimpan pengaturan."
      }
    }
  }

  fun searchUsersByUsername(query: String) {
    viewModelScope.launch {
      val clean = query.trim().lowercase().removePrefix("@")
      if (clean.length < 3) {
        _searchedUsers.value = emptyList()
        _statusBannerMessage.value = "Masukkan minimal 3 karakter username."
        return@launch
      }
      _isSearchingUsers.value = true
      val res = repository.searchUserByUsername(clean)
      _searchedUsers.value = res.getOrDefault(emptyList())
      if (_searchedUsers.value.isEmpty()) {
        _statusBannerMessage.value = "Pengguna dengan username @$clean tidak ditemukan."
      }
      _isSearchingUsers.value = false
    }
  }

  fun addContactAndStartChat(profile: UserProfile, onChatOpened: (String) -> Unit) {
    viewModelScope.launch {
      repository.addContact(
        contactUid = profile.userId,
        contactUsername = profile.username,
        contactDisplayName = profile.displayName,
        contactBio = profile.bio,
        contactAvatarData = profile.avatarData,
      )
      val convRes = repository.getOrCreateDirectConversation(
        otherUid = profile.userId,
        otherDisplayName = profile.displayName,
        otherAvatarData = profile.avatarData,
      )
      convRes.onSuccess { convId ->
        _activeConversationId.value = convId
        onChatOpened(convId)
      }.onFailure { e ->
        _statusBannerMessage.value = "Gagal memulai percakapan: ${e.localizedMessage}"
      }
    }
  }

  fun startDirectChatWithContact(contact: Contact, onChatOpened: (String) -> Unit) {
    viewModelScope.launch {
      val convRes = repository.getOrCreateDirectConversation(
        otherUid = contact.contactUid,
        otherDisplayName = contact.contactDisplayName,
        otherAvatarData = contact.contactAvatarData,
      )
      convRes.onSuccess { convId ->
        _activeConversationId.value = convId
        onChatOpened(convId)
      }.onFailure { e ->
        _statusBannerMessage.value = "Gagal membuka chat: ${e.localizedMessage}"
      }
    }
  }

  fun createNewGroup(
    title: String,
    description: String,
    memberUids: List<String>,
    communityId: String = "",
    avatarData: String = "",
    onCreated: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      val res = repository.createGroupConversation(
        title = title,
        description = description,
        memberUids = memberUids,
        communityId = communityId,
        avatarData = avatarData,
      )
      res.onSuccess { convId ->
        if (communityId.isNotBlank()) {
          repository.addGroupToCommunity(communityId, convId)
        }
        _statusBannerMessage.value = "Grup \"$title\" berhasil dibuat."
        onCreated(convId)
      }.onFailure { e ->
        _statusBannerMessage.value = "Gagal membuat grup: ${e.localizedMessage}"
      }
    }
  }

  fun joinGroupWithInvite(inviteCode: String) {
    viewModelScope.launch {
      val res = repository.joinGroupByInviteCode(inviteCode)
      res.onSuccess { convId ->
        _activeConversationId.value = convId
        _statusBannerMessage.value = "Berhasil bergabung ke grup!"
      }.onFailure { e ->
        _statusBannerMessage.value = e.localizedMessage ?: "Gagal bergabung ke grup."
      }
    }
  }

  fun toggleConversationPin(conv: Conversation) {
    viewModelScope.launch {
      val isPinned = conv.pinnedByUids.contains(currentUserId)
      repository.toggleConversationFlag(conv.id, "pinnedByUids", !isPinned)
    }
  }

  fun toggleConversationArchive(conv: Conversation) {
    viewModelScope.launch {
      val isArchived = conv.archivedByUids.contains(currentUserId)
      repository.toggleConversationFlag(conv.id, "archivedByUids", !isArchived)
      _statusBannerMessage.value = if (!isArchived) "Chat diarsipkan" else "Chat dikeluarkan dari arsip"
    }
  }

  fun toggleConversationMute(conv: Conversation) {
    viewModelScope.launch {
      val isMuted = conv.mutedByUids.contains(currentUserId)
      repository.toggleConversationFlag(conv.id, "mutedByUids", !isMuted)
      _statusBannerMessage.value = if (!isMuted) "Notifikasi chat dibisukan" else "Notifikasi chat diaktifkan"
    }
  }

  fun deleteOrLeaveConversation(conversationId: String) {
    viewModelScope.launch {
      repository.leaveOrDeleteConversation(conversationId)
      if (_activeConversationId.value == conversationId) {
        _activeConversationId.value = null
      }
      _statusBannerMessage.value = "Percakapan telah dihapus / ditinggalkan."
    }
  }

  fun sendChatMessage(
    conversation: Conversation,
    text: String,
    messageType: String = "TEXT",
    mediaData: String = "",
    mediaCaption: String = "",
    replyToMessage: ChatMessage? = null,
    isForwarded: Boolean = false,
  ) {
    viewModelScope.launch {
      val senderName = userProfile.value?.displayName?.ifBlank { "Saya" } ?: "Saya"
      val res = repository.sendMessage(
        conversationId = conversation.id,
        senderName = senderName,
        memberUids = conversation.memberUids,
        text = text,
        messageType = messageType,
        mediaData = mediaData,
        mediaCaption = mediaCaption,
        replyToMessageId = replyToMessage?.id ?: "",
        replyToSenderName = replyToMessage?.senderName ?: "",
        replyToText = replyToMessage?.text?.take(120) ?: "",
        isForwarded = isForwarded,
      )
      if (res.isFailure) {
        _statusBannerMessage.value = "Gagal mengirim pesan: ${res.exceptionOrNull()?.localizedMessage}"
      }
    }
  }

  fun setTyping(conversationId: String, isTyping: Boolean) {
    viewModelScope.launch {
      repository.setTypingStatus(conversationId, isTyping)
    }
  }

  fun editOwnMessage(messageId: String, newText: String) {
    viewModelScope.launch {
      repository.editMessage(messageId, newText)
    }
  }

  fun deleteOwnMessage(messageId: String) {
    viewModelScope.launch {
      repository.deleteMessageForEveryone(messageId)
    }
  }

  fun reactToMessage(messageId: String, emoji: String) {
    viewModelScope.launch {
      repository.addReactionToMessage(messageId, emoji)
    }
  }

  fun markIncomingMessagesRead(conversationId: String, messages: List<ChatMessage>) {
    if (!userSettings.value.readReceipts) return
    viewModelScope.launch {
      val unreadIncoming = messages.filter { it.senderId != currentUserId && it.status != "READ" }
      unreadIncoming.takeLast(5).forEach { msg ->
        repository.markMessageAsRead(msg.id, conversationId)
      }
    }
  }

  fun updateGroupDetails(
    conversationId: String,
    title: String,
    description: String,
    memberUids: List<String>,
    adminUids: List<String>,
  ) {
    viewModelScope.launch {
      val res = repository.updateGroupInfo(conversationId, title, description, memberUids, adminUids)
      if (res.isSuccess) {
        _statusBannerMessage.value = "Informasi grup diperbarui."
      } else {
        _statusBannerMessage.value = "Gagal memperbarui grup: ${res.exceptionOrNull()?.localizedMessage}"
      }
    }
  }

  // --- STATUS 24 JAM ---

  fun postStatus(
    statusType: String,
    contentText: String,
    mediaData: String = "",
    backgroundColorHex: String = "#00A884",
    privacyMode: String = userSettings.value.privacyStatus,
  ) {
    viewModelScope.launch {
      val profile = userProfile.value
      val contactUids = contacts.value.map { it.contactUid }
      val res = repository.createStatusPost(
        authorName = profile?.displayName ?: "Saya",
        authorAvatarData = profile?.avatarData ?: "",
        statusType = statusType,
        contentText = contentText,
        mediaData = mediaData,
        backgroundColorHex = backgroundColorHex,
        privacyMode = privacyMode,
        contactUids = contactUids,
      )
      if (res.isSuccess) {
        _statusBannerMessage.value = "Status berhasil dibagikan (aktif 24 jam)."
      } else {
        _statusBannerMessage.value = "Gagal mengunggah status: ${res.exceptionOrNull()?.localizedMessage}"
      }
    }
  }

  fun markStatusSeen(statusId: String) {
    viewModelScope.launch {
      repository.markStatusViewed(statusId)
    }
  }

  fun removeStatus(statusId: String) {
    viewModelScope.launch {
      repository.deleteStatusPost(statusId)
      _statusBannerMessage.value = "Status dihapus."
    }
  }

  // --- KOMUNITAS ---

  fun createNewCommunity(name: String, description: String, groupIds: List<String>) {
    viewModelScope.launch {
      val res = repository.createCommunity(name, description, groupIds)
      if (res.isSuccess) {
        _statusBannerMessage.value = "Komunitas \"$name\" berhasil dibuat."
      } else {
        _statusBannerMessage.value = "Gagal membuat komunitas: ${res.exceptionOrNull()?.localizedMessage}"
      }
    }
  }

  fun joinCommunityByCode(inviteCode: String) {
    viewModelScope.launch {
      val res = repository.joinCommunityByInviteCode(inviteCode)
      if (res.isSuccess) {
        _statusBannerMessage.value = "Berhasil bergabung ke komunitas!"
      } else {
        _statusBannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Kode undangan tidak valid."
      }
    }
  }

  fun linkGroupToCommunity(communityId: String, groupId: String) {
    viewModelScope.launch {
      repository.addGroupToCommunity(communityId, groupId)
      _statusBannerMessage.value = "Grup berhasil ditambahkan ke komunitas."
    }
  }

  // --- PANGGILAN WEBRTC ---

  fun initiateCall(receiverId: String, receiverName: String, callType: String) {
    viewModelScope.launch {
      val callerName = userProfile.value?.displayName ?: "Pengguna WhaTalk"
      val res = repository.startCall(
        callerName = callerName,
        receiverId = receiverId,
        receiverName = receiverName,
        callType = callType,
      )
      res.onSuccess { callId ->
        _activeCallId.value = callId
      }.onFailure { e ->
        _statusBannerMessage.value = "Gagal memulai panggilan: ${e.localizedMessage}"
      }
    }
  }

  fun acceptIncomingCall(callId: String) {
    viewModelScope.launch {
      _activeCallId.value = callId
      repository.updateCallState(
        callId = callId,
        newState = "CONNECTED",
        sdpAnswer = "v=0\r\no=- 882193 2 IN IP4 127.0.0.1\r\ns=WhaTalk-WebRTC-Answer\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n",
      )
    }
  }

  fun endOrRejectCall(callId: String, state: String, durationSeconds: Long) {
    viewModelScope.launch {
      repository.updateCallState(callId, state, durationSeconds)
      if (_activeCallId.value == callId) {
        _activeCallId.value = null
      }
    }
  }

  fun dismissCallScreen() {
    _activeCallId.value = null
  }

  // --- BLOKIR & LAPORAN ---

  fun blockContact(uid: String, username: String, displayName: String) {
    viewModelScope.launch {
      repository.blockUser(uid, username, displayName)
      _statusBannerMessage.value = "$displayName telah diblokir."
    }
  }

  fun unblockContact(blockId: String) {
    viewModelScope.launch {
      repository.unblockUser(blockId)
      _statusBannerMessage.value = "Blokir dibuka."
    }
  }

  fun submitAbuseReport(reportedUid: String, reportedUsername: String, reason: String, details: String) {
    viewModelScope.launch {
      val res = repository.reportUser(reportedUid, reportedUsername, reason, details)
      if (res.isSuccess) {
        _statusBannerMessage.value = "Laporan akun @$reportedUsername telah dikirim ke tim keamanan."
      } else {
        _statusBannerMessage.value = "Gagal mengirim laporan."
      }
    }
  }

  companion object {
    private const val TAG = "WhaTalkVM"

    fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val auth = FirebaseAuth.getInstance()
        WhaTalkViewModel(WhaTalkRepository(db, auth), currentUserId)
      }
    }
  }
}
