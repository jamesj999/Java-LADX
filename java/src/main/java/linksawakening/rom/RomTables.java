package linksawakening.rom;

/**
 * One-time copies of fixed lookup tables that the original game reads from ROM.
 * Kept together so all ROM bank/address constants live in one place.
 */
public final class RomTables {

    private static final int BANK_SIZE = 0x4000;

    // Three consecutive 256-byte physics-flag tables at bank $08:$4AD4:
    // Overworld → Indoors1 → Indoors2. GetObjectPhysicsFlags (bank0.asm:4513)
    // reads {@code table[(wIsIndoor << 8) + objectId]}, picking the right
    // table by the wIsIndoor value in register d.
    private static final int PHYSICS_FLAGS_BANK = 0x08;
    private static final int PHYSICS_FLAGS_ADDR = 0x4AD4;
    private static final int PHYSICS_FLAGS_TABLE_SIZE = 0x100;
    public static final int PHYSICS_TABLE_OVERWORLD = 0;
    public static final int PHYSICS_TABLE_INDOORS1 = 1;
    public static final int PHYSICS_TABLE_INDOORS2 = 2;

    private static final int LINK_SPEED_TABLE_BANK = 0x02;
    private static final int LINK_SPEED_TABLE_X_ADDR = 0x48C5;
    private static final int LINK_SPEED_TABLE_Y_ADDR = 0x48E5;
    private static final int LINK_SPEED_TABLE_LENGTH = 0x20;

    // Sword-swing animation and position tables in bank 2. All 24 bytes,
    // indexed as direction * 6 + wSwordAnimationState. Direction encoding
    // is the ROM's (RIGHT=0, LEFT=1, UP=2, DOWN=3). Addresses derived from
    // the instructions that load them (see bank2.asm:796+, 4827+).
    private static final int SWORD_TABLES_BANK = 0x02;
    private static final int TABLE_LEN = 24;
    // LinkDirectionToSwordDirection (bank2.asm:796)
    private static final int SWORD_DIR_TABLE_ADDR = 0x461E;
    // LinkDirectionToLinkAnimationState1 (bank2.asm:806)
    private static final int SWORD_ANIM_TABLE_ADDR = 0x4636;
    // LinkDirectionTo_wC13A — blade X offset (bank2.asm:816)
    private static final int SWORD_X_OFFSET_TABLE_ADDR = 0x464E;
    // LinkDirectionTo_wC139 — blade Y offset added to Link Y (bank2.asm:821)
    private static final int SWORD_Y_OFFSET_TABLE_ADDR = 0x4666;
    // LinkDirectionTo_wC13B — additional Y offset (bank2.asm:831)
    private static final int SWORD_Y_BASE_TABLE_ADDR = 0x4696;
    private static final int STATIC_SWORD_COLLISION_TABLE_BANK = 0x00;
    private static final int STATIC_SWORD_COLLISION_X_ADDR = 0x158F;
    private static final int STATIC_SWORD_COLLISION_Y_ADDR = 0x159B;
    private static final int STATIC_SWORD_COLLISION_TABLE_LEN = 12;

    // Sword blade sprite tiles + attrs, bank $20. 16 bytes each (8 sword
    // directions x 2 adjacent 8x16 sprites). See Data_020_4A93/4AA3 in
    // bank20.asm and func_020_4AB3 for the OAM layout.
    private static final int SWORD_SPRITE_BANK = 0x20;
    private static final int SWORD_SPRITE_TILES_ADDR = 0x4A93;
    private static final int SWORD_SPRITE_ATTRS_ADDR = 0x4AA3;
    private static final int SWORD_SPRITE_TABLE_LEN = 16;

    private final int[][] physicsFlags;
    private final byte[] linkSpeedX;
    private final byte[] linkSpeedY;
    private final int[] swordAnimState;
    private final int[] swordDirection;
    private final byte[] swordXOffset;
    private final byte[] swordYOffset;
    private final byte[] swordYBase;
    private final int[] swordSpriteTiles;
    private final int[] swordSpriteAttrs;
    private final byte[] staticSwordCollisionX;
    private final byte[] staticSwordCollisionY;

    private RomTables(int[][] physicsFlags, byte[] linkSpeedX, byte[] linkSpeedY,
                      int[] swordAnimState, int[] swordDirection,
                      byte[] swordXOffset, byte[] swordYOffset, byte[] swordYBase,
                      int[] swordSpriteTiles, int[] swordSpriteAttrs,
                      byte[] staticSwordCollisionX, byte[] staticSwordCollisionY) {
        this.physicsFlags = physicsFlags;
        this.linkSpeedX = linkSpeedX;
        this.linkSpeedY = linkSpeedY;
        this.swordAnimState = swordAnimState;
        this.swordDirection = swordDirection;
        this.swordXOffset = swordXOffset;
        this.swordYOffset = swordYOffset;
        this.swordYBase = swordYBase;
        this.swordSpriteTiles = swordSpriteTiles;
        this.swordSpriteAttrs = swordSpriteAttrs;
        this.staticSwordCollisionX = staticSwordCollisionX;
        this.staticSwordCollisionY = staticSwordCollisionY;
    }

    public static RomTables loadFromRom(byte[] romData) {
        int[][] flags = new int[3][PHYSICS_FLAGS_TABLE_SIZE];
        int baseOffset = romOffset(PHYSICS_FLAGS_BANK, PHYSICS_FLAGS_ADDR);
        for (int t = 0; t < 3; t++) {
            for (int i = 0; i < PHYSICS_FLAGS_TABLE_SIZE; i++) {
                flags[t][i] = Byte.toUnsignedInt(romData[baseOffset + t * PHYSICS_FLAGS_TABLE_SIZE + i]);
            }
        }

        byte[] speedX = new byte[LINK_SPEED_TABLE_LENGTH];
        byte[] speedY = new byte[LINK_SPEED_TABLE_LENGTH];
        int speedXOffset = romOffset(LINK_SPEED_TABLE_BANK, LINK_SPEED_TABLE_X_ADDR);
        int speedYOffset = romOffset(LINK_SPEED_TABLE_BANK, LINK_SPEED_TABLE_Y_ADDR);
        System.arraycopy(romData, speedXOffset, speedX, 0, LINK_SPEED_TABLE_LENGTH);
        System.arraycopy(romData, speedYOffset, speedY, 0, LINK_SPEED_TABLE_LENGTH);

        int[] swordAnim = loadUnsignedTable(romData, SWORD_TABLES_BANK, SWORD_ANIM_TABLE_ADDR, TABLE_LEN);
        int[] swordDir = loadUnsignedTable(romData, SWORD_TABLES_BANK, SWORD_DIR_TABLE_ADDR, TABLE_LEN);
        byte[] swordX = loadSignedTable(romData, SWORD_TABLES_BANK, SWORD_X_OFFSET_TABLE_ADDR, TABLE_LEN);
        byte[] swordY = loadSignedTable(romData, SWORD_TABLES_BANK, SWORD_Y_OFFSET_TABLE_ADDR, TABLE_LEN);
        byte[] swordYBaseBytes = loadSignedTable(romData, SWORD_TABLES_BANK, SWORD_Y_BASE_TABLE_ADDR, TABLE_LEN);
        int[] swordTiles = loadUnsignedTable(romData, SWORD_SPRITE_BANK, SWORD_SPRITE_TILES_ADDR, SWORD_SPRITE_TABLE_LEN);
        int[] swordAttrs = loadUnsignedTable(romData, SWORD_SPRITE_BANK, SWORD_SPRITE_ATTRS_ADDR, SWORD_SPRITE_TABLE_LEN);
        byte[] staticSwordCollisionX = loadSignedTable(
            romData, STATIC_SWORD_COLLISION_TABLE_BANK, STATIC_SWORD_COLLISION_X_ADDR, STATIC_SWORD_COLLISION_TABLE_LEN);
        byte[] staticSwordCollisionY = loadSignedTable(
            romData, STATIC_SWORD_COLLISION_TABLE_BANK, STATIC_SWORD_COLLISION_Y_ADDR, STATIC_SWORD_COLLISION_TABLE_LEN);

        return new RomTables(flags, speedX, speedY, swordAnim, swordDir,
                             swordX, swordY, swordYBaseBytes, swordTiles, swordAttrs,
                             staticSwordCollisionX, staticSwordCollisionY);
    }

    private static int[] loadUnsignedTable(byte[] romData, int bank, int addr, int len) {
        int off = romOffset(bank, addr);
        int[] out = new int[len];
        for (int i = 0; i < len; i++) {
            out[i] = Byte.toUnsignedInt(romData[off + i]);
        }
        return out;
    }

    private static byte[] loadSignedTable(byte[] romData, int bank, int addr, int len) {
        int off = romOffset(bank, addr);
        byte[] out = new byte[len];
        System.arraycopy(romData, off, out, 0, len);
        return out;
    }

    /**
     * Physics flag for an object in the given context table. Pick
     * {@link #PHYSICS_TABLE_OVERWORLD} for overworld rooms and
     * {@link #PHYSICS_TABLE_INDOORS1} for regular indoor rooms (houses,
     * caves, dungeons). {@link #PHYSICS_TABLE_INDOORS2} covers Color Dungeon
     * with {@code wIsIndoor != 0}.
     */
    public int objectPhysicsFlag(int tableIndex, int objectId) {
        if (tableIndex < 0 || tableIndex >= physicsFlags.length) {
            return 0;
        }
        if (objectId < 0 || objectId >= PHYSICS_FLAGS_TABLE_SIZE) {
            return 0;
        }
        return physicsFlags[tableIndex][objectId];
    }

    /** Back-compat shim: overworld physics flag for an object id. */
    public int overworldPhysicsFlag(int objectId) {
        return objectPhysicsFlag(PHYSICS_TABLE_OVERWORLD, objectId);
    }

    /** Signed 8-bit speed increment for the given D-Pad mask (R<<3|L<<2|U<<1|D). */
    public int linkSpeedX(int joypadMask) {
        return linkSpeedX[joypadMask & 0x0F];
    }

    public int linkSpeedY(int joypadMask) {
        return linkSpeedY[joypadMask & 0x0F];
    }

    /**
     * Look up an {@code hLinkAnimationState} for (ROM-encoded direction, sword
     * animation state). Returns {@code 0xFF} when the table is marked
     * LINK_ANIMATION_STATE_HIDDEN — callers should treat that as "keep the
     * current state". {@code romDirection} uses the disassembly's encoding
     * (RIGHT=0, LEFT=1, UP=2, DOWN=3), not the Java Link.DIRECTION_* values.
     */
    public int swordDirectionAnimState(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        if (index < 0 || index >= swordAnimState.length) {
            return 0xFF;
        }
        return swordAnimState[index];
    }

    /**
     * Returns the 8-valued sword direction (SWORD_DIRECTION_* in
     * {@code constants/gameplay.asm}) that the ROM assigns for a given
     * Link-direction + sword-animation-state pair.
     */
    public int swordDirectionFor(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        return index < swordDirection.length ? swordDirection[index] : 0;
    }

    /**
     * Blade X offset from Link's world X (wC13A in the ROM). Signed.
     */
    public int swordBladeXOffset(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        return index < swordXOffset.length ? swordXOffset[index] : 0;
    }

    /**
     * Blade Y offset from Link's world Y: wC13B + wC139 combined (see
     * {@code ApplyLinkMotionState} in bank0.asm and
     * {@code func_020_4AB3} in bank20.asm).
     */
    public int swordBladeYOffset(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        if (index >= swordYBase.length || index >= swordYOffset.length) {
            return 0;
        }
        return swordYBase[index] + swordYOffset[index];
    }

    /**
     * Sword blade OAM tile id for the given sword direction (0-7) and sprite
     * slot (0 = left half, 1 = right half). {@code 0xFF} means the slot is
     * hidden (vertical blades only use slot 1).
     */
    public int swordSpriteTile(int swordDirection, int spriteSlot) {
        int index = (swordDirection & 0x7) * 2 + (spriteSlot & 0x1);
        return swordSpriteTiles[index];
    }

    /**
     * OAM attribute byte for the blade sprite (bit 5 = flipX, bit 6 = flipY,
     * bits 0-2 = palette).
     */
    public int swordSpriteAttr(int swordDirection, int spriteSlot) {
        int index = (swordDirection & 0x7) * 2 + (spriteSlot & 0x1);
        return swordSpriteAttrs[index];
    }

    /**
     * Signed X collision offset from Link's position for static-object sword
     * collision in the ROM collision-map index:
     * 0-3 = normal swing (RIGHT, LEFT, UP, DOWN),
     * 4-11 = spin attack angles.
     * Table: {@code SwordCollisionMapX} in
     * {@code CheckStaticSwordCollision}.
     */
    public int staticSwordCollisionOffsetX(int collisionIndex) {
        return staticSwordCollisionX[collisionIndex % staticSwordCollisionX.length];
    }

    /**
     * Signed Y collision offset from Link's position for static-object sword
     * collision in the ROM collision-map index:
     * 0-3 = normal swing (RIGHT, LEFT, UP, DOWN),
     * 4-11 = spin attack angles.
     * Table: {@code SwordCollisionMapY} in
     * {@code CheckStaticSwordCollision}.
     */
    public int staticSwordCollisionOffsetY(int collisionIndex) {
        return staticSwordCollisionY[collisionIndex % staticSwordCollisionY.length];
    }

    private static int swingTableIndex(int romDirection, int swordState) {
        return (romDirection & 0x3) * 6 + (swordState & 0x7);
    }

    private static int romOffset(int bank, int address) {
        if (bank == 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - BANK_SIZE);
    }
}
