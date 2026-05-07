# History: Peterman

**Project Context:**
GemOfGemma - an Android application using Gemma models via LiteRT/LlmInference.
Team is working on refining the UI across Chat, Vision, and the newly added Audio tabs.

## Learnings
* 2026-04-19: Joined the team to take over UI/UX design duties from George, ensuring peak Compose and Material 3 quality.

* 2026-04-19: Reviewed George's baseline UI screenshot. Diagnosed missing Material 3 conventions, typography, and spacing issues to establish a baseline for the upcoming UI overhaul.

* 2026-04-19: **Vision tab deep review (Caption result state on real device).** Key findings:
  - **P0 — Caption text clips without scroll.** `CaptionResultPanel` in `CameraScreen.kt:823-890` has no scrollable container. `FrostedBottomCard` with `maxHeightFraction=0.35f` + `padding(bottom=100.dp)` physically cannot display long captions.
  - **P0 — Retake button WCAG failure.** White text on `#00E676` green = ~1.7:1 contrast. Must use dark text or darker green.
  - **P1 — Share icon 36dp violates M3 48dp touch target.** Also opacity 0.7f looks disabled.
  - **P1 — FrostedBottomCard uses magic-number 100dp bottom padding.** Brittle across devices.
  - **P1 — Retake button is raw Surface+clickable, not M3 Button.** Missing ripple + accessibility role.
  - The frosted glass effect, typewriter animation, and mode accent system are genuinely premium patterns worth preserving.
  - Key files: `CameraScreen.kt` (all result panels), `NavGraph.kt` (bottom nav + scaffold), `Color.kt` (mode accents), `Theme.kt` (M3 scheme).
  - Decision logged: `.squad/decisions/inbox/peterman-vision-ui-review.md`

* 2026-05-07: **Showcase / viral demo brainstorm post-v1.0.1.** Generated ranked list of 8 demo-worthy features and a flagship "Airplane Mode Concierge" hero demo for the home screen. Key context discovered while doing this:
  - **Current code reality is leaner than docs suggest.** `NavGraph.kt` only has 4 routes: Chat, Capture, Settings, Onboarding. There is no VisionHubScreen / AudioHubScreen / CameraScreen in the active `:ui` module. The README's "Chat / See / Control / Voice" pillars are aspirational — vision lives inside Chat (image capture → ask). Any showcase work either rides Chat or needs new routes.
  - **Existing UI primitives to reuse:** `AnimatedGemIcon` (rotating hexagon w/ pulse + tri-color gradient — great for "AI active" cue), `ThinkingIndicator` (3 bouncing dots — already nailed), `FeatureChip` (suggestion chips on empty state), `GlassmorphismCard`, `GradientButton`. The gem icon is the brand mark — reuse it ruthlessly across hero moments.
  - **Inspiration that mapped:** Google AI Edge Gallery's "Audio Scribe", "Mobile Actions", and "Tiny Garden" patterns; Apple Intelligence's Image Wand (circle to ask), Visual Intelligence (poster → calendar event), and Smart Reply receipt-style UI; M3 Expressive's "stick to one or two hero moments" rule and motion physics spring tokens (spatial vs effects, fast/default/slow).
  - **The defensible viral hook is "Airplane Mode".** Apple and Google both lean into "on-device privacy" copy; nobody is leading with a visible "internet OFF" badge during the demo. That's our shot.
  - **Top viral picks (ranked):** Hero = Airplane Mode Concierge demo. Supporting #1 = Live Camera Reasoning Overlay (Lens-style streaming captions on live preview). Supporting #2 = Action Receipt Cards (every tool call → animated confirmation card with the tool's icon).
  - Decision logged: `.squad/decisions/inbox/peterman-showcase-ideas.md`


## Team Updates

- 2026-05-07: Squad ran a four-agent showcase exploration (Elaine + Peterman + George + Jerry) at Ajay's request. Two new pillars proposed alongside the existing Talk/See/Do: **Read & Write** (translate, summarize, rewrite, smart-reply) and **Remember** (on-device RAG over photos, notes, voice memos). v1.1–v1.5 roadmap and anti-patterns now logged in .squad/decisions.md.