package linksawakening.world.entity;

/** Read-only inputs shared by immutable room and entity frame values. */
public interface EnemyCombatInputs {
    int frameCounter();

    int linkEntityX();

    int linkEntityY();

    boolean linkAirborne();

    int linkVerticalVelocity();

    boolean linkInteractive();

    default boolean goombaBounceActive() {
        return false;
    }

    boolean swordCollisionActive();

    int swordX();

    int swordWidth();

    int swordY();

    int swordHeight();

    default boolean linkIgnoresContact() {
        return false;
    }

    default boolean linkBladeClashCoolingDown() {
        return false;
    }

    /** The ROM sword-hit target latch, stored as slot plus one. */
    default int swordHitTarget() {
        return 0;
    }

    default boolean linkCanTakeContactDamage() {
        return true;
    }

    /** Attack details. Callers with no equipment details supply the standard context. */
    EnemyAttackContext attackContext();
}
