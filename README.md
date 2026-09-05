# OmniCode

### On-Device AI Assistant for Android — Powered by Gemma 4

<p align="center">
  <a href="https://github.com/abdulrehman958280-max/GemOfGemma/actions/workflows/build-apk.yml"><img src="https://img.shields.io/badge/Build%20APK-GitHub%20Actions-2088FF?logo=github-actions&logoColor=white" alt="Build OmniCode APK with GitHub Actions" /></a>
  <a href="https://github.com/abdulrehman958280-max/GemOfGemma/releases/latest"><img src="https://img.shields.io/badge/Download-Latest%20APK-34A853?logo=android&logoColor=white" alt="Download latest OmniCode APK" /></a>
</p>

![Android](https://img.shields.io/badge/Android-14%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![LiteRT-LM](https://img.shields.io/badge/LiteRT--LM-0.10.2-FF6F00?logo=google&logoColor=white)

An open-source Android app showcasing **on-device AI inference** with [Gemma 4](https://blog.google/technology/developers/gemma-4/) and [LiteRT-LM](https://ai.google.dev/edge/litert-lm). Chat, understand images, and control your phone — all running locally with **zero internet** after the initial model download. Entirely vibe coded with [GitHub Copilot](https://github.com/features/copilot).

No cloud APIs. No subscriptions. No data leaving your device. This is private, portable AI running on your phone's hardware.

> **Keywords:** Gemma 4, on-device LLM, Android AI, LiteRT-LM, offline AI assistant, on-device inference, Jetpack Compose, function calling, multimodal AI, object detection, OCR, image captioning, visual question answering, speech to text, phone automation, Material 3, Kotlin, open source

## 📱 Download APK

**Latest APK:** [Download from Releases](https://github.com/abdulrehman958280-max/GemOfGemma/releases/latest)

**Build a fresh APK:** [Run Build APK on GitHub Actions](https://github.com/abdulrehman958280-max/GemOfGemma/actions/workflows/build-apk.yml)

The **Build APK** workflow can be started manually with **Run workflow**. It builds the release APKs, publishes them to a GitHub Release, and also uploads them as a 30-day Actions artifact. The workflow uses the project's Gradle wrapper and JDK 17, matching the Android build configuration.

## 📦 Offline Model Catalog

The app now keeps models as **separate, verified downloads** instead of replacing a single hard-coded model file. Open **Settings → Models** to download, resume, delete, and switch between installed models.

| Model | Approx. size | Recommended RAM | Vision | Audio | Role |
|---|---:|---:|:---:|:---:|---|
| Gemma 4 E2B | 2.59 GB | 4 GB+ | ✓ | ✓ | Default / best Android balance |
| Gemma 4 E4B | 3.66 GB | 8 GB+ | ✓ | ✓ | Higher-capability option |

Only curated LiteRT-LM artifacts are exposed in the app. Arbitrary Hugging Face files are intentionally not accepted because a `.litertlm` extension alone does not guarantee compatibility with the Android runtime or this app's multimodal/tool pipeline.

Downloads support **HTTP resume**, storage preflight checks, per-model isolation, and **SHA-256 integrity verification** before an artifact becomes active. The selected model is persisted locally and installed models can be switched without re-downloading them.

## 📸 Screenshots

<p align="center">
  <img src="screenshots/chat-home.png" width="220" alt="OmniCode chat home screen with suggestion chips" />
  <img src="screenshots/chat-response.png" width="220" alt="Gemma 4 native function calling - set alarm tool" />
  <img src="screenshots/thinking-mode.png" width="220" alt="Gemma 4 thinking mode" />
</p>
<p align="center">
  <img src="screenshots/image-caption.png" width="220" alt="On-device OCR and image understanding with Gemma 4" />
  <img src="screenshots/tool-picker.png" width="220" alt="22 toggleable phone automation tools" />
</p>

## What It Can Do

- **Chat** — Natural conversation with real-time token streaming and visible thinking/reasoning, powered by Gemma 4 running entirely on-device
- **See** — Multimodal image understanding from camera or gallery: describe scenes, detect objects with bounding boxes, read text (OCR), answer visual questions
- **Control your phone** — 22 toggleable tools via LiteRT-LM's native ToolSet API: send SMS, make calls, set alarms, toggle flashlight, adjust volume/brightness, navigate, control media, and more
- **Voice input** — On-device speech recognition for hands-free interaction
- **Persistent conversations** — Chat history saved locally, multiple conversations supported
- **Model management** — Download and switch between curated LiteRT-LM models without losing the local model cache

## Getting Started

```bash
git clone https://github.com/abdulrehman958280-max/GemOfGemma.git
cd GemOfGemma
./gradlew installDebug
```

**Requirements:** Android Studio, JDK 17+, Android device with 4GB+ RAM, and enough free storage for the selected model.

On first launch, download a model from **Settings → Models**. After the model is installed and verified, inference can run fully offline.

## How It Works

The app uses [LiteRT-LM](https://ai.google.dev/edge/litert-lm) to run Google's Gemma 4 models directly on Android hardware. Key technical highlights:

- **Streaming inference** via `Conversation.sendMessageAsync()` — tokens appear in real-time
- **Native function calling** via LiteRT-LM's `ToolSet` API with `@Tool` annotations
- **Thinking mode** with `Channel("thinking")` — visible reasoning channel
- **Format-based response parsing** — model outputs `` ```json `` with `box_2d` for object detection (following [Google's official approach](https://ai.google.dev/gemma/docs/capabilities/vision/image))
- **Resumable model downloads** with SHA-256 verification and isolated per-model storage
- **Hot model switching** — the LiteRT-LM engine reloads the selected installed artifact
- **Multi-module architecture** — `:app`, `:ui`, `:ai`, `:core`, `:actions`, `:camera`, `:voice`, `:accessibility`

## Model License

The Gemma model is subject to the [Gemma Terms of Use](https://ai.google.dev/gemma/terms). This project's source code is [Apache 2.0](LICENSE).

## Contributing

Contributions welcome — open an issue first to discuss, then submit a PR.

## Acknowledgments

[Google DeepMind](https://deepmind.google/) (Gemma) · [Google AI Edge](https://ai.google.dev/edge) (LiteRT-LM) · [Jetpack Compose](https://developer.android.com/compose)
