package linksawakening.world;

/** Fixed-point motion state for indoor hookshot bridge entity {@code $68}. */
public final class HookshotBridgeMotion {
    public static final int ENTITY_TYPE = 0x68;
    public static final int PULL_DOWN_DIRECTION = 0;
    public static final int PULL_UP_DIRECTION = 1;
    public static final int SPEED_DOWN = 0x30;
    public static final int SPEED_UP = 0xD0;

    private final State[] states = new State[EntityRoomLoader.MAX_ENTITIES];

    public record State(int x, int y, int direction, int speedY, int speedAccumulator) {
        public State {
            validateByte(x, "x");
            validateByte(y, "y");
            validateDirection(direction);
            validateByte(speedY, "speedY");
            validateByte(speedAccumulator, "speedAccumulator");
        }
    }

    public record Step(State state) {
        public Step {
            if (state == null) {
                throw new IllegalArgumentException("Bridge step state cannot be null");
            }
        }
    }

    /** The aligned object cell rewritten by the bridge handler. */
    public record ObjectCell(int objectLeft, int objectTop) {
        public ObjectCell {
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
        }
    }

    public static State spawn(int bridgeX, int bridgeY, int direction) {
        return new State(bridgeX, bridgeY, direction, speedForDirection(direction), 0);
    }

    public static int speedForDirection(int direction) {
        validateDirection(direction);
        return direction == PULL_DOWN_DIRECTION ? SPEED_DOWN : SPEED_UP;
    }

    public static Step advance(State state) {
        if (state == null) {
            throw new IllegalArgumentException("Bridge state cannot be null");
        }
        SpeedStep y = addSpeed(state.y(), state.speedY(), state.speedAccumulator());
        return new Step(new State(state.x(), y.position(), state.direction(), state.speedY(),
            y.accumulator()));
    }

    /**
     * Mirrors the bridge handler's use of the pre-move active visual position.
     * A bridge spawned at {@code objectTop + $10} therefore targets the same
     * cell on its first handler frame.
     */
    public static ObjectCell objectCell(State state) {
        if (state == null) {
            throw new IllegalArgumentException("Bridge state cannot be null");
        }
        return new ObjectCell(state.x() - 0x08,
            state.y() + 0x04 - 0x10);
    }

    public void initializeSpawn(int slot, int bridgeX, int bridgeY, int direction) {
        validateSlot(slot);
        states[slot] = spawn(bridgeX, bridgeY, direction);
    }

    public State state(int slot) {
        validateSlot(slot);
        return states[slot];
    }

    public boolean active(int slot) {
        validateSlot(slot);
        return states[slot] != null;
    }

    public Step advance(int slot) {
        validateSlot(slot);
        State state = states[slot];
        if (state == null) {
            throw new IllegalStateException("No active hookshot bridge in slot " + slot);
        }
        Step step = advance(state);
        states[slot] = step.state();
        return step;
    }

    public void clear(int slot) {
        validateSlot(slot);
        states[slot] = null;
    }

    private static SpeedStep addSpeed(int position, int speed, int accumulator) {
        int unsignedSpeed = speed & 0xFF;
        int fractionalSum = (accumulator & 0xFF) + ((unsignedSpeed << 4) & 0xF0);
        int delta = signedByte(unsignedSpeed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return new SpeedStep((position + delta) & 0xFF, fractionalSum & 0xFF);
    }

    private static int signedByte(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static void validateDirection(int direction) {
        if (direction < PULL_DOWN_DIRECTION || direction > PULL_UP_DIRECTION) {
            throw new IllegalArgumentException("Bridge direction must be 0 or 1: " + direction);
        }
    }

    private static void validateByte(int value, String name) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private record SpeedStep(int position, int accumulator) {
    }
}
