package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 BushCrawlerEntityHandler movement and lift trigger. */
final class BushCrawlerMotion {
    static final int ENTITY_BUSH_CRAWLER = 0xBB;
    static final int ENTITY_SWORD = 0x01;
    static final int ENTITY_POWER_BRACELET = 0x03;

    private static final int[] SPEED_X_BY_DIRECTION = {0x10, 0xF0, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF0, 0x10};
    // Data_007_41E6 is immediately followed by Data_007_41E8 in bank $07.
    // The handler intentionally indexes across that label boundary for Y.
    private static final int[] SPECIAL_SPEED_X = {0x00, 0x0C, 0x10, 0x0C,
        0x00, 0xF4, 0xF0, 0xF4};
    private static final int[] SPECIAL_SPEED_Y = {0xF0, 0xF4, 0x00, 0x0C,
        0x10, 0x0C, 0x00, 0xF4};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] presentationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int state, int transitionCountdown,
                  int privateState1, int privateState4, int visualYOffset,
                  boolean liftRequested, boolean specialState,
                  boolean crawlOverlayRendered) {
    }

    void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    void initializeSpecial(int slot) {
        initialize(slot);
        privateState1[slot] = 2;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown, boolean linkCollision,
                   boolean actionButtonAHeld, boolean actionButtonBHeld,
                   int linkItemA, int linkItemB) {
        if ((entity.type() & 0xFF) != ENTITY_BUSH_CRAWLER) {
            throw new IllegalArgumentException("Unsupported Bush Crawler motion entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Bush Crawler random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        boolean wasInitialized = initialized[slot];
        if (!wasInitialized) {
            initialize(slot);
        }
        if (privateState1[slot] == 0) {
            privateState1[slot] = 1;
            privateState4[slot] = ((entity.x() & 0xFF) >>> 4) & 0x01;
        }

        boolean crawlOverlayRendered = privateState1[slot] != 2
            && privateState2[slot] == 4;
        int renderedVariant = privateState1[slot] == 2 || crawlOverlayRendered
            ? (wasInitialized ? presentationVariant[slot] : entity.spriteVariant())
            : privateState4[slot];
        int renderedVisualYOffset = privateState2[slot] == 4 ? -4 : 0;
        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        boolean liftRequested = false;

        if (privateState1[slot] == 2) {
            Position moved = move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            if (countdown == 0) {
                int randomByte = randomByteSupplier.getAsInt() & 0xFF;
                int random = randomByte & 0x07;
                countdown = 0x20 + (randomByte & 0x1F);
                speedX[slot] = SPECIAL_SPEED_X[random];
                speedY[slot] = SPECIAL_SPEED_Y[random];
            }
            presentationVariant[slot] = (frameCounter >>> 3) & 0x01;
        } else {
            switch (state[slot]) {
                case 0 -> {
                    privateState2[slot] = 0;
                    if (countdown == 0 && startsCrawl(x, y, linkEntityX, linkEntityY)) {
                        state[slot] = 1;
                        int direction = directionToLink(x, y, linkEntityX, linkEntityY);
                        speedX[slot] = SPEED_X_BY_DIRECTION[direction];
                        speedY[slot] = SPEED_Y_BY_DIRECTION[direction];
                        countdown = 0x30;
                    } else if (linkCollision && powerBraceletHeld(
                        actionButtonAHeld, actionButtonBHeld, linkItemA, linkItemB)) {
                        liftRequested = true;
                    }
                }
                case 1 -> {
                    privateState2[slot] = 4;
                    if (countdown == 0) {
                        countdown = 0x20;
                        state[slot] = 0;
                    } else {
                        Position moved = move(entity, x, y, backgroundCollision);
                        x = moved.x();
                        y = moved.y();
                        if (moved.collided()) {
                            countdown = 0x20;
                            state[slot] = 0;
                        } else {
                            presentationVariant[slot] = (frameCounter >>> 3) & 0x01;
                        }
                    }
                }
                default -> throw new IllegalStateException("Invalid Bush Crawler state: "
                    + state[slot]);
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), renderedVariant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, state[slot], countdown, privateState1[slot],
            privateState4[slot], renderedVisualYOffset, liftRequested,
            privateState1[slot] == 2, crawlOverlayRendered);
    }

    void clear(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateState4[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = false;
    }

    void forceStateForTest(int slot, int newState, int transitionCountdown,
                           int newPrivateState1, int newPrivateState4,
                           int newSpeedX, int newSpeedY) {
        state[slot] = newState & 0xFF;
        privateState1[slot] = newPrivateState1 & 0xFF;
        privateState2[slot] = newState == 1 ? 4 : 0;
        privateState4[slot] = newPrivateState4 & 0x01;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = true;
    }

    void forcePrivateStateForTest(int slot, int newPrivateState1, int transitionCountdown) {
        state[slot] = 0;
        privateState1[slot] = newPrivateState1 & 0xFF;
        privateState2[slot] = 0;
        privateState4[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = true;
    }

    int state(int slot) {
        return state[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState4(int slot) {
        return privateState4[slot];
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    private static boolean startsCrawl(int x, int y, int linkX, int linkY) {
        int distanceY = signedByte(linkY - y);
        if (!inSignedWindow(distanceY, 8)) {
            return false;
        }
        int distanceX = signedByte(linkX - x);
        if (!inSignedWindow(distanceX, 8)) {
            return false;
        }
        return directionToLink(x, y, linkX, linkY) != 2;
    }

    private static boolean powerBraceletHeld(boolean actionButtonAHeld,
                                               boolean actionButtonBHeld,
                                               int linkItemA, int linkItemB) {
        if ((linkItemB & 0xFF) == ENTITY_POWER_BRACELET) {
            return actionButtonBHeld;
        }
        return (linkItemA & 0xFF) == ENTITY_POWER_BRACELET && actionButtonAHeld;
    }

    private Position move(RoomEntity entity, int x, int y,
                          RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Position(int x, int y, boolean collided) {
    }
}
