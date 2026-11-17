package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.byteValue;
import static linksawakening.world.entity.EntityMotionMath.unsignedByteAbs;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withType;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntityTypeRules.initialHitboxFlags;
import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;

import java.util.Objects;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.BushCrawlerMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion.LiftedEntityFrame;
import linksawakening.world.entity.motion.SideViewPotMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** ROM lift, carry and throw mechanics, sharing the owning room slot table. */
public final class EntityLiftThrowController {
    private final EntityLiftState state;
    private final boolean indoorRoom;
    private final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    private final EntityBombState bombInteractions;
    private final EntityCombatTables combatTables;
    private final EntityEventChannels events;
    private final EntityLinkState linkState;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntityPickupState pickups;
    private final EntityPresentationState presentationState;
    private final EntityProjectileController projectiles;
    private final EntitySlotProperties slotProperties;
    private final GenieEntityController genie;
    private final EntitySlots slots;
    private final EntitySlotLifecycle lifecycle;
    private final EntityPlayerCommands playerCommands;
    private final EntityPlayerSpawns playerSpawns;

    EntityLiftThrowController(
            EntityLiftState state,
            boolean indoorRoom,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBombState bombInteractions,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileController projectiles,
            EntitySlotProperties slotProperties,
            GenieEntityController genie,
            EntitySlots slots,
            EntitySlotLifecycle lifecycle,
            EntityPlayerCommands playerCommands,
            EntityPlayerSpawns playerSpawns) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.state = Objects.requireNonNull(state, "state");
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = Objects.requireNonNull(projectiles, "projectiles");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.genie = Objects.requireNonNull(genie, "genie");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.playerCommands = Objects.requireNonNull(playerCommands, "playerCommands");
        this.playerSpawns = Objects.requireNonNull(playerSpawns, "playerSpawns");
    }

    public RoomEntity advanceLiftedEntity(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) {
        int slot = entity.slot();
        if (!this.state.readLiftedStateInitialized(slot)) {
            this.state.writeLiftedPhase(slot, 0);
            this.state.writeLiftedSourceDirection(slot, romLinkDirection);
            this.state.writeLiftedStateInitialized(slot, true);
            this.state.writeLiftedEffectiveDirection(romLinkDirection);
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0x02);
        }

        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(new LiftedEntityFrame(
                this.state.readLiftedPhase(slot),
                this.physicsTables.readEnemyTransitionCountdown(slot),
                this.state.readLiftedSourceDirection(slot),
                romLinkDirection,
                byteValue(linkEntityX),
                byteValue(linkEntityY),
                byteValue(linkZ),
                this.state.readLiftedLinkC13B(),
                entity.type(),
                this.physicsTables.readGroundInteractionSideScrolling(),
                this.pickups.readChestPowerBraceletLevel() >= 2 || this.linkState.readFastLiftTransition(),
                entity.z()));
        this.state.writeLiftedPhase(slot, update.phase());
        this.physicsTables.writeEnemyTransitionCountdown(slot, update.transitionCountdown());
        this.state.writeLiftedCarryState(update.carryState());
        this.state.writeLiftedEffectiveDirection(update.effectiveDirection());
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
        var update = this.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(entity, false, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN);
        this.slots.replace(
                entity.slot(), update.entity().z() == 0 ? RoomEntity.disabled(entity.slot()) : update.entity());
        return true;
    }

    void smashStatue(RoomEntity entity) {
        // LiftableStatue_SpawnRubble ($19:4165-$41FE).
        int[] xs = {0, 8, 0, 8, 0, 8};
        int[] ys = {-8, -8, 0, 0, 8, 8};
        int[] vx = {0xFC, 5, 0xFA, 6, 0xFB, 4};
        int[] vy = {0xFC, 0xF8, 0xFE, 0xFF, 3, 2};
        int[] vz = {0x18, 0x14, 0x13, 0x16, 0x12, 0x14};
        if (this.spriteHandlers.supportsPresentation()) {
            var definition = this.spriteHandlers.decodeSingle(0x9D, 0x19, 0x4020, 1, 0);
            for (int piece = 0; piece < 6; piece++) {
                int slot = this.slots.findFree(this.slots.size() - 1);
                if (slot < 0) break;
                this.slots.replace(
                        slot,
                        new RoomEntity(
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
                                false));
                this.lifecycle.initializeHealthGroup(slot);
                this.physicsTables.writeEnemyPhysicsFlags(slot, 0xC1);
                this.combatTables.writeEnemyHitboxFlags(slot, 0);
                this.physicsTables.writeDynamicEntitySpawnedThisFrame(slot, true);
                var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
                thrownEntityMotion.startEnemyBomb(slot, vx[piece], vy[piece], vz[piece]);
            }
        }
        int visualY = (entity.y() - entity.z()) & 255;
        this.events.vfx(new linksawakening.world.entity.event.TransientVfxRequest(
                linksawakening.vfx.TransientVfxType.POOF, entity.x(), visualY));
        this.events.vfx(new linksawakening.world.entity.event.TransientVfxRequest(
                linksawakening.vfx.TransientVfxType.POOF, entity.x(), (visualY - 16) & 255));
        this.events.sound(entity.slot(), 0x9D, EntityCombatEvent.SoundChannel.NOISE, 0x29);
    }

    RoomEntity advanceThrownEntity(
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        var resolvedThrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);

        int slot = entity.slot();
        if (!this.state.readThrownMotionInitialized(slot)) {
            this.state.writeThrownDirection(slot, romLinkDirection);

            resolvedThrownEntityMotion.start(slot, romLinkDirection, entity.type(), sideScrolling);
            this.state.writeThrownMotionInitialized(slot, true);
        }

        ThrownEntityMotion.Update update = resolvedThrownEntityMotion.advance(
                entity, sideScrolling, backgroundCollision, frame, this.physicsTables.readEntityGroundStatus(slot));
        RoomEntity updated = update.entity();
        if (entity.type() == EntityRomConstants.ENTITY_GENIE && (update.blockedX() || update.blockedY())) {
            updated = this.genie.registerGenieJarImpact(updated);
            if (updated.status() == EntityStatus.ACTIVE) {
                return updated;
            }
        }
        if (entity.type() == 0x9D && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            this.slots.clear(slot);
            resolvedThrownEntityMotion.clear(slot);
            this.state.writeThrownMotionInitialized(slot, false);
            this.smashStatue(updated);
            return this.slots.get(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            this.collideThrownEntityWithEntities(updated, frame);
            this.slots.clear(slot);
            resolvedThrownEntityMotion.clear(slot);
            this.state.writeThrownMotionInitialized(slot, false);
            this.playerSpawns.spawnLiftableRockSmash(updated.x(), updated.y(), entity.spriteVariant());
            return this.slots.get(slot);
        }
        if (resolvedThrownEntityMotion.speedX(slot) == 0 && resolvedThrownEntityMotion.speedY(slot) == 0) {
            resolvedThrownEntityMotion.clear(slot);
            this.state.writeThrownMotionInitialized(slot, false);
            this.combatTables.writeEnemyStunnedCountdown(slot, 0xFF);
            updated = withStatus(updated, EntityStatus.STUNNED);
        }
        return updated;
    }

    public boolean sideViewPotBackgroundCollision(
            RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) {
        fallback = RoomEntityBackgroundCollision.orOpen(fallback);
        int slot = original.slot();
        boolean blocked = false;
        var sideViewPotMotion = this.motions.get(SideViewPotMotion.class, SideViewPotMotion::new);
        if (sideViewPotMotion.speedX(slot) != 0) {
            int direction = (sideViewPotMotion.speedX(slot) & 0x80) != 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            if (this.physicsTables.hasRichBackgroundInteraction()) {
                blocked |= this.physicsTables
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                original.y(),
                                this.combatTables.readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else {
                blocked |= fallback.blocks(original, direction, moved.x(), original.y());
            }
        }
        if (sideViewPotMotion.speedY(slot) != 0) {
            int direction = (sideViewPotMotion.speedY(slot) & 0x80) != 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            if (this.physicsTables.hasRichBackgroundInteraction()) {
                blocked |= this.physicsTables
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                moved.y(),
                                this.combatTables.readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else {
                blocked |= fallback.blocks(original, direction, moved.x(), moved.y());
            }
        }
        return blocked;
    }

    void collideThrownEntityWithEntities(RoomEntity thrown, int frame) {
        int sourceSlot = thrown.slot();
        int sourceVisualY = (thrown.y() - thrown.z()) & 0xFF;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }
            RoomEntity target = this.slots.get(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (this.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || target.spriteVariant() < 0
                    || unsignedByteAbs(thrown.x() - target.x()) >= 0x0C
                    || unsignedByteAbs(sourceVisualY - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                    || (this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
            this.projectiles.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_THROW_AT,
                    thrownEntityMotion.speedX(sourceSlot),
                    thrownEntityMotion.speedY(sourceSlot));
        }
    }

    public RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        var resolvedThrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);

        int slot = entity.slot();
        if (!this.bombInteractions.readPlacedBombMotionInitialized(slot)) {
            int direction = this.bombInteractions.readBombDirection(slot);
            if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
                    || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
                // A room-loaded type-$02 entity has no SpawnPlayerProjectile direction.
                // Preserve its static source state instead of inventing a table index.
                return entity;
            }

            resolvedThrownEntityMotion.startPlacedBomb(slot, this.bombInteractions.readBombDirection(slot));
            this.bombInteractions.writePlacedBombMotionInitialized(slot, true);
        }

        return resolvedThrownEntityMotion
                .advance(
                        entity,
                        this.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        this.physicsTables.readEntityGroundStatus(slot))
                .entity();
    }

    public RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!this.bombInteractions.readEnemyBombMotionInitialized(slot)) {
            // Room-loaded type-$02 entities and uninitialized enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        return thrownEntityMotion
                .advance(
                        entity,
                        this.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        this.physicsTables.readEntityGroundStatus(slot))
                .entity();
    }

    public boolean cuccoPowerBraceletHeld() {
        if (this.linkState.readLinkItemB() == 0x03) {
            return this.linkState.readActionButtonBHeld();
        }
        return this.linkState.readLinkItemA() == 0x03 && this.linkState.readActionButtonAHeld();
    }

    public boolean beginBushCrawlerLift(int index, RoomEntity source, int privateState4, int romLinkDirection) {
        if (source.type() != EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            return false;
        }
        this.slots.replace(index, source);
        if (!this.playerCommands.beginLift(source.slot(), romLinkDirection)) {
            return false;
        }

        int slot = source.slot();
        RoomEntity carried = this.slots.get(slot);
        EntitySpriteDefinition rockDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        carried = withType(carried, EntityRomConstants.ENTITY_LIFTABLE_ROCK, rockDefinition);
        carried = withVariant(carried, rockDefinition.supported() ? (privateState4 ^ 0x01) & 0x01 : -1);
        this.slots.replace(slot, carried);
        this.physicsTables.writeFallingVisualYOffset(slot, 0);
        var bushCrawlerMotion = this.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new);
        bushCrawlerMotion.clear(slot);
        this.events.sound(slot, EntityRomConstants.ENTITY_BUSH_CRAWLER, EntityCombatEvent.SoundChannel.WAVE, 0x02);

        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return true;
        }
        EntitySpriteDefinition crawlerDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BUSH_CRAWLER);
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
        this.slots.replace(freeSlot, replacement);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0x40);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_BUSH_CRAWLER));
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.physicsTables.writeFallingVisualYOffset(freeSlot, 0);
        bushCrawlerMotion.initializeSpecial(freeSlot);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    public void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        int slot = entity.slot();
        int countdown = this.state.readLiftableRockSmashCountdown(slot) & 0xFF;
        if (countdown <= 1) {
            // LiftableRockEntityHandler unloads on the countdown-$01 frame,
            // before the display-list helper gets a chance to render it.
            this.lifecycle.clearEntity(slot);
            return;
        }

        int sourceVariant = this.state.readLiftableRockSmashSourceVariant(slot);
        boolean swampLeaves = !this.indoorRoom
                && this.presentationState.readSpriteSelection().roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && this.presentationState.readSpriteSelection().roomId() == 0x32;
        int variant = EntityMotionMath.liftableRockSmashVariant(sourceVariant, swampLeaves, countdown);
        if (sourceVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS && ((frameCounter ^ slot) & 0x01) != 0) {
            // The $FF tall-grass path in func_019_7C50 only emits OAM on
            // alternating frames. -1 is the same hidden-display sentinel
            // consumed by RenderActiveEntitySpritesRect.
            variant = -1;
        }
        this.slots.replace(index, withVariant(entity, variant));
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

    public boolean tryLiftBombIfRequested(int index, BombMotion.Decision decision, EntityFrameInputLatch inputs) {
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkZ = inputs.linkZ();
        int romLinkDirection = inputs.romLinkDirection();
        if (decision.phase() != BombMotion.Phase.NORMAL || !this.linkState.readBombButtonHeld()) {
            return false;
        }
        RoomEntity entity = this.slots.get(index);
        if (!entity.loaded()
                || entity.type() != EntityRomConstants.ENTITY_BOMB
                || this.bombInteractions.readBombOrigin(entity.slot()) != 0
                || this.bombInteractions.readBombCollisionGraceCountdown(entity.slot()) != 0
                || !RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }
        if (!this.playerCommands.beginLift(entity.slot(), romLinkDirection)) {
            return false;
        }
        this.slots.replace(
                index,
                this.advanceLiftedEntity(
                        this.renderLiftedBomb(this.slots.get(index)),
                        linkEntityX,
                        linkEntityY,
                        linkZ,
                        romLinkDirection));
        return true;
    }

    public boolean sideViewPotLiftDelay(int slot) {
        return state.readLiftedPhase(slot) == 0 && this.physicsTables.readEnemyTransitionCountdown(slot) != 0;
    }

    public boolean sideViewPotPowerBraceletHeld() {
        if (!this.linkState.readLinkInventoryConfigured()) {
            return this.linkState.readPowerBraceletButtonHeld();
        }
        // EntityGetLiftedUp checks the B slot first. An equipped bracelet in
        // B suppresses the A-slot branch even when A also contains one.
        if (this.linkState.readLinkItemB() == 0x03) {
            return this.linkState.readActionButtonBHeld();
        }
        return this.linkState.readLinkItemA() == 0x03 && this.linkState.readActionButtonAHeld();
    }

    public void pendingLinkPotContact(int sourceSlot, SideViewPotContact.Result contact) {
        this.events.sideViewPotLink(new SideViewPotLinkRequest(
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
}
