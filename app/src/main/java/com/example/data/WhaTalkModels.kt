package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val displayName: String = "",
  val bio: String = "Ada di WhaTalk",
  val avatarData: String = "",
  @get:PropertyName("isOnline") @set:PropertyName("isOnline")
  var isOnline: Boolean = true,
  val lastSeenText: String = "Online",
  @get:PropertyName("isDiscoverable") @set:PropertyName("isDiscoverable")
  var isDiscoverable: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserSettings(
  @DocumentId val id: String = "",
  val userId: String = "",
  val phoneNumber: String = "",
  val email: String = "",
  val privacyPhoto: String = "EVERYONE",
  val privacyBio: String = "EVERYONE",
  val privacyLastSeen: String = "EVERYONE",
  val privacyOnline: String = "EVERYONE",
  val privacyStatus: String = "CONTACTS",
  val readReceipts: Boolean = true,
  val themeMode: String = "SYSTEM",
  val wallpaperHex: String = "#ECE5DD",
  val fontScale: String = "MEDIUM",
  val notificationsEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Contact(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val contactUid: String = "",
  val contactUsername: String = "",
  val contactDisplayName: String = "",
  val contactBio: String = "",
  val contactAvatarData: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Conversation(
  @DocumentId val id: String = "",
  val creatorId: String = "",
  val memberUids: List<String> = emptyList(),
  val adminUids: List<String> = emptyList(),
  @get:PropertyName("isGroup") @set:PropertyName("isGroup")
  var isGroup: Boolean = false,
  val title: String = "",
  val description: String = "",
  val avatarData: String = "",
  val communityId: String = "",
  val inviteCode: String = "",
  val lastMessageText: String = "",
  val lastMessageSenderId: String = "",
  val lastMessageStatus: String = "SENT",
  val typingUserId: String = "",
  val pinnedByUids: List<String> = emptyList(),
  val archivedByUids: List<String> = emptyList(),
  val mutedByUids: List<String> = emptyList(),
  val unreadCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val conversationId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val memberUids: List<String> = emptyList(),
  val text: String = "",
  val messageType: String = "TEXT", // TEXT, IMAGE, VIDEO, DOCUMENT, VOICE, LOCATION
  val mediaData: String = "",
  val mediaCaption: String = "",
  val replyToMessageId: String = "",
  val replyToSenderName: String = "",
  val replyToText: String = "",
  @get:PropertyName("isForwarded") @set:PropertyName("isForwarded")
  var isForwarded: Boolean = false,
  @get:PropertyName("isEdited") @set:PropertyName("isEdited")
  var isEdited: Boolean = false,
  @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
  var isDeleted: Boolean = false,
  val status: String = "SENT", // SENT, DELIVERED, READ
  val reactionsSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Community(
  @DocumentId val id: String = "",
  val creatorId: String = "",
  val name: String = "",
  val description: String = "",
  val inviteCode: String = "",
  val memberUids: List<String> = emptyList(),
  val groupIds: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StatusPost(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatarData: String = "",
  val statusType: String = "TEXT", // TEXT, IMAGE, VIDEO
  val contentText: String = "",
  val mediaData: String = "",
  val backgroundColorHex: String = "#00A884",
  val privacyMode: String = "CONTACTS", // CONTACTS, PUBLIC, PRIVATE
  val visibleToUids: List<String> = emptyList(),
  val viewerUids: List<String> = emptyList(),
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CallLog(
  @DocumentId val id: String = "",
  val callerId: String = "",
  val callerName: String = "",
  val receiverId: String = "",
  val receiverName: String = "",
  val participantUids: List<String> = emptyList(),
  val callType: String = "VOICE", // VOICE, VIDEO
  val callState: String = "RINGING", // RINGING, CONNECTED, ENDED, REJECTED, MISSED
  val durationSeconds: Long = 0L,
  val sdpOffer: String = "",
  val sdpAnswer: String = "",
  val iceCandidatesInfo: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BlockedUser(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val blockedUid: String = "",
  val blockedUsername: String = "",
  val blockedDisplayName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AbuseReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val reportedUid: String = "",
  val reportedUsername: String = "",
  val reason: String = "",
  val details: String = "",
  val status: String = "SUBMITTED",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
