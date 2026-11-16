package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class PolsVoiceRuntimeTest {

    @Test
    void polsVoiceUsesTheRomJumpLoopAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x18)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 80, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 80, sequence(0x00, 0x00)));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceSpeedZ(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceSpeedX(0));
        assertEquals(0xFC, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceSpeedY(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x08, runtime.execution().slotProperties().options1(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 80, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x0F, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceSpeedZ(0));

        int frame = 3;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceState(0) == 1 && frame < 80) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0x03)));
            frame++;
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceSpeedY(0));
        assertEquals(0x1B, new EntityEnemyInspectionQueries(runtime.enemies()).polsVoiceTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0xFF)));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, frame + 1, 120, 80, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void polsVoiceForcesTheRomIgnoreHitsByteOnlyAroundBackgroundInteraction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x18)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 80, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 80, sequence(0x00, 0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 80, sequence(0xFF)));

        assertTrue(observedIgnoreHits.contains(0x01));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void polsVoiceUsesTheRomBalladOcarinaDeathPreamble() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x18)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(0x01, 0x04, 0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 80, sequence(0x00)));

        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x18, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13)),
                runtime.outbox().consumePendingEntityEvents());
    }

    @Test
    void polsVoiceUsesTheRomHealthAndContactDamageValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x18)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));

        assertEquals(1, events.size());
        assertEquals(0x08, events.get(0).linkDamage());
        assertEquals(0x04, runtime.enemies().enemyHealth(0));
    }
}
