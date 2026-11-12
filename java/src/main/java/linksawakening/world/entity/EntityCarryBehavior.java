package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.LiftedEntityFrame;
import linksawakening.world.entity.motion.LiftedEntityMotion;

/** Focused behavior using the state owned by EntityLiftThrowController. */
final class EntityCarryBehavior {
    static RoomEntity advanceLiftedEntity(
            EntityLiftThrowController owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int romLinkDirection) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (!owner.readLiftedStateInitialized(slot)) {
            owner.writeLiftedPhase(slot, 0);
            owner.writeLiftedSourceDirection(slot, romLinkDirection);
            owner.writeLiftedStateInitialized(slot, true);
            owner.writeLiftedEffectiveDirection(romLinkDirection);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x02);
        }

        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(new LiftedEntityFrame(
                owner.readLiftedPhase(slot),
                runtime.physicsTables().readEnemyTransitionCountdown(slot),
                owner.readLiftedSourceDirection(slot),
                romLinkDirection,
                byteValue(linkEntityX),
                byteValue(linkEntityY),
                byteValue(linkZ),
                owner.readLiftedLinkC13B(),
                entity.type(),
                runtime.physicsTables().readGroundInteractionSideScrolling(),
                runtime.pickups().readChestPowerBraceletLevel() >= 2 || runtime.linkState().fastLiftTransition,
                entity.z()));
        owner.writeLiftedPhase(slot, update.phase());
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, update.transitionCountdown());
        owner.writeLiftedCarryState(update.carryState());
        owner.writeLiftedEffectiveDirection(update.effectiveDirection());
        return new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                update.x(),
                update.y(),
                EntityStatus.LIFTED,
                entity.spriteDefinition(),
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                update.z(),
                -1,
                false);
    }

    static boolean beginBushCrawlerLift(
            EntityLiftThrowController owner, int index, RoomEntity source, int privateState4, int romLinkDirection) {
        var runtime = owner.runtime;
        if (source.type() != EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            return false;
        }
        runtime.slots()[index] = source;
        if (!runtime.beginLift(source.slot(), romLinkDirection)) {
            return false;
        }

        int slot = source.slot();
        RoomEntity carried = runtime.slots()[slot];
        EntitySpriteDefinition rockDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        carried = withType(carried, EntityRomConstants.ENTITY_LIFTABLE_ROCK, rockDefinition);
        carried = withVariant(carried, rockDefinition.supported() ? (privateState4 ^ 0x01) & 0x01 : -1);
        runtime.slots()[slot] = carried;
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_BUSH_CRAWLER,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        0x02));

        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return true;
        }
        EntitySpriteDefinition crawlerDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        int crawlerVariant = crawlerDefinition.supported() ? crawlerDefinition.initialVariant() : -1;
        RoomEntity replacement = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BUSH_CRAWLER,
                carried.x(),
                carried.y(),
                EntityStatus.ACTIVE,
                crawlerDefinition,
                crawlerVariant,
                0,
                0,
                carried.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = replacement;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0x40);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables()
                .writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeFallingVisualYOffset(freeSlot, 0);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).initializeSpecial(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    static boolean tryLiftBombIfRequested(
            EntityLiftThrowController owner,
            int index,
            BombMotion.Decision decision,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int romLinkDirection) {
        var runtime = owner.runtime;
        if (decision.phase() != BombMotion.Phase.NORMAL || !runtime.linkState().bombButtonHeld) {
            return false;
        }
        RoomEntity entity = runtime.slots()[index];
        if (!entity.loaded()
                || entity.type() != EntityRomConstants.ENTITY_BOMB
                || runtime.bombInteractions().readBombPrivateState4(entity.slot()) != 0
                || runtime.bombInteractions().readBombPrivateCountdown1(entity.slot()) != 0
                || !RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }
        if (!runtime.beginLift(entity.slot(), romLinkDirection)) {
            return false;
        }
        runtime.slots()[index] = owner.advanceLiftedEntity(
                owner.renderLiftedBomb(runtime.slots()[index]), linkEntityX, linkEntityY, linkZ, romLinkDirection);
        return true;
    }
}
