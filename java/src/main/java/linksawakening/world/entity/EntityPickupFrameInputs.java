package linksawakening.world.entity;

/** Read-only inputs shared by immutable room and entity frame values. */
public interface EntityPickupFrameInputs {
    int frameCounter();

    int linkEntityX();

    int linkEntityY();

    boolean linkAirborne();

    boolean linkInteractive();

    int linkDirection();

    int linkZ();

    boolean swordActive();

    int swordX();

    int swordWidth();

    int swordY();

    int swordHeight();
}
