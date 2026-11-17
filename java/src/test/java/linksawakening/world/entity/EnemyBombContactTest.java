package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EnemyBombContactTest {

    @Test
    void enemyBombExplosionUsesTheSourceLinkCollisionBranch() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        int bombSlot = runtime.enemySpawns().spawnEnemyBomb(0x48, 0x50, 0x00, 0x13);
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x50,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x00),
                0x10,
                0xF0));

        assertEquals(
                List.of(new EntityProjectileEvent(
                        bombSlot,
                        0x02,
                        EntityProjectileEvent.Kind.LINK_DAMAGE,
                        0x00,
                        0x08,
                        EntityProjectileEvent.SoundChannel.WAVE,
                        0x03,
                        false,
                        false,
                        0x00,
                        0x00,
                        0x28,
                        0x28,
                        0x10)),
                events);
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                runtime.outbox().consumeBombExplosionEvents().stream()
                        .map(BombExplosionEvent::targetSlot)
                        .toList());
    }

    @Test
    void enemyBombUsesRawEntityYInsteadOfExplosionVisualYForLinkCollision() throws IOException {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(TestRoms.partial(rom)))
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        int bombSlot = runtime.enemySpawns().spawnEnemyBomb(0x48, 0x68, 0x18, 0x13);
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x50,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x00),
                0x10,
                0xF0));

        assertEquals(List.of(), events);
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        List<BombExplosionEvent> explosionEvents = runtime.outbox().consumeBombExplosionEvents();
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                explosionEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(bombSlot, explosionEvents.getFirst().bombSlot());
    }

    @Test
    void protectedEnemyBombStillDoublesLinkSpeedWithoutDamageOrHurtSound() throws IOException {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(TestRoms.partial(rom)))
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();

        int bombSlot = runtime.enemySpawns().spawnEnemyBomb(0x48, 0x50, 0x00, 0x13);
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x50,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x01),
                0x10,
                0xF0));

        assertEquals(
                List.of(new EntityProjectileEvent(
                        bombSlot,
                        0x02,
                        EntityProjectileEvent.Kind.LINK_DAMAGE,
                        0x00,
                        0x00,
                        EntityProjectileEvent.SoundChannel.NONE,
                        -1,
                        false,
                        false,
                        0x00,
                        0x00,
                        0x20,
                        0xE0,
                        0x00)),
                events);
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        List<BombExplosionEvent> explosionEvents = runtime.outbox().consumeBombExplosionEvents();
        assertEquals(
                List.of(BombExplosionEvent.OBJECT_TARGET),
                explosionEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(bombSlot, explosionEvents.getFirst().bombSlot());
    }
}
