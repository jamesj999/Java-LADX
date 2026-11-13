package linksawakening.world.room;

import linksawakening.state.Direction;

public record EntityPickupInputs(int frameCounter, Link link, SwordBox sword)
        implements linksawakening.world.entity.EntityPickupFrameInputs {
    public int linkEntityX() {
        return link.x();
    }

    public int linkEntityY() {
        return link.y();
    }

    public boolean linkAirborne() {
        return link.airborne();
    }

    public boolean linkInteractive() {
        return link.interactive();
    }

    public int linkDirection() {
        return link.rawDirection();
    }

    public int linkZ() {
        return link.z();
    }

    public boolean swordActive() {
        return sword.active();
    }

    public int swordX() {
        return sword.x();
    }

    public int swordWidth() {
        return sword.width();
    }

    public int swordY() {
        return sword.y();
    }

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
        /** Compatibility: the raw direction has always been authoritative. */
        @Deprecated(forRemoval = true)
        public Link(int x, int y, boolean airborne, boolean interactive, Direction direction, int z, int rawDirection) {
            this(x, y, airborne, interactive, rawDirection, z);
        }
        /** Compatibility: use the Java direction index directly. */
        @Deprecated(forRemoval = true)
        public Link(int x, int y, boolean airborne, boolean interactive, Direction direction, int z) {
            this(x, y, airborne, interactive, direction.javaIndex(), z);
        }
    }
}
