package linksawakening.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

/** Bank-$04 active movement and 128-entry tail history for Moldorm ($59). */
public final class MoldormMotion {
    static final int ENTITY_TYPE = 0x59;
    static final int INITIAL_PHYSICS_FLAGS = 0x08;
    static final int INITIAL_OPTIONS1 = 0xD0;
    static final int INITIAL_HITBOX_FLAGS = 0x80;

    private static final int HISTORY_LENGTH = 0x80;
    private static final int HISTORY_MASK = HISTORY_LENGTH - 1;
    private static final int[] HEAD_SPRITE_VARIANTS = {
        0x06, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05
    };
    private static final int[] TAIL_HISTORY_OFFSETS = {0x0C, 0x18, 0x24, 0x2E};
    private static final int[] Y_SPEEDS = {
        0x00, 0x06, 0x0C, 0x0E, 0x10, 0x0E, 0x0C, 0x06,
        0x00, 0xFA, 0xF4, 0xF2, 0xF0, 0xF2, 0xF4, 0xFA
    };
    private static final int[] X_SPEEDS = {
        0x10, 0x0E, 0x0C, 0x06, 0x00, 0xFA, 0xF4, 0xF2,
        0xF0, 0xF2, 0xF4, 0xFA, 0x00, 0x06, 0x0C, 0x0E
    };

    private final int[][] historyX =
        new int[EntityRoomLoader.MAX_ENTITIES][HISTORY_LENGTH];
    private final int[][] historyY =
        new int[EntityRoomLoader.MAX_ENTITIES][HISTORY_LENGTH];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] headSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] noiseCadence = new int[EntityRoomLoader.MAX_ENTITIES];
    private final TailPosition[] vulnerableTail =
        new TailPosition[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record TailPosition(int x, int y) {
        public TailPosition {
            x &= 0xFF;
            y &= 0xFF;
        }
    }

    record Update(RoomEntity entity, int transitionCountdown, int headSpriteVariant,
                  int speedX, int speedY, int collisionFlags, int inertia,
                  boolean noiseRequested, List<TailPosition> segments,
                  Presentation presentation) {
        Update {
            segments = List.copyOf(segments);
        }
    }

    record Presentation(int headSpriteVariant, int headX, int headY,
                        List<TailPosition> segments) {
        Presentation {
            segments = List.copyOf(segments);
        }
    }

    void initialize(RoomEntity entity) {
        int slot = entity.slot();
        Arrays.fill(historyX[slot], 0);
        Arrays.fill(historyY[slot], 0);
        inertia[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        headSpriteVariant[slot] = entity.spriteVariant() >= 0
            && entity.spriteVariant() < 8 ? entity.spriteVariant() : 0;
        noiseCadence[slot] = 0;
        vulnerableTail[slot] = new TailPosition(0, 0);
        initialized[slot] = true;
    }

    void clear(int slot) {
        if (slot < 0 || slot >= initialized.length) {
            return;
        }
        Arrays.fill(historyX[slot], 0);
        Arrays.fill(historyY[slot], 0);
        initialized[slot] = false;
        inertia[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        headSpriteVariant[slot] = 0;
        noiseCadence[slot] = 0;
        vulnerableTail[slot] = new TailPosition(0, 0);
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   int ignoreHitsCountdown, int flashCountdown, int health,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(entity);
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Moldorm random source cannot be null");
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }

        int nextTransition = transitionCountdown & 0xFF;
        if (nextTransition > 0) {
            nextTransition--;
        }
        List<TailPosition> presentationSegments = tailSegments(slot);
        Presentation presentation = new Presentation(headSpriteVariant[slot],
            entity.x(), (entity.y() - entity.z()) & 0xFF, presentationSegments);

        RoomEntity moved = entity;
        int collisionFlags = 0;

        boolean accelerated = (health & 0xFF) < 0x02 || privateCountdown2[slot] != 0;
        int movementPasses = accelerated ? 2 : 1;
        for (int pass = 0; pass < movementPasses; pass++) {
            recordHistory(slot, entity.x(), (entity.y() - entity.z()) & 0xFF);
            Movement movement = moveAndSteer(moved, frameCounter, nextTransition,
                ignoreHitsCountdown, flashCountdown, randomByteSupplier,
                backgroundInteraction);
            moved = movement.entity();
            nextTransition = movement.transitionCountdown();
            collisionFlags |= movement.collisionFlags();
        }

        int cadenceLimit = accelerated ? 0x0B : 0x10;
        noiseCadence[slot]++;
        boolean noiseRequested = noiseCadence[slot] >= cadenceLimit;
        if (noiseRequested) {
            noiseCadence[slot] = 0;
        }

        headSpriteVariant[slot] = HEAD_SPRITE_VARIANTS[privateState1[slot] >> 1];
        List<TailPosition> segments = tailSegments(slot);
        vulnerableTail[slot] = segments.get(3);
        return new Update(moved, nextTransition, headSpriteVariant[slot],
            speedX[slot], speedY[slot], collisionFlags, inertia[slot],
            noiseRequested, segments, presentation);
    }

    int headSpriteVariant(int slot) {
        return headSpriteVariant[slot];
    }

    int inertia(int slot) {
        return inertia[slot];
    }

    int privateCountdown2(int slot) {
        return privateCountdown2[slot];
    }

    void onSwordHit(int slot) {
        privateCountdown2[slot] = 0xC8;
    }

    TailPosition vulnerableTail(int slot) {
        TailPosition position = vulnerableTail[slot];
        return position == null ? new TailPosition(0, 0) : position;
    }

    List<TailPosition> currentTailSegments(int slot) {
        return tailSegments(slot);
    }

    private Movement moveAndSteer(RoomEntity entity, int frameCounter,
                                  int transitionCountdown, int ignoreHitsCountdown,
                                  int flashCountdown, IntSupplier randomByteSupplier,
                                  RoomEntityBackgroundInteraction backgroundInteraction) {
        int slot = entity.slot();
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int collisionFlags = 0;
        if ((flashCountdown & 0xFF) == 0) {
            int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
            if (nextX != x && backgroundInteraction != null) {
                int direction = signedByte(speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction, nextX, y, ignoreHitsCountdown & 0xFF, frameCounter);
                if (result.blocked()) {
                    collisionFlags |= result.collisionFlag();
                } else {
                    x = nextX;
                }
            } else {
                x = nextX;
            }

            int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
            if (nextY != y && backgroundInteraction != null) {
                int direction = signedByte(speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction, x, nextY, ignoreHitsCountdown & 0xFF, frameCounter);
                if (result.blocked()) {
                    collisionFlags |= result.collisionFlag();
                } else {
                    y = nextY;
                }
            } else {
                y = nextY;
            }
        } else {
            collisionFlags = probeBackgroundWithoutMovement(entity, x, y,
                ignoreHitsCountdown, frameCounter, backgroundInteraction);
        }

        if (collisionFlags != 0) {
            privateState1[slot] = (collisionFlags & 0x01) != 0 ? 0x08
                : (collisionFlags & 0x02) != 0 ? 0x00
                : (collisionFlags & 0x04) != 0 ? 0x04 : 0x0C;
            if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                privateState2[slot] = -privateState2[slot];
            }
            transitionCountdown = 0x10;
        }

        if (privateCountdown1[slot] == 0) {
            privateCountdown1[slot] = 0x06;
            privateState1[slot] = (privateState1[slot] + privateState2[slot]) & 0x0F;
            speedY[slot] = Y_SPEEDS[privateState1[slot]];
            speedX[slot] = X_SPEEDS[privateState1[slot]];
        }
        if (transitionCountdown == 0) {
            transitionCountdown = 0x10 + (randomByteSupplier.getAsInt() & 0x1F);
            privateState2[slot] = (randomByteSupplier.getAsInt() & 0x02) - 1;
        }
        return new Movement(withPosition(entity, x, y), transitionCountdown, collisionFlags);
    }

    private record Movement(RoomEntity entity, int transitionCountdown, int collisionFlags) {
    }

    private void recordHistory(int slot, int x, int y) {
        inertia[slot] = (inertia[slot] + 1) & HISTORY_MASK;
        historyX[slot][inertia[slot]] = x & 0xFF;
        historyY[slot][inertia[slot]] = y & 0xFF;
    }

    private List<TailPosition> tailSegments(int slot) {
        List<TailPosition> segments = new ArrayList<>(TAIL_HISTORY_OFFSETS.length);
        for (int offset : TAIL_HISTORY_OFFSETS) {
            int index = (inertia[slot] - offset) & HISTORY_MASK;
            segments.add(new TailPosition(historyX[slot][index], historyY[slot][index]));
        }
        return List.copyOf(segments);
    }

    private int probeBackgroundWithoutMovement(
            RoomEntity entity, int x, int y, int ignoreHitsCountdown, int frameCounter,
            RoomEntityBackgroundInteraction backgroundInteraction) {
        if (backgroundInteraction == null) {
            return 0;
        }
        int collisionFlags = 0;
        int signedSpeedX = signedByte(speedX[entity.slot()]);
        if (signedSpeedX != 0) {
            int direction = signedSpeedX < 0
                ? EntityBackgroundCollisionResult.LEFT
                : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                entity, direction, x, y,
                ignoreHitsCountdown & 0xFF, frameCounter);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
            }
        }
        int signedSpeedY = signedByte(speedY[entity.slot()]);
        if (signedSpeedY != 0) {
            int direction = signedSpeedY < 0
                ? EntityBackgroundCollisionResult.UP
                : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                entity, direction, x, y,
                ignoreHitsCountdown & 0xFF, frameCounter);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
            }
        }
        return collisionFlags;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }
}
