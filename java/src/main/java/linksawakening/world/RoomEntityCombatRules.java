package linksawakening.world;

/** ROM collision constants and predicates for the currently ported enemies. */
public final class RoomEntityCombatRules {
    private static final int ENTITY_PUSHED_BLOCK = 0x06;
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_IRON_MASK = 0x24;
    private static final int ENTITY_MOBLIN_SWORD = 0x14;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_LEEVER = 0x0E;
    private static final int ENTITY_ANTI_FAIRY = 0x15;
    private static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    private static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    private static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    private static final int ENTITY_STALFOS_EVASIVE = 0x1E;
    private static final int ENTITY_ZOL = 0x1B;
    private static final int ENTITY_GEL = 0x1C;
    private static final int ENTITY_HIDING_ZOL = 0x9B;
    private static final int ENTITY_STAR = 0x9C;
    private static final int ENTITY_BLOOPER = 0xA9;
    private static final int ENTITY_WINGED_OCTOROK = 0xAE;
    private static final int ENTITY_PINCER = 0xB0;
    private static final int ENTITY_BUSH_CRAWLER = 0xBB;
    private static final int ENTITY_GIBDO = 0x1F;
    private static final int ENTITY_MIMIC = 0x28;
    private static final int ENTITY_MINI_MOLDORM = 0x29;
    private static final int ENTITY_MOLDORM = 0x59;
    private static final int ENTITY_MASKED_MIMIC_GORIYA = 0x8F;
    private static final int ENTITY_LIKE_LIKE = 0x23;
    private static final int ENTITY_GOOMBA = 0x9F;
    private static final int ENTITY_SNAKE = 0xA1;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_ARMOS_KNIGHT = 0x88;
    private static final int ENTITY_HIDING_GHINI = 0x10;
    private static final int ENTITY_GIANT_GHINI = 0x11;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_POLS_VOICE = 0x18;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;
    private static final int ENTITY_SPIKED_BEETLE = 0x2C;
    private static final int ENTITY_WIZROBE = 0x21;
    private static final int ENTITY_SPIKE_TRAP = 0x27;
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    private static final int ENTITY_TIMER_BOMBITE = 0x56;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
    private static final int ENTITY_FISH = 0xCC;
    private static final int ENTITY_CROW = 0x7A;
    private static final int ENTITY_CUCCO = 0x6C;
    private static final int ENTITY_BOO_BUDDY = 0x50;
    private static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    private static final int ENTITY_COLOR_SHELL_GREEN = 0xEA;
    private static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;
    private static final int ENTITY_CRYSTAL_SWITCH = 0x66;
    private static final int ENTITY_MAD_BOMBER = 0x93;
    private static final int ENTITY_BOMBER = 0xBA;
    private static final int ENTITY_GOPONGA_FLOWER = 0x7E;
    private static final int ENTITY_GIANT_GOPONGA_FLOWER = 0x7C;
    private static final int ENTITY_GOPONGA_FLOWER_PROJECTILE = 0x7D;
    private static final int ENTITY_POKEY = 0xE3;
    private static final int ENTITY_PIRANHA_PLANT = 0xA2;
    private static final int ENTITY_ZORA = 0xCB;
    private static final int ENTITY_ZOMBIE = 0xBF;
    private static final int ENTITY_BUZZ_BLOB = 0xB9;
    private static final int ENTITY_SAND_CRAB = 0xC6;
    private static final int ENTITY_URCHIN = 0xC5;
    private static final int ENTITY_WITCH_RAT = 0xE1;

    private static final int ENTITY_OWL_EVENT = 0x41;
    private static final int ENTITY_OWL_STATUE = 0x42;
    private static final int ENTITY_TRENDY_GAME_OWNER = 0x4F;
    private static final int ENTITY_FISHERMAN_FISHING_GAME = 0x54;
    private static final int ENTITY_GENIE = 0x5C;
    private static final int ENTITY_RAFT_OWNER = 0x6A;
    private static final int ENTITY_GRANDPA_ULRIRA = 0x77;
    private static final int ENTITY_MADAM_MEOWMEOW = 0x79;
    private static final int ENTITY_CRAZY_TRACY = 0x7B;
    private static final int ENTITY_TURTLE_ROCK_HEAD = 0x7F;
    private static final int ENTITY_HOLE_FILLER = 0xB1;
    private static final int ENTITY_PAPAHL = 0xB6;
    private static final int ENTITY_MOVING_BLOCK_MOVER = 0x69;
    private static final int ENTITY_SMASHABLE_PILLAR = 0xA7;
    private static final int ENTITY_LIFTABLE_STATUE = 0x9D;
    private static final int ENTITY_BUNNY_D3 = 0xD3;
    private static final int ENTITY_BANANAS_SCHULE_SALE = 0xCD;
    private static final int ENTITY_THWIMP = 0xD7;
    private static final int ENTITY_THWOMP = 0xD8;
    private static final int ENTITY_THWOMP_RAMMABLE = 0xD9;
    private static final int ENTITY_FLYING_ROOSTER_EVENTS = 0xDC;
    private static final int ENTITY_COLOR_GUARDIAN_BLUE = 0xF6;
    private static final int ENTITY_COLOR_GUARDIAN_RED = 0xF7;
    private static final int ENTITY_PHOTOGRAPHER = 0xFA;

    private static final int ENTITY_MARIN = 0x3E;
    private static final int ENTITY_TARIN = 0x3F;
    private static final int ENTITY_WITCH = 0x40;
    private static final int ENTITY_SHOP_OWNER = 0x4D;
    private static final int ENTITY_DOG = 0x6F;
    private static final int ENTITY_KID_70 = 0x70;
    private static final int ENTITY_KID_71 = 0x71;
    private static final int ENTITY_KID_72 = 0x72;
    private static final int ENTITY_KID_73 = 0x73;
    private static final int ENTITY_PAPAHLS_WIFE = 0x74;
    private static final int ENTITY_GRANDMA_ULRIRA = 0x75;
    private static final int ENTITY_MR_WRITE = 0x76;
    private static final int ENTITY_MR_WRITES_BIRD = 0x85;
    private static final int ENTITY_RICHARD = 0x95;
    private static final int ENTITY_RICHARD_FROG = 0x96;
    private static final int ENTITY_KIKI = 0xAD;
    private static final int ENTITY_TARIN_BEEKEEPER = 0xB4;
    private static final int ENTITY_BEAR = 0xB5;
    private static final int ENTITY_MERMAID = 0xB7;
    private static final int ENTITY_MARIN_SHORE = 0xC1;
    private static final int ENTITY_MARIN_TAL_TAL = 0xC2;
    private static final int ENTITY_MAMU = 0xC3;
    private static final int ENTITY_WALRUS = 0xC4;
    private static final int ENTITY_MANBO_AND_FISHES = 0xC7;
    private static final int ENTITY_MERMAID_STATUE = 0xCE;
    private static final int ENTITY_ANIMAL_D0 = 0xD0;
    private static final int ENTITY_ANIMAL_D1 = 0xD1;
    private static final int ENTITY_ANIMAL_D2 = 0xD2;
    private static final int ENTITY_GHOST = 0xD4;
    private static final int ENTITY_ROOSTER = 0xD5;

    // HitboxPositions in home/entities.asm:3AAA. The NPC entries are not the
    // ordinary enemy entry: Marin and Tarin select HITFLAGS_HITBOX_NPC ($18),
    // while Bear/Mamu/Walrus select HITFLAGS_HITBOX_BIG_NPC ($34).
    private static final int HITBOX_X = 0x08;
    private static final int HITBOX_WIDTH = 0x05;
    private static final int HITBOX_Y = 0x08;
    private static final int HITBOX_HEIGHT = 0x05;
    private static final int NPC_HITBOX_X = 0x08;
    private static final int NPC_HITBOX_WIDTH = 0x06;
    private static final int NPC_HITBOX_Y = 0x06;
    private static final int NPC_HITBOX_HEIGHT = 0x08;
    private static final int BIG_NPC_HITBOX_X = 0x10;
    private static final int BIG_NPC_HITBOX_WIDTH = 0x0C;
    private static final int BIG_NPC_HITBOX_Y = 0x08;
    private static final int BIG_NPC_HITBOX_HEIGHT = 0x10;
    private static final int PILLAR_HITBOX_X = 0x08;
    private static final int PILLAR_HITBOX_WIDTH = 0x07;
    private static final int PILLAR_HITBOX_Y = 0x04;
    private static final int PILLAR_HITBOX_HEIGHT = 0x0A;
    private static final int SIDE_VIEW_PLATFORM_HITBOX_X = 0x10;
    private static final int SIDE_VIEW_PLATFORM_HITBOX_WIDTH = 0x10;
    private static final int SIDE_VIEW_PLATFORM_HITBOX_Y = 0x0C;
    private static final int SIDE_VIEW_PLATFORM_HITBOX_HEIGHT = 0x12;
    private static final int SMALL_ENEMY_HITBOX_WIDTH = 0x02;
    private static final int SMALL_ENEMY_HITBOX_HEIGHT = 0x02;
    private static final int BIG_ENEMY_HITBOX_WIDTH = 0x0A;
    private static final int BIG_ENEMY_HITBOX_HEIGHT = 0x0A;

    // HealthGroupForEntity (bank 3:41F6), InitialHealthForGroup (bank
    // 3:47BC), and EntityDamagesForGroup (bank 3:47F1).
    private static final int OCTOROK_AND_KEESE_CONTACT_DAMAGE = 0x04;
    private static final int MOBLIN_CONTACT_DAMAGE = 0x04;
    private static final int GHINI_CONTACT_DAMAGE = 0x08;
    private static final int HARDHAT_CONTACT_DAMAGE = 0x08;
    private static final int WIZROBE_CONTACT_DAMAGE = 0x08;
    private static final int ANTI_FAIRY_CONTACT_DAMAGE = 0x04;
    private static final int GIBDO_CONTACT_DAMAGE = 0x08;
    private static final int CROW_CONTACT_DAMAGE = 0x08;
    private static final int BOO_BUDDY_CONTACT_DAMAGE = 0x08;
    private static final int OCTOROK_AND_KEESE_INITIAL_HEALTH = 0x01;
    private static final int MOBLIN_INITIAL_HEALTH = 0x02;
    private static final int GHINI_INITIAL_HEALTH = 0x08;
    private static final int HARDHAT_INITIAL_HEALTH = 0x04;
    private static final int WIZROBE_INITIAL_HEALTH = 0x04;
    private static final int SPIKE_TRAP_CONTACT_DAMAGE = 0x08;
    private static final int SPIKE_TRAP_INITIAL_HEALTH = 0x04;
    private static final int CRYSTAL_SWITCH_CONTACT_DAMAGE = 0x04;
    private static final int CRYSTAL_SWITCH_INITIAL_HEALTH = 0x08;
    private static final int PAIRODD_INITIAL_HEALTH = 0x02;
    private static final int ANTI_FAIRY_INITIAL_HEALTH = 0x04;
    private static final int ZOL_INITIAL_HEALTH = 0x02;
    private static final int GEL_INITIAL_HEALTH = 0x01;
    private static final int HIDING_ZOL_INITIAL_HEALTH = 0x01;
    private static final int GIBDO_INITIAL_HEALTH = 0x06;
    private static final int CROW_INITIAL_HEALTH = 0x02;
    private static final int BOO_BUDDY_INITIAL_HEALTH = 0x04;
    private static final int LIKE_LIKE_INITIAL_HEALTH = 0x02;
    private static final int MAD_BOMBER_INITIAL_HEALTH = 0x04;
    private static final int MAD_BOMBER_CONTACT_DAMAGE = 0x04;
    private static final int BOMBER_INITIAL_HEALTH = 0x03;
    private static final int BOMBER_CONTACT_DAMAGE = 0x08;
    private static final int BOMBITE_INITIAL_HEALTH = 0x04;
    private static final int BOMBITE_CONTACT_DAMAGE = 0x08;
    private static final int PINCER_CONTACT_DAMAGE = 0x08;
    private static final int BASIC_SWORD_DAMAGE = 0x01;

    private RoomEntityCombatRules() {
    }

    static boolean supportsEnemyCollision(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_MOBLIN, ENTITY_IRON_MASK, ENTITY_MOBLIN_SWORD,
                ENTITY_TEKTITE, ENTITY_LEEVER,
                ENTITY_ANTI_FAIRY, ENTITY_SPARK_COUNTER_CLOCKWISE,
                ENTITY_SPARK_CLOCKWISE,
                ENTITY_STALFOS_AGGRESSIVE, ENTITY_STALFOS_EVASIVE,
                ENTITY_ZOL, ENTITY_GEL, ENTITY_HIDING_ZOL,
                ENTITY_STAR, ENTITY_BLOOPER, ENTITY_WINGED_OCTOROK, ENTITY_PINCER,
                ENTITY_BUSH_CRAWLER,
                ENTITY_GIBDO, ENTITY_POLS_VOICE, ENTITY_LIKE_LIKE, ENTITY_PEAHAT,
                ENTITY_GOOMBA, ENTITY_SNAKE,
                ENTITY_WIZROBE,
                ENTITY_ARMOS_STATUE,
                ENTITY_ARMOS_KNIGHT,
                ENTITY_HIDING_GHINI, ENTITY_GIANT_GHINI, ENTITY_GHINI,
                ENTITY_HARDHAT_BEETLE, ENTITY_SPIKE_TRAP, ENTITY_WATER_TEKTITE, ENTITY_FISH,
                ENTITY_CROW,
                ENTITY_CUCCO, ENTITY_GOPONGA_FLOWER, ENTITY_GIANT_GOPONGA_FLOWER,
                ENTITY_GOPONGA_FLOWER_PROJECTILE,
                ENTITY_POKEY, ENTITY_PIRANHA_PLANT, ENTITY_ZORA, ENTITY_ZOMBIE,
                ENTITY_BUZZ_BLOB, ENTITY_SAND_CRAB, ENTITY_URCHIN,
                ENTITY_BOO_BUDDY,
                ENTITY_SPIKED_BEETLE,
                ENTITY_PAIRODD, ENTITY_COLOR_SHELL_RED, ENTITY_COLOR_SHELL_GREEN,
                ENTITY_COLOR_SHELL_BLUE,
                ENTITY_CRYSTAL_SWITCH,
                ENTITY_OCTOROK, ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE,
                ENTITY_MAD_BOMBER, ENTITY_BOMBER, ENTITY_MIMIC, ENTITY_MINI_MOLDORM,
                ENTITY_MOLDORM,
                ENTITY_MASKED_MIMIC_GORIYA, ENTITY_WITCH_RAT -> true;
            default -> false;
        };
    }

    static boolean supportsLinkCollision(int type) {
        return supportsEnemyCollision(type)
            || EntityLinkCollisionRules.policyFor(type).restoreFinalPosition()
            || (type & 0xFF) == ENTITY_ARMOS_STATUE || (type & 0xFF) == ENTITY_ROOSTER
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
            case ENTITY_GEL, ENTITY_STAR, ENTITY_ANTI_FAIRY,
                ENTITY_BOUNCING_BOMBITE, ENTITY_SPIKED_BEETLE -> false;
            default -> true;
        };
    }

    static int contactDamage(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_OCTOROK, ENTITY_PEAHAT, ENTITY_ZOL, ENTITY_GEL,
                ENTITY_WATER_TEKTITE, ENTITY_PAIRODD,
                ENTITY_HIDING_ZOL, ENTITY_STAR, ENTITY_BLOOPER, ENTITY_WINGED_OCTOROK,
                ENTITY_GOOMBA, ENTITY_SNAKE, ENTITY_BUSH_CRAWLER, ENTITY_POKEY,
                ENTITY_PIRANHA_PLANT, ENTITY_ZORA, ENTITY_ZOMBIE ->
                OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_BUZZ_BLOB -> 0x08;
            case ENTITY_SAND_CRAB -> 0x04;
            case ENTITY_URCHIN -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_FISH -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_CROW -> CROW_CONTACT_DAMAGE;
            // Cucco's static health-group table points at the ordinary enemy
            // damage row; physics_flags.asm marks it harmless, and the
            // runtime suppresses this raw value at the entity boundary.
            case ENTITY_CUCCO -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_GOPONGA_FLOWER, ENTITY_GIANT_GOPONGA_FLOWER -> 0x08;
            case ENTITY_BOO_BUDDY -> BOO_BUDDY_CONTACT_DAMAGE;
            case ENTITY_ARMOS_STATUE -> 0x10;
            case ENTITY_ARMOS_KNIGHT -> 0x0C;
            case ENTITY_MOBLIN, ENTITY_IRON_MASK, ENTITY_MOBLIN_SWORD, ENTITY_TEKTITE, ENTITY_LEEVER,
                ENTITY_STALFOS_AGGRESSIVE, ENTITY_STALFOS_EVASIVE,
                ENTITY_MINI_MOLDORM ->
                MOBLIN_CONTACT_DAMAGE;
            case ENTITY_MIMIC -> 0x18;
            case ENTITY_MASKED_MIMIC_GORIYA -> 0x04;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE ->
                OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_MAD_BOMBER -> MAD_BOMBER_CONTACT_DAMAGE;
            case ENTITY_BOMBER -> BOMBER_CONTACT_DAMAGE;
            case ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE -> BOMBITE_CONTACT_DAMAGE;
            case ENTITY_GIBDO -> GIBDO_CONTACT_DAMAGE;
            case ENTITY_POLS_VOICE -> 0x08;
            case ENTITY_LIKE_LIKE -> 0;
            case ENTITY_ANTI_FAIRY -> ANTI_FAIRY_CONTACT_DAMAGE;
            case ENTITY_HIDING_GHINI, ENTITY_GIANT_GHINI, ENTITY_GHINI ->
                GHINI_CONTACT_DAMAGE;
            case ENTITY_HARDHAT_BEETLE -> HARDHAT_CONTACT_DAMAGE;
            case ENTITY_SPIKED_BEETLE -> 0x04;
            case ENTITY_WIZROBE -> WIZROBE_CONTACT_DAMAGE;
            case ENTITY_SPIKE_TRAP -> SPIKE_TRAP_CONTACT_DAMAGE;
            case ENTITY_CRYSTAL_SWITCH -> CRYSTAL_SWITCH_CONTACT_DAMAGE;
            case ENTITY_PINCER -> PINCER_CONTACT_DAMAGE;
            case ENTITY_WITCH_RAT -> 0x04;
            case ENTITY_MOLDORM -> 0x08;
            default -> 0;
        };
    }

    static int initialHealth(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_OCTOROK, ENTITY_PEAHAT, ENTITY_WATER_TEKTITE,
                ENTITY_STAR, ENTITY_BLOOPER, ENTITY_WINGED_OCTOROK,
                ENTITY_GOOMBA, ENTITY_SNAKE, ENTITY_POKEY, ENTITY_PIRANHA_PLANT,
                ENTITY_ZORA, ENTITY_ZOMBIE ->
                OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_BUZZ_BLOB -> 0x04;
            case ENTITY_SAND_CRAB -> 0x02;
            case ENTITY_FISH -> OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_CROW -> CROW_INITIAL_HEALTH;
            case ENTITY_CUCCO -> 0x01;
            case ENTITY_GOPONGA_FLOWER, ENTITY_GIANT_GOPONGA_FLOWER -> 0x04;
            case ENTITY_BOO_BUDDY -> BOO_BUDDY_INITIAL_HEALTH;
            case ENTITY_ARMOS_STATUE -> 0x04;
            case ENTITY_ARMOS_KNIGHT -> 0x0C;
            case ENTITY_PAIRODD -> PAIRODD_INITIAL_HEALTH;
            case ENTITY_MOBLIN, ENTITY_IRON_MASK, ENTITY_MOBLIN_SWORD, ENTITY_TEKTITE, ENTITY_LEEVER,
                ENTITY_STALFOS_AGGRESSIVE, ENTITY_STALFOS_EVASIVE,
                ENTITY_MINI_MOLDORM ->
                MOBLIN_INITIAL_HEALTH;
            case ENTITY_MIMIC -> 0x04;
            case ENTITY_MASKED_MIMIC_GORIYA -> 0x02;
            case ENTITY_PINCER -> 0x02;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE ->
                OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_ZOL -> ZOL_INITIAL_HEALTH;
            case ENTITY_HIDING_ZOL -> HIDING_ZOL_INITIAL_HEALTH;
            case ENTITY_GEL -> GEL_INITIAL_HEALTH;
            case ENTITY_GIBDO -> GIBDO_INITIAL_HEALTH;
            case ENTITY_POLS_VOICE -> 0x04;
            case ENTITY_LIKE_LIKE -> LIKE_LIKE_INITIAL_HEALTH;
            case ENTITY_MAD_BOMBER -> MAD_BOMBER_INITIAL_HEALTH;
            case ENTITY_BOMBER -> BOMBER_INITIAL_HEALTH;
            case ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE -> BOMBITE_INITIAL_HEALTH;
            case ENTITY_ANTI_FAIRY -> ANTI_FAIRY_INITIAL_HEALTH;
            case ENTITY_HIDING_GHINI, ENTITY_GIANT_GHINI, ENTITY_GHINI ->
                GHINI_INITIAL_HEALTH;
            case ENTITY_HARDHAT_BEETLE -> HARDHAT_INITIAL_HEALTH;
            case ENTITY_SPIKED_BEETLE -> 0x02;
            case ENTITY_WIZROBE -> WIZROBE_INITIAL_HEALTH;
            case ENTITY_SPIKE_TRAP -> SPIKE_TRAP_INITIAL_HEALTH;
            case ENTITY_CRYSTAL_SWITCH -> CRYSTAL_SWITCH_INITIAL_HEALTH;
            case ENTITY_WITCH_RAT -> 0x01;
            case ENTITY_MOLDORM -> 0x04;
            default -> 0;
        };
    }

    static int basicSwordDamage(int type) {
        return (type & 0xFF) == ENTITY_WIZROBE
            ? 0 : (supportsEnemyCollision(type) ? BASIC_SWORD_DAMAGE : 0);
    }

    /**
     * Mirrors Options1ForEntity._27 in data/entities/options1.asm: the spike
     * trap takes the EnemyCollidedWithSword sword-clink path.
     */
    static boolean swordPokeForSwordCollision(int type) {
        return (type & 0xFF) == ENTITY_SPIKE_TRAP
            || (type & 0xFF) == ENTITY_MOLDORM;
    }

    /** Adds a handler-owned dynamic sword-clink flag to the static exceptions. */
    static boolean swordPokeForSwordCollision(int type, boolean dynamicSwordClinkOff) {
        return dynamicSwordClinkOff || swordPokeForSwordCollision(type);
    }

    /** Mirrors func_003_6C6B's alternating entity-slot cadence. */
    static boolean collisionCadenceMatches(int frameCounter, int slot) {
        return (((frameCounter & 0xFF) ^ slot) & 0x01) != 0;
    }

    /** Mirrors CheckLinkCollisionWithEnemy using the entity's ROM hitbox entry. */
    static boolean overlapsLink(RoomEntity entity, int linkPixelX, int linkPixelY) {
        if (!supportsLinkCollision(entity.type())) {
            return false;
        }
        int hitboxX = hitboxX(entity.type());
        int hitboxWidth = hitboxWidth(entity.type());
        int hitboxY = hitboxY(entity.type());
        int hitboxHeight = hitboxHeight(entity.type());
        int xDistance = unsignedByteAbs(
            entity.x() + hitboxX - linkPixelX - 0x08);
        if (xDistance >= hitboxWidth + 0x04) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() - entity.z() + hitboxY - linkPixelY - 0x08);
        return yDistance < hitboxHeight + 0x04;
    }

    /**
     * Mirrors DefaultEnemyDamageCollisionHandler's four unsigned-byte
     * comparisons against wC140..wC143.
     */
    static boolean overlapsSword(RoomEntity entity, int swordX, int swordWidth,
                                 int swordY, int swordHeight) {
        if (!supportsEnemyCollision(entity.type()) || swordWidth <= 0 || swordHeight <= 0) {
            return false;
        }
        int hitboxX = hitboxX(entity.type());
        int hitboxWidth = hitboxWidth(entity.type());
        int hitboxY = hitboxY(entity.type());
        int hitboxHeight = hitboxHeight(entity.type());
        int xDistance = unsignedByteAbs(
            entity.x() + hitboxX - swordX);
        if (xDistance >= hitboxWidth + swordWidth) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() - entity.z() + hitboxY - swordY);
        return yDistance < hitboxHeight + swordHeight;
    }

    private static int hitboxWidth(int type) {
        if (usesBigNpcHitbox(type)) {
            return BIG_NPC_HITBOX_WIDTH;
        }
        if (usesNpcHitbox(type)) {
            return NPC_HITBOX_WIDTH;
        }
        if (usesPillarHitbox(type)) {
            return PILLAR_HITBOX_WIDTH;
        }
        if (usesSideViewPlatformHitbox(type)) {
            return SIDE_VIEW_PLATFORM_HITBOX_WIDTH;
        }
        return switch (type & 0xFF) {
            case ENTITY_GEL -> SMALL_ENEMY_HITBOX_WIDTH;
            case ENTITY_GIANT_GHINI, ENTITY_GIANT_GOPONGA_FLOWER,
                ENTITY_SPIKE_TRAP, ENTITY_ARMOS_KNIGHT, ENTITY_URCHIN,
                ENTITY_MOLDORM ->
                BIG_ENEMY_HITBOX_WIDTH;
            default -> HITBOX_WIDTH;
        };
    }

    private static int hitboxHeight(int type) {
        if (usesBigNpcHitbox(type)) {
            return BIG_NPC_HITBOX_HEIGHT;
        }
        if (usesNpcHitbox(type)) {
            return NPC_HITBOX_HEIGHT;
        }
        if (usesPillarHitbox(type)) {
            return PILLAR_HITBOX_HEIGHT;
        }
        if (usesSideViewPlatformHitbox(type)) {
            return SIDE_VIEW_PLATFORM_HITBOX_HEIGHT;
        }
        return switch (type & 0xFF) {
            case ENTITY_GEL -> SMALL_ENEMY_HITBOX_HEIGHT;
            case ENTITY_GIANT_GHINI, ENTITY_GIANT_GOPONGA_FLOWER,
                ENTITY_SPIKE_TRAP, ENTITY_ARMOS_KNIGHT, ENTITY_URCHIN,
                ENTITY_MOLDORM ->
                BIG_ENEMY_HITBOX_HEIGHT;
            default -> HITBOX_HEIGHT;
        };
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
    private static boolean usesNpcHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_MARIN, ENTITY_TARIN, ENTITY_WITCH, ENTITY_OWL_EVENT,
                ENTITY_OWL_STATUE, ENTITY_SHOP_OWNER, ENTITY_TRENDY_GAME_OWNER,
                ENTITY_KID_70, ENTITY_KID_71, ENTITY_KID_72, ENTITY_KID_73,
                ENTITY_PAPAHLS_WIFE, ENTITY_GRANDMA_ULRIRA, ENTITY_GRANDPA_ULRIRA,
                ENTITY_MADAM_MEOWMEOW, ENTITY_CRAZY_TRACY, ENTITY_MR_WRITE,
                ENTITY_HOLE_FILLER, ENTITY_PAPAHL,
                ENTITY_TARIN_BEEKEEPER, ENTITY_MERMAID, ENTITY_MARIN_SHORE,
                ENTITY_MARIN_TAL_TAL, ENTITY_MERMAID_STATUE, ENTITY_BANANAS_SCHULE_SALE,
                ENTITY_ANIMAL_D0, ENTITY_ANIMAL_D1, ENTITY_ANIMAL_D2, ENTITY_BUNNY_D3,
                ENTITY_PHOTOGRAPHER, ENTITY_ROOSTER -> true;
            default -> false;
        };
    }

    private static boolean usesPillarHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_LIFTABLE_STATUE, ENTITY_SMASHABLE_PILLAR -> true;
            default -> false;
        };
    }

    private static boolean usesSideViewPlatformHitbox(int type) {
        return switch (type & 0xFF) {
            case ENTITY_THWOMP, ENTITY_THWOMP_RAMMABLE -> true;
            default -> false;
        };
    }

    /** HITFLAGS_HITBOX_BIG_NPC ($34) from data/entities/hitbox_flags.asm. */
    private static boolean usesBigNpcHitbox(int type) {
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
