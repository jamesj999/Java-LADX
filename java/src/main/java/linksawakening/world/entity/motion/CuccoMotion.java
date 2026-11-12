package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityCombatRules;

/** Bank-$05 CuccoEntityHandler flight, lift, and angry-spawn state. */
public final class CuccoMotion {
    static final int ENTITY_TYPE = 0x6C;
    public static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10 | 0x80;
    public static final int ANGRY_PHYSICS_FLAGS = 0x02 | 0x10;
    public static final int ANGRY_HITBOX_FLAGS = 0x80;
    public static final int ANGRY_OPTIONS1 = 0x40;
    public static final int HEALTH_OVERRIDE = 0x4C;
    public static final int CUCCO_HURT_WAVE_SFX = 0x13;
    public static final int LIFT_WAVE_SFX = 0x02;

    private static final int[] SPEED_BY_RANDOM = {0x00, 0x04, 0x06, 0x04, 0x00, 0xFC, 0xFA, 0xFC};
    private static final int[] SPAWN_X = {
        0x28, 0x48, 0x68, 0x88, 0x18, 0x38, 0x58, 0x78,
        0x00, 0x00, 0x00, 0x00, 0xA0, 0xA0, 0xA0, 0xA0
    };
    private static final int[] SPAWN_Y = {
        0x00, 0x00, 0x00, 0x00, 0x90, 0x90, 0x90, 0x90,
        0x20, 0x40, 0x60, 0x80, 0x20, 0x40, 0x60, 0x80
    };

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record SpawnRequest(int x, int y, int z, int speedX, int speedY) {
        public SpawnRequest {
            x &= 0xFF;
            y &= 0xFF;
            z &= 0xFF;
            speedX &= 0xFF;
            speedY &= 0xFF;
        }
    }

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int flashCountdown,
            boolean liftRequested,
            boolean unloaded,
            boolean boomerangSound,
            int waveSoundId,
            int dialogTableId,
            int dialogLowId,
            SpawnRequest spawnRequest) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public void initializeAngry(int slot, int newSpeedX, int newSpeedY) {
        initialize(slot);
        state[slot] = 3;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    /**
     * Advances one active Cucco handler pass. Countdown and flash values are
     * the bytes after the shared entity-loop decrement, matching the source
     * call order in AnimateEntities.
     */
    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            int flashCountdown,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            boolean linkInteractive,
            boolean powerBraceletHeld,
            boolean linkAttackStepActive,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean indoorRoom,
            boolean marinFollowing,
            int transitionSequenceCounter) {
        return advance(
                entity,
                frameCounter,
                transitionCountdown,
                flashCountdown,
                linkEntityX,
                linkEntityY,
                linkZ,
                linkInteractive,
                powerBraceletHeld,
                linkAttackStepActive,
                randomByteSupplier,
                backgroundCollision,
                indoorRoom,
                marinFollowing,
                transitionSequenceCounter,
                true);
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            int flashCountdown,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            boolean linkInteractive,
            boolean powerBraceletHeld,
            boolean linkAttackStepActive,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean indoorRoom,
            boolean marinFollowing,
            int transitionSequenceCounter,
            boolean angrySpawnSlotAvailable) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Cucco motion entity");
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Cucco random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int frame = frameCounter & 0xFF;
        int countdown = transitionCountdown & 0xFF;
        int nextFlashCountdown = flashCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int z = entity.z() & 0xFF;
        int variant = spriteVariant[slot];
        int renderedVariant = variant + (direction[slot] == 0 ? 2 : 0);
        boolean unloaded = false;
        boolean boomerangSound = false;
        int waveSoundId = -1;
        int dialogTableId = -1;
        int dialogLowId = -1;
        SpawnRequest spawnRequest = null;

        // $05:452A-453B mirrors only hActiveEntitySpriteVariant for the
        // current draw. SetEntitySpriteVariant later writes the next frame's
        // unmirrored WRAM variant; keep the two values separate.

        boolean landed = false;
        if (state[slot] != 3) {
            z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
            speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                speedZ[slot] = 0;
            }
        }

        if (nextFlashCountdown != 0) {
            if (nextFlashCountdown == 0x08 && marinFollowing) {
                nextFlashCountdown--;
                if ((transitionSequenceCounter & 0xFF) == 0x04) {
                    if ((randomByteSupplier.getAsInt() & 0x3F) == 0) {
                        dialogTableId = 0x02;
                        dialogLowId = 0x76;
                    } else {
                        dialogTableId = 0x00;
                        dialogLowId = 0x8F;
                    }
                }
            }
            if (privateState1[slot] != 0x23) {
                privateState1[slot] = (privateState1[slot] + 1) & 0xFF;
            }
            state[slot] = 2;
        }

        RoomEntity collisionEntity = new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                x,
                y,
                entity.status(),
                entity.spriteDefinition(),
                variant,
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                z,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
        boolean linkCollision = linkInteractive
                && (linkZ & 0xFF) == 0
                && state[slot] != 3
                && RoomEntityCombatRules.overlapsLink(collisionEntity, linkEntityX, linkEntityY);
        if (linkCollision && !linkAttackStepActive && powerBraceletHeld) {
            RoomEntity updated = withPosition(entity, x, y, z, renderedVariant);
            return new Update(
                    updated, countdown, nextFlashCountdown, true, false, false, -1, dialogTableId, dialogLowId, null);
        }

        switch (state[slot]) {
            case 0 -> {
                variant = 0;
                if (countdown == 0) {
                    int random = randomByteSupplier.getAsInt() & 0x07;
                    speedX[slot] = SPEED_BY_RANDOM[random];
                    direction[slot] = random & 0x04;
                    speedY[slot] = SPEED_BY_RANDOM[randomByteSupplier.getAsInt() & 0x07];
                    countdown = 0x30 + (randomByteSupplier.getAsInt() & 0x1F);
                    state[slot] = 1;
                }
            }
            case 1 -> {
                Position moved = move(entity, x, y, backgroundCollision);
                x = moved.x();
                y = moved.y();
                if (landed) {
                    if (countdown == 0) {
                        countdown = 0x30;
                        state[slot] = 0;
                        variant = withUnsignedByte(variant);
                        break;
                    }
                    speedZ[slot] = 0x05;
                    z = (z + 1) & 0xFF;
                }
                variant = (frame >>> 3) & 0x01;
            }
            case 2 -> {
                if ((((frame ^ slot) & 0x1F) == 0) && z == 0) {
                    Vector vector = vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x0C);
                    speedY[slot] = negate(vector.y());
                    speedX[slot] = negate(vector.x());
                }
                Position moved = move(entity, x, y, backgroundCollision);
                x = moved.x();
                y = moved.y();
                variant = (frame >>> 1) & 0x01;
                direction[slot] = signedByte((linkEntityX - x) & 0xFF) < 0 ? 0 : 1;
                if (privateState1[slot] == 0x23 && !indoorRoom && (frame & 0x0F) == 0 && angrySpawnSlotAvailable) {
                    int spawnIndex = randomByteSupplier.getAsInt() & 0x0F;
                    int spawnX = SPAWN_X[spawnIndex];
                    int spawnY = SPAWN_Y[spawnIndex];
                    Vector spawnVector = vectorTowardsLink(spawnX, spawnY, 0x10, linkEntityX, linkEntityY, 0x18);
                    spawnRequest = new SpawnRequest(spawnX, spawnY, 0x10, spawnVector.x(), spawnVector.y());
                }
            }
            case 3 -> {
                Position moved = move(entity, x, y, null);
                x = moved.x();
                y = moved.y();
                if (x >= 0xA9 || ((y - z) & 0xFF) >= 0x91) {
                    unloaded = true;
                }
                variant = (frame >>> 1) & 0x01;
                direction[slot] = signedByte(speedX[slot]) < 0 ? 1 : 0;
                boomerangSound = true;
            }
            default -> throw new IllegalStateException("Invalid Cucco state: " + state[slot]);
        }

        spriteVariant[slot] = variant & 1;
        RoomEntity updated = withPosition(entity, x, y, z, renderedVariant);
        if (spawnRequest != null) {
            waveSoundId = CUCCO_HURT_WAVE_SFX;
        }
        return new Update(
                updated,
                countdown,
                nextFlashCountdown,
                false,
                unloaded,
                boomerangSound,
                waveSoundId,
                dialogTableId,
                dialogLowId,
                spawnRequest);
    }

    public void clear(int slot) {
        state[slot] = 0;
        spriteVariant[slot] = 0;
        direction[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    private Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null
                && nextX != x
                && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
        }
        if (backgroundCollision != null
                && nextY != y
                && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
        }
        return new Position(nextX, nextY);
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z, int variant) {
        return new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                x & 0xFF,
                y & 0xFF,
                entity.status(),
                entity.spriteDefinition(),
                variant,
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                z & 0xFF,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smallerDistance, largerDistance);
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
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum > 0xFF || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    private static int negate(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private static int withUnsignedByte(int value) {
        return value & 0xFF;
    }

    void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    private record Position(int x, int y) {}

    private record Vector(int x, int y) {}
}
