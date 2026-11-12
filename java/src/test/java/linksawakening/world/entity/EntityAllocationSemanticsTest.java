package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import org.junit.jupiter.api.Test;

/** Ownership and nested-call contracts for the allocation-free inspection/scratch paths. */
final class EntityAllocationSemanticsTest {
    @Test
    void liveViewTracksSlotsWhileOwnedSnapshotRetainsItsFrame() {
        RoomEntityRuntime runtime = runtime();
        RoomEntitySnapshot owned = runtime.inspection().snapshot();
        RoomEntitySnapshot view = runtime.inspection().view();
        assertSame(view, runtime.inspection().view());
        runtime.slots[0] = EntitySnapshots.withPositionAndVariant(runtime.slots[0], 80, 88, 1);
        assertEquals(64, owned.slots().getFirst().x());
        assertEquals(80, view.slots().getFirst().x());
        assertThrows(UnsupportedOperationException.class, () -> view.slots().set(0, RoomEntity.disabled(0)));
        runtime.slots[0] = RoomEntity.disabled(0);
        assertTrue(view.loadedEntities().isEmpty());
        assertEquals(1, owned.loadedEntities().size());
    }

    @Test
    void transformingALiveViewProducesAnOwnedResult() {
        RoomEntityRuntime runtime = runtime();
        RoomEntitySnapshot view = runtime.inspection().view();
        RoomEntitySnapshot transformed = view.withVisualYOffset(0, 3);
        runtime.slots[0] = EntitySnapshots.withPositionAndVariant(runtime.slots[0], 80, 88, 1);
        runtime.entityState.physicsTables.writeFallingVisualYOffset(0, 7);
        assertEquals(64, transformed.slots().getFirst().x());
        assertEquals(3, transformed.visualYOffset(0));
        assertEquals(7, view.visualYOffset(0));
    }

    @Test
    void nestedMotionInvocationDoesNotOverwriteTheOuterScratch() {
        RoomEntity first = entity(0, 0x09);
        RoomEntity second = entity(1, 0x0B);
        RoamingEnemyMotion actual = new RoamingEnemyMotion();
        RoamingEnemyMotion expected = new RoamingEnemyMotion();
        RoomEntity expectedOuter = expected.advance(first, 32, 32, () -> 36, null);
        RoomEntity expectedInner = expected.advance(second, 48, 24, () -> 36, null);
        RoomEntity[] nested = new RoomEntity[1];
        RoomEntity actualOuter = actual.advance(
                first,
                32,
                32,
                () -> {
                    if (nested[0] == null) nested[0] = actual.advance(second, 48, 24, () -> 36, null);
                    return 36;
                },
                null);
        assertEquals(expectedOuter, actualOuter);
        assertEquals(expectedInner, nested[0]);
        assertEquals(
                expected.advance(actualOuter, 32, 32, () -> 36, null),
                actual.advance(actualOuter, 32, 32, () -> 36, null));
    }

    @Test
    void retainedMoldormSegmentsRemainImmutableAcrossLaterTicks() {
        MoldormMotion motion = new MoldormMotion();
        RoomEntity entity = entity(0, EntityRomConstants.ENTITY_MOLDORM);
        var first = motion.advance(entity, 0, 0, 0, 0, 5, () -> 36, null);
        var retained = first.segments();
        var values = java.util.List.copyOf(retained);
        for (int frame = 1; frame < 80; frame++) {
            entity = motion.advance(entity, frame, 0, 0, 0, 5, () -> 36, null).entity();
        }
        assertEquals(values, retained);
        assertThrows(UnsupportedOperationException.class, () -> retained.set(0, values.get(1)));
    }

    private static RoomEntityRuntime runtime() {
        RoomEntity[] slots = new RoomEntity[RoomEntity.SLOT_COUNT];
        Arrays.setAll(slots, RoomEntity::disabled);
        slots[0] = entity(0, 0x09);
        return RoomEntityRuntime.builder(new RoomEntitySnapshot(Arrays.asList(slots)))
                .build();
    }

    private static RoomEntity entity(int slot, int type) {
        return new RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(type)
                .at(64, 80)
                .spriteDefinition(EntitySpriteDefinition.unsupported(type))
                .build();
    }
}
