# ROM-selected entity collision probe

## Goal

Make the shared entity background probe use the same collision-box selection as
bank `$03`'s `ApplyEntityInteractionWithBackground`, without changing the
handler-local movement or rollback behavior already implemented in Java.

## Source invariants

- `HitboxFlagsForEntity` is indexed by entity type at bank `$03:$40FB`.
- The low two bits select one of four collision boxes.
- `EntityCollisionPointsX` is bank `$03:$785F`, with four signed offsets per
  collision box in right/left/up/down order.
- `EntityCollisionPointsY` is bank `$03:$786F`, with the same direction order.
- The ROM samples `posX - 8 + xOffset` and `posY - 16 + yOffset`.
- A missing or out-of-range hitbox entry must preserve the normal box default;
  this keeps synthetic/test entities and uninitialized ROM fixtures safe.

## Design

`RomTables` loads the three small ROM-backed tables. A package-local
`EntityCollisionPointProbe` turns an entity, direction, and proposed position
into the exact sampled pixel. `RoomSession.entityBackgroundCollision` delegates
to that probe, then keeps its existing physics/overlay decision.

This is intentionally a probe-only increment. It does not add a second generic
post-handler collision pass, directional collision flags, or collided-object
reporting. Those behaviors need a separate state model because several current
motion handlers already perform their own source-ordered rollback.

## Verification

- Assert the table loader preserves unsigned hitbox flags and signed point
  offsets from the ROM.
- Assert all four collision-box rows and all four directions produce the
  disassembly coordinates.
- Keep the existing live-room movement tests passing.
