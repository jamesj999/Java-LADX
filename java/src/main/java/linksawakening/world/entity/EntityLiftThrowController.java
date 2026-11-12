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
    private final int[] liftedPhase = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftedSourceDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] liftedStateInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftableRockSmashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftableRockSmashSourceVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] liftableRockSmashActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] thrownDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] ledgeTransitionTimer = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] thrownMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int liftedEntitySlot = -1;
    private int liftedCarryState;
    private int liftedEffectiveDirection;
    private int liftedLinkC13B;
    private final EntityLiftThrowControllerServices runtime;

    EntityLiftThrowController(EntityLiftThrowControllerServices runtime) {
        this.runtime = runtime;
    }

    public RoomEntity advanceLiftedEntity(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) {
        int slot = entity.slot();
        if (!this.readLiftedStateInitialized(slot)) {
            this.writeLiftedPhase(slot, 0);
            this.writeLiftedSourceDirection(slot, romLinkDirection);
            this.writeLiftedStateInitialized(slot, true);
            this.writeLiftedEffectiveDirection(romLinkDirection);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x02);
        }

        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
                this.readLiftedPhase(slot),
                runtime.physicsTables().readEnemyTransitionCountdown(slot),
                this.readLiftedSourceDirection(slot),
                romLinkDirection,
                byteValue(linkEntityX),
                byteValue(linkEntityY),
                byteValue(linkZ),
                this.readLiftedLinkC13B(),
                entity.type(),
                runtime.physicsTables().readGroundInteractionSideScrolling(),
                runtime.pickups().readChestPowerBraceletLevel() >= 2 || runtime.linkState().fastLiftTransition,
                entity.z());
        this.writeLiftedPhase(slot, update.phase());
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, update.transitionCountdown());
        this.writeLiftedCarryState(update.carryState());
        this.writeLiftedEffectiveDirection(update.effectiveDirection());
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
        var update = runtime.motionSet().thrownEntityMotion.advance(entity, false, null);
        runtime.slots()[entity.slot()] =
                update.entity().z() == 0 ? RoomEntity.disabled(entity.slot()) : update.entity();
        return true;
    }

    private void smashStatue(RoomEntity entity) {
        // LiftableStatue_SpawnRubble ($19:4165-$41FE).
        int[] xs = {0, 8, 0, 8, 0, 8};
        int[] ys = {-8, -8, 0, 0, 8, 8};
        int[] vx = {0xFC, 5, 0xFA, 6, 0xFB, 4};
        int[] vy = {0xFC, 0xF8, 0xFE, 0xFF, 3, 2};
        int[] vz = {0x18, 0x14, 0x13, 0x16, 0x12, 0x14};
        if (runtime.spriteHandlers() != null) {
            var definition = runtime.spriteHandlers().decodeSingle(0x9D, 0x19, 0x4020, 1, 0);
            for (int piece = 0; piece < 6; piece++) {
                int slot = runtime.findFreeEntitySlot();
                if (slot < 0) break;
                runtime.slots()[slot] = new RoomEntity(
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
                runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0xC1);
                runtime.combatTables().writeEnemyHitboxFlags(slot, 0);
                runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(slot, true);
                runtime.motionSet().thrownEntityMotion.startEnemyBomb(slot, vx[piece], vy[piece], vz[piece]);
            }
        }
        int visualY = (entity.y() - entity.z()) & 255;
        runtime.events()
                .transientVfxRequests
                .add(new linksawakening.world.entity.event.TransientVfxRequest(
                        linksawakening.vfx.TransientVfxType.POOF, entity.x(), visualY));
        runtime.events()
                .transientVfxRequests
                .add(new linksawakening.world.entity.event.TransientVfxRequest(
                        linksawakening.vfx.TransientVfxType.POOF, entity.x(), (visualY - 16) & 255));
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(entity.slot(), 0x9D, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x29));
    }

    RoomEntity advanceThrownEntity(
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        int slot = entity.slot();
        if (!this.readThrownMotionInitialized(slot)) {
            this.writeThrownDirection(slot, romLinkDirection);
            runtime.motionSet().thrownEntityMotion.start(slot, romLinkDirection, entity.type(), sideScrolling);
            this.writeThrownMotionInitialized(slot, true);
        }
        ThrownEntityMotion.Update update = runtime.motionSet()
                .thrownEntityMotion
                .advance(
                        entity,
                        sideScrolling,
                        backgroundCollision,
                        frame,
                        runtime.physicsTables().readEntityGroundStatus(slot));
        RoomEntity updated = update.entity();
        if (entity.type() == EntityRomConstants.ENTITY_GENIE && (update.blockedX() || update.blockedY())) {
            updated = runtime.genie().registerGenieJarImpact(updated);
            if (updated.status() == EntityStatus.ACTIVE) {
                return updated;
            }
        }
        if (entity.type() == 0x9D && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            runtime.slots()[slot] = RoomEntity.disabled(slot);
            runtime.motionSet().thrownEntityMotion.clear(slot);
            this.writeThrownMotionInitialized(slot, false);
            smashStatue(updated);
            return runtime.slots()[slot];
        }
        if (entity.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            collideThrownEntityWithEntities(updated, frame);
            runtime.slots()[slot] = RoomEntity.disabled(slot);
            runtime.motionSet().thrownEntityMotion.clear(slot);
            this.writeThrownMotionInitialized(slot, false);
            runtime.spawnLiftableRockSmash(updated.x(), updated.y(), entity.spriteVariant());
            return runtime.slots()[slot];
        }
        if (runtime.motionSet().thrownEntityMotion.speedX(slot) == 0
                && runtime.motionSet().thrownEntityMotion.speedY(slot) == 0) {
            runtime.motionSet().thrownEntityMotion.clear(slot);
            this.writeThrownMotionInitialized(slot, false);
            runtime.combatTables().writeEnemyStunnedCountdown(slot, 0xFF);
            updated = withStatus(updated, EntityStatus.STUNNED);
        }
        return updated;
    }

    public boolean sideViewPotBackgroundCollision(
            RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) {
        int slot = original.slot();
        boolean blocked = false;
        if (runtime.motionSet().sideViewPotMotion.speedX(slot) != 0) {
            int direction = (runtime.motionSet().sideViewPotMotion.speedX(slot) & 0x80) != 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            if (runtime.physicsTables().readBackgroundInteraction() != null) {
                blocked |= runtime.physicsTables()
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                original.y(),
                                runtime.combatTables().readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), original.y());
            }
        }
        if (runtime.motionSet().sideViewPotMotion.speedY(slot) != 0) {
            int direction = (runtime.motionSet().sideViewPotMotion.speedY(slot) & 0x80) != 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            if (runtime.physicsTables().readBackgroundInteraction() != null) {
                blocked |= runtime.physicsTables()
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                moved.y(),
                                runtime.combatTables().readEnemyIgnoreHitsCountdown(slot),
                                frame)
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
                    || target.spriteVariant() < 0
                    || unsignedByteAbs(thrown.x() - target.x()) >= 0x0C
                    || unsignedByteAbs(sourceVisualY - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                    || (runtime.combatTables().readEnemyHitboxFlags(targetSlot)
                                    & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            runtime.projectiles()
                    .applyPlayerProjectileDamage(
                            target,
                            EntityRomConstants.DAMAGE_TYPE_THROW_AT,
                            runtime.motionSet().thrownEntityMotion.speedX(sourceSlot),
                            runtime.motionSet().thrownEntityMotion.speedY(sourceSlot));
        }
    }

    public RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!runtime.bombInteractions().readPlacedBombMotionInitialized(slot)) {
            int direction = runtime.bombInteractions().readBombDirection(slot);
            if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
                    || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
                // A room-loaded type-$02 entity has no SpawnPlayerProjectile direction.
                // Preserve its static source state instead of inventing a table index.
                return entity;
            }
            runtime.motionSet()
                    .thrownEntityMotion
                    .startPlacedBomb(slot, runtime.bombInteractions().readBombDirection(slot));
            runtime.bombInteractions().writePlacedBombMotionInitialized(slot, true);
        }
        return runtime.motionSet()
                .thrownEntityMotion
                .advance(
                        entity,
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        runtime.physicsTables().readEntityGroundStatus(slot))
                .entity();
    }

    public RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!runtime.bombInteractions().readEnemyBombMotionInitialized(slot)) {
            // Room-loaded type-$02 entities and legacy test-created enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        return runtime.motionSet()
                .thrownEntityMotion
                .advance(
                        entity,
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        runtime.physicsTables().readEntityGroundStatus(slot))
                .entity();
    }

    public boolean cuccoPowerBraceletHeld() {
        if (runtime.linkState().linkItemB == 0x03) {
            return runtime.linkState().actionButtonBHeld;
        }
        return runtime.linkState().linkItemA == 0x03 && runtime.linkState().actionButtonAHeld;
    }

    public boolean beginBushCrawlerLift(int index, RoomEntity source, int privateState4, int romLinkDirection) {
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
        runtime.motionSet().bushCrawlerMotion.clear(slot);
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
        runtime.motionSet().enemyRecoilMotion.clear(freeSlot);
        runtime.physicsTables().writeFallingVisualYOffset(freeSlot, 0);
        runtime.motionSet().bushCrawlerMotion.initializeSpecial(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    public void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        int slot = entity.slot();
        int countdown = this.readLiftableRockSmashCountdown(slot) & 0xFF;
        if (countdown <= 1) {
            // LiftableRockEntityHandler unloads on the countdown-$01 frame,
            // before the display-list helper gets a chance to render it.
            runtime.clearEntity(slot);
            return;
        }

        int sourceVariant = this.readLiftableRockSmashSourceVariant(slot);
        boolean swampLeaves = !runtime.indoorRoom()
                && runtime.presentationState().readSpriteSelection() != null
                && runtime.presentationState().readSpriteSelection().roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.presentationState().readSpriteSelection().roomId() == 0x32;
        int variant = EntityMotionMath.liftableRockSmashVariant(sourceVariant, swampLeaves, countdown);
        if (sourceVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS && ((frameCounter ^ slot) & 0x01) != 0) {
            // The $FF tall-grass path in func_019_7C50 only emits OAM on
            // alternating frames. -1 is the same hidden-display sentinel
            // consumed by RenderActiveEntitySpritesRect.
            variant = -1;
        }
        runtime.slots()[index] = withVariant(entity, variant);
    }

    public RoomEntity renderLiftedEntity(RoomEntity entity, int frameCounter) {
        if (entity.type() != EntityRomConstants.ENTITY_BOMB) {
            return entity.type() == EntityRomConstants.ENTITY_CUCCO
                    ? withVariant(entity, (frameCounter >>> 2) & 0x01)
                    : entity;
        }
        runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0);
        return withDefinition(entity, runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB), 0);
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
        runtime.slots()[index] = advanceLiftedEntity(
                renderLiftedBomb(runtime.slots()[index]), linkEntityX, linkEntityY, linkZ, romLinkDirection);
        return true;
    }

    public boolean sideViewPotLiftDelay(int slot) {
        return this.readLiftedPhase(slot) == 0 && runtime.physicsTables().readEnemyTransitionCountdown(slot) != 0;
    }

    public boolean sideViewPotPowerBraceletHeld() {
        if (!runtime.linkState().linkInventoryConfigured) {
            return runtime.linkState().powerBraceletButtonHeld;
        }
        // EntityGetLiftedUp checks the B slot first. An equipped bracelet in
        // B suppresses the A-slot branch even when A also contains one.
        if (runtime.linkState().linkItemB == 0x03) {
            return runtime.linkState().actionButtonBHeld;
        }
        return runtime.linkState().linkItemA == 0x03 && runtime.linkState().actionButtonAHeld;
    }

    public void pendingLinkPotContact(int sourceSlot, SideViewPotContact.Result contact) {
        runtime.events()
                .pendingSideViewPotLinkRequests
                .add(new SideViewPotLinkRequest(
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
        this.writeLiftedSourceDirection(slot, value);
    }

    public boolean liftableRockSmashActive(int slot) {
        return this.readLiftableRockSmashActive(slot);
    }

    int readLiftedPhase(int slot) {
        return liftedPhase[slot];
    }

    int writeLiftedPhase(int slot, int value) {
        return liftedPhase[slot] = value;
    }

    int readLiftedSourceDirection(int slot) {
        return liftedSourceDirection[slot];
    }

    int writeLiftedSourceDirection(int slot, int value) {
        return liftedSourceDirection[slot] = value;
    }

    boolean readLiftedStateInitialized(int slot) {
        return liftedStateInitialized[slot];
    }

    boolean writeLiftedStateInitialized(int slot, boolean value) {
        return liftedStateInitialized[slot] = value;
    }

    int readLiftableRockSmashCountdown(int slot) {
        return liftableRockSmashCountdown[slot];
    }

    int writeLiftableRockSmashCountdown(int slot, int value) {
        return liftableRockSmashCountdown[slot] = value;
    }

    int postLiftableRockSmashCountdown(int slot, int delta) {
        int previous = liftableRockSmashCountdown[slot];
        liftableRockSmashCountdown[slot] += delta;
        return previous;
    }

    int readLiftableRockSmashSourceVariant(int slot) {
        return liftableRockSmashSourceVariant[slot];
    }

    int writeLiftableRockSmashSourceVariant(int slot, int value) {
        return liftableRockSmashSourceVariant[slot] = value;
    }

    boolean readLiftableRockSmashActive(int slot) {
        return liftableRockSmashActive[slot];
    }

    boolean writeLiftableRockSmashActive(int slot, boolean value) {
        return liftableRockSmashActive[slot] = value;
    }

    int readThrownDirection(int slot) {
        return thrownDirection[slot];
    }

    int writeThrownDirection(int slot, int value) {
        return thrownDirection[slot] = value;
    }

    int[] storageThrownDirection() {
        return thrownDirection;
    }

    int readLedgeTransitionTimer(int slot) {
        return ledgeTransitionTimer[slot];
    }

    int writeLedgeTransitionTimer(int slot, int value) {
        return ledgeTransitionTimer[slot] = value;
    }

    boolean readThrownMotionInitialized(int slot) {
        return thrownMotionInitialized[slot];
    }

    boolean writeThrownMotionInitialized(int slot, boolean value) {
        return thrownMotionInitialized[slot] = value;
    }

    int readLiftedEntitySlot() {
        return liftedEntitySlot;
    }

    int writeLiftedEntitySlot(int value) {
        return liftedEntitySlot = value;
    }

    int readLiftedCarryState() {
        return liftedCarryState;
    }

    int writeLiftedCarryState(int value) {
        return liftedCarryState = value;
    }

    int readLiftedEffectiveDirection() {
        return liftedEffectiveDirection;
    }

    int writeLiftedEffectiveDirection(int value) {
        return liftedEffectiveDirection = value;
    }

    int readLiftedLinkC13B() {
        return liftedLinkC13B;
    }

    int writeLiftedLinkC13B(int value) {
        return liftedLinkC13B = value;
    }
}
