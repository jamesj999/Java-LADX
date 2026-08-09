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

final class WitchRatRuntimeTest {
    private static final int ENTITY_WITCH_RAT = 0xE1;

    @Test
    void decodesWitchRatRomSpritePairs() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_WITCH_RAT, EntityRoomLoader.RoomTable.INDOORS_B);

        assertTrue(definition.supported());
        assertEquals(0x15, definition.bank());
        assertEquals(0x788D, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(4, definition.variantCount());
        int[][] expected = {
            {0x50, 0x03, 0x52, 0x03},
            {0x54, 0x03, 0x56, 0x03},
            {0x52, 0x23, 0x50, 0x23},
            {0x56, 0x23, 0x54, 0x23}
        };
        for (int variant = 0; variant < expected.length; variant++) {
            assertEquals(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(expected[variant][0], expected[variant][1]),
                new EntitySpriteDefinition.OamAttribute(expected[variant][2], expected[variant][3])),
                definition.variant(variant));
        }
    }

    @Test
    void exposesWitchRatRomCombatAndPhysicsMetadata() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_WITCH_RAT, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_WITCH_RAT, EntityRoomLoader.RoomTable.INDOORS_B), 0)),
            true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_WITCH_RAT));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_WITCH_RAT));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(ENTITY_WITCH_RAT));
        assertEquals(0xD2, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        assertEquals(0x01, runtime.enemyHealth(0));
    }

    @Test
    void stateZeroSeedsTheRomMovementTableAndWitchRatZSpeed() throws IOException {
        WitchRatMotion motion = new WitchRatMotion();
        RoomEntity rat = witchRat(loadRom());

        WitchRatMotion.Update started = motion.advance(rat, 0, 0x00,
            sequence(0x02, 0x00, 0x00), null);

        assertEquals(1, started.state());
        assertEquals(0x30, started.transitionCountdown());
        assertEquals(0x00, started.direction());
        assertEquals(0x0C, started.speedX());
        assertEquals(0x02, started.speedY());
        assertEquals(0xFE, started.speedZ());
        assertEquals(0x40, started.entity().x());
        assertEquals(0x40, started.entity().y());
        assertEquals(0x00, started.entity().z());
        assertEquals(2, started.entity().spriteVariant());
    }

    @Test
    void stateOneUsesFixedPointMovementAndBouncesWhenZWraps() throws IOException {
        WitchRatMotion motion = new WitchRatMotion();
        RoomEntity rat = witchRat(loadRom());

        WitchRatMotion.Update started = motion.advance(rat, 0, 0x00,
            sequence(0x02, 0x00, 0x00), null);
        WitchRatMotion.Update firstMove = motion.advance(started.entity(), 1, 0x2F,
            sequence(0), null);

        assertEquals(0x01, firstMove.state());
        assertEquals(0x08, firstMove.speedZ());
        assertEquals(0x01, firstMove.entity().z());
        assertEquals(0x40, firstMove.entity().x());
        assertEquals(0x40, firstMove.entity().y());
        assertTrue(firstMove.appliesBackgroundInteraction());

        motion.setStateForTest(0, 1, 0x00, 0x00, 0x00, 0x00, 0xFE);
        WitchRatMotion.Update landed = motion.advance(rat, 2, 0x00,
            sequence(0), null);
        assertEquals(0, landed.state());
        assertEquals(0x48, landed.transitionCountdown());
        assertEquals(0x00, landed.speedZ());
        assertEquals(0x00, landed.entity().z());
    }

    @Test
    void stateZeroFlipsFacingOnlyOnTheRomThirtyTwoFrameBoundary() throws IOException {
        WitchRatMotion motion = new WitchRatMotion();
        RoomEntity rat = witchRat(loadRom());

        WitchRatMotion.Update flipped = motion.advance(rat, 0, 0x10,
            sequence(0x00), null);
        assertEquals(0, flipped.state());
        assertEquals(0x04, flipped.direction());
        assertEquals(0x10, flipped.transitionCountdown());
        assertEquals(2, flipped.entity().spriteVariant());

        WitchRatMotion.Update held = motion.advance(flipped.entity(), 1, 0x0F,
            sequence(0x00), null);
        assertEquals(0x04, held.direction());
        assertEquals(1, held.entity().spriteVariant());
    }

    @Test
    void blockedStateOneMovementPreservesPositionAndReportsRomCollisionFlag()
            throws IOException {
        WitchRatMotion motion = new WitchRatMotion();
        RoomEntity rat = witchRat(loadRom());
        WitchRatMotion.Update started = motion.advance(rat, 0, 0x00,
            sequence(0x02, 0x00, 0x00), null);
        WitchRatMotion.Update first = motion.advance(started.entity(), 1, 0x2F,
            sequence(0), null);
        WitchRatMotion.Update blocked = motion.advance(first.entity(), 2, 0x2E,
            sequence(0), (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY));

        assertEquals(0x40, blocked.entity().x());
        assertEquals(0x40, blocked.entity().y());
        assertEquals(0x01, blocked.collisionFlags());
        assertTrue(blocked.appliesBackgroundInteraction());
    }

    @Test
    void runtimeWiresWitchRatStateAndRomPresentation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_WITCH_RAT, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_WITCH_RAT, EntityRoomLoader.RoomTable.INDOORS_B), 0)),
            true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tickWithProjectileEvents(0, 0x20, 0x20, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x20, 0x20, 0, 0, 0, false));

        assertEquals(1, runtime.witchRatState(0));
        assertEquals(0x30, runtime.witchRatTransitionCountdown(0));
        assertEquals(0xFE, runtime.witchRatSpeedZ(0));
        assertEquals(0x15, runtime.snapshot().slots().get(0).spriteDefinition().bank());
        assertEquals(0x788D, runtime.snapshot().slots().get(0).spriteDefinition().address());
    }

    private static RoomEntity witchRat(byte[] rom) {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(ENTITY_WITCH_RAT, EntityRoomLoader.RoomTable.INDOORS_B);
        return new RoomEntity(0, 0, ENTITY_WITCH_RAT, 0x40, 0x40,
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

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = WitchRatRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
