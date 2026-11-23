# Room integration — round 5

Branch: `t3/refactor-room-entity-facade`. All six steps are complete, in separate
commits after merging develop. Work was headless only. Our implementation edits
leave off-limits sources/tests, fixtures, ROM/assets and build configuration alone;
the authorized develop merges preserve incoming composition, recording and build
changes. Nothing was pushed. This is a refactor and pacing correction; game
updates preserve input order, slot order, byte arithmetic, transitions and indexed
output.

## Step status

| Step | Outcome |
| --- | --- |
| 1. Full-frame baseline | Added three `@RequiresRom` ordinary-input scenarios and a pixel/ownership comparison of retained and owned capture APIs |
| 2. Running fast paths | Session refills reusable inputs, reads live room/dungeon views and builds borrowed scenes; screen overrides and owned capture/history APIs remain |
| 3. Allocation remainder | Removed empty snapshots, palette copies, iterators, duplicate frame wrappers and unchanged result/metadata allocations; tightened guards |
| 4. Typed ROM and retirement | Five wiring switches applied; 23 unused deprecated members removed; no unsafe-byte access remains; all Java warning counts zero |
| 5. Pacing | One rational 60 Hz clock, spurious-wakeup checks, short final spin, catch-up without deadline resets, interrupt-safe exit and no second vsync clock |
| 6. Documentation | Architecture, render lifetime and every allocation guard/budget documented |

The six new pacing tests, ROM slice allocation guard, reusable assembly guard,
session pixel comparison and four initial whole-frame/capture tests increase the
suite from 2,909 at integration to 2,922. The final develop merge adds another
24 tests, including gameplay recording and the empty-allow-list implementation
inheritance guard. Final default gate: **2,946 tests, zero
failures/errors, one existing skip**. Every implementation commit passed
`LA_ROM=/path/to/user/rom ./gradlew spotlessApply test`. The same gate is run for
the documentation commit. All 986 scene-parity captures remain identical, plus
the render/special parity tests and 80 live-session inventory-transition frames.
No game-wide or independent emulator parity claim is implied.

## Whole-frame allocation

All values are calling-thread bytes/frame, measured by `AllocationProbe` after
warm-up. Setup uses normal input from a new game: opening dialog remains open;
walking leaves the bed and alternates left/right with room entities visible;
inventory is opened by Start and remains fully open. No teleport/debug state
is used for these three scenarios.

| Scenario | Baseline EA on | Final EA on | Baseline EA off | Final EA off | Guard |
| --- | ---: | ---: | ---: | ---: | ---: |
| Opening dialog idle | 23,224 | 1,008 | 27,320 | 1,072 | <=1,340 |
| Walking in the starting house | 24,219.2 | ~885 | 28,315.13 | ~1,029 | <=1,287 |
| Fully open inventory | 20,344 | 720 | 20,344 | 720 | <=900 |

EA-off measurements use a temporary local JVM init script; no build setting is
committed. Small walking variations reflect window phase and JIT compilation.
`headlessFrame(retainedBuffer)` runs the full simulation/assembly/build/compose
path and copies pixels into caller-owned output. The no-argument API keeps its
owned framebuffer allocation and is tested pixel-for-pixel against the overload;
that separate capture allocation is not counted as runtime overhead.

Update-only idle/active measured **64 / about 673 B/frame EA off** and
**0 / 641 EA on**, guarded at **80 / 842**.
Reusable render assembly and assembly+scene measured **0 / 0 B/frame**, both
asserted exactly zero. The owned capture/scene reference path keeps its existing
tighter bounds; it remains available for callers needing history. Positive new
budgets are measured EA-off cost plus approximately 25% rounding headroom.
See [TESTING.md](../../TESTING.md#performance-budget) for every guard and scope.

## Attribution and residuals

A separate warmed JFR diagnostic recorded 2,000 full frames per scenario with
escape analysis and TLABs disabled and `jdk.ObjectAllocationOutsideTLAB` stack
traces enabled. Allocation events were filtered to the frame-producing Java
thread. Diagnostic source rates below are rounded steady rates; rare JVM linkage
strings/byte arrays are excluded. JFR files/runners remained in `/tmp`.

Paths are under `java/src/main/java/linksawakening/`.

| Off-limits source | Idle B/frame | Walking B/frame | Inventory B/frame |
| --- | ---: | ---: | ---: |
| `dialog/DialogFont.java:tileFor` | 800 | 0 | 0 |
| `dialog/DialogController.java:visibleText` (byte data + String) | 80 | 0 | 0 |
| `ui/InventoryMenu.java:setWorldContext` | 24 | 24 | 24 |
| `ui/InventoryMenu.java:setDungeonContext` (two arrays) | 104 | 104 | 104 |
| `startup/MarinWakeUpMotion.java:tick` | 32 | 0 | 0 |
| `startup/MarinWakeUpMotion.java:tickMarinPresentation` | 32 | 32 | 0 |
| `physics/OverworldCollision.java:groundInteractionSample` | 0 | 160 | 0 |
| `entity/LinkTunicPalette.java:forTunic` | 0 | 32 | 0 |
| `entity/LinkSpriteComposer.java:submitOam` | 0 | 32 | 0 |
| `entity/LinkDash.java:consumePegasusScreenShakeRequest` | 0 | 24 | 0 |
| `startup/TarinShieldMotion.java:Update.unblocked` | 0 | 24 | 0 |
| `ui/InventoryInstrumentPanel.java:submit` | 0 | 0 | 256 |
| `ui/InventoryInstrumentPanel.java:tilemapForInstrument` | 0 | 0 | 256 |
| `ui/InventoryInstrumentPanel.java:palettes` | 0 | 0 | 80 |
| **Off-limits total** | **1,072** | **432** | **720** |

Owned idle and inventory sources measure zero. Thus idle's 256-byte target is
exceeded solely by the listed off-limits 1,072 bytes, with **zero residual owned
idle allocation**. Inventory meets 1 KB including all UI allocation. Walking
meets 2 KB including about **592 B/frame** of genuinely changed owned immutable
input/boundary values: EntityFrameContext, projectile Link state, room boundary
Link/input/state, app Link position/motion/ground/pickup/combined inputs and the
tick-only entity input. They remain owned values because callers can retain them;
mutable reuse would weaken that contract. No frame-cycle-specific interning was
introduced just to optimize the probe. Actual emitted immutable events and
changed coordinate-dependent sprite geometry can still allocate.

Avoidable owned costs removed include empty rupee/VFX snapshot requests, rupee
palette clones, dialog-sound and room-event iterators, repeated none values,
duplicate combat/pickup frame conversions and unchanged collision/room metadata.
Active VFX still uses the off-limits snapshot API; its no-event path no longer
requests it. Combat/pickup record implementations share read-only input contracts;
their original field evaluation and validation order is retained.

## Recording integration

The recording feature, its tests, launch options, frame limit, toggle callback,
capture lifecycle and cleanup are preserved. Both headless capture overloads run
through the same recording-aware frame hook. With recording disabled, its added
steady allocation is **0 B/frame** in the measured default host path: JFR shows no
recording allocation sites, the null-recorder branches allocate nothing, and the
existing reusable PCM output avoids the decorator's fallback clone. The decorator
and callback are initialized once, not per frame. Recording-on capture is not
optimized or included in the non-recording budgets. No manual recording or native
window was started; the feature's normal tests are included in the full gate.

## Boundary and ownership

`RoomRenderView` is the common indexed contract for owned `RoomRenderSnapshot`
and cached live projections (the existing nested `View` name remains compatible).
The live room/dungeon values are valid only on the same thread until the next
simulation tick, room change or world mutation. Reusable renderer inputs/scene
storage are valid until their next refill/build. Array reads do not expose
writable world storage. `renderSnapshot()`, `ownedCopy()` and reusable input
`snapshot()` explicitly retain history; scrolling's previous room stays owned.
`currentGameFrameState()` is the owned capture path; ordinary composition refills
`currentReusableGameFrameState()` and preserves gameplay/file-menu/save overrides.

All five typed ROM wiring switches are applied. `RomSlice` supplies bounded
primitive offset reads without per-byte `RomAddress` construction; signedness,
checks and caller-owned storage remain unchanged. Exact backing identity is
asserted by a test-only helper rather than a runtime array escape. The five
entry-point tests now require the typed API and absence of the retired byte form.
The world route's Feather registration targets the airborne role directly.

After merging composition and recording, fresh main/test compilation with temporary `-Xlint:deprecation,removal` and
`-Xmaxwarns 10000` reports **zero `[removal]` and zero `[deprecation]` warnings**
for every package: app, gameplay, world, render, gpu, audio, rom, save, config,
input, architecture and the unchanged off-limits packages. Owned deprecated
members: **23 -> 0**. `unsafeBytes()` calls: **11 -> 0**, and the accessor itself
is deleted because no caller remains anywhere. No compiler suppression was added.

## Frame pacing

The old loop truncated the fractional frame period and reset its clock whenever
a frame overran. The new `FramePacer` carries `1_000_000_000 % 60`, counts the
whole frame's work/presentation time, and preserves pending simulation deadlines.
At a sustainable workload it averages exactly 60 steps per wall-clock second,
catching up after stalls rather than silently losing simulation time. It parks
for most of the wait, spins only within the final 100 microseconds, rechecks
spurious wakeups and exits on interruption without clearing the flag. Headless
entry points never invoke it.

GLFW swap interval is zero so the explicit pacer owns cadence independently of
monitor refresh; framebuffer pixels are unchanged. Native presentation/tearing
was not visually exercised, in accordance with the headless-only rule. Existing
OpenAL prefill covers three gameplay frames (one playing plus two jitter frames),
with underrun recovery and bounded queue-pressure rebuffering left unchanged.
Six fake-clock tests verify long-run rational timing, oversleep recovery,
spurious wakeups, overrun catch-up, interrupt preservation and clock wraparound.
Sustained workloads slower than 60 Hz cannot physically meet real time; the loop
keeps doing real simulation/render work rather than introducing a separate busy wait.
