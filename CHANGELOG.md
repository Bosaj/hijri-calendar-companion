# Changelog

All notable changes to this project are documented here. Format: [Keep a Changelog](https://keepachangelog.com/), versioning: [SemVer](https://semver.org/).

## [Unreleased]

## [0.2.0] - 2026-11-06
### Added
- Android app (Kotlin, Compose, Material 3): today's Hijri and Gregorian dates, a month view with white days and occasions highlighted, and the coming occasions with countdowns.
- Date engine with two methods, Umm al-Qura (1356–1500 AH, from a table generated from ICU) and the civil calendar, plus a day offset from −2 to +2. Tested against 407 days converted by ICU, and against the dates printed in Morocco's Bulletin Officiel.
- Home-screen widget (Glance) with both dates and today's occasion, refreshed after midnight and on clock or time-zone changes.
- Evening reminders (WorkManager) for white days, Monday and Thursday fasts, and occasions. Fasting reminders skip Ramadan, the Eids and Tashriq. Reminders for moon-dependent dates ask to confirm the official announcement.
- Arabic, French and English. No internet permission.
- CI builds the app, runs unit tests and Android lint, and builds the minified release APK.

## [0.1.0] - 2026-10-18
### Added
- Initial repository scaffold: README (with architecture, roadmap and an LLM-handoff section), LICENSE, .gitignore, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, issue/PR templates, and wiki pages.
