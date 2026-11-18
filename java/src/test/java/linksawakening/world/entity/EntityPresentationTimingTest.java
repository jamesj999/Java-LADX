package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

final class EntityPresentationTimingTest {
    @Test
    void roamingPoseTransfersOnNextFrameAndClearsOnSlotReuse() {
        var initial = emptySlots();
        var slots = new EntitySlots(initial);
        var before = new RoomEntityBuilder()
                .slot(0)
                .type(0x09)
                .at(32, 48)
                .status(EntityStatus.ACTIVE)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x09))
                .spriteVariant(0)
                .build();
        var after = EntitySnapshots.withPositionAndVariant(before, 34, 48, 1);
        slots.replace(0, before);

        slots.transferRoamingOam();
        slots.captureRoamingOam(before);
        slots.replace(0, after);
        assertNull(slots.roamingDisplayPose(0));

        slots.transferRoamingOam();
        var poses = new RoomEntity[RoomEntity.SLOT_COUNT];
        poses[0] = slots.roamingDisplayPose(0);
        var snapshot = new RoomEntitySnapshot(
                slots.view(),
                null,
                null,
                false,
                List.of(),
                List.of(),
                List.of(),
                new int[RoomEntity.SLOT_COUNT],
                List.of(),
                1,
                poses);
        var copied = snapshot.withVisualYOffset(0, 2);
        assertSame(after, copied.slots().getFirst());
        assertEquals(32, copied.displayPose(after).x());
        assertEquals(0, copied.displayPose(after).spriteVariant());
        assertTrue(copied.inShallowWater(0));
        assertFalse(copied.inShallowWater(1));

        slots.clear(0);
        assertNull(slots.roamingDisplayPose(0));
        assertEquals(32, copied.displayPose(after).x());
    }

    private static List<RoomEntity> emptySlots() {
        return IntStream.range(0, RoomEntity.SLOT_COUNT)
                .mapToObj(RoomEntity::disabled)
                .toList();
    }
}
