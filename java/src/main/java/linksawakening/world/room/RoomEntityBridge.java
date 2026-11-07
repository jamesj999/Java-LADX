package linksawakening.world.room;

import linksawakening.world.object.SwitchBlockAnimation;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.BombExplosionEvent;

import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.FollowingNpcState;

import linksawakening.world.transition.Warp;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
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
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

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
        return entityRuntime != null && entityRuntime.inspection().witchGotItemPresentationActive();
    }

    boolean powerUpPickupSequenceActive() {
        return entityRuntime != null && entityRuntime.inspection().powerUpPickupSequenceActive();
    }

    boolean swordPickupSequenceActive() {
        return entityRuntime != null && entityRuntime.inspection().swordPickupSequenceActive();
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
        session.context.enemyDropMaxHearts = maxHearts;
        session.context.enemyDropHealth = health;
        session.context.enemyDropActivePowerUp = activePowerUp;
        session.configureEnemyDropRuntime();
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            session.context.boomerangSoundCounter = (session.context.boomerangSoundCounter + 1)
                % RoomEntityRuntime.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        session.context.boomerangSoundClockProvided = true;
    }

    void tickEntities(int frameCounter) {
        session.tickEntities(frameCounter, 0, 0);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        session.tickEntities(frameCounter, linkEntityX, linkEntityY,
            session.context.followingLinkZ, session.context.followingLinkDirection);
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
        session.context.followingLinkX = linkEntityX & 0xFF;
        session.context.followingLinkY = linkEntityY & 0xFF;
        session.context.followingLinkZ = linkEntityZ & 0xFF;
        session.context.followingLinkDirection = linkDirection & 0xFF;
        session.context.currentLinkMotionState = linkMotionState & 0xFF;
        session.context.shouldGetLostInMysteriousWoods = false;
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return List.of();
        }
        if (!session.context.boomerangSoundClockProvided) {
            session.advanceBoomerangSoundClock(true);
        }
        session.context.entityBridge.entityRuntime.configuration().setBoomerangSoundCounterForFrame(session.context.boomerangSoundCounter);
        session.context.boomerangSoundClockProvided = false;
        session.context.indoorTorchPaletteEffect.tick(session.context.entityInputs.entityDialogActive, false,
            session.context.currentLinkMotionState, session.context.activeRoom.mapId());
        session.context.entityBridge.entityRuntime.configuration().setTorchPaletteTransitionState(session.context.indoorTorchPaletteEffect.targetAddress(),
            session.context.indoorTorchPaletteEffect.transitionActive());
        session.copyIndoorTorchPalettesToActiveRoom();
        session.tickOcarinaAnimationHandler();
        session.context.entityBridge.entityRuntime.configuration().setSwitchBlockAnimationActive(
            SwitchBlockAnimation.isAnimating(session.context.switchableObjectAnimationStage));
        session.context.entityBridge.entityRuntime.configuration().setDialogActive(session.context.entityInputs.entityDialogActive);
        session.context.entityBridge.entityRuntime.configuration().setTalkState(
            session.context.entityInputs.entityInventoryAppearing, session.context.entityInputs.entityDialogCooldown, session.context.entityInputs.entityWindowY);
        session.context.entityBridge.entityRuntime.configuration().setDialogAskSelectionIndex(session.context.entityInputs.entityDialogAskSelectionIndex);
        session.context.entityBridge.entityRuntime.configuration().setActiveMusic(session.context.entityInputs.entityMusicActive);
        session.context.entityBridge.entityRuntime.configuration().setBowWowState(session.context.bowWowState);
        session.context.entityBridge.entityRuntime.configuration().setLinkVelocityZ(session.context.entityInputs.entityLinkVelocityZ);
        session.context.entityBridge.entityRuntime.configuration().setOwlInstrumentFlags(
            Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[0]),
            Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[1]));
        session.context.entityBridge.entityRuntime.configuration().setActionButtonsHeld(session.context.entityInputs.actionButtonAHeld, session.context.entityInputs.actionButtonBHeld);
        int statueMapId = session.context.activeRoom.mapId();
        session.context.entityBridge.entityRuntime.configuration().setOwlStatueDialogId(
            session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && (statueMapId < 8 || statueMapId == 0xFF)
                ? session.context.owlStatueDialogResolver.globalDialogId(statueMapId, session.context.activeRoom.roomId(),
                    session.context.roomState.dungeonItemState.currentFlag(DungeonItemState.STONE_BEAK_INDEX) != 0)
                : -1);
        session.context.entityBridge.entityRuntime.configuration().setJoypadHeld(session.context.entityInputs.joypadHeld);
        session.context.entityBridge.entityRuntime.configuration().setPressedButtonsMask(session.context.entityInputs.entityPressedButtonsMask);
        session.context.entityBridge.entityRuntime.configuration().setPowerBraceletButtonHeld(session.context.entityInputs.powerBraceletButtonHeld);
        session.context.entityBridge.entityRuntime.configuration().setBombButtonHeld(session.context.entityInputs.bombButtonHeld);
        session.context.entityBridge.entityRuntime.configuration().setLiftedLinkC13B(session.context.followingEntityYOffset);
        session.context.entityBridge.entityRuntime.configuration().setBooBuddyTriggerCount(session.context.roomTriggerCount);
        session.updateWitchEnvironment();
        session.context.entityBridge.entityRuntime.configuration().setOcarinaPlayback(session.context.ocarinaPlaybackCountdown,
            session.context.ocarinaSongFlags, session.context.selectedSongIndex, session.context.ocarinaAnimationCounter,
            session.context.ocarinaAnimationPhase);
        session.context.entityBridge.entityRuntime.configuration().setGoldenLeavesCount(session.context.entityInputs.entityGoldenLeavesCount);
        session.context.entityBridge.entityRuntime.configuration().setSecretSeashellPegasusCollisionState(
            session.context.entityInputs.secretSeashellScreenShakeActive, session.context.entityInputs.secretSeashellPegasusCollisionActive,
            session.context.entityInputs.secretSeashellPegasusCollisionX, session.context.entityInputs.secretSeashellPegasusCollisionY);
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        session.context.entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        if (session.context.pendingShovelDrop != null) {
            PendingShovelDrop drop = session.context.pendingShovelDrop;
            session.context.pendingShovelDrop = null;
            session.context.entityBridge.entityRuntime.spawnShovelDrop(
                drop.objectLeft(), drop.objectTop(), drop.linkEntityX(), drop.linkEntityY(),
                drop.allowDrop());
        }
        List<EntityProjectileEvent> events = session.context.entityBridge.entityRuntime.tick(new EntityFrameContext(
            frameCounter, linkEntityX, linkEntityY, collisionType & 0xFF,
            session.context.entityRandomByteSource,
            session::entityBackgroundCollision, session::pairoddProjectileObjectCollision,
            session.context.followingLinkPositionHistory, session.context.followingLinkZ, linkAirborne,
            session.context.followingLinkDirection, session.context.followingEntityYOffset,
            new EnemyProjectileCollision.LinkState(
                linkEntityX, linkEntityY, linkEntityZ, linkMotionState,
                session.romDirectionForProjectileCollision(linkDirection), usingShield, shieldLevel,
                invincibilityCounter), swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY));
        for (DungeonWarpRequest request
            : session.context.entityBridge.entityRuntime.consumePendingDungeonWarpRequests()) {
            linksawakening.world.entity.event.WarpDestination destination = request.destination();
            session.context.pendingDungeonWarp = new Warp(Warp.CATEGORY_INDOOR, destination.mapId(),
                destination.roomId(), destination.x(), destination.y(), 0x34);
        }
        session.context.shouldGetLostInMysteriousWoods = session.context.entityBridge.entityRuntime.inspection().shouldGetLostInMysteriousWoods();
        int runtimeBowWowState = session.context.entityBridge.entityRuntime.inspection().bowWowState();
        if (runtimeBowWowState != session.context.bowWowState) {
            session.context.bowWowState = runtimeBowWowState;
            session.context.followingNpcState = new FollowingNpcState(
                session.context.followingNpcState.roosterFollowing(),
                session.context.followingNpcState.ghostFollowingState(),
                session.context.followingNpcState.marinFollowing(), runtimeBowWowState == 0x01,
                session.context.followingNpcState.instrument4Flags(),
                session.context.followingNpcState.powerBraceletLevel(), session.context.followingNpcState.db10());
            // The rescue/return handler changes the existing entity in place.
            // CreateFollowingNpcEntity runs on room entry, not on this quest
            // flag write: respawning here teleports BowWow onto Link and
            // reconstructs every other entity's runtime state.
        }
        if (session.context.ocarinaPlaybackCountdown > 0) {
            session.context.ocarinaPlaybackCountdown--;
            if (session.context.ocarinaPlaybackCountdown == 0) {
                session.finishOcarinaPlayback();
            }
        }
        List<BombExplosionEvent> bombExplosionEvents = session.context.entityBridge.entityRuntime.consumeBombExplosionEvents();
        session.context.entityBridge.pendingBombExplosionEvents.clear();
        session.context.entityBridge.pendingBombExplosionEvents.addAll(bombExplosionEvents);
        List<ChestRewardEvent> chestRewards =
            session.context.entityBridge.entityRuntime.consumePendingChestRewardEvents();
        for (ChestRewardEvent reward : chestRewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                session.context.roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        session.context.entityBridge.pendingChestRewardEvents.addAll(chestRewards);
        session.harvestKeyQuicksandEvents();
        session.harvestKeyRewardEvents();
        session.harvestSlimeKeyRewardEvents();
        session.harvestThrownRoomEventCollisions();
        session.harvestHeartContainerRewards();
        session.context.entityBridge.pendingMadamMeowMeowFullHealRequests.addAll(
            session.context.entityBridge.entityRuntime.consumePendingMadamMeowMeowFullHealRequests());
        session.harvestSwordPickupRewards();
        session.harvestToadstoolRewards();
        session.harvestInstrumentRewards();
        session.harvestInstrumentCompletions();
        session.harvestRichardProgressEvents();
        session.harvestWitchEvents();
        session.harvestOwlEventCompletions();
        for (RoomStatusPersistenceRequest request
            : session.context.entityBridge.entityRuntime.consumePendingRoomStatusPersistenceRequests()) {
            if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.context.roomState.overworldRoomStatus[session.context.activeRoom.roomId()] |= (byte) request.roomStatusMask();
            }
            session.context.tarinFlag = request.tarinFlag();
        }
        if (session.context.entityBridge.entityRuntime.consumePendingSwitchBlockAnimationRequest()
            && session.context.switchableObjectAnimationStage == 0) {
            session.context.switchableObjectAnimationStage = 0x01;
        }
        session.context.enemyDropCounters = session.context.entityBridge.entityRuntime.inspection().enemyDropCounters();
        if (session.context.transientVfxSystem != null) {
            for (TransientVfxRequest request
                : session.context.entityBridge.entityRuntime.enemies().transientVfxRequests()) {
                session.context.transientVfxSystem.spawn(request.type(), request.worldX(), request.worldY(),
                    request.variant());
            }
        }
        int clearedMask = session.context.entityBridge.entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            session.context.recentRoomEntityClears.addMask(session.context.activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = session.context.entityBridge.entityRuntime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = session.indoorStatusTableForMap(session.context.activeRoom.mapId());
            status[session.context.activeRoom.roomId()] |= (byte) roomStatusMask;
            session.context.entityBridge.entityRuntime.configuration().setEntityRoomStatus(
                Byte.toUnsignedInt(status[session.context.activeRoom.roomId()]));
        }
        session.applyHookshotBridgeUpdates(session.context.entityBridge.entityRuntime.inspection().hookshotBridgeUpdates());
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.inspection().snapshot());
        session.applyBombObjectInteractions(bombExplosionEvents);
        session.applyBoomerangObjectInteractions(session.context.entityBridge.entityRuntime.enemies().boomerangObjectRequests());
        session.applyMagicRodObjectInteractions(session.context.entityBridge.entityRuntime.enemies().magicRodObjectRequests());
        session.applyMagicPowderObjectInteractions(session.context.entityBridge.entityRuntime.enemies().magicPowderObjectRequests());
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.inspection().snapshot());
        session.tickPushedBlockMotion(linkEntityX, linkEntityY);
        session.tickRoomEvent(linkEntityX, linkEntityY);
        return events;
    }

    List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests = session.context.entityBridge.entityRuntime == null
            ? List.of() : session.context.entityBridge.entityRuntime.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            session.context.ocarinaPlaybackCountdown = 0;
            session.context.ocarinaAnimationCounter = 0;
            session.context.ocarinaAnimationPhase = 0;
            session.context.entityInputs.entityDialogActive = false;
            session.context.entityInputs.entityDialogCooldown = 0;
            session.context.entityInputs.entityInventoryAppearing = false;
        }
        return requests;
    }

    boolean consumeWorldLinkMotionBlockRequest() {
        boolean pending = session.context.worldLinkMotionBlockPending;
        session.context.worldLinkMotionBlockPending = false;
        return pending;
    }

    void tickGameplayVBlank() {
        if (SwitchBlockAnimation.isAnimating(session.context.switchableObjectAnimationStage)) {
            SwitchBlockAnimation.Step step = SwitchBlockAnimation.advance(
                session.context.switchableObjectAnimationStage, session.context.switchBlocksState);
            session.context.switchBlocksState = step.switchBlocksState();
            session.context.switchableObjectAnimationStage = step.nextStage();
            session.synchronizeSwitchBlockCollisionState();
            if (step.tileCopy() != null) {
                session.context.gpu.copySwitchBlockTiles(session.context.romData,
                    step.tileCopy().sourceOffset(), step.tileCopy().destinationTile());
            }
            // AnimateTiles returns after UpdateSwitchBlockTiles while the
            // switch animation is active, so ordinary animated BG tiles wait
            // until the next idle VBlank.
            return;
        }
        session.context.gpu.tickAnimatedTiles(session.context.romData);
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
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return List.of();
        }
        List<EntityCombatEvent> events = session.context.entityBridge.entityRuntime.resolveCombat(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity,
            linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.inspection().snapshot());
        return events;
    }

    boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return session.context.entityBridge.entityRuntime != null
            && session.context.entityBridge.entityRuntime.deferContactRecoil(slot, linkX, linkY);
    }

    int clearEntity(int slot) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return 0;
        }
        int mask = session.context.entityBridge.entityRuntime.clearEntity(slot);
        if (mask != 0) {
            session.context.recentRoomEntityClears.addMask(session.context.activeRoom.roomId(), mask);
        }
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.inspection().snapshot());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return session.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, session.context.followingLinkDirection, session.context.followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection) {
        return session.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, linkDirection, session.context.followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return null;
        }
        session.context.followingLinkZ = linkZ & 0xFF;
        session.context.followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = session.context.entityBridge.entityRuntime.collectIfNeeded(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive,
            linkDirection, linkZ);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            session.context.recentRoomEntityClears.addMask(
                session.context.activeRoom.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30 && session.context.activeRoom.roomId() == 0x7C
            && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            session.context.roomState.indoorARoomStatus[0x69] |= 0x10;
        }
        if (event.type() == session.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            session.markActiveRoomCompleted();
            session.context.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0xEF));
            session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                event.slot(), event.type(), 0, false,
                EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        session.harvestKeyRewardEvents();
        session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.inspection().snapshot());
        return event;
    }

}
