# Slime Eye — controls-only fight and live ROM checkpoints

This follows the connected encounter milestone in
[the earlier evidence](slime-eye-half-motion.md). It closes the assisted
half-defeat limitation, while preserving the earlier isolated zero-difference
claims at their original scope.

## Production run

`SlimeEyeControlsTest` runs the real `GameSession` input, Link/equipment,
combat, entity, sound and framebuffer paths. Setup skips the opening bed/shield
sequence, grants a level-one sword and Pegasus Boots, gives six full hearts,
loads D3 map `02` room `5A`, places Link once at `$48,$50` facing up and starts
one screen shake of 7. All writes end before the first gameplay frame. The
normal frame order initializes the boss, decrements the shake to 5 and triggers
the ceiling fall. The boss intro is advanced with normal A-button presses.

After the setup cutoff the policy only calls `headlessButton` and
`headlessFrame`: no Link relocation, boots reset, enemy state write, RNG
write, direct combat invocation, damage override or immunity grant. It walks
to reachable cardinal sword poses, pulses A, charges boots at full stretch,
keeps the blade held during the split, pursues the halves, and walks into the
reward. Both halves die and max-heart capacity increases from six to seven.
The final engine run takes 1,302 frames and retains 40 health units before the
heart's healing sequence completes. Inputs and engine state are written to
ignored `java/build/slime-eye/controls/engine.tsv`.

The older `SlimeEyeSessionTest` remains explicitly assisted: it repositions Link
for attack-pipeline captures. Its former fixed-standing split input stopped
working after the correct sword push was implemented; the replacement whole
fight test moves using normal directions. This is a correction to the prior
fixed-standing input evidence, not a new claim that its old pilot still works.

## Live original-ROM run

`tools/slime-eye/live.py` starts the original game, redirects the house door
warp structs to D3, and lets the ROM load the room graphics. It then sets the
initial boss slot to INIT, initial Link/equipment/health/clock/seed state, clears
other entity slots and starts the same shake. Exact setup writes are saved
locally in `live-emulator.json`. The setup cutoff is marked in the tool.
After it, only normal button events and emulator ticks mutate gameplay.
A read-only hook at `GetRandomByte`'s return records RNG observations; it does
not override RNG or CPU registers. ROM SHA-256:
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.

The same bounded cardinal-pose policy independently responds to each run's
actual state. It is not a shared fixed button transcript once trajectories
diverge. The ROM run completes in 1,438 frames, retains eight health units, and
increases max-heart capacity from six to seven.

## Source-backed corrections found by the live comparison

- Bank `03:7081–70B6` applies the Slime Eye sword special case to halves too.
  Pegasus contact calls `func_003_6DDF`: reset boots, push Link with vector
  length `$14`, and ignore Link collisions for `$10`. Ordinary hits push with
  length `$10` and ignore collisions for four frames. The former controller
  omitted these Link responses and could keep running after the split.
- `6DDF` clears the boots flag before sword damage selection. Retaining the
  pre-collision attack context incorrectly made the first half hit deal two
  damage. The actual ROM first boots hit changes HP from five to four.
- `ApplyRecoilIfNeeded_04` at `04:6D80–6DC9` discards its caller's return address
  at `$6DC8`. The half's hop and Z state machine must stop for that slot visit.
  The former controller applied recoil and continued Z integration.
- Slime Eye owns its ignore-hit countdown at these source call sites; it is
  now excluded from the generic ignore-hit tick. Fresh sword-hit recoil starts
  on the next visit, flash is not prematurely aged, and a lethal hit starts
  destruction on the following visit.

Focused regressions cover the five-to-four boots hit, actual Link response,
ordinary four-frame push and held Z during ten following recoil frames.
No generic combat-phase reorder was made.

## Compared checkpoints

`tools/slime-eye/live_compare.py` reports both runs without asserting identical
trajectories or pictures. Both naturally allocate half slots 14 and 15 at five
HP, reset the run on the first half hit, keep that half's Z at 1 through ten
following frames, and clear each dying half 225 frames after its lethal hit.

| Checkpoint | Engine frame | Original-ROM frame |
|---|---:|---:|
| First body state 3 | 253 | 256 |
| Two-half split | 457 | 428 |
| First boots half hit, HP 5 → 4 and run reset | 459 | 430 |
| Slot 14 lethal hit / clear | 891 / 1116 | 1084 / 1309 |
| Slot 15 lethal hit / clear | 963 / 1188 | 1072 / 1297 |
| Reward capacity increase observed | 1301 | 1437 |

Production and original-ROM screenshots are paired under ignored
`java/build/slime-eye/controls/`: `body.png` / `body-emulator.png`, `split.png` /
`split-emulator.png`, `first-death.png` / `first-death-emulator.png`, and
`reward.png` / `reward-emulator.png`. They include actual Link/HUD and room
presentation and were visually inspected. These are semantic checkpoints with
different live state, not fixed-pose pixel matches. Full-frame RGB555 mismatch
counts are 339, 1,485, 1,494 and 1,473 respectively (of 23,040 pixels per frame).
The report includes both input-policy source hashes.

## Verification and remaining gaps

The focused Slime Eye, Sword, sound-routing and required architecture selection
passes 91 tests. The selection's 2,694 internal scene comparisons are identical.
This is engine-internal compositor parity, not emulator parity. No full suite
or normal-route D3 replay was run. Reproduce the original-ROM run and report:

```
python tools/slime-eye/live.py
python tools/slime-eye/live_compare.py
```

Live fight trajectories, button histories, damage taken, sprite phases and
whole frames do not match. The read-only RNG trace identifies concrete causes
outside the half controller: the ROM consumes a footstep RNG call returning to
`00:1547` before its first body-wander call; the engine Link footstep path does
not consume that call. Starting at seed `$A2`, the ROM's first observed call at
frame 194 / LY 17 yields `$BA`; its first body-wander return to `04:4B85` at
frame 2 / LY 29 yields `$EC`. `GameSession` currently supplies scanline zero to
its RNG, so neither consumption cadence nor LY matches. The three-frame body
state-3 entry difference under these dialog/input clocks also remains open.
These are explicit followups; no RNG values were forced to obtain this run.

Collision placement relative to the earlier generic combat pass, complete
reward/healing/OAM presentation timing, and sound waveform equivalence remain
unverified against the emulator. The earlier fixed-pose and injected-handler
zero-mismatch evidence does not resolve these live-fight differences. This
milestone establishes a complete controls-only isolated encounter and the
specific source corrections above, not a normal D3 approach or game-wide parity.
