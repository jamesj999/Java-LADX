package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PokeyRuntimeTest {
    private static final int ENTITY_POKEY = 0xE3;

    @Test
    void decodesPokeysThreeRomDisplayStatesAndDetachedPair() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition main = catalog.forEntityType(
            ENTITY_POKEY, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x15, main.bank());
        assertEquals(0x4B17, main.address());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, main.shape());
        assertEquals(4, main.variantCount());
        assertEquals(6, main.rectangleVariant(0).size());
        assertEquals(new EntitySpriteDefinition.RectangleSprite(-24, -2,
            new EntitySpriteDefinition.OamAttribute(0x74, 0x00)),
            main.rectangleVariant(0).get(0));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(0, 8,
            new EntitySpriteDefinition.OamAttribute(0x76, 0x20)),
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
        assertEquals(new EntitySpriteDefinition.Variant(
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_POKEY, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_POKEY, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(loadRom()));

        assertEquals(0x01, runtime.enemyHealth(0));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));

        runtime.tick(0, 0x70, 0x50, () -> 0);
        assertEquals(0x02, runtime.enemyHealth(0));
        assertEquals(0, runtime.entityInertia(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(8, 0x70, 0x50, () -> 0);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void flashSeamSpawnsAHighSlotDetachedSegment() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_POKEY, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_POKEY, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        // The runtime performs the shared pre-handler decrement first, so $15
        // reaches PokeyEntityHandler as the source's $14 spawn trigger.
        runtime.setEnemyFlashCountdownForTest(0, 0x15);
        runtime.tick(0, 0x70, 0x50, () -> 0);

        RoomEntity segment = runtime.snapshot().slots().stream()
            .filter(entity -> entity.type() == ENTITY_POKEY && entity.sourceLoadOrder() == -1)
            .findFirst()
            .orElseThrow();
        assertEquals(15, segment.slot());
        assertEquals(0x40, segment.x());
        assertEquals(0x40, segment.y());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, segment.spriteDefinition().shape());
        assertEquals(0x4CD5, segment.spriteDefinition().address());
        assertEquals(0x01, runtime.enemyHealth(segment.slot()));
        assertEquals(0x12, runtime.physicsFlags(segment.slot()));
        assertEquals(0x02, runtime.options1(segment.slot()));
        assertEquals(1, runtime.pokeyPrivateState1(segment.slot()));
        assertEquals(0x18, runtime.pokeyTransitionCountdown(segment.slot()));
        assertEquals(1, runtime.entityInertia(0));
        assertNotNull(segment);

        runtime.tick(1, 0x70, 0x50, () -> 0);
        RoomEntity body = runtime.snapshot().slots().get(0);
        assertEquals(0x4B77, body.spriteDefinition().address());
        assertEquals(4, body.spriteDefinition().rectangleVariant(0).size());
    }

    @Test
    void detachedSegmentsReverseOnWallsAndPoofAfterThreeHits() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        RoomEntity segment = new RoomEntity(0, -1, ENTITY_POKEY, 0x40, 0x40,
            EntityStatus.ACTIVE, catalog.forPokeySegment(), 0);
        PokeyMotion motion = new PokeyMotion();
        motion.initializeSegment(0, 0x10, 0x00);

        PokeyMotion.Update first = motion.advanceSegment(segment, 0, 0,
            (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.RIGHT);
        assertEquals(1, first.inertia());
        assertEquals(0xF0, motion.speedX(0));
        assertTrue(first.bumpJingle());
        assertTrue(!first.unloadRequested());

        PokeyMotion.Update second = motion.advanceSegment(first.entity(), first.inertia(),
            first.transitionCountdown(),
            (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.RIGHT);
        assertEquals(2, second.inertia());
        assertTrue(!second.unloadRequested());

        PokeyMotion.Update third = motion.advanceSegment(second.entity(), second.inertia(),
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
        try (var stream = PokeyRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
