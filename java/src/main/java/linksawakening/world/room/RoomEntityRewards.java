package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.OW_ROOM_STATUS_FLAG_CHANGED;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_2;

import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.transition.Warp;

/** RoomEntityRewards operations for the live room session. */
final class RoomEntityRewards {
    private final Dependencies dependencies;

    RoomEntityRewards(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
    }

    void harvestKeyQuicksandEvents(RoomRuntime loaded) {
        if (loaded.entities.outbox().consumePendingKeyQuicksandEvents().isEmpty()) {
            return;
        }
        dependencies.roomState().overworldRoomStatus[0xCE] |= (byte) OW_ROOM_STATUS_FLAG_CHANGED;
        dependencies.roomState().indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestHeartContainerRewards(RoomRuntime loaded) {

        List<HeartContainerRewardEvent> rewards = loaded.entities.outbox().consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomStoryState().markHeartContainerCollected(loaded);
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingHeartContainerRewards, rewards);
    }

    void harvestSwordPickupRewards(RoomRuntime loaded) {

        List<SwordPickupRewardEvent> rewards = loaded.entities.outbox().consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        dependencies.roomState().markCompleted(loaded.room);
        RoomEntityOutbox.append(dependencies.entityRuntimeState().outbox.pendingSwordPickupRewards, rewards);
    }

    void harvestToadstoolRewards(RoomRuntime loaded) {

        RoomEntityOutbox.append(
                dependencies.entityRuntimeState().outbox.pendingToadstoolRewards,
                loaded.entities.outbox().consumePendingToadstoolRewards());
    }

    void harvestInstrumentRewards(RoomRuntime loaded) {
        if (loaded.entities.outbox().consumePendingInstrumentRewards().isEmpty()
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

        var instrumentCompletions = loaded.entities.outbox().consumePendingInstrumentCompletions();
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

        List<RichardProgressEvent> events = loaded.entities.outbox().consumePendingRichardProgressEvents();
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
                loaded.entities.outbox().consumePendingWitchExchangeEvents());
        RoomEntityOutbox.append(
                dependencies.entityRuntimeState().outbox.pendingWitchRewardEvents,
                loaded.entities.outbox().consumePendingWitchRewardEvents());
    }

    void harvestOwlEventCompletions(RoomRuntime loaded) {

        if (loaded.entities.outbox().consumePendingOwlEventCompletions().isEmpty()) {
            return;
        }
        dependencies.roomState().overworldRoomStatus[loaded.room.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
    }

    void harvestSlimeKeyRewardEvents(RoomRuntime loaded) {

        List<SlimeKeyRewardEvent> rewards = loaded.entities.outbox().consumePendingSlimeKeyRewardEvents();
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
            Objects.requireNonNull(followers, "followers");
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            Objects.requireNonNull(entityInputs, "entityInputs");
            Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            Objects.requireNonNull(roomState, "roomState");
            Objects.requireNonNull(roomStoryState, "roomStoryState");
        }
    }
}
