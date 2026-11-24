# Key Cavern entrance controls

The same 2,503 controller frames in PyBoy 2.8.1 and Java's headless
`GameSession` opened the entrance shutter, earned the first small key, and
spent it to enter room `$54` in dungeon map `$02`. Both runs reached `$54`
with health 12 and no small keys remaining.

The replay starts from a controlled dungeon setup. PyBoy starts a blank file
and redirects the house exit through the ROM's normal room loader to `$52`.
Java initializes a new headless session and loads the same room through
the room session. Both set Link to (72,108), facing up, with a level-one sword on A
and a level-one Power Bracelet on B. All actions after setup use buttons.

| Frame | Room | Observed progression in both runs |
| ---: | :--- | :--- |
| 118 | `$52` | Lifted a pot with the Power Bracelet |
| 225 | `$52` | Threw the pot into the north shutter and opened it |
| 273 | `$51` | Passed through the entrance shutter |
| 911 | `$51` | Defeated the Gel, two Bombites, and Sword Moblin and revealed chest tile `$A0` |
| 1064 | `$51` | Collected one small key from the chest, which changed to tile `$A1` |
| 1479 | `$4F` | Reached the room with four hidden Zols |
| 1800 | `$4F` | Defeated the four Zols and opened the north shutter |
| 1945 | `$4E` | Entered the next room north |
| 2155 | `$4C` | Reached the staircase room |
| 2337 | `$56` | Took the staircase warp to the basement with one key |
| 2503 | `$54` | Spent that key and entered through the south doorway |

The ROM checks the thrown pot against the entrance's shutter metatiles
in [bank 03 near `$546A`](../../LADX-Disassembly/src/code/entities/bank3.asm).
Room event dispatch at [`$02:5D4F`](../../LADX-Disassembly/src/code/events.asm)
drives the kill-all chest and shutter checks. The native incoming room status
at the last checkpoint is `$88`, including the open south door bit `$08`.
The Java test checks both that bit and the outgoing north door bit in `$56`.

Enemy trajectories and exact Link positions differ during the run. At the final
checkpoint the ROM was at (80,110) and Java was at (82,100). The route to the dungeon entrance from a fresh file is outside this replay.

## Room $54 and the east basement door

After the shared entrance trace, the PyBoy continuation adds 52 frames of Up.
On frame 2555, the Aggressive Stalfos in slot 7 proposed position (96,79),
height 19, with signed X/Y speeds of −16/−9. Bank 06 calls
`ApplyEntityInteractionWithBackground` before updating height during its jump.
The ROM reported left collision flag `$02` against room metatile `$94`
and restored X to 97. The Y position stayed at 79. The next frame repeated
the X correction and continued the ascent. The Java handler previously
skipped background checks during pursuit and jumping. It now integrates both
axes, restores a blocked X before probing Y, and keeps the fractional
accumulators and jump state intact.
[Stalfos handler](../../LADX-Disassembly/src/code/entities/06_stalfos_aggressive.asm)
[Axis collision code](../../LADX-Disassembly/src/code/entities/bank3.asm)

Separate combat inputs from the same entry cleared the room and spawned the
small key at (40,60) in both runs. After collecting it, both rejoined the same
inputs from (38,62), returned to `$56`, and spent the key on the east door to
enter `$57` at (28,70). The Java test verifies both directional door bits.

| Checkpoint | PyBoy frame | Java frame |
| --- | ---: | ---: |
| Stalfos defeated | 2641 | 2729 |
| Second key dropped | 2849 | 2937 |
| Second key collected | 2981 | 3146 |
| Returned to `$56` | 3167 | 3332 |
| Spent second key and entered `$57` | 3375 | 3540 |
| Took the stairs into `$4D` | 3791 | 3942 |

PyBoy reached `$57` with health 16 after picking up a dropped heart on the
return path through `$54`. Java reached it with health 8. Both skirted the conveyors and took the stairs
to `$4D`, at (72,80) with zero small keys. The continuous route from `$4D`
to Slime Eye remains to be verified.

In `$57`, a local native hook at `$02:7C25` and `$02:7C3F` observed
metatile `$D1` (physics `$F2`) pushing left one pixel every fourth CPU
frame, including (33,51) to (32,51) on replay frame 3414. Metatiles
`$D0` and `$D2` pushed up and right respectively. Java has entity conveyor
movement and still lacks this displacement for Link. The two routes use
different left durations to align with the stairs.
[Link conveyor routine](../../LADX-Disassembly/src/code/room_transition.asm)

## Reproduction

With a locally supplied ROM and the repository's PyBoy dependencies installed,
run from the repository root.

```sh
python tools/key-cavern/live.py --rom /path/to/azle.gbc \
  --extension tools/key-cavern/room-54-rom.tsv --trace-stalfos \
  --output /tmp/key-cavern.json
cd java
./gradlew test --tests '*KeyCavernEntranceControlsTest' --tests '*AggressiveStalfosRuntimeTest'
```

The replay uses [entrance.tsv](../../tools/key-cavern/entrance.tsv), SHA-256
`cc808f311564dd72676dbefee87c51d12d2ac73f65c93db06b15d1ebebfb39e7`.
The ROM used for the native comparison had SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
The continuations are [room-54-rom.tsv](../../tools/key-cavern/room-54-rom.tsv)
(SHA-256 `652a6e166466177a2dae93d8ced8c0ac797ce960450a59989c946861f95e07b2`)
and [room-54-java.tsv](../../tools/key-cavern/room-54-java.tsv)
(SHA-256 `27bd876198bb139aa00d8b8788ed0a80c6aef11475babedddd9ec9ce6ae91238`).
