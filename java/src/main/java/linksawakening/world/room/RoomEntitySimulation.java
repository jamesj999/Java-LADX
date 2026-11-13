package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.object.SwitchBlockAnimation;
import linksawakening.world.transition.Warp;

final class RoomEntitySimulation {
    private final Dependencies dependencies;
    private final SetupDependencies setup;
    private final EffectsDependencies effects;
    private final linksawakening.world.entity.EntityFrameContextBuilder frameInputs =
            linksawakening.world.entity.EntityFrameContext.builder();
    private final linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private final linksawakening.world.entity.RoomEntityObjectCollision objectCollision;

    RoomEntitySimulation(Dependencies dependencies, SetupDependencies setup, EffectsDependencies effects) {
        this.setup = setup;
        this.effects = effects;
        this.dependencies = dependencies;
        backgroundCollision = (entity, direction, x, y) ->
                dependencies.roomCollisionQueries().get().entityBackgroundCollision(entity, direction, x, y);
        objectCollision = entity -> dependencies.roomCollisionQueries().get().pairoddProjectileObjectCollision(entity);
    }

    List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.position().x();
        int linkEntityY = inputs.position().y();
        int linkEntityZ = inputs.position().z();
        boolean linkAirborne = inputs.motion().airborne();
        int linkMotionState = inputs.motion().rawState();
        int linkDirection = inputs.position().rawDirection();
        int collisionType = inputs.position().collisionType();
        boolean usingShield = inputs.motion().shield();
        int shieldLevel = inputs.shieldLevel();
        int invincibilityCounter = inputs.motion().invincibility();
        boolean swordCollisionActive = inputs.sword().active();
        int swordX = inputs.sword().x();
        int swordWidth = inputs.sword().width();
        int swordY = inputs.sword().y();
        int swordHeight = inputs.sword().height();
        int linkSpeedX = inputs.motion().speedX();
        int linkSpeedY = inputs.motion().speedY();

        dependencies.followers().followingLinkX = linkEntityX & 0xFF;
        dependencies.followers().followingLinkY = linkEntityY & 0xFF;
        dependencies.followers().followingLinkZ = linkEntityZ & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        dependencies.entityFrame().currentLinkMotionState = linkMotionState & 0xFF;
        dependencies.location().shouldGetLostInMysteriousWoods = false;
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return List.of();
        }
        configure();
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        dependencies.entityRandomByteSource().beginFrame(frameCounter & 0xFF, 0);
        spawnPendingShovelDrop();
        List<EntityProjectileEvent> events = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .tick(frameInputs
                        .frameCounter(frameCounter)
                        .linkEntityX(linkEntityX)
                        .linkEntityY(linkEntityY)
                        .collisionType(collisionType & 0xFF)
                        .randomByteSupplier(dependencies.entityRandomByteSource())
                        .backgroundCollision(backgroundCollision)
                        .objectCollision(objectCollision)
                        .linkPositionHistory(dependencies.followers().followingLinkPositionHistory)
                        .linkZ(dependencies.followers().followingLinkZ)
                        .linkAirborne(linkAirborne)
                        .linkDirection(dependencies.followers().followingLinkDirection)
                        .entityYOffset(dependencies.followers().followingEntityYOffset)
                        .projectileLinkState(new EnemyProjectileCollision.LinkState(
                                linkEntityX,
                                linkEntityY,
                                linkEntityZ,
                                linkMotionState,
                                RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection),
                                usingShield,
                                shieldLevel,
                                invincibilityCounter))
                        .swordCollisionActive(swordCollisionActive)
                        .swordX(swordX)
                        .swordWidth(swordWidth)
                        .swordY(swordY)
                        .swordHeight(swordHeight)
                        .linkSpeedX(linkSpeedX)
                        .linkSpeedY(linkSpeedY)
                        .build());
        complete(linkEntityX, linkEntityY);
        return events;
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            RomRandomByteSource entityRandomByteSource,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomEntityBridge> owner) {}

    private void spawnPendingShovelDrop() {
        if (dependencies.items().pendingShovelDrop != null) {
            PendingShovelDrop drop = dependencies.items().pendingShovelDrop;
            dependencies.items().pendingShovelDrop = null;
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .spawnShovelDrop(
                            drop.objectLeft(),
                            drop.objectTop(),
                            drop.linkEntityX(),
                            drop.linkEntityY(),
                            drop.allowDrop());
        }
    }

    void configure() {
        configurePresentation();
        configureInteraction();
        configureItems();
    }

    private void configurePresentation() {
        if (!setup.entityFrame().boomerangSoundClockProvided) {
            setup.entityBridge().get().advanceBoomerangSoundClock(true);
        }
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBoomerangSoundCounterForFrame(setup.entityFrame().boomerangSoundCounter);
        setup.entityFrame().boomerangSoundClockProvided = false;
        setup.indoorTorchPaletteEffect()
                .tick(
                        setup.entityInputs().get().entityDialogActive,
                        false,
                        setup.entityFrame().currentLinkMotionState,
                        setup.location().activeRoom.mapId());
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setTorchPaletteTransitionState(
                        setup.indoorTorchPaletteEffect().targetAddress(),
                        setup.indoorTorchPaletteEffect().transitionActive());
        setup.roomObjectMutations().get().copyIndoorTorchPalettesToActiveRoom();
        setup.roomTransitionController().get().tickOcarinaAnimationHandler();
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setSwitchBlockAnimationActive(
                        SwitchBlockAnimation.isAnimating(setup.terrain().switchableObjectAnimationStage));
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setDialogActive(setup.entityInputs().get().entityDialogActive);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setTalkState(
                        setup.entityInputs().get().entityInventoryAppearing,
                        setup.entityInputs().get().entityDialogCooldown,
                        setup.entityInputs().get().entityWindowY);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setDialogAskSelectionIndex(setup.entityInputs().get().entityDialogAskSelectionIndex);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setActiveMusic(setup.entityInputs().get().entityMusicActive);
        setup.entityBridge().get().entityRuntime.configuration().setBowWowState(setup.followers().bowWowState);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setLinkVelocityZ(setup.entityInputs().get().entityLinkVelocityZ);
    }

    private void configureInteraction() {
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOwlInstrumentFlags(
                        Byte.toUnsignedInt(setup.roomState().dungeonProgressFlags[0]),
                        Byte.toUnsignedInt(setup.roomState().dungeonProgressFlags[1]));
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setActionButtonsHeld(
                        setup.entityInputs().get().actionButtonAHeld,
                        setup.entityInputs().get().actionButtonBHeld);
        int statueMapId = setup.location().activeRoom.mapId();
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOwlStatueDialogId(
                        setup.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                                        && (statueMapId < 8 || statueMapId == 0xFF)
                                ? setup.owlStatueDialogResolver()
                                        .globalDialogId(
                                                statueMapId,
                                                setup.location().activeRoom.roomId(),
                                                setup.roomState()
                                                                .dungeonItemState
                                                                .currentFlag(DungeonItemState.STONE_BEAK_INDEX)
                                                        != 0)
                                : -1);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setJoypadHeld(setup.entityInputs().get().joypadHeld);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setPressedButtonsMask(setup.entityInputs().get().entityPressedButtonsMask);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setPowerBraceletButtonHeld(setup.entityInputs().get().powerBraceletButtonHeld);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBombButtonHeld(setup.entityInputs().get().bombButtonHeld);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setLiftedLinkC13B(setup.followers().followingEntityYOffset);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBooBuddyTriggerCount(setup.puzzles().roomTriggerCount);
    }

    private void configureItems() {
        setup.roomStoryState().get().updateWitchEnvironment();
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOcarinaPlayback(
                        setup.items().ocarinaPlaybackCountdown,
                        setup.items().ocarinaSongFlags,
                        setup.items().selectedSongIndex,
                        setup.items().ocarinaAnimationCounter,
                        setup.items().ocarinaAnimationPhase);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setGoldenLeavesCount(setup.entityInputs().get().entityGoldenLeavesCount);
        setup.entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setSecretSeashellPegasusCollisionState(
                        setup.entityInputs().get().secretSeashellScreenShakeActive,
                                setup.entityInputs().get().secretSeashellPegasusCollisionActive,
                        setup.entityInputs().get().secretSeashellPegasusCollisionX,
                                setup.entityInputs().get().secretSeashellPegasusCollisionY);
    }

    record SetupDependencies(
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

    void complete(int linkEntityX, int linkEntityY) {
        synchronizeQuestState();
        var bombs = harvestRewards();
        persistEffects();
        applyObjects(bombs, linkEntityX, linkEntityY);
    }

    private void synchronizeQuestState() {
        var dungeonWarpRequests = effects.entityBridge().get().entityRuntime.consumePendingDungeonWarpRequests();
        for (int requestIndex = 0; requestIndex < dungeonWarpRequests.size(); requestIndex++) {
            DungeonWarpRequest request = dungeonWarpRequests.get(requestIndex);

            linksawakening.world.entity.event.WarpDestination destination = request.destination();
            effects.location().pendingDungeonWarp = new Warp(
                    Warp.CATEGORY_INDOOR,
                    destination.mapId(),
                    destination.roomId(),
                    destination.x(),
                    destination.y(),
                    0x34);
        }
        effects.location().shouldGetLostInMysteriousWoods =
                effects.entityBridge().get().entityRuntime.inspection().shouldGetLostInMysteriousWoods();
        int runtimeBowWowState =
                effects.entityBridge().get().entityRuntime.inspection().bowWowState();
        if (runtimeBowWowState != effects.followers().bowWowState) {
            effects.followers().bowWowState = runtimeBowWowState;
            effects.followers().followingNpcState = new FollowingNpcState(
                    effects.followers().followingNpcState.roosterFollowing(),
                    effects.followers().followingNpcState.ghostFollowingState(),
                    effects.followers().followingNpcState.marinFollowing(),
                    runtimeBowWowState == 0x01,
                    effects.followers().followingNpcState.instrument4Flags(),
                    effects.followers().followingNpcState.powerBraceletLevel(),
                    effects.followers().followingNpcState.db10());
            // The rescue/return handler changes the existing entity in place.
            // CreateFollowingNpcEntity runs on room entry, not on this quest
            // flag write: respawning here teleports BowWow onto Link and
            // reconstructs every other entity's runtime state.
        }
        if (effects.items().ocarinaPlaybackCountdown > 0) {
            effects.items().ocarinaPlaybackCountdown--;
            if (effects.items().ocarinaPlaybackCountdown == 0) {
                effects.roomTransitionController().get().finishOcarinaPlayback();
            }
        }
    }

    private List<BombExplosionEvent> harvestRewards() {
        List<BombExplosionEvent> bombExplosionEvents =
                effects.entityBridge().get().entityRuntime.consumeBombExplosionEvents();
        effects.entityBridge().get().outbox.pendingBombExplosionEvents.clear();
        RoomEntityOutbox.append(effects.entityBridge().get().outbox.pendingBombExplosionEvents, bombExplosionEvents);
        List<ChestRewardEvent> chestRewards =
                effects.entityBridge().get().entityRuntime.consumePendingChestRewardEvents();
        for (int rewardIndex = 0; rewardIndex < chestRewards.size(); rewardIndex++) {
            ChestRewardEvent reward = chestRewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                effects.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        RoomEntityOutbox.append(effects.entityBridge().get().outbox.pendingChestRewardEvents, chestRewards);
        effects.roomEntityRewards().get().harvestKeyQuicksandEvents();
        effects.roomEntityRewards().get().harvestKeyRewardEvents();
        effects.roomEntityRewards().get().harvestSlimeKeyRewardEvents();
        effects.roomPuzzleController().get().harvestThrownRoomEventCollisions();
        effects.roomEntityRewards().get().harvestHeartContainerRewards();
        RoomEntityOutbox.append(
                effects.entityBridge().get().outbox.pendingMadamMeowMeowFullHealRequests,
                effects.entityBridge().get().entityRuntime.consumePendingMadamMeowMeowFullHealRequests());
        effects.roomEntityRewards().get().harvestSwordPickupRewards();
        effects.roomEntityRewards().get().harvestToadstoolRewards();
        effects.roomEntityRewards().get().harvestInstrumentRewards();
        effects.roomEntityRewards().get().harvestInstrumentCompletions();
        effects.roomEntityRewards().get().harvestRichardProgressEvents();
        effects.roomEntityRewards().get().harvestWitchEvents();
        effects.roomEntityRewards().get().harvestOwlEventCompletions();
        return bombExplosionEvents;
    }

    private void persistEffects() {
        var roomStatusPersistenceRequests =
                effects.entityBridge().get().entityRuntime.consumePendingRoomStatusPersistenceRequests();
        for (int requestIndex = 0; requestIndex < roomStatusPersistenceRequests.size(); requestIndex++) {
            RoomStatusPersistenceRequest request = roomStatusPersistenceRequests.get(requestIndex);

            if (effects.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                effects.roomState()
                        .overworldRoomStatus[effects.location().activeRoom.roomId()] |= (byte) request.roomStatusMask();
            }
            effects.followers().tarinFlag = request.tarinFlag();
        }
        if (effects.entityBridge().get().entityRuntime.consumePendingSwitchBlockAnimationRequest()
                && effects.terrain().switchableObjectAnimationStage == 0) {
            effects.terrain().switchableObjectAnimationStage = 0x01;
        }
        effects.entityFrame().enemyDropCounters =
                effects.entityBridge().get().entityRuntime.inspection().enemyDropCounters();
        if (effects.transientVfxSystem() != null) {
            var transientVfxRequests =
                    effects.entityBridge().get().entityRuntime.enemies().transientVfxRequests();
            for (int requestIndex = 0; requestIndex < transientVfxRequests.size(); requestIndex++) {
                TransientVfxRequest request = transientVfxRequests.get(requestIndex);

                effects.transientVfxSystem()
                        .spawn(request.type(), request.worldX(), request.worldY(), request.variant());
            }
        }
        int clearedMask = effects.entityBridge().get().entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            effects.entityFrame()
                    .recentRoomEntityClears
                    .addMask(effects.location().activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = effects.entityBridge().get().entityRuntime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && effects.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = effects.roomSessionViews()
                    .get()
                    .indoorStatusTableForMap(effects.location().activeRoom.mapId());
            status[effects.location().activeRoom.roomId()] |= (byte) roomStatusMask;
            effects.entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEntityRoomStatus(Byte.toUnsignedInt(
                            status[effects.location().activeRoom.roomId()]));
        }
    }

    private void applyObjects(List<BombExplosionEvent> bombExplosionEvents, int linkEntityX, int linkEntityY) {
        effects.roomTileRedraw()
                .get()
                .applyHookshotBridgeUpdates(
                        effects.entityBridge().get().entityRuntime.inspection().hookshotBridgeUpdates());
        effects.location()
                .activeRoom
                .replaceEntities(
                        effects.entityBridge().get().entityRuntime.inspection().view());
        boolean objectRequests = !bombExplosionEvents.isEmpty();
        effects.roomObjectMutations().get().applyBombObjectInteractions(bombExplosionEvents);
        var boomerangs = effects.entityBridge().get().entityRuntime.enemies().boomerangObjectRequests();
        objectRequests |= !boomerangs.isEmpty();
        effects.roomObjectMutations().get().applyBoomerangObjectInteractions(boomerangs);
        var rods = effects.entityBridge().get().entityRuntime.enemies().magicRodObjectRequests();
        objectRequests |= !rods.isEmpty();
        effects.roomObjectMutations().get().applyMagicRodObjectInteractions(rods);
        var powders = effects.entityBridge().get().entityRuntime.enemies().magicPowderObjectRequests();
        objectRequests |= !powders.isEmpty();
        effects.roomObjectMutations().get().applyMagicPowderObjectInteractions(powders);
        // Empty object-request lists return without mutating runtime entities. The first
        // immutable snapshot already contains their final state in that common case.
        if (objectRequests)
            effects.location()
                    .activeRoom
                    .replaceEntities(effects.entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .view());
        effects.roomPuzzleController().get().tickPushedBlockMotion(linkEntityX, linkEntityY);
        effects.roomPuzzleController().get().tickRoomEvent(linkEntityX, linkEntityY);
    }

    record EffectsDependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomTerrainState terrain,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityRewards> roomEntityRewards,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController,
            TransientVfxSystem transientVfxSystem) {}
}
