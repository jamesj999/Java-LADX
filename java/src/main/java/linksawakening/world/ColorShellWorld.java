package linksawakening.world;

/**
 * Room-side effects used by the bank-$36 Color Shell puzzle handler. The
 * relative object offset is measured from the object pointer computed by
 * {@code func_036_6BCF}; {@code -1} is the byte compared by State 8.
 */
public interface ColorShellWorld {
    int objectAt(RoomEntity entity, int relativeOffset);

    void writeObject(RoomEntity entity, int objectId);

    void playJingle(int id);

    void playNoise(int id);

    void spawnPoof(int x, int y);

    static ColorShellWorld none() {
        return NoneHolder.INSTANCE;
    }

    final class NoneHolder {
        private static final ColorShellWorld INSTANCE = new ColorShellWorld() {
            @Override
            public int objectAt(RoomEntity entity, int relativeOffset) {
                return 0xFF;
            }

            @Override
            public void writeObject(RoomEntity entity, int objectId) {
            }

            @Override
            public void playJingle(int id) {
            }

            @Override
            public void playNoise(int id) {
            }

            @Override
            public void spawnPoof(int x, int y) {
            }
        };

        private NoneHolder() {
        }
    }
}
