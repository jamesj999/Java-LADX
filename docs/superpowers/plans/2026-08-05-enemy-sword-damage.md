# ROM-Backed Enemy Sword Damage Implementation Plan

> Execute each implementation task test-first: add the failing test, run the
> focused test and record the RED result, implement the smallest
> ROM-faithful change, rerun for GREEN, then run the relevant regressions.

**Goal:** Replace the active room runtime's one-point sword shortcut with the
bank-$03 health-group and damage tables, while preserving the existing recoil,
sound, and Link-contact boundaries.

**Design:** `RomEnemyCombatTables` decodes the five source tables from the
shipped ROM. `EnemyAttackContext` mirrors the ROM's sword-level modifiers.
`RoomSession` supplies the decoded tables to each runtime, and `Main` supplies
the current player attack state. No Java copy of the ROM tables is used by the
active engine path.

**Required references:**

- `docs/superpowers/specs/2026-08-05-enemy-sword-damage-design.md`
- `LADX-Disassembly/src/data/entities/health_groups.asm`
- `LADX-Disassembly/src/data/entities/health.asm`
- `LADX-Disassembly/src/data/entities/damages.asm`
- `LADX-Disassembly/src/code/entities/bank3.asm`

## Task 1: Add ROM combat table decoding and pure attack resolution

**Files:**

- Create: `java/src/main/java/linksawakening/world/RomEnemyCombatTables.java`
- Create: `java/src/main/java/linksawakening/world/EnemyAttackContext.java`
- Create: `java/src/test/java/linksawakening/world/RomEnemyCombatTablesTest.java`

1. Add failing tests that load `rom/azle.gbc` and assert representative bytes
   at bank `$03:$41F6`, `$43EC`, `$473C`, `$47BC`, and `$47F1` through the
   public ROM table API.
2. Add failing tests for Octorok (`$09`), Moblin (`$0B`), and a resistant
   group. Assert health group, initial health, and contact damage come from
   the ROM bytes.
3. Add failing attack-context tests for sword levels 1, 2, and 3, then enable
   each red-tunic, Piece-of-Power, spin, and Pegasus-Boots modifier. Assert
   the effective attack type and raw table result, including zero and special
   values.
4. Run:

   ```bash
   gradle test --tests linksawakening.world.RomEnemyCombatTablesTest
   ```

   Confirm RED because the decoder and context do not exist.
5. Implement checked banked ROM reads, immutable table copies, exact unsigned
   lookup math, and an explicit result type that distinguishes ignored,
   numeric, and special raw values.
6. Rerun the focused test for GREEN and commit:

   ```bash
   git add java/src/main/java/linksawakening/world/RomEnemyCombatTables.java \
     java/src/main/java/linksawakening/world/EnemyAttackContext.java \
     java/src/test/java/linksawakening/world/RomEnemyCombatTablesTest.java
   git commit -m "feat: decode ROM enemy damage tables"
   ```

## Task 2: Thread ROM tables and attack state into the room runtime

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`
- Modify: `java/src/main/java/linksawakening/state/PlayerState.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/test/java/linksawakening/state/PlayerStateTest.java`

1. Add failing runtime tests using the shipped ROM tables. Assert Octorok
   initial health/contact damage, two-health Moblin progression, and a
   sword-level upgrade that does more than one point.
2. Add failing tests proving an ignored ROM result leaves health and flash
   unchanged but keeps the default recoil/bump path, while a numeric result
   emits enemy-hit feedback and enters the existing flash/death path.
3. Add failing player-state tests for green/red/blue tunic values and the
   explicit Pegasus-Boots-running state. Keep green and false as defaults.
4. Run the focused tests and confirm RED.
5. Add the ROM-table field and factory overload to `RoomEntityRuntime`; keep
   existing no-ROM factories as test-only compatibility paths. Initialize
   health and contact damage from the ROM table when supplied.
6. Construct one `RomEnemyCombatTables` in `RoomSession` and pass it on both
   initial room creation and follower synchronization/reload paths.
7. Thread `EnemyAttackContext` through `RoomSession.resolveEntityCombat` and
   preserve a standard-context overload for existing tests.
8. Rerun focused runtime/state tests for GREEN and commit:

   ```bash
   git add java/src/main/java/linksawakening/world \
     java/src/main/java/linksawakening/state/PlayerState.java \
     java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
     java/src/test/java/linksawakening/state/PlayerStateTest.java
   git commit -m "feat: thread ROM enemy combat state"
   ```

## Task 3: Apply the ROM sword result and expose combat outcome data

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/EntityCombatEvent.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

1. Add failing tests for the exact standard sword ordering: configure recoil
   and bump first, resolve the result, subtract only numeric damage, retain
   `$0A`/`$20` ignore timing as appropriate, and preserve lethal recoil state.
2. Add failing tests that assert `EntityCombatEvent` exposes numeric enemy
   damage and raw special-action data without breaking its old constructors.
3. Run the focused runtime tests and confirm RED.
4. Replace `basicSwordDamage` in the ROM-backed runtime path with
   `RomEnemyCombatTables.resolveSwordDamage`. Keep the no-ROM compatibility
   path for old isolated tests.
5. Pass `PlayerState.swordLevel()`, tunic state, Piece-of-Power state,
   `Sword.spinAttackActive()`, and the explicit Pegasus-Boots-running state
   from `Main`. The default/active engine state must remain green, non-powered,
   and non-boots-running unless gameplay has set otherwise.
6. Apply special raw codes only through explicit branches; do not subtract
   `$F0-$FF` as ordinary health. Unknown special codes remain data and do not
   produce fabricated gameplay effects.
7. Rerun focused runtime tests for GREEN and commit:

   ```bash
   git add java/src/main/java/linksawakening/world \
     java/src/main/java/linksawakening/Main.java \
     java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
   git commit -m "feat: apply ROM enemy sword damage"
   ```

## Task 4: Regression, roadmap, and evidence checkpoint

1. Run focused tests:

   ```bash
   gradle test --tests linksawakening.world.RomEnemyCombatTablesTest \
     --tests linksawakening.world.RoomEntityRuntimeTest \
     --tests linksawakening.state.PlayerStateTest
   ```

2. Run the clean full suite:

   ```bash
   gradle clean test
   ```

3. Run `git diff --check` and inspect the changed-file list. Update
   `docs/reconstruction-roadmap.md` with exact ROM anchors, the runtime
   behavior now verified, and the remaining non-goals (other weapons,
   entity-specific branches, and complete equipment acquisition).
4. Request independent review before claiming this increment complete. Do not
   claim visual/gameplay parity beyond the tested combat paths.

