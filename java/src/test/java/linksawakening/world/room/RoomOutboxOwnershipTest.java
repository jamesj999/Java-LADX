package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.world.entity.event.*;
import org.junit.jupiter.api.Test;

class RoomOutboxOwnershipTest {
    @Test
    void combatResultsRemainImmutableAndIndependentOfLaterDrains() {
        var box = new RoomEntityOutbox(() -> null);
        var event = new EntityCombatEvent(0, 1, 0, false, EntityCombatEvent.SoundChannel.NONE, -1);
        box.pendingRoomEntityEvents.add(event);
        var first = box.consumeEntityEvents();
        assertEquals(List.of(event), first);
        assertTrue(box.consumeEntityEvents().isEmpty());
        box.pendingRoomEntityEvents.add(event);
        box.consumeEntityEvents();
        assertEquals(List.of(event), first);
        assertThrows(UnsupportedOperationException.class, first::clear);
    }

    @Test
    void dialogResultsRemainImmutableAndIndependentOfLaterDrains() {
        var box = new RoomEntityOutbox(() -> null);
        var event = new DialogRequest(0, 1);
        box.pendingRoomDialogRequests.add(event);
        var first = box.consumeEntityDialogRequests();
        assertEquals(List.of(event), first);
        assertTrue(box.consumeEntityDialogRequests().isEmpty());
        box.pendingRoomDialogRequests.add(new DialogRequest(0, 2));
        box.consumeEntityDialogRequests();
        assertEquals(List.of(event), first);
        assertThrows(UnsupportedOperationException.class, first::clear);
    }

    @Test
    void emptyDrainsKeepAllChannelsEmpty() {
        var box = new RoomEntityOutbox(() -> null);
        assertEquals(List.of(), box.consumeEntityEvents());
        assertEquals(List.of(), box.consumeEntityDialogRequests());
        assertEquals(List.of(), box.consumeHeartContainerRewards());
    }

    @Test
    void scopedRewardDrainClearsTheQueueBeforeReadOnlyCallbacksAndKeepsNewlyQueuedRewards() {
        var box = new RoomEntityOutbox(() -> null);
        var first = new ChestRewardEvent(0, 1);
        var next = new ChestRewardEvent(1, 2);
        box.pendingChestRewardEvents.add(first);
        box.drainChestRewardEvents(events -> {
            assertEquals(List.of(first), events);
            assertTrue(box.consumeChestRewardEvents().isEmpty());
            assertThrows(UnsupportedOperationException.class, events::clear);
            box.pendingChestRewardEvents.add(next);
        });
        assertEquals(List.of(next), box.consumeChestRewardEvents());
    }

    @Test
    void throwingDuringAFrameDrainStillConsumesTheOriginalBatch() {
        var box = new RoomEntityOutbox(() -> null);
        box.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(0));
        var failure = new IllegalStateException("consumer");
        assertSame(
                failure,
                assertThrows(
                        IllegalStateException.class,
                        () -> box.drainHeartContainerRewards(events -> {
                            throw failure;
                        })));
        assertTrue(box.consumeHeartContainerRewards().isEmpty());
        box.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(1));
        box.drainHeartContainerRewards(events -> assertEquals(1, events.size()));
    }

    @Test
    void firstDialogRequestDrainStillConsumesEveryQueuedDialog() {
        var box = new RoomEntityOutbox(() -> null);
        var first = new DialogRequest(0, 1);
        box.pendingRoomDialogRequests.add(first);
        box.pendingRoomDialogRequests.add(new DialogRequest(0, 2));
        assertEquals(first, box.consumeFirstEntityDialogRequest());
        assertTrue(box.consumeEntityDialogRequests().isEmpty());
        assertNull(box.consumeFirstEntityDialogRequest());
    }
}
