package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGroundEnemies5Test extends EntityRuntimeTestSupport {

    @Test
    void fleeingEvasiveStalfosPokesAndClearsOnBackgroundCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x1E)
                                .at(0x40, 0x50)
                                .spriteDefinition(definition)
                                .z(0x02)
                                .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEvasiveFleeingForTest(0, 0x10, 0);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x00), wall));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x07, events.getFirst().soundId());
        assertEquals(
                List.of(new TransientVfxRequest(TransientVfxType.SWORD_POKE, 0x40, 0x4E)),
                runtime.enemies().transientVfxRequests());
    }

    @Test
    void fleeingEvasiveStalfosClearsAtTheRomScreenEdgeWithoutSwordPoke() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x1E)
                                .at(0xA7, 0x50)
                                .spriteDefinition(definition)
                                .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEvasiveFleeingForTest(0, 0x10, 0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x70, 0x50, sequence(0x00)));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        assertTrue(runtime.enemies().transientVfxRequests().isEmpty());
    }

    @Test
    void gibdoFallsBackToTheRomVerticalSpeedTableWhenHorizontalSpeedIsZero() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00, 0x01)));

        assertEquals(0, runtime.enemies().gibdoSpeedX(0));
        assertEquals(0x08, runtime.enemies().gibdoSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void gibdoReversesTheMovingAxisWhenTheBackgroundQueryBlocksIt() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0xFF)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x02)));

        RoomEntityBackgroundCollision leftWall = (entity, direction, nextX, nextY) -> direction == 1;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x01), leftWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x08, runtime.enemies().gibdoSpeedX(0));
    }

    @Test
    void gibdoUsesHealthGroupTwoFCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
        assertEquals(0x06, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> sword = runtime.resolveCombat(1, 72, 72, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0x05, runtime.enemies().enemyHealth(0));
    }

    @Test
    void sparkInitializersSelectTheRomDirectionAndOffset() {
        EntitySpriteDefinition counterDefinition = pairDefinition(0x16, 2);
        RoomEntityRuntime counterClockwise = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x16)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(counterDefinition)
                                .build()))
                .build();
        counterClockwise.tick(EntityTestFrames.frame(counterClockwise, 0));

        assertEquals(61, counterClockwise.inspection().snapshot().slots().get(0).y());
        assertEquals(0, counterClockwise.enemies().sparkPrivateState2(0));
        assertEquals(0, counterClockwise.enemies().sparkSpeedX(0));
        assertEquals(0, counterClockwise.enemies().sparkSpeedY(0));

        EntitySpriteDefinition clockwiseDefinition = pairDefinition(0x17, 2);
        RoomEntityRuntime clockwise = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x17)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(clockwiseDefinition)
                                .build()))
                .build();
        clockwise.tick(EntityTestFrames.frame(clockwise, 0));

        assertEquals(67, clockwise.inspection().snapshot().slots().get(0).y());
        assertEquals(4, clockwise.enemies().sparkPrivateState2(0));
    }

    @Test
    void sparkFollowsTheRomEightDirectionTableAndFrameCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x16)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, () -> 0));

        assertEquals(0, runtime.enemies().sparkPrivateState1(0));
        assertEquals(9, runtime.enemies().sparkTransitionCountdown(0));
        assertEquals(0x10, runtime.enemies().sparkSpeedX(0));
        assertEquals(0x00, runtime.enemies().sparkSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, () -> 0));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void sparkUsesCollisionMaskToAdvanceItsRomDirectionIndex() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x16)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(definition)
                                .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0));

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, () -> 0, rightWall));

        assertEquals(1, runtime.enemies().sparkPrivateState1(0));
        assertEquals(0x00, runtime.enemies().sparkSpeedX(0));
        assertEquals(0x10, runtime.enemies().sparkSpeedY(0));
        assertEquals(0, runtime.enemies().sparkTransitionCountdown(0));
    }

    @Test
    void sparkUsesHealthGroupTwoCCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x16)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
        assertEquals(0x01, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> sword = runtime.resolveCombat(1, 72, 72, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void waterTektiteUsesNoopInitializationAndTheRomFrameVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x99)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(definition)
                                .build()))
                .build();
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0x00;
        };

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        assertEquals(0, randomCalls.get());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());

        runtime.tick(EntityTestFrames.frame(runtime, 0x00, 120, 120, randomBytes));
        assertEquals(2, randomCalls.get());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 0x10, 120, 120, randomBytes));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void waterTektiteUsesTheRomThreeStateAccelerationAndSignedDecelerationLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x99)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(definition)
                                .build()))
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, randomBytes));
        assertEquals(1, runtime.enemies().waterTektiteState(0));
        assertEquals(0x20, runtime.enemies().waterTektiteTransitionCountdown(0));
        assertEquals(0xFF, runtime.enemies().waterTektitePrivateState1(0));
        assertEquals(0x01, runtime.enemies().waterTektitePrivateState2(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, randomBytes));
        assertEquals(0, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0, runtime.enemies().waterTektiteSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 120, 120, randomBytes));
        assertEquals(0xFF, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0x01, runtime.enemies().waterTektiteSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        for (int frame = 4; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(2, runtime.enemies().waterTektiteState(0));
        assertEquals(0xF1, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0x0F, runtime.enemies().waterTektiteSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 34, 120, 120, randomBytes));
        assertEquals(0xF2, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0x0E, runtime.enemies().waterTektiteSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 35, 120, 120, randomBytes));
        assertEquals(0xF2, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0x0E, runtime.enemies().waterTektiteSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 36, 120, 120, randomBytes));
        assertEquals(0xF3, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0x0D, runtime.enemies().waterTektiteSpeedY(0));
    }

    @Test
    void waterTektiteResetsToStateZeroWhenTheMovedPositionHitsBackground() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x99)
                                .at(64, 64)
                                .status(EntityStatus.INIT)
                                .spriteDefinition(definition)
                                .build()))
                .build();
        IntSupplier randomBytes = sequence(0x02);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        for (int frame = 1; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(2, runtime.enemies().waterTektiteState(0));
        int beforeX = runtime.inspection().snapshot().slots().get(0).x();
        int beforeY = runtime.inspection().snapshot().slots().get(0).y();
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> true;

        runtime.tick(EntityTestFrames.frame(runtime, 34, 120, 120, randomBytes, wall));

        RoomEntity blocked = runtime.inspection().snapshot().slots().get(0);
        assertEquals(beforeX, blocked.x());
        assertEquals(beforeY, blocked.y());
        assertEquals(0, runtime.enemies().waterTektiteState(0));
        assertEquals(0x10, runtime.enemies().waterTektiteTransitionCountdown(0));
        assertEquals(0, runtime.enemies().waterTektiteSpeedX(0));
        assertEquals(0, runtime.enemies().waterTektiteSpeedY(0));
    }

    @Test
    void waterTektiteAppliesBankSevenRecoilBeforeItsStateMovement() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x99)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging);

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01)));

        assertEquals(61, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(61, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(1, runtime.enemies().waterTektiteState(0));
    }

    @Test
    void waterTektiteKeepsBankSevenRecoilWhenBackgroundBlocksTheStep() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x99)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .spriteHandlers(null)
                .enemyCombatTables(tables)
                .build();

        runtime.resolveCombat(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01), wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertTrue(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(1, runtime.enemies().waterTektiteState(0));
    }

    @Test
    void waterTektiteUsesHealthGroupZeroAndTheNormalEnemyHitbox() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x99)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertTrue(RoomEntityCombatRules.overlapsLink(
                runtime.inspection().snapshot().slots().get(0), 64, 64));
        assertFalse(RoomEntityCombatRules.overlapsLink(
                runtime.inspection().snapshot().slots().get(0), 73, 64));

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        runtime = RoomEntityRuntime.builder(snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x99)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        List<EntityCombatEvent> sword = runtime.resolveCombat(1, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void moblinLaunchUsesTheHighestDisabledSlotAndDoesNotTickItTwice() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .z(0x03)
                .build());
        RoomEntitySnapshot initial = new RoomEntitySnapshot(slots);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }

        RoomEntity projectile = runtime.inspection().snapshot().slots().get(14);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x0C, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x4C, projectile.y());
        assertEquals(0x03, projectile.z());
        assertEquals(0, projectile.spriteVariant());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(1, runtime.enemies().enemyIgnoreHitsCountdown(14));
        assertEquals(0x20, runtime.enemies().enemyProjectileSpeedX(14));
        assertEquals(1, runtime.inspection().projectileLaunchRequests().size());
        assertEquals(15, runtime.inspection().projectileLaunchRequests().get(0).sourceSlot());
        assertEquals(
                0x0C, runtime.inspection().projectileLaunchRequests().get(0).projectileType());

        assertEquals(0, runtime.clearEntity(14));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(14).status());
        assertEquals(0, runtime.enemies().enemyProjectileSpeedX(14));
        assertEquals(0, runtime.enemies().enemyProjectileTransitionCountdown(14));
    }

    @Test
    void armosKnightDeathUsesItsRomBossCountdownsBeforeDidKillEnemy() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        RoomEntity source = new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x88)
                .at(0x50, 0x60)
                .status(EntityStatus.DYING)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x88))
                .spriteVariant(-1)
                .z(0x05)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotAt(source))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0xA0, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, runtime.enemies().enemyFlashCountdown(15));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(15).status());

        for (int frame = 1; frame <= 0xA0; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(0xC0, runtime.enemies().transitionCountdown(15));
        assertEquals(0xFF, runtime.enemies().enemyFlashCountdown(15));

        for (int frame = 0xA1; frame <= 0x160; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(15).status());
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(0x30, drop.type());
        assertEquals(0x03, drop.spriteVariant());
        assertEquals(0x50, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x05, drop.z());
        assertEquals(
                List.of(new EntityCombatEvent(15, 0x88, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A)),
                runtime.consumePendingEntityEvents());
    }
}
