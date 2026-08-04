package linksawakening.world;

/** ROM collision constants and predicates for the currently ported enemies. */
public final class RoomEntityCombatRules {
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;

    // HitboxPositions._00 in home/entities.asm:3AAA. Octorok, Moblin, Armos,
    // and Keese all select the normal collision box in hitbox_flags.asm.
    private static final int HITBOX_X = 0x08;
    private static final int HITBOX_WIDTH = 0x05;
    private static final int HITBOX_Y = 0x08;
    private static final int HITBOX_HEIGHT = 0x05;

    // HealthGroupForEntity (bank 3:41F6), InitialHealthForGroup (bank
    // 3:47BC), and EntityDamagesForGroup (bank 3:47F1).
    private static final int OCTOROK_AND_KEESE_CONTACT_DAMAGE = 0x04;
    private static final int MOBLIN_CONTACT_DAMAGE = 0x04;
    private static final int GHINI_CONTACT_DAMAGE = 0x08;
    private static final int HARDHAT_CONTACT_DAMAGE = 0x08;
    private static final int OCTOROK_AND_KEESE_INITIAL_HEALTH = 0x01;
    private static final int MOBLIN_INITIAL_HEALTH = 0x02;
    private static final int GHINI_INITIAL_HEALTH = 0x08;
    private static final int HARDHAT_INITIAL_HEALTH = 0x04;
    private static final int BASIC_SWORD_DAMAGE = 0x01;

    private RoomEntityCombatRules() {
    }

    static boolean supportsEnemyCollision(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_MOBLIN, ENTITY_TEKTITE, ENTITY_GHINI, ENTITY_HARDHAT_BEETLE,
                ENTITY_OCTOROK -> true;
            default -> false;
        };
    }

    static boolean supportsLinkCollision(int type) {
        return supportsEnemyCollision(type) || (type & 0xFF) == ENTITY_ARMOS_STATUE;
    }

    static int contactDamage(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_OCTOROK -> OCTOROK_AND_KEESE_CONTACT_DAMAGE;
            case ENTITY_MOBLIN, ENTITY_TEKTITE -> MOBLIN_CONTACT_DAMAGE;
            case ENTITY_GHINI -> GHINI_CONTACT_DAMAGE;
            case ENTITY_HARDHAT_BEETLE -> HARDHAT_CONTACT_DAMAGE;
            default -> 0;
        };
    }

    static int initialHealth(int type) {
        return switch (type & 0xFF) {
            case ENTITY_KEESE, ENTITY_OCTOROK -> OCTOROK_AND_KEESE_INITIAL_HEALTH;
            case ENTITY_MOBLIN, ENTITY_TEKTITE -> MOBLIN_INITIAL_HEALTH;
            case ENTITY_GHINI -> GHINI_INITIAL_HEALTH;
            case ENTITY_HARDHAT_BEETLE -> HARDHAT_INITIAL_HEALTH;
            default -> 0;
        };
    }

    static int basicSwordDamage(int type) {
        return supportsEnemyCollision(type) ? BASIC_SWORD_DAMAGE : 0;
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
        int xDistance = unsignedByteAbs(
            entity.x() + HITBOX_X - linkPixelX - 0x08);
        if (xDistance >= HITBOX_WIDTH + 0x04) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() + HITBOX_Y - linkPixelY - 0x08);
        return yDistance < HITBOX_HEIGHT + 0x04;
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
        int xDistance = unsignedByteAbs(
            entity.x() + HITBOX_X - swordX);
        if (xDistance >= HITBOX_WIDTH + swordWidth) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() + HITBOX_Y - swordY);
        return yDistance < HITBOX_HEIGHT + swordHeight;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }
}
