package linksawakening.world.room;

import linksawakening.world.room.RoomSession.*;

final class RoomPlayerItemsView implements RoomSession.PlayerItems {
    private final RoomPlayerItemActions delegate;

    RoomPlayerItemsView(RoomPlayerItemActions delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean throwLiftedEntity(int linkDirection) {
        return delegate.throwLiftedEntity(linkDirection);
    }

    @Override
    public boolean fireHookshot(
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean linkAirborne,
            boolean linkPushing) {
        return delegate.fireHookshot(linkEntityX, linkEntityY, linkEntityZ, romDirection, linkAirborne, linkPushing);
    }

    @Override
    public boolean fireBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return delegate.fireBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    @Override
    public boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return delegate.fireSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    @Override
    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return delegate.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    @Override
    public boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return delegate.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    @Override
    public boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return delegate.sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    @Override
    public ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        return delegate.startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne);
    }

    @Override
    public void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        delegate.advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
    }

    @Override
    public int shovelAnimationState(int javaDirection, int timer) {
        return delegate.shovelAnimationState(javaDirection, timer);
    }

    @Override
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return delegate.placeBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    @Override
    public boolean lastBombPlacementPlayedBump() {
        return delegate.lastBombPlacementPlayedBump();
    }

    @Override
    public boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return delegate.shootArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    @Override
    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return delegate.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    @Override
    public ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return delegate.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    @Override
    public int activeProjectileCount() {
        return delegate.activeProjectileCount();
    }

    @Override
    public boolean bombActive() {
        return delegate.bombActive();
    }

    @Override
    public boolean hookshotActive() {
        return delegate.hookshotActive();
    }

    @Override
    public boolean boomerangActive() {
        return delegate.boomerangActive();
    }
}
