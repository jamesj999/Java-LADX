package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.addFallingSpeedToPosition;
import static linksawakening.world.entity.EntityMotionMath.approachSignedByteZero;
import static linksawakening.world.entity.EntityMotionMath.directionFromLinkToEntity;
import static linksawakening.world.entity.EntityMotionMath.directionToLink;
import static linksawakening.world.entity.EntityMotionMath.signedByte;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_HARMLESS;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import java.util.Objects;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.WitchMotion;

/** OwlEventEntityHandler, RichardEntityHandler and village NPC presentation sequences. */
public final class EntityStoryController {
    private final EntityStoryState state;
    final EntityMotionRegistry motions;
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityPresentationState presentationState;
    final EntityRoomInspection inspection;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlots slots;
    final EntitySpriteViews spriteViews;
    final EntityPlayerCommands playerCommands;

    EntityStoryController(
            EntityStoryState state,
            EntityMotionRegistry motions,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBossState bossRewards,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlots slots,
            EntityPlayerCommands playerCommands) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        EntityRoomInspection inspection = roomInspection;
        EntitySpriteViews spriteViews = spriteHandlers;
        this.state = Objects.requireNonNull(state, "state");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.backgroundMechanics = Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
        this.inspection = Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.spriteViews = Objects.requireNonNull(spriteViews, "spriteViews");
        this.playerCommands = Objects.requireNonNull(playerCommands, "playerCommands");
    }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        this.villageNpcFrameStep.run(entity, frame, linkX, linkY);
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            this.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
    }

    public void advanceKidnapWarningKid(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int facingVariant = ((yDistance < 0 ? 0 : 2) ^ 2) + ((frameCounter >>> 3) & 0x01);
        this.slots.replace(slot, withVariant(entity, facingVariant));
        switch (this.readKidKidnapState(slot)) {
            case 0 -> {
                this.writeKidKidnapDelay(slot, 0x30);
                this.presentationState.writePendingMusicTrack(0x0E);
                this.writeKidKidnapState(slot, 1);
            }
            case 1 -> {
                if (((yDistance >= -0x20 && yDistance < 0x20) || this.preKidKidnapDelay(slot, -1) == 0)
                        && this.roomVariables.readTransitionSequenceCounter() == 0x04) {
                    if (entity.type() == EntityRomConstants.ENTITY_KID_71) {
                        this.events.dialog(2, 0x20);
                    }
                    this.writeKidKidnapState(slot, 2);
                } else {
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLink(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x08);
                    this.slots.replace(
                            slot,
                            withPositionAndVariant(
                                    entity,
                                    addFallingSpeedToPosition(
                                            entity.x(), vector.x() & 0xFF, this.storageKidSpeedXAccumulator(), slot),
                                    addFallingSpeedToPosition(
                                            entity.y(), vector.y() & 0xFF, this.storageKidSpeedYAccumulator(), slot),
                                    facingVariant));
                    this.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
                }
            }
            default -> {
                this.pushLinkFromKid(entity, linkEntityX, linkEntityY);
                int z = addFallingSpeedToPosition(
                        entity.z(), this.readKidZSpeed(slot) & 255, this.storageKidSpeedYAccumulator(), slot);
                this.writeKidZSpeed(slot, this.readKidZSpeed(slot) - 2);
                if (z == 0 || (z & 128) != 0) {
                    z = 0;
                    this.writeKidZSpeed(slot, (frameCounter & 31) == 0 ? 0x10 : 0);
                }
                this.slots.replace(slot, withZ(this.slots.get(slot), z));
                int xDistance = Math.abs(signedByte((linkEntityX - entity.x()) & 0xFF));
                if (this.linkState.readActionButtonsHeld()
                        && !this.linkState.readDialogActive()
                        && xDistance < 0x18
                        && Math.abs(yDistance) < 0x18) {
                    this.events.dialog(2, 0x20);
                }
            }
        }
    }

    public void advanceMadamMeowMeow(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        int slot = entity.slot();
        if (this.physicsTables.readEnemyTransitionCountdown(slot) == 1) {
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0);
            this.events.madamMeowMeowFullHeal(new MadamMeowMeowFullHealRequest(slot));
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int direction = Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        int variant = direction * 2 + ((frameCounter >>> 4) & 0x01);
        this.slots.replace(entity.slot(), withVariant(entity, variant));
        boolean linkFacingMadam = (this.linkState.readLastRomLinkDirection() ^ 0x01) == direction;
        boolean interactionWindow = ((yDistance + 0x14) & 0xFF) < 0x28 && ((xDistance + 0x10) & 0xFF) < 0x20;
        if (this.linkState.readActionButtonAHeld()
                && interactionWindow
                && linkFacingMadam
                && !this.linkState.readDialogActive()
                && !this.linkState.readInventoryAppearing()
                && !linkAirborne
                && this.linkState.readDialogCooldown() == 0
                && this.linkState.readWindowY() == 0x80) {
            var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
            if ((this.roomVariables.readInstrument2Flags() & 0x02) != 0 && bowWowMotion.readBowWowState() == 0x01) {
                bowWowMotion.writeBowWowState(0);
                this.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
                this.events.dialog(1, 0x2F);
                return;
            }
            int dialogLowId =
                    switch (bowWowMotion.readBowWowState()) {
                        case 0 -> 0x30;
                        case 1 -> 0x32;
                        default -> 0x31;
                    };
            this.events.dialog(1, dialogLowId);
        }
    }

    public void advanceOwlStatue(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int linkMotionState, boolean linkAirborne) {
        // The shipped handler pushes before ReturnIfNonInteractive_18.
        // CheckLinkCollisionWithEnemy uses the already incremented visual Y;
        // translating the Link probe keeps the statue's world position unchanged.
        if (linkZ == 0
                && linkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY - 1)) {
            this.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        if (!this.roomVariables.readGameplayWorld()
                || this.roomVariables.readTransitionSequenceCounter() != 0x04
                || this.linkState.readDialogActive()
                || this.linkState.readInventoryAppearing()
                || this.roomVariables.readRoomTransitionActive()
                || this.inspection.witchGotItemPresentationActive()
                || this.owlState().readOwlStatueDialogId() < 0) {
            return;
        }
        int dx = signedByte((linkEntityX - entity.x()) & 0xFF);
        int dy = signedByte((linkEntityY - entity.y()) & 0xFF);
        if (this.roomInspection.sourceNpcInteractionAllowed(dx, dy, linkAirborne, directionToLink(dx, dy))) {
            this.events.dialog(
                    this.owlState().readOwlStatueDialogId() >>> 8,
                    this.owlState().readOwlStatueDialogId() & 0xFF);
        }
    }

    public void advanceRichard(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        this.richardStep.run(entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
    }

    public void advanceOwlEvent(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean handlerLinkInteractive) {
        this.owlEventStep.run(entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
    }

    void advanceDepartingOwl(RoomEntity entity, int slot, int frameCounter) {
        this.blockLinkForOwl(slot);
        RoomEntity flying = this.roomInspection.owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(
                flying.x(), this.owlState().readOwlSpeedX(slot), this.owlState().storageOwlXAccumulator(), slot);
        int y = addFallingSpeedToPosition(
                flying.y(), this.owlState().readOwlSpeedY(slot), this.owlState().storageOwlYAccumulator(), slot);
        int z = addFallingSpeedToPosition(
                flying.z(), this.owlState().readOwlSpeedZ(slot), this.owlState().storageOwlZAccumulator(), slot);
        this.slots.replace(slot, withZ(withPositionAndVariant(flying, x, y, 0), z));
        if ((frameCounter & 0x07) == 0) {
            this.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.NOISE, 0x05);
        }
        if (x < 0x08 || x > 0x98 || y < 0x10 || y > 0x88 || z >= 0x40) {
            this.presentationState.writePendingMusicTrack(
                    this.bossRewards.readEnemyDropActivePowerUp()
                            ? 0x49
                            : this.owlState().readOwlPreviousMusicTrack(slot));
            this.lifecycle.disableEntityWithoutPersistence(slot);
        }
    }

    public void advanceWitchGotItemPresentation(int slot) {
        var witchMotion = this.motions.get(WitchMotion.class, WitchMotion::new);
        int countdown = witchMotion.readWitchGotItemCountdown(slot);
        if (countdown == 0) {
            return;
        }
        this.playerCommands.presentWitchGotItem(slot);
        if (this.linkState.readDialogActive()) {
            return;
        }
        countdown--;
        witchMotion.writeWitchGotItemCountdown(slot, countdown);
        if (countdown == 0x02) {
            this.events.dialog(0, 0x99);
        }
    }

    public void presentWitchGotItem(int slot) {
        this.playerCommands.presentWitchGotItem(slot);
    }

    public void enableBowWowFollowing() {
        var followingNpcMotion = this.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
        followingNpcMotion.writeFollowingNpcState(new FollowingNpcState(
                followingNpcMotion.readFollowingNpcState().roosterFollowing(),
                followingNpcMotion.readFollowingNpcState().ghostFollowingState(),
                followingNpcMotion.readFollowingNpcState().marinFollowing(),
                true,
                followingNpcMotion.readFollowingNpcState().instrument4Flags(),
                followingNpcMotion.readFollowingNpcState().powerBraceletLevel(),
                followingNpcMotion.readFollowingNpcState().db10()));
    }

    public void markBowWowRescued() {
        var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
        bowWowMotion.writeBowWowState(0x01);
    }

    public void consumeToadstool() {
        state.writePlayerHasToadstool(false);
    }

    public void startWitchGotItemCountdown(int slot) {
        var witchMotion = this.motions.get(WitchMotion.class, WitchMotion::new);
        witchMotion.writeWitchGotItemCountdown(slot, 0x2A);
    }

    public int bowWowState() {
        var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
        return bowWowMotion.readBowWowState();
    }

    public FollowingNpcState followingNpcState() {
        var followingNpcMotion = this.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
        return followingNpcMotion.readFollowingNpcState();
    }

    public int booBuddyTriggerCount() {
        var booBuddyMotion = this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        return booBuddyMotion.readBooBuddyTriggerCount();
    }

    public boolean playerHasToadstool() {
        return state.readPlayerHasToadstool();
    }

    public int playerMagicPowderCount() {
        return state.readPlayerMagicPowderCount();
    }

    public int bgPaletteEffectAddress() {
        return this.presentationState.readBgPaletteEffectAddress();
    }

    public int paletteDataFlags() {
        return this.presentationState.readPaletteDataFlags();
    }

    public void setShouldGetLostInMysteriousWoods(boolean value) {
        state.writeShouldGetLostInMysteriousWoods(value);
    }

    final RichardStep richardStep = new RichardStep(this);
    final VillageNpcFrameStep villageNpcFrameStep = new VillageNpcFrameStep(this);
    final OwlEventStep owlEventStep = new OwlEventStep(this);

    void blockLinkForOwl(int slot) {
        events.linkMotionBlock(new linksawakening.world.entity.event.LinkMotionBlockRequest(slot));
    }

    static final class OwlEventStep {
        private final linksawakening.world.entity.EntityStoryController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean handlerLinkInteractive;
        private int slot;

        OwlEventStep(linksawakening.world.entity.EntityStoryController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {

            slot = entity.slot();
            switch (owner.owlState().readOwlEventState(slot)) {
                case 0 -> {
                    if (!advanceState0()) return;
                }
                case 1 -> {
                    if (!advanceState1()) return;
                }
                case 2 -> {
                    if (!advanceState2()) return;
                }
                case 3 -> {
                    if (!advanceState3()) return;
                }
                default -> {
                    if (!advanceDefault()) return;
                }
            }
        }

        private boolean advanceState0() {
            if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
                if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                    return false;
                }
                owner.owlState().writeOwlPreviousMusicTrack(slot, 0x1D);
            } else if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
                if (!owner.readPlayerHasTailKey() || owner.owlState().readOwlPrivateCountdown1(slot) != 0) {
                    return false;
                }
                owner.owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
            } else if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
                if (owner.pickups.readChestSwordLevel() == 0
                        || (owner.roomVariables.readInstrument1Flags() & 0x02) == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                owner.owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
                owner.presentationState.writePendingMusicTrack(0x22);
                owner.owlState().writeOwlEventState(slot, 2);
                return false;
            } else if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                var bowWowMotion = owner.motions.get(BowWowMotion.class, BowWowMotion::new);
                if (owner.pickups.readChestSwordLevel() == 0
                        || owner.roomVariables.readInstrument2Flags() != 0
                        || bowWowMotion.readBowWowState() != 0x01
                        || (owner.roomVariables.readInstrument1Flags() & 0x02) == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                owner.owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
                owner.presentationState.writePendingMusicTrack(0x22);
                owner.owlState().writeOwlEventState(slot, 2);
                return false;
            } else {
                if (owner.pickups.readChestSwordLevel() == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                if (owner.roomVariables.readEntityRoomId() != 0x80) {
                    return false;
                }
                owner.owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
            }
            owner.presentationState.writePendingMusicTrack(0x22);
            owner.owlState().writeOwlSpeedX(slot, 0x18);
            owner.owlState().writeOwlSpeedY(slot, 0x10);
            owner.owlState().writeOwlSpeedZ(slot, 0xFC);
            owner.slots.replace(slot, withZ(entity, 0x20));
            owner.owlState().writeOwlEventState(slot, 1);

            return true;
        }

        private boolean advanceState1() {
            owner.roomVariables.writeEntityInteractionCountdown(5); // OwlState1Handler, $06:693B.
            owner.blockLinkForOwl(slot);
            owner.events.linkFacing(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY));
            if (owner.owlState().readOwlBoomerangSfxCounter(slot) == 0) {
                owner.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.NOISE, 0x2D);
                owner.owlState().writeOwlBoomerangSfxCounter(slot, 0x1A);
            } else {
                owner.owlState().postOwlBoomerangSfxCounter(slot, -1);
            }
            RoomEntity flying = owner.roomInspection.owlPresentation(entity, frameCounter);
            int x = addFallingSpeedToPosition(
                    flying.x(),
                    owner.owlState().readOwlSpeedX(slot),
                    owner.owlState().storageOwlXAccumulator(),
                    slot);
            int y = addFallingSpeedToPosition(
                    flying.y(),
                    owner.owlState().readOwlSpeedY(slot),
                    owner.owlState().storageOwlYAccumulator(),
                    slot);
            if (flying.z() == 0) {
                owner.owlState().writeOwlEventState(slot, 2);
                owner.slots.replace(slot, withPositionAndVariant(flying, x, y, 0));
                return false;
            }
            int z = addFallingSpeedToPosition(
                    flying.z(),
                    owner.owlState().readOwlSpeedZ(slot),
                    owner.owlState().storageOwlZAccumulator(),
                    slot);
            if ((frameCounter & 0x03) == 0) {
                owner.owlState()
                        .writeOwlSpeedX(
                                slot, approachSignedByteZero(owner.owlState().readOwlSpeedX(slot)));
                owner.owlState()
                        .writeOwlSpeedY(
                                slot, approachSignedByteZero(owner.owlState().readOwlSpeedY(slot)));
            }
            if (signedByte(z) <= 0) z = 0;
            owner.slots.replace(slot, withZ(withPositionAndVariant(flying, x, y, 0), z));

            return true;
        }

        private boolean advanceState2() {
            if (!handlerLinkInteractive || owner.roomVariables.readTransitionSequenceCounter() != 0x04) {
                return false;
            }
            if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                owner.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            owner.slots.replace(slot, owner.roomInspection.owlPresentation(entity, frameCounter));
            int globalDialogId =
                    owner.owlState().readOwlDialogResolver().applyAsInt(owner.roomVariables.readEntityRoomId());
            owner.events.dialog((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
            owner.owlState().writeOwlEventState(slot, 3);

            return true;
        }

        private boolean advanceState3() {
            if (!handlerLinkInteractive) {
                return false;
            }
            owner.blockLinkForOwl(slot);
            owner.slots.replace(slot, owner.roomInspection.owlPresentation(entity, frameCounter));
            if (owner.linkState.readDialogActive()) {
                if (owner.physicsTables.readEnemyTransitionCountdown(slot) > 0) {
                    owner.physicsTables.postEnemyTransitionCountdown(slot, 1);
                }
                return false;
            }
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                if (!owner.owlState().readOwlCompletionEmitted(slot)) {
                    owner.events.owlEventCompletion(new OwlEventCompletion(slot));
                    owner.owlState().writeOwlCompletionEmitted(slot, true);
                }
                owner.owlState().writeOwlSpeedX(slot, linkEntityX < entity.x() ? 0x20 : 0xE0);
                owner.owlState().writeOwlSpeedY(slot, linkEntityY < entity.y() ? 0x20 : 0xE0);
                owner.owlState().writeOwlSpeedZ(slot, 0x04);
                owner.owlState().writeOwlEventState(slot, 4);
            }

            return true;
        }

        private boolean advanceDefault() {
            if (handlerLinkInteractive) {
                owner.advanceDepartingOwl(entity, slot, frameCounter);
            }

            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                boolean handlerLinkInteractive) {
            if (inUse) {
                new OwlEventStep(owner).run(entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
                return;
            }
            inUse = true;

            slot = 0;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.handlerLinkInteractive = handlerLinkInteractive;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;
            }
        }
    }

    static final class RichardStep {
        private final linksawakening.world.entity.EntityStoryController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean linkAirborne;
        private int slot;
        private int xDistance;
        private int yDistance;
        private int[] baseVariant;
        private int variant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        RichardStep(linksawakening.world.entity.EntityStoryController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {

            slot = entity.slot();
            if (!owner.roomVariables.readGameplayWorld()
                    || owner.roomVariables.readTransitionSequenceCounter() != 0x04
                    || owner.linkState.readDialogActive()
                    || owner.linkState.readInventoryAppearing()
                    || owner.roomVariables.readRoomTransitionActive()
                    || owner.inspection.witchGotItemPresentationActive()) {
                return;
            }
            if (owner.readRichardState(slot) == 0 && owner.roomVariables.readEntityGoldenLeavesCount() >= 0x06) {
                entity = withPositionAndVariant(entity, 0x58, entity.y(), entity.spriteVariant());
                owner.writeRichardDirection(slot, 3);
            }
            xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
            yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
            if ((frameCounter & 0x1F) == 0) {
                owner.writeRichardDirection(
                        slot,
                        Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3));
            }
            baseVariant = new int[] {6, 4, 2, 0};
            variant =
                    baseVariant[owner.readRichardDirection(slot)] + ((owner.postRichardInertia(slot, 1) >>> 4) & 0x01);
            updated = withVariant(entity, variant);
            if (owner.readRichardState(slot) < 3
                    && RoomEntityCombatRules.overlapsLink(updated, linkEntityX, linkEntityY)) {
                owner.backgroundMechanics.requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
            }

            switch (owner.readRichardState(slot)) {
                case 0 -> {
                    if (!advanceState0()) return;
                }
                case 1 -> {
                    if (!advanceState1()) return;
                }
                case 2 -> {
                    if (!advanceState2()) return;
                }
                case 3 -> {
                    if (!advanceState3()) return;
                }
                case 4 -> {
                    if (!advanceState4()) return;
                }
                default -> throw new IllegalStateException("Invalid Richard state");
            }
            owner.slots.replace(slot, updated);
        }

        private boolean advanceState0() {
            if (owner.pickups.readChestSwordLevel() != 0) {
                owner.presentationState.writePendingMusicTrack(0x40);
            }
            owner.writeRichardState(slot, owner.roomVariables.readEntityGoldenLeavesCount() >= 0x06 ? 4 : 1);
            if (owner.readRichardState(slot) == 4) {
                updated = withPositionAndVariant(updated, 0x58, updated.y(), variant);
            }

            return true;
        }

        private boolean advanceState1() {
            if (!owner.roomInspection.sourceNpcInteractionAllowed(
                    xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                owner.slots.replace(slot, updated);
                return false;
            }
            var bowWowMotion = owner.motions.get(BowWowMotion.class, BowWowMotion::new);
            if (bowWowMotion.readBowWowState() != 0) {
                owner.events.dialog(1, 0x2D);
            } else if ((owner.roomVariables.readEntityRoomStatus() & 0x10) == 0
                    || owner.roomVariables.readEntityGoldenLeavesCount() == 0) {
                owner.events.dialog(1, 0x3A);
                owner.events.richardProgress(new RichardProgressEvent(
                        slot,
                        RichardProgressEvent.Kind.START_QUEST,
                        owner.roomVariables.readEntityGoldenLeavesCount()));
                owner.roomVariables.writeEntityRoomStatus(owner.roomVariables.readEntityRoomStatus() | 0x10);
                owner.writeRichardState(slot, 2);
            } else if (owner.roomVariables.readEntityGoldenLeavesCount() < 5) {
                owner.events.dialog(1, 0x3F);
            } else {
                owner.events.dialog(1, 0x3D);
                owner.roomVariables.writeEntityGoldenLeavesCount(0xFF);
                owner.events.richardProgress(
                        new RichardProgressEvent(slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                owner.writeRichardState(slot, 3);
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x20);
            }

            return true;
        }

        private boolean advanceState2() {
            if (owner.linkState.readDialogActive() || owner.linkState.readInventoryAppearing()) {
                owner.slots.replace(slot, updated);
                return false;
            }
            owner.events.dialog(1, owner.linkState.readDialogAskSelectionIndex() == 0 ? 0x3B : 0x3C);
            owner.writeRichardState(slot, 1);

            return true;
        }

        private boolean advanceState3() {
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                owner.writeRichardState(slot, 4);
            }
            int x = addFallingSpeedToPosition(updated.x(), 0xF8, owner.storageRichardXAccumulator(), slot);
            updated = withPositionAndVariant(updated, x, updated.y(), variant);

            return true;
        }

        private boolean advanceState4() {
            if (owner.roomInspection.sourceNpcInteractionAllowed(
                    xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                int dialog = owner.roomVariables.readEntityGoldenLeavesCount() == 0x06 ? 0x3E : 0x3D;
                owner.events.dialog(1, dialog);
            }
            if (((linkEntityX - 0x78 + 0x02) & 0xFF) < 0x04 && ((linkEntityY - 0x20 + 0x05) & 0xFF) < 0x0A) {
                owner.events.dungeonWarp(new DungeonWarpRequest(
                        slot, new linksawakening.world.entity.event.WarpDestination(0x11, 0xD8, 0x88, 0x70)));
                owner.presentationState.writePendingMusicFadeOutCountdown(0x30);
                owner.events.sound(slot, EntityRomConstants.ENTITY_RICHARD, EntityCombatEvent.SoundChannel.NOISE, 0x06);
                owner.lifecycle.disableEntityWithoutPersistence(slot);
                return false;
            }

            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                boolean linkAirborne) {
            if (inUse) {
                new RichardStep(owner).run(entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
                return;
            }
            inUse = true;

            slot = 0;
            xDistance = 0;
            yDistance = 0;
            variant = 0;
            updated = null;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.linkAirborne = linkAirborne;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;

                this.updated = null;
            }
        }
    }

    static final class VillageNpcFrameStep {
        private final linksawakening.world.entity.EntityStoryController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frame;
        private int linkX;
        private int linkY;
        private int slot;
        private int z;
        private boolean landed;
        private int timer;

        VillageNpcFrameStep(linksawakening.world.entity.EntityStoryController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!advanceKidMotion()) return;
            if (!advancePlayTimer()) return;
            if (!openKidDialog()) return;
        }

        private boolean advanceKidMotion() {

            slot = entity.slot();
            if (owner.readKidBall(slot)) {
                if (owner.preKidPlayTimer(slot, -1) <= 0) {
                    owner.slots.replace(slot, withStatus(entity, EntityStatus.DISABLED));
                    owner.writeKidBall(slot, false);
                    return false;
                }
                int x = addFallingSpeedToPosition(
                        entity.x(), owner.readKidPlayState(slot), owner.storageKidSpeedXAccumulator(), slot);
                int z = addFallingSpeedToPosition(
                        entity.z(), owner.postKidZSpeed(slot, -1) & 255, owner.storageKidSpeedYAccumulator(), slot);
                owner.slots.replace(slot, withZ(withPositionAndVariant(entity, x, entity.y(), 0), z));
                return false;
            }
            var bowWowMotion = owner.motions.get(BowWowMotion.class, BowWowMotion::new);
            if (bowWowMotion.readBowWowState() == 0x80) {
                owner.advanceKidnapWarningKid(entity, frame, linkX, linkY);
                return false;
            }
            owner.pushLinkFromKid(entity, linkX, linkY);
            {
                entity = owner.spriteHandlers.present(
                        entity,
                        owner.spriteViews.forKidPlaying(),
                        (entity.type() == 0x71 ? 2 : 0) | (owner.readKidPlayState(slot) == 1 ? 1 : 0));
            }
            z = addFallingSpeedToPosition(
                    entity.z(), owner.postKidZSpeed(slot, -1) & 255, owner.storageKidSpeedYAccumulator(), slot);
            landed = (z & 128) != 0;
            if (landed) {
                z = 0;
                owner.writeKidZSpeed(slot, 0);
            }
            owner.slots.replace(slot, withZ(entity, z));
            timer = owner.readKidPlayTimer(slot);
            if (timer > 0) timer--;
            return true;
        }

        private boolean advancePlayTimer() {

            if (timer == 0) {
                if (owner.readKidPlayState(slot) == 0) {
                    owner.writeKidPlayState(slot, 1);
                    timer = 0x80;
                    int ball = owner.slots.findFree(owner.slots.size() - 1);
                    if (ball >= 0 && owner.spriteHandlers.supportsPresentation()) {
                        var definition = owner.spriteViews.forKidBall();
                        owner.slots.replace(
                                ball,
                                new RoomEntity(
                                        ball,
                                        -1,
                                        0x71,
                                        entity.x(),
                                        entity.y(),
                                        EntityStatus.ACTIVE,
                                        definition,
                                        0,
                                        0,
                                        0,
                                        0,
                                        -1,
                                        false));
                        owner.lifecycle.initializeHealthGroup(ball);
                        owner.physicsTables.writeEnemyPhysicsFlags(ball, ENTITY_PHYSICS_HARMLESS);
                        owner.combatTables.writeEnemyHitboxFlags(ball, 0);
                        owner.physicsTables.writeEntityOptions1Override(
                                ball, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
                        owner.writeKidBall(ball, true);
                        owner.writeKidPlayTimer(ball, 0x24);
                        owner.writeKidPlayState(ball, entity.type() == 0x71 ? 0x14 : 0xEC);
                        owner.writeKidZSpeed(ball, 0x10);
                        owner.writeKidSpeedXAccumulator(ball, 0);
                        owner.writeKidSpeedYAccumulator(ball, 0);
                        owner.physicsTables.writeDynamicEntitySpawnedThisFrame(ball, true);
                    }
                } else {
                    owner.writeKidPlayState(slot, 0);
                    timer = 0x60;
                }
            } else if (owner.readKidPlayState(slot) == 1 && timer < 0x60) {
                owner.slots.replace(slot, withVariant(owner.slots.get(slot), entity.type() == 0x71 ? 2 : 0));
                if (timer < 0x40 && landed) owner.writeKidZSpeed(slot, 8);
            }
            owner.writeKidPlayTimer(slot, timer);
            return true;
        }

        private boolean openKidDialog() {

            if (owner.linkState.readActionButtonAHeld()
                    && !owner.linkState.readDialogActive()
                    && owner.linkState.readDialogCooldown() == 0
                    && !owner.linkState.readInventoryAppearing()
                    && owner.linkState.readWindowY() == 0x80
                    && Math.abs(signedByte(linkX - entity.x())) < 0x10
                    && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
                int dialog = (owner.roomVariables.readInstrument1Flags() & 2) == 0
                        ? 0x18 + (owner.postKidHintIndex(1) & 3)
                        : 0x1C + entity.type() - 0x70;
                owner.events.dialog(1, dialog);
            }
            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.RoomEntity entity, int frame, int linkX, int linkY) {
            if (inUse) {
                new VillageNpcFrameStep(owner).run(entity, frame, linkX, linkY);
                return;
            }
            inUse = true;

            slot = 0;
            z = 0;
            landed = false;
            timer = 0;

            this.entity = entity;
            this.frame = frame;
            this.linkX = linkX;
            this.linkY = linkY;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;
            }
        }
    }

    OwlEventState owlState() {
        return state.owlState();
    }

    int postKidHintIndex(int delta) {
        return state.postKidHintIndex(delta);
    }

    int postKidZSpeed(int slot, int delta) {
        return state.postKidZSpeed(slot, delta);
    }

    int postRichardInertia(int slot, int delta) {
        return state.postRichardInertia(slot, delta);
    }

    int preKidKidnapDelay(int slot, int delta) {
        return state.preKidKidnapDelay(slot, delta);
    }

    int preKidPlayTimer(int slot, int delta) {
        return state.preKidPlayTimer(slot, delta);
    }

    boolean readKidBall(int slot) {
        return state.readKidBall(slot);
    }

    int readKidKidnapState(int slot) {
        return state.readKidKidnapState(slot);
    }

    int readKidPlayState(int slot) {
        return state.readKidPlayState(slot);
    }

    int readKidPlayTimer(int slot) {
        return state.readKidPlayTimer(slot);
    }

    int readKidZSpeed(int slot) {
        return state.readKidZSpeed(slot);
    }

    boolean readPlayerHasTailKey() {
        return state.readPlayerHasTailKey();
    }

    int readRichardDirection(int slot) {
        return state.readRichardDirection(slot);
    }

    int readRichardState(int slot) {
        return state.readRichardState(slot);
    }

    int[] storageKidSpeedXAccumulator() {
        return state.storageKidSpeedXAccumulator();
    }

    int[] storageKidSpeedYAccumulator() {
        return state.storageKidSpeedYAccumulator();
    }

    int[] storageRichardXAccumulator() {
        return state.storageRichardXAccumulator();
    }

    boolean writeKidBall(int slot, boolean value) {
        return state.writeKidBall(slot, value);
    }

    int writeKidKidnapDelay(int slot, int value) {
        return state.writeKidKidnapDelay(slot, value);
    }

    int writeKidKidnapState(int slot, int value) {
        return state.writeKidKidnapState(slot, value);
    }

    int writeKidPlayState(int slot, int value) {
        return state.writeKidPlayState(slot, value);
    }

    int writeKidPlayTimer(int slot, int value) {
        return state.writeKidPlayTimer(slot, value);
    }

    int writeKidSpeedXAccumulator(int slot, int value) {
        return state.writeKidSpeedXAccumulator(slot, value);
    }

    int writeKidSpeedYAccumulator(int slot, int value) {
        return state.writeKidSpeedYAccumulator(slot, value);
    }

    int writeKidZSpeed(int slot, int value) {
        return state.writeKidZSpeed(slot, value);
    }

    int writeRichardDirection(int slot, int value) {
        return state.writeRichardDirection(slot, value);
    }

    int writeRichardState(int slot, int value) {
        return state.writeRichardState(slot, value);
    }

    boolean writeShouldGetLostInMysteriousWoods(boolean value) {
        return state.writeShouldGetLostInMysteriousWoods(value);
    }
}
