# Intro audio capture milestone — 2026-11-07

The production `GameSession` now supports an injected headless PCM output and
reset-intro initialization. `IntroAudioCapture` captures its actual shared
`FrameAudioPlayer` output, decorates the already installed intro noise sink,
and traces APU register writes. It does not substitute a separate sequencer.
Diagnostic reflection is confined to the local capture harness.

The bounded no-input capture covers 3000 intro updates, including lightning
thunder and beach sea-wave onset. ROM SHA256:
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
The route definition `no-input;first-sea-stage;3000-frames` hashes to
`a92825b2e1cd8fec1f7afab7fcd294c94b8d6f106b56e95e6da360473ae9f199`.
Reference: PyBoy 2.8.1, CGB, built-in boot ROM, zeroed save RAM, sound enabled,
48 kHz stereo. Numerical evidence is in
[evidence/intro-audio-2026-11-07.json](evidence/intro-audio-2026-11-07.json).

## Frame origin and event results

Engine frame 0 is its first session update after `initializeHeadlessIntro`.
Emulator origin is tick 88, the first **post-tick** `wGameplaySubtype == 3`
(initial sea state). Thus engine frame 0 advances the initial state, whereas
emulator relative frame 0 observes that state. All 17 paired noise requests
are one frame earlier in engine labels, consistent with this origin convention.
There is no accumulating event offset in this bounded interval.

Both captures request eight thunder effects (noise 0C) followed by nine sea
waves (noise 0F). First thunder: engine 127 / emulator relative 128; first
wave: 1313 / 1314. Subsequent waves repeat every 160 updates through
2593 / 2594. Emulator hooks execute the actual ROM instructions at 01:70A9
(`RenderLightning.playSfx`) and 01:71D6 (sea-wave request), rather than infer
requests from audible energy or polling transient request bytes.

After the constant one-frame alignment, held NR42 and NR43 values each match
2871 of 2872 post-frame comparisons. The two mismatches are NR43 at engine
1065 (engine 6E, reference 6D) and NR42 at engine 1393 (67 versus 60).
These remain uncorrected: post-tick emulator snapshots do not establish the
complete CPU write sequence or instruction timing. NR44 readback masks trigger
bits and is not compared as a write trace. Intro scream/music and all other
registers during updates are captured but not individually verified here.
Initial music setup writes occur before the diagnostic observer is installed;
its PCM is included.

## PCM and clock results

Engine: 2,400,000 stereo sample frames (50.000 s). Emulator: 2,408,774
(50.182792 s). Engine uses exactly 800 samples/update. The hardware frame rate
is 4194304 / 70224 = 59.72750057 Hz; 3000 full hardware frames would yield
2,410,949.707 samples. The measured emulator interval includes ten shorter
LCD-transition ticks: sample counts are 1946×804, 1044×803, 5×757, and one
each of 482, 555, 484, 115 and 437. Consequently, the actual 8774-sample
difference is the nominal 10949.707-sample rate difference less 2175.707
samples of shortened-frame time (including fractional sample rounding).

Stereo PCM16 RMS is 2676.991 engine versus 2123.193 emulator. Per-frame RMS
correlation after one-frame alignment is 0.807189. This is envelope evidence,
not waveform parity: mixer amplitude, channel phase, filtering, and timing
within updates remain different or unverified. PyBoy signed PCM8 is promoted
losslessly to PCM16 for WAV storage. No synthesis or timing correction was made;
changing only samples/update would require reconciling the production 60 Hz
simulation and host clock as well as hardware LCD-off timing.

## Reproduction and local artifacts

From the repository root, with JDK 21 and the Python dependencies installed:

```sh
java/gradlew -p java -I ../tools/audio/capture.gradle captureIntroAudio \
  -PaudioOutput=/tmp/ladx-intro-audio -PaudioFrames=3000
python tools/audio/compare.py --output /tmp/ladx-intro-audio --frames 3000
```

The Java task honors `-Pla.rom` / `LA_ROM`; Python accepts `--rom`.
Use the same user-supplied ROM for both. The actual Python environment for
this run was `/tmp/ladx-visual-venv/bin/python`. Local output contains
`engine.wav`, `emulator.wav`, `engine.jsonl`, `emulator.jsonl`,
`comparison.json` and `envelopes.json`. Both WAVs begin at their declared
intro origins; the emulator JSONL also retains boot records. WAVs and traces
stay outside Git. Never publish the ROM-derived audio or image assets.

Focused verification passed 20 tests: `IntroSessionAudioTest` (1),
`IntroSequenceTest` (7), and `GameBoyApuTest` (12). The session test advances the real
session through thunder and first beach wave into the supplied output.
This bounded evidence establishes intro
request ordering and measured PCM differences; it does not establish full
sound, cutscene, or game parity.

## Exact noise write follow-up

`tools/audio/trace_noise_writes.py` resolves the two apparent held-register
mismatches using actual instruction hooks, not another post-frame snapshot.
It derives E0/E2 write sites from annotated audio disassembly and verifies
instruction bytes against the user ROM before installing hooks. Each noise
write records the instruction address, A/C register values, PyBoy CPU-cycle
counter and ordinal of the ROM `PlayActiveNoiseSfx` call (1F:64EC). The bounded
1600-frame run verifies 61 hook sites; it records 102 NR42/43/44 writes after
the first thunder. All 102 values and their order match the production trace.
Within each requested effect, every write also has exactly the same elapsed
number of noise-driver calls as engine updates (minimum/maximum delta both 0).
[Exact-write evidence](evidence/intro-noise-writes-2026-11-07.json).

The previous mismatch at engine 1065 is NR43=6E followed by NR44=00.
Emulator executes these at relative frame 1067, CPU cycles 157258520 and
157258556, via 1F:7A92. Previous NR43=6D occurred at audio call 1074;
6E occurs at 1081: exactly seven driver calls, matching engine 1058→1065.
The emulator frame labels span eight ticks (1059→1067), so aligning every
write by one constant displayed-frame offset was insufficient.

The engine 1393 NR42=67/NR43=03/NR44=80 group executes in the emulator at
relative 1395, cycles 202987116/202987152/202987188, through the same writer.
The preceding NR42=60 group is audio call 1363 and this group is 1411:
48 calls, matching engine 1345→1393. The displayed labels span 49 ticks.
Audio calls group variably into displayed ticks: for example, there are no
noise-driver entry calls at relative frames 1060/1061 and two at 1062. The
mismatch windows have ordinary 803/804-sample frames, so LCD-off shortening
is not established as their cause. The exact write trace establishes a
driver-call versus display-tick scheduling difference behind the two snapshot
mismatches; it does not identify the responsible CPU workload. There is no
demonstrated SFX sequencing or envelope-command bug
to compensate in production. CPU-cycle values describe the diagnostic emulator
clock; the engine does not model instruction-level execution time.

Reproduce after the ordinary engine capture:

```sh
python tools/audio/trace_noise_writes.py --frames 1600
```

It writes local `noise-writes.jsonl` and `noise-writes.summary.json` alongside
the WAVs. The hooks cover annotated E0/E2 instructions in the audio sources,
not arbitrary writes from every bank or a full memory-bus trace.

The next actionable PCM investigation is channel-isolated synthesis against
matched register commands and trigger times. NR50 is 77 and NR51 is FF in
both bounded captures, so a routing-volume difference is not established.
The measured mixed RMS ratio (engine/reference ≈1.261) cannot justify a
universal gain correction: the 60 Hz versus hardware clock difference,
noise/envelope phase, signed channel DAC behavior, and music-channel mixing
must first be separated. In particular, `NoiseChannel` advances its envelope
at a sample-driven 64 Hz and resets its envelope accumulator on trigger;
this has not been compared against the emulator's shared hardware frame
sequencer phase. No amplitude correction or hardware-phase equivalence is
claimed by the matching register-command trace.
