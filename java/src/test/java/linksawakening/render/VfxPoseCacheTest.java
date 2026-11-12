package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.vfx.*;
import org.junit.jupiter.api.Test;

final class VfxPoseCacheTest {
    @Test
    void everyCachedPhaseMatchesTheExistingRenderer() {
        var renderer = new SceneConstructionFixture().state.cutLeavesEffectRenderer();
        var cache = new VfxPoseCache(renderer);
        for (var type : TransientVfxType.values())
            for (int countdown = -2; countdown <= 40; countdown++)
                for (int variant = 0; variant < 4; variant++)
                    for (int frame = 0; frame < 4; frame++)
                        for (int slot = 0; slot < 2; slot++) {
                            var state = new TransientVfxSystem.Slot(slot, type, countdown, 70, 50, variant);
                            var expected =
                                    switch (type) {
                                        case BUSH_LEAVES -> renderer.renderBushLeaves(70, 50, countdown);
                                        case WATER_SPLASH -> renderer.renderWaterSplash(70, 50, countdown);
                                        case SWORD_POKE -> renderer.renderSwordPoke(70, 50, countdown);
                                        case SMOKE -> renderer.renderSmoke(70, 50, countdown);
                                        case LASER_BEAM -> renderer.renderLaserBeam(70, 50, countdown, frame, slot);
                                        case MOVING_SPARKLE -> renderer.renderMovingSparkle(70, 50, countdown, variant);
                                        case SWORD_BEAM -> renderer.renderSwordBeam(
                                                70, 50, countdown, frame, slot, variant);
                                        default -> renderer.renderPoof(70, 50, countdown);
                                    };
                            var actual = cache.pose(state, frame);
                            assertEquals(expected.size(), actual.size(), type.toString());
                            for (int i = 0; i < actual.size(); i++) {
                                var a = actual.get(i);
                                var e = expected.get(i);
                                assertEquals(e.x(), 70 + a.x());
                                assertEquals(e.y(), 50 + a.y());
                                assertEquals(e.tileId(), a.tileId());
                                assertEquals(e.attributes(), a.attributes());
                                assertSame(e.tile(), a.tile());
                            }
                        }
    }
}
