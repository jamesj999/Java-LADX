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

/** Reused, reentrancy-safe scratch for OwlEventBehavior.advanceOwlEvent; values are assigned in source order. */
final class OwlEventStep {
    private linksawakening.world.entity.EntityStoryController owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private boolean handlerLinkInteractive;
    private linksawakening.world.entity.EntityStoryControllerServices runtime;
    private int slot;

    OwlEventStep() {}

    void run() {
        runtime = owner.runtime;
        slot = entity.slot();
        switch (owner.owlState().readOwlEventState(slot)) {
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
        if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_BEACH_WITH_SWORD) {
            if (linkEntityY < 0x44 || linkEntityX < 0x40 || linkEntityX >= 0x70) {
                return false;
            }
            owner.owlState().writeOwlPreviousMusicTrack(slot, 0x1D);
        } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_TAIL_KEY_CHEST) {
            if (!owner.readPlayerHasTailKey() || owner.owlState().readOwlPrivateCountdown1(slot) != 0) {
                return false;
            }
            owner.owlState()
                    .writeOwlPreviousMusicTrack(
                            slot,
                            owner.owlState()
                                    .readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
        } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_WEST_OF_TAIL_CAVE) {
            if (runtime.pickups().readChestSwordLevel() == 0
                    || (runtime.roomVariables().readInstrument1Flags() & 0x02) == 0) {
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
            }
            owner.owlState()
                    .writeOwlPreviousMusicTrack(
                            slot,
                            owner.owlState()
                                    .readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
            runtime.presentationState().writePendingMusicTrack(0x22);
            owner.owlState().writeOwlEventState(slot, 2);
            return false;
        } else if (runtime.roomVariables().readEntityRoomId() == EntityRomConstants.ROOM_OW_EAST_OF_MOBLIN_HIDEOUT) {
            if (runtime.pickups().readChestSwordLevel() == 0
                    || runtime.roomVariables().readInstrument2Flags() != 0
                    || runtime.motions()
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState()
                            != 0x01
                    || (runtime.roomVariables().readInstrument1Flags() & 0x02) == 0) {
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
            }
            owner.owlState()
                    .writeOwlPreviousMusicTrack(
                            slot,
                            owner.owlState()
                                    .readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
            runtime.presentationState().writePendingMusicTrack(0x22);
            owner.owlState().writeOwlEventState(slot, 2);
            return false;
        } else {
            if (runtime.pickups().readChestSwordLevel() == 0) {
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
            }
            if (runtime.roomVariables().readEntityRoomId() != 0x80) {
                return false;
            }
            owner.owlState()
                    .writeOwlPreviousMusicTrack(
                            slot,
                            owner.owlState()
                                    .readOwlDefaultMusicResolver()
                                    .applyAsInt(runtime.roomVariables().readEntityRoomId()));
        }
        runtime.presentationState().writePendingMusicTrack(0x22);
        owner.owlState().writeOwlSpeedX(slot, 0x18);
        owner.owlState().writeOwlSpeedY(slot, 0x10);
        owner.owlState().writeOwlSpeedZ(slot, 0xFC);
        runtime.slots()[slot] = withZ(entity, 0x20);
        owner.owlState().writeOwlEventState(slot, 1);

        return true;
    }

    private boolean advanceState1() {
        runtime.roomVariables().writeEntityInteractionCountdown(5); // OwlState1Handler, $06:693B.
        runtime.blockLinkForOwl(slot);
        runtime.events()
                .pendingLinkFacingRequests
                .add(new LinkFacingRequest(slot, directionFromLinkToEntity(entity, linkEntityX, linkEntityY)));
        if (owner.owlState().readOwlBoomerangSfxCounter(slot) == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2D));
            owner.owlState().writeOwlBoomerangSfxCounter(slot, 0x1A);
        } else {
            owner.owlState().postOwlBoomerangSfxCounter(slot, -1);
        }
        RoomEntity flying = runtime.roomInspection().owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(
                flying.x(),
                owner.owlState().readOwlSpeedX(slot),
                owner.owlState().storageOwlXAccumulator(),
                slot);
        int y = addFallingSpeedToPosition(
                flying.y(),
                owner.owlState().readOwlSpeedY(slot),
                owner.owlState().storageOwlYAccumulator(),
                slot);
        if (flying.z() == 0) {
            owner.owlState().writeOwlEventState(slot, 2);
            runtime.slots()[slot] = withPositionAndVariant(flying, x, y, 0);
            return false;
        }
        int z = addFallingSpeedToPosition(
                flying.z(),
                owner.owlState().readOwlSpeedZ(slot),
                owner.owlState().storageOwlZAccumulator(),
                slot);
        if ((frameCounter & 0x03) == 0) {
            owner.owlState()
                    .writeOwlSpeedX(
                            slot, approachSignedByteZero(owner.owlState().readOwlSpeedX(slot)));
            owner.owlState()
                    .writeOwlSpeedY(
                            slot, approachSignedByteZero(owner.owlState().readOwlSpeedY(slot)));
        }
        if (signedByte(z) <= 0) z = 0;
        runtime.slots()[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);

        return true;
    }

    private boolean advanceState2() {
        if (!handlerLinkInteractive || runtime.roomVariables().readTransitionSequenceCounter() != 0x04) {
            return false;
        }
        if (RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            runtime.backgroundMechanics().requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        runtime.slots()[slot] = runtime.roomInspection().owlPresentation(entity, frameCounter);
        int globalDialogId = owner.owlState()
                .readOwlDialogResolver()
                .applyAsInt(runtime.roomVariables().readEntityRoomId());
        runtime.events()
                .pendingDialogRequests
                .add(new DialogRequest((globalDialogId >>> 8) & 0xFF, globalDialogId & 0xFF));
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
        owner.owlState().writeOwlEventState(slot, 3);

        return true;
    }

    private boolean advanceState3() {
        if (!handlerLinkInteractive) {
            return false;
        }
        runtime.blockLinkForOwl(slot);
        runtime.slots()[slot] = runtime.roomInspection().owlPresentation(entity, frameCounter);
        if (runtime.linkState().dialogActive) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) > 0) {
                runtime.physicsTables().postEnemyTransitionCountdown(slot, 1);
            }
            return false;
        }
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            if (!owner.owlState().readOwlCompletionEmitted(slot)) {
                runtime.events().pendingOwlEventCompletions.add(new OwlEventCompletion(slot));
                owner.owlState().writeOwlCompletionEmitted(slot, true);
            }
            owner.owlState().writeOwlSpeedX(slot, linkEntityX < entity.x() ? 0x20 : 0xE0);
            owner.owlState().writeOwlSpeedY(slot, linkEntityY < entity.y() ? 0x20 : 0xE0);
            owner.owlState().writeOwlSpeedZ(slot, 0x04);
            owner.owlState().writeOwlEventState(slot, 4);
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
