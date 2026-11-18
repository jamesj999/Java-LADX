package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityAnimationTest {

    @Test
    void followsPieceOfPowerFrameDrivenPaletteVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x33)
                .at(24, 32)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0x00));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 0x08));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 0x10));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void simplePairEnemiesUseTheirRomAnimationCadences() {
        RoomEntitySnapshot initial = snapshot(
                new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(24, 32)
                        .spriteDefinition(pairDefinition(0x0F, 2))
                        .build(),
                new RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x12)
                        .at(40, 32)
                        .spriteDefinition(pairDefinition(0x12, 2))
                        .build(),
                new RoomEntityBuilder()
                        .slot(2)
                        .sourceLoadOrder(2)
                        .type(0x20)
                        .at(56, 32)
                        .spriteDefinition(pairDefinition(0x20, 2))
                        .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(1).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(2).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 8));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(1).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(2).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 16));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(1).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(2).spriteVariant());
    }
}
