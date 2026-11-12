# Pairodd projectile object-intersection design

## Scope

Port the room-object side effect in the bank-$04 Pairodd projectile handler.
Movement, Link contact, shield reflection, sword contact, and VFX/audio event
routing already exist. This slice only adds the ROM's post-movement
`ApplySwordIntersectionWithObjects` query and the resulting `$58` removal.

## ROM behavior

`PairoddProjectileEntityHandler` at bank-$04 `$5EFC-$5F28` calls
`UpdateEntityPosWithSpeed_04`, then `label_3B2E`. The trampoline at home
`$3B2E` calls bank-$03 `ApplySwordIntersectionWithObjects` at `$7CAB`.

That routine samples the padded `wRoomObjects` buffer at:

```text
object column = entity X & $F0, divided by $10
object row    = (entity Y - $08) & $F0
object index  = $11 + object row + object column
```

It reads the active room's object-physics table through
`GetObjectPhysicsFlagsAndRestoreBank3`. For the ordinary Pairodd path, the
collision byte is set for physics flag `$01`, ranges `$10-$4F`, `$52-$7B`, and
`$90-$9F`. Flags `$00`, `$02-$0F`, `$50`, `$51`, `$7C-$8F`, and `$A0-$CF`
are passable. `$FF` unloads non-boomerang entities and therefore also removes
Pairodd. The `$D0-$D3` ledge branch is treated as colliding for this
projectile: entity reset leaves thrown-direction `$FF` and unknown state zero,
so the branch reaches the collision write before the generic range tests.

When the object pass sets the entity collision byte, the handler continues
through the shared collision routine and reaches `$5F1B`, clears the active
slot, and creates the existing sword-poke VFX. The object pass itself does not
write a new sound event or bounce response.

## Java contract

Expose the active room's source-backed object query through the existing
`OverworldCollision`, which already owns the padded room grid, current map
physics-table selection, and ROM physics flags. Pass a small object-collision
callback alongside the existing movement collision callback.

After Pairodd fixed-point movement and before Link/sword projectile checks,
call the callback. If it reports a colliding object, remove the slot and queue
one existing `SWORD_POKE` VFX request, matching the shared handler's final
collision-byte branch. Keep Link and sword event ordering unchanged when an
object collision is absent; an object collision ends the handler for that
slot, just as the ROM does.

## Non-goals

- No projectile bounce, reflected direction, or new audio.
- No generic change to every entity's background movement collision.
- No reinterpretation of the room overlay as a second physics source; the ROM
  object-intersection routine reads `wRoomObjects` and the selected physics
  table, not the GBC render overlay.
- No claim that all thrown-object or special `$D0-$D3` stateful paths are
  complete for other entity types.
