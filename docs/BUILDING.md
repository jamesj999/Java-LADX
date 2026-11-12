# Building

This is an open source engine reimplementation for preservation and research.
Supply your own English Link's Awakening DX v1.0 ROM; ROMs and extracted
assets must never be committed or distributed.

Install JDK 21. From java/, use ./gradlew (gradlew.bat on Windows).
The wrapper pins Gradle 9.8.1. No system Gradle is required.

Place your ROM at java/src/main/resources/rom/azle.gbc, or pass
-Pla.rom=/absolute/path/azle.gbc or set LA_ROM. Run ./gradlew run.
The verifyRom helper reports a non-fatal MD5 mismatch against
07c211479386825042efb4ad31bb525f, from LADX-Disassembly/ladx.md5.

./gradlew test runs the default suite when a ROM is available. Without one,
ROM integration tests are skipped with a reason. ./gradlew romFreeTest
excludes tests tagged rom. Mark every new ROM integration test class with
@linksawakening.testing.RequiresRom (including indirect ROM consumers).
This composed annotation supplies the JUnit rom tag and skips tests when the
ROM is absent. Use TestRom.open() for test ROM resource reads; unmarked test
classes fail with an instruction to add @RequiresRom. No classifier list is needed.

./gradlew distZip builds a distribution for the current platform.
Override with -Pplatform=macos, macos-arm64, linux, linux-arm64, or windows.
-PallPlatforms includes all five native classifiers. Distribution archives
exclude ROMs and saves. Supply your ROM separately when running.
The macOS run task adds -XstartOnFirstThread; when launching a distribution
on macOS, set JAVA_OPTS=-XstartOnFirstThread.

Emulator comparisons live in tools/{intro,menu,house,dialog,opening}.
Install Python dependencies with python3 -m pip install -r tools/requirements.txt.
Run the relevant compare.py --help (opening also has capture.py) for local
capture and comparison options. These tools require your own ROM and may
generate screenshots and reference data: never publish extracted assets.

## Controls

During gameplay, **Escape** requests the same save screen as **Start + Select + A + B**.
It uses the same gameplay gates: finish dialogs, inventory, recovery, room
transitions and scripted opening/keyhole sequences first. A blocked Escape
press is discarded; press it again when gameplay is ready.
The save screen keeps its existing options and controls; Escape does nothing there.
On the title, intro and file-selection/name-entry screens, Escape quits the app.

## Debug menu

Set `"debugEnabled": true` in `java/src/main/resources/config/config.json`
and restart the game. Press **F4 during gameplay** to open the paused debug
menu. Set it to `false` to disable the menu and the existing F1/F2/F3 shortcuts.
Finish any dialog, inventory animation, room transition or keyhole sequence
before opening it. From the title/intro, F2 starts the existing standalone
all-items debug session; F4 itself preserves the current game and save slot.

- **Tab / Page Down / Page Up:** change page.
- **Up / Down:** select an entry (long pages scroll automatically).
- **Left / Right:** decrease/increase; hold **Shift** for steps of 16.
- **Home / End:** minimum/maximum value, including `FF` for Color Dungeon.
- **Enter on Teleport:** load the selected destination; remain paused for edits.
- **F4 / Escape:** close and resume. Escape here does not quit the game.

The Teleport page accepts an overworld, indoor or side-view category, hexadecimal
map and room IDs, and decimal pixel X/Y for Link's **top-left sprite origin**
within a room (0–144, 0–112). Overworld room IDs are row/column packed: `92`
is row 9, column 2 on the 16×16 map. The map field is ignored for overworld.
Indoor room IDs address the ROM room table; they are not dungeon minimap cells.
Use matching map/room/category values from the disassembly's room warp records.
The menu uses normal ROM room loading and does not force solid tiles walkable;
F3 toggles the existing no-clip mode after closing the menu.

Useful map IDs: `00` Tail Cave, `01` Bottle Grotto, `02` Key Cavern,
`03` Angler's Tunnel, `04` Catfish's Maw, `05` Face Shrine, `06` Eagle's Tower,
`07` Turtle Rock, `08` Wind Fish's Egg, `10` houses, `FF` Color Dungeon.
Color Dungeon rooms are `00`–`15`. For example, indoor map `00`, room `17`,
X `72`, Y `96` places Link in the Tail Cave entrance room. Teleporting resets
Link's transient movement and room effects and preserves collected progress.
It does not reproduce story events leading up to that destination.

Inventory edits the A/B slots and all ten backpack slots; select **Empty** to
remove an item or select any of the thirteen inventory item IDs to give it.
The ROM has twelve total slots, so swap items when experimenting with a full
loadout. Sword, shield and bracelet levels are separate entries: set a nonzero
level as well as assigning the item. Resources edits rupees, heart containers,
health in eighth-hearts, heart pieces, ammunition/capacities and seashells.
Health and ammunition remain limited by their current capacities; raise the
capacity first. Home/End provides quick empty/refill operations.

Quest includes flippers, toadstool, medicine, keys, trade items, songs, tunic
and modeled story flags. `Leaves/key` values 0–5 count golden leaves; 6 means
the Slime Key. Song bits are 1=Frog, 2=Mambo, 4=Ballad (7=all); tunics are
0=green, 1=red, 2=blue. Dungeons edits each dungeon's map, compass, stone beak,
Nightmare Key and small keys, plus the eight instrument flags
(0=none, bit 0=miniboss defeated, bit 1=instrument collected; 3=both).
These are development controls over state the engine currently models;
they do not implement missing item behavior or guarantee every later room is
playable. Re-enter a room to refresh entities affected by progression edits.

The overlay uses an engine-authored bitmap font directly in the RGBA framebuffer.
It must not use AWT/Java2D or Swing: initializing another native UI toolkit
inside the GLFW process can stop macOS keyboard and close events. Headless
checks alone do not exercise that native event-loop interaction.

Edits apply immediately in memory. They are written only through the normal
save action if playing a save slot; a standalone F2 debug session has no save
slot. Closing the menu does not undo edits or automatically save them.

Debug-menu verification (2026-11-07): the ROM-free suite passed (953 tests).
ROM-backed menu tests cover pausing, input isolation, configuration gating,
inventory/resource writes and overworld/indoor/side-view/Color Dungeon loads.
The full suite passed 2,473 of 2,474 tests. The opening replay
`tools/opening/tail-cave-warp-route.tsv` (SHA-256
`79c179a1bce0e23d63c84238a6da404b9ddc8f2786b8751550873dcfffb4bf80`)
blocked at `down+a*+b y=88 4`, map `00`, room `42`, ROM position `(140,64)`,
frame counter 118, health 4, powder 19, before clearing the forest. Re-running
with the original `GameSession` reproduced the identical failure/state.
No new emulator pixel comparison was performed: these checks verify debug
controls and bounded room loading, not later-game parity.

## Code style and checks

Java uses Spotless with Palantir Java Format. From `java/`, run
`./gradlew spotlessApply` before committing and `./gradlew spotlessCheck` to
check formatting without edits. With a local ROM configured, the pre-merge gate
is `./gradlew spotlessApply test` (or `./gradlew test` after formatting).

`test` is the default regression oracle, including `@RequiresRom` integrations
when a ROM is available. `romFreeTest` excludes ROM-tagged tests. `knownGapTest`
is a separate opt-in task for a deliberately excluded pre-existing route gap;
it is not part of the default gate. Do not weaken tests or route fixtures.

`ComposeAllocationTest` protects reusable indexed composition scratch below
4,096 bytes/frame. `SessionAllocationTest` bounds headless frame allocation;
`AudioAllocationTest` checks retained audio buffers. See [TESTING.md](TESTING.md)
for setup, assumptions and adding coverage.

Public CI runs only for pull requests into `main`, with wrapper validation,
`spotlessCheck`, ROM-free tests and Python lint/unit checks. ROM-backed tests
cannot run in public CI by design: no ROM may be distributed or uploaded as a
CI fixture. A passing public build does not replace the local ROM-enabled gate.

Python tools target Python 3.11+ (validated locally with 3.12); use the exact
pins in `tools/requirements.txt`. From the repository root run
`python -m ruff check tools`, `python -m ruff format --check tools` and
`python -m pytest tools`. These checks need no ROM. Capture commands do;
see [tools/README.md](../tools/README.md).

## Gameplay recording

Press **F9 during gameplay** to start recording; press it again to stop and
finalize the MP4. This works during dialogs, inventory, transitions and the debug
menu, independently of `debugEnabled`. Once recording is active, the shortcut
can stop it on any screen. Holding the key does not repeatedly toggle recording.
The window title shows **[REC]** while active; the console reports the output
path and any start/stop errors.

Configure the key in `java/src/main/resources/config/config.json` and restart:

```json
"controls": {
  "recordingKey": "F9"
}
```

Keep the other controls in that object. Key names support F1–F25 and the usual
letters, digits and named keys; `"NONE"` disables the shortcut. Use an otherwise
unused key: the recording binding consumes that key before gameplay/debug input.
An invalid recording key falls back to F9. On keyboards using function keys for
media controls, you may need Fn+F9.

Shortcut recordings go to `recordings/gameplay-<timestamp>-<unique-id>.mp4`
relative to the working directory. When launched with `--record`, the shortcut
can stop that recording; subsequent clips use the same directory, scale, quality
and FFmpeg executable with fresh filenames. Without `--record`, recording stays
off until the first shortcut press and uses the defaults below.

Recording is built into the session loop and works with either host. Install an
FFmpeg executable with `libx264` and `aac` encoders on `PATH`, or pass
`--ffmpeg /absolute/path/to/ffmpeg`. FFmpeg is not bundled with distributions;
normal gameplay does not require it. No screen-capture permissions or display
server are needed for headless recording.

From `java/`, with your ROM configured:

```sh
# Windowed gameplay: close the window normally to finish the recording.
./gradlew run --args='--record build/recordings/gameplay.mp4'

# Thirty seconds of the configured intro/title, rendered without a window.
./gradlew run --args='--headless --frames 1800 --record build/recordings/intro.mp4'

# Start directly in the opening house; optionally stop a windowed run by frame count too.
./gradlew run --args='--new-game --frames 600 --record build/recordings/house.mp4'

# Record an existing scripted opening route, including partial footage if it fails.
./gradlew captureOpeningRoute -PopeningRecording=build/recordings/route.mp4
```

The default is **160×144 at 60 fps**, independent of window size or display DPI.
The rate matches the engine's simulation clock (not the original hardware's
approximately 59.73 Hz). Each rendered engine frame becomes one video frame;
headless acceleration does not accelerate the resulting video's playback.
Audio is tapped from the engine's mixed 48 kHz stereo output, before device
buffering. Paused debug-menu frames contain silence.

MP4 uses H.264 (`libx264`, medium preset, CRF 18, `yuv420p`) and AAC stereo at
96 kbit/s. These defaults prioritize small files and broad browser/preview
compatibility. Compression is lossy, including chroma subsampling: use raw
framebuffer captures for exact pixel comparisons. `--record-crf 14` increases
quality and file size; `--record-crf 23` reduces size. `--record-scale 2` gives
320×288 using nearest-neighbor scaling; supported integer scales are 1–8.
See `./gradlew run --args='--help'` for all options. FFmpeg documents the
[H.264/AAC encoders](https://ffmpeg.org/ffmpeg-codecs.html) and
[MP4 fast-start metadata](https://ffmpeg.org/ffmpeg-formats.html#mov_002c-mp4_002c-ismv).

Video and audio are compressed incrementally into a temporary directory beside
the output, then remuxed without re-encoding with fast-start metadata when the
session closes. Memory use does not grow with recording duration. Finalization
briefly needs space for both the temporary streams and the finished file.
A slow encoder applies backpressure and can slow windowed gameplay; frames are
not silently dropped. Existing destinations are never overwritten. Empty
sessions create no MP4. Encoder failures are reported and temporary files are
removed on normal/error cleanup. Forced process termination cannot finalize a
recording and can leave a `.recording-*` directory that may be deleted manually.

For custom input replays, use a recording-enabled driver and close it:

```java
try (var game = new HeadlessGameDriver(RecordingOptions.defaults(Path.of("run.mp4")))) {
    game.initializeHeadlessNewGame();
    game.headlessButton("right", true);
    for (int frame = 0; frame < 120; frame++) game.headlessFrame();
    game.headlessButton("right", false);
}
```

`GameSession.configureRecording(options)` must be called before initialization;
`GameSession` and `HeadlessGameDriver` implement `AutoCloseable`. Recording is
opt-in and makes no encoder calls during ordinary gameplay. Generated footage
contains ROM-derived graphics/audio: keep it local, out of commits and releases.
The ignore rules cover MP4/M4A and temporary recording directories.

ROM-free encoder integration tests use synthetic frames/audio and skip when
FFmpeg/ffprobe are unavailable. The ROM-backed session recording check compares
660 framebuffers and gameplay states with an unrecorded session; it uses the
user-supplied ROM, never a fixture.

Recording verification (2026-11-12, Java 21/macOS ARM64): `test` passed 2,812
with one skipped; `romFreeTest` passed 1,137; `spotlessCheck` passed. Native
windowed capture produced 180 frames / 3 seconds with matching audio duration;
headless capture produced 1,800 frames / 30 seconds (476,778 bytes).
`tools/opening/house-route.tsv` (SHA-256
`e6065107a5ef0ed993855dc4e7acfabe8f4bfb5756106b5032bfede2e12b5142`)
completed all 22 checkpoints with and without recording. All 22 checkpoint PNGs
and five trace/metadata files were byte-identical. The final checkpoint was
map `00`, room `A2`, position `(88,82)`, shield 1, health 24; the recording had
2,959 frames with matching video/audio duration (49.316667 seconds). Trace
SHA-256: `30638dc045f59722f1208e7abda2febbed18a15c18b8fba71f04e171e0742343`.
This verifies recording does not alter the tested opening route. No new emulator
comparison was performed; existing gameplay parity gaps remain outside this check.

Shortcut verification (2026-11-12): `test` passed 2,814 with one skipped;
`romFreeTest` passed 1,138; formatting passed. Input integration exercises
stop/restart, fresh filenames, ignored repeat/release events, continued frames
between clips, and stopping on the title screen. Configuration tests cover
remapping, disabling and invalid-name fallback. A 120-frame native windowed
recording smoke test completed with the recording-status title update enabled.
