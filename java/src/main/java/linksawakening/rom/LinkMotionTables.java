package linksawakening.rom;
/** Immutable copies of LinkMotionTables ROM lookup data. */
public final class LinkMotionTables {
    // FrameCounterToLinkDirection: original loader length.
    private static final int PIT_WARP_ARRIVAL_DIRECTIONS_LENGTH = 4;
    // Data_002_7C40: original loader length.
    private static final int WATER_CURRENT_X_LENGTH = 8;
    // Data_002_7C48: original loader length.
    private static final int WATER_CURRENT_Y_LENGTH = 8;
    // HorizontalIncrementForLinkPosition: original loader length.
    private static final int LINK_SPEED_X_LENGTH = 32;
    // VerticalIncrementForLinkPosition: original loader length.
    private static final int LINK_SPEED_Y_LENGTH = 32;
    // Data_002_68B1: original loader length.
    private static final int AIR_SPEED_X_LENGTH = 4;
    // Data_002_44E7: original loader length.
    private static final int AIR_SPEED_Y_LENGTH = 4;
    // Data_002_4EF0: original loader length.
    private static final int SWIMMING_SPEED_X_LENGTH = 32;
    // Data_002_4F10: original loader length.
    private static final int SWIMMING_SPEED_Y_LENGTH = 32;
    // Data_002_750A: original loader length.
    private static final int SWIMMING_ENTRY_SPEED_X_LENGTH = 4;
    // Data_002_750E: original loader length.
    private static final int SWIMMING_ENTRY_SPEED_Y_LENGTH = 4;
    // LinkRevolvingDoorAnimation: original loader length.
    private static final int REVOLVING_DOOR_ANIMATION_LENGTH = 7;
    private final byte[] pitWarpArrivalDirections;
    private final byte[] waterCurrentX;
    private final byte[] waterCurrentY;
    private final byte[] linkSpeedX;
    private final byte[] linkSpeedY;
    private final byte[] airSpeedX;
    private final byte[] airSpeedY;
    private final byte[] swimmingSpeedX;
    private final byte[] swimmingSpeedY;
    private final byte[] swimmingEntrySpeedX;
    private final byte[] swimmingEntrySpeedY;
    private final int[] revolvingDoorAnimation;

    private final int[][] revolvingDoorPalettes;

    private LinkMotionTables() {
        pitWarpArrivalDirections = new byte[PIT_WARP_ARRIVAL_DIRECTIONS_LENGTH];
        waterCurrentX = new byte[WATER_CURRENT_X_LENGTH];
        waterCurrentY = new byte[WATER_CURRENT_Y_LENGTH];
        linkSpeedX = new byte[LINK_SPEED_X_LENGTH];
        linkSpeedY = new byte[LINK_SPEED_Y_LENGTH];
        airSpeedX = new byte[AIR_SPEED_X_LENGTH];
        airSpeedY = new byte[AIR_SPEED_Y_LENGTH];
        swimmingSpeedX = new byte[SWIMMING_SPEED_X_LENGTH];
        swimmingSpeedY = new byte[SWIMMING_SPEED_Y_LENGTH];
        swimmingEntrySpeedX = new byte[SWIMMING_ENTRY_SPEED_X_LENGTH];
        swimmingEntrySpeedY = new byte[SWIMMING_ENTRY_SPEED_Y_LENGTH];
        revolvingDoorAnimation = new int[REVOLVING_DOOR_ANIMATION_LENGTH];
        revolvingDoorPalettes = new int[2][4];
    }

    /** Motion targets for an inert, ROM-free actor. */
    public static LinkMotionTables stationary() {
        return new LinkMotionTables();
    }

    private LinkMotionTables(Rom rom) {
        pitWarpArrivalDirections =
                rom.bytes(RomSymbols.FrameCounterToLinkDirection, PIT_WARP_ARRIVAL_DIRECTIONS_LENGTH);
        waterCurrentX = rom.bytes(RomSymbols.Data_002_7C40, WATER_CURRENT_X_LENGTH);
        waterCurrentY = rom.bytes(RomSymbols.Data_002_7C48, WATER_CURRENT_Y_LENGTH);
        linkSpeedX = rom.bytes(RomSymbols.HorizontalIncrementForLinkPosition, LINK_SPEED_X_LENGTH);
        linkSpeedY = rom.bytes(RomSymbols.VerticalIncrementForLinkPosition, LINK_SPEED_Y_LENGTH);
        airSpeedX = rom.bytes(RomSymbols.Data_002_68B1, AIR_SPEED_X_LENGTH);
        airSpeedY = rom.bytes(RomSymbols.Data_002_44E7, AIR_SPEED_Y_LENGTH);
        swimmingSpeedX = rom.bytes(RomSymbols.Data_002_4EF0, SWIMMING_SPEED_X_LENGTH);
        swimmingSpeedY = rom.bytes(RomSymbols.Data_002_4F10, SWIMMING_SPEED_Y_LENGTH);
        swimmingEntrySpeedX = rom.bytes(RomSymbols.Data_002_750A, SWIMMING_ENTRY_SPEED_X_LENGTH);
        swimmingEntrySpeedY = rom.bytes(RomSymbols.Data_002_750E, SWIMMING_ENTRY_SPEED_Y_LENGTH);
        revolvingDoorAnimation = rom.u8Table(RomSymbols.LinkRevolvingDoorAnimation, REVOLVING_DOOR_ANIMATION_LENGTH);
        revolvingDoorPalettes = loadRevolvingDoorPalettes(rom);
    }

    public static LinkMotionTables load(Rom rom) {
        return new LinkMotionTables(rom);
    }

    /** LinkMotionRevolvingDoorHandler and Data_002_4E1C. */
    public int revolvingDoorAnimation(int frame) {
        return revolvingDoorAnimation[(frame >> 3) % 7];
    }

    public int[] revolvingDoorPalette(int variant) {
        return revolvingDoorPalettes[variant].clone();
    }

    /** Signed current speeds from Data_002_7C40/7C48. */
    public int pitWarpArrivalDirection(int frameCounter) {
        return pitWarpArrivalDirections[(frameCounter >>> 2) & 3];
    }

    public int waterCurrentSpeedX(int index) {
        return waterCurrentX[index & 7];
    }

    public int waterCurrentSpeedY(int index) {
        return waterCurrentY[index & 7];
    }

    /** Signed ground speed for the D-Pad mask and power-up row bit. */
    public int linkSpeedX(int joypadMask) {
        return linkSpeedX[joypadMask & 0x1F];
    }

    /** ApplyLinkGroundMotion's per-axis targets, including opposite-button inputs. */
    public int airSpeedX(int axisMask) {
        return airSpeedX[axisMask & 3];
    }

    public int airSpeedY(int axisMask) {
        return airSpeedY[axisMask & 3];
    }

    public int linkSpeedY(int joypadMask) {
        return linkSpeedY[joypadMask & 0x1F];
    }

    /** Signed swimming target speed from Data_002_4EF0/4F10 or its fast row. */
    public int swimmingSpeedX(int joypadMask, boolean fast) {
        return swimmingSpeedX[(fast ? 0x10 : 0) | (joypadMask & 0x0F)];
    }

    /** Signed swimming target speed from Data_002_4EF0/4F10 or its fast row. */
    public int swimmingSpeedY(int joypadMask, boolean fast) {
        return swimmingSpeedY[(fast ? 0x10 : 0) | (joypadMask & 0x0F)];
    }

    /** Signed direction-indexed entry speed written when flippers start swimming. */
    public int swimmingEntrySpeedX(int romDirection) {
        return swimmingEntrySpeedX[romDirection & 0x03];
    }

    /** Signed direction-indexed entry speed written when flippers start swimming. */
    public int swimmingEntrySpeedY(int romDirection) {
        return swimmingEntrySpeedY[romDirection & 0x03];
    }

    private static int[][] loadRevolvingDoorPalettes(Rom rom) {
        int[][] palettes = new int[2][4];
        int base = RomSymbols.Data_002_4E1C.offset();
        for (int row = 0; row < 2; row++) {
            for (int color = 0; color < 4; color++) {
                int offset = base + row * 8 + color * 2;
                int value = rom.u16le(RomAddress.fromOffset(offset));
                palettes[row][color] = ((value & 31) << 19) | (((value >> 5) & 31) << 11) | (((value >> 10) & 31) << 3);
            }
        }
        return palettes;
    }
}
