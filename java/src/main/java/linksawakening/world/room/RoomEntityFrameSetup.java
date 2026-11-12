package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.object.SwitchBlockAnimation;
import linksawakening.world.transition.Warp;

final class RoomEntityFrameSetup {
    private final Dependencies dependencies;

    RoomEntityFrameSetup(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void configure() {
        configurePresentation();
        configureInteraction();
        configureItems();
    }

    private void configurePresentation() {
        if (!dependencies.entityFrame().boomerangSoundClockProvided) {
            dependencies.entityBridge().get().advanceBoomerangSoundClock(true);
        }
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBoomerangSoundCounterForFrame(dependencies.entityFrame().boomerangSoundCounter);
        dependencies.entityFrame().boomerangSoundClockProvided = false;
        dependencies
                .indoorTorchPaletteEffect()
                .tick(
                        dependencies.entityInputs().get().entityDialogActive,
                        false,
                        dependencies.entityFrame().currentLinkMotionState,
                        dependencies.location().activeRoom.mapId());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setTorchPaletteTransitionState(
                        dependencies.indoorTorchPaletteEffect().targetAddress(),
                        dependencies.indoorTorchPaletteEffect().transitionActive());
        dependencies.roomObjectMutations().get().copyIndoorTorchPalettesToActiveRoom();
        dependencies.roomTransitionController().get().tickOcarinaAnimationHandler();
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setSwitchBlockAnimationActive(
                        SwitchBlockAnimation.isAnimating(dependencies.terrain().switchableObjectAnimationStage));
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setDialogActive(dependencies.entityInputs().get().entityDialogActive);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setTalkState(
                        dependencies.entityInputs().get().entityInventoryAppearing,
                        dependencies.entityInputs().get().entityDialogCooldown,
                        dependencies.entityInputs().get().entityWindowY);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setDialogAskSelectionIndex(dependencies.entityInputs().get().entityDialogAskSelectionIndex);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setActiveMusic(dependencies.entityInputs().get().entityMusicActive);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBowWowState(dependencies.followers().bowWowState);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setLinkVelocityZ(dependencies.entityInputs().get().entityLinkVelocityZ);
    }

    private void configureInteraction() {
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOwlInstrumentFlags(
                        Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[0]),
                        Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[1]));
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setActionButtonsHeld(
                        dependencies.entityInputs().get().actionButtonAHeld,
                        dependencies.entityInputs().get().actionButtonBHeld);
        int statueMapId = dependencies.location().activeRoom.mapId();
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOwlStatueDialogId(
                        dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                                        && (statueMapId < 8 || statueMapId == 0xFF)
                                ? dependencies
                                        .owlStatueDialogResolver()
                                        .globalDialogId(
                                                statueMapId,
                                                dependencies
                                                        .location()
                                                        .activeRoom
                                                        .roomId(),
                                                dependencies
                                                                .roomState()
                                                                .dungeonItemState
                                                                .currentFlag(DungeonItemState.STONE_BEAK_INDEX)
                                                        != 0)
                                : -1);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setJoypadHeld(dependencies.entityInputs().get().joypadHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setPressedButtonsMask(dependencies.entityInputs().get().entityPressedButtonsMask);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setPowerBraceletButtonHeld(dependencies.entityInputs().get().powerBraceletButtonHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBombButtonHeld(dependencies.entityInputs().get().bombButtonHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBooBuddyTriggerCount(dependencies.puzzles().roomTriggerCount);
    }

    private void configureItems() {
        dependencies.roomStoryState().get().updateWitchEnvironment();
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOcarinaPlayback(
                        dependencies.items().ocarinaPlaybackCountdown,
                        dependencies.items().ocarinaSongFlags,
                        dependencies.items().selectedSongIndex,
                        dependencies.items().ocarinaAnimationCounter,
                        dependencies.items().ocarinaAnimationPhase);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setGoldenLeavesCount(dependencies.entityInputs().get().entityGoldenLeavesCount);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setSecretSeashellPegasusCollisionState(
                        dependencies.entityInputs().get().secretSeashellScreenShakeActive,
                                dependencies.entityInputs().get().secretSeashellPegasusCollisionActive,
                        dependencies.entityInputs().get().secretSeashellPegasusCollisionX,
                                dependencies.entityInputs().get().secretSeashellPegasusCollisionY);
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OwlStatueDialogResolver owlStatueDialogResolver,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController) {}
}
