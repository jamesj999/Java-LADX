package linksawakening.world;

import java.util.List;
import java.util.function.IntSupplier;

/** Bank-$06 {@code ThreeOfAKindEntityHandler}. */
final class ThreeOfAKindMotion {
    static final int ENTITY_TYPE = 0x90;
    private static final int[] SPEED_X = {0x0C, 0xF4, 0, 0};
    private static final int[] SPEED_Y = {0, 0, 0xF4, 0x0C};

    enum PuzzleResult { NONE, MISMATCH, MATCH }

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] accumulatorX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] accumulatorY = new int[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown) {}

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   int ignoreHitsCountdown, IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision collision) {
        int slot = entity.slot();
        int x = move(entity, true, collision);
        int y = move(new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, entity.y(), entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z()), false, collision);

        switch (state[slot]) {
            case 0 -> {
                if (transitionCountdown == 0) {
                    transitionCountdown = 0x20;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    state[slot] = 1;
                }
                inertia[slot] = (inertia[slot] + 1) & 0xFF;
            }
            case 1 -> {
                if (transitionCountdown == 0) {
                    transitionCountdown = 0x20 + (randomByteSupplier.getAsInt() & 0x1F);
                    state[slot] = 0;
                    int nextDirection = randomByteSupplier.getAsInt() & 0x03;
                    speedX[slot] = SPEED_X[nextDirection];
                    speedY[slot] = SPEED_Y[nextDirection];
                }
            }
            case 2 -> { }
            default -> throw new IllegalStateException("Unknown Three-of-a-Kind state: "
                + state[slot]);
        }

        if ((frameCounter & 0x0F) == 0 && state[slot] != 2) {
            direction[slot] = (direction[slot] + 1) & 0x03;
        }
        int variant = direction[slot] * 2 + ((inertia[slot] >>> 3) & 1);
        if (state[slot] != 2 && ignoreHitsCountdown != 0) {
            state[slot] = 2;
            transitionCountdown = 0x40;
            speedX[slot] = 0;
            speedY[slot] = 0;
        }
        return new Update(withPositionAndVariant(entity, x, y, variant), transitionCountdown);
    }

    PuzzleResult resolvePuzzle(List<RoomEntity> entities, int[] transitionCountdowns) {
        List<RoomEntity> cards = entities.stream()
            .filter(entity -> entity.status() == EntityStatus.ACTIVE
                && entity.type() == ENTITY_TYPE)
            .toList();
        if (cards.size() != 3 || cards.stream().anyMatch(entity ->
            state[entity.slot()] != 2 || transitionCountdowns[entity.slot()] != 0)) {
            return PuzzleResult.NONE;
        }
        int face = direction[cards.getFirst().slot()];
        boolean match = cards.stream().allMatch(entity -> direction[entity.slot()] == face);
        if (!match) {
            cards.forEach(entity -> state[entity.slot()] = 0);
            return PuzzleResult.MISMATCH;
        }
        return PuzzleResult.MATCH;
    }

    int matchedDrop(int slot) {
        return switch (direction[slot]) {
            case 0 -> 0x2D;
            case 1 -> 0x2E;
            default -> 0xFF;
        };
    }

    /** The bundled ROM is the unpatched revision: faces 2/3 retain wrong-answer. */
    int matchedJingle(int slot) {
        return direction[slot] < 2 ? 0x02 : 0x1D;
    }

    int state(int slot) { return state[slot]; }
    int direction(int slot) { return direction[slot]; }
    int speedX(int slot) { return speedX[slot]; }
    int speedY(int slot) { return speedY[slot]; }

    void setSettledForTest(int slot, int face) {
        state[slot] = 2;
        direction[slot] = face & 3;
    }

    void clear(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        accumulatorX[slot] = 0;
        accumulatorY[slot] = 0;
    }

    private int move(RoomEntity entity, boolean horizontal,
                     RoomEntityBackgroundCollision collision) {
        int slot = entity.slot();
        int speed = horizontal ? speedX[slot] : speedY[slot];
        int[] accumulator = horizontal ? accumulatorX : accumulatorY;
        int position = horizontal ? entity.x() : entity.y();
        int next = addSpeed(position, speed, accumulator, slot);
        int nextX = horizontal ? next : entity.x();
        int nextY = horizontal ? entity.y() : next;
        if (next != position && collision != null && collision.blocks(entity,
            horizontal ? (signed(speed) < 0 ? 1 : 0) : (signed(speed) < 0 ? 2 : 3),
            nextX, nextY)) {
            return position;
        }
        return next;
    }

    private static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        int fractional = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractional & 0xFF;
        int delta = signed(speed) >> 4;
        if (fractional > 0xFF) delta++;
        return (position + delta) & 0xFF;
    }

    private static int signed(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                      int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }
}
