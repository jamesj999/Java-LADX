# ROM-Backed Water Tektite Water-Collision Design

## Goal

Restore the bank-$07 entity-specific collision rule for Water Tektite. It
must move through shallow-water (`$05`) and deep-water (`$07`) cells, while
ordinary terrain remains subject to the existing background collision query.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/bank3.asm:7820-7934` implements
  `ApplyEntityCollisionWithObject`. After reading the object physics byte, it
  special-cases `ENTITY_WATER_TEKTITE` and returns no collision only for
  `OBJ_PHYSICS_SHALLOW_WATER` and `OBJ_PHYSICS_DEEP_WATER`; all other physics
  values continue through the normal collision path.
- `LADX-Disassembly/src/code/entities/07_water_tektite.asm:10-41` calls the
  shared background interaction after `UpdateEntityPosWithSpeed_07`, so this
  exception must be applied at the room-session collision boundary used by
  `WaterTektiteMotion`.
- `RoomSession.entityBackgroundCollision` currently exposes only the final
  blocked/not-blocked result from `OverworldCollision.pointBlocked`, which
  loses the physics byte needed for the Water Tektite exception.

## Design

Add a coordinate-based physics lookup to `OverworldCollision` using the same
cell conversion, padded room-object stride, and active physics-table selection
as `pointBlocked`. In `RoomSession.entityBackgroundCollision`, compute the
existing ROM collision point first; for entity `$99`, let the movement pass
when that point's physics flag is `$05` or `$07`. Otherwise delegate to the
existing `pointBlocked` logic unchanged.

This keeps the exception narrow: it does not make Water Tektite pass lava,
pits, doors, fine-collision objects, or overlay tree cells, and it does not
change Link collision semantics.

## Tests

- Verify the new coordinate physics lookup reads the expected padded room cell
  and selected ROM physics table.
- Load real ROM indoor room `$65`, replace its active object cells with the
  ROM deep-water object `$0E`, and advance the room session. The resident
  Water Tektite must leave its initial position instead of being reset by a
  wall collision.
- Keep the existing solid-cell behavior covered through the unchanged
  `pointBlocked` path.

## Scope boundary

This increment does not add the broader ground-status, pit-fall, conveyor,
splash, or entity-options model. Those remain separate source-backed gaps.
