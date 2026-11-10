package net.lds.online.network.nos

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Live checks against nos.lds.online.
 * Requires NOS_LOGIN / NOS_PASSWORD env + remove @Ignore. Do not commit credentials.
 */
@Ignore
class NosAuthSessionTest {
    @Test
    fun loginCapturesPhpSessionAndOpensOsm() {
        runBlocking {
            val login = System.getenv("NOS_LOGIN") ?: error("NOS_LOGIN not set")
            val password = System.getenv("NOS_PASSWORD") ?: error("NOS_PASSWORD not set")
            val repo = NosAuthRepository()
            try {
                val result = repo.login(login, password)
                assertNotNull(result.phpSessionId)
                assertTrue(result.phpSessionId!!.isNotBlank())
                assertTrue(result.authenticatedPageMarker.isNotBlank())
            } finally {
                repo.close()
            }
        }
    }

    @Test
    fun wrongPasswordDoesNotAuthenticate() {
        runBlocking {
            val repo = NosAuthRepository()
            try {
                assertFails {
                    repo.login("wrong.user", "definitely-wrong-password")
                }
            } finally {
                repo.close()
            }
        }
    }
}
