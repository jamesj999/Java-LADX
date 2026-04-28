package linksawakening.world;

/**
 * A single warp record extracted from a room's object stream.
 *
 * <p>In the LADX disassembly each warp is a 5-byte record stored in the room
 * object stream starting with a byte where {@code (firstByte & 0xFC) == 0xE0}.
 * The low two bits of the first byte become {@code category} (0=overworld,
 * 1=indoor normal, 2=sidescrolling, 3=other). The parser writes these to
 * {@code wWarpStructs} (wram.asm:$D401+) — see {@code bank0.asm:6050}.
 *
 * <p>{@code tileLocation} is the room-object grid cell (YX-packed, one row =
 * 0x10 units) where Link must stand to trigger this warp. Populated separately
 * by scanning the expanded {@code roomObjectsArea} for door-type object IDs,
 * mirroring {@code wWarpPositions} (wram.asm:$D416) via
 * {@code configureDoorWarpData} (bank0.asm:6345).
 *
 * <p>A warp is triggered when Link's centre-tile index equals
 * {@code tileLocation} — see {@code LinkMotionMapFadeOutHandler.loop}
 * (bank0.asm:2798) for the matching loop.
 */
public final class Warp {

    public static final int CATEGORY_OVERWORLD = 0;
    public static final int CATEGORY_INDOOR = 1;
    public static final int CATEGORY_SIDESCROLL = 2;

    private final int category;
    private final int destMap;
    private final int destRoom;
    private final int destX;
    private final int destY;
    private final int tileLocation;

    public Warp(int category, int destMap, int destRoom, int destX, int destY,
                int tileLocation) {
        this.category = category;
        this.destMap = destMap;
        this.destRoom = destRoom;
        this.destX = destX;
        this.destY = destY;
        this.tileLocation = tileLocation;
    }

    public Warp withTileLocation(int newTileLocation) {
        return new Warp(category, destMap, destRoom, destX, destY, newTileLocation);
    }

    public int category() { return category; }
    public int destMap() { return destMap; }
    public int destRoom() { return destRoom; }
    public int destX() { return destX; }
    public int destY() { return destY; }
    public int tileLocation() { return tileLocation; }

    /**
     * Pack Link's pixel position into the room-object grid location byte used
     * to match against {@code tileLocation}. Mirrors the hash computed at
     * bank0.asm:2799-2806 but adjusted for Java's sprite-origin convention.
     *
     * <p>In the disassembly {@code hLinkPositionY} is Link's OAM y — i.e. the
     * visible-bottom of the 16-tall sprite. Its formula {@code (y − 8) & 0xF0}
     * gives the tile row of the sprite's *center* pixel. Java's
     * {@code Link.pixelY()} is the visible-TOP of the sprite, so the
     * equivalent expression is {@code (pixelY + 8) / 16}. Same asymmetry on
     * X: disassembly {@code hLinkPositionX} is the visible-center of the
     * 16-wide sprite (OAM-x of the first 8-wide sub-sprite), whereas Java
     * {@code pixelX()} is the left edge — hence {@code +8} there too.
     */
    public static int packTileLocation(int pixelX, int pixelY) {
        int col = ((pixelX + 8) >> 4) & 0x0F;
        int row = ((pixelY + 8) >> 4) & 0x0F;
        return (row << 4) | col;
    }

    /**
     * Convert a warp's ROM-encoded destination (disassembly convention —
     * center X, bottom Y) into Java sprite-origin (top-left) coordinates.
     * The ROM bytes land directly in {@code hLinkPositionX/Y} via
     * {@code GameplayWorldLoadRoomHandler} (world_handler.asm:180-185), so
     * the conversion offset is the same as {@link #packTileLocation}.
     */
    public int javaPixelX() { return destX - 8; }
    public int javaPixelY() { return destY - 16; }
}
