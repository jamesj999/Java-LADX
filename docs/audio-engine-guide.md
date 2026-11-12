# Audio Engine Guide

This project is an open source engine reimplementation for preservation of
*The Legend of Zelda: Link's Awakening DX* and research into how it works.
Users must supply their own ROMs to use it. The engine must not bundle or
redistribute the original game, ROMs, or extracted original game assets.

This guide is for future agents working on the Link's Awakening DX audio code. It describes the current music/SFX implementation, how the pieces fit together, and the safest way to extend it.

The browser retains its sample-driven `OpenAlMusicPlayer` transport. Its
pause/resume, looping, volume, channel meters and device-status controls go
beyond the frame-driven `FrameAudioPlayer` API. Moving it requires preserving
those controls and buffering timing; gameplay continues to use the shared PCM
output through `FrameAudioPlayer`.

## Current Scope

The implemented slice is ROM-driven music playback, ROM-backed SFX playback for gameplay and dialog events through the shared `FrameAudioPlayer` transport, and a standalone Swing browser. It is not a complete game audio system yet.

Included:

- Music catalog extraction from the user-supplied ROM.
- A Java reconstruction of the music driver command stream.
- A software Game Boy APU model for channels 1-4.
- LWJGL OpenAL streaming for host playback.
- A standalone AWT/Swing browser launched by Gradle.
- Dialog/messagebox and gameplay SFX routed through the real `SoundEffectPlayer` path, sharing one APU and PCM output with music (see the end of this guide).
- SFX pointer-table reads and bank `0x1F` SFX routine execution from the user-supplied ROM.

Not included yet:

- Gameplay SFX coverage for every ROM event (only events wired by the engine so far are emitted).
- Jingle/SFX priority and channel stealing.
- CPU-level execution of the original music routines.
- Perfect analog filtering or hardware speaker coloration.

## Source Of Truth

Use the ROM and the LADX disassembly before guessing.

Primary disassembly files:

- `LADX-Disassembly/src/code/audio/music_1.asm`
- `LADX-Disassembly/src/code/audio/music_2.asm`
- `LADX-Disassembly/src/code/audio_macros.asm`
- `LADX-Disassembly/src/data/music/music_tracks_data_1b_*.asm`
- `LADX-Disassembly/src/data/music/music_tracks_data_1e_*.asm`
- `LADX-Disassembly/tools/generate_music.py`
- `LADX-Disassembly/src/constants/sfx.asm`

Runtime ROM:

- `java/src/main/resources/rom/azle.gbc`

Banked ROM addresses use the project convention:

```text
offset = bank * 0x4000 + (address - 0x4000)
```

In Java, use `RomBank.romOffset(bank, address)` rather than duplicating this math.

## Package Map

The audio implementation lives under `java/src/main/java/linksawakening/audio`.

`linksawakening.audio.music`

- `MusicCatalog`: builds the selectable music track list from ROM pointer tables.
- `MusicTrack`: immutable metadata for one track.
- `MusicOpcode`: opcode classification helpers.
- `MusicDriver`: sequences ROM music commands at 60 Hz.
- `MusicVoiceEmitter`: emits channel registers, waveforms and timed articulation.
- `FrameAudioMixer`: native-free browser PCM mixing and sample clock.
- `MusicChannelState`: mutable per-channel driver state.

`linksawakening.audio.apu`

- `GameBoyApu`: register file, routing, master volume, and PCM mixing.
- `SquareChannel`: channel 1 and 2 synthesis.
- `WaveChannel`: channel 3 wave RAM synthesis.
- `NoiseChannel`: channel 4 LFSR noise synthesis.

`linksawakening.audio.openal`

- `OpenAlMusicPlayer`: browser transport controls.
- `OpenAlStream`: native device/context/source, reusable streaming storage and buffer pool.

`linksawakening.audio.sfx`

- `SoundEffectCatalog`: selectable SFX metadata and public ids.
- `SoundEffectPlayer`: 60 Hz SFX playback facade.
- `RomSoundEffectTables`: reads SFX handler pointers from ROM bank `0x1F`.
- `RomSoundEffectEngine`: ROM memory/register bus, requests and channel ownership.
- `SfxCpu`: one cohesive ROM instruction interpreter owning private registers,
  reusable stack, flags and ALU operations. There is no interpreter superclass.

`linksawakening.audio.browser`

- `AudioBrowserMain`: standalone entry point.
- `AudioBrowserFrame`: package-local Swing view with its own private panel assembly;
  `AudioBrowserMain` remains the public launcher entry point.
- `MusicBrowserModel` / `BrowserTransportState`: headless search and transport state.
  The frame uses the standalone models directly; former nested aliases were removed.

Gradle wiring:

- `java/build.gradle` adds LWJGL OpenAL dependencies and the `runAudioBrowser` task.

## Runtime Flow

The browser starts in `AudioBrowserMain`.

1. Load the user ROM into `Rom.of(bytes)` at the input boundary.
2. Build `MusicCatalog.fromRom(rom)`.
3. Construct `GameBoyApu` at 48 kHz.
4. Construct `MusicDriver(rom, apu)`.
5. Construct `OpenAlMusicPlayer(driver, apu)`.
6. Show `AudioBrowserFrame`.

When the user presses Play:

1. `AudioBrowserFrame` calls `OpenAlMusicPlayer.play(track)`.
2. `OpenAlMusicPlayer` stops any current playback, starts the `MusicDriver`, and pre-fills OpenAL buffers.
3. `MusicDriver.start(track)` reads the track header and channel definition pointers.
4. `MusicDriver.tick60Hz()` interprets stream commands and writes Game Boy sound registers to `GameBoyApu`.
5. `FrameAudioMixer.renderBufferPcm()` renders PCM from `GameBoyApu`.
6. The player advances the driver at exactly 60 Hz based on rendered sample frames, not wall-clock time.
7. PCM is queued into OpenAL as stereo 16-bit buffers.

This separation is intentional. The music driver owns sequencing; the APU owns sound generation; OpenAL only owns host playback.

## Music Catalog

`MusicCatalog` mirrors `tools/generate_music.py` track-id transformations.

Bank `0x1B`:

- Pointer table starts at `0x4077`.
- Input ids `0x01..0x10` map to public ids `0x01..0x10`.
- Input ids `0x11..0x20` map to public ids `input + 0x20`.
- Input ids `0x21..0x30` map to public ids `input + 0x40`.

Bank `0x1E`:

- Pointer table starts at `0x407F`.
- Input ids `0x01..0x20` map to public ids `input + 0x10`.
- Input ids `0x21..0x40` map to public ids `input + 0x20`.

Do not flatten these into a single guessed table. Track ids, banks, pointer addresses, and header addresses should remain visible because they are useful for debugging.

## Music Driver

`MusicDriver` is a direct Java reconstruction of the observable music driver behavior. It is not an SM83 emulator.

Track header layout:

- control byte
- speed table pointer
- channel 1 stream pointer
- channel 2 stream pointer
- channel 3 stream pointer
- channel 4 stream pointer

Each active channel has a stream of definition pointers. A definition then contains opcodes and notes. `0x0000` ends a channel stream. `0xFFFF` loops to another stream pointer.

Core opcodes currently handled:

- `0x00`: end current channel definition and load the next definition.
- `0x01`: rest for the currently selected length.
- `0x94..0x9A`: driver flags, currently recognized and skipped for this music slice.
- `0x9B`: begin loop with repeat count.
- `0x9C`: loop or continue.
- `0x9D`: set square envelope/duty on channels 1-2, or wave RAM/output level on channel 3.
- `0x9E`: set speed table pointer.
- `0x9F`: set transpose.
- `0xA0..0xAF`: select note length from the speed table.
- `0x02..0x90`: pitched notes on channels 1-3.
- channel 4 noise notes use the noise table path, with `0xFF` as a special two-stage `NOISE_FF` sequence.

Important details:

- Pitched-note frequency values come from the mirrored `SquareAndWaveFrequencyTable`.
- Frequency table indexing advances one entry per two pitched opcodes.
- Rests silence the channel but must not permanently erase later note setup.
- Channel 4 normal noise notes use `NOISE_FREQUENCY_TABLE`.
- `NOISE_FF` is a timed two-stage register sequence.
- The second operand of square-channel `0x9D` is stored as software-envelope control and affects delayed articulation.
- Bank `0x1B` and bank `0x1E` use different software-envelope tables.

When changing driver behavior, add a small synthetic-ROM test first. The existing `MusicDriverTest` style is designed for this.

## APU

`GameBoyApu` exposes Game Boy-style register writes and deterministic PCM rendering.

Important registers:

- Channel 1: `NR10..NR14`
- Channel 2: `NR21..NR24`
- Channel 3: `NR30..NR34` and wave RAM
- Channel 4: `NR41..NR44`
- Mixer/master: `NR50..NR52`

`GameBoyApu.renderInto(samples, offset, frames)` writes interleaved signed 16-bit stereo samples into caller-owned storage. It is deterministic and safe to test directly. The mixer respects `NR51` routing and `NR50` volume.

Keep the APU independent from ROM parsing and OpenAL. It should know only registers, wave RAM, channels, and sample rate.

## OpenAL Player

`OpenAlStream`, owned by `OpenAlMusicPlayer`, owns native audio resources:

- ALC device
- ALC context
- one AL source
- a fixed queue of OpenAL buffers

It queues 1024-frame stereo buffers and keeps a small buffer pool. Driver ticks are scheduled by sample count:

```text
samples per music tick = sampleRate / 60, with remainder carried forward
```

Do not move 60 Hz ticking to Swing timers or wall-clock timing. Doing so makes playback depend on UI scheduling and causes drift.

Cleanup is intentionally defensive. If OpenAL initialization or playback fails, the browser should still open where possible and show a concise status message.

## Browser

Run the browser with:

```bash
cd java
gradle runAudioBrowser
```

The browser is independent from `linksawakening.Main`. It should not initialize GLFW or OpenGL.

UI responsibilities:

- track list and search
- play/pause/resume/stop
- loop toggle
- volume slider
- selected-track metadata
- channel activity labels
- status line for OpenAL or playback errors

`BrowserTransportState` is deliberately separate from `OpenAlMusicPlayer`. It keeps Swing button state testable without requiring a native audio device.

## Testing

Use targeted tests while changing a specific area, then run the wider audio suite.

Catalog and opcode changes:

```bash
cd java
gradle test --tests linksawakening.audio.music.MusicCatalogTest
gradle test --tests linksawakening.audio.music.MusicOpcodeTest
```

Driver changes:

```bash
cd java
gradle test --tests linksawakening.audio.music.MusicDriverTest
gradle test --tests 'linksawakening.audio.*'
```

APU changes:

```bash
cd java
gradle test --tests linksawakening.audio.apu.GameBoyApuTest
gradle test --tests 'linksawakening.audio.*'
```

Browser changes:

```bash
cd java
gradle test --tests linksawakening.audio.browser.AudioBrowserFrameTest
gradle compileJava
```

Final verification before claiming audio work is complete:

```bash
cd java
gradle test --rerun-tasks --tests 'linksawakening.audio.*'
gradle test --rerun-tasks
gradle compileJava
gradle runAudioBrowser --dry-run
```

In the Codex sandbox, `gradle runAudioBrowser --dry-run` may fail to load Gradle's native platform dylib on macOS. If that happens, rerun the same command with escalation rather than treating it as an implementation failure.

## Extension Rules

When adding SFX or jingles:

- Keep music, SFX, and host playback boundaries explicit.
- Model channel stealing and priority in a driver-level component, not inside OpenAL.
- Preserve APU register writes as the handoff point.
- Use disassembly labels and ROM bytes as source of truth.
- Add tests with tiny synthetic command streams before using full-ROM smoke checks.

When improving accuracy:

- First identify the disassembly routine and observed register effects.
- Add a regression test against the Java driver or APU boundary.
- Mirror tables from the disassembly with comments or nearby tests proving the mapping.
- Avoid changing OpenAL code for sequencing bugs unless the problem is truly buffering/timing.

When adding UI:

- Keep native audio optional. The frame should still be able to represent unavailable audio.
- Test state transitions through model/helper classes, not by requiring a real OpenAL device.

## Common Pitfalls

- Do not treat `MusicOpcode.isNoiseNote` as the full channel 4 classifier. Channel 4 accepts normal note-code values through the driver because the same byte range means different things by channel.
- Do not consume a rest by stopping future notes. Rests mute current output only for the selected duration.
- Do not infer public music ids from sorted ROM order. Use the bank-specific transformations.
- Do not tick the music driver once per OpenAL buffer. It must tick at 60 Hz inside buffer generation.
- Do not let Swing timers drive audio sequencing. Swing timers are only for UI refresh and player update polling.
- Do not make OpenAL required for unit tests. Keep native-device tests out of the normal headless suite.
- Do not silently replace ROM tables with hand-made guesses. If a table is mirrored in Java, verify it against the disassembly behavior with tests.

## Useful Files

- Design spec: `docs/archive/history/superpowers/specs/2026-04-28-music-engine-design.md`
- Implementation plan history: `docs/archive/history/superpowers/plans/2026-04-28-music-engine.md`
- Music package tests: `java/src/test/java/linksawakening/audio/music`
- APU tests: `java/src/test/java/linksawakening/audio/apu`
- Browser tests: `java/src/test/java/linksawakening/audio/browser`

The game session now owns one `FrameAudioPlayer` and one host-created
`PcmSoundOutput`. Desktop uses OpenAL; headless sessions discard PCM. Dialog
and gameplay event maps submit effects to the same ROM-backed SFX driver and
APU as music. `MusicDirector.tick()` advances this transport once per game
frame (60 Hz); sample remainders preserve exact long-term sample counts.
The session closes the output. The audio browser retains `OpenAlMusicPlayer`
for its independent sample-driven audition controls. Legacy one-shot sinks
remain available to standalone clients, but are no longer session wiring.

## Allocation and compatibility contracts

`GameBoyApu.renderInto(samples, offset, frames)` writes stereo samples into
caller-owned storage, including clearing silent spans. The allocating `render`
and boxed observer adapters were removed after their owned callers migrated.
Tests that need owned PCM use `TestPcm`; register traces use
`setPrimitiveRegisterObserver`. No observer is installed by default.

Browser streaming reuses PCM and direct upload storage and keeps native buffer
ids in a primitive ring. The SFX interpreter reuses its CPU and stack, resetting
all execution registers for each invocation. Music cycle detection stores exact
parse-position tuples in reusable primitive storage and retains zero-time-loop errors.
Validated hot ROM byte reads avoid constructing temporary address wrappers.

`ReusableFramePcmOutput` explicitly promises synchronous consumption without
retaining caller storage. Desktop output and the default `HeadlessHost` discard output implement it, allowing `FrameAudioPlayer`
to reuse its whole/fractional frame buffers. Other outputs retain the original
fresh-array ownership behavior. One-shot audition returns owned storage and uses
a single working buffer across its emulated ticks.

`AudioGoldenTest` pins SHA-256 register streams and little-endian stereo PCM for
three tracks and three SFX, each over 240 frames at 48 kHz. These hashes were
captured before the refactor. `AudioAllocationTest` measures warmed-up synthetic
music plus APU rendering with ThreadMXBean; its guard is below 16 bytes/tick.
It also guards `FrameAudioPlayer.update` with the default reusable headless output
and the fractional browser sample clock. `SfxAllocationTest` measures an active
synthetic interpreter handler. These paths measured 0.0 bytes per tick/frame/buffer
with escape analysis both enabled and explicitly disabled in the test JVM.
`FrameAudioMixerTest` checks original chunk/tick ordering at 48,000, 48,001 and
30 Hz sample rates without native audio. Boundary tests reject LWJGL references
outside `audio.openal` and anywhere in `input`; source checks also catch inlined
GLFW constant imports. All these tests are headless.

## Native output failures

`OpenAlStream` reports `OpenAL unavailable: ...` and remains silent when device
opening or native loading fails. Its startup opener is injectable for device-free
failure tests. Native linkage errors during cleanup are retained as warnings while
later cleanup continues, rather than escaping the fallback path.

The engine still renders PCM for discarded headless output, preserving sample
clocks and APU state. Only explicitly reusable outputs receive retained storage;
third-party `PcmSoundOutput` implementations continue receiving fresh owned arrays.
Frame pacing belongs to the app loop and is independent of these output contracts.


## Structural boundaries after the round-4 review

Implementation inheritance is absent from the audio packages. The former
SfxCpu/SfxCpuState chain was one interpreter split for line limits, so it is now
one 506-line class. BrowserPanels was a view slice reaching back into every
AudioBrowserFrame widget; its assembly methods now belong to the 448-line view.
Cohesion takes priority over file-length targets. MusicVoiceEmitter remains a
collaborator because it owns waveform/NOISE_FF articulation state and ordered
channel register emission; ParsePositions owns reusable cycle-detection storage.

All remaining GPU byte-array migration delegates were removed after world callers
migrated. Audio has no deprecated production members. A clean temporary
`-Xlint:deprecation,removal` compile reports zero warnings in the owned platform
main/test roots; the active music test now uses EntityProjectileEvent.builder.
The normal headless suite retains the register/PCM golden tests and the allocation
guards. No native device or Swing window is needed for verification.

See [the structural audit](archive/history/platform-round-4.md) for the inheritance,
helper-responsibility and public-visibility inventory. Platform main sources have
zero project-class inheritance and 63 public top-level types after the review.

After merging the interpreter, the final escape-analysis-disabled ROM-free check
still measured 0.0 bytes/tick for music+APU and active SFX, and 0.0 bytes/frame or
buffer for FrameAudioPlayer and FrameAudioMixer. The temporary test JVM argument
was not added to build configuration.
