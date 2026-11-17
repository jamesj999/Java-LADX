package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class TektiteRuntimeTest {

    @Test
    void tektiteAppliesBankSixRecoilBeforeItsOrdinaryMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0D)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.combat().resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x00)));

        assertEquals(61, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(61, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void tektiteLandsWithTheRomCountdownThenStartsItsNextJump() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x80)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteTransitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteTransitionCountdown(0));

        for (int frame = 17; frame <= 31; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 32, 120, 120, randomBytes));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedZ(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void tektiteReversesAndHalvesSpeedXAfterHorizontalWallCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0D)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .z(0x80)
                        .build()))
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 33, 120, 120, randomBytes, rightWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
    }

    @Test
    void tektiteVerticalWallCollisionUsesTheRomXSpeedHelper() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0D)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .z(0x80)
                        .build()))
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        RoomEntityBackgroundCollision downWall = (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 33, 120, 120, randomBytes, downWall));

        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
    }

    @Test
    void tektiteAddsTheRomLandingTimerBaseAfterMaskingRandomness() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x80)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x1F)));

        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteTransitionCountdown(0));
    }

    @Test
    void tektiteCanReplaceItsRandomJumpDirectionWithTheRomLinkVector() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x80)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 72, randomBytes));
        }

        assertEquals(0x14, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedX(0));
        // GetEntityYDistanceToLink includes the post-launch Z position, so
        // dy is 9 here and the ROM divide loop yields 11.
        assertEquals(0x0B, new EntityEnemyInspectionQueries(runtime.enemies()).tektiteSpeedY(0));
    }

    @Test
    void tektiteUsesTheHealthGroupOneContactAndSwordValues() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0D)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
