package com.innaval.ingresosnamao.feature.auth.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.innaval.ingresosnamao.feature.auth.domain.AuthError
import com.innaval.ingresosnamao.feature.auth.domain.AuthRepository
import com.innaval.ingresosnamao.feature.auth.domain.AuthResult
import com.innaval.ingresosnamao.feature.auth.domain.AuthUser
import com.innaval.ingresosnamao.feature.auth.domain.AuthValidation
import com.innaval.ingresosnamao.feature.auth.domain.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal class FirebaseAuthDataSource {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun currentUser(): AuthUser? = auth.currentUser?.toDomain()
    suspend fun login(email: String, password: String): AuthUser =
        requireNotNull(auth.signInWithEmailAndPassword(email, password).await().user).toDomain()
    suspend fun loginWithGoogle(token: String): AuthUser =
        requireNotNull(auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user).toDomain()
    suspend fun register(email: String, password: String): AuthUser =
        requireNotNull(auth.createUserWithEmailAndPassword(email, password).await().user).toDomain()

    private fun FirebaseUser.toDomain() = AuthUser(uid, requireNotNull(email), displayName.orEmpty())
}

internal class FirestoreProfileDataSource {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun get(user: AuthUser): UserProfile? {
        val document = db.collection("users").document(user.id)
        val snapshot = readWithConfirmedCache(
            server = { document.get(Source.SERVER).await() },
            cache = { document.get(Source.CACHE).await() },
            confirmed = { it.exists() && !it.metadata.hasPendingWrites() },
            canUseCache = {
                it is FirebaseFirestoreException &&
                    (it.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                        it.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
            },
        )
        if (!snapshot.exists()) return null
        val profile = UserProfile(
            name = requireNotNull(snapshot.getString("name")),
            email = requireNotNull(snapshot.getString("email")),
            address = requireNotNull(snapshot.getString("address")),
        )
        require(AuthValidation.profile(profile.name, profile.email, profile.address).isEmpty())
        require(profile.email.equals(user.email, ignoreCase = true))
        return profile
    }

    suspend fun save(user: AuthUser, profile: UserProfile) {
        db.collection("users").document(user.id).set(
            mapOf("name" to profile.name, "email" to profile.email, "address" to profile.address)
        ).await()
    }
}

internal suspend fun <T> readWithConfirmedCache(
    server: suspend () -> T,
    cache: suspend () -> T,
    confirmed: (T) -> Boolean,
    canUseCache: (Exception) -> Boolean,
): T = try {
    server()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    if (!canUseCache(e)) throw e
    val cached = try {
        cache()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (cacheError: Exception) {
        throw e
    }
    if (!confirmed(cached)) throw e
    cached
}

internal class AuthRepositoryImpl(
    private val auth: FirebaseAuthDataSource,
    private val profiles: FirestoreProfileDataSource,
) : AuthRepository {
    override suspend fun currentUser(): AuthResult<AuthUser?> = authCall { auth.currentUser() }
    override suspend fun login(email: String, password: String): AuthResult<AuthUser> =
        authCall { auth.login(email, password) }
    override suspend fun loginWithGoogle(idToken: String): AuthResult<AuthUser> =
        authCall { auth.loginWithGoogle(idToken) }
    override suspend fun register(email: String, password: String): AuthResult<AuthUser> =
        authCall { auth.register(email, password) }
    override suspend fun getProfile(user: AuthUser): AuthResult<UserProfile?> =
        profileCall(AuthError.PROFILE_READ) { profiles.get(user) }
    override suspend fun saveProfile(user: AuthUser, profile: UserProfile): AuthResult<Unit> =
        profileCall(AuthError.PROFILE_WRITE) { profiles.save(user, profile) }

    private suspend fun <T> authCall(block: suspend () -> T): AuthResult<T> = try {
        AuthResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Failure(
            when (e) {
                is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
                is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
                is FirebaseAuthInvalidCredentialsException -> AuthError.INVALID_CREDENTIALS
                is FirebaseAuthInvalidUserException ->
                    if (e.errorCode == "ERROR_USER_DISABLED") AuthError.USER_DISABLED else AuthError.INVALID_CREDENTIALS
                is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
                is FirebaseNetworkException -> AuthError.NETWORK
                is IllegalStateException -> AuthError.CONFIGURATION
                else -> AuthError.UNKNOWN
            }
        )
    }

    private suspend fun <T> profileCall(error: AuthError, block: suspend () -> T): AuthResult<T> = try {
        AuthResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Failure(if (e is IllegalStateException) AuthError.CONFIGURATION else error)
    }
}
