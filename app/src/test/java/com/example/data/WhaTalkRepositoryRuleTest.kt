package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WhaTalkRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun extractFirestoreException(t: Throwable): FirebaseFirestoreException? {
    var curr: Throwable? = t
    while (curr != null) {
      if (curr is FirebaseFirestoreException) return curr
      curr = curr.cause
    }
    return null
  }

  @Test
  fun saveProfileAndSendMessage_validMembers_succeeds() = runBlocking {
    val aliceUid = signInTestUser("alice_whatalk@test.com")
    val repo = WhaTalkRepository(firestore, auth)

    val profileRes = withTimeout(5000L) {
      repo.saveUserProfile(
        displayName = "Alice Pratama",
        username = "alice_pratama",
        bio = "Halo, saya menggunakan WhaTalk!",
      )
    }
    assertTrue("Profile save should succeed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)

    val fetchedProfile = withTimeout(5000L) { repo.getUserProfile(aliceUid).getOrThrow() }
    assertNotNull(fetchedProfile)
    assertEquals("alice_pratama", fetchedProfile?.username)

    val convId = withTimeout(5000L) {
      repo.getOrCreateDirectConversation(
        otherUid = "bob_target_uid",
        otherDisplayName = "Budi Santoso",
      ).getOrThrow()
    }
    assertTrue(convId.isNotBlank())

    val msgId = withTimeout(5000L) {
      repo.sendMessage(
        conversationId = convId,
        senderName = "Alice Pratama",
        memberUids = listOf(aliceUid, "bob_target_uid"),
        text = "Halo Budi, apa kabar?",
      ).getOrThrow()
    }
    assertTrue(msgId.isNotBlank())

    val messages = withTimeout(4000L) {
      repo.observeMessages(convId, aliceUid).first { list -> list.any { it.id == msgId } }
    }
    assertTrue(messages.any { it.text == "Halo Budi, apa kabar?" })
  }

  @Test
  fun crossUserSettingsAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_settings@test.com")
    val aliceRepo = WhaTalkRepository(firestore, auth)

    withTimeout(5000L) {
      aliceRepo.saveUserSettings(
        UserSettings(
          userId = aliceUid,
          phoneNumber = "+6281122334455",
          email = "alice_settings@test.com",
        )
      ).getOrThrow()
    }

    // Switch to Bob and attempt to read Alice's private settings
    signInTestUser("bob_intruder@test.com")
    val bobRepo = WhaTalkRepository(firestore, auth)
    try {
      withTimeout(4000L) {
        bobRepo.observeUserSettings(aliceUid).first()
      }
      fail("Bob should not be able to read Alice's private UserSettings")
    } catch (e: Throwable) {
      val fsEx = extractFirestoreException(e)
      assertNotNull("Expected FirebaseFirestoreException in cause chain, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  @Test
  fun unauthenticatedUser_cannotObserveConversations() = runBlocking {
    auth.signOut()
    val repo = WhaTalkRepository(firestore, auth)
    try {
      withTimeout(4000L) {
        repo.observeConversations("any_uid").first()
      }
      fail("Unauthenticated user should not be able to observe conversations")
    } catch (e: Throwable) {
      val fsEx = extractFirestoreException(e)
      assertNotNull("Expected FirebaseFirestoreException in cause chain, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }
}
