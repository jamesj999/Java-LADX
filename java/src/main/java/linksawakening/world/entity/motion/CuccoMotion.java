package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
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

    static final int[] SPEED_BY_RANDOM = {0x00, 0x04, 0x06, 0x04, 0x00, 0xFC, 0xFA, 0xFC};
    static final int[] SPAWN_X = {
        0x28, 0x48, 0x68, 0x88, 0x18, 0x38, 0x58, 0x78,
        0x00, 0x00, 0x00, 0x00, 0xA0, 0xA0, 0xA0, 0xA0
    };
    static final int[] SPAWN_Y = {
        0x00, 0x00, 0x00, 0x00, 0x90, 0x90, 0x90, 0x90,
        0x20, 0x40, 0x60, 0x80, 0x20, 0x40, 0x60, 0x80
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState1Table: flashed-frame buildup saturating at $23 before angry spawns.
    // ROM wEntitiesPrivateState1Table.
    private final int[] angerBuildup = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
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
            /* Optional emitted spawn request; absent when no spawn is due. */ SpawnRequest spawnRequest) {}

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
    public Update advance(CuccoFrame inputs) {
        return CuccoMotion.advanceBody(this, inputs);
    }

    public Update advance(CuccoInteractionFrame inputs) {
        return CuccoMotion.advanceBody(this, inputs);
    }

    public void clear(int slot) {
        state[slot] = 0;
        spriteVariant[slot] = 0;
        direction[slot] = 0;
        angerBuildup[slot] = 0;
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

    int angerBuildup(int slot) {
        return angerBuildup[slot];
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

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (nextX != x && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
        }
        if (nextY != y && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
        }
        return new Position(nextX, nextY);
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                variant,
                entity.entityFlipAttribute(),
                z & 0xFF,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
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

    static int negate(int value) {
        return -signedByte(value) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static int withUnsignedByte(int value) {
        return value & 0xFF;
    }

    void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    record Position(int x, int y) {}

    record Vector(int x, int y) {}

    private final CuccoStep cuccoStep = new CuccoStep(this);

    static CuccoMotion.Update advanceBody(CuccoMotion owner, CuccoFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int transitionCountdown = inputs.transitionCountdown();
        int flashCountdown = inputs.flashCountdown();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkZ = inputs.linkZ();
        boolean linkInteractive = inputs.linkInteractive();
        boolean powerBraceletHeld = inputs.powerBraceletHeld();
        boolean linkAttackStepActive = inputs.linkAttackStepActive();
        IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        boolean indoorRoom = inputs.indoorRoom();
        boolean marinFollowing = inputs.marinFollowing();
        int transitionSequenceCounter = inputs.transitionSequenceCounter();
        return owner.advance(new CuccoInteractionFrame(
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
                true));
    }

    static CuccoMotion.Update advanceBody(CuccoMotion owner, CuccoInteractionFrame inputs) {
        return owner.cuccoStep.run(inputs);
    }

    record CuccoFrame(
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
            int transitionSequenceCounter) {}

    public record CuccoInteractionFrame(
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
        public CuccoInteractionFrame {
            backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        }
    }

    static final class CuccoStep {
        private final linksawakening.world.entity.motion.CuccoMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.CuccoMotion.CuccoInteractionFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int transitionCountdown;
        private int flashCountdown;
        private int linkEntityX;
        private int linkEntityY;
        private int linkZ;
        private boolean linkInteractive;
        private boolean powerBraceletHeld;
        private boolean linkAttackStepActive;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private boolean indoorRoom;
        private boolean marinFollowing;
        private int transitionSequenceCounter;
        private boolean angrySpawnSlotAvailable;
        private int slot;
        private int frame;
        private int countdown;
        private int nextFlashCountdown;
        private int x;
        private int y;
        private int z;
        private int variant;
        private int renderedVariant;
        private boolean unloaded;
        private boolean boomerangSound;
        private int waveSoundId;
        private int dialogTableId;
        private int dialogLowId;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional emitted spawn request; absent when no spawn is due. */ private linksawakening.world.entity.motion
                        .CuccoMotion.SpawnRequest
                spawnRequest;
        private boolean landed;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity collisionEntity;
        private boolean linkCollision;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        CuccoStep(linksawakening.world.entity.motion.CuccoMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.CuccoMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceHeightAndAnimation()) return completedResult;
            if (!advanceBehavior()) return completedResult;

            return new CuccoMotion.Update(
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

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.CuccoMotion.Update completedResult;

        private boolean advanceState0() {
            variant = 0;
            if (countdown == 0) {
                int random = randomByteSupplier.getAsInt() & 0x07;
                owner.speedX[slot] = SPEED_BY_RANDOM[random];
                owner.direction[slot] = random & 0x04;
                owner.speedY[slot] = SPEED_BY_RANDOM[randomByteSupplier.getAsInt() & 0x07];
                countdown = 0x30 + (randomByteSupplier.getAsInt() & 0x1F);
                owner.state[slot] = 1;
            }

            return true;
        }

        private boolean advanceState1() {
            CuccoMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            if (landed) {
                if (countdown == 0) {
                    countdown = 0x30;
                    owner.state[slot] = 0;
                    variant = CuccoMotion.withUnsignedByte(variant);
                    return true;
                }
                owner.speedZ[slot] = 0x05;
                z = (z + 1) & 0xFF;
            }
            variant = (frame >>> 3) & 0x01;

            return true;
        }

        private boolean advanceState2() {
            if ((((frame ^ slot) & 0x1F) == 0) && z == 0) {
                CuccoMotion.Vector vector = CuccoMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x0C);
                owner.speedY[slot] = CuccoMotion.negate(vector.y());
                owner.speedX[slot] = CuccoMotion.negate(vector.x());
            }
            CuccoMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            variant = (frame >>> 1) & 0x01;
            owner.direction[slot] = CuccoMotion.signedByte((linkEntityX - x) & 0xFF) < 0 ? 0 : 1;
            if (owner.angerBuildup[slot] == 0x23 && !indoorRoom && (frame & 0x0F) == 0 && angrySpawnSlotAvailable) {
                int spawnIndex = randomByteSupplier.getAsInt() & 0x0F;
                int spawnX = SPAWN_X[spawnIndex];
                int spawnY = SPAWN_Y[spawnIndex];
                CuccoMotion.Vector spawnVector =
                        CuccoMotion.vectorTowardsLink(spawnX, spawnY, 0x10, linkEntityX, linkEntityY, 0x18);
                spawnRequest = new CuccoMotion.SpawnRequest(spawnX, spawnY, 0x10, spawnVector.x(), spawnVector.y());
            }

            return true;
        }

        private boolean advanceState3() {
            CuccoMotion.Position moved = owner.move(entity, x, y, RoomEntityBackgroundCollision.OPEN);
            x = moved.x();
            y = moved.y();
            if (x >= 0xA9 || ((y - z) & 0xFF) >= 0x91) {
                unloaded = true;
            }
            variant = (frame >>> 1) & 0x01;
            owner.direction[slot] = CuccoMotion.signedByte(owner.speedX[slot]) < 0 ? 1 : 0;
            boomerangSound = true;

            return true;
        }

        private boolean prepareFrame() {

            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            transitionCountdown = inputs.transitionCountdown();
            flashCountdown = inputs.flashCountdown();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkZ = inputs.linkZ();
            linkInteractive = inputs.linkInteractive();
            powerBraceletHeld = inputs.powerBraceletHeld();
            linkAttackStepActive = inputs.linkAttackStepActive();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();
            indoorRoom = inputs.indoorRoom();
            marinFollowing = inputs.marinFollowing();
            transitionSequenceCounter = inputs.transitionSequenceCounter();
            angrySpawnSlotAvailable = inputs.angrySpawnSlotAvailable();
            if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
                throw new IllegalArgumentException("Unsupported Cucco motion entity");
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Cucco random-byte supplier cannot be null");
            }

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            frame = frameCounter & 0xFF;
            countdown = transitionCountdown & 0xFF;
            nextFlashCountdown = flashCountdown & 0xFF;
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;
            z = entity.z() & 0xFF;
            variant = owner.spriteVariant[slot];
            renderedVariant = variant + (owner.direction[slot] == 0 ? 2 : 0);
            unloaded = false;
            boomerangSound = false;
            waveSoundId = -1;
            dialogTableId = -1;
            dialogLowId = -1;
            spawnRequest = null;

            // $05:452A-453B mirrors only hActiveEntitySpriteVariant for the
            // current draw. SetEntitySpriteVariant later writes the next frame's
            // unmirrored WRAM variant; keep the two values separate.

            landed = false;
            return true;
        }

        private boolean advanceHeightAndAnimation() {

            if (owner.state[slot] != 3) {
                z = CuccoMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
                owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
                landed = (z & 0x80) != 0;
                if (landed) {
                    z = 0;
                    owner.speedZ[slot] = 0;
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
                if (owner.angerBuildup[slot] != 0x23) {
                    owner.angerBuildup[slot] = (owner.angerBuildup[slot] + 1) & 0xFF;
                }
                owner.state[slot] = 2;
            }

            collisionEntity = EntitySnapshots.withMotion(
                    entity,
                    x,
                    y,
                    variant,
                    entity.entityFlipAttribute(),
                    z,
                    entity.deathSpriteVariant(),
                    entity.powerRecoilDeath());
            linkCollision = linkInteractive
                    && (linkZ & 0xFF) == 0
                    && owner.state[slot] != 3
                    && RoomEntityCombatRules.overlapsLink(collisionEntity, linkEntityX, linkEntityY);
            if (linkCollision && !linkAttackStepActive && powerBraceletHeld) {
                RoomEntity updated = CuccoMotion.withPosition(entity, x, y, z, renderedVariant);

                completedResult = new CuccoMotion.Update(
                        updated,
                        countdown,
                        nextFlashCountdown,
                        true,
                        false,
                        false,
                        -1,
                        dialogTableId,
                        dialogLowId,
                        null);
                return false;
            }
            return true;
        }

        private boolean advanceBehavior() {

            switch (owner.state[slot]) {
                case 0 -> {
                    if (!advanceState0()) return false;
                }
                case 1 -> {
                    if (!advanceState1()) return false;
                }
                case 2 -> {
                    if (!advanceState2()) return false;
                }
                case 3 -> {
                    if (!advanceState3()) return false;
                }
                default -> throw new IllegalStateException("Invalid Cucco state: " + owner.state[slot]);
            }

            owner.spriteVariant[slot] = variant & 1;
            updated = CuccoMotion.withPosition(entity, x, y, z, renderedVariant);
            if (spawnRequest != null) {
                waveSoundId = CUCCO_HURT_WAVE_SFX;
            }
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.CuccoMotion.Update run(
                linksawakening.world.entity.motion.CuccoMotion.CuccoInteractionFrame inputs) {
            if (inUse) {
                return new CuccoStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            transitionCountdown = 0;
            flashCountdown = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            linkZ = 0;
            linkInteractive = false;
            powerBraceletHeld = false;
            linkAttackStepActive = false;
            randomByteSupplier = null;
            backgroundCollision = null;
            indoorRoom = false;
            marinFollowing = false;
            transitionSequenceCounter = 0;
            angrySpawnSlotAvailable = false;
            slot = 0;
            frame = 0;
            countdown = 0;
            nextFlashCountdown = 0;
            x = 0;
            y = 0;
            z = 0;
            variant = 0;
            renderedVariant = 0;
            unloaded = false;
            boomerangSound = false;
            waveSoundId = 0;
            dialogTableId = 0;
            dialogLowId = 0;
            spawnRequest = null;
            landed = false;
            collisionEntity = null;
            linkCollision = false;
            updated = null;
            completedResult = null;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.spawnRequest = null;
                this.collisionEntity = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }

    int[] state() {
        return this.state;
    }

    int[] angerBuildup() {
        return this.angerBuildup;
    }

    int[] speedX() {
        return this.speedX;
    }

    int[] speedY() {
        return this.speedY;
    }

    int[] speedZ() {
        return this.speedZ;
    }

    int[] speedXAccumulator() {
        return this.speedXAccumulator;
    }

    int[] speedYAccumulator() {
        return this.speedYAccumulator;
    }

    int[] speedZAccumulator() {
        return this.speedZAccumulator;
    }
}
