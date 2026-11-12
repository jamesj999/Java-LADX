package linksawakening.render;

import java.util.List;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

/** Bounded origin-relative poses sampled once from the existing VFX renderer, without duplicating ROM geometry. */
final class VfxPoseCache {
    private final List<CutLeavesEffectRenderer.SpritePlacement>[][] poses;

    @SuppressWarnings("unchecked")
    VfxPoseCache(CutLeavesEffectRenderer renderer) {
        poses = new List[TransientVfxType.values().length][];
        for (var type : TransientVfxType.values()) {
            int size =
                    switch (type) {
                        case BUSH_LEAVES -> 8;
                        case WATER_SPLASH, SWORD_POKE, SMOKE, LASER_BEAM -> 2;
                        case MOVING_SPARKLE -> 140;
                        case SWORD_BEAM -> 16;
                        default -> 4;
                    };
            poses[type.ordinal()] = new List[size];
            for (int i = 0; i < size; i++)
                poses[type.ordinal()][i] = switch (type) {
                    case BUSH_LEAVES -> renderer.renderBushLeaves(0, 0, i * 4);
                    case WATER_SPLASH -> renderer.renderWaterSplash(0, 0, i * 8);
                    case SWORD_POKE -> renderer.renderSwordPoke(0, 0, i * 8);
                    case SMOKE -> renderer.renderSmoke(0, 0, i * 8);
                    case LASER_BEAM -> renderer.renderLaserBeam(0, 0, 0, i, 0);
                    case MOVING_SPARKLE -> renderer.renderMovingSparkle(0, 0, i / 4, i & 3);
                    case SWORD_BEAM -> renderer.renderSwordBeam(0, 0, 0, i / 4, 0, i & 3);
                    default -> renderer.renderPoof(0, 0, i * 4);
                };
        }
    }

    List<CutLeavesEffectRenderer.SpritePlacement> pose(TransientVfxSystem.Slot slot, int frame) {
        int countdown = slot.countdown();
        int index =
                switch (slot.type()) {
                    case BUSH_LEAVES -> (countdown & 0x1c) >>> 2;
                    case WATER_SPLASH, SWORD_POKE, SMOKE -> (countdown & 8) >>> 3;
                    case LASER_BEAM -> (frame ^ slot.slotIndex()) & 1;
                    case MOVING_SPARKLE -> Math.max(0, Math.min(34, countdown)) * 4 + (slot.variant() & 3);
                    case SWORD_BEAM -> (((frame ^ slot.slotIndex()) & 1) | (frame & 2)) * 4 + (slot.variant() & 3);
                    default -> Math.max(0, Math.min(15, countdown)) >>> 2;
                };
        return poses[slot.type().ordinal()][index];
    }
}
