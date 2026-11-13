package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import org.junit.jupiter.api.Test;

class RoomOutboxOwnershipTest {
    private static RoomSession unloadedSession() {
        var rom = linksawakening.testing.TestRoms.partial(new byte[0x100000]);
        return new RoomSession(new RoomSessionConfig(
                rom,
                new linksawakening.gpu.GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new linksawakening.physics.OverworldCollision(linksawakening.rom.RomTables.load(rom)),
                new RoomSessionConfig.Presentation(null, null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
    }

    @Test
    void combatResultsRemainImmutableAndIndependentOfLaterDrains() {
        var session = unloadedSession();
        var box = session.components.entityFrame().outbox;
        var event = new EntityCombatEvent(0, 1, 0, false, EntityCombatEvent.SoundChannel.NONE, -1);
        box.pendingRoomEntityEvents.add(event);
        var first = session.consumeEntityEvents();
        assertEquals(List.of(event), first);
        assertTrue(session.consumeEntityEvents().isEmpty());
        box.pendingRoomEntityEvents.add(event);
        session.consumeEntityEvents();
        assertEquals(List.of(event), first);
        assertThrows(UnsupportedOperationException.class, first::clear);
    }

    @Test
    void dialogResultsRemainImmutableAndIndependentOfLaterDrains() {
        var session = unloadedSession();
        var box = session.components.entityFrame().outbox;
        var event = new DialogRequest(0, 1);
        box.pendingRoomDialogRequests.add(event);
        var first = session.consumeEntityDialogRequests();
        assertEquals(List.of(event), first);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        box.pendingRoomDialogRequests.add(new DialogRequest(0, 2));
        session.consumeEntityDialogRequests();
        assertEquals(List.of(event), first);
        assertThrows(UnsupportedOperationException.class, first::clear);
    }

    @Test
    void emptyDrainsKeepAllChannelsEmpty() {
        var session = unloadedSession();
        var box = session.components.entityFrame().outbox;
        assertEquals(List.of(), session.consumeEntityEvents());
        assertEquals(List.of(), session.consumeEntityDialogRequests());
        assertEquals(List.of(), box.consumeHeartContainerRewards());
    }

    @Test
    void scopedRewardDrainClearsTheQueueBeforeReadOnlyCallbacksAndKeepsNewlyQueuedRewards() {
        var session = unloadedSession();
        var box = session.components.entityFrame().outbox;
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
        var session = unloadedSession();
        var box = session.components.entityFrame().outbox;
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
        var session = unloadedSession();
        var box = session.components.entityFrame().outbox;
        var first = new DialogRequest(0, 1);
        box.pendingRoomDialogRequests.add(first);
        box.pendingRoomDialogRequests.add(new DialogRequest(0, 2));
        assertEquals(first, session.consumeFirstEntityDialogRequest());
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        assertNull(session.consumeFirstEntityDialogRequest());
    }
}
