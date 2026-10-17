# Hijri Calendar Companion

> A home-screen widget and reminders that put the Hijri date next to the Gregorian one on Android (Samsung first).

**Status:** pre-alpha scaffold (no code yet). **Feasibility rating:** 4/10 — useful daily, but partly solved by built-in calendars in some regions.
**Idea:** #01 in the owner's ideas backlog. **Author:** Oussama EL HADJI. Part of the Bosaj ideas portfolio (sibling repo: `dual_portfolio_hub`).

---

## Problem

On many Galaxy phones the Hijri date is hidden, region-locked, or missing from Samsung Calendar. Users who follow Islamic dates (Ramadan, Ayyam al-Bid, Eid) have to open a separate app to know today's Hijri date.

## Why build it (gap analysis)

Islamic calendar support already exists on some devices and in some regions, and Android ships `android.icu.util.IslamicCalendar`. A full calendar app would duplicate that. The realistic product is a complementary widget + reminder app that reads the existing calendar and adds the Hijri layer, with a user-chosen calculation method (Umm al-Qura, civil, or local moon-sighting offset).

## Prior art & open bases to fork

- [xsoh/awesome-hijri](https://github.com/xsoh/awesome-hijri) — Curated list of Hijri libraries in every language.
- [alhazmy13/HijriDatePicker](https://github.com/alhazmy13/HijriDatePicker) — Android Hijri date picker component.
- [android.icu.util.IslamicCalendar](https://developer.android.com/reference/android/icu/util/IslamicCalendar) — Built-in Android API (API 24+) with civil, tabular and Umm al-Qura variants.

## Proposed architecture

1. Date engine: wrap `IslamicCalendar` with a per-user day offset, because Morocco and other countries start months by local moon sighting.
2. Widgets: Jetpack Glance widgets (small, medium, lock-screen) that show both dates.
3. Reminders: WorkManager jobs for white days, Ramadan, Eid and user events stored with a Hijri date.
4. Calendar bridge: read-only `CalendarContract` overlay so Samsung Calendar events show their Hijri date.

## Tech stack (proposed)

- Kotlin + Jetpack Compose
- Jetpack Glance (widgets)
- WorkManager
- android.icu IslamicCalendar

## Roadmap

- v0.1 — Hijri date engine with method choice + manual day offset.
- v0.2 — Home-screen and lock-screen widgets.
- v0.3 — Hijri reminders (white days, Ramadan, Eid, custom).
- v0.4 — Samsung Calendar overlay + Wear OS tile.

---

## For LLM agents & contributors continuing this project

This repo is a **scaffold**: README + standard files, no implementation yet. If you are an AI agent asked to "continue developing", read this section first.

- Never hard-code one calculation method: Morocco follows local moon sighting, so the offset setting is a core feature.
- Check the latest Samsung Calendar build first: if the Hijri layer is now available in Morocco, pivot to the widget only.

**Ground rules:** keep this README in sync with reality as you build; prefer forking/reusing the bases above over greenfield code; write tests for anything correctness-critical; never commit secrets (see `.gitignore`); follow Conventional Commits (see `CONTRIBUTING.md`).

## Getting started

```bash
# nothing to run yet — this is a scaffold.
```

## License

[MIT](LICENSE) — © 2026 Oussama EL HADJI
