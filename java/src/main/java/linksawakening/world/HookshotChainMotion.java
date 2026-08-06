package linksawakening.world;

/**
 * ROM-facing state for entity {@code $03}, the hookshot chain.
 *
 * <p>The source stores position speed as unsigned bytes whose signed high
 * nibble is the whole-pixel delta and whose low nibble is a 1/16-pixel
 * accumulator. This class keeps that WRAM-like state outside the immutable
 * {@link RoomEntity} snapshot.</p>
 */
public final class HookshotChainMotion {
    public static final int ENTITY_TYPE = 0x03;
    public static final int HOOKSHOT_SPEED = 0x30;
    public static final int INITIAL_TRANSITION_COUNTDOWN = 0x2A;

    private static final int[] SPEED_X = {0x30, 0xD0, 0x00, 0x00};
    private static final int[] SPEED_Y = {0x00, 0x00, 0xD0, 0x30};

    // HitboxPositions._00 in home/entities.asm. Entity $03 selects the
    // normal collision box because its hitbox flags are $03 and the table is
    // indexed by (flags & $7C).
    private static final int HITBOX_OFFSET_X = 0x08;
    private static final int HITBOX_OFFSET_Y = 0x08;
    private static final int HITBOX_WIDTH = 0x05;
    private static final int HITBOX_HEIGHT = 0x05;

    private final State[] states = new State[EntityRoomLoader.MAX_ENTITIES];

    public record State(int x, int y, int z, int direction, int speedX, int speedY,
                        int transitionCountdown, int speedXAccumulator,
                        int speedYAccumulator) {
        public State {
            validateByte(x, "x");
            validateByte(y, "y");
            validateByte(z, "z");
            validateDirection(direction);
            validateByte(speedX, "speedX");
            validateByte(speedY, "speedY");
            validateByte(transitionCountdown, "transitionCountdown");
            validateByte(speedXAccumulator, "speedXAccumulator");
            validateByte(speedYAccumulator, "speedYAccumulator");
        }
    }

    public record Step(State state, boolean collided, boolean reachedLink, boolean returning) {
        public Step {
            if (state == null) {
                throw new IllegalArgumentException("Hookshot step state cannot be null");
            }
        }
    }

    public static State spawn(int linkX, int linkY, int linkZ, int direction) {
        validateByte(linkX, "linkX");
        validateByte(linkY, "linkY");
        validateByte(linkZ, "linkZ");
        validateDirection(direction);
        return new State(linkX, linkY, (linkZ + 1) & 0xFF, direction,
            speedXForDirection(direction), speedYForDirection(direction),
            INITIAL_TRANSITION_COUNTDOWN, 0, 0);
    }

    public static int speedXForDirection(int direction) {
        validateDirection(direction);
        return SPEED_X[direction];
    }

    public static int speedYForDirection(int direction) {
        validateDirection(direction);
        return SPEED_Y[direction];
    }

    /** Returns the next position before the shared entity wall probe runs. */
    public static Position nextPosition(State state) {
        if (state == null) {
            throw new IllegalArgumentException("Hookshot state cannot be null");
        }
        SpeedStep x = addSpeed(state.x(), state.speedX(), state.speedXAccumulator());
        SpeedStep y = addSpeed(state.y(), state.speedY(), state.speedYAccumulator());
        return new Position(x.position(), y.position());
    }

    /**
     * Advances one handler frame. A blocked proposed position is left in
     * place; the caller owns the source-specific wall response.
     *
     * <p>When the initial transition countdown reaches zero, the ROM applies
     * a vector toward Link for the following frame. This keeps the chain from
     * remaining indefinitely active in an open room while leaving the
     * hookshotable-object pull state for the shared collision increment.</p>
     */
    public static Step advance(State state, int linkX, int linkY, boolean blocked) {
        if (state == null) {
            throw new IllegalArgumentException("Hookshot state cannot be null");
        }
        validateByte(linkX, "linkX");
        validateByte(linkY, "linkY");
        if (blocked) {
            return new Step(state, true, false, state.transitionCountdown() == 0);
        }

        SpeedStep nextX = addSpeed(state.x(), state.speedX(), state.speedXAccumulator());
        SpeedStep nextY = addSpeed(state.y(), state.speedY(), state.speedYAccumulator());
        int nextCountdown = state.transitionCountdown() == 0
            ? 0 : state.transitionCountdown() - 1;
        boolean returning = nextCountdown == 0;
        int nextSpeedX = state.speedX();
        int nextSpeedY = state.speedY();
        if (returning) {
            Vector vector = vectorTowardsLink(nextX.position(), nextY.position(), state.z(),
                linkX, linkY, HOOKSHOT_SPEED);
            nextSpeedX = vector.x();
            nextSpeedY = vector.y();
        }
        State next = new State(nextX.position(), nextY.position(), state.z(), state.direction(),
            nextSpeedX, nextSpeedY, nextCountdown, nextX.accumulator(), nextY.accumulator());
        // The chain is spawned on Link's collision box. The ROM only treats
        // that overlap as the unload condition after the transition countdown
        // has switched the entity to its return path; otherwise the outbound
        // launch would immediately collide with its owner.
        return new Step(next, false, returning && overlapsLink(next, linkX, linkY), returning);
    }

    /**
     * Mirrors {@code CheckLinkCollisionWithEnemy} for entity {@code $03}.
     * The entity's visual Y is its stored Y minus Z; comparing the raw stored
     * Y to Link would miss the ROM's spawn-at-Link-Z-plus-one case.
     */
    public static boolean overlapsLink(State state, int linkX, int linkY) {
        if (state == null) {
            throw new IllegalArgumentException("Hookshot state cannot be null");
        }
        validateByte(linkX, "linkX");
        validateByte(linkY, "linkY");

        int xDistance = unsignedByteAbs(
            state.x() + HITBOX_OFFSET_X - linkX - 0x08);
        if (xDistance >= HITBOX_WIDTH + 0x04) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            state.y() - state.z() + HITBOX_OFFSET_Y - linkY - 0x08);
        return yDistance < HITBOX_HEIGHT + 0x04;
    }

    public void initializeSpawn(int slot, int linkX, int linkY, int linkZ, int direction) {
        validateSlot(slot);
        states[slot] = spawn(linkX, linkY, linkZ, direction);
    }

    public boolean active(int slot) {
        validateSlot(slot);
        return states[slot] != null;
    }

    public State state(int slot) {
        validateSlot(slot);
        return states[slot];
    }

    public Step advance(int slot, int linkX, int linkY, boolean blocked) {
        validateSlot(slot);
        State state = requireState(slot);
        Step step = advance(state, linkX, linkY, blocked);
        states[slot] = step.state();
        return step;
    }

    public int transitionCountdown(int slot) {
        State state = requireState(slot);
        return state.transitionCountdown();
    }

    public void clear(int slot) {
        validateSlot(slot);
        states[slot] = null;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        if (absoluteX == 0 && absoluteY == 0) {
            return new Vector(0, 0);
        }
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smaller, larger);
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0 || largerDistance == 0) {
            return largerDistance == 0 ? length : 0;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    private static SpeedStep addSpeed(int position, int speed, int accumulator) {
        int unsignedSpeed = speed & 0xFF;
        if (unsignedSpeed == 0) {
            return new SpeedStep(position & 0xFF, accumulator & 0xFF);
        }
        int fractionalSum = (accumulator & 0xFF) + ((unsignedSpeed << 4) & 0xF0);
        int delta = signedByte(unsignedSpeed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return new SpeedStep((position + delta) & 0xFF, fractionalSum & 0xFF);
    }

    private State requireState(int slot) {
        State state = states[slot];
        if (state == null) {
            throw new IllegalStateException("No active hookshot chain in slot " + slot);
        }
        return state;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static void validateDirection(int direction) {
        if (direction < 0 || direction > 3) {
            throw new IllegalArgumentException("Hookshot direction must be 0..3: " + direction);
        }
    }

    private static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    public record Position(int x, int y) {
    }

    private record SpeedStep(int position, int accumulator) {
    }

    private record Vector(int x, int y) {
    }
}
