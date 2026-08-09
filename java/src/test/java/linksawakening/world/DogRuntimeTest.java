package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DogRuntimeTest {
    private static final int ENTITY_DOG = 0x6F;

    @Test
    void decodesDogsFourRomSpritePairs() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_DOG, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(0x19, definition.bank());
        assertEquals(0x48CA, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(2, definition.initialVariant());
        assertEquals(4, definition.variantCount());

        int[][] expected = {
            {0x58, 0x01, 0x5A, 0x01},
            {0x58, 0x01, 0x5C, 0x01},
            {0x5A, 0x21, 0x58, 0x21},
            {0x5C, 0x21, 0x58, 0x21}
        };
        for (int variant = 0; variant < expected.length; variant++) {
            assertEquals(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(expected[variant][0], expected[variant][1]),
                new EntitySpriteDefinition.OamAttribute(expected[variant][2], expected[variant][3])),
                definition.variant(variant));
        }
    }

    @Test
    void usesDogsRomMetadataAndHandlerHealthSeed() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_DOG, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_DOG, EntityRoomLoader.RoomTable.OVERWORLD), 2)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertFalse(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_DOG));
        assertEquals(0x92, runtime.physicsFlags(0));
        assertEquals(0x00, runtime.options1(0));
        assertEquals(0x01, runtime.enemyHealth(0));

        runtime.tick(0, 0x40, 0x40, () -> 0);

        assertEquals(0x4C, runtime.enemyHealth(0));
        assertEquals(0x01, runtime.dogState(0));
    }

    @Test
    void stateZeroUsesRomRandomWalkTableAndStartsStateOne() throws IOException {
        DogMotion motion = new DogMotion();
        RoomEntity dog = dog(loadRom());

        DogMotion.Update update = motion.advance(dog, 0, 0x00, 0x92,
            0x70, 0x40, 0, false, false,
            sequence(0x00, 0x01, 0x1F), null);

        assertEquals(0x01, update.state());
        assertEquals(0x4F, update.transitionCountdown());
        assertEquals(0x02, update.speedX());
        assertEquals(0x00, update.direction());
        assertEquals(0x08, update.speedY());
        assertEquals(0xFE, update.speedZ());
        assertEquals(0x92, update.physicsFlags());
        assertEquals(0x40, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(0x02, update.entity().spriteVariant());
    }

    @Test
    void stateOneMovesBeforeRetargetingAndHopsWhenBackgroundBlocksIt() throws IOException {
        DogMotion motion = new DogMotion();
        RoomEntity dog = dog(loadRom());
        motion.setStateForTest(0, 1, 0, 0x10, 0x00, 0x00);

        DogMotion.Update moved = motion.advance(dog, 8, 0x03, 0x92,
            0x70, 0x40, 0, false, false, () -> 0, null);
        assertEquals(0x41, moved.entity().x());
        assertEquals(0x40, moved.entity().y());
        assertEquals(0x01, moved.state());
        assertEquals(0x03, moved.transitionCountdown());

        DogMotion.Update blocked = motion.advance(moved.entity(), 16, 0x03, 0x92,
            0x70, 0x40, 0, false, false, () -> 0,
            (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY));

        assertEquals(0x41, blocked.entity().x());
        assertEquals(0x40, blocked.entity().y());
        assertEquals(0x01, blocked.state());
        assertEquals(0x08, blocked.speedZ());
        assertEquals(0x01, blocked.entity().z());
        assertEquals(0x01, blocked.collisionFlags());
    }

    @Test
    void stateOneCollisionWithExpiredTimerReturnsToStateZero() throws IOException {
        DogMotion motion = new DogMotion();
        RoomEntity dog = dog(loadRom());
        motion.setStateForTest(0, 1, 0, 0x10, 0x00, 0x00);

        DogMotion.Update update = motion.advance(dog, 0, 0x00, 0x92,
            0x70, 0x40, 0, false, false, () -> 0,
            (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY));

        assertEquals(0x00, update.state());
        assertEquals(0x30, update.transitionCountdown());
        assertEquals(0xFE, update.speedZ());
        assertEquals(0x00, update.entity().z());
    }

    @Test
    void stateTwoLaunchesWithRomVectorAndStateThreeResetsOnCollision() throws IOException {
        DogMotion motion = new DogMotion();
        RoomEntity dog = dog(loadRom());
        motion.setStateForTest(0, 2, 0, 0x00, 0x00, 0x00);

        DogMotion.Update launched = motion.advance(dog, 0, 0x00, 0x92,
            0x70, 0x40, 0, false, false, () -> 0, null);
        assertEquals(0x03, launched.state());
        assertEquals(0x24, launched.speedX());
        assertEquals(0x00, launched.speedY());
        assertEquals(0x18, launched.speedZ());
        assertEquals(0x00, launched.direction());
        assertEquals(0x00, launched.entity().z());

        motion.setStateForTest(0, 3, 0, 0x10, 0x00, 0x00);
        DogMotion.Update blocked = motion.advance(dog, 0, 0x20, 0x92,
            0x70, 0x40, 0, false, false, () -> 0,
            (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY));

        assertEquals(0x00, blocked.state());
        assertEquals(0x20, blocked.transitionCountdown());
        assertEquals(0x92, blocked.physicsFlags());
    }

    @Test
    void dogCanRequestItsRomDialogWhenLinkFacesIt() throws IOException {
        DogMotion motion = new DogMotion();
        RoomEntity dog = dog(loadRom());

        DogMotion.Update update = motion.advance(dog, 0, 0x20, 0x92,
            0x4F, 0x40, 1, true, false, () -> 0, null);

        assertTrue(update.dialogRequested());
        assertEquals(0x20, update.dialogLowId());
    }

    @Test
    void nonInteractiveLinkMotionSkipsDogTerrainInteraction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_DOG, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_DOG, EntityRoomLoader.RoomTable.OVERWORLD), 2)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        AtomicInteger terrainCalls = new AtomicInteger();

        runtime.tickWithProjectileEvents(0, 0x40, 0x40, () -> 0,
            (entity, direction, nextX, nextY) -> {
                terrainCalls.incrementAndGet();
                return true;
            }, new EnemyProjectileCollision.LinkState(0x40, 0x40, 0,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE, 0, false));

        assertEquals(0, terrainCalls.get());
    }

    private static RoomEntity dog(byte[] rom) {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(ENTITY_DOG, EntityRoomLoader.RoomTable.OVERWORLD);
        return new RoomEntity(0, 0, ENTITY_DOG, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 2);
    }

    private static RoomEntitySnapshot snapshot(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = DogRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
