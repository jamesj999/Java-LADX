package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntUnaryOperator;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** ROM story and NPC presentation sequences, sharing the owning room slot table. */
public final class EntityStoryController {
    private boolean shouldGetLostInMysteriousWoods;
    private final int[] kidPlayTimer = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidPlayState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidZSpeed = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] kidBall = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int kidHintIndex;
    private final int[] kidKidnapState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidKidnapDelay = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private boolean playerHasToadstool;
    private int playerMagicPowderCount;
    private boolean playerHasTailKey;
    private int houseTarinFlag;
    private int houseTradeItem;
    private int houseInstrument3Flags;
    private int houseWalrusStatus;
    private final int[] owlEventState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlBoomerangSfxCounter = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlPreviousMusicTrack = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] owlCompletionEmitted = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private IntUnaryOperator owlDialogResolver = roomId -> 0x0D9;
    private int owlStatueDialogId = -1;
    private IntUnaryOperator owlDefaultMusicResolver = roomId -> 0x05;
    private final EntityStoryControllerServices runtime;

    EntityStoryController(EntityStoryControllerServices runtime) {
        this.runtime = runtime;
    }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        int slot = entity.slot();
        if (this.readKidBall(slot)) {
            if (this.preKidPlayTimer(slot, -1) <= 0) {
                runtime.slots()[slot] = withStatus(entity, EntityStatus.DISABLED);
                this.writeKidBall(slot, false);
                return;
            }
            int x = addFallingSpeedToPosition(
                    entity.x(), this.readKidPlayState(slot), this.storageKidSpeedXAccumulator(), slot);
            int z = addFallingSpeedToPosition(
                    entity.z(), this.postKidZSpeed(slot, -1) & 255, this.storageKidSpeedYAccumulator(), slot);
            runtime.slots()[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
            return;
        }
        if (runtime.motionSet().bowWowMotion.readBowWowState() == 0x80) {
            advanceKidnapWarningKid(entity, frame, linkX, linkY);
            return;
        }
        pushLinkFromKid(entity, linkX, linkY);
        if (runtime.spriteHandlers() != null)
            entity = withDefinition(
                    entity,
                    runtime.spriteHandlers().forKidPlaying(),
                    (entity.type() == 0x71 ? 2 : 0) | (this.readKidPlayState(slot) == 1 ? 1 : 0));
        int z = addFallingSpeedToPosition(
                entity.z(), this.postKidZSpeed(slot, -1) & 255, this.storageKidSpeedYAccumulator(), slot);
        boolean landed = (z & 128) != 0;
        if (landed) {
            z = 0;
            this.writeKidZSpeed(slot, 0);
        }
        runtime.slots()[slot] = withZ(entity, z);
        int timer = this.readKidPlayTimer(slot);
        if (timer > 0) timer--;
        if (timer == 0) {
            if (this.readKidPlayState(slot) == 0) {
                this.writeKidPlayState(slot, 1);
                timer = 0x80;
                int ball = runtime.findFreeEntitySlot();
                if (ball >= 0 && runtime.spriteHandlers() != null) {
                    var definition = runtime.spriteHandlers().forKidBall();
                    runtime.slots()[ball] = new RoomEntity(
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
                    runtime.initializeHealthGroup(ball);
                    runtime.physicsTables().writeEnemyPhysicsFlags(ball, ENTITY_PHYSICS_HARMLESS);
                    runtime.combatTables().writeEnemyHitboxFlags(ball, 0);
                    runtime.physicsTables()
                            .writeEntityOptions1Override(ball, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
                    this.writeKidBall(ball, true);
                    this.writeKidPlayTimer(ball, 0x24);
                    this.writeKidPlayState(ball, entity.type() == 0x71 ? 0x14 : 0xEC);
                    this.writeKidZSpeed(ball, 0x10);
                    this.writeKidSpeedXAccumulator(ball, 0);
                    this.writeKidSpeedYAccumulator(ball, 0);
                    runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(ball, true);
                }
            } else {
                this.writeKidPlayState(slot, 0);
                timer = 0x60;
            }
        } else if (this.readKidPlayState(slot) == 1 && timer < 0x60) {
            runtime.slots()[slot] = withVariant(runtime.slots()[slot], entity.type() == 0x71 ? 2 : 0);
            if (timer < 0x40 && landed) this.writeKidZSpeed(slot, 8);
        }
        this.writeKidPlayTimer(slot, timer);
        if (runtime.linkState().actionButtonAHeld
                && !runtime.linkState().dialogActive
                && runtime.linkState().dialogCooldown == 0
                && !runtime.linkState().inventoryAppearing
                && runtime.linkState().windowY == 0x80
                && Math.abs(signedByte(linkX - entity.x())) < 0x10
                && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
            int dialog = (runtime.roomVariables().readInstrument1Flags() & 2) == 0
                    ? 0x18 + (this.postKidHintIndex(1) & 3)
                    : 0x1C + entity.type() - 0x70;
            runtime.events().pendingDialogRequests.add(new DialogRequest(1, dialog));
        }
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    private void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            runtime.backgroundMechanics().requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
    }

    public void advanceKidnapWarningKid(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int facingVariant = ((yDistance < 0 ? 0 : 2) ^ 2) + ((frameCounter >>> 3) & 0x01);
        runtime.slots()[slot] = withVariant(entity, facingVariant);
        switch (this.readKidKidnapState(slot)) {
            case 0 -> {
                this.writeKidKidnapDelay(slot, 0x30);
                runtime.presentationState().writePendingMusicTrack(0x0E);
                this.writeKidKidnapState(slot, 1);
            }
            case 1 -> {
                if ((yDistance >= -0x20 && yDistance < 0x20 || this.preKidKidnapDelay(slot, -1) == 0)
                        && runtime.roomVariables().readTransitionSequenceCounter() == 0x04) {
                    if (entity.type() == EntityRomConstants.ENTITY_KID_71) {
                        runtime.events().pendingDialogRequests.add(new DialogRequest(2, 0x20));
                    }
                    this.writeKidKidnapState(slot, 2);
                } else {
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLink(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x08);
                    runtime.slots()[slot] = withPositionAndVariant(
                            entity,
                            addFallingSpeedToPosition(
                                    entity.x(), vector.x() & 0xFF, this.storageKidSpeedXAccumulator(), slot),
                            addFallingSpeedToPosition(
                                    entity.y(), vector.y() & 0xFF, this.storageKidSpeedYAccumulator(), slot),
                            facingVariant);
                    runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
                }
            }
            default -> {
                pushLinkFromKid(entity, linkEntityX, linkEntityY);
                int z = addFallingSpeedToPosition(
                        entity.z(), this.readKidZSpeed(slot) & 255, this.storageKidSpeedYAccumulator(), slot);
                this.writeKidZSpeed(slot, this.readKidZSpeed(slot) - (2));
                if (z == 0 || (z & 128) != 0) {
                    z = 0;
                    this.writeKidZSpeed(slot, (frameCounter & 31) == 0 ? 0x10 : 0);
                }
                runtime.slots()[slot] = withZ(runtime.slots()[slot], z);
                int xDistance = Math.abs(signedByte((linkEntityX - entity.x()) & 0xFF));
                if (runtime.linkState().actionButtonsHeld
                        && !runtime.linkState().dialogActive
                        && xDistance < 0x18
                        && Math.abs(yDistance) < 0x18) {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(2, 0x20));
                }
            }
        }
    }

    public void advanceMadamMeowMeow(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        int slot = entity.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 1) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
            runtime.events().pendingMadamMeowMeowFullHealRequests.add(new MadamMeowMeowFullHealRequest(slot));
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int direction = Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        int variant = direction * 2 + ((frameCounter >>> 4) & 0x01);
        runtime.slots()[entity.slot()] = withVariant(entity, variant);
        boolean linkFacingMadam = (runtime.linkState().lastRomLinkDirection ^ 0x01) == direction;
        boolean interactionWindow = ((yDistance + 0x14) & 0xFF) < 0x28 && ((xDistance + 0x10) & 0xFF) < 0x20;
        if (runtime.linkState().actionButtonAHeld
                && interactionWindow
                && linkFacingMadam
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !linkAirborne
                && runtime.linkState().dialogCooldown == 0
                && runtime.linkState().windowY == 0x80) {
            if ((runtime.roomVariables().readInstrument2Flags() & 0x02) != 0
                    && runtime.motionSet().bowWowMotion.readBowWowState() == 0x01) {
                runtime.motionSet().bowWowMotion.writeBowWowState(0);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
                runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x2F));
                return;
            }
            int dialogLowId =
                    switch (runtime.motionSet().bowWowMotion.readBowWowState()) {
                        case 0 -> 0x30;
                        case 1 -> 0x32;
                        default -> 0x31;
                    };
            runtime.events().pendingDialogRequests.add(new DialogRequest(1, dialogLowId));
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
            runtime.backgroundMechanics().requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        if (!runtime.roomVariables().readGameplayWorld()
                || runtime.roomVariables().readTransitionSequenceCounter() != 0x04
                || runtime.linkState().dialogActive
                || runtime.linkState().inventoryAppearing
                || runtime.roomVariables().readRoomTransitionActive()
                || runtime.inspection().witchGotItemPresentationActive()
                || this.readOwlStatueDialogId() < 0) {
            return;
        }
        int dx = signedByte((linkEntityX - entity.x()) & 0xFF);
        int dy = signedByte((linkEntityY - entity.y()) & 0xFF);
        if (runtime.roomInspection().sourceNpcInteractionAllowed(dx, dy, linkAirborne, directionToLink(dx, dy))) {
            runtime.events()
                    .pendingDialogRequests
                    .add(new DialogRequest(this.readOwlStatueDialogId() >>> 8, this.readOwlStatueDialogId() & 0xFF));
        }
    }

    public void advanceRichard(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        int slot = entity.slot();
        if (!runtime.roomVariables().readGameplayWorld()
                || runtime.roomVariables().readTransitionSequenceCounter() != 0x04
                || runtime.linkState().dialogActive
                || runtime.linkState().inventoryAppearing
                || runtime.roomVariables().readRoomTransitionActive()
                || runtime.inspection().witchGotItemPresentationActive()) {
            return;
        }
        if (this.readRichardState(slot) == 0 && runtime.roomVariables().readEntityGoldenLeavesCount() >= 0x06) {
            entity = withPositionAndVariant(entity, 0x58, entity.y(), entity.spriteVariant());
            this.writeRichardDirection(slot, 3);
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        if ((frameCounter & 0x1F) == 0) {
            this.writeRichardDirection(
                    slot,
                    Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3));
        }
        int[] baseVariant = {6, 4, 2, 0};
        int variant = baseVariant[this.readRichardDirection(slot)] + ((this.postRichardInertia(slot, 1) >>> 4) & 0x01);
        RoomEntity updated = withVariant(entity, variant);
        if (this.readRichardState(slot) < 3 && RoomEntityCombatRules.overlapsLink(updated, linkEntityX, linkEntityY)) {
            runtime.backgroundMechanics().requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
        }

        switch (this.readRichardState(slot)) {
            case 0 -> {
                if (runtime.pickups().readChestSwordLevel() != 0) {
                    runtime.presentationState().writePendingMusicTrack(0x40);
                }
                this.writeRichardState(slot, runtime.roomVariables().readEntityGoldenLeavesCount() >= 0x06 ? 4 : 1);
                if (this.readRichardState(slot) == 4) {
                    updated = withPositionAndVariant(updated, 0x58, updated.y(), variant);
                }
            }
            case 1 -> {
                if (!runtime.roomInspection()
                        .sourceNpcInteractionAllowed(
                                xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                    runtime.slots()[slot] = updated;
                    return;
                }
                if (runtime.motionSet().bowWowMotion.readBowWowState() != 0) {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x2D));
                } else if ((runtime.roomVariables().readEntityRoomStatus() & 0x10) == 0
                        || runtime.roomVariables().readEntityGoldenLeavesCount() == 0) {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x3A));
                    runtime.events()
                            .pendingRichardProgressEvents
                            .add(new RichardProgressEvent(
                                    slot,
                                    RichardProgressEvent.Kind.START_QUEST,
                                    runtime.roomVariables().readEntityGoldenLeavesCount()));
                    runtime.roomVariables()
                            .writeEntityRoomStatus(runtime.roomVariables().readEntityRoomStatus() | (0x10));
                    this.writeRichardState(slot, 2);
                } else if (runtime.roomVariables().readEntityGoldenLeavesCount() < 5) {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x3F));
                } else {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x3D));
                    runtime.roomVariables().writeEntityGoldenLeavesCount(0xFF);
                    runtime.events()
                            .pendingRichardProgressEvents
                            .add(new RichardProgressEvent(slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                    this.writeRichardState(slot, 3);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x20);
                }
            }
            case 2 -> {
                if (runtime.linkState().dialogActive || runtime.linkState().inventoryAppearing) {
                    runtime.slots()[slot] = updated;
                    return;
                }
                runtime.events()
                        .pendingDialogRequests
                        .add(new DialogRequest(1, runtime.linkState().dialogAskSelectionIndex == 0 ? 0x3B : 0x3C));
                this.writeRichardState(slot, 1);
            }
            case 3 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    this.writeRichardState(slot, 4);
                }
                int x = addFallingSpeedToPosition(updated.x(), 0xF8, this.storageRichardXAccumulator(), slot);
                updated = withPositionAndVariant(updated, x, updated.y(), variant);
            }
            case 4 -> {
                if (runtime.roomInspection()
                        .sourceNpcInteractionAllowed(
                                xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                    int dialog = runtime.roomVariables().readEntityGoldenLeavesCount() == 0x06 ? 0x3E : 0x3D;
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, dialog));
                }
                if (((linkEntityX - 0x78 + 0x02) & 0xFF) < 0x04 && ((linkEntityY - 0x20 + 0x05) & 0xFF) < 0x0A) {
                    runtime.events()
                            .pendingDungeonWarpRequests
                            .add(new DungeonWarpRequest(
                                    slot,
                                    new linksawakening.world.entity.event.WarpDestination(0x11, 0xD8, 0x88, 0x70)));
                    runtime.presentationState().writePendingMusicFadeOutCountdown(0x30);
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot,
                                    EntityRomConstants.ENTITY_RICHARD,
                                    0,
                                    false,
                                    EntityCombatEvent.SoundChannel.NOISE,
                                    0x06));
                    runtime.lifecycle().disableEntityWithoutPersistence(slot);
                    return;
                }
            }
            default -> throw new IllegalStateException("Invalid Richard state");
        }
        runtime.slots()[slot] = updated;
    }

    public void advanceOwlEvent(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean handlerLinkInteractive) {
        int slot = entity.slot();
        switch (this.readOwlEventState(slot)) {
            case 0 -> {
                if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
                    if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                        return;
                    }
                    this.writeOwlPreviousMusicTrack(slot, 0x1D);
                } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
                    if (!this.readPlayerHasTailKey() || this.readOwlPrivateCountdown1(slot) != 0) {
                        return;
                    }
                    this.writeOwlPreviousMusicTrack(
                            slot,
                            this.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
                    if (runtime.pickups().readChestSwordLevel() == 0
                            || (runtime.roomVariables().readInstrument1Flags() & 0x02) == 0) {
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                        return;
                    }
                    this.writeOwlPreviousMusicTrack(
                            slot,
                            this.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                    runtime.presentationState().writePendingMusicTrack(0x22);
                    this.writeOwlEventState(slot, 2);
                    return;
                } else if (runtime.roomVariables().readEntityRoomId()
                        == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                    if (runtime.pickups().readChestSwordLevel() == 0
                            || runtime.roomVariables().readInstrument2Flags() != 0
                            || runtime.motionSet().bowWowMotion.readBowWowState() != 0x01
                            || (runtime.roomVariables().readInstrument1Flags() & 0x02) == 0) {
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                        return;
                    }
                    this.writeOwlPreviousMusicTrack(
                            slot,
                            this.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                    runtime.presentationState().writePendingMusicTrack(0x22);
                    this.writeOwlEventState(slot, 2);
                    return;
                } else {
                    if (runtime.pickups().readChestSwordLevel() == 0) {
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                        return;
                    }
                    if (runtime.roomVariables().readEntityRoomId() != 0x80) {
                        return;
                    }
                    this.writeOwlPreviousMusicTrack(
                            slot,
                            this.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                }
                runtime.presentationState().writePendingMusicTrack(0x22);
                this.writeOwlSpeedX(slot, 0x18);
                this.writeOwlSpeedY(slot, 0x10);
                this.writeOwlSpeedZ(slot, 0xFC);
                runtime.slots()[slot] = withZ(entity, 0x20);
                this.writeOwlEventState(slot, 1);
            }
            case 1 -> {
                runtime.roomVariables().writeEntityInteractionCountdown(5); // OwlState1Handler, $06:693B.
                runtime.blockLinkForOwl(slot);
                runtime.events()
                        .pendingLinkFacingRequests
                        .add(new LinkFacingRequest(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
                if (this.readOwlBoomerangSfxCounter(slot) == 0) {
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2D));
                    this.writeOwlBoomerangSfxCounter(slot, 0x1A);
                } else {
                    this.postOwlBoomerangSfxCounter(slot, -1);
                }
                RoomEntity flying = runtime.roomInspection().owlPresentation(entity, frameCounter);
                int x = addFallingSpeedToPosition(
                        flying.x(), this.readOwlSpeedX(slot), this.storageOwlXAccumulator(), slot);
                int y = addFallingSpeedToPosition(
                        flying.y(), this.readOwlSpeedY(slot), this.storageOwlYAccumulator(), slot);
                if (flying.z() == 0) {
                    this.writeOwlEventState(slot, 2);
                    runtime.slots()[slot] = withPositionAndVariant(flying, x, y, 0);
                    return;
                }
                int z = addFallingSpeedToPosition(
                        flying.z(), this.readOwlSpeedZ(slot), this.storageOwlZAccumulator(), slot);
                if ((frameCounter & 0x03) == 0) {
                    this.writeOwlSpeedX(slot, approachSignedByteZero(this.readOwlSpeedX(slot)));
                    this.writeOwlSpeedY(slot, approachSignedByteZero(this.readOwlSpeedY(slot)));
                }
                if (signedByte(z) <= 0) z = 0;
                runtime.slots()[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
            }
            case 2 -> {
                if (!handlerLinkInteractive || runtime.roomVariables().readTransitionSequenceCounter() != 0x04) {
                    return;
                }
                if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                    runtime.backgroundMechanics().requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
                }
                runtime.slots()[slot] = runtime.roomInspection().owlPresentation(entity, frameCounter);
                int globalDialogId = this.readOwlDialogResolver()
                        .applyAsInt(runtime.roomVariables().readEntityRoomId());
                runtime.events()
                        .pendingDialogRequests
                        .add(new DialogRequest((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
                this.writeOwlEventState(slot, 3);
            }
            case 3 -> {
                if (!handlerLinkInteractive) {
                    return;
                }
                runtime.blockLinkForOwl(slot);
                runtime.slots()[slot] = runtime.roomInspection().owlPresentation(entity, frameCounter);
                if (runtime.linkState().dialogActive) {
                    if (runtime.physicsTables().readEnemyTransitionCountdown(slot) > 0) {
                        runtime.physicsTables().postEnemyTransitionCountdown(slot, 1);
                    }
                    return;
                }
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    if (!this.readOwlCompletionEmitted(slot)) {
                        runtime.events().pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
                        this.writeOwlCompletionEmitted(slot, true);
                    }
                    this.writeOwlSpeedX(slot, linkEntityX < entity.x() ? 0x20 : 0xE0);
                    this.writeOwlSpeedY(slot, linkEntityY < entity.y() ? 0x20 : 0xE0);
                    this.writeOwlSpeedZ(slot, 0x04);
                    this.writeOwlEventState(slot, 4);
                }
            }
            default -> {
                if (handlerLinkInteractive) {
                    advanceDepartingOwl(entity, slot, frameCounter);
                }
            }
        }
    }

    void advanceDepartingOwl(RoomEntity entity, int slot, int frameCounter) {
        runtime.blockLinkForOwl(slot);
        RoomEntity flying = runtime.roomInspection().owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(flying.x(), this.readOwlSpeedX(slot), this.storageOwlXAccumulator(), slot);
        int y = addFallingSpeedToPosition(flying.y(), this.readOwlSpeedY(slot), this.storageOwlYAccumulator(), slot);
        int z = addFallingSpeedToPosition(flying.z(), this.readOwlSpeedZ(slot), this.storageOwlZAccumulator(), slot);
        runtime.slots()[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
        if ((frameCounter & 0x07) == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x05));
        }
        if (x < 0x08 || x > 0x98 || y < 0x10 || y > 0x88 || z >= 0x40) {
            runtime.presentationState()
                    .writePendingMusicTrack(
                            runtime.bossRewards().readEnemyDropActivePowerUp()
                                    ? 0x49
                                    : this.readOwlPreviousMusicTrack(slot));
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }

    public void advanceWitchGotItemPresentation(int slot) {
        int countdown = runtime.motionSet().witchMotion.readWitchGotItemCountdown(slot);
        if (countdown == 0) {
            return;
        }
        runtime.presentWitchGotItem(slot);
        if (runtime.linkState().dialogActive) {
            return;
        }
        countdown--;
        runtime.motionSet().witchMotion.writeWitchGotItemCountdown(slot, countdown);
        if (countdown == 0x02) {
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x99));
        }
    }

    public void presentWitchGotItem(int slot) {
        runtime.presentWitchGotItem(slot);
    }

    public void enableBowWowFollowing() {
        runtime.motionSet()
                .followingNpcMotion
                .writeFollowingNpcState(new FollowingNpcState(
                        runtime.motionSet()
                                .followingNpcMotion
                                .readFollowingNpcState()
                                .roosterFollowing(),
                        runtime.motionSet()
                                .followingNpcMotion
                                .readFollowingNpcState()
                                .ghostFollowingState(),
                        runtime.motionSet()
                                .followingNpcMotion
                                .readFollowingNpcState()
                                .marinFollowing(),
                        true,
                        runtime.motionSet()
                                .followingNpcMotion
                                .readFollowingNpcState()
                                .instrument4Flags(),
                        runtime.motionSet()
                                .followingNpcMotion
                                .readFollowingNpcState()
                                .powerBraceletLevel(),
                        runtime.motionSet()
                                .followingNpcMotion
                                .readFollowingNpcState()
                                .db10()));
    }

    public void markBowWowRescued() {
        runtime.motionSet().bowWowMotion.writeBowWowState(0x01);
    }

    public void consumeToadstool() {
        this.writePlayerHasToadstool(false);
    }

    public void startWitchGotItemCountdown(int slot) {
        runtime.motionSet().witchMotion.writeWitchGotItemCountdown(slot, 0x2A);
    }

    public int bowWowState() {
        return runtime.motionSet().bowWowMotion.readBowWowState();
    }

    public FollowingNpcState followingNpcState() {
        return runtime.motionSet().followingNpcMotion.readFollowingNpcState();
    }

    public int booBuddyTriggerCount() {
        return runtime.motionSet().booBuddyMotion.readBooBuddyTriggerCount();
    }

    public boolean playerHasToadstool() {
        return this.readPlayerHasToadstool();
    }

    public int playerMagicPowderCount() {
        return this.readPlayerMagicPowderCount();
    }

    public int bgPaletteEffectAddress() {
        return runtime.presentationState().readBgPaletteEffectAddress();
    }

    public int paletteDataFlags() {
        return runtime.presentationState().readPaletteDataFlags();
    }

    public void setShouldGetLostInMysteriousWoods(boolean value) {
        this.writeShouldGetLostInMysteriousWoods(value);
    }

    boolean readShouldGetLostInMysteriousWoods() {
        return shouldGetLostInMysteriousWoods;
    }

    boolean writeShouldGetLostInMysteriousWoods(boolean value) {
        return shouldGetLostInMysteriousWoods = value;
    }

    int readKidPlayTimer(int slot) {
        return kidPlayTimer[slot];
    }

    int writeKidPlayTimer(int slot, int value) {
        return kidPlayTimer[slot] = value;
    }

    int[] storageKidPlayTimer() {
        return kidPlayTimer;
    }

    int preKidPlayTimer(int slot, int delta) {
        return kidPlayTimer[slot] += delta;
    }

    int readKidPlayState(int slot) {
        return kidPlayState[slot];
    }

    int writeKidPlayState(int slot, int value) {
        return kidPlayState[slot] = value;
    }

    int[] storageKidPlayState() {
        return kidPlayState;
    }

    int readKidZSpeed(int slot) {
        return kidZSpeed[slot];
    }

    int writeKidZSpeed(int slot, int value) {
        return kidZSpeed[slot] = value;
    }

    int[] storageKidZSpeed() {
        return kidZSpeed;
    }

    int postKidZSpeed(int slot, int delta) {
        int previous = kidZSpeed[slot];
        kidZSpeed[slot] += delta;
        return previous;
    }

    boolean readKidBall(int slot) {
        return kidBall[slot];
    }

    boolean writeKidBall(int slot, boolean value) {
        return kidBall[slot] = value;
    }

    boolean[] storageKidBall() {
        return kidBall;
    }

    int postKidHintIndex(int delta) {
        int previous = kidHintIndex;
        kidHintIndex += delta;
        return previous;
    }

    int readKidKidnapState(int slot) {
        return kidKidnapState[slot];
    }

    int writeKidKidnapState(int slot, int value) {
        return kidKidnapState[slot] = value;
    }

    int[] storageKidKidnapState() {
        return kidKidnapState;
    }

    int writeKidKidnapDelay(int slot, int value) {
        return kidKidnapDelay[slot] = value;
    }

    int[] storageKidKidnapDelay() {
        return kidKidnapDelay;
    }

    int preKidKidnapDelay(int slot, int delta) {
        return kidKidnapDelay[slot] += delta;
    }

    int writeKidSpeedXAccumulator(int slot, int value) {
        return kidSpeedXAccumulator[slot] = value;
    }

    int[] storageKidSpeedXAccumulator() {
        return kidSpeedXAccumulator;
    }

    int writeKidSpeedYAccumulator(int slot, int value) {
        return kidSpeedYAccumulator[slot] = value;
    }

    int[] storageKidSpeedYAccumulator() {
        return kidSpeedYAccumulator;
    }

    int readRichardState(int slot) {
        return richardState[slot];
    }

    int writeRichardState(int slot, int value) {
        return richardState[slot] = value;
    }

    int readRichardDirection(int slot) {
        return richardDirection[slot];
    }

    int writeRichardDirection(int slot, int value) {
        return richardDirection[slot] = value;
    }

    int[] storageRichardXAccumulator() {
        return richardXAccumulator;
    }

    int postRichardInertia(int slot, int delta) {
        int previous = richardInertia[slot];
        richardInertia[slot] += delta;
        return previous;
    }

    boolean readPlayerHasToadstool() {
        return playerHasToadstool;
    }

    boolean writePlayerHasToadstool(boolean value) {
        return playerHasToadstool = value;
    }

    int readPlayerMagicPowderCount() {
        return playerMagicPowderCount;
    }

    int writePlayerMagicPowderCount(int value) {
        return playerMagicPowderCount = value;
    }

    boolean readPlayerHasTailKey() {
        return playerHasTailKey;
    }

    boolean writePlayerHasTailKey(boolean value) {
        return playerHasTailKey = value;
    }

    int readHouseTarinFlag() {
        return houseTarinFlag;
    }

    int writeHouseTarinFlag(int value) {
        return houseTarinFlag = value;
    }

    int readHouseTradeItem() {
        return houseTradeItem;
    }

    int writeHouseTradeItem(int value) {
        return houseTradeItem = value;
    }

    int readHouseInstrument3Flags() {
        return houseInstrument3Flags;
    }

    int writeHouseInstrument3Flags(int value) {
        return houseInstrument3Flags = value;
    }

    int readHouseWalrusStatus() {
        return houseWalrusStatus;
    }

    int writeHouseWalrusStatus(int value) {
        return houseWalrusStatus = value;
    }

    int readOwlEventState(int slot) {
        return owlEventState[slot];
    }

    int writeOwlEventState(int slot, int value) {
        return owlEventState[slot] = value;
    }

    int readOwlPrivateCountdown1(int slot) {
        return owlPrivateCountdown1[slot];
    }

    int writeOwlPrivateCountdown1(int slot, int value) {
        return owlPrivateCountdown1[slot] = value;
    }

    int postOwlPrivateCountdown1(int slot, int delta) {
        int previous = owlPrivateCountdown1[slot];
        owlPrivateCountdown1[slot] += delta;
        return previous;
    }

    int readOwlSpeedX(int slot) {
        return owlSpeedX[slot];
    }

    int writeOwlSpeedX(int slot, int value) {
        return owlSpeedX[slot] = value;
    }

    int readOwlSpeedY(int slot) {
        return owlSpeedY[slot];
    }

    int writeOwlSpeedY(int slot, int value) {
        return owlSpeedY[slot] = value;
    }

    int readOwlSpeedZ(int slot) {
        return owlSpeedZ[slot];
    }

    int writeOwlSpeedZ(int slot, int value) {
        return owlSpeedZ[slot] = value;
    }

    int[] storageOwlXAccumulator() {
        return owlXAccumulator;
    }

    int[] storageOwlYAccumulator() {
        return owlYAccumulator;
    }

    int[] storageOwlZAccumulator() {
        return owlZAccumulator;
    }

    int readOwlBoomerangSfxCounter(int slot) {
        return owlBoomerangSfxCounter[slot];
    }

    int writeOwlBoomerangSfxCounter(int slot, int value) {
        return owlBoomerangSfxCounter[slot] = value;
    }

    int postOwlBoomerangSfxCounter(int slot, int delta) {
        int previous = owlBoomerangSfxCounter[slot];
        owlBoomerangSfxCounter[slot] += delta;
        return previous;
    }

    int readOwlPreviousMusicTrack(int slot) {
        return owlPreviousMusicTrack[slot];
    }

    int writeOwlPreviousMusicTrack(int slot, int value) {
        return owlPreviousMusicTrack[slot] = value;
    }

    boolean readOwlCompletionEmitted(int slot) {
        return owlCompletionEmitted[slot];
    }

    boolean writeOwlCompletionEmitted(int slot, boolean value) {
        return owlCompletionEmitted[slot] = value;
    }

    IntUnaryOperator readOwlDialogResolver() {
        return owlDialogResolver;
    }

    IntUnaryOperator writeOwlDialogResolver(IntUnaryOperator value) {
        return owlDialogResolver = value;
    }

    int readOwlStatueDialogId() {
        return owlStatueDialogId;
    }

    int writeOwlStatueDialogId(int value) {
        return owlStatueDialogId = value;
    }

    IntUnaryOperator readOwlDefaultMusicResolver() {
        return owlDefaultMusicResolver;
    }

    IntUnaryOperator writeOwlDefaultMusicResolver(IntUnaryOperator value) {
        return owlDefaultMusicResolver = value;
    }
}
