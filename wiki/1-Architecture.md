# 1. Architecture

1. Date engine: wrap `IslamicCalendar` with a per-user day offset, because Morocco and other countries start months by local moon sighting.
2. Widgets: Jetpack Glance widgets (small, medium, lock-screen) that show both dates.
3. Reminders: WorkManager jobs for white days, Ramadan, Eid and user events stored with a Hijri date.
4. Calendar bridge: read-only `CalendarContract` overlay so Samsung Calendar events show their Hijri date.

## Proposed stack
- Kotlin + Jetpack Compose
- Jetpack Glance (widgets)
- WorkManager
- android.icu IslamicCalendar

## Open bases to fork
- [xsoh/awesome-hijri](https://github.com/xsoh/awesome-hijri) — Curated list of Hijri libraries in every language.
- [alhazmy13/HijriDatePicker](https://github.com/alhazmy13/HijriDatePicker) — Android Hijri date picker component.
- [android.icu.util.IslamicCalendar](https://developer.android.com/reference/android/icu/util/IslamicCalendar) — Built-in Android API (API 24+) with civil, tabular and Umm al-Qura variants.
