package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class CuccoRuntimeTest {

    @Test
    void decodesCuccoDisplayListFromBankFive() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
                .forEntityType(EntitySpriteHandlerCatalog.ENTITY_CUCCO, EntityRoomLoader.RoomTable.OVERWORLD);

        assertEquals(0x05, definition.bank());
        assertEquals(0x4514, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(4, definition.variantCount());
        assertEquals(0x50, definition.variant(0).first().tile());
        assertEquals(0x01, definition.variant(0).first().attributes());
        assertEquals(0x56, definition.variant(3).first().tile());
        assertEquals(0x21, definition.variant(3).first().attributes());
    }

    @Test
    void routesCuccoThroughItsHarmlessHandlerAndPowerBraceletLift() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition =
                catalog.forEntityType(EntitySpriteHandlerCatalog.ENTITY_CUCCO, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(EntitySpriteHandlerCatalog.ENTITY_CUCCO)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        assertEquals(0x92, runtime.enemies().physicsFlags(0));
        assertEquals(0x02, runtime.options1(0));
        runtime.configuration().setActionButtonsHeld(true, false);
        runtime.configuration().setLikeLikeLinkInventory(0x03, 0x00);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertEquals(
                EntityStatus.LIFTED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .noneMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE));
    }

    @Test
    void harmlessPhysicsSuppressesCuccoContactDamageButKeepsEnemyCollision() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition =
                catalog.forEntityType(EntitySpriteHandlerCatalog.ENTITY_CUCCO, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(EntitySpriteHandlerCatalog.ENTITY_CUCCO)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x00, 0x00, () -> 0));
        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x50, false, true, false, 0, 0, 0, 0));

        assertFalse(events.isEmpty());
        assertEquals(0, events.getFirst().linkDamage());
        assertEquals(0x4C, runtime.enemies().enemyHealth(0));
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
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
