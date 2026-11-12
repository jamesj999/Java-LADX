package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeFollowersTest extends EntityRuntimeTestSupport {

    @Test
    void dynamicGhostRuntimeAdvancesZBobEvenWhenItsVariantIsUnchanged() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom()).forFollowerEntityType(0xD4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .type(0xD4)
                .at(0x40, 0x40)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 1, false, false, 0, 0, false));

        for (int frame = 0; frame <= 8; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x30, () -> 0));
        }

        RoomEntity ghost = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x0D, ghost.z());
        assertEquals(4, ghost.spriteVariant());
    }

    @Test
    void dynamicMarinRuntimeConsumesTheSharedLinkHistory() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom()).forFollowerEntityType(0xC1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .type(0xC1)
                .at(0x10, 0x20)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false));
        LinkPositionHistory history = new LinkPositionHistory();
        history.fill(0x10, 0x20, 0x01, 0x02);
        history.write(1, 0x30, 0x40, 0x03, 0x01);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x60, () -> 0, null, history, 0x04, 0x00, 0x00));

        RoomEntity marin = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x30, marin.x());
        assertEquals(0x40, marin.y());
        assertEquals(0x03, marin.z());
    }
}
