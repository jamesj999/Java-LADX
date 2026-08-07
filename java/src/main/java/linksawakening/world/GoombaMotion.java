package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 GoombaEntityHandler's ordinary-room random walk. */
final class GoombaMotion {
    private static final int ENTITY_GOOMBA = 0x9F;
    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        inertia[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown, boolean sideScrolling) {
        if ((entity.type() & 0xFF) != ENTITY_GOOMBA) {
            throw new IllegalArgumentException("Unsupported Goomba motion entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int variant;
        boolean startDying = false;
        if (sideScrolling) {
            SideUpdate sideUpdate = advanceSideScrolling(entity, frameCounter, linkEntityX,
                x, y, backgroundCollision);
            x = sideUpdate.x();
            y = sideUpdate.y();
            variant = sideUpdate.variant();
            if (state[slot] == 2 && countdown == 0) {
                startDying = true;
            }
        } else {
            switch (state[slot]) {
                case 0 -> {
                    if (countdown == 0) {
                        countdown = 0x30 + (randomByteSupplier.getAsInt() & 0x3F);
                        inertia[slot] = (inertia[slot] + 1) & 0xFF;
                        if (inertia[slot] == 4) {
                            inertia[slot] = 0;
                            direction[slot] = directionToLink(
                                x, y, linkEntityX, linkEntityY);
                        } else {
                            direction[slot] = randomByteSupplier.getAsInt() & 0x03;
                        }
                        speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
                        speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
                        state[slot] = 1;
                    }
                    variant = animationVariant(frameCounter);
                }
                case 1 -> {
                    if (countdown == 0) {
                        countdown = 0x20;
                        state[slot] = 2;
                        inertia[slot] = 0;
                    }
                    Position position = move(entity, x, y, backgroundCollision);
                    x = position.x();
                    y = position.y();
                    variant = animationVariant(frameCounter);
                }
                case 2 -> {
                    variant = 2;
                    if (countdown == 0) {
                        // func_007_666B writes the heart/drop byte and hands
                        // the entity to the shared EntityDeathHandler. The
                        // runtime owns those WRAM fields, so report the
                        // transition rather than mutating the entity here.
                        startDying = true;
                    }
                }
                default -> throw new IllegalStateException("Invalid Goomba state: "
                    + state[slot]);
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, countdown, startDying);
    }

    void enterStomp(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        state[slot] = 2;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private SideUpdate advanceSideScrolling(RoomEntity entity, int frameCounter, int linkEntityX,
                                            int x, int y,
                                            RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (state[slot] == 0) {
            direction[slot] = linkEntityX - x < 0 ? 1 : 0;
            speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
            speedY[slot] = 0;
            state[slot] = 1;
            return new SideUpdate(x, y, animationVariant(frameCounter));
        }
        if (state[slot] == 1) {
            // The side-view branch uses the same bank-$07 position helper and
            // gravity as the ordinary branch, but chooses horizontal speed
            // from Link's X distance instead of the random walk tables.
            Position position = move(entity, x, y, backgroundCollision);
            return new SideUpdate(position.x(), position.y(), animationVariant(frameCounter));
        }
        return new SideUpdate(x, y, 2);
    }

    private Position move(RoomEntity entity, int x, int y,
                          RoomEntityBackgroundCollision backgroundCollision) {
        int nextX = addSpeedToPosition(x, speedX[entity.slot()], speedXAccumulator, entity.slot());
        int nextY = addSpeedToPosition(y, speedY[entity.slot()], speedYAccumulator, entity.slot());
        // UpdateEntityPosWithSpeed_07 runs before the handler's two INC [HL]
        // gravity writes; ApplyEntityInteractionWithBackground then sees the
        // accelerated speed and can clear it on a floor collision.
        speedY[entity.slot()] = (speedY[entity.slot()] + 0x02) & 0xFF;
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, directionForX(speedX[entity.slot()]),
                nextX, y)) {
            nextX = x;
            speedX[entity.slot()] = negateByte(speedX[entity.slot()]);
        }
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, directionForY(speedY[entity.slot()]),
                nextX, nextY)) {
            nextY = ((nextY & 0xF0) + 0x03) & 0xFF;
            speedY[entity.slot()] = 0;
        }
        return new Position(nextX, nextY);
    }

    private static int animationVariant(int frameCounter) {
        return (frameCounter >>> 4) & 0x01;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
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

    private static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int negateByte(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Position(int x, int y) {
    }

    private record SideUpdate(int x, int y, int variant) {
    }

    record Update(RoomEntity entity, int transitionCountdown, boolean startDying) {
    }
}
