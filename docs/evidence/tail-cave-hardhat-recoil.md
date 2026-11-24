# Tail Cave Hardhat contact and recoil

The 843-checkpoint rescue route with SHA-256
`2ab4fefcf61a853ccc5fc31b48e2f5c1a5a9bf6dd0c8b9f1d17b9051c80cafd4`
enters Tail Cave map `$00`, room `$16`, at checkpoint 323. On the
Java build before the Hardhat changes, Link enters at (10,72) with 16
health. During checkpoint 324's unarmed Right input he takes eight
damage and reaches (104,92), with eight health. The next Down target
of y80 is already satisfied, so checkpoint 325 leaves him at y92.
The run dies before exiting right during checkpoint 326. In the
unmodified PyBoy replay, Link enters at (11,72) with 24 health, passes
checkpoint 324 at (104,72), and exits with 24 health.

A temporary Java input diagnostic added pulsed A and held B to
checkpoint 324 while retaining the ROM's Right input. It reached
(104,72) at 16 health, completed checkpoint 340, and died in Tail Cave
room `$13`. Holding B without A on that build reproduced the earlier
death during checkpoint 326. The diagnostic investigates timing and sword
interactions. The standard route retains its recorded inputs.

## Controlled ROM probes

Use the normal route to enter room `$00:$16` and finish checkpoint 323
before changing any state. Leave one active Hardhat in slot 0. For the
movement probe, set raw position (104,72), fractions `$80/$80`,
ordinary speed (8,8), recoil speed (8,8), and ignore-hits countdown 3.
The ROM enters `$06:4F4E` on each frame. On the first three frames
its `$06:653F` return skips both the contact check at `$06:4F51`
and ordinary movement at `$06:4F54`.

| Frame | Raw position | X/Y fractions | Ignore-hits countdown |
| --- | --- | --- | --- |
| Before | (104,72) | `80/80` | 3 |
| First | (105,73) | `00/00` | 2 |
| Second | (105,73) | `80/80` | 1 |
| Third | (106,74) | `00/00` | 0 |
| Fourth | (106,74) | `80/80` | 0 |

Ordinary speed stays (8,8). The fourth frame uses the same fraction
bytes for ordinary movement. The source applies the handler's
non-interactive guard at `$06:4F4B` before the recoil step.

For a separate shield probe, use a stationary Hardhat at (104,72),
no initial recoil, and set Link to (90,72) facing right with B held
and no sword collision. The ROM reaches `$03:6F93` at gameplay clock
234. Link gets collision-ignore countdown 12 with no pending damage,
and the Hardhat gets recoil (+32,0) and ignore-hits countdown 10.
It stays at X104 on the contact frame and advances to X106 on the
following frame. The pose has no geometric body overlap.

With the shield released and Link at the Hardhat's exact position
(104,72), slot 0 skips body contact at clock 234. At clock 235 it
reaches `$03:6DB4`, queues eight damage and sets Link's
collision-ignore timer to 16. This is the existing
`(hFrameCounter XOR slot) & 1` body-contact cadence.

Java now allows the same Hardhat shield rectangle, applies new weapon
recoil from the handler's contact phase, and uses the ordinary
fractional accumulators on subsequent bank-$06 recoil frames.
The regression covers the first contact frame, active recoil on a
steering frame, and the dialog gate.

The isolated Java build with this change and the then-current
scroll/recoil rules completed 390 checkpoints on the standard route.
Checkpoint 324 reaches (104,72) with the incoming 16 health, checkpoint
325 reaches (104,80), and checkpoint 326 enters room `$17` at (10,80).
The next route blocker is the reward chest sequence in Tail Cave
room `$14`, with one evasive Stalfos still active. Fifty-three
focused tests passed, including the existing progression fixture.

## Room `$14` reward chest

The first post-Hardhat replay leaves one evasive Stalfos at (22,30).
The ROM has cleared both evasive Stalfos by checkpoint 355, then
defeats the last aggressive Stalfos by checkpoint 383. The room event
checks for remaining enemies at `$02:5FFC`. Java's inherited extra
sword wait at (80,32) leaves the lone Stalfos active. Moving left from
there also encounters the room's partition at X68.

A controlled Java game-loop probe entered map `$00`, room `$14`,
with Link away from the reward tile. Chest cell `$28` kept its
original floor object `$0D` while an evasive Stalfos remained.
After the last enemy was cleared, the cell became `$A0`.
Ordinary A at (135,58), facing up, changed it to `$A1` and opened
the reward dialog. This matches the original checkpoint 391 approach.

Two Stalfos motion discrepancies were independently reproduced in
PyBoy after the normal approach to checkpoint 349:

- Place a grounded evasive Stalfos at (64,72) with velocity (16,0),
  and Link at (100,72) holding B. The ROM integrates to X65 first and
  reads the resulting distance 35 at `$15:4F69`. It enters the jump
  at `$15:4F78`, with timer 8 and X velocity `$EE`. Java now uses
  the post-movement and post-background-rollback coordinates for
  both the jump window and its velocity vector.
- Place the Stalfos at (22,30), at rest between handlers, with both
  fractional accumulators `$80` and both velocities 6. With the
  random reversal branch at `$15:4FD9` held on its existing-direction
  path, the ROM probes both solid axes even though neither integer
  changes. It records collision byte `$09` and reverses only X
  (`$06 → $F6`) through the horizontal-first branch at `$15:4FAE`.
  Java now uses the same fraction-only probes and axis priority.

Fourteen focused Stalfos and progression tests passed. The later
integrated fresh route encountered an earlier blocker in room `$62`
after checkpoint 111, so the controlled comparisons establish the
Stalfos behavior and chest trigger. A continuous chest acquisition
with the latest integration is still unverified.

## Room `$03` Spiked Beetles and Feather stair

The native shield comparison uses the normal approach through
checkpoint 443, then one active Spiked Beetle at (89,75) and
Link at (103,80) facing left with B held. CPU hooks at
`$03:6F37`, `$07:77C7`, and `$07:780D` separate the stored
state from the active handler state. The shield writes stored
state 3, horizontal speed `$F0`, Z speed `$20`, and timer `$FF`.
On that contact frame the walking handler remains active. The
beetle moves to (88,75,Z2) with its existing fractions
`$80/$60/$D0`, options `$48`, and hitbox flag `$80`.
On the next dispatch its timer is `$FE` and the flipped handler
sets options `$08` and hitbox flag zero. In an aligned (89,80)
walking case, the normal handler's dash check instead increments
the newly written state 3 to 4. The ROM skips the state table
for that value at `$07:780F`.

A separate native dash-state probe starts with X fraction `$E0`
and its previous dash directed right. The contact frame integrates
left to X88. The still-active dash handler adjusts the new X speed
from `$F0` to `$F1`. The next two X positions are 87 and 87,
with the retained fraction carrying on the third frame. Java now
preserves the fractions and timer on contact and executes the
state latched at the beginning of the visit.

The original room `$03` warp bytes are `E2 00 19 78 10`.
PyBoy's checkpoint 473 reports the new room number during load,
before Link's position has been set. After the next 64-frame wait
the original and Java both report the room `$19` spawn (120,16).
The controlled Java continuation uses the route's normal buttons
from a directly loaded room `$03` at (148,56), with 16 health and
sword and shield equipped. It defeats both beetles, takes the revealed
stairs, and opens the Feather chest at checkpoint 489 in room `$1D`,
at (72,58). All 14 targeted Spiked Beetle, Feather and progression
tests passed, including the existing fixture with the source's
extra dispatch before the underside can take damage.
