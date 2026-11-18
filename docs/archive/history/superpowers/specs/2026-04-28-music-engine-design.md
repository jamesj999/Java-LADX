# Music Engine Design

**Date:** 2026-04-28

## Goal

Add a music-first sound engine that reads Link's Awakening DX music data from the ROM, recreates the Game Boy audio path in software, and exposes a standalone Swing browser for selecting and playing tracks through LWJGL OpenAL.

## Source Of Truth

- `LADX-Disassembly/src/code/audio/music_1.asm`
- `LADX-Disassembly/src/code/audio/music_2.asm`
- `LADX-Disassembly/src/code/audio_macros.asm`
- `LADX-Disassembly/src/data/music/music_tracks_data_1b_*.asm`
- `LADX-Disassembly/src/data/music/music_tracks_data_1e_*.asm`
- `LADX-Disassembly/src/constants/sfx.asm`
- `LADX-Disassembly/tools/generate_music.py`
- `java/src/main/resources/rom/azle.gbc`

## Scope

The first milestone implements music playback only. Sound effects, jingles, channel stealing, and gameplay event integration are out of scope for this slice.

Music playback should be accurate from the start. The implementation should not use a throwaway note scheduler or pregenerated audio files. It should decode ROM music streams, execute the music-driver command behavior at 60 Hz, write Game Boy sound-register state, and synthesize PCM from a software Game Boy APU model.

Channels 1, 2, and 3 are required for the first audible pass. Channel 4 should have the same public APU/channel boundary reserved for the future SFX milestone, but music-browser acceptance does not require fully accurate noise output unless a selected music track depends on it.

## Architecture

Add a new `linksawakening.audio` package with four focused areas:

- `linksawakening.audio.music`
  Loads the music catalog from ROM, maps public music ids to the correct ROM bank/table entry, owns per-track and per-channel playback state, and interprets music stream opcodes.
- `linksawakening.audio.apu`
  Models Game Boy sound registers and synthesizes mixed PCM from square channel 1, square channel 2, wave channel 3, and the channel 4 boundary.
- `linksawakening.audio.openal`
  Owns OpenAL device/context setup and streaming buffer management.
- `linksawakening.audio.browser`
  Owns the Swing window and transport controls for the standalone browser launch target.

The music driver should be a Java reconstruction of the disassembly behavior rather than an SM83 CPU emulator. It should mirror the observable driver state: active track, track bank, speed table pointer, channel definition pointers, note length, length countdowns, loop counters, transpose, waveform pointer, envelope/duty settings, rest state, and register writes.

## Music Data Flow

`MusicCatalog` reads track ids from `constants/sfx.asm` semantics and maps them to the disassembly's two music banks:

- bank `0x1B`, pointer table at ROM bank-local address `0x4077`, for tracks transformed by `generate_music.py`'s `indexTransformer1b`
- bank `0x1E`, pointer table at ROM bank-local address `0x407F`, for tracks transformed by `generate_music.py`'s `indexTransformer1e`

All ROM reads use the existing banked offset rule: `bank * 0x4000 + (address - 0x4000)` for switchable-bank addresses.

When the browser starts playback, `MusicDriver` loads the selected track header:

- control byte
- speed table pointer
- four channel stream pointers

Each emulated 60 Hz tick advances active channel streams. Opcodes from `audio_macros.asm` must be handled by meaning, not by label text:

- `0x00`: end channel definition
- `0x01`: rest
- `0x94..0x9A`: driver mode flags used by the music routines
- `0x9B`: begin loop with count
- `0x9C`: loop or continue
- `0x9D`: set waveform on channel 3, or set envelope/duty on channels 1 and 2
- `0x9E`: set speed table
- `0x9F`: set transpose
- `0xA0..0xAF`: select note length from the speed table
- `0x02..0x90`: pitched note for channels 1-3
- `0xFF`: valid noise note for channel 4

The driver writes register-level state into `GameBoyApu` rather than writing directly to OpenAL.

## APU And PCM Output

`GameBoyApu` owns the emulated register file for the Game Boy audio registers used by music:

- `NR10..NR14` for channel 1
- `NR21..NR24` for channel 2
- `NR30..NR34` and wave RAM for channel 3
- `NR41..NR44` for channel 4 boundary support
- `NR50..NR52` and `NR51` for global volume/output routing

The APU generates signed 16-bit stereo PCM at a fixed host sample rate, preferably 48 kHz unless OpenAL device constraints require 44.1 kHz. The synthesis step must be deterministic for tests: given the same register writes and sample count, it emits the same PCM bytes.

The square channels implement duty pattern, trigger, frequency, length counter behavior needed by the music driver, volume/envelope behavior used by the ROM music, and sweep support for channel 1 as needed by observed tracks. The wave channel implements wave RAM loading, DAC enable, output level, trigger, frequency, and sample position. Channel 4 starts with a compatible interface and can initially output silence for the music milestone if tests and selected tracks confirm that no required music content is lost.

## OpenAL Streaming

Add LWJGL OpenAL dependencies and natives to `java/build.gradle`. The existing game `run` target keeps its current GLFW/OpenGL behavior. The new browser target uses OpenAL without opening the game window.

`OpenAlMusicPlayer` maintains a small queue of PCM buffers, refills them from the music driver/APU pipeline, and exposes:

- `play(trackId)`
- `pause()`
- `resume()`
- `stop()`
- `setLoopEnabled(boolean)`
- `setVolume(float)`
- `close()`

Stopping a track clears queued OpenAL buffers and resets driver/APU state. Looping restarts the selected track when all active music channels have ended.

## Swing Browser

Add a Gradle launch target named `runAudioBrowser`. It opens a simple AWT/Swing window with:

- track list with id and display name
- search/filter text field
- Play, Pause/Resume, and Stop buttons
- loop toggle
- volume slider
- current-track metadata: id, name, bank, header address
- basic channel activity indicators for channels 1-4
- status line for load/playback errors

The browser should load `rom/azle.gbc` from resources by default. It should run independently from `linksawakening.Main` and should not initialize GLFW/OpenGL.

## Error Handling

ROM parsing errors should report the selected track id, bank, and address being decoded. Invalid channel pointers, unsupported opcodes, and out-of-bounds speed/waveform pointers should stop the selected track and show a concise status message in the browser.

OpenAL initialization failure should disable playback controls and show the native audio failure message in the status line. The browser window should still open so users can see that audio initialization failed.

## File Responsibilities

- `java/src/main/java/linksawakening/audio/music/MusicCatalog.java`
  Builds the track list and resolves music ids to bank/header addresses.
- `java/src/main/java/linksawakening/audio/music/MusicTrack.java`
  Immutable track metadata.
- `java/src/main/java/linksawakening/audio/music/MusicDriver.java`
  Owns track playback state and 60 Hz music-driver ticks.
- `java/src/main/java/linksawakening/audio/music/MusicChannelState.java`
  Per-channel driver state.
- `java/src/main/java/linksawakening/audio/music/MusicOpcode.java`
  Opcode constants and operand sizing helpers.
- `java/src/main/java/linksawakening/audio/apu/GameBoyApu.java`
  Register file, channel ownership, and PCM mixing.
- `java/src/main/java/linksawakening/audio/apu/SquareChannel.java`
  Square channel synthesis for channels 1 and 2.
- `java/src/main/java/linksawakening/audio/apu/WaveChannel.java`
  Wave channel synthesis and wave RAM behavior.
- `java/src/main/java/linksawakening/audio/apu/NoiseChannel.java`
  Channel 4 boundary and future noise synthesis home.
- `java/src/main/java/linksawakening/audio/openal/OpenAlMusicPlayer.java`
  OpenAL context, queued buffers, and transport API.
- `java/src/main/java/linksawakening/audio/browser/AudioBrowserMain.java`
  Browser entry point.
- `java/src/main/java/linksawakening/audio/browser/AudioBrowserFrame.java`
  Swing UI, user actions, and status display.
- `java/build.gradle`
  Adds OpenAL dependencies and `runAudioBrowser`.

## Testing Strategy

Use focused unit tests before implementation code.

- `MusicCatalogTest`
  Proves music ids map to the expected bank/header addresses for representative tracks from both banks.
- `MusicOpcodeTest`
  Proves opcode classification and operand sizes match `audio_macros.asm`.
- `MusicDriverTest`
  Uses tiny synthetic ROM streams to prove note length selection, rest handling, loop handling, set-speed, transpose, waveform loading, and register writes.
- `GameBoyApuTest`
  Proves deterministic PCM generation for simple square and wave register states.
- `AudioBrowserFrameTest`
  Tests browser model/control state without requiring OpenAL.

OpenAL streaming is integration-tested with a small smoke test that can be disabled automatically when no audio device is available. Normal unit tests must run headless.

## Out Of Scope

- SFX and jingle playback
- Gameplay integration with `Main`
- CPU-level execution of original audio routines
- Pregenerated music exports
- Perfect analog filtering and device-specific speaker coloration
