package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.entity.motion.ZolGelMotion.ZolGelFrame;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GelClingingRuntimeTest {

    @Test
    void gelClingingStateRunsTheRomBackgroundProbeAfterLinkRelativePlacement() {
        ZolGelMotion motion = new ZolGelMotion();
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1C)
                .at(64, 64)
                .spriteDefinition(pairDefinition(0x1C, 2))
                .build();
        motion.prepareChestSpawn(entity.slot());
        motion.onLinkCollision(entity.slot());
        List<Integer> observedDirections = new ArrayList<>();
        List<Integer> observedIgnoreHits = new ArrayList<>();

        RoomEntityBackgroundInteraction background = new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity probed, int direction, int nextX, int nextY) {
                observedDirections.add(direction);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity probed, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedDirections.add(direction);
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        };

        motion.advance(new ZolGelFrame(entity, 80, 64, 0, () -> 0, background, 0, 0x09, false));

        assertEquals(List.of(0), observedDirections);
        assertEquals(List.of(0x09), observedIgnoreHits);
    }

    @Test
    void gelUsesTheRomSmallEnemyHitboxAndClingingCollisionState() {
        EntitySpriteDefinition definition = pairDefinition(0x1C, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 72, 64, false, true, false, 0, 0, 0, 0))
                .isEmpty());

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0, contact.get(0).linkDamage());
        assertFalse(contact.get(0).linkCollisionResponse().active());
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0))
                .isEmpty());
    }

    @Test
    void gelExportsClingFlagOnlyWhileItsInteractiveHandlerIsAttached() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1C)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x1C, 2))
                        .build()))
                .build();
        runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0));
        assertFalse(runtime.enemies().gelClingingToLink());
        runtime.configuration().setJoypadHeld(true);
        for (int frame = 0; frame < 43; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 68, 64, () -> 0));
            assertTrue(runtime.enemies().gelClingingToLink());
            if (frame == 2) {
                assertEquals(67, runtime.inspection().snapshot().slots().get(0).x());
                assertEquals(62, runtime.inspection().snapshot().slots().get(0).y());
            }
        }
        runtime.tick(EntityTestFrames.frame(runtime, 43, 68, 64, () -> 0));
        assertFalse(runtime.enemies().gelClingingToLink());
    }

    @Test
    void gelReleasesClingingStateAfterRomJoypadCountdownDecrementsThreeTimesPerFrame() {
        EntitySpriteDefinition definition = pairDefinition(0x1C, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(
                1,
                runtime.combat()
                        .resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0))
                        .size());
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0x80, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        runtime.configuration().setJoypadHeld(true);
        for (int frame = 0; frame < 42; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 68, 64, sequence(0x00)));
        }
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0x02, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        // The third decrement reaches zero at the end of this clinging
        // handler invocation; the state-4 release runs on the next frame.
        runtime.tick(EntityTestFrames.frame(runtime, 42, 68, 64, sequence(0x00)));
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 43, 68, 64, sequence(0x00)));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
    }
}
