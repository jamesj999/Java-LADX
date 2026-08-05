# ROM-Backed PeaHat Recoil Design

## Goal

Mirror PeaHat's bank-$07 sword-recoil admission and fixed-point movement in
the live Java room runtime. A grounded PeaHat sword hit must configure the
shared ROM `$30` recoil vector, consume the existing `$0A` ignore-hits
countdown on the next handler tick, and preserve the recoil state when the
background path blocks that step.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/07_peahat.asm:10-22` renders PeaHat,
  returns during non-interactive frames, calls `ApplyRecoilIfNeeded_07`, then
  runs the normal damage handler, movement, Z update, and background
  interaction before dispatching the rest/takeoff/flying state.
- `LADX-Disassembly/src/code/entities/07_peahat.asm:31-55` clears the
  airborne hitbox and sword-clink suppression flags only when the resting
  handler sees `z == 0`; the live combat gate therefore admits PeaHat only in
  its grounded state for this increment.
- `LADX-Disassembly/src/code/entities/bank7.asm:214-273` implements
  `ApplyRecoilIfNeeded_07`: it decrements the ignore-hits countdown, temporarily
  uses the stored recoil speeds for bank-$07 fixed-point movement, applies the
  background helper, and restores ordinary speeds. A blocked step does not
  clear the recoil state.
- `LADX-Disassembly/src/code/home/entities.asm:3E8E-3EDE` configures the
  default `$30` recoil vector before the sword-damage result is applied.

## Design

Add entity `$A0` to `RoomEntityRuntime.usesSharedRecoil`. The existing
`EnemyRecoilMotion` already matches the bank-$07 signed fixed-point recoil
movement and directional background rollback. PeaHat must use the
non-clearing-on-block policy already selected for non-roaming bank-$06 and
bank-$04 handlers; the bank-$03 Octorok/Moblin stop-on-collision behavior must
remain unchanged.

No PeaHat state-machine changes are included. No new hitbox-flag or
sword-clink state is inferred from the Java combat path; the existing
grounded-only gate remains authoritative for this slice. Generic ground,
water, pit, conveyor, recoil-smoke, and remaining damage-state behavior stay
deferred.

## Tests

Add live `RoomEntityRuntime` tests using a grounded PeaHat fixture:

1. A sword hit with a level-zero/non-damaging attack context emits the normal
   hit event while keeping the entity active, configures recoil speeds
   `$D0/$D0`, and moves `(64,64)` to `(61,61)` on the next tick while changing
   the ignore-hits countdown from `$0A` to `$09`.
2. A left/up blocking background callback restores `(64,64)` after that recoil
   step, leaves recoil active, and still decrements the countdown to `$09`.

The existing grounded/airborne combat regression continues to guard the
source hitbox behavior boundary.

## Scope boundary

This increment does not claim PeaHat's hitbox-flag or sword-clink plumbing,
post-movement wall/ground interaction, water behavior, recoil smoke, or the
remaining bank-$07 handlers.
