# ROM Hard Hat Beetle Recoil Design

## Context

The Java runtime already ports Hard Hat Beetle's ordinary bank-$06 movement,
health, and contact damage, but a sword hit does not yet push it away from
Link. The authoritative handler is
`LADX-Disassembly/src/code/entities/06_hard_hat_beetle.asm`:

- `HardHatBeetleEntityHandler` renders the pair and returns when the entity is
  non-interactive.
- It calls `ApplyRecoilIfNeeded_06` before `UpdateEntityPosWithSpeed_06`.
- The bank-$06 helper decrements the shared ignore-hits countdown, temporarily
  swaps in `wEntitiesRecoilVelocityX/Y`, advances fixed-point position, applies
  background interaction, then restores the ordinary speed.
- The normal handler continues with Hard Hat's own target-seeking speed update,
  so recoil must not replace or permanently clear its movement state.

`ApplyRecoilIfNeeded_06` uses the same recoil velocity and countdown tables as
the already modeled bank-$03 Octorok/Moblin path. The Java
`EnemyRecoilMotion` helper already mirrors the vector calculation, signed
sixteen-subpixel accumulation, wrapping, and blocking behavior.

## Design

Reuse `EnemyRecoilMotion` for `ENTITY_HARDHAT_BEETLE` (`$20`) in the runtime's
shared pre-handler phase. The runtime will:

1. Configure the same default `$30` recoil vector when a normal sword hit
   reaches Hard Hat's shared enemy damage path.
2. Apply one recoil step before `HardHatMotion.advance`, matching the source
   order of `ApplyRecoilIfNeeded_06` followed by `UpdateEntityPosWithSpeed_06`.
3. Leave Hard Hat's ordinary speed and four-frame target refresh intact, so the
   same frame can apply normal motion after the temporary recoil movement.
4. Decrement the existing ignore-hits countdown while recoil is active and
   clear recoil when the background blocks it, matching the shared helper's
   externally observable state.

The existing `RoamingEnemyMotion.beginRecoil` call remains restricted to
Octorok/Moblin. Hard Hat has no roaming state-1 recoil branch; it only needs the
shared velocity application before its own handler movement.

Spike-trap sword clinks remain an explicit exception: they use the source
clink-off path and must not configure recoil.

## Scope boundary

This increment covers only the currently modeled Hard Hat Beetle. It does not
claim recoil for every bank-$04/$06 enemy, shield/clink behavior, water
behavior, collision-table corner cases, recoil smoke VFX, or a generalized
entity-handler scheduler.

## Verification

- Assert a Hard Hat sword hit configures the source `$30` recoil vector and
  preserves normal damage/flash/ignore-hit state.
- Assert the next runtime tick applies recoil before ordinary Hard Hat motion
  and decrements the countdown.
- Assert a blocked recoil step clears the active recoil state.
- Assert Octorok/Moblin recoil and spike-trap clink behavior remain unchanged.
- Run focused tests, `git diff --check`, and a fresh `gradle clean test`.
