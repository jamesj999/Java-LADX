package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;
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
    final EntityLiftThrowControllerServices runtime;

    EntityLiftThrowController(EntityLiftThrowControllerServices runtime) {
        this.runtime = runtime;
    }

    public RoomEntity advanceLiftedEntity(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) {
        return EntityCarryBehavior.advanceLiftedEntity(this, entity, linkEntityX, linkEntityY, linkZ, romLinkDirection);
    }

    boolean advanceStatueRubble(RoomEntity entity, boolean interactive) {
        if (entity.type() != 0x9D || entity.spriteDefinition().shape() != EntitySpriteDefinition.Shape.SINGLE)
            return false;
        if (!interactive) return true;
        var update = runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(entity, false, null);
        runtime.slots()[entity.slot()] =
                update.entity().z() == 0 ? RoomEntity.disabled(entity.slot()) : update.entity();
        return true;
    }

    void smashStatue(RoomEntity entity) {
        LiftableObjectBehavior.smashStatue(this, entity);
    }

    RoomEntity advanceThrownEntity(
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        return ThrownEntityCollisions.advanceThrownEntity(
                this, entity, backgroundCollision, sideScrolling, romLinkDirection, frame);
    }

    public boolean sideViewPotBackgroundCollision(
            RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) {
        return ThrownEntityCollisions.sideViewPotBackgroundCollision(this, original, moved, fallback, frame);
    }

    void collideThrownEntityWithEntities(RoomEntity thrown, int frame) {
        ThrownEntityCollisions.collideThrownEntityWithEntities(this, thrown, frame);
    }

    public RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        return LiftableObjectBehavior.advancePlacedBombEntity(this, entity, backgroundCollision, frame);
    }

    public RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!runtime.bombInteractions().readEnemyBombMotionInitialized(slot)) {
            // Room-loaded type-$02 entities and legacy test-created enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        return runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
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
        return EntityCarryBehavior.beginBushCrawlerLift(this, index, source, privateState4, romLinkDirection);
    }

    public void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        LiftableObjectBehavior.advanceLiftableRockSmash(this, index, entity, frameCounter);
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
        return EntityCarryBehavior.tryLiftBombIfRequested(
                this, index, decision, linkEntityX, linkEntityY, linkZ, romLinkDirection);
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
