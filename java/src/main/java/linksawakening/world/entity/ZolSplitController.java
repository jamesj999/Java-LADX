package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ZolGelMotion;

/** Source-ordered Zol replacement and second Gel allocation. */
final class ZolSplitController {
    static RoomEntity applyZolSplit(
            EntitySlotLifecycleServices runtime, RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_GEL);
        int gelVariant = gelDefinition.supported() ? 0 : -1;
        int slot = original.slot();
        RoomEntity gel = new RoomEntity(
                slot,
                original.sourceLoadOrder(),
                EntityRomConstants.ENTITY_GEL,
                (split.originalX() - 4) & 0xFF,
                split.originalY(),
                EntityStatus.ACTIVE,
                gelDefinition,
                gelVariant,
                original.entityFlipAttribute(),
                original.spriteTileOffset(),
                split.originalZ(),
                -1,
                false);
        // ConfigureNewEntity ($03:485B) reloads these for the new type.
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables().writeEnemyHitboxFlags(slot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
        runtime.physicsTables().writeEntityOptions1Override(slot, -1);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables()
                .writeEnemyHealthGroup(
                        slot,
                        runtime.enemyCombatTables() == null
                                ? 0
                                : runtime.enemyCombatTables().healthGroup(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables().writeEnemyHealth(slot, runtime.initialHealth(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.combatTables().writeDyingCountdown(slot, 0);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);

        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot >= 0) {
            runtime.physicsTables()
                    .writeBaseEntityFlipAttribute(
                            freeSlot, runtime.physicsTables().readBaseEntityFlipAttribute(original.slot()));
            RoomEntity spawnedGel = new RoomEntity(
                    freeSlot,
                    original.sourceLoadOrder(),
                    EntityRomConstants.ENTITY_GEL,
                    (split.originalX() + 8) & 0xFF,
                    split.originalY(),
                    EntityStatus.ACTIVE,
                    gelDefinition,
                    gelVariant,
                    original.entityFlipAttribute(),
                    original.spriteTileOffset(),
                    split.originalZ(),
                    -1,
                    false);
            runtime.slots()[freeSlot] = spawnedGel;
            runtime.initializeHealthGroup(freeSlot);
            runtime.physicsTables()
                    .writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
            runtime.combatTables().writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
            runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
            runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
            runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
            runtime.combatTables().writeDyingCountdown(freeSlot, 0);
            runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
            runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_GEL));
            runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(freeSlot);
            runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).prepareSpawnedGel(freeSlot);
        }
        return gel;
    }
}
