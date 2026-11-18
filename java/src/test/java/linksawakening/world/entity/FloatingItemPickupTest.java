package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class FloatingItemPickupTest {

    @Test
    void floatingItemsAnimateRomZOnTheFirstAndSubsequentActiveFramesWithoutMovingXY() {
        EntitySpriteDefinition definition = pairDefinition(0x86, 7);
        RoomEntity initialEntity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x86)
                .at(24, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .z(0x13)
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(initialEntity)).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        RoomEntity first = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, first.status());
        assertEquals(0x0F, first.z());
        assertEquals(24, first.x());
        assertEquals(64, first.y());

        runtime.tick(EntityTestFrames.frame(runtime, 24));
        assertEquals(0x11, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void floatingCollectionUsesVisualYPersistenceAndSourceVariantData() {
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x86)
                .at(24, 64)
                .spriteDefinition(pairDefinition(0x86, 7))
                .z(0x13)
                .build();
        RoomEntityRuntime belowLinkZ = new RoomEntityRuntimeBuilder(snapshot(entity)).build();
        assertNull(belowLinkZ.pickups().collectIfNeeded(1, 24, 47, true, true, 0, 0x0B));

        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(entity)).build();
        EntityPickupEvent pickup = runtime.pickups().collectIfNeeded(1, 24, 47, true, true, 0, 0x0C);

        assertNotNull(pickup);
        assertEquals(0x86, pickup.type());
        assertEquals(0, pickup.sourceVariant());
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void floatingCollectionAllowsAirborneLinkInSideScrollingRoomsWithoutZGate() {
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE5)
                .at(24, 64)
                .spriteDefinition(pairDefinition(0xE5, 7))
                .spriteVariant(5)
                .z(0x13)
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(entity)).build();
        runtime.configuration().setGroundInteractionSideScrolling(true);

        EntityPickupEvent pickup = runtime.pickups().collectIfNeeded(0, 24, 47, true, true, 0, 0x00);

        assertNotNull(pickup);
        assertEquals(0xE5, pickup.type());
        assertEquals(5, pickup.sourceVariant());
    }
}
