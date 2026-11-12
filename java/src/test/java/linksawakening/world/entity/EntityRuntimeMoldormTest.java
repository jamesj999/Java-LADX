package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeMoldormTest extends EntityRuntimeTestSupport {

    @Test
    void tailCaveBossesPushLinkInsideTheArenaBeforeBlockingInput() {
        int[][] cases = {
            {8, 72, 1, 0},
            {16, 72, 1, 0},
            {17, 72, 0, 0},
            {142, 72, 0, 0},
            {143, 72, -1, 0},
            {80, 116, 0, -1},
            {80, 115, 0, 0},
            {80, 21, 0, -1},
            {80, 22, 0, 0},
            {8, 116, 1, 0}
        };
        for (int type : new int[] {0x81, EntitySpriteHandlerCatalog.ENTITY_MOLDORM}) {
            for (int[] point : cases) {
                RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(type)
                                .at(0x70, 0x40)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(type))
                                .build()))
                        .indoorRoom(true)
                        .build();
                runtime.tick(EntityTestFrames.frame(runtime, 0, point[0], point[1], () -> 0));
                var requests = runtime.consumePendingLinkMotionBlockRequests();
                if (point[2] == 0 && point[3] == 0) {
                    assertTrue(requests.isEmpty());
                } else {
                    assertEquals(1, requests.size());
                    assertEquals(point[2], requests.get(0).positionDeltaX());
                    assertEquals(point[3], requests.get(0).positionDeltaY());
                }
                runtime.setFreeMovementMode(true);
                runtime.tick(EntityTestFrames.frame(runtime, 1, 8, 72, () -> 0));
                assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
                runtime.setFreeMovementMode(false);
                EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(true);
                runtime.tick(EntityTestFrames.frame(runtime, 2, 8, 72, () -> 0));
                assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
            }
        }
    }

    @Test
    void moldormContactUsesTheLargerLinkRecoilMagnitude() {
        var enemy = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x59)
                .at(0x40, 0x50)
                .spriteDefinition(pairDefinition(0x59, 2))
                .build();
        var runtime = RoomEntityRuntime.builder(snapshot(enemy)).build();
        var events = runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x58, false, true, false, 0, 0, 0, 0));
        assertEquals(1, events.size());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(0, 0x18, 0x10),
                events.get(0).linkCollisionResponse());
    }

    @Test
    void miniMoldormUsesDelayedRomPositionHistoryForItsDynamicTail() throws Exception {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_MINI_MOLDORM, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntity initialEntity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(EntitySpriteHandlerCatalog.ENTITY_MINI_MOLDORM)
                .at(0x50, 0x60)
                .spriteDefinition(definition)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(initialEntity))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> false));

        RoomEntity updated = runtime.inspection().snapshot().slots().get(0);
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC, updated.spriteDefinition().shape());
        assertEquals(0, updated.spriteVariant());
        assertEquals(6, updated.spriteDefinition().dynamicVariant(0).size());
        assertEquals(
                0x74, updated.spriteDefinition().dynamicVariant(0).get(0).oam().tile());
        assertEquals(-0x50, updated.spriteDefinition().dynamicVariant(0).get(2).xOffset());
        assertEquals(-0x60, updated.spriteDefinition().dynamicVariant(0).get(2).yOffset());
        assertEquals(-0x50, updated.spriteDefinition().dynamicVariant(0).get(4).xOffset());
        assertEquals(-0x60, updated.spriteDefinition().dynamicVariant(0).get(4).yOffset());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> false));
        assertEquals(0x51, runtime.inspection().snapshot().slots().get(0).x());
    }
}
