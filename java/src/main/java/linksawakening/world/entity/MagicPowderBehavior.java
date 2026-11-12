package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityProjectileController. */
final class MagicPowderBehavior {
    static void advanceMagicPowderSprinkleEntity(
            EntityProjectileController owner,
            int index,
            RoomEntity entity,
            int frame,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (timersInteractive) {
            runtime.lifecycle().decrementSlowTransitionCountdown(slot, frame);
        }

        // func_018_7B02 renders before ReturnIfNonInteractive_18, even when
        // the handler is paused. Zero means no sprinkle sprite, not unloading.
        int transitionCountdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        runtime.slots()[index] =
                withVariant(entity, transitionCountdown == 0 ? -1 : (transitionCountdown >>> 2) & 0x07);
        if (!handlerInteractive) {
            return;
        }

        if (owner.readMagicPowderState(slot) != 0) {
            // State one has no ordinary sprinkle rectangle: the source emits
            // its short torch pair only at the state transition boundaries.
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) != 0) {
                runtime.slots()[index] = withVariant(entity, -1);
                return;
            }

            int objectLeft = entity.x() & 0xF0;
            int objectTop = entity.y() & 0xF0;
            int location = (objectTop | (objectLeft >>> 4)) & 0xFF;
            runtime.events()
                    .magicPowderObjectRequests
                    .add(new MagicPowderObjectRequest(
                            slot, location, objectLeft, objectTop, MagicPowderObjectAction.EXTINGUISH_TORCH));
            // Keep the palette state visible to later powder handlers in
            // this pass, before RoomSession applies their object requests.
            if (runtime.roomVariables().readEntityRoomId() != 0x74 && owner.readTorchPaletteTargetAddress() < 0x0C) {
                owner.writeTorchPaletteTargetAddress(owner.readTorchPaletteTargetAddress() + (4));
                if (linkMotionState != 0x05) {
                    owner.writeTorchPaletteTransitionActive(true);
                }
            }
            runtime.clearEntity(slot);
            return;
        }

        if (transitionCountdown == 0) {
            runtime.clearEntity(slot);
            return;
        }

        if (transitionCountdown != 0x07) {
            if (transitionCountdown < 0x10 && owner.readMagicPowderPrivateState4(slot) == 0) {
                owner.collideMagicPowderWithEntities(entity, frame);
            }
            return;
        }

        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null
                // The shared query samples (X, Y-8). Powder's $797E-$7997
                // formula instead samples (X-1, Y-9), including byte wrapping.
                ? null
                : intersectionQuery.sample(withPositionAndVariant(
                        entity, (entity.x() - 1) & 0xFF, (entity.y() - 1) & 0xFF, entity.spriteVariant()));
        int objectLeft = EntityMotionMath.magicPowderObjectLeft(entity);
        int objectTop = EntityMotionMath.magicPowderObjectTop(entity);
        int location = (objectTop | (objectLeft >>> 4)) & 0xFF;
        int objectId = object == null ? 0xFF : object.objectId() & 0xFF;

        if (!runtime.indoorRoom()
                && (objectId == EntityRomConstants.OBJECT_BUSH
                        || objectId == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS)) {
            runtime.events()
                    .magicPowderObjectRequests
                    .add(new MagicPowderObjectRequest(
                            slot, location, objectLeft, objectTop, MagicPowderObjectAction.REVEAL));
            runtime.events()
                    .transientVfxRequests
                    .add(new TransientVfxRequest(TransientVfxType.POOF, entity.x(), (entity.y() - entity.z()) & 0xFF));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.MAGIC_POWDER_POOF_JINGLE_ID));
            runtime.clearEntity(slot);
            return;
        }

        if (runtime.indoorRoom() && objectId == EntityRomConstants.OBJECT_TORCH_UNLIT) {
            // This motion-state check belongs only to GBC torch ignition.
            if (linkMotionState == 0x05 || owner.readTorchPaletteTransitionActive()) {
                return;
            }
            if (owner.readTorchPaletteTargetAddress() != 0) {
                owner.writeTorchPaletteTargetAddress((owner.readTorchPaletteTargetAddress() - 4) & 0xFF);
                owner.writeTorchPaletteTransitionActive(true);
            }
            owner.writeMagicPowderState(slot, 1);
            runtime.physicsTables()
                    .writeSlowTransitionCountdown(slot, EntityRomConstants.MAGIC_POWDER_SLOW_TRANSITION_COUNTDOWN);
            runtime.physicsTables().writeSlowTimerInitialized(slot, true);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
            RoomEntity torch = withPositionAndVariant(entity, objectLeft, objectTop, 0);
            EntitySpriteDefinition torchDefinition = runtime.spriteHandlers() == null
                    ? entity.spriteDefinition()
                    : runtime.spriteHandlers().forMagicPowderTorchState(true);
            runtime.slots()[index] = withDefinition(torch, torchDefinition, 0);
            runtime.events()
                    .magicPowderObjectRequests
                    .add(new MagicPowderObjectRequest(
                            slot, location, objectLeft, objectTop, MagicPowderObjectAction.IGNITE_TORCH));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x12));
            return;
        }

        // label_018_7A4B reaches the common collision helper only during the
        // final sixteen transition ticks and only when private state 4 is 0.
        // $79DA returns for non-torch indoor objects. Outdoors, $79CF leaves
        // A=0 and jumps to the collision branch regardless of the object id.
        if (!runtime.indoorRoom() && owner.readMagicPowderPrivateState4(slot) == 0) {
            owner.collideMagicPowderWithEntities(entity, frame);
        }
    }

    static boolean collideMagicPowderWithEntities(EntityProjectileController owner, RoomEntity sprinkle, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = sprinkle.slot();
        int sourceVisualY = (sprinkle.y() - sprinkle.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((sprinkle.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }
            if (!runtime.indoorRoom()
                    && (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) == 0
                    && target.type() == EntityRomConstants.ENTITY_TARIN
                    && runtime.motions()
                                    .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                                    .state(targetSlot)
                            == 0) {
                runtime.motions()
                        .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                        .startPowderTransformation(targetSlot);
                runtime.physicsTables().writeSlowTransitionCountdown(targetSlot, 0x7F);
                runtime.physicsTables().writeSlowTimerInitialized(targetSlot, true);
                runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x10);
                collided = true;
                continue;
            }
            if ((targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (runtime.combatTables().readEnemyHitboxFlags(targetSlot)
                                    & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            collided = true;
            owner.applyPlayerProjectileDamage(target, EntityRomConstants.DAMAGE_TYPE_MAGIC_POWDER, 0, 0);
        }
        return collided;
    }
}
