package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.testing.AllocationProbe;
import linksawakening.world.entity.*;
import org.junit.jupiter.api.Test;

class RoomLiveBoundaryTest {
    @Test
    void retainedRenderSnapshotOwnsTheEntitiesReadFromALiveRoom() {
        var runtime = RoomEntityRuntime.builder(TestEntities.room(
                        TestEntities.active(9).slot(0).at(64, 64).build()))
                .build();
        var live = runtime.inspection().view();
        var render = new RoomRenderSnapshot(new int[] {1}, new int[] {0}, new int[][] {{0, 0, 0, 0}}, live);
        assertEquals(1, render.entities().loadedEntities().size());
        runtime.clearEntity(0);
        assertTrue(live.loadedEntities().isEmpty());
        assertEquals(1, render.entities().loadedEntities().size());
        assertSame(render.entities(), render.entities().ownedCopy());
    }

    @Test
    void emptyRoomAndEntityOutboxDrainsAndMergesAllocateNothing() {
        var runtime = RoomEntityRuntime.builder(TestEntities.room()).build();
        var box = new RoomEntityOutbox(() -> runtime);
        Runnable drain = () -> {
            box.consumeEntityEvents();
            box.consumeEntityDialogRequests();
            box.consumeScreenShakeRequests();
            box.consumeHinoxLinkEffectRequests();
            RoomLinkWrites.positions(
                    box.consumeSideViewPlatformLinkRequests(),
                    box.consumeSideViewPotLinkRequests(),
                    box.consumeLinkFinalPositionRequests());
            RoomLinkWrites.attacks(box.consumeLinkAttackClearRequests(), box.consumeLinkAttackStepRequests());
            box.consumeChestRewardEvents();
            box.consumeHeartContainerRewards();
        };
        assertEquals(0.0, AllocationProbe.bytesPerIteration(4000, 1000, drain));
    }

    @Test
    void publicRoomEntitiesStayOwnedWhileInternalReadsUseTheLiveView() {
        var runtime = RoomEntityRuntime.builder(TestEntities.room(
                        TestEntities.active(0x3E).slot(0).at(64, 64).build()))
                .build();
        var loaded = new LoadedRoom(new LoadedRoomSpec(
                new LoadedRoomSpec.Identity(1, 0, 0, 0),
                new LoadedRoomSpec.Objects(new int[256], null, null),
                new LoadedRoomSpec.Graphics(new int[] {1}, new int[] {0}, new int[][] {{0, 0, 0, 0}}),
                new LoadedRoomSpec.Exits(List.of(), false, 0, -1),
                runtime.inspection().snapshot()));
        var room = ActiveRoom.from(loaded);
        room.replaceEntities(runtime.inspection().view());
        var first = room.entities();
        assertSame(first, room.renderSnapshot().entities());
        runtime.applyMarinWakeUpPresentation(56, 56, 1);
        var second = room.entities();
        assertEquals(1, second.slots().get(0).spriteVariant());
        assertEquals(0, first.slots().get(0).spriteVariant());
        runtime.applyMarinWakeUpPresentation(64, 64, 0);
        assertSame(first, room.entities());
        assertSame(runtime.inspection().view(), room.entityView());
    }
}
