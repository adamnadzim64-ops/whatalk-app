package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ChatMessage
import com.example.data.Conversation
import com.example.ui.theme.WhaTalkChatWallpaperDark
import com.example.ui.theme.WhaTalkChatWallpaperLight
import com.example.ui.theme.WhaTalkIncomingBubbleDark
import com.example.ui.theme.WhaTalkIncomingBubbleLight
import com.example.ui.theme.WhaTalkOutgoingBubbleDark
import com.example.ui.theme.WhaTalkOutgoingBubbleLight
import com.example.ui.theme.WhaTalkReadBlue
import com.example.util.MediaAndLocationHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
  conversation: Conversation,
  messages: List<ChatMessage>,
  allConversations: List<Conversation>,
  currentUserId: String,
  fontScaleOption: String,
  wallpaperHex: String,
  onBack: () -> Unit,
  onSendMessage: (text: String, type: String, mediaData: String, caption: String, replyTo: ChatMessage?, isForwarded: Boolean) -> Unit,
  onForwardMessage: (targetConversation: Conversation, message: ChatMessage) -> Unit,
  onEditMessage: (messageId: String, newText: String) -> Unit,
  onDeleteMessage: (messageId: String) -> Unit,
  onReactMessage: (messageId: String, emoji: String) -> Unit,
  onTypingChanged: (Boolean) -> Unit,
  onLoadOlderMessages: () -> Unit,
  onStartVoiceCall: () -> Unit,
  onStartVideoCall: () -> Unit,
  onUpdateGroupInfo: (title: String, description: String, memberUids: List<String>, adminUids: List<String>) -> Unit,
  onLeaveOrDeleteConversation: () -> Unit,
  onBlockUser: (uid: String, username: String, displayName: String) -> Unit,
  onReportUser: (uid: String, username: String, reason: String, details: String) -> Unit,
  onShowBanner: (String) -> Unit,
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val textScaleMultiplier = when (fontScaleOption) {
    "SMALL" -> 0.9f
    "LARGE" -> 1.15f
    else -> 1.0f
  }

  var messageInput by remember { mutableStateOf("") }
  var showEmojiBar by remember { mutableStateOf(false) }
  var showAttachmentRow by remember { mutableStateOf(false) }
  var showInChatSearch by remember { mutableStateOf(false) }
  var inChatSearchQuery by remember { mutableStateOf("") }
  var showTopMenu by remember { mutableStateOf(false) }
  var showGroupInfoDialog by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }

  var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
  var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
  var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
  var messageToForward by remember { mutableStateOf<ChatMessage?>(null) }

  // Voice recording state
  var isRecordingVoice by remember { mutableStateOf(false) }
  var voiceRecordSeconds by remember { mutableIntStateOf(0) }

  LaunchedEffect(isRecordingVoice) {
    if (isRecordingVoice) {
      voiceRecordSeconds = 0
      while (isRecordingVoice) {
        delay(1000L)
        voiceRecordSeconds += 1
      }
    }
  }

  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  // Media & Permission Launchers
  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val compressed = MediaAndLocationHelper.compressImageUriToDataUri(context, uri)
      if (compressed != null) {
        onSendMessage("Foto", "IMAGE", compressed, messageInput.ifBlank { "Foto WhaTalk" }, replyingTo, false)
        messageInput = ""
        replyingTo = null
        showAttachmentRow = false
      } else {
        onShowBanner("Gagal memproses gambar.")
      }
    }
  }

  val documentPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      val docName = uri.lastPathSegment?.substringAfterLast('/') ?: "Dokumen_WhaTalk.pdf"
      onSendMessage(docName, "DOCUMENT", uri.toString(), docName, replyingTo, false)
      replyingTo = null
      showAttachmentRow = false
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      val locSummary = MediaAndLocationHelper.getCurrentLocationSummary(context)
      onSendMessage(locSummary, "LOCATION", locSummary, "Lokasi Terkini", replyingTo, false)
      replyingTo = null
      showAttachmentRow = false
    } else {
      onShowBanner("Izin lokasi ditolak. Aktifkan izin lokasi untuk berbagi lokasi.")
    }
  }

  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      isRecordingVoice = true
    } else {
      onShowBanner("Izin mikrofon diperlukan untuk mengirim pesan suara.")
    }
  }

  val filteredMessages = remember(messages, inChatSearchQuery) {
    if (inChatSearchQuery.isBlank()) {
      messages
    } else {
      messages.filter {
        it.text.contains(inChatSearchQuery, ignoreCase = true) ||
          it.mediaCaption.contains(inChatSearchQuery, ignoreCase = true)
      }
    }
  }

  val chatBackground = remember(isDark, wallpaperHex) {
    if (isDark) {
      WhaTalkChatWallpaperDark
    } else {
      when (wallpaperHex) {
        "#DCF8C6" -> Color(0xFFE3F5D8)
        "#E3F2FD" -> Color(0xFFE3F2FD)
        "#FFF3E0" -> Color(0xFFFFF3E0)
        else -> WhaTalkChatWallpaperLight
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(chatBackground)
      .imePadding(),
  ) {
    // HEADER CHAT
    Surface(
      color = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      shadowElevation = 4.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.statusBars)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("chat_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
          }

          val avatarBmp = remember(conversation.avatarData) {
            MediaAndLocationHelper.decodeDataUriToImageBitmap(conversation.avatarData)
          }
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.2f))
              .clickable { showGroupInfoDialog = true },
            contentAlignment = Alignment.Center,
          ) {
            if (avatarBmp != null) {
              Image(
                bitmap = avatarBmp,
                contentDescription = "Foto kontak",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            } else {
              Icon(
                imageVector = if (conversation.isGroup) Icons.Default.Group else Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))
          Column(
            modifier = Modifier
              .weight(1f)
              .clickable { showGroupInfoDialog = true },
          ) {
            Text(
              text = conversation.title.ifBlank { "Percakapan WhaTalk" },
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = Color.White,
            )
            val subtitle = when {
              conversation.typingUserId.isNotBlank() && conversation.typingUserId != currentUserId ->
                "Sedang mengetik..."
              conversation.isGroup ->
                "${conversation.memberUids.size} anggota • Ketuk untuk info grup"
              else ->
                "Online • Terenkripsi aturan keamanan"
            }
            Text(
              text = subtitle,
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.85f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          IconButton(
            onClick = onStartVoiceCall,
            modifier = Modifier.testTag("chat_voice_call_button"),
          ) {
            Icon(Icons.Default.Call, contentDescription = "Panggilan Suara", tint = Color.White)
          }
          IconButton(
            onClick = onStartVideoCall,
            modifier = Modifier.testTag("chat_video_call_button"),
          ) {
            Icon(Icons.Default.Videocam, contentDescription = "Panggilan Video", tint = Color.White)
          }
          IconButton(
            onClick = { showInChatSearch = !showInChatSearch },
            modifier = Modifier.testTag("chat_search_button"),
          ) {
            Icon(Icons.Default.Search, contentDescription = "Cari di percakapan", tint = Color.White)
          }
          Box {
            IconButton(
              onClick = { showTopMenu = true },
              modifier = Modifier.testTag("chat_more_menu_button"),
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "Menu lainnya", tint = Color.White)
            }
            DropdownMenu(
              expanded = showTopMenu,
              onDismissRequest = { showTopMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text(if (conversation.isGroup) "Info Grup & Anggota" else "Info Kontak") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = {
                  showTopMenu = false
                  showGroupInfoDialog = true
                },
              )
              if (!conversation.isGroup) {
                val otherUid = conversation.memberUids.firstOrNull { it != currentUserId } ?: ""
                DropdownMenuItem(
                  text = { Text("Blokir Kontak") },
                  leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                  onClick = {
                    showTopMenu = false
                    if (otherUid.isNotBlank()) {
                      onBlockUser(otherUid, conversation.title.lowercase(), conversation.title)
                    }
                  },
                )
                DropdownMenuItem(
                  text = { Text("Laporkan Akun") },
                  leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                  onClick = {
                    showTopMenu = false
                    showReportDialog = true
                  },
                )
              }
              DropdownMenuItem(
                text = { Text(if (conversation.isGroup) "Keluar dari Grup" else "Hapus Percakapan") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                  showTopMenu = false
                  onLeaveOrDeleteConversation()
                },
              )
            }
          }
        }

        AnimatedVisibility(visible = showInChatSearch) {
          OutlinedTextField(
            value = inChatSearchQuery,
            onValueChange = { inChatSearchQuery = it },
            placeholder = { Text("Cari kata kunci dalam percakapan...") },
            singleLine = true,
            trailingIcon = {
              if (inChatSearchQuery.isNotEmpty()) {
                IconButton(onClick = { inChatSearchQuery = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Bersihkan")
                }
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
    }

    // MESSAGES LIST
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      item {
        if (messages.size >= 45) {
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            OutlinedButton(
              onClick = onLoadOlderMessages,
              modifier = Modifier.testTag("load_older_messages_button"),
            ) {
              Text("Muat pesan lebih lama", fontSize = 12.sp)
            }
          }
        }
      }

      itemsIndexed(filteredMessages, key = { _, item -> item.id }) { index, msg ->
        val prevMsg = filteredMessages.getOrNull(index - 1)
        val currentDay = MediaAndLocationHelper.formatDateSeparator(msg.createdAt?.seconds)
        val prevDay = if (prevMsg != null) {
          MediaAndLocationHelper.formatDateSeparator(prevMsg.createdAt?.seconds)
        } else {
          ""
        }

        if (currentDay != prevDay) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
              shape = RoundedCornerShape(10.dp),
              shadowElevation = 1.dp,
            ) {
              Text(
                text = currentDay,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              )
            }
          }
        }

        val isMine = msg.senderId == currentUserId
        val bubbleColor = when {
          isMine && isDark -> WhaTalkOutgoingBubbleDark
          isMine && !isDark -> WhaTalkOutgoingBubbleLight
          !isMine && isDark -> WhaTalkIncomingBubbleDark
          else -> WhaTalkIncomingBubbleLight
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        ) {
          Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
              topStart = 14.dp,
              topEnd = 14.dp,
              bottomStart = if (isMine) 14.dp else 2.dp,
              bottomEnd = if (isMine) 2.dp else 14.dp,
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
              .widthIn(min = 90.dp, max = 310.dp)
              .combinedClickable(
                onClick = { selectedMessageForActions = msg },
                onLongClick = { selectedMessageForActions = msg },
              ),
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
              if (conversation.isGroup && !isMine) {
                Text(
                  text = msg.senderName,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              if (msg.isForwarded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Diteruskan",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              if (msg.replyToMessageId.isNotBlank()) {
                Surface(
                  color = Color.Black.copy(alpha = 0.06f),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                ) {
                  Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                      text = msg.replyToSenderName.ifBlank { "Pesan sebelumnya" },
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = msg.replyToText,
                      fontSize = 11.sp,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }

              // Media content rendering
              when (msg.messageType) {
                "IMAGE" -> {
                  val bmp = remember(msg.mediaData) {
                    MediaAndLocationHelper.decodeDataUriToImageBitmap(msg.mediaData)
                  }
                  if (bmp != null) {
                    Image(
                      bitmap = bmp,
                      contentDescription = "Gambar terkirim",
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(8.dp)),
                      contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                  }
                }

                "VOICE" -> {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.PlayArrow,
                      contentDescription = "Putar pesan suara",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(4.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "Pesan Suara • ${msg.mediaCaption.ifBlank { "0:05" }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                      )
                      Text(
                        text = "▂▄▆█▄▂▅▇▃▂▄▆▂",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }

                "DOCUMENT" -> {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color.Black.copy(alpha = 0.05f))
                      .padding(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Description,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = msg.mediaCaption.ifBlank { msg.text },
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                "LOCATION" -> {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color.Black.copy(alpha = 0.05f))
                      .padding(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = Color(0xFFD32F2F),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = msg.text,
                      fontSize = 12.sp,
                    )
                  }
                }
              }

              if (msg.text.isNotBlank() && msg.messageType != "LOCATION") {
                Text(
                  text = msg.text,
                  fontSize = (15f * textScaleMultiplier).sp,
                  fontStyle = if (msg.isDeleted) FontStyle.Italic else FontStyle.Normal,
                )
              }

              // Link preview if message contains http/https
              if (msg.text.contains("http://") || msg.text.contains("https://")) {
                Surface(
                  color = Color.Black.copy(alpha = 0.05f),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                ) {
                  Text(
                    text = "🔗 Pratinjau Tautan Terverifikasi",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(6.dp),
                  )
                }
              }

              if (msg.reactionsSummary.isNotBlank()) {
                Surface(
                  color = MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(12.dp),
                  shadowElevation = 1.dp,
                  modifier = Modifier.padding(top = 4.dp),
                ) {
                  Text(
                    text = msg.reactionsSummary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }
              }

              Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (msg.isEdited && !msg.isDeleted) {
                  Text(
                    text = "Diedit • ",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = MediaAndLocationHelper.formatChatTime(msg.createdAt?.seconds),
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isMine) {
                  Spacer(modifier = Modifier.width(4.dp))
                  val isRead = msg.status == "READ"
                  Icon(
                    imageVector = if (msg.status == "SENT") Icons.Default.Done else Icons.Default.DoneAll,
                    contentDescription = msg.status,
                    modifier = Modifier.size(14.dp),
                    tint = if (isRead) WhaTalkReadBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    // REPLY OR EDIT BANNER
    if (replyingTo != null || editingMessage != null) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (editingMessage != null) {
                "Mengedit pesan"
              } else {
                "Membalas ${replyingTo?.senderName ?: ""}"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = editingMessage?.text ?: replyingTo?.text ?: "",
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = {
              replyingTo = null
              editingMessage = null
            }
          ) {
            Icon(Icons.Default.Close, contentDescription = "Batal")
          }
        }
      }
    }

    // EMOJI QUICK BAR
    AnimatedVisibility(visible = showEmojiBar) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          listOf("😀", "😂", "😍", "🥳", "👍", "🙏", "🔥", "❤️", "🎉", "😎", "😢", "🤝").forEach { emoji ->
            Text(
              text = emoji,
              fontSize = 24.sp,
              modifier = Modifier.clickable {
                messageInput += emoji
              },
            )
          }
        }
      }
    }

    // ATTACHMENT ROW
    AnimatedVisibility(visible = showAttachmentRow) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          AttachmentOptionButton(
            icon = Icons.Default.Image,
            label = "Galeri Foto",
            color = Color(0xFF9C27B0),
            onClick = {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          )
          AttachmentOptionButton(
            icon = Icons.Default.Videocam,
            label = "Video",
            color = Color(0xFFE91E63),
            onClick = {
              onSendMessage("🎥 Video Pendek (0:15)", "VIDEO", "video_clip", "Video Pendek", replyingTo, false)
              replyingTo = null
              showAttachmentRow = false
            },
          )
          AttachmentOptionButton(
            icon = Icons.Default.Description,
            label = "Dokumen",
            color = Color(0xFF3F51B5),
            onClick = {
              documentPicker.launch(arrayOf("*/*"))
            },
          )
          AttachmentOptionButton(
            icon = Icons.Default.LocationOn,
            label = "Lokasi",
            color = Color(0xFF009688),
            onClick = {
              val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
              ) == PackageManager.PERMISSION_GRANTED
              if (hasPerm) {
                val locSummary = MediaAndLocationHelper.getCurrentLocationSummary(context)
                onSendMessage(locSummary, "LOCATION", locSummary, "Lokasi Terkini", replyingTo, false)
                replyingTo = null
                showAttachmentRow = false
              } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
              }
            },
          )
        }
      }
    }

    // VOICE RECORDING ACTIVE BAR
    if (isRecordingVoice) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Merekam suara... 0:${voiceRecordSeconds.toString().padStart(2, '0')}",
              fontWeight = FontWeight.Bold,
            )
          }
          Row {
            TextButton(onClick = { isRecordingVoice = false }) {
              Text("Batal")
            }
            Button(
              onClick = {
                val dur = "0:${voiceRecordSeconds.coerceAtLeast(1).toString().padStart(2, '0')}"
                isRecordingVoice = false
                onSendMessage("Pesan Suara ($dur)", "VOICE", "voice_note", dur, replyingTo, false)
                replyingTo = null
              }
            ) {
              Icon(Icons.Default.Stop, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Kirim")
            }
          }
        }
      }
    }

    // BOTTOM MESSAGE COMPOSER BAR
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { showEmojiBar = !showEmojiBar },
          modifier = Modifier.testTag("chat_emoji_button"),
        ) {
          Icon(
            imageVector = Icons.Default.EmojiEmotions,
            contentDescription = "Emoji",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        OutlinedTextField(
          value = messageInput,
          onValueChange = {
            messageInput = it
            onTypingChanged(it.isNotBlank())
          },
          placeholder = { Text("Ketik pesan...") },
          modifier = Modifier
            .weight(1f)
            .testTag("chat_message_input"),
          shape = RoundedCornerShape(24.dp),
          maxLines = 4,
          trailingIcon = {
            IconButton(
              onClick = { showAttachmentRow = !showAttachmentRow },
              modifier = Modifier.testTag("chat_attachment_button"),
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = "Lampirkan file")
            }
          },
        )

        Spacer(modifier = Modifier.width(6.dp))

        val hasText = messageInput.isNotBlank()
        IconButton(
          onClick = {
            if (hasText) {
              val cleanText = messageInput.trim()
              if (editingMessage != null) {
                onEditMessage(editingMessage!!.id, cleanText)
                editingMessage = null
              } else {
                onSendMessage(cleanText, "TEXT", "", "", replyingTo, false)
                replyingTo = null
              }
              messageInput = ""
              onTypingChanged(false)
            } else {
              val hasAudioPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
              ) == PackageManager.PERMISSION_GRANTED
              if (hasAudioPerm) {
                isRecordingVoice = true
              } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            }
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .testTag("chat_send_or_mic_button"),
        ) {
          Icon(
            imageVector = if (hasText) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
            contentDescription = if (hasText) "Kirim Pesan" else "Rekam Pesan Suara",
            tint = Color.White,
          )
        }
      }
    }
  }

  // MESSAGE ACTIONS DIALOG (Reply, Forward, Copy, Edit, Delete, Reactions)
  if (selectedMessageForActions != null) {
    val target = selectedMessageForActions!!
    val isMine = target.senderId == currentUserId
    AlertDialog(
      onDismissRequest = { selectedMessageForActions = null },
      title = { Text("Aksi Pesan", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Beri Reaksi Emoji:",
            style = MaterialTheme.typography.labelMedium,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
              Text(
                text = emoji,
                fontSize = 26.sp,
                modifier = Modifier.clickable {
                  onReactMessage(target.id, emoji)
                  selectedMessageForActions = null
                },
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = {
              replyingTo = target
              selectedMessageForActions = null
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Balas Pesan Ini")
          }
          OutlinedButton(
            onClick = {
              messageToForward = target
              selectedMessageForActions = null
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Teruskan Pesan")
          }
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Pesan WhaTalk", target.text))
              onShowBanner("Pesan disalin ke papan klip.")
              selectedMessageForActions = null
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salin Teks")
          }
          if (isMine && !target.isDeleted) {
            OutlinedButton(
              onClick = {
                editingMessage = target
                messageInput = target.text
                selectedMessageForActions = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Default.Edit, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Edit Pesan")
            }
            OutlinedButton(
              onClick = {
                onDeleteMessage(target.id)
                selectedMessageForActions = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Hapus untuk Semua Orang", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedMessageForActions = null }) {
          Text("Tutup")
        }
      },
    )
  }

  // FORWARD MESSAGE TARGET DIALOG
  if (messageToForward != null) {
    val msg = messageToForward!!
    AlertDialog(
      onDismissRequest = { messageToForward = null },
      title = { Text("Teruskan Pesan ke...") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          allConversations.take(8).forEach { targetConv ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onForwardMessage(targetConv, msg)
                  messageToForward = null
                  onShowBanner("Pesan diteruskan ke ${targetConv.title}")
                },
            ) {
              Text(
                text = targetConv.title.ifBlank { "Percakapan" },
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { messageToForward = null }) {
          Text("Batal")
        }
      },
    )
  }

  // GROUP / CONTACT INFO DIALOG
  if (showGroupInfoDialog) {
    var editTitle by remember { mutableStateOf(conversation.title) }
    var editDesc by remember { mutableStateOf(conversation.description) }
    val isAdmin = conversation.adminUids.contains(currentUserId)

    AlertDialog(
      onDismissRequest = { showGroupInfoDialog = false },
      title = {
        Text(if (conversation.isGroup) "Informasi & Pengelolaan Grup" else "Informasi Kontak")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (conversation.isGroup) {
            OutlinedTextField(
              value = editTitle,
              onValueChange = { editTitle = it },
              label = { Text("Nama Grup") },
              enabled = isAdmin,
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = editDesc,
              onValueChange = { editDesc = it },
              label = { Text("Deskripsi Grup") },
              enabled = isAdmin,
              modifier = Modifier.fillMaxWidth(),
            )
            if (conversation.inviteCode.isNotBlank()) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Kode Undangan Grup Aman:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  Text(conversation.inviteCode, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
            Text(
              text = "Daftar Anggota (${conversation.memberUids.size}):",
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
            )
            conversation.memberUids.forEach { memberUid ->
              val isMemberAdmin = conversation.adminUids.contains(memberUid)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (memberUid == currentUserId) "Anda ($memberUid)" else "UID: ${memberUid.take(12)}...",
                  fontSize = 12.sp,
                )
                Row {
                  if (isMemberAdmin) {
                    Text(
                      text = "Admin",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  if (isAdmin && memberUid != currentUserId) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Keluarkan",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.clickable {
                        onUpdateGroupInfo(
                          editTitle,
                          editDesc,
                          conversation.memberUids - memberUid,
                          conversation.adminUids - memberUid
                        )
                      },
                    )
                  }
                }
              }
            }
          } else {
            Text("Nama: ${conversation.title}", fontWeight = FontWeight.Bold)
            Text("Status Keamanan: Percakapan Pribadi Terlindungi Firestore Rules", fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        if (conversation.isGroup && isAdmin) {
          Button(
            onClick = {
              onUpdateGroupInfo(editTitle, editDesc, conversation.memberUids, conversation.adminUids)
              showGroupInfoDialog = false
            }
          ) {
            Text("Simpan Grup")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showGroupInfoDialog = false }) {
          Text("Tutup")
        }
      },
    )
  }

  // REPORT DIALOG
  if (showReportDialog) {
    var reason by remember { mutableStateOf("Spam atau Penipuan") }
    var details by remember { mutableStateOf("") }
    val otherUid = conversation.memberUids.firstOrNull { it != currentUserId } ?: "unknown"
    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      title = { Text("Laporkan Akun") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Alasan Laporan") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Keterangan Tambahan") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onReportUser(otherUid, conversation.title, reason, details)
            showReportDialog = false
          }
        ) {
          Text("Kirim Laporan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) {
          Text("Batal")
        }
      },
    )
  }
}

@Composable
private fun AttachmentOptionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  color: Color,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable { onClick() }
      .padding(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(color),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = label, tint = Color.White)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(label, fontSize = 11.sp)
  }
}
