package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeArrowsTest extends EntityRuntimeTestSupport {

    @Test
    void enemyArrowAndRockPlayThePokeJingleOnceWhenHittingAWall() {
        for (int type : new int[] {0x0A, 0x0C}) {
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                            snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                    .type(type)
                                    .at(0x40, 0x50)
                                    .spriteDefinition(pairDefinition(type, 4))
                                    .build()))
                    .build();
            RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;
            runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, wall));
            assertEquals(
                    List.of(new EntityCombatEvent(0, type, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x07)),
                    runtime.consumePendingEntityEvents());

            runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0, wall));
            assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        }
    }

    @Test
    void shieldBlockedEnemyArrowAndRockKeepTheTingWithoutAddingAPokeJingle() {
        for (int type : new int[] {0x0A, 0x0C}) {
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                            snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                    .type(type)
                                    .at(0x40, 0x50)
                                    .spriteDefinition(pairDefinition(type, 4))
                                    .build()))
                    .build();
            List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                    runtime,
                    0,
                    0x40,
                    0x50,
                    () -> 0,
                    null,
                    new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 1, true)));

            assertEquals(1, events.size());
            assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, events.get(0).kind());
            assertEquals(0x16, events.get(0).soundId());
            assertEquals(0x18, runtime.enemies().enemyProjectileTransitionCountdown(0));
            assertEquals(4, runtime.enemies().swordMoblinAlertingSoundCounter());
            assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        }
    }

    @Test
    void runtimePublishesArrowTransitionVariantsButLeavesRockVariantUnspun() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition arrowDefinition = catalog.forEntityType(0x0C, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime arrowRuntime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .type(0x0C)
                                .at(0x20, 0x20)
                                .spriteDefinition(arrowDefinition)
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;

        arrowRuntime.tick(EntityTestFrames.frame(arrowRuntime, 0, 0, 0, () -> 0, wall));
        assertEquals(0x18, arrowRuntime.enemies().enemyProjectileTransitionCountdown(0));
        arrowRuntime.tick(EntityTestFrames.frame(arrowRuntime, 1, 0, 0, () -> 0, wall));
        assertEquals(0x17, arrowRuntime.enemies().enemyProjectileTransitionCountdown(0));
        assertEquals(0, arrowRuntime.inspection().snapshot().slots().get(0).spriteVariant());
        for (int frame = 2; frame <= 9; frame++) {
            arrowRuntime.tick(EntityTestFrames.frame(arrowRuntime, frame, 0, 0, () -> 0, wall));
        }
        assertEquals(0x0F, arrowRuntime.enemies().enemyProjectileTransitionCountdown(0));
        assertEquals(1, arrowRuntime.inspection().snapshot().slots().get(0).spriteVariant());
        arrowRuntime.tick(EntityTestFrames.frame(arrowRuntime, 10, 0, 0, () -> 0, wall));
        assertEquals(3, arrowRuntime.inspection().snapshot().slots().get(0).spriteVariant());

        EntitySpriteDefinition rockDefinition = catalog.forEntityType(0x0A, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime rockRuntime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .type(0x0A)
                                .at(0x20, 0x20)
                                .spriteDefinition(rockDefinition)
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();
        rockRuntime.tick(EntityTestFrames.frame(rockRuntime, 0, 0, 0, () -> 0, wall));
        rockRuntime.tick(EntityTestFrames.frame(rockRuntime, 1, 0, 0, () -> 0, wall));
        assertEquals(0, rockRuntime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void spawnPlayerArrowUsesLinkStateAndTheRomArrowDisplayList() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x03, 0x6BC6, 0x2E, 0x21, 0x2C, 0x21, 0x2C, 0x01, 0x2E, 0x01, 0x2A, 0x41, 0x2A, 0x61, 0x2A, 0x01,
                0x2A, 0x21);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int slot = runtime.spawnArrow(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity arrow = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x00, arrow.type());
        assertEquals(-1, arrow.sourceLoadOrder());
        assertEquals(EntityStatus.ACTIVE, arrow.status());
        assertEquals(0x40, arrow.x());
        assertEquals(0x50, arrow.y());
        assertEquals(0x08, arrow.z());
        assertEquals(0x03, arrow.spriteVariant());
        assertEquals(0x03, arrow.spriteDefinition().bank());
        assertEquals(0x6BC6, arrow.spriteDefinition().address());
        assertEquals(0x42, runtime.enemies().physicsFlags(slot));
        assertEquals(0x12, runtime.options1(slot));
        assertEquals(1, runtime.enemies().enemyIgnoreHitsCountdown(slot));
        // ShootArrow calls label_140F after SpawnPlayerProjectile. The
        // initial $20 movement table is replaced by data_13AD/data_13B5.
        assertEquals(0x30, runtime.inspection().playerArrowSpeedY(slot));
        assertEquals(3, runtime.inspection().playerArrowDirection(slot));
        assertEquals(1, runtime.inspection().activePlayerArrowCount());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertEquals(0x53, runtime.inspection().snapshot().slots().get(slot).y());
    }

    @Test
    void pieceOfPowerSelectsTheAlternateShootArrowSpeedTable() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();

        int slot = runtime.spawnArrow(0x40, 0x50, 0, 3, true);

        assertEquals(0x40, runtime.inspection().playerArrowSpeedY(slot));
        assertEquals(0x00, runtime.inspection().playerArrowSpeedX(slot));
    }

    @Test
    void ordinaryPlayerArrowRunsTheRomEntityDamagePassBeforeMoving() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition octorokDefinition = pairDefinition(0x09, 8);
        RoomEntity octorok = new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(15)
                .type(0x09)
                .at(0x40, 0x50)
                .spriteDefinition(octorokDefinition)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(octorok))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertEquals(14, arrowSlot);

        // $75A2 checks (hFrameCounter XOR targetSlot) bit 0. Slot 15 is
        // therefore checked on frame 1. The arrow collision runs before its
        // own movement, exactly as ArrowEntityHandler does.
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));

        assertEquals(0, runtime.enemies().enemyHealth(15));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(15));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(15));
        assertEquals(
                List.of(new EntityCombatEvent(
                        15,
                        0x09,
                        0,
                        false,
                        1,
                        -1,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x03,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        null)),
                runtime.consumePendingEntityEvents());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(arrowSlot).status());
        // func_003_77A7 copies the active arrow's speed directly into the
        // target recoil velocity, even on a lethal hit.
        assertEquals(0x30, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedXForTest(15));
        assertEquals(0x00, EntityRuntimeFixture.enemies(runtime).enemyRecoilSpeedYForTest(15));
    }

    @Test
    void reboundingPlayerArrowDoesNotRunTheEnemyDamagePass() throws IOException {
        RoomEntity target = new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(15)
                .type(0x09)
                .at(0x40, 0x50)
                .spriteDefinition(pairDefinition(0x09, 8))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(target))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(new RomEnemyCombatTables(loadRom()))
                .build();
        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);

        // Target slot 15 is skipped by the even-frame damage cadence. The
        // arrow hits a wall instead, then overlaps the target on frame 1.
        runtime.tick(EntityTestFrames.frame(
                runtime, 0, 0, 0, () -> 0, (entity, direction, x, y) -> entity.type() == 0 && direction == 0));
        assertEquals(0x18, runtime.inspection().playerArrowTransitionCountdown(arrowSlot));
        runtime.consumePendingEntityEvents();
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));

        assertEquals(1, runtime.enemies().enemyHealth(15));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(arrowSlot).status());
        assertEquals(0x17, runtime.inspection().playerArrowTransitionCountdown(arrowSlot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void playerArrowWallImpactStartsTheSharedMoblinAlertTimer() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x03, 0x6BC6, 0x2E, 0x21, 0x2C, 0x21, 0x2C, 0x01, 0x2E, 0x01, 0x2A, 0x41, 0x2A, 0x61, 0x2A, 0x01,
                0x2A, 0x21);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawnArrow(0x40, 0x50, 0x00, 0);

        runtime.tick(
                EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> direction == 0));

        assertEquals(0x04, runtime.enemies().swordMoblinAlertingSoundCounter());
        assertEquals(0x18, runtime.inspection().playerArrowTransitionCountdown(slot));
        assertEquals(0xF4, runtime.inspection().playerArrowSpeedX(slot));
        assertEquals(
                List.of(new EntityCombatEvent(slot, 0x00, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x07)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void playerArrowRuntimeDisplaysThePreviousReboundSpinFrame() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();
        int slot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        runtime.tick(
                EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> direction == 0));

        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
            int expected = frame == 1 ? 0 : frame <= 9 ? 1 : 3;
            assertEquals(
                    expected, runtime.inspection().snapshot().slots().get(slot).spriteVariant(), "frame " + frame);
        }
    }

    @Test
    void playerArrowReboundUsesTheSideViewRoomGravity() {
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(snapshot().withSideScrolling(true)).build();
        int slot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        runtime.tick(
                EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> direction == 0));
        int startingZ = runtime.inspection().snapshot().slots().get(slot).z();
        int startingY = runtime.inspection().snapshot().slots().get(slot).y();

        for (int frame = 1; frame <= 9; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        RoomEntity arrow = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, arrow.status());
        assertEquals(startingZ, arrow.z());
        assertEquals(startingY + 4, arrow.y());
        assertEquals(18, runtime.inspection().playerArrowSpeedY(slot));
    }

    @Test
    void playerArrowCountIsCappedAtTwoAndSlotsCanBeReused() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();

        int first = runtime.spawnArrow(0x40, 0x50, 0, 0);
        int second = runtime.spawnArrow(0x40, 0x50, 0, 1);

        assertEquals(15, first);
        assertEquals(14, second);
        assertEquals(-1, runtime.spawnArrow(0x40, 0x50, 0, 2));
        assertEquals(2, runtime.inspection().activePlayerArrowCount());

        runtime.clearEntity(first);

        assertEquals(15, runtime.spawnArrow(0x40, 0x50, 0, 2));
        assertEquals(2, runtime.inspection().activePlayerArrowCount());
    }
}
