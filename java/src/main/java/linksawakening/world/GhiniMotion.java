package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$04 Ghini motion, including the shared hidden/visible handler state. */
final class GhiniMotion {
    private static final int HIDING_GHINI = 0x10;
    private static final int GIANT_GHINI = 0x11;
    private static final int ORDINARY_GHINI = 0x12;
    private static final int HIDDEN_STATE = 1;
    private static final int HIDDEN_SPRITE_VARIANT = -1;
    private static final int GIANT_ORIENTATION_OFFSET = 2;

    private static final int[] TARGET_X_SPEEDS = {0x0C, 0xF4};
    private static final int[] TARGET_Y_SPEEDS = {0x08, 0xF8};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetXDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetYDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] baseEntityFlipAttribute = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] giantRectangleVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] baseFlipInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        initialize(slot, ORDINARY_GHINI);
    }

    void initialize(int slot, int entityType) {
        state[slot] = isHidingType(entityType) ? HIDDEN_STATE : 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        targetXDirection[slot] = 0;
        targetYDirection[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        baseEntityFlipAttribute[slot] = 0;
        giantRectangleVariant[slot] = 0;
        baseFlipInitialized[slot] = false;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        decrementCountdowns(slot);

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = correctZPosition(entity.z(), frameCounter);

        // The ordinary Ghini never enters the hiding state. Preserve the ROM
        // early return so the same state can be enabled for hiding Ghinis
        // without changing this visible branch.
        if (privateCountdown2[slot] != 0) {
            return withPosition(entity, x, y, z);
        }

        if (transitionCountdown[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            transitionCountdown[slot] = 0x20 | (random & 0x1F);
            targetXDirection[slot] = random & 0x01;
        }
        if (privateCountdown1[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            privateCountdown1[slot] = 0x18 | (random & 0x0F);
            targetYDirection[slot] = random & 0x01;
        }

        if ((((frameCounter & 0xFF) ^ slot) & 0x03) == 0) {
            if (x < 0x28) {
                targetXDirection[slot] = 0;
                transitionCountdown[slot] = 0x20;
            } else if (x >= 0x78) {
                targetXDirection[slot] = 1;
                transitionCountdown[slot] = 0x20;
            }

            int visualY = (y - z) & 0xFF;
            if (visualY < 0x28) {
                targetYDirection[slot] = 0;
                privateCountdown1[slot] = 0x20;
            } else if (visualY >= 0x60) {
                targetYDirection[slot] = 1;
                privateCountdown1[slot] = 0x20;
            }

            speedX[slot] = approachTarget(speedX[slot],
                TARGET_X_SPEEDS[targetXDirection[slot]]);
            speedY[slot] = approachTarget(speedY[slot],
                TARGET_Y_SPEEDS[targetYDirection[slot]]);
        }

        int flipAttribute = entity.entityFlipAttribute()
            ^ (isNegative(speedX[slot]) ? 0 : 0x20);
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(), flipAttribute,
            entity.spriteTileOffset(), z);
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int entityType,
                       int linkEntityX, int linkEntityY, int collisionType,
                       IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, entityType);
        }
        rememberBaseFlipAttribute(entity);

        if (hidden(slot)) {
            return advanceHidden(entity, frameCounter, entityType, linkEntityX, linkEntityY,
                collisionType);
        }
        return advanceVisible(entity, frameCounter, entityType, randomByteSupplier);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    boolean hidden(int slot) {
        return state[slot] != 0;
    }

    int state(int slot) {
        return state[slot];
    }

    int privateCountdown2(int slot) {
        return privateCountdown2[slot];
    }

    int giantRectangleVariant(int slot) {
        return giantRectangleVariant[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int targetXDirection(int slot) {
        return targetXDirection[slot];
    }

    int targetYDirection(int slot) {
        return targetYDirection[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private void decrementCountdowns(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    private RoomEntity advanceHidden(RoomEntity entity, int frameCounter, int entityType,
                                      int linkEntityX, int linkEntityY, int collisionType) {
        int animation = animationVariant(frameCounter, entity.slot());
        int flipAttribute = computedFlipAttribute(entity.slot());
        RoomEntity hiddenEntity = withPresentation(entity, entity.x(), entity.y(), entity.z(),
            entityType, animation, flipAttribute, HIDDEN_SPRITE_VARIANT);

        if (withinRevealWindow(linkEntityX, entity.x())
            && withinRevealWindow(linkEntityY, entity.y())
            && (collisionType & 0xFF) != 0) {
            state[entity.slot()] = 0;
            privateCountdown2[entity.slot()] = 0x30;
        }
        return hiddenEntity;
    }

    private RoomEntity advanceVisible(RoomEntity entity, int frameCounter, int entityType,
                                      IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        decrementCountdowns(slot);

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = correctZPosition(entity.z(), frameCounter);

        if (privateCountdown2[slot] == 0) {
            refreshTargetsAndAccelerate(slot, frameCounter, x, y, z, randomByteSupplier);
        }

        int animation = animationVariant(frameCounter, slot);
        int flipAttribute = computedFlipAttribute(slot);
        return withPresentation(entity, x, y, z, entityType, animation, flipAttribute, animation);
    }

    private void refreshTargetsAndAccelerate(int slot, int frameCounter, int x, int y, int z,
                                             IntSupplier randomByteSupplier) {
        if (transitionCountdown[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            transitionCountdown[slot] = 0x20 | (random & 0x1F);
            targetXDirection[slot] = random & 0x01;
        }
        if (privateCountdown1[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            privateCountdown1[slot] = 0x18 | (random & 0x0F);
            targetYDirection[slot] = random & 0x01;
        }

        if ((((frameCounter & 0xFF) ^ slot) & 0x03) != 0) {
            return;
        }

        if (x < 0x28) {
            targetXDirection[slot] = 0;
            transitionCountdown[slot] = 0x20;
        } else if (x >= 0x78) {
            targetXDirection[slot] = 1;
            transitionCountdown[slot] = 0x20;
        }

        int visualY = (y - z) & 0xFF;
        if (visualY < 0x28) {
            targetYDirection[slot] = 0;
            privateCountdown1[slot] = 0x20;
        } else if (visualY >= 0x60) {
            targetYDirection[slot] = 1;
            privateCountdown1[slot] = 0x20;
        }

        speedX[slot] = approachTarget(speedX[slot], TARGET_X_SPEEDS[targetXDirection[slot]]);
        speedY[slot] = approachTarget(speedY[slot], TARGET_Y_SPEEDS[targetYDirection[slot]]);
    }

    private void rememberBaseFlipAttribute(RoomEntity entity) {
        int slot = entity.slot();
        if (!baseFlipInitialized[slot]) {
            baseEntityFlipAttribute[slot] = entity.entityFlipAttribute() & 0xFF;
            baseFlipInitialized[slot] = true;
        }
    }

    private int computedFlipAttribute(int slot) {
        return (baseEntityFlipAttribute[slot]
            ^ (isNegative(speedX[slot]) ? 0 : 0x20)) & 0xFF;
    }

    private RoomEntity withPresentation(RoomEntity entity, int x, int y, int z,
                                        int entityType, int animation, int flipAttribute,
                                        int ordinaryVariant) {
        int selectedVariant = ordinaryVariant;
        int genericFlipAttribute = flipAttribute;
        if (entityType == GIANT_GHINI) {
            int giantVariant = animation
                + ((flipAttribute & 0x20) != 0 ? GIANT_ORIENTATION_OFFSET : 0);
            giantRectangleVariant[entity.slot()] = giantVariant;
            selectedVariant = ordinaryVariant == HIDDEN_SPRITE_VARIANT
                ? HIDDEN_SPRITE_VARIANT : giantVariant;
            genericFlipAttribute = flipAttribute & 0x0F;
        }
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), selectedVariant,
            genericFlipAttribute, entity.spriteTileOffset(), z);
    }

    private static int animationVariant(int frameCounter, int slot) {
        return (((frameCounter & 0xFF) >>> 4) ^ slot) & 0x01;
    }

    private static boolean withinRevealWindow(int linkPosition, int entityPosition) {
        int signedDistance = (linkPosition - entityPosition) & 0xFF;
        return ((signedDistance + 0x10) & 0xFF) < 0x20;
    }

    private static boolean isHidingType(int entityType) {
        int type = entityType & 0xFF;
        return type == HIDING_GHINI || type == GIANT_GHINI;
    }

    private static int approachTarget(int current, int target) {
        int difference = (target - current) & 0xFF;
        return (difference & 0x80) != 0 ? (current - 1) & 0xFF : (current + 1) & 0xFF;
    }

    private static int correctZPosition(int z, int frameCounter) {
        if ((frameCounter & 0x03) != 0 || z == 0x10) {
            return z & 0xFF;
        }
        if ((z & 0x80) != 0 || z < 0x10) {
            return (z + 1) & 0xFF;
        }
        return (z - 1) & 0xFF;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = speed < 0x80 ? speed : speed - 0x100;
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static boolean isNegative(int speed) {
        return (speed & 0x80) != 0;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
    }
}
