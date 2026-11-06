# Hijri Calendar Companion

> A home-screen widget and reminders that put the Hijri date next to the Gregorian one on Android (Samsung first).

**Status:** v0.2.0, a working Android app: the date engine, a home-screen widget and evening reminders. Not yet tested on a real phone. **Feasibility rating:** 4/10 — useful daily, but partly solved by built-in calendars in some regions.
**Idea:** #01 in the owner's ideas backlog. **Author:** Oussama Elhadji (`@Bosaj`) <oussousselhadji@gmail.com>.

---

## Problem

On many Galaxy phones the Hijri date is hidden, region-locked, or missing from Samsung Calendar. Users who follow Islamic dates (Ramadan, Ayyam al-Bid, Eid) have to open a separate app to know today's Hijri date.

## Why build it (gap analysis)

Islamic calendar support already exists on some devices and in some regions, and Android ships `android.icu.util.IslamicCalendar`. A full calendar app would duplicate that. The realistic product is a complementary widget + reminder app that reads the existing calendar and adds the Hijri layer, with a user-chosen calculation method (Umm al-Qura, civil, or local moon-sighting offset).

## Prior art & open bases to fork

- [xsoh/awesome-hijri](https://github.com/xsoh/awesome-hijri) — Curated list of Hijri libraries in every language.
- [alhazmy13/HijriDatePicker](https://github.com/alhazmy13/HijriDatePicker) — Android Hijri date picker component.
- [android.icu.util.IslamicCalendar](https://developer.android.com/reference/android/icu/util/IslamicCalendar) — Built-in Android API (API 24+) with civil, tabular and Umm al-Qura variants.

## What it does (v0.2.0)

- **Today**: the Hijri date above the Gregorian one, and what falls today.
- **Month view**: each Hijri month, with the Gregorian day under each Hijri day. White days and occasions are highlighted.
- **Coming up**: Ramadan, the two Eids, Arafah, the Islamic New Year, Ashura and the Mawlid, with their Gregorian dates and a countdown.
- **Calculation**: Umm al-Qura or the arithmetic civil calendar, plus a day offset from −2 to +2.
- **Widget**: a home-screen widget with both dates and today's occasion. It refreshes just after midnight and when the clock or time zone changes.
- **Reminders**: a notification the evening before, at an hour you choose, for white days, Monday and Thursday fasts, and occasions. Fasting reminders skip Ramadan, the Eids and the days of Tashriq. Dates that depend on the moon say they need the official announcement.
- Arabic, French and English, right-to-left in Arabic. No internet permission: nothing leaves the phone.

### Why the offset matters

Morocco starts each month when the new moon is seen, so no calculation knows the date in advance. Bulletin Officiel issue 7556 is dated 17 Jumada al-Ula 1448, 29 October 2026, while Umm al-Qura says 18. With the offset at −1, the app matches the official date. A test checks this.

## How it is built

1. **Date engine** (`engine/HijriEngine.kt`): pure Kotlin on `java.time`, so it runs and is tested on the JVM.
   - Umm al-Qura comes from a table of month lengths for 1356–1500 AH (1937–2077), generated from ICU by `tools/umalqura_table.mjs`. Embedding it gives the same answer on every Android version. Outside the table, the civil calendar answers and the app says so.
   - The civil calendar uses the same arithmetic as ICU's `islamic-civil`.
2. **Widget**: Jetpack Glance. **Reminders**: WorkManager, once a day. **Settings**: DataStore.
3. **Tests**: both methods are checked against 407 days from 1990 to 2076 converted by ICU (`tools/hijri_vectors.mjs`), plus offsets, month lengths, occasions and fasting exceptions.

Stack: Kotlin 2.2, Jetpack Compose with Material 3, Glance 1.1, WorkManager, DataStore. minSdk 26, targetSdk 36.

## Roadmap

- v0.1 — Hijri date engine with method choice + manual day offset. **Done.**
- v0.2 — Home-screen widget: **done**. Phones have no lock-screen widgets since Android 5, so that part waits for tablets or a later Android release.
- v0.3 — Reminders for white days, Ramadan, Eid and other occasions: **done**. Custom Hijri events are not built yet.
- v0.4 — Samsung Calendar overlay + Wear OS tile.
- Later: prayer times (#8), qibla (#9) and a calendar feed (#11). Prayer times need checking against the Ministry of Habous tables before they ship.

---

## For LLM agents & contributors continuing this project

v0.2.0 is built: the engine, widget and reminders above. If you are an AI agent asked to "continue developing", read this section first.

- Never hard-code one calculation method: Morocco follows local moon sighting, so the offset setting is a core feature.
- Change the engine only with its tests. Regenerate the reference days with `node tools/hijri_vectors.mjs > app/src/test/resources/icu_vectors.json` if you widen the range.
- On Windows, build with `TMP` and `TEMP` set to a short path such as `C:\gtmp`, and `GRADLE_OPTS=-Djdk.net.unixdomain.tmpdir=C:\gtmp`.
- Check the latest Samsung Calendar build first: if the Hijri layer is now available in Morocco, pivot to the widget only.

**Ground rules:** keep this README in sync with reality as you build; prefer forking/reusing the bases above over greenfield code; write tests for anything correctness-critical; never commit secrets (see `.gitignore`); follow Conventional Commits (see `CONTRIBUTING.md`).

## Getting started

You need JDK 17 and the Android SDK (platform 36).

```bash
./gradlew testDebugUnitTest   # engine and occasion tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

Install the APK, open the app once, then add the "Hijri date" widget from the home screen's widget list.

## 📄 License
**Proprietary & Commercial Software License** — All Rights Reserved © 2026 Oussama Elhadji (`@Bosaj`) <[oussousselhadji@gmail.com](mailto:oussousselhadji@gmail.com)>.

Unauthorized copying, distribution, resale, or reverse engineering is strictly prohibited. See [LICENSE](LICENSE) for full legal terms.
