package linksawakening.entity;

import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Composes immutable registration lookup, validated ROM decoding and specialized sprite composition. */
public final class EntitySpriteHandlerCatalog {

    static final int ENTITY_ARROW = 0x00;
    static final int ENTITY_BOOMERANG = 0x01;
    static final int ENTITY_MAGIC_ROD_FIREBALL = 0x04;
    static final int ENTITY_MAGIC_POWDER_SPRINKLE = 0x08;
    static final int ENTITY_MUSICAL_NOTE = 0xC9;
    static final int ENTITY_SWORD_BEAM = 0xDF;
    static final int ENTITY_BOMB = 0x02;
    static final int ENTITY_BUTTERFLY = 0x6E;
    static final int ENTITY_HOOKSHOT_CHAIN = 0x03;
    static final int ENTITY_CRYSTAL_SWITCH = 0x66;
    static final int ENTITY_OCTOROK = 0x09;
    static final int ENTITY_OCTOROK_ROCK = 0x0A;
    static final int ENTITY_MOBLIN = 0x0B;
    static final int ENTITY_IRON_MASK = 0x24;
    static final int ENTITY_MOBLIN_ARROW = 0x0C;
    static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    static final int ENTITY_TIMER_BOMBITE = 0x56;
    static final int ENTITY_MAD_BOMBER = 0x93;
    static final int ENTITY_BOMBER = 0xBA;
    static final int ENTITY_MOBLIN_SWORD = 0x14;
    static final int ENTITY_TEKTITE = 0x0D;
    static final int ENTITY_LEEVER = 0x0E;
    static final int ENTITY_ANTI_FAIRY = 0x15;
    static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    static final int ENTITY_ZOL = 0x1B;
    static final int ENTITY_GEL = 0x1C;
    static final int ENTITY_HIDING_ZOL = 0x9B;
    static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    static final int ENTITY_STALFOS_EVASIVE = 0x1E;
    static final int ENTITY_GIBDO = 0x1F;
    static final int ENTITY_LIKE_LIKE = 0x23;
    static final int ENTITY_PEAHAT = 0xA0;
    static final int ENTITY_WIZROBE = 0x21;
    static final int ENTITY_WIZROBE_PROJECTILE = 0x22;
    static final int ENTITY_ARMOS_STATUE = 0x0F;
    static final int ENTITY_ARMOS_KNIGHT = 0x88;
    static final int ENTITY_HIDING_GHINI = 0x10;
    static final int ENTITY_GIANT_GHINI = 0x11;
    static final int ENTITY_GHINI = 0x12;
    static final int ENTITY_POLS_VOICE = 0x18;
    static final int ENTITY_KEESE = 0x19;
    static final int ENTITY_GOOMBA = 0x9F;
    static final int ENTITY_SNAKE = 0xA1;
    static final int ENTITY_HARDHAT_BEETLE = 0x20;
    static final int ENTITY_SPIKED_BEETLE = 0x2C;
    static final int ENTITY_LASER = 0x2A;
    static final int ENTITY_LASER_BEAM = 0x2B;
    static final int ENTITY_SPIKE_TRAP = 0x27;
    static final int ENTITY_PAIRODD = 0x57;
    static final int ENTITY_PAIRODD_PROJECTILE = 0x58;
    static final int ENTITY_WATER_TEKTITE = 0x99;
    static final int ENTITY_FISH = 0xCC;
    static final int ENTITY_BOW_WOW = 0x6D;
    static final int ENTITY_DOG = 0x6F;
    static final int ENTITY_KID_70 = 0x70;
    static final int ENTITY_KID_71 = 0x71;
    static final int ENTITY_KID_72 = 0x72;
    static final int ENTITY_KID_73 = 0x73;
    static final int ENTITY_MADAM_MEOWMEOW = 0x79;
    static final int ENTITY_CROW = 0x7A;
    static final int ENTITY_BOO_BUDDY = 0x50;
    static final int ENTITY_DROPPABLE_HEART = 0x2D;
    static final int ENTITY_DROPPABLE_RUPEE = 0x2E;
    static final int ENTITY_DROPPABLE_FAIRY = 0x2F;
    static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;
    static final int ENTITY_IRON_MASKS_MASK = 0x32;
    static final int ENTITY_PIECE_OF_POWER = 0x33;
    static final int ENTITY_GUARDIAN_ACORN = 0x34;
    static final int ENTITY_HEART_PIECE = 0x35;
    static final int ENTITY_HEART_CONTAINER = 0x36;
    static final int ENTITY_DROPPABLE_ARROWS = 0x37;
    static final int ENTITY_DROPPABLE_BOMBS = 0x38;
    static final int ENTITY_SLEEPY_TOADSTOOL = 0x3A;
    static final int ENTITY_DROPPABLE_MAGIC_POWDER = 0x3B;
    static final int ENTITY_HIDING_SLIME_KEY = 0x3C;
    static final int ENTITY_DROPPABLE_SECRET_SEASHELL = 0x3D;
    static final int ENTITY_FLOATING_ITEM = 0x86;
    static final int ENTITY_FLOATING_ITEM_2 = 0xE5;
    static final int ENTITY_MARIN = 0x3E;
    static final int ENTITY_TARIN = 0x3F;
    static final int ENTITY_GRANDPA_ULRIRA = 0x77;
    static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    static final int ENTITY_MARIN_AT_TAL_TAL_HEIGHTS = 0xC2;
    static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;

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

    /** Selects the runtime-dependent display list used by entity $31. */
    public EntitySpriteDefinition forSwordShieldPickup(boolean beachSword) {
        return states.forSwordShieldPickup(beachSword);
    }

    /** Selects SirensInstrumentState1Handler's two projectile variants. */
    public EntitySpriteDefinition forKidPlaying() {
        return states.forKidPlaying();
    }

    public EntitySpriteDefinition forKidBall() {
        return states.forKidBall();
    }

    public EntitySpriteDefinition forInstrumentShard() {
        return states.forInstrumentShard();
    }

    /** Selects the perched pair or four-sprite flying layout used by the owl event. */
    public EntitySpriteDefinition forOwlEvent(boolean flying) {
        return states.forOwlEvent(flying);
    }

    /**
     * Returns the chest display list selected by the source's two GBC room
     * exceptions. The alternate list contains two single-sprite entries;
     * every other room uses the normal 33-item list.
     */
    public EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
        return states.forChestState(mapId, roomId, itemVariant);
    }

    /** Selects Pokey's bank-$15 body list for its current detached-segment count. */
    public EntitySpriteDefinition forPokeyState(int inertia) {
        return states.forPokeyState(inertia);
    }

    /** Selects Urchin's normal or credits bank-$15 display-list pair. */
    public EntitySpriteDefinition forUrchinState(boolean creditsGameplay) {
        return states.forUrchinState(creditsGameplay);
    }

    /** Decodes Unknown044SpriteVariants at bank-$15:$4CD5 for detached segments. */
    public EntitySpriteDefinition forPokeySegment() {
        return states.forPokeySegment();
    }

    /** Selects the two bank-$15 display-list pairs used by Stalfos Evasive. */
    public EntitySpriteDefinition forStalfosEvasiveState(int privateState1) {
        return states.forStalfosEvasiveState(privateState1);
    }

    /**
     * Selects Bush Crawler's bank-$07 display list for the lifted replacement
     * state. State $02 is the adjacent Bushcrawler2 list; state $01 is the
     * ordinary outdoor/indoor pair selected by the room table.
     */
    public EntitySpriteDefinition forBushCrawlerState(int privateState1, EntityRoomLoader.RoomTable roomTable) {
        return states.forBushCrawlerState(privateState1, roomTable);
    }

    /**
     * Builds the two-pass display list used while a normal Bush Crawler is
     * crawling. The handler first renders its room-specific pair, then the
     * four-tile Bushcrawler2 pair at the same active position.
     */
    public EntitySpriteDefinition forBushCrawlerCrawlState(EntityRoomLoader.RoomTable roomTable, int privateState4) {
        return states.forBushCrawlerCrawlState(roomTable, privateState4);
    }

    /**
     * Selects the bank-$03 Iron Mask display list. Private state 2 is zero
     * while the mask is intact and nonzero after the mask has been removed.
     */
    public EntitySpriteDefinition forIronMaskState(int privateState2) {
        return states.forIronMaskState(privateState2);
    }

    /**
     * Builds Mini Moldorm's handler-generated head and delayed-history tail.
     * The ROM renders all three two-sprite pairs in one pass, with the latter
     * two pairs sourced from history rather than from the active entity
     * position.
     */
    public EntitySpriteDefinition forMiniMoldormState(
            int headVariant, int entityX, int entityY, int segment1X, int segment1Y, int segment2X, int segment2Y) {
        return bosses.forMiniMoldormState(headVariant, entityX, entityY, segment1X, segment1Y, segment2X, segment2Y);
    }

    /** Builds Moldorm's eight-sprite head and delayed four-pair tail. */
    public EntitySpriteDefinition forMoldormState(
            int headVariant,
            int entityX,
            int entityY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter) {
        return bosses.forMoldormState(headVariant, entityX, entityY, segments, privateState3, frameCounter);
    }

    /** Builds pre-update Moldorm OAM relative to the post-update entity position. */
    public EntitySpriteDefinition forMoldormPresentation(
            int headVariant,
            int headX,
            int headY,
            int renderBaseX,
            int renderBaseY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter) {
        return bosses.forMoldormPresentation(
                headVariant, headX, headY, renderBaseX, renderBaseY, segments, privateState3, frameCounter);
    }

    /** Slime Eye bank-04 display lists followed by the ground shadow. */
    public EntitySpriteDefinition forSlimeEyeState(boolean half, int variant, int z) {
        return bosses.forSlimeEyeState(half, variant, z);
    }

    /** Builds the bar using the current animated pair selected by the handler. */
    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY, int spriteVariant) {
        return bosses.forRollingBonesBar(privateState1, entityY, spriteVariant);
    }

    /** EntityFallHandler's three single frames followed by Unknown020's pair. */
    public EntitySpriteDefinition forFallingEntity() {
        return effects.forFallingEntity();
    }

    /** Decodes the bank-$03 pair shown immediately before bomb detonation. */
    public EntitySpriteDefinition forBombRightBeforeExploding() {
        return effects.forBombRightBeforeExploding();
    }

    /** Decodes the bank-$03 rectangle list used for bomb explosion frames. */
    public EntitySpriteDefinition forBombExplosion() {
        return effects.forBombExplosion();
    }

    /** Decodes Data_006_7AEB, the two-frame rectangle list after the main item sprite. */
    public EntitySpriteDefinition forFloatingItemOverlay() {
        return effects.forFloatingItemOverlay();
    }

    /** The green Zol list selected after Slime Eye has split its Zol. */
    public EntitySpriteDefinition forZolSlimeEye() {
        return effects.forZolSlimeEye();
    }

    /**
     * Returns the display list selected by CreateFollowingNpcEntity handlers.
     * The caller supplies the matching follower entity type in the same form
     * used by the disassembly's dynamic entity spawner.
     */
    public EntitySpriteDefinition forFollowerEntityType(int entityType) {
        return states.forFollowerEntityType(entityType);
    }

    /** Builds func_015_4AC9's body, optional weapon, and airborne shadow layers. */
    public EntitySpriteDefinition forMoblinKingState(int bodyVariant, int weaponVariant, boolean airborne) {
        return bosses.forMoblinKingState(bodyVariant, weaponVariant, airborne);
    }

    /**
     * Builds the mixed display list emitted by BombArrowHandler. The handler
     * draws the bomb's single sprite first, then the ordinary arrow pair, all
     * at the current arrow position.
     */
    public EntitySpriteDefinition forBombArrow() {
        return effects.forBombArrow();
    }

    /** Shared bank-$03 fire display used after a Magic Rod fireball hits a wall. */
    public EntitySpriteDefinition forMagicRodFireState() {
        return effects.forMagicRodFireState();
    }

    /**
     * Returns the two-entry OAM pair emitted by MagicPowderSprinkle's torch
     * transition. The source selects Data_018_795E while lighting and
     * Data_018_7962 while restoring the unlit torch.
     */
    public EntitySpriteDefinition forMagicPowderTorchState(boolean lighting) {
        return effects.forMagicPowderTorchState(lighting);
    }

    public EntitySpriteDefinition decodePair(
            int entityType, int bank, int address, int variantCount, int initialVariant) {
        return decoder.decodePair(entityType, bank, address, variantCount, initialVariant);
    }

    public EntitySpriteDefinition decodeSingle(
            int entityType, int bank, int address, int variantCount, int initialVariant) {
        return decoder.decodeSingle(entityType, bank, address, variantCount, initialVariant);
    }

    public EntitySpriteDefinition decodeRectangle(
            int entityType, int bank, int address, int variantCount, int spriteCount, int initialVariant) {
        return decoder.decodeRectangle(entityType, bank, address, variantCount, spriteCount, initialVariant);
    }

    private final EntitySpriteDecoder decoder;
    private final EntitySpriteRegistrationData registrations = EntitySpriteRegistrationData.instance();
    private final BossSpriteComposer bosses;
    private final EntitySpriteStates states;
    private final EntityEffectSprites effects;

    public EntitySpriteHandlerCatalog(Rom romData) {
        decoder = new EntitySpriteDecoder(romData);
        bosses = new BossSpriteComposer(decoder);
        states = new EntitySpriteStates(decoder, registrations);
        effects = new EntityEffectSprites(decoder);
    }
    /** @deprecated Use the Rom overload; retains legacy partial-buffer validation. */
    @Deprecated(forRemoval = true)
    public EntitySpriteHandlerCatalog(byte[] romData) {
        this(romData == null ? null : Rom.unchecked(romData));
    }

    public EntitySpriteDefinition forEntityType(int entityType, EntityRoomLoader.RoomTable roomTable) {
        return forEntityType(entityType, roomTable, -1);
    }

    public EntitySpriteDefinition forEntityType(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        EntitySpriteRecipe recipe = registrations.lookup(entityType, roomTable, mapId);
        return switch (recipe.kind()) {
            case PAIR, SINGLE, RECTANGLE -> decoder.decode(recipe);
            case HOOKSHOT_CHAIN -> effects.hookshotChain(entityType);
            case OWL_STATUE -> states.owlStatue(recipe);
            case UNSUPPORTED -> EntitySpriteDefinition.unsupported(entityType);
            case FLOATING_ITEM -> effects.decodeFloatingItemMain(entityType);
            case COLOR_SHELL -> states.forColorShellState(entityType, 0, EntityStatus.INIT);
            case ROCK -> effects.decodeLiftableRock(roomTable);
            case OWL -> states.forOwlEvent(false);
            case URCHIN -> states.forUrchinState(false);
            case FISH -> states.decodeFish(entityType);
            case HIDING_ZOL -> states.decodeHidingZol(entityType);
            case MOBLIN_SWORD -> states.decodeMoblinSword(entityType);
            case IRON_MASK -> states.forIronMaskState(0);
            case POKEY -> states.forPokeyState(0);
            case DUNGEON_WARP -> states.decodeDungeonWarp();
            case SLIME_EYE -> bosses.forSlimeEyeState(false, 0, 0);
            case MOLDORM -> bosses.forMoldormState(
                    0,
                    0,
                    0,
                    List.of(
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0)),
                    0,
                    0);
            case ROLLING_BAR -> bosses.forRollingBonesBar(0, 0x38);
        };
    }

    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY) {
        return bosses.forRollingBonesBar(privateState1, entityY);
    }

    public EntitySpriteDefinition forColorShellState(int entityType, int state, EntityStatus status) {
        return states.forColorShellState(entityType, state, status);
    }

    public EntitySpriteDefinition forBurningEntity() {
        return effects.forBurningEntity();
    }

    public EntitySpriteDefinition forDeathEntity() {
        return effects.forDeathEntity();
    }

    public EntitySpriteDefinition forPowerRecoilDeathEntity() {
        return effects.forPowerRecoilDeathEntity();
    }
}
