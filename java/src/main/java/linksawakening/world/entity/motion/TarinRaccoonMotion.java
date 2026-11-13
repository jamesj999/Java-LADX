package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$05 outdoor {@code TarinEntityHandler}, including the raccoon transformation. */
public final class TarinRaccoonMotion {
    public static final int ENTITY_TYPE = 0x3F;
    static final int NO_EVENT = -1;
    static final int[] TRANSFORMATION_VARIANTS = {0, 4, 5, 6, 7, 1};

    public record Input(
            int frameCounter,
            int linkX,
            int linkY,
            int linkDirection,
            boolean actionHeld,
            boolean dialogActive,
            boolean powderHit,
            int linkAttackStepAnimationCountdown,
            boolean linkAirborne,
            boolean inventoryAppearing,
            int dialogCooldown,
            int windowY,
            int slowTransitionCountdown,
            int transitionCountdown) {
        Input(
                int frameCounter,
                int linkX,
                int linkY,
                int linkDirection,
                boolean actionHeld,
                boolean dialogActive,
                boolean powderHit) {
            this(frameCounter, linkX, linkY, linkDirection, actionHeld, dialogActive, powderHit, 0, false);
        }

        Input(
                int frameCounter,
                int linkX,
                int linkY,
                int linkDirection,
                boolean actionHeld,
                boolean dialogActive,
                boolean powderHit,
                int linkAttackStepAnimationCountdown) {
            this(
                    frameCounter,
                    linkX,
                    linkY,
                    linkDirection,
                    actionHeld,
                    dialogActive,
                    powderHit,
                    linkAttackStepAnimationCountdown,
                    false);
        }

        Input(
                int frameCounter,
                int linkX,
                int linkY,
                int linkDirection,
                boolean actionHeld,
                boolean dialogActive,
                boolean powderHit,
                int linkAttackStepAnimationCountdown,
                boolean linkAirborne) {
            this(
                    frameCounter,
                    linkX,
                    linkY,
                    linkDirection,
                    actionHeld,
                    dialogActive,
                    powderHit,
                    linkAttackStepAnimationCountdown,
                    linkAirborne,
                    false,
                    0,
                    0x80,
                    0,
                    0);
        }

        public Input {
            if (linkDirection < 0 || linkDirection > 3) {
                throw new IllegalArgumentException("Invalid Link direction: " + linkDirection);
            }
            validateByte(frameCounter, "Frame counter");
            validateByte(linkX, "Link X");
            validateByte(linkY, "Link Y");
            validateByte(dialogCooldown, "Dialog cooldown");
            validateByte(windowY, "Window Y");
            validateByte(slowTransitionCountdown, "Slow transition countdown");
            validateByte(transitionCountdown, "Transition countdown");
        }
    }

    public record Update(
            RoomEntity entity,
            int state,
            int spriteVariant,
            boolean shouldGetLost,
            int dialogGlobalId,
            boolean linkMotionBlocked,
            boolean clearLinkAttack,
            int linkFacingDirection,
            boolean pushLink,
            boolean roomChanged,
            boolean tarinFlag,
            int soundChannel,
            int soundId,
            boolean spawnBomb,
            int slowTransitionCountdown,
            int transitionCountdown,
            int speedX,
            int speedY,
            int speedZ,
            int collisionFlags,
            boolean nearLinkLatch) {}

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] warningShown = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] accumulatorX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] accumulatorY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] accumulatorZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationIndex = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] nearLinkLatch = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public Update advance(RoomEntity source, Input input) {
        return TarinRaccoonMotion.advanceBody(this, source, input);
    }

    public Update advance(
            RoomEntity source,
            Input input, /* Optional room geometry/history; null preserves the path without that room input. */
            RoomEntityBackgroundInteraction backgroundInteraction) {
        return TarinRaccoonMotion.advanceBody(this, source, input, backgroundInteraction);
    }

    public int state(int slot) {
        return state[slot];
    }

    public void startPowderTransformation(int slot) {
        if (state[slot] == 0) {
            state[slot] = 1;
        }
    }

    Movement moveWithBackground(
            RoomEntity entity, /* Optional room geometry/history; null preserves the path without that room input. */
            RoomEntityBackgroundInteraction interaction,
            int slot) {
        int x = TarinRaccoonMotion.addSpeedToPosition(entity.x(), this.speedX[slot], this.accumulatorX, slot);
        int y = TarinRaccoonMotion.addSpeedToPosition(entity.y(), this.speedY[slot], this.accumulatorY, slot);
        int flags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = TarinRaccoonMotion.signedByte(this.speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, entity.y());
            if (result.blocked()) {
                flags |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = TarinRaccoonMotion.signedByte(this.speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                flags |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new TarinRaccoonMotion.Movement(x & 0xFF, y & 0xFF, flags & 0x0F);
    }

    static int accelerateToLimit(int value) {
        int unsigned = value & 0xFF;
        if (unsigned == 0x30 || unsigned == 0xD0) {
            return unsigned;
        }
        return (unsigned + (signedByte(unsigned) < 0 ? -1 : 1)) & 0xFF;
    }

    static int dampTowardsZero(int value) {
        int signed = signedByte(value);
        return signed == 0 ? 0 : (signed + (signed < 0 ? 1 : -1)) & 0xFF;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
        int fractionalSum = accumulator[slot] + ((speed & 0x0F) << 4);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    static boolean nearbyAndFacing(RoomEntity entity, Input input) {
        int yWindow = (input.linkY() - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x28) {
            return false;
        }
        int xWindow = (input.linkX() - entity.x() + 0x10) & 0xFF;
        return xWindow < 0x20
                && directionToLink(entity, input.linkX(), input.linkY()) == (input.linkDirection() ^ 0x01);
    }

    static int directionToLink(RoomEntity entity, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entity.x());
        int distanceY = signedByte(linkY - entity.y());
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    static boolean withinSignedWindow(int value, int radius) {
        return ((value + radius) & 0xFF) < radius * 2;
    }

    static RoomEntity withPositionAndVariant(RoomEntity source, int x, int y, int variant) {
        return EntitySnapshots.withMotion(
                source,
                x,
                y,
                variant,
                source.entityFlipAttribute(),
                source.z(),
                source.deathSpriteVariant(),
                source.powerRecoilDeath());
    }

    static RoomEntity withVariant(RoomEntity source, int variant) {
        return withPositionAndVariant(source, source.x(), source.y(), variant);
    }

    static RoomEntity withZ(RoomEntity source, int z) {
        return EntitySnapshots.withMotion(
                source,
                source.x(),
                source.y(),
                source.spriteVariant(),
                source.entityFlipAttribute(),
                z,
                source.deathSpriteVariant(),
                source.powerRecoilDeath());
    }

    static void checkType(RoomEntity source) {
        if (source == null || source.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("Entity is not Tarin");
        }
    }

    private static void validateByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte");
        }
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record Movement(int x, int y, int collisionFlags) {}

    final TarinRaccoonStep tarinRaccoonStep = new TarinRaccoonStep(this);

    static TarinRaccoonMotion.Update advanceBody(
            TarinRaccoonMotion owner, RoomEntity source, TarinRaccoonMotion.Input input) {
        return owner.advance(source, input, null);
    }

    static TarinRaccoonMotion.Update advanceBody(
            TarinRaccoonMotion owner,
            RoomEntity source,
            TarinRaccoonMotion.Input input,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundInteraction
                    backgroundInteraction) {
        return owner.tarinRaccoonStep.run(source, input, backgroundInteraction);
    }

    static final class TarinRaccoonStep {
        private final linksawakening.world.entity.motion.TarinRaccoonMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity source;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.TarinRaccoonMotion.Input input;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int slot;
        private int slowCountdown;
        private int transitionCountdown;
        private int variant;
        private boolean shouldGetLost;
        private int dialog;
        private boolean blockLink;
        private boolean clearAttack;
        private int linkFacing;
        private boolean pushLink;
        private boolean roomChanged;
        private boolean tarinFlag;
        private int soundId;
        private boolean spawnBomb;
        private int collisionFlags;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;

        TarinRaccoonStep(linksawakening.world.entity.motion.TarinRaccoonMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.TarinRaccoonMotion.Update run() {
            TarinRaccoonMotion.checkType(source);
            slot = source.slot();
            slowCountdown = input.slowTransitionCountdown();
            transitionCountdown = input.transitionCountdown();
            variant = source.spriteVariant();
            shouldGetLost = false;
            dialog = NO_EVENT;
            blockLink = false;
            clearAttack = false;
            linkFacing = NO_EVENT;
            pushLink = false;
            roomChanged = false;
            tarinFlag = false;
            soundId = NO_EVENT;
            spawnBomb = false;
            collisionFlags = 0;
            entity = source;

            switch (owner.state[slot]) {
                case 0 -> {
                    if (!advanceState0()) return completedResult;
                }
                case 1 -> {
                    if (!advanceState1()) return completedResult;
                }
                case 2 -> {
                    if (!advanceState2()) return completedResult;
                }
                case 3 -> {
                    if (!advanceState3()) return completedResult;
                }
                default -> throw new IllegalStateException("Invalid Tarin state: " + owner.state[slot]);
            }

            return new TarinRaccoonMotion.Update(
                    entity,
                    owner.state[slot],
                    entity.spriteVariant(),
                    shouldGetLost,
                    dialog,
                    blockLink,
                    clearAttack,
                    linkFacing,
                    pushLink,
                    roomChanged,
                    tarinFlag,
                    soundId < 0 ? NO_EVENT : 0,
                    soundId,
                    spawnBomb,
                    slowCountdown,
                    transitionCountdown,
                    owner.speedX[slot] & 0xFF,
                    owner.speedY[slot] & 0xFF,
                    owner.speedZ[slot] & 0xFF,
                    collisionFlags,
                    owner.nearLinkLatch[slot]);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.TarinRaccoonMotion.Update completedResult;

        private boolean advanceState0() {
            variant = (input.frameCounter() >>> 4) & 1;
            if ((input.linkY() & 0xFF) < 0x30) {
                shouldGetLost = true;
                variant = 2 + ((input.frameCounter() >>> 3) & 1);
            } else {
                owner.warningShown[slot] = false;
            }
            entity = TarinRaccoonMotion.withVariant(source, variant);
            if (!input.dialogActive() && !input.inventoryAppearing()) {
                if ((input.linkY() & 0xFF) < 0x20 && !owner.warningShown[slot]) {
                    owner.warningShown[slot] = true;
                    dialog = 0x021;
                } else if (input.actionHeld()
                        && TarinRaccoonMotion.nearbyAndFacing(entity, input)
                        && input.linkAttackStepAnimationCountdown() == 0
                        && !input.linkAirborne()
                        && input.dialogCooldown() == 0
                        && input.windowY() == 0x80) {
                    dialog = 0x00D;
                }
            }

            return true;
        }

        private boolean advanceState1() {
            blockLink = true;
            clearAttack = true;
            linkFacing = TarinRaccoonMotion.directionToLink(source, input.linkX(), input.linkY()) ^ 0x01;

            int animationSum = owner.animationAccumulator[slot] + owner.animationInertia[slot];
            owner.animationAccumulator[slot] = animationSum & 0xFF;
            if (animationSum > 0xFF) {
                owner.animationIndex[slot] = (owner.animationIndex[slot] + 1) % 6;
            }
            variant = TRANSFORMATION_VARIANTS[owner.animationIndex[slot]];
            entity = TarinRaccoonMotion.withVariant(source, variant);

            if (slowCountdown == 0) {
                owner.speedZ[slot] = 0;
                owner.state[slot] = 2;
                variant = 9;
                entity = TarinRaccoonMotion.withVariant(entity, variant);
                spawnBomb = true;
                roomChanged = true;
                tarinFlag = true;
                return true;
            }

            if ((input.frameCounter() & 1) == 0 && owner.animationInertia[slot] < 0xF0) {
                owner.animationInertia[slot]++;
            }
            TarinRaccoonMotion.Movement moved = owner.moveWithBackground(entity, backgroundInteraction, slot);
            entity = TarinRaccoonMotion.withPositionAndVariant(entity, moved.x(), moved.y(), variant);
            collisionFlags = moved.collisionFlags();

            if (slowCountdown < 6) {
                if ((entity.y() & 0xFF) < 0x30) {
                    slowCountdown = 8;
                } else {
                    owner.speedZ[slot] = (owner.speedZ[slot] + 1) & 0xFF;
                    owner.speedX[slot] = TarinRaccoonMotion.dampTowardsZero(owner.speedX[slot]);
                    owner.speedY[slot] = TarinRaccoonMotion.dampTowardsZero(owner.speedY[slot]);
                    entity = TarinRaccoonMotion.withZ(
                            entity,
                            TarinRaccoonMotion.addSpeedToPosition(
                                    entity.z(), owner.speedZ[slot], owner.accumulatorZ, slot));
                    return true;
                }
            }

            if ((collisionFlags & 0x03) != 0) {
                owner.speedX[slot] = (-TarinRaccoonMotion.signedByte(owner.speedX[slot])) & 0xFF;
                soundId = 0x09;
            }
            if ((collisionFlags & 0x0C) != 0) {
                owner.speedY[slot] = (-TarinRaccoonMotion.signedByte(owner.speedY[slot])) & 0xFF;
                soundId = 0x09;
            }
            if (slowCountdown < 0x60 && (input.frameCounter() & 3) == 0) {
                owner.speedX[slot] = TarinRaccoonMotion.accelerateToLimit(owner.speedX[slot]);
                owner.speedY[slot] = TarinRaccoonMotion.accelerateToLimit(owner.speedY[slot]);
            }

            return true;
        }

        private boolean advanceState2() {
            blockLink = true;
            int z = TarinRaccoonMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.accumulatorZ, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
            entity = TarinRaccoonMotion.withZ(entity, z);
            if ((z & 0x80) != 0) {
                entity = TarinRaccoonMotion.withZ(entity, 0);
                soundId = 0x23;
                transitionCountdown = 0x40;
                variant = 8 + TarinRaccoonMotion.directionToLink(entity, input.linkX(), input.linkY());
                entity = TarinRaccoonMotion.withVariant(entity, variant);
                owner.nearLinkLatch[slot] = TarinRaccoonMotion.withinSignedWindow(input.linkX() - entity.x(), 0x12)
                        && TarinRaccoonMotion.withinSignedWindow(input.linkY() - entity.y(), 0x12);
                owner.state[slot] = 3;
            }

            return true;
        }

        private boolean advanceState3() {
            if (transitionCountdown == 1) {
                dialog = 0x00A;
                return true;
            }
            if (transitionCountdown != 0) {
                blockLink = true;
                return true;
            }
            if ((input.frameCounter() & 0x1F) == 0) {
                variant = 8 + TarinRaccoonMotion.directionToLink(entity, input.linkX(), input.linkY());
                entity = TarinRaccoonMotion.withVariant(entity, variant);
            }
            pushLink = !owner.nearLinkLatch[slot];
            if (!input.dialogActive()
                    && !input.inventoryAppearing()
                    && input.actionHeld()
                    && TarinRaccoonMotion.nearbyAndFacing(entity, input)
                    && !input.linkAirborne()
                    && input.dialogCooldown() == 0
                    && input.windowY() == 0x80) {
                dialog = 0x00B;
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.TarinRaccoonMotion.Update run(
                linksawakening.world.entity.RoomEntity source,
                linksawakening.world.entity.motion.TarinRaccoonMotion.Input input,
                /* Optional room geometry/history; null preserves the path without that room input. */ linksawakening
                                .world.entity.RoomEntityBackgroundInteraction
                        backgroundInteraction) {
            if (inUse) {
                return new TarinRaccoonStep(owner).run(source, input, backgroundInteraction);
            }
            inUse = true;
            slot = 0;
            slowCountdown = 0;
            transitionCountdown = 0;
            variant = 0;
            shouldGetLost = false;
            dialog = 0;
            blockLink = false;
            clearAttack = false;
            linkFacing = 0;
            pushLink = false;
            roomChanged = false;
            tarinFlag = false;
            soundId = 0;
            spawnBomb = false;
            collisionFlags = 0;
            entity = null;
            completedResult = null;

            this.source = source;
            this.input = input;
            this.backgroundInteraction = backgroundInteraction;
            try {
                return run();
            } finally {
                inUse = false;

                this.source = null;
                this.input = null;
                this.backgroundInteraction = null;
                this.entity = null;
                this.completedResult = null;
            }
        }
    }
}
