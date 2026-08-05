# Evasive Stalfos clone and fleeing side effects

## Goal

Complete the bank-$15 Evasive Stalfos branch that follows its airborne jump:
the Angler's Tunnel clone spawn, the clone's ROM attributes and vector, the
whoosh sound write, and the fleeing entity's sword-poke/unload side effects.

This remains a room-entity runtime implementation. It does not introduce a
Game Boy CPU or interrupt emulator.

## Source facts

The source of truth is `LADX-Disassembly/src/code/entities/15_stalfos_evasive.asm`
and the shared routines in `src/code/entities/bank3.asm`.

- The normal handler checks `wEntitiesPrivateState1`; a nonzero value enters
  the fleeing handler.
- After the shared countdown decrement, `wEntitiesPrivateCountdown1 == 1`
  enters the clone branch. It returns without cloning when `hMapId < $03`
  and otherwise calls `SpawnNewEntity` with type `$1E`.
- `SpawnNewEntity` scans slots `$0F` down to `$00`, copies the source
  position X/Y/direction/Z into `hMultiPurpose0..3`, configures the new slot,
  and sets its ignore-hits countdown to `$01`. The Evasive handler then
  overwrites that field with register `B`; `AnimateEntities` keeps `B` at
  `$00`, so the clone's effective ignore-hits countdown is `$00`. It then:
  - writes `NOISE_SFX_WHOOSH` (`$0A`);
  - sets projectile-noclip and shadow, giving physics byte `$52`;
  - sets excluded-from-kill-all and no-ground-interaction while preserving
    the type's splash-in-water bit, giving options byte `$1A`;
  - copies the saved X/Y/Z scratch values;
  - increments the new slot's private state to enter fleeing; and
  - applies a length-$18 vector toward Link to the new slot.
- The source Evasive type's configured physics byte is `2 | shadow = $12`.
  The clone's `$52` is an explicit override; it is not the ordinary `$12`
  type default.
- The fleeing handler moves with the current speed and applies background
  interaction. A collision or nonzero ignore-hits countdown writes jingle
  `$07`, creates transient sword-poke VFX at active X/visual Y, and clears the
  entity. Without that condition it clears once active X is at least `$A8` or
  active visual Y is at least `$84`.

## Design

`StalfosEvasiveMotion` remains responsible for handler-local state and returns
a source-shaped `CloneRequest` alongside its normal update. `RoomEntityRuntime`
owns slot allocation and the mutable per-slot arrays, matching the existing
dynamic projectile/entity architecture. This keeps ROM movement math separate
from the runtime's slot bookkeeping and makes failed allocation observable in
tests.

The runtime will keep a per-slot options override only for dynamically spawned
entities. The RoomSession's terrain callback will remain unchanged; the runtime
will skip the generic ground-interaction callback when the override contains
`ENTITY_OPT1_NO_GROUND_INTERACTION`. The original loaded Evasive type continues
to use the ROM table's splash-in-water-only options byte.

Clone creation is an active dynamic `$1E` entity with source load order `-1`,
the ROM catalog's fleeing sprite definition, private state `1`, effective
ignore-hits countdown `0`, physics `$52`, options `$1A`, copied source X/Y/Z, and a vector
calculated by the same fixed-point `GetVectorTowardsLink` algorithm used by the
existing Evasive jump. Newly created slots are marked as spawned this frame so
the current host-side reverse slot traversal does not process them twice.

The existing raw entity event boundary carries the ROM noise write as
`SoundChannel.NOISE, 0x0A`. That is already the shipped spike-trap whoosh
effect, so Evasive clone events reuse the existing raw mapping and do not add a
duplicate gameplay enum. Fleeing sword-poke continues to emit the existing
jingle and VFX request.

## Verification boundary

Tests will cover:

1. clone gating below map `$03` and successful allocation from the last free
   slot at map `$03`;
2. copied source coordinates, private state, sprite pair, physics/options,
   ignore-hits countdown, and length-$18 vector;
3. no whoosh when the slot scan fails;
4. fleeing collision/ignore side effects and out-of-bounds unload without a
   false sword-poke; and
5. raw whoosh consumption through `EnemyCombatEventConsumer` and the shipped
   sound catalog.

The generic room ground-status/water/pit behavior remains a separate slice.
