# Room entity allocation and physics initialization

A fresh PyBoy 2.8.1 replay of the 843-row BowWow rescue route recorded
126 adjacent room loads at `$02:7ABD`. The route SHA256 was
`d21b8ba1c6f702689be10cda86e58b564959f14427431ac5c02be56e693ad208`.
The nonzero status mask on the previous video frame and at the loader
call agreed for all 126 loads. The second replay below also checked
the mask at the first frame of scroll and found the same result. For each
definition, read the source ordinal at `hMultiPurposeD` and cleared
mask for `hMapRoom` at `$00:3883`. Compare the predicted first
available slot with DE at `$00:38B4`. This reproduced 271 allocations
and 11 cleared definitions, with zero discrepancies.

| Checkpoint | Destination | Outgoing status mask | Incoming occupied slots |
| ---: | --- | --- | --- |
| 45 | Overworld `$F1` | `$0019` | 1, 2, 5 |
| 49 | Overworld `$F2` | `$0026` | 0, 3, 4 |
| 71 | Overworld `$F1` | `$0000` | 0, 1, 2 |
| 74 | Overworld `$F0` | `$0005` | 1, 3, 4 |

The loader applies the cleared mask only to ordinals 0 through 7.
A controlled call redirected to Indoor A `$FA` at `$02:7ABD` with
outgoing mask `$0003` and injected cleared mask `$FF` after
`UpdateRecentRoomsList`. The eight `$DD` definitions were skipped.
Type `$4A` loaded into slot 2 with source ordinal 8.

A natural revisit to Overworld `$80` at checkpoint 262 allocated its
owl event in slot 1 and unloaded it at the init status guard `$03:4A7C`
within the same video frame. A controlled replay set the visited
Overworld `$F2` flags to `$B0` at checkpoint 49. The loader placed
the owl, sword and urchin into slots 0, 3 and 4, then the owl and sword
unloaded within the same frame. The Java allocator reserves these
slots through the definition scan and omits the two entities from its
resulting snapshot, preserving the urchin in slot 4. This confirms
allocation and post-frame occupancy for the sampled transitions.

## Physics flags

A second fresh replay recorded `wEntitiesPhysicsFlagsTable[C]` at
`$03:487B`, just after `ConfigureNewEntity` copies
`PhysicsFlagsForEntity`. Its route SHA256 was
`2ab4fefcf61a853ccc5fc31b48e2f5c1a5a9bf6dd0c8b9f1d17b9051c80cafd4`.
The ROM SHA256 for both runs was
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.

| Checkpoint | Destination | Entity | Native flags |
| ---: | --- | --- | --- |
| 45 | Overworld `$F1` | Octorok and Leever | `$12` |
| 103 | Overworld `$70` | Moblin | `$12` |
| 155 | Overworld `$52` | Sword Moblin | `$12` |
| 299 | Indoor A `$16` | Hardhat Beetle | `$12` |
| 650 | Indoor A `$06` | Heart Container | `$B2` |

Java now uses the 251-entry ROM table for types missing an explicit
physics policy. Existing per-type and handler overrides still apply.
The side-view pot, for example, still uses the engine's explicit
grabbable flag `$20`, while its raw table byte is `$82`. ROM-free
sessions retain their existing defaults.

## Reproduction and scope

```sh
out=$(mktemp -d)
awk 'NF && !/^#/ { print; if (++rows == 74) exit }' \
    tools/opening/bowwow-rescue-route.tsv > "$out/first74.tsv"
python tools/opening/capture.py --route "$out/first74.tsv" \
    --trace-entity-loads --shield-trace-room 0xf1 --output "$out"
```

The 74-row input SHA-256 is
`bf086cf7566ed5ac5ec9574251a5b348c17c50e16bb07e70617c01cc7119a452`.
The ROM SHA-256 is
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
The hooks write `emulator-entity-loads.tsv` and `emulator-shield.tsv`.

## Scroll preparation before the first movement

The same fresh route recorded the boundary handler at `$02:6DE4`,
preparation at `$02:79FA`, tileset selection at `$02:7B3E`, scroll
target configuration at `$02:7B7F`, and the first Link integration
at `$02:790B`. Count elapsed `hFrameCounter` values modulo 256.
One adjacent load used an entry path outside `$02:6DE4`. The other
125 had these observed delays.

| CPU frames before first integration | Transitions |
| ---: | ---: |
| 4 | 76 |
| 7 | 18 |
| 11 | 21 |
| 15 | 5 |
| 19 | 5 |

The three mandatory phases each take one CPU frame. At `$00:02EF`
the main loop waits when background or entity tile requests remain.
Each VBlank handles one request increment. An indoor background
takes four increments and an outdoor background takes eight. The
selector queues at most two changed entity sheets, requiring four
increments each. The first transfer can run in the VBlank of the
requesting frame. The configure handler also waits for the two
switch-block copies when the incoming room contains movable blocks.
The Color Dungeon has its own requests during both preparation and
selection. The queued-transfer formula matched every paired native
entry on this route. Switch-copy timing is covered separately by a
controller test.

| Checkpoint | Scroll | Preparation evidence |
| ---: | --- | --- |
| 24 | Overworld `$A2 → $A1` | Two entity sheets. Entry 3086, prepare 3087, select 3088, configure 3096, first integration 3097. |
| 40 | Overworld `$D0 → $E0` | Eight background copies. Entry 4142, first integration 4153. |
| 45 | Overworld `$F0 → $F1` | No tile requests. Entry 4521, prepare 4522, select 4523, configure 4524, first integration 4525. |
| 349 | Indoor A `$13 → $14` | Four background copies. Entry 42803, first integration 42810. |

Java preserves the fixed scroll camera and blocks ordinary Link
movement throughout these phases. Transfer-only ticks bypass gameplay
simulation while completing input, matching the order of the native
`$00:02EC` joypad read and `$00:02EF` gate. Its renderer still uploads
decoded tiles and installs the destination room synchronously.
The transfer model controls when scrolling starts. Native entity init
runs on the preparation frame after allocation, including the owl
and sword examples above. Java models that dispatch for the incoming
families described below. Full movement and intermediate VRAM uploads
retain their existing host paths.

In an isolated Java A/B replay, the route SHA256 was
`2ab4fefcf61a853ccc5fc31b48e2f5c1a5a9bf6dd0c8b9f1d17b9051c80cafd4`.
With the preparation latch disabled locally, Java completed 111
checkpoints and died at frame 16550 in Overworld `$62`. With the
latch enabled it completed 157 and died at frame 21040 in Overworld
`$52`. The two runs used the same classes except for that latch.


## Incoming entity dispatch during preparation and scrolling

A fresh 49-row route prefix with SHA256
`cd9b16d16dcc936f83169828a816ef727e87ebb00ed3bb6835f80ee9a7570a3f`
hooked `EntityInitHandler` at `$03:48B5`. The beach transition to
`$F1` at checkpoint 45 initializes Leever slots 5 and 2 and Octorok
slot 1 in descending order on frame 4522, CPU counter 191, after
the prepare handler advances phase 1 to phase 2. The next dispatch
sees the Leevers active with the init-only hidden variant `$FF`.
There are no further init calls for these incoming slots on that
scroll.

The first `$E0` entry shows the Octoroks unchanged at `40,80` and
`72,32` on the first ordinary frame (4184). Their state-zero pause
logic at `$03:58D7` sets transition countdowns 17 and 30 before the
next frame decrements them. At checkpoint 42, native Link reaches
`104,88` with health 24.

A fresh prefix through checkpoint 408 with SHA256
`aa472b429ba7a15d0383270efeb36e02c7d99de84af3bcd4c74841906db6830f`
also hooked `$06:6643` and `$06:6649` during the Tail Cave scroll `$0F → $0E`. Each Spark
initializes once at CPU counter 186, drawing first at 187. The room-local
counterclockwise position changes from `120,112` to `120,109`.
The clockwise position changes from `72,32` to `72,35`. Both positions
remain fixed relative to the room for 41 transition draw dispatches.
The cached native draw X matches the camera offset. The raw entity X
table is already one camera increment ahead because `AnimateEntity`
caches the drawing position before shifting the stored coordinates.
The draw variant uses bit 1 of the CPU frame counter. It remains on
the last dispatched variant when tile work preempts the entity pass.

Java runs the existing init dispatch and its presentation writes once
for incoming Octorok, Moblin, Leever and both Spark types. The existing
entity tick interfaces carry the incoming-only mode. The session
publishes the resulting live room view, and the renderer presents it
using the current room scroll offset. The new stage omits ordinary
movement, Link combat and follower updates. The renderer suppresses
the incoming snapshot before Prepare. Outgoing entities still use
the retained render snapshot, and other incoming entity families
retain the prior lifecycle.

Same-build Java comparisons used the canonical route SHA256
`de3e447dbf39a75bfe8de8c5b9f58ab9d15cc45d8d08d3a0f1523b079331fc6e`.
With incoming dispatch disabled locally the run completed 474
checkpoints and died in room `$19` at frame 56756. With all five
types enabled the run completed 157 and died in room `$52` at
frame 20280. Ordinary-only yielded the same endpoint. Spark-only
completed 460 and died in room `$03` at frame 55894. The earliest
Java difference with ordinary types enabled occurs at frame 4273
in `$E0`. Checkpoint 42 then matches native Link health 24 and
position `104,88`; the disabled run reaches `104,99` with
health 12. These route results measure the effects of the dispatch
change on the recorded controls.
