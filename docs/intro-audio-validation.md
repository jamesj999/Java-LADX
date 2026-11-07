# Intro audio trigger milestone

Source: intro.asm RenderLightning.playSfx (01:70A9) calls
PlayBombExplosionSfx (00:0C4B), requesting noise 0C. Both sea lightning
and face lightning use this path. Production now forwards these requests
through CutsceneManager to the session FrameAudioPlayer.

func_001_71C7 (01:71C7) increments the sea-wave counter only when entity
slot 1 state is below 2; at A0 it requests noise 0F and resets the counter.
Its callers are IntroStage6Handler and RenderIntroMarin, not the opening
sea handler. The modeled beach fade and Marin ticks now emit this request.

Validation: focused IntroSequenceTest passed, including a bounded 1040-tick
opening regression requiring explosion requests and no beach-wave requests.
ROM verified by Gradle: English DX v1.0 MD5 07c211479386825042efb4ad31bb525f.
Route: reset/default intro, no input; origin is IntroSequence construction.

Limits: this milestone verifies engine trigger routing, not emulator event
or PCM parity. No WAV captures, sample-count comparison, emulator version,
register timing trace or route hash have been collected here. The 800-sample
frame clock is unchanged. Exact emulator alignment, synthesis differences,
and beach counter phase remain unverified. No extracted assets are included.
