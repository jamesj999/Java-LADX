package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteSelection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FollowingNpcEntitySpawnerTest {

    @Test
    void marinDisablesExistingEntitySpawnsInLastSlotAndSeedsHistory() {
        EntitySpriteSelection selection = selection();
        EntitySpriteDefinition atHome = EntitySpriteDefinition.unsupported(0xC1);
        List<RoomEntity> slots = disabledSlots();
        slots.set(0, active(0, 0xC1, atHome));
        slots.set(2, active(2, 0x70, EntitySpriteDefinition.unsupported(0x70)));
        RoomEntitySnapshot initial = new RoomEntitySnapshot(slots, selection);
        LinkPositionHistory history = new LinkPositionHistory();

        FollowingNpcEntitySpawner.Result result = new FollowingNpcEntitySpawner(
            new EntitySpriteHandlerCatalog(syntheticRom()))
            .synchronize(initial,
                new FollowingNpcRoomContext(false, false, 0x00, 0x20),
                new FollowingNpcState(false, 0, true, false, 0, 0, false),
                0x58, 0x60, 0x02, 0x04, 0x03, history);

        RoomEntity spawned = result.snapshot().slots().get(15);
        assertEquals(EntityStatus.DISABLED, result.snapshot().slots().get(0).status());
        assertEquals(0xC1, spawned.type());
        assertEquals(-1, spawned.sourceLoadOrder());
        assertEquals(EntityStatus.ACTIVE, spawned.status());
        assertEquals(0x58, spawned.x());
        assertEquals(0x64, spawned.y());
        assertEquals(0x18, spawned.spriteDefinition().bank());
        assertEquals(0x59B8, spawned.spriteDefinition().address());
        assertEquals(0x59B8,
            result.snapshot().spriteSelection().spriteOverrideFor(0xC1).address());
        assertEquals(0x58, history.xAt(0));
        assertEquals(0x64, history.yAt(15));
        assertEquals(0x02, history.zAt(7));
        assertEquals(0x03, history.directionAt(3));
        assertEquals(0x58, history.xAt(15));
    }

    @Test
    void runtimeSnapshotPreservesSideScrollingThroughFollowerSynchronization() {
        RoomEntitySnapshot initial = new RoomEntitySnapshot(disabledSlots(), selection())
            .withSideScrolling(true);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        FollowingNpcEntitySpawner.Result result = new FollowingNpcEntitySpawner(
            new EntitySpriteHandlerCatalog(syntheticRom()))
            .synchronize(runtime.snapshot(),
                new FollowingNpcRoomContext(false, false, 0x00, 0x20),
                new FollowingNpcState(true, 0, false, false, 0, 0, false),
                0x58, 0x60, 0x00, 0x00, 0x00, new LinkPositionHistory());

        assertTrue(runtime.snapshot().sideScrolling());
        assertTrue(result.snapshot().sideScrolling());
    }

    @Test
    void sourceOrderUsesHighestFreeSlotForRoosterGhostMarinThenBowWow() {
        FollowingNpcEntitySpawner.Result result = new FollowingNpcEntitySpawner(
            new EntitySpriteHandlerCatalog(syntheticRom()))
            .synchronize(new RoomEntitySnapshot(disabledSlots(), selection()),
                new FollowingNpcRoomContext(false, false, 0x00, 0x20),
                new FollowingNpcState(true, 1, true, true, 0, 0, false),
                0x50, 0x60, 0, 0, 0, new LinkPositionHistory());

        assertEquals(0xD5, result.snapshot().slots().get(15).type());
        assertEquals(0xD4, result.snapshot().slots().get(14).type());
        assertEquals(0xC1, result.snapshot().slots().get(13).type());
        assertEquals(0x6D, result.snapshot().slots().get(12).type());
        assertTrue(result.snapshot().slots().get(15).spriteDefinition().supported());
        assertEquals(0x59BC, result.snapshot().spriteSelection()
            .spriteOverrideFor(0xD5).address());
    }

    @Test
    void ghostTriggerStateIsUpdatedWithoutSpawningDuringTheCheckPass() {
        FollowingNpcEntitySpawner.Result result = new FollowingNpcEntitySpawner(
            new EntitySpriteHandlerCatalog(syntheticRom()))
            .synchronize(new RoomEntitySnapshot(disabledSlots(), selection()),
                new FollowingNpcRoomContext(false, false, 0x00, 0x40),
                new FollowingNpcState(false, 2, false, false, 0x02, 1, false),
                0x50, 0x60, 0, 0, 0, new LinkPositionHistory());

        assertEquals(1, result.state().ghostFollowingState());
        assertTrue(result.snapshot().loadedEntities().isEmpty());
    }

    @Test
    void excludedIndoorRoomLeavesEntitiesAndHistoryUntouched() {
        RoomEntity existing = active(0, 0x70, EntitySpriteDefinition.unsupported(0x70));
        List<RoomEntity> slots = disabledSlots();
        slots.set(0, existing);
        RoomEntitySnapshot initial = new RoomEntitySnapshot(slots, selection());
        LinkPositionHistory history = new LinkPositionHistory();

        FollowingNpcEntitySpawner.Result result = new FollowingNpcEntitySpawner(
            new EntitySpriteHandlerCatalog(syntheticRom()))
            .synchronize(initial,
                new FollowingNpcRoomContext(true, false, 0x09, 0x20),
                new FollowingNpcState(true, 1, true, true, 0, 0, false),
                0x50, 0x60, 0, 0, 0, history);

        assertSameEntity(existing, result.snapshot().slots().get(0));
        assertEquals(EntityStatus.DISABLED, result.snapshot().slots().get(15).status());
        assertEquals(0, history.xAt(0));
        assertFalse(result.changed());
    }

    private static EntitySpriteSelection selection() {
        return new EntitySpriteSelection(EntityRoomLoader.RoomTable.OVERWORLD, 0x20, 0,
            new int[0], false, new int[][] {{0, 0, 0, 0}});
    }

    private static List<RoomEntity> disabledSlots() {
        List<RoomEntity> slots = new ArrayList<>(EntityRoomLoader.MAX_ENTITIES);
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return slots;
    }

    private static RoomEntity active(int slot, int type, EntitySpriteDefinition definition) {
        return new RoomEntity(slot, slot, type, 0x20, 0x30, EntityStatus.ACTIVE,
            definition, -1);
    }

    private static void assertSameEntity(RoomEntity expected, RoomEntity actual) {
        assertEquals(expected.slot(), actual.slot());
        assertEquals(expected.sourceLoadOrder(), actual.sourceLoadOrder());
        assertEquals(expected.type(), actual.type());
        assertEquals(expected.x(), actual.x());
        assertEquals(expected.y(), actual.y());
        assertEquals(expected.status(), actual.status());
    }

    private static byte[] syntheticRom() {
        return new byte[0x100000];
    }
}
