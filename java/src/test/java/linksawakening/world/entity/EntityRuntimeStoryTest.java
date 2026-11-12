package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeStoryTest extends EntityRuntimeTestSupport {

    @Test
    void kidHandlersAnimateTheirTwoWalkingFramesEverySixteenFrames() {
        EntitySpriteDefinition definition = pairDefinition(0x70, 4);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x70)
                .at(24, 32)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 8));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 16));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 32));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }
}
