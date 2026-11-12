package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Per-invocation scratch for ItemPickupBehavior.advanceSwordPickup; values are assigned in source order. */
final class ItemPickupFrameStep {
    private linksawakening.world.entity.EntityRewardController owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int linkZ;
    private linksawakening.world.entity.EntityRewardControllerServices runtime;
    private int slot;

    ItemPickupFrameStep(
            linksawakening.world.entity.EntityRewardController owner,
            linksawakening.world.entity.RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ) {
        this.owner = owner;
        this.entity = entity;
        this.frameCounter = frameCounter;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.linkZ = linkZ;
    }

    void run() {
        runtime = owner.runtime;
        slot = entity.slot();
        if (runtime.linkState().dialogActive) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) > 0) {
                runtime.physicsTables().postEnemyTransitionCountdown(slot, 1);
            }
            if (owner.readSwordPickupState(slot) != 2) {
                runtime.holdSwordAboveLink(
                        entity, linkEntityX, linkEntityY, linkZ, owner.readSwordPickupState(slot) == 3);
            } else {
                runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
            }
            return;
        }
        if (owner.readSwordPickupState(slot) == 1) {
            runtime.lifecycle().decrementSlowTransitionCountdown(slot, frameCounter);
        }
        switch (owner.readSwordPickupState(slot)) {
            case 0 -> {
                if (!advanceState0()) return;
            }
            case 1 -> {
                if (!advanceState1()) return;
            }
            case 2 -> {
                if (!advanceState2()) return;
            }
            default -> {
                if (!advanceDefault()) return;
            }
        }
    }

    private boolean advanceState0() {
        runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if (countdown == 0x10) {
            runtime.physicsTables().postEnemyTransitionCountdown(slot, -1);
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x9B));
        } else if (countdown == 1) {
            runtime.presentationState().writePendingMusicTrack(0x31);
            runtime.physicsTables().writeSlowTransitionCountdown(slot, 0x52);
            runtime.physicsTables().writeSlowTimerInitialized(slot, true);
            owner.writeSwordPickupState(slot, 1);
        }

        return true;
    }

    private boolean advanceState1() {
        runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
        if (runtime.physicsTables().readSlowTransitionCountdown(slot) == 0) {
            runtime.slots()[slot] = withVariant(runtime.slots()[slot], -1);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x20);
            owner.writeSwordPickupState(slot, 2);
            runtime.events().pendingLinkSwordSpinPoseRequests.add(new LinkSwordSpinPoseRequest(slot, 0x20));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x03));
        }

        return true;
    }

    private boolean advanceState2() {
        // SwordShieldPickableState2Handler only advances the entity
        // countdown and restores variant 0; Link's spin handler owns
        // the motion lock and animation pose for these frames.
        runtime.events()
                .pendingLinkSwordSpinPoseRequests
                .add(new LinkSwordSpinPoseRequest(slot, runtime.physicsTables().readEnemyTransitionCountdown(slot)));
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x20);
            runtime.slots()[slot] = withVariant(runtime.slots()[slot], 0);
            owner.writeSwordPickupState(slot, 3);
        }

        return true;
    }

    private boolean advanceDefault() {
        runtime.events().pendingLinkSwordFinalPoseRequests.add(new LinkSwordFinalPoseRequest(slot));
        runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, true);
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0x1A) {
            runtime.events()
                    .transientVfxRequests
                    .add(new TransientVfxRequest(TransientVfxType.SWORD_POKE, entity.x(), (entity.y() - 0x0C) & 0xFF));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x07));
        }
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            // The ROM leaves MUSIC_OVERWORLD_INTRO playing after the pose;
            // the track data itself continues into the overworld loop.
            runtime.events().pendingSwordPickupRewards.add(new SwordPickupRewardEvent(slot));
            owner.writeSwordPickupSequenceActive(slot, false);
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }

        return true;
    }
}
