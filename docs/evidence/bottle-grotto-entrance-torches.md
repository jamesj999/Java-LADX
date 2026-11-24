# Bottle Grotto entrance torches

A fresh PyBoy 2.8.1 replay of the historical BowWow rescue route, the
[swamp extension](bowwow-follower-spawn-height.md), and the 19-row
`tools/opening/bottle-grotto-torches-rom-extension.tsv` completes 942
checkpoints using controller input. Checkpoint 923 is inside the entrance
(`$01:$36`), at ROM position (80, 124) with the dungeon title open. The new
suffix dismisses the title, enters `$01:$31`, equips powder to B, lights both
torches, and passes through the east shutter. Checkpoint 942 is in
`$01:$32` at (10, 74), with 28 health units and 16 uses of powder remaining.

A controlled Java run started in overworld `$24` at (10, 48) with the
rescued BowWow, clock 135, 28 health, four hearts, the level-one sword and
shield, sword on A, feather on B, powder and shield in the first two
backpack slots, inventory cursor 1, and 18 uses of powder. It restored the
native story flags at that checkpoint. The last 16 swamp rows and all 19
torch rows then ran with controller input, including the dungeon entrance
warp, and reached the same final position, health, and ammunition. Both
torches (`$34`, `$35`) change from `$AB` to `$AC` 15 ticks after their
respective powder uses. The shutter tile at `$39` changes from `$3B` to
`$0B` nine ticks after the second ignition.

The `$36` to `$31` scroll saves entry Y 128 in both engines. Live Y after
completion is 127 in PyBoy and 128 in Java. PyBoy still has cached Up at
the native completion hook (`$02:790E`). Both reach the first powder pose
at (68, 74). The controlled Java start has a different global clock and
Spark phase. The comparison covers the entrance and torch gate from
these arrivals.

```sh
cat tools/opening/bowwow-rescue-route.tsv \
    tools/opening/bowwow-swamp-rom-extension.tsv \
    tools/opening/bottle-grotto-torches-rom-extension.tsv \
    > /tmp/ladx-bottle-grotto-torches.tsv
python tools/opening/capture.py --route /tmp/ladx-bottle-grotto-torches.tsv \
    --output /tmp/ladx-bottle-grotto-torches
```

| Input sequence | SHA256 |
| --- | --- |
| Rescue and swamp prefix | `435d580976592c05816d4f8a1be1620651402aee490c29282248b23178a94474` |
| Torches extension | `559c6c7fb3844525853e9b0da45b73b3f90ff01f76ddabdcd9d349c8cb785a98` |
| Combined route | `11e624c2732966b0f57fb02fe94645ea32a45b8fcd62442600580f5cb2a5ec0c` |

## First small key

Append `tools/opening/bottle-grotto-first-key-rom-extension.tsv` after the
torches extension. Its ten controller rows defeat the aggressive and
evasive Stalfos in `$01:$32` and collect the falling key. The fresh PyBoy
run completes 952 checkpoints, ending at frame 89837 in `$01:$32` at
(12, 63), with 28 health units, 16 powder, and one small key. The same
suffix from the controlled Java arrival reaches the same endpoint with
the same resources. Both spawn the key entity (`$30`) at (40, 60) after
the enemies die. The enemy trajectories and their death times differ.
The native run also drops a Guardian Acorn, which remains uncollected.

The event byte for this room is `$81` in
`LADX-Disassembly/src/data/events/dungeons.asm`. The spawn routine at
`$02:5425` uses X `$28`, Y `$3C`, and initial Z `$70` for the Bottle
Grotto key.

| Input sequence | SHA256 |
| --- | --- |
| First key extension | `57e483180d5635dd37bbdeaa5303874d776ee11114acb57333972728ef8bc3a6` |
| Combined route through the first key | `62598ff65a62adbce19dab03c29e83f5c760cf65078fef6526e38332361acc54` |

## First crystal switch

In room `$01:$33`, walking into the switch from below stops Link before
the switch without damage. The crystal's entry in
`LADX-Disassembly/src/data/entities/physics_flags.asm` is
`2 | ENTITY_PHYSICS_HARMLESS`. Its handler calls
`PushLinkOutOfEntity_15` at `$15:4341` and starts the switch animation on
a weapon flash at `$15:4358`.

The Java switch lacked the harmless flag. The replay lost four health
units and recoiled from (89, 77) to (90, 90) before the first sword press,
leaving Link in `$33` with the blocks untoggled. With the ROM flag restored,
controlled Java stops at (89, 73), and PyBoy at (87, 73), with 28 health.
The sword changes the block state from 0 to 2 in both. Both advance to
`$01:$34` at (10, 73) with 28 health, 16 powder, and one small key.

The six rows below follow the first-key route. Fresh PyBoy completes 958
checkpoints at frame 90354. The complete sequence SHA256 is
`2cfae8d85623087a0e62ac4fdbc34edc1842ff681db2904768e943482f1ccfd4`.

```text
right 185 45
down 14 2
right 77 2
up 20 0
a 1 26
right 100 45
```

The earlier rescue prefix produced byte-identical native frame, checkpoint,
combat, instrument, and King traces over the full 958-checkpoint run.
The prior combined hashes were
`64084871e9f1dc2aa93ff1d7b0680379fcf09404d9795d6cd2453cfc04f635c7` through the torches,
`ef7064ee5cd511e49239bc11ad14efe8e7a4c9b2b5b80b3483f276fae23196d3` through the first key,
and `fd89e1ae144fa16a4e93c5150af31bf6e3bdf1d8bcfb51caa8f42655ae886706` through the crystal.

## Paired blocks and the Hinox approach

Append `tools/opening/bottle-grotto-bracelet-rom-extension.tsv` directly after
the first-key extension. Its 182 rows collect the small-key chests in
`$38` and `$39`, return through `$34` from the south, open the north lock
in `$35`, push both `$2F` blocks, and cross the side-view rooms.
Fresh PyBoy reaches Hinox in `$01:$28` at (80, 110) at checkpoint 1065
and frame 97448 with 20 health units, 20 powder, and two small keys.
The replay uses controller input throughout. The per-row ROM button
overrides are in the rescue prefix.

The route defeats Hinox, collects his Fairy, opens the west lock in
`$21`, lights one torch in `$20`, and defeats both Boo Buddies.
Checkpoint 1134 at frame 102018 is in `$01:$20` at (136, 58)
with Power Bracelet level 1, 12 health units, 19 powder, and one small key.
The previous full-route hash
`b62bfa31c3605e5628d9197748e69d1da0d5e38f1aa4742bc237a410d7f78d9a`
produced byte-identical frame, checkpoint, combat, instrument, and
King traces through checkpoint 1134.

| Input sequence | SHA256 |
| --- | --- |
| Bracelet extension | `c6add4439a3ed7d6c5102d76e91ada4329f1ff2614a7bc7114c8c029f8b830ec` |
| Combined route through the Power Bracelet | `ca982d2a394e5665d2d83b3e41ba7c2166fffdc008fe34e858fc1ccb49987397` |

The earlier 112-row Hinox approach ended at checkpoint 1064, frame 97550,
at (80, 110) with 12 health units. It used suffix SHA256
`e5b439400f7b2f01fb7c2294e30196a6e4b64188882d6b1774b077079e52bd8e`
and combined SHA256
`08074c21885f56a5396e75634ca338ab8e88a7b4e6a3f24f68a479e8c2b953d8`.

Native horizontal contact with the east `$2F` block, starting at (124, 63),
calls `$02:742F` for both collision points (111, 56) and (111, 59).
The shared counter advances twice per frame and allocates the pushed
block at `$02:53FB` on the 32nd contact frame. In a native edge control
near Y 54, each frame's single qualifying probe sees the counter reset.
The clear probe jumps from `$02:6F63` to `$02:7461`.

Java had counted only the first probe. The fixed implementation counts
both contacts and clears the counter on a clear probe. A separate
controlled Java load of `$2F` starts Link at (124, 63), with sword A,
feather B, 24 health, and clock 179. The exact native `left 50 4` row
leaves Link at (113, 63) and the moving block at (97, 64) in both.
Different enemy trajectories require separate controller input for the
west approach. In Java both blocks settle on `$34/$35` and the hidden
stairs reveal. The native route through Hinox's entrance and the
controlled Java block comparison cover separate parts of progression.

## Moving platforms

The `$3F` horizontal platform (`$A3`) and `$3E` vertical platform
(`$A4`) share the native handler at `$07:6358`. They begin with zero
speed and accelerate by one unit every four interactive ticks on
their respective axes. At tick 24 they have moved four pixels and
reach speed 6, which starts the `$6A` turnaround countdown. The
subpixel position keeps advancing at the current speed during the
countdown. A rider's horizontal position follows the platform delta,
and the rider's Y is set to platform Y minus 16.

Java previously dispatched only `$A5`, the pressure-activated platform.
The new dispatch runs the autonomous cycle for `$A3/$A4`. From the
controlled `$2F` arrival above, ordinary input takes the revealed stairs
to `$3F` at (136, 16) with 20 health. The side-view inputs below carry
Link upward on `$A4` from Y 95 to Y 48, then ascend the ladder to
`$2C` at (120, 112) with 16 health. Standard movement from there reaches
Hinox in `$28` at (80, 111). The fresh native route reaches (80, 110)
via the same rooms. The two arrivals have separate enemy timing.

```text
down 60 3
left+b* 175 35
none 1 36
left+b 38 16
left+b 31 16
none 1 150
left+b 38 16
up 100 55
```

Replaying the revised forest controls (base `de3e447d…`) and the same four
extensions produces combined SHA-256
`31fce2c66bd067e646949e775bffde69885ac379b04ac362e3679789ee5e0fa4`.
PyBoy again reaches checkpoint 1064 at frame 97550 with no RAM injection.
The full 97,551-row frame trace and the checkpoint, combat, instrument and
king tables match the previous ROM run byte for byte. This version uses
96 explicit per-row ROM overrides.

Both automatic platforms also decode their shared four-sprite display
list at `$07:6348`. The catalog test checks the offsets, tiles, and
attributes against the user-supplied ROM.

## Power Bracelet in Java

A separate controlled Java arrival loads map `$01` room `$20` with Link
at the native pre-powder pose (92, 99), facing Down, with sword A, powder
B, 20 health, 20 powder, and clock 124. Both Boo Buddies start from the
room loader's ordinary entity state. The first powder use lights the
southern torch, which makes both enemies sword-vulnerable. The inputs
below defeat the enemies, reveal the chest, and collect Power Bracelet
level 1 at (136, 58) with 28 health and 19 powder. The chest approach holds Right nine ticks longer to align from Java’s
post-fight position.

```text
b 1 25
up+a* 73 8
left+a* 68 20
up+a* 118 12
left+a* 58 16
right 78 2
down 31 2
right 16 2
up 6 2
a 1 45
a* 480 44
```

## Continuous Java approach to Hinox

The 150 rows in `tools/opening/bottle-grotto-hinox-java-continuation.tsv`
start from a controlled Java overworld `$24` arrival at (10, 48),
facing Down, with clock 135, 28 health, Sword A, Feather B, 18 powder,
an inventory cursor on slot 1, and rescued BowWow following Link.
The usual house intro ran first. Room status flags came from the
native pre-swamp rescue state. The dungeon phase uses normal controls
continuously through the crystal and key rooms, both push blocks,
both side-view platforms, and the dungeon corridor to Hinox.

At local tick 9616 Java reaches `$01:$28` at (80, 111) with 8 health
and two keys. Hinox is active at (88, 48). The fresh native approach
reaches (80, 110) with 20 health. The Java run through the Hinox
entrance and the controlled chest check in `$20` have separate starts.

| Input sequence | SHA256 |
| --- | --- |
| Java controlled swamp-to-Hinox continuation | `afeebf61c733ef308678b0958b6cccdd8bfe7767553923b085adb081d6b5cd9b` |

### Hinox charge timeout

The state 3 handler at `$06:50CE–$5102` clears the state on timeout,
applies motion, and then tests Link's displacement and motion state.
The range on each axis is −24 through +23. The successful proximity
branch increments the current state, sets the timer to `$4F`, and
writes the growl to the wave channel. The recurring bounce uses the
jingle channel.

PyBoy CPU hooks set the input state at `$06:50CE` in the loaded
Hinox room and sampled the result at `$06:5102`. Hinox was at
(88, 48) with zero speed and Link had default motion at Y 48.
These are controlled probes.

| Timer in | Link X minus Hinox X | State out | Timer out | Wave out |
| --- | --- | --- | --- | --- |
| `$00` | +24 | `$00` | `$00` | `$00` |
| `$00` | −24 | `$01` | `$4F` | `$16` |
| `$01` | +24 | `$03` | `$01` | `$00` |
| `$01` | −24 | `$04` | `$4F` | `$16` |

Java now reproduces the timeout reset, proximity branch, and audio
channels. `HinoxMotionTest`, `EntityRuntimeHinoxTest`, and
`GameplayRouteTest` passed together (21 tests). A continuous Java
clear of the fight remains unverified.

### Bracelet use in the east corridor

The native run continues by equipping the Bracelet to B, returning to
`$21`, killing the Keese, lifting the five pots in the eastern corridor,
and crossing into `$22` at (10, 48) with 20 health, 19 powder and one
small key. The native route enters with switch state `$02`, which
raises `$DB` across the west approach. A separate controlled PyBoy
entry with state `$00` reaches the room's crystal and changes the state
to `$02` using the sword. The next chest and the path to Genie remain
unverified in this controller replay.

In a controlled Java `$21` arrival at (104, 57), facing Up, with
12 health, clock 247, and level-one Bracelet on B, the matched input
`up 4 2; b 1 0; b+down 12 18; b 1 45` lifts and throws the first
pot. The pot at room location `$26` becomes object `$0D` and the
carried-entity flag clears after throwing.

## Native replay

```sh
cat tools/opening/bowwow-rescue-route.tsv \
    tools/opening/bowwow-swamp-rom-extension.tsv \
    tools/opening/bottle-grotto-torches-rom-extension.tsv \
    tools/opening/bottle-grotto-first-key-rom-extension.tsv \
    tools/opening/bottle-grotto-bracelet-rom-extension.tsv \
    > /tmp/ladx-bottle-grotto-bracelet.tsv
python tools/opening/capture.py --route /tmp/ladx-bottle-grotto-bracelet.tsv \
    --output /tmp/ladx-bottle-grotto-bracelet
```

## Fresh replay on the 899-checkpoint rescue

The four extensions above, appended to the current
[rescue route](rescue-forest-approach.md#fresh-rescue-through-the-hideout),
give SHA-256 `bcfc1ddf568c9f80db05e4cc7eb1755c22d1c7ef6f178214d306c0a6a91b35e0`.
PyBoy 2.8.1 completes 1,190 checkpoints from a fresh English DX v1.0 game.
It ends in `$01:$20` at (136,58) with the Power Bracelet, 12 health,
19 powder and one small key. The 56 extra rescue checkpoints consume zero
ROM frames. All 1,134 previous checkpoint states match at their new indices.
All 102,019 per-frame records and the complete combat, instrument and Moblin
King traces are byte-identical to the earlier run.

Fresh Java first diverges in **room** at checkpoint 918. At 917, both have
32 health in overworld `$44`, with Java at (67,84) and PyBoy at (64,73).
After `left+b 72 56`, Java is still in `$44` at (54,97) while PyBoy has
crossed into `$43` at (148,73). Start the next comparison on the approach
to this west exit. Java later reaches overworld `$24` at checkpoint 1059
and completes 1077 at (25,42) with 12 health. It dies there on frame
96002 at (22,42). These last checkpoint numbers count the fixed-duration
controller rows even when the two players occupy different rooms. The
controlled Java Hinox and Bracelet runs described above start separately.

To repeat the fresh Java continuation after assembling the TSV above:

```sh
(cd java && ./gradlew captureOpeningRoute \
  -PopeningRoute=/tmp/ladx-bottle-grotto-bracelet.tsv \
  -PopeningOutput=/tmp/ladx-bottle-grotto-java)
```
