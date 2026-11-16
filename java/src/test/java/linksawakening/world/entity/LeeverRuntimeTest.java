package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LeeverRuntimeTest {

    @Test
    void leeverUsesTheRomHideEmergeAndChaseStateBoundaries() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 31, 120, 120, randomBytes));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x70, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 32, 80, 64, randomBytes));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedY(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void leeverInitializationAppliesTheRomHiddenSpriteVariantBeforeItsFirstHandlerPass() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
    }

    @Test
    void leeverBurrowsAfterItsChaseTimerAndStartsTheRomHidingWindow() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        for (int frame = 0; frame <= 143; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 64, randomBytes));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedY(0));

        for (int frame = 144; frame <= 173; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 64, randomBytes));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 174, 120, 64, randomBytes));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(0));
        assertEquals(0x30, new EntityEnemyInspectionQueries(runtime.enemies()).leeverTransitionCountdown(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 175, 120, 64, randomBytes));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void leeverOnlyUsesItsHealthGroupOneCombatValuesWhileChasing() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        List<EntityCombatEvent> hiddenContact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertTrue(hiddenContact.isEmpty());

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemies().enemyHealth(0));
    }

    @Test
    void leeverSwordHitConfiguresBankFourRecoilBeforeItsStateMovement() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void leeverKeepsBankFourRecoilWhenBackgroundBlocksTheStep() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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

        runtime.combat().resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
}
