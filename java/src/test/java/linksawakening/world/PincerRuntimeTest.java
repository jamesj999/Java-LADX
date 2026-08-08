package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PincerRuntimeTest {

    @Test
    void catalogReadsPincersTenPairDisplayListFromBankSeven() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0xB0, EntityRoomLoader.RoomTable.OVERWORLD);

        assertEquals(0x07, definition.bank());
        assertEquals(0x542B, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(10, definition.variantCount());
        assertEquals(0xFF, definition.variant(0).first().tile());
        assertEquals(0x6C, definition.variant(1).first().tile());
        assertEquals(0x64, definition.variant(2).first().tile());
        assertEquals(0x62, definition.variant(2).second().tile());
        assertEquals(0x02, definition.variant(4).first().attributes());
    }

    @Test
    void pincerUsesRomGroupTwoCombatValues() {
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xB0));
        assertEquals(0x08, RoomEntityCombatRules.contactDamage(0xB0));
        assertEquals(0x02, RoomEntityCombatRules.initialHealth(0xB0));
    }

    @Test
    void nonInteractiveLinkMotionLeavesPincerInItsHiddenInitialState() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xB0,
            EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xB0, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, () -> 0, catalog, tables);

        runtime.tickWithProjectileEvents(0, 48, 40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(48, 40, 0,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE, 0, false));

        assertEquals(0, runtime.pincerState(0));
        assertEquals(0, runtime.transitionCountdown(0));
        assertEquals(0, runtime.pincerSpeedX(0));
        assertEquals(0, runtime.pincerSpeedY(0));
        assertEquals(0x02, runtime.physicsFlags(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertEquals(List.of(), runtime.pincerBodyOam());
    }

    @Test
    void liveRuntimePublishesTheGeneratedBodyAfterTheLungeStarts() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xB0,
            EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xB0, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, () -> 0, catalog, tables);

        runtime.tick(0, 64, 64, () -> 0);
        runtime.tick(1, 64, 64, () -> 0);
        for (int frame = 2; frame < 50; frame++) {
            runtime.tick(frame, 96, 64, () -> 0);
        }

        assertEquals(3, runtime.pincerState(0));
        assertEquals(3, runtime.pincerBodyOam().size());
        assertEquals(0x6A, runtime.pincerBodyOam().get(0).tileIndex());
        assertEquals(0x02, runtime.pincerBodyOam().get(0).attributes());
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
        try (var stream = PincerRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
