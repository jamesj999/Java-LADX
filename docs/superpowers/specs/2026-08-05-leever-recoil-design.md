# ROM-Backed Leever Recoil Design

## Goal

Add Leever's missing bank-$04 sword-recoil path to the live Java room
runtime. A normal sword result must configure the shared ROM `$30` recoil
vector before Leever's hide/emerge/chase/burrow motion, consume the existing
`$0A` ignore-hits countdown, and preserve the recoil state when background
collision blocks the recoil step.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/04_leever.asm:16-22`
  renders Leever, returns during non-interactive frames, calls
  `ApplyRecoilIfNeeded_04`, then runs `UpdateEntityPosWithSpeed_04` and
  `ApplyEntityInteractionWithBackground_trampoline` before its state dispatch.
- `LADX-Disassembly/src/code/entities/bank4.asm:32-94`
  implements `ApplyRecoilIfNeeded_04`: it decrements the ignore-hits
  countdown, moves with the stored recoil speeds using bank-$04 fixed-point
  arithmetic, applies background interaction unless the no-ground option is
  set, and restores the entity's ordinary speed registers. A blocked step
  therefore rolls back position but does not clear the recoil countdown.
- `LADX-Disassembly/src/code/entities/bank3.asm:7386-7500`
  supplies the shared background collision position rollback used by the
  bank-$04 helper. The Java callback already exposes the directional query
  needed by `EnemyRecoilMotion`.
- `LADX-Disassembly/src/code/home/entities.asm:3E8E-3EDE`
  configures the default `$30` recoil vector before the sword damage result
  is applied.

## Design

Add entity `$0E` to `RoomEntityRuntime.usesSharedRecoil`. The existing
`EnemyRecoilMotion` already matches the bank-$04 signed fixed-point movement,
axis order, and blocked-coordinate rollback. `applyEnemyRecoilIfNeeded` must
continue passing `clearOnBlocked == false` for Leever, as it does for the
bank-$06 handlers; only the bank-$03 Octorok/Moblin path clears recoil on a
blocked step.

No Leever state-machine changes are included. After one recoil step, the
existing `LeeverMotion.advance` receives the recoil-adjusted entity and keeps
its current countdown/state behavior. No guessed reversal, stop, ground,
water, pit, conveyor, or recoil-smoke behavior is added in this increment.

## Tests

Add runtime tests that use the existing sword-combat boundary:

1. A Leever sword hit emits the normal enemy-hit jingle, leaves the entity
   active with one health, configures recoil speeds `$D0/$D0`, and moves from
   `(64,64)` to `(61,61)` on the next tick while decrementing ignore-hits to
   `$09`.
2. A left/up blocking callback restores `(64,64)` after that recoil step,
   leaves recoil active, and still decrements ignore-hits to `$09`.

These tests intentionally exercise the live `RoomEntityRuntime`, not the
private motion helper, so dispatch and combat admission are covered together.

## Scope boundary

This slice does not claim Leever's generic post-movement wall/ground
interaction, recoil smoke, entity-specific damage states, or the remaining
bank-$04 handlers. Those remain explicit follow-up work.
