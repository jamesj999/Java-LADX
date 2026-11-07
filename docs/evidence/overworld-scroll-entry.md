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
then run `EntryProbe after` from the repository root with that classpath plus
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
classpath. Java `EntryProbe phase-after 210` reproduces the matched phase;
`capture_rom.py` prints the source CPU gate trace and post-completion positions.
No image assets are committed. Focused grass/Gel tests and existing entry
regressions pass, including a new test with skipped Link updates that checks
the global phases211/212/219/220.
