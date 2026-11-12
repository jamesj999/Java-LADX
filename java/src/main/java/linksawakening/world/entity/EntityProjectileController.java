package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileEntityCollision;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.SwordBeamMotion;

/** ROM player-projectile collision and object effects, sharing the owning room slot table. */
public final class EntityProjectileController {
    private final int[] magicPowderPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private int latestShotArrowEntityIndex = -1;
    private boolean lastArrowShotPlayedWhoosh;
    private final int[] magicPowderState = new int[EntityRoomLoader.MAX_ENTITIES];
    private int torchPaletteTargetAddress;
    private boolean torchPaletteTransitionActive;
    private final int[] musicalNoteInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private int boomerangSfxCounter;
    private boolean boomerangSoundClockProvided;
    private final EntityProjectileControllerServices runtime;

    EntityProjectileController(EntityProjectileControllerServices runtime) {
        this.runtime = runtime;
    }

    public void playBoomerangSoundIfDue(RoomEntity entity) {
        // PlayBoomerangSfx ($20:4C98) only reads the shared clock.
        if (this.readBoomerangSfxCounter() == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            entity.slot(),
                            entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            EntityRomConstants.BOOMERANG_SFX_ID));
        }
    }

    public void advanceBoomerangEntity(
            int index,
            RoomEntity entity,
            int frame,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {
        int slot = entity.slot();
        if (timersInteractive) {
            runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).decrementTransitionCountdown(slot);
        }
        int variant = runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .advanceSpriteVariant(slot, frame, handlerInteractive);
        if (!handlerInteractive) {
            runtime.slots()[index] = withVariant(entity, variant);
            return;
        }
        playBoomerangSoundIfDue(entity);

        // label_3B7B runs before the position update and uses the launch
        // vector for recoil. This pass also clears the outbound countdown on
        // a successful enemy/object collision, as the ROM does.
        collideBoomerangWithEntities(entity, frame);
        RoomEntity moved = runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        boolean objectCollision = object != null
                && projectileObjectCollision(moved, object, frame, slot) == ProjectileObjectCollision.BLOCKED;
        boolean bushCollision = object != null
                && !runtime.indoorRoom()
                && (object.objectId() == EntityRomConstants.OBJECT_BUSH
                        || object.objectId() == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS);
        RoomEntity positioned = objectCollision
                        && runtime.motions()
                                        .get(BoomerangMotion.class, BoomerangMotion::new)
                                        .transitionCountdown(slot)
                                != 0
                ? entity
                : moved;
        if (bushCollision) {
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            runtime.events()
                    .boomerangObjectRequests
                    .add(new BoomerangObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
        }

        // BoomerangDestroyBushIfNeeded clears the source collision byte before
        // the state handler, so a bush does not also produce the wall-poke
        // branch in the same frame.
        boolean stateCollision = objectCollision && !bushCollision;
        if (runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).state(slot) == 0) {
            if (runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .transitionCountdown(slot)
                    == 0) {
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setVectorTowardsLink(slot, positioned, linkEntityX, linkEntityY, 0x08);
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setState(slot, 1);
            } else if (stateCollision) {
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setTransitionCountdown(slot, 0);
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.SWORD_POKE,
                                positioned.x(),
                                (positioned.y() - positioned.z() + 0x03) & 0xFF));
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_BOOMERANG,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x07));
            }
        } else {
            if ((frame & 0x03) == 0) {
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setVectorTowardsLink(slot, positioned, linkEntityX, linkEntityY, 0x20);
            }
            if (((frame ^ slot) & 0x01) != 0
                    && linkEntityZ == 0
                    && linkMotionState < 0x02
                    // label_3B44 reads hActiveEntityPosX/VisualPosY, cached before
                    // movement. The vector above instead reads the updated WRAM.
                    && BoomerangMotion.overlapsLink(entity, linkEntityX, linkEntityY)) {
                runtime.clearEntity(slot);
                return;
            }
        }
        runtime.slots()[index] = withPositionAndVariant(positioned, positioned.x(), positioned.y(), variant);
    }

    public void advanceSwordBeamEntity(
            int index,
            RoomEntity entity,
            int frame,
            int linkEntityX,
            int linkEntityY,
            int romLinkDirection,
            boolean handlerInteractive) {
        int slot = entity.slot();
        if (runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).state(slot) == 0) {
            int x = (linkEntityX + SwordBeamMotion.firstHandlerOffsetX(romLinkDirection)) & 0xFF;
            int y = (linkEntityY + SwordBeamMotion.firstHandlerOffsetY(romLinkDirection)) & 0xFF;
            runtime.motions()
                    .get(SwordBeamMotion.class, SwordBeamMotion::new)
                    .initializeFirstHandler(slot, romLinkDirection);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_SWORD_BEAM,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.SWORD_BEAM_JINGLE_ID));
            // The state-0 handler only applies the Link-relative launch
            // offset and enters state 1; it does not render the beam yet.
            runtime.slots()[index] = withPositionAndVariant(entity, x, y, -1);
            return;
        }

        int variant = runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .direction(slot);
        if (!handlerInteractive) {
            // ReturnIfNonInteractive_19 renders state 1, then returns before
            // damage, movement, object intersection, or VFX creation.
            runtime.slots()[index] = withPositionAndVariant(entity, entity.x(), entity.y(), variant);
            return;
        }

        ProjectileEntityCollision entityCollision = collideSwordBeamWithEntities(entity, frame);
        RoomEntity moved = runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        ProjectileObjectCollision objectCollision =
                object == null ? ProjectileObjectCollision.NONE : projectileObjectCollision(moved, object, frame, slot);
        if (entityCollision.unload()
                || entityCollision.blocked()
                || objectCollision != ProjectileObjectCollision.NONE) {
            runtime.clearEntity(slot);
            // $45A0 clears the slot and falls through to the shared poke VFX.
            // Physics $FF unloads inside bank $03, bypassing this branch.
            if (objectCollision != ProjectileObjectCollision.UNLOAD
                    && (entityCollision.blocked() || objectCollision == ProjectileObjectCollision.BLOCKED)) {
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.SWORD_POKE, entity.x(), (entity.y() - entity.z() + 3) & 0xFF));
            }
            return;
        }

        if (((frame + 1) & 0x03) == 0) {
            runtime.events()
                    .transientVfxRequests
                    .add(new TransientVfxRequest(
                            TransientVfxType.SWORD_BEAM, entity.x(), (entity.y() - entity.z()) & 0xFF, variant));
        }
        runtime.slots()[index] = withPositionAndVariant(moved, moved.x(), moved.y(), variant);
    }

    public void advanceMagicRodFireballEntity(
            int index, RoomEntity entity, int frame, boolean handlerInteractive, boolean timersInteractive) {
        int slot = entity.slot();
        // The source calls func_003_75A2 before either the wall-fire branch or
        // ReturnIfNonInteractive. A fireball can therefore still deliver the
        // common target collision while Link is being transitioned.
        boolean unloadOnHit = collideMagicRodFireballWithEntities(entity, frame);
        if (unloadOnHit) {
            // $7782-$7795 unloads a normal projectile hit. Blocking targets
            // and existing wall flames retain a nonzero collision byte.
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        int variant = runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .advanceSpriteVariant(slot, frame);
        if (runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .privateCountdown1(slot)
                != 0) {
            if (runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .tickFireTransition(slot, timersInteractive)) {
                runtime.clearEntity(slot);
                return;
            }
            EntitySpriteDefinition fireDefinition = runtime.spriteHandlers() == null
                    ? entity.spriteDefinition()
                    : runtime.spriteHandlers().forMagicRodFireState();
            runtime.slots()[index] = withDefinition(entity, fireDefinition, variant);
            return;
        }

        // ReturnIfNonInteractive_03 is after the normal sprite write and
        // before movement/object lookup in the ROM helper.
        if (!handlerInteractive) {
            runtime.slots()[index] = withVariant(entity, variant);
            return;
        }

        RoomEntity moved = runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        ProjectileObjectCollision objectCollision =
                object == null ? ProjectileObjectCollision.NONE : projectileObjectCollision(moved, object, frame, slot);
        if (objectCollision == ProjectileObjectCollision.UNLOAD) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        // $7DF3-$7E00 restores BOTH coordinates from the active pre-movement
        // position before the fireball handler can clear a burnable collision.
        RoomEntity positioned = objectCollision == ProjectileObjectCollision.BLOCKED ? entity : moved;
        if (isMagicRodBurnableObject(object)) {
            runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .setCollision(slot, false);
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            runtime.events()
                    .magicRodObjectRequests
                    .add(new MagicRodObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
            runtime.slots()[index] = withVariant(positioned, variant);
            return;
        }

        if (runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .collided(slot)
                || objectCollision == ProjectileObjectCollision.BLOCKED) {
            // ArrowRenderAndMove writes privateCountdown1 after the normal
            // fireball display has already been rendered. Keep the normal
            // definition for this frame; the next handler tick switches to
            // FireSpriteVariants and decrements the $30 countdown.
            runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .beginFireTransition(slot);
        }
        runtime.slots()[index] = withVariant(positioned, variant);
    }

    boolean isMagicRodBurnableObject(RoomEntityObjectSample object) {
        if (object == null) {
            return false;
        }
        int objectId = object.objectId() & 0xFF;
        if (runtime.indoorRoom()) {
            return objectId == 0x8A; // OBJECT_FROZEN_BLOCK
        }
        return objectId == EntityRomConstants.OBJECT_BUSH || objectId == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS;
    }

    public void advanceMagicPowderSprinkleEntity(
            int index,
            RoomEntity entity,
            int frame,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {
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

        if (this.readMagicPowderState(slot) != 0) {
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
            if (runtime.roomVariables().readEntityRoomId() != 0x74 && this.readTorchPaletteTargetAddress() < 0x0C) {
                this.writeTorchPaletteTargetAddress(this.readTorchPaletteTargetAddress() + (4));
                if (linkMotionState != 0x05) {
                    this.writeTorchPaletteTransitionActive(true);
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
            if (transitionCountdown < 0x10 && this.readMagicPowderPrivateState4(slot) == 0) {
                collideMagicPowderWithEntities(entity, frame);
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
            if (linkMotionState == 0x05 || this.readTorchPaletteTransitionActive()) {
                return;
            }
            if (this.readTorchPaletteTargetAddress() != 0) {
                this.writeTorchPaletteTargetAddress((this.readTorchPaletteTargetAddress() - 4) & 0xFF);
                this.writeTorchPaletteTransitionActive(true);
            }
            this.writeMagicPowderState(slot, 1);
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
        if (!runtime.indoorRoom() && this.readMagicPowderPrivateState4(slot) == 0) {
            collideMagicPowderWithEntities(entity, frame);
        }
    }

    boolean collideMagicPowderWithEntities(RoomEntity sprinkle, int frame) {
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
            applyPlayerProjectileDamage(target, EntityRomConstants.DAMAGE_TYPE_MAGIC_POWDER, 0, 0);
        }
        return collided;
    }

    ProjectileObjectCollision projectileObjectCollision(
            RoomEntity entity, RoomEntityObjectSample object, int frame, int slot) {
        int physics = object.physicsFlag() & 0xFF;
        if (physics == 0xFF) {
            // $7E05 redirects boomerangs to the ordinary collision branch;
            // other projectiles unload immediately.
            return entity.type() == EntityRomConstants.ENTITY_BOOMERANG
                    ? ProjectileObjectCollision.BLOCKED
                    : ProjectileObjectCollision.UNLOAD;
        }
        if (physics >= 0xD0 && physics <= 0xD3) {
            if (runtime.liftThrow().readThrownDirection(slot) == physics - 0xD0) {
                if (entity.z() != 0) {
                    runtime.physicsTables()
                            .writeEntityUnknownJ(slot, (runtime.physicsTables().readEntityUnknownJ(slot) + 1) & 0xFF);
                    return ProjectileObjectCollision.NONE;
                }
                return ProjectileObjectCollision.BLOCKED;
            }
            if (runtime.physicsTables().readEntityUnknownJ(slot) == 0) {
                return ProjectileObjectCollision.BLOCKED;
            }
            if ((frame & 0x03) == 0 || (!runtime.indoorRoom() && (frame & 0x01) == 0)) {
                return ProjectileObjectCollision.NONE;
            }
            runtime.physicsTables()
                    .writeEntityUnknownJ(slot, (runtime.physicsTables().readEntityUnknownJ(slot) - 1) & 0xFF);
            return ProjectileObjectCollision.NONE;
        }
        return BoomerangMotion.objectPhysicsCollides(physics)
                ? ProjectileObjectCollision.BLOCKED
                : ProjectileObjectCollision.NONE;
    }

    ProjectileEntityCollision collideSwordBeamWithEntities(RoomEntity beam, int frame) {
        int sourceSlot = beam.slot();
        int sourceVisualY = (beam.y() - beam.z()) & 0xFF;
        boolean unload = false;
        boolean blocked = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((beam.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            // $7656 forces a collision before reading the ignore-hits timer.
            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                blocked = true;
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            if (ironMaskBlocksProjectile(
                    target,
                    runtime.motions()
                            .get(SwordBeamMotion.class, SwordBeamMotion::new)
                            .direction(sourceSlot))) {
                blocked = true;
                continue;
            }
            unload |= !blocked;
            applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_SWORD_BEAM,
                    runtime.motions()
                            .get(SwordBeamMotion.class, SwordBeamMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions()
                            .get(SwordBeamMotion.class, SwordBeamMotion::new)
                            .speedY(sourceSlot));
        }
        return new ProjectileEntityCollision(unload, blocked);
    }

    boolean collideMagicRodFireballWithEntities(RoomEntity fireball, int frame) {
        int sourceSlot = fireball.slot();
        int sourceVisualY = (fireball.y() - fireball.z()) & 0xFF;
        boolean unloadOnHit = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((fireball.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .setCollision(sourceSlot, true);
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            if (target.type() == EntityRomConstants.ENTITY_IRON_MASK
                    && runtime.motions()
                                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                    .maskRemovalState(targetSlot)
                            == 0
                    && (runtime.motions()
                                            .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                            .direction(targetSlot)
                                    ^ 0x01)
                            == runtime.motions()
                                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                                    .direction(sourceSlot)) {
                runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .setCollision(sourceSlot, true);
                continue;
            }

            unloadOnHit |= !runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .collided(sourceSlot);
            applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_MAGIC_ROD,
                    runtime.motions()
                            .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions()
                            .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                            .speedY(sourceSlot));
        }
        return unloadOnHit;
    }

    public boolean collidePlayerArrowWithEntities(RoomEntity arrow, int frame) {
        boolean unload = false;
        int arrowSlot = arrow.slot();
        int attackType = runtime.bombInteractions().readPlayerArrowBombArrow(arrowSlot)
                ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                : EntityRomConstants.DAMAGE_TYPE_ARROW;
        int arrowVisualY = (arrow.y() - arrow.z()) & 0xFF;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == arrowSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE)
                            != 0
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || target.spriteVariant() < 0) {
                continue;
            }

            if (((arrow.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - arrowVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .markCollision(arrowSlot);
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            if (ironMaskBlocksProjectile(
                    target,
                    runtime.motions()
                            .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                            .direction(arrowSlot))) {
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .markCollision(arrowSlot);
                continue;
            }
            unload |= !runtime.motions()
                    .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                    .collided(arrowSlot);
            if (attackType == EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW) {
                // BombArrowHandler selects DAMAGE_TYPE_BOMB_ARROW. The
                // shared damage helper deliberately treats this active-arrow
                // state as a harmless target transition rather than calling
                // ApplySwordDamagesToEnemy.
                runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x03);
            } else {
                applyPlayerArrowDamage(arrow, target);
            }
        }
        return unload;
    }

    boolean collideBoomerangWithEntities(RoomEntity boomerang, int frame) {
        int sourceSlot = boomerang.slot();
        int sourceVisualY = (boomerang.y() - boomerang.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            boolean grabbable = (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0;
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (!grabbable && !RoomEntityCombatRules.supportsEnemyCollision(target.type()))
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((boomerang.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if (grabbable) {
                collided = true;
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setTransitionCountdown(sourceSlot, 0);
                // The source writes sourceSlot + 1 to the target's private
                // state five. The current runtime has no consumer for that
                // field, but the source-side transition is still observable.
                continue;
            }
            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                collided = true;
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setTransitionCountdown(sourceSlot, 0);
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            collided = true;
            runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).setTransitionCountdown(sourceSlot, 0);
            if (ironMaskBlocksProjectile(
                    target,
                    runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .direction(sourceSlot))) {
                continue;
            }
            applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_BOOMERANG,
                    runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .speedY(sourceSlot));
        }
        return collided;
    }

    boolean ironMaskBlocksProjectile(RoomEntity target, int direction) {
        return target.type() == EntityRomConstants.ENTITY_IRON_MASK
                && runtime.motions()
                                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                .maskRemovalState(target.slot())
                        == 0
                && (runtime.motions()
                                        .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                        .direction(target.slot())
                                ^ 1)
                        == direction;
    }

    public boolean collideBouncingBombiteWithEntities(RoomEntity source, int frame) {
        int sourceSlot = source.slot();
        int sourceVisualY = (source.y() - source.z()) & 0xFF;
        int sourceSpeedX =
                runtime.motions().get(BombiteMotion.class, BombiteMotion::new).speedX(sourceSlot);
        int sourceSpeedY =
                runtime.motions().get(BombiteMotion.class, BombiteMotion::new).speedY(sourceSlot);
        boolean collided = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || ((source.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18
                    || target.spriteVariant() == -1) {
                continue;
            }

            collided = true;
            // GetEntityTransitionCountdown uses BC, which remains the active
            // source slot throughout func_003_75A2.
            runtime.physicsTables().writeEnemyTransitionCountdown(sourceSlot, 0);
            if (target.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE) {
                runtime.motions()
                        .get(BombiteMotion.class, BombiteMotion::new)
                        .enterBouncingLitFromEntityCollision(targetSlot, sourceSpeedX, sourceSpeedY);
                runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x40);
                runtime.bombInteractions().writeBombPrivateCountdown1(targetSlot, 0x08);
            }
        }
        return collided;
    }

    public boolean collideHookshotWithEntities(
            RoomEntity hookshot, HookshotChainMotion.State hookshotState, int frame) {
        boolean unmasked = false;
        int sourceSlot = hookshot.slot();
        // $7C39 moves WRAM, but $7C59's shared hit test still reads the
        // hActiveEntity coordinates cached at the beginning of this pass.
        int sourceVisualY = (hookshot.y() - hookshot.z()) & 0xFF;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || ((hookshot.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18
                    || target.spriteVariant() == -1
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            if (target.type() != EntityRomConstants.ENTITY_IRON_MASK
                    || ((runtime.motions()
                                            .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                            .direction(targetSlot)
                                    ^ 0x01)
                            != hookshotState.direction())
                    || runtime.motions()
                                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                    .maskRemovalState(targetSlot)
                            != 0) {
                continue;
            }

            runtime.motions()
                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                    .setMaskRemovalState(targetSlot, 1);
            runtime.motions()
                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                    .initializeFromMasked(
                            targetSlot,
                            runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .direction(targetSlot),
                            runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .speedX(targetSlot),
                            runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .speedY(targetSlot));
            runtime.entityPlayerSpawns()
                    .spawnIronMasksMask(
                            hookshotState.x(), hookshotState.y(), hookshot.spriteVariant() & 0x01, sourceSlot);
            unmasked = true;
        }
        return unmasked;
    }

    void applyPlayerArrowDamage(RoomEntity arrow, RoomEntity target) {
        applyPlayerProjectileDamage(
                target,
                runtime.bombInteractions().readPlayerArrowBombArrow(arrow.slot())
                        ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                        : EntityRomConstants.DAMAGE_TYPE_ARROW,
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .speedX(arrow.slot()),
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .speedY(arrow.slot()));
    }

    void applyPlayerProjectileDamage(RoomEntity target, int damageType, int speedX, int speedY) {
        int targetSlot = target.slot();
        runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configureFromSpeed(targetSlot, speedX, speedY);

        RomEnemyCombatTables.SwordDamageResult damageResult = runtime.enemyCombatTables() == null
                ? null
                : runtime.enemyCombatTables()
                        .resolveAttackDamage(
                                target.type(), runtime.combatTables().readEnemyHealthGroup(target.slot()), damageType);
        int rawDamage =
                damageResult == null ? RoomEntityCombatRules.basicSwordDamage(target.type()) : damageResult.rawValue();
        if (rawDamage == 0) {
            return;
        }

        int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
        int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
        EntityCombatEvent.SoundChannel secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
        int secondarySoundId = -1;

        if (rawDamage == 0xFE) {
            runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x60);
            runtime.combatTables().writeEnemyStunnedCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
            runtime.physicsTables()
                    .writeEnemyPhysicsFlags(
                            targetSlot, (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot) + 2) & 0xFF);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(targetSlot);
            runtime.slots()[targetSlot] = withStatus(target, EntityStatus.BURNING);
            secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
            secondarySoundId = 0x12;
        } else if (rawDamage == 0xFF) {
            runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyStunnedCountdown(targetSlot, 0xFF);
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(targetSlot);
            runtime.slots()[targetSlot] = withStatus(target, EntityStatus.STUNNED);
        } else if (rawDamage == 0xFD) {
            // The fairy-conversion branch is entity-specific in the ROM. The
            // generic table result is still surfaced so the pending special
            // action is not silently changed into numeric damage.
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(targetSlot);
        } else {
            runtime.combatTables()
                    .writeEnemyHealth(
                            targetSlot, Math.max(0, runtime.combatTables().readEnemyHealth(targetSlot) - enemyDamage));
            if (runtime.combatTables().readEnemyHealth(targetSlot) == 0) {
                runtime.combatTables().writeDyingCountdown(targetSlot, 0x2F);
                runtime.combatTables().writePowerRecoilDeath(targetSlot, false);
                runtime.slots()[targetSlot] = withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false);
            } else if (isZolGelType(target.type())) {
                runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).onSwordHit(targetSlot);
                if (target.type() == EntityRomConstants.ENTITY_ZOL && runtime.spriteHandlers() != null) {
                    runtime.slots()[targetSlot] =
                            withDefinition(target, runtime.spriteHandlers().forZolSlimeEye(), target.spriteVariant());
                }
            }
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x18);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
        }

        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        targetSlot,
                        target.type(),
                        0,
                        false,
                        enemyDamage,
                        specialAction,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x03,
                        secondaryChannel,
                        secondarySoundId,
                        null));
    }

    public boolean playerArrowBombArrow(int slot) {
        return runtime.bombInteractions().readPlayerArrowBombArrow(slot);
    }

    public void setWingedStateTwoThisFrame(int slot, boolean value) {
        runtime.slotProperties().writeWingedStateTwoThisFrame(slot, value);
    }

    public void setWingedSwordAttackThisFrame(int slot, boolean value) {
        runtime.slotProperties().writeWingedSwordAttackThisFrame(slot, value);
    }

    public int swordMoblinAlertingSoundCounter() {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .readSwordMoblinAlertingSoundCounter();
    }

    public void setSwordMoblinAlertingSoundCounter(int value) {
        runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .writeSwordMoblinAlertingSoundCounter(value);
    }

    int readMagicPowderPrivateState4(int slot) {
        return magicPowderPrivateState4[slot];
    }

    int writeMagicPowderPrivateState4(int slot, int value) {
        return magicPowderPrivateState4[slot] = value;
    }

    int readLatestShotArrowEntityIndex() {
        return latestShotArrowEntityIndex;
    }

    int writeLatestShotArrowEntityIndex(int value) {
        return latestShotArrowEntityIndex = value;
    }

    boolean readLastArrowShotPlayedWhoosh() {
        return lastArrowShotPlayedWhoosh;
    }

    boolean writeLastArrowShotPlayedWhoosh(boolean value) {
        return lastArrowShotPlayedWhoosh = value;
    }

    int readMagicPowderState(int slot) {
        return magicPowderState[slot];
    }

    int writeMagicPowderState(int slot, int value) {
        return magicPowderState[slot] = value;
    }

    int readTorchPaletteTargetAddress() {
        return torchPaletteTargetAddress;
    }

    int writeTorchPaletteTargetAddress(int value) {
        return torchPaletteTargetAddress = value;
    }

    boolean readTorchPaletteTransitionActive() {
        return torchPaletteTransitionActive;
    }

    boolean writeTorchPaletteTransitionActive(boolean value) {
        return torchPaletteTransitionActive = value;
    }

    int readMusicalNoteInertia(int slot) {
        return musicalNoteInertia[slot];
    }

    int writeMusicalNoteInertia(int slot, int value) {
        return musicalNoteInertia[slot] = value;
    }

    int readMusicalNoteSpeedX(int slot) {
        return musicalNoteSpeedX[slot];
    }

    int writeMusicalNoteSpeedX(int slot, int value) {
        return musicalNoteSpeedX[slot] = value;
    }

    int readMusicalNoteSpeedY(int slot) {
        return musicalNoteSpeedY[slot];
    }

    int writeMusicalNoteSpeedY(int slot, int value) {
        return musicalNoteSpeedY[slot] = value;
    }

    int readBoomerangSfxCounter() {
        return boomerangSfxCounter;
    }

    int writeBoomerangSfxCounter(int value) {
        return boomerangSfxCounter = value;
    }

    boolean readBoomerangSoundClockProvided() {
        return boomerangSoundClockProvided;
    }

    boolean writeBoomerangSoundClockProvided(boolean value) {
        return boomerangSoundClockProvided = value;
    }
}
