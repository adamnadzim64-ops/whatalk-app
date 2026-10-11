package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.util.MediaAndLocationHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CountryDialCode(val name: String, val code: String)

val SUPPORTED_COUNTRIES = listOf(
  CountryDialCode("Indonesia", "+62"),
  CountryDialCode("Malaysia", "+60"),
  CountryDialCode("Singapura", "+65"),
  CountryDialCode("Arab Saudi", "+966"),
  CountryDialCode("Jepang", "+81"),
  CountryDialCode("Amerika Serikat", "+1"),
)

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Konfigurasi Google Sign-In belum lengkap: default_web_client_id tidak ditemukan.")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Tipe kredensial Google tidak dikenali.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Gagal masuk dengan Google.")
    }
  }
}

fun signOutWhaTalk(
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

@Composable
fun WhaTalkSplashScreen() {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = Modifier.height(24.dp))
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
          painter = painterResource(id = R.drawable.img_launcher_icon_1791687432895),
          contentDescription = "Logo WhaTalk",
          modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(24.dp)),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "WhaTalk",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Pesan pribadi. Terhubung setiap saat.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
          modifier = Modifier.size(28.dp),
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = 2.5.dp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Memeriksa sesi login aman...",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun WhaTalkAuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var selectedTab by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember { mutableStateOf<String?>(null) }

  // Phone Tab State
  var selectedCountry by remember { mutableStateOf(SUPPORTED_COUNTRIES.first()) }
  var countryMenuExpanded by remember { mutableStateOf(false) }
  var phoneInput by remember { mutableStateOf("") }
  var otpCooldownSeconds by remember { mutableIntStateOf(0) }

  // Email Tab State
  var isRegisterMode by remember { mutableStateOf(false) }
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var confirmPasswordInput by remember { mutableStateOf("") }

  LaunchedEffect(otpCooldownSeconds) {
    if (otpCooldownSeconds > 0) {
      delay(1000L)
      otpCooldownSeconds -= 1
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_whatalk_hero_1791687490573),
          contentDescription = "Ilustrasi WhaTalk",
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp)),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Selamat Datang di WhaTalk",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Pesan pribadi. Terhubung setiap saat.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
          selectedTabIndex = selectedTab,
          modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = {
              selectedTab = 0
              errorMessage = null
              infoMessage = null
            },
            text = { Text("Google (Aktif)", fontSize = 13.sp) },
            modifier = Modifier.testTag("auth_tab_google"),
          )
          Tab(
            selected = selectedTab == 1,
            onClick = {
              selectedTab = 1
              errorMessage = null
              infoMessage = null
            },
            text = { Text("No. Telepon", fontSize = 13.sp) },
            modifier = Modifier.testTag("auth_tab_phone"),
          )
          Tab(
            selected = selectedTab == 2,
            onClick = {
              selectedTab = 2
              errorMessage = null
              infoMessage = null
            },
            text = { Text("Email", fontSize = 13.sp) },
            modifier = Modifier.testTag("auth_tab_email"),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage ?: "",
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        if (infoMessage != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = infoMessage ?: "",
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        when (selectedTab) {
          0 -> {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = "Keamanan Google",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Masuk & Daftar Instan dengan Google",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Penyedia autentikasi Google Sign-In telah dikonfigurasi penuh pada proyek Firebase WhaTalk ini. Setelah masuk, Anda dapat memilih @username unik dan melengkapi profil WhaTalk.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                  onClick = {
                    isLoading = true
                    errorMessage = null
                    onGoogleSignInClicked(
                      context = context,
                      credentialManager = credentialManager,
                      onAuthSuccess = {
                        isLoading = false
                        onAuthSuccess()
                      },
                      onAuthError = { msg ->
                        isLoading = false
                        errorMessage = msg
                      },
                      scope = scope,
                      onAuthCancelled = {
                        isLoading = false
                        errorMessage = "Login Google dibatalkan. Silakan pilih akun Google untuk melanjutkan."
                      },
                    )
                  },
                  enabled = !isLoading,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_sign_in_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                  if (isLoading) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(22.dp),
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.dp,
                    )
                  } else {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lanjutkan dengan Google", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          1 -> {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Verifikasi Nomor Telepon (OTP SMS)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Masukkan kode negara dan nomor telepon aktif Anda.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box {
                  OutlinedButton(
                    onClick = { countryMenuExpanded = true },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("country_code_selector"),
                  ) {
                    Text("${selectedCountry.name} (${selectedCountry.code})")
                  }
                  DropdownMenu(
                    expanded = countryMenuExpanded,
                    onDismissRequest = { countryMenuExpanded = false },
                  ) {
                    SUPPORTED_COUNTRIES.forEach { country ->
                      DropdownMenuItem(
                        text = { Text("${country.name} (${country.code})") },
                        onClick = {
                          selectedCountry = country
                          countryMenuExpanded = false
                        },
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                  value = phoneInput,
                  onValueChange = { phoneInput = it.filter { ch -> ch.isDigit() }.take(14) },
                  label = { Text("Nomor Telepon (contoh: 81234567890)") },
                  leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_number_input"),
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = {
                    val clean = phoneInput.trim().removePrefix("0")
                    if (clean.length < 8) {
                      errorMessage = "Format nomor telepon tidak valid. Masukkan minimal 8 digit."
                      infoMessage = null
                    } else {
                      errorMessage = null
                      otpCooldownSeconds = 60
                      infoMessage =
                        "Permintaan OTP ke ${selectedCountry.code}$clean membutuhkan aktivasi Firebase Phone Authentication (paket Blaze & Play Integrity) di Firebase Console (Project: gen-lang-client-0822526707). WhaTalk tidak membuat OTP palsu di frontend. Gunakan tab 'Google (Aktif)' untuk masuk sekarang, lalu kelola nomor telepon di Profil."
                    }
                  },
                  enabled = otpCooldownSeconds == 0,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("request_otp_button"),
                ) {
                  Text(
                    if (otpCooldownSeconds > 0) {
                      "Kirim Ulang Kode dalam ${otpCooldownSeconds}d"
                    } else {
                      "Kirim Kode OTP SMS"
                    }
                  )
                }
              }
            }
          }

          2 -> {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = if (isRegisterMode) "Pendaftaran dengan Email" else "Login dengan Email",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                  value = emailInput,
                  onValueChange = { emailInput = it.trim() },
                  label = { Text("Alamat Email") },
                  leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                  value = passwordInput,
                  onValueChange = { passwordInput = it },
                  label = { Text("Kata Sandi") },
                  leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                  visualTransformation = PasswordVisualTransformation(),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input"),
                )
                if (isRegisterMode) {
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = { confirmPasswordInput = it },
                    label = { Text("Konfirmasi Kata Sandi") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = {
                    if (!emailInput.contains("@") || passwordInput.length < 6) {
                      errorMessage = "Masukkan email yang valid dan kata sandi minimal 6 karakter."
                      infoMessage = null
                    } else if (isRegisterMode && passwordInput != confirmPasswordInput) {
                      errorMessage = "Konfirmasi kata sandi tidak cocok."
                      infoMessage = null
                    } else {
                      errorMessage = null
                      infoMessage =
                        "Pada lingkungan Cloud AI Studio ini, penyedia autentikasi yang diprovisikan secara otomatis adalah Google Sign-In (Credential Manager). Untuk mengaktifkan Email/Password Auth, buka Firebase Console > Authentication > Sign-in method. Silakan pilih tab 'Google (Aktif)' untuk langsung masuk menggunakan akun Google Anda."
                    }
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("email_submit_button"),
                ) {
                  Text(if (isRegisterMode) "Daftar dengan Email" else "Masuk dengan Email")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  OutlinedButton(
                    onClick = { isRegisterMode = !isRegisterMode },
                  ) {
                    Text(if (isRegisterMode) "Sudah punya akun? Masuk" else "Daftar Akun Baru", fontSize = 12.sp)
                  }
                  OutlinedButton(
                    onClick = {
                      infoMessage = "Tautan pengaturan ulang kata sandi dikelola melalui akun Google / Firebase Auth Anda."
                    },
                  ) {
                    Text("Lupa Sandi?", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Catatan Keamanan: WhaTalk menggunakan Firebase Authentication & Cloud Firestore asli dengan aturan keamanan Zero-Trust. Nomor telepon tidak pernah ditampilkan ke publik.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ProfileSetupScreen(
  initialDisplayName: String,
  initialEmail: String,
  isSaving: Boolean,
  errorMessage: String?,
  onCompleteProfile: (displayName: String, username: String, bio: String, phone: String, avatarData: String) -> Unit,
) {
  val context = LocalContext.current
  var displayName by remember { mutableStateOf(initialDisplayName.ifBlank { "Pengguna WhaTalk" }) }
  var username by remember {
    val defaultUser = initialEmail.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "")
    mutableStateOf(if (defaultUser.length >= 3) defaultUser else "user_${(100..999).random()}")
  }
  var bio by remember { mutableStateOf("Ada di WhaTalk") }
  var phone by remember { mutableStateOf("+62") }
  var avatarData by remember { mutableStateOf("") }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val compressed = MediaAndLocationHelper.compressImageUriToDataUri(context, uri, maxDimension = 160, jpegQuality = 65)
      if (compressed != null) {
        avatarData = compressed
      }
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "Lengkapi Profil WhaTalk",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Tentukan nama tampilan, @username unik, dan foto profil Anda.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(20.dp))

      val decodedAvatar = remember(avatarData) {
        MediaAndLocationHelper.decodeDataUriToImageBitmap(avatarData)
      }
      Box(
        modifier = Modifier
          .size(96.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer)
          .clickable {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }
          .testTag("setup_avatar_picker"),
        contentAlignment = Alignment.Center,
      ) {
        if (decodedAvatar != null) {
          Image(
            bitmap = decodedAvatar,
            contentDescription = "Foto Profil",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Pilih Foto Profil",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Ketuk lingkaran untuk memilih foto profil (opsional)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(16.dp))
      if (errorMessage != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it.take(60) },
        label = { Text("Nama Tampilan *") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setup_display_name_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = username,
        onValueChange = { username = it.lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30) },
        label = { Text("Username Unik (tanpa @, min. 3 karakter) *") },
        prefix = { Text("@") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setup_username_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it.take(200) },
        label = { Text("Info / Bio (Opsional)") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setup_bio_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it.take(24) },
        label = { Text("Nomor Telepon Pribadi (Disimpan Aman & Privat)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setup_phone_input"),
      )

      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = {
          onCompleteProfile(displayName, username, bio, phone, avatarData)
        },
        enabled = !isSaving && displayName.isNotBlank() && username.length >= 3,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_initial_profile_button"),
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(Icons.Default.CheckCircle, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Simpan Profil & Masuk ke WhaTalk", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
