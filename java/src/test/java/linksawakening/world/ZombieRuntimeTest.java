package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ZombieRuntimeTest {
    private static final int ENTITY_ZOMBIE = 0xBF;

    @Test
    void decodesZombiesFiveRomSpritePairs() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(0x18, definition.bank());
        assertEquals(0x63F8, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(5, definition.variantCount());
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0xFF, 0xFF),
            new EntitySpriteDefinition.OamAttribute(0xFF, 0xFF)), definition.variant(0));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x6C, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x6C, 0x22)), definition.variant(1));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x68, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x6A, 0x02)), definition.variant(2));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x60, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x62, 0x02)), definition.variant(3));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x64, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x66, 0x02)), definition.variant(4));
    }

    @Test
    void usesTheRomZombieCombatAndPhysicsRows() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_ZOMBIE, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_ZOMBIE));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_ZOMBIE));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(ENTITY_ZOMBIE));
        assertEquals(0x52, runtime.physicsFlags(0));
        assertEquals(0x00, runtime.options1(0));
        assertEquals(0x01, runtime.enemyHealth(0));
    }

    @Test
    void hiddenParentSelectsAnAllowedRomCoordinateAndRequestsAChild() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD);
        ZombieMotion motion = new ZombieMotion();
        RoomEntity parent = new RoomEntity(0, 0, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
        AtomicInteger randomCalls = new AtomicInteger();

        ZombieMotion.Update update = motion.advance(parent, 0, 0x00, 0x52,
            0x70, 0x50,
            () -> randomCalls.getAndIncrement() == 0 ? 0x03 : 0x12,
            entity -> new RoomEntityObjectSample(0x00, 0x00, 0x40, 0x40),
            null, 0);

        assertEquals(0, update.state());
        assertEquals(0, update.privateState1());
        assertEquals(0x52, update.transitionCountdown());
        assertEquals(0x48, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(-1, update.entity().spriteVariant());
        assertFalse(update.appliesBackgroundInteraction());
        assertNotNull(update.spawnRequest());
        assertEquals(0x48, update.spawnRequest().x());
        assertEquals(0x40, update.spawnRequest().y());
        assertEquals(2, randomCalls.get());
    }

    @Test
    void childFollowsTheRomEmergenceAndVectorStates() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD);
        ZombieMotion motion = new ZombieMotion();
        motion.initializeChild(1);
        RoomEntity child = new RoomEntity(1, -1, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        ZombieMotion.Update emerging = motion.advance(child, 0, 0x00, 0x12,
            0x70, 0x50, () -> 0, null, null, 0);
        assertEquals(1, emerging.state());
        assertEquals(0x30, emerging.transitionCountdown());
        assertEquals(0, emerging.entity().spriteVariant());

        ZombieMotion.Update visible = motion.advance(emerging.entity(), 0, 0x30, 0x12,
            0x70, 0x50, () -> 0, null, null, 0);
        assertEquals(1, visible.state());
        assertEquals(0x30, visible.transitionCountdown());
        assertEquals(0, visible.entity().spriteVariant());
        assertFalse(motion.allowsEnemyCollision(1));
        assertFalse(visible.appliesBackgroundInteraction());

        ZombieMotion.Update moving = motion.advance(visible.entity(), 0, 0x00, 0x12,
            0x70, 0x50, () -> 0, null, null, 0);
        assertEquals(2, moving.state());
        assertEquals(0x70, moving.transitionCountdown());
        assertEquals(1, moving.entity().spriteVariant());
        assertEquals(0x05, moving.speedX());
        assertEquals(0x01, moving.speedY());
        assertTrue(motion.allowsEnemyCollision(1));
        assertFalse(moving.appliesBackgroundInteraction());

        ZombieMotion.Update movingFrame = motion.advance(moving.entity(), 0, 0x70, 0x12,
            0x70, 0x50, () -> 0, null, null, 0);
        assertTrue(movingFrame.appliesBackgroundInteraction());
        assertEquals(2, movingFrame.entity().spriteVariant());
    }

    @Test
    void blockedMovementOrExpiredTimerStartsTheRomHiddenState() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD);
        ZombieMotion motion = new ZombieMotion();
        motion.initializeChild(0);
        RoomEntity child = new RoomEntity(0, -1, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        ZombieMotion.Update state0 = motion.advance(child, 0, 0, 0x12,
            0x70, 0x50, () -> 0, null, null, 0);
        ZombieMotion.Update state1 = motion.advance(state0.entity(), 0, 0x00, 0x12,
            0x70, 0x50, () -> 0, null, null, 0);
        ZombieMotion.Update state2 = motion.advance(state1.entity(), 0, 0x00, 0x12,
            0x70, 0x50, () -> 0, null,
            (entity, direction, nextX, nextY) ->
                EntityBackgroundCollisionResult.blocked(direction, 0x01, 0x01,
                    nextX, nextY), 0);

        assertEquals(3, state2.state());
        assertEquals(0x30, state2.transitionCountdown());
        assertEquals(0x52, state2.physicsFlags());
        assertEquals(2, state2.entity().spriteVariant());
        assertTrue(state2.appliesBackgroundInteraction());

        ZombieMotion.Update cleared = motion.advance(state2.entity(), 0, 0, 0x52,
            0x70, 0x50, () -> 0, null, null, 0);
        assertTrue(cleared.unloadRequested());
        assertFalse(cleared.appliesBackgroundInteraction());
    }

    @ParameterizedTest
    @CsvSource({"112, 64, 0, 2", "16, 64, 1, 1",
        "64, 16, 2, 2", "64, 112, 3, 2"})
    void newlyWalkingZombieProbesWallsBeforeAccumulatingAFullPixel(
        int linkX, int linkY, int blockedDirection, int emergenceVariant) throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD);
        ZombieMotion motion = new ZombieMotion();
        motion.initializeChild(0);
        RoomEntity child = new RoomEntity(0, -1, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
        ZombieMotion.Update rising = motion.advance(child, 0, 0, 0x12,
            linkX, linkY, () -> 0, null, null, 0);
        ZombieMotion.Update walking = motion.advance(rising.entity(), 0, 0, 0x12,
            linkX, linkY, () -> 0, null, null, 0);
        List<Integer> probes = new ArrayList<>();

        ZombieMotion.Update blocked = motion.advance(walking.entity(), 0, 0x70, 0x12,
            linkX, linkY, () -> 0, null, (entity, direction, x, y) -> {
                probes.add(direction);
                return EntityBackgroundCollisionResult.blocked(direction, 1, 1, x, y);
            }, 0);

        assertEquals(List.of(blockedDirection), probes);
        assertEquals(3, blocked.state());
        assertEquals(0x30, blocked.transitionCountdown());
        assertEquals(0x52, blocked.physicsFlags());
        assertEquals(0x40, blocked.entity().x());
        assertEquals(0x40, blocked.entity().y());
        // At $6447 the transition branch compares the X-speed byte returned
        // by ApplyVectorTowardsLink, not the new $70 countdown. That selected
        // frame is drawn by the first walking pass, before frame 3 is stored.
        assertEquals(emergenceVariant, blocked.entity().spriteVariant());
    }

    @Test
    void emergenceWalkingAndBurrowingPresentThePreviouslySelectedRomFrame() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD);
        ZombieMotion motion = new ZombieMotion();
        motion.initializeChild(0);
        RoomEntity child = new RoomEntity(0, -1, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
        int[] countdowns = {0, 0x30, 0x18, 0x17, 0, 0x70, 0x6F, 0,
            0x30, 0x18, 0x17, 1};
        int[] frames = {0, 1, 2, 3, 4, 0, 16, 17, 18, 19, 20, 21};
        int[] variants = {0, 0, 1, 1, 2, 2, 3, 4, 4, 2, 2, 1};
        for (int i = 0; i < countdowns.length; i++) {
            ZombieMotion.Update update = motion.advance(child, frames[i], countdowns[i], 0x12,
                0x70, 0x50, () -> 0, null, null, 0);
            child = update.entity();
            assertEquals(variants[i], child.spriteVariant(), "Handler pass " + i);
        }
    }

    @Test
    void roomRuntimeSpawnsTheZombieChildInTheRomRange() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity parent = new RoomEntity(0, 0, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD), 0);
        AtomicInteger randomCalls = new AtomicInteger();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(parent), false,
            () -> randomCalls.getAndIncrement() == 0 ? 0x03 : 0x12,
            catalog, new RomEnemyCombatTables(rom));
        runtime.setObjectQuery(entity -> new RoomEntityObjectSample(0x00, 0x00,
            entity.x(), entity.y()));

        runtime.tick(0, 0x70, 0x50, () -> randomCalls.getAndIncrement() == 0 ? 0x03 : 0x12);

        RoomEntity child = runtime.snapshot().slots().get(5);
        assertEquals(EntityStatus.ACTIVE, child.status());
        assertEquals(ENTITY_ZOMBIE, child.type());
        assertEquals(0x48, child.x());
        assertEquals(0x40, child.y());
        assertEquals(0, child.spriteVariant());
        assertEquals(0x12, runtime.physicsFlags(5));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(5));
        assertFalse(runtime.snapshot().slots().get(0).spriteVariant() >= 0);

        runtime.tick(1, 0x70, 0x50, () -> 0);
        runtime.tick(2, 0x70, 0x50, () -> 0);
        assertEquals(0, runtime.snapshot().slots().get(5).spriteVariant());
        runtime.tick(3, 0x70, 0x50, () -> 0);
        assertEquals(1, runtime.snapshot().slots().get(5).spriteVariant());
    }

    @Test
    void hiddenParentDoesNotRunTheRomEnemyDamageCollisionHandler() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity parent = new RoomEntity(0, 0, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(parent), false,
            () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(runtime.resolveCombat(1, 0x40, 0x40, false, true,
            false, 0, 0, 0, 0).isEmpty());
    }

    @Test
    void roomInitializationKeepsTheHiddenParentOutOfTheDisplayList() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity parent = new RoomEntity(0, 0, ENTITY_ZOMBIE, 0x40, 0x40,
            EntityStatus.INIT,
            catalog.forEntityType(ENTITY_ZOMBIE, EntityRoomLoader.RoomTable.OVERWORLD), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(parent), false,
            () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tick(0, 0x70, 0x50, () -> 0);

        assertFalse(runtime.snapshot().slots().get(0).spriteVariant() >= 0);
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
        try (var stream = ZombieRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
