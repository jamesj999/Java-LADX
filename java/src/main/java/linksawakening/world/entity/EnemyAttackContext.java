package linksawakening.world.entity;

/**
 * Player-state inputs for bank-$03's sword and shield collision paths.
 *
 * <p>The ROM stores sword levels as {@code 0..2}; the level-3 constructor
 * value is retained for table tests and debug states that represent the
 * strongest blade directly.</p>
 */
public record EnemyAttackContext(
        int swordLevel,
        boolean redTunic,
        boolean pieceOfPower,
        boolean spinAttack,
        boolean pegasusBoots,
        boolean usingShield,
        int romDirection,
        int linkZ) {
    // $20:4B3A-$4B49, in ROM right/left/up/down order.
    private static final int[] SHIELD_X = {16, 0, 8, 8};
    private static final int[] SHIELD_WIDTH = {3, 3, 8, 8};
    private static final int[] SHIELD_Y = {8, 8, 0, 13};
    private static final int[] SHIELD_HEIGHT = {8, 8, 3, 4};

    int shieldX(int linkX) {
        return (linkX + SHIELD_X[romDirection]) & 0xFF;
    }

    int shieldY(int linkY) {
        return (linkY - linkZ + SHIELD_Y[romDirection]) & 0xFF;
    }

    int shieldWidth() {
        return SHIELD_WIDTH[romDirection];
    }

    int shieldHeight() {
        return SHIELD_HEIGHT[romDirection];
    }

    public EnemyAttackContext(
            int swordLevel, boolean redTunic, boolean pieceOfPower, boolean spinAttack, boolean pegasusBoots) {
        this(swordLevel, redTunic, pieceOfPower, spinAttack, pegasusBoots, false, 0, 0);
    }

    public EnemyAttackContext {
        if (romDirection < 0 || romDirection > 3 || linkZ < 0 || linkZ > 255) {
            throw new IllegalArgumentException("Invalid shield direction or Link height");
        }
        if (swordLevel < 0 || swordLevel > 3) {
            throw new IllegalArgumentException("Sword level must be 0..3: " + swordLevel);
        }
    }

    public static EnemyAttackContext standard() {
        return new EnemyAttackContext(1, false, false, false, false);
    }

    /** Mirrors ApplySwordDamagesToEnemy's wAttackDamageType calculation. */
    public int effectiveDamageType() {
        // ApplySwordDamagesToEnemy increments for spin before subtracting one.
        // This makes the swordless acquisition spin a normal level-one hit.
        int effectiveLevel = swordLevel;
        if (redTunic || pieceOfPower || spinAttack || pegasusBoots) {
            effectiveLevel = Math.min(3, effectiveLevel + 1);
        }
        return effectiveLevel - 1;
    }

    /** Red tunic and Piece of Power select the ROM power-recoil branch. */
    public boolean powerRecoil() {
        return redTunic || pieceOfPower;
    }
}
