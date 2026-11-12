package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityStoryController. */
final class OwlEventBehavior {
    static void advanceOwlEvent(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean handlerLinkInteractive) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        switch (owner.readOwlEventState(slot)) {
            case 0 -> {
                if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
                    if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                        return;
                    }
                    owner.writeOwlPreviousMusicTrack(slot, 0x1D);
                } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
                    if (!owner.readPlayerHasTailKey() || owner.readOwlPrivateCountdown1(slot) != 0) {
                        return;
                    }
                    owner.writeOwlPreviousMusicTrack(
                            slot,
                            owner.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
                    if (runtime.pickups().readChestSwordLevel() == 0
                            || (runtime.roomVariables().readInstrument1Flags() & 0x02) == 0) {
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                        return;
                    }
                    owner.writeOwlPreviousMusicTrack(
                            slot,
                            owner.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                    runtime.presentationState().writePendingMusicTrack(0x22);
                    owner.writeOwlEventState(slot, 2);
                    return;
                } else if (runtime.roomVariables().readEntityRoomId()
                        == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
                    if (runtime.pickups().readChestSwordLevel() == 0
                            || runtime.roomVariables().readInstrument2Flags() != 0
                            || runtime.motions()
                                            .get(BowWowMotion.class, BowWowMotion::new)
                                            .readBowWowState()
                                    != 0x01
                            || (runtime.roomVariables().readInstrument1Flags() & 0x02) == 0) {
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                        return;
                    }
                    owner.writeOwlPreviousMusicTrack(
                            slot,
                            owner.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                    runtime.presentationState().writePendingMusicTrack(0x22);
                    owner.writeOwlEventState(slot, 2);
                    return;
                } else {
                    if (runtime.pickups().readChestSwordLevel() == 0) {
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                        return;
                    }
                    if (runtime.roomVariables().readEntityRoomId() != 0x80) {
                        return;
                    }
                    owner.writeOwlPreviousMusicTrack(
                            slot,
                            owner.readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
                }
                runtime.presentationState().writePendingMusicTrack(0x22);
                owner.writeOwlSpeedX(slot, 0x18);
                owner.writeOwlSpeedY(slot, 0x10);
                owner.writeOwlSpeedZ(slot, 0xFC);
                runtime.slots()[slot] = withZ(entity, 0x20);
                owner.writeOwlEventState(slot, 1);
            }
            case 1 -> {
                runtime.roomVariables().writeEntityInteractionCountdown(5); // OwlState1Handler, $06:693B.
                runtime.blockLinkForOwl(slot);
                runtime.events()
                        .pendingLinkFacingRequests
                        .add(new LinkFacingRequest(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
                if (owner.readOwlBoomerangSfxCounter(slot) == 0) {
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2D));
                    owner.writeOwlBoomerangSfxCounter(slot, 0x1A);
                } else {
                    owner.postOwlBoomerangSfxCounter(slot, -1);
                }
                RoomEntity flying = runtime.roomInspection().owlPresentation(entity, frameCounter);
                int x = addFallingSpeedToPosition(
                        flying.x(), owner.readOwlSpeedX(slot), owner.storageOwlXAccumulator(), slot);
                int y = addFallingSpeedToPosition(
                        flying.y(), owner.readOwlSpeedY(slot), owner.storageOwlYAccumulator(), slot);
                if (flying.z() == 0) {
                    owner.writeOwlEventState(slot, 2);
                    runtime.slots()[slot] = withPositionAndVariant(flying, x, y, 0);
                    return;
                }
                int z = addFallingSpeedToPosition(
                        flying.z(), owner.readOwlSpeedZ(slot), owner.storageOwlZAccumulator(), slot);
                if ((frameCounter & 0x03) == 0) {
                    owner.writeOwlSpeedX(slot, approachSignedByteZero(owner.readOwlSpeedX(slot)));
                    owner.writeOwlSpeedY(slot, approachSignedByteZero(owner.readOwlSpeedY(slot)));
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
                int globalDialogId = owner.readOwlDialogResolver()
                        .applyAsInt(runtime.roomVariables().readEntityRoomId());
                runtime.events()
                        .pendingDialogRequests
                        .add(new DialogRequest((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
                owner.writeOwlEventState(slot, 3);
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
                    if (!owner.readOwlCompletionEmitted(slot)) {
                        runtime.events().pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
                        owner.writeOwlCompletionEmitted(slot, true);
                    }
                    owner.writeOwlSpeedX(slot, linkEntityX < entity.x() ? 0x20 : 0xE0);
                    owner.writeOwlSpeedY(slot, linkEntityY < entity.y() ? 0x20 : 0xE0);
                    owner.writeOwlSpeedZ(slot, 0x04);
                    owner.writeOwlEventState(slot, 4);
                }
            }
            default -> {
                if (handlerLinkInteractive) {
                    owner.advanceDepartingOwl(entity, slot, frameCounter);
                }
            }
        }
    }

    static void advanceDepartingOwl(EntityStoryController owner, RoomEntity entity, int slot, int frameCounter) {
        var runtime = owner.runtime;
        runtime.blockLinkForOwl(slot);
        RoomEntity flying = runtime.roomInspection().owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(flying.x(), owner.readOwlSpeedX(slot), owner.storageOwlXAccumulator(), slot);
        int y = addFallingSpeedToPosition(flying.y(), owner.readOwlSpeedY(slot), owner.storageOwlYAccumulator(), slot);
        int z = addFallingSpeedToPosition(flying.z(), owner.readOwlSpeedZ(slot), owner.storageOwlZAccumulator(), slot);
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
                                    : owner.readOwlPreviousMusicTrack(slot));
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }
}
