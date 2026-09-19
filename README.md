# Aerial Fishing Tracker

A RuneLite plugin for aerial fishing on Molch Island. Keeps lifetime catches and pearl streaks per RuneScape character, with separate session counters and cumulative golden tench probability.

## Installation status

Not yet published or approved in the Plugin Hub. The development launcher below is for local testing. After acceptance and publication, install by searching for **Aerial Fishing Tracker** in RuneLite's Plugin Hub.

## Features

- White: lifetime fish, session fish, rolls since the last pearl, fish per hour.
- Light green: session/lifetime pearls, actual pearl rate, wiki pearl rate, pearls per hour.
- Light blue: longest dry and most spooned.
- Pink: lifetime golden tenches and cumulative tench chance, selectable between **This session** and **Lifetime catches**.
- Every row is independently toggleable in RuneLite's plugin settings. The default overlay uses RuneLite's Plain 12 (`p12_full`) font at size 15 and background opacity 160. The default width is 170 logical pixels. Standard RuneLite overlay moving/resizing is supported; saved sizing can override the default. The overlay appears only on Molch Island.
- The **Historical additions** section in plugin settings accepts previous fish, optional pearls, and optional tenches. Enter amounts, enable **Apply historical additions**, and confirm the character and exact additions. Historical values are **additions**, not replacement totals. Add each past amount only once. The fish-shaped sidebar provides the same controls.
- Right-click the overlay using RuneLite's overlay-menu modifier, or use the sidebar, to **Reset session only**. RuneLite's confirmation-protected **Reset** button at the bottom of this plugin's settings performs a full reset: it deletes the loaded character's tracker data and restores plugin/overlay defaults.

## What is counted

**A catch roll means one successful cormorant return, not the number of fish items retained.** A full inventory still allows a successful roll. Rada's blessing and spirit flakes award no extra XP and duplicate the selected catch rather than making another independent golden-tench roll. The tracker supports at most two awarded tench items per successful return. Bonus-item behavior is subject to the live verification described below. The tench item counter includes a detected duplicate.

The plugin reconciles paired Fishing/Hunter XP updates while holding the cormorant with the successful-return game message once per server tick. It does not count the bird-launch message. This is designed to support full inventories and repeated frenzied-pool catches; when one skill is capped, it uses the other, and the return message supplies a fallback when both are capped. Inventory changes alone never create a catch. These event combinations have automated coverage; current live game behavior still needs the acceptance pass below.

Pearls and tenches are identified from inventory/owned ground loot in the catch tick and relevant game messages, reconciled to avoid duplicate counting. Signed inventory/ground changes cancel out transfers such as picking up old loot or dropping it. Ground expiry is excluded from transfer cancellation. During a confirmed catch, the optional `Untradeable drop: Molch pearl` chat line can identify a pearl; inventory and owned-ground events still detect it when that notification is disabled. A 100-pearl tench trade-in or shop refund is not counted as caught pearls. Ground rewards must be owned by this player and on their tile. See [validation notes](docs/VALIDATION.md) for limitations and the required live checks.

## Sessions and lifetime data

- A session resets after the configured idle time without a successful catch (default: **15 minutes**), on manual reset, or when the RuneLite process restarts.
- World hops, teleporting away, logging out/in within the configured idle period, and toggling the plugin in the same process preserve the session. The timeout is checked during game ticks and before recording the next catch, so that catch starts the new session.
- Lifetime statistics, historical additions and streaks are saved per RuneScape character using RuneLite's RS-profile configuration API. Special world types have RuneLite's separate RS profiles.
- Lifetime statistics have a separate storage group from display preferences. This plugin extends RuneLite's confirmed settings reset to explicitly delete both saved snapshots for the currently loaded character. The latest valid snapshot is mirrored to a backup key; malformed/unsupported primary data pauses tracking rather than silently starting over. A missing primary key can recover from the mirrored backup.
- RuneLite handles flushing its configuration to disk. No local plugin can guarantee survival of disk failure, deletion of the RuneLite profile, or every last catch after a power loss. Back up your RuneLite profile data if these totals matter. Do not run two clients writing the same character's tracker data concurrently.
- Historical fish and pearls affect lifetime totals, lifetime tench probability, and actual pearl rate. They do not affect sessions or recorded pearl streaks.

## Mathematics

For `n` catches, chance of **at least one** golden tench is:

```
1 - (1 - 1/20,000)^n
```

Each next catch still has a 1/20,000 chance. Receiving a tench does not reset the cumulative statistic. At 20,000 catches the cumulative chance is about 63.213%. The implementation uses `log1p` and `expm1` to retain precision for small counts and displays `>99.9999%` rather than implying a guarantee.

Actual pearl rate is `lifetime fish / lifetime pearls`, including historical additions. It displays `--` until a pearl has been recorded or added. This is meaningful when the historical fish and pearl counts cover the same period.

Wiki pearl rate is an estimate. It starts with the wiki's level formula and applies the approximately 50% pearl-rate increase from the 19 August 2026 update:

```
X = floor((2 * visible Fishing level + visible Hunter level) / 3)
pearl probability = 1.5 / (100 - (X - 40) * 25 / 59)
```

Visible boosts above 99 are included. Jagex described the increase as approximately 50%, so the displayed value remains an estimate of the current rate. At 99/99 it gives 1/50.

Fish per hour and pearls per hour use the current session totals divided by elapsed time since the session's first catch. They display `--` until the second catch and reset whenever the session resets. Displayed rates refresh together every five seconds and are rounded to whole numbers.

“Rolls since pearl” is zero immediately after a caught pearl; before the first recorded pearl it counts from tracking start. “Longest dry” and “Most spooned” both include the successful pearl roll that ends an interval, so 174 failed rolls followed by a pearl display as an interval of 175. Longest dry also considers an unfinished run. Most spooned stays `--` until two pearls establish a complete interval.

## Build and run locally

Use **JDK 17**. This project targets Java 11 bytecode to match the official RuneLite example plugin. A Gradle wrapper with the official distribution checksum is included. No globally installed Gradle is required.

Windows PowerShell, in this directory:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\gradlew.bat clean test build --no-daemon
.\gradlew.bat run --no-daemon
```

In this prepared workspace, the portable JDK is already under `.tools/jdk17/`:

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/jdk17 -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.tools/gradle-cache'
.\gradlew.bat run --no-daemon
```

Alternatively, double-click `Start-Tracker.cmd`. It selects the prepared portable JDK and runs the same development launcher. On another computer, set `JAVA_HOME` to JDK 17 first.

On macOS/Linux use a JDK 17 `JAVA_HOME` and `bash ./gradlew clean test build --no-daemon`, then `bash ./gradlew run --no-daemon`.

The `run` task launches a development RuneLite client with this plugin loaded as a built-in plugin. Search for **Aerial Fishing Tracker** in the plugin list and enable it. It uses RuneLite's normal local profile directory. Do not overwrite your normal RuneLite installation or copy this JAR into its cache: standalone JARs are not a supported Plugin Hub installation method.


For Jagex-account authentication, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) developer guide. Authentication must be done by the account owner; this project contains no credentials or authentication workaround.

The default dependency is RuneLite `latest.release`, matching the official example. To reproduce validation against the version tested here:

```powershell
.\gradlew.bat clean test build -PruneliteVersion=1.12.39
```

`gradlew preview` renders the actual overlay implementation to `build/overlay-preview.png` without logging in. Test results are in `build/reports/tests/test/index.html`; the plugin-only JAR is in `build/libs/`.

## Public release

The project includes standard `runelite-plugin.properties`, a license, an isolated tracking model, tests, and a development launcher. Before submission, run the live checklist in [docs/VALIDATION.md](docs/VALIDATION.md), verify the author metadata, create a public source repository, and follow the Plugin Hub's current submission instructions. Plugin Hub approval is separate from this project and does not certify functional correctness.

The production plugin makes no network requests, automates no gameplay, and adds no runtime dependencies beyond RuneLite's existing libraries. Mockito and JUnit are test-only dependencies. It reads only relevant game events and stores statistics through RuneLite configuration.

## References

- [OSRS Wiki: Aerial fishing](https://oldschool.runescape.wiki/w/Aerial_fishing) — retrieved 2026-09-18; raw source inspected for full-inventory behavior, bonus fish and revised pearl formula.
- [RuneLite developer guide](https://github.com/runelite/runelite/wiki/Developer-Guide)
- [Official example plugin](https://github.com/runelite/example-plugin)
- [Plugin Hub development/submission](https://github.com/runelite/plugin-hub)
- [RuneLite Plugin Hub review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
- [RuneLite FishingPlugin](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/fishing/FishingPlugin.java) — successful cormorant return message.

This is an independent implementation, not endorsed by RuneLite or Jagex. See [release preparation](docs/RELEASE.md) for publication status and submission steps.
