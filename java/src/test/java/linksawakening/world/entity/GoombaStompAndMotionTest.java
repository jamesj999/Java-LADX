package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GoombaMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GoombaStompAndMotionTest {

    @Test
    void goombaUsesTheRomNormalRoomRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        // The room init is a noop for Goomba; its first active handler pass
        // chooses the random-walk interval and direction.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00, 0x00)));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0xFF)));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x10, 120, 120, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void ordinaryWalkEndsInAnotherPauseAndKeepsTryingTheSameDirectionAtAWall() {
        var runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(EntityRuntimeTestSupport.pairDefinition(0x9F, 3))
                        .build()))
                .build();
        var enemies = new EntityEnemyInspectionQueries(runtime.enemies());
        runtime.tick(EntityTestFrames.frame(runtime, 1, 100, 100, () -> 0));
        RoomEntityBackgroundCollision wall = (entity, direction, x, y) -> direction == 0;
        for (int step = 1; step <= 0x30; step++) {
            runtime.tick(EntityTestFrames.frame(runtime, 1 + step, 100, 100, () -> 0, wall));
        }
        assertEquals(0, enemies.goombaState(0));
        assertEquals(0x20, enemies.goombaTransitionCountdown(0));
        assertEquals(8, enemies.goombaSpeedX(0));
        assertEquals(0, enemies.goombaSpeedY(0));
        assertEquals(64, runtime.inspection().slots().get(0).x());
        assertEquals(64, runtime.inspection().slots().get(0).y());
    }

    @Test
    void sideViewPatrolProbesFractionalStepsAndAnimatesOnlyOnTheFloor() {
        var runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x9F)
                                .at(88, 112)
                                .spriteDefinition(EntityRuntimeTestSupport.pairDefinition(0x9F, 3))
                                .build())
                        .withSideScrolling(true))
                .build();
        var enemies = new EntityEnemyInspectionQueries(runtime.enemies());
        runtime.tick(EntityTestFrames.frame(runtime, 86, 120, 16, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 87, 120, 16, () -> 0));
        assertEquals(88, runtime.inspection().slots().get(0).x());
        assertEquals(2, enemies.goombaSpeedY(0));
        assertEquals(0, runtime.inspection().slots().get(0).spriteVariant());
        RoomEntityBackgroundCollision corner = (entity, direction, x, y) -> direction == 0 || direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 88, 120, 16, () -> 0, corner));
        assertEquals(88, runtime.inspection().slots().get(0).x());
        assertEquals(115, runtime.inspection().slots().get(0).y());
        assertEquals(0xF8, enemies.goombaSpeedX(0));
        assertEquals(0, enemies.goombaSpeedY(0));
        assertEquals(1, runtime.inspection().slots().get(0).spriteVariant());
    }

    @Test
    void sideViewRecoilHoldsThePatrolVelocityAndSkipsOrdinaryMotionForAllTenTicks() {
        var runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x9F)
                                .at(104, 115)
                                .spriteDefinition(EntityRuntimeTestSupport.pairDefinition(0x9F, 3))
                                .build())
                        .withSideScrolling(true))
                .build();
        var enemies = new EntityEnemyInspectionQueries(runtime.enemies());
        RoomEntityBackgroundCollision floor = (entity, direction, x, y) -> direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 214, 120, 112, () -> 0, floor));
        runtime.tick(EntityTestFrames.frame(runtime, 215, 120, 112, () -> 0, floor));
        runtime.enemies()
                .motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configureFromSpeed(0, 0xE0, 6);
        runtime.inspection().combatTables().writeEnemyIgnoreHitsCountdown(0, 10);
        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 215, 120, 112, () -> 0, floor));
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        runtime.configuration().setDialogActive(false);
        for (int step = 1; step <= 10; step++) {
            runtime.tick(EntityTestFrames.frame(runtime, 215 + step, 120, 112, () -> 0, floor));
            var entity = runtime.inspection().slots().get(0);
            assertEquals(104 - 2 * step, entity.x());
            assertEquals(115, entity.y());
            assertEquals(10 - step, runtime.enemies().enemyIgnoreHitsCountdown(0));
            assertEquals(8, enemies.goombaSpeedX(0));
            assertEquals(0, enemies.goombaSpeedY(0));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 226, 120, 112, () -> 0, floor));
        assertEquals(85, runtime.inspection().slots().get(0).x());
    }

    @Test
    void goombaUsesRomHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(2, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void goombaStompUsesTheRomDescendingVelocityBranchAndSharedDeathHandler() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0xF0, true, false, 0, 0, 0, 0));
        assertEquals(1, events.size());
        assertEquals(
                EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN,
                events.get(0).linkAction());
        assertEquals(EntityCombatEvent.SoundChannel.WAVE, events.get(0).soundChannel());
        assertEquals(0x0E, events.get(0).soundId());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());

        runtime.configuration().setDialogActive(true);
        for (int frame = 0; frame < 60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0)));
        }
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).goombaTransitionCountdown(0));
        runtime.configuration().setDialogActive(false);
        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
            if (frame == 0) assertEquals(0xC2, runtime.enemies().physicsFlags(0));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x2D, EntityRuntimeFixture.inspection(runtime).droppedItemForTest(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(0x04, runtime.enemies().physicsFlags(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void topDownContactDamagesAnAscendingLinkBelowEightPixels() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        var tooHigh = new EnemyAttackContext(1, false, false, false, false, false, 0, 8);
        assertTrue(runtime.combat()
                .resolveCombat(new EnemyCombatFrame(2, 64, 64, true, 0xF8, true, false, 0, 0, 0, 0, tooHigh))
                .isEmpty());
        var low = new EnemyAttackContext(1, false, false, false, false, false, 0, 7);
        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EnemyCombatFrame(2, 64, 64, true, 0x08, true, false, 0, 0, 0, 0, low));
        assertEquals(1, events.size());
        assertEquals(4, events.getFirst().linkDamage());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
    }

    @Test
    void recentStompAllowsAscendingContactUntilTheCountdownExpires() {
        assertTrue(GoombaMotion.canStomp(true, true, 0xF8, true));
        assertFalse(GoombaMotion.canStomp(true, true, 0xF8, false));
        assertTrue(GoombaMotion.canStomp(false, true, 8, true));
        assertFalse(GoombaMotion.canStomp(false, true, 8, false));
        assertFalse(GoombaMotion.canStomp(true, false, 8, true));
    }

    @Test
    void goombaSideScrollStompUsesTheRomVerticalBounceOnEitherFrameParity() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9F)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build())
                .withSideScrolling(true);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial)
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(2, 64, 64, true, 0x08, true, false, 0, 0, 0, 0));
        assertEquals(1, events.size());
        assertEquals(
                EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING,
                events.get(0).linkAction());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));

        assertTrue(runtime.combat()
                .resolveCombat(new EntityAttackContactFrame(3, 64, 64, true, 0x08, true, false, 0, 0, 0, 0))
                .isEmpty());
        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
            if (frame == 0) assertEquals(0xC2, runtime.enemies().physicsFlags(0));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
