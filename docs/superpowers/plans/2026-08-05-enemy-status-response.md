# ROM-Backed Enemy Status Response Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the shared ROM `$FE` burning and `$FF` stunned outcomes in the active room entity runtime, with exact countdowns, collision suppression, Color Shell coverage, and ROM sound routing.

**Architecture:** `RoomEntityRuntime` owns per-slot status countdowns and performs the common status transitions before family-specific active motion. `EntityCombatEvent` carries the existing primary sound plus one optional secondary ROM sound write so a burning hit preserves both registers; `EnemyCombatEventConsumer` maps those writes at the gameplay boundary. The three Color Shell types are admitted to the existing collision gate so the shipped ROM's `$FF` result is reachable, while their bank-$36 movement handler remains a separate increment.

**Tech Stack:** Java 21, Gradle, JUnit 5, LWJGL runtime, shipped `azle.gbc` ROM, and the LADX disassembly as the behavioral source of truth.

---

**Required references:**

- `docs/superpowers/specs/2026-08-05-enemy-status-response-design.md`
- `LADX-Disassembly/src/code/entities/bank3.asm:6111-6315`
- `LADX-Disassembly/src/code/bank14.asm:699-750`
- `LADX-Disassembly/src/code/entities/bank36.asm:6130-6150`
- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

### Task 1: Preserve multiple ROM sound writes in combat events

**Files:**

- Modify: `java/src/main/java/linksawakening/world/EntityCombatEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/EnemyCombatEventConsumer.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java`
- Test: `java/src/test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java`
- Test: `java/src/test/java/linksawakening/gameplay/SfxGameplaySoundSinkTest.java`

- [ ] **Step 1: Write the failing tests.** Add a consumer test that constructs an event with primary `JINGLE/$03` and secondary `NOISE/$12`, then expects `[ENEMY_HIT, ENEMY_BURNING]`. Add a sound-map assertion for `ENEMY_BURNING` resolving to `SoundEffectNamespace.NOISE`, ID `$12`, named `NOISE_SFX_BURSTING_FLAME`.

```java
new EntityCombatEvent(0, 0x09, 0, true, 0, 0xFE,
    EntityCombatEvent.SoundChannel.JINGLE, 0x03,
    EntityCombatEvent.SoundChannel.NOISE, 0x12)
```

- [ ] **Step 2: Run the focused tests to verify RED.**

```bash
gradle test --tests linksawakening.gameplay.EnemyCombatEventConsumerTest \
  --tests linksawakening.gameplay.SfxGameplaySoundSinkTest
```

Expected: compilation/test failure because the event has no secondary sound fields and `ENEMY_BURNING` is not defined.

- [ ] **Step 3: Add the optional secondary sound fields.** Extend the record with `secondarySoundChannel` and `secondarySoundId`. Keep the four-argument and existing six-argument constructors by defaulting the secondary pair to `NONE/-1`. Validate the secondary pair using the same invariants as the primary pair.

```java
public record EntityCombatEvent(
        int slot, int type, int linkDamage, boolean swordHit,
        int enemyDamage, int enemySpecialAction,
        SoundChannel soundChannel, int soundId,
        SoundChannel secondarySoundChannel, int secondarySoundId) {
    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit) {
        this(slot, type, linkDamage, swordHit, 0, -1,
            SoundChannel.NONE, -1, SoundChannel.NONE, -1);
    }

    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             SoundChannel soundChannel, int soundId) {
        this(slot, type, linkDamage, swordHit, 0, -1,
            soundChannel, soundId, SoundChannel.NONE, -1);
    }
}
```

Use a private validation helper for both channel/id pairs so `NONE` requires `-1`, non-`NONE` requires an unsigned ID, and null channels are rejected.

- [ ] **Step 4: Route both sound writes and map bursting flame.** In `EnemyCombatEventConsumer.consume`, pass each event through the existing switch once for the primary pair and once for the secondary pair. Add `ENEMY_BURNING` to `GameplaySoundEvent` and map it to noise ID `$12` in `GameplaySoundEffectMap.fromCatalog`.

- [ ] **Step 5: Run the focused tests to verify GREEN.** Run the Gradle command from Step 2. Expected: all tests in both classes pass, including the old constructor and unknown-channel tests.

- [ ] **Step 6: Commit the event/audio boundary.**

```bash
git add java/src/main/java/linksawakening/world/EntityCombatEvent.java \
  java/src/main/java/linksawakening/gameplay/EnemyCombatEventConsumer.java \
  java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java \
  java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java \
  java/src/test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java \
  java/src/test/java/linksawakening/gameplay/SfxGameplaySoundSinkTest.java
git commit -m "feat: route ROM enemy status sounds"
```

### Task 2: Admit Color Shells to the ROM collision gate

**Files:**

- Modify: java/src/main/java/linksawakening/world/RoomEntityCombatRules.java
- Test: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java

- [ ] **Step 1: Write the failing gate test.** Add a test asserting that entity types $E9, $EA, and $EB are accepted by supportsEnemyCollision. In the same test class, use RomEnemyCombatTables to assert their health values are read from the shipped ROM; do not add a Java health table.

~~~java
assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xE9));
assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xEA));
assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xEB));
~~~

- [ ] **Step 2: Run the focused test to verify RED.**

~~~bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
~~~

Expected: failure because the three Color Shell types are rejected before the ROM combat tables are consulted.

- [ ] **Step 3: Add only the shared collision constants.** Add ENTITY_COLOR_SHELL_RED = 0xE9, ENTITY_COLOR_SHELL_GREEN = 0xEA, and ENTITY_COLOR_SHELL_BLUE = 0xEB to the collision predicate's switch. Do not add guessed health, contact damage, movement, or state tables; the ROM health-group decoder already supplies those values.

- [ ] **Step 4: Run the focused test to verify GREEN.** Run the command from Step 2. Expected: the gate and existing runtime tests pass.

- [ ] **Step 5: Commit the collision-gate change.**

~~~bash
git add java/src/main/java/linksawakening/world/RoomEntityCombatRules.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: include Color Shell combat targets"
~~~

### Task 3: Port common burning, stunned, and status countdown transitions

**Files:**

- Modify: java/src/main/java/linksawakening/world/RoomEntityRuntime.java
- Test: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java

- [ ] **Step 1: Add failing ROM-backed status tests.** Load the shipped ROM and make a copy-on-write fixture helper in RoomEntityRuntimeTest. Mutate only the test copy at RomBank.romOffset(3, 0x473C) + 1, attack type 0's entry 1 for the Octorok health group, to inject $FE. Add tests for:

  - Octorok $FE: the event reports special $FE, health is unchanged, status is BURNING, countdown is $60, ignore is $0A, primary sound is jingle $03, and secondary sound is noise $12; a second combat pass is empty; 96 ticks end in DYING with countdown $1F.
  - Color Shell Red $E9 from the unmodified ROM: the event reports special $FF, status is STUNNED, countdown is $FF, health is unchanged, only jingle $03 is emitted, a second combat pass is empty, and 255 ticks return it to ACTIVE.
  - Burning Gibdo $1F using the same $FE fixture: after 96 ticks the slot is type $1E, status ACTIVE, and has no burning countdown.
  - Clearing a burning or stunned slot zeros both new countdowns and prevents state transfer to a replacement entity.

Use RoomEntityRuntime.from(snapshot, false, null, null, tables) with an entity at (64,64) and sword rectangle (72,1,72,1) with Link at (120,120) so the existing sword-overlap predicate is true. Assert raw event fields rather than inferring state from sound IDs.

- [ ] **Step 2: Run the focused tests to verify RED.**

~~~bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
~~~

Expected: failures show that $FE/$FF are currently event data only, BURNING/STUNNED are not dispatched by tickInternal, and Color Shells were not collision candidates.

- [ ] **Step 3: Add per-slot status countdowns.** In RoomEntityRuntime, add:

~~~java
private final int[] transitionCountdown =
    new int[EntityRoomLoader.MAX_ENTITIES];
private final int[] stunnedCountdown =
    new int[EntityRoomLoader.MAX_ENTITIES];
~~~

Add package-private validated accessors transitionCountdown(int slot) and stunnedCountdown(int slot). At the start of each loaded entity iteration, decrement transitionCountdown, stunnedCountdown, and dyingCountdown only when positive, matching bank-$14 UpdateEntityTimers. Move the existing DYING decrement into this one timer step so it is not decremented twice.

- [ ] **Step 4: Dispatch the status lifecycle before active motion.** Replace the current DYING-only early branch with these exact transitions:

~~~java
if (status == EntityStatus.DYING) {
    if (dyingCountdown[slot] == 0) {
        disableEntityWithoutPersistence(slot);
    }
    continue;
}
if (status == EntityStatus.BURNING) {
    if (transitionCountdown[slot] != 0) {
        continue;
    }
    if (entity.type() == ENTITY_GIBDO) {
        RoomEntity converted = withType(entity, 0x1E, spriteDefinitionFor(0x1E));
        slots[index] = withStatus(converted, EntityStatus.ACTIVE);
        continue;
    }
    dyingCountdown[slot] = 0x1F;
    slots[index] = withStatus(entity, EntityStatus.DYING);
    continue;
}
if (status == EntityStatus.STUNNED) {
    if (stunnedCountdown[slot] != 0) {
        continue;
    }
    slots[index] = withStatus(entity, EntityStatus.ACTIVE);
    continue;
}
~~~

The withType helper preserves slot, load order, coordinates, attributes, tile offset, and Z while replacing type and sprite definition. Do not call active family motion for burning or stunned entities in this increment.

- [ ] **Step 5: Apply raw status results in resolveCombat.** Initialize the secondary sound pair to NONE/-1 for every event and branch before numeric subtraction:

~~~java
if (swordResult != null && swordResult.rawValue() == 0xFE) {
    transitionCountdown[slot] = 0x60;
    enemyIgnoreHitsCountdown[slot] = 0x0A;
    enemyRecoilMotion.clear(slot);
    slots[slot] = withStatus(entity, EntityStatus.BURNING);
    secondarySoundChannel = EntityCombatEvent.SoundChannel.NOISE;
    secondarySoundId = 0x12;
} else if (swordResult != null && swordResult.rawValue() == 0xFF) {
    stunnedCountdown[slot] = 0xFF;
    enemyIgnoreHitsCountdown[slot] = 0x0A;
    enemyRecoilMotion.clear(slot);
    slots[slot] = withStatus(entity, EntityStatus.STUNNED);
} else if (swordDamage > 0) {
    // Keep the existing numeric health, flash, ignore, and death branch.
}
~~~

Leave $FD and other $F0..$FC values as raw event data with no state mutation. Use the existing jingle decision ($03 for every nonzero result and $09 for ignored zero) so $FE/$FF retain the ROM's pre-branch enemy-hit jingle.

- [ ] **Step 6: Reset and reinitialize new state.** Clear both arrays in clearEntity, set them to zero in applyZolSplit, spawnPairoddProjectile, and spawnEnemyProjectile, and initialize them to zero for loaded room snapshots. Keep disabled-slot health at zero.

- [ ] **Step 7: Run focused tests to verify GREEN.**

~~~bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.gameplay.EnemyCombatEventConsumerTest \
  --tests linksawakening.gameplay.SfxGameplaySoundSinkTest
~~~

Expected: all status, Color Shell, event, and prior numeric combat tests pass.

- [ ] **Step 8: Commit the runtime status implementation.**

~~~bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/RoomEntityCombatRules.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: apply ROM enemy status responses"
~~~

### Task 4: Regression checkpoint and roadmap evidence

**Files:**

- Modify: docs/reconstruction-roadmap.md

- [ ] **Step 1: Run the complete clean suite.**

~~~bash
gradle clean test
~~~

Expected: BUILD SUCCESSFUL with zero failing tests.

- [ ] **Step 2: Run the source and diff checks.**

~~~bash
git diff --check
git status --short --branch
~~~

Expected: no whitespace errors; only the intended implementation and roadmap files are changed before the final commit.

- [ ] **Step 3: Record the verified increment.** Add a dated roadmap entry naming bank-$03 $7235-$7278, bank-$03 $4C4C-$4CA3, bank-$03 $4E07-$4E9D, and bank-$14 $4D73-$4DDC. State that $FE/$FF status transitions, Color Shell $E9-$EB collision admission, countdown reset, and primary/secondary sound routing are covered by tests. Explicitly list deferred burn fire sprites, burn-expiry noise $13, poof VFX, lifting/thrown physics, and other weapon collision paths.

- [ ] **Step 4: Run the final suite after the roadmap edit.**

~~~bash
gradle clean test && git diff --check
~~~

Expected: the command exits 0 and reports BUILD SUCCESSFUL.

- [ ] **Step 5: Commit the checkpoint.**

~~~bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record enemy status response"
~~~
