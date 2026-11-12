package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityStoryController. */
final class RichardBehavior {
    static void advanceRichard(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (!runtime.roomVariables().readGameplayWorld()
                || runtime.roomVariables().readTransitionSequenceCounter() != 0x04
                || runtime.linkState().dialogActive
                || runtime.linkState().inventoryAppearing
                || runtime.roomVariables().readRoomTransitionActive()
                || runtime.inspection().witchGotItemPresentationActive()) {
            return;
        }
        if (owner.readRichardState(slot) == 0 && runtime.roomVariables().readEntityGoldenLeavesCount() >= 0x06) {
            entity = withPositionAndVariant(entity, 0x58, entity.y(), entity.spriteVariant());
            owner.writeRichardDirection(slot, 3);
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        if ((frameCounter & 0x1F) == 0) {
            owner.writeRichardDirection(
                    slot,
                    Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3));
        }
        int[] baseVariant = {6, 4, 2, 0};
        int variant =
                baseVariant[owner.readRichardDirection(slot)] + ((owner.postRichardInertia(slot, 1) >>> 4) & 0x01);
        RoomEntity updated = withVariant(entity, variant);
        if (owner.readRichardState(slot) < 3 && RoomEntityCombatRules.overlapsLink(updated, linkEntityX, linkEntityY)) {
            runtime.backgroundMechanics().requestLinkPush(updated, EntityLinkCollisionRules.STANDARD_PUSH);
        }

        switch (owner.readRichardState(slot)) {
            case 0 -> {
                if (runtime.pickups().readChestSwordLevel() != 0) {
                    runtime.presentationState().writePendingMusicTrack(0x40);
                }
                owner.writeRichardState(slot, runtime.roomVariables().readEntityGoldenLeavesCount() >= 0x06 ? 4 : 1);
                if (owner.readRichardState(slot) == 4) {
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
                if (runtime.motions().get(BowWowMotion.class, BowWowMotion::new).readBowWowState() != 0) {
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
                    owner.writeRichardState(slot, 2);
                } else if (runtime.roomVariables().readEntityGoldenLeavesCount() < 5) {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x3F));
                } else {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x3D));
                    runtime.roomVariables().writeEntityGoldenLeavesCount(0xFF);
                    runtime.events()
                            .pendingRichardProgressEvents
                            .add(new RichardProgressEvent(slot, RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF));
                    owner.writeRichardState(slot, 3);
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
                owner.writeRichardState(slot, 1);
            }
            case 3 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    owner.writeRichardState(slot, 4);
                }
                int x = addFallingSpeedToPosition(updated.x(), 0xF8, owner.storageRichardXAccumulator(), slot);
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

    static void enableBowWowFollowing(EntityStoryController owner) {
        var runtime = owner.runtime;
        runtime.motions()
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .writeFollowingNpcState(new FollowingNpcState(
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .roosterFollowing(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .ghostFollowingState(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .marinFollowing(),
                        true,
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .instrument4Flags(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .powerBraceletLevel(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .db10()));
    }
}
