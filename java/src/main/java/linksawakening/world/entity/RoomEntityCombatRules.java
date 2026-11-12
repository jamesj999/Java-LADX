package linksawakening.world.entity;

/** CheckLinkCollisionWithEnemy and DefaultEnemyDamageCollisionHandler hitbox policies. */
public final class RoomEntityCombatRules {
    static final int ENTITY_PUSHED_BLOCK = 0x06;
    static final int ENTITY_OCTOROK = 0x09;
    static final int ENTITY_MOBLIN = 0x0B;
    static final int ENTITY_IRON_MASK = 0x24;
    static final int ENTITY_MOBLIN_SWORD = 0x14;
    static final int ENTITY_TEKTITE = 0x0D;
    static final int ENTITY_LEEVER = 0x0E;
    static final int ENTITY_ANTI_FAIRY = 0x15;
    static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    static final int ENTITY_STALFOS_EVASIVE = 0x1E;
    static final int ENTITY_ZOL = 0x1B;
    static final int ENTITY_GEL = 0x1C;
    static final int ENTITY_HIDING_ZOL = 0x9B;
    static final int ENTITY_STAR = 0x9C;
    static final int ENTITY_BLOOPER = 0xA9;
    static final int ENTITY_WINGED_OCTOROK = 0xAE;
    static final int ENTITY_PINCER = 0xB0;
    static final int ENTITY_BUSH_CRAWLER = 0xBB;
    static final int ENTITY_GIBDO = 0x1F;
    static final int ENTITY_MIMIC = 0x28;
    static final int ENTITY_MINI_MOLDORM = 0x29;
    static final int ENTITY_MOLDORM = 0x59;
    static final int ENTITY_MASKED_MIMIC_GORIYA = 0x8F;
    static final int ENTITY_THREE_OF_A_KIND = 0x90;
    static final int ENTITY_LIKE_LIKE = 0x23;
    static final int ENTITY_GOOMBA = 0x9F;
    static final int ENTITY_SNAKE = 0xA1;
    static final int ENTITY_PEAHAT = 0xA0;
    static final int ENTITY_ARMOS_STATUE = 0x0F;
    static final int ENTITY_ARMOS_KNIGHT = 0x88;
    static final int ENTITY_HINOX = 0x89;
    static final int ENTITY_ROLLING_BONES = 0x81;
    static final int ENTITY_ROLLING_BONES_BAR = 0x82;
    static final int ENTITY_HIDING_GHINI = 0x10;
    static final int ENTITY_GIANT_GHINI = 0x11;
    static final int ENTITY_GHINI = 0x12;
    static final int ENTITY_POLS_VOICE = 0x18;
    static final int ENTITY_KEESE = 0x19;
    static final int ENTITY_HARDHAT_BEETLE = 0x20;
    static final int ENTITY_SPIKED_BEETLE = 0x2C;
    static final int ENTITY_WIZROBE = 0x21;
    static final int ENTITY_SPIKE_TRAP = 0x27;
    static final int ENTITY_PAIRODD = 0x57;
    static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    static final int ENTITY_TIMER_BOMBITE = 0x56;
    static final int ENTITY_WATER_TEKTITE = 0x99;
    static final int ENTITY_FISH = 0xCC;
    static final int ENTITY_CROW = 0x7A;
    static final int ENTITY_CUCCO = 0x6C;
    static final int ENTITY_BOO_BUDDY = 0x50;
    static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    static final int ENTITY_COLOR_SHELL_GREEN = 0xEA;
    static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;
    static final int ENTITY_CRYSTAL_SWITCH = 0x66;
    static final int ENTITY_MAD_BOMBER = 0x93;
    static final int ENTITY_BOMBER = 0xBA;
    static final int ENTITY_GOPONGA_FLOWER = 0x7E;
    static final int ENTITY_GIANT_GOPONGA_FLOWER = 0x7C;
    static final int ENTITY_GOPONGA_FLOWER_PROJECTILE = 0x7D;
    static final int ENTITY_POKEY = 0xE3;
    static final int ENTITY_PIRANHA_PLANT = 0xA2;
    static final int ENTITY_ZORA = 0xCB;
    static final int ENTITY_ZOMBIE = 0xBF;
    static final int ENTITY_BUZZ_BLOB = 0xB9;
    static final int ENTITY_SAND_CRAB = 0xC6;
    static final int ENTITY_URCHIN = 0xC5;
    static final int ENTITY_WITCH_RAT = 0xE1;

    static final int ENTITY_OWL_EVENT = 0x41;
    static final int ENTITY_OWL_STATUE = 0x42;
    static final int ENTITY_TRENDY_GAME_OWNER = 0x4F;
    private static final int ENTITY_FISHERMAN_FISHING_GAME = 0x54;
    private static final int ENTITY_GENIE = 0x5C;
    private static final int ENTITY_RAFT_OWNER = 0x6A;
    static final int ENTITY_GRANDPA_ULRIRA = 0x77;
    static final int ENTITY_MADAM_MEOWMEOW = 0x79;
    static final int ENTITY_CRAZY_TRACY = 0x7B;
    private static final int ENTITY_TURTLE_ROCK_HEAD = 0x7F;
    static final int ENTITY_HOLE_FILLER = 0xB1;
    static final int ENTITY_PAPAHL = 0xB6;
    private static final int ENTITY_MOVING_BLOCK_MOVER = 0x69;
    private static final int ENTITY_SMASHABLE_PILLAR = 0xA7;
    private static final int ENTITY_LIFTABLE_STATUE = 0x9D;
    static final int ENTITY_BUNNY_D3 = 0xD3;
    static final int ENTITY_BANANAS_SCHULE_SALE = 0xCD;
    private static final int ENTITY_THWIMP = 0xD7;
    private static final int ENTITY_THWOMP = 0xD8;
    private static final int ENTITY_THWOMP_RAMMABLE = 0xD9;
    private static final int ENTITY_FLYING_ROOSTER_EVENTS = 0xDC;
    private static final int ENTITY_COLOR_GUARDIAN_BLUE = 0xF6;
    private static final int ENTITY_COLOR_GUARDIAN_RED = 0xF7;
    static final int ENTITY_PHOTOGRAPHER = 0xFA;

    static final int ENTITY_MARIN = 0x3E;
    static final int ENTITY_TARIN = 0x3F;
    static final int ENTITY_WITCH = 0x40;
    static final int ENTITY_SHOP_OWNER = 0x4D;
    private static final int ENTITY_DOG = 0x6F;
    static final int ENTITY_KID_70 = 0x70;
    static final int ENTITY_KID_71 = 0x71;
    static final int ENTITY_KID_72 = 0x72;
    static final int ENTITY_KID_73 = 0x73;
    static final int ENTITY_PAPAHLS_WIFE = 0x74;
    static final int ENTITY_GRANDMA_ULRIRA = 0x75;
    static final int ENTITY_MR_WRITE = 0x76;
    private static final int ENTITY_MR_WRITES_BIRD = 0x85;
    private static final int ENTITY_RICHARD = 0x95;
    private static final int ENTITY_RICHARD_FROG = 0x96;
    private static final int ENTITY_KIKI = 0xAD;
    static final int ENTITY_TARIN_BEEKEEPER = 0xB4;
    private static final int ENTITY_BEAR = 0xB5;
    static final int ENTITY_MERMAID = 0xB7;
    static final int ENTITY_MARIN_SHORE = 0xC1;
    static final int ENTITY_MARIN_TAL_TAL = 0xC2;
    private static final int ENTITY_MAMU = 0xC3;
    private static final int ENTITY_WALRUS = 0xC4;
    private static final int ENTITY_MANBO_AND_FISHES = 0xC7;
    static final int ENTITY_MERMAID_STATUE = 0xCE;
    static final int ENTITY_ANIMAL_D0 = 0xD0;
    static final int ENTITY_ANIMAL_D1 = 0xD1;
    static final int ENTITY_ANIMAL_D2 = 0xD2;
    private static final int ENTITY_GHOST = 0xD4;
    static final int ENTITY_ROOSTER = 0xD5;

    // HitboxPositions in home/entities.asm:3AAA. The NPC entries are not the
    // ordinary enemy entry: Marin and Tarin select HITFLAGS_HITBOX_NPC ($18),
    // while Bear/Mamu/Walrus select HITFLAGS_HITBOX_BIG_NPC ($34).
    // CheckLinkCollisionWithEnemy subtracts Link's center, then adds its half-extent.
    private static final int LINK_CENTER_OFFSET = 0x08;
    private static final int LINK_HALF_EXTENT = 0x04;
    private static final int HITBOX_X = 0x08;
    static final int HITBOX_WIDTH = 0x05;
    private static final int HITBOX_Y = 0x08;
    static final int HITBOX_HEIGHT = 0x05;
    private static final int NPC_HITBOX_X = 0x08;
    static final int NPC_HITBOX_WIDTH = 0x06;
    private static final int NPC_HITBOX_Y = 0x06;
    static final int NPC_HITBOX_HEIGHT = 0x08;
    private static final int BIG_NPC_HITBOX_X = 0x10;
    static final int BIG_NPC_HITBOX_WIDTH = 0x0C;
    private static final int BIG_NPC_HITBOX_Y = 0x08;
    static final int BIG_NPC_HITBOX_HEIGHT = 0x10;
    private static final int PILLAR_HITBOX_X = 0x08;
    static final int PILLAR_HITBOX_WIDTH = 0x07;
    private static final int PILLAR_HITBOX_Y = 0x04;
    static final int PILLAR_HITBOX_HEIGHT = 0x0A;
    private static final int SIDE_VIEW_PLATFORM_HITBOX_X = 0x10;
    static final int SIDE_VIEW_PLATFORM_HITBOX_WIDTH = 0x10;
    private static final int SIDE_VIEW_PLATFORM_HITBOX_Y = 0x0C;
    static final int SIDE_VIEW_PLATFORM_HITBOX_HEIGHT = 0x12;
    static final int SMALL_ENEMY_HITBOX_WIDTH = 0x02;
    static final int SMALL_ENEMY_HITBOX_HEIGHT = 0x02;
    static final int BIG_ENEMY_HITBOX_WIDTH = 0x0A;
    static final int BIG_ENEMY_HITBOX_HEIGHT = 0x0A;

    // HealthGroupForEntity (bank 3:41F6), InitialHealthForGroup (bank
    // 3:47BC), and EntityDamagesForGroup (bank 3:47F1).
    static final int OCTOROK_AND_KEESE_CONTACT_DAMAGE = 0x04;
    static final int MOBLIN_CONTACT_DAMAGE = 0x04;
    static final int GHINI_CONTACT_DAMAGE = 0x08;
    static final int HARDHAT_CONTACT_DAMAGE = 0x08;
    static final int WIZROBE_CONTACT_DAMAGE = 0x08;
    static final int ANTI_FAIRY_CONTACT_DAMAGE = 0x04;
    static final int GIBDO_CONTACT_DAMAGE = 0x08;
    static final int CROW_CONTACT_DAMAGE = 0x08;
    static final int BOO_BUDDY_CONTACT_DAMAGE = 0x08;
    static final int OCTOROK_AND_KEESE_INITIAL_HEALTH = 0x01;
    static final int MOBLIN_INITIAL_HEALTH = 0x02;
    static final int GHINI_INITIAL_HEALTH = 0x08;
    static final int HARDHAT_INITIAL_HEALTH = 0x04;
    static final int WIZROBE_INITIAL_HEALTH = 0x04;
    static final int SPIKE_TRAP_CONTACT_DAMAGE = 0x08;
    static final int SPIKE_TRAP_INITIAL_HEALTH = 0x04;
    static final int CRYSTAL_SWITCH_CONTACT_DAMAGE = 0x04;
    static final int CRYSTAL_SWITCH_INITIAL_HEALTH = 0x08;
    static final int PAIRODD_INITIAL_HEALTH = 0x02;
    static final int ANTI_FAIRY_INITIAL_HEALTH = 0x04;
    static final int ZOL_INITIAL_HEALTH = 0x02;
    static final int GEL_INITIAL_HEALTH = 0x01;
    static final int HIDING_ZOL_INITIAL_HEALTH = 0x01;
    static final int GIBDO_INITIAL_HEALTH = 0x06;
    static final int CROW_INITIAL_HEALTH = 0x02;
    static final int BOO_BUDDY_INITIAL_HEALTH = 0x04;
    static final int LIKE_LIKE_INITIAL_HEALTH = 0x02;
    static final int MAD_BOMBER_INITIAL_HEALTH = 0x04;
    static final int MAD_BOMBER_CONTACT_DAMAGE = 0x04;
    static final int BOMBER_INITIAL_HEALTH = 0x03;
    static final int BOMBER_CONTACT_DAMAGE = 0x08;
    static final int BOMBITE_INITIAL_HEALTH = 0x04;
    static final int BOMBITE_CONTACT_DAMAGE = 0x08;
    static final int PINCER_CONTACT_DAMAGE = 0x08;
    private static final int BASIC_SWORD_DAMAGE = 0x01;

    private RoomEntityCombatRules() {}

    static boolean supportsEnemyCollision(int type) {
        return EntityCollisionTypes.supportsEnemyCollision(type);
    }

    static boolean supportsLinkCollision(int type) {
        return supportsEnemyCollision(type)
                || EntityLinkCollisionRules.policyFor(type).restoreFinalPosition()
                || (type & 0xFF) == ENTITY_ARMOS_STATUE
                || (type & 0xFF) == ENTITY_ROOSTER
                || (type & 0xFF) == ENTITY_URCHIN;
    }

    /**
     * Compatibility predicate for callers that need to identify the
     * non-damaging PushLinkOutOfEntity handlers. The policy table is the
     * source of truth; enemy handlers remain available through
     * {@link #supportsEnemyCollision(int)} as well.
     */
    static boolean supportsFriendlyNpcCollision(int type) {
        return !supportsEnemyCollision(type)
                && EntityLinkCollisionRules.genericPolicyFor(type).restoreFinalPosition();
    }

    /**
     * Entity types that reach the ordinary ApplyLinkCollisionWithEnemy path.
     * The excluded handlers write their own Link action or state transition;
     * their behavior is intentionally left for their dedicated ports.
     */
    static boolean usesStandardLinkContactResponse(int type) {
        return switch (type & 0xFF) {
            case ENTITY_GEL, ENTITY_STAR, ENTITY_ANTI_FAIRY, ENTITY_BOUNCING_BOMBITE -> false;
            default -> true;
        };
    }

    static int contactDamage(int type) {
        return EntityCombatDefaults.contactDamage(type);
    }

    static int initialHealth(int type) {
        return EntityCombatDefaults.initialHealth(type);
    }

    static int basicSwordDamage(int type) {
        return (type & 0xFF) == ENTITY_WIZROBE ? 0 : (supportsEnemyCollision(type) ? BASIC_SWORD_DAMAGE : 0);
    }

    /**
     * Mirrors Options1ForEntity._27 in data/entities/options1.asm: the spike
     * trap takes the EnemyCollidedWithSword sword-clink path.
     */
    static boolean swordPokeForSwordCollision(int type) {
        return (type & 0xFF) == ENTITY_SPIKE_TRAP || (type & 0xFF) == ENTITY_MOLDORM;
    }

    /** Adds a handler-owned dynamic sword-clink flag to the static exceptions. */
    static boolean swordPokeForSwordCollision(int type, boolean dynamicSwordClinkOff) {
        return dynamicSwordClinkOff || swordPokeForSwordCollision(type);
    }

    /** Mirrors func_003_6C6B's alternating entity-slot cadence. */
    public static boolean collisionCadenceMatches(int frameCounter, int slot) {
        return (((frameCounter & 0xFF) ^ slot) & 0x01) != 0;
    }

    /** Mirrors CheckLinkCollisionWithEnemy using the entity's ROM hitbox entry. */
    public static boolean overlapsLink(RoomEntity entity, int linkPixelX, int linkPixelY) {
        if (!supportsLinkCollision(entity.type())) {
            return false;
        }
        int hitboxX = hitboxX(entity.type());
        int hitboxWidth = hitboxWidth(entity.type());
        int hitboxY = hitboxY(entity.type());
        int hitboxHeight = hitboxHeight(entity.type());
        int xDistance = unsignedByteAbs(entity.x() + hitboxX - linkPixelX - LINK_CENTER_OFFSET);
        if (xDistance >= hitboxWidth + LINK_HALF_EXTENT) {
            return false;
        }
        int yDistance = unsignedByteAbs(entity.y() - entity.z() + hitboxY - linkPixelY - LINK_CENTER_OFFSET);
        return yDistance < hitboxHeight + LINK_HALF_EXTENT;
    }

    /**
     * Mirrors DefaultEnemyDamageCollisionHandler's four unsigned-byte
     * comparisons against wC140..wC143.
     */
    static boolean overlapsSword(RoomEntity entity, int swordX, int swordWidth, int swordY, int swordHeight) {
        if (!supportsEnemyCollision(entity.type()) || swordWidth <= 0 || swordHeight <= 0) {
            return false;
        }
        int hitboxX = hitboxX(entity.type());
        int hitboxWidth = hitboxWidth(entity.type());
        int hitboxY = hitboxY(entity.type());
        int hitboxHeight = hitboxHeight(entity.type());
        int xDistance = unsignedByteAbs(entity.x() + hitboxX - swordX);
        if (xDistance >= hitboxWidth + swordWidth) {
            return false;
        }
        int yDistance = unsignedByteAbs(entity.y() - entity.z() + hitboxY - swordY);
        return yDistance < hitboxHeight + swordHeight;
    }

    private static int hitboxWidth(int type) {
        return EntityCollisionTypes.hitboxWidth(type);
    }

    private static int hitboxHeight(int type) {
        return EntityCollisionTypes.hitboxHeight(type);
    }

    private static int hitboxX(int type) {
        if (usesBigNpcHitbox(type)) {
            return BIG_NPC_HITBOX_X;
        }
        if (usesPillarHitbox(type)) {
            return PILLAR_HITBOX_X;
        }
        if (usesSideViewPlatformHitbox(type)) {
            return SIDE_VIEW_PLATFORM_HITBOX_X;
        }
        return usesNpcHitbox(type) ? NPC_HITBOX_X : HITBOX_X;
    }

    private static int hitboxY(int type) {
        if ((type & 0xFF) == ENTITY_WITCH) {
            return 0x0C;
        }
        if (usesBigNpcHitbox(type)) {
            return BIG_NPC_HITBOX_Y;
        }
        if (usesPillarHitbox(type)) {
            return PILLAR_HITBOX_Y;
        }
        if (usesSideViewPlatformHitbox(type)) {
            return SIDE_VIEW_PLATFORM_HITBOX_Y;
        }
        return usesNpcHitbox(type) ? NPC_HITBOX_Y : HITBOX_Y;
    }

    /** HITFLAGS_HITBOX_NPC ($18) from data/entities/hitbox_flags.asm. */
    static boolean usesNpcHitbox(int type) {
        return EntityCollisionTypes.usesNpcHitbox(type);
    }

    static boolean usesPillarHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_LIFTABLE_STATUE, ENTITY_SMASHABLE_PILLAR -> true;
            default -> false;
        };
    }

    static boolean usesSideViewPlatformHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_THWOMP, ENTITY_THWOMP_RAMMABLE -> true;
            default -> false;
        };
    }

    /** HITFLAGS_HITBOX_BIG_NPC ($34) from data/entities/hitbox_flags.asm. */
    static boolean usesBigNpcHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_BEAR, ENTITY_MAMU, ENTITY_WALRUS, ENTITY_MANBO_AND_FISHES -> true;
            default -> false;
        };
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }
}
