# ROM Common Enemy-Drop Motion Design

**Date:** 2026-08-06  
**Status:** Approved for implementation as the next reconstruction slice

## Goal

Make the common item entity created by the already-wired ROM
`SpawnEnemyDrop` path move with the original fixed-point velocity, gravity,
terrain status, side-scroll ground bounce, and top-down landing bounce. The
existing ROM drop-selection, delayed pickup, slow despawn, and renderer paths
remain unchanged.

## Source behavior

The implementation follows `LADX-Disassembly/src/code/entities/bank3.asm`:

- `SpawnEnemyDrop` initializes top-down `wEntitiesSpeedZTable` to `$18`, or
  side-scroll `wEntitiesSpeedYTable` to `$EC`; the other common-drop speed
  starts at zero.
- `PickableHandler` calls `BouncingEntityPhysics` after the item has passed
  `ReturnIfNonInteractive_03`.
- `BouncingEntityPhysics` calls `UpdateEntityPosWithSpeed_03`, then
  `func_003_6B7B`, then `ApplyEntityInteractionWithBackground`.
- `AddEntitySpeedToPos_03` and `AddEntityZSpeedToPos_03` use an independent
  eight-bit fractional accumulator and signed high-nibble movement. The
  implementation must preserve byte wrapping and carry behavior.
- Top-down gravity subtracts `$02` from speed Z. When the resulting Z byte is
  negative, the item lands at Z `$00`; sufficiently strong landings invert and
  halve speed Z through the ROM's `SRA`/`CPL` sequence, while shallow water or
  weak landings stop the drop.
- Side-scroll gravity adds the status-indexed bytes `{`$02`, `$01`, `$02`,
  `$02`}` and clamps at `{`$40`, `$08`, `$40`, `$40`}`. A down-direction
  background collision aligns Y to `(Y & $F0) + $05`; strong landings use the
  same ROM `CPL`/`SRA` bounce transform and weak landings stop.
- The runtime's existing ground-interaction callback already samples the ROM
  object physics and returns the current ground status. The runtime's existing
  background collision callback already probes the entity collision point and
  exposes the down-direction blocked result needed for side-scroll drops.

## Architecture

### `EnemyDropMotion`

Add a focused handler-owned state object in `linksawakening.world`. It owns
per-slot speed Y/Z and their fixed-point accumulators, initializes the two ROM
drop launch speeds, applies one pre-terrain motion step, and applies the
post-terrain bounce. It returns immutable `RoomEntity` values and keeps all
byte arithmetic masked to `$00..$FF`.

The helper does not add horizontal speed because `SpawnEnemyDrop` leaves the
common drop's speed X at zero. It does not emit audio; the existing event
boundary can receive the source-specific bounce sounds in a later slice.

### `RoomEntityRuntime`

Track which dynamic slot is a common enemy drop rather than inferring it from
`sourceLoadOrder == -1` (projectiles and scripted clones also use that value).
Run `EnemyDropMotion.advance` before the shared ground callback, passing the
previous slot ground status. Run `EnemyDropMotion.bounce` after the callback,
using the callback's current ground status and a down-direction probe from the
existing `RoomEntityBackgroundCollision` boundary. Clear the handler state
when the slot is collected, unloaded, or reused.

The existing `verticalSpeedZ` bridge will expose the drop's speed Z to the
ground callback, preserving the source splash/ground-speed boundary for
top-down drops.

### Tests and documentation

Add a pure motion test for fixed-point launch/gravity and both bounce branches,
then extend the runtime integration tests so a real ROM-selected death drop
changes Z/Y on subsequent ticks and responds to a supplied down-direction
collision. Update the reconstruction roadmap to record that common drops are
now visibly moving rather than only being spawned.

## Explicit scope boundaries

This slice does not implement handler-specific drop writes, item-specific
pickup behavior, bounce audio, every entity's complete background collision
table, or room-wide emulator state. It only completes the motion/terrain
consumer of the common drop object that already exists in the Java runtime.

## Verification

The focused motion and runtime tests must pass first. Then run:

```text
gradle -p java test --no-build-cache --rerun-tasks
git diff --check
```

The final report must distinguish the newly visible common-drop motion from
the still-pending handler-specific drop families.
