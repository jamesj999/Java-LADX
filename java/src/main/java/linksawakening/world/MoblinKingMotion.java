package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$15 {@code MoblinKingEntityHandler}'s ten-state fight sequence. */
final class MoblinKingMotion {
    record ArrowRequest(int x, int y, int speedX, int speedY, int direction) {
    }

    record Update(RoomEntity entity, int transitionCountdown,
                  int slowTransitionCountdown, ArrowRequest arrowRequest,
                  boolean openIntroDialog, int jingleId, boolean strongBump,
                  int screenShakeCountdown, int linkDamage,
                  int ignoreLinkCollisionsCountdown) {
    }

    private static final int[] ARROW_X_OFFSETS = {0x08, -0x08};
    private static final int[] ARROW_Y_OFFSETS = {-0x04, -0x04};
    private static final int[] ARROW_SPEED_X = {0x20, -0x20};
    private static final int[] CHARGE_SPEED_X = {0x28, -0x28};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] xAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] yAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] zAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];

    Update advance(RoomEntity entity, int linkX, int linkY,
                   int transitionCountdown, int slowTransitionCountdown,
                   int frameCounter, IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   boolean touchingLink) {
        int slot = entity.slot();
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }

        int x = entity.x();
        int y = entity.y();
        int z = entity.z();
        boolean blocked = false;
        if (speedX[slot] != 0) {
            int nextX = addSpeed(x, speedX[slot], xAccumulator, slot);
            int collisionDirection = signedByte(speedX[slot]) < 0 ? 1 : 0;
            blocked = backgroundCollision != null
                && backgroundCollision.blocks(entity, collisionDirection, nextX, y);
            if (!blocked) {
                x = nextX;
            }
        }
        if (speedY[slot] != 0) {
            int nextY = addSpeed(y, speedY[slot], yAccumulator, slot);
            int collisionDirection = signedByte(speedY[slot]) < 0 ? 2 : 3;
            boolean yBlocked = backgroundCollision != null
                && backgroundCollision.blocks(entity, collisionDirection, x, nextY);
            blocked |= yBlocked;
            if (!yBlocked) {
                y = nextY;
            }
        }

        z = addSpeed(z, speedZ[slot], zAccumulator, slot);
        speedZ[slot] = signedByte(speedZ[slot]) - 3;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            speedZ[slot] = 0;
        }

        ArrowRequest arrow = null;
        boolean openDialog = false;
        int jingle = -1;
        boolean strongBump = false;
        int screenShake = 0;
        int linkDamage = 0;
        int ignoreLinkCollisions = 0;
        int variant = entity.spriteVariant() < 0 ? 0 : entity.spriteVariant();

        switch (state[slot]) {
            case 0 -> {
                transitionCountdown = 0x20;
                speedX[slot] = 0;
                speedY[slot] = 0;
                variant = 0;
                state[slot] = 1;
                direction[slot] = directionToLink(x, linkX);
            }
            case 1 -> {
                if (transitionCountdown == 0) {
                    openDialog = true;
                    state[slot] = 2;
                    slowTransitionCountdown = 0x30;
                    variant = 1;
                }
            }
            case 2 -> {
                if (privateCountdown1[slot] != 0) {
                    if (privateCountdown1[slot] == 0x0C) {
                        int facing = direction[slot] & 1;
                        arrow = new ArrowRequest((x + ARROW_X_OFFSETS[facing]) & 0xFF,
                            (y + ARROW_Y_OFFSETS[facing]) & 0xFF,
                            ARROW_SPEED_X[facing] & 0xFF, 0, facing);
                    }
                    variant = privateCountdown1[slot] < 0x0C ? 0 : 2;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                } else if (slowTransitionCountdown == 0) {
                    transitionCountdown = 0x30;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    state[slot] = 3;
                } else {
                    if (landed) {
                        Vector vector = vectorTowardsOffsetLink(x, y, linkX, linkY,
                            direction[slot], 0x0C);
                        speedX[slot] = vector.x();
                        speedY[slot] = vector.y();
                        speedZ[slot] = 0x10;
                    }
                    variant = (frameCounter >>> 4) & 1;
                    direction[slot] = directionToLink(x, linkX);
                    if (privateCountdown2[slot] == 0) {
                        privateCountdown2[slot] = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
                        privateCountdown1[slot] = 0x18;
                    }
                }
            }
            case 3 -> {
                if (transitionCountdown == 0) {
                    transitionCountdown = 0x22;
                    speedX[slot] = CHARGE_SPEED_X[direction[slot] & 1];
                    speedY[slot] = 0;
                    state[slot] = 4;
                } else {
                    if ((transitionCountdown & 7) == 0) {
                        jingle = 0x09;
                    }
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    variant = 2 + ((frameCounter >>> 3) & 1);
                }
            }
            case 4 -> {
                if (touchingLink) {
                    state[slot] = 8;
                    Update hit = applyLinkImpact(entity, x, y, z, variant,
                        transitionCountdown, slowTransitionCountdown);
                    return hit;
                }
                if (transitionCountdown == 0 && !blocked) {
                    state[slot] = 6;
                } else if (blocked) {
                    speedX[slot] = -signedByte(speedX[slot]) / 4;
                    speedZ[slot] = 0x28;
                    transitionCountdown = 0x60;
                    state[slot] = 5;
                    strongBump = true;
                    screenShake = 0x20;
                    jingle = 0x0B;
                }
                variant = 4 + ((frameCounter >>> 2) & 1);
            }
            case 5 -> {
                if (transitionCountdown == 0) {
                    transitionCountdown = 0x40;
                    state[slot] = 7;
                    variant = 0;
                } else {
                    if (landed) {
                        speedX[slot] = 0;
                        speedY[slot] = 0;
                    }
                    variant = 0x0C + ((frameCounter >>> 3) & 1);
                }
            }
            case 6 -> {
                int signedSpeedX = signedByte(speedX[slot]);
                if ((signedSpeedX & 0xFE) == 0) {
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    transitionCountdown = 0x40;
                    state[slot] = 7;
                } else {
                    speedX[slot] = signedSpeedX > 0 ? signedSpeedX - 2 : signedSpeedX + 2;
                    variant = 0;
                }
            }
            case 7 -> {
                speedX[slot] = 0;
                speedY[slot] = 0;
                if (transitionCountdown == 0) {
                    slowTransitionCountdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x20;
                    state[slot] = 2;
                }
            }
            case 8 -> {
                return applyLinkImpact(entity, x, y, z, variant,
                    transitionCountdown, slowTransitionCountdown);
            }
            case 9 -> {
                speedX[slot] = 0;
                speedY[slot] = 0;
                if (transitionCountdown == 0) {
                    slowTransitionCountdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x20;
                    state[slot] = 2;
                } else {
                    variant = transitionCountdown >= 0x40
                        ? 0 : ((frameCounter >>> 5) & 1);
                    direction[slot] = directionToLink(x, linkX);
                }
            }
            default -> state[slot] = 0;
        }

        return new Update(withPositionAndVariant(entity, x, y, z,
            presentationVariant(slot, variant)),
            transitionCountdown & 0xFF, slowTransitionCountdown & 0xFF, arrow,
            openDialog, jingle, strongBump, screenShake, linkDamage,
            ignoreLinkCollisions);
    }

    private Update applyLinkImpact(RoomEntity entity, int x, int y, int z, int variant,
                                   int transitionCountdown, int slowTransitionCountdown) {
        int slot = entity.slot();
        state[slot] = 9;
        transitionCountdown = 0x60;
        return new Update(withPositionAndVariant(entity, x, y, z,
            presentationVariant(slot, variant)),
            transitionCountdown, slowTransitionCountdown, null, false, 0x0B,
            false, 0, 0x08, 0x28);
    }

    void clear(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        xAccumulator[slot] = 0;
        yAccumulator[slot] = 0;
        zAccumulator[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
    }

    int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    void setPrivateCountdownsForTest(int slot, int countdown1, int countdown2) {
        privateCountdown1[slot] = countdown1 & 0xFF;
        privateCountdown2[slot] = countdown2 & 0xFF;
    }

    void setStateForTest(int slot, int nextState, int nextDirection,
                         int nextSpeedX, int nextSpeedY, int nextSpeedZ) {
        state[slot] = nextState;
        direction[slot] = nextDirection & 1;
        speedX[slot] = signedByte(nextSpeedX);
        speedY[slot] = signedByte(nextSpeedY);
        speedZ[slot] = signedByte(nextSpeedZ);
    }

    private static int directionToLink(int x, int linkX) {
        return signedByte((linkX - x) & 0xFF) < 0 ? 1 : 0;
    }

    private int presentationVariant(int slot, int variant) {
        return direction[slot] == 0 && variant < 0x0C ? variant + 0x06 : variant;
    }

    private static Vector vectorTowardsOffsetLink(int x, int y, int linkX, int linkY,
                                                  int direction, int length) {
        int targetX = (linkX + (direction == 0 ? -0x30 : 0x30)) & 0xFF;
        int dx = signedByte((targetX - x) & 0xFF);
        int dy = signedByte((linkY - y) & 0xFF);
        int total = Math.max(1, Math.abs(dx) + Math.abs(dy));
        return new Vector(dx * length / total, dy * length / total);
    }

    private static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        int fixed = ((position & 0xFF) << 4) | (accumulator[slot] & 0x0F);
        fixed += signedByte(speed);
        accumulator[slot] = fixed & 0x0F;
        return (fixed >>> 4) & 0xFF;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                     int z, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z & 0xFF);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
