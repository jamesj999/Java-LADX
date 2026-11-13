package linksawakening.world.entity.motion;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$36 ColorShellRed/Green/BlueEntityHandler state machine and fixed-point movement. */
public final class ColorShellMotion {
    private ColorShellWorld colorShellWorld = ColorShellWorld.none();

    public ColorShellWorld readColorShellWorld() {
        return colorShellWorld;
    }

    public ColorShellWorld writeColorShellWorld(ColorShellWorld value) {
        return colorShellWorld = value;
    }

    static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;

    static final int[] SPEED_X_BY_DIRECTION = {0x03, 0xFD, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xFD, 0x03};
    private static final int[] CHARGE_DIRECTION_BY_DIRECTION = {0x02, 0x03, 0x01, 0x00};
    private static final int[] PUZZLE_EXPECTED_OBJECTS = {0x5E, 0x59, 0x63};
    static final int[] PUZZLE_FINAL_OBJECTS = {0x5F, 0x5A, 0x64};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] positionX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] positionY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] positionZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        resetSlot(slot);
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return ColorShellMotion.advanceBody(
                this, entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision);
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int ignoreHitsCountdown) {
        return ColorShellMotion.advanceBody(
                this,
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                ignoreHitsCountdown);
    }

    public Update advance(ColorShellFrame inputs) {
        return ColorShellMotion.advanceBody(this, inputs);
    }

    public void clear(int slot) {
        resetSlot(slot);
        initialized[slot] = false;
    }

    private void resetSlot(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        physicsFlags[slot] = 0;
        positionX[slot] = 0;
        positionY[slot] = 0;
        positionZ[slot] = 0;
    }

    public int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    public int spriteVariant(int slot) {
        return spriteVariant[slot];
    }

    public int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    /** Restore bank-$36 WRAM state supplied by room debug configuration. */
    public void restoreState(int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        if (state < 0 || state > 0x0D) {
            throw new IllegalArgumentException("Color Shell state out of range: " + state);
        }
        initialize(slot);
        this.state[slot] = state;
        this.transitionCountdown[slot] = transitionCountdown & 0xFF;
        this.direction[slot] = direction & 0x03;
        this.speedX[slot] = speedX & 0xFF;
        this.speedY[slot] = speedY & 0xFF;
    }

    /** Enters the Color Dungeon's state-$06 branch without discarding WRAM position. */
    public void enterState6(RoomEntity entity) {
        int slot = entity.slot();
        initialize(slot);
        positionX[slot] = entity.x();
        positionY[slot] = entity.y();
        positionZ[slot] = entity.z();
        state[slot] = 0x06;
    }

    void advanceState0(int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier, int slot) {
        if (transitionCountdown[slot] == 0) {
            direction[slot] = ((randomByteSupplier.getAsInt() & 0xFF) & 0x06) >>> 1;
            transitionCountdown[slot] = 0x40;
            state[slot] = 1;
        }
        if (unsignedDistance(linkEntityX, positionX[slot]) < 0x30
                && unsignedDistance(linkEntityY, positionY[slot]) < 0x30) {
            state[slot] = 1;
        }
    }

    void advanceState1(
            int frame,
            int linkEntityX,
            int linkEntityY,
            RoomEntityBackgroundCollision backgroundCollision,
            int slot,
            RoomEntity entity) {
        ColorShellMotion.advanceState1Body(this, frame, linkEntityX, linkEntityY, backgroundCollision, slot, entity);
    }

    void advanceState2(int frame, int ignoreHitsCountdown, int slot) {
        if (transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x18;
            state[slot] = 3;
        }
        animateJumpVariant(frame, slot);
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
        }
    }

    void advanceState3(
            int frame,
            int ignoreHitsCountdown,
            RoomEntityBackgroundCollision backgroundCollision,
            int slot,
            RoomEntity entity) {
        if (transitionCountdown[slot] == 0) {
            state[slot] = 1;
            speedX[slot] = 0;
            speedY[slot] = 0;
        }
        moveWithBackground(entity, backgroundCollision, true, slot);
        animateJumpVariant(frame, slot);
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
        }
    }

    int advanceState4(int slot) {
        int nextState = (state[slot] + 1) & 0xFF;
        transitionCountdown[slot] = 0;
        state[slot] = nextState;
        return 0;
    }

    int advanceState5(RoomEntity entity, int ignoreHitsCountdown, int slot) {
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
            return ignoreHitsCountdown & 0xFF;
        }
        physicsFlags[slot] |= 0x80;
        if (entity.status().value() < EntityStatus.STUNNED.value()) {
            physicsFlags[slot] &= 0x7F;
            state[slot] = 1;
        }
        return 0;
    }

    void advanceState6(int slot) {
        positionX[slot] = positionX[slot] < 0x50 ? 0x28 : 0x78;
        positionY[slot] = positionY[slot] < 0x48 ? 0x30 : 0x60;
        speedZ[slot] = 0x10;
        state[slot]++;
    }

    void advanceState7(int slot) {
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        positionZ[slot] = addSpeedToPosition(positionZ[slot], speedZ[slot], speedZAccumulator, slot);
        if ((positionZ[slot] & 0x80) != 0) {
            positionZ[slot] = 0;
            speedZ[slot] = 0;
            state[slot]++;
        }
    }

    void advanceState8(RoomEntity entity, ColorShellWorld world, int slot) {
        int color = entity.type() - ENTITY_COLOR_SHELL_RED;
        if (world.objectAt(entity, -1) == PUZZLE_EXPECTED_OBJECTS[color]) {
            state[slot] = 0x0C;
            physicsFlags[slot] |= 0xF0;
            world.writeObject(entity, 0x67 + color);
            world.playNoise(0x04);
            return;
        }

        world.playJingle(0x1D);
        speedY[slot] = 0;
        speedX[slot] = positionX[slot] < 0x50 ? 0x10 : 0xF0;
        speedZ[slot] = 0x10;
        transitionCountdown[slot] = 0x18;
        state[slot] = 9;
    }

    void advanceState9(RoomEntityBackgroundCollision backgroundCollision, int slot, RoomEntity entity) {
        ColorShellMotion.advanceState9Body(this, backgroundCollision, slot, entity);
    }

    void advanceStateA(RoomEntityBackgroundCollision backgroundCollision, int slot, RoomEntity entity) {
        ColorShellMotion.advanceStateABody(this, backgroundCollision, slot, entity);
    }

    void advanceStateB(int slot) {
        ColorShellMotion.advanceStateBBody(this, slot);
    }

    void advanceStateC(RoomEntity entity, ColorShellWorld world, List<RoomEntity> allEntities, int slot) {
        ColorShellMotion.advanceStateCBody(this, entity, world, allEntities, slot);
    }

    boolean advanceStateD(RoomEntity entity, ColorShellWorld world, int slot) {
        return ColorShellMotion.advanceStateDBody(this, entity, world, slot);
    }

    private void animateJumpVariant(int frame, int slot) {
        if ((frame & 0x01) != 0) {
            return;
        }
        spriteVariant[slot] = (spriteVariant[slot] + 1) & 0x01;
        if (spriteVariant[slot] == 0) {
            direction[slot] = CHARGE_DIRECTION_BY_DIRECTION[direction[slot] & 0x03];
        }
    }

    void moveWithBackground(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, boolean clampPosition, int slot) {
        int x = addSpeedToPosition(positionX[slot], speedX[slot], speedXAccumulator, slot);
        if (x != positionX[slot]
                && backgroundCollision != null
                && backgroundCollision.blocks(entity, directionForX(speedX[slot]), x, positionY[slot])) {
            x = positionX[slot];
        }
        int y = addSpeedToPosition(positionY[slot], speedY[slot], speedYAccumulator, slot);
        if (y != positionY[slot]
                && backgroundCollision != null
                && backgroundCollision.blocks(entity, directionForY(speedY[slot]), x, y)) {
            y = positionY[slot];
        }
        positionX[slot] = clampPosition ? clamp(x, 0x16, 0x89) : x & 0xFF;
        positionY[slot] = clampPosition ? clamp(y, 0x1E, 0x72) : y & 0xFF;
    }

    public static boolean isColorShellType(int type) {
        return type >= ENTITY_COLOR_SHELL_RED && type <= ENTITY_COLOR_SHELL_BLUE;
    }

    void decrementTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    private static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value & 0xFF));
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static int arithmeticShiftRight(int value) {
        return (signedByte(value) >> 1) & 0xFF;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int linkEntityX, int linkEntityY, int length) {
        return ColorShellMotion.vectorTowardsLinkBody(entityX, entityY, linkEntityX, linkEntityY, length);
    }

    static int unsignedDistance(int first, int second) {
        int difference = (first - second) & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    static int signedByte(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }

    public record Update(RoomEntity entity, int nextIgnoreHitsCountdown, boolean unloadRequested) {
        Update(RoomEntity entity) {
            this(entity, 0, false);
        }
    }

    record Vector(int x, int y) {}

    static ColorShellMotion.Update advanceBody(
            ColorShellMotion owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return owner.advance(new ColorShellFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                0,
                ColorShellWorld.none(),
                List.of(entity)));
    }

    static ColorShellMotion.Update advanceBody(
            ColorShellMotion owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int ignoreHitsCountdown) {
        return owner.advance(new ColorShellFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                ignoreHitsCountdown,
                ColorShellWorld.none(),
                List.of(entity)));
    }

    static ColorShellMotion.Update advanceBody(ColorShellMotion owner, ColorShellFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        int ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        ColorShellWorld world = inputs.world();
        java.util.List<RoomEntity> allEntities = inputs.allEntities();
        if (entity == null || randomByteSupplier == null) {
            throw new IllegalArgumentException("Color Shell motion inputs cannot be null");
        }
        int slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        owner.positionX[slot] = entity.x();
        owner.positionY[slot] = entity.y();
        owner.positionZ[slot] = entity.z();
        owner.decrementTransitionCountdown(slot);

        ColorShellWorld effects = world == null ? ColorShellWorld.none() : world;
        List<RoomEntity> entities = allEntities == null ? List.of(entity) : allEntities;
        int frame = frameCounter & 0xFF;
        int dispatchedState = owner.state[slot];
        int nextIgnoreHitsCountdown = ignoreHitsCountdown & 0xFF;
        boolean unloadRequested = false;
        switch (dispatchedState) {
            case 0 -> owner.advanceState0(linkEntityX, linkEntityY, randomByteSupplier, slot);
            case 1 -> owner.advanceState1(frame, linkEntityX, linkEntityY, backgroundCollision, slot, entity);
            case 2 -> owner.advanceState2(frame, ignoreHitsCountdown, slot);
            case 3 -> owner.advanceState3(frame, ignoreHitsCountdown, backgroundCollision, slot, entity);
            case 4 -> nextIgnoreHitsCountdown = owner.advanceState4(slot);
            case 5 -> nextIgnoreHitsCountdown = owner.advanceState5(entity, ignoreHitsCountdown, slot);
            case 6 -> owner.advanceState6(slot);
            case 7 -> owner.advanceState7(slot);
            case 8 -> owner.advanceState8(entity, effects, slot);
            case 9 -> owner.advanceState9(backgroundCollision, slot, entity);
            case 0x0A -> owner.advanceStateA(backgroundCollision, slot, entity);
            case 0x0B -> owner.advanceStateB(slot);
            case 0x0C -> owner.advanceStateC(entity, effects, entities, slot);
            case 0x0D -> unloadRequested = owner.advanceStateD(entity, effects, slot);
            default -> throw new IllegalStateException("Invalid Color Shell state: " + owner.state[slot]);
        }

        RoomEntity updated = EntitySnapshots.withMotion(
                entity,
                owner.positionX[slot],
                owner.positionY[slot],
                owner.spriteVariant[slot],
                entity.entityFlipAttribute(),
                owner.positionZ[slot],
                -1,
                false);
        return new ColorShellMotion.Update(updated, nextIgnoreHitsCountdown, unloadRequested);
    }

    static void advanceState1Body(
            ColorShellMotion owner,
            int frame,
            int linkEntityX,
            int linkEntityY,
            RoomEntityBackgroundCollision backgroundCollision,
            int slot,
            RoomEntity entity) {
        int currentDirection = owner.direction[slot] & 0x03;
        owner.speedX[slot] = SPEED_X_BY_DIRECTION[currentDirection];
        owner.speedY[slot] = SPEED_Y_BY_DIRECTION[currentDirection];
        owner.moveWithBackground(entity, backgroundCollision, true, slot);

        if (owner.transitionCountdown[slot] == 0) {
            owner.transitionCountdown[slot] = 0x10;
            owner.state[slot] = 0;
            if (ColorShellMotion.unsignedDistance(linkEntityX, owner.positionX[slot]) < 0x20
                    && ColorShellMotion.unsignedDistance(linkEntityY, owner.positionY[slot]) < 0x20) {
                ColorShellMotion.Vector vector = ColorShellMotion.vectorTowardsLink(
                        owner.positionX[slot], owner.positionY[slot], linkEntityX, linkEntityY, 0x0E);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.transitionCountdown[slot] = 0x20;
                owner.state[slot] = 2;
            }
        }
        if ((frame & 0x07) == 0) {
            owner.spriteVariant[slot] = (owner.spriteVariant[slot] + 1) & 0x01;
        }
    }

    static ColorShellMotion.Vector vectorTowardsLinkBody(
            int entityX, int entityY, int linkEntityX, int linkEntityY, int length) {
        int distanceX = ColorShellMotion.signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = ColorShellMotion.signedByte((linkEntityY - entityY) & 0xFF);
        boolean yIsLargerAxis = Math.abs(distanceY) > Math.abs(distanceX);
        int smallerDistance = Math.min(Math.abs(distanceX), Math.abs(distanceY));
        int largerDistance = Math.max(Math.abs(distanceX), Math.abs(distanceY));
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new ColorShellMotion.Vector(x & 0xFF, y & 0xFF);
    }

    static void advanceState9Body(
            ColorShellMotion owner, RoomEntityBackgroundCollision backgroundCollision, int slot, RoomEntity entity) {
        if (owner.transitionCountdown[slot] != 0) {
            return;
        }
        if (owner.speedX[slot] != 0) {
            owner.moveWithBackground(entity, backgroundCollision, false, slot);
        }
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        owner.positionZ[slot] = ColorShellMotion.addSpeedToPosition(
                owner.positionZ[slot], owner.speedZ[slot], owner.speedZAccumulator, slot);
        if ((owner.positionZ[slot] & 0x80) != 0) {
            owner.positionZ[slot] = 0;
            owner.speedZ[slot] = 0x08;
            owner.speedX[slot] = ColorShellMotion.arithmeticShiftRight(owner.speedX[slot]);
            owner.state[slot] = 0x0A;
        }
    }

    static void advanceStateABody(
            ColorShellMotion owner, RoomEntityBackgroundCollision backgroundCollision, int slot, RoomEntity entity) {
        owner.moveWithBackground(entity, backgroundCollision, false, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        owner.positionZ[slot] = ColorShellMotion.addSpeedToPosition(
                owner.positionZ[slot], owner.speedZ[slot], owner.speedZAccumulator, slot);
        if ((owner.positionZ[slot] & 0x80) != 0) {
            owner.transitionCountdown[slot] = 0x20;
            owner.state[slot]++;
        }
    }

    static void advanceStateBBody(ColorShellMotion owner, int slot) {
        if (owner.transitionCountdown[slot] != 0) {
            return;
        }
        owner.transitionCountdown[slot] = 0;
        owner.speedZ[slot] = 0;
        owner.speedX[slot] = 0;
        owner.state[slot] = 1;
        owner.physicsFlags[slot] &= 0x7F;
    }

    static void advanceStateCBody(
            ColorShellMotion owner, RoomEntity entity, ColorShellWorld world, List<RoomEntity> allEntities, int slot) {
        for (RoomEntity other : allEntities) {
            if (other == null
                    || !ColorShellMotion.isColorShellType(other.type())
                    || !other.loaded()
                    || other.status().value() == 0
                    || owner.state[other.slot()] >= 0x0C) {
                continue;
            }
            return;
        }
        int color = entity.type() - ColorShellMotion.ENTITY_COLOR_SHELL_RED;
        owner.transitionCountdown[slot] = 0x18;
        owner.state[slot] = 0x0D;
        world.writeObject(entity, 0x67 + color);
    }

    static boolean advanceStateDBody(ColorShellMotion owner, RoomEntity entity, ColorShellWorld world, int slot) {
        if (owner.transitionCountdown[slot] != 0) {
            return false;
        }
        int color = entity.type() - ColorShellMotion.ENTITY_COLOR_SHELL_RED;
        world.writeObject(entity, ColorShellMotion.PUZZLE_FINAL_OBJECTS[color]);
        world.spawnPoof(owner.positionX[slot], (owner.positionY[slot] - owner.positionZ[slot]) & 0xFF);
        return true;
    }

    public record ColorShellFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int ignoreHitsCountdown,
            ColorShellWorld world,
            java.util.List<RoomEntity> allEntities) {}
}
