package linksawakening.world.room;

import linksawakening.state.Direction;

public record EntityPickupInputs(int frameCounter, Link link, SwordBox sword) {
    public record Link(
            int x, int y, boolean airborne, boolean interactive, Direction direction, int z, int rawDirection) {
        public Link {
            direction = Direction.fromJavaOrUnknown(rawDirection);
        }

        public Link(int x, int y, boolean airborne, boolean interactive, Direction direction, int z) {
            this(x, y, airborne, interactive, direction, z, direction.javaIndex());
        }
        /** @deprecated Pass Direction; retained for app/gameplay/LinkEquipmentPhase.java. */
        @Deprecated(forRemoval = true)
        public Link(int x, int y, boolean airborne, boolean interactive, int direction, int z) {
            this(x, y, airborne, interactive, Direction.fromJavaOrUnknown(direction), z, direction);
        }
    }
}
