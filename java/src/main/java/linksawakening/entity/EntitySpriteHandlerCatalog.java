package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;
import linksawakening.world.EntityStatus;
import linksawakening.world.MoldormMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * ROM-backed display-list decoder plus the small handler-to-list mapping
 * required by the first entity rendering increment.
 */
public final class EntitySpriteHandlerCatalog {

    public static final int ENTITY_LIFTABLE_ROCK = 0x05;
    public static final int ENTITY_CHEST_WITH_ITEM = 0x07;
    public static final int ENTITY_KEY_DROP_POINT = 0x30;
    public static final int ENTITY_STAR = 0x9C;
    public static final int ENTITY_BLOOPER = 0xA9;
    public static final int ENTITY_WINGED_OCTOROK = 0xAE;
    public static final int ENTITY_PINCER = 0xB0;
    public static final int ENTITY_BUSH_CRAWLER = 0xBB;
    public static final int ENTITY_MIMIC = 0x28;
    public static final int ENTITY_MINI_MOLDORM = 0x29;
    public static final int ENTITY_MOLDORM = 0x59;
    public static final int ENTITY_ROLLING_BONES = 0x81;
    public static final int ENTITY_ROLLING_BONES_BAR = 0x82;
    public static final int ENTITY_MASKED_MIMIC_GORIYA = 0x8F;
    public static final int ENTITY_CUCCO = 0x6C;
    public static final int ENTITY_GOPONGA_FLOWER = 0x7E;
    public static final int ENTITY_GIANT_GOPONGA_FLOWER = 0x7C;
    public static final int ENTITY_GOPONGA_FLOWER_PROJECTILE = 0x7D;
    public static final int ENTITY_POKEY = 0xE3;
    public static final int ENTITY_PIRANHA_PLANT = 0xA2;
    public static final int ENTITY_ZORA = 0xCB;
    public static final int ENTITY_ZOMBIE = 0xBF;
    public static final int ENTITY_BUZZ_BLOB = 0xB9;
    public static final int ENTITY_SAND_CRAB = 0xC6;
    public static final int ENTITY_URCHIN = 0xC5;
    public static final int ENTITY_WITCH_RAT = 0xE1;
    public static final int ENTITY_WITCH = 0x40;
    public static final int LIFTABLE_ROCK_INTACT_ROCK_VARIANT = 0;
    public static final int LIFTABLE_ROCK_INTACT_BUSH_VARIANT = 1;
    public static final int LIFTABLE_ROCK_SMASHED_ROCK_VARIANT_BASE = 2;
    public static final int LIFTABLE_ROCK_CUT_LEAVES_VARIANT_BASE = 6;
    public static final int LIFTABLE_ROCK_CUT_LEAVES_SWAMP_VARIANT_BASE = 14;

    private static final int ENTITY_ARROW = 0x00;
    private static final int ENTITY_BOOMERANG = 0x01;
    private static final int ENTITY_MAGIC_ROD_FIREBALL = 0x04;
    private static final int ENTITY_MAGIC_POWDER_SPRINKLE = 0x08;
    private static final int ENTITY_MUSICAL_NOTE = 0xC9;
    private static final int ENTITY_SWORD_BEAM = 0xDF;
    private static final int ENTITY_BOMB = 0x02;
    private static final int ENTITY_BUTTERFLY = 0x6E;
    private static final int ENTITY_HOOKSHOT_CHAIN = 0x03;
    private static final int ENTITY_CRYSTAL_SWITCH = 0x66;
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_IRON_MASK = 0x24;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;
    private static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    private static final int ENTITY_TIMER_BOMBITE = 0x56;
    private static final int ENTITY_MAD_BOMBER = 0x93;
    private static final int ENTITY_BOMBER = 0xBA;
    private static final int ENTITY_MOBLIN_SWORD = 0x14;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_LEEVER = 0x0E;
    private static final int ENTITY_ANTI_FAIRY = 0x15;
    private static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    private static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    private static final int ENTITY_ZOL = 0x1B;
    private static final int ENTITY_GEL = 0x1C;
    private static final int ENTITY_HIDING_ZOL = 0x9B;
    private static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    private static final int ENTITY_STALFOS_EVASIVE = 0x1E;
    private static final int ENTITY_GIBDO = 0x1F;
    private static final int ENTITY_LIKE_LIKE = 0x23;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_WIZROBE = 0x21;
    private static final int ENTITY_WIZROBE_PROJECTILE = 0x22;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_ARMOS_KNIGHT = 0x88;
    private static final int ENTITY_HIDING_GHINI = 0x10;
    private static final int ENTITY_GIANT_GHINI = 0x11;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_POLS_VOICE = 0x18;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_GOOMBA = 0x9F;
    private static final int ENTITY_SNAKE = 0xA1;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;
    private static final int ENTITY_SPIKED_BEETLE = 0x2C;
    private static final int ENTITY_LASER = 0x2A;
    private static final int ENTITY_LASER_BEAM = 0x2B;
    private static final int ENTITY_SPIKE_TRAP = 0x27;
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int ENTITY_PAIRODD_PROJECTILE = 0x58;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
    private static final int ENTITY_FISH = 0xCC;
    private static final int ENTITY_BOW_WOW = 0x6D;
    private static final int ENTITY_DOG = 0x6F;
    private static final int ENTITY_KID_70 = 0x70;
    private static final int ENTITY_KID_73 = 0x73;
    private static final int ENTITY_CROW = 0x7A;
    private static final int ENTITY_BOO_BUDDY = 0x50;
    private static final int ENTITY_DROPPABLE_HEART = 0x2D;
    private static final int ENTITY_DROPPABLE_RUPEE = 0x2E;
    private static final int ENTITY_DROPPABLE_FAIRY = 0x2F;
    private static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;
    private static final int ENTITY_IRON_MASKS_MASK = 0x32;
    private static final int ENTITY_PIECE_OF_POWER = 0x33;
    private static final int ENTITY_GUARDIAN_ACORN = 0x34;
    private static final int ENTITY_HEART_PIECE = 0x35;
    private static final int ENTITY_HEART_CONTAINER = 0x36;
    private static final int ENTITY_DROPPABLE_ARROWS = 0x37;
    private static final int ENTITY_DROPPABLE_BOMBS = 0x38;
    private static final int ENTITY_SLEEPY_TOADSTOOL = 0x3A;
    private static final int ENTITY_DROPPABLE_MAGIC_POWDER = 0x3B;
    private static final int ENTITY_HIDING_SLIME_KEY = 0x3C;
    private static final int ENTITY_DROPPABLE_SECRET_SEASHELL = 0x3D;
    private static final int ENTITY_FLOATING_ITEM = 0x86;
    private static final int ENTITY_FLOATING_ITEM_2 = 0xE5;
    private static final int ENTITY_MARIN = 0x3E;
    private static final int ENTITY_TARIN = 0x3F;
    private static final int ENTITY_GRANDPA_ULRIRA = 0x77;
    private static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    private static final int ENTITY_MARIN_AT_TAL_TAL_HEIGHTS = 0xC2;
    private static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    private static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;

    private static final int[] COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES = {
        0x6688, 0x66E8, 0x6748
    };
    private static final int[] COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES = {
        0x67A8, 0x67D8, 0x6808
    };

    private final byte[] romData;

    public EntitySpriteHandlerCatalog(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
    }

    public EntitySpriteDefinition forEntityType(int entityType,
                                                 EntityRoomLoader.RoomTable roomTable) {
        return forEntityType(entityType, roomTable, -1);
    }

    /** Selects the runtime-dependent display list used by entity $31. */
    public EntitySpriteDefinition forSwordShieldPickup(boolean beachSword) {
        return decodeSingle(ENTITY_SWORD_SHIELD_PICKUP, 0x03,
            beachSword ? 0x5B97 : 0x5B95, 1, 0);
    }

    /** Selects the perched pair or four-sprite flying layout used by the owl event. */
    public EntitySpriteDefinition forOwlEvent(boolean flying) {
        return flying
            ? decodeRectangle(0x41, 0x06, 0x6A3F, 1, 4, 0)
            : decodePair(0x41, 0x06, 0x6A37, 2, 0);
    }

    public EntitySpriteDefinition forEntityType(int entityType,
                                                 EntityRoomLoader.RoomTable roomTable,
                                                 int mapId) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        if (roomTable == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }

        // Color Dungeon's $86 handler is bank-$36 func_036_4F9B, not the
        // ordinary bank-$06 floating-item display path. That branch remains
        // intentionally deferred until its dedicated renderer is modeled.
        if ((entityType == ENTITY_FLOATING_ITEM || entityType == ENTITY_FLOATING_ITEM_2)
            && !(roomTable == EntityRoomLoader.RoomTable.COLOR_DUNGEON
                && entityType == ENTITY_FLOATING_ITEM)) {
            return decodeFloatingItemMain(entityType);
        }

        if (isColorShellType(entityType)) {
            // Room streams begin in INIT. The bank-$36 renderer uses its
            // inactive four-variant list until the first active handler tick.
            return forColorShellState(entityType, 0, EntityStatus.INIT);
        }

        if (entityType == ENTITY_LIFTABLE_ROCK) {
            return decodeLiftableRock(roomTable);
        }
        if (entityType == 0x41) {
            return forOwlEvent(false);
        }
        if (entityType == ENTITY_CHEST_WITH_ITEM) {
            return decodePair(entityType, 0x07, 0x7B57, 0x21, 0);
        }
        if (entityType == ENTITY_ARROW) {
            return decodePair(entityType, 0x03, 0x6BC6, 4, 0);
        }
        if (entityType == ENTITY_BOOMERANG) {
            return decodePair(entityType, 0x19, 0x4451, 4, 0);
        }
        if (entityType == ENTITY_MAGIC_ROD_FIREBALL) {
            return decodePair(entityType, 0x03, 0x69AA, 2, 0);
        }
        if (entityType == ENTITY_MAGIC_POWDER_SPRINKLE) {
            return decodeRectangle(entityType, 0x18, 0x7ABA, 8, 3, 0);
        }
        if (entityType == ENTITY_MUSICAL_NOTE) {
            return decodeSingle(entityType, 0x05, 0x7EF8, 1, 0);
        }
        if (entityType == ENTITY_SWORD_BEAM) {
            return decodeRectangle(entityType, 0x19, 0x44FC, 4, 2, 0);
        }
        if (entityType == ENTITY_BOMB) {
            return decodeSingle(entityType, 0x03, 0x652E, 1, 0);
        }
        if (entityType == ENTITY_CROW) {
            return decodePair(entityType, 0x06, 0x5C89, 4, 2);
        }
        if (entityType == ENTITY_CUCCO) {
            return decodePair(entityType, 0x05, 0x4514, 4, 0);
        }
        if (entityType == ENTITY_GOPONGA_FLOWER) {
            return decodePair(entityType, 0x06, 0x63F4, 2, 0);
        }
        if (entityType == ENTITY_GIANT_GOPONGA_FLOWER) {
            return decodeRectangle(entityType, 0x06, 0x6316, 3, 8, 0);
        }
        if (entityType == ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return decodePair(entityType, 0x06, 0x638F, 4, 0);
        }
        if (entityType == ENTITY_POKEY) {
            return forPokeyState(0);
        }
        if (entityType == ENTITY_PIRANHA_PLANT) {
            return decodeRectangle(entityType, 0x36, 0x6FC2, 6, 4, 0);
        }
        if (entityType == ENTITY_ZORA) {
            return decodePair(entityType, 0x18, 0x49C0, 5, 0);
        }
        if (entityType == ENTITY_ZOMBIE) {
            return decodePair(entityType, 0x18, 0x63F8, 5, 0);
        }
        if (entityType == ENTITY_BUZZ_BLOB) {
            return decodePair(entityType, 0x18, 0x7729, 9, 0);
        }
        if (entityType == ENTITY_SAND_CRAB) {
            return decodePair(entityType, 0x15, 0x7320, 2, 0);
        }
        if (entityType == ENTITY_URCHIN) {
            return forUrchinState(false);
        }
        if (entityType == ENTITY_WITCH_RAT) {
            return decodePair(entityType, 0x15, 0x788D, 4, 0);
        }
        if (entityType == ENTITY_WITCH) {
            return decodeRectangle(entityType, 0x05, 0x4780, 4, 4, 0);
        }
        if (entityType == ENTITY_BOO_BUDDY) {
            return decodePair(entityType, 0x06, 0x79A9, 8, 0);
        }
        if (entityType == ENTITY_CRYSTAL_SWITCH) {
            return decodePair(entityType, 0x15, 0x4320, 1, 0);
        }
        if (entityType == ENTITY_HOOKSHOT_CHAIN) {
            // HookshotChainSpriteVariants is an inline handler list in
            // entities/18_hookshot_chain.asm, not a bank display-list label.
            return new EntitySpriteDefinition(entityType, -1, -1,
                EntitySpriteDefinition.Shape.PAIR, 0, List.of(
                    new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x36, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0x36, 0x20))));
        }
        if (entityType == ENTITY_BOW_WOW) {
            return decodePair(entityType, 0x05, 0x4000, 7, 0);
        }
        if (entityType == ENTITY_DOG) {
            return decodePair(entityType, 0x19, 0x48CA, 4, 2);
        }
        if (entityType == ENTITY_MARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                ? decodePair(entityType, 0x05, 0x4E0A, 8, 6)
                : decodePair(entityType, 0x05, 0x4E2A, 11, 6);
        }
        if (entityType == ENTITY_TARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                ? decodePair(entityType, 0x05, 0x4932, 4, 0)
                : decodePair(entityType, 0x05, 0x4912, 6, 0);
        }
        if (entityType == ENTITY_MARIN_AT_TAL_TAL_HEIGHTS) {
            return decodePair(entityType, 0x18, 0x5EB7, 8, 0);
        }
        if (entityType == ENTITY_KID_70 || entityType == ENTITY_KID_73) {
            return decodePair(entityType, 0x06, 0x604D, 4, 0);
        }
        if (entityType == ENTITY_BUTTERFLY) {
            return decodeSingle(entityType, 0x06, 0x6BBD, 2, 0);
        }
        if (entityType == ENTITY_KEESE) {
            return decodePair(entityType, 0x06, mapId == 0x0A ? 0x6710 : 0x6708, 2, 0);
        }
        if (entityType == ENTITY_GOOMBA) {
            return decodePair(entityType, 0x07, 0x65CE, 3, 0);
        }
        if (entityType == ENTITY_SNAKE) {
            return decodePair(entityType, 0x07, 0x683E, 4, 0);
        }
        if (entityType == ENTITY_BUSH_CRAWLER) {
            return decodePair(entityType, 0x07,
                isIndoorRoomTable(roomTable) ? 0x401A : 0x4012, 2, 0,
                EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
        }
        if (entityType == ENTITY_WIZROBE) {
            return decodePair(entityType, 0x06, 0x7604, 5, 0);
        }
        if (entityType == ENTITY_WIZROBE_PROJECTILE) {
            return decodePair(entityType, 0x06, 0x65E1, 4, 0);
        }
        if (entityType == ENTITY_OCTOROK) {
            return decodePair(entityType, 0x03, 0x57FB, 8, 0);
        }
        if (entityType == ENTITY_OCTOROK_ROCK) {
            return decodePair(entityType, 0x03, 0x6A1E, 2, 0);
        }
        if (entityType == ENTITY_MOBLIN) {
            return decodePair(entityType, 0x03, 0x5917, 8, 0);
        }
        if (entityType == ENTITY_IRON_MASK) {
            return forIronMaskState(0);
        }
        if (entityType == ENTITY_BOMBER) {
            return decodeRectangle(entityType, 0x18, 0x77ED, 4, 3, 0);
        }
        if (entityType == ENTITY_MAD_BOMBER) {
            return decodePair(entityType, 0x06, 0x4126, 5, 0);
        }
        if (entityType == ENTITY_BOUNCING_BOMBITE) {
            return decodePair(entityType, 0x04, mapId == 0x07 ? 0x7E0D : 0x7DF5, 2, 0);
        }
        if (entityType == ENTITY_TIMER_BOMBITE) {
            return decodePair(entityType, 0x04, mapId == 0x07 ? 0x7D07 : 0x7CEF, 6, 0);
        }
        if (entityType == ENTITY_MOBLIN_SWORD) {
            return decodeMoblinSword(entityType);
        }
        if (entityType == ENTITY_MOBLIN_ARROW) {
            return decodePair(entityType, 0x03, 0x6BC6, 4, 0);
        }
        if (entityType == ENTITY_TEKTITE) {
            return decodePair(entityType, 0x06, 0x78B7, 2, 0);
        }
        if (entityType == ENTITY_LEEVER) {
            return decodePair(entityType, 0x04, 0x7EE5, 4, 0);
        }
        if (entityType == ENTITY_MIMIC) {
            // The eighth pair consumes the $21 byte at Data_019_6AAC,
            // immediately following the source's final commented attribute.
            return decodePair(entityType, 0x19, 0x6A8D, 8, 0);
        }
        if (entityType == ENTITY_MINI_MOLDORM) {
            return decodePair(entityType, 0x04, 0x5A49, 10, 0);
        }
        if (entityType == ENTITY_MOLDORM) {
            return forMoldormState(0, 0, 0, List.of(
                new MoldormMotion.TailPosition(0, 0),
                new MoldormMotion.TailPosition(0, 0),
                new MoldormMotion.TailPosition(0, 0),
                new MoldormMotion.TailPosition(0, 0)), 0, 0);
        }
        if (entityType == ENTITY_ROLLING_BONES) {
            return decodeRectangle(entityType, 0x06, 0x6E1E, 6, 4, 0);
        }
        if (entityType == ENTITY_ROLLING_BONES_BAR) {
            return forRollingBonesBar(0, 0x38);
        }
        if (entityType == ENTITY_MASKED_MIMIC_GORIYA && mapId != 0x1F) {
            return decodePair(entityType, 0x19, 0x4796, 8, 0);
        }
        if (entityType == ENTITY_ANTI_FAIRY) {
            return decodePair(entityType, 0x06, 0x786E, 2, 0);
        }
        if (entityType == ENTITY_SPARK_COUNTER_CLOCKWISE
            || entityType == ENTITY_SPARK_CLOCKWISE) {
            return decodePair(entityType, 0x06, 0x6615, 2, 0);
        }
        if (entityType == ENTITY_ZOL) {
            return decodePair(entityType, 0x06, 0x7C09, 2, 0);
        }
        if (entityType == ENTITY_GEL) {
            return decodeSingle(entityType, 0x06, 0x7BFA, 2, 0);
        }
        if (entityType == ENTITY_HIDING_ZOL) {
            return decodeHidingZol(entityType);
        }
        if (entityType == ENTITY_STAR) {
            return decodePair(entityType, 0x07, 0x7247, 5, 0);
        }
        if (entityType == ENTITY_BLOOPER) {
            return decodePair(entityType, 0x07, 0x5BF1, 2, 0);
        }
        if (entityType == ENTITY_WINGED_OCTOROK) {
            return decodePair(entityType, 0x07, 0x562D, 8, 0);
        }
        if (entityType == ENTITY_PINCER) {
            return decodePair(entityType, 0x07, 0x542B, 10, 0);
        }
        if (entityType == ENTITY_STALFOS_AGGRESSIVE) {
            return decodePair(entityType, 0x06, 0x4AA8, 3, 0);
        }
        if (entityType == ENTITY_STALFOS_EVASIVE) {
            return decodePair(entityType, 0x15, 0x4E7D, 3, 0);
        }
        if (entityType == ENTITY_GIBDO) {
            return decodePair(entityType, 0x06, mapId == 0x07 ? 0x7E77 : 0x7E6F, 2, 0);
        }
        if (entityType == ENTITY_LIKE_LIKE) {
            return decodePair(entityType, 0x06, 0x7DD4, 2, 0);
        }
        if (entityType == ENTITY_PEAHAT) {
            return decodePair(entityType, 0x07, 0x6701, 2, 0);
        }
        if (entityType == ENTITY_ARMOS_STATUE) {
            return decodePair(entityType, 0x06, 0x7446, 2, 0);
        }
        if (entityType == ENTITY_ARMOS_KNIGHT) {
            return decodeRectangle(entityType, 0x06, 0x5523, 4, 8, 0);
        }
        if (entityType == ENTITY_HIDING_GHINI || entityType == ENTITY_GHINI) {
            return decodePair(entityType, 0x04, 0x5BFC, 2, 0);
        }
        if (entityType == ENTITY_GIANT_GHINI) {
            return decodeRectangle(entityType, 0x04, 0x5D26, 4, 8, 0);
        }
        if (entityType == ENTITY_HARDHAT_BEETLE) {
            return decodePair(entityType, 0x06, mapId == 0x0A ? 0x4F34 : 0x4F2C, 2, 0);
        }
        if (entityType == ENTITY_POLS_VOICE) {
            return decodePair(entityType, 0x06, 0x7373, 2, 0);
        }
        if (entityType == ENTITY_SPIKED_BEETLE) {
            return decodePair(entityType, 0x07, mapId == 0x03 ? 0x7794 : 0x7784, 4, 0);
        }
        if (entityType == ENTITY_LASER) {
            return decodePair(entityType, 0x04, 0x6C2D, 8, 0);
        }
        if (entityType == ENTITY_LASER_BEAM) {
            // LaserBeamEntityHandler never renders a normal entity sprite;
            // its visible pixels are transient VFX $06 from bank $02.
            return EntitySpriteDefinition.unsupported(entityType);
        }
        if (entityType == ENTITY_SPIKE_TRAP) {
            return decodePair(entityType, 0x06, 0x74FA, 1, 0);
        }
        if (entityType == ENTITY_PAIRODD) {
            return decodePair(entityType, 0x04, 0x5DD1, 8, 0);
        }
        if (entityType == ENTITY_PAIRODD_PROJECTILE) {
            return decodePair(entityType, 0x04, 0x5EF4, 2, 0);
        }
        if (entityType == ENTITY_WATER_TEKTITE) {
            return decodePair(entityType, 0x07, 0x752D, 2, 0);
        }
        if (entityType == ENTITY_FISH) {
            return decodeFish(entityType);
        }
        if (entityType == ENTITY_GRANDPA_ULRIRA) {
            return decodeRectangle(entityType, 0x06, 0x5C51, 2, 4, 0);
        }
        if (entityType == ENTITY_DROPPABLE_HEART) {
            return decodeSingle(entityType, 0x03, 0x5D36, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_RUPEE) {
            return decodeSingle(entityType, 0x03, 0x609C, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_FAIRY) {
            return decodePair(entityType, 0x03, 0x6157, 2, 0);
        }
        if (entityType == ENTITY_KEY_DROP_POINT) {
            return decodeSingle(entityType, 0x03, 0x5C78, 6, 0);
        }
        if (entityType == ENTITY_SWORD_SHIELD_PICKUP) {
            return decodeSingle(entityType, 0x03, 0x5B95, 1, 0);
        }
        if (entityType == ENTITY_IRON_MASKS_MASK) {
            return decodePair(entityType, 0x03, 0x5B80, 2, 0);
        }
        if (entityType == ENTITY_PIECE_OF_POWER) {
            return decodePair(entityType, 0x03, 0x5B65, 2, 0);
        }
        if (entityType == ENTITY_GUARDIAN_ACORN) {
            return decodeSingle(entityType, 0x03, 0x5B5B, 1, 0);
        }
        if (entityType == ENTITY_HEART_PIECE) {
            return decodePair(entityType, 0x03, 0x5A4D, 1, 0);
        }
        if (entityType == ENTITY_HEART_CONTAINER) {
            return decodePair(entityType, 0x03, 0x59D8, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_ARROWS) {
            return decodePair(entityType, 0x03, 0x6079, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_BOMBS) {
            return decodeSingle(entityType, 0x03, 0x5FC0, 1, 0);
        }
        if (entityType == ENTITY_SLEEPY_TOADSTOOL) {
            return decodePair(entityType, 0x03, 0x5D47, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_MAGIC_POWDER) {
            return decodeSingle(entityType, 0x03, 0x6055, 1, 0);
        }
        if (entityType == ENTITY_HIDING_SLIME_KEY) {
            return decodeSingle(entityType, 0x03, 0x5FFB, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return decodeSingle(entityType, 0x03, 0x5FD1, 1, 0);
        }
        if (entityType == ENTITY_MARIN_AT_THE_SHORE) {
            return decodePair(entityType, 0x18, 0x5EB7, 8, 0);
        }
        return EntitySpriteDefinition.unsupported(entityType);
    }

    /**
     * Returns the chest display list selected by the source's two GBC room
     * exceptions.  The alternate list is still ROM-backed and has one pair;
     * every other room uses the normal 33-item list.
     */
    public EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
        if ((mapId & ~0xFF) != 0 || (roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Chest map and room ids must be unsigned bytes");
        }
        if (itemVariant < 0 || itemVariant > 0x20) {
            throw new IllegalArgumentException("Chest item variant must be in range 0..0x20: "
                + itemVariant);
        }
        if ((mapId == 0x05 && roomId == 0xCE && itemVariant == 0x00)
            || (mapId == 0x06 && roomId == 0x1A && itemVariant == 0x01)) {
            return decodePair(ENTITY_CHEST_WITH_ITEM, 0x07, 0x7B53, 1, 0);
        }
        return decodePair(ENTITY_CHEST_WITH_ITEM, 0x07, 0x7B57, 0x21, 0);
    }

    /** Selects Pokey's bank-$15 body list for its current detached-segment count. */
    public EntitySpriteDefinition forPokeyState(int inertia) {
        if (inertia < 0) {
            throw new IllegalArgumentException("Pokey inertia cannot be negative: " + inertia);
        }
        if (inertia == 0) {
            return decodeRectangle(ENTITY_POKEY, 0x15, 0x4B17, 4, 6, 0);
        }
        if (inertia == 1) {
            return decodeRectangle(ENTITY_POKEY, 0x15, 0x4B77, 4, 4, 0);
        }
        return decodeRectangle(ENTITY_POKEY, 0x15, 0x4BB7, 4, 2, 0);
    }

    /** Selects Urchin's normal or credits bank-$15 display-list pair. */
    public EntitySpriteDefinition forUrchinState(boolean creditsGameplay) {
        return decodePair(ENTITY_URCHIN, 0x15,
            creditsGameplay ? 0x7393 : 0x7383, 4, 0);
    }

    /** Decodes Unknown044SpriteVariants at bank-$15:$4CD5 for detached segments. */
    public EntitySpriteDefinition forPokeySegment() {
        return decodePair(ENTITY_POKEY, 0x15, 0x4CD5, 1, 0);
    }

    /** Selects the two bank-$15 display-list pairs used by Stalfos Evasive. */
    public EntitySpriteDefinition forStalfosEvasiveState(int privateState1) {
        if (privateState1 < 0) {
            throw new IllegalArgumentException("Stalfos Evasive private state cannot be negative");
        }
        return privateState1 == 0
            ? decodePair(ENTITY_STALFOS_EVASIVE, 0x15, 0x4E7D, 3, 0)
            : decodePair(ENTITY_STALFOS_EVASIVE, 0x15, 0x4E8E, 2, 0);
    }

    /**
     * Selects Bush Crawler's bank-$07 display list for the lifted replacement
     * state. State $02 is the adjacent Bushcrawler2 list; state $01 is the
     * ordinary outdoor/indoor pair selected by the room table.
     */
    public EntitySpriteDefinition forBushCrawlerState(int privateState1,
                                                       EntityRoomLoader.RoomTable roomTable) {
        if (privateState1 < 0 || privateState1 > 0x02) {
            throw new IllegalArgumentException("Bush Crawler private state out of range: "
                + privateState1);
        }
        if (privateState1 == 0x02) {
            return decodePair(ENTITY_BUSH_CRAWLER, 0x07, 0x400A, 2, 0);
        }
        return forEntityType(ENTITY_BUSH_CRAWLER, roomTable);
    }

    /**
     * Builds the two-pass display list used while a normal Bush Crawler is
     * crawling. The handler first renders its room-specific pair, then the
     * four-tile Bushcrawler2 pair at the same active position.
     */
    public EntitySpriteDefinition forBushCrawlerCrawlState(
            EntityRoomLoader.RoomTable roomTable, int privateState4) {
        if (privateState4 < 0 || privateState4 > 1) {
            throw new IllegalArgumentException("Bush Crawler private state 4 must be 0 or 1: "
                + privateState4);
        }
        EntitySpriteDefinition normal = forEntityType(ENTITY_BUSH_CRAWLER, roomTable);
        EntitySpriteDefinition crawling = decodePair(ENTITY_BUSH_CRAWLER, 0x07,
            0x400A, 2, 0);
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(2);
        for (int animation = 0; animation < 2; animation++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(4);
            appendDynamicPair(sprites, normal.variant(privateState4),
                EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
            appendDynamicPair(sprites, crawling.variant(animation),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(ENTITY_BUSH_CRAWLER, 0x07,
            normal.address(), 0, variants);
    }

    /**
     * Selects the bank-$03 Iron Mask display list. Private state 2 is zero
     * while the mask is intact and nonzero after the mask has been removed.
     */
    public EntitySpriteDefinition forIronMaskState(int privateState2) {
        if ((privateState2 & ~0xFF) != 0) {
            throw new IllegalArgumentException(
                "Iron Mask private state must be an unsigned byte: " + privateState2);
        }
        return privateState2 == 0
            ? decodePair(ENTITY_IRON_MASK, 0x03, 0x4FCB, 8, 0)
            : decodePair(ENTITY_IRON_MASK, 0x03, 0x4FEB, 2, 0);
    }

    /**
     * Builds Mini Moldorm's handler-generated head and delayed-history tail.
     * The ROM renders all three two-sprite pairs in one pass, with the latter
     * two pairs sourced from history rather than from the active entity
     * position.
     */
    public EntitySpriteDefinition forMiniMoldormState(
            int headVariant, int entityX, int entityY,
            int segment1X, int segment1Y, int segment2X, int segment2Y) {
        if (headVariant < 0 || headVariant > 7) {
            throw new IllegalArgumentException("Mini Moldorm head variant must be 0..7: "
                + headVariant);
        }
        EntitySpriteDefinition pairs = decodePair(ENTITY_MINI_MOLDORM, 0x04,
            0x5A49, 10, 0);
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(6);
        appendDynamicPairAtOffset(sprites, pairs.variant(headVariant), 0, 0);
        appendDynamicPairAtOffset(sprites, pairs.variant(8),
            signedByte(segment1Y - entityY), signedByte(segment1X - entityX));
        appendDynamicPairAtOffset(sprites, pairs.variant(9),
            signedByte(segment2Y - entityY), signedByte(segment2X - entityX));
        return EntitySpriteDefinition.dynamic(ENTITY_MINI_MOLDORM, 0x04,
            0x5A49, 0, List.of(List.copyOf(sprites)));
    }

    /** Builds Moldorm's eight-sprite head and delayed four-pair tail. */
    public EntitySpriteDefinition forMoldormState(
            int headVariant, int entityX, int entityY,
            List<MoldormMotion.TailPosition> segments,
            int privateState3, int frameCounter) {
        return forMoldormPresentation(headVariant, entityX, entityY,
            entityX, entityY, segments, privateState3, frameCounter);
    }

    /** Builds pre-update Moldorm OAM relative to the post-update entity position. */
    public EntitySpriteDefinition forMoldormPresentation(
            int headVariant, int headX, int headY, int renderBaseX, int renderBaseY,
            List<MoldormMotion.TailPosition> segments,
            int privateState3, int frameCounter) {
        if (headVariant < 0 || headVariant > 7) {
            throw new IllegalArgumentException("Moldorm head variant must be 0..7: "
                + headVariant);
        }
        if (segments == null || segments.size() != 4) {
            throw new IllegalArgumentException("Moldorm requires four tail history positions");
        }
        if (privateState3 < 0 || privateState3 > 5) {
            throw new IllegalArgumentException("Moldorm private state 3 must be 0..5: "
                + privateState3);
        }

        if (privateState3 == 5) {
            return EntitySpriteDefinition.unsupported(ENTITY_MOLDORM);
        }

        EntitySpriteDefinition head = decodeRectangle(ENTITY_MOLDORM, 0x04,
            0x57F2, 8, 8, headVariant);
        EntitySpriteDefinition tail = decodePair(ENTITY_MOLDORM, 0x04,
            0x58F2, 4, 0);
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(16);
        int headYOffset = signedByte(headY - renderBaseY);
        int headXOffset = signedByte(headX - renderBaseX);
        for (EntitySpriteDefinition.RectangleSprite sprite :
                head.rectangleVariant(headVariant)) {
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(sprite.yOffset() + headYOffset),
                signedByte(sprite.xOffset() + headXOffset), sprite.oam(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        }

        int visibleTailPairs = 4 - privateState3;
        int[] tailVariants = {0, 0, 1, 2 + ((frameCounter >> 3) & 0x01)};
        for (int index = 0; index < visibleTailPairs; index++) {
            MoldormMotion.TailPosition position = segments.get(index);
            EntitySpriteDefinition.Variant pair = tail.variant(tailVariants[index]);
            int paletteXor = index == 3 && (frameCounter & 0x04) != 0 ? 0x10 : 0;
            appendMoldormTailPair(sprites, pair,
                signedByte(position.y() - renderBaseY),
                signedByte(position.x() - renderBaseX), paletteXor);
        }
        return EntitySpriteDefinition.dynamic(ENTITY_MOLDORM, 0x04,
            0x57F2, 0, List.of(List.copyOf(sprites)));
    }

    /** Builds the repeated vertical bar pairs emitted by {@code func_006_6FEA}. */
    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY) {
        return forRollingBonesBar(privateState1, entityY, 0);
    }

    /** Builds the bar using the current animated pair selected by the handler. */
    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY,
                                                      int spriteVariant) {
        if (privateState1 < 0 || privateState1 > 5) {
            throw new IllegalArgumentException(
                "Rolling Bones bar private state must be 0..5: " + privateState1);
        }
        EntitySpriteDefinition pair = decodePair(ENTITY_ROLLING_BONES_BAR,
            0x06, 0x6ED5, 2, 0);
        int selectedVariant = spriteVariant & 0x01;
        int pairCount = 6 - privateState1;
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(pairCount * 2);
        for (int index = 0; index < pairCount; index++) {
            int visualY = 0x20 + index * 0x10;
            appendDynamicPairAtOffset(sprites, pair.variant(selectedVariant),
                signedByte(visualY - entityY), 0);
        }
        return EntitySpriteDefinition.dynamic(ENTITY_ROLLING_BONES_BAR, 0x06,
            0x6ED5, 0, List.of(List.copyOf(sprites)));
    }

    /**
     * Selects the bank-$20 rectangle list used by bank-$36's
     * {@code func_036_69D9}. States below six while ACTIVE use the animated
     * eight-variant list; all other status/state combinations use the
     * four-variant inactive list.
     */
    public EntitySpriteDefinition forColorShellState(int entityType, int state,
                                                      EntityStatus status) {
        if (!isColorShellType(entityType)) {
            throw new IllegalArgumentException("Not a Color Shell entity type: 0x"
                + Integer.toHexString(entityType));
        }
        if (state < 0 || state > 0x0D) {
            throw new IllegalArgumentException("Color Shell state out of range: " + state);
        }
        if (status == null) {
            throw new IllegalArgumentException("Entity status cannot be null");
        }
        int color = entityType - ENTITY_COLOR_SHELL_RED;
        boolean activeList = status == EntityStatus.ACTIVE && state < 0x06;
        int address = (activeList ? COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES
                                  : COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES)[color];
        return decodeRectangle(entityType, 0x20, address, activeList ? 8 : 4, 3, 0);
    }

    /** The bank-$03 fire pair rendered over entities in {@code BURNING} status. */
    public EntitySpriteDefinition forBurningEntity() {
        return decodePair(0x00, 0x03, 0x4C44, 2, 0);
    }

    /** Decodes the bank-$03 pair shown immediately before bomb detonation. */
    public EntitySpriteDefinition forBombRightBeforeExploding() {
        return decodePair(ENTITY_BOMB, 0x03, 0x5484, 1, 0);
    }

    /** Decodes the bank-$03 rectangle list used for bomb explosion frames. */
    public EntitySpriteDefinition forBombExplosion() {
        return decodeRectangle(ENTITY_BOMB, 0x03, 0x6530, 4, 8, 0);
    }

    /** Decodes the bank-$03 {@code Data_003_5488} ordinary death display list. */
    public EntitySpriteDefinition forDeathEntity() {
        return decodeRectangle(0x00, 0x03, 0x5488, 4, 4, 0);
    }

    /**
     * Decodes the bank-$03 {@code Data_003_54C8} power-recoil death display list.
     * The source's five four-entry groups are exposed as four variants by
     * concatenating groups 3 and 4 into the final eight-entry frame.
     */
    public EntitySpriteDefinition forPowerRecoilDeathEntity() {
        return decodePowerRecoilDeathRectangle();
    }

    /** Decodes Data_006_7AEB, the two-frame rectangle list after the main item sprite. */
    public EntitySpriteDefinition forFloatingItemOverlay() {
        return decodeRectangle(ENTITY_FLOATING_ITEM, 0x06, 0x7AEB, 2, 2, 0);
    }

    /** The green Zol list selected after Slime Eye has split its Zol. */
    public EntitySpriteDefinition forZolSlimeEye() {
        return decodePair(0x1B, 0x06, 0x7C11, 2, 0);
    }

    /**
     * Decodes Hiding Zol's mixed display path. The bank-$07 handler selects
     * the single-sprite list when the entity variant is $01 and the pair list
     * for variants $00, $02, and $03. A null second OAM entry represents that
     * single-sprite branch without inventing a second tile.
     */
    private EntitySpriteDefinition decodeHidingZol(int entityType) {
        EntitySpriteDefinition pair = decodePair(entityType, 0x07, 0x729B, 4, 0);
        EntitySpriteDefinition single = decodeSingle(entityType, 0x07, 0x72AB, 1, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(pair.variants());
        variants.set(1, new EntitySpriteDefinition.Variant(single.variant(0).first(), null));
        return new EntitySpriteDefinition(entityType, 0x07, 0x729B,
            EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Decodes FishEntityHandler's five pair variants followed by its four
     * single-sprite jump variants. The handler selects the latter by
     * subtracting five from the active variant before rendering.
     */
    private EntitySpriteDefinition decodeFish(int entityType) {
        EntitySpriteDefinition pair = decodePair(entityType, 0x15, 0x449F, 5, 0);
        EntitySpriteDefinition single = decodeSingle(entityType, 0x15, 0x44B3, 4, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(pair.variants());
        for (int variant = 0; variant < 4; variant++) {
            variants.add(single.variant(variant));
        }
        return new EntitySpriteDefinition(entityType, 0x15, 0x449F,
            EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Decodes bank-$07's Moblin Sword handler presentation. The handler does
     * not point at one ordinary display list: it emits a shared-tile warning,
     * a two-entry bank-$20-generated list, and the inline sword pair in
     * variant-dependent order.
     */
    private EntitySpriteDefinition decodeMoblinSword(int entityType) {
        EntitySpriteDefinition body = decodePair(entityType, 0x07, 0x7A95, 8, 0);
        int[] dynamicY = {0x08, 0x0E, 0xF8, 0xF2, 0x00, 0x00, 0x00, 0x00};
        int[] dynamicX = {0x00, 0x00, 0xF9, 0xF9, 0xF8, 0xF2, 0x08, 0x0E};
        int[] tileSelector = {0x02, 0x02, 0x06, 0x06, 0x04, 0x04, 0x00, 0x00};
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(8);

        for (int variant = 0; variant < 8; variant++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(5);
            if (variant < 2) {
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    (variant ^ 0x01) + 0x04, -0x02,
                    new EntitySpriteDefinition.OamAttribute(0x86, 0x16),
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU, false));
            }
            if (variant == 2 || variant == 3) {
                appendMoblinSwordBody(sprites, body.variant(variant));
            }

            appendMoblinSwordGeneratedPair(sprites, dynamicY[variant], dynamicX[variant],
                tileSelector[variant]);

            if (variant != 2 && variant != 3) {
                appendMoblinSwordBody(sprites, body.variant(variant));
            }
            variants.add(List.copyOf(sprites));
        }

        return EntitySpriteDefinition.dynamic(entityType, 0x07, 0x7A95, 0, variants);
    }

    private void appendMoblinSwordGeneratedPair(
        List<EntitySpriteDefinition.DynamicSprite> sprites, int yOffset, int xOffset,
        int tileSelector) {
        int tileAddress = 0x4A93 + tileSelector * 2;
        int attributeAddress = 0x4AA3 + tileSelector * 2;
        int firstTile = readByte(0x20, tileAddress);
        if (firstTile == 0xFF) {
            // func_020_4AB3 turns the $FF tile sentinel into the ROM's $F0
            // tile before committing the OAM entry.
            firstTile = 0xF0;
        }
        int secondTile = readByte(0x20, tileAddress + 1);
        int firstAttributes = readByte(0x20, attributeAddress);
        int secondAttributes = readByte(0x20, attributeAddress + 1);
        EntitySpriteDefinition.DynamicSprite.TileSource source =
            EntitySpriteDefinition.DynamicSprite.TileSource.GPU;
        sprites.add(new EntitySpriteDefinition.DynamicSprite(signedByte(yOffset),
            signedByte(xOffset), new EntitySpriteDefinition.OamAttribute(
                firstTile, firstAttributes), source, false));
        sprites.add(new EntitySpriteDefinition.DynamicSprite(signedByte(yOffset),
            signedByte(xOffset + 0x08), new EntitySpriteDefinition.OamAttribute(
                secondTile, secondAttributes), source, false));
    }

    private static void appendMoblinSwordBody(
        List<EntitySpriteDefinition.DynamicSprite> sprites,
        EntitySpriteDefinition.Variant body) {
        sprites.add(new EntitySpriteDefinition.DynamicSprite(0, 0, body.first(),
            EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        if (body.second() != null) {
            sprites.add(new EntitySpriteDefinition.DynamicSprite(0, 0x08, body.second(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        }
    }

    /**
     * Decodes Data_006_7ADD as a mixed pair definition. The bank-$06 handler
     * renders variants $00-$06 as single sprites, except that variant $05 of
     * ENTITY_FLOATING_ITEM_2 deliberately points at Data_006_7AD1 + 2. That
     * source address is inside the preceding Boo Buddy code; preserve its ROM
     * bytes rather than correcting the disassembly's source quirk.
     */
    private EntitySpriteDefinition decodeFloatingItemMain(int entityType) {
        EntitySpriteDefinition singles = decodeSingle(entityType, 0x06, 0x7ADD, 7, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(7);
        for (EntitySpriteDefinition.Variant variant : singles.variants()) {
            variants.add(variant);
        }
        if (entityType == ENTITY_FLOATING_ITEM_2) {
            EntitySpriteDefinition quirk = decodePair(entityType, 0x06, 0x7AD3, 1, 0);
            variants.set(5, quirk.variant(0));
        }
        return new EntitySpriteDefinition(entityType, 0x06, 0x7ADD,
            EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Returns the display list selected by CreateFollowingNpcEntity handlers.
     * The caller supplies the matching follower entity type in the same form
     * used by the disassembly's dynamic entity spawner.
     */
    public EntitySpriteDefinition forFollowerEntityType(int entityType) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        return switch (entityType) {
            case ENTITY_BOW_WOW -> decodePair(entityType, 0x05, 0x401C, 7, 0);
            case ENTITY_MARIN_AT_THE_SHORE -> decodePair(entityType, 0x18, 0x59B8, 11, 0);
            case 0xD4 -> decodePair(entityType, 0x19, 0x5DF8, 6, 0);
            case 0xD5 -> decodePair(entityType, 0x19, 0x59BC, 8, 0);
            default -> EntitySpriteDefinition.unsupported(entityType);
        };
    }

    /**
     * Builds the mixed display list emitted by BombArrowHandler. The handler
     * draws the bomb's single sprite first, then the ordinary arrow pair, all
     * at the current arrow position.
     */
    public EntitySpriteDefinition forBombArrow() {
        EntitySpriteDefinition arrow = decodePair(ENTITY_ARROW, 0x03, 0x6BC6, 4, 0);
        EntitySpriteDefinition bomb = decodeSingle(ENTITY_BOMB, 0x03, 0x652E, 1, 0);
        int[] bombXOffsets = {0x04, -0x04, 0x00, 0x00};
        int[] bombYOffsets = {-0x02, -0x02, -0x06, 0x04};
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(4);
        for (int direction = 0; direction < 4; direction++) {
            EntitySpriteDefinition.Variant arrowVariant = arrow.variant(direction);
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(3);
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                bombYOffsets[direction], bombXOffsets[direction] + 0x04,
                bomb.variant(0).first(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, false));
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                0, 0, arrowVariant.first(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, false));
            if (arrowVariant.second() != null) {
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    0, 0x08, arrowVariant.second(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, false));
            }
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(ENTITY_ARROW, 0x03, 0x6BC6, 0, variants);
    }

    /** Shared bank-$03 fire display used after a Magic Rod fireball hits a wall. */
    public EntitySpriteDefinition forMagicRodFireState() {
        return decodePair(ENTITY_MAGIC_ROD_FIREBALL, 0x03, 0x4C44, 2, 0);
    }

    /**
     * Returns the two-entry OAM pair emitted by MagicPowderSprinkle's torch
     * transition. The source selects Data_018_795E while lighting and
     * Data_018_7962 while restoring the unlit torch.
     */
    public EntitySpriteDefinition forMagicPowderTorchState(boolean lighting) {
        return decodePair(ENTITY_MAGIC_POWDER_SPRINKLE, 0x18,
            lighting ? 0x795E : 0x7962, 1, 0);
    }

    private EntitySpriteDefinition decodeLiftableRock(EntityRoomLoader.RoomTable roomTable) {
        int intactAddress = roomTable == EntityRoomLoader.RoomTable.OVERWORLD
            ? 0x5398 : 0x53A0;
        EntitySpriteDefinition intact = decodePair(
            ENTITY_LIFTABLE_ROCK, 0x03, intactAddress, 2,
            LIFTABLE_ROCK_INTACT_ROCK_VARIANT);

        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(22);
        variants.add(dynamicPair(intact.variant(LIFTABLE_ROCK_INTACT_ROCK_VARIANT)));
        variants.add(dynamicPair(intact.variant(LIFTABLE_ROCK_INTACT_BUSH_VARIANT)));
        appendDynamicRectangleVariants(variants, 0x19, 0x7B10, 4, 4);
        appendDynamicRectangleVariants(variants, 0x19, 0x7B50, 8, 4);
        appendDynamicRectangleVariants(variants, 0x19, 0x7BD0, 8, 4);
        return EntitySpriteDefinition.dynamic(ENTITY_LIFTABLE_ROCK, 0x19, 0x7B50,
            LIFTABLE_ROCK_INTACT_ROCK_VARIANT, variants);
    }

    private List<EntitySpriteDefinition.DynamicSprite> dynamicPair(
            EntitySpriteDefinition.Variant pair) {
        if (pair.second() == null) {
            throw new IllegalArgumentException("Liftable-rock pair must contain two OAM entries");
        }
        return List.of(
            new EntitySpriteDefinition.DynamicSprite(0, 0, pair.first(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true),
            new EntitySpriteDefinition.DynamicSprite(0, 0x08, pair.second(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
    }

    private void appendDynamicRectangleVariants(
            List<List<EntitySpriteDefinition.DynamicSprite>> destination,
            int bank, int address, int variantCount, int spriteCount) {
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangles =
            decodeRectangleVariants(ENTITY_LIFTABLE_ROCK, bank, address, variantCount,
                spriteCount, 0);
        for (List<EntitySpriteDefinition.RectangleSprite> rectangle : rectangles) {
            List<EntitySpriteDefinition.DynamicSprite> dynamic = new ArrayList<>(rectangle.size());
            for (EntitySpriteDefinition.RectangleSprite sprite : rectangle) {
                dynamic.add(new EntitySpriteDefinition.DynamicSprite(
                    sprite.yOffset(), sprite.xOffset(), sprite.oam(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, false));
            }
            destination.add(List.copyOf(dynamic));
        }
    }

    public EntitySpriteDefinition decodePair(int entityType, int bank, int address,
                                              int variantCount, int initialVariant) {
        return decodePair(entityType, bank, address, variantCount, initialVariant,
            EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }

    private EntitySpriteDefinition decodePair(int entityType, int bank, int address,
                                              int variantCount, int initialVariant,
                                              EntitySpriteDefinition.DynamicSprite.TileSource
                                                  tileSource) {
        int offset = validateDisplayList(entityType, bank, address, variantCount, 4,
            initialVariant);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            int index = offset + variant * 4;
            variants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(
                    Byte.toUnsignedInt(romData[index]), Byte.toUnsignedInt(romData[index + 1])),
                new EntitySpriteDefinition.OamAttribute(
                    Byte.toUnsignedInt(romData[index + 2]), Byte.toUnsignedInt(romData[index + 3]))));
        }
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.PAIR, initialVariant, variants, tileSource);
    }

    public EntitySpriteDefinition decodeSingle(int entityType, int bank, int address,
                                                int variantCount, int initialVariant) {
        int offset = validateDisplayList(entityType, bank, address, variantCount, 2,
            initialVariant);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            int index = offset + variant * 2;
            variants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(
                    Byte.toUnsignedInt(romData[index]), Byte.toUnsignedInt(romData[index + 1])),
                null));
        }
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.SINGLE, initialVariant, variants);
    }

    public EntitySpriteDefinition decodeRectangle(int entityType, int bank, int address,
                                                   int variantCount, int spriteCount,
                                                   int initialVariant) {
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangleVariants =
            decodeRectangleVariants(entityType, bank, address, variantCount, spriteCount,
                initialVariant);
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.RECTANGLE, initialVariant, List.of(), rectangleVariants);
    }

    private EntitySpriteDefinition decodePowerRecoilDeathRectangle() {
        int entityType = 0x00;
        int bank = 0x03;
        int address = 0x54C8;
        List<List<EntitySpriteDefinition.RectangleSprite>> sourceVariants =
            decodeRectangleVariants(entityType, bank, address, 5, 4, 0);
        List<List<EntitySpriteDefinition.RectangleSprite>> variants =
            new ArrayList<>(sourceVariants.subList(0, 3));
        List<EntitySpriteDefinition.RectangleSprite> finalFrame =
            new ArrayList<>(sourceVariants.get(3));
        finalFrame.addAll(sourceVariants.get(4));
        variants.add(List.copyOf(finalFrame));
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), variants);
    }

    private List<List<EntitySpriteDefinition.RectangleSprite>> decodeRectangleVariants(
            int entityType, int bank, int address, int variantCount, int spriteCount,
            int initialVariant) {
        if (spriteCount <= 0) {
            throw new IllegalArgumentException("Rectangle display lists need sprites");
        }
        long strideLong = (long) spriteCount * 4;
        if (strideLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rectangle display list is too large");
        }
        int stride = (int) strideLong;
        int offset = validateDisplayList(entityType, bank, address, variantCount, stride,
            initialVariant);
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangleVariants =
            new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            List<EntitySpriteDefinition.RectangleSprite> sprites = new ArrayList<>(spriteCount);
            int variantOffset = offset + variant * stride;
            for (int sprite = 0; sprite < spriteCount; sprite++) {
                int index = variantOffset + sprite * 4;
                int yOffset = signedByte(romData[index]);
                int xOffset = signedByte(romData[index + 1]);
                sprites.add(new EntitySpriteDefinition.RectangleSprite(
                    yOffset, xOffset,
                    new EntitySpriteDefinition.OamAttribute(
                        Byte.toUnsignedInt(romData[index + 2]),
                        Byte.toUnsignedInt(romData[index + 3]))));
            }
            rectangleVariants.add(List.copyOf(sprites));
        }
        return List.copyOf(rectangleVariants);
    }

    private int validateDisplayList(int entityType, int bank, int address,
                                    int variantCount, int stride, int initialVariant) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        if (bank < 0 || bank > 0xFF || address < 0x4000 || address > 0x7FFF) {
            throw new IllegalArgumentException("Display list address is outside a switchable ROM bank");
        }
        if (variantCount <= 0 || initialVariant < 0 || initialVariant >= variantCount) {
            throw new IllegalArgumentException("Invalid display list variant range");
        }
        long byteCount = (long) variantCount * stride;
        if (address + byteCount > 0x8000L) {
            throw new IllegalArgumentException("Display list crosses the selected ROM bank boundary");
        }
        int offset = RomBank.romOffset(bank, address);
        if (offset < 0 || byteCount > romData.length - offset) {
            throw new IllegalArgumentException("Display list exceeds ROM bounds at 0x"
                + Integer.toHexString(Math.max(offset, 0)));
        }
        return offset;
    }

    private int readByte(int bank, int address) {
        int offset = RomBank.romOffset(bank, address);
        if (offset < 0 || offset >= romData.length) {
            throw new IllegalArgumentException("ROM byte is outside the ROM at 0x"
                + Integer.toHexString(offset));
        }
        return Byte.toUnsignedInt(romData[offset]);
    }

    private static int signedByte(byte value) {
        return value;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private static boolean isIndoorRoomTable(EntityRoomLoader.RoomTable roomTable) {
        return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B;
    }

    private static void appendDynamicPair(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            EntitySpriteDefinition.DynamicSprite.TileSource tileSource) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
            0, 0, pair.first(),
            tileSource, true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                0, 0x08, pair.second(),
                tileSource, true));
        }
    }

    private static void appendDynamicPairAtOffset(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair, int yOffset, int xOffset) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
            signedByte(yOffset), signedByte(xOffset), pair.first(),
            EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset), signedByte(xOffset + 0x08), pair.second(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        }
    }

    private static void appendMoldormTailPair(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair, int yOffset, int xOffset,
            int paletteXor) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
            signedByte(yOffset), signedByte(xOffset), xorAttributes(pair.first(), paletteXor),
            EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset), signedByte(xOffset + 0x08),
                xorAttributes(pair.second(), paletteXor),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        }
    }

    private static EntitySpriteDefinition.OamAttribute xorAttributes(
            EntitySpriteDefinition.OamAttribute oam, int attributes) {
        return new EntitySpriteDefinition.OamAttribute(
            oam.tile(), oam.attributes() ^ attributes);
    }

    private static boolean isColorShellType(int entityType) {
        return entityType >= ENTITY_COLOR_SHELL_RED && entityType <= ENTITY_COLOR_SHELL_BLUE;
    }
}
