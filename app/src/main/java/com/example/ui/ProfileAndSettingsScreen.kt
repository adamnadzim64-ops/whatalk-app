package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockedUser
import com.example.data.Contact
import com.example.data.UserProfile
import com.example.data.UserSettings
import com.example.util.MediaAndLocationHelper

@Composable
fun ContactsAndUserSearchDialog(
  contacts: List<Contact>,
  searchedUsers: List<UserProfile>,
  isSearching: Boolean,
  onSearchUsername: (String) -> Unit,
  onSelectUserToAddAndChat: (UserProfile) -> Unit,
  onSelectContactToChat: (Contact) -> Unit,
  onBlockContact: (Contact) -> Unit,
  onOpenCreateGroup: () -> Unit,
  onDismiss: () -> Unit,
) {
  var usernameQuery by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Kontak & Cari Pengguna (@username)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenCreateGroup()
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_create_group_button"),
        ) {
          Text("+ Buat Grup Baru")
        }

        Text(
          text = "Cari pengguna WhaTalk berdasarkan @username unik (Privasi aman: tidak menampilkan seluruh database publik):",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = usernameQuery,
            onValueChange = { usernameQuery = it.lowercase().trim() },
            label = { Text("Ketik @username") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("search_username_input"),
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = { onSearchUsername(usernameQuery) },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
              .testTag("execute_username_search_button"),
          ) {
            if (isSearching) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color.White)
            }
          }
        }

        if (searchedUsers.isNotEmpty()) {
          Text("Hasil Pencarian Username:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          searchedUsers.forEach { profile ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(profile.displayName, fontWeight = FontWeight.Bold)
                  Text("@${profile.username} • ${profile.bio}", fontSize = 12.sp)
                }
                Button(
                  onClick = { onSelectUserToAddAndChat(profile) },
                  modifier = Modifier.testTag("start_chat_with_${profile.username}"),
                ) {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Chat")
                }
              }
            }
          }
        }

        HorizontalDivider()
        Text("Daftar Kontak Tersimpan (${contacts.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (contacts.isEmpty()) {
          Text(
            text = "Belum ada kontak tersimpan. Cari teman di atas menggunakan @username mereka.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          contacts.forEach { contact ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectContactToChat(contact) }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(contact.contactDisplayName, fontWeight = FontWeight.SemiBold)
                Text("@${contact.contactUsername} • ${contact.contactBio}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Row {
                IconButton(onClick = { onSelectContactToChat(contact) }) {
                  Icon(Icons.Default.Chat, contentDescription = "Mulai Chat", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onBlockContact(contact) }) {
                  Icon(Icons.Default.Block, contentDescription = "Blokir", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Tutup")
      }
    },
  )
}

@Composable
fun CreateGroupDialog(
  contacts: List<Contact>,
  onCreateGroup: (title: String, desc: String, selectedMemberUids: List<String>) -> Unit,
  onDismiss: () -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val selectedUids = remember { mutableStateListOf<String>() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Buat Grup Baru WhaTalk", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(80) },
          label = { Text("Nama Grup *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_group_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it.take(240) },
          label = { Text("Deskripsi Grup") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_group_desc_input"),
        )
        Text("Pilih Anggota dari Kontak (${selectedUids.size} dipilih):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (contacts.isEmpty()) {
          Text(
            text = "Anda tetap dapat membuat grup sekarang dan mengundang anggota melalui Kode Undangan Grup (WTG-...).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          contacts.forEach { contact ->
            val checked = selectedUids.contains(contact.contactUid)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (checked) selectedUids.remove(contact.contactUid) else selectedUids.add(contact.contactUid)
                },
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { isChecked ->
                  if (isChecked) selectedUids.add(contact.contactUid) else selectedUids.remove(contact.contactUid)
                },
              )
              Text("${contact.contactDisplayName} (@${contact.contactUsername})")
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onCreateGroup(title.trim(), description.trim(), selectedUids.toList())
            onDismiss()
          }
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("confirm_create_group_button"),
      ) {
        Text("Buat Grup")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    },
  )
}

@Composable
fun UserProfileScreen(
  profile: UserProfile?,
  settings: UserSettings,
  isSaving: Boolean,
  onBack: () -> Unit,
  onSaveProfile: (displayName: String, username: String, bio: String, phone: String, avatarData: String) -> Unit,
  onSignOut: () -> Unit,
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
  var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
  var bio by remember(profile) { mutableStateOf(profile?.bio ?: "Ada di WhaTalk") }
  var phone by remember(settings) { mutableStateOf(settings.phoneNumber.ifBlank { "+62" }) }
  var avatarData by remember(profile) { mutableStateOf(profile?.avatarData ?: "") }

  val avatarPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val compressed = MediaAndLocationHelper.compressImageUriToDataUri(context, uri, maxDimension = 180, jpegQuality = 65)
      if (compressed != null) {
        avatarData = compressed
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primary,
      contentColor = Color.White,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        Text("Profil Pengguna WhaTalk", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val avatarBmp = remember(avatarData) {
        MediaAndLocationHelper.decodeDataUriToImageBitmap(avatarData)
      }
      Box(
        modifier = Modifier
          .size(110.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer)
          .clickable {
            avatarPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        if (avatarBmp != null) {
          Image(
            bitmap = avatarBmp,
            contentDescription = "Foto Profil",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Ubah Foto",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("Ketuk foto untuk mengganti gambar profil", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

      Spacer(modifier = Modifier.height(16.dp))
      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it.take(60) },
        label = { Text("Nama Tampilan") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_display_name_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = username,
        onValueChange = { username = it.lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30) },
        label = { Text("Username Unik (@username)") },
        prefix = { Text("@") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_username_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it.take(200) },
        label = { Text("Info / Bio") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_bio_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it.take(24) },
        label = { Text("Nomor Telepon Pribadi (Hanya Dilihat Anda)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Catatan Keamanan: Penggantian nomor telepon terverifikasi SMS memerlukan verifikasi ulang Firebase Phone Auth.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = {
          onSaveProfile(displayName, username, bio, phone, avatarData)
        },
        enabled = !isSaving && displayName.isNotBlank() && username.length >= 3,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("save_profile_changes_button"),
      ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Simpan Perubahan Profil")
      }

      Spacer(modifier = Modifier.height(16.dp))
      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("profile_logout_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Keluar (Logout)")
      }
    }
  }
}

@Composable
fun SettingsScreen(
  profile: UserProfile?,
  settings: UserSettings,
  blockedUsers: List<BlockedUser>,
  onBack: () -> Unit,
  onUpdateSettings: (UserSettings) -> Unit,
  onUnblockUser: (String) -> Unit,
  onOpenProfile: () -> Unit,
  onSignOut: () -> Unit,
) {
  BackHandler { onBack() }

  var selectedSection by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primary,
      contentColor = Color.White,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            if (selectedSection != null) selectedSection = null else onBack()
          },
          modifier = Modifier.testTag("settings_back_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        Text(
          text = selectedSection ?: "Pengaturan WhaTalk",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
        )
      }
    }

    if (selectedSection == null) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenProfile() }
              .testTag("settings_profile_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val bmp = remember(profile?.avatarData) {
                MediaAndLocationHelper.decodeDataUriToImageBitmap(profile?.avatarData ?: "")
              }
              Box(
                modifier = Modifier
                  .size(60.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
              ) {
                if (bmp != null) {
                  Image(
                    bitmap = bmp,
                    contentDescription = "Foto Profil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = profile?.displayName?.ifBlank { "Pengguna WhaTalk" } ?: "Pengguna WhaTalk",
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                )
                Text(
                  text = "@${profile?.username ?: "user"} • ${profile?.bio ?: "Ada di WhaTalk"}",
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        val menuItems = listOf(
          Triple("Akun", "Informasi UID, sesi keamanan, nomor telepon, dan keluar", Icons.Default.AccountCircle),
          Triple("Privasi", "Foto profil, info, terakhir dilihat, status, dan blokir", Icons.Default.Lock),
          Triple("Chat", "Tema, wallpaper, ukuran teks, dan pengelolaan media", Icons.Default.Chat),
          Triple("Notifikasi", "Notifikasi pesan pribadi, grup, dan panggilan", Icons.Default.Notifications),
          Triple("Penyimpanan dan Data", "Optimasi RAM 1 GB, kompresi foto, dan penggunaan jaringan", Icons.Default.Storage),
          Triple("Bahasa", "Bahasa Indonesia (Default)", Icons.Default.Language),
          Triple("Tema", "Mode terang, gelap, atau mengikuti perangkat (${settings.themeMode})", Icons.Default.DarkMode),
          Triple("Bantuan & Konfigurasi Firebase", "Panduan mengaktifkan OTP SMS, Storage, FCM, dan TURN", Icons.AutoMirrored.Filled.HelpOutline),
          Triple("Tentang Aplikasi", "WhaTalk v1.0 • Arsitektur Real-Time Cloud Firestore", Icons.Default.Info),
        )

        items(menuItems) { (title, subtitle, icon) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedSection = title }
              .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
              Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        when (selectedSection) {
          "Akun" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Informasi Akun Terautentikasi", fontWeight = FontWeight.Bold)
                Text("UID Firebase: ${settings.userId}", fontSize = 12.sp)
                Text("Email Terhubung: ${settings.email.ifBlank { "Akun Google Terverifikasi" }}", fontSize = 13.sp)
                Text("Nomor Telepon Pribadi: ${settings.phoneNumber.ifBlank { "Belum diatur" }}", fontSize = 13.sp)
                Text("Status Sesi: Aktif & Terlindungi Google Credential Manager", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
              }
            }
            Button(
              onClick = onSignOut,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Logout dari Sesi Perangkat Ini")
            }
          }

          "Privasi" -> {
            PrivacySelectorRow(
              title = "Siapa yang dapat melihat Foto Profil",
              current = settings.privacyPhoto,
              options = listOf("EVERYONE" to "Semua Orang", "CONTACTS" to "Kontak Saya", "NOBODY" to "Tidak Ada"),
              onSelect = { onUpdateSettings(settings.copy(privacyPhoto = it)) },
            )
            PrivacySelectorRow(
              title = "Siapa yang dapat melihat Info / Bio",
              current = settings.privacyBio,
              options = listOf("EVERYONE" to "Semua Orang", "CONTACTS" to "Kontak Saya", "NOBODY" to "Tidak Ada"),
              onSelect = { onUpdateSettings(settings.copy(privacyBio = it)) },
            )
            PrivacySelectorRow(
              title = "Siapa yang dapat melihat Terakhir Dilihat",
              current = settings.privacyLastSeen,
              options = listOf("EVERYONE" to "Semua Orang", "CONTACTS" to "Kontak Saya", "NOBODY" to "Tidak Ada"),
              onSelect = { onUpdateSettings(settings.copy(privacyLastSeen = it)) },
            )
            PrivacySelectorRow(
              title = "Privasi Status 24 Jam",
              current = settings.privacyStatus,
              options = listOf("CONTACTS" to "Kontak Saya", "PUBLIC" to "Publik", "PRIVATE" to "Hanya Saya"),
              onSelect = { onUpdateSettings(settings.copy(privacyStatus = it)) },
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Laporan Dibaca (Centang Biru)", fontWeight = FontWeight.SemiBold)
                Text("Jika dimatikan, Anda tidak akan mengirim tanda baca centang biru.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Switch(
                checked = settings.readReceipts,
                onCheckedChange = { onUpdateSettings(settings.copy(readReceipts = it)) },
              )
            }

            HorizontalDivider()
            Text("Daftar Kontak Diblokir (${blockedUsers.size}):", fontWeight = FontWeight.Bold)
            if (blockedUsers.isEmpty()) {
              Text("Tidak ada kontak yang diblokir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              blockedUsers.forEach { blk ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text("${blk.blockedDisplayName} (@${blk.blockedUsername})")
                  TextButton(onClick = { onUnblockUser(blk.id) }) {
                    Text("Buka Blokir")
                  }
                }
              }
            }
          }

          "Chat", "Tema" -> {
            PrivacySelectorRow(
              title = "Tema Aplikasi",
              current = settings.themeMode,
              options = listOf("SYSTEM" to "Ikuti Perangkat", "LIGHT" to "Mode Terang", "DARK" to "Mode Gelap"),
              onSelect = { onUpdateSettings(settings.copy(themeMode = it)) },
            )
            PrivacySelectorRow(
              title = "Ukuran Teks Pesan",
              current = settings.fontScale,
              options = listOf("SMALL" to "Kecil", "MEDIUM" to "Sedang", "LARGE" to "Besar"),
              onSelect = { onUpdateSettings(settings.copy(fontScale = it)) },
            )
            PrivacySelectorRow(
              title = "Warna Wallpaper Chat (Mode Terang)",
              current = settings.wallpaperHex,
              options = listOf(
                "#ECE5DD" to "Klasik Krem",
                "#DCF8C6" to "Hijau Lembut",
                "#E3F2FD" to "Biru Langit",
                "#FFF3E0" to "Hangat Senja",
              ),
              onSelect = { onUpdateSettings(settings.copy(wallpaperHex = it)) },
            )
          }

          "Notifikasi" -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Notifikasi Pesan & Panggilan Masuk", fontWeight = FontWeight.SemiBold)
                Text("Tampilkan peringatan real-time untuk pesan baru, balasan, dan panggilan WebRTC.", fontSize = 12.sp)
              }
              Switch(
                checked = settings.notificationsEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(notificationsEnabled = it)) },
              )
            }
          }

          "Penyimpanan dan Data" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Optimasi Perangkat RAM 1 GB Aktif", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• Dekode gambar menggunakan profil RGB_565 (hemat 50% memori dibandingkan ARGB_8888).", fontSize = 13.sp)
                Text("• Kompresi otomatis foto profil & gambar chat sebelum diunggah ke cloud.", fontSize = 13.sp)
                Text("• Cache LRU memori dibatasi maksimal 30 thumbnail agar tidak terjadi OutOfMemoryError.", fontSize = 13.sp)
                Text("• Paginasi pesan aktif (memuat 50 pesan terbaru terlebih dahulu).", fontSize = 13.sp)
              }
            }
          }

          "Bahasa" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Bahasa Antarmuka: Bahasa Indonesia (ID)", fontWeight = FontWeight.Bold)
                Text("Seluruh menu, pesan status, dan panduan keamanan menggunakan Bahasa Indonesia.", fontSize = 12.sp)
              }
            }
          }

          "Bantuan & Konfigurasi Firebase" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status Layanan Backend WhaTalk", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("✅ AKTIF & TERUJI: Google Authentication (Credential Manager), Cloud Firestore Enterprise Real-Time Database, Firestore Security Rules Zero-Trust, Chat Pribadi & Grup Real-Time, Status 24 Jam, Komunitas, dan Signaling WebRTC.", fontSize = 12.sp)
                HorizontalDivider()
                Text("⚙️ Konfigurasi Tambahan di Firebase Console (Project ID: gen-lang-client-0822526707):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("1. Phone Auth (OTP SMS): Buka Firebase Console > Authentication > Sign-in method > aktifkan Phone. Pastikan proyek menggunakan paket Blaze dan sidik jari SHA-1/SHA-256 terdaftar.", fontSize = 12.sp)
                Text("2. Email/Password Auth: Buka Authentication > Sign-in method > aktifkan Email/Password.", fontSize = 12.sp)
                Text("3. Firebase Storage: Buka Storage > Get Started, lalu terapkan file storage.rules yang tersedia di root proyek.", fontSize = 12.sp)
                Text("4. TURN Server WebRTC: STUN publik Google (stun:stun.l.google.com:19302) sudah aktif. Untuk jaringan NAT simetris ketat, tambahkan kredensial server TURN (Coturn / Metered) pada konfigurasi ICE.", fontSize = 12.sp)
              }
            }
          }

          "Tentang Aplikasi" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WhaTalk Messenger untuk Android", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Versi 1.0.0 • Ringan & Cepat (Dioptimalkan untuk RAM 1 GB)", fontSize = 13.sp)
                Text("Pesan pribadi. Terhubung setiap saat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrivacySelectorRow(
  title: String,
  current: String,
  options: List<Pair<String, String>>,
  onSelect: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      options.forEach { (key, label) ->
        FilterChip(
          selected = current == key,
          onClick = { onSelect(key) },
          label = { Text(label, fontSize = 11.sp) },
        )
      }
    }
  }
}
