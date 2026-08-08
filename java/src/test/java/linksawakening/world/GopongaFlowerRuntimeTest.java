package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GopongaFlowerRuntimeTest {
    private static final int ENTITY_GOPONGA_FLOWER = 0x7E;

    @Test
    void decodesGopongaFlowerPairsFromItsBankSixRomDisplayList() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(ENTITY_GOPONGA_FLOWER, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(0x06, definition.bank());
        assertEquals(0x63F4, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(2, definition.variantCount());
        assertEquals(0, definition.initialVariant());
        assertEquals(0x50, definition.variant(0).first().tile());
        assertEquals(0x02, definition.variant(0).first().attributes());
        assertEquals(0x50, definition.variant(0).second().tile());
        assertEquals(0x22, definition.variant(0).second().attributes());
        assertEquals(0x52, definition.variant(1).first().tile());
        assertEquals(0x02, definition.variant(1).first().attributes());
        assertEquals(0x52, definition.variant(1).second().tile());
        assertEquals(0x22, definition.variant(1).second().attributes());
    }

    @Test
    void alternatesOpenAndClosedFlowersOnTheRomFrameMask() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, ENTITY_GOPONGA_FLOWER, 0x40, 0x40,
                EntityStatus.ACTIVE,
                catalog.forEntityType(ENTITY_GOPONGA_FLOWER,
                    EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tick(0x00, 0, 0, () -> 0);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(0x10, 0, 0, () -> 0);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(0x30, 0, 0, () -> 0);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(0x40, 0, 0, () -> 0);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void usesRomPhysicsOptionsHealthAndContactDamage() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, ENTITY_GOPONGA_FLOWER, 0x40, 0x40,
                EntityStatus.ACTIVE,
                catalog.forEntityType(ENTITY_GOPONGA_FLOWER,
                    EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        assertEquals(0x04, runtime.enemyHealth(0));
        assertEquals(0x02, runtime.physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        assertEquals(0x04, RoomEntityCombatRules.initialHealth(ENTITY_GOPONGA_FLOWER));
        assertEquals(0x08, RoomEntityCombatRules.contactDamage(ENTITY_GOPONGA_FLOWER));

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 0x40, 0x40, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
    }

    @Test
    void requestsLinkFinalPositionEvenWhenLinkIsInTheAir() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, ENTITY_GOPONGA_FLOWER, 0x38, 0x40,
                EntityStatus.ACTIVE,
                catalog.forEntityType(ENTITY_GOPONGA_FLOWER,
                    EntityRoomLoader.RoomTable.OVERWORLD), 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tickWithProjectileEvents(0, 0x40, 0x40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x40, 0x10, 0, 0, false));

        assertEquals(List.of(new RoomEntityRuntime.LinkFinalPositionRequest(0)),
            runtime.consumePendingLinkFinalPositionRequests());
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
        try (var stream = GopongaFlowerRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
