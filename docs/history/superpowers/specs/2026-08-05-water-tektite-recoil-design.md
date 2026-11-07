# ROM-Backed Water Tektite Shared Recoil Design

## Goal

Mirror Water Tektite's bank-$07 sword-recoil path before its own movement
handler runs. A sword hit must configure the shared `$30` recoil vector and
the `$0A` ignore-hits window, then the next handler tick must apply one
fixed-point recoil step before Water Tektite's three-state motion. A blocked
step must preserve the bank-$07 recoil state.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/07_water_tektite.asm:10-41` calls
  `ApplyRecoilIfNeeded_07` immediately after the interactive-entity gate and
  before sprite-frame selection, movement, background interaction, and the
  normal enemy damage handler.
- `LADX-Disassembly/src/code/entities/bank3.asm` supplies the shared enemy
  sword-hit setup: the default sword recoil uses the `$30` recoil vector and
  the normal enemy hit path starts a `$0A` ignore-hits countdown.
- The bank-$07 recoil helper does not use the roaming-enemy collision stop
  path. If background interaction blocks the movement step, the recoil
  countdown and fixed-point recoil state therefore remain active.

## Design

Admit `ENTITY_WATER_TEKTITE` to the existing shared-recoil family in
`RoomEntityRuntime`. Reuse the current ordering and state holder:

1. `resolveCombat` configures the shared `$30` recoil and `$0A` countdown.
2. `tick` decrements and applies one recoil step before
   `WaterTektiteMotion.advance`.
3. The existing non-roaming collision behavior retains the countdown when a
   background callback blocks the step.

No Water Tektite motion changes are part of this increment. Its ground,
water, pit, conveyor, and remaining damage-state behavior remain separate
follow-up gaps.

## Tests

Use a level-zero attack context so the one-health Water Tektite stays active
long enough to observe the handler tick. Assert that a sword hit configures
`$D0/$D0` recoil, moves from `(64,64)` to `(61,61)` on the next frame, and
decrements the countdown from `$0A` to `$09`. Repeat with a background
callback blocking the left/up recoil directions and assert that the entity
stays at `(64,64)` while recoil remains active and the countdown still
decrements to `$09`.

## Scope boundary

This increment does not add generic background ground-status handling, water
or pit transitions, conveyor movement, or Water Tektite-specific damage
branches. It only closes the missing shared bank-$07 recoil admission.
