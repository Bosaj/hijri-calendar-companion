# 2. Roadmap

- v0.1 — Hijri date engine with method choice + manual day offset.
- v0.2 — Home-screen and lock-screen widgets.
- v0.3 — Hijri reminders (white days, Ramadan, Eid, custom).
- v0.4 — Samsung Calendar overlay + Wear OS tile.

## Notes for contributors / LLM agents
- Never hard-code one calculation method: Morocco follows local moon sighting, so the offset setting is a core feature.
- Check the latest Samsung Calendar build first: if the Hijri layer is now available in Morocco, pivot to the widget only.
