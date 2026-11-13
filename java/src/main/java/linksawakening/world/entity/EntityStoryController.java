package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** ROM story and NPC presentation sequences, sharing the owning room slot table. */
public final class EntityStoryController {
    final EntityStoryState state;
    final EntityMotionRegistry motions;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
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
    final RoomEntity[] slots;
    final EntitySpriteViews spriteViews;
    final EntityPlayerCommands playerCommands;

    EntityStoryController(
            EntityStoryState state,
            EntityMotionRegistry motions,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityRoomInspection inspection,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            RoomEntity[] slots,
            EntitySpriteViews spriteViews,
            EntityPlayerCommands playerCommands) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.spriteHandlers = spriteHandlers;
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.spriteViews = java.util.Objects.requireNonNull(spriteViews, "spriteViews");
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
    }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        EntityStoryController.villageNpcBehaviorAdvanceVillageKid(this, entity, frame, linkX, linkY);
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            this.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
    }

    public void advanceKidnapWarningKid(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        EntityStoryController.villageNpcBehaviorAdvanceKidnapWarningKid(
                this, entity, frameCounter, linkEntityX, linkEntityY);
    }

    public void advanceMadamMeowMeow(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        EntityStoryController.villageNpcBehaviorAdvanceMadamMeowMeow(
                this, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
    }

    public void advanceOwlStatue(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int linkMotionState, boolean linkAirborne) {
        EntityStoryController.villageNpcBehaviorAdvanceOwlStatue(
                this, entity, linkEntityX, linkEntityY, linkZ, linkMotionState, linkAirborne);
    }

    public void advanceRichard(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        EntityStoryController.richardBehaviorAdvanceRichard(
                this, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
    }

    public void advanceOwlEvent(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean handlerLinkInteractive) {
        EntityStoryController.owlEventBehaviorAdvanceOwlEvent(
                this, entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
    }

    void advanceDepartingOwl(RoomEntity entity, int slot, int frameCounter) {
        EntityStoryController.owlEventBehaviorAdvanceDepartingOwl(this, entity, slot, frameCounter);
    }

    public void advanceWitchGotItemPresentation(int slot) {
        int countdown = this.motions.get(WitchMotion.class, WitchMotion::new).readWitchGotItemCountdown(slot);
        if (countdown == 0) {
            return;
        }
        this.playerCommands.presentWitchGotItem(slot);
        if (this.linkState.dialogActive) {
            return;
        }
        countdown--;
        this.motions.get(WitchMotion.class, WitchMotion::new).writeWitchGotItemCountdown(slot, countdown);
        if (countdown == 0x02) {
            this.events.pendingDialogRequests.add(new DialogRequest(0, 0x99));
        }
    }

    public void presentWitchGotItem(int slot) {
        this.playerCommands.presentWitchGotItem(slot);
    }

    public void enableBowWowFollowing() {
        EntityStoryController.richardBehaviorEnableBowWowFollowing(this);
    }

    public void markBowWowRescued() {
        this.motions.get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(0x01);
    }

    public void consumeToadstool() {
        state.writePlayerHasToadstool(false);
    }

    public void startWitchGotItemCountdown(int slot) {
        this.motions.get(WitchMotion.class, WitchMotion::new).writeWitchGotItemCountdown(slot, 0x2A);
    }

    public int bowWowState() {
        return this.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState();
    }

    public FollowingNpcState followingNpcState() {
        return this.motions
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .readFollowingNpcState();
    }

    public int booBuddyTriggerCount() {
        return this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new).readBooBuddyTriggerCount();
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

    final RichardStep richardStep = new RichardStep();
    final VillageNpcFrameStep villageNpcFrameStep = new VillageNpcFrameStep();
    final OwlEventStep owlEventStep = new OwlEventStep();

    void blockLinkForOwl(int slot) {
        events.pendingLinkMotionBlockRequests.add(new linksawakening.world.entity.event.LinkMotionBlockRequest(slot));
    }

    static void owlEventBehaviorAdvanceOwlEvent(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean handlerLinkInteractive) {
        owner.owlEventStep.run(owner, entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
    }

    static void owlEventBehaviorAdvanceDepartingOwl(
            EntityStoryController owner, RoomEntity entity, int slot, int frameCounter) {
        var runtime = owner;
        runtime.blockLinkForOwl(slot);
        RoomEntity flying = runtime.roomInspection.owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(
                flying.x(),
                owner.state.owlState().readOwlSpeedX(slot),
                owner.state.owlState().storageOwlXAccumulator(),
                slot);
        int y = addFallingSpeedToPosition(
                flying.y(),
                owner.state.owlState().readOwlSpeedY(slot),
                owner.state.owlState().storageOwlYAccumulator(),
                slot);
        int z = addFallingSpeedToPosition(
                flying.z(),
                owner.state.owlState().readOwlSpeedZ(slot),
                owner.state.owlState().storageOwlZAccumulator(),
                slot);
        runtime.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
        if ((frameCounter & 0x07) == 0) {
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x05));
        }
        if (x < 0x08 || x > 0x98 || y < 0x10 || y > 0x88 || z >= 0x40) {
            runtime.presentationState.writePendingMusicTrack(
                    runtime.bossRewards.readEnemyDropActivePowerUp()
                            ? 0x49
                            : owner.state.owlState().readOwlPreviousMusicTrack(slot));
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
    }

    static void richardBehaviorAdvanceRichard(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne) {
        owner.richardStep.run(owner, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
    }

    static void richardBehaviorEnableBowWowFollowing(EntityStoryController owner) {
        var runtime = owner;
        runtime.motions
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .writeFollowingNpcState(new FollowingNpcState(
                        runtime.motions
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .roosterFollowing(),
                        runtime.motions
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .ghostFollowingState(),
                        runtime.motions
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .marinFollowing(),
                        true,
                        runtime.motions
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .instrument4Flags(),
                        runtime.motions
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .powerBraceletLevel(),
                        runtime.motions
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .db10()));
    }

    static void villageNpcBehaviorAdvanceVillageKid(
            EntityStoryController owner, RoomEntity entity, int frame, int linkX, int linkY) {
        owner.villageNpcFrameStep.run(owner, entity, frame, linkX, linkY);
    }

    static void villageNpcBehaviorAdvanceKidnapWarningKid(
            EntityStoryController owner, RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        var runtime = owner;
        int slot = entity.slot();
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int facingVariant = ((yDistance < 0 ? 0 : 2) ^ 2) + ((frameCounter >>> 3) & 0x01);
        runtime.slots[slot] = withVariant(entity, facingVariant);
        switch (owner.state.readKidKidnapState(slot)) {
            case 0 -> {
                owner.state.writeKidKidnapDelay(slot, 0x30);
                runtime.presentationState.writePendingMusicTrack(0x0E);
                owner.state.writeKidKidnapState(slot, 1);
            }
            case 1 -> {
                if ((yDistance >= -0x20 && yDistance < 0x20 || owner.state.preKidKidnapDelay(slot, -1) == 0)
                        && runtime.roomVariables.readTransitionSequenceCounter() == 0x04) {
                    if (entity.type() == EntityRomConstants.ENTITY_KID_71) {
                        runtime.events.pendingDialogRequests.add(new DialogRequest(2, 0x20));
                    }
                    owner.state.writeKidKidnapState(slot, 2);
                } else {
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLink(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x08);
                    runtime.slots[slot] = withPositionAndVariant(
                            entity,
                            addFallingSpeedToPosition(
                                    entity.x(), vector.x() & 0xFF, owner.state.storageKidSpeedXAccumulator(), slot),
                            addFallingSpeedToPosition(
                                    entity.y(), vector.y() & 0xFF, owner.state.storageKidSpeedYAccumulator(), slot),
                            facingVariant);
                    runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
                }
            }
            default -> {
                owner.pushLinkFromKid(entity, linkEntityX, linkEntityY);
                int z = addFallingSpeedToPosition(
                        entity.z(),
                        owner.state.readKidZSpeed(slot) & 255,
                        owner.state.storageKidSpeedYAccumulator(),
                        slot);
                owner.state.writeKidZSpeed(slot, owner.state.readKidZSpeed(slot) - (2));
                if (z == 0 || (z & 128) != 0) {
                    z = 0;
                    owner.state.writeKidZSpeed(slot, (frameCounter & 31) == 0 ? 0x10 : 0);
                }
                runtime.slots[slot] = withZ(runtime.slots[slot], z);
                int xDistance = Math.abs(signedByte((linkEntityX - entity.x()) & 0xFF));
                if (runtime.linkState.actionButtonsHeld
                        && !runtime.linkState.dialogActive
                        && xDistance < 0x18
                        && Math.abs(yDistance) < 0x18) {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(2, 0x20));
                }
            }
        }
    }

    static void villageNpcBehaviorAdvanceMadamMeowMeow(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne) {
        var runtime = owner;
        int slot = entity.slot();
        if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 1) {
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0);
            runtime.events.pendingMadamMeowMeowFullHealRequests.add(new MadamMeowMeowFullHealRequest(slot));
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int direction = Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        int variant = direction * 2 + ((frameCounter >>> 4) & 0x01);
        runtime.slots[entity.slot()] = withVariant(entity, variant);
        boolean linkFacingMadam = (runtime.linkState.lastRomLinkDirection ^ 0x01) == direction;
        boolean interactionWindow = ((yDistance + 0x14) & 0xFF) < 0x28 && ((xDistance + 0x10) & 0xFF) < 0x20;
        if (runtime.linkState.actionButtonAHeld
                && interactionWindow
                && linkFacingMadam
                && !runtime.linkState.dialogActive
                && !runtime.linkState.inventoryAppearing
                && !linkAirborne
                && runtime.linkState.dialogCooldown == 0
                && runtime.linkState.windowY == 0x80) {
            if ((runtime.roomVariables.readInstrument2Flags() & 0x02) != 0
                    && runtime.motions
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState()
                            == 0x01) {
                runtime.motions.get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(0);
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x2F));
                return;
            }
            int dialogLowId =
                    switch (runtime.motions
                            .get(BowWowMotion.class, BowWowMotion::new)
                            .readBowWowState()) {
                        case 0 -> 0x30;
                        case 1 -> 0x32;
                        default -> 0x31;
                    };
            runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialogLowId));
        }
    }

    static void villageNpcBehaviorAdvanceOwlStatue(
            EntityStoryController owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int linkMotionState,
            boolean linkAirborne) {
        var runtime = owner;
        // The shipped handler pushes before ReturnIfNonInteractive_18.
        // CheckLinkCollisionWithEnemy uses the already incremented visual Y;
        // translating the Link probe keeps the statue's world position unchanged.
        if (linkZ == 0
                && linkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY - 1)) {
            runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        if (!runtime.roomVariables.readGameplayWorld()
                || runtime.roomVariables.readTransitionSequenceCounter() != 0x04
                || runtime.linkState.dialogActive
                || runtime.linkState.inventoryAppearing
                || runtime.roomVariables.readRoomTransitionActive()
                || runtime.inspection.witchGotItemPresentationActive()
                || owner.state.owlState().readOwlStatueDialogId() < 0) {
            return;
        }
        int dx = signedByte((linkEntityX - entity.x()) & 0xFF);
        int dy = signedByte((linkEntityY - entity.y()) & 0xFF);
        if (runtime.roomInspection.sourceNpcInteractionAllowed(dx, dy, linkAirborne, directionToLink(dx, dy))) {
            runtime.events.pendingDialogRequests.add(new DialogRequest(
                    owner.state.owlState().readOwlStatueDialogId() >>> 8,
                    owner.state.owlState().readOwlStatueDialogId() & 0xFF));
        }
    }

    static final class OwlEventStep {
        private linksawakening.world.entity.EntityStoryController owner;
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean handlerLinkInteractive;
        private linksawakening.world.entity.EntityStoryController runtime;
        private int slot;

        OwlEventStep() {}

        void run() {
            runtime = owner;
            slot = entity.slot();
            switch (owner.state.owlState().readOwlEventState(slot)) {
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
            if (runtime.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
                if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                    return false;
                }
                owner.state.owlState().writeOwlPreviousMusicTrack(slot, 0x1D);
            } else if (runtime.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
                if (!owner.state.readPlayerHasTailKey()
                        || owner.state.owlState().readOwlPrivateCountdown1(slot) != 0) {
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(runtime.roomVariables.readEntityRoomId()));
            } else if (runtime.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
                if (runtime.pickups.readChestSwordLevel() == 0
                        || (runtime.roomVariables.readInstrument1Flags() & 0x02) == 0) {
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(runtime.roomVariables.readEntityRoomId()));
                runtime.presentationState.writePendingMusicTrack(0x22);
                owner.state.owlState().writeOwlEventState(slot, 2);
                return false;
            } else if (runtime.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                if (runtime.pickups.readChestSwordLevel() == 0
                        || runtime.roomVariables.readInstrument2Flags() != 0
                        || runtime.motions
                                        .get(BowWowMotion.class, BowWowMotion::new)
                                        .readBowWowState()
                                != 0x01
                        || (runtime.roomVariables.readInstrument1Flags() & 0x02) == 0) {
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(runtime.roomVariables.readEntityRoomId()));
                runtime.presentationState.writePendingMusicTrack(0x22);
                owner.state.owlState().writeOwlEventState(slot, 2);
                return false;
            } else {
                if (runtime.pickups.readChestSwordLevel() == 0) {
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                if (runtime.roomVariables.readEntityRoomId() != 0x80) {
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(runtime.roomVariables.readEntityRoomId()));
            }
            runtime.presentationState.writePendingMusicTrack(0x22);
            owner.state.owlState().writeOwlSpeedX(slot, 0x18);
            owner.state.owlState().writeOwlSpeedY(slot, 0x10);
            owner.state.owlState().writeOwlSpeedZ(slot, 0xFC);
            runtime.slots[slot] = withZ(entity, 0x20);
            owner.state.owlState().writeOwlEventState(slot, 1);

            return true;
        }

        private boolean advanceState1() {
            runtime.roomVariables.writeEntityInteractionCountdown(5); // OwlState1Handler, $06:693B.
            runtime.blockLinkForOwl(slot);
            runtime.events.pendingLinkFacingRequests.add(
                    new LinkFacingRequest(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
            if (owner.state.owlState().readOwlBoomerangSfxCounter(slot) == 0) {
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2D));
                owner.state.owlState().writeOwlBoomerangSfxCounter(slot, 0x1A);
            } else {
                owner.state.owlState().postOwlBoomerangSfxCounter(slot, -1);
            }
            RoomEntity flying = runtime.roomInspection.owlPresentation(entity, frameCounter);
            int x = addFallingSpeedToPosition(
                    flying.x(),
                    owner.state.owlState().readOwlSpeedX(slot),
                    owner.state.owlState().storageOwlXAccumulator(),
                    slot);
            int y = addFallingSpeedToPosition(
                    flying.y(),
                    owner.state.owlState().readOwlSpeedY(slot),
                    owner.state.owlState().storageOwlYAccumulator(),
                    slot);
            if (flying.z() == 0) {
                owner.state.owlState().writeOwlEventState(slot, 2);
                runtime.slots[slot] = withPositionAndVariant(flying, x, y, 0);
                return false;
            }
            int z = addFallingSpeedToPosition(
                    flying.z(),
                    owner.state.owlState().readOwlSpeedZ(slot),
                    owner.state.owlState().storageOwlZAccumulator(),
                    slot);
            if ((frameCounter & 0x03) == 0) {
                owner.state
                        .owlState()
                        .writeOwlSpeedX(
                                slot,
                                approachSignedByteZero(owner.state.owlState().readOwlSpeedX(slot)));
                owner.state
                        .owlState()
                        .writeOwlSpeedY(
                                slot,
                                approachSignedByteZero(owner.state.owlState().readOwlSpeedY(slot)));
            }
            if (signedByte(z) <= 0) z = 0;
            runtime.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);

            return true;
        }

        private boolean advanceState2() {
            if (!handlerLinkInteractive || runtime.roomVariables.readTransitionSequenceCounter() != 0x04) {
                return false;
            }
            if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            runtime.slots[slot] = runtime.roomInspection.owlPresentation(entity, frameCounter);
            int globalDialogId =
                    owner.state.owlState().readOwlDialogResolver().applyAsInt(runtime.roomVariables.readEntityRoomId());
            runtime.events.pendingDialogRequests.add(
                    new DialogRequest((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
            owner.state.owlState().writeOwlEventState(slot, 3);

            return true;
        }

        private boolean advanceState3() {
            if (!handlerLinkInteractive) {
                return false;
            }
            runtime.blockLinkForOwl(slot);
            runtime.slots[slot] = runtime.roomInspection.owlPresentation(entity, frameCounter);
            if (runtime.linkState.dialogActive) {
                if (runtime.physicsTables.readEnemyTransitionCountdown(slot) > 0) {
                    runtime.physicsTables.postEnemyTransitionCountdown(slot, 1);
                }
                return false;
            }
            if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                if (!owner.state.owlState().readOwlCompletionEmitted(slot)) {
                    runtime.events.pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
                    owner.state.owlState().writeOwlCompletionEmitted(slot, true);
                }
                owner.state.owlState().writeOwlSpeedX(slot, linkEntityX < entity.x() ? 0x20 : 0xE0);
                owner.state.owlState().writeOwlSpeedY(slot, linkEntityY < entity.y() ? 0x20 : 0xE0);
                owner.state.owlState().writeOwlSpeedZ(slot, 0x04);
                owner.state.owlState().writeOwlEventState(slot, 4);
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
                linksawakening.world.entity.EntityStoryController owner,
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                boolean handlerLinkInteractive) {
            if (inUse) {
                new OwlEventStep().run(owner, entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
                return;
            }
            inUse = true;
            runtime = null;
            slot = 0;
            this.owner = owner;
            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.handlerLinkInteractive = handlerLinkInteractive;
            try {
                run();
            } finally {
                inUse = false;
                this.owner = null;
                this.entity = null;
                this.runtime = null;
            }
        }
    }

    static final class RichardStep {
        private linksawakening.world.entity.EntityStoryController owner;
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean linkAirborne;
        private linksawakening.world.entity.EntityStoryController runtime;
        private int slot;
        private int xDistance;
        private int yDistance;
        private int[] baseVariant;
        private int variant;
        private linksawakening.world.entity.RoomEntity updated;

        RichardStep() {}

        void run() {
            runtime = owner;
            slot = entity.slot();
            if (!runtime.roomVariables.readGameplayWorld()
                    || runtime.roomVariables.readTransitionSequenceCounter() != 0x04
                    || runtime.linkState.dialogActive
                    || runtime.linkState.inventoryAppearing
                    || runtime.roomVariables.readRoomTransitionActive()
                    || runtime.inspection.witchGotItemPresentationActive()) {
                return;
            }
            if (owner.state.readRichardState(slot) == 0
                    && runtime.roomVariables.readEntityGoldenLeavesCount() >= 0x06) {
                entity = withPositionAndVariant(entity, 0x58, entity.y(), entity.spriteVariant());
                owner.state.writeRichardDirection(slot, 3);
            }
            xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
            yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
            if ((frameCounter & 0x1F) == 0) {
                owner.state.writeRichardDirection(
                        slot,
                        Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3));
            }
            baseVariant = new int[] {6, 4, 2, 0};
            variant = baseVariant[owner.state.readRichardDirection(slot)]
                    + ((owner.state.postRichardInertia(slot, 1) >>> 4) & 0x01);
            updated = withVariant(entity, variant);
            if (owner.state.readRichardState(slot) < 3
                    && RoomEntityCombatRules.overlapsLink(updated, linkEntityX, linkEntityY)) {
                runtime.backgroundMechanics.requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
            }

            switch (owner.state.readRichardState(slot)) {
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
            runtime.slots[slot] = updated;
        }

        private boolean advanceState0() {
            if (runtime.pickups.readChestSwordLevel() != 0) {
                runtime.presentationState.writePendingMusicTrack(0x40);
            }
            owner.state.writeRichardState(slot, runtime.roomVariables.readEntityGoldenLeavesCount() >= 0x06 ? 4 : 1);
            if (owner.state.readRichardState(slot) == 4) {
                updated = withPositionAndVariant(updated, 0x58, updated.y(), variant);
            }

            return true;
        }

        private boolean advanceState1() {
            if (!runtime.roomInspection.sourceNpcInteractionAllowed(
                    xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                runtime.slots[slot] = updated;
                return false;
            }
            if (runtime.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState() != 0) {
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x2D));
            } else if ((runtime.roomVariables.readEntityRoomStatus() & 0x10) == 0
                    || runtime.roomVariables.readEntityGoldenLeavesCount() == 0) {
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x3A));
                runtime.events.pendingRichardProgressEvents.add(new RichardProgressEvent(
                        slot,
                        RichardProgressEvent.Kind.START_QUEST,
                        runtime.roomVariables.readEntityGoldenLeavesCount()));
                runtime.roomVariables.writeEntityRoomStatus(runtime.roomVariables.readEntityRoomStatus() | (0x10));
                owner.state.writeRichardState(slot, 2);
            } else if (runtime.roomVariables.readEntityGoldenLeavesCount() < 5) {
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x3F));
            } else {
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x3D));
                runtime.roomVariables.writeEntityGoldenLeavesCount(0xFF);
                runtime.events.pendingRichardProgressEvents.add(
                        new RichardProgressEvent(slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                owner.state.writeRichardState(slot, 3);
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x20);
            }

            return true;
        }

        private boolean advanceState2() {
            if (runtime.linkState.dialogActive || runtime.linkState.inventoryAppearing) {
                runtime.slots[slot] = updated;
                return false;
            }
            runtime.events.pendingDialogRequests.add(
                    new DialogRequest(1, runtime.linkState.dialogAskSelectionIndex == 0 ? 0x3B : 0x3C));
            owner.state.writeRichardState(slot, 1);

            return true;
        }

        private boolean advanceState3() {
            if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                owner.state.writeRichardState(slot, 4);
            }
            int x = addFallingSpeedToPosition(updated.x(), 0xF8, owner.state.storageRichardXAccumulator(), slot);
            updated = withPositionAndVariant(updated, x, updated.y(), variant);

            return true;
        }

        private boolean advanceState4() {
            if (runtime.roomInspection.sourceNpcInteractionAllowed(
                    xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                int dialog = runtime.roomVariables.readEntityGoldenLeavesCount() == 0x06 ? 0x3E : 0x3D;
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
            }
            if (((linkEntityX - 0x78 + 0x02) & 0xFF) < 0x04 && ((linkEntityY - 0x20 + 0x05) & 0xFF) < 0x0A) {
                runtime.events.pendingDungeonWarpRequests.add(new DungeonWarpRequest(
                        slot, new linksawakening.world.entity.event.WarpDestination(0x11, 0xD8, 0x88, 0x70)));
                runtime.presentationState.writePendingMusicFadeOutCountdown(0x30);
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_RICHARD, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x06));
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
                return false;
            }

            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.EntityStoryController owner,
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                boolean linkAirborne) {
            if (inUse) {
                new RichardStep().run(owner, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
                return;
            }
            inUse = true;
            runtime = null;
            slot = 0;
            xDistance = 0;
            yDistance = 0;
            variant = 0;
            updated = null;
            this.owner = owner;
            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.linkAirborne = linkAirborne;
            try {
                run();
            } finally {
                inUse = false;
                this.owner = null;
                this.entity = null;
                this.runtime = null;
                this.updated = null;
            }
        }
    }

    static final class VillageNpcFrameStep {
        private linksawakening.world.entity.EntityStoryController owner;
        private linksawakening.world.entity.RoomEntity entity;
        private int frame;
        private int linkX;
        private int linkY;
        private linksawakening.world.entity.EntityStoryController runtime;
        private int slot;
        private int z;
        private boolean landed;
        private int timer;

        VillageNpcFrameStep() {}

        void run() {
            if (!advanceKidMotion()) return;
            if (!advancePlayTimer()) return;
            if (!openKidDialog()) return;
        }

        private boolean advanceKidMotion() {

            runtime = owner;
            slot = entity.slot();
            if (owner.state.readKidBall(slot)) {
                if (owner.state.preKidPlayTimer(slot, -1) <= 0) {
                    runtime.slots[slot] = withStatus(entity, EntityStatus.DISABLED);
                    owner.state.writeKidBall(slot, false);
                    return false;
                }
                int x = addFallingSpeedToPosition(
                        entity.x(),
                        owner.state.readKidPlayState(slot),
                        owner.state.storageKidSpeedXAccumulator(),
                        slot);
                int z = addFallingSpeedToPosition(
                        entity.z(),
                        owner.state.postKidZSpeed(slot, -1) & 255,
                        owner.state.storageKidSpeedYAccumulator(),
                        slot);
                runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
                return false;
            }
            if (runtime.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState() == 0x80) {
                owner.advanceKidnapWarningKid(entity, frame, linkX, linkY);
                return false;
            }
            owner.pushLinkFromKid(entity, linkX, linkY);
            if (runtime.spriteHandlers != null)
                entity = withDefinition(
                        entity,
                        runtime.spriteViews.forKidPlaying(),
                        (entity.type() == 0x71 ? 2 : 0) | (owner.state.readKidPlayState(slot) == 1 ? 1 : 0));
            z = addFallingSpeedToPosition(
                    entity.z(),
                    owner.state.postKidZSpeed(slot, -1) & 255,
                    owner.state.storageKidSpeedYAccumulator(),
                    slot);
            landed = (z & 128) != 0;
            if (landed) {
                z = 0;
                owner.state.writeKidZSpeed(slot, 0);
            }
            runtime.slots[slot] = withZ(entity, z);
            timer = owner.state.readKidPlayTimer(slot);
            if (timer > 0) timer--;
            return true;
        }

        private boolean advancePlayTimer() {

            if (timer == 0) {
                if (owner.state.readKidPlayState(slot) == 0) {
                    owner.state.writeKidPlayState(slot, 1);
                    timer = 0x80;
                    int ball = EntitySlotAllocator.findFreeSlot(
                            runtime.slots, runtime.combatTables, runtime.slots.length - 1);
                    if (ball >= 0 && runtime.spriteHandlers != null) {
                        var definition = runtime.spriteViews.forKidBall();
                        runtime.slots[ball] = new RoomEntity(
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
                                false);
                        runtime.lifecycle.initializeHealthGroup(ball);
                        runtime.physicsTables.writeEnemyPhysicsFlags(ball, ENTITY_PHYSICS_HARMLESS);
                        runtime.combatTables.writeEnemyHitboxFlags(ball, 0);
                        runtime.physicsTables.writeEntityOptions1Override(
                                ball, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
                        owner.state.writeKidBall(ball, true);
                        owner.state.writeKidPlayTimer(ball, 0x24);
                        owner.state.writeKidPlayState(ball, entity.type() == 0x71 ? 0x14 : 0xEC);
                        owner.state.writeKidZSpeed(ball, 0x10);
                        owner.state.writeKidSpeedXAccumulator(ball, 0);
                        owner.state.writeKidSpeedYAccumulator(ball, 0);
                        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(ball, true);
                    }
                } else {
                    owner.state.writeKidPlayState(slot, 0);
                    timer = 0x60;
                }
            } else if (owner.state.readKidPlayState(slot) == 1 && timer < 0x60) {
                runtime.slots[slot] = withVariant(runtime.slots[slot], entity.type() == 0x71 ? 2 : 0);
                if (timer < 0x40 && landed) owner.state.writeKidZSpeed(slot, 8);
            }
            owner.state.writeKidPlayTimer(slot, timer);
            return true;
        }

        private boolean openKidDialog() {

            if (runtime.linkState.actionButtonAHeld
                    && !runtime.linkState.dialogActive
                    && runtime.linkState.dialogCooldown == 0
                    && !runtime.linkState.inventoryAppearing
                    && runtime.linkState.windowY == 0x80
                    && Math.abs(signedByte(linkX - entity.x())) < 0x10
                    && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
                int dialog = (runtime.roomVariables.readInstrument1Flags() & 2) == 0
                        ? 0x18 + (owner.state.postKidHintIndex(1) & 3)
                        : 0x1C + entity.type() - 0x70;
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
            }
            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.EntityStoryController owner,
                linksawakening.world.entity.RoomEntity entity,
                int frame,
                int linkX,
                int linkY) {
            if (inUse) {
                new VillageNpcFrameStep().run(owner, entity, frame, linkX, linkY);
                return;
            }
            inUse = true;
            runtime = null;
            slot = 0;
            z = 0;
            landed = false;
            timer = 0;
            this.owner = owner;
            this.entity = entity;
            this.frame = frame;
            this.linkX = linkX;
            this.linkY = linkY;
            try {
                run();
            } finally {
                inUse = false;
                this.owner = null;
                this.entity = null;
                this.runtime = null;
            }
        }
    }
}
