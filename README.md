<div align="center">

# 🏁 LMU Assister

**A companion app for [Le Mans Ultimate](https://www.lemansultimate.com/) — schedule, race details, leaderboards and your driver profile, on every platform from one Kotlin codebase.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Desktop-2ea44f)](#-platforms)
[![Android](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white)](#-running-the-apps)
[![Ktor](https://img.shields.io/badge/Ktor-3.5-087CFA?logo=ktor&logoColor=white)](https://ktor.io)
[![Koin](https://img.shields.io/badge/DI-Koin%204-orange)](https://insert-koin.io)

</div>

---

## ✨ Features

- **📅 Schedule** — daily / weekly / special / championship races with a collapsing week-and-category header, per-class colours, countdowns and pull-to-refresh.
- **🏎️ Race details** — circuit emblem, minimap, weather, settings, and the official fastest-lap **leaderboards** split per class (with your own row pinned via *"Your position"*).
- **🥇 Full leaderboard** — cursor-paginated (Paging 3), infinite scroll, aggressive prefetch.
- **👤 Steam profile** — sign in with your Steam account to see your Driver/Safety rating, badges, suspensions and recent races (offline-first, optimistic UI).
- **📜 Race history** — paginated "See all races", plus a per-race **detail page** with track card, your start→finish + positions gained/lost, and full qualifying/race classification (windowed around you, expandable, with player flags).
- **🌑 Dark motorsport theme** throughout, hand-built vector icons, no `material-icons-extended` dependency.

## 📱 Platforms

| Target | Status | Steam auth |
| --- | --- | --- |
| **Android** | ✅ | On-device (JavaSteam) |
| **Desktop (JVM)** | ✅ | On-device (JavaSteam) |
| **iOS** | ✅ | Backend + device tunnel |

> **Android & Desktop** sign in to Steam **on-device** with JavaSteam (a SteamKit2 port): your credentials never leave the machine — only a short-lived Steam Web API ticket is sent to the backend, which exchanges it for a game-data session. **iOS** has no native Steam library, so credentials go to the backend sidecar (.NET SteamKit2); but the Steam connection itself is routed back out through the device over a SOCKS-over-WebSocket tunnel, so Steam sees your normal home IP. Session tokens are persisted securely per platform (Android `EncryptedSharedPreferences`, iOS **Keychain**, JVM a local file).

## 🔐 Security & privacy

- **Android & Desktop — credentials never leave your device.** JavaSteam performs the whole Steam login locally; only a short-lived Steam Web API ticket is sent to the backend (exchanged for a game-data session). Your password and 2FA code never touch the network beyond Steam itself.
- **iOS — credentials are used once, server-side, and never kept.** They go to the backend sidecar (.NET SteamKit2) over HTTPS, are used in memory to sign in, then discarded immediately — not stored, not logged, not shared. The login egresses through your device (tunnel), so Steam sees your home IP.
- **Steam refresh tokens stay on your device, encrypted** (Android `EncryptedSharedPreferences`, iOS Keychain, JVM a local file). The backend stores only game-backend session tokens tied to your game-account id — never your Steam password or 2FA.
- **No ads, no third-party analytics.** Public content (schedule, leaderboards) is read through a shared service account, so browsing needs no sign-in; only your own data (profile, ratings, history, your leaderboard row) requires it.
- The full policy is fetched live and rendered in-app (`GET /api/v2/privacy`), linked from the sign-in screen.

## 🧱 Tech stack

| Concern | Library |
| --- | --- |
| UI | Compose Multiplatform |
| Navigation | `navigation-compose` (type-safe routes) |
| Networking | Ktor 3 (OkHttp / Darwin / CIO engines) |
| Serialization | `kotlinx.serialization` (snake_case ↔ camelCase) |
| DI | Koin 4 |
| Images | Coil 3 (+ SVG) |
| Pagination | AndroidX Paging 3 (multiplatform) |
| Secure storage | `androidx.security.crypto` / iOS Keychain |

## 🚀 Running the apps

### 1. Configure the backend

The app reads its backend base URL from `local.properties` (git-ignored, never committed):

```properties
backend.url=https://your-backend.example.com/api/v2
```

If unset, it falls back to `http://localhost:8000/api/v2`.

### 2. Run

| Platform | Command |
| --- | --- |
| **Android** | `./gradlew :androidApp:assembleDebug` (or run from the IDE) |
| **Desktop** | `./gradlew :desktopApp:run` — hot reload: `./gradlew :desktopApp:hotRun --auto` |
| **iOS** | open `iosApp/` in Xcode and run, or use the KMP run configuration |

## ⚙️ Requirements

- JDK 21
- Android SDK (compileSdk 37, minSdk 24)
- Xcode (for the iOS target)

---

<div align="center">
<sub>Built with ❤️ and Kotlin Multiplatform · not affiliated with Studio 397 / Motorsport Games.</sub>
</div>
