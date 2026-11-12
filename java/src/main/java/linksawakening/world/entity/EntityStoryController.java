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
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** ROM story and NPC presentation sequences, sharing the owning room slot table. */
public final class EntityStoryController {
    private final RoomEntityRuntime runtime;

    EntityStoryController(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        int slot = entity.slot();
        if (runtime.entityState.kidBall[slot]) {
            if (--runtime.entityState.kidPlayTimer[slot] <= 0) {
                runtime.slots[slot] = withStatus(entity, EntityStatus.DISABLED);
                runtime.entityState.kidBall[slot] = false;
                return;
            }
            int x = addFallingSpeedToPosition(
                    entity.x(), runtime.entityState.kidPlayState[slot], runtime.entityState.kidSpeedXAccumulator, slot);
            int z = addFallingSpeedToPosition(
                    entity.z(),
                    runtime.entityState.kidZSpeed[slot]-- & 255,
                    runtime.entityState.kidSpeedYAccumulator,
                    slot);
            runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
            return;
        }
        if (runtime.entityState.bowWowState == 0x80) {
            advanceKidnapWarningKid(entity, frame, linkX, linkY);
            return;
        }
        pushLinkFromKid(entity, linkX, linkY);
        if (runtime.spriteHandlers != null)
            entity = withDefinition(
                    entity,
                    runtime.spriteHandlers.forKidPlaying(),
                    (entity.type() == 0x71 ? 2 : 0) | (runtime.entityState.kidPlayState[slot] == 1 ? 1 : 0));
        int z = addFallingSpeedToPosition(
                entity.z(),
                runtime.entityState.kidZSpeed[slot]-- & 255,
                runtime.entityState.kidSpeedYAccumulator,
                slot);
        boolean landed = (z & 128) != 0;
        if (landed) {
            z = 0;
            runtime.entityState.kidZSpeed[slot] = 0;
        }
        runtime.slots[slot] = withZ(entity, z);
        int timer = runtime.entityState.kidPlayTimer[slot];
        if (timer > 0) timer--;
        if (timer == 0) {
            if (runtime.entityState.kidPlayState[slot] == 0) {
                runtime.entityState.kidPlayState[slot] = 1;
                timer = 0x80;
                int ball = runtime.findFreeEntitySlot();
                if (ball >= 0 && runtime.spriteHandlers != null) {
                    var definition = runtime.spriteHandlers.forKidBall();
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
                    runtime.initializeHealthGroup(ball);
                    runtime.entityState.enemyPhysicsFlags[ball] = ENTITY_PHYSICS_HARMLESS;
                    runtime.entityState.enemyHitboxFlags[ball] = 0;
                    runtime.entityState.entityOptions1Override[ball] =
                            EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION;
                    runtime.entityState.kidBall[ball] = true;
                    runtime.entityState.kidPlayTimer[ball] = 0x24;
                    runtime.entityState.kidPlayState[ball] = entity.type() == 0x71 ? 0x14 : 0xEC;
                    runtime.entityState.kidZSpeed[ball] = 0x10;
                    runtime.entityState.kidSpeedXAccumulator[ball] = 0;
                    runtime.entityState.kidSpeedYAccumulator[ball] = 0;
                    runtime.entityState.dynamicEntitySpawnedThisFrame[ball] = true;
                }
            } else {
                runtime.entityState.kidPlayState[slot] = 0;
                timer = 0x60;
            }
        } else if (runtime.entityState.kidPlayState[slot] == 1 && timer < 0x60) {
            runtime.slots[slot] = withVariant(runtime.slots[slot], entity.type() == 0x71 ? 2 : 0);
            if (timer < 0x40 && landed) runtime.entityState.kidZSpeed[slot] = 8;
        }
        runtime.entityState.kidPlayTimer[slot] = timer;
        if (runtime.entityState.linkState.actionButtonAHeld
                && !runtime.entityState.linkState.dialogActive
                && runtime.entityState.linkState.dialogCooldown == 0
                && !runtime.entityState.linkState.inventoryAppearing
                && runtime.entityState.linkState.windowY == 0x80
                && Math.abs(signedByte(linkX - entity.x())) < 0x10
                && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
            int dialog = (runtime.entityState.instrument1Flags & 2) == 0
                    ? 0x18 + (runtime.entityState.kidHintIndex++ & 3)
                    : 0x1C + entity.type() - 0x70;
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
        }
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    private void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
    }

    public void advanceKidnapWarningKid(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int facingVariant = ((yDistance < 0 ? 0 : 2) ^ 2) + ((frameCounter >>> 3) & 0x01);
        runtime.slots[slot] = withVariant(entity, facingVariant);
        switch (runtime.entityState.kidKidnapState[slot]) {
            case 0 -> {
                runtime.entityState.kidKidnapDelay[slot] = 0x30;
                runtime.entityState.pendingMusicTrack = 0x0E;
                runtime.entityState.kidKidnapState[slot] = 1;
            }
            case 1 -> {
                if ((yDistance >= -0x20 && yDistance < 0x20 || --runtime.entityState.kidKidnapDelay[slot] == 0)
                        && runtime.entityState.transitionSequenceCounter == 0x04) {
                    if (entity.type() == EntityRomConstants.ENTITY_KID_71) {
                        runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(2, 0x20));
                    }
                    runtime.entityState.kidKidnapState[slot] = 2;
                } else {
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLink(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x08);
                    runtime.slots[slot] = withPositionAndVariant(
                            entity,
                            addFallingSpeedToPosition(
                                    entity.x(), vector.x() & 0xFF, runtime.entityState.kidSpeedXAccumulator, slot),
                            addFallingSpeedToPosition(
                                    entity.y(), vector.y() & 0xFF, runtime.entityState.kidSpeedYAccumulator, slot),
                            facingVariant);
                    runtime.entityState.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
                }
            }
            default -> {
                pushLinkFromKid(entity, linkEntityX, linkEntityY);
                int z = addFallingSpeedToPosition(
                        entity.z(),
                        runtime.entityState.kidZSpeed[slot] & 255,
                        runtime.entityState.kidSpeedYAccumulator,
                        slot);
                runtime.entityState.kidZSpeed[slot] -= 2;
                if (z == 0 || (z & 128) != 0) {
                    z = 0;
                    runtime.entityState.kidZSpeed[slot] = (frameCounter & 31) == 0 ? 0x10 : 0;
                }
                runtime.slots[slot] = withZ(runtime.slots[slot], z);
                int xDistance = Math.abs(signedByte((linkEntityX - entity.x()) & 0xFF));
                if (runtime.entityState.linkState.actionButtonsHeld
                        && !runtime.entityState.linkState.dialogActive
                        && xDistance < 0x18
                        && Math.abs(yDistance) < 0x18) {
                    runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(2, 0x20));
                }
            }
        }
    }

    public void advanceMadamMeowMeow(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        int slot = entity.slot();
        if (runtime.entityState.enemyTransitionCountdown[slot] == 1) {
            runtime.entityState.enemyTransitionCountdown[slot] = 0;
            runtime.entityState.events.pendingMadamMeowMeowFullHealRequests.add(new MadamMeowMeowFullHealRequest(slot));
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int direction = Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        int variant = direction * 2 + ((frameCounter >>> 4) & 0x01);
        runtime.slots[entity.slot()] = withVariant(entity, variant);
        boolean linkFacingMadam = (runtime.entityState.linkState.lastRomLinkDirection ^ 0x01) == direction;
        boolean interactionWindow = ((yDistance + 0x14) & 0xFF) < 0x28 && ((xDistance + 0x10) & 0xFF) < 0x20;
        if (runtime.entityState.linkState.actionButtonAHeld
                && interactionWindow
                && linkFacingMadam
                && !runtime.entityState.linkState.dialogActive
                && !runtime.entityState.linkState.inventoryAppearing
                && !linkAirborne
                && runtime.entityState.linkState.dialogCooldown == 0
                && runtime.entityState.linkState.windowY == 0x80) {
            if ((runtime.entityState.instrument2Flags & 0x02) != 0 && runtime.entityState.bowWowState == 0x01) {
                runtime.entityState.bowWowState = 0;
                runtime.entityState.enemyTransitionCountdown[slot] = 0x10;
                runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, 0x2F));
                return;
            }
            int dialogLowId =
                    switch (runtime.entityState.bowWowState) {
                        case 0 -> 0x30;
                        case 1 -> 0x32;
                        default -> 0x31;
                    };
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, dialogLowId));
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
            runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        if (!runtime.entityState.gameplayWorld
                || runtime.entityState.transitionSequenceCounter != 0x04
                || runtime.entityState.linkState.dialogActive
                || runtime.entityState.linkState.inventoryAppearing
                || runtime.entityState.roomTransitionActive
                || runtime.inspection().witchGotItemPresentationActive()
                || runtime.entityState.owlStatueDialogId < 0) {
            return;
        }
        int dx = signedByte((linkEntityX - entity.x()) & 0xFF);
        int dy = signedByte((linkEntityY - entity.y()) & 0xFF);
        if (runtime.roomInspection.sourceNpcInteractionAllowed(dx, dy, linkAirborne, directionToLink(dx, dy))) {
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(
                    runtime.entityState.owlStatueDialogId >>> 8, runtime.entityState.owlStatueDialogId & 0xFF));
        }
    }

    public void advanceRichard(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        int slot = entity.slot();
        if (!runtime.entityState.gameplayWorld
                || runtime.entityState.transitionSequenceCounter != 0x04
                || runtime.entityState.linkState.dialogActive
                || runtime.entityState.linkState.inventoryAppearing
                || runtime.entityState.roomTransitionActive
                || runtime.inspection().witchGotItemPresentationActive()) {
            return;
        }
        if (runtime.entityState.richardState[slot] == 0 && runtime.entityState.entityGoldenLeavesCount >= 0x06) {
            entity = withPositionAndVariant(entity, 0x58, entity.y(), entity.spriteVariant());
            runtime.entityState.richardDirection[slot] = 3;
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        if ((frameCounter & 0x1F) == 0) {
            runtime.entityState.richardDirection[slot] =
                    Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        }
        int[] baseVariant = {6, 4, 2, 0};
        int variant = baseVariant[runtime.entityState.richardDirection[slot]]
                + ((runtime.entityState.richardInertia[slot]++ >>> 4) & 0x01);
        RoomEntity updated = withVariant(entity, variant);
        if (runtime.entityState.richardState[slot] < 3
                && RoomEntityCombatRules.overlapsLink(updated, linkEntityX, linkEntityY)) {
            runtime.backgroundMechanics.requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
        }

        switch (runtime.entityState.richardState[slot]) {
            case 0 -> {
                if (runtime.entityState.chestSwordLevel != 0) {
                    runtime.entityState.pendingMusicTrack = 0x40;
                }
                runtime.entityState.richardState[slot] = runtime.entityState.entityGoldenLeavesCount >= 0x06 ? 4 : 1;
                if (runtime.entityState.richardState[slot] == 4) {
                    updated = withPositionAndVariant(updated, 0x58, updated.y(), variant);
                }
            }
            case 1 -> {
                if (!runtime.roomInspection.sourceNpcInteractionAllowed(
                        xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                    runtime.slots[slot] = updated;
                    return;
                }
                if (runtime.entityState.bowWowState != 0) {
                    runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, 0x2D));
                } else if ((runtime.entityState.entityRoomStatus & 0x10) == 0
                        || runtime.entityState.entityGoldenLeavesCount == 0) {
                    runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, 0x3A));
                    runtime.entityState.events.pendingRichardProgressEvents.add(new RichardProgressEvent(
                            slot, RichardProgressEvent.Kind.START_QUEST, runtime.entityState.entityGoldenLeavesCount));
                    runtime.entityState.entityRoomStatus |= 0x10;
                    runtime.entityState.richardState[slot] = 2;
                } else if (runtime.entityState.entityGoldenLeavesCount < 5) {
                    runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, 0x3F));
                } else {
                    runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, 0x3D));
                    runtime.entityState.entityGoldenLeavesCount = 0xFF;
                    runtime.entityState.events.pendingRichardProgressEvents.add(
                            new RichardProgressEvent(slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                    runtime.entityState.richardState[slot] = 3;
                    runtime.entityState.enemyTransitionCountdown[slot] = 0x20;
                }
            }
            case 2 -> {
                if (runtime.entityState.linkState.dialogActive || runtime.entityState.linkState.inventoryAppearing) {
                    runtime.slots[slot] = updated;
                    return;
                }
                runtime.entityState.events.pendingDialogRequests.add(
                        new DialogRequest(1, runtime.entityState.linkState.dialogAskSelectionIndex == 0 ? 0x3B : 0x3C));
                runtime.entityState.richardState[slot] = 1;
            }
            case 3 -> {
                if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
                    runtime.entityState.richardState[slot] = 4;
                }
                int x = addFallingSpeedToPosition(updated.x(), 0xF8, runtime.entityState.richardXAccumulator, slot);
                updated = withPositionAndVariant(updated, x, updated.y(), variant);
            }
            case 4 -> {
                if (runtime.roomInspection.sourceNpcInteractionAllowed(
                        xDistance, yDistance, linkAirborne, directionToLink(xDistance, yDistance))) {
                    int dialog = runtime.entityState.entityGoldenLeavesCount == 0x06 ? 0x3E : 0x3D;
                    runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
                }
                if (((linkEntityX - 0x78 + 0x02) & 0xFF) < 0x04 && ((linkEntityY - 0x20 + 0x05) & 0xFF) < 0x0A) {
                    runtime.entityState.events.pendingDungeonWarpRequests.add(new DungeonWarpRequest(
                            slot, new linksawakening.world.entity.event.WarpDestination(0x11, 0xD8, 0x88, 0x70)));
                    runtime.entityState.pendingMusicFadeOutCountdown = 0x30;
                    runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_RICHARD,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x06));
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return;
                }
            }
            default -> throw new IllegalStateException("Invalid Richard state");
        }
        runtime.slots[slot] = updated;
    }

    public void advanceOwlEvent(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean handlerLinkInteractive) {
        int slot = entity.slot();
        switch (runtime.entityState.owlEventState[slot]) {
            case 0 -> {
                if (runtime.entityState.entityRoomId == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
                    if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                        return;
                    }
                    runtime.entityState.owlPreviousMusicTrack[slot] = 0x1D;
                } else if (runtime.entityState.entityRoomId == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
                    if (!runtime.entityState.playerHasTailKey || runtime.entityState.owlPrivateCountdown1[slot] != 0) {
                        return;
                    }
                    runtime.entityState.owlPreviousMusicTrack[slot] =
                            runtime.entityState.owlDefaultMusicResolver.applyAsInt(runtime.entityState.entityRoomId);
                } else if (runtime.entityState.entityRoomId == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
                    if (runtime.entityState.chestSwordLevel == 0
                            || (runtime.entityState.instrument1Flags & 0x02) == 0) {
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                        return;
                    }
                    runtime.entityState.owlPreviousMusicTrack[slot] =
                            runtime.entityState.owlDefaultMusicResolver.applyAsInt(runtime.entityState.entityRoomId);
                    runtime.entityState.pendingMusicTrack = 0x22;
                    runtime.entityState.owlEventState[slot] = 2;
                    return;
                } else if (runtime.entityState.entityRoomId == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                    if (runtime.entityState.chestSwordLevel == 0
                            || runtime.entityState.instrument2Flags != 0
                            || runtime.entityState.bowWowState != 0x01
                            || (runtime.entityState.instrument1Flags & 0x02) == 0) {
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                        return;
                    }
                    runtime.entityState.owlPreviousMusicTrack[slot] =
                            runtime.entityState.owlDefaultMusicResolver.applyAsInt(runtime.entityState.entityRoomId);
                    runtime.entityState.pendingMusicTrack = 0x22;
                    runtime.entityState.owlEventState[slot] = 2;
                    return;
                } else {
                    if (runtime.entityState.chestSwordLevel == 0) {
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                        return;
                    }
                    if (runtime.entityState.entityRoomId != 0x80) {
                        return;
                    }
                    runtime.entityState.owlPreviousMusicTrack[slot] =
                            runtime.entityState.owlDefaultMusicResolver.applyAsInt(runtime.entityState.entityRoomId);
                }
                runtime.entityState.pendingMusicTrack = 0x22;
                runtime.entityState.owlSpeedX[slot] = 0x18;
                runtime.entityState.owlSpeedY[slot] = 0x10;
                runtime.entityState.owlSpeedZ[slot] = 0xFC;
                runtime.slots[slot] = withZ(entity, 0x20);
                runtime.entityState.owlEventState[slot] = 1;
            }
            case 1 -> {
                runtime.entityState.entityInteractionCountdown = 5; // OwlState1Handler, $06:693B.
                runtime.blockLinkForOwl(slot);
                runtime.entityState.events.pendingLinkFacingRequests.add(
                        new LinkFacingRequest(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
                if (runtime.entityState.owlBoomerangSfxCounter[slot] == 0) {
                    runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2D));
                    runtime.entityState.owlBoomerangSfxCounter[slot] = 0x1A;
                } else {
                    runtime.entityState.owlBoomerangSfxCounter[slot]--;
                }
                RoomEntity flying = runtime.roomInspection.owlPresentation(entity, frameCounter);
                int x = addFallingSpeedToPosition(
                        flying.x(), runtime.entityState.owlSpeedX[slot], runtime.entityState.owlXAccumulator, slot);
                int y = addFallingSpeedToPosition(
                        flying.y(), runtime.entityState.owlSpeedY[slot], runtime.entityState.owlYAccumulator, slot);
                if (flying.z() == 0) {
                    runtime.entityState.owlEventState[slot] = 2;
                    runtime.slots[slot] = withPositionAndVariant(flying, x, y, 0);
                    return;
                }
                int z = addFallingSpeedToPosition(
                        flying.z(), runtime.entityState.owlSpeedZ[slot], runtime.entityState.owlZAccumulator, slot);
                if ((frameCounter & 0x03) == 0) {
                    runtime.entityState.owlSpeedX[slot] = approachSignedByteZero(runtime.entityState.owlSpeedX[slot]);
                    runtime.entityState.owlSpeedY[slot] = approachSignedByteZero(runtime.entityState.owlSpeedY[slot]);
                }
                if (signedByte(z) <= 0) z = 0;
                runtime.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
            }
            case 2 -> {
                if (!handlerLinkInteractive || runtime.entityState.transitionSequenceCounter != 0x04) {
                    return;
                }
                if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                    runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
                }
                runtime.slots[slot] = runtime.roomInspection.owlPresentation(entity, frameCounter);
                int globalDialogId = runtime.entityState.owlDialogResolver.applyAsInt(runtime.entityState.entityRoomId);
                runtime.entityState.events.pendingDialogRequests.add(
                        new DialogRequest((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
                runtime.entityState.enemyTransitionCountdown[slot] = 0x10;
                runtime.entityState.owlEventState[slot] = 3;
            }
            case 3 -> {
                if (!handlerLinkInteractive) {
                    return;
                }
                runtime.blockLinkForOwl(slot);
                runtime.slots[slot] = runtime.roomInspection.owlPresentation(entity, frameCounter);
                if (runtime.entityState.linkState.dialogActive) {
                    if (runtime.entityState.enemyTransitionCountdown[slot] > 0) {
                        runtime.entityState.enemyTransitionCountdown[slot]++;
                    }
                    return;
                }
                if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
                    if (!runtime.entityState.owlCompletionEmitted[slot]) {
                        runtime.entityState.events.pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
                        runtime.entityState.owlCompletionEmitted[slot] = true;
                    }
                    runtime.entityState.owlSpeedX[slot] = linkEntityX < entity.x() ? 0x20 : 0xE0;
                    runtime.entityState.owlSpeedY[slot] = linkEntityY < entity.y() ? 0x20 : 0xE0;
                    runtime.entityState.owlSpeedZ[slot] = 0x04;
                    runtime.entityState.owlEventState[slot] = 4;
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
        RoomEntity flying = runtime.roomInspection.owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(
                flying.x(), runtime.entityState.owlSpeedX[slot], runtime.entityState.owlXAccumulator, slot);
        int y = addFallingSpeedToPosition(
                flying.y(), runtime.entityState.owlSpeedY[slot], runtime.entityState.owlYAccumulator, slot);
        int z = addFallingSpeedToPosition(
                flying.z(), runtime.entityState.owlSpeedZ[slot], runtime.entityState.owlZAccumulator, slot);
        runtime.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
        if ((frameCounter & 0x07) == 0) {
            runtime.entityState.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x05));
        }
        if (x < 0x08 || x > 0x98 || y < 0x10 || y > 0x88 || z >= 0x40) {
            runtime.entityState.pendingMusicTrack =
                    runtime.entityState.enemyDropActivePowerUp ? 0x49 : runtime.entityState.owlPreviousMusicTrack[slot];
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
    }

    public void advanceWitchGotItemPresentation(int slot) {
        int countdown = runtime.entityState.witchGotItemCountdown[slot];
        if (countdown == 0) {
            return;
        }
        runtime.presentWitchGotItem(slot);
        if (runtime.entityState.linkState.dialogActive) {
            return;
        }
        countdown--;
        runtime.entityState.witchGotItemCountdown[slot] = countdown;
        if (countdown == 0x02) {
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(0, 0x99));
        }
    }

    public void presentWitchGotItem(int slot) {
        runtime.presentWitchGotItem(slot);
    }

    public void enableBowWowFollowing() {
        runtime.entityState.followingNpcState = new FollowingNpcState(
                runtime.entityState.followingNpcState.roosterFollowing(),
                runtime.entityState.followingNpcState.ghostFollowingState(),
                runtime.entityState.followingNpcState.marinFollowing(),
                true,
                runtime.entityState.followingNpcState.instrument4Flags(),
                runtime.entityState.followingNpcState.powerBraceletLevel(),
                runtime.entityState.followingNpcState.db10());
    }

    public void markBowWowRescued() {
        runtime.entityState.bowWowState = 0x01;
    }

    public void consumeToadstool() {
        runtime.entityState.playerHasToadstool = false;
    }

    public void startWitchGotItemCountdown(int slot) {
        runtime.entityState.witchGotItemCountdown[slot] = 0x2A;
    }

    public int bowWowState() {
        return runtime.entityState.bowWowState;
    }

    public FollowingNpcState followingNpcState() {
        return runtime.entityState.followingNpcState;
    }

    public int booBuddyTriggerCount() {
        return runtime.entityState.booBuddyTriggerCount;
    }

    public boolean playerHasToadstool() {
        return runtime.entityState.playerHasToadstool;
    }

    public int playerMagicPowderCount() {
        return runtime.entityState.playerMagicPowderCount;
    }

    public int bgPaletteEffectAddress() {
        return runtime.entityState.bgPaletteEffectAddress;
    }

    public int paletteDataFlags() {
        return runtime.entityState.paletteDataFlags;
    }

    public void setShouldGetLostInMysteriousWoods(boolean value) {
        runtime.entityState.shouldGetLostInMysteriousWoods = value;
    }
}
