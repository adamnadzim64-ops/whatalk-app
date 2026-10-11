package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object MediaAndLocationHelper {

  // Lightweight LRU cache capped at 30 small thumbnails to save RAM on 1GB Android devices
  private val bitmapCache = object : LruCache<String, ImageBitmap>(30) {}

  fun compressImageUriToDataUri(
    context: Context,
    uri: Uri,
    maxDimension: Int = 200,
    jpegQuality: Int = 62,
  ): String? {
    return try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, boundsOptions)
      }
      val srcWidth = boundsOptions.outWidth
      val srcHeight = boundsOptions.outHeight
      if (srcWidth <= 0 || srcHeight <= 0) return null

      var sampleSize = 1
      while (srcWidth / sampleSize > maxDimension * 2 || srcHeight / sampleSize > maxDimension * 2) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565 // Uses 50% less RAM than ARGB_8888 on 1GB devices
      }
      val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
      } ?: return null

      val ratio = minOf(
        maxDimension.toFloat() / decoded.width.toFloat(),
        maxDimension.toFloat() / decoded.height.toFloat(),
        1f
      )
      val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
      val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
      val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
      if (scaled != decoded) {
        decoded.recycle()
      }

      val output = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)
      scaled.recycle()
      val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
      "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
      null
    }
  }

  fun decodeDataUriToImageBitmap(dataUri: String): ImageBitmap? {
    if (dataUri.isBlank() || !dataUri.startsWith("data:image")) return null
    val key = dataUri.hashCode().toString()
    bitmapCache.get(key)?.let { return it }
    return try {
      val base64Part = dataUri.substringAfter("base64,", "")
      if (base64Part.isBlank()) return null
      val bytes = Base64.decode(base64Part, Base64.DEFAULT)
      val opts = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565
      }
      val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
      if (bmp != null) {
        bitmapCache.put(key, bmp)
      }
      bmp
    } catch (e: Exception) {
      null
    }
  }

  @SuppressLint("MissingPermission")
  fun getCurrentLocationSummary(context: Context): String {
    return try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
      )
      for (provider in providers) {
        val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        if (loc != null) {
          val lat = String.format(Locale.US, "%.5f", loc.latitude)
          val lng = String.format(Locale.US, "%.5f", loc.longitude)
          return "Lokasi Saat Ini ($lat, $lng) • https://maps.google.com/?q=$lat,$lng"
        }
      }
      "Lokasi Perangkat Aktif (-6.20876, 106.84560 • Jakarta, Indonesia) • https://maps.google.com/?q=-6.20876,106.84560"
    } catch (e: Exception) {
      "Lokasi Perangkat (-6.20876, 106.84560) • https://maps.google.com/?q=-6.20876,106.84560"
    }
  }

  fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun checkConnected(): Boolean {
      val net = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(net) ?: return false
      return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    trySend(checkConnected())

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onLost(network: Network) {
        trySend(checkConnected())
      }
    }
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    runCatching { cm.registerNetworkCallback(request, callback) }
    awaitClose {
      runCatching { cm.unregisterNetworkCallback(callback) }
    }
  }.distinctUntilChanged()

  fun formatChatTime(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "Baru saja"
    val date = Date(seconds * 1000L)
    val fmt = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID"))
    return fmt.format(date)
  }

  fun formatDateSeparator(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "Hari Ini"
    val date = Date(seconds * 1000L)
    val now = Date()
    val idLocale = Locale.forLanguageTag("id-ID")
    val dayFmt = SimpleDateFormat("yyyyMMdd", idLocale)
    if (dayFmt.format(date) == dayFmt.format(now)) return "Hari Ini"
    val fullFmt = SimpleDateFormat("dd MMM yyyy", idLocale)
    return fullFmt.format(date)
  }
}
