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

    // Options1ForEntity in bank $03, included by entities/bank3.asm. The
    // entity initialization path copies this 256-byte ROM table into
    // wEntitiesOptions1Table before dispatching the entity handler.
    private static final int ENTITY_OPTIONS_BANK = 0x03;
    private static final int ENTITY_OPTIONS_ADDR = 0x42F1;
    private static final int ENTITY_OPTIONS_TABLE_SIZE = 0x100;

    // HitboxFlagsForEntity in bank $03. The table contains entries $00..$FA;
    // the following health-group table begins at $41F6.
    private static final int ENTITY_HITBOX_FLAGS_BANK = 0x03;
    private static final int ENTITY_HITBOX_FLAGS_ADDR = 0x40FB;
    private static final int ENTITY_HITBOX_FLAGS_TABLE_SIZE = 0xFB;

    // Shared entity wall-collision probe tables in bank $03. Each table has
    // four signed offsets for each of the four collision-box types, in ROM
    // direction order: right, left, up, down.
    private static final int ENTITY_COLLISION_POINTS_BANK = 0x03;
    private static final int ENTITY_COLLISION_POINTS_X_ADDR = 0x785F;
    private static final int ENTITY_COLLISION_POINTS_Y_ADDR = 0x786F;
    private static final int ENTITY_COLLISION_POINTS_TABLE_SIZE = 0x10;

    // FineCollisionShapes in bank $03. Four unsigned shape bytes for each
    // physics flag from $7C through $8D.
    private static final int ENTITY_FINE_COLLISION_SHAPES_BANK = 0x03;
    private static final int ENTITY_FINE_COLLISION_SHAPES_ADDR = 0x7A85;
    private static final int ENTITY_FINE_COLLISION_SHAPES_FIRST_PHYSICS = 0x7C;
    private static final int ENTITY_FINE_COLLISION_SHAPES_LAST_PHYSICS = 0x8D;
    private static final int ENTITY_FINE_COLLISION_SHAPES_QUADRANTS = 4;
    private static final int ENTITY_FINE_COLLISION_SHAPES_LENGTH = 0x48;

    // Link uses its own contiguous Data_002_49CA lookup for physics $7C-$8F.
    // Unlike the entity table, the source indexes all twenty four-byte rows,
    // including $8E/$8F at the bytes immediately following the named data.
    private static final int LINK_FINE_COLLISION_SHAPES_BANK = 0x02;
    private static final int LINK_FINE_COLLISION_SHAPES_ADDR = 0x49CA;
    private static final int LINK_FINE_COLLISION_SHAPES_FIRST_PHYSICS = 0x7C;
    private static final int LINK_FINE_COLLISION_SHAPES_LAST_PHYSICS = 0x8F;
    private static final int LINK_FINE_COLLISION_SHAPES_LENGTH = 0x50;

    private static final int LINK_SPEED_TABLE_BANK = 0x02;
    private static final int LINK_SPEED_TABLE_X_ADDR = 0x48C5;
    private static final int LINK_SPEED_TABLE_Y_ADDR = 0x48E5;
    private static final int LINK_SPEED_TABLE_LENGTH = 0x20;

    // Swimming target-speed tables in bank 2. Data_002_4EF0/4F00 and
    // Data_002_4F10/4F20 are two consecutive 16-entry tables: normal
    // swimming followed by the faster A-button swimming cadence.
    private static final int SWIMMING_SPEED_TABLE_X_ADDR = 0x4EF0;
    private static final int SWIMMING_SPEED_TABLE_Y_ADDR = 0x4F10;
    private static final int SWIMMING_SPEED_TABLE_LENGTH = 0x20;
    private static final int SWIMMING_ENTRY_SPEED_X_ADDR = 0x750A;
    private static final int SWIMMING_ENTRY_SPEED_Y_ADDR = 0x750E;
    private static final int SWIMMING_ENTRY_SPEED_LENGTH = 4;

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
    // Sword collision rectangle tables used by DefaultEnemyDamageCollisionHandler.
    private static final int SWORD_COLLISION_NEEDED_TABLE_ADDR = 0x45BE;
    private static final int SWORD_COLLISION_WIDTH_TABLE_ADDR = 0x45D6;
    private static final int SWORD_COLLISION_OFFSET_TABLE_ADDR = 0x45EE;
    private static final int SWORD_COLLISION_HEIGHT_TABLE_ADDR = 0x4606;
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

    // Magic-rod Link OAM tables in bank $02 (bank2.asm:52E0-5307).
    // The first four entries are the forward swing and the second four are
    // the side swing, in ROM direction order RIGHT, LEFT, UP, DOWN.
    private static final int MAGIC_ROD_TABLE_BANK = 0x02;
    private static final int MAGIC_ROD_X_OFFSET_ADDR = 0x52E0;
    private static final int MAGIC_ROD_Y_OFFSET_ADDR = 0x52E8;
    private static final int MAGIC_ROD_TILES_ADDR = 0x52F0;
    private static final int MAGIC_ROD_ATTRIBUTES_ADDR = 0x5300;
    private static final int MAGIC_ROD_DIRECTION_TABLE_LEN = 8;
    private static final int MAGIC_ROD_SPRITE_TABLE_LEN = 16;

    // LinkDirectionToLinkAnimationState2 (bank2.asm:$4B41), used while the
    // ROM's shovel timer is active. The table is indexed by ROM direction
    // (RIGHT, LEFT, UP, DOWN), then by the two timer phases.
    private static final int SHOVEL_ANIMATION_BANK = 0x02;
    private static final int SHOVEL_ANIMATION_ADDR = 0x4B41;
    private static final int SHOVEL_ANIMATION_LENGTH = 8;

    private final int[][] physicsFlags;
    private final int[] entityOptions1;
    private final int[] entityHitboxFlags;
    private final byte[] entityCollisionPointsX;
    private final byte[] entityCollisionPointsY;
    private final int[] entityFineCollisionShapes;
    private final int[] linkFineCollisionShapes;
    private final byte[] linkSpeedX;
    private final byte[] linkSpeedY;
    private final byte[] swimmingSpeedX;
    private final byte[] swimmingSpeedY;
    private final byte[] swimmingEntrySpeedX;
    private final byte[] swimmingEntrySpeedY;
    private final int[] swordAnimState;
    private final int[] swordDirection;
    private final byte[] swordXOffset;
    private final byte[] swordYOffset;
    private final byte[] swordYBase;
    private final int[] swordCollisionNeeded;
    private final int[] swordCollisionWidth;
    private final int[] swordCollisionOffset;
    private final int[] swordCollisionHeight;
    private final int[] swordSpriteTiles;
    private final int[] swordSpriteAttrs;
    private final byte[] magicRodXOffset;
    private final byte[] magicRodYOffset;
    private final int[] magicRodTiles;
    private final int[] magicRodAttributes;
    private final int[] shovelAnimationStates;
    private final byte[] staticSwordCollisionX;
    private final byte[] staticSwordCollisionY;

    private RomTables(int[][] physicsFlags, int[] entityOptions1,
                      int[] entityHitboxFlags, byte[] entityCollisionPointsX,
                      byte[] entityCollisionPointsY,
                      int[] entityFineCollisionShapes,
                      int[] linkFineCollisionShapes,
                      byte[] linkSpeedX, byte[] linkSpeedY,
                      byte[] swimmingSpeedX, byte[] swimmingSpeedY,
                      byte[] swimmingEntrySpeedX, byte[] swimmingEntrySpeedY,
                      int[] swordAnimState, int[] swordDirection,
                      byte[] swordXOffset, byte[] swordYOffset, byte[] swordYBase,
                      int[] swordCollisionNeeded, int[] swordCollisionWidth,
                      int[] swordCollisionOffset, int[] swordCollisionHeight,
                      int[] swordSpriteTiles, int[] swordSpriteAttrs,
                      byte[] magicRodXOffset, byte[] magicRodYOffset,
                      int[] magicRodTiles, int[] magicRodAttributes,
                      int[] shovelAnimationStates,
                      byte[] staticSwordCollisionX, byte[] staticSwordCollisionY) {
        this.physicsFlags = physicsFlags;
        this.entityOptions1 = entityOptions1;
        this.entityHitboxFlags = entityHitboxFlags;
        this.entityCollisionPointsX = entityCollisionPointsX;
        this.entityCollisionPointsY = entityCollisionPointsY;
        this.entityFineCollisionShapes = entityFineCollisionShapes;
        this.linkFineCollisionShapes = linkFineCollisionShapes;
        this.linkSpeedX = linkSpeedX;
        this.linkSpeedY = linkSpeedY;
        this.swimmingSpeedX = swimmingSpeedX;
        this.swimmingSpeedY = swimmingSpeedY;
        this.swimmingEntrySpeedX = swimmingEntrySpeedX;
        this.swimmingEntrySpeedY = swimmingEntrySpeedY;
        this.swordAnimState = swordAnimState;
        this.swordDirection = swordDirection;
        this.swordXOffset = swordXOffset;
        this.swordYOffset = swordYOffset;
        this.swordYBase = swordYBase;
        this.swordCollisionNeeded = swordCollisionNeeded;
        this.swordCollisionWidth = swordCollisionWidth;
        this.swordCollisionOffset = swordCollisionOffset;
        this.swordCollisionHeight = swordCollisionHeight;
        this.swordSpriteTiles = swordSpriteTiles;
        this.swordSpriteAttrs = swordSpriteAttrs;
        this.magicRodXOffset = magicRodXOffset;
        this.magicRodYOffset = magicRodYOffset;
        this.magicRodTiles = magicRodTiles;
        this.magicRodAttributes = magicRodAttributes;
        this.shovelAnimationStates = shovelAnimationStates;
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

        int[] options1 = loadUnsignedTable(
            romData, ENTITY_OPTIONS_BANK, ENTITY_OPTIONS_ADDR, ENTITY_OPTIONS_TABLE_SIZE);
        int[] hitboxFlags = loadUnsignedTable(
            romData, ENTITY_HITBOX_FLAGS_BANK, ENTITY_HITBOX_FLAGS_ADDR,
            ENTITY_HITBOX_FLAGS_TABLE_SIZE);
        byte[] collisionPointsX = loadSignedTable(
            romData, ENTITY_COLLISION_POINTS_BANK, ENTITY_COLLISION_POINTS_X_ADDR,
            ENTITY_COLLISION_POINTS_TABLE_SIZE);
        byte[] collisionPointsY = loadSignedTable(
            romData, ENTITY_COLLISION_POINTS_BANK, ENTITY_COLLISION_POINTS_Y_ADDR,
            ENTITY_COLLISION_POINTS_TABLE_SIZE);
        int[] fineCollisionShapes = loadUnsignedTable(
            romData, ENTITY_FINE_COLLISION_SHAPES_BANK, ENTITY_FINE_COLLISION_SHAPES_ADDR,
            ENTITY_FINE_COLLISION_SHAPES_LENGTH);
        int[] linkFineCollisionShapes = loadUnsignedTable(
            romData, LINK_FINE_COLLISION_SHAPES_BANK, LINK_FINE_COLLISION_SHAPES_ADDR,
            LINK_FINE_COLLISION_SHAPES_LENGTH);

        byte[] speedX = new byte[LINK_SPEED_TABLE_LENGTH];
        byte[] speedY = new byte[LINK_SPEED_TABLE_LENGTH];
        int speedXOffset = romOffset(LINK_SPEED_TABLE_BANK, LINK_SPEED_TABLE_X_ADDR);
        int speedYOffset = romOffset(LINK_SPEED_TABLE_BANK, LINK_SPEED_TABLE_Y_ADDR);
        System.arraycopy(romData, speedXOffset, speedX, 0, LINK_SPEED_TABLE_LENGTH);
        System.arraycopy(romData, speedYOffset, speedY, 0, LINK_SPEED_TABLE_LENGTH);

        byte[] swimmingX = new byte[SWIMMING_SPEED_TABLE_LENGTH];
        byte[] swimmingY = new byte[SWIMMING_SPEED_TABLE_LENGTH];
        int swimmingXOffset = romOffset(LINK_SPEED_TABLE_BANK, SWIMMING_SPEED_TABLE_X_ADDR);
        int swimmingYOffset = romOffset(LINK_SPEED_TABLE_BANK, SWIMMING_SPEED_TABLE_Y_ADDR);
        System.arraycopy(romData, swimmingXOffset, swimmingX, 0,
            SWIMMING_SPEED_TABLE_LENGTH);
        System.arraycopy(romData, swimmingYOffset, swimmingY, 0,
            SWIMMING_SPEED_TABLE_LENGTH);
        byte[] swimmingEntryX = loadSignedTable(
            romData, LINK_SPEED_TABLE_BANK, SWIMMING_ENTRY_SPEED_X_ADDR,
            SWIMMING_ENTRY_SPEED_LENGTH);
        byte[] swimmingEntryY = loadSignedTable(
            romData, LINK_SPEED_TABLE_BANK, SWIMMING_ENTRY_SPEED_Y_ADDR,
            SWIMMING_ENTRY_SPEED_LENGTH);

        int[] swordAnim = loadUnsignedTable(romData, SWORD_TABLES_BANK, SWORD_ANIM_TABLE_ADDR, TABLE_LEN);
        int[] swordDir = loadUnsignedTable(romData, SWORD_TABLES_BANK, SWORD_DIR_TABLE_ADDR, TABLE_LEN);
        byte[] swordX = loadSignedTable(romData, SWORD_TABLES_BANK, SWORD_X_OFFSET_TABLE_ADDR, TABLE_LEN);
        byte[] swordY = loadSignedTable(romData, SWORD_TABLES_BANK, SWORD_Y_OFFSET_TABLE_ADDR, TABLE_LEN);
        byte[] swordYBaseBytes = loadSignedTable(romData, SWORD_TABLES_BANK, SWORD_Y_BASE_TABLE_ADDR, TABLE_LEN);
        int[] swordCollisionNeeded = loadUnsignedTable(
            romData, SWORD_TABLES_BANK, SWORD_COLLISION_NEEDED_TABLE_ADDR, TABLE_LEN);
        int[] swordCollisionWidth = loadUnsignedTable(
            romData, SWORD_TABLES_BANK, SWORD_COLLISION_WIDTH_TABLE_ADDR, TABLE_LEN);
        int[] swordCollisionOffset = loadUnsignedTable(
            romData, SWORD_TABLES_BANK, SWORD_COLLISION_OFFSET_TABLE_ADDR, TABLE_LEN);
        int[] swordCollisionHeight = loadUnsignedTable(
            romData, SWORD_TABLES_BANK, SWORD_COLLISION_HEIGHT_TABLE_ADDR, TABLE_LEN);
        int[] swordTiles = loadUnsignedTable(romData, SWORD_SPRITE_BANK, SWORD_SPRITE_TILES_ADDR, SWORD_SPRITE_TABLE_LEN);
        int[] swordAttrs = loadUnsignedTable(romData, SWORD_SPRITE_BANK, SWORD_SPRITE_ATTRS_ADDR, SWORD_SPRITE_TABLE_LEN);
        byte[] magicRodX = loadSignedTable(romData, MAGIC_ROD_TABLE_BANK,
            MAGIC_ROD_X_OFFSET_ADDR, MAGIC_ROD_DIRECTION_TABLE_LEN);
        byte[] magicRodY = loadSignedTable(romData, MAGIC_ROD_TABLE_BANK,
            MAGIC_ROD_Y_OFFSET_ADDR, MAGIC_ROD_DIRECTION_TABLE_LEN);
        int[] magicRodTiles = loadUnsignedTable(romData, MAGIC_ROD_TABLE_BANK,
            MAGIC_ROD_TILES_ADDR, MAGIC_ROD_SPRITE_TABLE_LEN);
        int[] magicRodAttrs = loadUnsignedTable(romData, MAGIC_ROD_TABLE_BANK,
            MAGIC_ROD_ATTRIBUTES_ADDR, MAGIC_ROD_SPRITE_TABLE_LEN);
        int[] shovelAnimationStates = loadUnsignedTable(
            romData, SHOVEL_ANIMATION_BANK, SHOVEL_ANIMATION_ADDR,
            SHOVEL_ANIMATION_LENGTH);
        byte[] staticSwordCollisionX = loadSignedTable(
            romData, STATIC_SWORD_COLLISION_TABLE_BANK, STATIC_SWORD_COLLISION_X_ADDR, STATIC_SWORD_COLLISION_TABLE_LEN);
        byte[] staticSwordCollisionY = loadSignedTable(
            romData, STATIC_SWORD_COLLISION_TABLE_BANK, STATIC_SWORD_COLLISION_Y_ADDR, STATIC_SWORD_COLLISION_TABLE_LEN);

        return new RomTables(flags, options1, hitboxFlags, collisionPointsX,
                             collisionPointsY, fineCollisionShapes, linkFineCollisionShapes,
                             speedX, speedY, swimmingX, swimmingY,
                             swimmingEntryX, swimmingEntryY,
                             swordAnim, swordDir,
                             swordX, swordY, swordYBaseBytes,
                             swordCollisionNeeded, swordCollisionWidth,
                             swordCollisionOffset, swordCollisionHeight,
                             swordTiles, swordAttrs,
                             magicRodX, magicRodY, magicRodTiles, magicRodAttrs,
                             shovelAnimationStates,
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

    /**
     * ROM {@code Options1ForEntity[entityType]} from bank $03. Bit $10 in
     * this byte is {@code ENTITY_OPT1_B_NO_GROUND_INTERACTION}.
     */
    public int entityOptions1(int entityType) {
        if (entityType < 0 || entityType >= ENTITY_OPTIONS_TABLE_SIZE) {
            return 0;
        }
        return entityOptions1[entityType];
    }

    /** ROM {@code HitboxFlagsForEntity[entityType]}. */
    public int entityHitboxFlags(int entityType) {
        if (entityType < 0 || entityType >= entityHitboxFlags.length) {
            return 0;
        }
        return entityHitboxFlags[entityType];
    }

    /** The low two bits used by ApplyEntityInteractionWithBackground. */
    public int entityCollisionBoxType(int entityType) {
        return entityHitboxFlags(entityType) & 0x03;
    }

    /** Signed X offset from EntityCollisionPointsX[box][direction]. */
    public int entityCollisionPointX(int collisionBoxType, int direction) {
        int index = collisionPointIndex(collisionBoxType, direction);
        return entityCollisionPointsX[index];
    }

    /** Signed Y offset from EntityCollisionPointsY[box][direction]. */
    public int entityCollisionPointY(int collisionBoxType, int direction) {
        int index = collisionPointIndex(collisionBoxType, direction);
        return entityCollisionPointsY[index];
    }

    /** Unsigned fine-collision shape byte for a physics flag and quadrant. */
    public int entityFineCollisionShape(int physicsFlag, int quadrant) {
        if (physicsFlag < ENTITY_FINE_COLLISION_SHAPES_FIRST_PHYSICS
            || physicsFlag > ENTITY_FINE_COLLISION_SHAPES_LAST_PHYSICS
            || quadrant < 0 || quadrant >= ENTITY_FINE_COLLISION_SHAPES_QUADRANTS) {
            return 0;
        }
        return entityFineCollisionShapes[
            (physicsFlag - ENTITY_FINE_COLLISION_SHAPES_FIRST_PHYSICS)
                * ENTITY_FINE_COLLISION_SHAPES_QUADRANTS + quadrant];
    }

    /** Unsigned Link collision byte from Data_002_49CA[physics-$7C][quadrant]. */
    public int linkFineCollisionShape(int physicsFlag, int quadrant) {
        if (physicsFlag < LINK_FINE_COLLISION_SHAPES_FIRST_PHYSICS
            || physicsFlag > LINK_FINE_COLLISION_SHAPES_LAST_PHYSICS
            || quadrant < 0 || quadrant >= ENTITY_FINE_COLLISION_SHAPES_QUADRANTS) {
            return 0;
        }
        return linkFineCollisionShapes[
            (physicsFlag - LINK_FINE_COLLISION_SHAPES_FIRST_PHYSICS)
                * ENTITY_FINE_COLLISION_SHAPES_QUADRANTS + quadrant];
    }

    private static int collisionPointIndex(int collisionBoxType, int direction) {
        if (collisionBoxType < 0 || collisionBoxType >= 4
            || direction < 0 || direction >= 4) {
            return 0;
        }
        return collisionBoxType * 4 + direction;
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

    /** True when the current sword pose enables the enemy collision rectangle. */
    public boolean swordEnemyCollisionEnabled(int romDirection, int swordState) {
        int index = swingTableIndex(romDirection, swordState);
        return index >= 0 && index < swordCollisionNeeded.length
            && swordCollisionNeeded[index] != 0;
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

    /** Signed Link-top-left X offset for one Magic Rod swing direction. */
    public int magicRodXOffset(int tableIndex) {
        return magicRodXOffset[tableIndex & 0x07];
    }

    /** Signed Link-top-left Y offset for one Magic Rod swing direction. */
    public int magicRodYOffset(int tableIndex) {
        return magicRodYOffset[tableIndex & 0x07];
    }

    /** Link-character tile used by one Magic Rod OAM slot. */
    public int magicRodTile(int tableIndex, int spriteSlot) {
        return magicRodTiles[(tableIndex & 0x07) * 2 + (spriteSlot & 0x01)];
    }

    /** OAM attributes used by one Magic Rod OAM slot. */
    public int magicRodAttribute(int tableIndex, int spriteSlot) {
        return magicRodAttributes[(tableIndex & 0x07) * 2 + (spriteSlot & 0x01)];
    }

    /**
     * Link animation state selected by the ROM shovel timer. Timer values
     * below $10 use phase zero; values from $10 onward use phase one.
     */
    public int shovelAnimationState(int timer, int romDirection) {
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM shovel direction out of range: "
                + romDirection);
        }
        int phase = ((timer & 0xFF) >>> 4) & 0x01;
        return shovelAnimationStates[(romDirection & 0x03) * 2 + phase];
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
