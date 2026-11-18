package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RichardVillaRuntimeTest {
    @Test
    void frogHopsAndRetainsItsPoseWhileDialogPausesTheHandler() {
        int type = EntityRomConstants.ENTITY_RICHARD_FROG;
        var runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(type)
                        .status(EntityStatus.INIT)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(type, 8))
                        .build()))
                .indoorRoom(true)
                .build();

        var random = sequence(0, 1);
        for (int frame = 0; frame < 4; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, random));
        }
        RoomEntity hopping = runtime.inspection().snapshot().slots().getFirst();
        assertTrue(hopping.x() > 64);
        assertTrue(hopping.z() > 0);
        assertEquals(7, hopping.spriteVariant());

        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 4, 120, 120, random));
        assertEquals(hopping, runtime.inspection().snapshot().slots().getFirst());
    }
}
