package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeRollingBonesTest extends EntityRuntimeTestSupport {

    @Test
    void rollingBonesBarContactUsesHorizontalRecoilOnBothSides() {
        for (int linkX : new int[] {0x3C, 0x44}) {
            var enemy = new RoomEntityBuilder()
                    .sourceLoadOrder(0)
                    .type(0x82)
                    .at(0x40, 0x50)
                    .spriteDefinition(pairDefinition(0x82, 2))
                    .build();
            var runtime = RoomEntityRuntime.builder(snapshot(enemy)).build();
            var events =
                    runtime.resolveCombat(new EntitySwordContactFrame(1, linkX, 0x58, false, true, false, 0, 0, 0, 0));
            assertEquals(1, events.size());
            assertEquals(
                    new EntityCombatEvent.LinkCollisionResponse(linkX < 0x40 ? 0xE8 : 0x18, 0, 0x10),
                    events.get(0).linkCollisionResponse());
        }
    }

    @Test
    void rollingBonesLaunchesTheBarAfterTheRomPreparationDelay() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bar = new RoomEntityBuilder()
                .sourceLoadOrder(1)
                .type(0x82)
                .at(0x38, 0x38)
                .spriteDefinition(catalog.forEntityType(0x82, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntity boss = new RoomEntityBuilder()
                .slot(1)
                .sourceLoadOrder(0)
                .type(0x81)
                .at(0x47, 0x38)
                .spriteDefinition(catalog.forEntityType(0x81, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(bar, boss))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x48, () -> 0));
        assertEquals(0x18, runtime.enemies().transitionCountdown(1));

        for (int frame = 1; frame <= 0x18; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x48, () -> 0));
        }

        assertEquals(1, EntityRuntimeFixture.bosses(runtime).rollingBonesBarStateForTest(0));
        assertEquals(-0x10, EntityRuntimeFixture.bosses(runtime).rollingBonesBarSpeedXForTest(0));
        assertTrue(runtime.inspection().snapshot().slots().get(0).x() < 0x38);
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .shape());

        for (int frame = 0x19; frame <= 0x1C; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x48, () -> 0));
        }
        RoomEntity animatedBar = runtime.inspection().snapshot().slots().get(0);
        assertEquals(
                catalog.forRollingBonesBar(0, animatedBar.y(), 1).dynamicVariant(0),
                animatedBar.spriteDefinition().dynamicVariant(0));
        assertEquals(0, animatedBar.spriteVariant());
    }

    @Test
    void rollingBonesRecoilSkipsItsApproachAndJumpPhysics() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bar = new RoomEntityBuilder()
                .sourceLoadOrder(1)
                .type(0x82)
                .at(56, 64)
                .spriteDefinition(catalog.forEntityType(0x82, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntity boss = new RoomEntityBuilder()
                .slot(1)
                .sourceLoadOrder(0)
                .type(0x81)
                .at(71, 64)
                .spriteDefinition(catalog.forEntityType(0x81, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(bar, boss))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(1, 87, 64, 0x30);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(1, 10);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 72, () -> 0));
        assertEquals(68, runtime.inspection().snapshot().slots().get(1).x());
        assertEquals(0, runtime.enemies().transitionCountdown(1), "Recoil must not also enter the prepare-roll state");
        assertEquals(9, runtime.enemies().enemyIgnoreHitsCountdown(1));
    }

    @Test
    void rollingBonesNewSwordHitStartsRecoilOnTheFollowingHandlerFrame() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bar = new RoomEntityBuilder()
                .sourceLoadOrder(1)
                .type(0x82)
                .at(56, 64)
                .spriteDefinition(catalog.forEntityType(0x82, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntity boss = new RoomEntityBuilder()
                .slot(1)
                .sourceLoadOrder(0)
                .type(0x81)
                .at(71, 64)
                .spriteDefinition(catalog.forEntityType(0x81, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(bar, boss))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.resolveCombat(new EntitySwordContactFrame(0, 87, 64, false, true, true, 71, 8, 64, 8));
        assertEquals(7, runtime.enemies().enemyHealth(1));
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 72, () -> 0));
        assertEquals(71, runtime.inspection().snapshot().slots().get(1).x());
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(1));
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(1));
        assertEquals(
                0x18,
                runtime.enemies().transitionCountdown(1),
                "The hit frame still executes the ordinary approach handler");
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 72, () -> 0));
        assertEquals(68, runtime.inspection().snapshot().slots().get(1).x());
        assertEquals(9, runtime.enemies().enemyIgnoreHitsCountdown(1));
        for (int frame = 2; frame < 24; frame++) runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 72, () -> 0));
        RoomEntity recovered = runtime.inspection().snapshot().slots().get(1);
        assertEquals(1, runtime.enemies().enemyFlashCountdown(1));
        runtime.resolveCombat(
                new EntitySwordContactFrame(24, 87, 64, false, true, true, recovered.x(), 8, recovered.y(), 8));
        assertEquals(
                6,
                runtime.enemies().enemyHealth(1),
                "The collision phase sees the flash timer decremented for this frame");
    }

    @Test
    void rollingBonesDeathSetsTailCaveMinibossEventAndRemovesTheBar() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bar = new RoomEntityBuilder()
                .slot(14)
                .sourceLoadOrder(1)
                .type(0x82)
                .at(0x38, 0x38)
                .spriteDefinition(catalog.forEntityType(0x82, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntity boss = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x81)
                .at(0x58, 0x58)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x81, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .spriteVariant(-1)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(bar, boss))
                .indoorRoom(true)
                .randomByteSupplier(sequence(0x00, 0x1F))
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x00);
        runtime.configuration().setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.configuration().setEnemyDropPlayerState(3, 4, false);
        runtime.entityState.slotView.setOptions1Override(15, 0x80);
        runtime.initializeBossBattle(boss);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, sequence(0x00, 0x1F)));
        assertEquals(0xFF, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, runtime.enemies().enemyFlashCountdown(15));

        for (int frame = 1; frame <= 0xFF; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, sequence(0x00, 0x1F)));
        }

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(0x20, runtime.consumePendingRoomStatusMask());
        assertEquals(1, runtime.consumePendingClearedEntityMask());
        var fairy = runtime.inspection().snapshot().loadedEntities().stream()
                .filter(e -> e.type() == 0x2F)
                .findFirst()
                .orElseThrow();
        assertEquals(boss.x(), fairy.x());
        assertEquals(boss.y(), fairy.y());

        EntityRuntimeFixture.debug(runtime).setEntityRoomStatusForTest(0x20);
        runtime.tick(EntityTestFrames.frame(runtime, 0x100, 0, 0, () -> 0));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(14).status());
    }

    @Test
    void rollingBonesBarSkipsAirborneLinkAndClinksAGroundedSword() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bar = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x82)
                .at(0x50, 0x50)
                .spriteDefinition(catalog.forEntityType(0x82, EntityRoomLoader.RoomTable.INDOORS_A, -1))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(bar))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();

        assertTrue(runtime.resolveCombat(
                        new EntitySwordContactFrame(1, 0x50, 0x50, true, true, true, 0x50, 0x10, 0x50, 0x10))
                .isEmpty());

        List<EntityCombatEvent> events = runtime.resolveCombat(
                new EntitySwordContactFrame(1, 0xC0, 0xC0, false, true, true, 0x50, 0x10, 0x50, 0x10));

        assertEquals(1, events.size());
        assertEquals(0, events.get(0).enemyDamage());
        assertNotNull(events.get(0).swordPokeVfx());
        assertEquals(0x07, events.get(0).soundId());
    }
}
