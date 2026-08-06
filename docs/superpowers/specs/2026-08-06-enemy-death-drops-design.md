# ROM Enemy Death and Drop Lifecycle Design

**Date:** 2026-08-06  
**Status:** Approved for implementation as the next reconstruction slice

## Goal

Connect the existing ROM-backed enemy death presentation to the common
`DidKillEnemy`/`SpawnEnemyDrop` lifecycle, so a supported ordinary enemy can
finish dying, update first-eight room persistence, and spawn the same ROM item
entity and drop timers that the original game would create.

This is an incremental subsystem boundary, not a claim that every scripted,
boss, or entity-specific drop handler is complete.

## Source behavior

The implementation follows these disassembly paths:

- `LADX-Disassembly/src/code/entities/bank3.asm:EntityDeathHandler`
  dispatches the terminal death frame to `DidKillEnemy`.
- `LADX-Disassembly/src/code/home/entities.asm:DidKillEnemy` calls
  `SpawnEnemyDrop`, increments `wKillCount`, records `wKillOrder`, and sets a
  first-eight `wEntitiesClearedRooms` bit before unloading the entity. A
  dynamic entity whose load order is `$FF` skips the kill-count and persistence
  writes, matching the existing Java `sourceLoadOrder == -1` convention.
- `LADX-Disassembly/src/code/entities/bank3.asm:SpawnEnemyDrop` handles a
  non-random per-entity drop, the guardian-acorn and Piece-of-Power counters,
  low-health random chances, the indexed drop table, and the eight-entry random
  fallback table.
- `LADX-Disassembly/src/code/entities/reset_entity.asm:ResetEntity` clears
  `wEntitiesDroppedItemTable` to zero. The first implementation therefore
  treats zero as the source random-drop sentinel and preserves an explicit
  `$FF` as no drop when future handler initialization supplies it.
- `SpawnEnemyDrop` configures the created item with reverse-slot allocation,
  source position, source Z, slow despawn `$80`, blink countdown `$18`, and
  private countdown `$03`; side-scroll items additionally receive speed-Y
  `$EC` instead of the top-down Z speed `$18`.

The ROM data is read from the shipped ROM, never copied into a Java literal
drop table except for test assertions over decoded bytes. The indexed data
comes from bank `$03`'s `DestroyedEntityHealthGroupOffsetTable`,
`DropTableByIndex`, `RandomDropChanceTable` at `$55AB`,
`RandomDropChanceTableLowHealth` at `$55B9`, and `DropTableRandom`.

## Architecture

### `EnemyDropResolver`

Add a pure ROM-table decoder/resolver in `linksawakening.world`.

It accepts the destroyed entity's type, health group, current dropped-item
byte, room mode, boss/power-up/low-health state, maximum hearts, persistent
drop counters, and the frame-scoped `IntSupplier` already used by the entity
runtime. It returns an immutable result containing the selected item type (or
no item), updated counters, and the exact number/order of random-byte reads.

The resolver preserves the source branch order:

1. explicit dropped item (`$FF` means none; any other nonzero byte is direct),
2. guardian-acorn counter and eligibility gates,
3. Piece-of-Power counter and eligibility gates,
4. low-health or normal chance-table gate,
5. indexed drop table, then the eight-entry random table when the indexed
   entry is `ENTITY_NONE`.

Counters live in the runtime/session state rather than `PlayerState`, because
they are WRAM gameplay counters and are not currently part of the saved-file
model. Room mode and player values are supplied at the existing
`RoomSession.tickEntities` boundary.

### `RoomEntityRuntime`

Extend each runtime slot with the source dropped-item byte, initialized to the
`ResetEntity` zero sentinel. At the terminal DYING dispatch, resolve the drop
before clearing the source entity. For a resolved item, allocate the highest
free slot, use the item catalog definition already available through
`EntitySpriteHandlerCatalog`, and initialize its ROM status/timers/position/Z.

The runtime also emits the existing pending first-eight clear mask for static
entities and exposes the updated drop counters to `RoomSession`. Dynamic
entities retain load order `$FF` and never create room-persistence bits.

The drop item uses the normal room entity renderer and pickup path. No generic
fabricated sprite or separate host-only draw path is introduced.

### `RoomSession` and `Main`

`RoomSession` owns the resolver counters and passes a source-shaped drop
context into each entity tick. It applies the returned clear mask and keeps
the dynamic drop in the live `RoomEntitySnapshot`. The low-health flag is
computed from the ROM `ThresholdLowHealthTable` used by `UpdateHealth`, and
the boss flag is derived from the room's ROM entity options at the same
room-load/runtime boundary. `Main` supplies the existing `PlayerState` values
needed for maximum hearts and active power-up; pickup consumption remains the
existing entity pickup boundary.

## Explicit scope boundaries

This slice covers common drops emitted after the already-modeled ordinary
enemy death path. It does not fabricate handler-specific writes for Like-Like
shield recovery, key points, Color Dungeon scripts, bosses, bomb destruction,
or entity handlers that set `wEntitiesDroppedItemTable` dynamically. Those
handlers remain explicit follow-up work and must be added at their source
initialization points.

The slice also does not turn the project into an emulator: only the ROM tables,
entity state transitions, and existing renderer/runtime boundaries are
implemented.

## Verification

Tests will cover:

- all decoded drop-table bytes and source sentinel semantics;
- guardian-acorn and Piece-of-Power counter thresholds and eligibility gates;
- normal versus low-health chance masks and random fallback selection;
- random-byte call order and no-read early returns;
- `DidKillEnemy` kill count, kill order, first-eight persistence, and dynamic
  entity exclusion;
- highest-free-slot item spawning, source position/Z, `$80/$18/$03` timers,
  side-scroll speed-Y, and renderer/pickup visibility;
- terminal death integration through a real `RoomEntityRuntime` and
  `RoomSession` snapshot.

The focused tests must pass before the complete `gradle -p java test
--no-build-cache` suite is run.
