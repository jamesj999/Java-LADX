package linksawakening.world;

/**
 * The player-state inputs consumed by bank-$03's sword damage lookup.
 *
 * <p>The ROM stores sword levels as {@code 0..2}; the level-3 constructor
 * value is retained for table tests and debug states that represent the
 * strongest blade directly.</p>
 */
public record EnemyAttackContext(int swordLevel,
                                 boolean redTunic,
                                 boolean pieceOfPower,
                                 boolean spinAttack,
                                 boolean pegasusBoots) {

    public EnemyAttackContext {
        if (swordLevel < 0 || swordLevel > 3) {
            throw new IllegalArgumentException("Sword level must be 0..3: " + swordLevel);
        }
    }

    public static EnemyAttackContext standard() {
        return new EnemyAttackContext(1, false, false, false, false);
    }

    /** Mirrors ApplySwordDamagesToEnemy's wAttackDamageType calculation. */
    public int effectiveDamageType() {
        if (swordLevel == 0) {
            return -1;
        }
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
