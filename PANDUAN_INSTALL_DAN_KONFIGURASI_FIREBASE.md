# Panduan Instalasi, Konfigurasi Firebase & Deployment WhaTalk

Dokumen ini menjelaskan arsitektur proyek **WhaTalk**, panduan konfigurasi layanan Firebase tambahan, cara menjalankan pengujian keamanan, serta daftar status fitur.

---

## 1. Struktur Proyek & Arsitektur
- **`/app/src/main/java/com/example/MainActivity.kt`**: Titik masuk aplikasi (`enableEdgeToEdge`), pemeriksa sesi login otomatis, gerbang autentikasi (`authStateFlow`), navigasi 4 tab utama (**Chat**, **Status**, **Komunitas**, **Panggilan**), kamera cepat, dan pencarian universal.
- **`/app/src/main/java/com/example/auth/WhaTalkAuthManager.kt`**: Halaman Pembuka (Splash), Login & Pendaftaran (Google Credential Manager, Nomor Telepon OTP, Email), serta halaman pembuatan profil awal (**Lengkapi Profil WhaTalk** dengan validasi `@username` unik).
- **`/app/src/main/java/com/example/data/WhaTalkModels.kt`**: Model data Kotlin lengkap (`UserProfile`, `UserSettings`, `Contact`, `Conversation`, `ChatMessage`, `Community`, `StatusPost`, `CallLog`, `BlockedUser`, `AbuseReport`) dengan `@DocumentId` dan `@ServerTimestamp`.
- **`/app/src/main/java/com/example/data/WhaTalkRepository.kt`**: Lapisan data Cloud Firestore real-time (`snapshots()`) yang terhubung ke database bernama (`R.string.firestore_database_id`) dan dilengkapi penanganan error terstruktur (`handleFirestoreError`).
- **`/app/src/main/java/com/example/ui/ChatDetailScreen.kt`**: Layar percakapan pribadi & grup secara real-time (balas pesan, teruskan, edit, hapus untuk semua, reaksi emoji, indikator mengetik, tanda baca centang biru, lampiran foto terkompresi, dokumen, rekaman suara, dan lokasi GPS).
- **`/app/src/main/java/com/example/ui/TabsAndDialogs.kt`**: Implementasi tab Chat, Status 24 Jam, Komunitas, Riwayat Panggilan, dan layar panggilan WebRTC (`ActiveCallOverlay`).
- **`/app/src/main/java/com/example/ui/ProfileAndSettingsScreen.kt`**: Pencarian pengguna berdasarkan `@username`, manajemen kontak, pembuatan grup, pembaruan profil, dan 9 menu Pengaturan.
- **`/app/src/main/java/com/example/util/MediaAndLocationHelper.kt`**: Optimasi khusus perangkat Android RAM 1 GB (dekode gambar `RGB_565` hemat 50% RAM, kompresi foto otomatis, cache LRU terbatas 30 gambar, deteksi jaringan offline real-time, dan pembacaan koordinat GPS).
- **`/firestore.rules`** & **`/storage.rules`**: Aturan keamanan Zero-Trust untuk Cloud Firestore dan Firebase Storage.

---

## 2. Daftar Fitur yang Sudah Diuji & Berfungsi Penuh Saat Ini
1. **Autentikasi Google Resmi (Jetpack Credential Manager)**: Login interaktif & auto-login aman menggunakan `default_web_client_id` yang telah diprovisikan pada proyek Firebase `gen-lang-client-0822526707`.
2. **Pembuatan & Pengelolaan Profil Unik**: Pengecekan ketersediaan `@username` unik ke Firestore, foto profil terkompresi, bio, dan status online.
3. **Chat Pribadi & Grup Real-Time**: Pengiriman pesan teks, emoji, foto terkompresi, pesan suara, dokumen, lokasi GPS, balas pesan, teruskan pesan, salin, edit pesan, hapus pesan untuk semua, reaksi emoji, indikator sedang mengetik, dan pemisah tanggal.
4. **Manajemen Daftar Chat**: Pin chat, arsip chat, bisukan notifikasi, filter chip (*Semua, Belum Dibaca, Grup, Diarsipkan*), dan pencarian dalam percakapan maupun universal.
5. **Sistem Kontak, Blokir & Pelaporan**: Pencarian pengguna aman berdasarkan `@username` unik tanpa membocorkan seluruh daftar pengguna atau PII (`userSettings` terisolasi dari `users`).
6. **Status 24 Jam**: Pembuatan status teks berwarna & status foto, daftar pemirsa (*Dilihat oleh X pengguna*), balasan status langsung ke chat, dan kedaluwarsa otomatis setelah 24 jam.
7. **Grup & Komunitas**: Pembuatan grup & komunitas, kode undangan aman (`WTG-...` dan `WTC-...`), penambahan grup ke komunitas, dan pengelolaan anggota/admin.
8. **Signaling Panggilan Suara & Video WebRTC**: Pertukaran status panggilan (`RINGING`, `CONNECTED`, `ENDED`, `REJECTED`, `MISSED`) dan SDP/ICE (`stun:stun.l.google.com:19302`) secara real-time melalui koleksi `calls` di Firestore beserta riwayat panggilan.
9. **Firestore Security Rules Zero-Trust**: Telah diuji menggunakan emulator lokal (`node --test firestore.test.js` dan Robolectric `WhaTalkRepositoryRuleTest`) dan di-deploy ke backend.

---

## 3. Panduan Mengaktifkan Fitur yang Membutuhkan Konfigurasi Eksternal di Firebase Console

Proyek Firebase aplikasi ini adalah **`gen-lang-client-0822526707`** dengan database Firestore Enterprise **`ai-studio-android-whatalk-97cf5363-aa71-4736-8c8e-85f9a70ac28b`**.

### A. Mengaktifkan Phone Authentication (OTP SMS Sungguhan)
1. Buka [Firebase Console](https://console.firebase.google.com/project/gen-lang-client-0822526707/authentication/providers).
2. Tingkatkan paket proyek dari Spark ke **Blaze (Pay as you go)** karena pengiriman SMS OTP operator seluler dikenai biaya sesuai penggunaan kuota harian.
3. Masuk ke menu **Authentication > Sign-in method**, pilih **Phone**, lalu klik **Enable** dan **Save**.
4. Pada tab **Authentication > Settings > SMS region policy**, pastikan kode negara **Indonesia (+62)** dan negara tujuan lainnya diizinkan (*Allow*).
5. Pastikan sidik jari sertifikat SHA-1 (`CD:A7:F9:72:81:28:18:DD:12:9B:FB:FB:A0:EA:A3:5F:41:51:D9:C1`) dan SHA-256 (`1E:63:1C:7F:59:53:49:04:C6:11:AD:D8:C1:E6:0B:CE:E9:FB:B7:B3:E9:95:90:8F:0B:89:85:BB:81:7C:36:C7`) tetap terdaftar di **Project Settings > Your apps** untuk verifikasi Play Integrity / SafetyNet.

### B. Mengaktifkan Email/Password Authentication
1. Buka **Firebase Console > Authentication > Sign-in method**.
2. Klik **Add new provider > Email/Password**, aktifkan toggle **Enable**, lalu klik **Save**.

### C. Mengaktifkan Firebase Storage untuk File Video/Dokumen Berukuran Besar
Saat ini foto profil, gambar chat, dan gambar status dikompresi secara otomatis menjadi thumbnail ringan agar langsung tersimpan secara real-time di Firestore dan hemat RAM 1 GB. Untuk menyimpan file video/dokumen mentah berukuran hingga 15 MB:
1. Buka **Firebase Console > Storage > Get started**.
2. Pilih lokasi bucket dan salin isi file `/storage.rules` pada repositori ini ke tab **Storage > Rules**, lalu klik **Publish**.

### D. Mengaktifkan Server TURN untuk Panggilan WebRTC Lintas Jaringan NAT Simetris
Signaling WebRTC (SDP Offer/Answer) dan STUN publik Google (`stun:stun.l.google.com:19302`) sudah dikonfigurasi. Untuk koneksi media P2P pada jaringan seluler dengan firewall/NAT simetris ketat:
1. Siapkan server **Coturn** atau layanan TURN terkelola (seperti Metered TURN / Twilio Network Traversal).
2. Tambahkan kredensial URL `turn:your-turn-server.com:3478` beserta `username` dan `credential` ke daftar `iceServers` WebRTC.

### E. Mengaktifkan Notifikasi Push Latar Belakang (Firebase Cloud Messaging)
1. Buka **Firebase Console > Project Settings > Cloud Messaging** dan pastikan **Firebase Cloud Messaging API (V1)** berstatus *Enabled*.
2. Deploy Cloud Function (Node.js `firebase-admin`) yang memantau pemicu `onDocumentCreated("messages/{messageId}")` dan `onDocumentCreated("calls/{callId}")` untuk mengirim payload FCM ke token perangkat penerima. Jangan pernah menaruh *Service Account Private Key* di kode aplikasi Android (frontend).

---

## 4. Cara Menjalankan Pengujian & Build Proyek
- **Menjalankan Uji Aturan Keamanan Firestore (JS)**:
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- **Menjalankan Uji Integrasi Repositori Robolectric (Kotlin)**:
  ```bash
  gradle :app:testDebugUnitTest --tests '*Rule*'
  ```
- **Mengompilasi APK Debug**:
  ```bash
  gradle assembleDebug
  ```
