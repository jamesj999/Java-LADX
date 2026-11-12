package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeBombArrowsTest extends EntityRuntimeTestSupport {

    @Test
    void bombArrowCollisionIsHarmlessButSetsTheTargetTransitionCountdown() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition moblinDefinition = pairDefinition(0x0B, 8);
        RoomEntity moblin = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(15)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(moblinDefinition)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(moblin))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        int candidateBomb = runtime.spawnBomb(0x40, 0x50, 0, 0);
        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertTrue(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(candidateBomb).status());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));

        assertEquals(2, runtime.enemies().enemyHealth(15));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x03, runtime.enemies().transitionCountdown(15));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(arrowSlot).status());
        assertTrue(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
    }

    @Test
    void shootingWithinTheBombArrowWindowRemovesTheLatestBombAndMarksTheArrow() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        assertEquals(0x06, EntityRuntimeFixture.inspection(runtime).bombArrowCooldownForTest());

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(bombSlot).status());
        assertTrue(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).bombArrowCooldownForTest());
        assertFalse(runtime.inspection().lastArrowShotPlayedWhoosh());
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(arrowSlot)
                        .spriteDefinition()
                        .shape());
    }

    @Test
    void placingWithinTheBombArrowWindowMarksTheLatestArrowButKeepsTheBomb() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 1);
        assertEquals(0x06, EntityRuntimeFixture.inspection(runtime).bombArrowCooldownForTest());

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 1);

        assertTrue(runtime.inspection().snapshot().slots().get(bombSlot).loaded());
        assertTrue(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).bombArrowCooldownForTest());
        assertEquals(0x10, EntityRuntimeFixture.inspection(runtime).bombPrivateCountdown1ForTest(bombSlot));
    }

    @Test
    void bombArrowCooldownExpiresAfterSixEntityFrames() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertFalse(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
        for (int frame = 0; frame < 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x50, () -> 0));
        }

        assertEquals(0, EntityRuntimeFixture.inspection(runtime).bombArrowCooldownForTest());
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        assertTrue(runtime.inspection().snapshot().slots().get(bombSlot).loaded());
        assertFalse(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
        assertEquals(0x06, EntityRuntimeFixture.inspection(runtime).bombArrowCooldownForTest());
    }

    @Test
    void bombArrowSpawnsTheSourceBombExplosionAfterAWallTransition() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertTrue(EntityRuntimeFixture.inspection(runtime).isBombArrowForTest(arrowSlot));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(bombSlot).status());

        runtime.tick(EntityTestFrames.frame(
                runtime, 0, 0x40, 0x50, () -> 0, (entity, direction, nextX, nextY) -> direction == 0));
        assertEquals(
                0x18, new EntityRoomInspectionQueries(runtime.inspection()).playerArrowTransitionCountdown(arrowSlot));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));

        RoomEntity explosion = runtime.inspection().snapshot().loadedEntities().getFirst();
        assertEquals(0x02, explosion.type());
        assertEquals(0x17, runtime.enemies().transitionCountdown(explosion.slot()));
        assertEquals(
                List.of(new EntityCombatEvent(
                        explosion.slot(), 0x02, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C)),
                runtime.consumePendingEntityEvents());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(arrowSlot).status());
    }
}
