# Walking at a pit edge

Link can step back from the lip of a pit in the ROM. While his ground status
is 7, the normal walking handler applies input on even global frames for the
first 24 slipping updates, then once every eight frames ($02:448D–44A7).
The ground handler samples the cell after movement, increments the slipping
counter over a pit, and pulls Link on global frames divisible by four
($02:7644–7660). Reaching ordinary ground clears the counter. Link's motion
state stays 0 until the pull starts a fall at $02:76AA.

## Controlled ROM trace

PyBoy 2.8.1, English DX v1.0, ROM SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
Starting from the normal house-route exit into outdoor room A2, each case
restores the same state. Disable the 16 entity slots, fill the visible
10×8 room-object buffer (base $D711, stride 16) with $FF, and set cell $22
to pit object $E8. Write the initial raw X/Y to $FF98/$FF99 and $C11A/$C11B.
Set the global frame ($FFE7) to 1, ground status ($C11F) to 7, cached
physics ($C181) to $50, normal motion ($C11C) and X/Y speeds to zero.
The initial slipping counter ($C1BB) is zero except in the last case.
These are injected, in-room movement observations. No screenshots were compared.

| Initial raw X,Y | Held input | Observed raw positions and state |
| --- | --- | --- |
| 32,46 | Left | CPU frame 1 stays at (32,46), counter 1. Frame 2 moves to (31,46), clears the counter and ground status. Frame 3 reaches (30,46). |
| 40,48 | Right | Moves to (41,48) on frame 2. Frame 4 ends at (41,52) in falling motion 6. |
| 40,48 | None | Frame 4 ends at (39,52) in falling motion 6. |
| 35,46, counter 24 | Left | Holds position on frames 1–3. Frame 4 pulls to (36,47). On frame 8 the left step and right pull cancel, ending at (36,48). |

The equality cases are direction-sensitive. At the center, the pull moves X
left and Y down. The ROM tests each adjusted coordinate's low nibble against
the four-pixel fall window, then adds three to raw Y before starting the fall
($02:7662–76AC). Java's normal grounded movement now executes this check after
walking and uses the world frame supplied by the session. Isolated Link tests
use the local motion clock when there is no world frame.

`LinkPitBehaviorTest.canStepBackOffPitEdgeBeforeTheNextPull` compares the first
three positions of the left-edge case and checks both ground and motion state.
The centered, stationary case is covered by
`pitCenterEqualityNudgesLeftAndDownBeforeTheFall`. The ROM house route SHA-256
is `e6065107a5ef0ed993855dc4e7acfabe8f4bfb5756106b5032bfede2e12b5142`.
