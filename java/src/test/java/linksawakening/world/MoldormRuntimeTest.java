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

final class MoldormRuntimeTest {
    private static final int ENTITY_MOLDORM = 0x59;

    @Test
    void decodesMoldormRectangleAndTailFromRom() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A);

        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x57F2, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, definition.shape());
        assertEquals(1, definition.variantCount());
        assertEquals(16, definition.dynamicVariant(0).size());

        EntitySpriteDefinition.DynamicSprite first = definition.dynamicVariant(0).get(0);
        assertEquals(-8, first.yOffset());
        assertEquals(-8, first.xOffset());
        assertEquals(new EntitySpriteDefinition.OamAttribute(0x60, 0x00), first.oam());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
            first.tileSource());
        assertTrue(first.appliesEntityFlipAttribute());

        EntitySpriteDefinition.DynamicSprite firstTail = definition.dynamicVariant(0).get(8);
        assertEquals(0, firstTail.yOffset());
        assertEquals(0, firstTail.xOffset());
        assertEquals(new EntitySpriteDefinition.OamAttribute(0x70, 0x00), firstTail.oam());
        EntitySpriteDefinition.DynamicSprite fourthTail = definition.dynamicVariant(0).get(14);
        assertEquals(new EntitySpriteDefinition.OamAttribute(0x74, 0x00), fourthTail.oam());
        assertEquals(new EntitySpriteDefinition.OamAttribute(0x74, 0x20),
            definition.dynamicVariant(0).get(15).oam());
    }

    @Test
    void buildsHistoryTailAtRomOffsetsAndAppliesLastSegmentPaletteFlash() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition = catalog.forMoldormState(
            6, 0x40, 0x40,
            List.of(new MoldormMotion.TailPosition(0x30, 0x30),
                new MoldormMotion.TailPosition(0x24, 0x34),
                new MoldormMotion.TailPosition(0x28, 0x38),
                new MoldormMotion.TailPosition(0x2C, 0x3C)),
            0, 0x0C);

        assertEquals(16, definition.dynamicVariant(0).size());
        EntitySpriteDefinition.DynamicSprite firstTail = definition.dynamicVariant(0).get(8);
        assertEquals(-0x10, firstTail.xOffset());
        assertEquals(-0x10, firstTail.yOffset());
        assertEquals(new EntitySpriteDefinition.OamAttribute(0x70, 0x00), firstTail.oam());
        EntitySpriteDefinition.DynamicSprite lastTail = definition.dynamicVariant(0).get(14);
        assertEquals(0x76, lastTail.oam().tile());
        assertEquals(0x10, lastTail.oam().attributes());
        assertEquals(0x30, definition.dynamicVariant(0).get(15).oam().attributes());
    }

    @Test
    void activeMotionSeedsRomAngleTableAndFixedPointHistory() throws IOException {
        MoldormMotion motion = new MoldormMotion();
        RoomEntity moldorm = moldorm(loadRom());

        MoldormMotion.Update started = motion.advance(moldorm, 0, 0x00, 0x00, 0x00, 0x04,
            sequence(0x00, 0x00), null);

        assertEquals(0x06, started.headSpriteVariant());
        assertEquals(0x10, started.speedX());
        assertEquals(0x00, started.speedY());
        assertEquals(0x10, started.transitionCountdown());
        assertEquals(0x01, started.inertia());
        assertFalse(started.noiseRequested());
        assertEquals(List.of(
            new MoldormMotion.TailPosition(0, 0),
            new MoldormMotion.TailPosition(0, 0),
            new MoldormMotion.TailPosition(0, 0),
            new MoldormMotion.TailPosition(0, 0)), started.segments());
    }

    @Test
    void activeMotionUsesSixFrameMoldormAngleCadenceAndReportsWallCollision()
            throws IOException {
        MoldormMotion motion = new MoldormMotion();
        RoomEntity moldorm = moldorm(loadRom());
        MoldormMotion.Update started = motion.advance(moldorm, 0, 0x10, 0x00, 0x00, 0x04,
            sequence(0x00), null);

        MoldormMotion.Update moving = motion.advance(started.entity(), 1,
            0x0F, 0x00, 0x00, 0x04, sequence(0x00), null);
        assertEquals(0x41, moving.entity().x());
        assertEquals(0x06, moving.headSpriteVariant());

        MoldormMotion.Update blocked = motion.advance(moving.entity(), 2, 0x0E, 0x00, 0x00,
            0x04, sequence(0x01), (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY));
        assertEquals(0x41, blocked.entity().x());
        assertEquals(0x01, blocked.collisionFlags());
        assertEquals(0x10, blocked.transitionCountdown());
    }

    @Test
    void hitCountdownAndLowHealthRunTwoHistoryAndMovementPasses() throws IOException {
        MoldormMotion hitMotion = new MoldormMotion();
        RoomEntity moldorm = moldorm(loadRom());
        MoldormMotion.Update started = hitMotion.advance(moldorm, 0, 0x10,
            0, 0, 4, sequence(0), null);

        hitMotion.onSwordHit(0);
        MoldormMotion.Update hitAccelerated = hitMotion.advance(started.entity(), 1,
            started.transitionCountdown(), 0, 0, 4, sequence(0), null);

        assertEquals(3, hitAccelerated.inertia());
        assertEquals(0xC7, hitMotion.privateCountdown2(0));

        MoldormMotion lowHealthMotion = new MoldormMotion();
        MoldormMotion.Update lowHealth = lowHealthMotion.advance(moldorm, 0, 0x10,
            0, 0, 1, sequence(0), null);
        assertEquals(2, lowHealth.inertia());
    }

    @Test
    void flashingSkipsPositionUpdateButStillSteersFromBackgroundCollision()
            throws IOException {
        MoldormMotion motion = new MoldormMotion();
        RoomEntity moldorm = moldorm(loadRom());
        MoldormMotion.Update started = motion.advance(moldorm, 0, 0x10,
            0, 0, 4, sequence(0), null);
        AtomicInteger probes = new AtomicInteger();
        List<MoldormMotion.TailPosition> probePositions = new ArrayList<>();

        MoldormMotion.Update flashing = motion.advance(started.entity(), 1,
            started.transitionCountdown(), 0, 1, 4, sequence(1),
            (entity, direction, nextX, nextY) -> {
                probes.incrementAndGet();
                probePositions.add(new MoldormMotion.TailPosition(nextX, nextY));
                return EntityBackgroundCollisionResult.blocked(direction, 0x40, 0x40,
                    nextX, nextY);
            });

        assertEquals(0x40, flashing.entity().x());
        assertTrue(probes.get() > 0);
        assertTrue(probePositions.stream().allMatch(position ->
            position.x() == 0x40 && position.y() == 0x40));
        assertEquals(0x01, flashing.collisionFlags());
        assertEquals(0x10, flashing.transitionCountdown());
    }

    @Test
    void vulnerableTailTracksTheLatestCompletedHistoryUpdate() throws IOException {
        MoldormMotion motion = new MoldormMotion();
        MoldormMotion.Update update = motion.advance(moldorm(loadRom()), 0, 0x10,
            0, 0, 4, sequence(0), null);

        for (int frame = 1; frame <= 64; frame++) {
            update = motion.advance(update.entity(), frame, update.transitionCountdown(),
                0, 0, 4, sequence(0), null);
        }

        assertEquals(update.segments().get(3), motion.vulnerableTail(0));
    }

    @Test
    void exposesMoldormRomCombatAndBossMetadataInLiveRuntime() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_MOLDORM, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A), 0)),
            true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_MOLDORM));
        assertEquals(0x08, RoomEntityCombatRules.contactDamage(ENTITY_MOLDORM));
        assertEquals(0x04, RoomEntityCombatRules.initialHealth(ENTITY_MOLDORM));
        assertEquals(0x08, runtime.physicsFlags(0));
        assertEquals(0xD0, runtime.options1(0));
        assertEquals(0x80, runtime.hitboxFlagsForTest(0));
        assertEquals(0x04, runtime.enemyHealth(0));
    }

    @Test
    void liveTickPublishesMoldormDynamicPresentationAndMotionState() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_MOLDORM, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A), 0)),
            true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tickWithProjectileEvents(0, 0x20, 0x20, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x20, 0x20, 0, 0, 0, false));

        assertEquals(0x06, runtime.moldormHeadSpriteVariant(0));
        assertEquals(0x10, runtime.moldormTransitionCountdown(0));
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC,
            runtime.snapshot().slots().get(0).spriteDefinition().shape());
        assertEquals(16, runtime.snapshot().slots().get(0).spriteDefinition()
            .dynamicVariant(0).size());
        assertEquals(0x62, runtime.snapshot().slots().get(0).spriteDefinition()
            .dynamicVariant(0).get(2).oam().tile());
        assertEquals(0x02, runtime.physicsFlags(0));

        runtime.tickWithProjectileEvents(1, 0x20, 0x20, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x20, 0x20, 0, 0, 0, false));

        RoomEntity rendered = runtime.snapshot().slots().get(0);
        assertEquals(0x41, rendered.x());
        assertEquals(-9, rendered.spriteDefinition().dynamicVariant(0).get(0).xOffset());
        assertEquals(0x6A,
            rendered.spriteDefinition().dynamicVariant(0).get(2).oam().tile());
    }

    @Test
    void onlyTheFinalTailSegmentTakesSwordDamageAndStartsHitAcceleration()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_MOLDORM, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A), 0)),
            true, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.tickWithProjectileEvents(0, 0x20, 0x20, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x20, 0x20, 0, 0, 0, false));

        List<EntityCombatEvent> headClink = runtime.resolveCombat(1,
            0x20, 0x20, false, true, true, 0x40, 8, 0x40, 8);
        assertEquals(0x04, runtime.enemyHealth(0));
        assertTrue(headClink.get(0).swordPokeVfx() != null);

        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0);
        List<EntityCombatEvent> tailHit = runtime.resolveCombat(1,
            0x20, 0x20, false, true, true, 0, 8, 0, 8);

        assertEquals(1, tailHit.size());
        assertTrue(tailHit.get(0).swordHit());
        assertEquals(1, tailHit.get(0).enemyDamage());
        assertEquals(0x03, runtime.enemyHealth(0));
        assertEquals(0xC8, runtime.moldormPrivateCountdown2(0));
        assertEquals(0x28, runtime.enemyFlashCountdown(0));

        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0);
        List<EntityCombatEvent> flashingHeadClink = runtime.resolveCombat(2,
            0x20, 0x20, false, true, true, 0x40, 8, 0x40, 8);
        assertEquals(1, flashingHeadClink.size());
        assertTrue(flashingHeadClink.get(0).swordPokeVfx() != null);

        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0);
        List<EntityCombatEvent> flashingTailHit = runtime.resolveCombat(2,
            0x20, 0x20, false, true, true, 0, 8, 0, 8);
        assertTrue(flashingTailHit.isEmpty());
        assertEquals(0x03, runtime.enemyHealth(0));
    }

    @Test
    void linkMotionStateDoesNotFreezeMoldormHandler() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_MOLDORM, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A), 0)),
            true, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tickWithProjectileEvents(0, 0x20, 0x20, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x20, 0x20, 0, 0,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE, false));

        assertEquals(1, runtime.moldormInertia(0));
    }

    private static RoomEntity moldorm(byte[] rom) {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(ENTITY_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A);
        return new RoomEntity(0, 0, ENTITY_MOLDORM, 0x40, 0x40,
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
        try (var stream = MoldormRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
