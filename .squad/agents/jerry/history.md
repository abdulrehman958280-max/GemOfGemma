# Jerry — History

## Project Context
- **Project:** GemOfGemma — All-in-one on-device AI assistant powered by Gemma 4
- **Stack:** Android (Kotlin), Jetpack Compose, CameraX, Gemma 4 E2B, LiteRT-LM, AccessibilityService, SpeechRecognizer
- **Features:** Object detection, image captioning, visual Q&A, OCR, on-device chat, voice commands, phone automation
- **User:** Ajay Sainy

## Learnings

### Architecture Design — April 17, 2026
- **Layered architecture chosen:** Presentation → Input → AI → Action → Feedback. Clear separation prevents AI layer from leaking into UI or action logic.
- **Foreground service for model hosting** is mandatory — 676 MB GPU memory + 10s init time means the model can't live in a ViewModel. Bound service pattern gives direct `process()` calls from ViewModels.
- **Function-calling via LiteRT-LM ToolSet API** is the linchpin for voice→action. Gemma 4's native tool-calling eliminates the need for a separate NLU layer.
- **SafetyValidator is non-negotiable** — AI-triggered SMS/calls must always go through confirmation dialog. No shortcutting this for "convenience."
- **AccessibilityService deferred to Phase 4** — high Play Store rejection risk. App must be fully functional without it (Phases 1-3). Sideload variant can include it.
- **minSdkVersion 31** — driven by `SpeechRecognizer.createOnDeviceSpeechRecognizer()`. Sacrifices ~15% of Android install base but gains on-device voice privacy.
- **Token budget is the key latency/accuracy tradeoff** — 70 tokens for fast classification, 560 for detection, 1120 for OCR. Must be configurable per vision mode.
- **Hilt over Koin** — 8 modules with cross-cutting dependencies. Compile-time graph validation prevents runtime DI crashes.
- **Module ownership split:** George owns UI/Camera/Voice (user-facing), Elaine owns AI/Actions/Accessibility (system-facing). Clean boundary.
- **Biggest risk:** Play Store rejection for AccessibilityService + foreground service `specialUse` type. Must prepare justification narratives early.
- **Model download is the #1 onboarding friction** — 2.58 GB download before first use. WorkManager with resume support and clear progress UI is critical. Future AICore integration could eliminate this entirely.

### UX Audit — April 18, 2026
- **"Cosmetic completeness" is a trap.** Settings screen has Dark Mode toggle and Accessibility switch that look real but do nothing. Hardcoded permission states show "Not granted" even after granting. Users lose trust when UI lies — worse than missing the feature entirely.
- **Prototype gaps cluster in three areas:** (1) missing feedback loops (no error display, no copy, no haptics), (2) visual lies (fake toggles, hardcoded states), (3) absent standard features (timestamps, markdown rendering, scroll-to-bottom).
- **Biggest single UX uplift = markdown rendering in chat.** Raw asterisks and backticks in AI responses is the clearest "this is a prototype" signal. A Compose markdown renderer is 3-4 hours of work for massive perceived quality improvement.
- **Camera has functional bones but dead interactions.** Shutter scale animation is a no-op (target always 1f), Copy/Share buttons are TODO stubs visible to user, bounding boxes use hardcoded 1000×1000 dims. These aren't design problems — they're unfinished code wearing a finished UI.
- **Font loading via GoogleFont.Provider is fragile.** Depends on GMS availability. Need bundled fallback for non-GMS devices or the entire visual identity breaks.
- **35 total findings: 8 P0, 13 P1, 14 P2.** P0s are fixable in ~5-6 hours. Full audit saved to `.squad/decisions/inbox/jerry-ux-audit.md`.


## Cross-Agent Update (20260419T044949Z)
Elaine has researched Android background tools (ambient sensors, device health, clipboard, haptic) requiring no user action and proposed new additions to PhoneActionToolSet. Please check the updated command matrix and decisions for details.

## Cross-Agent Update
Elaine proposed new additions to PhoneActionToolSet based on background actions (sensors, clipboard, etc.). Checked updated matrix and decisions.

- 2026-04-19: George reviewed UI screenshot. Awaiting fixes for chat bubble spacing and input bar padding.

### Unified Chat Architecture Plan — April 19, 2026
- **Decision:** Overhaul from 3-tab (Chat/Vision/Audio) to single unified chat interface. Plan written to `.squad/decisions/inbox/jerry-chat-overhaul-plan.md`.
- **Key architectural choice:** Inline vision within the persistent LiteRT-LM conversation (Option A) rather than side-channel one-shot conversations. Rationale: user expects image Q&A to flow naturally within chat context. Token cost (~256/image) manageable with existing context reset mechanism.
- **Single-conversation constraint** is the #1 risk. LiteRT-LM only supports one conversation — vision was previously one-shot (destroying chat context). New design keeps everything in one persistent conversation.
- **Migration is 5 phases:** data layer → chat UI (attachments + rich bubbles) → nav overhaul → cleanup → polish. No new module dependencies needed; `:ui` already depends on `:camera`.
- **What's deleted:** VisionHubScreen, AudioHubScreen, CameraScreen/ViewModel/UiState, bottom NavigationBar, global TopAppBar, `AiRequest.VoiceCommand`.
- **What's added:** `AiRequest.VisionChat`, `ChatMessage.imageBytes/detections/ocrBlocks/messageType`, image attachment UI in chat input, `ImageCaptureScreen` (lightweight camera-to-chat), inline `BoundingBoxOverlay` in chat bubbles.
- **What's kept:** `CameraManager`, `BoundingBoxOverlay`, all parsers, `ActionDispatcher`, `VoiceRecognizer`.
- **Open questions for Ajay:** Chat header style, conversation persistence across restarts, multi-conversation support, image handling during conversation recovery.

### Showcase Strategy Memo — May 7, 2026
- **The strategic frame is "the open alternative to Apple Intelligence + Google AICore."** Not "yet another local LLM chat app." Apple/Google have decided which phones are smart; we decide every Android is. Use this exact framing in marketing.
- **Five-pillar architecture committed**: Talk, See, Do (existing) + Read & Write, Remember (NEW). Every feature proposal must declare its pillar at intake. No pillar fit → no merge. Decision file: `.squad/decisions/inbox/jerry-showcase-strategy.md`.
- **The biggest gaps in the current showcase are Read & Write and Remember.** Apple Intelligence's tentpoles (Live Translate, Writing Tools, Smart Photo Search) all sit in these pillars and we have *zero* presence today.
- **Reuse > invent**: Live Translate, Writing Tools, and Capture-to-Action are *all* repackaging of capabilities we already ship. No new ML required — only new UX surfaces (system TTS, share-sheet target, Android process_text intent). This is the highest-leverage near-term work.
- **Live Translate is the single biggest demo win.** ASR + translation already work; bolt on Android `TextToSpeech` and we have the Apple Intelligence tentpole running airplane-mode on a Pixel 6. Effort small, impact maximum.
- **Smart Photo Search is the signature "magic" demo** but requires a background indexer + permissions + UI — it's the medium-effort feature, second-wave.
- **Anti-patterns are as important as features**: real-time news, image generation, serious code assistant, enterprise doc RAG, live AR video narration, always-listening voice agent. These are where on-device honestly loses to cloud — they make us look weaker, not stronger. NEVER headline them.
- **Demo-first build order**: features demoable in 60 seconds with no setup go first (Live Translate, Writing Tools). Indexer-dependent features go second wave (Smart Photo Search, Voice Memory).
- **Open questions for Ajay** to lock the v1.1 plan: which tentpole first (Translate vs. Writing Tools)? System TTS vs. bundled Kokoro? Smart Photo Search scope (gallery only vs. screenshots+downloads)?
- **Competitor scan**: NativeMind (browser ext.), parlor (desktop Gemma 4 + Kokoro), claude-code-local (airgap macOS), MLC-LLM (runtime, not assistant), Mano-P (GUI agent macOS) — none are an end-user *Android* assistant covering all five pillars. Our position is uncrowded if we move fast.


## Team Updates

- 2026-05-07: Squad ran a four-agent showcase exploration (Elaine + Peterman + George + Jerry) at Ajay's request. Two new pillars proposed alongside the existing Talk/See/Do: **Read & Write** (translate, summarize, rewrite, smart-reply) and **Remember** (on-device RAG over photos, notes, voice memos). v1.1–v1.5 roadmap and anti-patterns now logged in .squad/decisions.md.
- 2026-05-07: v1.0.2 shipped — GPU backend + MTP enabled with proper CPU fallback (commit 4e6b864, tag v1.0.2).