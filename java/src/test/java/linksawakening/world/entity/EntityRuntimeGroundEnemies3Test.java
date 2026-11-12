package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGroundEnemies3Test extends EntityRuntimeTestSupport {

    @Test
    void leeverKeepsBankFourRecoilWhenBackgroundBlocksTheStep() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0E)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));

        runtime.resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00), wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void leeverRestoresItsPositionWhenTheRomBackgroundHelperBlocksRightwardMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0E)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 32, 80, 64, sequence(0x00)));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 33, 80, 64, sequence(0x00), rightWall));
        runtime.tick(EntityTestFrames.frame(runtime, 34, 80, 64, sequence(0x00), rightWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
    }

    @Test
    void leeverAdvancesItsPositionWhenTheBackgroundDoesNotBlock() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0E)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 32, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 33, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 34, 80, 64, sequence(0x00)));

        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
    }

    @Test
    void stalfosAggressiveUsesTheRomPursuitAndJumpArc() {
        EntitySpriteDefinition definition = pairDefinition(0x1A, 3);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1A)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 64, sequence(0x00)));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0x28, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 64, 64, sequence(0x00)));
        assertEquals(0x02, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x26, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 3; frame <= 21; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 64, 64, sequence(0x00)));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosTransitionCountdown(0));
        assertEquals(0xC0, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        int frame = 22;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0) != 0 && frame < 100) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 64, 64, sequence(0x00)));
        }
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
    }

    @Test
    void stalfosAggressiveUsesHealthGroupTwoACombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x1A, 3);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1A)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemies().enemyHealth(0));
    }

    @Test
    void gibdoRunsTheRomInitDirectionChoiceAndFixedPointWalk() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // EntityInitHandlersTable._1F only increments the state. The first
        // handler pass therefore takes the forced-direction path.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0xFF)));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedY(0));

        // LikeLikeGibdoSpeeds[2] is $F8 (-8 pixels per 16 frames).
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x02)));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoState(0));
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());

        // A nonzero random low six bits keeps the selected direction and the
        // ROM fixed-point update moves the entity left by one pixel here.
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x01)));
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
    }

    @Test
    void likeLikeRunsTheRomInitDirectionChoiceAndFixedPointWalk() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0xFF)));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeWalkState(0));

        // LikeLikeGibdoSpeeds[1] is $08 (+8 pixels per 16 frames).
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00, 0x01)));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeWalkState(0));
        assertEquals(0x08, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeWalkSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0xFF)));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 120, 120, sequence(0xFF)));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void likeLikeCapturesLinkOnTheRomCollisionCadenceWithoutContactDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00, 0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                64,
                64,
                sequence(0x00, 0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));

        List<LikeLikeEvent> events = runtime.consumePendingLikeLikeEvents();
        assertEquals(1, events.size());
        assertEquals(LikeLikeEvent.Kind.CAPTURE, events.getFirst().kind());
        assertEquals(-1, events.getFirst().stolenInventorySlot());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeState(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(3, 64, 64, false, true, true, 72, 1, 72, 1))
                .isEmpty());
        assertEquals(2, runtime.enemies().enemyHealth(0));
    }

    @Test
    void likeLikeStealsTheBShieldFirstAndRefusesToStealTheLevelTwoShield() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLikeLikeLinkInventoryForTest(0x04, 0x04);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.consumePendingLikeLikeEvents();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        LikeLikeEvent stolen = runtime.consumePendingLikeLikeEvents().getFirst();
        assertEquals(LikeLikeEvent.Kind.CAPTURE, stolen.kind());
        assertEquals(0, stolen.stolenInventorySlot());
        assertEquals(1, stolen.stolenShieldLevel());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikePrivateState1(0));

        RoomEntityRuntime levelTwo = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(levelTwo).setLikeLikeLinkInventoryForTest(0x00, 0x04);
        levelTwo.tick(EntityTestFrames.frame(levelTwo, 0, 64, 64, sequence(0xFF)));
        levelTwo.tick(EntityTestFrames.projectileFrame(
                levelTwo,
                1,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 2, 0)));
        levelTwo.consumePendingLikeLikeEvents();
        levelTwo.tick(EntityTestFrames.projectileFrame(
                levelTwo,
                2,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 2, 0)));
        LikeLikeEvent protectedShield = levelTwo.consumePendingLikeLikeEvents().getFirst();
        assertEquals(-1, protectedShield.stolenInventorySlot());
        assertEquals(0, new EntityRoomInspectionQueries(levelTwo.inspection()).likeLikePrivateState1(0));
    }

    @Test
    void likeLikeDeliversShieldTheftWhileLinksMotionStatePreventsPositionWrites() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(pairDefinition(0x23, 2))
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setLikeLikeLinkInventoryForTest(0x00, 0x04);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.consumePendingLikeLikeEvents();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                80,
                80,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(80, 80, 0, 1, 0, false, 1, 0)));
        var events = runtime.consumePendingLikeLikeEvents();
        assertEquals(1, events.size());
        assertEquals(LikeLikeEvent.Kind.CAPTURE, events.getFirst().kind());
        assertFalse(events.getFirst().pinLinkPosition());
        assertEquals(0, events.getFirst().stolenInventorySlot());
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikePrivateState1(0));
    }

    @Test
    void likeLikeReleasesLinkAfterEightHeldActionFramesAndStartsTheRomSlowTimer() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x23)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0xFF)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        runtime.consumePendingLikeLikeEvents();

        runtime.configuration().setActionButtonsHeld(true);
        for (int frame = 2; frame <= 8; frame++) {
            runtime.tick(EntityTestFrames.projectileFrame(
                    runtime,
                    frame,
                    64,
                    64,
                    sequence(0x00),
                    null,
                    new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
            runtime.consumePendingLikeLikeEvents();
        }
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeState(0));
        assertEquals(7, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeInertia(0));

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                9,
                64,
                64,
                sequence(0x00),
                null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0)));
        LikeLikeEvent release = runtime.consumePendingLikeLikeEvents().getFirst();
        assertEquals(LikeLikeEvent.Kind.RELEASE, release.kind());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).likeLikeState(0));
        assertEquals(0x15, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(0));
    }

    @Test
    void goombaUsesTheRomNormalRoomRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        assertEquals(0x02, new EntityEnemyInspectionQueries(runtime.enemies()).goombaSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x10, 120, 120, sequence(0xFF)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void goombaUsesRomHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
    void goombaStompUsesTheRomDescendingVelocityBranchAndSharedDeathHandler() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0xF0, true, false, 0, 0, 0, 0));
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

        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
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
    void goombaDoesNotStompWhileLinkIsAscending() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0x10, true, false, 0, 0, 0, 0));
        assertTrue(events.isEmpty());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));
    }

    @Test
    void goombaSideScrollStompUsesTheRomHorizontalBounceAction() throws IOException {
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
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntityAttackContactFrame(1, 72, 72, true, 0x10, true, false, 0, 0, 0, 0));
        assertEquals(1, events.size());
        assertEquals(
                EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING,
                events.get(0).linkAction());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).goombaState(0));

        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void snakeUsesTheRomInitCountdownRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
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

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).snakeState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0xFF)));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x2E, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 120, 120, sequence(0xFF)));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void snakeStartsTheRomAxisDashWhenItsPrivateTimerExpires() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
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
        for (int frame = 1; frame <= 48; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 64, 120, sequence(0x02)));
        }

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).snakeState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).snakeTransitionCountdown(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).snakeSpeedY(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).snakePrivateCountdown1(0));
    }
}
