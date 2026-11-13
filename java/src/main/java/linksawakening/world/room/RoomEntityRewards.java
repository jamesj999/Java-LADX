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

    void harvestKeyQuicksandEvents(RoomRuntime loaded) {
        if (loaded.entities.consumePendingKeyQuicksandEvents().isEmpty()) {
            return;
        }
        dependencies.roomState().overworldRoomStatus[0xCE] |= (byte) OW_ROOM_STATUS_FLAG_CHANGED;
        dependencies.roomState().indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestKeyRewardEvents(RoomRuntime loaded) {

        List<KeyRewardEvent> rewards = loaded.entities.consumePendingKeyRewardEvents();
        for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
            KeyRewardEvent reward = rewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                dependencies.roomState().dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
            dependencies.roomState().markCompleted(loaded.room);
        }
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingKeyRewardEvents, rewards);
    }

    void harvestHeartContainerRewards(RoomRuntime loaded) {

        List<HeartContainerRewardEvent> rewards = loaded.entities.consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomStoryState().markHeartContainerCollected(loaded);
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingHeartContainerRewards, rewards);
    }

    void harvestSwordPickupRewards(RoomRuntime loaded) {

        List<SwordPickupRewardEvent> rewards = loaded.entities.consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomState().markCompleted(loaded.room);
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingSwordPickupRewards, rewards);
    }

    void harvestToadstoolRewards(RoomRuntime loaded) {

        RoomEntityOutbox.append(
                dependencies.entityRuntimeState().outbox.pendingToadstoolRewards,
                loaded.entities.consumePendingToadstoolRewards());
    }

    void harvestInstrumentRewards(RoomRuntime loaded) {
        if (loaded.entities.consumePendingInstrumentRewards().isEmpty()
                || loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        dependencies.roomSessionViews().indoorStatusTableForMap(loaded.room.mapId())[loaded.room.roomId()] |=
                (byte) ROOM_STATUS_EVENT_1;
        if (loaded.room.mapId() >= 0 && loaded.room.mapId() < dependencies.roomState().dungeonProgressFlags.length) {
            dependencies.roomState().dungeonProgressFlags[loaded.room.mapId()] |= 0x02;
        }
        loaded.entities
                .configuration()
                .setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags(loaded.room));
    }

    void harvestInstrumentCompletions(RoomRuntime loaded) {

        var instrumentCompletions = loaded.entities.consumePendingInstrumentCompletions();
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

    void harvestRichardProgressEvents(RoomRuntime loaded) {

        List<RichardProgressEvent> events = loaded.entities.consumePendingRichardProgressEvents();
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            RichardProgressEvent event = events.get(eventIndex);

            if (event.kind() == RichardProgressEvent.Kind.START_QUEST) {
                dependencies.roomState().indoorBRoomStatus[0xC7] |= (byte) ROOM_STATUS_EVENT_1;
                dependencies.followers().richardSpokenFlag = Math.max(dependencies.followers().richardSpokenFlag, 0x02);
            } else {
                dependencies.entityInputs().entityGoldenLeavesCount = event.goldenLeavesCount();
            }
        }
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingRichardProgressEvents, events);
    }

    void harvestWitchEvents(RoomRuntime loaded) {

        RoomEntityOutbox.append(
                dependencies.entityRuntimeState().outbox.pendingWitchExchangeEvents,
                loaded.entities.consumePendingWitchExchangeEvents());
        RoomEntityOutbox.append(
                dependencies.entityRuntimeState().outbox.pendingWitchRewardEvents,
                loaded.entities.consumePendingWitchRewardEvents());
    }

    void harvestOwlEventCompletions(RoomRuntime loaded) {

        if (loaded.entities.consumePendingOwlEventCompletions().isEmpty()) {
            return;
        }
        dependencies.roomState().overworldRoomStatus[loaded.room.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
    }

    void harvestSlimeKeyRewardEvents(RoomRuntime loaded) {

        List<SlimeKeyRewardEvent> rewards = loaded.entities.consumePendingSlimeKeyRewardEvents();
        for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
            SlimeKeyRewardEvent reward = rewards.get(rewardIndex);

            dependencies.entityInputs().entityGoldenLeavesCount = reward.goldenLeavesCount();
            dependencies.roomState().markCompleted(loaded.room);
        }
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingSlimeKeyRewardEvents, rewards);
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomEntitySessionState entityRuntimeState,
            RoomEntityInputs entityInputs,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomStoryState roomStoryState) {
        Dependencies {
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
        }
    }
}
