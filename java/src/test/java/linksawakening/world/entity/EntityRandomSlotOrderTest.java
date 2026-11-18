package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.keeseDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRandomSlotOrderTest {

    @Test
    void entityHandlersConsumeSharedRandomBytesInRomReverseSlotOrder() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
                new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x19)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build(),
                new RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x19)
                        .at(64, 80)
                        .spriteDefinition(definition)
                        .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // AnimateEntities starts at MAX_ENTITIES - 1 and decrements. Slot 1
        // therefore consumes the first wake timer byte.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 72, 72, sequence(0x00, 0x3F)));

        assertEquals(0x8F, runtime.flyingEnemies().keeseTransitionCountdown(0));
        assertEquals(0x50, runtime.flyingEnemies().keeseTransitionCountdown(1));
    }
}
