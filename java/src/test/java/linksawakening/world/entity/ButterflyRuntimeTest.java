package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.butterflyDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class ButterflyRuntimeTest {

    @Test
    void staggersButterflyWingVariantsByEntitySlot() {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
                0x6E,
                0x06,
                0x6BBD,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
        RoomEntitySnapshot initial = snapshot(
                new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x6E)
                        .at(24, 32)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build(),
                new RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x6E)
                        .at(40, 32)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));

        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(1).spriteVariant());
    }

    @Test
    void butterflyUsesBankSixSignedFixedPointPositionUpdates() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6E)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // State 0 selects +4 X speed; state 16 selects -4 Y speed.
        IntSupplier randomBytes = sequence(0x00, 0x01);
        for (int frame = 0; frame <= 17; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }

        RoomEntity butterfly = runtime.inspection().snapshot().slots().get(0);
        assertEquals(28, butterfly.x());
        assertEquals(31, butterfly.y());
    }

    @Test
    void butterflyAppliesTheTwoPixelVectorTowardLinkOnItsSixtyFourFramePhase() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6E)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // The initial speed is zero. At state 0 the handler chooses a speed,
        // then writes the vector toward Link into its private state. The
        // subsequent 32-frame speed selection adds that private X component.
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00);
        runtime.tick(EntityTestFrames.frame(runtime, 64, 40, 32, randomBytes));
        for (int frame = 65; frame <= 99; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 40, 32, randomBytes));
        }

        // The attraction vector is stored in private state at frame 64. The
        // frame-96 speed refresh therefore selects 4 + 2 = 6, which produces
        // its first pixel one frame earlier than a plain +4 speed.
        RoomEntity butterfly = runtime.inspection().snapshot().slots().get(0);
        assertEquals(33, butterfly.x());
        assertEquals(36, butterfly.y());
    }

    @Test
    void butterflyUsesTheRomRatioForDiagonalAttractionVectors() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // GetVectorTowardsLink(2) turns dx=16, dy=8 into (2,1), not (2,2).
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 72, sequence(0x00, 0x00)));

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).butterflyPrivateStateX(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).butterflyPrivateStateY(0));
    }
}
