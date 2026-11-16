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

    RoomEntitySimulation(Dependencies dependencies, SetupDependencies setup, EffectsDependencies effects) {
        this.setup = setup;
        this.effects = effects;
        this.dependencies = dependencies;
    }

    List<EntityProjectileEvent> tickEntities(RoomRuntime loaded, EntityTickValues inputs) {
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

        configureRuntimeForFrame(loaded);
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        dependencies.entityRandomByteSource().beginFrame(frameCounter & 0xFF, 0);
        spawnPendingShovelDrop(loaded);
        List<EntityProjectileEvent> events = loaded.entities.tick(frameInputs
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType & 0xFF)
                .randomByteSupplier(dependencies.entityRandomByteSource())
                .backgroundCollision(loaded.backgroundCollision)
                .objectCollision(loaded.objectCollision)
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
        applyFrameEffects(loaded, linkEntityX, linkEntityY);
        return events;
    }

    record Dependencies(
            RoomEntityBridge entityBridge,
            RoomEntitySessionState entityFrame,
            RomRandomByteSource entityRandomByteSource,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location) {
        Dependencies {
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
        }
    }

    private void spawnPendingShovelDrop(RoomRuntime loaded) {
        if (dependencies.items().pendingShovelDrop != null) {
            PendingShovelDrop drop = dependencies.items().pendingShovelDrop;
            dependencies.items().pendingShovelDrop = null;
            loaded.entities.spawnShovelDrop(
                    drop.objectLeft(), drop.objectTop(), drop.linkEntityX(), drop.linkEntityY(), drop.allowDrop());
        }
    }

    void configureRuntimeForFrame(RoomRuntime loaded) {
        configurePresentation(loaded);
        configureInteraction(loaded);
        configureItems(loaded);
    }

    private void configurePresentation(RoomRuntime loaded) {
        if (!setup.entityFrame().boomerangSoundClockProvided) {
            setup.entityBridge().advanceBoomerangSoundClock(true);
        }
        loaded.entities.configuration().setBoomerangSoundCounterForFrame(setup.entityFrame().boomerangSoundCounter);
        setup.entityFrame().boomerangSoundClockProvided = false;
        setup.indoorTorchPaletteEffect()
                .tick(
                        setup.entityInputs().entityDialogActive,
                        false,
                        setup.entityFrame().currentLinkMotionState,
                        loaded.room.mapId());
        loaded.entities
                .configuration()
                .setTorchPaletteTransitionState(
                        setup.indoorTorchPaletteEffect().targetAddress(),
                        setup.indoorTorchPaletteEffect().transitionActive());
        setup.roomObjectMutations().copyIndoorTorchPalettesToActiveRoom(loaded.contents);
        setup.roomTransitionController().tickOcarinaAnimationHandler();
        loaded.entities
                .configuration()
                .setSwitchBlockAnimationActive(
                        SwitchBlockAnimation.isAnimating(setup.terrain().switchableObjectAnimationStage));
        loaded.entities.configuration().setDialogActive(setup.entityInputs().entityDialogActive);
        loaded.entities
                .configuration()
                .setTalkState(
                        setup.entityInputs().entityInventoryAppearing,
                        setup.entityInputs().entityDialogCooldown,
                        setup.entityInputs().entityWindowY);
        loaded.entities.configuration().setDialogAskSelectionIndex(setup.entityInputs().entityDialogAskSelectionIndex);
        loaded.entities.configuration().setActiveMusic(setup.entityInputs().entityMusicActive);
        loaded.entities.configuration().setBowWowState(setup.followers().bowWowState);
        loaded.entities.configuration().setLinkVelocityZ(setup.entityInputs().entityLinkVelocityZ);
    }

    private void configureInteraction(RoomRuntime loaded) {
        loaded.entities
                .configuration()
                .setOwlInstrumentFlags(
                        Byte.toUnsignedInt(setup.roomState().dungeonProgressFlags[0]),
                        Byte.toUnsignedInt(setup.roomState().dungeonProgressFlags[1]));
        loaded.entities
                .configuration()
                .setActionButtonsHeld(setup.entityInputs().actionButtonAHeld, setup.entityInputs().actionButtonBHeld);
        int statueMapId = loaded.room.mapId();
        loaded.entities
                .configuration()
                .setOwlStatueDialogId(
                        loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD && (statueMapId < 8 || statueMapId == 0xFF)
                                ? setup.owlStatueDialogResolver()
                                        .globalDialogId(
                                                statueMapId,
                                                loaded.room.roomId(),
                                                setup.roomState()
                                                                .dungeonItemState
                                                                .currentFlag(DungeonItemState.STONE_BEAK_INDEX)
                                                        != 0)
                                : -1);
        loaded.entities.configuration().setJoypadHeld(setup.entityInputs().joypadHeld);
        loaded.entities.configuration().setPressedButtonsMask(setup.entityInputs().entityPressedButtonsMask);
        loaded.entities.configuration().setPowerBraceletButtonHeld(setup.entityInputs().powerBraceletButtonHeld);
        loaded.entities.configuration().setBombButtonHeld(setup.entityInputs().bombButtonHeld);
        loaded.entities.configuration().setLiftedLinkC13B(setup.followers().followingEntityYOffset);
        loaded.entities.configuration().setBooBuddyTriggerCount(loaded.contents.puzzles.roomTriggerCount);
    }

    private void configureItems(RoomRuntime loaded) {
        setup.roomStoryState().updateWitchEnvironment(loaded);
        loaded.entities
                .configuration()
                .setOcarinaPlayback(
                        setup.items().ocarinaPlaybackCountdown,
                        setup.items().ocarinaSongFlags,
                        setup.items().selectedSongIndex,
                        setup.items().ocarinaAnimationCounter,
                        setup.items().ocarinaAnimationPhase);
        loaded.entities.configuration().setGoldenLeavesCount(setup.entityInputs().entityGoldenLeavesCount);
        loaded.entities
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

    void applyFrameEffects(RoomRuntime loaded, int linkEntityX, int linkEntityY) {
        synchronizeQuestState(loaded);
        var bombs = harvestRewards(loaded);
        persistEffects(loaded);
        applyObjects(loaded, bombs, linkEntityX, linkEntityY);
    }

    private void synchronizeQuestState(RoomRuntime loaded) {
        var dungeonWarpRequests = loaded.entities.consumePendingDungeonWarpRequests();
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
                loaded.entities.inspection().shouldGetLostInMysteriousWoods();
        int runtimeBowWowState = loaded.entities.inspection().bowWowState();
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
                effects.roomTransitionController().finishOcarinaPlayback(loaded);
            }
        }
    }

    private List<BombExplosionEvent> harvestRewards(RoomRuntime loaded) {
        List<BombExplosionEvent> bombExplosionEvents = loaded.entities.consumeBombExplosionEvents();
        effects.entityBridge().outbox.pendingBombExplosionEvents.clear();
        RoomEntityOutbox.append(effects.entityBridge().outbox.pendingBombExplosionEvents, bombExplosionEvents);
        List<ChestRewardEvent> chestRewards = loaded.entities.consumePendingChestRewardEvents();
        for (int rewardIndex = 0; rewardIndex < chestRewards.size(); rewardIndex++) {
            ChestRewardEvent reward = chestRewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                effects.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        RoomEntityOutbox.append(effects.entityBridge().outbox.pendingChestRewardEvents, chestRewards);
        effects.roomEntityRewards().harvestKeyQuicksandEvents(loaded);
        effects.roomEntityRewards().harvestKeyRewardEvents(loaded);
        effects.roomEntityRewards().harvestSlimeKeyRewardEvents(loaded);
        effects.roomPuzzleController().harvestThrownRoomEventCollisions(loaded);
        effects.roomEntityRewards().harvestHeartContainerRewards(loaded);
        RoomEntityOutbox.append(
                effects.entityBridge().outbox.pendingMadamMeowMeowFullHealRequests,
                loaded.entities.consumePendingMadamMeowMeowFullHealRequests());
        effects.roomEntityRewards().harvestSwordPickupRewards(loaded);
        effects.roomEntityRewards().harvestToadstoolRewards(loaded);
        effects.roomEntityRewards().harvestInstrumentRewards(loaded);
        effects.roomEntityRewards().harvestInstrumentCompletions(loaded);
        effects.roomEntityRewards().harvestRichardProgressEvents(loaded);
        effects.roomEntityRewards().harvestWitchEvents(loaded);
        effects.roomEntityRewards().harvestOwlEventCompletions(loaded);
        return bombExplosionEvents;
    }

    private void persistEffects(RoomRuntime loaded) {
        var roomStatusPersistenceRequests = loaded.entities.consumePendingRoomStatusPersistenceRequests();
        for (int requestIndex = 0; requestIndex < roomStatusPersistenceRequests.size(); requestIndex++) {
            RoomStatusPersistenceRequest request = roomStatusPersistenceRequests.get(requestIndex);

            if (loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                effects.roomState().overworldRoomStatus[loaded.room.roomId()] |= (byte) request.roomStatusMask();
            }
            effects.followers().tarinFlag = request.tarinFlag();
        }
        if (loaded.entities.consumePendingSwitchBlockAnimationRequest()
                && effects.terrain().switchableObjectAnimationStage == 0) {
            effects.terrain().switchableObjectAnimationStage = 0x01;
        }
        effects.entityFrame().enemyDropCounters = loaded.entities.inspection().enemyDropCounters();
        if (effects.transientVfxEnabled()) {
            var transientVfxRequests = loaded.entities.enemies().transientVfxRequests();
            for (int requestIndex = 0; requestIndex < transientVfxRequests.size(); requestIndex++) {
                TransientVfxRequest request = transientVfxRequests.get(requestIndex);

                effects.transientVfxSystem()
                        .spawn(request.type(), request.worldX(), request.worldY(), request.variant());
            }
        }
        int clearedMask = loaded.entities.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            effects.entityFrame().recentRoomEntityClears.addMask(loaded.room.roomId(), clearedMask);
        }
        int roomStatusMask = loaded.entities.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = effects.roomSessionViews().indoorStatusTableForMap(loaded.room.mapId());
            status[loaded.room.roomId()] |= (byte) roomStatusMask;
            loaded.entities.configuration().setEntityRoomStatus(Byte.toUnsignedInt(status[loaded.room.roomId()]));
        }
    }

    private void applyObjects(
            RoomRuntime loaded, List<BombExplosionEvent> bombExplosionEvents, int linkEntityX, int linkEntityY) {
        effects.roomTileRedraw()
                .applyHookshotBridgeUpdates(
                        loaded.contents, loaded.entities.inspection().hookshotBridgeUpdates());
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        boolean objectRequests = !bombExplosionEvents.isEmpty();
        effects.roomObjectMutations().applyBombObjectInteractions(loaded, bombExplosionEvents);
        var boomerangs = loaded.entities.enemies().boomerangObjectRequests();
        objectRequests |= !boomerangs.isEmpty();
        effects.roomObjectMutations().applyBoomerangObjectInteractions(loaded.contents, boomerangs);
        var rods = loaded.entities.enemies().magicRodObjectRequests();
        objectRequests |= !rods.isEmpty();
        effects.roomObjectMutations().applyMagicRodObjectInteractions(loaded.contents, rods);
        var powders = loaded.entities.enemies().magicPowderObjectRequests();
        objectRequests |= !powders.isEmpty();
        effects.roomObjectMutations().applyMagicPowderObjectInteractions(loaded.contents, powders);
        // Empty object-request lists return without mutating runtime entities. The first
        // immutable snapshot already contains their final state in that common case.
        if (objectRequests)
            loaded.room.replaceEntities(loaded.entities.inspection().view());
        effects.roomPuzzleController().tickPushedBlockMotion(loaded, linkEntityX, linkEntityY);
        effects.roomPuzzleController().tickRoomEvent(loaded, linkEntityX, linkEntityY);
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
            TransientVfxSystem transientVfxSystem,
            boolean transientVfxEnabled) {
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
            java.util.Objects.requireNonNull(transientVfxSystem, "transientVfxSystem");
        }
    }
}
