package com.orioooneee.lmuasister.data.mock

import com.orioooneee.lmuasister.data.steam.SignInOutcome
import com.orioooneee.lmuasister.data.steam.SteamSignIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Offline mock backend (enabled when `local.properties` has no `backend.url`, or
 * `backend.mock=true`). Lets anyone build & run the app from git without the real
 * server: a Ktor [MockEngine] answers every endpoint with deterministic data from
 * [MockData], with simulated latency so loaders behave like the real thing.
 *
 * Nothing downstream knows it's mocked — [com.orioooneee.lmuasister.data.remote.BackendApi]
 * and [com.orioooneee.lmuasister.data.remote.SteamBackendApi] just see normal HTTP bodies.
 */

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/** Drop-in for the real network client — every request is served from [MockData]. */
fun mockHttpClient(): HttpClient = HttpClient(MockEngine) {
    followRedirects = true
    engine {
        addHandler { request ->
            val full = request.url.encodedPath
            val path = full.substringAfter("/api/v2", full) // version-agnostic route
            val params = request.url.parameters
            val authed = request.headers[HttpHeaders.Authorization] != null

            delay(MockData.latencyFor(path)) // simulated loading

            fun json(body: String) = respond(body, HttpStatusCode.OK, jsonHeaders)

            when {
                path == "/schedule" -> json(MockData.schedule())
                path == "/cars" -> json(MockData.cars())
                path == "/tracks" -> json(MockData.tracks())
                path == "/privacy" -> respond(MockData.privacy(), HttpStatusCode.OK)

                path == "/profile" -> json(MockData.profile())
                path == "/profile/stats" -> json(MockData.stats())
                path == "/profile/races" -> json(MockData.racesPage(params["page"]?.toIntOrNull() ?: 1))
                path.startsWith("/profile/races/") ->
                    json(MockData.categoryRacesPage(path.removePrefix("/profile/races/"), params["page"]?.toIntOrNull() ?: 1))
                path.startsWith("/profile/track/") ->
                    json(MockData.trackDetail(path.removePrefix("/profile/track/")))
                path.startsWith("/profile/race/") ->
                    json(MockData.profileRaceDetail(path.removePrefix("/profile/race/")))

                path.startsWith("/race/") && path.endsWith("/hotlaps") ->
                    json(MockData.hotlaps(path.removePrefix("/race/").removeSuffix("/hotlaps")))
                path.startsWith("/race/") -> json(MockData.raceDetail(path.removePrefix("/race/")))

                path.startsWith("/leaderboard/") -> json(
                    MockData.leaderboardPage(
                        leaderboardId = path.removePrefix("/leaderboard/"),
                        cursor = params["cursor"],
                        limit = params["limit"]?.toIntOrNull() ?: 50,
                        withMe = authed,
                    ),
                )

                // Auth surface — never hit while MockSteamSignIn is active, but answered
                // so anything calling the backend directly still gets a sane response.
                path == "/auth/sign-out" -> respondOk()
                path.startsWith("/auth/") -> json("""{"token":"mock-token","uid":"mock-uid-0397"}""")

                else -> json("{}")
            }
        }
    }
}

/**
 * Fake sign-in: skips Steam/JavaSteam/tunnel entirely and hands back a token, so the
 * profile screen lands signed-in on mock data. Auto-restores on launch; "sign out"
 * drops back to the login form (any credentials sign back in).
 */
class MockSteamSignIn : SteamSignIn {
    private var signedIn = true

    override suspend fun signIn(username: String, password: String, guardCode: String?): SignInOutcome {
        delay(600) // simulate the round-trip
        signedIn = true
        return SignInOutcome.Success(appToken = MOCK_TOKEN, uid = "mock-uid-0397")
    }

    override suspend fun restore(): String? = MOCK_TOKEN.takeIf { signedIn }

    override suspend fun reauth(): String? = MOCK_TOKEN.takeIf { signedIn }

    override fun signOut() {
        signedIn = false
    }

    private companion object {
        const val MOCK_TOKEN = "mock-token"
    }
}

/** Replaces the per-platform `steamModule()` when running on mock data. */
val mockModule: Module = module {
    single<SteamSignIn> { MockSteamSignIn() }
}
