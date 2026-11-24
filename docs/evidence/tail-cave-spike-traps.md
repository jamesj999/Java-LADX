# Tail Cave Spike Traps

The Feather chest room ($00:$1D) and the approach to Moldorm ($00:$0B)
use the same trap handler. At $06:7510 it renders the trap, checks whether
entities can interact, then decrements the ignore-hits timer and updates
movement. The travel timer comes from the shared slot countdown. Java now
uses that countdown for the launch, turn and return states.

A fresh PyBoy 2.8.1 run of the English v1.0 ROM logged the traps on entry
to $06:7510 and on reaching $06:7523. A separate Java room replay used the
recorded Link positions, dialogue and inventory flags, aligned the first
ordinary dispatch, and opened the Feather chest through the room API.
The comparison covers both coordinates of each trap before movement.

| Room | Native frame range | Dispatches per trap | Results |
| --- | --- | ---: | --- |
| $1D | 53555–54691 | 986 | Both traps matched on all 1,972 position samples. |
| $0B | 61725–62053 | 329 | All four traps matched on all 1,316 position samples. |

The first range includes pauses in world dispatch. The Feather chest held
the handler on 431 draws, including 390 with the dialogue open. Native
slots in $0B were 0, 2, 3 and 4. The Java comparison paired traps by
their room definitions and starting positions.

A controlled one-frame probe started with the left trap at (28,96),
travel timer 22 and ignore-hits timer 2. Opening dialogue held all three
values. Ordinary play advanced X to 30 and the timers to 21 and 1. With
a four-tick ocarina countdown and the Ballad selected, the ROM rewrote
the interaction latch at $02:4AF9. X remained 28 for four ticks. The
travel timer reached 21 on the fourth tick and motion resumed on the
fifth. The Java slot tests distinguish the movement and timer gates.
Matching the full ocarina playback latch in the room simulation remains
open.
