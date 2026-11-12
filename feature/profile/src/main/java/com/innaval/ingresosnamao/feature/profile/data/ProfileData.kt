package com.innaval.ingresosnamao.feature.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileRepository
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal data class ProfileDocument(val fields: Map<String, Any?>?, val fromCache: Boolean)

internal interface ProfileAuthSource {
    fun currentUser(): ProfileIdentity?
    fun signOut()
}

internal fun interface ProfileDocumentSource {
    suspend fun get(uid: String): ProfileDocument
}

internal class FirebaseProfileAuthSource : ProfileAuthSource {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override fun currentUser(): ProfileIdentity? = auth.currentUser?.let {
        ProfileIdentity(it.uid, it.displayName, it.email)
    }

    override fun signOut() = auth.signOut()
}

internal class FirestoreProfileDocumentSource : ProfileDocumentSource {
    override suspend fun get(uid: String): ProfileDocument {
        val document = FirebaseFirestore.getInstance().collection("users").document(uid)
        val snapshot = readConfirmedProfile(
            server = { document.get(Source.SERVER).await() },
            cache = { document.get(Source.CACHE).await() },
            confirmed = { it.exists() && !it.metadata.hasPendingWrites() },
        )
        return ProfileDocument(snapshot.data, snapshot.metadata.isFromCache)
    }
}

@Suppress("ThrowsCount", "TooGenericExceptionCaught")
internal suspend fun <T> readConfirmedProfile(
    server: suspend () -> T,
    cache: suspend () -> T,
    confirmed: (T) -> Boolean,
): T = try {
    server()
} catch (e: FirebaseFirestoreException) {
    if (e.code != FirebaseFirestoreException.Code.UNAVAILABLE &&
        e.code != FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    ) throw e
    val cached = try {
        cache()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (cacheError: Exception) {
        e.addSuppressed(cacheError)
        throw e
    }
    if (!confirmed(cached)) throw e
    cached
}

internal class FirebaseProfileRepository(
    private val auth: ProfileAuthSource,
    private val documents: ProfileDocumentSource,
) : ProfileRepository {
    override suspend fun currentUser(): ProfileResult<ProfileIdentity?> = guard(ProfileError.AUTH) {
        auth.currentUser()
    }

    override suspend fun load(identity: ProfileIdentity): ProfileResult<UserProfile> = guard(ProfileError.READ) {
        checkIdentity(identity.uid)
        val document = documents.get(identity.uid)
        checkIdentity(identity.uid)
        val name = document.fields.optionalString("name")
        val email = document.fields.optionalString("email")
        val address = document.fields.optionalString("address")
        if (email != null && identity.email != null && !email.equals(identity.email, ignoreCase = true)) {
            throw InvalidProfileException()
        }
        UserProfile(
            name = name ?: identity.name.nonBlank(),
            email = identity.email.nonBlank() ?: email,
            address = address,
            fromCache = document.fromCache,
        )
    }

    override suspend fun signOut(expectedUid: String): ProfileResult<Unit> = guard(ProfileError.AUTH) {
        checkIdentity(expectedUid)
        auth.signOut()
    }

    private fun checkIdentity(expectedUid: String) {
        if (expectedUid.isBlank() || auth.currentUser()?.uid != expectedUid) throw SessionChangedException()
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private suspend fun <T> guard(error: ProfileError, action: suspend () -> T): ProfileResult<T> = try {
        ProfileResult.Success(action())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (changed: SessionChangedException) {
        ProfileResult.Failure(ProfileError.SESSION_CHANGED)
    } catch (invalid: InvalidProfileException) {
        ProfileResult.Failure(ProfileError.INVALID_DATA)
    } catch (failure: Exception) {
        ProfileResult.Failure(error)
    }
}

private fun Map<String, Any?>?.optionalString(key: String): String? {
    val value = this?.get(key) ?: return null
    if (value !is String) throw InvalidProfileException()
    return value.nonBlank()
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
private class SessionChangedException : Exception()
private class InvalidProfileException : Exception()
