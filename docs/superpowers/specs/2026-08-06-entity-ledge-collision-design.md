# Entity ledge collision state design

**Status:** Approved under the standing pixel-perfect reconstruction objective

## Goal

Replace the Java resolver's conservative "all ledges are solid" behavior with
the stateful branch from `ApplyEntityCollisionWithObject` in
`LADX-Disassembly/src/code/entities/bank3.asm`.

## Source contract

The ROM's ledge branch at `$03:$7BE4-$7C28` handles physics values `$D0-$D3`
(`OBJ_PHYSICS_LEDGE + direction`):

- It compares the ledge direction (`physics - $D0`) with the entity's
  `wEntitiesThrownDirectionTable` byte.
- A matching direction means the entity is being thrown off the ledge. At
  `z == 0` it collides; at nonzero `z` it passes and increments
  `wEntitiesUnknowTableJ`.
- A nonmatching direction is solid for `ENTITY_WRECKING_BALL` (`$A8`) and
  whenever `wEntitiesUnknowTableJ` is zero.
- With a nonzero timer, the nonmatching direction passes. The timer decrements
  only when `(hFrameCounter & $03) != 0`; outdoors it also requires
  `(hFrameCounter & $01) != 0`. Indoors therefore decrements on frames 1, 2,
  and 3 of each four-frame group, while overworld rooms decrement on frames 1
  and 3.
- The timer is an unsigned byte and increments/decrements with Game Boy byte
  semantics.

The source also clears `wEntitiesUnknowTableJ` in the negative-Z path before
collision processing. The Java room probe therefore treats a signed-negative
raw Z byte (`z & $80 != 0`) as a zero ledge timer for that decision and keeps
the zeroed value in runtime state.

## Architecture

Add a small immutable collision-state record containing the current frame,
indoor flag, thrown direction, and ledge timer. The resolver exposes a
state-aware result carrying both the normal `EntityBackgroundCollisionResult`
and the timer value to store after the decision. Existing stateless resolver
callers continue to delegate with ROM-neutral state and preserve their API.

`RoomEntityRuntime` owns per-slot `thrownDirection` and ledge-timer arrays.
The arrays are initialized/reset as the ROM's entity reset path does. Throw
setup keeps the direction already used by the Java thrown-motion port; timer
updates are applied by the session's room-owned collision probe.

`RoomEntityBackgroundInteraction` gains a default frame-aware overload after
the existing five-argument ignore-hits overload. `RoomEntityRuntime` captures
the current frame while adapting the rich probe to legacy boolean movement
callbacks, and `RoamingEnemyMotion` receives the same frame for its direct rich
probes. Existing callers continue to delegate with frame zero.

`RoomSession` supplies the active room's indoor flag and runtime slot state to
the resolver, writes the returned ledge timer back to the runtime, and clears
the timer for negative-Z samples before resolving. This keeps all collision
lookups on the existing ROM-backed object/physics path.

## Testing

- Resolver tests cover matching-direction ground collision, matching-direction
  airborne pass plus timer increment, nonmatching zero-timer collision,
  nonmatching timed pass, wrecking-ball collision, indoor cadence, overworld
  cadence, and unsigned timer updates.
- Runtime tests cover the default/reset state and the existing thrown-direction
  setup path.
- Room-session tests prove a live stateful probe updates the timer on a
  matching ledge and applies indoor versus overworld cadence on subsequent
  probes.
- A roaming-motion test proves the frame-aware rich probe receives both the
  ignore-hits value and current frame during movement collision.
- Run focused tests, then `gradle -p java clean test` and `git diff --check`.

## Scope boundary

This slice does not implement switch-block state (`wSwitchBlocksState`), the
hookshot-chain transition threshold, or unrelated entity-handler migrations.
It also does not reinterpret the ROM's unknown table as a generic countdown;
the Java field is named for the ledge behavior it now models.
