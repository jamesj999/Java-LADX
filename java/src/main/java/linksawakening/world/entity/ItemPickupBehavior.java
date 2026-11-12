package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityRewardController. */
final class ItemPickupBehavior {
    static void advanceToadstoolPickup(
            EntityRewardController owner, RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if (runtime.linkState().dialogActive) {
            // UpdateEntityTimers is paused while a dialogue is open in the
            // ROM. Undo this runtime's generic tick before holding the item.
            if (countdown > 0) runtime.physicsTables().postEnemyTransitionCountdown(slot, 1);
        } else if (countdown == 0x10) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x0F);
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x0F));
        } else if (countdown == 1) {
            runtime.events().pendingToadstoolRewards.add(new ToadstoolRewardEvent(slot));
            runtime.presentationState()
                    .writePendingMusicTrack(runtime.story()
                            .owlState()
                            .readOwlDefaultMusicResolver()
                            .applyAsInt(runtime.roomVariables().readEntityRoomId()));
            owner.writeToadstoolPickupSequenceActive(slot, false);
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }

    static void advanceSwordPickup(
            EntityRewardController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ) {
        owner.itemPickupFrameStep.run(owner, entity, frameCounter, linkEntityX, linkEntityY, linkZ);
    }

    static void advancePowerUpPickup(
            EntityRewardController owner, RoomEntity entity, int frame, int linkX, int linkY, int linkZ) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (owner.readPowerUpPickupCountdown(slot) == 0x30) {
            int[] xOffsets = {-0x1C, 0x14, -0x1C, 0x14};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.MOVING_SPARKLE,
                                (linkX + xOffsets[variant]) & 0xFF,
                                (linkY + yOffsets[variant]) & 0xFF,
                                variant));
            }
        }
        if (!runtime.linkState().dialogActive) {
            int countdown = owner.prePowerUpPickupCountdown(slot, -1);
            if (countdown == 0x2E) {
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x17));
            }
            if (countdown == 2) {
                owner.writePowerUpPickupCountdown(slot, 1);
                runtime.events().pendingDialogRequests.add(new DialogRequest(0, entity.type() == 0x33 ? 0x08 : 0xEC));
            } else if (countdown == 0) {
                runtime.presentationState().writePendingMusicTrack(0x49);
                runtime.clearEntity(slot);
                return;
            }
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot, true));
        RoomEntity held = withPositionAndVariant(
                entity,
                linkX,
                (linkY - 0x10) & 0xFF,
                entity.type() == 0x33 ? (frame >>> 4) & 1 : entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }
}
