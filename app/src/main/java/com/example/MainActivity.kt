package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.ProfileSetupScreen
import com.example.auth.WhaTalkAuthScreen
import com.example.auth.WhaTalkSplashScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutWhaTalk
import com.example.ui.ActiveCallOverlay
import com.example.ui.CallsTab
import com.example.ui.ChatDetailScreen
import com.example.ui.ChatListTab
import com.example.ui.CommunitiesTab
import com.example.ui.ContactsAndUserSearchDialog
import com.example.ui.CreateGroupDialog
import com.example.ui.SettingsScreen
import com.example.ui.StatusTab
import com.example.ui.UserProfileScreen
import com.example.ui.WhaTalkViewModel
import com.example.ui.theme.WhaTalkTheme
import com.example.util.MediaAndLocationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WhaTalkRootApp()
    }
  }
}

@Composable
fun WhaTalkRootApp(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var showSplash by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { showSplash = false },
        onUnauthenticated = { showSplash = false },
        scope = scope,
      )
    } else {
      delay(450L)
      showSplash = false
    }
  }

  val user = currentUser
  if (showSplash) {
    WhaTalkTheme {
      WhaTalkSplashScreen()
    }
  } else if (user == null) {
    WhaTalkTheme {
      WhaTalkAuthScreen(
        onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ }
      )
    }
  } else {
    WhaTalkAuthenticatedApp(
      user = user,
      onSignOut = {
        signOutWhaTalk(
          credentialManager = credentialManager,
          onSignOutComplete = {},
          scope = scope,
        )
      },
    )
  }
}

@Composable
fun WhaTalkAuthenticatedApp(
  user: FirebaseUser,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val vm: WhaTalkViewModel = viewModel(
    key = user.uid,
    factory = WhaTalkViewModel.factory(user.uid),
  )

  val profileLoaded by vm.profileLoaded.collectAsStateWithLifecycle()
  val userProfile by vm.userProfile.collectAsStateWithLifecycle()
  val userSettings by vm.userSettings.collectAsStateWithLifecycle()
  val contacts by vm.contacts.collectAsStateWithLifecycle()
  val conversations by vm.conversations.collectAsStateWithLifecycle()
  val statuses by vm.statuses.collectAsStateWithLifecycle()
  val communities by vm.communities.collectAsStateWithLifecycle()
  val calls by vm.calls.collectAsStateWithLifecycle()
  val blockedUsers by vm.blockedUsers.collectAsStateWithLifecycle()
  val activeConvId by vm.activeConversationId.collectAsStateWithLifecycle()
  val activeMessages by vm.activeMessages.collectAsStateWithLifecycle()
  val searchedUsers by vm.searchedUsers.collectAsStateWithLifecycle()
  val isSearchingUsers by vm.isSearchingUsers.collectAsStateWithLifecycle()
  val activeCallId by vm.activeCallId.collectAsStateWithLifecycle()
  val isSaving by vm.isSaving.collectAsStateWithLifecycle()
  val bannerMsg by vm.statusBannerMessage.collectAsStateWithLifecycle()

  val isOnline by MediaAndLocationHelper.observeNetworkConnectivity(context)
    .collectAsStateWithLifecycle(initialValue = true)

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(bannerMsg) {
    val msg = bannerMsg
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      vm.showMessage(null)
    }
  }

  WhaTalkTheme(themeMode = userSettings.themeMode) {
    if (!profileLoaded) {
      WhaTalkSplashScreen()
      return@WhaTalkTheme
    }

    if (userProfile == null) {
      ProfileSetupScreen(
        initialDisplayName = user.displayName ?: "",
        initialEmail = user.email ?: "",
        isSaving = isSaving,
        errorMessage = bannerMsg,
        onCompleteProfile = { displayName, username, bio, phone, avatarData ->
          vm.completeOrUpdateProfile(
            displayName = displayName,
            username = username,
            bio = bio,
            phone = phone,
            avatarData = avatarData,
            email = user.email ?: "",
          )
        },
      )
      return@WhaTalkTheme
    }

    // Check if there is an active or incoming ringing call
    val activeOrIncomingCall = remember(calls, activeCallId, user.uid) {
      calls.firstOrNull { it.id == activeCallId }
        ?: calls.firstOrNull { it.receiverId == user.uid && it.callState == "RINGING" }
    }

    if (activeOrIncomingCall != null && (activeCallId != null || activeOrIncomingCall.callState == "RINGING")) {
      ActiveCallOverlay(
        callLog = activeOrIncomingCall,
        currentUserId = user.uid,
        onAcceptCall = { vm.acceptIncomingCall(activeOrIncomingCall.id) },
        onRejectOrEndCall = { state, dur -> vm.endOrRejectCall(activeOrIncomingCall.id, state, dur) },
        onDismiss = { vm.dismissCallScreen() },
      )
      return@WhaTalkTheme
    }

    var currentSubScreen by remember { mutableStateOf<String?>(null) } // "PROFILE", "SETTINGS"
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Status, 2: Komunitas, 3: Panggilan
    var showUniversalSearch by remember { mutableStateOf(false) }
    var universalSearchQuery by remember { mutableStateOf("") }
    var showTopMenu by remember { mutableStateOf(false) }
    var showContactsDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val quickCameraLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        val compressed = MediaAndLocationHelper.compressImageUriToDataUri(context, uri, maxDimension = 260, jpegQuality = 68)
        if (compressed != null) {
          vm.postStatus("IMAGE", "Foto dari Kamera Cepat WhaTalk", compressed)
          selectedTab = 1
        }
      }
    }

    // Active Chat Detail Screen
    val activeConversation = conversations.firstOrNull { it.id == activeConvId }
    if (activeConversation != null) {
      LaunchedEffect(activeConversation.id, activeMessages.size) {
        vm.markIncomingMessagesRead(activeConversation.id, activeMessages)
      }

      ChatDetailScreen(
        conversation = activeConversation,
        messages = activeMessages,
        allConversations = conversations,
        currentUserId = user.uid,
        fontScaleOption = userSettings.fontScale,
        wallpaperHex = userSettings.wallpaperHex,
        onBack = { vm.openConversation(null) },
        onSendMessage = { text, type, mediaData, caption, replyTo, isForwarded ->
          vm.sendChatMessage(
            conversation = activeConversation,
            text = text,
            messageType = type,
            mediaData = mediaData,
            mediaCaption = caption,
            replyToMessage = replyTo,
            isForwarded = isForwarded,
          )
        },
        onForwardMessage = { targetConv, msg ->
          vm.sendChatMessage(
            conversation = targetConv,
            text = msg.text,
            messageType = msg.messageType,
            mediaData = msg.mediaData,
            mediaCaption = msg.mediaCaption,
            replyToMessage = null,
            isForwarded = true,
          )
        },
        onEditMessage = { msgId, newText -> vm.editOwnMessage(msgId, newText) },
        onDeleteMessage = { msgId -> vm.deleteOwnMessage(msgId) },
        onReactMessage = { msgId, emoji -> vm.reactToMessage(msgId, emoji) },
        onTypingChanged = { isTyping -> vm.setTyping(activeConversation.id, isTyping) },
        onLoadOlderMessages = { vm.loadOlderMessages() },
        onStartVoiceCall = {
          val otherUid = activeConversation.memberUids.firstOrNull { it != user.uid } ?: user.uid
          vm.initiateCall(otherUid, activeConversation.title, "VOICE")
        },
        onStartVideoCall = {
          val otherUid = activeConversation.memberUids.firstOrNull { it != user.uid } ?: user.uid
          vm.initiateCall(otherUid, activeConversation.title, "VIDEO")
        },
        onUpdateGroupInfo = { title, desc, members, admins ->
          vm.updateGroupDetails(activeConversation.id, title, desc, members, admins)
        },
        onLeaveOrDeleteConversation = {
          vm.deleteOrLeaveConversation(activeConversation.id)
        },
        onBlockUser = { uid, uname, dname -> vm.blockContact(uid, uname, dname) },
        onReportUser = { uid, uname, reason, details -> vm.submitAbuseReport(uid, uname, reason, details) },
        onShowBanner = { msg -> vm.showMessage(msg) },
      )
      return@WhaTalkTheme
    }

    if (currentSubScreen == "PROFILE") {
      UserProfileScreen(
        profile = userProfile,
        settings = userSettings,
        isSaving = isSaving,
        onBack = { currentSubScreen = null },
        onSaveProfile = { displayName, username, bio, phone, avatarData ->
          vm.completeOrUpdateProfile(displayName, username, bio, phone, avatarData, user.email ?: "")
          currentSubScreen = null
        },
        onSignOut = onSignOut,
      )
      return@WhaTalkTheme
    }

    if (currentSubScreen == "SETTINGS") {
      SettingsScreen(
        profile = userProfile,
        settings = userSettings,
        blockedUsers = blockedUsers,
        onBack = { currentSubScreen = null },
        onUpdateSettings = { vm.updateSettings(it) },
        onUnblockUser = { vm.unblockContact(it) },
        onOpenProfile = { currentSubScreen = "PROFILE" },
        onSignOut = onSignOut,
      )
      return@WhaTalkTheme
    }

    // MAIN HOME SCREEN (WhatsApp Android Inspired Layout)
    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          contentColor = Color.White,
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_launcher_icon_1791687432895),
                contentDescription = "Logo WhaTalk",
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "WhaTalk",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
              )

              IconButton(
                onClick = {
                  quickCameraLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                modifier = Modifier.testTag("top_camera_button"),
              ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Kamera Status Cepat", tint = Color.White)
              }

              IconButton(
                onClick = { showUniversalSearch = !showUniversalSearch },
                modifier = Modifier.testTag("top_search_button"),
              ) {
                Icon(Icons.Default.Search, contentDescription = "Pencarian Universal", tint = Color.White)
              }

              IconButton(
                onClick = { currentSubScreen = "SETTINGS" },
                modifier = Modifier.testTag("top_settings_button"),
              ) {
                Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = Color.White)
              }

              Box {
                IconButton(
                  onClick = { showTopMenu = true },
                  modifier = Modifier.testTag("top_more_menu_button"),
                ) {
                  Icon(Icons.Default.MoreVert, contentDescription = "Menu Utama", tint = Color.White)
                }
                DropdownMenu(
                  expanded = showTopMenu,
                  onDismissRequest = { showTopMenu = false },
                ) {
                  DropdownMenuItem(
                    text = { Text("Grup Baru") },
                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    onClick = {
                      showTopMenu = false
                      showCreateGroupDialog = true
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("Cari & Tambah Kontak") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    onClick = {
                      showTopMenu = false
                      showContactsDialog = true
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("Profil Saya (@${userProfile?.username ?: ""})") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = {
                      showTopMenu = false
                      currentSubScreen = "PROFILE"
                    },
                  )
                  DropdownMenuItem(
                    text = { Text("Pengaturan") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                      showTopMenu = false
                      currentSubScreen = "SETTINGS"
                    },
                  )
                }
              }
            }

            // Offline Connectivity Indicator
            AnimatedVisibility(visible = !isOnline) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFFD32F2F))
                  .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Mode Offline • Menunggu koneksi internet...",
                  fontSize = 12.sp,
                  color = Color.White,
                  fontWeight = FontWeight.Medium,
                )
              }
            }

            // Universal Search Input Bar
            AnimatedVisibility(visible = showUniversalSearch) {
              OutlinedTextField(
                value = universalSearchQuery,
                onValueChange = { universalSearchQuery = it },
                placeholder = { Text("Cari chat, kontak, atau grup...") },
                singleLine = true,
                trailingIcon = {
                  if (universalSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { universalSearchQuery = "" }) {
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
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("universal_search_input"),
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (selectedTab == 0) {
          FloatingActionButton(
            onClick = { showContactsDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier.testTag("fab_new_chat"),
          ) {
            Icon(Icons.Default.Chat, contentDescription = "Mulai Chat Baru")
          }
        }
      },
      bottomBar = {
        NavigationBar {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
            label = { Text("Chat") },
            modifier = Modifier.testTag("nav_tab_chat"),
          )
          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(Icons.Default.Update, contentDescription = "Status") },
            label = { Text("Status") },
            modifier = Modifier.testTag("nav_tab_status"),
          )
          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(Icons.Default.Groups, contentDescription = "Komunitas") },
            label = { Text("Komunitas") },
            modifier = Modifier.testTag("nav_tab_communities"),
          )
          NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            icon = { Icon(Icons.Default.Call, contentDescription = "Panggilan") },
            label = { Text("Panggilan") },
            modifier = Modifier.testTag("nav_tab_calls"),
          )
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (selectedTab) {
          0 -> {
            ChatListTab(
              conversations = conversations,
              currentUserId = user.uid,
              searchQuery = universalSearchQuery,
              onOpenConversation = { conv -> vm.openConversation(conv.id) },
              onStartNewChat = { showContactsDialog = true },
              onCreateNewGroup = { showCreateGroupDialog = true },
              onTogglePin = { conv -> vm.toggleConversationPin(conv) },
              onToggleArchive = { conv -> vm.toggleConversationArchive(conv) },
              onToggleMute = { conv -> vm.toggleConversationMute(conv) },
              onDeleteConversation = { conv -> vm.deleteOrLeaveConversation(conv.id) },
            )
          }

          1 -> {
            StatusTab(
              statuses = statuses,
              currentUserId = user.uid,
              onCreateStatus = { type, text, mediaData, bgHex ->
                vm.postStatus(type, text, mediaData, bgHex)
              },
              onMarkStatusViewed = { statusId -> vm.markStatusSeen(statusId) },
              onDeleteStatus = { statusId -> vm.removeStatus(statusId) },
              onReplyToStatus = { statusPost, replyText ->
                vm.addContactAndStartChat(
                  com.example.data.UserProfile(
                    userId = statusPost.authorId,
                    username = statusPost.authorName.lowercase().replace(" ", "_"),
                    displayName = statusPost.authorName,
                    avatarData = statusPost.authorAvatarData,
                  )
                ) { convId ->
                  val targetConv = conversations.firstOrNull { it.id == convId }
                  if (targetConv != null) {
                    vm.sendChatMessage(
                      conversation = targetConv,
                      text = "Membalas Status Anda: \"$replyText\"",
                    )
                  }
                }
              },
            )
          }

          2 -> {
            CommunitiesTab(
              communities = communities,
              groupConversations = conversations.filter { it.isGroup },
              onCreateCommunity = { name, desc, grpIds -> vm.createNewCommunity(name, desc, grpIds) },
              onJoinCommunityByCode = { code -> vm.joinCommunityByCode(code) },
              onJoinGroupByCode = { code -> vm.joinGroupWithInvite(code) },
              onLinkGroupToCommunity = { commId, grpId -> vm.linkGroupToCommunity(commId, grpId) },
              onOpenGroupChat = { grp -> vm.openConversation(grp.id) },
            )
          }

          3 -> {
            CallsTab(
              calls = calls,
              contacts = contacts,
              currentUserId = user.uid,
              onStartCall = { recId, recName, callType ->
                vm.initiateCall(recId, recName, callType)
              },
            )
          }
        }
      }
    }

    if (showContactsDialog) {
      ContactsAndUserSearchDialog(
        contacts = contacts,
        searchedUsers = searchedUsers,
        isSearching = isSearchingUsers,
        onSearchUsername = { q -> vm.searchUsersByUsername(q) },
        onSelectUserToAddAndChat = { profile ->
          showContactsDialog = false
          vm.addContactAndStartChat(profile) {}
        },
        onSelectContactToChat = { contact ->
          showContactsDialog = false
          vm.startDirectChatWithContact(contact) {}
        },
        onBlockContact = { contact ->
          vm.blockContact(contact.contactUid, contact.contactUsername, contact.contactDisplayName)
        },
        onOpenCreateGroup = { showCreateGroupDialog = true },
        onDismiss = { showContactsDialog = false },
      )
    }

    if (showCreateGroupDialog) {
      CreateGroupDialog(
        contacts = contacts,
        onCreateGroup = { title, desc, members ->
          vm.createNewGroup(title, desc, members) { convId ->
            vm.openConversation(convId)
          }
        },
        onDismiss = { showCreateGroupDialog = false },
      )
    }
  }
}
