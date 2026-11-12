package linksawakening.world.entity.motion;

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
        return TarinRaccoonBehavior.advance(this, source, input);
    }

    public Update advance(RoomEntity source, Input input, RoomEntityBackgroundInteraction backgroundInteraction) {
        return TarinRaccoonBehavior.advance(this, source, input, backgroundInteraction);
    }

    public int state(int slot) {
        return state[slot];
    }

    public void startPowderTransformation(int slot) {
        if (state[slot] == 0) {
            state[slot] = 1;
        }
    }

    Movement moveWithBackground(RoomEntity entity, RoomEntityBackgroundInteraction interaction, int slot) {
        return TarinRaccoonBehavior.moveWithBackground(this, entity, interaction, slot);
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
}
