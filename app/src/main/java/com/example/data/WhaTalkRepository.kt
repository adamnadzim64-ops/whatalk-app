package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WhaTalkRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance()
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus login sebelum mengakses WhaTalk.")
  }

  private fun sanitizeId(raw: String): String {
    val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    return cleaned.take(120).ifBlank { UUID.randomUUID().toString() }
  }

  // --- 1. USER PROFILE & SETTINGS ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.GET, "users/$userId")
        throw e
      }

  suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
    val path = "users/$userId"
    try {
      val snap = db.collection("users").document(userId).get().await()
      if (snap.exists()) {
        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      throw e
    }
  }

  suspend fun isUsernameAvailable(username: String, currentUserId: String): Result<Boolean> = runCatching {
    val cleanUsername = username.trim().lowercase()
    val path = "users"
    try {
      val query = db.collection("users")
        .whereEqualTo("isDiscoverable", true)
        .whereEqualTo("username", cleanUsername)
        .limit(5)
        .get()
        .await()
      query.documents.none { it.id != currentUserId }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      throw e
    }
  }

  suspend fun saveUserProfile(
    displayName: String,
    username: String,
    bio: String,
    avatarData: String = "",
    isOnline: Boolean = true,
    lastSeenText: String = "Online",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)
    require(cleanUsername.length >= 3) { "Username minimal 3 karakter." }
    val docRef = db.collection("users").document(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "username" to cleanUsername,
          "displayName" to displayName.trim().take(60).ifBlank { "Pengguna WhaTalk" },
          "bio" to bio.trim().take(200).ifBlank { "Ada di WhaTalk" },
          "avatarData" to avatarData.take(40000),
          "isOnline" to isOnline,
          "lastSeenText" to lastSeenText.take(80),
          "isDiscoverable" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "username" to cleanUsername,
          "displayName" to displayName.trim().take(60).ifBlank { "Pengguna WhaTalk" },
          "bio" to bio.trim().take(200).ifBlank { "Ada di WhaTalk" },
          "avatarData" to avatarData.take(40000),
          "isOnline" to isOnline,
          "lastSeenText" to lastSeenText.take(80),
          "isDiscoverable" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateOnlinePresence(isOnline: Boolean, lastSeenText: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "isOnline" to isOnline,
            "lastSeenText" to lastSeenText.take(80),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  fun observeUserSettings(userId: String): Flow<UserSettings?> =
    db.collection("userSettings").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.GET, "userSettings/$userId")
        throw e
      }

  suspend fun saveUserSettings(settings: UserSettings): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("userSettings").document(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "phoneNumber" to settings.phoneNumber.take(32),
          "email" to settings.email.take(128),
          "privacyPhoto" to settings.privacyPhoto,
          "privacyBio" to settings.privacyBio,
          "privacyLastSeen" to settings.privacyLastSeen,
          "privacyOnline" to settings.privacyOnline,
          "privacyStatus" to settings.privacyStatus,
          "readReceipts" to settings.readReceipts,
          "themeMode" to settings.themeMode,
          "wallpaperHex" to settings.wallpaperHex.take(16),
          "fontScale" to settings.fontScale,
          "notificationsEnabled" to settings.notificationsEnabled,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "phoneNumber" to settings.phoneNumber.take(32),
          "email" to settings.email.take(128),
          "privacyPhoto" to settings.privacyPhoto,
          "privacyBio" to settings.privacyBio,
          "privacyLastSeen" to settings.privacyLastSeen,
          "privacyOnline" to settings.privacyOnline,
          "privacyStatus" to settings.privacyStatus,
          "readReceipts" to settings.readReceipts,
          "themeMode" to settings.themeMode,
          "wallpaperHex" to settings.wallpaperHex.take(16),
          "fontScale" to settings.fontScale,
          "notificationsEnabled" to settings.notificationsEnabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  // --- 2. CONTACTS & USER SEARCH ---

  suspend fun searchUserByUsername(usernameQuery: String): Result<List<UserProfile>> = runCatching {
    val cleanQuery = usernameQuery.trim().lowercase().removePrefix("@")
    if (cleanQuery.length < 3) return@runCatching emptyList()
    val path = "users"
    try {
      val snap = db.collection("users")
        .whereEqualTo("isDiscoverable", true)
        .whereEqualTo("username", cleanQuery)
        .limit(10)
        .get()
        .await()
      snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      throw e
    }
  }

  fun observeContacts(userId: String): Flow<List<Contact>> =
    db.collection("contacts")
      .whereEqualTo("ownerId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(Contact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.contactDisplayName.lowercase() }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "contacts")
        throw e
      }

  suspend fun addContact(
    contactUid: String,
    contactUsername: String,
    contactDisplayName: String,
    contactBio: String,
    contactAvatarData: String = "",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = sanitizeId("${uid}_${contactUid}")
    val docRef = db.collection("contacts").document(docId)
    try {
      val payload = mapOf(
        "ownerId" to uid,
        "contactUid" to contactUid,
        "contactUsername" to contactUsername.trim().lowercase().take(32),
        "contactDisplayName" to contactDisplayName.trim().take(60),
        "contactBio" to contactBio.trim().take(200),
        "contactAvatarData" to contactAvatarData.take(40000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun removeContact(contactDocId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("contacts").document(contactDocId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 3. CONVERSATIONS & GROUPS ---

  fun observeConversations(userId: String): Flow<List<Conversation>> =
    db.collection("conversations")
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(
            compareByDescending<Conversation> { it.pinnedByUids.contains(userId) }
              .thenByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
          )
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "conversations")
        throw e
      }

  suspend fun getOrCreateDirectConversation(
    otherUid: String,
    otherDisplayName: String,
    otherAvatarData: String = "",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val sortedPair = listOf(uid, otherUid).sorted()
    val convId = sanitizeId("dm_${sortedPair[0]}_${sortedPair[1]}")
    val docRef = db.collection("conversations").document(convId)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val payload = mapOf(
          "creatorId" to uid,
          "memberUids" to sortedPair.distinct(),
          "adminUids" to listOf(uid),
          "isGroup" to false,
          "title" to otherDisplayName.trim().take(100).ifBlank { "Chat Pribadi" },
          "description" to "",
          "avatarData" to otherAvatarData.take(40000),
          "communityId" to "",
          "inviteCode" to "",
          "lastMessageText" to "Percakapan dimulai",
          "lastMessageSenderId" to uid,
          "lastMessageStatus" to "SENT",
          "typingUserId" to "",
          "pinnedByUids" to emptyList<String>(),
          "archivedByUids" to emptyList<String>(),
          "mutedByUids" to emptyList<String>(),
          "unreadCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      }
      convId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun createGroupConversation(
    title: String,
    description: String,
    memberUids: List<String>,
    communityId: String = "",
    avatarData: String = "",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val convId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val inviteCode = "WTG-${UUID.randomUUID().toString().take(8).uppercase()}"
    val allMembers = (memberUids + uid).distinct().take(50)
    val docRef = db.collection("conversations").document(convId)
    try {
      val payload = mapOf(
        "creatorId" to uid,
        "memberUids" to allMembers,
        "adminUids" to listOf(uid),
        "isGroup" to true,
        "title" to title.trim().take(100).ifBlank { "Grup Baru" },
        "description" to description.trim().take(300),
        "avatarData" to avatarData.take(40000),
        "communityId" to communityId.take(128),
        "inviteCode" to inviteCode,
        "lastMessageText" to "Grup \"$title\" telah dibuat",
        "lastMessageSenderId" to uid,
        "lastMessageStatus" to "SENT",
        "typingUserId" to "",
        "pinnedByUids" to emptyList<String>(),
        "archivedByUids" to emptyList<String>(),
        "mutedByUids" to emptyList<String>(),
        "unreadCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      convId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun toggleConversationFlag(
    conversationId: String,
    field: String, // pinnedByUids, archivedByUids, mutedByUids
    enable: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(field in listOf("pinnedByUids", "archivedByUids", "mutedByUids"))
    val docRef = db.collection("conversations").document(conversationId)
    try {
      val snap = docRef.get().await()
      val conv = snap.toObject(Conversation::class.java) ?: return@runCatching
      val currentList = when (field) {
        "pinnedByUids" -> conv.pinnedByUids
        "archivedByUids" -> conv.archivedByUids
        else -> conv.mutedByUids
      }
      val newList = if (enable) (currentList + uid).distinct().take(50) else currentList - uid
      docRef.update(
        mapOf(
          field to newList,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("conversations").document(conversationId)
    try {
      docRef.update(
        mapOf(
          "typingUserId" to if (isTyping) uid else "",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateGroupInfo(
    conversationId: String,
    title: String,
    description: String,
    memberUids: List<String>,
    adminUids: List<String>,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("conversations").document(conversationId)
    try {
      docRef.update(
        mapOf(
          "title" to title.trim().take(100),
          "description" to description.trim().take(300),
          "memberUids" to memberUids.distinct().take(50),
          "adminUids" to adminUids.distinct().take(20),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun joinGroupByInviteCode(inviteCode: String): Result<String> = runCatching {
    val uid = requireUserId()
    val path = "conversations"
    try {
      val query = db.collection("conversations")
        .whereEqualTo("isGroup", true)
        .whereEqualTo("inviteCode", inviteCode.trim().uppercase())
        .limit(1)
        .get()
        .await()
      val doc = query.documents.firstOrNull()
        ?: throw IllegalArgumentException("Kode undangan grup tidak ditemukan.")
      val conv = doc.toObject(Conversation::class.java)
        ?: throw IllegalStateException("Data grup tidak valid.")
      val updatedMembers = (conv.memberUids + uid).distinct().take(50)
      doc.reference.update(
        mapOf(
          "memberUids" to updatedMembers,
          "lastMessageText" to "Anggota baru bergabung melalui tautan undangan",
          "lastMessageSenderId" to uid,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      doc.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      throw e
    }
  }

  suspend fun leaveOrDeleteConversation(conversationId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("conversations").document(conversationId)
    try {
      val snap = docRef.get().await()
      val conv = snap.toObject(Conversation::class.java) ?: return@runCatching
      if (conv.isGroup && conv.memberUids.size > 1) {
        val remaining = conv.memberUids - uid
        docRef.update(
          mapOf(
            "memberUids" to remaining,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.delete().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 4. REAL-TIME MESSAGES ---

  fun observeMessages(conversationId: String, userId: String, limitCount: Long = 60L): Flow<List<ChatMessage>> =
    db.collection("messages")
      .whereEqualTo("conversationId", conversationId)
      .whereArrayContains("memberUids", userId)
      .limit(limitCount)
      .snapshots()
      .map { snap ->
        snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "messages")
        throw e
      }

  suspend fun sendMessage(
    conversationId: String,
    senderName: String,
    memberUids: List<String>,
    text: String,
    messageType: String = "TEXT",
    mediaData: String = "",
    mediaCaption: String = "",
    replyToMessageId: String = "",
    replyToSenderName: String = "",
    replyToText: String = "",
    isForwarded: Boolean = false,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
    val docRef = db.collection("messages").document(msgId)
    val validMembers = (memberUids + uid).distinct().take(50)
    try {
      val payload = mapOf(
        "conversationId" to conversationId,
        "senderId" to uid,
        "senderName" to senderName.trim().take(60).ifBlank { "Pengguna" },
        "memberUids" to validMembers,
        "text" to text.trim().take(4000),
        "messageType" to messageType,
        "mediaData" to mediaData.take(60000),
        "mediaCaption" to mediaCaption.trim().take(500),
        "replyToMessageId" to replyToMessageId.take(128),
        "replyToSenderName" to replyToSenderName.take(60),
        "replyToText" to replyToText.take(300),
        "isForwarded" to isForwarded,
        "isEdited" to false,
        "isDeleted" to false,
        "status" to "DELIVERED",
        "reactionsSummary" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()

      val previewText = when (messageType) {
        "IMAGE" -> "📷 Foto: ${text.ifBlank { mediaCaption.ifBlank { "Gambar" } }}"
        "VIDEO" -> "🎥 Video: ${text.ifBlank { "Video pendek" }}"
        "VOICE" -> "🎤 Pesan suara (${mediaCaption.ifBlank { "0:05" }})"
        "DOCUMENT" -> "📄 Dokumen: ${mediaCaption.ifBlank { text }}"
        "LOCATION" -> "📍 Lokasi dibagikan"
        else -> text
      }
      db.collection("conversations").document(conversationId).update(
        mapOf(
          "lastMessageText" to previewText.take(500),
          "lastMessageSenderId" to uid,
          "lastMessageStatus" to "DELIVERED",
          "typingUserId" to "",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      msgId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun editMessage(messageId: String, newText: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("messages").document(messageId)
    try {
      docRef.update(
        mapOf(
          "text" to newText.trim().take(4000),
          "isEdited" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteMessageForEveryone(messageId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("messages").document(messageId)
    try {
      docRef.update(
        mapOf(
          "text" to "🚫 Pesan ini telah dihapus",
          "mediaData" to "",
          "mediaCaption" to "",
          "isDeleted" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun addReactionToMessage(messageId: String, emoji: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("messages").document(messageId)
    try {
      val snap = docRef.get().await()
      val msg = snap.toObject(ChatMessage::class.java) ?: return@runCatching
      val updatedReactions = if (msg.reactionsSummary.contains(emoji)) {
        msg.reactionsSummary.replace(emoji, "").trim()
      } else {
        "${msg.reactionsSummary} $emoji".trim().take(300)
      }
      docRef.update(
        mapOf(
          "reactionsSummary" to updatedReactions,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun markMessageAsRead(messageId: String, conversationId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("messages").document(messageId)
    try {
      docRef.update(
        mapOf(
          "status" to "READ",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      db.collection("conversations").document(conversationId).update(
        mapOf(
          "lastMessageStatus" to "READ",
          "unreadCount" to 0L,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- 5. STATUS 24 HOURS ---

  fun observeActiveStatuses(userId: String): Flow<List<StatusPost>> =
    db.collection("statuses")
      .whereArrayContains("visibleToUids", userId)
      .snapshots()
      .map { snap ->
        val now = System.currentTimeMillis()
        snap.toObjects(StatusPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { it.expiresAtMillis > now }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "statuses")
        throw e
      }

  suspend fun createStatusPost(
    authorName: String,
    authorAvatarData: String,
    statusType: String,
    contentText: String,
    mediaData: String = "",
    backgroundColorHex: String = "#00A884",
    privacyMode: String = "CONTACTS",
    contactUids: List<String> = emptyList(),
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val statusId = "sts_${UUID.randomUUID().toString().replace("-", "").take(18)}"
    val docRef = db.collection("statuses").document(statusId)
    val visibleUids = when (privacyMode) {
      "PRIVATE" -> listOf(uid)
      else -> (contactUids + uid).distinct().take(50)
    }
    val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
    try {
      val payload = mapOf(
        "authorId" to uid,
        "authorName" to authorName.trim().take(60).ifBlank { "Saya" },
        "authorAvatarData" to authorAvatarData.take(40000),
        "statusType" to statusType,
        "contentText" to contentText.trim().take(700),
        "mediaData" to mediaData.take(60000),
        "backgroundColorHex" to backgroundColorHex.take(16),
        "privacyMode" to privacyMode,
        "visibleToUids" to visibleUids,
        "viewerUids" to listOf(uid),
        "expiresAtMillis" to expiresAt,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      statusId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun markStatusViewed(statusId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("statuses").document(statusId)
    try {
      val snap = docRef.get().await()
      val post = snap.toObject(StatusPost::class.java) ?: return@runCatching
      if (!post.viewerUids.contains(uid)) {
        docRef.update(
          mapOf(
            "viewerUids" to (post.viewerUids + uid).distinct().take(50),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteStatusPost(statusId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("statuses").document(statusId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 6. COMMUNITIES ---

  fun observeCommunities(userId: String): Flow<List<Community>> =
    db.collection("communities")
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(Community::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "communities")
        throw e
      }

  suspend fun createCommunity(
    name: String,
    description: String,
    initialGroupIds: List<String> = emptyList(),
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val commId = "com_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val inviteCode = "WTC-${UUID.randomUUID().toString().take(8).uppercase()}"
    val docRef = db.collection("communities").document(commId)
    try {
      val payload = mapOf(
        "creatorId" to uid,
        "name" to name.trim().take(80).ifBlank { "Komunitas WhaTalk" },
        "description" to description.trim().take(300),
        "inviteCode" to inviteCode,
        "memberUids" to listOf(uid),
        "groupIds" to initialGroupIds.distinct().take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      commId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun addGroupToCommunity(communityId: String, groupId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("communities").document(communityId)
    try {
      val snap = docRef.get().await()
      val comm = snap.toObject(Community::class.java) ?: return@runCatching
      docRef.update(
        mapOf(
          "groupIds" to (comm.groupIds + groupId).distinct().take(30),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      db.collection("conversations").document(groupId).update(
        mapOf(
          "communityId" to communityId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun joinCommunityByInviteCode(inviteCode: String): Result<String> = runCatching {
    val uid = requireUserId()
    val path = "communities"
    try {
      val query = db.collection("communities")
        .whereEqualTo("inviteCode", inviteCode.trim().uppercase())
        .limit(1)
        .get()
        .await()
      val doc = query.documents.firstOrNull()
        ?: throw IllegalArgumentException("Kode undangan komunitas tidak ditemukan.")
      val comm = doc.toObject(Community::class.java)
        ?: throw IllegalStateException("Data komunitas tidak valid.")
      doc.reference.update(
        mapOf(
          "memberUids" to (comm.memberUids + uid).distinct().take(50),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      doc.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      throw e
    }
  }

  // --- 7. WEBRTC SIGNALING & CALL LOGS ---

  fun observeCalls(userId: String): Flow<List<CallLog>> =
    db.collection("calls")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(CallLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "calls")
        throw e
      }

  suspend fun startCall(
    callerName: String,
    receiverId: String,
    receiverName: String,
    callType: String,
    sdpOffer: String = "v=0\r\no=- 4611731400430051336 2 IN IP4 127.0.0.1\r\ns=WhaTalk-WebRTC-Session\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(18)}"
    val docRef = db.collection("calls").document(callId)
    val participants = listOf(uid, receiverId).distinct()
    try {
      val payload = mapOf(
        "callerId" to uid,
        "callerName" to callerName.trim().take(60).ifBlank { "Pemanggil" },
        "receiverId" to receiverId,
        "receiverName" to receiverName.trim().take(60).ifBlank { "Kontak" },
        "participantUids" to participants,
        "callType" to callType,
        "callState" to "RINGING",
        "durationSeconds" to 0L,
        "sdpOffer" to sdpOffer.take(10000),
        "sdpAnswer" to "",
        "iceCandidatesInfo" to "stun:stun.l.google.com:19302",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      callId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateCallState(
    callId: String,
    newState: String, // CONNECTED, ENDED, REJECTED, MISSED
    durationSeconds: Long = 0L,
    sdpAnswer: String = "",
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("calls").document(callId)
    try {
      val updates = mutableMapOf<String, Any>(
        "callState" to newState,
        "durationSeconds" to durationSeconds,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (sdpAnswer.isNotBlank()) {
        updates["sdpAnswer"] = sdpAnswer.take(10000)
      }
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- 8. BLOCKED USERS & ABUSE REPORTS ---

  fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> =
    db.collection("blockedUsers")
      .whereEqualTo("ownerId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, "blockedUsers")
        throw e
      }

  suspend fun blockUser(
    blockedUid: String,
    blockedUsername: String,
    blockedDisplayName: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val blockId = sanitizeId("blk_${uid}_${blockedUid}")
    val docRef = db.collection("blockedUsers").document(blockId)
    try {
      val payload = mapOf(
        "ownerId" to uid,
        "blockedUid" to blockedUid,
        "blockedUsername" to blockedUsername.trim().take(32),
        "blockedDisplayName" to blockedDisplayName.trim().take(60),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      blockId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun unblockUser(blockId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("blockedUsers").document(blockId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun reportUser(
    reportedUid: String,
    reportedUsername: String,
    reason: String,
    details: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(18)}"
    val docRef = db.collection("reports").document(reportId)
    try {
      val payload = mapOf(
        "reporterId" to uid,
        "reportedUid" to reportedUid,
        "reportedUsername" to reportedUsername.trim().take(60).ifBlank { "unknown" },
        "reason" to reason.trim().take(120).ifBlank { "Pelanggaran ketentuan" },
        "details" to details.trim().take(500),
        "status" to "SUBMITTED",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      reportId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }
}
