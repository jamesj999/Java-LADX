# Entity collision ignore-hits propagation design

**Status:** Approved under the standing pixel-perfect reconstruction objective

## Goal

Carry the ROM entity's current `wEntitiesIgnoreHitsCountdownTable` value into
the shared background-collision decision so recoil and other moving entity
handlers make the same pit/lava decisions as `ApplyEntityCollisionWithObject`.

This is a focused follow-up to the shared entity collision resolver. It does
not attempt to model every entity handler's private WRAM state.

## Source contract

The source of truth is `LADX-Disassembly/src/code/entities/bank3.asm`:

- `ApplyEntityCollisionWithObject` at `$03:$7ACD` checks the entity Z value
  and then `wEntitiesIgnoreHitsCountdownTable` for lava (`$0B`), normal pits
  (`$50`), and pit warps (`$51`).
- A grounded entity treats those physics values as walls when the countdown is
  zero, but passes them when the countdown is nonzero.
- `ENTITY_MOLDORM` (`$59`) is the explicit exception: it still treats pits as
  walls while ignoring hits.
- The countdown is read after the handler's source-level decrement/recoil
  ordering. Java's existing `tickInternal` and `applyEnemyRecoilIfNeeded`
  already maintain that ordering; the probe must observe the array's current
  value rather than decrementing or mutating it.

## Architecture

Extend `RoomEntityBackgroundInteraction` with a default five-argument probe
that accepts `ignoreHitsCountdown` and delegates to the existing four-argument
probe. This preserves direct callers and existing lambdas while allowing the
room session to override the state-aware call.

When a `RoomEntityRuntime` has a rich interaction installed, its tick pass
adapts that interaction into the legacy boolean `RoomEntityBackgroundCollision`
callback used by the family handlers. Every legacy movement/recoil handler
therefore sees the same state-aware result without widening all of their
individual APIs. Direct runtime callers that only provide the old boolean
callback retain their current behavior.

The bank-$03 roaming handler is also a direct rich-probe consumer, so
`RoamingEnemyMotion` exposes a state-aware overload. The existing overload
delegates with a zero countdown for compatibility; the live runtime selects
the new overload and passes the slot's current value to both axis probes.

`RoomSession` owns the state-aware rich probe. It keeps the existing ROM sample,
object, and physics lookup, then calls the resolver overload with the supplied
countdown. The four-argument test/helper path uses the active runtime's current
countdown when one exists.

`EntityBackgroundCollisionResolver` gains an overload that accepts the
countdown; its existing overload delegates with zero. Only the pit/lava branch
uses the new value. All fine-shape, open-door, switch-block, ledge, broad
physics, and no-wall decisions remain unchanged.

## Testing

- Extend the resolver tests to prove grounded ordinary entities pass lava,
  pits, and pit warps only with a nonzero countdown, while Moldorm remains
  blocked and airborne entities remain passable.
- Add a room-session regression that sets the active entity countdown and
  probes the shipped room through the session boundary, proving the four-
  argument helper observes live runtime state.
- Add a roaming-motion regression that proves the five-argument probe receives
  the supplied countdown on a movement collision.
- Add a runtime regression with an Octorok receiving recoil. Install a rich
  probe that passes and an old boolean callback that blocks; the entity must
  move through the rich path, and the rich callback must observe the current
  countdown. This proves the adapter is used by a legacy movement handler.
- Run the focused tests, then `gradle -p java clean test` and `git diff --check`.

## Scope boundary

This slice does not invent thrown-direction or ledge timers, switch-block WRAM
state, hookshot-chain transition state, or a full per-handler decrement table.
It only makes the already-exposed shared ignore-hits countdown available to the
already-wired entity/background collision path.
