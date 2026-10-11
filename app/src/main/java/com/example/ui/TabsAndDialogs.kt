package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.CallLog
import com.example.data.Community
import com.example.data.Contact
import com.example.data.Conversation
import com.example.data.StatusPost
import com.example.ui.theme.WhaTalkReadBlue
import com.example.util.MediaAndLocationHelper
import kotlinx.coroutines.delay

@Composable
fun ChatListTab(
  conversations: List<Conversation>,
  currentUserId: String,
  searchQuery: String,
  onOpenConversation: (Conversation) -> Unit,
  onStartNewChat: () -> Unit,
  onCreateNewGroup: () -> Unit,
  onTogglePin: (Conversation) -> Unit,
  onToggleArchive: (Conversation) -> Unit,
  onToggleMute: (Conversation) -> Unit,
  onDeleteConversation: (Conversation) -> Unit,
) {
  var selectedFilter by remember { mutableIntStateOf(0) } // 0: Semua, 1: Belum Dibaca, 2: Grup, 3: Diarsipkan

  val filteredList = remember(conversations, selectedFilter, searchQuery, currentUserId) {
    conversations.filter { conv ->
      val isArchived = conv.archivedByUids.contains(currentUserId)
      val matchesSearch = searchQuery.isBlank() ||
        conv.title.contains(searchQuery, ignoreCase = true) ||
        conv.lastMessageText.contains(searchQuery, ignoreCase = true)

      val matchesTab = when (selectedFilter) {
        1 -> !isArchived && conv.unreadCount > 0L
        2 -> !isArchived && conv.isGroup
        3 -> isArchived
        else -> !isArchived
      }
      matchesSearch && matchesTab
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val filters = listOf("Semua", "Belum Dibaca", "Grup", "Diarsipkan")
      filters.forEachIndexed { idx, label ->
        FilterChip(
          selected = selectedFilter == idx,
          onClick = { selectedFilter = idx },
          label = { Text(label, fontSize = 12.sp) },
          modifier = Modifier.testTag("chat_filter_chip_$idx"),
        )
      }
    }

    if (filteredList.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_whatalk_hero_1791687490573),
          contentDescription = "Mulai Chat WhaTalk",
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp)),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = if (selectedFilter == 3) "Belum ada chat yang diarsipkan" else "Belum ada percakapan",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Cari teman menggunakan @username unik mereka atau buat grup baru untuk mulai mengobrol secara real-time.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = onStartNewChat,
            modifier = Modifier.testTag("empty_state_start_chat_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mulai Chat Baru")
          }
          OutlinedButton(
            onClick = onCreateNewGroup,
            modifier = Modifier.testTag("empty_state_create_group_button"),
          ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Buat Grup")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
      ) {
        items(filteredList, key = { it.id }) { conv ->
          var menuExpanded by remember { mutableStateOf(false) }
          val isPinned = conv.pinnedByUids.contains(currentUserId)
          val isMuted = conv.mutedByUids.contains(currentUserId)
          val isArchived = conv.archivedByUids.contains(currentUserId)

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenConversation(conv) }
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("conversation_item_${conv.id}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val avatarBmp = remember(conv.avatarData) {
              MediaAndLocationHelper.decodeDataUriToImageBitmap(conv.avatarData)
            }
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              if (avatarBmp != null) {
                Image(
                  bitmap = avatarBmp,
                  contentDescription = "Foto profil ${conv.title}",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop,
                )
              } else {
                Icon(
                  imageVector = if (conv.isGroup) Icons.Default.Group else Icons.Default.Person,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(28.dp),
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = conv.title.ifBlank { "Percakapan" },
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = MediaAndLocationHelper.formatChatTime(
                    conv.updatedAt?.seconds ?: conv.createdAt?.seconds
                  ),
                  fontSize = 12.sp,
                  color = if (conv.unreadCount > 0L) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }

              Spacer(modifier = Modifier.height(3.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (conv.lastMessageSenderId == currentUserId) {
                  val isRead = conv.lastMessageStatus == "READ"
                  Icon(
                    imageVector = if (conv.lastMessageStatus == "SENT") {
                      Icons.Default.Done
                    } else {
                      Icons.Default.DoneAll
                    },
                    contentDescription = conv.lastMessageStatus,
                    modifier = Modifier.size(16.dp),
                    tint = if (isRead) WhaTalkReadBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                }

                val subText = if (conv.typingUserId.isNotBlank() && conv.typingUserId != currentUserId) {
                  "Sedang mengetik..."
                } else {
                  conv.lastMessageText.ifBlank { "Ketuk untuk mengirim pesan" }
                }

                Text(
                  text = subText,
                  fontSize = 14.sp,
                  color = if (conv.typingUserId.isNotBlank() && conv.typingUserId != currentUserId) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )

                if (isMuted) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = "Dibisukan",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (isPinned) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Disematkan",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                if (conv.unreadCount > 0L) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                      text = conv.unreadCount.toString(),
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 4.dp),
                    )
                  }
                }
              }
            }

            Box {
              IconButton(onClick = { menuExpanded = true }) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "Opsi percakapan",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
              ) {
                DropdownMenuItem(
                  text = { Text(if (isPinned) "Lepas Sematan Pin" else "Sematkan Pin Chat") },
                  leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onTogglePin(conv)
                  },
                )
                DropdownMenuItem(
                  text = { Text(if (isArchived) "Keluarkan dari Arsip" else "Arsipkan Chat") },
                  leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onToggleArchive(conv)
                  },
                )
                DropdownMenuItem(
                  text = { Text(if (isMuted) "Bunyikan Notifikasi" else "Bisukan Notifikasi") },
                  leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onToggleMute(conv)
                  },
                )
                DropdownMenuItem(
                  text = { Text(if (conv.isGroup) "Keluar dari Grup" else "Hapus Percakapan") },
                  leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onDeleteConversation(conv)
                  },
                )
              }
            }
          }
          HorizontalDivider(
            modifier = Modifier.padding(start = 78.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun StatusTab(
  statuses: List<StatusPost>,
  currentUserId: String,
  onCreateStatus: (type: String, text: String, mediaData: String, bgHex: String) -> Unit,
  onMarkStatusViewed: (String) -> Unit,
  onDeleteStatus: (String) -> Unit,
  onReplyToStatus: (StatusPost, String) -> Unit,
) {
  val context = LocalContext.current
  var showCreateTextStatusDialog by remember { mutableStateOf(false) }
  var viewingStatus by remember { mutableStateOf<StatusPost?>(null) }
  var statusCaptionInput by remember { mutableStateOf("") }

  val statusPhotoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val compressed = MediaAndLocationHelper.compressImageUriToDataUri(context, uri, maxDimension = 260, jpegQuality = 68)
      if (compressed != null) {
        onCreateStatus("IMAGE", statusCaptionInput.ifBlank { "Status Foto WhaTalk" }, compressed, "#00A884")
        statusCaptionInput = ""
      }
    }
  }

  val myStatuses = remember(statuses, currentUserId) {
    statuses.filter { it.authorId == currentUserId }
  }
  val otherStatuses = remember(statuses, currentUserId) {
    statuses.filter { it.authorId != currentUserId }
  }
  val unseenStatuses = remember(otherStatuses, currentUserId) {
    otherStatuses.filter { !it.viewerUids.contains(currentUserId) }
  }
  val seenStatuses = remember(otherStatuses, currentUserId) {
    otherStatuses.filter { it.viewerUids.contains(currentUserId) }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                  if (myStatuses.isNotEmpty()) {
                    viewingStatus = myStatuses.first()
                  } else {
                    showCreateTextStatusDialog = true
                  }
                },
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Status Saya",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Status Saya", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text(
                text = if (myStatuses.isEmpty()) {
                  "Ketuk tombol di bawah untuk membuat status 24 jam"
                } else {
                  "${myStatuses.size} status aktif • Ketuk untuk melihat siapa yang melihat"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showCreateTextStatusDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("create_text_status_button"),
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Status Teks")
            }
            OutlinedButton(
              onClick = {
                statusPhotoLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("create_photo_status_button"),
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Status Foto")
            }
          }
        }
      }
    }

    if (myStatuses.isNotEmpty()) {
      item {
        Text(
          text = "Status Aktif Saya (Kedaluwarsa Otomatis 24 Jam)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      items(myStatuses, key = { it.id }) { sts ->
        StatusItemRow(
          status = sts,
          isUnseen = false,
          isMine = true,
          onClick = { viewingStatus = sts },
          onDelete = { onDeleteStatus(sts.id) },
        )
      }
    }

    if (unseenStatuses.isNotEmpty()) {
      item {
        Text(
          text = "Pembaruan Terkini",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      items(unseenStatuses, key = { it.id }) { sts ->
        StatusItemRow(
          status = sts,
          isUnseen = true,
          isMine = false,
          onClick = {
            onMarkStatusViewed(sts.id)
            viewingStatus = sts
          },
          onDelete = {},
        )
      }
    }

    if (seenStatuses.isNotEmpty()) {
      item {
        Text(
          text = "Pembaruan yang Telah Dilihat",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      items(seenStatuses, key = { it.id }) { sts ->
        StatusItemRow(
          status = sts,
          isUnseen = false,
          isMine = false,
          onClick = { viewingStatus = sts },
          onDelete = {},
        )
      }
    }
  }

  // CREATE TEXT STATUS DIALOG
  if (showCreateTextStatusDialog) {
    var textContent by remember { mutableStateOf("") }
    var selectedBgHex by remember { mutableStateOf("#00A884") }
    val bgChoices = listOf("#00A884", "#1976D2", "#7B1FA2", "#D32F2F", "#E65100")

    AlertDialog(
      onDismissRequest = { showCreateTextStatusDialog = false },
      title = { Text("Buat Status Teks (24 Jam)") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = textContent,
            onValueChange = { textContent = it.take(500) },
            label = { Text("Ketik isi status Anda...") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("status_text_input"),
            minLines = 3,
          )
          Text("Pilih Warna Latar:", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            bgChoices.forEach { hex ->
              val color = when (hex) {
                "#1976D2" -> Color(0xFF1976D2)
                "#7B1FA2" -> Color(0xFF7B1FA2)
                "#D32F2F" -> Color(0xFFD32F2F)
                "#E65100" -> Color(0xFFE65100)
                else -> Color(0xFF00A884)
              }
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(
                    width = if (selectedBgHex == hex) 3.dp else 0.dp,
                    color = Color.Black,
                    shape = CircleShape,
                  )
                  .clickable { selectedBgHex = hex }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (textContent.isNotBlank()) {
              onCreateStatus("TEXT", textContent.trim(), "", selectedBgHex)
              showCreateTextStatusDialog = false
            }
          },
          modifier = Modifier.testTag("publish_status_button"),
        ) {
          Text("Bagikan Status")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateTextStatusDialog = false }) {
          Text("Batal")
        }
      },
    )
  }

  // FULL STATUS VIEWER DIALOG
  if (viewingStatus != null) {
    val sts = viewingStatus!!
    var replyText by remember { mutableStateOf("") }
    val bgColor = when (sts.backgroundColorHex) {
      "#1976D2" -> Color(0xFF1976D2)
      "#7B1FA2" -> Color(0xFF7B1FA2)
      "#D32F2F" -> Color(0xFFD32F2F)
      "#E65100" -> Color(0xFFE65100)
      else -> Color(0xFF008069)
    }

    AlertDialog(
      onDismissRequest = { viewingStatus = null },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(sts.authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
              text = MediaAndLocationHelper.formatChatTime(sts.createdAt?.seconds),
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (sts.authorId == currentUserId) {
            IconButton(
              onClick = {
                onDeleteStatus(sts.id)
                viewingStatus = null
              }
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Hapus Status", tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(bgColor)
              .padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            val bmp = remember(sts.mediaData) {
              MediaAndLocationHelper.decodeDataUriToImageBitmap(sts.mediaData)
            }
            if (bmp != null) {
              Image(
                bitmap = bmp,
                contentDescription = "Gambar Status",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            } else {
              Text(
                text = sts.contentText,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
            }
          }

          if (sts.mediaData.isNotBlank() && sts.contentText.isNotBlank()) {
            Text(sts.contentText, fontWeight = FontWeight.Medium)
          }

          if (sts.authorId == currentUserId) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Dilihat oleh ${sts.viewerUids.size} pengguna",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          } else {
            OutlinedTextField(
              value = replyText,
              onValueChange = { replyText = it },
              placeholder = { Text("Balas status ${sts.authorName}...") },
              singleLine = true,
              trailingIcon = {
                IconButton(
                  onClick = {
                    if (replyText.isNotBlank()) {
                      onReplyToStatus(sts, replyText.trim())
                      viewingStatus = null
                    }
                  }
                ) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim balasan")
                }
              },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { viewingStatus = null }) {
          Text("Tutup")
        }
      },
    )
  }
}

@Composable
private fun StatusItemRow(
  status: StatusPost,
  isUnseen: Boolean,
  isMine: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(50.dp)
        .clip(CircleShape)
        .border(
          width = 2.5.dp,
          color = if (isUnseen) MaterialTheme.colorScheme.primary else Color.Gray,
          shape = CircleShape,
        )
        .background(MaterialTheme.colorScheme.secondaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = status.authorName.take(1).uppercase(),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(status.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Text(
        text = "${status.contentText.take(40)} • ${MediaAndLocationHelper.formatChatTime(status.createdAt?.seconds)}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (isMine) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.RemoveRedEye,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("${status.viewerUids.size}", fontSize = 12.sp)
        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}

@Composable
fun CommunitiesTab(
  communities: List<Community>,
  groupConversations: List<Conversation>,
  onCreateCommunity: (name: String, desc: String, groupIds: List<String>) -> Unit,
  onJoinCommunityByCode: (String) -> Unit,
  onJoinGroupByCode: (String) -> Unit,
  onLinkGroupToCommunity: (communityId: String, groupId: String) -> Unit,
  onOpenGroupChat: (Conversation) -> Unit,
) {
  var showCreateCommDialog by remember { mutableStateOf(false) }
  var showJoinInviteDialog by remember { mutableStateOf(false) }
  var linkingCommunity by remember { mutableStateOf<Community?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Komunitas WhaTalk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text(
                text = "Satukan beberapa grup terkait dalam satu wadah komunitas yang terorganisir.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showCreateCommDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("create_community_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Buat Komunitas")
            }
            OutlinedButton(
              onClick = { showJoinInviteDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("join_via_invite_button"),
            ) {
              Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Gabung Kode")
            }
          }
        }
      }
    }

    if (communities.isEmpty()) {
      item {
        Text(
          text = "Anda belum tergabung dalam komunitas. Buat komunitas baru atau masukkan kode undangan (WTC-... / WTG-...).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        )
      }
    } else {
      items(communities, key = { it.id }) { comm ->
        val linkedGroups = groupConversations.filter {
          comm.groupIds.contains(it.id) || it.communityId == comm.id
        }
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(comm.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (comm.description.isNotBlank()) {
                  Text(comm.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                  text = "Kode Undangan: ${comm.inviteCode} • ${comm.memberUids.size} anggota",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              TextButton(onClick = { linkingCommunity = comm }) {
                Text("+ Tambah Grup")
              }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (linkedGroups.isEmpty()) {
              Text(
                text = "Belum ada grup dalam komunitas ini.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              linkedGroups.forEach { grp ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGroupChat(grp) }
                    .padding(vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(grp.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                      text = grp.lastMessageText,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // CREATE COMMUNITY DIALOG
  if (showCreateCommDialog) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCreateCommDialog = false },
      title = { Text("Buat Komunitas Baru") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(80) },
            label = { Text("Nama Komunitas") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("community_name_input"),
          )
          OutlinedTextField(
            value = desc,
            onValueChange = { desc = it.take(250) },
            label = { Text("Deskripsi Komunitas") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank()) {
              onCreateCommunity(name.trim(), desc.trim(), emptyList())
              showCreateCommDialog = false
            }
          },
          modifier = Modifier.testTag("confirm_create_community_button"),
        ) {
          Text("Buat Komunitas")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateCommDialog = false }) {
          Text("Batal")
        }
      },
    )
  }

  // JOIN VIA INVITE CODE DIALOG
  if (showJoinInviteDialog) {
    var inviteCode by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showJoinInviteDialog = false },
      title = { Text("Gabung melalui Kode Undangan") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Masukkan kode undangan Grup (WTG-...) atau Komunitas (WTC-...):",
            fontSize = 13.sp,
          )
          OutlinedTextField(
            value = inviteCode,
            onValueChange = { inviteCode = it.uppercase().trim() },
            label = { Text("Kode Undangan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (inviteCode.startsWith("WTG-")) {
              onJoinGroupByCode(inviteCode)
            } else {
              onJoinCommunityByCode(inviteCode)
            }
            showJoinInviteDialog = false
          }
        ) {
          Text("Gabung Sekarang")
        }
      },
      dismissButton = {
        TextButton(onClick = { showJoinInviteDialog = false }) {
          Text("Batal")
        }
      },
    )
  }

  // LINK EXISTING GROUP TO COMMUNITY
  if (linkingCommunity != null) {
    val comm = linkingCommunity!!
    AlertDialog(
      onDismissRequest = { linkingCommunity = null },
      title = { Text("Tambahkan Grup ke ${comm.name}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (groupConversations.isEmpty()) {
            Text("Anda belum memiliki grup. Buat grup terlebih dahulu di tab Chat.")
          } else {
            groupConversations.forEach { grp ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onLinkGroupToCommunity(comm.id, grp.id)
                    linkingCommunity = null
                  }
              ) {
                Text(grp.title, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { linkingCommunity = null }) {
          Text("Tutup")
        }
      },
    )
  }
}

@Composable
fun CallsTab(
  calls: List<CallLog>,
  contacts: List<Contact>,
  currentUserId: String,
  onStartCall: (receiverId: String, receiverName: String, callType: String) -> Unit,
) {
  var showSelectContactForCall by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Panggilan Suara & Video WebRTC", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
              text = "Signaling real-time via Firestore + STUN (stun.l.google.com:19302)",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = { showSelectContactForCall = true },
            modifier = Modifier.testTag("start_new_call_button"),
          ) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Panggil")
          }
        }
      }
    }

    if (calls.isEmpty()) {
      item {
        Text(
          text = "Belum ada riwayat panggilan suara atau video.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        )
      }
    } else {
      items(calls, key = { it.id }) { call ->
        val isOutgoing = call.callerId == currentUserId
        val otherName = if (isOutgoing) call.receiverName else call.callerName
        val otherUid = if (isOutgoing) call.receiverId else call.callerId
        val isMissedOrRejected = call.callState in listOf("MISSED", "REJECTED")

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = otherName.take(1).uppercase(),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = otherName,
              fontWeight = FontWeight.Bold,
              color = if (isMissedOrRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = when {
                  isMissedOrRejected -> Icons.AutoMirrored.Filled.CallMissed
                  isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                  else -> Icons.AutoMirrored.Filled.CallReceived
                },
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (isMissedOrRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(4.dp))
              val stateLabel = when (call.callState) {
                "MISSED" -> "Tak terjawab"
                "REJECTED" -> "Ditolak"
                "RINGING" -> "Berdering..."
                "CONNECTED" -> "Sedang berlangsung"
                else -> "Selesai (${call.durationSeconds}d)"
              }
              Text(
                text = "$stateLabel • ${MediaAndLocationHelper.formatChatTime(call.createdAt?.seconds)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          IconButton(
            onClick = { onStartCall(otherUid, otherName, call.callType) }
          ) {
            Icon(
              imageVector = if (call.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
              contentDescription = "Panggil ulang",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
      }
    }
  }

  if (showSelectContactForCall) {
    AlertDialog(
      onDismissRequest = { showSelectContactForCall = false },
      title = { Text("Pilih Kontak untuk Dipanggil") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (contacts.isEmpty()) {
            Text("Daftar kontak masih kosong. Tambahkan kontak melalui pencarian @username terlebih dahulu.")
          } else {
            contacts.forEach { contact ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(contact.contactDisplayName, fontWeight = FontWeight.Medium)
                Row {
                  IconButton(
                    onClick = {
                      onStartCall(contact.contactUid, contact.contactDisplayName, "VOICE")
                      showSelectContactForCall = false
                    }
                  ) {
                    Icon(Icons.Default.Call, contentDescription = "Suara", tint = MaterialTheme.colorScheme.primary)
                  }
                  IconButton(
                    onClick = {
                      onStartCall(contact.contactUid, contact.contactDisplayName, "VIDEO")
                      showSelectContactForCall = false
                    }
                  ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSelectContactForCall = false }) {
          Text("Tutup")
        }
      },
    )
  }
}

@Composable
fun ActiveCallOverlay(
  callLog: CallLog,
  currentUserId: String,
  onAcceptCall: () -> Unit,
  onRejectOrEndCall: (newState: String, durationSec: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val isIncoming = callLog.receiverId == currentUserId
  val otherName = if (isIncoming) callLog.callerName else callLog.receiverName

  var isMicMuted by remember { mutableStateOf(false) }
  var isCameraOn by remember { mutableStateOf(callLog.callType == "VIDEO") }
  var isSpeakerOn by remember { mutableStateOf(callLog.callType == "VIDEO") }
  var durationSeconds by remember { mutableLongStateOf(0L) }
  var permissionsGranted by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { result ->
    permissionsGranted = result.values.all { it }
  }

  LaunchedEffect(callLog.id) {
    val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (callLog.callType == "VIDEO") {
      needed.add(Manifest.permission.CAMERA)
    }
    val allGranted = needed.all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    if (allGranted) {
      permissionsGranted = true
    } else {
      permissionLauncher.launch(needed.toTypedArray())
    }
  }

  LaunchedEffect(callLog.callState) {
    if (callLog.callState == "CONNECTED") {
      while (true) {
        delay(1000L)
        durationSeconds += 1L
      }
    } else if (callLog.callState in listOf("ENDED", "REJECTED", "MISSED")) {
      delay(800L)
      onDismiss()
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = Color(0xFF0B141A),
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = if (callLog.callType == "VIDEO") "Panggilan Video WhaTalk WebRTC" else "Panggilan Suara WhaTalk WebRTC",
          fontSize = 14.sp,
          color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "ICE Server: ${callLog.iceCandidatesInfo.ifBlank { "stun:stun.l.google.com:19302" }}",
          fontSize = 11.sp,
          color = Color(0xFF00A884),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
          modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(Color(0xFF008069)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = otherName.take(1).uppercase(),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = otherName,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val statusText = when (callLog.callState) {
          "RINGING" -> if (isIncoming) "Panggilan masuk..." else "Memanggil (Menunggu jawaban WebRTC SDP)..."
          "CONNECTED" -> {
            val mins = (durationSeconds / 60).toString().padStart(2, '0')
            val secs = (durationSeconds % 60).toString().padStart(2, '0')
            "Tersambung • $mins:$secs"
          }
          "REJECTED" -> "Panggilan ditolak"
          else -> "Panggilan berakhir"
        }
        Text(
          text = statusText,
          fontSize = 15.sp,
          color = Color(0xFF25D366),
        )
        if (!permissionsGranted) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Berikan izin mikrofon/kamera agar aliran media WebRTC aktif.",
            fontSize = 12.sp,
            color = Color(0xFFFFB74D),
            textAlign = TextAlign.Center,
          )
        }
      }

      if (callLog.callType == "VIDEO" && isCameraOn) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2C34)),
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (callLog.callState == "CONNECTED") {
                "Kamera Video Aktif (WebRTC P2P Stream)"
              } else {
                "Pratinjau Kamera Lokal Aktif • Menunggu Pihak Lawan Menerima"
              },
              fontSize = 13.sp,
              color = Color.White.copy(alpha = 0.85f),
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          IconButton(
            onClick = {
              isMicMuted = !isMicMuted
              runCatching {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.isMicrophoneMute = isMicMuted
              }
            },
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(if (isMicMuted) Color.Red else Color.White.copy(alpha = 0.18f)),
          ) {
            Icon(
              imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Mikrofon",
              tint = Color.White,
            )
          }

          IconButton(
            onClick = { isCameraOn = !isCameraOn },
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(if (!isCameraOn) Color.Red else Color.White.copy(alpha = 0.18f)),
          ) {
            Icon(
              imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
              contentDescription = "Kamera",
              tint = Color.White,
            )
          }

          IconButton(
            onClick = {
              isSpeakerOn = !isSpeakerOn
            },
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(if (isSpeakerOn) Color(0xFF00A884) else Color.White.copy(alpha = 0.18f)),
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = "Speaker",
              tint = Color.White,
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          if (isIncoming && callLog.callState == "RINGING") {
            Button(
              onClick = onAcceptCall,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
              modifier = Modifier
                .height(54.dp)
                .testTag("accept_call_button"),
            ) {
              Icon(Icons.Default.Call, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Terima")
            }
          }

          Button(
            onClick = {
              val nextState = if (callLog.callState == "RINGING") {
                if (isIncoming) "REJECTED" else "MISSED"
              } else {
                "ENDED"
              }
              onRejectOrEndCall(nextState, durationSeconds)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier
              .height(54.dp)
              .testTag("end_call_button"),
          ) {
            Icon(Icons.Default.CallEnd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isIncoming && callLog.callState == "RINGING") "Tolak" else "Akhiri Panggilan")
          }
        }
      }
    }
  }
}
