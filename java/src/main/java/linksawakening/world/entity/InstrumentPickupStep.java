package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Reused, reentrancy-safe scratch for InstrumentPickupBehavior.advanceInstrumentPickup; values are assigned in source order. */
final class InstrumentPickupStep {
    private linksawakening.world.entity.EntityRewardController owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int linkZ;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.EntityRewardController runtime;
    private int slot;
    private linksawakening.world.entity.RoomEntity held;

    InstrumentPickupStep() {}

    void run() {
        if (!prepareSparkles()) return;
        if (!advancePickupSequence()) return;
        if (!holdPickupPose()) return;
    }

    private boolean prepareSparkles() {

        runtime = owner;
        slot = entity.slot();
        if (owner.state.readInstrumentPickupSparklesPending(slot)) {
            owner.state.writeInstrumentPickupSparklesPending(slot, false);
            int[] xOffsets = {-0x18, 0x18, -0x18, 0x18};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                runtime.events.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.MOVING_SPARKLE,
                        (linkEntityX + xOffsets[variant]) & 0xFF,
                        (linkEntityY + yOffsets[variant]) & 0xFF,
                        variant));
            }
        }
        return true;
    }

    private boolean advancePickupSequence() {

        if (owner.state.readInstrumentPickupState(slot) == 0
                && runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0x10) {
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x0F);
            owner.state.writeInstrumentPickupState(slot, 1);
            runtime.events.pendingDialogRequests.add(
                    new DialogRequest(1, runtime.roomVariables.readEntityMapId() & 0xFF));
            runtime.events.pendingInstrumentRewards.add(new InstrumentRewardEvent(slot));
        } else if (owner.state.readInstrumentPickupState(slot) == 1
                && !runtime.presentationState.readActiveMusic()
                && !runtime.linkState.dialogActive) {
            int mapId = runtime.roomVariables.readEntityMapId() & 0xFF;
            if (mapId < EntityRomConstants.INSTRUMENT_MUSIC_TRACKS.length) {
                runtime.presentationState.writePendingMusicTrack(EntityRomConstants.INSTRUMENT_MUSIC_TRACKS[mapId]);
                owner.state.writeInstrumentPickupState(slot, 2);
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
            }
        } else if (owner.state.readInstrumentPickupState(slot) == 2
                && runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
            owner.state.writeInstrumentPickupState(slot, 3);
            owner.state.writeInstrumentWarpX(slot, (entity.x() - 1) & 255); // $03:5F43
            owner.state.writeInstrumentWarpY(slot, entity.y());
            runtime.physicsTables.writeSlowTransitionCountdown(slot, 0x80);
            runtime.physicsTables.writeSlowTimerInitialized(slot, true);
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x2B));
        } else if (owner.state.readInstrumentPickupState(slot) == 2) {
            owner.state.writeInstrumentParticleSpawnCountdown(
                    slot, (owner.state.readInstrumentParticleSpawnCountdown(slot) - 1) & 0xFF);
            if (owner.state.readInstrumentParticleSpawnCountdown(slot) == 0xFF) {
                owner.state.writeInstrumentParticleSpawnCountdown(slot, 0x17);
                owner.state.writeInstrumentParticleSpawnCounter(
                        slot, (owner.state.readInstrumentParticleSpawnCounter(slot) + 1) & 0xFF);
                owner.spawnInstrumentParticle(
                        entity,
                        owner.state.readInstrumentParticleSpawnCounter(slot) & 0x01,
                        randomByteSupplier.getAsInt() & 0x01);
            }
        } else if (owner.state.readInstrumentPickupState(slot) == 3
                && !runtime.linkState.dialogActive
                && !runtime.linkState.inventoryAppearing
                && !runtime.roomVariables.readRoomTransitionActive()) {
            return advanceWarpFade();
        }
        return true;
    }

    private boolean holdPickupPose() {

        runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        held = withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
        return true;
    }

    private boolean advanceWarpFade() {
        runtime.lifecycle.decrementSlowTransitionCountdown(slot, frameCounter);
        // $03:5E29: the warp controller advances its fade every eight
        // frames, then starts the fullscreen hint controller ($07:6523).
        if (owner.state.readInstrumentHintCountdown(slot) > 0) {
            owner.state.writeInstrumentHintBatches(slot, Math.min(9, owner.state.readInstrumentHintBatches(slot) + 1));
            if (owner.state.preInstrumentHintCountdown(slot, -1) == 0 && !owner.state.readInstrumentHintOpened(slot)) {
                owner.state.writeInstrumentHintOpened(slot, true);
                runtime.events.pendingDialogRequests.add(
                        new DialogRequest(1, 0x50 + (runtime.roomVariables.readEntityMapId() & 0xFF)));
            }
        }
        if (runtime.physicsTables.readSlowTransitionCountdown(slot) < 0x50
                && owner.state.readInstrumentWarpFadeStep(slot) < 0x19
                && (frameCounter & 7) == 0) {
            int step = owner.state.postInstrumentWarpFadeStep(slot, 1);
            if (step == 0) {
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2C));
            }
            if (step == 0x18) owner.state.writeInstrumentHintCountdown(slot, 0x60);
        }
        // func_020_6D0E raises RGB555 channels by one on even frames.
        // BG starts at private state 5; OBJ starts at state 15.
        int fadeStep = owner.state.readInstrumentWarpFadeStep(slot);
        if (runtime.physicsTables.readSlowTransitionCountdown(slot) < 0x50
                && fadeStep <= 0x19
                && (frameCounter & 1) == 0) {
            if (fadeStep >= 5)
                owner.state.writeInstrumentBackgroundFade(
                        slot, Math.min(31, owner.state.readInstrumentBackgroundFade(slot) + 1));
            if (fadeStep >= 15)
                owner.state.writeInstrumentObjectFade(
                        slot, Math.min(31, owner.state.readInstrumentObjectFade(slot) + 1));
        }
        if (runtime.physicsTables.readSlowTransitionCountdown(slot) == 0) {
            owner.state.writeInstrumentPickupState(slot, 4);
            owner.state.writeInstrumentPickupSequenceActive(slot, false);
            runtime.events.pendingInstrumentCompletions.add(
                    new InstrumentCompletionEvent(slot, runtime.roomVariables.readEntityMapId() & 0xFF));
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return false;
        }

        return true;
    }

    private boolean inUse;

    void run(
            linksawakening.world.entity.EntityRewardController owner,
            linksawakening.world.entity.RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            java.util.function.IntSupplier randomByteSupplier) {
        if (inUse) {
            new InstrumentPickupStep()
                    .run(owner, entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier);
            return;
        }
        inUse = true;
        runtime = null;
        slot = 0;
        held = null;
        this.owner = owner;
        this.entity = entity;
        this.frameCounter = frameCounter;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.linkZ = linkZ;
        this.randomByteSupplier = randomByteSupplier;
        try {
            run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.randomByteSupplier = null;
            this.runtime = null;
            this.held = null;
        }
    }
}
