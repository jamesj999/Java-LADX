package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PiranhaPlantRuntimeTest {
    private static final int ENTITY_PIRANHA_PLANT = 0xA2;

    @Test
    void decodesPiranhaPlantsRomRectangleDisplayList() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A);

        assertEquals(0x36, definition.bank());
        assertEquals(0x6FC2, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(6, definition.variantCount());
        assertEquals(4, definition.rectangleVariant(0).size());
        assertEquals(new EntitySpriteDefinition.RectangleSprite(-1, -1,
            new EntitySpriteDefinition.OamAttribute(0xFF, 0xFF)),
            definition.rectangleVariant(0).get(0));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(-16, 0,
            new EntitySpriteDefinition.OamAttribute(0x74, 0x02)),
            definition.rectangleVariant(1).get(0));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(16, 0,
            new EntitySpriteDefinition.OamAttribute(0x72, 0x00)),
            definition.rectangleVariant(4).get(2));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(16, 0,
            new EntitySpriteDefinition.OamAttribute(0x72, 0x00)),
            definition.rectangleVariant(5).get(2));
    }

    @Test
    void usesPiranhaPlantsRomCombatAndPhysicsMetadata() throws IOException {
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_PIRANHA_PLANT));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_PIRANHA_PLANT));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(ENTITY_PIRANHA_PLANT));

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A),
            0)), true, () -> 0, catalog, new RomEnemyCombatTables(loadRom()));

        assertEquals(0x14, runtime.physicsFlags(0));
        assertEquals(0x00, runtime.options1(0));
        assertEquals(0x01, runtime.enemyHealth(0));
    }

    @Test
    void clearsSharedSwordRecoilBeforeRunningItsStateMachine() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A),
            0)), true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        EnemyAttackContext nonDamaging =
            new EnemyAttackContext(0, false, false, false, false);
        runtime.tick(0, 0x20, 0x78, () -> 0);
        assertTrue(runtime.resolveCombat(1, 0x78, 0x78, false, true,
            true, 0x48, 1, 0x48, 1, nonDamaging).get(0).swordHit());

        runtime.tick(1, 0x78, 0x78, () -> 0);

        assertEquals(0x40, runtime.snapshot().slots().get(0).x());
        assertEquals(0x40, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void waitsForWorldGameplayAndTheStableTransitionSequence() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(ENTITY_PIRANHA_PLANT,
            EntityRoomLoader.RoomTable.INDOORS_A);

        RoomEntityRuntime creditsRuntime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x40, EntityStatus.ACTIVE,
            definition, 0)), true, () -> 0, catalog, new RomEnemyCombatTables(rom));
        creditsRuntime.tick(0, 0x20, 0x40, () -> 0, true);
        assertEquals(0, creditsRuntime.transitionCountdown(0));
        assertEquals(0x40, creditsRuntime.snapshot().slots().get(0).y());

        RoomEntityRuntime transitionRuntime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x40, EntityStatus.ACTIVE,
            definition, 0)), true, () -> 0, catalog, new RomEnemyCombatTables(rom));
        transitionRuntime.setTransitionSequenceCounterForTest(0x03);
        transitionRuntime.tick(0, 0x20, 0x40, () -> 0);
        assertEquals(0, transitionRuntime.transitionCountdown(0));
        assertEquals(0x40, transitionRuntime.snapshot().slots().get(0).y());
    }

    @Test
    void hiddenPipeStateDoesNotRunEnemyDamageCollision() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A),
            0)), true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(runtime.resolveCombat(1, 0x40, 0x40, false, true,
            false, 0, 0, 0, 0).isEmpty());
    }

    @Test
    void waitsUntilLinkLeavesItsPipeColumnBeforeStartingRise() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x60, EntityStatus.INIT,
            catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A),
            0)), true, () -> 0, catalog, new RomEnemyCombatTables(loadRom()));

        runtime.tick(0, 0x40, 0x20, () -> 0);
        runtime.tick(1, 0x40, 0x20, () -> 0);
        assertEquals(0x40, runtime.transitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 2; frame <= 0x41; frame++) {
            runtime.tick(frame, 0x40, 0x20, () -> 0);
        }
        assertEquals(0x40, runtime.transitionCountdown(0));

        for (int frame = 0x42; frame <= 0x81; frame++) {
            runtime.tick(frame, 0x00, 0x20, () -> 0);
        }
        assertEquals(0x40, runtime.transitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void risesThroughRomFramesThenReturnsToHiddenPosition() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_PIRANHA_PLANT, 0x40, 0x60, EntityStatus.INIT,
            catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A),
            0)), true, () -> 0, catalog, new RomEnemyCombatTables(loadRom()));

        runtime.tick(0, 0x00, 0x20, () -> 0);
        runtime.tick(1, 0x00, 0x20, () -> 0);
        assertEquals(0x40, runtime.transitionCountdown(0));

        for (int frame = 2; frame <= 0x41; frame++) {
            runtime.tick(frame, 0x00, 0x20, () -> 0);
        }
        assertEquals(0x80, runtime.transitionCountdown(0));
        assertEquals(4, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(0x40, runtime.snapshot().slots().get(0).y());

        for (int frame = 0x42; frame <= 0xC1; frame++) {
            runtime.tick(frame, 0x00, 0x20, () -> 0);
        }
        assertEquals(0x40, runtime.transitionCountdown(0));
        assertEquals(5, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 0xC2; frame <= 0x101; frame++) {
            runtime.tick(frame, 0x00, 0x20, () -> 0);
        }
        assertEquals(0x00, runtime.transitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(0x60, runtime.snapshot().slots().get(0).y());
    }

    private static RoomEntitySnapshot snapshot(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = PiranhaPlantRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
