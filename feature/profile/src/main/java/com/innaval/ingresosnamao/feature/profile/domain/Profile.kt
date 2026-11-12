package com.innaval.ingresosnamao.feature.profile.domain

data class ProfileIdentity(val uid: String, val name: String?, val email: String?)
data class UserProfile(val name: String?, val email: String?, val address: String?, val fromCache: Boolean)

enum class ProfileError {
    AUTH, READ, INVALID_DATA, CART, SESSION_CHANGED, SESSION_CHANGED_AFTER_CLEAR, SIGN_OUT_AFTER_CLEAR
}

sealed interface ProfileResult<out T> {
    data class Success<T>(val value: T) : ProfileResult<T>
    data class Failure(val error: ProfileError) : ProfileResult<Nothing>
}

interface ProfileRepository {
    suspend fun currentUser(): ProfileResult<ProfileIdentity?>
    suspend fun load(identity: ProfileIdentity): ProfileResult<UserProfile>
    suspend fun signOut(expectedUid: String): ProfileResult<Unit>
}

fun interface ProfileCartCleaner {
    suspend fun clear(userId: String): ProfileResult<Unit>
}

class GetProfileUseCase(private val repository: ProfileRepository) {
    suspend fun currentUser() = repository.currentUser()
    suspend operator fun invoke(identity: ProfileIdentity) = repository.load(identity)
}

class LogoutUseCase(private val repository: ProfileRepository, private val cart: ProfileCartCleaner) {
    suspend operator fun invoke(expectedUid: String): ProfileResult<Unit> {
        return when (val current = repository.currentUser()) {
            is ProfileResult.Failure -> current
            is ProfileResult.Success ->
                if (current.value?.uid != expectedUid) ProfileResult.Failure(ProfileError.SESSION_CHANGED)
                else clearAndSignOut(expectedUid)
        }
    }

    private suspend fun clearAndSignOut(uid: String): ProfileResult<Unit> =
        when (val cleared = cart.clear(uid)) {
            is ProfileResult.Failure -> cleared
            is ProfileResult.Success -> when (val result = repository.signOut(uid)) {
                is ProfileResult.Success -> result
                is ProfileResult.Failure -> ProfileResult.Failure(
                    if (result.error == ProfileError.SESSION_CHANGED) ProfileError.SESSION_CHANGED_AFTER_CLEAR
                    else ProfileError.SIGN_OUT_AFTER_CLEAR
                )
            }
        }
}
