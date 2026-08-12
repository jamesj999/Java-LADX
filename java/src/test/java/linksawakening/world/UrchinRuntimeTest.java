package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UrchinRuntimeTest {
    private static final int ENTITY_URCHIN = 0xC5;

    @Test
    void decodesNormalAndCreditsRomSpritePairs() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition normal = catalog.forEntityType(
            ENTITY_URCHIN, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition credits = catalog.forUrchinState(true);

        assertUrchinSprites(normal, 0x7383, new int[][] {
            {0x5C, 0x03, 0x5E, 0x03},
            {0x5E, 0x23, 0x5C, 0x23},
            {0x5E, 0x63, 0x5C, 0x63},
            {0x5C, 0x43, 0x5E, 0x43}
        });
        assertUrchinSprites(credits, 0x7393, new int[][] {
            {0x2C, 0x03, 0x2E, 0x03},
            {0x2E, 0x23, 0x2C, 0x23},
            {0x2E, 0x63, 0x2C, 0x63},
            {0x2C, 0x43, 0x2E, 0x43}
        });
    }

    @Test
    void exposesUrchinRomMetadataAndBigBlockCollision() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_URCHIN, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_URCHIN, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_URCHIN));
        assertTrue(RoomEntityCombatRules.supportsLinkCollision(ENTITY_URCHIN));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        assertEquals(0x01, runtime.enemyHealth(0));
        assertTrue(RoomEntityCombatRules.overlapsLink(
            runtime.snapshot().slots().get(0), 0x4C, 0x40));
    }

    @Test
    void normalContactUsesTheSharedEnemyDamagePath() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_URCHIN, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_URCHIN, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 0x4C, 0x40, false, true, false, 0, 0, 0, 0);

        assertEquals(1, events.size());
        EntityCombatEvent event = events.getFirst();
        assertEquals(ENTITY_URCHIN, event.type());
        assertEquals(0x04, event.linkDamage());
        assertTrue(event.linkCollisionResponse().active());
    }

    @Test
    void normalContactKeepsItsRomDamageWhenCombatTablesAreUnavailable() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_URCHIN, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_URCHIN, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 0x4C, 0x40, false, true, false, 0, 0, 0, 0);

        assertEquals(0x04, events.getFirst().linkDamage());
    }

    @Test
    void shieldFacingUsesRomDirectionAndPushesAwayFromLink() throws IOException {
        UrchinMotion motion = new UrchinMotion();
        RoomEntity urchin = urchin(loadRom());

        UrchinMotion.Update update = motion.advance(urchin, 0, 0x12,
            0x48, 0x40, 1, true, false, true, null);

        assertEquals(0, update.direction());
        assertEquals(0xFD, update.speedX());
        assertEquals(0x00, update.speedY());
        assertEquals(0x92, update.physicsFlags());
        assertTrue(update.linkCollision());
        assertTrue(update.pushed());
        assertEquals(0x3E, update.jingleId());
        assertEquals(0x3F, update.entity().x());
        assertEquals(0x40, update.entity().y());
    }

    @Test
    void shieldFacingSelectsTheDominantVerticalAxis() throws IOException {
        UrchinMotion motion = new UrchinMotion();
        RoomEntity urchin = urchin(loadRom());

        UrchinMotion.Update update = motion.advance(urchin, 0, 0x12,
            0x40, 0x3A, 3, true, false, true, null);

        assertEquals(2, update.direction());
        assertEquals(0x00, update.speedX());
        assertEquals(0x03, update.speedY());
        assertEquals(0x92, update.physicsFlags());
        assertTrue(update.pushed());
        assertEquals(0x40, update.entity().y());
    }

    @Test
    void shieldFromTheWrongDirectionDoesNotPush() throws IOException {
        UrchinMotion motion = new UrchinMotion();
        RoomEntity urchin = urchin(loadRom());

        UrchinMotion.Update update = motion.advance(urchin, 0, 0x12,
            0x48, 0x40, 0, true, false, true, null);

        assertEquals(0, update.direction());
        assertEquals(0x12, update.physicsFlags());
        assertFalse(update.linkCollision());
        assertFalse(update.pushed());
        assertEquals(0x40, update.entity().x());
        assertEquals(-1, update.jingleId());
    }

    @Test
    void nonInteractiveLinkSkipsUrchinHandlerWork() throws IOException {
        UrchinMotion motion = new UrchinMotion();
        RoomEntity urchin = urchin(loadRom());

        UrchinMotion.Update update = motion.advance(urchin, 0, 0x12,
            0x48, 0x40, 1, true, false, false, null);

        assertEquals(0x12, update.physicsFlags());
        assertFalse(update.linkCollision());
        assertFalse(update.pushed());
        assertEquals(0x40, update.entity().x());
        assertEquals(0, update.entity().spriteVariant());
    }

    @Test
    void runtimeEmitsPushJingleAndFinalPositionRequest() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_URCHIN, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_URCHIN, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tickWithProjectileEvents(0, 0x48, 0x40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x48, 0x40, 0, 0, 2, true));

        assertEquals(0x3F, runtime.snapshot().slots().get(0).x());
        assertEquals(0x92, runtime.physicsFlags(0));
        assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(List.of(new RoomEntityRuntime.LinkFinalPositionRequest(0)),
            runtime.consumePendingLinkFinalPositionRequests());
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x3E, events.get(0).soundId());
    }

    private static void assertUrchinSprites(EntitySpriteDefinition definition, int address,
                                             int[][] expected) {
        assertTrue(definition.supported());
        assertEquals(0x15, definition.bank());
        assertEquals(address, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(4, definition.variantCount());
        for (int variant = 0; variant < expected.length; variant++) {
            assertEquals(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(expected[variant][0], expected[variant][1]),
                new EntitySpriteDefinition.OamAttribute(expected[variant][2], expected[variant][3])),
                definition.variant(variant));
        }
    }

    private static RoomEntity urchin(byte[] rom) {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(ENTITY_URCHIN, EntityRoomLoader.RoomTable.OVERWORLD);
        return new RoomEntity(0, 0, ENTITY_URCHIN, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
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
        try (var stream = UrchinRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
