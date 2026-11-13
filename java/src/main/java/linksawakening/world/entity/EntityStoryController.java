package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.addFallingSpeedToPosition;
import static linksawakening.world.entity.EntityMotionMath.approachSignedByteZero;
import static linksawakening.world.entity.EntityMotionMath.directionFromLinkToEntity;
import static linksawakening.world.entity.EntityMotionMath.directionToLink;
import static linksawakening.world.entity.EntityMotionMath.signedByte;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_HARMLESS;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.WitchMotion;

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
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ linksawakening.entity
                            .EntitySpriteHandlerCatalog
                    spriteHandlers,
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

    final RichardStep richardStep = new RichardStep(this);
    final VillageNpcFrameStep villageNpcFrameStep = new VillageNpcFrameStep(this);
    final OwlEventStep owlEventStep = new OwlEventStep(this);

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
        private final linksawakening.world.entity.EntityStoryController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean handlerLinkInteractive;
        private int slot;

        OwlEventStep(linksawakening.world.entity.EntityStoryController owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {

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
            if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
                if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                    return false;
                }
                owner.state.owlState().writeOwlPreviousMusicTrack(slot, 0x1D);
            } else if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
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
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
            } else if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
                if (owner.pickups.readChestSwordLevel() == 0
                        || (owner.roomVariables.readInstrument1Flags() & 0x02) == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
                owner.presentationState.writePendingMusicTrack(0x22);
                owner.state.owlState().writeOwlEventState(slot, 2);
                return false;
            } else if (owner.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                if (owner.pickups.readChestSwordLevel() == 0
                        || owner.roomVariables.readInstrument2Flags() != 0
                        || owner.motions
                                        .get(BowWowMotion.class, BowWowMotion::new)
                                        .readBowWowState()
                                != 0x01
                        || (owner.roomVariables.readInstrument1Flags() & 0x02) == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
                owner.presentationState.writePendingMusicTrack(0x22);
                owner.state.owlState().writeOwlEventState(slot, 2);
                return false;
            } else {
                if (owner.pickups.readChestSwordLevel() == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                if (owner.roomVariables.readEntityRoomId() != 0x80) {
                    return false;
                }
                owner.state
                        .owlState()
                        .writeOwlPreviousMusicTrack(
                                slot,
                                owner.state
                                        .owlState()
                                        .readOwlDefaultMusicResolver()
                                        .applyAsInt(owner.roomVariables.readEntityRoomId()));
            }
            owner.presentationState.writePendingMusicTrack(0x22);
            owner.state.owlState().writeOwlSpeedX(slot, 0x18);
            owner.state.owlState().writeOwlSpeedY(slot, 0x10);
            owner.state.owlState().writeOwlSpeedZ(slot, 0xFC);
            owner.slots[slot] = withZ(entity, 0x20);
            owner.state.owlState().writeOwlEventState(slot, 1);

            return true;
        }

        private boolean advanceState1() {
            owner.roomVariables.writeEntityInteractionCountdown(5); // OwlState1Handler, $06:693B.
            owner.blockLinkForOwl(slot);
            owner.events.pendingLinkFacingRequests.add(
                    new LinkFacingRequest(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
            if (owner.state.owlState().readOwlBoomerangSfxCounter(slot) == 0) {
                owner.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2D));
                owner.state.owlState().writeOwlBoomerangSfxCounter(slot, 0x1A);
            } else {
                owner.state.owlState().postOwlBoomerangSfxCounter(slot, -1);
            }
            RoomEntity flying = owner.roomInspection.owlPresentation(entity, frameCounter);
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
                owner.slots[slot] = withPositionAndVariant(flying, x, y, 0);
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
            owner.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);

            return true;
        }

        private boolean advanceState2() {
            if (!handlerLinkInteractive || owner.roomVariables.readTransitionSequenceCounter() != 0x04) {
                return false;
            }
            if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                owner.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            owner.slots[slot] = owner.roomInspection.owlPresentation(entity, frameCounter);
            int globalDialogId =
                    owner.state.owlState().readOwlDialogResolver().applyAsInt(owner.roomVariables.readEntityRoomId());
            owner.events.pendingDialogRequests.add(
                    new DialogRequest((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
            owner.state.owlState().writeOwlEventState(slot, 3);

            return true;
        }

        private boolean advanceState3() {
            if (!handlerLinkInteractive) {
                return false;
            }
            owner.blockLinkForOwl(slot);
            owner.slots[slot] = owner.roomInspection.owlPresentation(entity, frameCounter);
            if (owner.linkState.dialogActive) {
                if (owner.physicsTables.readEnemyTransitionCountdown(slot) > 0) {
                    owner.physicsTables.postEnemyTransitionCountdown(slot, 1);
                }
                return false;
            }
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                if (!owner.state.owlState().readOwlCompletionEmitted(slot)) {
                    owner.events.pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
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
                new OwlEventStep(owner)
                        .run(owner, entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
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
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {

            slot = entity.slot();
            if (!owner.roomVariables.readGameplayWorld()
                    || owner.roomVariables.readTransitionSequenceCounter() != 0x04
                    || owner.linkState.dialogActive
                    || owner.linkState.inventoryAppearing
                    || owner.roomVariables.readRoomTransitionActive()
                    || owner.inspection.witchGotItemPresentationActive()) {
                return;
            }
            if (owner.state.readRichardState(slot) == 0 && owner.roomVariables.readEntityGoldenLeavesCount() >= 0x06) {
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
                owner.backgroundMechanics.requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
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
            owner.slots[slot] = updated;
        }

        private boolean advanceState0() {
            if (owner.pickups.readChestSwordLevel() != 0) {
                owner.presentationState.writePendingMusicTrack(0x40);
            }
            owner.state.writeRichardState(slot, owner.roomVariables.readEntityGoldenLeavesCount() >= 0x06 ? 4 : 1);
            if (owner.state.readRichardState(slot) == 4) {
                updated = withPositionAndVariant(updated, 0x58, updated.y(), variant);
            }

            return true;
        }

        private boolean advanceState1() {
            if (!owner.roomInspection.sourceNpcInteractionAllowed(
                    xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                owner.slots[slot] = updated;
                return false;
            }
            if (owner.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState() != 0) {
                owner.events.pendingDialogRequests.add(new DialogRequest(1, 0x2D));
            } else if ((owner.roomVariables.readEntityRoomStatus() & 0x10) == 0
                    || owner.roomVariables.readEntityGoldenLeavesCount() == 0) {
                owner.events.pendingDialogRequests.add(new DialogRequest(1, 0x3A));
                owner.events.pendingRichardProgressEvents.add(new RichardProgressEvent(
                        slot,
                        RichardProgressEvent.Kind.START_QUEST,
                        owner.roomVariables.readEntityGoldenLeavesCount()));
                owner.roomVariables.writeEntityRoomStatus(owner.roomVariables.readEntityRoomStatus() | (0x10));
                owner.state.writeRichardState(slot, 2);
            } else if (owner.roomVariables.readEntityGoldenLeavesCount() < 5) {
                owner.events.pendingDialogRequests.add(new DialogRequest(1, 0x3F));
            } else {
                owner.events.pendingDialogRequests.add(new DialogRequest(1, 0x3D));
                owner.roomVariables.writeEntityGoldenLeavesCount(0xFF);
                owner.events.pendingRichardProgressEvents.add(
                        new RichardProgressEvent(slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                owner.state.writeRichardState(slot, 3);
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x20);
            }

            return true;
        }

        private boolean advanceState2() {
            if (owner.linkState.dialogActive || owner.linkState.inventoryAppearing) {
                owner.slots[slot] = updated;
                return false;
            }
            owner.events.pendingDialogRequests.add(
                    new DialogRequest(1, owner.linkState.dialogAskSelectionIndex == 0 ? 0x3B : 0x3C));
            owner.state.writeRichardState(slot, 1);

            return true;
        }

        private boolean advanceState3() {
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                owner.state.writeRichardState(slot, 4);
            }
            int x = addFallingSpeedToPosition(updated.x(), 0xF8, owner.state.storageRichardXAccumulator(), slot);
            updated = withPositionAndVariant(updated, x, updated.y(), variant);

            return true;
        }

        private boolean advanceState4() {
            if (owner.roomInspection.sourceNpcInteractionAllowed(
                    xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                int dialog = owner.roomVariables.readEntityGoldenLeavesCount() == 0x06 ? 0x3E : 0x3D;
                owner.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
            }
            if (((linkEntityX - 0x78 + 0x02) & 0xFF) < 0x04 && ((linkEntityY - 0x20 + 0x05) & 0xFF) < 0x0A) {
                owner.events.pendingDungeonWarpRequests.add(new DungeonWarpRequest(
                        slot, new linksawakening.world.entity.event.WarpDestination(0x11, 0xD8, 0x88, 0x70)));
                owner.presentationState.writePendingMusicFadeOutCountdown(0x30);
                owner.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_RICHARD, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x06));
                owner.lifecycle.disableEntityWithoutPersistence(slot);
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
                new RichardStep(owner).run(owner, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
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
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!advanceKidMotion()) return;
            if (!advancePlayTimer()) return;
            if (!openKidDialog()) return;
        }

        private boolean advanceKidMotion() {

            slot = entity.slot();
            if (owner.state.readKidBall(slot)) {
                if (owner.state.preKidPlayTimer(slot, -1) <= 0) {
                    owner.slots[slot] = withStatus(entity, EntityStatus.DISABLED);
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
                owner.slots[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
                return false;
            }
            if (owner.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState() == 0x80) {
                owner.advanceKidnapWarningKid(entity, frame, linkX, linkY);
                return false;
            }
            owner.pushLinkFromKid(entity, linkX, linkY);
            if (owner.spriteHandlers != null)
                entity = withDefinition(
                        entity,
                        owner.spriteViews.forKidPlaying(),
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
            owner.slots[slot] = withZ(entity, z);
            timer = owner.state.readKidPlayTimer(slot);
            if (timer > 0) timer--;
            return true;
        }

        private boolean advancePlayTimer() {

            if (timer == 0) {
                if (owner.state.readKidPlayState(slot) == 0) {
                    owner.state.writeKidPlayState(slot, 1);
                    timer = 0x80;
                    int ball =
                            EntitySlotAllocator.findFreeSlot(owner.slots, owner.combatTables, owner.slots.length - 1);
                    if (ball >= 0 && owner.spriteHandlers != null) {
                        var definition = owner.spriteViews.forKidBall();
                        owner.slots[ball] = new RoomEntity(
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
                        owner.lifecycle.initializeHealthGroup(ball);
                        owner.physicsTables.writeEnemyPhysicsFlags(ball, ENTITY_PHYSICS_HARMLESS);
                        owner.combatTables.writeEnemyHitboxFlags(ball, 0);
                        owner.physicsTables.writeEntityOptions1Override(
                                ball, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
                        owner.state.writeKidBall(ball, true);
                        owner.state.writeKidPlayTimer(ball, 0x24);
                        owner.state.writeKidPlayState(ball, entity.type() == 0x71 ? 0x14 : 0xEC);
                        owner.state.writeKidZSpeed(ball, 0x10);
                        owner.state.writeKidSpeedXAccumulator(ball, 0);
                        owner.state.writeKidSpeedYAccumulator(ball, 0);
                        owner.physicsTables.writeDynamicEntitySpawnedThisFrame(ball, true);
                    }
                } else {
                    owner.state.writeKidPlayState(slot, 0);
                    timer = 0x60;
                }
            } else if (owner.state.readKidPlayState(slot) == 1 && timer < 0x60) {
                owner.slots[slot] = withVariant(owner.slots[slot], entity.type() == 0x71 ? 2 : 0);
                if (timer < 0x40 && landed) owner.state.writeKidZSpeed(slot, 8);
            }
            owner.state.writeKidPlayTimer(slot, timer);
            return true;
        }

        private boolean openKidDialog() {

            if (owner.linkState.actionButtonAHeld
                    && !owner.linkState.dialogActive
                    && owner.linkState.dialogCooldown == 0
                    && !owner.linkState.inventoryAppearing
                    && owner.linkState.windowY == 0x80
                    && Math.abs(signedByte(linkX - entity.x())) < 0x10
                    && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
                int dialog = (owner.roomVariables.readInstrument1Flags() & 2) == 0
                        ? 0x18 + (owner.state.postKidHintIndex(1) & 3)
                        : 0x1C + entity.type() - 0x70;
                owner.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
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
                new VillageNpcFrameStep(owner).run(owner, entity, frame, linkX, linkY);
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
}
