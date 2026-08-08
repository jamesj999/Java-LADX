package linksawakening.world;

/** ROM collision constants and predicates for the currently ported enemies. */
public final class RoomEntityCombatRules {
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
    private static final int ENTITY_GIBDO = 0x1F;
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
    private static final int ENTITY_BOO_BUDDY = 0x50;
    private static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    private static final int ENTITY_COLOR_SHELL_GREEN = 0xEA;
    private static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;
    private static final int ENTITY_CRYSTAL_SWITCH = 0x66;
    private static final int ENTITY_MAD_BOMBER = 0x93;
    private static final int ENTITY_BOMBER = 0xBA;

    // HitboxPositions._00 in home/entities.asm:3AAA. Octorok, Moblin, Armos,
    // and Keese all select the normal collision box in hitbox_flags.asm.
    private static final int HITBOX_X = 0x08;
    private static final int HITBOX_WIDTH = 0x05;
    private static final int HITBOX_Y = 0x08;
    private static final int HITBOX_HEIGHT = 0x05;
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
                ENTITY_GIBDO, ENTITY_POLS_VOICE, ENTITY_LIKE_LIKE, ENTITY_PEAHAT,
                ENTITY_GOOMBA, ENTITY_SNAKE,
                ENTITY_WIZROBE,
                ENTITY_ARMOS_STATUE,
                ENTITY_ARMOS_KNIGHT,
                ENTITY_HIDING_GHINI, ENTITY_GIANT_GHINI, ENTITY_GHINI,
                ENTITY_HARDHAT_BEETLE, ENTITY_SPIKE_TRAP, ENTITY_WATER_TEKTITE, ENTITY_FISH,
                ENTITY_CROW,
                ENTITY_BOO_BUDDY,
                ENTITY_SPIKED_BEETLE,
                ENTITY_PAIRODD, ENTITY_COLOR_SHELL_RED, ENTITY_COLOR_SHELL_GREEN,
                ENTITY_COLOR_SHELL_BLUE,
                ENTITY_CRYSTAL_SWITCH,
                ENTITY_OCTOROK, ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE,
                ENTITY_MAD_BOMBER, ENTITY_BOMBER -> true;
            default -> false;
        };
    }

    static boolean supportsLinkCollision(int type) {
        return supportsEnemyCollision(type) || (type & 0xFF) == ENTITY_ARMOS_STATUE;
    }

    static int contactDamage(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_OCTOROK, ENTITY_PEAHAT, ENTITY_ZOL, ENTITY_GEL,
                ENTITY_WATER_TEKTITE, ENTITY_PAIRODD,
                ENTITY_HIDING_ZOL, ENTITY_STAR, ENTITY_BLOOPER, ENTITY_WINGED_OCTOROK,
                ENTITY_GOOMBA, ENTITY_SNAKE ->
                OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_FISH -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_CROW -> CROW_CONTACT_DAMAGE;
            case ENTITY_BOO_BUDDY -> BOO_BUDDY_CONTACT_DAMAGE;
            case ENTITY_ARMOS_STATUE -> 0x10;
            case ENTITY_ARMOS_KNIGHT -> 0x0C;
            case ENTITY_MOBLIN, ENTITY_IRON_MASK, ENTITY_MOBLIN_SWORD, ENTITY_TEKTITE, ENTITY_LEEVER,
                ENTITY_STALFOS_AGGRESSIVE, ENTITY_STALFOS_EVASIVE ->
                MOBLIN_CONTACT_DAMAGE;
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
            default -> 0;
        };
    }

    static int initialHealth(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_OCTOROK, ENTITY_PEAHAT, ENTITY_WATER_TEKTITE,
                ENTITY_STAR, ENTITY_BLOOPER, ENTITY_WINGED_OCTOROK,
                ENTITY_GOOMBA, ENTITY_SNAKE ->
                OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_FISH -> OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_CROW -> CROW_INITIAL_HEALTH;
            case ENTITY_BOO_BUDDY -> BOO_BUDDY_INITIAL_HEALTH;
            case ENTITY_ARMOS_STATUE -> 0x04;
            case ENTITY_ARMOS_KNIGHT -> 0x0C;
            case ENTITY_PAIRODD -> PAIRODD_INITIAL_HEALTH;
            case ENTITY_MOBLIN, ENTITY_IRON_MASK, ENTITY_MOBLIN_SWORD, ENTITY_TEKTITE, ENTITY_LEEVER,
                ENTITY_STALFOS_AGGRESSIVE, ENTITY_STALFOS_EVASIVE ->
                MOBLIN_INITIAL_HEALTH;
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
        return (type & 0xFF) == ENTITY_SPIKE_TRAP;
    }

    /** Adds a handler-owned dynamic sword-clink flag to the static exceptions. */
    static boolean swordPokeForSwordCollision(int type, boolean dynamicSwordClinkOff) {
        return dynamicSwordClinkOff || swordPokeForSwordCollision(type);
    }

    /** Mirrors func_003_6C6B's alternating entity-slot cadence. */
    static boolean collisionCadenceMatches(int frameCounter, int slot) {
        return (((frameCounter & 0xFF) ^ slot) & 0x01) != 0;
    }

    /** Mirrors CheckLinkCollisionWithEnemy using HitboxPositions._00. */
    static boolean overlapsLink(RoomEntity entity, int linkPixelX, int linkPixelY) {
        if (!supportsLinkCollision(entity.type())) {
            return false;
        }
        int hitboxWidth = hitboxWidth(entity.type());
        int hitboxHeight = hitboxHeight(entity.type());
        int xDistance = unsignedByteAbs(
            entity.x() + HITBOX_X - linkPixelX - 0x08);
        if (xDistance >= hitboxWidth + 0x04) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() - entity.z() + HITBOX_Y - linkPixelY - 0x08);
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
        int hitboxWidth = hitboxWidth(entity.type());
        int hitboxHeight = hitboxHeight(entity.type());
        int xDistance = unsignedByteAbs(
            entity.x() + HITBOX_X - swordX);
        if (xDistance >= hitboxWidth + swordWidth) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() - entity.z() + HITBOX_Y - swordY);
        return yDistance < hitboxHeight + swordHeight;
    }

    private static int hitboxWidth(int type) {
        return switch (type & 0xFF) {
            case ENTITY_GEL -> SMALL_ENEMY_HITBOX_WIDTH;
            case ENTITY_GIANT_GHINI, ENTITY_SPIKE_TRAP, ENTITY_ARMOS_KNIGHT ->
                BIG_ENEMY_HITBOX_WIDTH;
            default -> HITBOX_WIDTH;
        };
    }

    private static int hitboxHeight(int type) {
        return switch (type & 0xFF) {
            case ENTITY_GEL -> SMALL_ENEMY_HITBOX_HEIGHT;
            case ENTITY_GIANT_GHINI, ENTITY_SPIKE_TRAP, ENTITY_ARMOS_KNIGHT ->
                BIG_ENEMY_HITBOX_HEIGHT;
            default -> HITBOX_HEIGHT;
        };
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }
}
