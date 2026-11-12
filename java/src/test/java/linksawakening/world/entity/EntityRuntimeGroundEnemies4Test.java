package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGroundEnemies4Test extends EntityRuntimeTestSupport {

    @Test
    void snakeBackgroundCollisionResetsTheRomStateAndPrivateRecoveryWindow() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA1)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(
                runtime, 3, 120, 120, sequence(0x00), (entity, direction, nextX, nextY) -> true));

        assertEquals(0, runtime.enemies().snakeState(0));
        assertEquals(0x08, runtime.enemies().snakeTransitionCountdown(0));
        assertEquals(0x20, runtime.enemies().snakePrivateCountdown1(0));
        assertEquals(0x00, runtime.enemies().snakeSpeedX(0));
        assertEquals(0x00, runtime.enemies().snakeSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void snakeUsesHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA1)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(2, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void spikedBeetleUsesTheRomRestWalkAndAnimationStateMachine() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
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

        assertEquals(1, runtime.enemies().spikedBeetleState(0));
        assertEquals(0x30, runtime.enemies().spikedBeetleTransitionCountdown(0));
        assertEquals(0x06, runtime.enemies().spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.enemies().spikedBeetleSpeedY(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x48, runtime.options1(0));
        assertEquals(0x80, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));

        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0xFF)));
        }
        assertEquals(0, runtime.enemies().spikedBeetleState(0));
        assertEquals(0x18, runtime.enemies().spikedBeetleTransitionCountdown(0));
        assertEquals(0x06, runtime.enemies().spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.enemies().spikedBeetleSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x40, 120, 64, sequence(0xFF)));
        assertEquals(1, runtime.enemies().spikedBeetleState(0));
        assertEquals(0xFF, runtime.enemies().spikedBeetleTransitionCountdown(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0x41, 120, 64, sequence(0xFF)));
        assertEquals(2, runtime.enemies().spikedBeetleState(0));
        assertEquals(0xFF, runtime.enemies().spikedBeetleTransitionCountdown(0));
        assertEquals(0x00, runtime.enemies().spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.enemies().spikedBeetleSpeedY(0));
    }

    @Test
    void spikedBeetleRestingKeepsTheLastHandlerSelectedAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
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
        runtime.tick(EntityTestFrames.frame(runtime, 0xE1, 120, 80, sequence(0x00, 0x00)));
        for (int frame = 0xE2; frame != 0x12; frame = (frame + 1) & 0xFF) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0xFF)));
        }

        assertEquals(0, runtime.enemies().spikedBeetleState(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 0x20, 120, 80, sequence(0xFF)));
        runtime.tick(EntityTestFrames.frame(runtime, 0x21, 120, 80, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void spikedBeetleFlipsOnShieldContactAndDoesNotTakeDamage() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertEquals(0x48, runtime.options1(0));

        List<EntityCombatEvent> events = runtime.resolveCombat(new EnemyCombatFrame(
                0,
                56,
                64,
                false,
                0,
                true,
                false,
                0,
                0,
                0,
                0,
                new EnemyAttackContext(1, false, false, false, false, true, 0, 0)));

        assertEquals(1, events.size());
        assertFalse(events.get(0).swordHit());
        assertEquals(0, events.get(0).linkDamage());
        assertEquals(0x0C, events.get(0).linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x09, events.get(0).soundId());
        assertEquals(3, runtime.enemies().spikedBeetleState(0));
        assertEquals(0xFF, runtime.enemies().spikedBeetleTransitionCountdown(0));
        assertEquals(0x10, runtime.enemies().spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.enemies().spikedBeetleSpeedY(0));
        assertEquals(0x02, runtime.enemies().enemyHealth(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(0x08, runtime.options1(0));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
    }

    @Test
    void spikedBeetleShieldChecksAllFourDirections() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        int[] linkX = {56, 72, 64, 64};
        int[] linkY = {64, 64, 72, 59};
        int[] flipX = {0x10, 0xF0, 0, 0};
        int[] flipY = {0, 0, 0xF0, 0x10};
        for (int direction = 0; direction < 4; direction++) {
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                            .sourceLoadOrder(0)
                            .type(0x2C)
                            .at(64, 64)
                            .spriteDefinition(catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0))
                            .build()))
                    .indoorRoom(true)
                    .randomByteSupplier(null)
                    .spriteHandlers(catalog)
                    .enemyCombatTables(tables)
                    .build();
            runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0)));
            var away = new EnemyAttackContext(1, false, false, false, false, true, direction ^ 1, 0);
            var misses = runtime.resolveCombat(new EnemyCombatFrame(
                    1, linkX[direction], linkY[direction], false, 0, true, false, 0, 0, 0, 0, away));
            assertFalse(misses.stream().anyMatch(e -> e.soundId() == 0x09));
            assertNotEquals(3, runtime.enemies().spikedBeetleState(0));
            var shield = new EnemyAttackContext(1, false, false, false, false, true, direction, 0);
            var hits = runtime.resolveCombat(new EnemyCombatFrame(
                    1, linkX[direction], linkY[direction], false, 0, true, false, 0, 0, 0, 0, shield));
            assertTrue(hits.stream().anyMatch(e -> e.soundId() == 0x09));
            assertEquals(3, runtime.enemies().spikedBeetleState(0));
            assertEquals(flipX[direction], runtime.enemies().spikedBeetleSpeedX(0));
            assertEquals(flipY[direction], runtime.enemies().spikedBeetleSpeedY(0));
        }
    }

    @Test
    void spikedBeetleNormalHandlerUsesTheRomSwordPokeOptions() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
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
        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(2, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(64, 64), events.get(0).swordPokeVfx());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x07, events.get(0).soundId());
        assertEquals(1, runtime.enemies().spikedBeetleState(0));
        assertEquals(0x02, runtime.enemies().enemyHealth(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 80, sequence(0xFF)));
        assertEquals(0, runtime.enemies().spikedBeetleSpeedX(0));
        assertEquals(0, runtime.enemies().spikedBeetleSpeedY(0));
    }

    @Test
    void spikedBeetleUsesTheRomHealthAndContactDamageValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));

        assertEquals(1, events.size());
        assertEquals(0x04, events.get(0).linkDamage());
    }

    @Test
    void polsVoiceUsesTheRomJumpLoopAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

        assertEquals(1, runtime.enemies().polsVoiceState(0));
        assertEquals(0x10, runtime.enemies().polsVoiceSpeedZ(0));
        assertEquals(0x08, runtime.enemies().polsVoiceSpeedX(0));
        assertEquals(0xFC, runtime.enemies().polsVoiceSpeedY(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x08, runtime.options1(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 80, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x0F, runtime.enemies().polsVoiceSpeedZ(0));

        int frame = 3;
        while (runtime.enemies().polsVoiceState(0) == 1 && frame < 80) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0x03)));
            frame++;
        }
        assertEquals(2, runtime.enemies().polsVoiceState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0, runtime.enemies().polsVoiceSpeedX(0));
        assertEquals(0, runtime.enemies().polsVoiceSpeedY(0));
        assertEquals(0x1B, runtime.enemies().polsVoiceTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 80, sequence(0xFF)));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, frame + 1, 120, 80, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void polsVoiceForcesTheRomIgnoreHitsByteOnlyAroundBackgroundInteraction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        assertEquals(0x1F, runtime.enemies().dyingCountdown(0));
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x18, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void polsVoiceUsesTheRomHealthAndContactDamageValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
                runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));

        assertEquals(1, events.size());
        assertEquals(0x08, events.get(0).linkDamage());
        assertEquals(0x04, runtime.enemies().enemyHealth(0));
    }

    @Test
    void evasiveStalfosUsesTheRomRandomWalkAndNormalAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1E)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x01)));
        for (int frame = 1; frame <= 4; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x01)));
        }

        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 120, 120, sequence(0x01)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void evasiveStalfosJumpsAwayFromHeldActionButtonsAndLandsWithRomState() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.configuration().setActionButtonsHeld(true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));

        assertEquals(1, runtime.enemies().evasiveInertia(0));
        assertEquals(0x08, runtime.enemies().transitionCountdown(0));
        assertEquals(0x15, runtime.enemies().evasiveSpeedZ(0));
        assertEquals(0xEE, runtime.enemies().evasiveSpeedX(0));
        assertEquals(0x00, runtime.enemies().evasiveSpeedY(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.configuration().setActionButtonsHeld(false);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        assertEquals(62, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x14, runtime.enemies().evasiveSpeedZ(0));

        int frame = 2;
        while (runtime.enemies().evasiveInertia(0) != 0 && frame < 100) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }

        assertEquals(0, runtime.enemies().evasiveInertia(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x08, runtime.enemies().evasiveSpeedX(0));
        assertEquals(0x08, runtime.enemies().evasiveSpeedY(0));
        assertEquals(0x10, runtime.enemies().evasivePrivateCountdown1(0));
    }

    @Test
    void evasiveStalfosCanBeHitInMidairAfterItsEightFrameJumpGracePeriod() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x1E, 3))
                        .build()))
                .build();
        runtime.configuration().setActionButtonsHeld(true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0)));
        runtime.configuration().setActionButtonsHeld(false);
        for (int frame = 0; frame < 8; frame++) {
            if (frame != 0) runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0)));
            RoomEntity entity = runtime.inspection().snapshot().slots().get(0);
            assertTrue(runtime.resolveCombat(new EntityAttackContactFrame(
                            frame, 0, 0, false, 0, true, true, entity.x() - 8, 16, entity.y() - entity.z() - 8, 16))
                    .isEmpty());
        }
        runtime.tick(EntityTestFrames.frame(runtime, 8, 80, 64, sequence(0)));
        RoomEntity airborne = runtime.inspection().snapshot().slots().get(0);
        assertTrue(airborne.z() > 0);
        assertEquals(1, runtime.enemies().evasiveInertia(0));
        assertEquals(0, runtime.enemies().transitionCountdown(0));
        var hits = runtime.resolveCombat(new EntityAttackContactFrame(
                8, 0, 0, false, 0, true, true, airborne.x() - 8, 16, airborne.y() - airborne.z() - 8, 16));
        assertTrue(hits.stream().anyMatch(hit -> hit.swordHit() && hit.enemyDamage() > 0));
    }

    @Test
    void evasiveStalfosClonesInAnglersTunnelWithRomAttributesAndVector() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .z(0x03)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x03);
        // AnimateEntities decrements private countdowns before dispatching the
        // handler; raw value two therefore enters the source's ==1 branch.
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));

        RoomEntity clone = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, clone.status());
        assertEquals(-1, clone.sourceLoadOrder());
        assertEquals(0x1E, clone.type());
        assertEquals(0x40, clone.x());
        assertEquals(0x50, clone.y());
        assertEquals(0x03, clone.z());
        assertEquals(1, runtime.enemies().evasivePrivateState1(15));
        assertEquals(0x52, runtime.enemies().physicsFlags(15));
        assertEquals(0x1A, runtime.options1(15));
        assertEquals(0x00, runtime.enemies().enemyIgnoreHitsCountdown(15));
        // GetVectorTowardsLink($18) sees dx=$30 and dy=$03 (source Z is
        // included in the Y distance), producing X=$18, Y=$01.
        assertEquals(0x18, runtime.enemies().evasiveSpeedX(15));
        assertEquals(0x01, runtime.enemies().evasiveSpeedY(15));
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.getFirst().soundChannel());
        assertEquals(0x0A, events.getFirst().soundId());
    }

    @Test
    void evasiveStalfosCloneSkipsGenericGroundInteractionButKeepsFleeingMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x03);
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);
        List<Integer> groundInteractionSlots = new ArrayList<>();
        runtime.configuration().setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
            groundInteractionSlots.add(entity.slot());
            return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));
        groundInteractionSlots.clear();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x70, 0x50, sequence(0x01)));

        assertFalse(groundInteractionSlots.contains(15));
    }

    @Test
    void evasiveStalfosCloneIsGatedBelowAnglersTunnelAndDoesNotWhoosh() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x02);
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x01, runtime.enemies().evasivePrivateCountdown1(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void evasiveStalfosCloneDoesNotEmitWhooshWhenAllSlotsAreOccupied() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        List<RoomEntity> entities = new ArrayList<>();
        entities.add(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1E)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            entities.add(new RoomEntityBuilder()
                    .slot(slot)
                    .type(0xFF)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0xFF))
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(entities)).build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x03);
        EntityRuntimeFixture.debug(runtime).setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x01)));

        assertEquals(0x1E, runtime.inspection().snapshot().slots().get(0).type());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }
}
