package linksawakening.entity;

import linksawakening.rom.Rom;

/**
 * ROM-backed display-list decoder plus the small handler-to-list mapping
 * required by the first entity rendering increment.
 */
public final class EntitySpriteHandlerCatalog extends EntitySpriteDecodeGeometry {

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
    public static final int ENTITY_THREE_OF_A_KIND = 0x90;
    public static final int ENTITY_MASKED_MIMIC_GORIYA = 0x8F;
    public static final int ENTITY_HINOX = 0x89;
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
    public static final int ENTITY_INSTRUMENT_OF_THE_SIRENS = 0x39;

    public EntitySpriteHandlerCatalog(Rom romData) {
        super(romData);
    }

    /** @deprecated Use the Rom overload; retains legacy partial-buffer validation. */
    @Deprecated(forRemoval = true)
    public EntitySpriteHandlerCatalog(byte[] romData) {
        this(romData == null ? null : Rom.unchecked(romData));
    }
}
