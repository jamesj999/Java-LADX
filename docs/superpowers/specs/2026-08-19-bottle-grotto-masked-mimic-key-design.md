# Bottle Grotto Masked-Mimic Key Ordered-Play Design

## Scope

Extend the uninterrupted fresh-game regression from Bottle Grotto room `$34`
through its two Masked Mimics and collect the room-event Small Key. Preserve the
global crystal-switch state carried from room `$33` and use the ordinary combat,
room-event, drop-motion, and pickup paths.

## Source behavior

- `IndoorsA34` contains the room's switch-block objects and its ROM-authored
  exits; `IndoorsA34Entities` contains two Masked Mimics (`$8F`) and one
  droppable rupee (`$2E`).
- `DungeonEventsTable[$34]` is `$81`:
  `TRIGGER_KILL_ALL_ENEMIES | EFFECT_DROP_KEY`.
- Masked Mimics mirror Link's held direction through the ordinary bank `$19`
  handler. Their dynamic options make the shielded side clink and leave the
  opposite side vulnerable; they retain normal enemy health, recoil, death,
  persistence, and kill-all participation.
- The droppable rupee is not part of the kill-all count. Defeating both Mimics
  completes the room event and invokes the ordinary key-drop effect. The key
  follows the shared airborne/landing pickup path and increments Bottle Grotto's
  Small Key count from one to two.

## Architecture

Continue the same `RoomSession`, `Link`, `PlayerState`, collision map, and frame
counter used by the ordered-play regression. Initialize room `$34` normally,
drive Masked-Mimic direction/options through live entity ticks, approach their
vulnerable side using collision-aware movement, and damage them through
`resolveEntityCombat`. Do not clear slots, rewrite health, or complete the room
event directly. Let the shared death/event runtime spawn the key, wait for its
normal landing, then collect it through `collectEntityIfNeeded`.

Any failure must be fixed in the shared handler, combat-option, room-event, or
drop/pickup component that owns the source behavior. Do not special-case room
`$34` or exclude the rupee with a room-specific rule.

## Verification

The slice is complete when uninterrupted ordered play observes event `$81`, two
ROM-loaded Masked Mimics and the droppable rupee, defeats both through live
vulnerable-side combat, completes the event, lands and collects entity `$30`, and
finishes with exactly two Bottle Grotto Small Keys. Focused Mimic, room-event,
transition, and full Java suites must remain green.
