package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ZoraRuntimeTest {
    private static final int ENTITY_ZORA = 0xCB;
    private static final int ENTITY_GOPONGA_FLOWER_PROJECTILE = 0x7D;

    @Test
    void decodesZorasFiveRomSpritePairs() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZORA, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(0x18, definition.bank());
        assertEquals(0x49C0, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0, definition.initialVariant());
        assertEquals(5, definition.variantCount());
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0xFF, 0xFF),
            new EntitySpriteDefinition.OamAttribute(0xFF, 0xFF)), definition.variant(0));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x54, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x54, 0x62)), definition.variant(1));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x54, 0x42),
            new EntitySpriteDefinition.OamAttribute(0x54, 0x22)), definition.variant(2));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x56, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x56, 0x22)), definition.variant(3));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x52, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x52, 0x22)), definition.variant(4));
    }

    @Test
    void usesZorasRomCombatAndPhysicsMetadata() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_ZORA, 0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_ZORA, EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(ENTITY_ZORA));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(ENTITY_ZORA));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(ENTITY_ZORA));
        assertEquals(0x02, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        assertEquals(0x01, runtime.enemyHealth(0));
    }

    @Test
    void stateZeroChoosesRomWaterCoordinateAndStartsRandomTimer() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZORA, EntityRoomLoader.RoomTable.OVERWORLD);
        ZoraMotion motion = new ZoraMotion();
        RoomEntity zora = new RoomEntity(0, 0, ENTITY_ZORA, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
        AtomicInteger randomCalls = new AtomicInteger();

        ZoraMotion.Update update = motion.advance(zora, 0, 0x02, 0x20, 0x20,
            () -> randomCalls.getAndIncrement() == 0 ? 0x03 : 0x15,
            entity -> new RoomEntityObjectSample(0x00, 0x07, 0x40, 0x40));

        assertEquals(1, update.state());
        assertEquals(0x55, update.transitionCountdown());
        assertEquals(0x42, update.physicsFlags());
        assertEquals(0x48, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(2, randomCalls.get());
    }

    @Test
    void stateTwoRemovesProjectileNoclipAndStateThreeUsesRomZTable() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZORA, EntityRoomLoader.RoomTable.OVERWORLD);
        ZoraMotion motion = new ZoraMotion();
        RoomEntity zora = new RoomEntity(0, 0, ENTITY_ZORA, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        ZoraMotion.Update submerged = motion.advance(zora, 0, 0x02, 0x20, 0x20, () -> 0,
            entity -> new RoomEntityObjectSample(0x00, 0x07, 0x40, 0x40));
        ZoraMotion.Update stateTwo = motion.advance(submerged.entity(), 1, 0x42, 0x20, 0x20,
            () -> 0, entity -> RoomEntityObjectSample.none());
        assertEquals(1, stateTwo.state());
        assertEquals(0x01, stateTwo.transitionCountdown());
        assertEquals(0x42, stateTwo.physicsFlags());

        stateTwo = motion.advance(stateTwo.entity(), 0, 0x42, 0x20, 0x20,
            () -> 0, entity -> RoomEntityObjectSample.none());
        assertEquals(2, stateTwo.state());
        assertEquals(0x60, stateTwo.transitionCountdown());
        assertEquals(0x42, stateTwo.physicsFlags());

        ZoraMotion.Update stateThree = motion.advance(stateTwo.entity(), 0,
            stateTwo.physicsFlags(), 0x20, 0x20, () -> 0,
            entity -> RoomEntityObjectSample.none());
        assertEquals(3, stateThree.state());
        assertEquals(0x60, stateThree.transitionCountdown());
        assertEquals(0x02, stateThree.physicsFlags());

        ZoraMotion.Update firing = motion.advance(stateThree.entity(), 0x58,
            stateThree.physicsFlags(), 0x58, 0x20, () -> 0,
            entity -> RoomEntityObjectSample.none());
        assertEquals(3, firing.state());
        assertEquals(0x00, firing.entity().z());
        assertEquals(3, firing.entity().spriteVariant());
        assertNull(firing.projectileSpawn());

        ZoraMotion.Update projectileFrame = motion.advance(firing.entity(), 0x30,
            firing.physicsFlags(), 0x30, 0x70, () -> 0,
            entity -> RoomEntityObjectSample.none());
        assertEquals(4, projectileFrame.entity().spriteVariant());
        assertEquals(0x00, projectileFrame.entity().z());
        assertNotNull(projectileFrame.projectileSpawn());
        assertEquals(0x18, projectileFrame.projectileSpawn().x());
        assertEquals(0x10, projectileFrame.projectileSpawn().y());
        assertEquals(0x06, projectileFrame.projectileSpawn().speedX());
        assertEquals(0x18, projectileFrame.projectileSpawn().speedY());

        ZoraMotion.Update hidden = motion.advance(projectileFrame.entity(), 0,
            projectileFrame.physicsFlags(), 0x00, 0x70, () -> 0,
            entity -> RoomEntityObjectSample.none());
        assertEquals(0, hidden.state());
        assertEquals(0, hidden.entity().spriteVariant());
        assertTrue(hidden.splash());
    }

    @Test
    void usesThePreIncrementInertiaForTheRomZTable() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_ZORA, EntityRoomLoader.RoomTable.OVERWORLD);
        ZoraMotion motion = new ZoraMotion();
        RoomEntity zora = new RoomEntity(0, 0, ENTITY_ZORA, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);

        ZoraMotion.Update submerged = motion.advance(zora, 0, 0x02, 0x20, 0x20, () -> 0,
            entity -> new RoomEntityObjectSample(0x00, 0x07, 0x40, 0x40));
        ZoraMotion.Update stateTwo = motion.advance(submerged.entity(), 0, 0x42,
            0x20, 0x20, () -> 0, entity -> RoomEntityObjectSample.none());
        ZoraMotion.Update stateThree = motion.advance(stateTwo.entity(), 0, 0x42,
            0x20, 0x20, () -> 0, entity -> RoomEntityObjectSample.none());

        ZoraMotion.Update boundary = stateThree;
        for (int frame = 0; frame < 16; frame++) {
            boundary = motion.advance(boundary.entity(), 0x60, 0x02,
                0x20, 0x20, () -> 0, entity -> RoomEntityObjectSample.none());
        }
        assertEquals(0x00, boundary.entity().z());
    }

    @Test
    void roomRuntimeSpawnsZoraProjectileInHighestFreeSlot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity zora = new RoomEntity(0, 0, ENTITY_ZORA, 0x40, 0x40,
            EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_ZORA, EntityRoomLoader.RoomTable.OVERWORLD), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(zora), false,
            () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.setObjectQuery(entity -> new RoomEntityObjectSample(0x00, 0x07, 0x40, 0x40));

        for (int frame = 0; frame <= 0xD0; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0);
        }

        RoomEntity projectile = runtime.snapshot().slots().stream()
            .filter(entity -> entity.type() == ENTITY_GOPONGA_FLOWER_PROJECTILE)
            .findFirst().orElseThrow();
        assertEquals(15, projectile.slot());
        assertEquals(0x18, projectile.x());
        assertEquals(0x0E, projectile.y());
        assertEquals(0x42, runtime.physicsFlags(projectile.slot()));
        assertEquals(0x30, runtime.enemyHealth(projectile.slot()));

        runtime.tick(0xD4, 0x70, 0x50, () -> 0);
        RoomEntity flashedProjectile = runtime.snapshot().slots().stream()
            .filter(entity -> entity.type() == ENTITY_GOPONGA_FLOWER_PROJECTILE)
            .findFirst().orElseThrow();
        assertEquals(0x10, flashedProjectile.entityFlipAttribute());
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
        try (var stream = ZoraRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
