package linksawakening.world;

/** ROM collision constants and predicates for the currently ported enemies. */
public final class RoomEntityCombatRules {
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_KEESE = 0x19;

    // HitboxPositions._00 in home/entities.asm:3AAA.
    private static final int KEESE_HITBOX_X = 0x08;
    private static final int KEESE_HITBOX_WIDTH = 0x05;
    private static final int KEESE_HITBOX_Y = 0x08;
    private static final int KEESE_HITBOX_HEIGHT = 0x05;
    private static final int KEESE_CONTACT_DAMAGE = 0x04;
    private static final int KEESE_INITIAL_HEALTH = 0x01;

    private RoomEntityCombatRules() {
    }

    static boolean supportsEnemyCollision(int type) {
        return (type & 0xFF) == ENTITY_KEESE || (type & 0xFF) == ENTITY_OCTOROK;
    }

    static int contactDamage(int type) {
        return supportsEnemyCollision(type) ? KEESE_CONTACT_DAMAGE : 0;
    }

    static boolean canBeKilledByBasicSword(int type) {
        return supportsEnemyCollision(type) && KEESE_INITIAL_HEALTH <= 1;
    }

    /** Mirrors func_003_6C6B's alternating entity-slot cadence. */
    static boolean collisionCadenceMatches(int frameCounter, int slot) {
        return (((frameCounter & 0xFF) ^ slot) & 0x01) != 0;
    }

    /** Mirrors CheckLinkCollisionWithEnemy using HitboxPositions._00. */
    static boolean overlapsLink(RoomEntity entity, int linkPixelX, int linkPixelY) {
        if (!supportsEnemyCollision(entity.type())) {
            return false;
        }
        int xDistance = unsignedByteAbs(
            entity.x() + KEESE_HITBOX_X - linkPixelX - 0x08);
        if (xDistance >= KEESE_HITBOX_WIDTH + 0x04) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() + KEESE_HITBOX_Y - linkPixelY - 0x08);
        return yDistance < KEESE_HITBOX_HEIGHT + 0x04;
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
            entity.x() + KEESE_HITBOX_X - swordX);
        if (xDistance >= KEESE_HITBOX_WIDTH + swordWidth) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() + KEESE_HITBOX_Y - swordY);
        return yDistance < KEESE_HITBOX_HEIGHT + swordHeight;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }
}
