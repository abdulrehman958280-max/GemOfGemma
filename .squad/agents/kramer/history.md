# Kramer — History

## Project Context
- **Project:** GemOfGemma — All-in-one on-device AI assistant powered by Gemma 4
- **Stack:** Android (Kotlin), Jetpack Compose, CameraX, Gemma 4 E2B, LiteRT-LM, AccessibilityService, SpeechRecognizer
- **Features:** Object detection, image captioning, visual Q&A, OCR, on-device chat, voice commands, phone automation
- **User:** Ajay Sainy

## Learnings
- ADB-based screenshot testing is the primary testing method for this project. Use `adb shell screencap` + `adb pull` for visual verification and `uiautomator dump` for element inspection. See `testing-playbook.md` for full reference.

## Team Updates

- 2026-05-07: v1.0.2 shipped — GPU backend + MTP enabled with proper CPU fallback (commit 4e6b864, tag v1.0.2).
