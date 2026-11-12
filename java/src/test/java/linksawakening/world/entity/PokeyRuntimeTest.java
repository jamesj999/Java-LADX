package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.motion.PokeyMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class PokeyRuntimeTest {
    private static final int ENTITY_POKEY = 0xE3;

    @Test
    void decodesPokeysThreeRomDisplayStatesAndDetachedPair() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));

        EntitySpriteDefinition main = catalog.forEntityType(ENTITY_POKEY, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x15, main.bank());
        assertEquals(0x4B17, main.address());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, main.shape());
        assertEquals(4, main.variantCount());
        assertEquals(6, main.rectangleVariant(0).size());
        assertEquals(
                new EntitySpriteDefinition.RectangleSprite(
                        -24, -2, new EntitySpriteDefinition.OamAttribute(0x74, 0x00)),
                main.rectangleVariant(0).get(0));
        assertEquals(
                new EntitySpriteDefinition.RectangleSprite(0, 8, new EntitySpriteDefinition.OamAttribute(0x76, 0x20)),
                main.rectangleVariant(0).get(5));

        EntitySpriteDefinition fourSprite = catalog.forPokeyState(1);
        assertEquals(0x4B77, fourSprite.address());
        assertEquals(4, fourSprite.rectangleVariant(0).size());

        EntitySpriteDefinition twoSprite = catalog.forPokeyState(2);
        assertEquals(0x4BB7, twoSprite.address());
        assertEquals(2, twoSprite.rectangleVariant(0).size());

        EntitySpriteDefinition segment = catalog.forPokeySegment();
        assertEquals(0x15, segment.bank());
        assertEquals(0x4CD5, segment.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, segment.shape());
        assertEquals(1, segment.variantCount());
        assertEquals(
                new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x76, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0x76, 0x20)),
                segment.variant(0));
    }

    @Test
    void usesPokeysRomCombatAndEntityFlags() {
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_POKEY));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_POKEY));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(ENTITY_POKEY));
    }

    @Test
    void mainPokeyAnimatesAndUsesItsRomHealthOverride() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_POKEY)
                        .at(0x40, 0x40)
                        .spriteDefinition(catalog.forEntityType(ENTITY_POKEY, EntityRoomLoader.RoomTable.OVERWORLD))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(
                        new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom())))
                .build();

        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));
        assertEquals(0x02, runtime.enemies().enemyHealth(0));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).entityInertia(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 0x70, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void flashSeamSpawnsAHighSlotDetachedSegment() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(ENTITY_POKEY)
                        .at(0x40, 0x40)
                        .spriteDefinition(catalog.forEntityType(ENTITY_POKEY, EntityRoomLoader.RoomTable.OVERWORLD))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        // The runtime performs the shared pre-handler decrement first, so $15
        // reaches PokeyEntityHandler as the source's $14 spawn trigger.
        EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(0, 0x15);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, () -> 0));

        RoomEntity segment = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.type() == ENTITY_POKEY && entity.sourceLoadOrder() == -1)
                .findFirst()
                .orElseThrow();
        assertEquals(15, segment.slot());
        assertEquals(0x40, segment.x());
        assertEquals(0x40, segment.y());
        assertEquals(
                EntitySpriteDefinition.Shape.PAIR, segment.spriteDefinition().shape());
        assertEquals(0x4CD5, segment.spriteDefinition().address());
        assertEquals(0x01, runtime.enemies().enemyHealth(segment.slot()));
        assertEquals(0x12, runtime.enemies().physicsFlags(segment.slot()));
        assertEquals(0x02, runtime.options1(segment.slot()));
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).pokeyPrivateState1(segment.slot()));
        assertEquals(
                0x18, new EntityRoomInspectionQueries(runtime.inspection()).pokeyTransitionCountdown(segment.slot()));
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).entityInertia(0));
        assertNotNull(segment);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x70, 0x50, () -> 0));
        RoomEntity body = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x4B77, body.spriteDefinition().address());
        assertEquals(4, body.spriteDefinition().rectangleVariant(0).size());
    }

    @Test
    void detachedSegmentsReverseOnWallsAndPoofAfterThreeHits() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        RoomEntity segment = new RoomEntityBuilder()
                .type(ENTITY_POKEY)
                .at(0x40, 0x40)
                .spriteDefinition(catalog.forPokeySegment())
                .build();
        PokeyMotion motion = new PokeyMotion();
        motion.initializeSegment(0, 0x10, 0x00);

        PokeyMotion.Update first = motion.advanceSegment(
                segment, 0, 0, (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.RIGHT);
        assertEquals(1, first.inertia());
        assertEquals(0xF0, motion.speedX(0));
        assertTrue(first.bumpJingle());
        assertTrue(!first.unloadRequested());

        PokeyMotion.Update second = motion.advanceSegment(
                first.entity(),
                first.inertia(),
                first.transitionCountdown(),
                (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.RIGHT);
        assertEquals(2, second.inertia());
        assertTrue(!second.unloadRequested());

        PokeyMotion.Update third = motion.advanceSegment(
                second.entity(),
                second.inertia(),
                second.transitionCountdown(),
                (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.RIGHT);
        assertEquals(3, third.inertia());
        assertTrue(third.unloadRequested());
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
