package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.OW_ROOM_STATUS_FLAG_CHANGED;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_2;

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
        if (dependencies.entityRuntimeState().runtime == null
                || dependencies
                        .entityRuntimeState()
                        .runtime
                        .consumePendingKeyQuicksandEvents()
                        .isEmpty()) {
            return;
        }
        dependencies.roomState().overworldRoomStatus[0xCE] |= (byte) OW_ROOM_STATUS_FLAG_CHANGED;
        dependencies.roomState().indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestKeyRewardEvents() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        List<KeyRewardEvent> rewards = dependencies.entityRuntimeState().runtime.consumePendingKeyRewardEvents();
        for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
            KeyRewardEvent reward = rewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                dependencies.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
            dependencies.roomStoryState().get().markActiveRoomCompleted();
        }
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingKeyRewardEvents, rewards);
    }

    void harvestHeartContainerRewards() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        List<HeartContainerRewardEvent> rewards =
                dependencies.entityRuntimeState().runtime.consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomStoryState().get().markHeartContainerCollected();
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingHeartContainerRewards, rewards);
    }

    void harvestSwordPickupRewards() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        List<SwordPickupRewardEvent> rewards =
                dependencies.entityRuntimeState().runtime.consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomStoryState().get().markActiveRoomCompleted();
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingSwordPickupRewards, rewards);
    }

    void harvestToadstoolRewards() {
        if (dependencies.entityRuntimeState().runtime != null) {
            RoomEntityOutbox.append(
                    dependencies.entityRuntimeState().outbox.pendingToadstoolRewards,
                    dependencies.entityRuntimeState().runtime.consumePendingToadstoolRewards());
        }
    }

    void harvestInstrumentRewards() {
        if (dependencies.entityRuntimeState().runtime == null
                || dependencies.location().activeRoom == null
                || dependencies
                        .entityRuntimeState()
                        .runtime
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
                .entityRuntimeState()
                .runtime
                .configuration()
                .setEntityRoomStatus(dependencies.roomSessionViews().get().activeRoomStatusFlags());
    }

    void harvestInstrumentCompletions() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        var instrumentCompletions = dependencies.entityRuntimeState().runtime.consumePendingInstrumentCompletions();
        for (int completionIndex = 0; completionIndex < instrumentCompletions.size(); completionIndex++) {
            InstrumentCompletionEvent completion = instrumentCompletions.get(completionIndex);

            dependencies.items().pendingInstrumentTransition = true;
            if (completion.mapId() == 0x00) {
                dependencies.followers().bowWowState = 0x80;
            } else if (completion.mapId() == 0x01) {
                dependencies.followers().tarinFlag = 0x02;
            }
        }
    }

    void harvestRichardProgressEvents() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        List<RichardProgressEvent> events =
                dependencies.entityRuntimeState().runtime.consumePendingRichardProgressEvents();
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            RichardProgressEvent event = events.get(eventIndex);

            if (event.kind() == RichardProgressEvent.Kind.START_QUEST) {
                dependencies.roomState().indoorBRoomStatus[0xC7] |= (byte) ROOM_STATUS_EVENT_1;
                dependencies.followers().richardSpokenFlag = Math.max(dependencies.followers().richardSpokenFlag, 0x02);
            } else {
                dependencies.entityInputs().get().entityGoldenLeavesCount = event.goldenLeavesCount();
            }
        }
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingRichardProgressEvents, events);
    }

    void harvestWitchEvents() {
        if (dependencies.entityRuntimeState().runtime != null) {
            RoomEntityOutbox.append(
                    dependencies.entityRuntimeState().outbox.pendingWitchExchangeEvents,
                    dependencies.entityRuntimeState().runtime.consumePendingWitchExchangeEvents());
            RoomEntityOutbox.append(
                    dependencies.entityRuntimeState().outbox.pendingWitchRewardEvents,
                    dependencies.entityRuntimeState().runtime.consumePendingWitchRewardEvents());
        }
    }

    void harvestOwlEventCompletions() {
        if (dependencies.entityRuntimeState().runtime == null || dependencies.location().activeRoom == null) {
            return;
        }
        if (dependencies
                .entityRuntimeState()
                .runtime
                .consumePendingOwlEventCompletions()
                .isEmpty()) {
            return;
        }
        dependencies.roomState()
                .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
    }

    void harvestSlimeKeyRewardEvents() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        List<SlimeKeyRewardEvent> rewards =
                dependencies.entityRuntimeState().runtime.consumePendingSlimeKeyRewardEvents();
        for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
            SlimeKeyRewardEvent reward = rewards.get(rewardIndex);

            dependencies.entityInputs().get().entityGoldenLeavesCount = reward.goldenLeavesCount();
            dependencies.roomStoryState().get().markActiveRoomCompleted();
        }
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingSlimeKeyRewardEvents, rewards);
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomEntityFrameState entityRuntimeState,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState) {
        Dependencies {
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
        }
    }
}
