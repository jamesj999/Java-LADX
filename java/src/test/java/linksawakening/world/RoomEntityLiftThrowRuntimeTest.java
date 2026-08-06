package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityLiftThrowRuntimeTest {

    @Test
    void liftedStatusUsesTheLinkAnchorAndPublishesTheRomCarryState() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.ACTIVE);

        assertTrue(runtime.beginLift(0, LiftedEntityMotion.ROM_DIRECTION_RIGHT));
        runtime.tick(0, 0x50, 0x60, () -> 0, null, null, 0x04,
            3, 0);

        RoomEntity lifted = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertEquals(0x60, lifted.x());
        assertEquals(0x60, lifted.y());
        assertEquals(0x04, lifted.z());
        assertEquals(1, runtime.liftedEntityState().phase());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
    }

    @Test
    void fullyHeldEntityCanHandOffToThrownMotion() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.ACTIVE);
        assertTrue(runtime.beginLift(0, LiftedEntityMotion.ROM_DIRECTION_RIGHT));

        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0x00,
                3, 0);
        }
        assertEquals(0x01, runtime.liftedEntityState().carryState());

        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        assertEquals(EntityStatus.THROWN, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.liftedEntityState().carryState());

        runtime.tick(frame, 0, 0, () -> 0);
        RoomEntity thrown = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.THROWN, thrown.status());
        assertEquals(0x53, thrown.x());
        assertEquals(0x0E, thrown.z());
    }

    @Test
    void stunnedLiftableEntityUsesTheHeldPowerBraceletButtonGate() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.STUNNED);
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x20, 0x30, () -> 0, null, null, 0x00, 3, 0);

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
    }

    private static RoomEntityRuntime runtimeWithEntity(EntityStatus status) {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
            0x05, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22))));
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0x05, 0x20, 0x30, status, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots));
    }
}
