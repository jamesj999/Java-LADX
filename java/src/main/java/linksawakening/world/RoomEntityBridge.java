package linksawakening.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;



/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge {
    final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    final List<EntityCombatEvent> pendingRoomEntityEvents = new ArrayList<>();
    final List<ChestRewardEvent> pendingChestRewardEvents =
        new ArrayList<>();
    final List<KeyRewardEvent> pendingKeyRewardEvents =
        new ArrayList<>();
    final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents =
        new ArrayList<>();
    final List<HeartContainerRewardEvent>
        pendingHeartContainerRewards = new ArrayList<>();
    final List<MadamMeowMeowFullHealRequest>
        pendingMadamMeowMeowFullHealRequests = new ArrayList<>();
    final List<RichardProgressEvent>
        pendingRichardProgressEvents = new ArrayList<>();
    final List<SwordPickupRewardEvent>
        pendingSwordPickupRewards = new ArrayList<>();
    final List<ToadstoolRewardEvent>
        pendingToadstoolRewards = new ArrayList<>();
    final List<WitchExchangeEvent>
        pendingWitchExchangeEvents = new ArrayList<>();
    final List<WitchRewardEvent>
        pendingWitchRewardEvents = new ArrayList<>();
    final List<DialogRequest> pendingRoomDialogRequests =
        new ArrayList<>();
    RoomEntityRuntime entityRuntime;
    int pendingRoomMusicTrack = -1;

    List<EntityCombatEvent> consumeEntityEvents() {
        List<EntityCombatEvent> events = new ArrayList<>();
        if (entityRuntime != null) {
            events.addAll(entityRuntime.consumePendingEntityEvents());
        }
        events.addAll(pendingRoomEntityEvents);
        pendingRoomEntityEvents.clear();
        return List.copyOf(events);
    }

    List<LinkFinalPositionRequest>
            consumeLinkFinalPositionRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkFinalPositionRequests();
    }

    List<RoosterLinkStateRequest>
            consumeRoosterLinkStateRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingRoosterLinkStateRequests();
    }

    List<LinkMotionBlockRequest>
            consumeLinkMotionBlockRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkMotionBlockRequests();
    }

    List<SideViewPlatformLinkRequest>
            consumeSideViewPlatformLinkRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingSideViewPlatformLinkRequests();
    }

    List<SideViewPotLinkRequest>
            consumeSideViewPotLinkRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingSideViewPotLinkRequests();
    }

    List<HinoxLinkEffectRequest>
            consumeHinoxLinkEffectRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingHinoxLinkEffectRequests();
    }

    List<LinkFallenPoseRequest>
            consumeLinkFallenPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkFallenPoseRequests();
    }

    void setFreeMovementMode(boolean enabled) {
        if (entityRuntime != null) entityRuntime.setFreeMovementMode(enabled);
    }

    List<LinkFacingRequest> consumeLinkFacingRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkFacingRequests();
    }

    List<LinkAttackClearRequest>
            consumeLinkAttackClearRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkAttackClearRequests();
    }

    List<LinkAttackStepRequest>
            consumeLinkAttackStepRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkAttackStepRequests();
    }

    List<LinkHeldItemPoseRequest>
            consumeLinkHeldItemPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkHeldItemPoseRequests();
    }

    boolean gotItemPresentationActive() {
        return entityRuntime != null && entityRuntime.witchGotItemPresentationActive();
    }

    boolean powerUpPickupSequenceActive() {
        return entityRuntime != null && entityRuntime.powerUpPickupSequenceActive();
    }

    boolean swordPickupSequenceActive() {
        return entityRuntime != null && entityRuntime.swordPickupSequenceActive();
    }

    List<LinkSwordSpinPoseRequest>
            consumeLinkSwordSpinPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkSwordSpinPoseRequests();
    }

    List<LinkSwordFinalPoseRequest>
            consumeLinkSwordFinalPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkSwordFinalPoseRequests();
    }

    List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingScreenShakeRequests();
    }

    List<ChestRewardEvent> consumeChestRewardEvents() {
        List<ChestRewardEvent> rewards = List.copyOf(pendingChestRewardEvents);
        pendingChestRewardEvents.clear();
        return rewards;
    }

    List<KeyRewardEvent> consumeKeyRewardEvents() {
        List<KeyRewardEvent> rewards = List.copyOf(pendingKeyRewardEvents);
        pendingKeyRewardEvents.clear();
        return rewards;
    }

    List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() {
        List<SlimeKeyRewardEvent> rewards =
            List.copyOf(pendingSlimeKeyRewardEvents);
        pendingSlimeKeyRewardEvents.clear();
        return rewards;
    }

    List<HeartContainerRewardEvent>
            consumeHeartContainerRewards() {
        List<HeartContainerRewardEvent> rewards =
            List.copyOf(pendingHeartContainerRewards);
        pendingHeartContainerRewards.clear();
        return rewards;
    }

    List<MadamMeowMeowFullHealRequest>
            consumeMadamMeowMeowFullHealRequests() {
        List<MadamMeowMeowFullHealRequest> requests =
            List.copyOf(pendingMadamMeowMeowFullHealRequests);
        pendingMadamMeowMeowFullHealRequests.clear();
        return requests;
    }

    List<RichardProgressEvent> consumeRichardProgressEvents() {
        List<RichardProgressEvent> events =
            List.copyOf(pendingRichardProgressEvents);
        pendingRichardProgressEvents.clear();
        return events;
    }

    List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        List<SwordPickupRewardEvent> rewards =
            List.copyOf(pendingSwordPickupRewards);
        pendingSwordPickupRewards.clear();
        return rewards;
    }

    List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        List<ToadstoolRewardEvent> rewards =
            List.copyOf(pendingToadstoolRewards);
        pendingToadstoolRewards.clear();
        return rewards;
    }

    List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        List<WitchExchangeEvent> events =
            List.copyOf(pendingWitchExchangeEvents);
        pendingWitchExchangeEvents.clear();
        return events;
    }

    List<WitchRewardEvent> consumeWitchRewardEvents() {
        List<WitchRewardEvent> events =
            List.copyOf(pendingWitchRewardEvents);
        pendingWitchRewardEvents.clear();
        return events;
    }

    int consumePendingMusicTrack() {
        if (pendingRoomMusicTrack >= 0) {
            int pending = pendingRoomMusicTrack;
            pendingRoomMusicTrack = -1;
            return pending;
        }
        return entityRuntime == null ? -1 : entityRuntime.consumePendingMusicTrack();
    }

    int consumePendingMusicFadeOutCountdown() {
        return entityRuntime == null ? 0
            : entityRuntime.consumePendingMusicFadeOutCountdown();
    }

    List<LikeLikeEvent> consumeLikeLikeEvents() {
        if (entityRuntime == null) {
            return List.of();
        }
        return entityRuntime.consumePendingLikeLikeEvents();
    }

    List<DialogRequest> consumeEntityDialogRequests() {
        List<DialogRequest> requests = new ArrayList<>(
            pendingRoomDialogRequests);
        pendingRoomDialogRequests.clear();
        if (entityRuntime != null) {
            requests.addAll(entityRuntime.consumePendingDialogRequests());
        }
        return List.copyOf(requests);
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        List<BombExplosionEvent> pending = List.copyOf(pendingBombExplosionEvents);
        pendingBombExplosionEvents.clear();
        return pending;
    }

    private final RoomSession session;
    RoomEntityBridge(RoomSession session) { this.session = session; }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        if (maxHearts < 0 || maxHearts > 0xFF) {
            throw new IllegalArgumentException("Maximum hearts must be an unsigned byte: "
                + maxHearts);
        }
        if (health < 0 || health > 0xFF) {
            throw new IllegalArgumentException("Health must be an unsigned byte: " + health);
        }
        session.enemyDropMaxHearts = maxHearts;
        session.enemyDropHealth = health;
        session.enemyDropActivePowerUp = activePowerUp;
        session.configureEnemyDropRuntime();
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            session.boomerangSoundCounter = (session.boomerangSoundCounter + 1)
                % RoomEntityRuntime.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        session.boomerangSoundClockProvided = true;
    }

    void tickEntities(int frameCounter) {
        session.tickEntities(frameCounter, 0, 0);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        session.tickEntities(frameCounter, linkEntityX, linkEntityY,
            session.followingLinkZ, session.followingLinkDirection);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
            linkDirection, false);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkDirection) {
        session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkAirborne, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
            linkDirection, false);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield) {
        return session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, usingShield, 1, 0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, boolean usingShield) {
        return session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkAirborne, linkMotionState, linkDirection, 0, usingShield,
            1, 0, false, 0, 0, 0, 0, 0, 0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield, int shieldLevel, int invincibilityCounter) {
        return session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, usingShield, shieldLevel,
            invincibilityCounter, false, 0, 0, 0, 0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) {
        return session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, 0, usingShield, shieldLevel,
            invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY,
            swordHeight);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) {
        return session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, collisionType, usingShield,
            shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, 0, 0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        return session.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, false, linkMotionState, linkDirection, collisionType, usingShield,
            shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        session.followingLinkX = linkEntityX & 0xFF;
        session.followingLinkY = linkEntityY & 0xFF;
        session.followingLinkZ = linkEntityZ & 0xFF;
        session.followingLinkDirection = linkDirection & 0xFF;
        session.currentLinkMotionState = linkMotionState & 0xFF;
        session.shouldGetLostInMysteriousWoods = false;
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return List.of();
        }
        if (!session.boomerangSoundClockProvided) {
            session.advanceBoomerangSoundClock(true);
        }
        session.entityBridge.entityRuntime.setBoomerangSoundCounterForFrame(session.boomerangSoundCounter);
        session.boomerangSoundClockProvided = false;
        session.indoorTorchPaletteEffect.tick(session.entityInputs.entityDialogActive, false,
            session.currentLinkMotionState, session.activeRoom.mapId());
        session.entityBridge.entityRuntime.setTorchPaletteTransitionState(session.indoorTorchPaletteEffect.targetAddress(),
            session.indoorTorchPaletteEffect.transitionActive());
        session.copyIndoorTorchPalettesToActiveRoom();
        session.tickOcarinaAnimationHandler();
        session.entityBridge.entityRuntime.setSwitchBlockAnimationActive(
            SwitchBlockAnimation.isAnimating(session.switchableObjectAnimationStage));
        session.entityBridge.entityRuntime.setDialogActive(session.entityInputs.entityDialogActive);
        session.entityBridge.entityRuntime.setTalkState(
            session.entityInputs.entityInventoryAppearing, session.entityInputs.entityDialogCooldown, session.entityInputs.entityWindowY);
        session.entityBridge.entityRuntime.setDialogAskSelectionIndex(session.entityInputs.entityDialogAskSelectionIndex);
        session.entityBridge.entityRuntime.setActiveMusic(session.entityInputs.entityMusicActive);
        session.entityBridge.entityRuntime.setBowWowState(session.bowWowState);
        session.entityBridge.entityRuntime.setLinkVelocityZ(session.entityInputs.entityLinkVelocityZ);
        session.entityBridge.entityRuntime.setOwlInstrumentFlags(
            Byte.toUnsignedInt(session.roomState.dungeonProgressFlags[0]),
            Byte.toUnsignedInt(session.roomState.dungeonProgressFlags[1]));
        session.entityBridge.entityRuntime.setActionButtonsHeld(session.entityInputs.actionButtonAHeld, session.entityInputs.actionButtonBHeld);
        int statueMapId = session.activeRoom.mapId();
        session.entityBridge.entityRuntime.setOwlStatueDialogId(
            session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && (statueMapId < 8 || statueMapId == 0xFF)
                ? session.owlStatueDialogResolver.globalDialogId(statueMapId, session.activeRoom.roomId(),
                    session.roomState.dungeonItemState.currentFlag(DungeonItemState.STONE_BEAK_INDEX) != 0)
                : -1);
        session.entityBridge.entityRuntime.setJoypadHeld(session.entityInputs.joypadHeld);
        session.entityBridge.entityRuntime.setPressedButtonsMask(session.entityInputs.entityPressedButtonsMask);
        session.entityBridge.entityRuntime.setPowerBraceletButtonHeld(session.entityInputs.powerBraceletButtonHeld);
        session.entityBridge.entityRuntime.setBombButtonHeld(session.entityInputs.bombButtonHeld);
        session.entityBridge.entityRuntime.setLiftedLinkC13B(session.followingEntityYOffset);
        session.entityBridge.entityRuntime.setBooBuddyTriggerCount(session.roomTriggerCount);
        session.updateWitchEnvironment();
        session.entityBridge.entityRuntime.setOcarinaPlayback(session.ocarinaPlaybackCountdown,
            session.ocarinaSongFlags, session.selectedSongIndex, session.ocarinaAnimationCounter,
            session.ocarinaAnimationPhase);
        session.entityBridge.entityRuntime.setGoldenLeavesCount(session.entityInputs.entityGoldenLeavesCount);
        session.entityBridge.entityRuntime.setSecretSeashellPegasusCollisionState(
            session.entityInputs.secretSeashellScreenShakeActive, session.entityInputs.secretSeashellPegasusCollisionActive,
            session.entityInputs.secretSeashellPegasusCollisionX, session.entityInputs.secretSeashellPegasusCollisionY);
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        session.entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        if (session.pendingShovelDrop != null) {
            PendingShovelDrop drop = session.pendingShovelDrop;
            session.pendingShovelDrop = null;
            session.entityBridge.entityRuntime.spawnShovelDrop(
                drop.objectLeft(), drop.objectTop(), drop.linkEntityX(), drop.linkEntityY(),
                drop.allowDrop());
        }
        List<EntityProjectileEvent> events = session.entityBridge.entityRuntime.tick(new EntityFrameContext(
            frameCounter, linkEntityX, linkEntityY, collisionType & 0xFF,
            session.entityRandomByteSource,
            session::entityBackgroundCollision, session::pairoddProjectileObjectCollision,
            session.followingLinkPositionHistory, session.followingLinkZ, linkAirborne,
            session.followingLinkDirection, session.followingEntityYOffset,
            new EnemyProjectileCollision.LinkState(
                linkEntityX, linkEntityY, linkEntityZ, linkMotionState,
                session.romDirectionForProjectileCollision(linkDirection), usingShield, shieldLevel,
                invincibilityCounter), swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY));
        for (DungeonWarpRequest request
            : session.entityBridge.entityRuntime.consumePendingDungeonWarpRequests()) {
            linksawakening.world.entity.event.WarpDestination destination = request.destination();
            session.pendingDungeonWarp = new Warp(Warp.CATEGORY_INDOOR, destination.mapId(),
                destination.roomId(), destination.x(), destination.y(), 0x34);
        }
        session.shouldGetLostInMysteriousWoods = session.entityBridge.entityRuntime.shouldGetLostInMysteriousWoods();
        int runtimeBowWowState = session.entityBridge.entityRuntime.bowWowState();
        if (runtimeBowWowState != session.bowWowState) {
            session.bowWowState = runtimeBowWowState;
            session.followingNpcState = new FollowingNpcState(
                session.followingNpcState.roosterFollowing(),
                session.followingNpcState.ghostFollowingState(),
                session.followingNpcState.marinFollowing(), runtimeBowWowState == 0x01,
                session.followingNpcState.instrument4Flags(),
                session.followingNpcState.powerBraceletLevel(), session.followingNpcState.db10());
            // The rescue/return handler changes the existing entity in place.
            // CreateFollowingNpcEntity runs on room entry, not on this quest
            // flag write: respawning here teleports BowWow onto Link and
            // reconstructs every other entity's runtime state.
        }
        if (session.ocarinaPlaybackCountdown > 0) {
            session.ocarinaPlaybackCountdown--;
            if (session.ocarinaPlaybackCountdown == 0) {
                session.finishOcarinaPlayback();
            }
        }
        List<BombExplosionEvent> bombExplosionEvents = session.entityBridge.entityRuntime.consumeBombExplosionEvents();
        session.entityBridge.pendingBombExplosionEvents.clear();
        session.entityBridge.pendingBombExplosionEvents.addAll(bombExplosionEvents);
        List<ChestRewardEvent> chestRewards =
            session.entityBridge.entityRuntime.consumePendingChestRewardEvents();
        for (ChestRewardEvent reward : chestRewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                session.roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        session.entityBridge.pendingChestRewardEvents.addAll(chestRewards);
        session.harvestKeyQuicksandEvents();
        session.harvestKeyRewardEvents();
        session.harvestSlimeKeyRewardEvents();
        session.harvestThrownRoomEventCollisions();
        session.harvestHeartContainerRewards();
        session.entityBridge.pendingMadamMeowMeowFullHealRequests.addAll(
            session.entityBridge.entityRuntime.consumePendingMadamMeowMeowFullHealRequests());
        session.harvestSwordPickupRewards();
        session.harvestToadstoolRewards();
        session.harvestInstrumentRewards();
        session.harvestInstrumentCompletions();
        session.harvestRichardProgressEvents();
        session.harvestWitchEvents();
        session.harvestOwlEventCompletions();
        for (RoomStatusPersistenceRequest request
            : session.entityBridge.entityRuntime.consumePendingRoomStatusPersistenceRequests()) {
            if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.roomState.overworldRoomStatus[session.activeRoom.roomId()] |= (byte) request.roomStatusMask();
            }
            session.tarinFlag = request.tarinFlag();
        }
        if (session.entityBridge.entityRuntime.consumePendingSwitchBlockAnimationRequest()
            && session.switchableObjectAnimationStage == 0) {
            session.switchableObjectAnimationStage = 0x01;
        }
        session.enemyDropCounters = session.entityBridge.entityRuntime.enemyDropCounters();
        if (session.transientVfxSystem != null) {
            for (TransientVfxRequest request
                : session.entityBridge.entityRuntime.transientVfxRequests()) {
                session.transientVfxSystem.spawn(request.type(), request.worldX(), request.worldY(),
                    request.variant());
            }
        }
        int clearedMask = session.entityBridge.entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            session.recentRoomEntityClears.addMask(session.activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = session.entityBridge.entityRuntime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = session.indoorStatusTableForMap(session.activeRoom.mapId());
            status[session.activeRoom.roomId()] |= (byte) roomStatusMask;
            session.entityBridge.entityRuntime.setEntityRoomStatus(
                Byte.toUnsignedInt(status[session.activeRoom.roomId()]));
        }
        session.applyHookshotBridgeUpdates(session.entityBridge.entityRuntime.hookshotBridgeUpdates());
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        session.applyBombObjectInteractions(bombExplosionEvents);
        session.applyBoomerangObjectInteractions(session.entityBridge.entityRuntime.boomerangObjectRequests());
        session.applyMagicRodObjectInteractions(session.entityBridge.entityRuntime.magicRodObjectRequests());
        session.applyMagicPowderObjectInteractions(session.entityBridge.entityRuntime.magicPowderObjectRequests());
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        session.tickPushedBlockMotion(linkEntityX, linkEntityY);
        session.tickRoomEvent(linkEntityX, linkEntityY);
        return events;
    }

    List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests = session.entityBridge.entityRuntime == null
            ? List.of() : session.entityBridge.entityRuntime.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            session.ocarinaPlaybackCountdown = 0;
            session.ocarinaAnimationCounter = 0;
            session.ocarinaAnimationPhase = 0;
            session.entityInputs.entityDialogActive = false;
            session.entityInputs.entityDialogCooldown = 0;
            session.entityInputs.entityInventoryAppearing = false;
        }
        return requests;
    }

    boolean consumeWorldLinkMotionBlockRequest() {
        boolean pending = session.worldLinkMotionBlockPending;
        session.worldLinkMotionBlockPending = false;
        return pending;
    }

    void tickGameplayVBlank() {
        if (SwitchBlockAnimation.isAnimating(session.switchableObjectAnimationStage)) {
            SwitchBlockAnimation.Step step = SwitchBlockAnimation.advance(
                session.switchableObjectAnimationStage, session.switchBlocksState);
            session.switchBlocksState = step.switchBlocksState();
            session.switchableObjectAnimationStage = step.nextStage();
            session.synchronizeSwitchBlockCollisionState();
            if (step.tileCopy() != null) {
                session.gpu.copySwitchBlockTiles(session.romData,
                    step.tileCopy().sourceOffset(), step.tileCopy().destinationTile());
            }
            // AnimateTiles returns after UpdateSwitchBlockTiles while the
            // switch animation is active, so ordinary animated BG tiles wait
            // until the next idle VBlank.
            return;
        }
        session.gpu.tickAnimatedTiles(session.romData);
    }

    List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) {
        return session.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            EnemyAttackContext.standard());
    }

    List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) {
        return session.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
    }

    List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, int linkVerticalVelocity, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return List.of();
        }
        List<EntityCombatEvent> events = session.entityBridge.entityRuntime.resolveCombat(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity,
            linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return events;
    }

    int clearEntity(int slot) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return 0;
        }
        int mask = session.entityBridge.entityRuntime.clearEntity(slot);
        if (mask != 0) {
            session.recentRoomEntityClears.addMask(session.activeRoom.roomId(), mask);
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return session.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, session.followingLinkDirection, session.followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection) {
        return session.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, linkDirection, session.followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return null;
        }
        session.followingLinkZ = linkZ & 0xFF;
        session.followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = session.entityBridge.entityRuntime.collectIfNeeded(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive,
            linkDirection, linkZ);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            session.recentRoomEntityClears.addMask(
                session.activeRoom.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30 && session.activeRoom.roomId() == 0x7C
            && session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            session.roomState.indoorARoomStatus[0x69] |= 0x10;
        }
        if (event.type() == session.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            session.markActiveRoomCompleted();
            session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0xEF));
            session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                event.slot(), event.type(), 0, false,
                EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        session.harvestKeyRewardEvents();
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return event;
    }

}
