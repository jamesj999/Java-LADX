package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
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
        assertEquals(
                new EntitySpriteDefinition.RectangleSprite(-1, -1, new EntitySpriteDefinition.OamAttribute(0xFF, 0xFF)),
                definition.rectangleVariant(0).get(0));
        assertEquals(
                new EntitySpriteDefinition.RectangleSprite(-16, 0, new EntitySpriteDefinition.OamAttribute(0x74, 0x02)),
                definition.rectangleVariant(1).get(0));
        assertEquals(
                new EntitySpriteDefinition.RectangleSprite(16, 0, new EntitySpriteDefinition.OamAttribute(0x72, 0x00)),
                definition.rectangleVariant(4).get(2));
        assertEquals(
                new EntitySpriteDefinition.RectangleSprite(16, 0, new EntitySpriteDefinition.OamAttribute(0x72, 0x00)),
                definition.rectangleVariant(5).get(2));
    }

    @Test
    void usesPiranhaPlantsRomCombatAndPhysicsMetadata() throws IOException {
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_PIRANHA_PLANT));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_PIRANHA_PLANT));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(ENTITY_PIRANHA_PLANT));

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x60)
                        .spriteDefinition(
                                catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(
                        new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom())))
                .build();

        assertEquals(0x14, runtime.enemies().physicsFlags(0));
        assertEquals(0x00, runtime.options1(0));
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
    }

    @Test
    void clearsSharedSwordRecoilBeforeRunningItsStateMachine() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x40)
                        .spriteDefinition(
                                catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x78, () -> 0));
        assertTrue(runtime.resolveCombat(
                        new EntityVerticalContactFrame(1, 0x78, 0x78, false, true, true, 0x48, 1, 0x48, 1, nonDamaging))
                .get(0)
                .swordHit());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x78, 0x78, () -> 0));

        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void waitsForWorldGameplayAndTheStableTransitionSequence() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition =
                catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A);

        RoomEntityRuntime creditsRuntime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();
        creditsRuntime.tick(EntityTestFrames.frame(creditsRuntime, 0, 0x20, 0x40, () -> 0, true));
        assertEquals(0, creditsRuntime.enemies().transitionCountdown(0));
        assertEquals(0x40, creditsRuntime.inspection().snapshot().slots().get(0).y());

        RoomEntityRuntime transitionRuntime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();
        EntityRuntimeFixture.debug(transitionRuntime).setTransitionSequenceCounterForTest(0x03);
        transitionRuntime.tick(EntityTestFrames.frame(transitionRuntime, 0, 0x20, 0x40, () -> 0));
        assertEquals(0, transitionRuntime.enemies().transitionCountdown(0));
        assertEquals(
                0x40, transitionRuntime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void hiddenPipeStateDoesNotRunEnemyDamageCollision() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x40)
                        .spriteDefinition(
                                catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x40, false, true, false, 0, 0, 0, 0))
                .isEmpty());
    }

    @Test
    void waitsUntilLinkLeavesItsPipeColumnBeforeStartingRise() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x60)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(
                                catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(
                        new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom())))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x20, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x20, () -> 0));
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 2; frame <= 0x41; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x20, () -> 0));
        }
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));

        for (int frame = 0x42; frame <= 0x81; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x00, 0x20, () -> 0));
        }
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void risesThroughRomFramesThenReturnsToHiddenPosition() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_PIRANHA_PLANT)
                        .at(0x40, 0x60)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(
                                catalog.forEntityType(ENTITY_PIRANHA_PLANT, EntityRoomLoader.RoomTable.INDOORS_A))
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(
                        new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom())))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x00, 0x20, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x00, 0x20, () -> 0));
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));

        for (int frame = 2; frame <= 0x41; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x00, 0x20, () -> 0));
        }
        assertEquals(0x80, runtime.enemies().transitionCountdown(0));
        assertEquals(4, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).y());

        for (int frame = 0x42; frame <= 0xC1; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x00, 0x20, () -> 0));
        }
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));
        assertEquals(5, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 0xC2; frame <= 0x101; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x00, 0x20, () -> 0));
        }
        assertEquals(0x00, runtime.enemies().transitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x60, runtime.inspection().snapshot().slots().get(0).y());
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
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
