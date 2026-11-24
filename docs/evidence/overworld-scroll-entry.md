# Overworld scroll landing, 2026-11-07

The engine snapped Link to a fixed destination edge when scrolling. The ROM
instead integrates the existing position and fractional accumulators. This
made right, left and upward entries observably wrong even with matched state.

`src/code/room_transition.asm` in LADX-Disassembly defines signed Link speed
`$3A` / `$C6` and background speed 4. `ApplyRoomTransition` calls
`UpdateFinalLinkPosition` once per scroll step at `$02:790B`, then saves the
map entry position at `$02:7945`. Forty horizontal steps integrate 145 pixels;
32 vertical steps integrate 116. Both totals are whole pixels, so both
fractional accumulators survive unchanged. `ComputeLinkPosition` at
`$00:21B6` is the source fixed-point addition, not a destination clamp.

The correction applies those deltas to the actual overworld boundary position
and retains both fractions when recording entry. Indoor thresholds, transition
preparation timing and rendering remain outside this correction.

## Controlled comparison

Local tools: `tools/entry/capture_rom.py` (PyBoy 2.8.1) and
`tools/entry/EntryProbe.java` (JDK 21). Both first replay
`tools/opening/house-route.tsv`, then inject Link's position in overworld A2.
This is a **synthetic state experiment**, not an uninterrupted gameplay replay.

ROM SHA256: `c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
House route SHA256: `e6065107a5ef0ed993855dc4e7acfabe8f4bfb5756106b5032bfede2e12b5142`.
ROM MD5 matches English DX v1.0. No ROM or captured assets are committed.

For each independent experiment, inject integer ROM positions below,
`wC11A=$A0`, `wC11B=$50` (Java fractional nibbles 10 and 5), speed zero,
and hold only the named direction until completion. The Java probe invokes
its ordinary room-boundary coordinator at the injected boundary, then runs
real session frames. The ROM runs its ordinary boundary handler, including
its preparation phases. RNG is not synchronized; no combat/random outcome is
used as evidence. Positions are compared at the completed scroll/entry marker,
not by equal global frame number.

| Direction | Initial ROM X,Y | Old entry X,Y | ROM and corrected Java entry X,Y |
| --- | --- | --- | --- |
| Right | 156,96 | 8,96 | 11,96 |
| Left | 3,96 | 152,96 | 148,96 |
| Up | 80,11 | 80,128 | 80,127 |
| Down | 80,132 | 80,16 | 80,16 |

Both fractions remain `$A0/$50` in the ROM and 10/5 in Java after the
transition. ROM trace includes phase, direction, integer/fractional position,
velocity, scroll offset, entry marker and held buttons on every frame.
ROM completion frames relative to trigger were 43/50/38/42 respectively;
Java performs 40/40/32/32 scroll frames without the ROM preparation phases.

Completion resumes motion: left/up/down current ROM coordinates also match
Java (147,96 / 80,126 / 80,17). On the right, saved entry markers match, but
current X is ROM12/Java11, with right speed16 in both. This additional
post-entry discrepancy was investigated separately below. The screenshots also show
background animation/scroll phase differences. These are not pixel-identical
scene comparisons or proof of global fidelity.

Local screenshots: `/tmp/ladx-entry-controlled/{rom,java-before,java-after}-up-complete.png`
and corresponding right/left/down names. Before images were produced by
compiling the pre-change boundary/coordinator classes into a temporary classpath,
then running the same Java probe against them. All captures remain local.

## Verification and fixture repair

Focused checks: RoomBoundaryControllerTest, LinkStateMachineTest,
RoomTransitionCoordinatorTest and WorldDependencyTest. The boundary regression
covers all directions and overshoot; the motion regression checks that saved
entry and live state retain both fractions.

The existing traversal fixture teleported right to Java X160 instead of the
actual trigger148, and down to Y128 instead of116. Fixed edge snapping hid
those overshoots. It also twice detoured D3→C3→D3 through C3's solid southern
border, which only worked because the previous upward snap put probes beyond
that wall. The fixture now uses the source trigger positions and a D3→D4→D3
east/west detour. It retains all room assertions and collision checks. Its
path search checks ROM collision at the destination as well, including the
next route boundary, so it does not select isolated edge pockets. The full
focused traversal class passes after these fixture corrections.

To reproduce the ROM probe, run `python tools/entry/capture_rom.py` using the
Python environment with `tools/requirements.txt`. For Java, compile
`tools/entry/EntryProbe.java` against Gradle's `sourceSets.test.runtimeClasspath`,
then run `linksawakening.render.EntryProbe after` from the repository root with that classpath plus
`java/build/test-rom` and the compiled probe directory. Run Gradle `testClasses`
and `testRomResources` first. Outputs go to `/tmp/ladx-entry-controlled`.


## Follow-up: rightward completion slowdown phase

The extra pixel was a slow-ground timing error, not a collision. A3's tall
grass sets source ground status3. The engine used a counter advanced only by
Link motion calls, so scrolling/dialog pauses shifted its slowdown phase.
Source `$02:44A9` reads global `hFrameCounter` and skips position integration
when its low two bits are zero. Production already supplies the global frame
alongside Gel state; slow ground now uses that supplied frame as well. Isolated
Link callers without a session clock retain their previous local-counter behavior.

The probe now records source world frame, ground status, collision type,
object under Link and final position as well as the original entry fields.
PyBoy CPU hooks at bank2:`$44A9` and `$44B2` also record the actual counter
used at the gate and at normal-motion integration. This distinguishes CPU motion phase from the screen-boundary snapshot:
the latter observes the next value of `hFrameCounter`. Therefore Java completion
frame210 matches the ROM screen-boundary frame211, followed by Java211..214
matching ROM screen212..215. Aligning the raw snapshot counters directly was
one frame early and did not establish matching motion phase.

At completion the saved entry remains11,96, fractional nibbles remain10/5,
and both versions report horizontal speed16 and collision0. The ROM sees ground
status0 before completion movement, then3 afterward. Subsequent CPU gate hooks
read frames211,212,213,214 with ground status3 and speed16; the frame212 gate
skips integration. The integration hook directly records frames210,211,213,214;
frame212 does not reach the position integration call.

| CPU motion frame | ROM screen frame | ROM X | Old Java X | Corrected Java X |
| --- | --- | --- | --- | --- |
| 210 | 211 | 12 | 11 | 12 |
| 211 | 212 | 13 | 12 | 13 |
| 212 | 213 | 13 | 13 | 13 |
| 213 | 214 | 14 | 14 | 14 |
| 214 | 215 | 15 | 14 | 15 |

All five Y observations are96; collision remains0. Input is held right throughout.
This closes the one-pixel rightward discrepancy for the matched experiment.
It does not establish every slow-ground entry/status transition or background
phase. The four original saved-entry markers remain unchanged.

Updated local screenshots:
`/tmp/ladx-entry-controlled/java-phase-before-right-complete.png`,
`java-phase-after-right-complete.png`, `rom-right-complete.png`.
The before capture uses the committed `8ed89ed` Link class in a temporary
classpath. Java `linksawakening.render.EntryProbe phase-after 210` reproduces the matched phase;
`capture_rom.py` prints the source CPU gate trace and post-completion positions.
No image assets are committed. Focused grass/Gel tests and existing entry
regressions pass, including a new test with skipped Link updates that checks
the global phases211/212/219/220.

## Visual follow-up: Link is visible, but the displayed pose differs

The completion screenshots do **not** establish sprite parity. Java Link is
visible: rendering the same production `LinkRenderLayer` in isolation gives
non-background colored pixels at X6..16/Y82..94, and the full production frame
differs from its room-only background at166 pixels inside Link's16×16 rectangle.
Its animation is `$2D`, Z0, and no bed presentation is active. It blends into
the grass as a small side-facing sprite; it is not hidden by an invisibility
flag or missing OAM submission.

Source analysis follows `skills/ladx-rom-tileset-debug/SKILL.md`. The ROM's
current animation/direction also say `$2D`/right at completion, but that does
not identify the currently uploaded graphics. Comparing actual VRAM bank0
`$8000..$803F` against the ROM-backed `LinkAnimationStateTable` and character
pairs gives an exact byte match to state `$22`, not `$2D`. Java resolves `$2D`
tiles and attributes immediately during composition.

The CPU hook at `$20:54F5` gives the real upload sequence for this experiment:

- Last upload before scrolling: CPU frame166, transition0, animation `$22`.
- No body-tile uploads throughout the scroll, including its completion frame210.
- Next upload: CPU frame211, transition0, animation `$2D`.
- Body VRAM remains `$2D` at the subsequent observations. The screen capture
  reflects the newly uploaded pose later than the raw VRAM/OAM snapshot.

At completion, source OAM slots0..3 are
`[104,11,$1A,$20] [104,19,$1A,0] [96,12,0,0] [96,20,2,0]`.
The first two entries are grass overlays and the last two are Link's body.
After CPU211, VRAM contains `$2D` and the body right-column flip changes to
`$20`. The source therefore stages both tile bytes and per-half attributes;
reading only current `hLinkAnimationState` misses this distinction.

Relevant source paths:

- `home/animated_tiles.asm` `$00:1B58..1B6E`: window, inventory appearance,
  pending draw command and room-transition gates can draw Link without
  uploading new body tiles.
- `$00:1B71..1B80`: switch-block and requested tile replacements also divert
  the upload schedule. Individual animated-background handlers can upload
  their own tiles and draw Link without replacing its body pairs.
- `$00:1D1E` → `func_020_54F5` at `$20:54F5`: resolve state-table pairs and
  half attributes, then `CopyLinkTilesPair` at `$00:1D0A` copies32 bytes per
  column from adjusted GBC bank `$2C` to `$8000`/`$8020`.
- `DrawLinkSprite` at `$00:1D2E`: OAM uses the staged half attributes in
  `wC11D/wC11E`, while position is read separately.
- `bank2.asm` `$02:7750` → `$02:787D`: grass writes two tile`$1A` OAM
  columns at raw Y+8 and raw X−1/X+7, with complementary horizontal flips.
  These lower-body overlays are also absent from the Java scene.

Reproducible local images:

- Completion gap: `java-phase-after-right-complete.png` versus
  `rom-right-complete.png` under `/tmp/ladx-entry-controlled`.
- Adjacent ROM displayed poses: `rom-right-post1.png` versus
  `rom-right-post2.png` shows the delayed change to the side-facing pose.
- Visible side-facing comparison at numeric ROM position13,96:
  `java-right-post1.png` versus `rom-right-post2.png`. Both show Link, but
  grass overlays/background phase differ. This pair does not prove pixel parity.
- Diagnostic-only `link-only-right.png` and `background-only-right.png` make
  Java's body submission and its166 changed pixels independently inspectable.

Run `capture_rom.py --visual` to record body-VRAM matching states, actual
upload-hook events, OAM entries and adjacent source images. Run
`linksawakening.render.EntryProbe render-diagnostic 210 visual` for the production Java image,
isolated body/background diagnostics and adjacent Java images. The Java
probe's equivalent diagnostic code was executed against the already compiled
`44afe9b` classes; no new Gradle task or production code was used for this
follow-up. Source screenshots remain local; no extracted assets are committed.

The body upload gap needs persistent Link body VRAM pairs and attributes,
an explicit source-ordered VBlank upload decision, and an agreed simulation/OAM/
display snapshot boundary. A blanket one-frame animation delay would mishandle
scroll, inventory, replacement and background-upload gates. The grass overlay
shipped separately. [Grass overlay evidence](link-grass-overlay.md)

## Walking and sword upload validation, 2026-11-24

A normal-input probe extends the sword route with rightward walking, a
stationary sword swing and leftward walking. Both captures completed 72
checkpoints with no ROM input overrides or RAM writes. The extended route
SHA-256 is `b275ff19aafe09a9a0c659a71ef9ff1c0685da1a1280e49a03303e88b651a648`.

```sh
out=$(mktemp -d)
cp tools/opening/sword-route.tsv "$out/route.tsv"
printf 'right 12 8\na 1 20\nleft 12 8\n' >> "$out/route.tsv"
python tools/opening/capture.py --body-trace-from 70 --route "$out/route.tsv" --output "$out"
(cd java && ./gradlew captureOpeningRoute -PopeningRoute="$out/route.tsv" -PopeningOutput="$out")
```

The body trace records the current animation at the screen boundary, CRC32 of
the four sprite tiles in VRAM bank 0, raw attributes from OAM slots 2/3, and
CPU hooks at `$00:1B58`, `$00:1C51`, `$20:54F5` and `$00:1D2E`. Each
CPU event includes the frame counter, pose, transition, window, inventory
appearance, draw command, switch stage, tile replacement, BG group and BG
tick, and the two requested graphics registers, in that order as hex fields
after the address. The trace also hooks the requested graphics load at
`$00:022F` and the frame increment at `$00:024D`. The Java trace records
the current pose. At the time of this upstream measurement its renderer resolved both columns and flips from that pose
during submission, as confirmed by tracing the first four tiles from the
production Link OAM producer.

| ROM route frame | CPU / screen counter | Current pose | VRAM CRC32 | OAM attrs | CPU decision |
| --- | --- | --- | --- | --- | --- |
| 8263, first right input | `$5C/$5D` | `$2D` | `da703aea` | `$00/$00` | Draw command `$9C` |
| 8264 | `$5D/$5E` | `$2D` | `07ed256e` | `$00/$20` | Upload |
| 8271, right walk | `$64/$65` | `$2C` | `07ed256e` | `$00/$20` | Draw command `$9C` |
| 8272 | `$65/$66` | `$2C` | `efcce262` | `$00/$20` | Upload |
| 8283, first A press | `$70/$71` | `$18` | `efcce262` | `$00/$20` | Draw command `$9C` |
| 8284 | `$71/$72` | `$18` | `90774ad0` | `$20/$20` | Upload |

All 61 observed frames reached DrawLinkSprite. The draw command preempted
16 uploads, and tide BG copies preempted another three when the animation
tick reached `$00`, `$10` or `$20`. The remaining 42 reached the body
upload routine. Java submitted CRC `90774ad0` and flips `$20/$20` on its
first A-press frame (8088), one frame earlier relative to that input than the
ROM's VRAM/OAM observation. The starting Y positions differ by one pixel and
their clocks differ. The table compares upload decisions and observed OAM,
with no claim about latched display pixels or a global one-frame offset.

## Frame counter during a full room load

The ROM render loop checks `wTilesetToLoad` and `wBGMapToLoad` before advancing
`hFrameCounter`. The requested graphics branch jumps to the frame wait
without executing the increment at `$00:024D`. On the normal rescue route
into room `$0A:$BD`, the graphics hook saw requests for BG map `$09` followed
by tileset IDs `$06`, `$09` and `$01`. The hook saw 17 screen observations
without an increment between the destination room first becoming current
and the first observation of interactive gameplay. One screen observation
contained an increment and the subsequent graphics-load branch.

Java loads the destination synchronously during the transition and advanced
its clock every update through the corresponding room-load and fade phases.
The ROM's ordinary scroll from `$BD` to `$AC` used no requested-graphics branch.
The Java room-load phase counts its own transition updates, while a single
ROM graphics request can span multiple emulator screen observations. The
measured screen stalls therefore do not specify which Java updates should
hold the clock. Pausing it for every transition would also alter the observed
ordinary scroll timing. No global clock correction was applied.

To repeat the ROM measurement, run the opening capture with
`--body-trace-from 113` on the rescue route prefix through checkpoint 118.
In `emulator-body.tsv`, compare CPU events `022f` and `024d` with the snapshot
counter on the same line.

## Ordinary indoor scrolls, 2026-11-24

Ordinary top-down indoor scrolls share the boundary thresholds at
`$02:6C99..6CB1` and the horizontal and vertical position deltas above.
`$02:6D5C` calls `$00:0CBE` before the scroll to copy the previous integer
position bytes back after the boundary-crossing motion. That copy leaves the
active fractional accumulators alone. Java now uses that saved integer
position for the ordinary indoor scroll and preserves both live fractions.

These observations used the historical normal-input rescue prefix from
`b5fd0ecf` through checkpoint 329 (SHA-256
`fa098fcee5b30be5fc1c6f389af3670cd7170df2f14c15dc0bc3d089394f4c35` for the
prefix with comment and blank lines omitted). The hook at `$02:790B`
observed 40 horizontal or 32 vertical integrations. The hook at `$02:7945`
observed the position saved at scroll completion.

| Checkpoint | Map and rooms | Direction | First integration X,Y | Saved entry X,Y | Start and end fractions X,Y |
| --- | --- | --- | --- | --- | --- |
| 118 | `$0A:$BD→$AC` | Up | 73,12 | 73,128 | `$00,$00` |
| 146 | `$0A:$AC→$BD` | Down | 72,131 | 72,15 | `$00,$00` |
| 299 | `$00:$17→$16` | Left | 4,80 | 149,80 | `$00,$00` |
| 326 | `$00:$16→$17` | Right | 155,80 | 10,80 | `$00,$80` |

A separate injected experiment loaded the normal route through checkpoint
298, then set raw X,Y to 4,80 with fractions `$00,$A0` and held Left+Down.
At `$02:6C99` the candidate was 3,81 with fractions `$40,$60`.
`$02:6DE4` and the first `$02:790B` hook observed 4,80 with the updated
fractions. The saved entry was 149,80 with the same fractions, and resumed
motion reached 148,81 during the completion frame. The Java coordinator
regression tests both the diagonal integer rollback and fractional retention,
plus saved entry for all four directions using the loaded rooms.

The normal-input Java rescue prefix reached checkpoint 118 at 72,128, with
saved Y128. The ROM's saved X,Y was 73,128 and its checkpoint was 73,127.
The X difference was present before starting the scroll. This comparison does
not align control release or resumed movement. On the standalone Tail Cave
dungeon route both Java versions had byte-identical state traces until death
at frame 13072 in overworld room `$70`, before any affected indoor scroll.

## Follow-up on grass OAM during movement and idle

The [grass-overlay implementation and aligned-frame comparison](link-grass-overlay.md)
followed the original scroll-entry investigation.

Run `python tools/entry/capture_rom.py --grass` with PyBoy 2.8.1 and the ROM
and house-route hashes above. The rightward experiment continues holding right
for 20 frames after completion, releases input for four frames, then holds
left back into A2. It records OAM slots 0–3, Link's raw position, ground status,
object physics and consecutive steps in
`/tmp/ladx-entry-controlled/rom-grass-trace.json`.

| Screen frame | Held | Raw Link X | Consecutive steps | Grass left/right OAM attributes |
| --- | --- | --- | --- | --- |
| 211 (entry) | Right | 12 | 9 | `$20 / $00` |
| 217 | Right | 16 | 15 | `$20 / $00` |
| 218 | Right | 17 | 16 | `$00 / $20` |
| 226 | Right | 23 | 24 | `$20 / $00` |
| 231 | Right | 27 | 29 | `$20 / $00` |
| 232–235 | Released | 27 | 7 | `$00 / $20` |
| 236 | Left | 26 | 8 | `$20 / $00` |

Link's raw Y is 96 throughout these observations. OAM slots 0 and 1 start
at raw Y 104 and raw X−1/X+7 with tile `$1A`. Body slots 2 and 3
remain at raw Y 96 and raw X/X+8. A motion integration skip still advances
the step count, as at screen frame 217 when X repeats at 16. Releasing
movement resets the count to 7 and retains both grass columns. The return
to A2 completes at screen frame 54 after wraparound. Object physics and
ground status both become zero, and the grass entries' raw Y becomes
`$F4`, hiding them even though their tile bytes still read `$1A`.

The source increments the step count at `$02:4440`, resets it at
`$02:445F`, and derives the left horizontal flip from bit 3 at
`$02:788A`–`$02:788F`. The right attribute flips the same bit at
`$02:78CF`. The CGB outdoor room `$32` palette-6 override is present
in the source and falls outside this A3 trace.


## Ordinary side-view and overworld scrolls on the fresh route

A read-only replay through checkpoint 510 measured the candidate position at
`$02:6CB6` or `$02:6D13`, the restored position at `$02:6DE4`, each scroll
step at `$02:790B`, and the saved room entry at `$02:7945`. Both Tail Cave
passage crossings use the same previous integer coordinate and live fractional
accumulators as the measured ordinary top-down crossings. The integrated Java
coordinator now uses this rule for side-view passages and the overworld.
Previously the side-view path snapped X to 8 or 152, and the overworld used the
integer coordinate from after motion. The earlier controlled overworld test
started with Link already over the threshold and could not distinguish these
two integer positions.

| Checkpoint | Rooms | Candidate X,Y | Restored X,Y | Saved entry X,Y | X,Y fraction bytes |
| --- | --- | --- | --- | --- | --- |
| 24 | Overworld A2 to A1 | 3,120 | 4,120 | 149,120 | 00,00 |
| 40 | Overworld D0 to E0 | 40,132 | 40,131 | 40,15 | 50,70 |
| 45 | Overworld F0 to F1 | 156,48 | 155,48 | 10,48 | 50,70 |
| 74 | Overworld F1 to F0 | 3,35 | 4,35 | 149,35 | 90,40 |
| 478 | Tail Cave 19 to 18 | 3,64 | 4,64 | 149,64 | 60,B0 |
| 509 | Tail Cave 18 to 19 | 156,64 | 155,64 | 10,64 | 00,40 |

Room identifiers and fraction bytes are hexadecimal. Coordinates are decimal.
There were 40 integrations on each measured horizontal crossing and 32 on
the downward crossing. The integration tests start with the recorded room,
position and fractions and assert both saved coordinates and retained fractions
after the production coordinator runs. For the two Tail Cave rows, Java is a
controlled room start and the original is a fresh game.

```sh
out=$(mktemp -d)
awk 'NF && !/^#/ { print; if (++rows == 510) exit }' \
    tools/opening/bowwow-rescue-route.tsv > "$out/first510.tsv"
python tools/opening/capture.py --route "$out/first510.tsv" \
    --trace-scroll-positions --output "$out"
```

The 510-row input SHA-256 is
`0933ef429e9772a844f95cdd13d3a7c310d2095b21fcca49523dd8256c03a596`.
`emulator-scroll.tsv` distinguishes the saved entry from resumed motion.
At checkpoint 478 the ROM had already moved left one pixel to (148,64)
after saving (149,64).
