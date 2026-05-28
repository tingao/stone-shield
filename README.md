# Stone Shield

**Biological hydration simulator for Android** — prevents kidney stones by minimizing time in the "Danger Zone."

Track your hydration like a fuel gauge: log water intake, pee output, alcohol, and sweat. The math engine calculates your current body water level, predicts when you'll hit critical thresholds, and sends alarms before it's too late.

---

## Screenshots

| Dashboard (empty) | Dashboard (with data) | Dashboard (dated) | Calendar |
|-----------|---------|---------------|---------|
| <img src="docs/screenshots/dashboard-clear.jpg" width="180"/> | <img src="docs/screenshots/dashboard-graphed.jpg" width="180"/> | <img src="docs/screenshots/dashboard-with-date.jpg" width="180"/> | <img src="docs/screenshots/calendar.jpg" width="180"/> |

| Intro 1 | Intro 2 | Intro 3 | Intro 4 | Settings |
|---------|---------|---------|---------|----------|
| <img src="docs/screenshots/intro1-track-hydration.jpg" width="180"/> | <img src="docs/screenshots/intro2-stay-in-the-green.jpg" width="180"/> | <img src="docs/screenshots/intro3-sleep-detection.jpg" width="180"/> | <img src="docs/screenshots/intro4-smart-alarms.jpg" width="180"/> | <img src="docs/screenshots/settings.jpg" width="180"/> |

---

## Features

### Core Mechanics
- **Live tank calculation** — body water level in mL, updated on every interaction.
- **Decay modeling** — configurable awake/sleep decay rates, temperature multiplier, and alcohol diuretic effect.
- **Color Snap** — log pee color to override the math with ground truth (Dark Orange → 0 mL, Clear → 800 mL).
- **Night Protocol** — automatically detects sleep via UsageStats and recalculates history with sleep decay rate.
- **Smart Alarms** — `AlarmManager.setExactAndAllowWhileIdle` schedules one-shot warnings before hitting safe/danger thresholds.

### User Interface
- **Animated gauge** — big bold mL reading with color-coded zones (Green/Yellow/Red).
- **Tank bar** — horizontal progress bar with danger/warning/safe markers.
- **Vico line chart** — hydration trend over time.
- **Quick actions** — +300/500/700 mL water, Alcohol log, Pee log with color + volume.
- **Bedtime Check** — "Did you sweat today?" dialog before sleeping (applies sweat penalty).
- **Morning Prompt** — "Wake up & Flow!" encourages 500 mL after sleep detection.
- **Event History** — reverse-chronological timeline with swipe-to-delete.
- **Snackbar feedback** — every action shows confirmation.
- **Dark mode** — automatically follows system theme.

### Sensors & Permissions
- **Battery temperature** — one-shot snapshot on interaction; clamped to 21°C when charging > 1 hr.
- **UsageStats** — screen-off sleep detection (requires Usage Access permission, guided via dialog).
- **Charge time tracking** — `BroadcastReceiver` monitors power connect/disconnect.

---

## Installation

Download the latest APK from [GitHub Releases](https://github.com/tingao/stone-shield/releases).

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires **Android 13+** (API 33).

---

## Build

```bash
# Requires: JDK 17+, Android SDK 35
git clone https://github.com/tingao/stone-shield.git
cd stone-shield
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

> **Note for OneDrive users:** If the project lives inside OneDrive, append `-Dorg.gradle.vfs.watch=false` to avoid file-sync conflicts.

---

## Architecture

```
com.stoneshield.app/
├── domain/              # Pure logic, no dependencies
│   ├── Constants.kt     # Physics constants (decay rates, thresholds)
│   └── HydrationMath.kt # Tank math engine (pure functions)
├── data/
│   ├── local/           # Room DB, EventEntity, DAO, DataStore prefs
│   └── repository/      # TankRepository (orchestrates math + DB)
├── di/                  # Hilt modules (Room, etc.)
├── scheduler/           # AlarmManager + ChargeTimeTracker
├── sensor/              # TemperatureProvider, UsageStatsProvider
└── ui/
    ├── dashboard/       # Main screen (gauge, chart, actions, dialogs)
    ├── history/         # Event timeline with swipe-to-delete
    ├── settings/        # Alarm toggle, clear data
    ├── onboarding/      # First-launch tutorial
    ├── navigation/      # NavHost with routes
    └── theme/           # Material3 light/dark color schemes
```

### "Calculus on Demand"
The app has **no background services**. Every interaction:
1. Queries events from Room DB (last 24h)
2. Replays the timeline through `HydrationMath` with current sensor data
3. Computes effective decay rate (sleep/temp/alcohol-adjusted)
4. Returns the current tank level
5. Schedules the next `AlarmManager` exact alarm
6. Composable renders the updated state

### Data Model
| Event Type | Value | Effect |
|-----------|-------|--------|
| `water` | mL added | Adds to tank |
| `alcohol` | — | Activates 1.5× diuretic for 120 min |
| `pee` | mL output | Subtracts from tank |
| `color_snap` | PeeColor ordinal | Overrides tank (Dark Orange→0, Clear→800, etc.) |
| `sleep` | — | Switches to `BASE_DECAY_SLEEP` |
| `wake` | — | Switches to `BASE_DECAY_AWAKE` |
| `sweat` | 0=light, 1=heavy | Instant penalty −200 or −400 mL |

### Tech Stack
| Component | Choice |
|-----------|--------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + Repository + Room) |
| DI | Hilt |
| Database | Room (SQLite) |
| Preferences | DataStore |
| Charting | Vico (Compose-native) |
| Async | Coroutines + Flow |
| Alarms | AlarmManager (`setExactAndAllowWhileIdle`) |
| Build | Gradle + Kotlin DSL |

---

---

## Built With AI

This entire application was built using **[opencode](https://opencode.ai)** and **DeepSeek V4 Flash** — an AI pair programmer, from concept to APK.

No hand-written code. Every line was generated through natural language conversation, iterated via build-feedback loops, and refined based on real device testing.

**Issues and improvements are welcome.** The codebase is fully editable — file an issue, open a PR, or just clone and build.

---

## License

Apache 2.0
