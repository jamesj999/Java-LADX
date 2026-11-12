package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

interface RoomItemCompatibility {
    RoomSession facade();
    /** Throws the currently held object using Link's Java direction order. */
    default boolean throwLiftedEntity(int linkDirection) {
        return facade().playerItems().throwLiftedEntity(linkDirection);
    }

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
    /** Mirrors UseBoomerang's player-projectile spawn bridge. */
    default boolean fireBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return facade().playerItems()
                .fireBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }
    /** Mirrors the level-two branch of UseSword's player-projectile spawn. */
    default boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().fireSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }
    /** Mirrors the delayed UseMagicRod player-projectile spawn bridge. */
    default boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    default boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return facade().playerItems()
                .fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }
    /** Mirrors the delayed UseMagicPowder sprinkle-entity spawn bridge. */
    default boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }
    /**
     * Mirrors {@code UseShovel}: the item always enters its 24-frame pose
     * window when Link is grounded, while the adjacent-cell probe determines
     * whether the sound is a poke or a normal dig.
     *
     * <p>The coordinates are the ROM's {@code hLinkPositionX/Y} values. The
     * caller supplies ROM direction order: RIGHT, LEFT, UP, DOWN.</p>
     */
    default ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        return facade().playerItems().startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne);
    }
    /** Applies the ROM shovel timer's delayed room mutation and finish path. */
    default void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        facade().playerItems().advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
    }
    /** Resolves the ROM shovel animation table through Java's Link direction order. */
    default int shovelAnimationState(int javaDirection, int timer) {
        return facade().playerItems().shovelAnimationState(javaDirection, timer);
    }
    /** Returns whether the last successful bomb placement queued the top-view bump jingle. */
    default boolean lastBombPlacementPlayedBump() {
        return facade().playerItems().lastBombPlacementPlayedBump();
    }

    default ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return facade().playerItems().shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }
    /** ShootArrow's label_140F speed-table selector for Piece of Power. */
    default ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return facade().playerItems()
                .shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }
    /** Returns the modeled player-projectile count used by the item gates. */
    default int activeProjectileCount() {
        return facade().playerItems().activeProjectileCount();
    }
    /** Returns whether the live room currently owns an ordinary bomb entity. */
    default boolean bombActive() {
        return facade().playerItems().bombActive();
    }
    /** Returns whether the live room currently owns entity {@code $03}. */
    default boolean hookshotActive() {
        return facade().playerItems().hookshotActive();
    }
    /** Returns whether the live room currently owns the boomerang entity. */
    default boolean boomerangActive() {
        return facade().playerItems().boomerangActive();
    }
}
