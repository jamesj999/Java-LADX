# Forest return on the rescue route, 2026-11-24

The fresh rescue replay enters overworld room `$52` at raw (56,127) in both
runtimes. The health gap already exists at that point. On the historical
route, Java enters with 8 health units and PyBoy with 20. Java still had 16
at checkpoint 143 in cave `$0A:$AC`, before walking south from (56,48).
It took two four-unit hits from the hiding Zols on that leg and settled at
(60,80) after recoil. PyBoy reached (56,65) on the same leg with 24 health.
Neither runtime lost health in `$62` on this baseline leg.

Two input changes were tested together. They remove A pulses from the Java
eastward leg in `$AC` and use the existing ROM Up+B policy for the Java
ascent from `$62`. The ROM receives the historical inputs and timing in both
segments.

| Checkpoint | Historical row | Candidate row |
| --- | --- | --- |
| 143 | `right+a* x=56 4` | `right x=56 4 right+a* - 4` |
| 155 | `up+a*+b room=0x52 4 up+b` | `up+b room=0x52 4` |

Removing the eastward A pulses let Java reach (56,64) at checkpoint 144 and
exit the cave with its original 16 health. With this change alone, the north
leg in `$62` lost those 8 units to a Moblin arrow. The trace places Link at
(56,37) with zero Y speed for eight consecutive frames while a sword swing
is active. The northbound `$0C` projectile advances from (56,56) to (56,42)
at two pixels per frame, disappears, and the next frame starts Link recoil
and health subtraction. This is consistent with the north projectile speed
`$E0` from `$03:5943` and the ordinary sword movement block at `$02:4807`.
Removing the northward A pulses too brought Java to `$52` with 16 health.
Removing the 197-frame Java-only cave wait, shortening the 24-frame
Java-only southern-leg idle, and making both idle changes still left Java
at 8 health by the end of that leg.

The historical route SHA-256 is
`62c1e95dd6d63f459a0cc541578591a90508daddf82d0b6612101d807bde3804`.
The two-row candidate SHA-256 is
`52cebcfd15553041956b7a9f08e89047643e317366d7d1fca9e02b8380de0c62`.
PyBoy 2.8.1 completed all 843 checkpoints with BowWow state 1 for both.
Its full state trace was byte-identical. In an earlier Java build the
candidate crossed `$52` and `$42` and completed checkpoint 301 before a
later death. Because the enemy phases depend on the engine revision, the
candidate's local health improvement does not establish repeatable rescue
progression.

The integrated build, including the Moblin motion fixes and animated tile
counter reset, completed 155 checkpoints on the historical route and died
in `$52` at frame 20508. The two-row candidate completed 157 checkpoints
on that build and died at (105,42) in the same room at frame 20858.
Using the existing ROM Up+B policy for checkpoint 156 as well
(`up+b y=64 4`) completed the outward crossing. This third row gives the
current input route SHA-256
`d21b8ba1c6f702689be10cda86e58b564959f14427431ac5c02be56e693ad208`.
The full PyBoy state trace still matches the historical 843-checkpoint
capture byte-for-byte. No ROM override or idle was added to the crossing.

| Checkpoint | Java room | Java raw X,Y | Java health |
| --- | --- | --- | --- |
| 155 | `$52` | 56,127 | 16 |
| 156 | `$52` | 65,64 | 16 |
| 158 | `$42` | 92,127 | 8 |
| 160 | `$42` | 92,88 | 8 |
| 166 | `$43` | 100,96 | 8 |

The current Java route then finishes the Witch sequence and completes
checkpoint 221. During the return south through `$52`, it dies at frame
27582 at (67,56). These measurements predate the per-slot combat latch
and sword/shield rectangle arbitration changes.

On that build, the first return-leg loss occurs in `$42` during checkpoint
218. At frame 27014, Link is at (120,74), facing south with the shield held
and sword in state 3. The ordinary Moblin enters its dying state on the
same tick Link queues four damage. The ROM's ordinary Moblin dispatch
checks body collision before sword damage at `$03:585B → $03:6E28`, which
allows both results on one dispatch.

Link enters `$52` from the north with four health units. At frame 27574
the left-moving arrow last seen at (80,67) disappears as Link reaches
(75,63), and Link queues eight damage. The raw coordinate deltas
(-5,-4) fit the ROM's pre-motion projectile window at `$03:6BEA`. The
ROM tests for a shield facing right against a leftward arrow at
`$03:6C41`, and Link faces south in this Java encounter. A ROM replay
through checkpoint 227 with CPU hooks at `$03:6C50`, `$03:6C5B`, and
`$03:6DB4` found no shield block or damage during the corresponding
`$42` and `$52` checkpoint legs. It crossed into `$62` with 20 health.
The enemy positions already differ when checkpoint 221 ends, so the
two captures exercise different contacts. A separate same-pose ROM probe
found that the arrow's group `$0D` gives four nominal damage, versus eight
on this Java build. Either damage amount consumes the four units remaining
at that contact.

Two temporary Java input variants advanced the replay on that build.
Running Down+B during checkpoint 222, with the original ROM input
retained, cleared `$52` at four health and died in `$62` after checkpoint
225. Skipping the Java-only participation in the 120-frame stationary
wait at checkpoint 217 preserved eight health through both `$42` and
`$52` and reached checkpoint 227 before dying in `$61`. These variants
were diagnostic and the current route retains both historical inputs.

## Earlier forest crossing on the subsequent combat build

With per-slot combat latches and sword/shield rectangle arbitration enabled,
the replay encountered a different blocker before the cave. Checkpoint 74
starts in `$F1` at (120,48) on both engines, with Left, pulsed A and held B.
Both have the initial non-damaging Leever shield recoil (+9,-18) from
Link position (110,48) and reach y35. Enemy positions already differ;
checkpoint 73 ends with the Java Octorok at (76,32) and a rock at
(116,32), while the ROM Octorok is at (56,30) and there is no rock.
The ROM exits into `$F0` at (148,35) with 24 health.

The first observed Leever state divergence is during checkpoint 72.
Both engines start from the same two spawn positions, (56,48) and
(88,80), and set the emergence timer to 31. At the transition to
chase, their west/east timers are 175/120 in the ROM and 118/128 in
Java. Hooks at `$04:7F2C`, `$00:2817`, and `$04:7F2F` recorded
the two ROM draws in slot order east, west. Both read gameplay counter
25. The east draw reads seed 94 and scanline 25, producing 72. The west
draw reads the new seed 72 and scanline 30, producing 191.
Java's room simulation uses its host scanline policy of zero for both
draws and has a different gameplay counter and random history. At the
first shield recoil, the eastern Leever has 59 chase frames left in
the ROM and 70 in Java. By the later Java body hit, the eastern ROM
Leever has already burrowed near (101,45).

In a separate controlled ROM replay, a hook at `$04:7F33` replaced
only the committed chase timers with the observed Java values, 128 for
east and 118 for west. The normal RNG calls and input were retained.
The first shield recoil still occurred at frame 8513. The east Leever
was now chasing at (101,43) when it dealt four damage to Link at (95,35)
on frame 8578, confirmed by the health-buffer write at `$03:6DB4`.
The unmodified ROM did not take that hit. The timer-injected ROM
continued into `$F0` at (148,29) with 20 health. The timers explain when
the initial damage becomes possible. The later wall death also depends
on how Link moves after the hit.

The same controlled trace identifies a separate recoil mismatch. On
frame 8583, Link first moves to (90,29) under the existing recoil.
The next A press enters `UseSword` at `$00:1528` with velocity
(-17,-20) and collision-ignore countdown 11. Grounded sword use
calls `$00:155F → $00:178E`, clearing both speeds while leaving the
countdown at 11. On the Java capture before the fix, the first A
press during the corresponding recoil enters sword draw but leaves
velocity (-11,-20) active, carrying Link from y26 to y15 over the
remaining recoil ticks. The Java grounded draw now clears both speeds
after that frame's motion step. The stored recoil velocity is cleared
alongside the ordinary motion bytes so the next frame cannot restore it.

On this Java capture, the eastern Leever at (101,42) hits Link at
(97,35) on frame 8438. The `$50` invincibility timer begins and
the four units of damage drain over the following frames. Recoil
(-11,-20) first sends Link northwest. Held Left then stalls at x68
as later Leever contacts push him against the north wall. The repeated
damage comes from Leever body contact. ROM and Java decode the same 80
playable cells. At (68,12), a proposed step to (67,12) samples (63,5)
and (63,8) using the source offsets at `$02:6E15`. Both samples land
in cell `$03`, object `$3B`, a solid tile.

A shared normal-input diagnostic replaced checkpoint 74 with
`down+b y=64 4`, then `left+b room=0xF0 4`. Both replays cleared
`$F1` and passed checkpoint 100. Java later blocked at (140,64) during
the Witch return in `$42`, and the ROM blocked on the later hideout
dialog approach. The Java-only version of the same diagnostic uses
`down+b y=64 4 none 0 0` and
`left+b room=0xF0 4 left+a*+b`. The Java endpoint was the same.
PyBoy completed all 844 rows, including the added zero-frame row, and
its per-frame state trace was byte-identical to the 843-row route.

A controlled PyBoy probe separated Leever movement from its transition
timer. Set a single active Leever to chase state 2, position (80,64),
X fraction `$80`, speed (8,0), and transition countdown 32 in room
`$F1`. Set `wDialogState` or `wLinkPlayingOcarinaCountdown` for the
Leever slot at `$14:4D97`, then restore the byte at the matching
`$04:7FCE` or `$04:7EFE` hook. The dialog leaves X, fraction and timer
unchanged. The ocarina still allows movement to X81 with fraction zero,
and the timer stays 32. The Leever handler's `$04:7FA3` check and the
shared transition timer at `$14:4DA8` now use their independent gates
in Java. The runtime regression and 34 related Leever and recoil tests
passed. The normal-input Java replay still hit the same `$F1` wall
after the timer correction.

With the subsequent room-scroll slot allocation, recoil boundary gate,
and grounded sword speed clear integrated, the three-row revision clears
checkpoint 74 without input changes. Java enters `$F0` at (148,37)
with 20 health and completes checkpoint 210. It then dies in `$43` at
(124,58) during checkpoint 211. On the same build the two-row
low-lane diagnostic dies earlier in `$F0`; it stays outside the route.

## Witch return input alignment

Checkpoint 210 used pulsed A on Java's westward approach to (104,96).
The ROM already used Left+B. On the integrated build with the three-row
route, Java travels for 124 frames during that approach and ends it
with a rightward Moblin arrow at (90,92). During the next Up+B leg
the projectile reaches (100,92), touches Link at (104,90) facing
north, and queues four damage on frame 26539. The arrow disappears
and recoil pushes Link northeast into later contacts. The last
completed checkpoint is 210.

Checkpoint 210 now uses the existing ROM input on both engines,
`left+b x=104 4`. On a fresh integrated Java build the approach
takes 86 frames and checkpoint 211 reaches (104,44) with 20 health.
It clears the Witch return, reaches checkpoint 325, and dies during
326 in Tail Cave map `$00`, room `$16`, at (108,63) on frame 41928.
The full 843-checkpoint PyBoy frame, checkpoint, combat, instrument
and Moblin King TSVs are byte-identical to the preceding route
revision. The resulting route SHA-256 is
`2ab4fefcf61a853ccc5fc31b48e2f5c1a5a9bf6dd0c8b9f1d17b9051c80cafd4`.

To replay either version from the repository root, prepare fresh output
directories and run the same input on both:

```sh
out=$(mktemp -d)
route="$PWD/tools/opening/bowwow-rescue-route.tsv"
python tools/opening/capture.py --route "$route" --output "$out"
(cd java && ./gradlew captureOpeningRoute -PopeningRoute="$route" -PopeningOutput="$out")
```

For the historical run, write `git show b5fd0ecf:tools/opening/bowwow-rescue-route.tsv`
to a temporary route file and pass that path instead. The replays use the
English DX v1.0 ROM, SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.

## Fresh passage through the return forest and Tail Cave

On the build with scroll preparation and the original first-shot delay,
the previous input file (`2ab4fefc…`) completed 157 rows. Link died at
frame 21040 in $52 at (80,66). Sharing the ROM controls on rows 157 and
158 alone moved that last pose to (87,53) and still cost the last heart.
The revised route also walks east in $62 on row 112. The ROM keeps its
existing sword pulses on that row. On rows 157 and 158 both engines now
use Right+B and Up+B. These are ordinary button changes in the replay.

With SHA-256
`de3e447dbf39a75bfe8de8c5b9f58ab9d15cc45d8d08d3a0f1523b079331fc6e`
the fresh Java run completed 474 rows, including clearing both Spiked
Beetles and entering the side-view passage in $00:$19 at (120,16).
It entered that passage with four health units and died descending the
ladder at frame 56756, (120,103). In a separate diagnostic, adding sword
pulses to row 475 reached the Feather chest and completed 536 rows.
PyBoy completed all 843 rows and rescued BowWow on the revised route.
All 83,854 emulator frame rows and the checkpoint, combat, instrument and
king tables were byte-for-byte identical to the previous full ROM replay.

Initializing incoming enemies during scrolling improved the early beach
comparison. At checkpoint 42, Java and the ROM now have 24 health at
(104,88). It also changed the later approach to $52. At checkpoint 156
the revised Java simulation reaches X103, to the east of the X88 lane.
The new row 157 approaches X88 from the left-facing direction for Java
and retains Right+B for the ROM.

The subsequent route shares the ROM's shield approach and its 12-frame
north step in the Spiked Beetle room, then uses an eastward sword sweep
for Java to reach the second overturned Beetle at (105,33). The
[side-view comparison](tail-cave-goomba-patrol.md) supplies the measured
wait after the warp. With route SHA-256
`dfeffe8fbc9c8ba7b73b013b89f80f97de20bbe76e299509a888958aef6031de`,
fresh Java reaches Roc's Feather, equips it and completes 535 rows.
It enters the second visit to $00:$0E with four health and dies on
frame 61340 while crossing east at (100,112).

## Fresh rescue through the hideout

The current [controller route](../../tools/opening/bowwow-rescue-route.tsv)
has SHA-256 `7a70de662ac03c03fa228cdca735b2fa7205b3363204ff9719609254e1b91422`.
From a fresh game, the standard Java capture and PyBoy 2.8.1 both complete
all 899 checkpoints. The ROM was English DX v1.0, SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
Both runs use ordinary controller input with no memory injection. The route
has 203 explicit ROM overrides, including zero-frame rows where only Java
needs more inputs. The Moldorm and Moblin King fights use separate inputs
for each engine's observed enemy positions.

| Java checkpoint | Result |
| ---: | --- |
| 536 | Crossed east on the second visit to Tail Cave `$0E` at (104,112) with 4 health. |
| 556 | Collected the Nightmare Key. |
| 600–604 | Defeated Rolling Bones and recovered to 24 health. |
| 660–669 | Defeated Moldorm, collected the fourth heart container and obtained the Full Moon Cello. |
| 885–891 | Defeated the Moblin King and rescued BowWow. |
| 899 | Returned to overworld `$35` at (104,80) with 32/32 health and BowWow state 1 in both engines. |

Java recorded 85,488 frames and PyBoy recorded 83,854. The fresh Java
Bottle Grotto attempt reproduced the full Java rescue trace byte-for-byte.
The first subsequent room mismatch is checkpoint 918. See the
[swamp continuation](bottle-grotto-entrance-torches.md#fresh-replay-on-the-899-checkpoint-rescue)
for the two approach positions and replay commands.

To replay the rescue from the repository root with a locally configured ROM:

```sh
(cd java && ./gradlew captureOpeningRoute \
  -PopeningRoute="$PWD/../tools/opening/bowwow-rescue-route.tsv" \
  -PopeningOutput=/tmp/ladx-rescue-java)
python tools/opening/capture.py --route tools/opening/bowwow-rescue-route.tsv \
  --output /tmp/ladx-rescue-rom
```
