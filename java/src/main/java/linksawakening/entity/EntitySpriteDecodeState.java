package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Entity display-list decoding and presentation used by ROM {@code RenderActiveEntitySpritesPair}. */
abstract class EntitySpriteDecodeState {

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

    protected static final int[] COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES = {0x6688, 0x66E8, 0x6748};
    protected static final int[] COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES = {0x67A8, 0x67D8, 0x6808};

    protected final Rom romData;

    public EntitySpriteDecodeState(Rom romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
    }

    static void appendMoblinSwordBody(
            List<EntitySpriteDefinition.DynamicSprite> sprites, EntitySpriteDefinition.Variant body) {
        sprites.add(new EntitySpriteDefinition.DynamicSprite(
                0, 0, body.first(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        if (body.second() != null) {
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    0, 0x08, body.second(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        }
    }

    static void appendRectangleSprites(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            List<EntitySpriteDefinition.RectangleSprite> source) {
        for (EntitySpriteDefinition.RectangleSprite sprite : source) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                    sprite.yOffset(),
                    sprite.xOffset(),
                    sprite.oam(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    false));
        }
    }

    static int signedByte(byte value) {
        return value;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static boolean isIndoorRoomTable(EntityRoomLoader.RoomTable roomTable) {
        return roomTable == EntityRoomLoader.RoomTable.INDOORS_A || roomTable == EntityRoomLoader.RoomTable.INDOORS_B;
    }

    static void appendDynamicPair(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            EntitySpriteDefinition.DynamicSprite.TileSource tileSource) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(0, 0, pair.first(), tileSource, true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(0, 0x08, pair.second(), tileSource, true));
        }
    }

    static void appendDynamicPairAtOffset(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            int yOffset,
            int xOffset) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset),
                signedByte(xOffset),
                pair.first(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                    signedByte(yOffset),
                    signedByte(xOffset + 0x08),
                    pair.second(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true));
        }
    }

    static void appendMoldormTailPair(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            int yOffset,
            int xOffset,
            int paletteXor) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset),
                signedByte(xOffset),
                xorAttributes(pair.first(), paletteXor),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                    signedByte(yOffset),
                    signedByte(xOffset + 0x08),
                    xorAttributes(pair.second(), paletteXor),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true));
        }
    }

    static EntitySpriteDefinition.OamAttribute xorAttributes(EntitySpriteDefinition.OamAttribute oam, int attributes) {
        return new EntitySpriteDefinition.OamAttribute(oam.tile(), oam.attributes() ^ attributes);
    }

    static boolean isColorShellType(int entityType) {
        return entityType >= ENTITY_COLOR_SHELL_RED && entityType <= ENTITY_COLOR_SHELL_BLUE;
    }

    public abstract EntitySpriteDefinition forEntityType(int entityType, EntityRoomLoader.RoomTable roomTable);

    public abstract EntitySpriteDefinition forSwordShieldPickup(boolean beachSword);

    public abstract EntitySpriteDefinition forKidPlaying();

    public abstract EntitySpriteDefinition forKidBall();

    public abstract EntitySpriteDefinition forInstrumentShard();

    public abstract EntitySpriteDefinition forOwlEvent(boolean flying);

    public abstract EntitySpriteDefinition forEntityType(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId);

    public abstract EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant);

    public abstract EntitySpriteDefinition forPokeyState(int inertia);

    public abstract EntitySpriteDefinition forUrchinState(boolean creditsGameplay);

    public abstract EntitySpriteDefinition forPokeySegment();

    public abstract EntitySpriteDefinition forStalfosEvasiveState(int privateState1);

    public abstract EntitySpriteDefinition forBushCrawlerState(int privateState1, EntityRoomLoader.RoomTable roomTable);

    public abstract EntitySpriteDefinition forBushCrawlerCrawlState(
            EntityRoomLoader.RoomTable roomTable, int privateState4);

    public abstract EntitySpriteDefinition forIronMaskState(int privateState2);

    public abstract EntitySpriteDefinition forMiniMoldormState(
            int headVariant, int entityX, int entityY, int segment1X, int segment1Y, int segment2X, int segment2Y);

    public abstract EntitySpriteDefinition forMoldormState(
            int headVariant,
            int entityX,
            int entityY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter);

    public abstract EntitySpriteDefinition forMoldormPresentation(
            int headVariant,
            int headX,
            int headY,
            int renderBaseX,
            int renderBaseY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter);

    public abstract EntitySpriteDefinition forSlimeEyeState(boolean half, int variant, int z);

    public abstract EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY);

    public abstract EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY, int spriteVariant);

    public abstract EntitySpriteDefinition forColorShellState(int entityType, int state, EntityStatus status);

    public abstract EntitySpriteDefinition forBurningEntity();

    public abstract EntitySpriteDefinition forFallingEntity();

    public abstract EntitySpriteDefinition forBombRightBeforeExploding();

    public abstract EntitySpriteDefinition forBombExplosion();

    public abstract EntitySpriteDefinition forDeathEntity();

    public abstract EntitySpriteDefinition forPowerRecoilDeathEntity();

    public abstract EntitySpriteDefinition forFloatingItemOverlay();

    public abstract EntitySpriteDefinition forZolSlimeEye();

    protected abstract EntitySpriteDefinition decodeHidingZol(int entityType);

    protected abstract EntitySpriteDefinition decodeFish(int entityType);

    protected abstract EntitySpriteDefinition decodeMoblinSword(int entityType);

    protected abstract void appendMoblinSwordGeneratedPair(
            List<EntitySpriteDefinition.DynamicSprite> sprites, int yOffset, int xOffset, int tileSelector);

    protected abstract EntitySpriteDefinition decodeFloatingItemMain(int entityType);

    public abstract EntitySpriteDefinition forFollowerEntityType(int entityType);

    public abstract EntitySpriteDefinition forMoblinKingState(int bodyVariant, int weaponVariant, boolean airborne);

    public abstract EntitySpriteDefinition forBombArrow();

    public abstract EntitySpriteDefinition forMagicRodFireState();

    public abstract EntitySpriteDefinition forMagicPowderTorchState(boolean lighting);

    protected abstract EntitySpriteDefinition decodeLiftableRock(EntityRoomLoader.RoomTable roomTable);

    protected abstract List<EntitySpriteDefinition.DynamicSprite> dynamicPair(EntitySpriteDefinition.Variant pair);

    protected abstract void appendDynamicRectangleVariants(
            List<List<EntitySpriteDefinition.DynamicSprite>> destination,
            int bank,
            int address,
            int variantCount,
            int spriteCount);

    public abstract EntitySpriteDefinition decodePair(
            int entityType, int bank, int address, int variantCount, int initialVariant);

    protected abstract EntitySpriteDefinition decodePair(
            int entityType,
            int bank,
            int address,
            int variantCount,
            int initialVariant,
            EntitySpriteDefinition.DynamicSprite.TileSource tileSource);

    public abstract EntitySpriteDefinition decodeSingle(
            int entityType, int bank, int address, int variantCount, int initialVariant);

    public abstract EntitySpriteDefinition decodeRectangle(
            int entityType, int bank, int address, int variantCount, int spriteCount, int initialVariant);

    protected abstract EntitySpriteDefinition decodePowerRecoilDeathRectangle();

    protected abstract List<List<EntitySpriteDefinition.RectangleSprite>> decodeRectangleVariants(
            int entityType, int bank, int address, int variantCount, int spriteCount, int initialVariant);

    protected abstract int validateDisplayList(
            int entityType, int bank, int address, int variantCount, int stride, int initialVariant);

    protected abstract int readByte(int bank, int address);

    protected abstract EntitySpriteDefinition decodeDungeonWarp();
}
