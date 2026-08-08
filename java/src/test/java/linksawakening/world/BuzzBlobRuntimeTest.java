package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuzzBlobRuntimeTest {
    private static final int ENTITY_BUZZ_BLOB = 0xB9;

    @Test
    void decodesBuzzBlobsNineRomSpritePairs() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(0x18, definition.bank());
        assertEquals(0x7729, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(9, definition.variantCount());

        int[][] expected = {
            {0x62, 0x00, 0x64, 0x00},
            {0x60, 0x00, 0x60, 0x20},
            {0x64, 0x20, 0x62, 0x20},
            {0x66, 0x00, 0x68, 0x00},
            {0x60, 0x00, 0x60, 0x20},
            {0x68, 0x20, 0x66, 0x20},
            {0x6C, 0x00, 0x6E, 0x00},
            {0x6A, 0x00, 0x6A, 0x20},
            {0x6E, 0x20, 0x6C, 0x20}
        };
        for (int variant = 0; variant < expected.length; variant++) {
            assertEquals(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(expected[variant][0], expected[variant][1]),
                new EntitySpriteDefinition.OamAttribute(expected[variant][2], expected[variant][3])),
                definition.variant(variant));
        }
    }

    @Test
    void usesBuzzBlobsRomCombatAndPhysicsMetadata() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_BUZZ_BLOB));
        assertEquals(0x08, RoomEntityCombatRules.contactDamage(ENTITY_BUZZ_BLOB));
        assertEquals(0x04, RoomEntityCombatRules.initialHealth(ENTITY_BUZZ_BLOB));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x00, runtime.options1(0));
        assertEquals(0x04, runtime.enemyHealth(0));
    }

    @Test
    void stateZeroUsesRomTimerSpeedAndFourFrameAnimation() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD);
        BuzzBlobMotion motion = new BuzzBlobMotion();
        RoomEntity buzzBlob = new RoomEntity(0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        BuzzBlobMotion.Update first = motion.advance(buzzBlob, 0, 0x00,
            () -> 0x03, null);
        assertEquals(0, first.state());
        assertEquals(0x33, first.transitionCountdown());
        assertEquals(0x04, first.speedX());
        assertEquals(0x04, first.speedY());
        assertEquals(0, first.entity().spriteVariant());

        BuzzBlobMotion.Update animated = motion.advance(first.entity(), 0x08, 0x32,
            () -> 0, null);
        assertEquals(1, animated.entity().spriteVariant());
        assertEquals(0x40, animated.entity().x());
        assertEquals(0x40, animated.entity().y());
        assertTrue(first.appliesBackgroundInteraction());

        BuzzBlobMotion.Update afterFiveFrames = animated;
        for (int frame = 2; frame <= 5; frame++) {
            afterFiveFrames = motion.advance(afterFiveFrames.entity(), frame * 8,
                0x33 - frame, () -> 0, null);
        }
        assertEquals(0x41, afterFiveFrames.entity().x());
        assertEquals(0x41, afterFiveFrames.entity().y());
    }

    @Test
    void backgroundInteractionRestoresBlockedBuzzBlobPosition() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD);
        BuzzBlobMotion motion = new BuzzBlobMotion();
        RoomEntity buzzBlob = new RoomEntity(0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        BuzzBlobMotion.Update first = motion.advance(buzzBlob, 0, 0x00,
            () -> 0x05, (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x01, 0x01, nextX, nextY));
        BuzzBlobMotion.Update blocked = motion.advance(first.entity(), 1, 0x34,
            () -> 0, (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x01, 0x01, nextX, nextY));

        assertEquals(0x40, blocked.entity().x());
        assertEquals(0x40, blocked.entity().y());
    }

    @Test
    void stateOneUsesItsSeparateRomAnimationAndReturnsToStateZero() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD);
        BuzzBlobMotion motion = new BuzzBlobMotion();
        motion.initializeStateOne(0);
        RoomEntity buzzBlob = new RoomEntity(0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        BuzzBlobMotion.Update animated = motion.advance(buzzBlob, 0, 0x04,
            () -> 0, null);
        assertEquals(1, animated.state());
        assertEquals(3, animated.entity().spriteVariant());
        assertFalse(animated.appliesBackgroundInteraction());

        BuzzBlobMotion.Update expired = motion.advance(animated.entity(), 0x08, 0x00,
            () -> 0, null);
        assertEquals(0, expired.state());
        assertEquals(3, expired.entity().spriteVariant());
        assertFalse(expired.appliesBackgroundInteraction());
    }

    @Test
    void nonInteractiveLinkMotionSkipsBuzzBlobTerrainInteraction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD),
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

    @Test
    void nonInteractiveLinkMotionSkipsBuzzBlobSharedRecoil() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD),
            0)), false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.configureEnemyRecoilForTest(0, 0x40, 0x40, 0x10);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x0A);

        runtime.tickWithProjectileEvents(0, 0x40, 0x40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x40, 0,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE, 0, false));

        RoomEntity after = runtime.snapshot().slots().get(0);
        assertEquals(0x40, after.x());
        assertEquals(0x40, after.y());
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
    }

    @Test
    void nonInteractiveLinkMotionSkipsBuzzBlobCombat() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_BUZZ_BLOB, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_BUZZ_BLOB, EntityRoomLoader.RoomTable.OVERWORLD),
            0)), false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(runtime.resolveCombat(0, 0x40, 0x40, false, false,
            true, 0x40, 1, 0x40, 1).isEmpty());
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
        try (var stream = BuzzBlobRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
