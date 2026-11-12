package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

public interface RoomItemCompatibility {
    RoomSession facade();
    /** @deprecated Use {@code playerItems().throwLiftedEntity()}. */
    @Deprecated(forRemoval = true)
    default boolean throwLiftedEntity(int linkDirection) {
        return facade().playerItems().throwLiftedEntity(linkDirection);
    }

    /** @deprecated Use {@code playerItems().fireHookshot()}. */
    @Deprecated(forRemoval = true)
    default boolean fireHookshot(
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean linkAirborne,
            boolean linkPushing) {
        return facade().playerItems()
                .fireHookshot(linkEntityX, linkEntityY, linkEntityZ, romDirection, linkAirborne, linkPushing);
    }
    /** @deprecated Use {@code playerItems().fireBoomerang()}. */
    @Deprecated(forRemoval = true)
    default boolean fireBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return facade().playerItems()
                .fireBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }
    /** @deprecated Use {@code playerItems().fireSwordBeam()}. */
    @Deprecated(forRemoval = true)
    default boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().fireSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }
    /** @deprecated Use {@code playerItems().fireMagicRodFireball()}. */
    @Deprecated(forRemoval = true)
    default boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** @deprecated Use {@code playerItems().fireMagicRodFireball()}. */
    @Deprecated(forRemoval = true)
    default boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return facade().playerItems()
                .fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }
    /** @deprecated Use {@code playerItems().sprinkleMagicPowder()}. */
    @Deprecated(forRemoval = true)
    default boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }
    /** @deprecated Use {@code playerItems().startShovel()}. */
    @Deprecated(forRemoval = true)
    default ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        return facade().playerItems().startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne);
    }
    /** @deprecated Use {@code playerItems().advanceShovel()}. */
    @Deprecated(forRemoval = true)
    default void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        facade().playerItems().advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
    }
    /** @deprecated Use {@code playerItems().shovelAnimationState()}. */
    @Deprecated(forRemoval = true)
    default int shovelAnimationState(int javaDirection, int timer) {
        return facade().playerItems().shovelAnimationState(javaDirection, timer);
    }
    /** @deprecated Use {@code playerItems().lastBombPlacementPlayedBump()}. */
    @Deprecated(forRemoval = true)
    default boolean lastBombPlacementPlayedBump() {
        return facade().playerItems().lastBombPlacementPlayedBump();
    }

    /** @deprecated Use {@code playerItems().shootArrowResult()}. */
    @Deprecated(forRemoval = true)
    default ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }
    /** @deprecated Use {@code playerItems().shootArrowResult()}. */
    @Deprecated(forRemoval = true)
    default ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return facade().playerItems()
                .shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }
    /** @deprecated Use {@code playerItems().activeProjectileCount()}. */
    @Deprecated(forRemoval = true)
    default int activeProjectileCount() {
        return facade().playerItems().activeProjectileCount();
    }
    /** @deprecated Use {@code playerItems().bombActive()}. */
    @Deprecated(forRemoval = true)
    default boolean bombActive() {
        return facade().playerItems().bombActive();
    }
    /** @deprecated Use {@code playerItems().hookshotActive()}. */
    @Deprecated(forRemoval = true)
    default boolean hookshotActive() {
        return facade().playerItems().hookshotActive();
    }
    /** @deprecated Use {@code playerItems().boomerangActive()}. */
    @Deprecated(forRemoval = true)
    default boolean boomerangActive() {
        return facade().playerItems().boomerangActive();
    }
}
