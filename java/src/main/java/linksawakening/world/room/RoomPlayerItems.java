package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public interface RoomPlayerItems {
    boolean throwLiftedEntity(linksawakening.state.Direction linkDirection);
    /** @deprecated Pass Direction; retained for app gameplay/item consumers. */
    @Deprecated(forRemoval = true)
    default boolean throwLiftedEntity(int linkDirection) {
        return throwLiftedEntity(linksawakening.state.Direction.fromJava(linkDirection));
    }

    boolean fireHookshot(
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean linkAirborne,
            boolean linkPushing);

    boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask);

    boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection);

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection);

    boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower);

    boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection);

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne);

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer);

    int shovelAnimationState(linksawakening.state.Direction javaDirection, int timer);
    /** @deprecated Pass Direction; retained for app gameplay/item consumers. */
    @Deprecated(forRemoval = true)
    default int shovelAnimationState(int javaDirection, int timer) {
        return shovelAnimationState(linksawakening.state.Direction.fromJava(javaDirection), timer);
    }

    boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection);

    boolean lastBombPlacementPlayedBump();

    boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection);

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection);

    ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower);

    int activeProjectileCount();

    boolean bombActive();

    boolean hookshotActive();

    boolean boomerangActive();
}
