package linksawakening.world.entity;

/** Read-only inputs shared by immutable room and entity frame values. */
public interface EnemyCombatInputs {
    int frameCounter();

    int linkEntityX();

    int linkEntityY();

    boolean linkAirborne();

    int linkVerticalVelocity();

    boolean linkInteractive();

    boolean swordCollisionActive();

    int swordX();

    int swordWidth();

    int swordY();

    int swordHeight();

    EnemyAttackContext attackContext();
}
