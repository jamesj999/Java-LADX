# BowWow Rescue Ordered-Play Design

## Scope

Continue the existing fresh-game ordered regression from Tail Cave's instrument
warp through the first post-dungeon quest: the room `$D2` owl event, the Mabe
children's kidnapping warning, Moblin Hideout, Moblin King, and BowWow's rescue.

## Source behavior

- Completing Tail Cave leaves Link in overworld room `$D3` and sets BowWow to
  `BOW_WOW_KIDNAPPED` (`$80`).
- `OwlEventEntityHandler` permits room `$D2` only after instrument 1's completion
  bit is set and persists `OW_ROOM_STATUS_FLAG_OWL_TALKED` after the dialog.
- Kid 71 in room `$B0` starts the kidnapping warning sequence and opens
  `Dialog220`; Kid 72 participates without duplicating the dialog.
- Overworld room `$35` warps to map `$15`, room `$F0`.
- Maps at or above `$0B` bypass dungeon `MapLayoutN` resolution in
  `RoomTransitionPrepareHandler`. Their direct room ids therefore use the
  overworld increments: vertical `$10`, horizontal `$01`. The hideout route is
  `$F0 -> $E0 -> $E1 -> $E2`.
- Room `$E1` contains `ENTITY_MOBLIN_KING`; room `$E2` contains
  `ENTITY_BOW_WOW`. Defeating the king persists the miniboss event, and touching
  kidnapped BowWow changes the state to `BOW_WOW_FOLLOWING` (`$01`).

## Architecture

Extend the existing single ordered-play regression so all quest state comes
from the preceding live sequence. Reuse `RoomTransitionCoordinator`, real ROM
collision, room entity ticking, and combat collision APIs. Correct only behavior
that the route proves inconsistent with the disassembly. Keep the focused entity
tests as unit-level coverage and use the ordered test to prove integration and
state handoff.

## End condition

The slice is complete when the same fresh-game session reaches room `$E2`,
rescues BowWow through entity collision, and reports BowWow following Link.
