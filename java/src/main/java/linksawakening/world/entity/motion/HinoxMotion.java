package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$06 {@code HinoxEntityHandler} state machine for entity {@code $89}. */
public final class HinoxMotion {
    public static final int ENTITY_TYPE = 0x89;
    static final int ENTITY_BOMB = 0x02;

    static final int STATE_INITIAL = 0;
    static final int STATE_WANDER = 1;
    static final int STATE_CHARGE_WINDUP = 2;
    static final int STATE_CHARGE = 3;
    public static final int STATE_GRAB = 4;
    static final int STATE_BOMB = 5;

    static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    static final int[] GRAB_LINK_X_OFFSETS = {0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xF3, 0xF7, 0xFB, 0x00};
    static final int[] GRAB_LINK_Z = {0x15, 0x15, 0x15, 0x15, 0x15, 0x14, 0x14, 0x14, 0x10, 0x08, 0x04, 0x00};
    static final int[] GRAB_BODY_VARIANT = {0, 0, 0, 0, 1, 1, 1, 1, 0, 0};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] entityCollisionFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bombOriginX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bombOriginY = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] bombOriginWritten = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record BombSpawn(
            int type, int x, int y, int z, int speedX, int speedY, int speedZ, int transitionCountdown) {}

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int spriteVariant,
            boolean linkMotionBlocked,
            int heldLinkX,
            int heldLinkY,
            int heldLinkZ,
            int linkSpeedX,
            int linkSpeedY,
            int linkVelocityZ,
            int linkAirborneState,
            int linkDamage,
            int jingleId,
            int waveId,
            boolean dustRequested,
            boolean heldLinkPositionWritten,
            boolean applyHeldLinkPose,
            BombSpawn bombSpawn) {}

    public void initialize(int slot) {
        validateSlot(slot);
        state[slot] = STATE_INITIAL;
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        inertia[slot] = 0;
        privateCountdown1[slot] = 0;
        entityCollisionFlags[slot] = 0;
        bombOriginX[slot] = 0;
        bombOriginY[slot] = 0;
        bombOriginWritten[slot] = false;
        initialized[slot] = true;
    }

    public Update advance(HinoxFrame inputs) {
        return HinoxMotion.advanceBody(this, inputs);
    }

    Update update(HinoxResult inputs) {
        return HinoxMotion.updateBody(this, inputs);
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundInteraction background, int slot) {
        return HinoxMotion.moveBody(this, entity, x, y, background, slot);
    }

    int tickInertia(int slot) {
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        return (inertia[slot] >>> 4) & 0x01;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        validateSlot(slot);
        return state[slot];
    }

    public int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot] & 0xFF;
    }

    int inertia(int slot) {
        validateSlot(slot);
        return inertia[slot] & 0xFF;
    }

    int privateCountdown1(int slot) {
        validateSlot(slot);
        return privateCountdown1[slot] & 0xFF;
    }

    public void decrementPrivateCountdown1(int slot) {
        validateSlot(slot);
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
    }

    public int collisionFlags(int slot) {
        validateSlot(slot);
        return entityCollisionFlags[slot] & 0xFF;
    }

    static Vector vectorTowardsLink(int x, int y, int entityZ, int linkX, int linkY, int length) {
        int dx = signedByte(linkX - x);
        int dy = signedByte(linkY - y + entityZ);
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int major = Math.max(ax, ay);
        int minor = Math.min(ax, ay);
        int small = major == 0 ? length : divide(minor, major, length);
        int vx = ax >= ay ? length : small;
        int vy = ay > ax ? length : small;
        if (dx < 0) vx = -vx;
        if (dy < 0) vy = -vy;
        return new Vector(vx & 0xFF, vy & 0xFF);
    }

    private static int divide(int minor, int major, int length) {
        int result = 0;
        int remainder = 0;
        for (int i = 0; i < length; i++) {
            int sum = remainder + minor;
            if (sum >= major) {
                sum -= major;
                result++;
            }
            remainder = sum;
        }
        return result;
    }

    static boolean withinDistance(int value, int target, int distance) {
        int delta = (value - target) & 0xFF;
        int absolute = delta < 0x80 ? delta : 0x100 - delta;
        return absolute < distance;
    }

    static int directionToLink(int x, int y, int linkX, int linkY) {
        int dx = signedByte(linkX - x);
        int dy = signedByte(linkY - y);
        if (Math.abs(dy) >= Math.abs(dx)) {
            return dy < 0 ? 2 : 3;
        }
        return dx < 0 ? 1 : 0;
    }

    static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) return position & 0xFF;
        int sum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = sum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (sum > 0xFF) delta++;
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Hinox slot out of range: " + slot);
        }
    }

    record Vector(int x, int y) {}

    record Position(int x, int y) {}

    final HinoxStep hinoxStep = new HinoxStep();

    static HinoxMotion.Update advanceBody(HinoxMotion owner, HinoxFrame inputs) {
        return owner.hinoxStep.run(owner, inputs);
    }

    static HinoxMotion.Position moveBody(
            HinoxMotion owner, RoomEntity entity, int x, int y, RoomEntityBackgroundInteraction background, int slot) {
        owner.entityCollisionFlags[slot] = 0;
        int nextX = HinoxMotion.addSpeed(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        if (nextX != x && background != null) {
            EntityBackgroundCollisionResult result =
                    background.probe(entity, HinoxMotion.horizontalDirection(owner.speedX[slot]), nextX, y);
            if (result.blocked()) {
                owner.entityCollisionFlags[slot] |= result.collisionFlag();
                nextX = x;
            }
        }
        int nextY = HinoxMotion.addSpeed(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        if (nextY != y && background != null) {
            EntityBackgroundCollisionResult result =
                    background.probe(entity, HinoxMotion.verticalDirection(owner.speedY[slot]), nextX, nextY);
            if (result.blocked()) {
                owner.entityCollisionFlags[slot] |= result.collisionFlag();
                nextY = y;
            }
        }
        return new HinoxMotion.Position(nextX, nextY);
    }

    public record HinoxFrame(
            RoomEntity entity,
            int transitionCountdown,
            int frameCounter,
            int linkX,
            int linkY,
            int linkZ,
            int linkMotionState,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int entityCollisionFlagsForFrame,
            int flashCountdown) {}

    static final class HinoxStep {
        private HinoxMotion owner;
        private HinoxFrame inputs;
        private linksawakening.world.entity.RoomEntity entity;
        private int transitionCountdown;
        private int frameCounter;
        private int linkX;
        private int linkY;
        private int linkZ;
        private int linkMotionState;
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int entityCollisionFlagsForFrame;
        private int flashCountdown;
        private int slot;
        private boolean linkMotionBlockedAtEntry;
        private int x;
        private int y;
        private int transition;
        private int frame;
        private int linkSpeedX;
        private int linkSpeedY;
        private int linkVelocityZ;
        private int airborne;
        private int linkDamage;
        private int jingle;
        private int wave;
        private boolean dust;
        private HinoxMotion.BombSpawn bomb;
        private int spriteVariant;

        HinoxStep() {}

        private HinoxMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!applyFlashBombTransition()) return completedResult;
            if (!advanceBehavior()) return completedResult;

            return result(spriteVariant, linkMotionBlockedAtEntry, linkX, linkY, 0, false, false);
        }

        private HinoxMotion.Update completedResult;

        private boolean advanceInitial() {
            if (((owner.entityCollisionFlags[slot] | entityCollisionFlagsForFrame) & 0x0F) != 0 || transition == 0) {
                transition = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
                owner.state[slot] = STATE_WANDER;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
            }
            HinoxMotion.Position next = owner.move(entity, x, y, backgroundInteraction, slot);
            x = next.x();
            y = next.y();
            spriteVariant = owner.tickInertia(slot);

            return true;
        }

        private boolean advanceWander() {
            if (transition == 0) {
                owner.privateState3[slot] = (owner.privateState3[slot] + 1) & 0xFF;
                if (owner.privateState3[slot] == 2) {
                    owner.privateState3[slot] = 0;
                    if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                        owner.state[slot] = STATE_CHARGE_WINDUP;
                        transition = 0x30;
                        owner.speedX[slot] = 0;
                        owner.speedY[slot] = 0;
                        return true;
                    }
                }
                transition = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                owner.state[slot] = STATE_WANDER;
                owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0x03;
                int direction = owner.privateState1[slot] == 0
                        ? HinoxMotion.directionToLink(x, y, linkX, linkY)
                        : randomByteSupplier.getAsInt() & 0x03;
                owner.speedX[slot] = SPEED_X_BY_DIRECTION[direction];
                owner.speedY[slot] = SPEED_Y_BY_DIRECTION[direction];
            }
            spriteVariant = entity.spriteVariant();

            return true;
        }

        private boolean advanceChargeWindup() {
            if (transition == 0) {
                transition = 0x20;
                owner.state[slot] = STATE_CHARGE;
                HinoxMotion.Vector vector = HinoxMotion.vectorTowardsLink(x, y, entity.z(), linkX, linkY, 0x18);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
            }
            // This branch advances inertia twice in the ROM; preserve both calls.
            owner.tickInertia(slot);
            spriteVariant = owner.tickInertia(slot);
            if ((frame & 0x0F) == 0) {
                jingle = 0x20;
            }

            return true;
        }

        private boolean advanceCharge() {
            if (transition == 0) {
                owner.state[slot] = STATE_GRAB;
                transition = 0x4F;
                wave = 0x16;
            }
            HinoxMotion.Position next = owner.move(entity, x, y, backgroundInteraction, slot);
            x = next.x();
            y = next.y();
            if (owner.state[slot] == STATE_CHARGE
                    && HinoxMotion.withinDistance(x, linkX, 0x18)
                    && HinoxMotion.withinDistance(y, linkY, 0x18)
                    && (linkMotionState & 0xFF) == 0) {
                owner.state[slot] = STATE_GRAB;
                transition = 0x4F;
                jingle = 0x16;
            }
            dust = (frame & 0x07) == 0;
            if (dust) {
                // State 3 writes hMultiPurpose0/1 for the later bomb
                // handler. Preserve those source registers per entity.
                owner.bombOriginX[slot] = x & 0xFF;
                owner.bombOriginY[slot] = (y + 0x0A) & 0xFF;
                owner.bombOriginWritten[slot] = true;
            }
            // This branch advances inertia twice in the ROM; preserve both calls.
            owner.tickInertia(slot);
            spriteVariant = owner.tickInertia(slot);
            if ((frame & 0x0F) == 0) {
                jingle = 0x20;
            }

            return true;
        }

        private boolean advanceGrab() {
            if (transition == 0) {
                owner.state[slot] = STATE_INITIAL;
                transition = 0;
                completedResult = result(spriteVariant, linkMotionBlockedAtEntry, linkX, linkY, linkZ, false, false);
                return false;
            } else if (transition == 0x20) {
                linkSpeedY = 0x20;
                linkSpeedX = (linkX & 0xFF) >= 0x50 ? 0xE0 : 0x20;
                linkVelocityZ = 0x10;
                airborne = 0x02;
                linkDamage = 0x08;
                jingle = 0x08;
                linkX = x;
                linkY = y;
                owner.privateCountdown1[slot] = 0x50;
                {
                    completedResult =
                            result(entity.spriteVariant(), linkMotionBlockedAtEntry, linkX, linkY, linkZ, true, false);
                    return false;
                }
            }
            return holdGrabbedLink();
        }

        private boolean advanceBomb() {
            if (transition == 0) {
                owner.state[slot] = STATE_INITIAL;
            } else if (transition == 0x10) {
                int originX = owner.bombOriginWritten[slot] ? owner.bombOriginX[slot] : x;
                int originY = owner.bombOriginWritten[slot] ? owner.bombOriginY[slot] : y;
                HinoxMotion.Vector vector =
                        HinoxMotion.vectorTowardsLink((originX - 0x0C) & 0xFF, originY, 0x10, linkX, linkY, 0x10);
                bomb = new HinoxMotion.BombSpawn(
                        ENTITY_BOMB, (originX - 0x0C) & 0xFF, originY, 0x10, vector.x(), vector.y(), 0x18, 0x40);
                jingle = 0x08;
            }
            spriteVariant = transition >= 0x10 ? 1 : 0;

            return true;
        }

        private boolean advanceDefault() {
            owner.state[slot] = STATE_INITIAL;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            spriteVariant = 0;

            return true;
        }

        private boolean prepareFrame() {

            entity = inputs.entity();
            transitionCountdown = inputs.transitionCountdown();
            frameCounter = inputs.frameCounter();
            linkX = inputs.linkX();
            linkY = inputs.linkY();
            linkZ = inputs.linkZ();
            linkMotionState = inputs.linkMotionState();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundInteraction = inputs.backgroundInteraction();
            entityCollisionFlagsForFrame = inputs.entityCollisionFlagsForFrame();
            flashCountdown = inputs.flashCountdown();
            HinoxMotion.validateSlot(entity.slot());
            if (!owner.initialized[entity.slot()]) {
                owner.initialize(entity.slot());
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Hinox random source cannot be null");
            }

            slot = entity.slot();
            linkMotionBlockedAtEntry = owner.privateCountdown1[slot] != 0;
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;
            transition = transitionCountdown & 0xFF;
            frame = frameCounter & 0xFF;
            linkSpeedX = 0;
            linkSpeedY = 0;
            linkVelocityZ = 0;
            airborne = 0;
            linkDamage = 0;
            jingle = -1;
            wave = -1;
            dust = false;
            bomb = null;
            spriteVariant = entity.spriteVariant() < 0 ? 0 : entity.spriteVariant();
            return true;
        }

        private boolean applyFlashBombTransition() {

            // HinoxEntityHandler performs this branch before selecting a private
            // state. A sword flash at exactly $03 is the ROM's bomb transition.
            if (owner.state[slot] < STATE_GRAB && (flashCountdown & 0xFF) == 0x03) {
                owner.state[slot] = STATE_BOMB;
                transition = 0x20;
                completedResult = result(spriteVariant, linkMotionBlockedAtEntry, linkX, linkY, linkZ, false, false);
                return false;
            }
            return true;
        }

        private boolean advanceBehavior() {

            return switch (owner.state[slot]) {
                case STATE_INITIAL -> advanceInitial();
                case STATE_WANDER -> advanceWander();
                case STATE_CHARGE_WINDUP -> advanceChargeWindup();
                case STATE_CHARGE -> advanceCharge();
                case STATE_GRAB -> advanceGrab();
                case STATE_BOMB -> advanceBomb();
                default -> advanceDefault();
            };
        }

        private boolean holdGrabbedLink() {
            boolean blocked = linkMotionBlockedAtEntry;
            int heldX = linkX;
            int heldY = linkY;
            int heldZ = linkZ;
            boolean heldPositionWritten = false;
            boolean applyHeldLinkPose = false;
            if (transition >= 0x20 && transition <= 0x4F) {
                int index = (transition - 0x20) >>> 2;
                index = Math.min(index, GRAB_LINK_X_OFFSETS.length - 1);
                heldX = (x + GRAB_LINK_X_OFFSETS[index]) & 0xFF;
                heldY = y;
                heldZ = GRAB_LINK_Z[index];
                heldPositionWritten = true;
                airborne = 0x02;
                applyHeldLinkPose = true;
            }
            spriteVariant = GRAB_BODY_VARIANT[Math.min(transition >>> 3, GRAB_BODY_VARIANT.length - 1)];
            {
                completedResult =
                        result(spriteVariant, blocked, heldX, heldY, heldZ, heldPositionWritten, applyHeldLinkPose);
                return false;
            }
        }

        private HinoxMotion.Update result(
                int variant, boolean blocked, int heldX, int heldY, int heldZ, boolean heldWritten, boolean heldPose) {
            return owner.update(new HinoxResult(
                    entity,
                    x,
                    y,
                    transition,
                    variant,
                    blocked,
                    heldX,
                    heldY,
                    heldZ,
                    linkSpeedX,
                    linkSpeedY,
                    linkVelocityZ,
                    airborne,
                    linkDamage,
                    jingle,
                    wave,
                    dust,
                    heldWritten,
                    heldPose,
                    bomb));
        }

        private boolean inUse;

        HinoxMotion.Update run(HinoxMotion owner, HinoxFrame inputs) {
            if (inUse) {
                return new HinoxStep().run(owner, inputs);
            }
            inUse = true;
            entity = null;
            transitionCountdown = 0;
            frameCounter = 0;
            linkX = 0;
            linkY = 0;
            linkZ = 0;
            linkMotionState = 0;
            randomByteSupplier = null;
            backgroundInteraction = null;
            entityCollisionFlagsForFrame = 0;
            flashCountdown = 0;
            slot = 0;
            linkMotionBlockedAtEntry = false;
            x = 0;
            y = 0;
            transition = 0;
            frame = 0;
            linkSpeedX = 0;
            linkSpeedY = 0;
            linkVelocityZ = 0;
            airborne = 0;
            linkDamage = 0;
            jingle = 0;
            wave = 0;
            dust = false;
            bomb = null;
            spriteVariant = 0;
            completedResult = null;
            this.owner = owner;
            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;
                this.owner = null;
                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundInteraction = null;
                this.bomb = null;
                this.completedResult = null;
            }
        }
    }

    static HinoxMotion.Update updateBody(HinoxMotion owner, HinoxResult inputs) {
        RoomEntity entity = inputs.entity();
        int x = inputs.x();
        int y = inputs.y();
        int transition = inputs.transition();
        int spriteVariant = inputs.spriteVariant();
        boolean blocked = inputs.blocked();
        int heldX = inputs.heldX();
        int heldY = inputs.heldY();
        int heldZ = inputs.heldZ();
        int linkSpeedX = inputs.linkSpeedX();
        int linkSpeedY = inputs.linkSpeedY();
        int linkVelocityZ = inputs.linkVelocityZ();
        int airborne = inputs.airborne();
        int linkDamage = inputs.linkDamage();
        int jingle = inputs.jingle();
        int wave = inputs.wave();
        boolean dust = inputs.dust();
        boolean heldLinkPositionWritten = inputs.heldLinkPositionWritten();
        boolean applyHeldLinkPose = inputs.applyHeldLinkPose();
        HinoxMotion.BombSpawn bomb = inputs.bomb();
        RoomEntity updated = new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                x & 0xFF,
                y & 0xFF,
                entity.status(),
                entity.spriteDefinition(),
                entity.spriteDefinition().supported()
                        ? Math.min(spriteVariant, entity.spriteDefinition().variantCount() - 1)
                        : -1,
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                entity.z(),
                -1,
                false);
        return new HinoxMotion.Update(
                updated,
                owner.state[entity.slot()],
                transition & 0xFF,
                spriteVariant,
                blocked,
                heldX & 0xFF,
                heldY & 0xFF,
                heldZ & 0xFF,
                linkSpeedX & 0xFF,
                linkSpeedY & 0xFF,
                linkVelocityZ & 0xFF,
                airborne & 0xFF,
                linkDamage & 0xFF,
                jingle,
                wave,
                dust,
                heldLinkPositionWritten,
                applyHeldLinkPose,
                bomb);
    }

    record HinoxResult(
            RoomEntity entity,
            int x,
            int y,
            int transition,
            int spriteVariant,
            boolean blocked,
            int heldX,
            int heldY,
            int heldZ,
            int linkSpeedX,
            int linkSpeedY,
            int linkVelocityZ,
            int airborne,
            int linkDamage,
            int jingle,
            int wave,
            boolean dust,
            boolean heldLinkPositionWritten,
            boolean applyHeldLinkPose,
            HinoxMotion.BombSpawn bomb) {}
}
