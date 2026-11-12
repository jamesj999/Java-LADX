package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

final class MarinWakeUpEntityRuntimeTest {
    @Test
    void outdoorMarinWaitsForSwordButIndoorMarinRemainsVisibleBeforeFirstTick() {
        var variant = new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x40, 0), new EntitySpriteDefinition.OamAttribute(0x42, 0));
        var definition = new EntitySpriteDefinition(
                0x3E, 5, 0x4E0A, EntitySpriteDefinition.Shape.PAIR, 6, Collections.nCopies(8, variant));
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        slots.set(0, new RoomEntity(0, 0, 0x3E, 80, 56, EntityStatus.INIT, definition, 6, 0, 0, 0, -1, false));
        for (boolean indoor : new boolean[] {false, true}) {
            var runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots), indoor);
            runtime.configuration().setChestPlayerLevels(0, 0, 0);
            assertEquals(
                    indoor ? 6 : -1,
                    runtime.inspection().snapshot().slots().get(0).spriteVariant());
            runtime.configuration().setChestPlayerLevels(0, 1, 0);
            assertEquals(
                    indoor ? -1 : 6,
                    runtime.inspection().snapshot().slots().get(0).spriteVariant());
        }
    }

    @Test
    void houseStoryGatesRunBeforeRenderingAndAllowTarinAfterTransformation() {
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        var runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots), true);
        runtime.configuration().setChestPlayerLevels(1, 1, 0);
        org.junit.jupiter.api.Assertions.assertTrue(runtime.inspection().indoorMarinAbsent());
        org.junit.jupiter.api.Assertions.assertTrue(runtime.inspection().indoorTarinAbsent());
        runtime.configuration().setHouseNpcStoryState(1, 0, 0, 0);
        org.junit.jupiter.api.Assertions.assertFalse(runtime.inspection().indoorTarinAbsent());
        runtime.configuration().setHouseNpcStoryState(1, 4, 0, 0);
        org.junit.jupiter.api.Assertions.assertTrue(runtime.inspection().indoorTarinAbsent());
        runtime.configuration().setHouseNpcStoryState(1, 0, 2, 0);
        org.junit.jupiter.api.Assertions.assertTrue(runtime.inspection().indoorTarinAbsent());
        runtime.configuration().setHouseNpcStoryState(1, 7, 0, 0);
        org.junit.jupiter.api.Assertions.assertFalse(runtime.inspection().indoorMarinAbsent()); // letter path
        runtime.configuration().setHouseNpcStoryState(1, 7, 0, 0x10);
        org.junit.jupiter.api.Assertions.assertTrue(runtime.inspection().indoorMarinAbsent());
    }

    @Test
    void appliesControllerPositionAndFacingToIndoorMarin() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        EntitySpriteDefinition.Variant variant = new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x40, 0), new EntitySpriteDefinition.OamAttribute(0x42, 0));
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
                0x3E, 5, 0x4E0A, EntitySpriteDefinition.Shape.PAIR, 6, Collections.nCopies(8, variant));
        slots.set(3, new RoomEntity(3, 0, 0x3E, 0x58, 0x60, EntityStatus.ACTIVE, definition, 6, 0, 0, 0, -1, false));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots), true);

        runtime.applyMarinWakeUpPresentation(0x50, 0x58, 4);

        RoomEntity marin = runtime.inspection().snapshot().slots().get(3);
        assertEquals(0x50, marin.x());
        assertEquals(0x58, marin.y());
        assertEquals(4, marin.spriteVariant());
    }
}
