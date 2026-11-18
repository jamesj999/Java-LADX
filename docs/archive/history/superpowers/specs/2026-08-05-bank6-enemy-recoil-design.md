# ROM Bank-$06 Enemy Recoil Design

## Context

The runtime already ports the movement and ordinary sword-damage paths for
Keese (`$19`), Tektite (`$0D`), Anti-Fairy (`$15`), aggressive Stalfos
(`$1A`), and Hard Hat (`$20`). Their sword hits currently reduce health and
start the shared flash/ignore window, but only Hard Hat, Octorok, and Moblin
also move away from Link during that window.

The authoritative handlers all call `ApplyRecoilIfNeeded_06` before their
family-specific movement:

- `06_keese.asm:673C-6745` applies recoil before the default damage handler
  and the state dispatch.
- `06_tektite.asm:78CA-78D6` applies recoil before damage, X/Y movement, and
  Z-state handling.
- `06_anti_fairy.asm:7879-7885` applies recoil before damage and its bounce
  movement.
- `06_stalfos_aggressive.asm:4AB7-4AC0` applies recoil before damage and its
  pursuit/jump state machine.
- `06_hard_hat_beetle.asm:4F48-4F54` is the already verified instance.

The helper uses the same shared recoil velocity/countdown tables and fixed
point position update as the existing Java `EnemyRecoilMotion`, but bank `$06`
does not call `StopEntityRecoilOnCollision` after the temporary movement.

## Design

Add an explicit `usesBank6Recoil` predicate in `RoomEntityRuntime` for the
five normal bank-$06 families. Keep Octorok/Moblin on their bank-$03 path and
keep the existing spike-trap clink exception outside this predicate.

When a normal sword collision targets one of these types, configure the
existing `$30` recoil vector from the entity and Link coordinates. During the
active pre-handler phase, apply one fixed-point recoil step before the existing
family motion. The bank-$06 step passes through the existing background query
but does not clear recoil or force the countdown to zero when blocked. The
current `RoomEntityRuntime` already decrements the countdown in the shared
helper and clears the state when the countdown reaches zero.

No new movement algorithm or fabricated visual data is introduced. The
family motion classes remain responsible for their ordinary speed/state
updates after the recoil step.

## Explicit exclusions

Spark (`$16/$17`) calls the same helper but sets
`hActiveEntityNoBGCollision`; its collision semantics require a separate
no-background path. Zol/Gel (`$1B/$1C`) calls the helper after split handling,
and must not be moved into this generic pre-handler without modeling that
ordering. Bank-$04 and bank-$07 families, laser beams, shield/clink branches,
recoil smoke, and other damage states remain separate increments.

## Verification

- Assert all four newly added normal families configure the `$30` recoil
  vector, normal health reduction, flash, and `$0A` ignore countdown.
- Assert a Tektite tick applies the recoil before its ordinary motion and
  decrements the countdown.
- Assert Hard Hat, Octorok, Moblin, and spike-trap behavior remains intact.
- Run focused tests, `git diff --check`, and a fresh `gradle clean test`.
