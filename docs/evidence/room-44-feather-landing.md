# Roc's Feather landing in overworld room $44

The first controlled post-rescue Java replay diverged in room `$44`
when the route changed from `left+b` to `down+b` without an idle frame.
The probe released and pressed B between frames. Java's keyboard event
queue records another press in that case. The ROM keeps B held when its
next joypad sample sees no release. Keeping buttons held across adjacent
rows removes the extra Feather activation. The production keyboard path
continues to handle taps, and the existing capture's input behavior is
unchanged.

The room `$44` state comes from checkpoint 854 of the uninterrupted
PyBoy run documented in [the swamp traversal evidence](bowwow-follower-spawn-height.md).
Link has raw position `(94,16)`, zero fractions and speeds, and 32 health.
PyBoy's screen boundary shows frame 183. The Java controlled probe
seeds its clock at 182 because it advances the clock before simulation,
while PyBoy's boundary reports the upcoming CPU frame.

From that state, hold the following inputs with no intermediate idle.
Leave B held across the consecutive rows that contain it.

| Buttons | Frames |
| --- | --- |
| Left | 12 |
| Down | 6 |
| Down+B | 24 |
| Left+B | 24 |
| Down+B | 24 |
| Left | 6 |
| Left+B | 72 |
| Released | 56 |

On CPU frame 225 the original still samples Down+B. In Java, hold that
input one additional frame and shorten the next Left+B segment by one
frame to compare the same game-visible input. From entry through frame
156, before the scroll, both engines agree on raw XY, Z, signed speeds
and XY fractions. Both reach room `$43` at `(148,73)`, with 32 health.
Their scroll preparation and in-flight scroll positions still differ.

## Ground physics on the landing frame

CPU hooks show a separate discrepancy in the implementation. The
original applies pit ground physics at `$02:4588` on landing, then again
through the regular grounded pass at `$02:7587`–`$02:75BD`. At the
natural room-`$44` landing on CPU frame 233, both calls to `$02:7657`
increment the slipping counter. The frame is outside the position
adjustment phase. Link steps off the pit on the next frame, so that
crossing's coordinates do not expose the missing pass.

For the controlled position check, seed the room-`$44` boundary counter
to 182 so landing runs on CPU frame 232. At this phase the ROM samples Left one
frame earlier with the original route timing. To compare the position
adjustment with equivalent input, hold Down+B for 25 frames in PyBoy and
23 frames of Left+B after it. Its effective Down+B lasts 26 frames, which
is the matching Java duration. The landing samples the normal pit
(`$E8`, physics `$50`) at raw `(80,55)` and runs two position adjustments.
The CPU hook records the combined result `(82,57)` and slipping count
2. Java now runs both passes, with the second sampling from the first
pass's updated position.

Additional controlled PyBoy runs inject frame 232, zero XY speed and
fraction, Z1, vertical velocity `$F0`, and the airborne flag. The
regressions use the same offsets relative to the test room's pit.

| Raw starting position in room `$44` | Raw position after landing | Result |
| --- | --- | --- |
| `80,55` | `82,57` | Escapes west to `77,58` after ten Left frames |
| `84,60` | `86,65` | Second adjustment starts the fall |
| `87,63` | `87,66` | The grounded adjustment still runs after the landing adjustment starts the fall |

The ROM chooses the top-down landing call below raw Y `$88`. The
subsequent grounded pass depends on the caller's motion path. Side-view
motion has its own landing path. A second start on the landing frame
keeps a single fall sound, matching the original's single wave-SFX
register.

## Slipping across consecutive pits

The shifted-phase run crosses grass after the first landing and later
lands on a second pit. Grass returns at `$02:78D7` before the default
ground handler clears `wPitSlippingCounter` at `$02:77A3`. The same
dispatcher gives the ordinary, shallow-water and raft paths their
source reset behavior, while the spike, conveyor and submerged-current
paths can return earlier. Finishing the fall also leaves the reset
to the next default ground update.

Before this correction, Java's unconditional reset when re-entering a
pit made it use the faster two-frame movement gate for longer. At
local frame 143, the ROM already has a slipping count of 25 and holds
horizontal motion before the scheduled pull. Java had moved one pixel
farther left. The [default ground dispatcher](../../java/src/main/java/linksawakening/physics/PhysicsFlags.java)
now serves both the slipping reset and the cracked-floor handler. After
the correction all 225 observed samples through local frame 224
match on raw position, speeds, Z, and XY fractions in the shifted-phase
comparison. Both engines are falling at `(38,84)` at the final sample.
The isolated focused run passed 64 tests covering Link airborne and pit
behavior, cracked floors, and follower spawn and scroll behavior.
