## Core Context
Old entries summarized.

## Learnings

### 2026-05-07 — v1.0.2 patch landed: GemmaEngine GPU + MTP fix
- Fixed `GemmaEngine.kt` so the first init branch now uses `Backend.GPU()` (was `Backend.CPU()` — bug from prior code).
- MTP is opt-in via `@OptIn(ExperimentalApi::class)` + `ExperimentalFlags.enableSpeculativeDecoding = true`, set BEFORE `EngineConfig` construction. Imports added for `ExperimentalApi` and `ExperimentalFlags`.
- CPU catch-block remains genuine fallback (still `Backend.CPU()`); log messages corrected ("GPU init failed, falling back to CPU" instead of misleading "CPU init failed").
- **Vision backend kept on `Backend.CPU()` in BOTH paths** — scoped change. GPU vision is parked as a separate optimization for a later patch.
- `:ai:assembleDebug` build is green.

### 2026-05-07 — LiteRT-LM 0.11.0 Multi-Token Prediction (MTP) — verified
- **What it is:** New decode-speed optimization shipped in LiteRT-LM 0.11.0 (released May 6, 2026). Uses a small "draft" model embedded in the `.litertlm` file to predict multiple upcoming tokens; the main model verifies them in parallel. >2× decode speedup on GPU with zero quality loss. The HuggingFace model card (`litert-community/gemma-4-E2B-it-litert-lm`) labels this as "Speculative Decoding".
- **API surface (Kotlin):**
  ```kotlin
  @OptIn(ExperimentalApi::class)
  ExperimentalFlags.enableSpeculativeDecoding = true  // BEFORE engine init
  val engineConfig = EngineConfig(modelPath = "...", backend = Backend.GPU())
  Engine(engineConfig).initialize()
  ```
- **Constraints:** GPU only on mobile (also works on CPU on desktop). Requires re-downloading model files dated after May 5, 2026 (older files lack the draft weights).
- **S26 Ultra benchmarks (E2B GPU):** baseline 51.5 tok/s → summarize 91.7, code 84.4, rewrite tone 87.4, free-form 66.5. Effectiveness is task-dependent.
- **Source:** https://ai.google.dev/edge/litert-lm/android (#-new-multi-token-prediction-mtp section), https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm

### 2026-05-07 — Gemma 4 E2B audio modality — fully supported in LiteRT-LM Kotlin API
- `EngineConfig` now accepts `audioBackend = Backend.CPU() | Backend.GPU() | Backend.NPU(...)`. Without it, audio content fails.
- `Content.AudioBytes(byteArray)` and `Content.AudioFile(path)` are the audio inputs; can be combined with `Content.ImageFile`/`ImageBytes` and `Content.Text` in a single `Contents.of(...)` call (true tri-modal).
- **Audio format:** 16kHz mono, float32 PCM, samples normalized to [-1, 1]. Max clip length 30 seconds. 25 tokens per second of audio (E2B/E4B); 6.25 tok/s for older Gemma 3n.
- Multilingual ASR + speech translation (AST) trained into both E2B and E4B (NOT in 31B Dense or 26B A4B MoE — those are vision-only).
- **Use cases proven in official docs:** ASR (single clip transcription), AST (speech-to-translated-text), multi-clip audio summarization (5-clip journal example shown in Gemma 4 audio cookbook), general audio understanding ("describe what you hear").
- **Codebase status:** GemOfGemma's `EngineConfig` in `GemmaEngine.kt` does NOT set `audioBackend`. Voice currently uses Android `SpeechRecognizer.createOnDeviceSpeechRecognizer()` and discards the raw audio — switching to Gemma audio is the unlock for ~4 high-value features.
- **Source:** https://ai.google.dev/gemma/docs/capabilities/audio, https://ai.google.dev/edge/litert-lm/android (Multi-Modality section)

### 2026-05-07 — Gemma 4 vs Gemma 3n on LiteRT-LM
- Gemma 4 E2B (2.58 GB) is strictly better than Gemma 3n E2B (~2.97 GB) on every dimension — same modalities, smaller file, better benchmarks, MTP support, FunctionGemma compatibility for tool calling.
- Gemma 4 E4B exists (~3.65 GB, 4.5B effective params, same 128K context, same modalities). Worth offering as a "high quality" option for devices with 6 GB+ RAM, but not the default.
- The 31B Dense and 26B A4B MoE variants are NOT mobile-targets (58 GB BF16 and 48 GB BF16 respectively).
- **Decision:** Stick with Gemma 4 E2B as default; consider E4B as advanced setting later. Switching off Gemma 3n is a non-issue (we never used it).

### 2026-05-07 — FunctionGemma 270m exists for tool-call-only workflows
- Google released `google/functiongemma-270m-it` — a tiny dedicated model for function calling. Used in Google AI Edge Gallery's "Mobile Actions" and "Tiny Garden" features.
- Useful as a sidecar architecture: tiny model handles instant phone actions, big model handles everything else. Lower latency, lower memory.
- LiteRT-LM Kotlin docs explicitly call out: "tools only works with models with tool support, e.g., FunctionGemma."
- **Verdict for GemOfGemma:** NOT prioritized — Gemma 4 E2B already does function calling well enough. Two-model architecture is heavy lift for marginal gain. Park for future consideration.
- **Source:** https://huggingface.co/google/functiongemma-270m-it

### 2026-05-07 — Google AI Edge Gallery feature reference (good benchmark)
The official Google sample app currently showcases:
- **Agent Skills** (loadable from URL — Wikipedia fact-grounding, maps, visual summary cards)
- **AI Chat with Thinking Mode** ✅ (we have this)
- **Ask Image** ✅ (we have this — our "See & Understand")
- **Audio Scribe** — ASR + translation in real-time ❌ (we don't — equivalent to Babelfish proposal)
- **Prompt Lab** — temperature/topK playground for power users ❌
- **Mobile Actions** — phone control via FunctionGemma 270m ✅ (we have a different model but same concept — our "Control Your Phone")
- **Tiny Garden** — natural-language mini-game ❌ (cute, low priority)
- **Model Management & Benchmark** — load custom models, benchmark comparison ❌

### 2026-05-07 — Architectural snags found in code review
1. **GPU init is broken in `GemmaEngine.kt`:** the "GPU" try-block constructs `Backend.CPU()` not `Backend.GPU()`. The catch path also constructs CPU. Net effect: GPU is never actually attempted. Blocks MTP entirely (requires GPU backend on mobile).
2. **`audioBackend` not set:** sending audio content today would fail. One config field unlocks all audio features.
3. **Single-conversation limitation:** LiteRT-LM allows only one active `Conversation` per engine — already handled in our `GemmaEngine.kt` via `closeActiveConversation()`. Long-context and live-streaming features need explicit reset cadence design.

### 2026-05-07 — Top-3 feature recommendations for next sprint
1. **Babelfish** — Speech translation via Gemma audio (M effort, Wow 5)
2. **Voice Memo Vault** — Multi-clip audio summarization (M effort, Wow 5)
3. **Speed Boost (MTP)** — Single-line flag flip + GPU-init fix (S effort, Wow 4)

Full ranked list of 10 ideas captured in the session report and `.squad/decisions/inbox/elaine-feature-ideas.md`.


## Team Updates

- 2026-05-07: Squad ran a four-agent showcase exploration (Elaine + Peterman + George + Jerry) at Ajay's request. Two new pillars proposed alongside the existing Talk/See/Do: **Read & Write** (translate, summarize, rewrite, smart-reply) and **Remember** (on-device RAG over photos, notes, voice memos). v1.1–v1.5 roadmap and anti-patterns now logged in .squad/decisions.md.
- 2026-05-07: v1.0.2 shipped — GPU backend + MTP enabled with proper CPU fallback (commit 4e6b864, tag v1.0.2).