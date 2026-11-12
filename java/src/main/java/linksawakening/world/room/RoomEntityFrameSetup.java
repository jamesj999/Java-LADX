package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.world.object.SwitchBlockAnimation;
import linksawakening.world.transition.Warp;

final class RoomEntityFrameSetup {
    private final RoomSession session;

    RoomEntityFrameSetup(RoomSession session) {
        this.session = session;
    }

    void configure() {
        configurePresentation();
        configureInteraction();
        configureItems();
    }

    private void configurePresentation() {
        if (!session.context.boomerangSoundClockProvided) {
            session.advanceBoomerangSoundClock(true);
        }
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setBoomerangSoundCounterForFrame(session.context.boomerangSoundCounter);
        session.context.boomerangSoundClockProvided = false;
        session.context.indoorTorchPaletteEffect.tick(
                session.context.entityInputs.entityDialogActive,
                false,
                session.context.currentLinkMotionState,
                session.context.activeRoom.mapId());
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setTorchPaletteTransitionState(
                        session.context.indoorTorchPaletteEffect.targetAddress(),
                        session.context.indoorTorchPaletteEffect.transitionActive());
        session.context.roomObjectMutations.copyIndoorTorchPalettesToActiveRoom();
        session.context.roomTransitionController.tickOcarinaAnimationHandler();
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setSwitchBlockAnimationActive(
                        SwitchBlockAnimation.isAnimating(session.context.switchableObjectAnimationStage));
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setDialogActive(session.context.entityInputs.entityDialogActive);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setTalkState(
                        session.context.entityInputs.entityInventoryAppearing,
                        session.context.entityInputs.entityDialogCooldown,
                        session.context.entityInputs.entityWindowY);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setDialogAskSelectionIndex(session.context.entityInputs.entityDialogAskSelectionIndex);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setActiveMusic(session.context.entityInputs.entityMusicActive);
        session.context.entityBridge.entityRuntime.configuration().setBowWowState(session.context.bowWowState);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setLinkVelocityZ(session.context.entityInputs.entityLinkVelocityZ);
    }

    private void configureInteraction() {
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setOwlInstrumentFlags(
                        Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[0]),
                        Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[1]));
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setActionButtonsHeld(
                        session.context.entityInputs.actionButtonAHeld, session.context.entityInputs.actionButtonBHeld);
        int statueMapId = session.context.activeRoom.mapId();
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setOwlStatueDialogId(
                        session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                                        && (statueMapId < 8 || statueMapId == 0xFF)
                                ? session.context.owlStatueDialogResolver.globalDialogId(
                                        statueMapId,
                                        session.context.activeRoom.roomId(),
                                        session.context.roomState.dungeonItemState.currentFlag(
                                                        DungeonItemState.STONE_BEAK_INDEX)
                                                != 0)
                                : -1);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setJoypadHeld(session.context.entityInputs.joypadHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setPressedButtonsMask(session.context.entityInputs.entityPressedButtonsMask);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setPowerBraceletButtonHeld(session.context.entityInputs.powerBraceletButtonHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setBombButtonHeld(session.context.entityInputs.bombButtonHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setLiftedLinkC13B(session.context.followingEntityYOffset);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setBooBuddyTriggerCount(session.context.roomTriggerCount);
    }

    private void configureItems() {
        session.context.roomStoryState.updateWitchEnvironment();
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setOcarinaPlayback(
                        session.context.ocarinaPlaybackCountdown,
                        session.context.ocarinaSongFlags,
                        session.context.selectedSongIndex,
                        session.context.ocarinaAnimationCounter,
                        session.context.ocarinaAnimationPhase);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setGoldenLeavesCount(session.context.entityInputs.entityGoldenLeavesCount);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setSecretSeashellPegasusCollisionState(
                        session.context.entityInputs.secretSeashellScreenShakeActive,
                                session.context.entityInputs.secretSeashellPegasusCollisionActive,
                        session.context.entityInputs.secretSeashellPegasusCollisionX,
                                session.context.entityInputs.secretSeashellPegasusCollisionY);
    }
}
