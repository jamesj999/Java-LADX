package linksawakening.world.room;

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

/** LoadRoomEntities runtime: configure inputs, tick descending slots, then apply room effects in source order. */
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
                dependencies.roomCollisionQueries().entityBackgroundCollision(entity, direction, x, y);
        objectCollision = entity -> dependencies.roomCollisionQueries().pairoddProjectileObjectCollision(entity);
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
                || dependencies.entityBridge().runtime() == null) {
            return List.of();
        }
        configureRuntimeForFrame();
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        dependencies.entityRandomByteSource().beginFrame(frameCounter & 0xFF, 0);
        spawnPendingShovelDrop();
        List<EntityProjectileEvent> events = dependencies
                .entityBridge()
                .runtime()
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
        applyFrameEffects(linkEntityX, linkEntityY);
        return events;
    }

    record Dependencies(
            RoomEntityBridge entityBridge,
            RoomEntitySessionState entityFrame,
            RomRandomByteSource entityRandomByteSource,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomCollisionQueries roomCollisionQueries) {
        Dependencies {
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
        }
    }

    private void spawnPendingShovelDrop() {
        if (dependencies.items().pendingShovelDrop != null) {
            PendingShovelDrop drop = dependencies.items().pendingShovelDrop;
            dependencies.items().pendingShovelDrop = null;
            dependencies
                    .entityBridge()
                    .runtime()
                    .spawnShovelDrop(
                            drop.objectLeft(),
                            drop.objectTop(),
                            drop.linkEntityX(),
                            drop.linkEntityY(),
                            drop.allowDrop());
        }
    }

    void configureRuntimeForFrame() {
        configurePresentation();
        configureInteraction();
        configureItems();
    }

    private void configurePresentation() {
        if (!setup.entityFrame().boomerangSoundClockProvided) {
            setup.entityBridge().advanceBoomerangSoundClock(true);
        }
        setup.entityBridge()
                .runtime()
                .configuration()
                .setBoomerangSoundCounterForFrame(setup.entityFrame().boomerangSoundCounter);
        setup.entityFrame().boomerangSoundClockProvided = false;
        setup.indoorTorchPaletteEffect()
                .tick(
                        setup.entityInputs().entityDialogActive,
                        false,
                        setup.entityFrame().currentLinkMotionState,
                        setup.location().activeRoom.mapId());
        setup.entityBridge()
                .runtime()
                .configuration()
                .setTorchPaletteTransitionState(
                        setup.indoorTorchPaletteEffect().targetAddress(),
                        setup.indoorTorchPaletteEffect().transitionActive());
        setup.roomObjectMutations().copyIndoorTorchPalettesToActiveRoom();
        setup.roomTransitionController().tickOcarinaAnimationHandler();
        setup.entityBridge()
                .runtime()
                .configuration()
                .setSwitchBlockAnimationActive(
                        SwitchBlockAnimation.isAnimating(setup.terrain().switchableObjectAnimationStage));
        setup.entityBridge().runtime().configuration().setDialogActive(setup.entityInputs().entityDialogActive);
        setup.entityBridge()
                .runtime()
                .configuration()
                .setTalkState(
                        setup.entityInputs().entityInventoryAppearing,
                        setup.entityInputs().entityDialogCooldown,
                        setup.entityInputs().entityWindowY);
        setup.entityBridge()
                .runtime()
                .configuration()
                .setDialogAskSelectionIndex(setup.entityInputs().entityDialogAskSelectionIndex);
        setup.entityBridge().runtime().configuration().setActiveMusic(setup.entityInputs().entityMusicActive);
        setup.entityBridge().runtime().configuration().setBowWowState(setup.followers().bowWowState);
        setup.entityBridge().runtime().configuration().setLinkVelocityZ(setup.entityInputs().entityLinkVelocityZ);
    }

    private void configureInteraction() {
        setup.entityBridge()
                .runtime()
                .configuration()
                .setOwlInstrumentFlags(
                        Byte.toUnsignedInt(setup.roomState().dungeonProgressFlags[0]),
                        Byte.toUnsignedInt(setup.roomState().dungeonProgressFlags[1]));
        setup.entityBridge()
                .runtime()
                .configuration()
                .setActionButtonsHeld(setup.entityInputs().actionButtonAHeld, setup.entityInputs().actionButtonBHeld);
        int statueMapId = setup.location().activeRoom.mapId();
        setup.entityBridge()
                .runtime()
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
        setup.entityBridge().runtime().configuration().setJoypadHeld(setup.entityInputs().joypadHeld);
        setup.entityBridge()
                .runtime()
                .configuration()
                .setPressedButtonsMask(setup.entityInputs().entityPressedButtonsMask);
        setup.entityBridge()
                .runtime()
                .configuration()
                .setPowerBraceletButtonHeld(setup.entityInputs().powerBraceletButtonHeld);
        setup.entityBridge().runtime().configuration().setBombButtonHeld(setup.entityInputs().bombButtonHeld);
        setup.entityBridge().runtime().configuration().setLiftedLinkC13B(setup.followers().followingEntityYOffset);
        setup.entityBridge().runtime().configuration().setBooBuddyTriggerCount(setup.puzzles().roomTriggerCount);
    }

    private void configureItems() {
        setup.roomStoryState().updateWitchEnvironment();
        setup.entityBridge()
                .runtime()
                .configuration()
                .setOcarinaPlayback(
                        setup.items().ocarinaPlaybackCountdown,
                        setup.items().ocarinaSongFlags,
                        setup.items().selectedSongIndex,
                        setup.items().ocarinaAnimationCounter,
                        setup.items().ocarinaAnimationPhase);
        setup.entityBridge()
                .runtime()
                .configuration()
                .setGoldenLeavesCount(setup.entityInputs().entityGoldenLeavesCount);
        setup.entityBridge()
                .runtime()
                .configuration()
                .setSecretSeashellPegasusCollisionState(
                        setup.entityInputs().secretSeashellScreenShakeActive,
                                setup.entityInputs().secretSeashellPegasusCollisionActive,
                        setup.entityInputs().secretSeashellPegasusCollisionX,
                                setup.entityInputs().secretSeashellPegasusCollisionY);
    }

    record SetupDependencies(
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            RoomEntitySessionState entityBridge,
            RoomEntityInputs entityInputs,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OwlStatueDialogResolver owlStatueDialogResolver,
            RoomObjectMutations roomObjectMutations,
            RoomWorldState roomState,
            RoomStoryState roomStoryState,
            RoomTransitionController roomTransitionController) {
        SetupDependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(indoorTorchPaletteEffect, "indoorTorchPaletteEffect");
            java.util.Objects.requireNonNull(owlStatueDialogResolver, "owlStatueDialogResolver");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
            java.util.Objects.requireNonNull(roomTransitionController, "roomTransitionController");
        }
    }

    void applyFrameEffects(int linkEntityX, int linkEntityY) {
        synchronizeQuestState();
        var bombs = harvestRewards();
        persistEffects();
        applyObjects(bombs, linkEntityX, linkEntityY);
    }

    private void synchronizeQuestState() {
        var dungeonWarpRequests = effects.entityBridge().runtime.consumePendingDungeonWarpRequests();
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
                effects.entityBridge().runtime.inspection().shouldGetLostInMysteriousWoods();
        int runtimeBowWowState = effects.entityBridge().runtime.inspection().bowWowState();
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
                effects.roomTransitionController().finishOcarinaPlayback();
            }
        }
    }

    private List<BombExplosionEvent> harvestRewards() {
        List<BombExplosionEvent> bombExplosionEvents =
                effects.entityBridge().runtime.consumeBombExplosionEvents();
        effects.entityBridge().outbox.pendingBombExplosionEvents.clear();
        RoomEntityOutbox.append(effects.entityBridge().outbox.pendingBombExplosionEvents, bombExplosionEvents);
        List<ChestRewardEvent> chestRewards = effects.entityBridge().runtime.consumePendingChestRewardEvents();
        for (int rewardIndex = 0; rewardIndex < chestRewards.size(); rewardIndex++) {
            ChestRewardEvent reward = chestRewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                effects.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        RoomEntityOutbox.append(effects.entityBridge().outbox.pendingChestRewardEvents, chestRewards);
        effects.roomEntityRewards().harvestKeyQuicksandEvents();
        effects.roomEntityRewards().harvestKeyRewardEvents();
        effects.roomEntityRewards().harvestSlimeKeyRewardEvents();
        effects.roomPuzzleController().harvestThrownRoomEventCollisions();
        effects.roomEntityRewards().harvestHeartContainerRewards();
        RoomEntityOutbox.append(
                effects.entityBridge().outbox.pendingMadamMeowMeowFullHealRequests,
                effects.entityBridge().runtime.consumePendingMadamMeowMeowFullHealRequests());
        effects.roomEntityRewards().harvestSwordPickupRewards();
        effects.roomEntityRewards().harvestToadstoolRewards();
        effects.roomEntityRewards().harvestInstrumentRewards();
        effects.roomEntityRewards().harvestInstrumentCompletions();
        effects.roomEntityRewards().harvestRichardProgressEvents();
        effects.roomEntityRewards().harvestWitchEvents();
        effects.roomEntityRewards().harvestOwlEventCompletions();
        return bombExplosionEvents;
    }

    private void persistEffects() {
        var roomStatusPersistenceRequests =
                effects.entityBridge().runtime.consumePendingRoomStatusPersistenceRequests();
        for (int requestIndex = 0; requestIndex < roomStatusPersistenceRequests.size(); requestIndex++) {
            RoomStatusPersistenceRequest request = roomStatusPersistenceRequests.get(requestIndex);

            if (effects.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                effects.roomState()
                        .overworldRoomStatus[effects.location().activeRoom.roomId()] |= (byte) request.roomStatusMask();
            }
            effects.followers().tarinFlag = request.tarinFlag();
        }
        if (effects.entityBridge().runtime.consumePendingSwitchBlockAnimationRequest()
                && effects.terrain().switchableObjectAnimationStage == 0) {
            effects.terrain().switchableObjectAnimationStage = 0x01;
        }
        effects.entityFrame().enemyDropCounters =
                effects.entityBridge().runtime.inspection().enemyDropCounters();
        if (effects.transientVfxSystem() != null) {
            var transientVfxRequests = effects.entityBridge().runtime.enemies().transientVfxRequests();
            for (int requestIndex = 0; requestIndex < transientVfxRequests.size(); requestIndex++) {
                TransientVfxRequest request = transientVfxRequests.get(requestIndex);

                effects.transientVfxSystem()
                        .spawn(request.type(), request.worldX(), request.worldY(), request.variant());
            }
        }
        int clearedMask = effects.entityBridge().runtime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            effects.entityFrame()
                    .recentRoomEntityClears
                    .addMask(effects.location().activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = effects.entityBridge().runtime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && effects.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = effects.roomSessionViews()
                    .indoorStatusTableForMap(effects.location().activeRoom.mapId());
            status[effects.location().activeRoom.roomId()] |= (byte) roomStatusMask;
            effects.entityBridge()
                    .runtime
                    .configuration()
                    .setEntityRoomStatus(Byte.toUnsignedInt(
                            status[effects.location().activeRoom.roomId()]));
        }
    }

    private void applyObjects(List<BombExplosionEvent> bombExplosionEvents, int linkEntityX, int linkEntityY) {
        effects.roomTileRedraw()
                .applyHookshotBridgeUpdates(
                        effects.entityBridge().runtime.inspection().hookshotBridgeUpdates());
        effects.location()
                .activeRoom
                .replaceEntities(effects.entityBridge().runtime.inspection().view());
        boolean objectRequests = !bombExplosionEvents.isEmpty();
        effects.roomObjectMutations().applyBombObjectInteractions(bombExplosionEvents);
        var boomerangs = effects.entityBridge().runtime.enemies().boomerangObjectRequests();
        objectRequests |= !boomerangs.isEmpty();
        effects.roomObjectMutations().applyBoomerangObjectInteractions(boomerangs);
        var rods = effects.entityBridge().runtime.enemies().magicRodObjectRequests();
        objectRequests |= !rods.isEmpty();
        effects.roomObjectMutations().applyMagicRodObjectInteractions(rods);
        var powders = effects.entityBridge().runtime.enemies().magicPowderObjectRequests();
        objectRequests |= !powders.isEmpty();
        effects.roomObjectMutations().applyMagicPowderObjectInteractions(powders);
        // Empty object-request lists return without mutating runtime entities. The first
        // immutable snapshot already contains their final state in that common case.
        if (objectRequests)
            effects.location()
                    .activeRoom
                    .replaceEntities(effects.entityBridge().runtime.inspection().view());
        effects.roomPuzzleController().tickPushedBlockMotion(linkEntityX, linkEntityY);
        effects.roomPuzzleController().tickRoomEvent(linkEntityX, linkEntityY);
    }

    record EffectsDependencies(
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomTerrainState terrain,
            RoomEntitySessionState entityBridge,
            RoomEntityRewards roomEntityRewards,
            RoomObjectMutations roomObjectMutations,
            RoomPuzzleController roomPuzzleController,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomObjectMutations roomTileRedraw,
            RoomTransitionController roomTransitionController,
            TransientVfxSystem transientVfxSystem) {
        EffectsDependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(roomEntityRewards, "roomEntityRewards");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomPuzzleController, "roomPuzzleController");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
            java.util.Objects.requireNonNull(roomTransitionController, "roomTransitionController");
        }
    }
}
