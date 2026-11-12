package linksawakening.rom;
/** Immutable copies of SwordTables ROM lookup data. */
public final class SwordTables {
    // LinkDirectionToLinkAnimationState1: original loader length.
    private static final int SWORD_ANIM_STATE_LENGTH = 24;
    private final int[] swordAnimState;
    // LinkDirectionToSwordDirection: original loader length.
    private static final int SWORD_DIRECTION_LENGTH = 24;
    private final int[] swordDirection;
    // LinkDirectionToSwordAnimationState: original loader length.
    private static final int SWORD_SPIN_ANIMATION_STATE_LENGTH = 32;
    private final int[] swordSpinAnimationState;
    // LinkDirectionToAbsolute: original loader length.
    private static final int SWORD_SPIN_ABSOLUTE_DIRECTION_LENGTH = 32;
    private final int[] swordSpinAbsoluteDirection;
    // LinkDirectionTo_wC13A: original loader length.
    private static final int SWORD_X_OFFSET_LENGTH = 24;
    private final byte[] swordXOffset;
    // LinkDirectionTo_wC139: original loader length.
    private static final int SWORD_Y_OFFSET_LENGTH = 24;
    private final byte[] swordYOffset;
    // LinkDirectionTo_wC13B: original loader length.
    private static final int SWORD_Y_BASE_LENGTH = 24;
    private final byte[] swordYBase;
    // LinkDirectionToStaticSwordCollitionCheckNeeded: original loader length.
    private static final int SWORD_COLLISION_NEEDED_LENGTH = 24;
    private final int[] swordCollisionNeeded;
    // LinkDirectionTo_wC141: original loader length.
    private static final int SWORD_COLLISION_WIDTH_LENGTH = 24;
    private final int[] swordCollisionWidth;
    // LinkDirectionToOffset: original loader length.
    private static final int SWORD_COLLISION_OFFSET_LENGTH = 24;
    private final int[] swordCollisionOffset;
    // LinkDirectionTo_wC143: original loader length.
    private static final int SWORD_COLLISION_HEIGHT_LENGTH = 24;
    private final int[] swordCollisionHeight;
    // Data_020_4A93: original loader length.
    private static final int SWORD_SPRITE_TILES_LENGTH = 16;
    private final int[] swordSpriteTiles;
    // Data_020_4AA3: original loader length.
    private static final int SWORD_SPRITE_ATTRS_LENGTH = 16;
    private final int[] swordSpriteAttrs;
    // SwordCollisionMapX: original loader length.
    private static final int STATIC_SWORD_COLLISION_X_LENGTH = 12;
    private final byte[] staticSwordCollisionX;
    // SwordCollisionMapY: original loader length.
    private static final int STATIC_SWORD_COLLISION_Y_LENGTH = 12;
    private final byte[] staticSwordCollisionY;

    private SwordTables(Rom rom) {
        swordAnimState = rom.u8Table(RomSymbols.LinkDirectionToLinkAnimationState1, SWORD_ANIM_STATE_LENGTH);
        swordDirection = rom.u8Table(RomSymbols.LinkDirectionToSwordDirection, SWORD_DIRECTION_LENGTH);
        swordSpinAnimationState =
                rom.u8Table(RomSymbols.LinkDirectionToSwordAnimationState, SWORD_SPIN_ANIMATION_STATE_LENGTH);
        swordSpinAbsoluteDirection =
                rom.u8Table(RomSymbols.LinkDirectionToAbsolute, SWORD_SPIN_ABSOLUTE_DIRECTION_LENGTH);
        swordXOffset = rom.bytes(RomSymbols.LinkDirectionTo_wC13A, SWORD_X_OFFSET_LENGTH);
        swordYOffset = rom.bytes(RomSymbols.LinkDirectionTo_wC139, SWORD_Y_OFFSET_LENGTH);
        swordYBase = rom.bytes(RomSymbols.LinkDirectionTo_wC13B, SWORD_Y_BASE_LENGTH);
        swordCollisionNeeded =
                rom.u8Table(RomSymbols.LinkDirectionToStaticSwordCollitionCheckNeeded, SWORD_COLLISION_NEEDED_LENGTH);
        swordCollisionWidth = rom.u8Table(RomSymbols.LinkDirectionTo_wC141, SWORD_COLLISION_WIDTH_LENGTH);
        swordCollisionOffset = rom.u8Table(RomSymbols.LinkDirectionToOffset, SWORD_COLLISION_OFFSET_LENGTH);
        swordCollisionHeight = rom.u8Table(RomSymbols.LinkDirectionTo_wC143, SWORD_COLLISION_HEIGHT_LENGTH);
        swordSpriteTiles = rom.u8Table(RomSymbols.Data_020_4A93, SWORD_SPRITE_TILES_LENGTH);
        swordSpriteAttrs = rom.u8Table(RomSymbols.Data_020_4AA3, SWORD_SPRITE_ATTRS_LENGTH);
        staticSwordCollisionX = rom.bytes(RomSymbols.SwordCollisionMapX, STATIC_SWORD_COLLISION_X_LENGTH);
        staticSwordCollisionY = rom.bytes(RomSymbols.SwordCollisionMapY, STATIC_SWORD_COLLISION_Y_LENGTH);
    }

    public static SwordTables load(Rom rom) {
        return new SwordTables(rom);
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

    /** ROM {@code LinkDirectionToSwordAnimationState} for a spin sector. */
    public int swordSpinAnimationState(int baseRomDirection, int sector) {
        return swordSpinAnimationState[spinTableIndex(baseRomDirection, sector)];
    }

    /** ROM {@code LinkDirectionToAbsolute} for a spin sector. */
    public int swordSpinAbsoluteDirection(int baseRomDirection, int sector) {
        return swordSpinAbsoluteDirection[spinTableIndex(baseRomDirection, sector)];
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

    /** True when the current sword pose enables the enemy collision rectangle. */
    public boolean swordEnemyCollisionEnabled(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        return index >= 0 && index < swordCollisionNeeded.length && swordCollisionNeeded[index] != 0;
    }

    /** wC140 offset from Link X, including LinkDirectionToStaticSword... . */
    public int swordCollisionXOriginOffset(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        return swordXOffset[index] + swordCollisionNeeded[index];
    }

    /** wC141 half-width used by DefaultEnemyDamageCollisionHandler. */
    public int swordCollisionWidth(int romDirection, int swordState) {
        return swordCollisionWidth[swingTableIndex(romDirection, swordState)];
    }

    /** wC142 offset from Link Y, excluding Link's vertical position. */
    public int swordCollisionYOriginOffset(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        return swordYOffset[index] + swordCollisionOffset[index];
    }

    /** wC143 half-height used by DefaultEnemyDamageCollisionHandler. */
    public int swordCollisionHeight(int romDirection, int swordState) {
        return swordCollisionHeight[swingTableIndex(romDirection, swordState)];
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

    private static int spinTableIndex(int baseRomDirection, int sector) {
        return (baseRomDirection & 0x3) * 8 + (sector & 0x7);
    }
}
