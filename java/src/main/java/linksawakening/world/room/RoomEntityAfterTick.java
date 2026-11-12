package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.transition.Warp;

final class RoomEntityAfterTick {
    private final RoomSession session;

    RoomEntityAfterTick(RoomSession session) {
        this.session = session;
    }

    void complete(int linkEntityX, int linkEntityY) {
        synchronizeQuestState();
        var bombs = harvestRewards();
        persistEffects();
        applyObjects(bombs, linkEntityX, linkEntityY);
    }

    private void synchronizeQuestState() {
        for (DungeonWarpRequest request :
                session.context.entityBridge.entityRuntime.consumePendingDungeonWarpRequests()) {
            linksawakening.world.entity.event.WarpDestination destination = request.destination();
            session.context.pendingDungeonWarp = new Warp(
                    Warp.CATEGORY_INDOOR,
                    destination.mapId(),
                    destination.roomId(),
                    destination.x(),
                    destination.y(),
                    0x34);
        }
        session.context.shouldGetLostInMysteriousWoods =
                session.context.entityBridge.entityRuntime.inspection().shouldGetLostInMysteriousWoods();
        int runtimeBowWowState =
                session.context.entityBridge.entityRuntime.inspection().bowWowState();
        if (runtimeBowWowState != session.context.bowWowState) {
            session.context.bowWowState = runtimeBowWowState;
            session.context.followingNpcState = new FollowingNpcState(
                    session.context.followingNpcState.roosterFollowing(),
                    session.context.followingNpcState.ghostFollowingState(),
                    session.context.followingNpcState.marinFollowing(),
                    runtimeBowWowState == 0x01,
                    session.context.followingNpcState.instrument4Flags(),
                    session.context.followingNpcState.powerBraceletLevel(),
                    session.context.followingNpcState.db10());
            // The rescue/return handler changes the existing entity in place.
            // CreateFollowingNpcEntity runs on room entry, not on this quest
            // flag write: respawning here teleports BowWow onto Link and
            // reconstructs every other entity's runtime state.
        }
        if (session.context.ocarinaPlaybackCountdown > 0) {
            session.context.ocarinaPlaybackCountdown--;
            if (session.context.ocarinaPlaybackCountdown == 0) {
                session.context.roomTransitionController.finishOcarinaPlayback();
            }
        }
    }

    private List<BombExplosionEvent> harvestRewards() {
        List<BombExplosionEvent> bombExplosionEvents =
                session.context.entityBridge.entityRuntime.consumeBombExplosionEvents();
        session.context.entityBridge.outbox.pendingBombExplosionEvents.clear();
        session.context.entityBridge.outbox.pendingBombExplosionEvents.addAll(bombExplosionEvents);
        List<ChestRewardEvent> chestRewards =
                session.context.entityBridge.entityRuntime.consumePendingChestRewardEvents();
        for (ChestRewardEvent reward : chestRewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                session.context.roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        session.context.entityBridge.outbox.pendingChestRewardEvents.addAll(chestRewards);
        session.context.roomEntityRewards.harvestKeyQuicksandEvents();
        session.context.roomEntityRewards.harvestKeyRewardEvents();
        session.context.roomEntityRewards.harvestSlimeKeyRewardEvents();
        session.context.roomPuzzleController.harvestThrownRoomEventCollisions();
        session.context.roomEntityRewards.harvestHeartContainerRewards();
        session.context.entityBridge.outbox.pendingMadamMeowMeowFullHealRequests.addAll(
                session.context.entityBridge.entityRuntime.consumePendingMadamMeowMeowFullHealRequests());
        session.context.roomEntityRewards.harvestSwordPickupRewards();
        session.context.roomEntityRewards.harvestToadstoolRewards();
        session.context.roomEntityRewards.harvestInstrumentRewards();
        session.context.roomEntityRewards.harvestInstrumentCompletions();
        session.context.roomEntityRewards.harvestRichardProgressEvents();
        session.context.roomEntityRewards.harvestWitchEvents();
        session.context.roomEntityRewards.harvestOwlEventCompletions();
        return bombExplosionEvents;
    }

    private void persistEffects() {
        for (RoomStatusPersistenceRequest request :
                session.context.entityBridge.entityRuntime.consumePendingRoomStatusPersistenceRequests()) {
            if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.context.roomState.overworldRoomStatus[session.context.activeRoom.roomId()] |=
                        (byte) request.roomStatusMask();
            }
            session.context.tarinFlag = request.tarinFlag();
        }
        if (session.context.entityBridge.entityRuntime.consumePendingSwitchBlockAnimationRequest()
                && session.context.switchableObjectAnimationStage == 0) {
            session.context.switchableObjectAnimationStage = 0x01;
        }
        session.context.enemyDropCounters =
                session.context.entityBridge.entityRuntime.inspection().enemyDropCounters();
        if (session.context.transientVfxSystem != null) {
            for (TransientVfxRequest request :
                    session.context.entityBridge.entityRuntime.enemies().transientVfxRequests()) {
                session.context.transientVfxSystem.spawn(
                        request.type(), request.worldX(), request.worldY(), request.variant());
            }
        }
        int clearedMask = session.context.entityBridge.entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            session.context.recentRoomEntityClears.addMask(session.context.activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = session.context.entityBridge.entityRuntime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status =
                    session.context.roomSessionViews.indoorStatusTableForMap(session.context.activeRoom.mapId());
            status[session.context.activeRoom.roomId()] |= (byte) roomStatusMask;
            session.context
                    .entityBridge
                    .entityRuntime
                    .configuration()
                    .setEntityRoomStatus(Byte.toUnsignedInt(status[session.context.activeRoom.roomId()]));
        }
    }

    private void applyObjects(List<BombExplosionEvent> bombExplosionEvents, int linkEntityX, int linkEntityY) {
        session.context.roomTileRedraw.applyHookshotBridgeUpdates(
                session.context.entityBridge.entityRuntime.inspection().hookshotBridgeUpdates());
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        session.context.roomObjectMutations.applyBombObjectInteractions(bombExplosionEvents);
        session.context.roomObjectMutations.applyBoomerangObjectInteractions(
                session.context.entityBridge.entityRuntime.enemies().boomerangObjectRequests());
        session.context.roomObjectMutations.applyMagicRodObjectInteractions(
                session.context.entityBridge.entityRuntime.enemies().magicRodObjectRequests());
        session.context.roomObjectMutations.applyMagicPowderObjectInteractions(
                session.context.entityBridge.entityRuntime.enemies().magicPowderObjectRequests());
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        session.context.roomPuzzleController.tickPushedBlockMotion(linkEntityX, linkEntityY);
        session.context.roomPuzzleController.tickRoomEvent(linkEntityX, linkEntityY);
    }
}
