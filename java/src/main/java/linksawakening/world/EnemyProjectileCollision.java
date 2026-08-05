package linksawakening.world;

import java.util.Optional;

/**
 * Pure implementation of bank $03's CheckLinkCollisionWithProjectile.
 *
 * <p>This class intentionally owns only the shared Octorok-rock and
 * Moblin-arrow path.  Generic enemy hitboxes remain in
 * {@link RoomEntityCombatRules}; the two projectile types are excluded from
 * that pass in the ROM.</p>
 */
public final class EnemyProjectileCollision {
    static final int ENTITY_OCTOROK_ROCK = 0x0A;
    static final int ENTITY_MOBLIN_ARROW = 0x0C;
    static final int LINK_MOTION_NON_INTERACTIVE = 0x02;
    static final int COLLISION_NONE = 0x00;
    static final int COLLISION_PROJECTILE = 0xFF;
    static final int LINK_DAMAGE = 0x08;
    static final int JINGLE_SHIELD_TING = 0x16;
    static final int WAVE_LINK_HURT = 0x03;

    /* ReversedDirectionsTable at bank $03:$6BD6. */
    private static final int[] REVERSED_DIRECTIONS = {1, 0, 3, 2};

    private EnemyProjectileCollision() {
    }

    /**
     * Applies the ROM's pre-movement projectile collision check.
     *
     * @return one event when the projectile touches Link, otherwise empty
     */
    public static Optional<EntityProjectileEvent> check(
        RoomEntity projectile, int projectileDirection, LinkState link) {
        if (projectile == null || link == null) {
            throw new IllegalArgumentException("Projectile collision inputs cannot be null");
        }
        int type = projectile.type() & 0xFF;
        if (type != ENTITY_OCTOROK_ROCK && type != ENTITY_MOBLIN_ARROW) {
            return Optional.empty();
        }
        int direction = checkedDirection(projectileDirection);

        // CheckLinkCollisionWithProjectile returns before touching any entity
        // collision state when Link is non-interactive or airborne.
        if (link.motionState() >= LINK_MOTION_NON_INTERACTIVE || link.z() != 0) {
            return Optional.empty();
        }

        // hActiveEntityVisualPosY is copied as entity Y minus entity Z by
        // CopyEntityPositionToActivePosition.
        int projectileVisualY = (projectile.y() - projectile.z()) & 0xFF;
        if (!withinRomWindow(link.x(), projectile.x())
            || !withinRomWindow(link.y(), projectileVisualY)) {
            return Optional.empty();
        }

        if (link.usingShield()
            && link.direction() == REVERSED_DIRECTIONS[direction]) {
            return Optional.of(new EntityProjectileEvent(
                projectile.slot(), type, EntityProjectileEvent.Kind.SHIELD_BLOCK,
                COLLISION_PROJECTILE, 0,
                EntityProjectileEvent.SoundChannel.JINGLE, JINGLE_SHIELD_TING,
                false, false));
        }

        // func_003_6CC0 reaches ApplyLinkCollisionWithEnemy for both
        // projectile types.  Moblin arrows then jump to UnloadEntityAndReturn;
        // Octorok rocks retain the $FF collision byte and enter the shared
        // wall-transition path.
        boolean remove = type == ENTITY_MOBLIN_ARROW;
        int collisionValue = remove ? COLLISION_NONE : COLLISION_PROJECTILE;
        return Optional.of(new EntityProjectileEvent(
            projectile.slot(), type, EntityProjectileEvent.Kind.LINK_DAMAGE,
            collisionValue, LINK_DAMAGE,
            EntityProjectileEvent.SoundChannel.WAVE, WAVE_LINK_HURT,
            remove, false));
    }

    private static boolean withinRomWindow(int linkPosition, int projectilePosition) {
        // The CPU performs an unsigned 8-bit subtraction, adds $06, and
        // rejects values >= $0C.  Keep the wrap explicit; Math.abs is not an
        // equivalent at the screen's byte boundaries.
        return ((linkPosition - projectilePosition + 0x06) & 0xFF) < 0x0C;
    }

    private static int checkedDirection(int direction) {
        if (direction < 0 || direction > 3) {
            throw new IllegalArgumentException("Projectile direction out of range: " + direction);
        }
        return direction;
    }

    /** ROM-equivalent Link fields needed by CheckLinkCollisionWithProjectile. */
    public record LinkState(int x, int y, int z, int motionState,
                            int direction, boolean usingShield) {
        public LinkState {
            x &= 0xFF;
            y &= 0xFF;
            z &= 0xFF;
            motionState &= 0xFF;
            if (direction < 0 || direction > 3) {
                throw new IllegalArgumentException("Link direction out of range: " + direction);
            }
        }

        /** Existing runtime callers without Link state must not collide. */
        public static LinkState nonInteractive() {
            return new LinkState(0, 0, 0, LINK_MOTION_NON_INTERACTIVE, 0, false);
        }
    }
}
