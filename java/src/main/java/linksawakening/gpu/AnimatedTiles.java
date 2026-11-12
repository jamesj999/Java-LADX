package linksawakening.gpu;

import linksawakening.rom.GbcBanks;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;

/** Stateful gameplay tile animation; counters intentionally survive room changes. */
final class AnimatedTiles {
    private static final int TILE_DATA_SIZE = Vram.TILE_DATA_SIZE;
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private final RomTileLoader target;

    AnimatedTiles(RomTileLoader target) {
        this.target = target;
    }

    private static final int ANIMATED_TILES_BANK = GbcBanks.adjust(RomSymbols.AnimatedTiles.bank());
    private static final int ANIMATED_TILES_BASE_ADDR = RomSymbols.AnimatedTiles.address();
    private static final int ANIMATED_TILES_FRAME_COUNT = 0x04;
    private static final int ANIMATED_TILES_FRAME_VRAM_INDEX = 0x16C;
    private static final int ANIMATED_TILES_FRAME_SIZE = 0x40;
    private static final int DUNGEON_TWO_ANIMATED_GROUP = 0x07;
    private static final int COLOR_DUNGEON_SCROLLING_VRAM_INDEX = 0x040;
    private static final int DUNGEON_TWO_SCROLLING_VRAM_INDEX = 0x10C;
    private static final int[] ANIMATED_TILES_PING_PONG = {0x00, 0x40, 0x80, 0xC0, 0xC0, 0xC0, 0x80, 0x40};
    private int currentAnimatedTilesGroup = 0;
    private int animatedTilesFrameCount = 0;
    private int animatedTilesDataOffset = 0;
    int currentMapId = 0;
    private final byte[] animatedScrollingTiles = new byte[0x40];

    public int animatedTilesFrameCount() {
        return animatedTilesFrameCount;
    }

    public void loadAnimatedTilesGroup(Rom romData, int animatedTilesGroup) {
        currentAnimatedTilesGroup = animatedTilesGroup;
        // Keep animatedTilesDataOffset across rooms, matching the original
        // (hAnimatedTilesDataOffset is never reset on room change).
        copyAnimatedTilesFrame(romData, animatedTilesDataOffset);
    }

    public void tickAnimatedTiles(Rom romData) {
        animatedTilesFrameCount = (animatedTilesFrameCount + 1) & 0xFF;

        if (currentAnimatedTilesGroup == DUNGEON_TWO_ANIMATED_GROUP) {
            if (currentMapId == MAP_COLOR_DUNGEON) {
                copyAnimatedScrollingTiles(COLOR_DUNGEON_SCROLLING_VRAM_INDEX);
            } else if (((animatedTilesFrameCount + 1) & 0x03) == 0) {
                copyAnimatedScrollingTiles(DUNGEON_TWO_SCROLLING_VRAM_INDEX);
            } else if ((animatedTilesFrameCount & 0x07) == 0) {
                int sourceOffset = ANIMATED_TILES_PING_PONG[(animatedTilesFrameCount >> 3) & 0x07];
                target.loadTilesFromROM(
                        romData,
                        ANIMATED_TILES_BANK,
                        ANIMATED_TILES_BASE_ADDR + 0x0300 + sourceOffset,
                        ANIMATED_TILES_FRAME_COUNT,
                        ANIMATED_TILES_FRAME_VRAM_INDEX);
            }
            return;
        }

        int newOffset = nextAnimatedTilesDataOffset(
                currentAnimatedTilesGroup, animatedTilesFrameCount, animatedTilesDataOffset);
        if (newOffset < 0) {
            return;
        }
        // Dungeon-1/lava index the ping-pong table directly; unlike the
        // linear groups they never write hAnimatedTilesDataOffset ($FFA7).
        if (currentAnimatedTilesGroup != 0x04 && currentAnimatedTilesGroup != 0x06) {
            animatedTilesDataOffset = newOffset;
        }
        copyAnimatedTilesFrame(romData, newOffset);
    }

    private void copyAnimatedScrollingTiles(int destinationTile) {
        System.arraycopy(
                animatedScrollingTiles,
                0,
                target.vram,
                destinationTile * TILE_DATA_SIZE,
                animatedScrollingTiles.length);
        for (int tile = destinationTile; tile < destinationTile + 4; tile++) {
            target.updateTile(tile);
        }
    }

    private void copyAnimatedTilesFrame(Rom romData, int dataOffset) {
        int groupOffset = animatedTilesGroupOffset(currentAnimatedTilesGroup);
        if (groupOffset < 0) {
            return;
        }
        target.loadTilesFromROM(
                romData,
                ANIMATED_TILES_BANK,
                ANIMATED_TILES_BASE_ADDR + groupOffset + dataOffset,
                ANIMATED_TILES_FRAME_COUNT,
                ANIMATED_TILES_FRAME_VRAM_INDEX);
    }

    private static int animatedTilesGroupOffset(int group) {
        return switch (group) {
            case 0x02 -> 0x0100; // tide
            case 0x03 -> 0x0200; // village
            case 0x04 -> 0x0300; // dungeon 1
            case 0x05 -> 0x0400; // underground
            case 0x06 -> 0x0500; // lava
            case 0x08 -> 0x0600; // warp tile
            case 0x09 -> 0x0700; // water currents
            case 0x0A -> 0x0800; // waterfall
            case 0x0B -> 0x0000; // slow waterfall
            case 0x0C -> 0x0900; // water dungeon
            case 0x0D -> 0x0B00; // light beam
            case 0x0E -> 0x0C00; // crystal block
            case 0x0F -> 0x0A00; // bubbles
            case 0x10 -> 0x0D00; // weather vane
            default -> -1;
        };
    }

    private static int nextAnimatedTilesDataOffset(int group, int frameCount, int currentOffset) {
        int linearNext = (currentOffset + ANIMATED_TILES_FRAME_SIZE) & 0xFF;
        return switch (group) {
            case 0x02, 0x03, 0x05, 0x0B, 0x0C -> (frameCount & 0x0F) == 0 ? linearNext : -1;
            case 0x08, 0x0E, 0x0F, 0x10 -> (frameCount & 0x07) == 0 ? linearNext : -1;
            case 0x09, 0x0A, 0x0D -> (frameCount & 0x03) == 0 ? linearNext : -1;
            case 0x04, 0x06 -> (frameCount & 0x07) == 0 ? ANIMATED_TILES_PING_PONG[(frameCount >> 3) & 0x07] : -1;
            default -> -1;
        };
    }

    void stageAnimatedScrollingTiles(Rom romData, int mapId, int roomId) {
        Rom rom = romData;
        byte[] staged = rom.bytes(
                RomSymbols.Items1Tiles.inBank(GbcBanks.adjust(RomSymbols.Items1Tiles.bank()))
                        .plus(0x3C0),
                animatedScrollingTiles.length);
        System.arraycopy(staged, 0, animatedScrollingTiles, 0, staged.length);
        if (mapId == MAP_COLOR_DUNGEON && roomId == 0x01) {
            byte[] symbols = rom.bytes(RomSymbols.ColorDungeonNpcTiles.plus(0xF00), 0x20);
            System.arraycopy(symbols, 0, animatedScrollingTiles, 0, symbols.length);
        }
    }
}
