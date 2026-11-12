package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.transition.Warp;

/** RoomEntityRewards operations for the live room session. */
final class RoomEntityRewards {
    private final Dependencies dependencies;

    RoomEntityRewards(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void harvestKeyQuicksandEvents() {
        if (dependencies.entityBridge().get().entityRuntime == null
                || dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .consumePendingKeyQuicksandEvents()
                        .isEmpty()) {
            return;
        }
        dependencies.roomState().overworldRoomStatus[0xCE] |= (byte) OW_ROOM_STATUS_FLAG_CHANGED;
        dependencies.roomState().indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestKeyRewardEvents() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        List<KeyRewardEvent> rewards =
                dependencies.entityBridge().get().entityRuntime.consumePendingKeyRewardEvents();
        for (KeyRewardEvent reward : rewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                dependencies.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
            dependencies.roomStoryState().get().markActiveRoomCompleted();
        }
        dependencies.entityBridge().get().outbox.pendingKeyRewardEvents.addAll(rewards);
    }

    void harvestHeartContainerRewards() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        List<HeartContainerRewardEvent> rewards =
                dependencies.entityBridge().get().entityRuntime.consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomStoryState().get().markHeartContainerCollected();
        dependencies.entityBridge().get().outbox.pendingHeartContainerRewards.addAll(rewards);
    }

    void harvestSwordPickupRewards() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        List<SwordPickupRewardEvent> rewards =
                dependencies.entityBridge().get().entityRuntime.consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomStoryState().get().markActiveRoomCompleted();
        dependencies.entityBridge().get().outbox.pendingSwordPickupRewards.addAll(rewards);
    }

    void harvestToadstoolRewards() {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingToadstoolRewards
                    .addAll(dependencies.entityBridge().get().entityRuntime.consumePendingToadstoolRewards());
        }
    }

    void harvestInstrumentRewards() {
        if (dependencies.entityBridge().get().entityRuntime == null
                || dependencies.location().activeRoom == null
                || dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .consumePendingInstrumentRewards()
                        .isEmpty()
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |=
                (byte) ROOM_STATUS_EVENT_1;
        if (dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length) {
            dependencies.roomState()
                    .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x02;
        }
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setEntityRoomStatus(dependencies.roomSessionViews().get().activeRoomStatusFlags());
    }

    void harvestInstrumentCompletions() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        for (InstrumentCompletionEvent completion :
                dependencies.entityBridge().get().entityRuntime.consumePendingInstrumentCompletions()) {
            dependencies.items().pendingInstrumentTransition = true;
            if (completion.mapId() == 0x00) {
                dependencies.followers().bowWowState = 0x80;
            } else if (completion.mapId() == 0x01) {
                dependencies.followers().tarinFlag = 0x02;
            }
        }
    }

    void harvestRichardProgressEvents() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        List<RichardProgressEvent> events =
                dependencies.entityBridge().get().entityRuntime.consumePendingRichardProgressEvents();
        for (RichardProgressEvent event : events) {
            if (event.kind() == RichardProgressEvent.Kind.START_QUEST) {
                dependencies.roomState().indoorBRoomStatus[0xC7] |= (byte) ROOM_STATUS_EVENT_1;
                dependencies.followers().richardSpokenFlag = Math.max(dependencies.followers().richardSpokenFlag, 0x02);
            } else {
                dependencies.entityInputs().get().entityGoldenLeavesCount = event.goldenLeavesCount();
            }
        }
        dependencies.entityBridge().get().outbox.pendingRichardProgressEvents.addAll(events);
    }

    void harvestWitchEvents() {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingWitchExchangeEvents
                    .addAll(dependencies.entityBridge().get().entityRuntime.consumePendingWitchExchangeEvents());
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingWitchRewardEvents
                    .addAll(dependencies.entityBridge().get().entityRuntime.consumePendingWitchRewardEvents());
        }
    }

    void harvestOwlEventCompletions() {
        if (dependencies.entityBridge().get().entityRuntime == null || dependencies.location().activeRoom == null) {
            return;
        }
        if (dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .consumePendingOwlEventCompletions()
                .isEmpty()) {
            return;
        }
        dependencies.roomState()
                .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
    }

    void harvestSlimeKeyRewardEvents() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        List<SlimeKeyRewardEvent> rewards =
                dependencies.entityBridge().get().entityRuntime.consumePendingSlimeKeyRewardEvents();
        for (SlimeKeyRewardEvent reward : rewards) {
            dependencies.entityInputs().get().entityGoldenLeavesCount = reward.goldenLeavesCount();
            dependencies.roomStoryState().get().markActiveRoomCompleted();
        }
        dependencies.entityBridge().get().outbox.pendingSlimeKeyRewardEvents.addAll(rewards);
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState) {}
}
