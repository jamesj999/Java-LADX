# Armos Statue activation and active combat design

## Scope

Complete the missing activation boundary for the already-portable Armos
Statue state machine. This slice carries the handler's initial harmless and
sword-ignore flags, wake flash, active-state flag clearing, active combat
admission, ROM fallback combat values, and bank-$06 recoil. It does not add
background collision, final-Link-position plumbing, or Armos-specific death
presentation.

## ROM behavior

`ArmosStatueEntityHandler` in
`LADX-Disassembly/src/code/entities/06_armos_statue.asm` performs the following
source-ordered work:

- It calls the sword collision helper and then
  `CheckLinkCollisionWithEnemy.collisionEvenInTheAir` before movement and the
  state handler.
- State 0 treats Link collision as a wake request, changes to state 1,
  loads transition countdown `$30`, and loads flash countdown `$18`.
- State 1 alternates its signed X speed while the countdown is nonzero. When
  it expires, it enters state 2, clears the physics harmless bit, clears the
  hitbox ignore-hits bit, clears the sword-clink-off option, and clears speed.
- State 2 selects a random `$20-$5F` movement countdown and the contiguous
  `Data_006_74C0/74C2` speed bytes.

The entity tables define Armos type `$0F` with initial physics `$92`
(`2 | shadow | harmless`), normal enemy hitbox plus ignore-hits, option
`SWORD_CLINK_OFF`, health group `$08`, initial health `$04`, and contact
damage `$10`.

## Java design

`ArmosMotion.advance` returns an `Update` record containing the updated entity
and transition booleans for `woke` and `activated`. `RoomEntityRuntime` owns
the runtime flag projection:

- initialize Armos physics to `$92`;
- on `woke`, set the existing enemy flash countdown to `$18`;
- on `activated`, clear the harmless bit, clear the runtime sword-ignore
  gate, and expose active state through `ArmosMotion.isActive`;
- admit Armos in `resolveCombat` only when `isActive` is true, so state 0/1
  remains harmless and sword-ignored without changing every entity snapshot;
- include Armos in bank-$06 recoil and provide fallback combat values for
  ROM-less test fixtures. ROM-backed sessions continue to read group `$08`
  from `RomEnemyCombatTables`.

The existing `enemyFlashCountdown`, `enemyPhysicsFlags`, enemy health, and
combat-event boundaries remain the shared state paths. Background collision is
not silently inferred from the source call; it remains a later increment.

## Testing and boundaries

`RoomEntityRuntimeTest` will prove initial `$92` flags and no combat before
activation, the `$18` wake flash, state-2 flags `$12`, active Armos contact
damage `$10`, active sword-hit admission, health reduction, and recoil setup.
Existing movement/countdown tests remain unchanged and continue to verify the
contiguous source speed tables.

No emulator, renderer, ROM loader, or unrelated entity-table refactor is part
of this design.
