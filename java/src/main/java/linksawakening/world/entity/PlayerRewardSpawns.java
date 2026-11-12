package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.room.ChestContentsTable;

/** Source-ordered allocation and initialization for this spawn family. */
final class PlayerRewardSpawns {
    static int spawnRoomEventKeyDrop(EntityPlayerSpawnsServices runtime) {
        int itemType = runtime.roomVariables().readEntityMapId() >= 0x0A
                        && runtime.roomVariables().readEntityMapId() != 0xFF
                ? EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                : EntityRomConstants.ENTITY_KEY_DROP_POINT;
        return runtime.pickups().spawnKeyDropAt(itemType, 0x28, 0x3C, 0, 0x70, 0, 0);
    }

    static void spawnMusicalNote(
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        int phase = animationPhase & 0x01;
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MUSICAL_NOTE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        int x = linkEntityX + (phase == 0 ? 0x08 : -0x08);
        RoomEntity note = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_MUSICAL_NOTE,
                x & 0xFF,
                (linkEntityY - 0x08) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = note;
        runtime.initializeHealthGroup(freeSlot);
        runtime.projectiles().writeMusicalNoteInertia(freeSlot, EntityRomConstants.MUSICAL_NOTE_INITIAL_INERTIA);
        runtime.projectiles().writeMusicalNoteSpeedX(freeSlot, phase == 0 ? 0x06 : 0x01);
        runtime.projectiles().writeMusicalNoteSpeedY(freeSlot, EntityRomConstants.MUSICAL_NOTE_SPEED_Y);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static int spawnShovelDrop(
            EntityPlayerSpawnsServices runtime,
            int objectLeft,
            int objectTop,
            int linkEntityX,
            int linkEntityY,
            boolean allowDrop) {
        if (runtime.defaultRandomByteSupplier() == null) {
            throw new IllegalStateException("Shovel drops require a ROM random-byte supplier");
        }
        if ((runtime.defaultRandomByteSupplier().getAsInt() & 0x07) != 0 || !allowDrop) {
            return -1;
        }

        int itemType = (runtime.defaultRandomByteSupplier().getAsInt() & 0x01) == 0 ? 0x2E : 0x2D;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity drop = new RoomEntity(
                freeSlot,
                -1,
                itemType,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        runtime.pickups().resetEnemyDropState(freeSlot);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0x80);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, true);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(itemType));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.bossRewards().writeDropPrivateCountdown1(freeSlot, 0x18);
        runtime.bossRewards().writeDropPrivateCountdown3(freeSlot, 0x03);
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling(), 0x20);
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initializeAwayFromLink(freeSlot, x & 0xFF, y & 0xFF, linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        runtime.bossRewards().writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int spawnChestWithItem(EntityPlayerSpawnsServices runtime, int objectLeft, int objectTop, int itemType) {
        if (runtime.chestContentsTable() == null) {
            throw new IllegalStateException("ROM chest contents table is not configured");
        }
        EntitySlotValidation.validateByte(objectLeft, "Chest object left");
        EntitySlotValidation.validateByte(objectTop, "Chest object top");
        if (itemType < 0 || itemType > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item variant out of range: " + itemType);
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionForChest(itemType);
        int spriteVariant = definition.supported() ? itemType : -1;
        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        RoomEntity chest = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.INIT,
                definition,
                spriteVariant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = chest;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.CHEST_INITIAL_PHYSICS_FLAGS);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.pickups().writeChestSpeedY(freeSlot, 0);
        runtime.pickups().writeChestSpeedYAccumulator(freeSlot, 0);
        runtime.pickups().writeChestInertia(freeSlot, 0);
        runtime.pickups().writeChestItemBySlot(freeSlot, itemType);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, false);
        return freeSlot;
    }
}
