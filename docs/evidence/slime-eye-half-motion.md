# Slime Eye — connected encounter and bounded comparison evidence

Follow-up: [controls-only full fight and live ROM checkpoints](slime-eye-controls.md)
supersedes this milestone's assisted-half limitation and fixed-standing pilot
evidence. The original fixed-pose/isolated-trace comparisons remain bounded.

The former unused half-motion helper is now called by a registered entity `$5B`
handler. `SlimeEyeController` owns the bank-04 slot state and uses the existing
room runtime, combat outbox, lifecycle, boss reward and ROM sprite services.
Runtime tables and graphics are read from the user's ROM. No extracted assets
or ROM-containing captures are committed.

Source: `LADX-Disassembly/src/code/entities/04_slime_eye.asm`, bank-03 sword
special case `$707D–70B6`, `EntityInitZol` and `06_zol.asm`, and bank-02
`$42EE–4315` / `$4827–48AD` for held sword collision during a Pegasus run.
The curated symbols include exact `Data_004_496B/496D/4B12/4B5C/4B6C/4BDD/
4D9D/4E94/4E9C/4FAE/4FEE` labels. These bank-04 data reads do not use a
GBC bank adjustment; room sprite sheets and palettes retain their existing
ROM loader path.

Connected behavior includes hidden ceiling wait, capped Zol spawns, exact
screen-shake countdown `$05` trigger, accelerating fall, impact shake and
Link stun, recovery, body wandering, sword-induced stretching and relaxation,
Pegasus split delay and two separately allocated five-HP half slots. Halves
hop and steer using ROM velocity tables, accept damage and recoil, rise after
successive hits, hide above the room, drop at Link's current position, stun a
grounded Link on heavy landing, and run independent destruction timers. The
last remaining Slime Eye slot produces one heart container through the existing
boss reward path. Clearing or reusing slots resets the encounter state.

The live combat phase now preserves the held blade's ROM collision rectangle
while running with Pegasus Boots. The previous static-object collision gate
otherwise suppressed this required encounter input. Jingles `$08/$0B/$20/$24`
and noise `$0C/$13/$1A` reach the gameplay sound routing; the added mappings
use their actual ROM SFX namespace and IDs.

## Focused engine verification

`SlimeEyeRuntimeTest` exercises the production registry from the initial entity
through main-body fall/stretch/split, both half deaths, and one heart container.
A second encounter uses the public `RoomSession` facade loading actual D3 map
`02`, room `5A`, through split and defeat. This is direct room setup, not a
normal progression route. Tests also cover arrow immunity from the ROM damage
table, lethal sword death dispatch, slot cleanup, and ROM body/half/shadow
OAM tuples. Existing half-motion tests remain connected to the production use.

Run from `java/`:

```
./gradlew test --tests '*SlimeEye*' --tests '*SwordTest' \
  --tests '*EnemyCombatEventConsumerTest' --tests '*GameplaySoundEffectMapTest' \
  --tests '*WorldDependencyTest' --tests '*MainArchitectureTest' \
  --tests '*GlBoundaryTest' --tests '*WorldRomConstantGuardTest'
```

This focused selection passes 88 tests, zero failures/errors/skips. The five
room fixtures and 1,069 production-session frame compositions also pass the
engine's internal scene parity checks. The full
suite and opening-route replays were not run for this milestone.

## Production session and visible gameplay

`SlimeEyeSessionTest` uses `GameSession.headlessFrame()` through the production
input, simulation, combat, entity consumption, audio and compositor paths.
The local `session-body.png`, `session-halves.png`, `session-defeated.png`
include the actual Link, HUD, animated torches and shutter presentation.
These were visually inspected. They are distinct from the fixed visual probes.

Setup is explicit: skip the new-game wake/shield sequence, grant sword and
boots with 14-heart capacity, directly load map `02` room `5A`, place Link at
`$50,$60` facing up, then inject one scroll-controller shake of 6 after 180
frames. The real gameplay frame decrements it to 5 and passes it into the
boss ceiling trigger. No boss state or OAM bytes are written.

The subsequent live comparison found the missing ROM sword push and recoil
return behavior. After those corrections, the old fixed-standing split pilot
no longer works because correct sword recoil pushes Link out of range. Its
capture harness now explicitly repositions Link for both body and half combat;
it remains an assisted attack-pipeline check. The new `SlimeEyeControlsTest`
completes the whole encounter with actual movement/buttons after one initial
setup. See the follow-up evidence for the current verified inputs, captures,
test counts and live comparison limitations.

## Isolated emulator comparison

Tools: PyBoy 2.8.1; `tools/slime-eye/capture.py` and `compare.py`. ROM SHA-256:
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.

The trace probe starts a new game in the house, clears entity storage and
injects explicit body-drop, half-rise/drop and half-death states. Link's target
position is held at `$50,$60` each frame. A hook at `GetRandomByte`'s return
`00:281D` sets returned register A to zero, matching the engine fixture's random
supplier. The engine fixture presets the same handler state and skips the boss
intro; it does not copy emulator state each frame. The frame alignment accounts
for the emulator's incremented displayed counter versus the handler input
counter. Exact writes are saved locally in `*-setup.json`.

Compared fields per frame: status, handler state, X/Y/Z, Z speed, transition
countdown, sprite variant, private state 4, slow countdown and health:

| Probe | Frames | Mismatched active/death rows |
|---|---:|---:|
| Body drop, recovery and initial wander | 145 | 0 |
| Half rise, hidden wait, targeted fall, landing and next hop | 145 | 0 |
| Half destruction countdowns and terminal clear | 226 | 0 |

When both statuses become zero, backing bytes are excluded: the ROM retains
irrelevant bytes while the engine replaces a cleared slot with a disabled value.

For the visual probe, the house door's four warp structs are redirected to
map `02`, room `5A`, letting the ROM load the actual room, sprite sheets and
palettes. Boss intro is suppressed. Fixed visible body and two-half poses are
injected, Link is frozen, and three display frames allow OAM/DMA presentation.
The engine uses the same room loaders and ROM display lists at the specified
poses. This is separate from the state trace probe.

Both image crops `(24,24)–(128,72)` match all 4,992 pixels each in RGB555.
The comparison maps displayed channels through `>>3`: PyBoy displays RGB555
with `<<3`, while the engine expands five-bit channels to eight bits. This
normalizes their documented output scaling; it does not alter geometry or
palette indices. The crop excludes the different Link/HUD setup. Outside this crop, the Java
fixture's corner flames differ: the fixture does not call `gpu.tickAnimatedTiles`,
while room `5A` selects group 7 and its animation path uploads tiles `$16C–16F`.
Flame phase has not been matched independently. The top doorway also differs;
its isolated-fixture cause was initially unresolved. The production session
capture runs animated tile updates and the room shutter lifecycle; it visibly
has the lit corner flames and closed upper doorway. Neither exterior region
is included in the fixed-pose pixel-parity claim, and no whole-screen emulator
match is asserted.

Commands (after the focused engine tests have written local fixtures):

```
python tools/slime-eye/capture.py
python tools/slime-eye/compare.py
```

All captures, per-frame traces, exact setup writes and `comparison.json` remain
under ignored `java/build/slime-eye/`. Reviewable local screenshots include
`body.png`, `halves.png`, `defeated.png` from live room-facade progression and
matched `body-fixture.png` / `body-emulator.png`, `halves-fixture.png` /
`halves-emulator.png`.

## Remaining comparison scope

No normal-route D3 approach/entry/completion is verified. The emulator probe
does not compare the whole battle's live sword, boot, background collision,
random-source cadence, sound waveform or reward-collection sequence. The
production-session input-only check covers the body stretch and split after
its disclosed initial combat pose; later half combat uses test positioning. It does
not establish game-wide parity. Split and defeat are verified through the
production engine registry/facade, while the emulator comparisons cover only
the explicit state traces and fixed visual crops above.
