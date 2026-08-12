package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GiantGopongaRuntimeTest {
    private static final int ENTITY_GIANT_GOPONGA_FLOWER = 0x7C;
    private static final int ENTITY_GOPONGA_FLOWER_PROJECTILE = 0x7D;

    @Test
    void decodesGiantRectangleAndProjectilePairsFromBankSixRomDisplayLists() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);

        EntitySpriteDefinition giant = catalog.forEntityType(
            ENTITY_GIANT_GOPONGA_FLOWER, EntityRoomLoader.RoomTable.OVERWORLD);
        assertTrue(giant.supported());
        assertEquals(0x06, giant.bank());
        assertEquals(0x6316, giant.address());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, giant.shape());
        assertEquals(3, giant.variantCount());
        assertEquals(8, giant.rectangleVariant(0).size());
        assertEquals(new EntitySpriteDefinition.RectangleSprite(-8, -8,
            new EntitySpriteDefinition.OamAttribute(0x70, 0x02)),
            giant.rectangleVariant(0).get(0));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(8, 16,
            new EntitySpriteDefinition.OamAttribute(0x74, 0x22)),
            giant.rectangleVariant(0).get(7));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(-8, -8,
            new EntitySpriteDefinition.OamAttribute(0x78, 0x02)),
            giant.rectangleVariant(2).get(0));
        assertEquals(new EntitySpriteDefinition.RectangleSprite(8, 16,
            new EntitySpriteDefinition.OamAttribute(0x7C, 0x22)),
            giant.rectangleVariant(2).get(7));

        EntitySpriteDefinition projectile = catalog.forEntityType(
            ENTITY_GOPONGA_FLOWER_PROJECTILE, EntityRoomLoader.RoomTable.OVERWORLD);
        assertTrue(projectile.supported());
        assertEquals(0x06, projectile.bank());
        assertEquals(0x638F, projectile.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, projectile.shape());
        assertEquals(4, projectile.variantCount());
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x1E, 0x02),
            new EntitySpriteDefinition.OamAttribute(0x1E, 0x62)), projectile.variant(0));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x1E, 0x42),
            new EntitySpriteDefinition.OamAttribute(0x1E, 0x22)), projectile.variant(1));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x32, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x32, 0x20)), projectile.variant(2));
        assertEquals(new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x30, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x30, 0x20)), projectile.variant(3));
    }

    @Test
    void usesRomCombatValuesAndTheGiantFlowerHitbox() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, ENTITY_GIANT_GOPONGA_FLOWER, 0x40, 0x40,
                EntityStatus.ACTIVE,
                catalog.forEntityType(ENTITY_GIANT_GOPONGA_FLOWER,
                    EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertEquals(0x04, runtime.enemyHealth(0));
        assertEquals(0x00, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        assertEquals(0x04, RoomEntityCombatRules.initialHealth(ENTITY_GIANT_GOPONGA_FLOWER));
        assertEquals(0x08, RoomEntityCombatRules.contactDamage(ENTITY_GIANT_GOPONGA_FLOWER));

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 0x4A, 0x40, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
    }

    @Test
    void advancesGiantStatesAndCreatesTheRomProjectileAtTheTimerSeam() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity giant = new RoomEntity(0, 0, ENTITY_GIANT_GOPONGA_FLOWER, 0x40, 0x40,
            EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_GIANT_GOPONGA_FLOWER,
                EntityRoomLoader.RoomTable.OVERWORLD), 0);
        GiantGopongaMotion motion = new GiantGopongaMotion();

        GiantGopongaMotion.Update stateOne = motion.advance(giant, 0, 0, 0x70, 0x50);
        assertEquals(1, stateOne.state());
        assertEquals(0xC0, stateOne.transitionCountdown());
        assertNull(stateOne.projectileSpawn());

        GiantGopongaMotion.Update stateTwo = motion.advance(giant, 0, 0, 0x70, 0x50);
        assertEquals(2, stateTwo.state());
        assertEquals(0x50, stateTwo.transitionCountdown());
        assertNull(stateTwo.projectileSpawn());

        GiantGopongaMotion.Update firing = motion.advance(giant, 0x00, 0x4A, 0x70, 0x50);
        assertEquals(2, firing.state());
        assertEquals(2, firing.entity().spriteVariant());
        assertNotNull(firing.projectileSpawn());
        assertEquals(0x40, firing.projectileSpawn().x());
        assertEquals(0x40, firing.projectileSpawn().y());
    }

    @Test
    void pushesLinkEvenWhenTheGiantFlowerSeesAnAirborneLink() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, ENTITY_GIANT_GOPONGA_FLOWER, 0x38, 0x40,
                EntityStatus.ACTIVE,
                catalog.forEntityType(ENTITY_GIANT_GOPONGA_FLOWER,
                    EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tickWithProjectileEvents(0, 0x40, 0x40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x40, 0x10, 0, 0, false));

        assertEquals(List.of(new RoomEntityRuntime.LinkFinalPositionRequest(0, true, true, false)),
            runtime.consumePendingLinkFinalPositionRequests());
    }

    @Test
    void roomRuntimeInsertsTheProjectileAsAHighestFreeDynamicEntity() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, ENTITY_GIANT_GOPONGA_FLOWER, 0x40, 0x40,
                EntityStatus.ACTIVE,
                catalog.forEntityType(ENTITY_GIANT_GOPONGA_FLOWER,
                    EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        for (int frame = 0; frame < 0xC8; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0);
        }

        RoomEntity projectile = runtime.snapshot().slots().stream()
            .filter(entity -> entity.type() == ENTITY_GOPONGA_FLOWER_PROJECTILE)
            .findFirst()
            .orElseThrow();
        assertEquals(15, projectile.slot());
        assertEquals(0x40, projectile.x());
        assertEquals(0x40, projectile.y());
        assertEquals(0x42, runtime.physicsFlags(projectile.slot()));
        assertEquals(0x02, runtime.options1(projectile.slot()));
        assertEquals(0x30, runtime.enemyHealth(projectile.slot()));
    }

    @Test
    void animatesAndMovesTheProjectileAccordingToItsHandlerTimers() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity projectile = new RoomEntity(0, -1, ENTITY_GOPONGA_FLOWER_PROJECTILE,
            0x40, 0x40, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_GOPONGA_FLOWER_PROJECTILE,
                EntityRoomLoader.RoomTable.OVERWORLD), 0);
        GopongaProjectileMotion motion = new GopongaProjectileMotion();
        motion.initializeSpawn(0, projectile, 0x70, 0x50);

        GopongaProjectileMotion.Update delayed = motion.advance(
            projectile, 0x00, 0x09, 0, true);
        assertEquals(3, delayed.entity().spriteVariant());
        assertEquals(0x40, delayed.entity().x());
        assertEquals(0x40, delayed.entity().y());
        assertTrue(!delayed.unloadRequested());

        GopongaProjectileMotion.Update ignored = motion.advance(
            projectile, 0x00, 0, 2, true);
        assertEquals(0x10, ignored.transitionCountdown());
        assertEquals(0x40, ignored.entity().x());
        assertEquals(0x40, ignored.entity().y());

        GopongaProjectileMotion.Update moved = motion.advance(
            projectile, 0x08, 0, 0, true);
        assertEquals(1, moved.entity().spriteVariant());
        GopongaProjectileMotion.Update movedAgain = motion.advance(
            moved.entity(), 0x08, 0, 0, true);
        assertTrue(movedAgain.entity().x() != 0x40 || movedAgain.entity().y() != 0x40);

        GopongaProjectileMotion.Update unloaded = motion.advance(
            projectile, 0x00, 1, 0, true);
        assertTrue(unloaded.unloadRequested());
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
        try (var stream = GiantGopongaRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
