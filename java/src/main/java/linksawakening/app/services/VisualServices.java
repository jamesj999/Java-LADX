package linksawakening.app.services;

import java.util.Objects;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;

/**
 * Long-lived visual services constructed before the first frame. The immutable bundle is retained for the
 * owning session.
 */
public record VisualServices(
        TransientVfxSystem transientVfxSystem,
        DroppableRupeeSystem droppableRupeeSystem,
        CutLeavesEffectRenderer cutLeavesEffectRenderer,
        int[] newGameTarinShieldPalette,
        byte[] indexedDisplayBuffer) {
    public VisualServices {
        Objects.requireNonNull(transientVfxSystem);
        Objects.requireNonNull(droppableRupeeSystem);
        Objects.requireNonNull(cutLeavesEffectRenderer);
        Objects.requireNonNull(newGameTarinShieldPalette);
        Objects.requireNonNull(indexedDisplayBuffer);
    }
}
