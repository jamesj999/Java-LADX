package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityPickupController. */
final class EnemyDropSpawns {
    static void spawnEnemyDrop(EntityPickupController owner, RoomEntity source, int itemType) {
        var runtime = owner.runtime;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        if (itemType == EntityRomConstants.ENTITY_KEY_DROP_POINT
                && source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
            variant = 3;
        }
        RoomEntity drop = new RoomEntity(
                freeSlot,
                -1,
                itemType,
                source.x(),
                source.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        owner.resetEnemyDropState(freeSlot);
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
        if (itemType == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            runtime.motions().get(FairyMotion.class, FairyMotion::new).initialize(freeSlot);
        } else {
            runtime.motions()
                    .get(EnemyDropMotion.class, EnemyDropMotion::new)
                    .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling());
            runtime.bossRewards().writeEnemyDropActive(freeSlot, true);
        }
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static int spawnKeyDropAt(
            EntityPickupController owner,
            int itemType,
            int x,
            int y,
            int spriteVariant,
            int z,
            int speedZ,
            int privateCountdown1) {
        var runtime = owner.runtime;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(itemType);
        int variant = definition.supported() ? Math.min(spriteVariant, definition.variantCount() - 1) : -1;
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
                z,
                -1,
                false);
        runtime.slots()[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        owner.resetEnemyDropState(freeSlot);
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling(), speedZ);
        runtime.bossRewards().writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, false);
        runtime.bossRewards().writeDropPrivateCountdown1(freeSlot, privateCountdown1);
        runtime.bossRewards().writeDropPrivateCountdown3(freeSlot, 0);
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
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }
}
