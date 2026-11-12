package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class WingedOctorokRuntimeTest {

    @Test
    void catalogReadsTheBankSevenPairListAndTheHandlerCombatGroup() throws IOException {
        EntitySpriteDefinition definition =
                new EntitySpriteHandlerCatalog(loadRom()).forEntityType(0xAE, EntityRoomLoader.RoomTable.OVERWORLD);

        assertEquals(0x07, definition.bank());
        assertEquals(0x562D, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(8, definition.variantCount());
        assertEquals(0x50, definition.variant(0).first().tile());
        assertEquals(0x02, definition.variant(0).first().attributes());
        assertEquals(0x22, definition.variant(0).second().attributes());
        assertEquals(0x42, definition.variant(2).first().attributes());
        assertEquals(0x62, definition.variant(2).second().attributes());
        assertEquals(0x54, definition.variant(4).first().tile());
        assertEquals(0x56, definition.variant(4).second().tile());
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xAE));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(0xAE));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(0xAE));
    }

    @Test
    void liveRuntimeAdvancesTheRomStateAndPublishesTheGeneratedRect() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xAE, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xAE)
                        .at(0x40, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x40, () -> 0x02));

        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).wingedOctorokState(0));
        assertEquals(0x12, runtime.enemies().transitionCountdown(0));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).wingedOctorokSpeedX(0));
        assertEquals(
                2,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .wingedOctorokOam()
                        .size());
        assertEquals(
                0x22,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .wingedOctorokOam()
                        .get(0)
                        .tileIndex());
        assertEquals(
                0x40,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .wingedOctorokOam()
                        .get(0)
                        .attributes());
    }

    @Test
    void liveRuntimeSpawnsTheRomVariantOneOctorokRockAtCountdownTen() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xAE, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xAE)
                        .at(0x40, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x60, 0x40, () -> 0));
        }

        RoomEntity rock = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x0A)
                .findFirst()
                .orElseThrow();
        assertEquals(1, rock.spriteVariant());
        assertEquals(0x48, rock.x());
        assertEquals(0x40, rock.y());
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
