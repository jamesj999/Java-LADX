package linksawakening.world.entity;



import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntitySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MarinWakeUpEntityRuntimeTest {
    @Test
    void appliesControllerPositionAndFacingToIndoorMarin() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        EntitySpriteDefinition.Variant variant = new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x40, 0),
            new EntitySpriteDefinition.OamAttribute(0x42, 0));
        EntitySpriteDefinition definition = new EntitySpriteDefinition(0x3E, 5, 0x4E0A,
            EntitySpriteDefinition.Shape.PAIR, 6, Collections.nCopies(8, variant));
        slots.set(3, new RoomEntity(3, 0, 0x3E, 0x58, 0x60,
            EntityStatus.ACTIVE, definition, 6, 0, 0, 0, -1, false));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots), true);

        runtime.applyMarinWakeUpPresentation(0x50, 0x58, 4);

        RoomEntity marin = runtime.inspection().snapshot().slots().get(3);
        assertEquals(0x50, marin.x());
        assertEquals(0x58, marin.y());
        assertEquals(4, marin.spriteVariant());
    }
}
