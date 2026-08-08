package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 Zol/Gel inching, leaping, and split state machine. */
final class ZolGelMotion {
    static final int ENTITY_ZOL = 0x1B;
    static final int ENTITY_GEL = 0x1C;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] flashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitZol and EntityInitWithRandomDirection for Gel. */
    void initialize(int slot, int entityType, IntSupplier randomByteSupplier) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        privateCountdown3[slot] = 0;
        direction[slot] = entityType == ENTITY_GEL
            ? randomByteSupplier.getAsInt() & 0x03 : 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        initialized[slot] = true;
    }

    void onSwordHit(int slot) {
        flashCountdown[slot] = 0x18;
    }

    void onLinkCollision(int slot) {
        initialized[slot] = true;
        if (state[slot] != 4) {
            transitionCountdown[slot] = 0x80;
            state[slot] = 4;
        }
    }

    boolean skipsEnemyCollision(int slot) {
        return state[slot] == 4 || privateCountdown3[slot] != 0;
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY, int linkEntityZ,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, linkEntityX, linkEntityY, linkEntityZ,
            randomByteSupplier,
            backgroundCollision == null
                ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision),
            0, false);
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY, int linkEntityZ,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int frameCounter,
                   boolean joypadHeld) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, entity.type(), randomByteSupplier);
        }
        decrementCountdowns(slot);

        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 3) & 0xFF;
        boolean hitGround = (z & 0x80) != 0;
        if (hitGround) {
            z = 0;
            speedZ[slot] = 0;
        }

        int type = entity.type();
        Split split = null;
        if (type == ENTITY_ZOL && flashCountdown[slot] == 8) {
            flashCountdown[slot] = 0;
            int originalX = entity.x();
            int originalY = entity.y();
            int originalZ = z;
            resetForGel(slot);
            speedZ[slot] = 0x20;
            split = new Split(originalX, originalY, originalZ);
            type = ENTITY_GEL;
        }

        if (privateCountdown2[slot] == 0) {
            spriteVariant[slot] = state[slot] & 0x01;
            if (spriteVariant[slot] == 1) {
                privateCountdown2[slot] = 8;
            }
        }

        int x = entity.x();
        int y = entity.y();
        if (type == ENTITY_GEL && state[slot] == 4 && transitionCountdown[slot] != 0) {
            int xOffset = ((transitionCountdown[slot] >>> 1) & 0x07) - 4;
            int yOffset = ((transitionCountdown[slot] >>> 3) & 0x07) - 4;
            x = (linkEntityX - xOffset) & 0xFF;
            y = (linkEntityY - yOffset) & 0xFF;
            z = linkEntityZ & 0xFF;
            if (joypadHeld) {
                decreaseTransitionCountdown(slot);
                decreaseTransitionCountdown(slot);
                decreaseTransitionCountdown(slot);
            }
        } else {
            if (state[slot] == 0) {
                int[] moved = physics(entity, x, y, backgroundInteraction, frameCounter);
                x = moved[0];
                y = moved[1];
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 7;
                    state[slot] = 1;
                    Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 4);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                }
            } else if (state[slot] == 1) {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x10;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    if ((randomByteSupplier.getAsInt() & 0x0F) != 0) {
                        transitionCountdown[slot] = 0;
                        state[slot] = 2;
                        int[] moved = physics(entity, x, y,
                            backgroundInteraction, frameCounter);
                        x = moved[0];
                        y = moved[1];
                    } else {
                        transitionCountdown[slot] = 0x50;
                        state[slot] = 2;
                    }
                } else {
                    int[] moved = physics(entity, x, y,
                        backgroundInteraction, frameCounter);
                    x = moved[0];
                    y = moved[1];
                }
            } else if (state[slot] == 2) {
                if (transitionCountdown[slot] == 0) {
                    state[slot] = 3;
                    Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x10);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                    speedZ[slot] = 0x20;
                } else {
                    speedX[slot] = (transitionCountdown[slot] & 0x04) == 0
                        ? 0x08 : 0xF8;
                    speedY[slot] = 0;
                    int[] moved = physics(entity, x, y,
                        backgroundInteraction, frameCounter);
                    x = moved[0];
                    y = moved[1];
                }
            } else if (state[slot] == 3) {
                int[] moved = physics(entity, x, y, backgroundInteraction, frameCounter);
                x = moved[0];
                y = moved[1];
                if (hitGround) {
                    state[slot] = 0;
                }
            } else if (state[slot] == 4) {
                if (transitionCountdown[slot] == 0) {
                    privateCountdown3[slot] = 0x30;
                    Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x10);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                    speedZ[slot] = 0x20;
                    z = (z + 1) & 0xFF;
                    state[slot] = 3;
                }
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), type, x, y,
            entity.status(), entity.spriteDefinition(), spriteVariant[slot], entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
        return new Update(updated, split);
    }

    /** Initializes a SpawnNewEntity Gel without consuming its init random byte. */
    void prepareSpawnedGel(int slot) {
        resetForGel(slot);
        initialized[slot] = true;
        speedZ[slot] = 0x20;
    }

    /** Mirrors the special Zol state written by ChestWithItemEntityHandler. */
    void prepareChestSpawn(int slot) {
        resetForGel(slot);
        initialized[slot] = true;
        state[slot] = 3;
        speedX[slot] = 0x08;
        speedZ[slot] = 0x18;
        privateCountdown1[slot] = 0x50;
    }

    void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        privateCountdown3[slot] = 0;
        flashCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    void setPrivateCountdown1ForTest(int slot, int value) {
        privateCountdown1[slot] = value & 0xFF;
    }

    private void decrementCountdowns(int slot) {
        // GelState4Handler only decreases its transition countdown when the
        // ROM sees joypad input; state 4 is therefore intentionally excluded
        // from the ordinary per-frame countdown pass.
        if (state[slot] != 4 && transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
        if (privateCountdown3[slot] > 0) {
            privateCountdown3[slot]--;
        }
        if (flashCountdown[slot] > 0) {
            flashCountdown[slot]--;
        }
    }

    private void decreaseTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    private int[] physics(RoomEntity entity, int x, int y,
                          RoomEntityBackgroundInteraction backgroundInteraction,
                          int frameCounter) {
        // ZolGelPhysics still updates position while private countdown 1 is
        // active, but the ROM skips its background helper for that frame. When
        // it does call the helper, it temporarily exposes ignore-hits value
        // $02 to the rich room probe.
        RoomEntityBackgroundInteraction physicsInteraction =
            privateCountdown1[entity.slot()] == 0 ? backgroundInteraction : null;
        return move(entity, x, y, physicsInteraction, 0x02, frameCounter);
    }

    private void resetForGel(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        privateCountdown3[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZ[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        flashCountdown[slot] = 0;
    }

    private int[] move(RoomEntity entity, int x, int y,
                       RoomEntityBackgroundInteraction backgroundInteraction,
                       int ignoreHitsCountdown, int frameCounter) {
        int slot = entity.slot();
        int movedX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int movedY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (backgroundInteraction != null) {
            if (movedX != x && backgroundInteraction.probe(entity,
                signedByte(speedX[slot]) < 0 ? 1 : 0, movedX, y,
                ignoreHitsCountdown, frameCounter).blocked()) {
                movedX = x;
            }
            if (movedY != y && backgroundInteraction.probe(entity,
                signedByte(speedY[slot]) < 0 ? 2 : 3, movedX, movedY,
                ignoreHitsCountdown, frameCounter).blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
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

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Update(RoomEntity entity, Split split) {
    }

    record Split(int originalX, int originalY, int originalZ) {
    }

    private record Vector(int x, int y) {
    }
}
