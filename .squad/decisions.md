# Squad Decisions & Architecture Log

## Core Scope
### Project Expansion & Renaming
**Date:** 2026-04-18 | **Author:** Ajay Sainy
- **Decision:** Project renamed from ObjectDetection to GemOfGemma.
- **Details:** Scope expanded to an all-in-one on-device AI assistant covering object detection, image captioning, visual Q&A, OCR, on-device chat, voice commands, and full phone automation (SMS, calls, alarms, toggles, app launching).
- **Rationale:** Gemma 4's multimodal capabilities and function-calling allow a single model to handle all features via different prompts.

## Architecture
### Dependency Injection & Modules
**Date:** 2026-04-17 to 2026-04-18 | **Authors:** Jerry (Lead), Elaine (ML Engineer)
- **Hilt for DI:** Selected for compile-time validation of 8 modules with complex cross-module dependencies. (Jerry)
- **Interface-in-Core Pattern:** Created AiProcessor interface in :core. :ui ViewModels inject this interface, uncoupling them from the :ai service implementation. (Elaine)
- **Service Binding:** GemmaServiceConnector uses MutableStateFlow<GemmaService?> to track the bound service, surviving process death and reconnects better than CompletableDeferred. (Elaine)
- **Direct UI Dependencies:** :voice and :camera are direct dependencies of :ui to avoid over-engineering abstractions. (Elaine)

### Minimum SDK Requirements
**Date:** 2026-04-17 | **Author:** Jerry (Lead)
- **API 31:** minSdkVersion 31 (Android 12) is required.
- **Rationale:** Needed for SpeechRecognizer.createOnDeviceSpeechRecognizer() to ensure voice privacy, and required by LiteRT-LM.

## AI & Data
### Model & Runtime Selection
**Date:** 2026-04-17 | **Author:** Jerry (Lead)
- **Gemma 4 E2B:** Selected as the sole on-device model (2.58 GB, 2.3B params, 52 tok/s GPU) to handle all modalities and simplify architecture.
- **LiteRT-LM:** Chosen as the inference runtime (com.google.ai.edge.litertlm:litertlm-android). It is GA, supports GPU/CPU/NPU, and has excellent tool-calling support compared to AICore (preview) or MediaPipe (deprecated).
- **Model Hosting:** LifecycleService with oregroundServiceType="specialUse" keeps the 676 MB GPU memory and 10s init alive across activity bounds.

### Autonomous Tool Execution & Safety
**Date:** 2026-04-17 to 2026-04-18 | **Authors:** Jerry (Lead), Elaine (ML Engineer)
- **Silent Capabilities Expanded:** Added environment telemetry (light, motion, battery), network context, haptics, and system settings to PhoneActionToolSet for rich context without user friction. (Elaine)
- **Action Confirmation:** External-facing actions require explicit user confirmation. (Jerry)
- **AccessibilityService:** Deferred to Phase 4. Avoids Play Store rejection risk; app must remain fully functional without it. (Jerry)

## UI & Design
### UI Quality & Vision Overhaul
**Date:** 2026-04-18 | **Authors:** Ajay Sainy, George (Android Dev)
- **Production-Grade Directive:** UI must be polished, professional, and classy. "Not a high school student app." (Ajay)
- **Vision Screen Rewrite:** Complete transition to a Google Lens-quality experience for Object Detection, Image Captioning, OCR, and Visual Q&A. (George)
- **UI Architecture:** Replaced BottomSheetScaffold with custom FrostedBottomCard for overlaid panels. Result history maintained in ViewModel state (max 10 items) instead of a database for now. Category colors determined via keyword matching. (George)

### 2026-04-19: UI Deep Code Review by Peterman

## 1. Global Scaffolding & Layout Architecture
**Finding: Duplicate Headers / Visual Clutter**
- `NavGraph.kt` implements a global `Scaffold` with a `TopAppBar` (displaying "Gem of Gemma" and a Settings icon) and a `NavigationBar`.
- However, the child screens (`ChatScreen`, `VisionHubScreen`) implement their own secondary headers. For example, `ChatScreen` has a `Row` with `AnimatedGemIcon` and "GemOfGemma" text, and `VisionHubScreen` has a manual `Spacer(32.dp)` followed by an `Icon` and "Vision" text. 
- **Impact:** When rendered, the user will see a double-header setup (the Scaffold's app bar stacked on top of the screen's custom header). This eats up vertical real estate and introduces confusing visual hierarchy.
- **Recommendation:** Either remove the global `TopAppBar` from `NavGraph` and let each screen manage its own top bar (allowing for richer, screen-specific headers), or remove the custom headers from the child screens and inject their actions into the global top bar.

**Finding: Safe Areas & Insets**
- The `NavHost` correctly applies the Scaffold's `paddingValues` to ensure content isn't obscured by the global top/bottom bars.
- `ChatScreen` excellently applies `imePadding()` to its root column, which guarantees the chat input shifts up gracefully when the software keyboard appears.
- However, manual `Spacer` usage for vertical offset (like the 32.dp spacer at the top of `VisionHubScreen`) is brittle. If dynamic insets or custom top bars are used later, relying on fixed spacers can cause overlapping or awkward gaps across different device densities.

## 2. Screen-Specific Analysis

### ChatScreen
- **Strengths:** 
  - The Empty State is beautifully designed. The use of `FlowRow` for suggestion chips and the `AnimatedGemIcon` creates a welcoming onboarding feel.
  - Chat bubbles make great use of asymmetrical rounded corners (e.g., `bottomStart = 20.dp, bottomEnd = 6.dp` for user) to indicate message direction visually without relying solely on alignment.
  - The input bar is polished—nice touch with an overlaying Surface, rounded text field, and clean state toggles (Mic vs. Send) complete with scale and fade animations.
- **Areas for Polish:**
  - The `AnimatedVisibility` for `!isModelAvailable` shifts content down when it appears. Because it's inside the main column, the sudden layout shift might disrupt the reading flow. Consider wrapping it as an overlay or pinning it more cleanly.
  - Double "Gem of Gemma" top bar constraint as mentioned above.

### VisionHubScreen
- **Strengths:**
  - The visual execution of the hub cards is top-notch. Using interaction source properties to apply a bouncy `spring` scale down (to 0.93f) on press adds delightful tactile feedback.
  - Combining `Brush.verticalGradient` with subtle horizontal accent glow bars makes the grid look premium and engaging. 
- **Areas for Polish:**
  - Layout constraint: The 2x2 grid is built using nested `Row` and `Column` elements with `modifier.weight(1f)`. While this works, a `LazyVerticalGrid` could provide better scaling behavior on tablets or foldables if more tools are added.
  - Fix the double header. The "Vision" title overlaps semantically with the bottom navigation state.

### AudioHubScreen
- **Current State:** A simple placeholder `Box` and `Text`.
- **Recommendation:** Needs to be brought up to the design standards of `VisionHubScreen`, utilizing similar interactive cards, custom gradients, and "Plus Jakarta Sans" typography.

## 3. Typography and Theming
- **Font Choice:** Using `Plus Jakarta Sans` via Google Fonts is stellar. It reads universally clean, giving the app a distinct, modern identity. The `GemTypography` scales map perfectly to Material 3 tokens (`displayLarge`, `headlineSmall`, `bodyMedium`, etc.).
- **Hierarchy:** Both screens heavily utilize semantic typography (e.g., `titleLarge` for primary headers, `bodyMedium` for text, `labelMedium` for badges). Everything appears compliant mathematically.

## Summary Verdict
The UI is built with a very strong declarative React-style mindset, full of delightful micro-interactions (`AnimatedVisibility`, Spring animations) and robust theming. The single most critical issue to resolve is the **Double Top Bar layout**. Resolving that will transform the UI from "slightly cluttered" to flawless.


## Feature Showcase Exploration — May 7, 2026

> Multi-agent fan-out (Elaine, Peterman, George, Jerry) at Ajay's request: "what other features can we showcase?" The four entries below are the canonical decisions from that session. v1.1+ work must reconcile against the pillars and roadmap committed in Jerry's strategy memo below.

### 2026-05-07: Untapped Gemma 4 capability backlog & top-3 recommendation
**By:** Elaine (ML Engineer), requested by Ajay Sainy
**What:** Researched Gemma 4 + LiteRT-LM 0.11.0 capabilities not yet showcased in GemOfGemma. Produced ranked list of 10 feature ideas (full report in session). Top-3 recommendation: **(1) Babelfish** — speech translation via Gemma audio modality, (2) **Voice Memo Vault** — multi-clip audio summarization, (3) **Speed Boost (MTP)** — flip `ExperimentalFlags.enableSpeculativeDecoding = true` for ~2× decode speedup on GPU.
**Why:** All three high-Wow features are unlocked with minimal incremental work; #1 and #2 share plumbing (audio modality wiring) so a single engineering investment lights up multiple demos.

### 2026-05-07: Audio modality is one config field away — recommend prioritizing
**By:** Elaine (ML Engineer)
**What:** Verified via official Kotlin LiteRT-LM docs that Gemma 4 E2B audio is fully supported. Required code change: add `audioBackend = Backend.CPU()` (or GPU) to `EngineConfig` in `GemmaEngine.kt`, then send `Content.AudioBytes(bytes)` or `Content.AudioFile(path)` in any `Contents.of(...)`. Audio constraints: 16kHz mono float32 PCM, max 30s clips, 25 tokens/sec audio. Gemma 4 E4B benchmarks: FLEURS WER 0.08, CoVoST BLEU 35.5.
**Why:** Unlocks **at least 4** of our top-10 feature ideas (Babelfish, Voice Memo Vault, Sound Detective, The Polyglot) for the cost of one config field plus per-feature prompt+UI work.

### 2026-05-07: GPU init path in GemmaEngine.kt is broken — blocks MTP
**By:** Elaine (ML Engineer)
**What:** `GemmaEngine.initialize()` claims a GPU-then-CPU fallback strategy but the first try-block creates `Backend.CPU()`, not `Backend.GPU()`. The catch path also creates CPU. Net result: app is shipping CPU-only inference. Multi-Token Prediction (the 0.11.0 headline feature, ~2× decode on GPU) cannot be enabled until this is fixed because MTP requires GPU backend.
**Why:** Single highest-leverage fix in the codebase: correctly wiring GPU unlocks both ~50% baseline speedup AND the MTP doubling on top. Needs Jerry's input on backend-selection strategy (e.g., GPU with NPU preference, OpenCL availability check, telemetry on which backend wins for which device).

### 2026-05-07: Re-download required for MTP support
**By:** Elaine (ML Engineer)
**What:** The `litert-community/gemma-4-E2B-it-litert-lm` HuggingFace model file was updated on/around May 5, 2026, to embed the MTP draft model. Users who downloaded the model before May 5 must re-download to use speculative decoding. Should be flagged in the migration plan if/when we ship MTP.
**Why:** Avoid mysterious "MTP enabled but no speedup" reports from existing users.

### 2026-05-07: Android platform integration shortlist (George)
**By:** George (Android Dev) — requested by Ajay Sainy
**What:** Inventoried 10 Android platform integrations that expand the GemOfGemma showcase using the existing permission surface (or special-access grants that need no new `<uses-permission>`). Top 3 picks, ranked by demo value vs. effort:

1. **Sharesheet receiver (ACTION_SEND for `image/*` and `text/*`).** Effort: S. No new permission. Killer demo: long-press any photo in Gallery → Share → "Gem of Gemma" → instant captioning / OCR / VQA. This makes the model feel "always on" without launching the app. Highest impact-to-effort ratio in the entire list.
2. **Quick Settings tile (`TileService` + `BIND_QUICK_SETTINGS_TILE`).** Effort: S. No new permission. "Ask Gemma" tile in the system pulldown launches a transparent overlay activity prewired to voice or chat — same gesture as Google Assistant on stock Android. Use `requestAddTileService()` on first run for one-tap add.
3. **Glance widget — "Tap to summarize" / "Daily brief".** Effort: M. No new permission. Home-screen widget that triggers a Gemma run against today's clipboard / latest screenshot / last-shared text and shows a one-line summary. Real Gemma inference must stay in the bound foreground service; the widget just dispatches an intent to it.

**Defer / requires special access:**
- NotificationListenerService (notif summarization, music control via MediaSessionManager) — needs user-granted special access, but no new manifest permission. Strong "smart inbox" demo. Effort: M.
- Health Connect (READ_STEPS/SLEEP/HEART_RATE) — requires Play Console declaration + per-datatype permissions. High demo value (private wellness coach) but governance overhead. Effort: M.
- Predictive Back + Credential Manager — polish wins, no demo punch. Effort: S each.
- Companion Device Manager + BLE GATT (smart-home control via the already-declared BLUETOOTH_CONNECT) — high demo value, but device-dependent. Effort: L.
- MediaProjection partial screen capture (Android 14+ single-app capture) — "screen-aware AI" demo, but UX requires a per-session consent prompt. Effort: M.
- Tasker / App Actions intents (capabilities in `shortcuts.xml`) — lets power users wire Gemma into automations. No permission. Effort: S–M.

**Why:** Maximizes WOW per permission. Sharesheet, Tile, and Widget all ship with zero new permission asks and turn Gemma into a system-level participant rather than just another app. The deferred items either need special-access grants (which are demo-friendly but require an in-app onboarding flow) or are governance-heavy (Health Connect).

### 2026-05-07: Showcase / viral demo strategy
**By:** Peterman (UI Expert), requested by Ajay Sainy
**What:** Recommend a hero "Airplane Mode Concierge" demo (30 s) anchoring the Chat home screen, plus 8 ranked supporting features oriented around visual virality and the "no-internet" hook. Top supporting picks: (1) Live Camera Reasoning Overlay (real-time streaming captions floating on the camera preview), (2) Action Receipt Cards (animated confirmation cards every time a tool is called).
**Why:** Apple Intelligence and Google AI Edge Gallery dominate by leading with shareable visual moments. We need ONE hero demo (M3 Expressive guidance: stick to one or two hero moments) and a small set of micro-interactions that announce "AI is thinking / acting" so screenshots and screen recordings sell the app on social. The "Airplane Mode" framing is our defensible differentiation — shows offline + multimodal + tool-calling in a single shot.
**Constraints noted:**
- Stick to M3 Expressive motion physics tokens (spatial fast/default + effects spring) — no ad-hoc tween durations.
- Maximum one or two hero moments per surface (M3 guidance).
- All recommendations are UI-only; do not bake in dependencies on new AI/runtime work without a separate decision.
**Files of interest:**
- `app/src/main/java/com/gemofgemma/navigation/NavGraph.kt` (currently 4 routes: Chat, Capture, Settings, Onboarding — Vision/Voice tabs not wired in current code; any showcase work has to land in Chat first or add new routes).
- `ui/src/main/java/com/gemofgemma/ui/chat/ChatScreen.kt` (empty state + suggestion chips are the natural home for the hero demo entry).
- `ui/src/main/java/com/gemofgemma/ui/components/` (existing primitives: AnimatedGemIcon, ThinkingIndicator, FeatureChip, GlassmorphismCard, GradientButton — reusable for receipt cards / live overlays).
**Open questions:**
- Do we expand to a real Vision/Voice tab on the bottom nav, or keep everything inside Chat for now? Affects scope of #1 (Live Camera Reasoning Overlay).
- Hero demo ideally needs a scripted "demo mode" in the app for the README GIF — confirm if that's in scope.

### 2026-05-07: Showcase Strategy — Pillars, Roadmap & Anti-Patterns

**By:** Jerry (Lead Architect) — at request of Ajay Sainy
**What:** Strategic positioning and showcase pillar decisions for GemOfGemma post-v1.0.1.
**Why:** v1.0.0/v1.0.1 shipped; need a coherent narrative for what GemOfGemma *is* and what it should add next, framed against Apple Intelligence (closed, Apple-Silicon-only) and Google AICore (closed, Pixel/Samsung-only).

---

#### Decision 1 — Strategic Frame
GemOfGemma's positioning is the **open-source, hardware-portable, fully-offline** alternative to Apple Intelligence and Google AICore. Every feature decision should reinforce: *Apache 2.0, runs on commodity Android, zero data leaves the device, zero subscription.*

Quotable one-liner adopted for README/marketing:
> "Apple Intelligence and Google AICore decide which phone is smart enough. Gem of Gemma decides every Android is."

#### Decision 2 — Five Showcase Pillars (canonical)
Every future feature proposal must cleanly map to ONE of these. Features that don't map either get dropped or trigger a pillar revision (decision required).

1. **Talk** — Conversational AI on-device (chat, voice, thinking-mode). *Existing.*
2. **See** — Camera & image intelligence (object detection, OCR, VQA). *Existing.*
3. **Do** — Phone automation via native function calling. *Existing.*
4. **Read & Write** — *NEW.* On-device language tools (translate, summarize, rewrite, proofread, smart-reply).
5. **Remember** — *NEW.* Personal-data intelligence via on-device RAG (photos, notes, voice memos, screenshots) — never indexed by any cloud.

#### Decision 3 — Top 5 Feature Roadmap (ranked)
Ranked by vision alignment × demo impact × implementation feasibility on E2B.

1. **Live Translate (text + spoken captions)** — `Read & Write`. Reuse existing ASR + translation; bolt on Android system `TextToSpeech` for spoken output. **Effort: small. Impact: maximum.** Tentpole demo.
2. **Writing Tools (rewrite/proofread/summarize/smart-reply)** — `Read & Write`. System-wide via Android's `process_text` intent + share-sheet target. Pure prompt-engineering on existing capability. **Effort: small-medium. Impact: very high (sticky daily utility).**
3. **Smart Photo Search ("my dog at the beach last summer")** — `Remember`. Background indexer + Gemma 4 vision captions + local SQLite FTS. **Effort: medium. Impact: signature "magic" demo.**
4. **Voice Memory Capture (record → transcribe → summarize → searchable journal)** — `Talk` × `Remember`. Segment audio into 30s chunks (E2B ASR limit), reason over full transcript, store in on-device journal. **Effort: medium. Impact: uniquely defensible (no cloud journal can offer this).**
5. **Capture-to-Action ("Poster to Calendar", "Receipt to Expense", "Screenshot to Reply")** — `See` × `Do`. Single share-sheet target stitching vision + function-calling + SafetyValidator. **Effort: small-medium. Impact: high — "AI as glue between apps."**

#### Decision 4 — Anti-Patterns (do NOT showcase)
These features may exist quietly but must NEVER be a headline screenshot, README bullet, or marketing claim, because they highlight where on-device honestly loses to cloud:

- **Real-time news/weather/stocks/"what happened today"** — internet-dependent; the moment we demo it, the "100% offline" narrative breaks. Quiet tool-call only.
- **High-quality image generation (Genmoji-equivalent, Image Playground)** — diffusion on E2B-class hardware is slow/low-quality. Apple wins side-by-side. **Skip.**
- **Serious code assistant** — 29% LiveCodeBench v6 on E2B; will lose to Copilot/Cursor every time. Don't pitch.
- **Long-form enterprise document RAG (legal/medical/financial)** — modest OmniDocBench accuracy on E2B; wrong audience expectations. **Defer to E4B/26B variants.**
- **Continuous live video AR narration** — 2-3s/frame latency; Apple Visual Intelligence and Google Lens win. **Defer until NPU tuning.**
- **Always-listening voice agent** — no privacy-LED hardware story; looks creepy without trust signal. **Defer.**

#### Decision 5 — Architectural Foundations
- **Pillar-fit gate**: New feature proposals must declare their pillar at intake. No pillar fit → no merge.
- **"On-device or it's not us"**: A feature earns a screenshot only if it's genuinely better as on-device, OR composes cleanly with existing on-device features. Cloud-tool-only features do not earn README placement.
- **Demo-first build order**: Live Translate and Writing Tools are demoable in 60 seconds with no setup; build those first. Smart Photo Search needs indexer ramp-up; second wave.
- **Reuse before invent**: Live Translate = existing audio + existing translation prompts + Android free TTS. Writing Tools = existing chat + Android process_text intent. We need new **packaging** of existing capability, not new ML.

#### Open Questions for Ajay
- Confirm pillar names ("Read & Write", "Remember") for README — happy to bikeshed wording but the categories are the architectural commitment.
- Confirm Live Translate as v1.1 tentpole (vs. Writing Tools first).
- Confirm we're OK using Android system `TextToSpeech` for spoken output (free, on-device, but quality varies by OEM) vs. bundling Kokoro TTS (~80MB, consistent quality, more APK weight).
- Confirm Smart Photo Search scope: gallery only (MediaStore) for v1, or include screenshots and downloaded images too?

---

**Ranking criterion to lock in:** vision alignment × demo impact × implementation feasibility — in that priority order. A feasible-but-off-strategy feature loses to a stretch-but-on-strategy feature.
