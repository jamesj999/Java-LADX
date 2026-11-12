package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.FloatingItemMotion;
import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.entity.motion.KeyDropPointMotion;
import linksawakening.world.room.ChestContentsTable;

/** Reused, reentrancy-safe scratch for EntitySlotLifecycle.collectIfNeeded; values are assigned in source order. */
final class EntitySlotLifecycleFrameStep {
    private EntitySlotLifecycle owner;
    private linksawakening.world.entity.EntityPickupFrame inputs;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private boolean linkAirborne;
    private boolean linkInteractive;
    private int linkDirection;
    private int linkZ;
    private boolean swordActive;
    private int swordX;
    private int swordWidth;
    private int swordY;
    private int swordHeight;
    private int romDirection;

    EntitySlotLifecycleFrameStep() {}

    linksawakening.world.entity.EntityPickupEvent run() {
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkAirborne = inputs.linkAirborne();
        linkInteractive = inputs.linkInteractive();
        linkDirection = inputs.linkDirection();
        linkZ = inputs.linkZ();
        swordActive = inputs.swordActive();
        swordX = inputs.swordX();
        swordWidth = inputs.swordWidth();
        swordY = inputs.swordY();
        swordHeight = inputs.swordHeight();
        if (!linkInteractive) {
            return null;
        }
        romDirection = romDirectionForJavaDirection(linkDirection);

        for (int index = owner.runtime.slots().length - 1; index >= 0; index--) {
            EntityPickupEvent pickup = collectSlot(index);
            if (pickup != null) return pickup;
        }
        return null;
    }

    private EntityPickupEvent collectSlot(int index) {

        RoomEntity entity = owner.runtime.slots()[index];
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            owner.runtime
                    .motions()
                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                    .ensureInitialized(
                            entity.slot(), owner.runtime.roomVariables().readEntityRoomId());
        }
        boolean swordPickup =
                swordActive && RoomEntityPickupRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight);
        boolean floatingType = FloatingItemMotion.isFloatingItem(entity.type());
        boolean floating = isRuntimeFloatingItem(entity);
        if (!canCollect(entity, swordPickup, floatingType, floating)) return null;

        int persistentClearMask = entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                        && owner.runtime.pickups().readChestSwordLevel() == 0
                ? 0
                : persistentClearMask(entity);
        if (entity.type() == EntityRomConstants.ENTITY_KEY_DROP_POINT) {
            if (owner.runtime.roomVariables().readEntityRoomId() == 0x80 || entity.spriteVariant() != 0) {
                owner.runtime
                        .physicsTables()
                        .writeEnemyTransitionCountdown(entity.slot(), KeyDropPointMotion.TRANSITION_COUNTDOWN);
            } else {
                owner.runtime
                        .events()
                        .pendingKeyRewardEvents
                        .add(new KeyRewardEvent(entity.slot(), ChestContentsTable.CHEST_SMALL_KEY));
                owner.clearEntity(entity.slot());
            }
        } else if (entity.type() == 0x35) {
            owner.runtime.pickups().writeHeartPiecePickup(entity.slot(), new HeartPiecePickupMotion());
            owner.runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 5);
            owner.runtime.presentationState().writePendingMusicTrack(0x10);
        } else if (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER) {
            owner.runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0x70);
        } else if (entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                && owner.runtime.pickups().readChestSwordLevel() == 0) {
            owner.runtime.startSwordPickup(entity.slot());
        } else if (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL) {
            owner.runtime.startToadstoolPickup(entity.slot());
        } else if (entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS) {
            owner.runtime.rewards().startInstrumentPickup(entity.slot());
        } else if (entity.type() == 0x33 || entity.type() == 0x34) {
            owner.runtime.rewards().writePowerUpPickupCountdown(entity.slot(), 0x30);
            owner.runtime.roomVariables().writeEntityInteractionCountdown(0x30);
            owner.runtime.presentationState().writePendingMusicTrack(0x27);
        } else if (requiresHeldPickupTransition(entity.type())) {
            owner.runtime.beginLift(entity.slot(), romDirection);
        } else {
            owner.clearEntity(entity.slot());
        }
        return new EntityPickupEvent(
                entity.slot(), entity.type(), persistentClearMask, floating ? entity.spriteVariant() : -1);
    }

    private boolean canCollect(RoomEntity entity, boolean swordPickup, boolean floatingType, boolean floating) {
        return !(!entity.loaded()
                || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityPickupRules.isPickable(entity.type())
                || (floatingType && !floating)
                || (entity.type() == 0x35 && owner.runtime.pickups().readHeartPiecePickup(entity.slot()) != null)
                || owner.runtime.rewards().readPowerUpPickupCountdown(entity.slot()) != 0
                || owner.runtime.bossRewards().readDropPrivateCountdown1(entity.slot()) > 0
                || (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER
                        && owner.runtime.physicsTables().readEnemyTransitionCountdown(entity.slot()) != 0)
                || (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL
                        && owner.runtime.rewards().readToadstoolPickupSequenceActive(entity.slot()))
                || (isCommonDroppableType(entity.type())
                        && owner.runtime.bossRewards().readDroppablePrivateState3(entity.slot()) != 0)
                || (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL
                        && (owner.runtime
                                                .motions()
                                                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                .privateState3(entity.slot())
                                        != 0
                                || owner.runtime
                                                .motions()
                                                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                .privateCountdown1(entity.slot())
                                        != 0))
                || (!swordPickup
                        && !floating
                        && !RoomEntityPickupRules.collisionCadenceMatches(frameCounter, entity.slot()))
                || (!swordPickup && !floating && linkAirborne)
                || (floating
                        && !FloatingItemMotion.linkZAllowsCollection(
                                owner.runtime.physicsTables().readGroundInteractionSideScrolling(), linkZ))
                || !(swordPickup
                        || (floating
                                ? RoomEntityPickupRules.overlapsFloatingItem(entity, linkEntityX, linkEntityY)
                                : RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY))));
    }

    private boolean inUse;

    linksawakening.world.entity.EntityPickupEvent run(
            EntitySlotLifecycle owner, linksawakening.world.entity.EntityPickupFrame inputs) {
        if (inUse) {
            return new EntitySlotLifecycleFrameStep().run(owner, inputs);
        }
        inUse = true;
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        linkAirborne = false;
        linkInteractive = false;
        linkDirection = 0;
        linkZ = 0;
        swordActive = false;
        swordX = 0;
        swordWidth = 0;
        swordY = 0;
        swordHeight = 0;
        romDirection = 0;
        this.owner = owner;
        this.inputs = inputs;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.inputs = null;
        }
    }
}
