## Core Context
Old entries summarized.

## Learnings
- 2026-04-19: Reviewed latest chat UI: The removal of the avatar and reduced top padding improves general density and screen real estate. The input bar gap and chat bubble padding still need subtle refinement for a truly production-grade classy feel to match the UI directive.

- 2026-04-19: Orchestrated background UI review (george-review2). Confirmed density improvements, but noted margins need padding.

- 2026-04-19: Orchestrated background UI review (george-review2). Confirmed density improvements, but noted margins need padding.

- 2026-05-07: Android platform integration audit. The current permission surface is far richer than the surfaced features — `ACTIVITY_RECOGNITION`, `BLUETOOTH_CONNECT`, `READ_CALENDAR`/`WRITE_CALENDAR`, `MODIFY_AUDIO_SETTINGS`, `WRITE_SETTINGS`, `ACCESS_NOTIFICATION_POLICY` are all declared but unused. Top opportunities (no new permission asks): Sharesheet receiver (image/text → instant Gemma analysis), Quick Settings tile (system-pulldown "Ask Gemma"), Glance widget (home-screen Gemma trigger). Each is S–M effort. Special-access wins (one-time user grant, no new manifest permission): NotificationListenerService for inbox summarization, MediaProjection for screen-aware AI. Health Connect is high demo value but adds Play Console governance overhead. Deliverable: `.squad/decisions/inbox/george-android-ideas.md`.

- 2026-05-07: Architecture note — widgets and tiles cannot host Gemma inference directly (cold-start cost + 676 MB GPU memory). They MUST dispatch intents to the existing `GemmaService` (LifecycleService with `foregroundServiceType="specialUse"`). The service is already designed for this — `GemmaServiceConnector` survives process death, so a tile tap that arrives before the service is up will queue cleanly.

- 2026-05-07: Permission policy reminder — `requestAddTileService()` (Android 13+) gives a one-tap "add to Quick Settings" dialog. Do NOT call it on every app open; call it once after the user discovers the feature in onboarding. Same pattern for `requestPinShortcut()`.

- 2026-05-07: NotificationListener gotcha — `BIND_NOTIFICATION_LISTENER_SERVICE` is the manifest permission, but the user-facing grant is via `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` (special access page, not the runtime permission dialog). On Android 12+ we can also use the per-listener filter types (`FLAG_FILTER_TYPE_CONVERSATIONS|ALERTING|SILENT|ONGOING`) declared in service `meta-data` so users see granular choices. MediaSessionManager.getActiveSessions() also rides on this same special access — one grant unlocks both notif summarization AND a "what's playing across all apps" feature.


## Team Updates

- 2026-05-07: Squad ran a four-agent showcase exploration (Elaine + Peterman + George + Jerry) at Ajay's request. Two new pillars proposed alongside the existing Talk/See/Do: **Read & Write** (translate, summarize, rewrite, smart-reply) and **Remember** (on-device RAG over photos, notes, voice memos). v1.1–v1.5 roadmap and anti-patterns now logged in .squad/decisions.md.