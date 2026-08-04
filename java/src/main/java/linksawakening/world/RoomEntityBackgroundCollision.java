package linksawakening.world;

/**
 * Background collision query used by handlers that move an entity one axis at
 * a time. The proposed coordinates are the ROM entity position after the
 * fixed-point speed update and {@code direction} uses the disassembly values:
 * right {@code 0}, left {@code 1}, up {@code 2}, down {@code 3}.
 */
@FunctionalInterface
public interface RoomEntityBackgroundCollision {
    boolean blocks(RoomEntity entity, int direction, int nextX, int nextY);
}
