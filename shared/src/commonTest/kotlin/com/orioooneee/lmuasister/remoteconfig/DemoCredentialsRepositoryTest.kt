package com.orioooneee.lmuasister.remoteconfig

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DemoCredentialsRepositoryTest {
    @Test
    fun resolvesAndCachesRemoteCredentials() = runTest {
        val source = FakeDemoCredentialsRemoteSource(
            mapOf("DEMO_LOGIN" to " demo ", "DEMO_PASS" to "demo_pass"),
        )
        val repository = DemoCredentialsRepository(source)

        val credentials = repository.get()

        assertEquals("demo", credentials?.login)
        assertEquals("demo_pass", credentials?.password)
        assertTrue(credentials?.matches("demo", "demo_pass") == true)
        repository.get()
        assertEquals(1, source.fetchCount)
    }

    @Test
    fun rejectsIncompleteRemoteConfig() = runTest {
        val source = FakeDemoCredentialsRemoteSource(mapOf("DEMO_LOGIN" to "demo"))
        val repository = DemoCredentialsRepository(source)

        assertNull(repository.get())
        assertNull(repository.get())
        assertEquals(2, source.fetchCount)
    }
}

private class FakeDemoCredentialsRemoteSource(
    private val values: Map<String, String>,
) : DemoCredentialsRemoteSource {
    var fetchCount = 0

    override fun fetch(keys: List<DemoCredentialKey>, onComplete: (Map<String, String>) -> Unit) {
        fetchCount++
        onComplete(values)
    }
}
