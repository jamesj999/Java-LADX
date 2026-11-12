package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** ROM lift, carry and throw mechanics, sharing the owning room slot table. */
public final class EntityLiftThrowController {
    private final RoomEntityRuntime runtime;

    EntityLiftThrowController(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    public RoomEntity advanceLiftedEntity(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) {
        int slot = entity.slot();
        if (!runtime.liftedStateInitialized[slot]) {
            runtime.liftedPhase[slot] = 0;
            runtime.liftedSourceDirection[slot] = romLinkDirection;
            runtime.liftedStateInitialized[slot] = true;
            runtime.liftedEffectiveDirection = romLinkDirection;
            runtime.enemyTransitionCountdown[slot] = 0x02;
        }

        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
                runtime.liftedPhase[slot],
                runtime.enemyTransitionCountdown[slot],
                runtime.liftedSourceDirection[slot],
                romLinkDirection,
                byteValue(linkEntityX),
                byteValue(linkEntityY),
                byteValue(linkZ),
                runtime.liftedLinkC13B,
                entity.type(),
                runtime.groundInteractionSideScrolling,
                runtime.chestPowerBraceletLevel >= 2 || runtime.linkState.fastLiftTransition,
                entity.z());
        runtime.liftedPhase[slot] = update.phase();
        runtime.enemyTransitionCountdown[slot] = update.transitionCountdown();
        runtime.liftedCarryState = update.carryState();
        runtime.liftedEffectiveDirection = update.effectiveDirection();
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

    boolean advanceStatueRubble(RoomEntity entity, boolean interactive) {
        if (entity.type() != 0x9D || entity.spriteDefinition().shape() != EntitySpriteDefinition.Shape.SINGLE)
            return false;
        if (!interactive) return true;
        var update = runtime.thrownEntityMotion.advance(entity, false, null);
        runtime.slots[entity.slot()] = update.entity().z() == 0 ? RoomEntity.disabled(entity.slot()) : update.entity();
        return true;
    }

    private void smashStatue(RoomEntity entity) {
        // LiftableStatue_SpawnRubble ($19:4165-$41FE).
        int[] xs = {0, 8, 0, 8, 0, 8};
        int[] ys = {-8, -8, 0, 0, 8, 8};
        int[] vx = {0xFC, 5, 0xFA, 6, 0xFB, 4};
        int[] vy = {0xFC, 0xF8, 0xFE, 0xFF, 3, 2};
        int[] vz = {0x18, 0x14, 0x13, 0x16, 0x12, 0x14};
        if (runtime.spriteHandlers != null) {
            var definition = runtime.spriteHandlers.decodeSingle(0x9D, 0x19, 0x4020, 1, 0);
            for (int piece = 0; piece < 6; piece++) {
                int slot = runtime.findFreeEntitySlot();
                if (slot < 0) break;
                runtime.slots[slot] = new RoomEntity(
                        slot,
                        -1,
                        0x9D,
                        (entity.x() + xs[piece]) & 255,
                        (entity.y() + ys[piece]) & 255,
                        EntityStatus.ACTIVE,
                        definition,
                        0,
                        0,
                        0,
                        entity.z(),
                        -1,
                        false);
                runtime.initializeHealthGroup(slot);
                runtime.enemyPhysicsFlags[slot] = 0xC1;
                runtime.enemyHitboxFlags[slot] = 0;
                runtime.dynamicEntitySpawnedThisFrame[slot] = true;
                runtime.thrownEntityMotion.startEnemyBomb(slot, vx[piece], vy[piece], vz[piece]);
            }
        }
        int visualY = (entity.y() - entity.z()) & 255;
        runtime.events.transientVfxRequests.add(new linksawakening.world.entity.event.TransientVfxRequest(
                linksawakening.vfx.TransientVfxType.POOF, entity.x(), visualY));
        runtime.events.transientVfxRequests.add(new linksawakening.world.entity.event.TransientVfxRequest(
                linksawakening.vfx.TransientVfxType.POOF, entity.x(), (visualY - 16) & 255));
        runtime.events.pendingEntityEvents.add(
                new EntityCombatEvent(entity.slot(), 0x9D, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x29));
    }

    RoomEntity advanceThrownEntity(
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        int slot = entity.slot();
        if (!runtime.thrownMotionInitialized[slot]) {
            runtime.thrownDirection[slot] = romLinkDirection;
            runtime.thrownEntityMotion.start(slot, romLinkDirection, entity.type(), sideScrolling);
            runtime.thrownMotionInitialized[slot] = true;
        }
        ThrownEntityMotion.Update update = runtime.thrownEntityMotion.advance(
                entity, sideScrolling, backgroundCollision, frame, runtime.entityGroundStatus[slot]);
        RoomEntity updated = update.entity();
        if (entity.type() == EntityRomConstants.ENTITY_GENIE && (update.blockedX() || update.blockedY())) {
            updated = runtime.genie.registerGenieJarImpact(updated);
            if (updated.status() == EntityStatus.ACTIVE) {
                return updated;
            }
        }
        if (entity.type() == 0x9D && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            runtime.slots[slot] = RoomEntity.disabled(slot);
            runtime.thrownEntityMotion.clear(slot);
            runtime.thrownMotionInitialized[slot] = false;
            smashStatue(updated);
            return runtime.slots[slot];
        }
        if (entity.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            collideThrownEntityWithEntities(updated, frame);
            runtime.slots[slot] = RoomEntity.disabled(slot);
            runtime.thrownEntityMotion.clear(slot);
            runtime.thrownMotionInitialized[slot] = false;
            runtime.spawnLiftableRockSmash(updated.x(), updated.y(), entity.spriteVariant());
            return runtime.slots[slot];
        }
        if (runtime.thrownEntityMotion.speedX(slot) == 0 && runtime.thrownEntityMotion.speedY(slot) == 0) {
            runtime.thrownEntityMotion.clear(slot);
            runtime.thrownMotionInitialized[slot] = false;
            runtime.enemyStunnedCountdown[slot] = 0xFF;
            updated = withStatus(updated, EntityStatus.STUNNED);
        }
        return updated;
    }

    public boolean sideViewPotBackgroundCollision(
            RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) {
        int slot = original.slot();
        boolean blocked = false;
        if (runtime.sideViewPotMotion.speedX(slot) != 0) {
            int direction = (runtime.sideViewPotMotion.speedX(slot) & 0x80) != 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            if (runtime.backgroundInteraction != null) {
                blocked |= runtime.backgroundInteraction
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                original.y(),
                                runtime.enemyIgnoreHitsCountdown[slot],
                                frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), original.y());
            }
        }
        if (runtime.sideViewPotMotion.speedY(slot) != 0) {
            int direction = (runtime.sideViewPotMotion.speedY(slot) & 0x80) != 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            if (runtime.backgroundInteraction != null) {
                blocked |= runtime.backgroundInteraction
                        .probe(original, direction, moved.x(), moved.y(), runtime.enemyIgnoreHitsCountdown[slot], frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), moved.y());
            }
        }
        return blocked;
    }

    void collideThrownEntityWithEntities(RoomEntity thrown, int frame) {
        int sourceSlot = thrown.slot();
        int sourceVisualY = (thrown.y() - thrown.z()) & 0xFF;
        for (int targetSlot = runtime.slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }
            RoomEntity target = runtime.slots[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.enemyPhysicsFlags[targetSlot] & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || target.spriteVariant() < 0
                    || unsignedByteAbs(thrown.x() - target.x()) >= 0x0C
                    || unsignedByteAbs(sourceVisualY - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                    || (runtime.enemyHitboxFlags[targetSlot] & EntityRomConstants.HITFLAGS_IGNORE_HITS) != 0
                    || runtime.enemyIgnoreHitsCountdown[targetSlot] != 0) {
                continue;
            }
            runtime.projectiles.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_THROW_AT,
                    runtime.thrownEntityMotion.speedX(sourceSlot),
                    runtime.thrownEntityMotion.speedY(sourceSlot));
        }
    }

    public RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!runtime.placedBombMotionInitialized[slot]) {
            int direction = runtime.bombDirection[slot];
            if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
                    || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
                // A room-loaded type-$02 entity has no SpawnPlayerProjectile direction.
                // Preserve its static source state instead of inventing a table index.
                return entity;
            }
            runtime.thrownEntityMotion.startPlacedBomb(slot, runtime.bombDirection[slot]);
            runtime.placedBombMotionInitialized[slot] = true;
        }
        return runtime.thrownEntityMotion
                .advance(
                        entity,
                        runtime.groundInteractionSideScrolling,
                        backgroundCollision,
                        frame,
                        runtime.entityGroundStatus[slot])
                .entity();
    }

    public RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!runtime.enemyBombMotionInitialized[slot]) {
            // Room-loaded type-$02 entities and legacy test-created enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        return runtime.thrownEntityMotion
                .advance(
                        entity,
                        runtime.groundInteractionSideScrolling,
                        backgroundCollision,
                        frame,
                        runtime.entityGroundStatus[slot])
                .entity();
    }

    public boolean cuccoPowerBraceletHeld() {
        if (runtime.linkState.linkItemB == 0x03) {
            return runtime.linkState.actionButtonBHeld;
        }
        return runtime.linkState.linkItemA == 0x03 && runtime.linkState.actionButtonAHeld;
    }

    public boolean beginBushCrawlerLift(int index, RoomEntity source, int privateState4, int romLinkDirection) {
        if (source.type() != EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            return false;
        }
        runtime.slots[index] = source;
        if (!runtime.beginLift(source.slot(), romLinkDirection)) {
            return false;
        }

        int slot = source.slot();
        RoomEntity carried = runtime.slots[slot];
        EntitySpriteDefinition rockDefinition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        carried = withType(carried, EntityRomConstants.ENTITY_LIFTABLE_ROCK, rockDefinition);
        carried = withVariant(carried, rockDefinition.supported() ? (privateState4 ^ 0x01) & 0x01 : -1);
        runtime.slots[slot] = carried;
        runtime.fallingVisualYOffset[slot] = 0;
        runtime.bushCrawlerMotion.clear(slot);
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, EntityRomConstants.ENTITY_BUSH_CRAWLER, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x02));

        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return true;
        }
        EntitySpriteDefinition crawlerDefinition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BUSH_CRAWLER);
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
        runtime.slots[freeSlot] = replacement;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyTransitionCountdown[freeSlot] = 0x40;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyHitboxFlags[freeSlot] = initialHitboxFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.fallingVisualYOffset[freeSlot] = 0;
        runtime.bushCrawlerMotion.initializeSpecial(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    public void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        int slot = entity.slot();
        int countdown = runtime.liftableRockSmashCountdown[slot] & 0xFF;
        if (countdown <= 1) {
            // LiftableRockEntityHandler unloads on the countdown-$01 frame,
            // before the display-list helper gets a chance to render it.
            runtime.clearEntity(slot);
            return;
        }

        int sourceVariant = runtime.liftableRockSmashSourceVariant[slot];
        boolean swampLeaves = !runtime.indoorRoom
                && runtime.spriteSelection != null
                && runtime.spriteSelection.roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.spriteSelection.roomId() == 0x32;
        int variant = EntityMotionMath.liftableRockSmashVariant(sourceVariant, swampLeaves, countdown);
        if (sourceVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS && ((frameCounter ^ slot) & 0x01) != 0) {
            // The $FF tall-grass path in func_019_7C50 only emits OAM on
            // alternating frames. -1 is the same hidden-display sentinel
            // consumed by RenderActiveEntitySpritesRect.
            variant = -1;
        }
        runtime.slots[index] = withVariant(entity, variant);
    }

    public RoomEntity renderLiftedEntity(RoomEntity entity, int frameCounter) {
        if (entity.type() != EntityRomConstants.ENTITY_BOMB) {
            return entity.type() == EntityRomConstants.ENTITY_CUCCO
                    ? withVariant(entity, (frameCounter >>> 2) & 0x01)
                    : entity;
        }
        runtime.enemyFlashCountdown[entity.slot()] = 0;
        return withDefinition(entity, runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB), 0);
    }

    RoomEntity renderLiftedBomb(RoomEntity entity) {
        return renderLiftedEntity(entity, 0);
    }

    public boolean tryLiftBombIfRequested(
            int index,
            BombMotion.Decision decision,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int romLinkDirection) {
        if (decision.phase() != BombMotion.Phase.NORMAL || !runtime.linkState.bombButtonHeld) {
            return false;
        }
        RoomEntity entity = runtime.slots[index];
        if (!entity.loaded()
                || entity.type() != EntityRomConstants.ENTITY_BOMB
                || runtime.bombPrivateState4[entity.slot()] != 0
                || runtime.bombPrivateCountdown1[entity.slot()] != 0
                || !RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }
        if (!runtime.beginLift(entity.slot(), romLinkDirection)) {
            return false;
        }
        runtime.slots[index] = advanceLiftedEntity(
                renderLiftedBomb(runtime.slots[index]), linkEntityX, linkEntityY, linkZ, romLinkDirection);
        return true;
    }

    public boolean sideViewPotLiftDelay(int slot) {
        return runtime.liftedPhase[slot] == 0 && runtime.enemyTransitionCountdown[slot] != 0;
    }

    public boolean sideViewPotPowerBraceletHeld() {
        if (!runtime.linkState.linkInventoryConfigured) {
            return runtime.linkState.powerBraceletButtonHeld;
        }
        // EntityGetLiftedUp checks the B slot first. An equipped bracelet in
        // B suppresses the A-slot branch even when A also contains one.
        if (runtime.linkState.linkItemB == 0x03) {
            return runtime.linkState.actionButtonBHeld;
        }
        return runtime.linkState.linkItemA == 0x03 && runtime.linkState.actionButtonAHeld;
    }

    public void pendingLinkPotContact(int sourceSlot, SideViewPotContact.Result contact) {
        runtime.events.pendingSideViewPotLinkRequests.add(new SideViewPotLinkRequest(
                sourceSlot,
                contact.resetPegasusBoots(),
                contact.restoreFinalPositionX(),
                contact.ignoreCollisionCountdown(),
                contact.speedX(),
                contact.snapTop(),
                contact.positionY(),
                contact.speedY()));
    }

    public boolean beginLift(int slot, int direction) {
        return runtime.beginLift(slot, direction);
    }

    public void setLiftedSourceDirection(int slot, int value) {
        runtime.liftedSourceDirection[slot] = value;
    }

    public boolean liftableRockSmashActive(int slot) {
        return runtime.liftableRockSmashActive[slot];
    }
}
