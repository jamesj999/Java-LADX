# ROM-Backed Enemy Sword Damage Design

## Goal

Replace the Java entity runtime's one-point sword shortcut with the shared
bank-$03 damage lookup used by `ApplySwordDamagesToEnemy`. The active engine
must resolve sword damage from the shipped ROM, honor the attack-state inputs
that change the effective sword level, and use the same ROM tables for enemy
initial health and contact damage.

This is a bounded combat increment. It does not attempt to port every
entity-specific collision branch, projectile weapon, boss rule, or inventory
equipment acquisition path.

## ROM source of truth

- `LADX-Disassembly/src/data/entities/health_groups.asm:3`
  `HealthGroupForEntity` maps each entity type to a health group. The table is
  in bank `$03` at CPU address `$41F6` and contains `$FB` bytes.
- `LADX-Disassembly/src/code/entities/bank3.asm:23`
  `Data_003_43EC` maps each health group and attack type to a damage-table
  entry. It is bank `$03:$43EC`, with 53 groups of 16 bytes.
- `LADX-Disassembly/src/code/entities/bank3.asm:82`
  `Data_003_473C` maps `(attack type * 8) + entry` to an amount or special
  action. It is bank `$03:$473C`.
- `LADX-Disassembly/src/data/entities/health.asm:3`
  `InitialHealthForGroup` is bank `$03:$47BC`, 53 bytes.
- `LADX-Disassembly/src/data/entities/damages.asm:3`
  `EntityDamagesForGroup` is bank `$03:$47F1`, 53 bytes.
- `LADX-Disassembly/src/code/entities/bank3.asm:6111`
  `ApplySwordDamagesToEnemy` starts with `wSwordLevel`, increments it when
  the red tunic, Piece of Power, spin attack, or Pegasus Boots is active, then
  decrements it to form `wAttackDamageType` before the two table lookups.
- `LADX-Disassembly/src/code/entities/bank3.asm:7162`
  `EnemyCollidedWithSword` applies default recoil and bump feedback before
  the damage routine. The damage routine overwrites the jingle with enemy-hit
  feedback only when the resolved table value is nonzero.

All bank addresses use `RomBank.romOffset(bank, address)`, so a banked address
is translated as `bank * $4000 + (address - $4000)`.

## Design

### ROM table object

Add `RomEnemyCombatTables`, an immutable decoder loaded from the ROM passed to
the room session. It owns the five source tables and exposes:

- `healthGroup(entityType)`;
- `initialHealth(entityType)`;
- `contactDamage(entityType)`;
- `resolveSwordDamage(entityType, attackContext)`.

The resolver returns the effective attack type, intermediate damage-table
entry, raw ROM result, numeric damage (when applicable), and special-action
code (when the raw result is at least `$F0`). A raw zero is an ignored hit.
The decoder validates every ROM range and does not embed a Java copy of the
source data.

`RoomEntityRuntime.from(...)` overloads that do not receive ROM tables remain
available for isolated motion tests. They use the existing compatibility
rules only in those no-ROM test fixtures. `RoomSession`, which is the active
engine path, always constructs and supplies `RomEnemyCombatTables` from its
ROM.

### Attack context

Add an immutable `EnemyAttackContext` carrying:

- ROM `wSwordLevel`;
- red-tunic state;
- Piece-of-Power state;
- active spin-attack state;
- active Pegasus-Boots charge/run state.

The context computes the same effective sword damage type as the ROM. A
swordless state is represented as an ignored sword result because the active
engine has no blade collision to apply in that state. The existing player
state gains a tunic value with the ROM's green/red/blue encodings; the current
default remains green. Pegasus Boots are supplied from the active gameplay
equipment state, and the current runtime will expose the running/charged
boolean at the combat boundary without inventing a new movement mechanic.

### Runtime integration

Thread the attack context from `Main` through `RoomSession` to
`RoomEntityRuntime.resolveCombat`. Preserve the existing overloads for unit
tests by defaulting them to a standard level-one green-tunic non-powered
attack.

At a sword collision, the runtime keeps the ROM ordering:

1. Configure ordinary enemy recoil and bump feedback.
2. Resolve the ROM sword result using the entity's health group.
3. If the result is zero, leave health and flash state unchanged and retain
   the bump sound.
4. If the result is numeric, subtract that amount, emit enemy-hit feedback,
   and enter the existing flash/ignore/death transitions.
5. Preserve raw special-action values in the result path. This increment only
   applies numeric sword results to health; burn, stun, and fairy conversions
   remain explicit follow-up behavior rather than being silently treated as
   large damage values.

Red tunic and Piece of Power also select the ROM's stronger power-recoil
  countdown (`$20`) and wave feedback where the current runtime already has a
  corresponding event boundary. Spin and Pegasus Boots affect the damage
  lookup as they do in the ROM but do not gain unrelated movement behavior in
  this slice.

Contact damage and initial health in the active room runtime come from the
same decoded health-group tables. The existing Java rules remain only as the
no-ROM compatibility path for focused tests that do not construct a ROM.

### Events and compatibility

Extend `EntityCombatEvent` with the resolved numeric enemy damage and raw
special-action code, retaining its existing constructors and accessors for
callers that only care about Link damage and sword-hit feedback. Unknown raw
special codes do not reach the audio sink. Existing Link contact damage and
room persistence behavior remain unchanged.

## Verification

Tests will cover:

1. ROM bank offsets and representative bytes for all five combat tables.
2. Octorok/Moblin health and contact values loaded through their ROM health
   groups rather than Java constants.
3. Sword level 1/2/3 and every attack modifier selecting the exact ROM damage
   type, including ignored and special raw results.
4. Runtime sword collisions using the ROM result: one-point Octorok damage,
   two-health Moblin progression, stronger sword/power damage, zero-result
   bump feedback, and lethal transitions.
5. Existing recoil, projectile, pickup, and entity-motion regressions.
6. `gradle clean test`, `git diff --check`, and a final source/diff audit.

