package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeCollision2Test extends EntityRuntimeTestSupport {

    @Test
    void antiFairyUsesHealthGroupSixCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x15)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
        assertEquals(0x04, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0x03, runtime.enemies().enemyHealth(0));
    }

    @Test
    void starGivesHorizontalBackgroundCollisionPriorityWhenBouncing() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        RoomEntityBackgroundCollision corner = (entity, direction, nextX, nextY) -> direction == 0 || direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00), corner));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF4, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
    }

    @Test
    void starLeavesItsRecoilAndMovementUntouchedDuringNonInteractiveLinkMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        EnemyProjectileCollision.LinkState nonInteractive =
                new EnemyProjectileCollision.LinkState(120, 120, 0, 0x02, 0, false);
        runtime.tick(EntityTestFrames.projectileFrame(runtime, 1, 120, 120, sequence(0x00), null, nonInteractive));
        runtime.tick(EntityTestFrames.projectileFrame(runtime, 2, 120, 120, sequence(0x00), null, nonInteractive));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
    }

    @Test
    void starUsesTheNormalEnemyCombatValuesAndShadowPhysics() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0x9C));
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
    }

    @Test
    void spikeTrapForwardEndpointPublishesOneRomSwordPokeJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00)));
        assertEquals(0x0A, runtime.consumePendingEntityEvents().getFirst().soundId());

        for (int frame = 3; frame <= 25; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
            assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        }

        runtime.tick(EntityTestFrames.frame(runtime, 26, 80, 64, sequence(0x00)));
        List<EntityCombatEvent> endpointEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, endpointEvents.size());
        assertEquals(
                EntityCombatEvent.SoundChannel.JINGLE, endpointEvents.get(0).soundChannel());
        assertEquals(0x07, endpointEvents.get(0).soundId());
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));

        runtime.tick(EntityTestFrames.frame(runtime, 27, 80, 64, sequence(0x00)));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapForwardBackgroundCollisionPublishesTheSameRomJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00)));
        assertEquals(0x0A, runtime.consumePendingEntityEvents().getFirst().soundId());

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> true;
        runtime.tick(EntityTestFrames.frame(runtime, 3, 80, 64, sequence(0x00), wall));

        List<EntityCombatEvent> collisionEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, collisionEvents.size());
        assertEquals(
                EntityCombatEvent.SoundChannel.JINGLE, collisionEvents.get(0).soundChannel());
        assertEquals(0x07, collisionEvents.get(0).soundId());
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
    }

    @Test
    void spikeTrapBackgroundCollisionKeepsItInItsLaunchState() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00), wall));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapTransitionCountdown(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapSpeedX(0));
    }

    @Test
    void spikeTrapSwordCollisionPublishesTheRomSwordPokeWithoutDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(4, runtime.enemies().enemyHealth(0));
        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(8, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        EntityCombatEvent event = sword.get(0);
        assertTrue(event.swordHit());
        assertEquals(0, event.enemyDamage());
        assertEquals(-1, event.enemySpecialAction());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x40, 0x40), event.swordPokeVfx());
        assertEquals(4, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }

    @Test
    void rockHittingLinkAlsoSuppressesTheWallPokeJingle() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x0A)
                        .at(0x40, 0x50)
                        .spriteDefinition(pairDefinition(0x0A, 1))
                        .build()))
                .build();
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x50,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 1, false)));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, events.get(0).kind());
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void sideScrollDropUsesDownCollisionToAlignAndStopWeakLanding() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x50, 0x60)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                .z(0x09)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(source).withSideScrolling(true))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration()
                .setEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(15, 0x2D);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        RoomEntityBackgroundCollision collision =
                (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.DOWN;
        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0, collision));
        }

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(0x55, drop.y());
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(14));
    }

    @Test
    void clearingAnEntityResetsTheRomHitboxIgnoreFlag() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot()).build();
        EntityRuntimeFixture.debug(runtime).setHitboxFlagsForTest(0, 0x80);

        runtime.clearEntity(0);

        assertEquals(0, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
    }
}
