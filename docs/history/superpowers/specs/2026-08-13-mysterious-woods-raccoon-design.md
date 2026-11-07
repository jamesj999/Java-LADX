# Mysterious Woods Raccoon Design

## Goal

Implement the first post-sword progression gate in player order: Tarin's raccoon
appearance in overworld room `$51`. The runtime must follow
`LADX-Disassembly/src/code/entities/05_tarin.asm` and use ROM-backed entity
presentation rather than substitute assets or guessed behavior.

## Scope

- Run the outdoor Tarin/raccoon handler only for entity `$3F` in an overworld
  room, with room `$51` as the opening-order integration target.
- Unload the entity when `OW_ROOM_STATUS_CHANGED` (`$10`) is already set.
- Reproduce state 0's raccoon animation, Link push/talk behavior, Dialogs `021`
  and `00D`, and the `wShouldGetLostInMysteriousWoods` boundary flag.
- Let a live magic-powder sprinkle trigger the transformation despite Tarin's
  normal NPC/ignore-hits metadata, as the source collision path does.
- Reproduce states 1-3 sufficiently for the complete transformation sequence:
  Link motion blocking, changing display variants, movement/Z motion, the
  transformation bomb presentation, source sound events, Dialog `00A`, and
  post-transformation Dialog `00B`.
- Persist room status bit `$10` and a RoomSession Tarin-progress equivalent when
  transformation begins so reloads do not restore the raccoon.
- Keep Marin/Tarin house startup behavior unchanged.

## Architecture

Add a focused `TarinRaccoonMotion` class that owns the ROM-shaped per-slot state
and returns immutable update/events. `RoomEntityRuntime` remains the adapter: it
supplies frame, Link position/input, powder collision, countdowns, and background
collision; applies the returned entity presentation; and queues dialog, sound,
motion-block, spawned-bomb, and persistence events. `RoomSession` harvests the
room-status update in the same way as other entity-driven room events.

The magic-powder collision helper will contain an explicit Tarin-raccoon branch
before ordinary enemy filtering. It will be restricted to active outdoor Tarin
in the pre-transformation state, so indoor Tarin and unrelated friendly NPCs
cannot receive powder damage.

## Verification

Use test-first slices:

1. Fresh room `$51` loads an active, ROM-renderable raccoon; status `$10`
   suppresses it.
2. Crossing the source Link-Y boundary exposes the lost-woods request.
3. Action interaction emits the source pre-transformation dialog.
4. A real spawned powder entity overlapping the raccoon starts state 1 and
   blocks Link.
5. The state sequence emits its source presentation/sounds/dialogs and marks
   room `$51` changed.
6. Reloading room `$51` leaves the raccoon absent while indoor house Tarin tests
   continue to pass.
7. Run focused tests, then the full Java suite and `git diff --check`.

## Non-goals

- Other outdoor/indoor Tarin story appearances beyond behavior already present.
- Reworking the generic combat model for every NPC.
- Implementing the remaining forest route (toadstool, witch, Tail Key) in the
  same patch; those remain subsequent order-of-play slices.
