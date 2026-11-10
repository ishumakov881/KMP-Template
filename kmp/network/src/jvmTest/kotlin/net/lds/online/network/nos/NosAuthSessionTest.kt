package net.lds.online.network.nos

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Live check against nos.lds.online.
 * Requires NOS_LOGIN / NOS_PASSWORD env + remove @Ignore. Do not commit credentials.
 */
@Ignore
class NosAuthSessionTest {
    @Test
    fun loginCapturesPhpSessionAndGetCount() = runBlocking {
        val login = System.getenv("NOS_LOGIN") ?: error("NOS_LOGIN not set")
        val password = System.getenv("NOS_PASSWORD") ?: error("NOS_PASSWORD not set")
        val repo = NosAuthRepository()
        try {
            val result = repo.login(login, password)
            assertNotNull(result.phpSessionId)
            assertTrue(result.phpSessionId!!.isNotBlank())
            assertTrue(result.switchCountsJson.contains("sw_ok"))
        } finally {
            repo.close()
        }
    }
}
