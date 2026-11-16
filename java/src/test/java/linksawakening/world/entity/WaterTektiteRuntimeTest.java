package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class WaterTektiteRuntimeTest {

    @Test
    void waterTektiteUsesNoopInitializationAndTheRomFrameVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteState(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteTransitionCountdown(0));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektitePrivateState1(0));
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektitePrivateState2(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, randomBytes));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 120, 120, randomBytes));
        assertEquals(0xFF, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0x01, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        for (int frame = 4; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteState(0));
        assertEquals(0xF1, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0x0F, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 34, 120, 120, randomBytes));
        assertEquals(0xF2, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0x0E, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 35, 120, 120, randomBytes));
        assertEquals(0xF2, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0x0E, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 36, 120, 120, randomBytes));
        assertEquals(0xF3, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0x0D, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));
    }

    @Test
    void waterTektiteResetsToStateZeroWhenTheMovedPositionHitsBackground() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteState(0));
        int beforeX = runtime.inspection().snapshot().slots().get(0).x();
        int beforeY = runtime.inspection().snapshot().slots().get(0).y();
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> true;

        runtime.tick(EntityTestFrames.frame(runtime, 34, 120, 120, randomBytes, wall));

        RoomEntity blocked = runtime.inspection().snapshot().slots().get(0);
        assertEquals(beforeX, blocked.x());
        assertEquals(beforeY, blocked.y());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteSpeedY(0));
    }

    @Test
    void waterTektiteAppliesBankSevenRecoilBeforeItsStateMovement() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(
                        new EntityVerticalContactFrame(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging));

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01)));

        assertEquals(61, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(61, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteState(0));
    }

    @Test
    void waterTektiteKeepsBankSevenRecoilWhenBackgroundBlocksTheStep() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        runtime.combat()
                .resolveCombat(
                        new EntityVerticalContactFrame(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01), wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).waterTektiteState(0));
    }

    @Test
    void waterTektiteUsesHealthGroupZeroAndTheNormalEnemyHitbox() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x99)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
