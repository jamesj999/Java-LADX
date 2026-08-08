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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SandCrabRuntimeTest {
    private static final int ENTITY_SAND_CRAB = 0xC6;

    @Test
    void decodesSandCrabsTwoRomSpritePairs() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_SAND_CRAB, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(0x15, definition.bank());
        assertEquals(0x7320, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(2, definition.variantCount());
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x58, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x58, 0x22)), definition.variant(0));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x5A, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x5A, 0x22)), definition.variant(1));
    }

    @Test
    void usesSandCrabsRomCombatAndPhysicsMetadata() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_SAND_CRAB, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SAND_CRAB, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_SAND_CRAB));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_SAND_CRAB));
        assertEquals(0x02, RoomEntityCombatRules.initialHealth(ENTITY_SAND_CRAB));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        assertEquals(0x02, runtime.enemyHealth(0));
    }

    @Test
    void movesBeforeStartingTheRomTimerAndUsesBankFifteenDirectionTables()
            throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_SAND_CRAB, EntityRoomLoader.RoomTable.OVERWORLD);
        SandCrabMotion motion = new SandCrabMotion();
        RoomEntity crab = new RoomEntity(0, 0, ENTITY_SAND_CRAB, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        SandCrabMotion.Update started = motion.advance(crab, 0, 0x00,
            sequence(0x7F, 0x02), null);
        assertEquals(0xAF, started.transitionCountdown());
        assertEquals(0x00, started.speedX());
        assertEquals(0xFB, started.speedY());
        assertEquals(0x40, started.entity().x());
        assertEquals(0x40, started.entity().y());
        assertEquals(0, started.entity().spriteVariant());
        assertEquals(0, started.collisionFlags());

        SandCrabMotion.Update moved = motion.advance(started.entity(), 8, 0xAE,
            sequence(0), null);
        assertEquals(0x3F, moved.entity().y());
        assertEquals(1, moved.entity().spriteVariant());
    }

    @Test
    void blockedMovementTriggersTheRomTimerAndChoosesANewDirection()
            throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_SAND_CRAB, EntityRoomLoader.RoomTable.OVERWORLD);
        SandCrabMotion motion = new SandCrabMotion();
        RoomEntity crab = new RoomEntity(0, 0, ENTITY_SAND_CRAB, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        SandCrabMotion.Update started = motion.advance(crab, 0, 0x00,
            sequence(0x00, 0x00), null);
        SandCrabMotion.Update blocked = motion.advance(started.entity(), 8, 0x2F,
            sequence(0x01, 0x03), (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY));

        assertEquals(0x40, blocked.entity().x());
        assertEquals(0x40, blocked.entity().y());
        assertEquals(0x31, blocked.transitionCountdown());
        assertEquals(0x00, blocked.speedX());
        assertEquals(0x05, blocked.speedY());
        assertEquals(0x01, blocked.collisionFlags());
    }

    @Test
    void nonInteractiveLinkMotionSkipsSandCrabTerrainInteraction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_SAND_CRAB, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SAND_CRAB, EntityRoomLoader.RoomTable.OVERWORLD),
            0)), false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        AtomicInteger groundCalls = new AtomicInteger();
        runtime.setGroundInteraction((entity, frameCounter, previousStatus, speedZ,
                                       sideScrolling) -> {
            groundCalls.incrementAndGet();
            return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
        });

        runtime.tickWithProjectileEvents(0, 0x40, 0x40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x40, 0,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE, 0, false));

        assertEquals(0, groundCalls.get());
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
        try (var stream = SandCrabRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
