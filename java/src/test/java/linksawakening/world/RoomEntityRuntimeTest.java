package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomEntityRuntimeTest {

    @Test
    void followsPieceOfPowerFrameDrivenPaletteVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x33, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0x00);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x08);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x10);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void staggersButterflyWingVariantsByEntitySlot() {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(0x6E, 0x06, 0x6BBD,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.INIT, definition, 0),
            new RoomEntity(1, 1, 0x6E, 40, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);

        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.snapshot().slots().get(1).spriteVariant());
    }

    @Test
    void onlyTheFirstEightLoadOrdersContributeToThePersistentClearMask() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 3, 0x33, 24, 32, EntityStatus.ACTIVE, definition, 0),
            new RoomEntity(1, 8, 0x33, 40, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        assertEquals(1 << 3, runtime.clearEntity(0));
        assertEquals(0, runtime.clearEntity(1));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(1).status());
    }

    private static EntitySpriteDefinition pairDefinition(int type, int variants) {
        List<EntitySpriteDefinition.Variant> displayList = new ArrayList<>();
        for (int i = 0; i < variants; i++) {
            displayList.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22)));
        }
        return new EntitySpriteDefinition(type, 0x03, 0x5B65,
            EntitySpriteDefinition.Shape.PAIR, 0, displayList);
    }

    private static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>(List.of(entities));
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }
}
