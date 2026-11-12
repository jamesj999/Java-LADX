package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HookshotBridgeMotion;

/** Focused behavior using the state owned by EntityBackgroundMechanics. */
final class EntityTerrainBehavior {
    static void advanceHookshotBridgeEntity(EntityBackgroundMechanics owner, int index, RoomEntity entity) {
        var runtime = owner.runtime;
        HookshotBridgeMotion.State bridgeState = runtime.motions()
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .state(entity.slot());
        if (bridgeState == null) {
            runtime.clearEntity(entity.slot());
            return;
        }

        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(bridgeState);
        HookshotBridgeMotion.Step step = runtime.motions()
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .advance(entity.slot());
        RoomEntity moved =
                withPositionAndVariant(entity, step.state().x(), step.state().y(), entity.spriteVariant());
        RoomEntityObjectSample object = runtime.physicsTables().readObjectQuery() == null
                ? null
                : runtime.physicsTables().readObjectQuery().sample(moved);
        boolean clear = object != null
                && object.objectId() != EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT
                && object.physicsFlag() == 0;
        runtime.events()
                .hookshotBridgeUpdates
                .add(new HookshotBridgeUpdate(cell.objectLeft(), cell.objectTop(), bridgeState.direction(), !clear));
        if (clear) {
            runtime.clearEntity(entity.slot());
        } else {
            runtime.slots()[index] = moved;
        }
    }

    static boolean beginFalling(
            EntityBackgroundMechanics owner,
            RoomEntity entity,
            RoomEntityGroundInteraction.PitTransition transition,
            boolean ignoreHitsDecrementedBeforeHandler) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int ignoreHitsCountdown = runtime.combatTables().readEnemyIgnoreHitsCountdown(slot);
        if (ignoreHitsDecrementedBeforeHandler) {
            // The Java runtime keeps a shared combat countdown pass for the
            // already-ported handlers.  In the ROM, the pit branch reads the
            // value before that branch's own decrement; reconstruct that
            // value here so INIT and ACTIVE entities both get one decrement
            // at the same source-level point.
            ignoreHitsCountdown = (ignoreHitsCountdown + 1) & 0xFF;
        }
        if (ignoreHitsCountdown == 0) {
            return false;
        }

        ignoreHitsCountdown--;
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, ignoreHitsCountdown);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.physicsTables().writeFallingTargetX(slot, transition.targetX() & 0xFF);
        runtime.physicsTables().writeFallingTargetY(slot, transition.targetY() & 0xFF);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);

        boolean longFallingTransition = entity.type() == EntityRomConstants.ENTITY_MOBLIN_SWORD
                || entity.type() == EntityRomConstants.ENTITY_MOBLIN
                || entity.type() == EntityRomConstants.ENTITY_OCTOROK;
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, longFallingTransition ? 0x6F : 0x48);
        if (!longFallingTransition && ignoreHitsCountdown == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x2F);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.FALLING_JINGLE_ID));
        }
        return true;
    }

    static RoomEntity advanceFallingEntity(EntityBackgroundMechanics owner, RoomEntity entity) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int transition = runtime.physicsTables().readEnemyTransitionCountdown(slot) & 0xFF;
        if (transition == 0) {
            return null;
        }

        if (transition >= 0x40) {
            // EntityFallHandler calls SetEntityVariantForDirection_03 three
            // times before the family handler. ReturnIfNonInteractive then
            // stops that handler after presentation because the status is
            // still FALLING, so movement and wall collision must not run here.
            runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
            int variant = entity.spriteVariant();
            if (entity.type() == EntityRomConstants.ENTITY_OCTOROK
                    || entity.type() == EntityRomConstants.ENTITY_MOBLIN) {
                variant = runtime.motions()
                        .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                        .advancePresentationVariant(slot, 3);
            } else if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_SWORD) {
                variant = runtime.motions()
                        .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                        .advancePresentationVariant(slot, entity.x(), 3);
            }
            return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
        }

        int phase = (transition >>> 4) & 0x03;
        runtime.physicsTables().writeFallingVisualYOffset(slot, EntityRomConstants.FALLING_VISUAL_Y_OFFSETS[phase]);
        if (transition == 0x3F) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.FALLING_JINGLE_ID));
        }

        FallingVector vector = vectorTowardsTarget(
                entity.x(),
                entity.y(),
                entity.z(),
                runtime.physicsTables().readFallingTargetX(slot),
                runtime.physicsTables().readFallingTargetY(slot),
                EntityRomConstants.FALLING_VECTOR_LENGTHS[phase]);
        runtime.physicsTables().writeFallingSpeedX(slot, vector.x());
        runtime.physicsTables().writeFallingSpeedY(slot, vector.y());
        int x = runtime.physicsTables()
                .advanceFallingX(entity.x(), runtime.physicsTables().readFallingSpeedX(slot), slot);
        int y = runtime.physicsTables()
                .advanceFallingY(entity.y(), runtime.physicsTables().readFallingSpeedY(slot), slot);
        if (runtime.spriteHandlers() != null) {
            entity = withDefinition(entity, runtime.spriteViews().forFallingEntity(), phase);
        }
        return withPositionAndVariant(entity, x, y, phase);
    }
}
