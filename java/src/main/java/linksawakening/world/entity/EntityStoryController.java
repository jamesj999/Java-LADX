package linksawakening.world.entity;

import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.EnemyRecoilMotion;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM story and NPC presentation sequences, sharing the owning room slot table. */
public final class EntityStoryController {
    private final RoomEntityRuntime runtime;
    EntityStoryController(RoomEntityRuntime runtime) { this.runtime = runtime; }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        int slot = entity.slot();
        if (runtime.kidBall[slot]) {
            if (--runtime.kidPlayTimer[slot] <= 0) {
                runtime.slots[slot] = withStatus(entity, EntityStatus.DISABLED);
                runtime.kidBall[slot] = false;
                return;
            }
            int x = addFallingSpeedToPosition(entity.x(), runtime.kidPlayState[slot],
                runtime.kidSpeedXAccumulator, slot);
            int z = addFallingSpeedToPosition(entity.z(), runtime.kidZSpeed[slot]-- & 255,
                runtime.kidSpeedYAccumulator, slot);
            runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
            return;
        }
        if (runtime.bowWowState == 0x80) {
            advanceKidnapWarningKid(entity, frame, linkX, linkY);
            return;
        }
        pushLinkFromKid(entity, linkX, linkY);
        if (runtime.spriteHandlers != null)
            entity = withDefinition(entity, runtime.spriteHandlers.forKidPlaying(),
                (entity.type() == 0x71 ? 2 : 0) | (runtime.kidPlayState[slot] == 1 ? 1 : 0));
        int z = addFallingSpeedToPosition(entity.z(), runtime.kidZSpeed[slot]-- & 255,
            runtime.kidSpeedYAccumulator, slot);
        boolean landed = (z & 128) != 0;
        if (landed) { z = 0; runtime.kidZSpeed[slot] = 0; }
        runtime.slots[slot] = withZ(entity, z);
        int timer = runtime.kidPlayTimer[slot];
        if (timer > 0) timer--;
        if (timer == 0) {
            if (runtime.kidPlayState[slot] == 0) {
                runtime.kidPlayState[slot] = 1;
                timer = 0x80;
                int ball = runtime.findFreeEntitySlot();
                if (ball >= 0 && runtime.spriteHandlers != null) {
                    var definition = runtime.spriteHandlers.forKidBall();
                    runtime.slots[ball] = new RoomEntity(ball, -1, 0x71, entity.x(), entity.y(),
                        EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false);
                    runtime.enemyPhysicsFlags[ball] = ENTITY_PHYSICS_HARMLESS;
                    runtime.enemyHitboxFlags[ball] = 0;
                    runtime.entityOptions1Override[ball] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION;
                    runtime.kidBall[ball] = true;
                    runtime.kidPlayTimer[ball] = 0x24;
                    runtime.kidPlayState[ball] = entity.type() == 0x71 ? 0x14 : 0xEC;
                    runtime.kidZSpeed[ball] = 0x10;
                    runtime.kidSpeedXAccumulator[ball] = 0;
                    runtime.kidSpeedYAccumulator[ball] = 0;
                    runtime.dynamicEntitySpawnedThisFrame[ball] = true;
                }
            } else { runtime.kidPlayState[slot] = 0; timer = 0x60; }
        } else if (runtime.kidPlayState[slot] == 1 && timer < 0x60) {
            runtime.slots[slot] = withVariant(runtime.slots[slot], entity.type() == 0x71 ? 2 : 0);
            if (timer < 0x40 && landed) runtime.kidZSpeed[slot] = 8;
        }
        runtime.kidPlayTimer[slot] = timer;
        if (runtime.linkState.actionButtonAHeld && !runtime.linkState.dialogActive
            && runtime.linkState.dialogCooldown == 0 && !runtime.linkState.inventoryAppearing
            && runtime.linkState.windowY == 0x80
            && Math.abs(signedByte(linkX - entity.x())) < 0x10
            && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
            int dialog = (runtime.instrument1Flags & 2) == 0
                ? 0x18 + (runtime.kidHintIndex++ & 3) : 0x1C + entity.type() - 0x70;
            runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
        }
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    private void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
    }

    public void advanceKidnapWarningKid(RoomEntity entity, int frameCounter,
                                         int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int facingVariant = ((yDistance < 0 ? 0 : 2) ^ 2)
            + ((frameCounter >>> 3) & 0x01);
        runtime.slots[slot] = withVariant(entity, facingVariant);
        switch (runtime.kidKidnapState[slot]) {
            case 0 -> {
                runtime.kidKidnapDelay[slot] = 0x30;
                runtime.pendingMusicTrack = 0x0E;
                runtime.kidKidnapState[slot] = 1;
            }
            case 1 -> {
                if ((yDistance >= -0x20 && yDistance < 0x20
                    || --runtime.kidKidnapDelay[slot] == 0)
                    && runtime.transitionSequenceCounter == 0x04) {
                    if (entity.type() == runtime.ENTITY_KID_71) {
                        runtime.events.pendingDialogRequests.add(new DialogRequest(2, 0x20));
                    }
                    runtime.kidKidnapState[slot] = 2;
                } else {
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLink(
                        entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x08);
                    runtime.slots[slot] = withPositionAndVariant(entity,
                        addFallingSpeedToPosition(entity.x(), vector.x() & 0xFF,
                            runtime.kidSpeedXAccumulator, slot),
                        addFallingSpeedToPosition(entity.y(), vector.y() & 0xFF,
                            runtime.kidSpeedYAccumulator, slot),
                        facingVariant);
                    runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
                }
            }
            default -> {
                pushLinkFromKid(entity, linkEntityX, linkEntityY);
                int z = addFallingSpeedToPosition(entity.z(), runtime.kidZSpeed[slot] & 255,
                    runtime.kidSpeedYAccumulator, slot);
                runtime.kidZSpeed[slot] -= 2;
                if (z == 0 || (z & 128) != 0) {
                    z = 0;
                    runtime.kidZSpeed[slot] = (frameCounter & 31) == 0 ? 0x10 : 0;
                }
                runtime.slots[slot] = withZ(runtime.slots[slot], z);
                int xDistance = Math.abs(signedByte((linkEntityX - entity.x()) & 0xFF));
                if (runtime.linkState.actionButtonsHeld && !runtime.linkState.dialogActive
                    && xDistance < 0x18 && Math.abs(yDistance) < 0x18) {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(2, 0x20));
                }
            }
        }
        }

    public void advanceMadamMeowMeow(RoomEntity entity, int frameCounter,
                                      int linkEntityX, int linkEntityY,
                                      boolean linkAirborne) {
        int slot = entity.slot();
        if (runtime.enemyTransitionCountdown[slot] == 1) {
            runtime.enemyTransitionCountdown[slot] = 0;
            runtime.events.pendingMadamMeowMeowFullHealRequests.add(
                new MadamMeowMeowFullHealRequest(slot));
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int direction = Math.abs(xDistance) >= Math.abs(yDistance)
            ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        int variant = direction * 2 + ((frameCounter >>> 4) & 0x01);
        runtime.slots[entity.slot()] = withVariant(entity, variant);
        boolean linkFacingMadam = (runtime.linkState.lastRomLinkDirection ^ 0x01) == direction;
        boolean interactionWindow = ((yDistance + 0x14) & 0xFF) < 0x28
            && ((xDistance + 0x10) & 0xFF) < 0x20;
        if (runtime.linkState.actionButtonAHeld && interactionWindow && linkFacingMadam
            && !runtime.linkState.dialogActive && !runtime.linkState.inventoryAppearing && !linkAirborne
            && runtime.linkState.dialogCooldown == 0 && runtime.linkState.windowY == 0x80) {
            if ((runtime.instrument2Flags & 0x02) != 0 && runtime.bowWowState == 0x01) {
                runtime.bowWowState = 0;
                runtime.enemyTransitionCountdown[slot] = 0x10;
                runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x2F));
                return;
            }
            int dialogLowId = switch (runtime.bowWowState) {
                case 0 -> 0x30;
                case 1 -> 0x32;
                default -> 0x31;
            };
            runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialogLowId));
        }
        }

    public void advanceOwlStatue(RoomEntity entity, int linkEntityX, int linkEntityY,
                                   int linkZ, int linkMotionState, boolean linkAirborne) {
        // The shipped handler pushes before ReturnIfNonInteractive_18.
        // CheckLinkCollisionWithEnemy uses the already incremented visual Y;
        // translating the Link probe keeps the statue's world position unchanged.
        if (linkZ == 0 && linkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
            && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY - 1)) {
            runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        if (!runtime.gameplayWorld || runtime.transitionSequenceCounter != 0x04
            || runtime.linkState.dialogActive || runtime.linkState.inventoryAppearing || runtime.roomTransitionActive
            || runtime.inspection().witchGotItemPresentationActive() || runtime.owlStatueDialogId < 0) {
            return;
        }
        int dx = signedByte((linkEntityX - entity.x()) & 0xFF);
        int dy = signedByte((linkEntityY - entity.y()) & 0xFF);
        if (runtime.roomInspection.sourceNpcInteractionAllowed(dx, dy, linkAirborne, directionToLink(dx, dy))) {
            runtime.events.pendingDialogRequests.add(new DialogRequest(
                runtime.owlStatueDialogId >>> 8, runtime.owlStatueDialogId & 0xFF));
        }
        }

    public void advanceRichard(RoomEntity entity, int frameCounter,
                                int linkEntityX, int linkEntityY,
                                boolean linkAirborne) {
        int slot = entity.slot();
        if (!runtime.gameplayWorld || runtime.transitionSequenceCounter != 0x04
            || runtime.linkState.dialogActive || runtime.linkState.inventoryAppearing || runtime.roomTransitionActive
            || runtime.inspection().witchGotItemPresentationActive()) {
            return;
        }
        if (runtime.richardState[slot] == 0 && runtime.entityGoldenLeavesCount >= 0x06) {
            entity = withPositionAndVariant(entity, 0x58, entity.y(), entity.spriteVariant());
            runtime.richardDirection[slot] = 3;
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        if ((frameCounter & 0x1F) == 0) {
            runtime.richardDirection[slot] = Math.abs(xDistance) >= Math.abs(yDistance)
                ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        }
        int[] baseVariant = {6, 4, 2, 0};
        int variant = baseVariant[runtime.richardDirection[slot]]
            + ((runtime.richardInertia[slot]++ >>> 4) & 0x01);
        RoomEntity updated = withVariant(entity, variant);
        if (runtime.richardState[slot] < 3
            && RoomEntityCombatRules.overlapsLink(updated, linkEntityX, linkEntityY)) {
            runtime.backgroundMechanics.requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
        }

        switch (runtime.richardState[slot]) {
            case 0 -> {
                if (runtime.chestSwordLevel != 0) {
                    runtime.pendingMusicTrack = 0x40;
                }
                runtime.richardState[slot] = runtime.entityGoldenLeavesCount >= 0x06 ? 4 : 1;
                if (runtime.richardState[slot] == 4) {
                    updated = withPositionAndVariant(updated, 0x58, updated.y(), variant);
                }
            }
            case 1 -> {
                if (!runtime.roomInspection.sourceNpcInteractionAllowed(xDistance, yDistance, linkAirborne,
                    directionToLink(xDistance, yDistance))) {
                    runtime.slots[slot] = updated;
                    return;
                }
                if (runtime.bowWowState != 0) {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x2D));
                } else if ((runtime.entityRoomStatus & 0x10) == 0 || runtime.entityGoldenLeavesCount == 0) {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x3A));
                    runtime.events.pendingRichardProgressEvents.add(new RichardProgressEvent(
                        slot, RichardProgressEvent.Kind.START_QUEST,
                        runtime.entityGoldenLeavesCount));
                    runtime.entityRoomStatus |= 0x10;
                    runtime.richardState[slot] = 2;
                } else if (runtime.entityGoldenLeavesCount < 5) {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x3F));
                } else {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x3D));
                    runtime.entityGoldenLeavesCount = 0xFF;
                    runtime.events.pendingRichardProgressEvents.add(new RichardProgressEvent(
                        slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                    runtime.richardState[slot] = 3;
                    runtime.enemyTransitionCountdown[slot] = 0x20;
                }
            }
            case 2 -> {
                if (runtime.linkState.dialogActive || runtime.linkState.inventoryAppearing) {
                    runtime.slots[slot] = updated;
                    return;
                }
                runtime.events.pendingDialogRequests.add(new DialogRequest(
                    1, runtime.linkState.dialogAskSelectionIndex == 0 ? 0x3B : 0x3C));
                runtime.richardState[slot] = 1;
            }
            case 3 -> {
                if (runtime.enemyTransitionCountdown[slot] == 0) {
                    runtime.richardState[slot] = 4;
                }
                int x = addFallingSpeedToPosition(
                    updated.x(), 0xF8, runtime.richardXAccumulator, slot);
                updated = withPositionAndVariant(updated, x, updated.y(), variant);
            }
            case 4 -> {
                if (runtime.roomInspection.sourceNpcInteractionAllowed(xDistance, yDistance, linkAirborne,
                    directionToLink(xDistance, yDistance))) {
                    int dialog = runtime.entityGoldenLeavesCount == 0x06 ? 0x3E : 0x3D;
                    runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
                }
                if (((linkEntityX - 0x78 + 0x02) & 0xFF) < 0x04
                    && ((linkEntityY - 0x20 + 0x05) & 0xFF) < 0x0A) {
                    runtime.events.pendingDungeonWarpRequests.add(new DungeonWarpRequest(
                        slot, new linksawakening.world.entity.event.WarpDestination(0x11, 0xD8, 0x88, 0x70)));
                    runtime.pendingMusicFadeOutCountdown = 0x30;
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, runtime.ENTITY_RICHARD, 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x06));
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return;
                }
            }
            default -> throw new IllegalStateException("Invalid Richard state");
        }
        runtime.slots[slot] = updated;
        }

    public void advanceOwlEvent(RoomEntity entity, int frameCounter,
                                 int linkEntityX, int linkEntityY,
                                 boolean handlerLinkInteractive) {
        int slot = entity.slot();
        switch (runtime.owlEventState[slot]) {
            case 0 -> {
                if (runtime.entityRoomId == runtime.ROOM_OW_BEACH_WITH_SWORD) {
                    if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                        return;
                    }
                    runtime.owlPreviousMusicTrack[slot] = 0x1D;
                } else if (runtime.entityRoomId == runtime.ROOM_OW_TAIL_KEY_CHEST) {
                    if (!runtime.playerHasTailKey || runtime.owlPrivateCountdown1[slot] != 0) {
                        return;
                    }
                    runtime.owlPreviousMusicTrack[slot] =
                        runtime.owlDefaultMusicResolver.applyAsInt(runtime.entityRoomId);
                } else if (runtime.entityRoomId == runtime.ROOM_OW_WEST_OF_TAIL_CAVE) {
                    if (runtime.chestSwordLevel == 0 || (runtime.instrument1Flags & 0x02) == 0) {
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                        return;
                    }
                    runtime.owlPreviousMusicTrack[slot] =
                        runtime.owlDefaultMusicResolver.applyAsInt(runtime.entityRoomId);
                    runtime.pendingMusicTrack = 0x22;
                    runtime.owlEventState[slot] = 2;
                    return;
                } else if (runtime.entityRoomId == runtime.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                    if (runtime.chestSwordLevel == 0 || runtime.instrument2Flags != 0
                        || runtime.bowWowState != 0x01 || (runtime.instrument1Flags & 0x02) == 0) {
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                        return;
                    }
                    runtime.owlPreviousMusicTrack[slot] =
                        runtime.owlDefaultMusicResolver.applyAsInt(runtime.entityRoomId);
                    runtime.pendingMusicTrack = 0x22;
                    runtime.owlEventState[slot] = 2;
                    return;
                } else {
                    if (runtime.chestSwordLevel == 0) {
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                        return;
                    }
                    if (runtime.entityRoomId != 0x80) {
                        return;
                    }
                    runtime.owlPreviousMusicTrack[slot] = runtime.owlDefaultMusicResolver.applyAsInt(runtime.entityRoomId);
                }
                runtime.pendingMusicTrack = 0x22;
                runtime.owlSpeedX[slot] = 0x18;
                runtime.owlSpeedY[slot] = 0x10;
                runtime.owlSpeedZ[slot] = 0xFC;
                runtime.slots[slot] = withZ(entity, 0x20);
                runtime.owlEventState[slot] = 1;
            }
            case 1 -> {
                runtime.entityInteractionCountdown = 5; // OwlState1Handler, $06:693B.
                runtime.blockLinkForOwl(slot);
                runtime.events.pendingLinkFacingRequests.add(new LinkFacingRequest(slot,
                    directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
                if (runtime.owlBoomerangSfxCounter[slot] == 0) {
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(slot, entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x2D));
                    runtime.owlBoomerangSfxCounter[slot] = 0x1A;
                } else {
                    runtime.owlBoomerangSfxCounter[slot]--;
                }
                RoomEntity flying = runtime.roomInspection.owlPresentation(entity, frameCounter);
                int x = addFallingSpeedToPosition(
                    flying.x(), runtime.owlSpeedX[slot], runtime.owlXAccumulator, slot);
                int y = addFallingSpeedToPosition(
                    flying.y(), runtime.owlSpeedY[slot], runtime.owlYAccumulator, slot);
                if (flying.z() == 0) {
                    runtime.owlEventState[slot] = 2;
                    runtime.slots[slot] = withPositionAndVariant(flying, x, y, 0);
                    return;
                }
                int z = addFallingSpeedToPosition(
                    flying.z(), runtime.owlSpeedZ[slot], runtime.owlZAccumulator, slot);
                if ((frameCounter & 0x03) == 0) {
                    runtime.owlSpeedX[slot] = approachSignedByteZero(runtime.owlSpeedX[slot]);
                    runtime.owlSpeedY[slot] = approachSignedByteZero(runtime.owlSpeedY[slot]);
                }
                if (signedByte(z) <= 0) z = 0;
                runtime.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
            }
            case 2 -> {
                if (!handlerLinkInteractive || runtime.transitionSequenceCounter != 0x04) {
                    return;
                }
                if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                    runtime.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
                }
                runtime.slots[slot] = runtime.roomInspection.owlPresentation(entity, frameCounter);
                int globalDialogId = runtime.owlDialogResolver.applyAsInt(runtime.entityRoomId);
                runtime.events.pendingDialogRequests.add(new DialogRequest(
                    (globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
                runtime.enemyTransitionCountdown[slot] = 0x10;
                runtime.owlEventState[slot] = 3;
            }
            case 3 -> {
                if (!handlerLinkInteractive) {
                    return;
                }
                runtime.blockLinkForOwl(slot);
                runtime.slots[slot] = runtime.roomInspection.owlPresentation(entity, frameCounter);
                if (runtime.linkState.dialogActive) {
                    if (runtime.enemyTransitionCountdown[slot] > 0) {
                        runtime.enemyTransitionCountdown[slot]++;
                    }
                    return;
                }
                if (runtime.enemyTransitionCountdown[slot] == 0) {
                    if (!runtime.owlCompletionEmitted[slot]) {
                        runtime.events.pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
                        runtime.owlCompletionEmitted[slot] = true;
                    }
                    runtime.owlSpeedX[slot] = linkEntityX < entity.x() ? 0x20 : 0xE0;
                    runtime.owlSpeedY[slot] = linkEntityY < entity.y() ? 0x20 : 0xE0;
                    runtime.owlSpeedZ[slot] = 0x04;
                    runtime.owlEventState[slot] = 4;
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
            flying.x(), runtime.owlSpeedX[slot], runtime.owlXAccumulator, slot);
        int y = addFallingSpeedToPosition(
            flying.y(), runtime.owlSpeedY[slot], runtime.owlYAccumulator, slot);
        int z = addFallingSpeedToPosition(
            flying.z(), runtime.owlSpeedZ[slot], runtime.owlZAccumulator, slot);
        runtime.slots[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
        if ((frameCounter & 0x07) == 0) {
            runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x05));
        }
        if (x < 0x08 || x > 0x98 || y < 0x10 || y > 0x88 || z >= 0x40) {
            runtime.pendingMusicTrack = runtime.enemyDropActivePowerUp ? 0x49 : runtime.owlPreviousMusicTrack[slot];
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
        }

    public void advanceWitchGotItemPresentation(int slot) {
        int countdown = runtime.witchGotItemCountdown[slot];
        if (countdown == 0) {
            return;
        }
        runtime.presentWitchGotItem(slot);
        if (runtime.linkState.dialogActive) {
            return;
        }
        countdown--;
        runtime.witchGotItemCountdown[slot] = countdown;
        if (countdown == 0x02) {
            runtime.events.pendingDialogRequests.add(new DialogRequest(0, 0x99));
        }
        }
  public void presentWitchGotItem(int slot) { runtime.presentWitchGotItem(slot); }

  public void enableBowWowFollowing() {
    runtime.followingNpcState =
                new FollowingNpcState(
                    runtime.followingNpcState.roosterFollowing(),
                    runtime.followingNpcState.ghostFollowingState(),
                    runtime.followingNpcState.marinFollowing(),
                    true,
                    runtime.followingNpcState.instrument4Flags(),
                    runtime.followingNpcState.powerBraceletLevel(),
                    runtime.followingNpcState.db10());
  }
  public void markBowWowRescued() { runtime.bowWowState = 0x01; }

  public void consumeToadstool() { runtime.playerHasToadstool = false; }
  public void startWitchGotItemCountdown(int slot) { runtime.witchGotItemCountdown[slot] = 0x2A; }

  public int bowWowState() { return runtime.bowWowState; }
  public FollowingNpcState followingNpcState() { return runtime.followingNpcState; }
  public int booBuddyTriggerCount() { return runtime.booBuddyTriggerCount; }
  public boolean playerHasToadstool() { return runtime.playerHasToadstool; }
  public int playerMagicPowderCount() { return runtime.playerMagicPowderCount; }
  public int bgPaletteEffectAddress() { return runtime.bgPaletteEffectAddress; }
  public int paletteDataFlags() { return runtime.paletteDataFlags; }
  public void setShouldGetLostInMysteriousWoods(boolean value) { runtime.shouldGetLostInMysteriousWoods = value; }
}
