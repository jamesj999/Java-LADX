# Bottle Grotto First Key Ordered-Play Design

## Scope

Extend the uninterrupted fresh-game regression from Bottle Grotto entrance
room `$36` through the dungeon's opening torch gate and first Small Key in
room `$32`.

## Source behavior

- `MapLayout1` places entrance `$36` south of `$31`; `$31` is west of `$32`.
- Room `$31` contains two unlit torch objects `$AB` at locations `$34` and
  `$35`, an open south door `$F5`, and a closed east shutter `$F3`.
- `DungeonEventsTable[$31]` is `$25`:
  `TRIGGER_LIGHT_TORCHES | EFFECT_OPEN_LOCKED_DOORS`.
- `CheckLightTorchesTrigger` resolves only when the live torch counter is
  exactly two. The ordinary open-door effect then opens the east shutter.
- Closed shutters finish by writing the direction-specific object pairs from
  bank `$02`'s close-door tables. The shared open-door effect restores the
  corresponding ROM open-door macro at the shutter's actual room position.
- Room `$32` contains one evasive and one aggressive Stalfos.
  `DungeonEventsTable[$32]` is `$81`:
  `TRIGGER_KILL_ALL_ENEMIES | EFFECT_DROP_KEY`.
- Defeating both Stalfos creates the normal entity `$30` Small Key. Collecting
  it increments the current dungeon's Small Key count.

## Architecture

Keep the same `RoomSession`, `Link`, inventory, collision, and transition
objects from the ordered regression. Use live magic-powder interactions on the
two ROM torch cells, extend the common room-event trigger dispatcher with the
missing source trigger `$05`, reconstruct the source closed/open shutter
states in room objects and tiles, cross the opened boundary, defeat the source
Stalfos through the existing combat runtime, and collect the ordinary key-drop
entity.

## Verification

The slice is complete when ordered play reaches `$31` through collision,
lights both torches, opens the east shutter via event `$25`, reaches `$32`,
defeats both Stalfos, resolves event `$81`, and collects exactly one Small Key.
