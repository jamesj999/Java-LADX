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
    static final int ENTITY_LASER_BEAM = 0x2B;
    static final int LINK_MOTION_NON_INTERACTIVE = 0x02;
    static final int COLLISION_NONE = 0x00;
    static final int COLLISION_PROJECTILE = 0xFF;
    static final int LINK_DAMAGE = 0x08;
    static final int JINGLE_SHIELD_TING = 0x16;
    static final int JINGLE_SWORD_POKE = 0x07;
    static final int WAVE_LINK_HURT = 0x03;

    /* ReversedDirectionsTable at bank $03:$6BD6. */
    private static final int[] REVERSED_DIRECTIONS = {1, 0, 3, 2};
    private static final int[] LASER_LINK_SPEED_X = {0xF8, 0x08, 0x00, 0x00};
    private static final int[] LASER_LINK_SPEED_Y = {0x00, 0x00, 0x08, 0xF8};

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
        if (type != ENTITY_OCTOROK_ROCK && type != ENTITY_MOBLIN_ARROW
            && type != ENTITY_LASER_BEAM) {
            return Optional.empty();
        }
        int direction = type == ENTITY_LASER_BEAM
            ? checkedLaserDirection(projectileDirection)
            : checkedDirection(projectileDirection);

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

        if (type == ENTITY_LASER_BEAM) {
            return checkLaserBeam(projectile, direction, link);
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

    private static Optional<EntityProjectileEvent> checkLaserBeam(
        RoomEntity projectile, int projectileDirection, LinkState link) {
        if (link.usingShield() && link.shieldLevel() >= 2
            && reflectsFromMirrorShield(projectileDirection, link.direction())) {
            return Optional.of(new EntityProjectileEvent(
                projectile.slot(), ENTITY_LASER_BEAM, EntityProjectileEvent.Kind.SHIELD_BLOCK,
                0x02, 0, EntityProjectileEvent.SoundChannel.JINGLE, JINGLE_SWORD_POKE,
                false, true, projectile.x(), projectile.y(),
                LASER_LINK_SPEED_X[link.direction()], LASER_LINK_SPEED_Y[link.direction()],
                0x10));
        }

        // A beam that is not reflected is unloaded by the bank-$15 handler;
        // its source damage group supplies eight points of Link damage.
        return Optional.of(new EntityProjectileEvent(
            projectile.slot(), ENTITY_LASER_BEAM, EntityProjectileEvent.Kind.LINK_DAMAGE,
            COLLISION_NONE, LINK_DAMAGE, EntityProjectileEvent.SoundChannel.WAVE, WAVE_LINK_HURT,
            true, false));
    }

    /* Data_003_6BDA: expected beam direction for Link right/left/up/down. */
    private static final int[] MIRROR_SHIELD_DIRECTIONS = {0x02, 0x0A, 0x0E, 0x06};

    private static boolean reflectsFromMirrorShield(int beamDirection, int linkDirection) {
        int expected = MIRROR_SHIELD_DIRECTIONS[linkDirection];
        return ((beamDirection - expected) & 0x0F) < 0x05;
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

    private static int checkedLaserDirection(int direction) {
        if (direction < 0 || direction > 0x0F) {
            throw new IllegalArgumentException("Laser direction out of range: " + direction);
        }
        return direction;
    }

    /** ROM-equivalent Link fields needed by CheckLinkCollisionWithProjectile. */
    public record LinkState(int x, int y, int z, int motionState,
                            int direction, boolean usingShield,
                            int shieldLevel, int invincibilityCounter) {
        public LinkState(int x, int y, int z, int motionState,
                         int direction, boolean usingShield) {
            this(x, y, z, motionState, direction, usingShield, 0, 0);
        }

        public LinkState {
            x &= 0xFF;
            y &= 0xFF;
            z &= 0xFF;
            motionState &= 0xFF;
            if (direction < 0 || direction > 3) {
                throw new IllegalArgumentException("Link direction out of range: " + direction);
            }
            if (shieldLevel < 0 || shieldLevel > 0xFF) {
                throw new IllegalArgumentException("Shield level out of range: " + shieldLevel);
            }
            if (invincibilityCounter < 0 || invincibilityCounter > 0xFF) {
                throw new IllegalArgumentException("Invincibility counter out of range: "
                    + invincibilityCounter);
            }
        }

        /** Existing runtime callers without Link state must not collide. */
        public static LinkState nonInteractive() {
            return new LinkState(0, 0, 0, LINK_MOTION_NON_INTERACTIVE, 0, false, 0, 0);
        }
    }
}
