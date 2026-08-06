# ROM Entity Lifting and Throwing Design

**Date:** 2026-08-06  
**Status:** Approved by continuation of the recommended bounded slice

## Goal

Implement the disassembly's entity status-7/status-8 path so a liftable entity
can enter the lifting transition, follow Link with the correct ROM offsets and
carry state, and leave Link with the ROM throw velocities. Link must expose the
corresponding lifting animation and suppress item use while the carry state is
active.

This is a bounded entity-runtime increment. It does not attempt to implement
all bomb explosion side effects, every entity-specific thrown handler, or the
separate item-pickup presentation state used by heart containers, instruments,
and other story pickups. Those handlers remain separate source-backed
increments; the existing pickup event path is retained for compatibility while
status 7/8 itself becomes ROM-shaped.

## Source of truth

The implementation follows:

- `LADX-Disassembly/src/constants/entities.asm`
  - `ENTITY_STATUS_LIFTED = 7`, `ENTITY_STATUS_THROWN = 8`, and the
    grabbable physics bit.
- `LADX-Disassembly/src/code/home/entities.asm`
  - `AnimateEntity` dispatch and the lifted room-transition position order.
- `LADX-Disassembly/src/code/entities/bank3.asm`
  - `EntityStunnedHandler`, `EntityGetLiftedUp`, `EntityLiftedHandler`,
    `func_003_5795`, and `EntityThrownHandler`.
  - `Data_003_56EA`, `Data_003_56EE`, `Data_003_56F1`, `Data_003_5701`,
    `Data_003_5711`, and `Data_003_5721` are copied as checked source tables.
- `LADX-Disassembly/src/code/bank14.asm`
  - `func_014_5347`, `func_014_53A3`, and the `Data_014_5313`/`5323`/`5333`
    throw-speed tables.
- `LADX-Disassembly/src/code/bank2.asm` and `src/constants/gameplay.asm`
  - `LinkAnimationsList_LiftingObject` and animation states `$3E..$45`.

The host continues to model fixed-point entity speed directly, using the same
16-pixel-per-16-frame accumulator convention already used by the other Java
entity motions. No CPU, VRAM, or emulator abstraction is introduced.

## Architecture

`LiftedEntityMotion` is a pure table-driven helper. It advances one slot's
lift phase and returns the exact carry visual value, selected direction, and
position/z result. Phase zero is promoted to phase one for rendering exactly as
`EntityLiftedHandler` does; transition countdown reloads use the old phase
index, not the incremented phase. Top-view and side-scrolling Z/Y handling are
kept separate.

`ThrownEntityMotion` owns per-slot signed X/Y/Z speeds and fractional
accumulators. `startThrow` selects the top-view/side-scrolling and bomb/non-bomb
table window. Each tick applies entity speed, gravity, and the existing
background collision interface. A blocked X or Y component uses the ROM's
negate-and-quarter wall response. Entity-specific throw triggers and damage
remain event work for later increments.

`RoomEntityRuntime` owns the per-slot status and phase arrays, exposes a
read-only `LiftedEntityState`, and handles status 7 before normal active
handlers and status 8 before ordinary status cleanup. A stunned grabbable
entity enters status 7 only when the caller reports that the equipped Power
Bracelet button is held and the ROM pickup collision rectangle overlaps Link.
An explicit `throwLiftedEntity` operation performs the bank-14 status/velocity
handoff for the supported generic liftable path. Existing tests that construct a
status-7 entity directly initialize the same ROM phase-zero state.

`RoomSession` carries Link's current ROM entity coordinates/direction into the
runtime and forwards the Power Bracelet button state. `Main` derives that state
from the actual A/B inventory slots and input keys, invokes the throw handoff at
the same pre-`AnimateEntities` boundary as the ROM, and synchronizes Link's
carry-state value after entity ticking.

`Link` stores the ROM's non-boolean carry value and source direction. When the
value is exactly `1`, its animation selection uses the ROM lifting animation
list; while nonzero, item dispatch is gated and the entity handler may restore
the source direction. Normal movement remains available during the fully-held
phase, matching the game's carry behavior.

## Error handling and compatibility

All public state setters validate unsigned byte ranges and slot/direction
ranges. Legacy runtime overloads retain their current signatures and default
to a non-interactive Link, so existing motion tests remain valid. Unknown or
unsupported entity types do not receive guessed sprites or entity-specific
effects; their status and table-driven position/velocity state still follow the
generic status path.

## Verification

- Pure lifting tests prove every direction's phase-0 promotion, countdown
  reload, carry-state table, X/Y/Z offsets, and side-scroll branch.
- Pure throwing tests prove all four ROM direction windows, bomb offset, fixed
  point motion, gravity, and wall response.
- Runtime tests prove a stunned grabbable entity lifts only with the Power
  Bracelet button, status-7 entities track Link, status-8 entities advance,
  and a throw clears the carry state.
- Link tests prove animation states `$3E..$45`, source-direction restoration,
  and item-use suppression while carrying.
- Run the complete Gradle suite, `git diff --check`, and inspect the active
  worktree without touching the root checkout's unrelated files.

