# BowWow follower spawn height

The ROM copies Link's raw X, Y and Z when it creates following BowWow
at `$01:614C–6160`. Java now supplies all three coordinates to the entity
spawn. The other followers retain their existing initialization.

PyBoy 2.8.1 replayed the historical `tools/opening/bowwow-rescue-route.tsv`
from a fresh save through checkpoint 842, outside the hideout in room `$35`. The
ROM SHA256 was
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
The route at `b5fd0ecf` had SHA256
`62c1e95dd6d63f459a0cc541578591a90508daddf82d0b6612101d807bde3804`.

The room `$35` state was replayed twice with the same movement. Hold
down for 29 frames, left for 81, up for 36, and left for 21. Release
all buttons for 55 frames, then hold up for 19 and left for 32. Release
all buttons for 60 frames to finish the westward transition. A CPU
hook at `$01:6161` records Link's position and the entity position in
the slot held in register E.

| Room `$34` spawn | Link X, Y, Z | BowWow X, Y, Z | Slot |
| --- | --- | --- | --- |
| Normal buttons | 149, 71, 0 | 149, 71, 0 | 15 |
| Controlled `$01:614C` hook sets `hLinkPositionZ=$0E` before the copy | 149, 71, 14 | 149, 71, 14 | 15 |

The second run changes one byte immediately before the copy. It verifies
the spawn routine with nonzero height independently of the transition's
grounded movement requirement. The Java follower spawner regression covers
that nonzero height and checks that the neighboring Marin slot starts at
height zero.

## Overworld scroll anchoring

At `$02:7986` the ROM creates the follower after the scroll has integrated
and recorded Link's destination position. Java loads the destination
before the scroll. It now also publishes Link's resolved destination
coordinates before the destination creates the follower.

A controlled Java run starts outside the hideout with Link at raw
`(104,80)`, BowWow quest state 1, sword on A, Roc's Feather on B, and
32 health. It then runs normal inputs. On the first settled westward
checkpoint into room `$34`, Link is at `(148,81)` in both versions.

| Settled checkpoint | BowWow X, Y |
| --- | --- |
| PyBoy | 144, 86 |
| Java before anchoring fix | 8, 99 |
| Java after anchoring fix | 148, 99 |

Before the fix, BowWow wrapped through X240 while trying to follow Link.
The regression checks the Java spawn itself at `(148,81)`. The table
samples later than spawn, when entity motion has already changed the
coordinates. Java still differs in BowWow's movement and scroll staging.

## Swamp traversal and Bottle Grotto entry

Append the ROM extension to the unchanged rescue route and replay it
from a fresh, in-memory save.

```sh
cat tools/opening/bowwow-rescue-route.tsv tools/opening/bowwow-swamp-rom-extension.tsv \
  > /tmp/ladx-bowwow-bottle-grotto.tsv
python tools/opening/capture.py --route /tmp/ladx-bowwow-bottle-grotto.tsv \
  --output /tmp/ladx-bowwow-bottle-grotto
```

The current rescue prefix has SHA256
`dfeffe8fbc9c8ba7b73b013b89f80f97de20bbe76e299509a888958aef6031de`.
With the extension the SHA256 is
`435d580976592c05816d4f8a1be1620651402aee490c29282248b23178a94474`.
Previous combined hashes include
`c943de9e630a30f49d356effd655c7a49152b96e0b290301bb9074abcd0c6cb8`,
`ff5aab1eddd3e68b240549b9813bfb7326a52838a52708a2e87cfe848032c77f` and
`f9115bede8c3f18030392bfbbdec67296e03bbf3dc6d152bda1dee601170a549`.
The emulator frame traces match byte-for-byte across the runs.
The fresh PyBoy run completes 923 checkpoints without memory injection.
It crosses rooms
`35→34→44→43→42→41→40→30→31→32→22→23→24` with BowWow following.
At checkpoint 906, Link reaches room `$24` at `(10,48)` with 28 health
and five Goponga flowers active. Hooks at `$05:43A3` confirm all five
are defeated by BowWow in the subsequent inputs. At checkpoint 922
Link is in Bottle Grotto, map `$01`, room `$36` at `(80,124)`, with
28 health and the title dialogue open. BowWow's following flag remains 1
while his entity stays outside the dungeon.

The Java room `$24` experiment seeds Link's `(10,48)` position, frame
clock 135, equipment, 28 health and BowWow following, then runs the
last 16 rows of the same extension with normal buttons. All five
flowers disappear before it arrives at map `$01`, room `$36`,
`(80,124)`, 28 health and the title dialogue. This setup lets the
destination loader initialize BowWow and leaves entity RNG and timers
unsynchronized. Individual flower removal frames differ. The controlled
Java post-rescue run reaches rooms `$34` and `$44` at the same Link
positions as PyBoy. A follow-up preserving held buttons across rows
also crosses `$44` and enters `$43` before the route diverges later.
The [room `$44` input and landing comparison](room-44-feather-landing.md)
records the matched segment. The Java evidence covers the arrival anchors and
room-`$24` flower clearing and entry from controlled state. The full
uninterrupted replay applies to PyBoy.

The isolated run passed 24 tests across `FollowingNpcEntitySpawnerTest`,
`RoomTransitionFollowersTest`, `RoomSessionFollowersTest` and
`BowWowChainTest`.
