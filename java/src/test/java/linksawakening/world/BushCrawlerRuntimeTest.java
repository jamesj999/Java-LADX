package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BushCrawlerRuntimeTest {

    @Test
    void catalogReadsOutdoorIndoorAndLiftedBushCrawlerListsFromBankSeven() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition outdoor = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_BUSH_CRAWLER,
            EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x07, outdoor.bank());
        assertEquals(0x4012, outdoor.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, outdoor.shape());
        assertEquals(2, outdoor.variantCount());
        assertEquals(0xF4, outdoor.variant(0).first().tile());
        assertEquals(0x16, outdoor.variant(0).first().attributes());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
            outdoor.pairTileSource());
        assertEquals(0xF0, outdoor.variant(1).first().tile());
        assertEquals(0x17, outdoor.variant(1).first().attributes());

        EntitySpriteDefinition indoor = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_BUSH_CRAWLER,
            EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x401A, indoor.address());
        assertEquals(0xF0, indoor.variant(1).first().tile());
        assertEquals(0x16, indoor.variant(1).first().attributes());

        EntitySpriteDefinition lifted = catalog.forBushCrawlerState(
            0x02, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x400A, lifted.address());
        assertEquals(0x7C, lifted.variant(0).first().tile());
        assertEquals(0x7E, lifted.variant(0).second().tile());
        assertEquals(0x7E, lifted.variant(1).first().tile());
        assertEquals(0x22, lifted.variant(1).first().attributes());

        EntitySpriteDefinition crawl = catalog.forBushCrawlerCrawlState(
            EntityRoomLoader.RoomTable.OVERWORLD, 0);
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, crawl.shape());
        assertEquals(2, crawl.variantCount());
        assertEquals(4, crawl.dynamicVariant(0).size());
        assertEquals(0xF4, crawl.dynamicVariant(0).get(0).oam().tile());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
            crawl.dynamicVariant(0).get(0).tileSource());
        assertEquals(0x7C, crawl.dynamicVariant(0).get(2).oam().tile());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
            crawl.dynamicVariant(0).get(2).tileSource());
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xBB));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(0xBB));
        // health_groups.asm assigns group $00; the handler writes $04 on
        // each ordinary interactive pass before default collision.
        assertEquals(0x00, RoomEntityCombatRules.initialHealth(0xBB));
    }

    @Test
    void liveRuntimeStartsTheThirtyFrameCrawlAndPublishesItsSecondPass() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xBB,
            EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xBB, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tick(0, 0x40, 0x47, () -> 0);
        assertEquals(1, runtime.bushCrawlerState(0));
        assertEquals(0x30, runtime.transitionCountdown(0));
        assertEquals(0x10, runtime.bushCrawlerSpeedY(0));
        assertEquals(0, runtime.snapshot().visualYOffset(0));

        runtime.tick(1, 0x00, 0x00, () -> 0);
        assertEquals(0x41, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.snapshot().visualYOffset(0));

        runtime.tick(8, 0x00, 0x00, () -> 0);
        RoomEntity crawling = runtime.snapshot().slots().get(0);
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, crawling.spriteDefinition().shape());
        assertEquals(4, crawling.spriteDefinition().dynamicVariant(0).size());
        assertEquals(-4, runtime.snapshot().visualYOffset(0));
    }

    @Test
    void powerBraceletLiftConvertsTheSourceAndSpawnsThePrivateStateTwoCrawler()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xBB,
            EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xBB, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.setActionButtonsHeld(true, false);
        runtime.setLikeLikeLinkInventory(0x03, 0x00);

        runtime.tick(0, 0x40, 0x38, () -> 0);

        RoomEntity carried = runtime.snapshot().slots().get(0);
        RoomEntity replacement = runtime.snapshot().loadedEntities().stream()
            .filter(entity -> entity.type() == 0xBB && entity.slot() != 0)
            .findFirst()
            .orElseThrow();
        assertEquals(0x05, carried.type());
        assertEquals(EntityStatus.LIFTED, carried.status());
        assertEquals(1, carried.spriteVariant());
        assertEquals(0xBB, replacement.type());
        assertEquals(EntityStatus.ACTIVE, replacement.status());
        assertEquals(0x40, runtime.transitionCountdown(replacement.slot()));
        assertEquals(0, runtime.liftedEntityState().carryState());

        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.WAVE, events.get(0).soundChannel());
        assertEquals(0x02, events.get(0).soundId());
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
        try (var stream = BushCrawlerRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
