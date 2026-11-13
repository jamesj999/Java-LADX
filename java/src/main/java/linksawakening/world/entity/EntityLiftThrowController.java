package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion.LiftedEntityFrame;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** ROM lift, carry and throw mechanics, sharing the owning room slot table. */
public final class EntityLiftThrowController {
    final EntityLiftState state;
    final boolean indoorRoom;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBombState bombInteractions;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityPresentationState presentationState;
    final EntityProjectileController projectiles;
    final EntitySlotProperties slotProperties;
    final GenieEntityController genie;
    final RoomEntity[] slots;
    final EntitySlotLifecycle lifecycle;
    final EntityPlayerCommands playerCommands;
    final EntityPlayerSpawns playerSpawns;

    EntityLiftThrowController(
            EntityLiftState state,
            boolean indoorRoom,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBombState bombInteractions,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileController projectiles,
            EntitySlotProperties slotProperties,
            GenieEntityController genie,
            RoomEntity[] slots,
            EntitySlotLifecycle lifecycle,
            EntityPlayerCommands playerCommands,
            EntityPlayerSpawns playerSpawns) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = spriteHandlers;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.genie = java.util.Objects.requireNonNull(genie, "genie");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
        this.playerSpawns = java.util.Objects.requireNonNull(playerSpawns, "playerSpawns");
    }

    public RoomEntity advanceLiftedEntity(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) {
        return EntityLiftThrowController.entityCarryBehaviorAdvanceLiftedEntity(
                this, entity, linkEntityX, linkEntityY, linkZ, romLinkDirection);
    }

    boolean advanceStatueRubble(RoomEntity entity, boolean interactive) {
        if (entity.type() != 0x9D || entity.spriteDefinition().shape() != EntitySpriteDefinition.Shape.SINGLE)
            return false;
        if (!interactive) return true;
        var update = this.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(entity, false, null);
        this.slots[entity.slot()] = update.entity().z() == 0 ? RoomEntity.disabled(entity.slot()) : update.entity();
        return true;
    }

    void smashStatue(RoomEntity entity) {
        EntityLiftThrowController.liftableObjectBehaviorSmashStatue(this, entity);
    }

    RoomEntity advanceThrownEntity(
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        return EntityLiftThrowController.thrownEntityCollisionsAdvanceThrownEntity(
                this, entity, backgroundCollision, sideScrolling, romLinkDirection, frame);
    }

    public boolean sideViewPotBackgroundCollision(
            RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) {
        return EntityLiftThrowController.thrownEntityCollisionsSideViewPotBackgroundCollision(
                this, original, moved, fallback, frame);
    }

    void collideThrownEntityWithEntities(RoomEntity thrown, int frame) {
        EntityLiftThrowController.thrownEntityCollisionsCollideThrownEntityWithEntities(this, thrown, frame);
    }

    public RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        return EntityLiftThrowController.liftableObjectBehaviorAdvancePlacedBombEntity(
                this, entity, backgroundCollision, frame);
    }

    public RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!this.bombInteractions.readEnemyBombMotionInitialized(slot)) {
            // Room-loaded type-$02 entities and legacy test-created enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        return this.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        entity,
                        this.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        this.physicsTables.readEntityGroundStatus(slot))
                .entity();
    }

    public boolean cuccoPowerBraceletHeld() {
        if (this.linkState.linkItemB == 0x03) {
            return this.linkState.actionButtonBHeld;
        }
        return this.linkState.linkItemA == 0x03 && this.linkState.actionButtonAHeld;
    }

    public boolean beginBushCrawlerLift(int index, RoomEntity source, int privateState4, int romLinkDirection) {
        return EntityLiftThrowController.entityCarryBehaviorBeginBushCrawlerLift(
                this, index, source, privateState4, romLinkDirection);
    }

    public void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        EntityLiftThrowController.liftableObjectBehaviorAdvanceLiftableRockSmash(this, index, entity, frameCounter);
    }

    public RoomEntity renderLiftedEntity(RoomEntity entity, int frameCounter) {
        if (entity.type() != EntityRomConstants.ENTITY_BOMB) {
            return entity.type() == EntityRomConstants.ENTITY_CUCCO
                    ? withVariant(entity, (frameCounter >>> 2) & 0x01)
                    : entity;
        }
        this.combatTables.writeEnemyFlashCountdown(entity.slot(), 0);
        return withDefinition(entity, this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB), 0);
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
        return EntityLiftThrowController.entityCarryBehaviorTryLiftBombIfRequested(
                this, index, decision, linkEntityX, linkEntityY, linkZ, romLinkDirection);
    }

    public boolean sideViewPotLiftDelay(int slot) {
        return state.readLiftedPhase(slot) == 0 && this.physicsTables.readEnemyTransitionCountdown(slot) != 0;
    }

    public boolean sideViewPotPowerBraceletHeld() {
        if (!this.linkState.linkInventoryConfigured) {
            return this.linkState.powerBraceletButtonHeld;
        }
        // EntityGetLiftedUp checks the B slot first. An equipped bracelet in
        // B suppresses the A-slot branch even when A also contains one.
        if (this.linkState.linkItemB == 0x03) {
            return this.linkState.actionButtonBHeld;
        }
        return this.linkState.linkItemA == 0x03 && this.linkState.actionButtonAHeld;
    }

    public void pendingLinkPotContact(int sourceSlot, SideViewPotContact.Result contact) {
        this.events.pendingSideViewPotLinkRequests.add(new SideViewPotLinkRequest(
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
        return this.playerCommands.beginLift(slot, direction);
    }

    public void setLiftedSourceDirection(int slot, int value) {
        state.writeLiftedSourceDirection(slot, value);
    }

    public boolean liftableRockSmashActive(int slot) {
        return state.readLiftableRockSmashActive(slot);
    }

    static RoomEntity entityCarryBehaviorAdvanceLiftedEntity(
            EntityLiftThrowController owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int romLinkDirection) {
        var runtime = owner;
        int slot = entity.slot();
        if (!owner.state.readLiftedStateInitialized(slot)) {
            owner.state.writeLiftedPhase(slot, 0);
            owner.state.writeLiftedSourceDirection(slot, romLinkDirection);
            owner.state.writeLiftedStateInitialized(slot, true);
            owner.state.writeLiftedEffectiveDirection(romLinkDirection);
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x02);
        }

        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(new LiftedEntityFrame(
                owner.state.readLiftedPhase(slot),
                runtime.physicsTables.readEnemyTransitionCountdown(slot),
                owner.state.readLiftedSourceDirection(slot),
                romLinkDirection,
                byteValue(linkEntityX),
                byteValue(linkEntityY),
                byteValue(linkZ),
                owner.state.readLiftedLinkC13B(),
                entity.type(),
                runtime.physicsTables.readGroundInteractionSideScrolling(),
                runtime.pickups.readChestPowerBraceletLevel() >= 2 || runtime.linkState.fastLiftTransition,
                entity.z()));
        owner.state.writeLiftedPhase(slot, update.phase());
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, update.transitionCountdown());
        owner.state.writeLiftedCarryState(update.carryState());
        owner.state.writeLiftedEffectiveDirection(update.effectiveDirection());
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

    static boolean entityCarryBehaviorBeginBushCrawlerLift(
            EntityLiftThrowController owner, int index, RoomEntity source, int privateState4, int romLinkDirection) {
        var runtime = owner;
        if (source.type() != EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            return false;
        }
        runtime.slots[index] = source;
        if (!runtime.playerCommands.beginLift(source.slot(), romLinkDirection)) {
            return false;
        }

        int slot = source.slot();
        RoomEntity carried = runtime.slots[slot];
        EntitySpriteDefinition rockDefinition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        carried = withType(carried, EntityRomConstants.ENTITY_LIFTABLE_ROCK, rockDefinition);
        carried = withVariant(carried, rockDefinition.supported() ? (privateState4 ^ 0x01) & 0x01 : -1);
        runtime.slots[slot] = carried;
        runtime.physicsTables.writeFallingVisualYOffset(slot, 0);
        runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, EntityRomConstants.ENTITY_BUSH_CRAWLER, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x02));

        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
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
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0x40);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables.writeEnemyHitboxFlags(
                freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables.writeFallingVisualYOffset(freeSlot, 0);
        runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new).initializeSpecial(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    static boolean entityCarryBehaviorTryLiftBombIfRequested(
            EntityLiftThrowController owner,
            int index,
            BombMotion.Decision decision,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int romLinkDirection) {
        var runtime = owner;
        if (decision.phase() != BombMotion.Phase.NORMAL || !runtime.linkState.bombButtonHeld) {
            return false;
        }
        RoomEntity entity = runtime.slots[index];
        if (!entity.loaded()
                || entity.type() != EntityRomConstants.ENTITY_BOMB
                || runtime.bombInteractions.readBombPrivateState4(entity.slot()) != 0
                || runtime.bombInteractions.readBombPrivateCountdown1(entity.slot()) != 0
                || !RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }
        if (!runtime.playerCommands.beginLift(entity.slot(), romLinkDirection)) {
            return false;
        }
        runtime.slots[index] = owner.advanceLiftedEntity(
                owner.renderLiftedBomb(runtime.slots[index]), linkEntityX, linkEntityY, linkZ, romLinkDirection);
        return true;
    }

    static void liftableObjectBehaviorSmashStatue(EntityLiftThrowController owner, RoomEntity entity) {
        var runtime = owner;
        // LiftableStatue_SpawnRubble ($19:4165-$41FE).
        int[] xs = {0, 8, 0, 8, 0, 8};
        int[] ys = {-8, -8, 0, 0, 8, 8};
        int[] vx = {0xFC, 5, 0xFA, 6, 0xFB, 4};
        int[] vy = {0xFC, 0xF8, 0xFE, 0xFF, 3, 2};
        int[] vz = {0x18, 0x14, 0x13, 0x16, 0x12, 0x14};
        if (runtime.spriteHandlers != null) {
            var definition = runtime.spriteHandlers.decodeSingle(0x9D, 0x19, 0x4020, 1, 0);
            for (int piece = 0; piece < 6; piece++) {
                int slot =
                        EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
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
                runtime.lifecycle.initializeHealthGroup(slot);
                runtime.physicsTables.writeEnemyPhysicsFlags(slot, 0xC1);
                runtime.combatTables.writeEnemyHitboxFlags(slot, 0);
                runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(slot, true);
                runtime.motions
                        .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                        .startEnemyBomb(slot, vx[piece], vy[piece], vz[piece]);
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

    static RoomEntity liftableObjectBehaviorAdvancePlacedBombEntity(
            EntityLiftThrowController owner,
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            int frame) {
        var runtime = owner;
        int slot = entity.slot();
        if (!runtime.bombInteractions.readPlacedBombMotionInitialized(slot)) {
            int direction = runtime.bombInteractions.readBombDirection(slot);
            if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
                    || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
                // A room-loaded type-$02 entity has no SpawnPlayerProjectile direction.
                // Preserve its static source state instead of inventing a table index.
                return entity;
            }
            runtime.motions
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .startPlacedBomb(slot, runtime.bombInteractions.readBombDirection(slot));
            runtime.bombInteractions.writePlacedBombMotionInitialized(slot, true);
        }
        return runtime.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        entity,
                        runtime.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        runtime.physicsTables.readEntityGroundStatus(slot))
                .entity();
    }

    static void liftableObjectBehaviorAdvanceLiftableRockSmash(
            EntityLiftThrowController owner, int index, RoomEntity entity, int frameCounter) {
        var runtime = owner;
        int slot = entity.slot();
        int countdown = owner.state.readLiftableRockSmashCountdown(slot) & 0xFF;
        if (countdown <= 1) {
            // LiftableRockEntityHandler unloads on the countdown-$01 frame,
            // before the display-list helper gets a chance to render it.
            runtime.lifecycle.clearEntity(slot);
            return;
        }

        int sourceVariant = owner.state.readLiftableRockSmashSourceVariant(slot);
        boolean swampLeaves = !runtime.indoorRoom
                && runtime.presentationState.readSpriteSelection() != null
                && runtime.presentationState.readSpriteSelection().roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.presentationState.readSpriteSelection().roomId() == 0x32;
        int variant = EntityMotionMath.liftableRockSmashVariant(sourceVariant, swampLeaves, countdown);
        if (sourceVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS && ((frameCounter ^ slot) & 0x01) != 0) {
            // The $FF tall-grass path in func_019_7C50 only emits OAM on
            // alternating frames. -1 is the same hidden-display sentinel
            // consumed by RenderActiveEntitySpritesRect.
            variant = -1;
        }
        runtime.slots[index] = withVariant(entity, variant);
    }

    static RoomEntity thrownEntityCollisionsAdvanceThrownEntity(
            EntityLiftThrowController owner,
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        var runtime = owner;
        int slot = entity.slot();
        if (!owner.state.readThrownMotionInitialized(slot)) {
            owner.state.writeThrownDirection(slot, romLinkDirection);
            runtime.motions
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .start(slot, romLinkDirection, entity.type(), sideScrolling);
            owner.state.writeThrownMotionInitialized(slot, true);
        }
        ThrownEntityMotion.Update update = runtime.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        entity,
                        sideScrolling,
                        backgroundCollision,
                        frame,
                        runtime.physicsTables.readEntityGroundStatus(slot));
        RoomEntity updated = update.entity();
        if (entity.type() == EntityRomConstants.ENTITY_GENIE && (update.blockedX() || update.blockedY())) {
            updated = runtime.genie.registerGenieJarImpact(updated);
            if (updated.status() == EntityStatus.ACTIVE) {
                return updated;
            }
        }
        if (entity.type() == 0x9D && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            runtime.slots[slot] = RoomEntity.disabled(slot);
            runtime.motions
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            owner.state.writeThrownMotionInitialized(slot, false);
            owner.smashStatue(updated);
            return runtime.slots[slot];
        }
        if (entity.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            owner.collideThrownEntityWithEntities(updated, frame);
            runtime.slots[slot] = RoomEntity.disabled(slot);
            runtime.motions
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            owner.state.writeThrownMotionInitialized(slot, false);
            runtime.playerSpawns.spawnLiftableRockSmash(updated.x(), updated.y(), entity.spriteVariant());
            return runtime.slots[slot];
        }
        if (runtime.motions
                                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                                .speedX(slot)
                        == 0
                && runtime.motions
                                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                                .speedY(slot)
                        == 0) {
            runtime.motions
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            owner.state.writeThrownMotionInitialized(slot, false);
            runtime.combatTables.writeEnemyStunnedCountdown(slot, 0xFF);
            updated = withStatus(updated, EntityStatus.STUNNED);
        }
        return updated;
    }

    static boolean thrownEntityCollisionsSideViewPotBackgroundCollision(
            EntityLiftThrowController owner,
            RoomEntity original,
            RoomEntity moved,
            RoomEntityBackgroundCollision fallback,
            int frame) {
        var runtime = owner;
        int slot = original.slot();
        boolean blocked = false;
        if (runtime.motions.get(SideViewPotMotion.class, SideViewPotMotion::new).speedX(slot) != 0) {
            int direction = (runtime.motions
                                            .get(SideViewPotMotion.class, SideViewPotMotion::new)
                                            .speedX(slot)
                                    & 0x80)
                            != 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            if (runtime.physicsTables.readBackgroundInteraction() != null) {
                blocked |= runtime.physicsTables
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                original.y(),
                                runtime.combatTables.readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), original.y());
            }
        }
        if (runtime.motions.get(SideViewPotMotion.class, SideViewPotMotion::new).speedY(slot) != 0) {
            int direction = (runtime.motions
                                            .get(SideViewPotMotion.class, SideViewPotMotion::new)
                                            .speedY(slot)
                                    & 0x80)
                            != 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            if (runtime.physicsTables.readBackgroundInteraction() != null) {
                blocked |= runtime.physicsTables
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                moved.y(),
                                runtime.combatTables.readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), moved.y());
            }
        }
        return blocked;
    }

    static void thrownEntityCollisionsCollideThrownEntityWithEntities(
            EntityLiftThrowController owner, RoomEntity thrown, int frame) {
        var runtime = owner;
        int sourceSlot = thrown.slot();
        int sourceVisualY = (thrown.y() - thrown.z()) & 0xFF;
        for (int targetSlot = runtime.slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }
            RoomEntity target = runtime.slots[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || target.spriteVariant() < 0
                    || unsignedByteAbs(thrown.x() - target.x()) >= 0x0C
                    || unsignedByteAbs(sourceVisualY - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                    || (runtime.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || runtime.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            runtime.projectiles.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_THROW_AT,
                    runtime.motions
                            .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions
                            .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                            .speedY(sourceSlot));
        }
    }
}
