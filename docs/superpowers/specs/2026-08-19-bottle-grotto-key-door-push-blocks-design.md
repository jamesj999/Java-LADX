# Bottle Grotto Key Door and Push-Blocks Ordered-Play Design

## Scope

Extend the uninterrupted fresh-game regression from Bottle Grotto room `$34`'s
second Small Key through room `$35`'s north key door and solve room `$2F`'s
two-block staircase event. Continue using the live `RoomSession`, `Link`, room
collision, transition, entity, pushed-block, event-effect, and persistence paths.

## Source behavior

- `MapLayout1` places room `$35` east of `$34` and room `$2F` immediately north
  of `$35`. East and south of `$35` contain no valid room.
- `IndoorsA35` contains `OBJECT_KEY_DOOR_TOP` (`$EC`) at location `$07` and an
  open west door. `IndoorsA35Entities` contains two optional Spiked Beetles
  (`$2C`), while `DungeonEventsTable[$35]` is `$00`; defeating them is not a
  progression condition.
- Opening the top key door consumes one Small Key, animates the door, blocks
  Link motion during the eight-frame opening, and synchronizes the directional
  open-door status with room `$2F` so both sides reload open.
- `IndoorsA2F` contains pushable blocks `$A7` at locations `$33` and `$36`, a
  hidden staircase `$BF` at `$18`, and room event `$A7`:
  `TRIGGER_PUSH_BLOCKS | EFFECT_REVEAL_STAIRWAY`.
- `PushedBlockEntityHandler` resolves trigger `$07` only when a pushed block
  finishes horizontally against object `$A7` or settled pushed block `$A6`.
  The intended arrangement pushes both blocks inward once: the first settles
  as `$A6`; the second then settles beside it and resolves the trigger.
- The ordinary stair-reveal effect marks room `$2F` complete, animates the
  reveal, and replaces the hidden staircase at `$18` with active stairs.

## Architecture

Extend the existing ordered-play test after room `$34` without resetting its
session or inventory. Cross east into `$35`, assert its two ROM-loaded Beetles
and event `$00`, unlock the top door through `tryUnlockIndoorKeyDoor`, tick the
normal animation, verify key consumption and bidirectional status persistence,
then cross north into `$2F` through the collision-aware boundary helper.

Use `tryPushIndoorBlock` for two sustained, collision-valid pushes and let the
shared pushed-block entity complete each 33-frame motion. Enhance the shared
completion path so trigger `$07` checks the settled destination's horizontal
neighbors for `$A7` or `$A6`; if present, set the same event-resolved state used
by the ROM. Do not special-case room `$2F`, location `$33`, or location `$36`.
The existing generic event-effect path must perform completion persistence and
stair reveal.

## Error boundaries and invariants

- A single settled block must not resolve event `$A7` when neither horizontal
  neighbor is `$A7` nor `$A6`.
- Vertical adjacency must not resolve `TRIGGER_PUSH_BLOCKS`.
- The second inward push must resolve the trigger only after block motion
  completes.
- Every animation or event wait in the integration test must have a deadline
  followed by an explicit state assertion.
- The room `$35` Beetles remain optional and active; this slice must not clear
  them, rewrite their health, or claim sword-only defeat behavior.

## Verification

The slice is complete when ordered play spends the second key in room `$35`,
persists the open north/south door status, enters `$2F`, performs both real
pushes, proves the first push leaves event `$A7` unresolved, proves the second
horizontal adjacency resolves it, reveals stairs at `$18`, and persists room
completion. Focused key-door, pushed-block, room-event, transition, and full
Java suites must remain green.
