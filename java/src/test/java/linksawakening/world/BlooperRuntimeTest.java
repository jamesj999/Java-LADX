package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlooperRuntimeTest {

    @Test
    void catalogReadsBlooperPairFromItsBankSevenDisplayList() throws IOException {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(EntitySpriteHandlerCatalog.ENTITY_BLOOPER,
                EntityRoomLoader.RoomTable.INDOORS_A);

        assertEquals(0x07, definition.bank());
        assertEquals(0x5BF1, definition.address());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(2, definition.variantCount());
        assertEquals(0, definition.initialVariant());
        assertEquals(0x6A, definition.variant(0).first().tile());
        assertEquals(0x00, definition.variant(0).first().attributes());
        assertEquals(0x6A, definition.variant(0).second().tile());
        assertEquals(0x20, definition.variant(0).second().attributes());
        assertEquals(0x68, definition.variant(1).first().tile());
        assertEquals(0x68, definition.variant(1).second().tile());
    }

    @Test
    void blooperUsesTheRomNormalEnemyCombatGroup() {
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xA9));
        assertEquals(0x04, RoomEntityCombatRules.contactDamage(0xA9));
        assertEquals(0x01, RoomEntityCombatRules.initialHealth(0xA9));
    }

    @Test
    void runtimeRunsBlooperAfterTheCurrentFrameGroundHelper() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xA9,
            EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA9, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, () -> 0, catalog, tables);
        AtomicInteger groundCalls = new AtomicInteger();
        runtime.setGroundInteraction((entity, frameCounter, previousStatus, speedZ,
                                       sideScrolling) -> {
            groundCalls.incrementAndGet();
            return RoomEntityGroundInteraction.Result.unchanged(entity, 0x01);
        });

        runtime.tick(0, 48, 40, () -> 0);
        assertEquals(1, runtime.blooperState(0));
        assertEquals(0x25, runtime.blooperTransitionCountdown(0));
        assertEquals(1, runtime.blooperDirection(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(1, 48, 40, () -> 0);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(2, 48, 40, () -> 0);
        assertEquals(0xFF, runtime.blooperSpeedX(0));
        assertEquals(0xFF, runtime.blooperSpeedY(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(3, groundCalls.get());
        assertEquals(0x02, runtime.physicsFlags(0));
        assertEquals(0x01, runtime.enemyHealth(0));
    }

    @Test
    void nonInteractiveLinkMotionStopsBlooperBeforeRecoilMovementAndGroundInteraction()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xA9,
            EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA9, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, () -> 0, catalog, tables);
        AtomicInteger groundCalls = new AtomicInteger();
        runtime.setGroundInteraction((entity, frameCounter, previousStatus, speedZ,
                                       sideScrolling) -> {
            groundCalls.incrementAndGet();
            return RoomEntityGroundInteraction.Result.unchanged(entity, 0x01);
        });

        runtime.tickWithProjectileEvents(0, 48, 40, () -> 0, null,
            new EnemyProjectileCollision.LinkState(48, 40, 0,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE, 0, false));

        assertEquals(0, runtime.blooperState(0));
        assertEquals(0, runtime.blooperTransitionCountdown(0));
        assertEquals(0, runtime.blooperSpeedX(0));
        assertEquals(0, runtime.blooperSpeedY(0));
        assertEquals(0, groundCalls.get());
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
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
        try (var stream = BlooperRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
