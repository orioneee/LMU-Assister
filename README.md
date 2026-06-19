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
| **Android** | ✅ | Device tunnel |
| **iOS** | ✅ | Device tunnel |
| **Desktop (JVM)** | ✅ | Device tunnel |

> Steam login runs through a backend **sidecar** that egresses via the device's own IP over a SOCKS-over-WebSocket tunnel — no on-device Steam client, identical flow on every platform. Tokens are persisted securely per platform (Android `EncryptedSharedPreferences`, iOS **Keychain**, JVM a local file).

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

## 🏗️ Architecture

```
HttpClient → BackendApi / SteamBackendApi → RaceRepository → ViewModels → Compose UI
```

- **Offline-first**: every screen paints from a local cache instantly, then revalidates over the network and keeps the cache on failure.
- **`expect`/`actual`** isolates platform code (secure storage, dial resolver, WS client) behind common interfaces; a shared `tunnelMain` source set carries the Steam tunnel for Android/JVM/iOS.
- All upstream merging (schedule, assets, weather, leaderboards) happens server-side; the client talks to one stable REST surface and never scrapes.

## 📂 Project structure

```
shared/        Kotlin Multiplatform module (all business logic + Compose UI)
  src/commonMain   shared code (UI, data, DI)
  src/androidMain  · iosMain · jvmMain   platform actuals
  src/tunnelMain   shared Steam device-tunnel (Android/JVM/iOS)
androidApp/    Android entry point
iosApp/        iOS entry point (Xcode project + SwiftUI host)
desktopApp/    Desktop (JVM) entry point
```

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
