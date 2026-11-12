package linksawakening.rom;
/** Immutable copies of LinkMotionTables ROM lookup data. */
public final class LinkMotionTables {
    // FrameCounterToLinkDirection: original loader length.
    private static final int PIT_WARP_ARRIVAL_DIRECTIONS_LENGTH = 4;
    private final byte[] pitWarpArrivalDirections;
    // Data_002_7C40: original loader length.
    private static final int WATER_CURRENT_X_LENGTH = 8;
    private final byte[] waterCurrentX;
    // Data_002_7C48: original loader length.
    private static final int WATER_CURRENT_Y_LENGTH = 8;
    private final byte[] waterCurrentY;
    // HorizontalIncrementForLinkPosition: original loader length.
    private static final int LINK_SPEED_X_LENGTH = 32;
    private final byte[] linkSpeedX;
    // VerticalIncrementForLinkPosition: original loader length.
    private static final int LINK_SPEED_Y_LENGTH = 32;
    private final byte[] linkSpeedY;
    // Data_002_68B1: original loader length.
    private static final int AIR_SPEED_X_LENGTH = 4;
    private final byte[] airSpeedX;
    // Data_002_44E7: original loader length.
    private static final int AIR_SPEED_Y_LENGTH = 4;
    private final byte[] airSpeedY;
    // Data_002_4EF0: original loader length.
    private static final int SWIMMING_SPEED_X_LENGTH = 32;
    private final byte[] swimmingSpeedX;
    // Data_002_4F10: original loader length.
    private static final int SWIMMING_SPEED_Y_LENGTH = 32;
    private final byte[] swimmingSpeedY;
    // Data_002_750A: original loader length.
    private static final int SWIMMING_ENTRY_SPEED_X_LENGTH = 4;
    private final byte[] swimmingEntrySpeedX;
    // Data_002_750E: original loader length.
    private static final int SWIMMING_ENTRY_SPEED_Y_LENGTH = 4;
    private final byte[] swimmingEntrySpeedY;
    // LinkRevolvingDoorAnimation: original loader length.
    private static final int REVOLVING_DOOR_ANIMATION_LENGTH = 7;
    private final int[] revolvingDoorAnimation;
    // Data_002_4E1C: original loader length.
    private static final int REVOLVING_DOOR_PALETTES_LENGTH = 8;
    private final int[][] revolvingDoorPalettes;

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

    public int revolvingDoorAnimation(int frame) {
        return revolvingDoorAnimation[(frame >> 3) % 7];
    }

    public int[] revolvingDoorPalette(int variant) {
        return revolvingDoorPalettes[variant].clone();
    }

    public int pitWarpArrivalDirection(int frameCounter) {
        return pitWarpArrivalDirections[(frameCounter >>> 2) & 3];
    }

    public int waterCurrentSpeedX(int index) {
        return waterCurrentX[index & 7];
    }

    public int waterCurrentSpeedY(int index) {
        return waterCurrentY[index & 7];
    }

    public int linkSpeedX(int joypadMask) {
        return linkSpeedX[joypadMask & 0x1F];
    }

    public int airSpeedX(int axisMask) {
        return airSpeedX[axisMask & 3];
    }

    public int airSpeedY(int axisMask) {
        return airSpeedY[axisMask & 3];
    }

    public int linkSpeedY(int joypadMask) {
        return linkSpeedY[joypadMask & 0x1F];
    }

    public int swimmingSpeedX(int joypadMask, boolean fast) {
        return swimmingSpeedX[(fast ? 0x10 : 0) | (joypadMask & 0x0F)];
    }

    public int swimmingSpeedY(int joypadMask, boolean fast) {
        return swimmingSpeedY[(fast ? 0x10 : 0) | (joypadMask & 0x0F)];
    }

    public int swimmingEntrySpeedX(int romDirection) {
        return swimmingEntrySpeedX[romDirection & 0x03];
    }

    public int swimmingEntrySpeedY(int romDirection) {
        return swimmingEntrySpeedY[romDirection & 0x03];
    }
}
