package linksawakening.vfx;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gpu.Tile;

/**
 * ROM-equivalent cut-leaves frame selection and OAM metadata for bush destroys.
 */
public final class CutLeavesEffectRenderer {

    public record SpritePlacement(int x, int y, int tileId, int attributes, Tile tile) {}

    private static final int TILE_ID = 0x28;
    private static final int OAM_X_BIAS = 8;
    private static final int OAM_Y_BIAS = 16;
    private static final int SPRITES_PER_FRAME = 4;
    private static final int FRAME_COUNT = 8;

    private static final int[][] CUT_LEAVES_SPRITE_RECT = {
        { 2, -4, TILE_ID, 0x00, -5,  4, TILE_ID, 0x60,  5,  6, TILE_ID, 0x00,  1, 10, TILE_ID, 0x20 },
        { 1, -1, TILE_ID, 0x00, -7,  4, TILE_ID, 0x60,  8,  6, TILE_ID, 0x00,  2,  7, TILE_ID, 0x20 },
        { 0,  0, TILE_ID, 0x20, -8,  2, TILE_ID, 0x60,  4,  4, TILE_ID, 0x20, 10,  7, TILE_ID, 0x20 },
        { -2, 1, TILE_ID, 0x20,  4,  1, TILE_ID, 0x60,  4,  5, TILE_ID, 0x20, 12,  7, TILE_ID, 0x20 },
        { -3, 0, TILE_ID, 0x20,  4, -2, TILE_ID, 0x60,  8,  8, TILE_ID, 0x20, 14,  9, TILE_ID, 0x20 },
        { -4, -1, TILE_ID, 0x00, 4, -6, TILE_ID, 0x40,  8,  9, TILE_ID, 0x20, 15, 10, TILE_ID, 0x00 },
        { -5, -2, TILE_ID, 0x00, 3, -7, TILE_ID, 0x40,  8, 12, TILE_ID, 0x00, 17, 11, TILE_ID, 0x00 },
        { -6, -3, TILE_ID, 0x00, 1, -9, TILE_ID, 0x40,  9, 13, TILE_ID, 0x00, 15, 12, TILE_ID, 0x00 }
    };

    private final TransientVfxSpriteSheet spriteSheet;

    public CutLeavesEffectRenderer(TransientVfxSpriteSheet spriteSheet) {
        this.spriteSheet = spriteSheet;
    }

    public int frameIndexForCountdown(int countdown) {
        return ((countdown & 0x1C) ^ 0x1C) >> 2;
    }

    public List<SpritePlacement> renderBushLeaves(int worldX, int worldY, int countdown) {
        int frameIndex = frameIndexForCountdown(countdown);
        int[] frame = CUT_LEAVES_SPRITE_RECT[frameIndex & (FRAME_COUNT - 1)];
        Tile leafTile = spriteSheet.tile(TILE_ID);
        List<SpritePlacement> placements = new ArrayList<>(SPRITES_PER_FRAME);
        for (int i = 0; i < SPRITES_PER_FRAME; i++) {
            int offset = i * 4;
            placements.add(new SpritePlacement(
                worldX + frame[offset] - OAM_X_BIAS,
                worldY + frame[offset + 1] - OAM_Y_BIAS,
                frame[offset + 2],
                frame[offset + 3],
                leafTile
            ));
        }
        return List.copyOf(placements);
    }
}
