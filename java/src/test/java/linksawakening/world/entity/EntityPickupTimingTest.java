package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityPickupTimingTest {

    @Test
    void collectionUsesTheRomPickableTableAndFrameSlotCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2D)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));

        assertNull(runtime.pickups().collectIfNeeded(0, 24, 34, false, true));
        EntityPickupEvent pickup = runtime.pickups().collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(0, pickup.slot());
        assertEquals(0x2D, pickup.type());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void collectionSkipsAirborneAndNonInteractiveLinkStates() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2D)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());

        assertNull(new RoomEntityRuntimeBuilder(initial).build().pickups().collectIfNeeded(1, 24, 34, true, true));
        assertNull(new RoomEntityRuntimeBuilder(initial).build().pickups().collectIfNeeded(1, 24, 34, false, false));
    }

    @Test
    void outdoorHeartUsesTheRomObjectUnderEntityRevealPath() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2D)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x50));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));
        assertEquals(0x02, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState4(0));
        assertEquals(0x0A, runtime.execution().slotProperties().options1(0));
        assertEquals(0x18, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(0));
        assertEquals(0x0C, runtime.inspection().dropSpeedX(0));
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(0));
        assertEquals(0x20, runtime.inspection().dropSpeedZ(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void outdoorFairyUsesTheSharedRevealVectorBeforeItsHoverHandler() {
        EntitySpriteDefinition definition = pairDefinition(0x2F, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2F)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));
        assertEquals(0x01, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));

        EntityRuntimeFixture.debug(runtime).setDroppablePegasusCollisionForTest(true, true, 0x48, 0x58);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x0A, runtime.execution().slotProperties().options1(0));
        // $629C returns past the fairy handler on the reveal frame too.
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).fairySpeedX(0));
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).fairySpeedY(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }
}
