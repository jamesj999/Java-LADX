# Entity Ground Status and Water Interaction

## Goal

Port the terrain-status and water-transition portion of the disassembly's
`ApplyEntityInteractionWithBackground` into the Java entity runtime. This
increment must make the runtime use the ROM object physics table for entity
ground status, preserve the existing conveyor behavior, unload ordinary
entities that enter deep water/lava, retain the five ROM exceptions, and emit
the ROM water-splash VFX and jingle at the correct transition boundary.

Pit falling, wall collision rollback, and `EntityFallHandler` remain a separate
increment. They are separate state machines in the source and require falling
targets, transition countdowns, and an unload path that this slice does not
invent.

## Source behavior

The authoritative path is `LADX-Disassembly/src/code/entities/bank3.asm`:

- `ApplyEntityInteractionWithBackground` saves and clears the entity's
  ground-status byte on every call.
- Positive nonzero Z skips terrain sampling. Negative Z continues through the
  helper, subject to `ENTITY_OPT1_NO_GROUND_INTERACTION`.
- `func_003_7E0E` samples the padded room-object area at
  `(entityX - 1, entityY - 7)`, returning the raw object, its physics byte,
  and the sampled cell's top-left coordinates.
- Deep-water and lava physics unload ordinary entities. Fish (`$CC`), PeaHat
  (`$A0`), Rooster (`$D5`), BowWow (`$6D`), and Marin at the shore (`$C1`)
  retain ground status `$02`. Marin's separate pit-falling exception—falling
  only when Link is falling on the well (`$61`)—belongs to the deferred pit
  state machine, not this deep-water branch.
- The regular status mapping is ROM-shaped: deep-water/side-scroll water and
  nonzero unclassified physics use `$01`, shallow water uses `$02`, and grass
  uses `$03`.
- A regular status-transition splash is considered only when options bit `$08`
  is set, the old and new ground status differ, neither status is tall grass,
  and the source's downward-motion gate permits it. An ordinary deep-water or
  lava unload jumps directly to `.createWaterSplash`, bypassing that gate. The
  event writes jingle `$0E` and transient VFX `$01` at the entity position.
- Conveyor movement is every fourth frame using the existing eight-entry ROM
  X/Y tables and remains after the terrain/status work.

## Design

### Alternatives considered

1. Keep the callback returning only `RoomEntity` and let `RoomSession` mutate
   runtime state through a side-channel. This preserves the old signature but
   makes unload/splash ordering implicit and couples the session to runtime
   internals.
2. Introduce a standalone terrain service that owns entity status and events.
   This gives the service a clean API but duplicates the runtime's existing
   post-handler lifecycle boundary and makes direct runtime tests less useful.
3. Return a small result record from the existing callback (chosen). The ROM
   sample and exception policy stay in the room/session layer while the
   runtime applies lifecycle resets and drains effects in source order.

### Terrain sample

Extend `OverworldCollision` with a source-shaped `GroundInteractionSample`
record containing the raw object ID, physics flag, and aligned object-left/top
coordinates. Keep `objectPhysicsFlagAtGroundInteraction` as a compatibility
wrapper over that sample. This prevents the future pit increment from having
to reconstruct `hObjectUnderEntity`, `hIntersectedObjectLeft`, and
`hIntersectedObjectTop` a second time.

### Runtime callback boundary

Change `RoomEntityGroundInteraction` to return a `Result` rather than only a
replacement entity. The callback also receives the current shared vertical
speed byte and whether the active room is side-scrolling, so the session can
apply the source's downward-motion splash gate without guessing from the
rendered Z position. The result carries:

- the post-terrain entity;
- the new per-slot ground-status byte;
- whether the source unloaded the entity;
- whether a water-splash side effect was requested.

For entity families whose vertical speed is not yet represented by a Java
motion object, the runtime passes zero; that is the source-safe result (no
downward splash), and the missing speed-table port remains explicit work for a
later entity increment.

`RoomEntityRuntime` owns the per-slot ground-status array and translates a
splash result into its existing transient-VFX request list plus a raw JINGLE
entity event. The callback stays responsible for ROM room physics and entity
exceptions; the runtime stays responsible for lifecycle reset and event
delivery. Existing direct runtime callers use an unchanged result by default.

The callback is invoked after the family handler and before final display-list
state is stored, matching the source call order. Ground status is reset on
slot unload and is observable in package tests for deterministic verification.

### Water-splash rendering and audio

Add ROM IDs `$01`/`$0E` to the Java VFX/audio boundaries. Extend the ROM-backed
transient sprite sheet to expose the Link-character tile range used by
`Data_002_57FD` (`$18`) in addition to the existing character-VFX range. Add
the two OAM placements and countdown phase selected by
`RenderTranscientWaterSplash` using the same `SpritePlacement` path as the
existing effects. Map raw jingle `$0E` to `GameplaySoundEvent.WATER_SPLASH`.

The renderer will follow the normal top-down water frame. The source's
side-scroll-specific `$C1A7` branch and speed-X/speed-Y damping are explicitly
left for the side-scroll terrain increment, because the current Java entity
snapshot does not yet expose the shared ROM speed tables.

## Tests and acceptance criteria

- Collision tests prove the raw object/physics/aligned-cell sample comes from
  the padded buffer and remains unsigned.
- Runtime tests prove the callback receives the previous status, stores the
  returned status, resets it on unload, queues splash VFX/sound, and preserves
  existing position updates.
- Room/session tests prove the ROM options and physics mappings, positive-Z
  skip, deep-water exception list, and conveyor cadence.
- VFX tests prove the `$18`/`$20` tile sources, ROM two-sprite placement, and
  countdown phase. Audio tests prove jingle `$0E` maps to the new gameplay
  event and catalog lookup.
- `gradle clean test` in `java/` must pass after the focused tests pass.
