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
    void thrownEntityDamagesOverlappingEnemyAfterBounceMotion() {
        EntitySpriteDefinition potDefinition = definition(0x05);
        EntitySpriteDefinition polsVoiceDefinition = definition(0x18);
        List<RoomEntity> slots = new ArrayList<>();
        int targetSlot = 0;
        slots.add(new RoomEntity(targetSlot, 1, 0x18, 0x53, 0x63,
            EntityStatus.ACTIVE, polsVoiceDefinition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES - 1; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        int thrownSlot = EntityRoomLoader.MAX_ENTITIES - 1;
        slots.add(new RoomEntity(thrownSlot, 0, 0x05, 0x53, 0x60,
            EntityStatus.THROWN, potDefinition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots), false, () -> 0, null,
            new RomEnemyCombatTables(loadRom()));
        int initialHealth = runtime.enemyHealth(targetSlot);

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity thrown = runtime.snapshot().slots().get(thrownSlot);
        assertEquals(0x53, thrown.x());
        assertEquals(0x63, thrown.y());
        assertEquals(0, thrown.z());
        assertEquals(4, initialHealth);
        assertEquals(0, runtime.enemyHealth(targetSlot));
        assertEquals(EntityStatus.DYING,
            runtime.snapshot().slots().get(targetSlot).status());
    }

    @Test
    void stunnedLiftableEntityUsesTheHeldPowerBraceletButtonGate() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.STUNNED);
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x20, 0x30, () -> 0, null, null, 0x00, 3, 0);

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
    }

    @Test
    void activeSideViewPotUsesItsBraceletPickupHandler() {
        EntitySpriteDefinition definition = definition(0xD6);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0xD6, 0x50, 0x60,
            EntityStatus.ACTIVE, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots).withSideScrolling(true));
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.liftedEntityState().slot());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
    }

    @Test
    void activeSideViewPotCannotBeLiftedWithoutTheInitialCollision() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x62, 0x60, () -> 0, null, null, 0, 3, 0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void activeSideViewPotCannotBeLiftedWhileLinkIsAirborneOrNonInteractive() {
        RoomEntityRuntime airborne = sideViewPotRuntime();
        airborne.setPowerBraceletButtonHeld(true);
        airborne.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 1, 0, 0, false));
        assertEquals(EntityStatus.ACTIVE, airborne.snapshot().slots().get(0).status());

        RoomEntityRuntime nonInteractive = sideViewPotRuntime();
        nonInteractive.setPowerBraceletButtonHeld(true);
        nonInteractive.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            EnemyProjectileCollision.LinkState.nonInteractive());
        assertEquals(EntityStatus.ACTIVE,
            nonInteractive.snapshot().slots().get(0).status());
    }

    private static RoomEntityRuntime sideViewPotRuntime() {
        EntitySpriteDefinition definition = definition(0xD6);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0xD6, 0x50, 0x60,
            EntityStatus.ACTIVE, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots).withSideScrolling(true));
    }

    private static RoomEntityRuntime runtimeWithEntity(EntityStatus status) {
        EntitySpriteDefinition definition = definition(0x05);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0x05, 0x20, 0x30, status, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots));
    }

    private static EntitySpriteDefinition definition(int type) {
        return new EntitySpriteDefinition(
            type, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22))));
    }

    private static byte[] loadRom() {
        try (var stream = RoomEntityLiftThrowRuntimeTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
