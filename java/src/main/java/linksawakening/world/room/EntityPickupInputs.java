package linksawakening.world.room;

import linksawakening.state.Direction;

public record EntityPickupInputs(int frameCounter, Link link, SwordBox sword)
        implements linksawakening.world.entity.EntityPickupFrameInputs {
    @Override
    public int linkEntityX() {
        return link.x();
    }

    @Override
    public int linkEntityY() {
        return link.y();
    }

    @Override
    public boolean linkAirborne() {
        return link.airborne();
    }

    @Override
    public boolean linkInteractive() {
        return link.interactive();
    }

    @Override
    public int linkDirection() {
        return link.rawDirection();
    }

    @Override
    public int linkZ() {
        return link.z();
    }

    @Override
    public boolean swordActive() {
        return sword.active();
    }

    @Override
    public int swordX() {
        return sword.x();
    }

    @Override
    public int swordWidth() {
        return sword.width();
    }

    @Override
    public int swordY() {
        return sword.y();
    }

    @Override
    public int swordHeight() {
        return sword.height();
    }

    /** Pickup probes preserve unknown facing bytes rather than normalizing them. */
    public record Link(int x, int y, boolean airborne, boolean interactive, int rawDirection, int z) {
        public Direction direction() {
            return Direction.fromJavaOrUnknown(rawDirection);
        }

        public Direction knownDirection() {
            return direction();
        }
    }
}
