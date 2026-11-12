package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;

/** Source-ordered allocation and initialization for this spawn family. */
final class HookshotSpawns {
    static void spawnIronMasksMask(
            EntityPlayerSpawnsServices runtime, int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_IRON_MASKS_MASK);
        int selectedVariant = definition.supported() ? Math.min(variant & 0x01, definition.variantCount() - 1) : -1;
        RoomEntity mask = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_IRON_MASKS_MASK,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                selectedVariant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = mask;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_IRON_MASKS_MASK));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.IRON_MASKS_MASK_OPTIONS1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        if (runtime.indoorRoom()) {
            runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0x80);
            runtime.physicsTables().writeSlowTimerInitialized(freeSlot, true);
        }
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(freeSlot, (sourceHookshotSlot + 1) & 0xFF);
    }

    static int spawnHookshotChain(
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (runtime.inspection().hookshotActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_CHAIN);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity chain = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_HOOKSHOT_CHAIN,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots()[freeSlot] = chain;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .initializeSpawn(freeSlot, linkEntityX & 0xFF, linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        2
                                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        return freeSlot;
    }

    static boolean spawnHookshotBridge(
            EntityPlayerSpawnsServices runtime, int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        if (!runtime.indoorRoom()) {
            return false;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bridge = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE,
                bridgeX & 0xFF,
                bridgeY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = bridge;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, transitionCountdown & 0xFF);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }
}
