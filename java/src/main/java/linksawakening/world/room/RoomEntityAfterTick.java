package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.transition.Warp;

final class RoomEntityAfterTick {
    private final Dependencies dependencies;

    RoomEntityAfterTick(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void complete(int linkEntityX, int linkEntityY) {
        synchronizeQuestState();
        var bombs = harvestRewards();
        persistEffects();
        applyObjects(bombs, linkEntityX, linkEntityY);
    }

    private void synchronizeQuestState() {
        var dungeonWarpRequests =
                dependencies.entityBridge().get().entityRuntime.consumePendingDungeonWarpRequests();
        for (int requestIndex = 0; requestIndex < dungeonWarpRequests.size(); requestIndex++) {
            DungeonWarpRequest request = dungeonWarpRequests.get(requestIndex);

            linksawakening.world.entity.event.WarpDestination destination = request.destination();
            dependencies.location().pendingDungeonWarp = new Warp(
                    Warp.CATEGORY_INDOOR,
                    destination.mapId(),
                    destination.roomId(),
                    destination.x(),
                    destination.y(),
                    0x34);
        }
        dependencies.location().shouldGetLostInMysteriousWoods =
                dependencies.entityBridge().get().entityRuntime.inspection().shouldGetLostInMysteriousWoods();
        int runtimeBowWowState =
                dependencies.entityBridge().get().entityRuntime.inspection().bowWowState();
        if (runtimeBowWowState != dependencies.followers().bowWowState) {
            dependencies.followers().bowWowState = runtimeBowWowState;
            dependencies.followers().followingNpcState = new FollowingNpcState(
                    dependencies.followers().followingNpcState.roosterFollowing(),
                    dependencies.followers().followingNpcState.ghostFollowingState(),
                    dependencies.followers().followingNpcState.marinFollowing(),
                    runtimeBowWowState == 0x01,
                    dependencies.followers().followingNpcState.instrument4Flags(),
                    dependencies.followers().followingNpcState.powerBraceletLevel(),
                    dependencies.followers().followingNpcState.db10());
            // The rescue/return handler changes the existing entity in place.
            // CreateFollowingNpcEntity runs on room entry, not on this quest
            // flag write: respawning here teleports BowWow onto Link and
            // reconstructs every other entity's runtime state.
        }
        if (dependencies.items().ocarinaPlaybackCountdown > 0) {
            dependencies.items().ocarinaPlaybackCountdown--;
            if (dependencies.items().ocarinaPlaybackCountdown == 0) {
                dependencies.roomTransitionController().get().finishOcarinaPlayback();
            }
        }
    }

    private List<BombExplosionEvent> harvestRewards() {
        List<BombExplosionEvent> bombExplosionEvents =
                dependencies.entityBridge().get().entityRuntime.consumeBombExplosionEvents();
        dependencies.entityBridge().get().outbox.pendingBombExplosionEvents.clear();
        dependencies.entityBridge().get().outbox.pendingBombExplosionEvents.addAll(bombExplosionEvents);
        List<ChestRewardEvent> chestRewards =
                dependencies.entityBridge().get().entityRuntime.consumePendingChestRewardEvents();
        for (int rewardIndex = 0; rewardIndex < chestRewards.size(); rewardIndex++) {
            ChestRewardEvent reward = chestRewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                dependencies.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        dependencies.entityBridge().get().outbox.pendingChestRewardEvents.addAll(chestRewards);
        dependencies.roomEntityRewards().get().harvestKeyQuicksandEvents();
        dependencies.roomEntityRewards().get().harvestKeyRewardEvents();
        dependencies.roomEntityRewards().get().harvestSlimeKeyRewardEvents();
        dependencies.roomPuzzleController().get().harvestThrownRoomEventCollisions();
        dependencies.roomEntityRewards().get().harvestHeartContainerRewards();
        dependencies
                .entityBridge()
                .get()
                .outbox
                .pendingMadamMeowMeowFullHealRequests
                .addAll(dependencies.entityBridge().get().entityRuntime.consumePendingMadamMeowMeowFullHealRequests());
        dependencies.roomEntityRewards().get().harvestSwordPickupRewards();
        dependencies.roomEntityRewards().get().harvestToadstoolRewards();
        dependencies.roomEntityRewards().get().harvestInstrumentRewards();
        dependencies.roomEntityRewards().get().harvestInstrumentCompletions();
        dependencies.roomEntityRewards().get().harvestRichardProgressEvents();
        dependencies.roomEntityRewards().get().harvestWitchEvents();
        dependencies.roomEntityRewards().get().harvestOwlEventCompletions();
        return bombExplosionEvents;
    }

    private void persistEffects() {
        var roomStatusPersistenceRequests =
                dependencies.entityBridge().get().entityRuntime.consumePendingRoomStatusPersistenceRequests();
        for (int requestIndex = 0; requestIndex < roomStatusPersistenceRequests.size(); requestIndex++) {
            RoomStatusPersistenceRequest request = roomStatusPersistenceRequests.get(requestIndex);

            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                dependencies.roomState()
                        .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte)
                        request.roomStatusMask();
            }
            dependencies.followers().tarinFlag = request.tarinFlag();
        }
        if (dependencies.entityBridge().get().entityRuntime.consumePendingSwitchBlockAnimationRequest()
                && dependencies.terrain().switchableObjectAnimationStage == 0) {
            dependencies.terrain().switchableObjectAnimationStage = 0x01;
        }
        dependencies.entityFrame().enemyDropCounters =
                dependencies.entityBridge().get().entityRuntime.inspection().enemyDropCounters();
        if (dependencies.transientVfxSystem() != null) {
            var transientVfxRequests =
                    dependencies.entityBridge().get().entityRuntime.enemies().transientVfxRequests();
            for (int requestIndex = 0; requestIndex < transientVfxRequests.size(); requestIndex++) {
                TransientVfxRequest request = transientVfxRequests.get(requestIndex);

                dependencies
                        .transientVfxSystem()
                        .spawn(request.type(), request.worldX(), request.worldY(), request.variant());
            }
        }
        int clearedMask = dependencies.entityBridge().get().entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            dependencies
                    .entityFrame()
                    .recentRoomEntityClears
                    .addMask(dependencies.location().activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = dependencies.entityBridge().get().entityRuntime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = dependencies
                    .roomSessionViews()
                    .get()
                    .indoorStatusTableForMap(dependencies.location().activeRoom.mapId());
            status[dependencies.location().activeRoom.roomId()] |= (byte) roomStatusMask;
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEntityRoomStatus(Byte.toUnsignedInt(
                            status[dependencies.location().activeRoom.roomId()]));
        }
    }

    private void applyObjects(List<BombExplosionEvent> bombExplosionEvents, int linkEntityX, int linkEntityY) {
        dependencies
                .roomTileRedraw()
                .get()
                .applyHookshotBridgeUpdates(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .hookshotBridgeUpdates());
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        boolean objectRequests = !bombExplosionEvents.isEmpty();
        dependencies.roomObjectMutations().get().applyBombObjectInteractions(bombExplosionEvents);
        var boomerangs =
                dependencies.entityBridge().get().entityRuntime.enemies().boomerangObjectRequests();
        objectRequests |= !boomerangs.isEmpty();
        dependencies.roomObjectMutations().get().applyBoomerangObjectInteractions(boomerangs);
        var rods = dependencies.entityBridge().get().entityRuntime.enemies().magicRodObjectRequests();
        objectRequests |= !rods.isEmpty();
        dependencies.roomObjectMutations().get().applyMagicRodObjectInteractions(rods);
        var powders = dependencies.entityBridge().get().entityRuntime.enemies().magicPowderObjectRequests();
        objectRequests |= !powders.isEmpty();
        dependencies.roomObjectMutations().get().applyMagicPowderObjectInteractions(powders);
        // Empty object-request lists return without mutating runtime entities. The first
        // immutable snapshot already contains their final state in that common case.
        if (objectRequests)
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .snapshot());
        dependencies.roomPuzzleController().get().tickPushedBlockMotion(linkEntityX, linkEntityY);
        dependencies.roomPuzzleController().get().tickRoomEvent(linkEntityX, linkEntityY);
    }

    record Dependencies(
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
