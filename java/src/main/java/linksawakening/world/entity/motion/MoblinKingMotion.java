package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;

/** Bank-$15 {@code MoblinKingEntityHandler}'s ten-state fight sequence. */
public final class MoblinKingMotion {
    private final int[] lethalHitFrame = new int[EntityRoomLoader.MAX_ENTITIES];

    public int lethalHitFrame(int slot) {
        return lethalHitFrame[slot];
    }

    public void setLethalHitFrame(int slot, int value) {
        lethalHitFrame[slot] = value;
    }

    public record ArrowRequest(int x, int y, int speedX, int speedY, int direction) {}

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int slowTransitionCountdown,
            ArrowRequest arrowRequest,
            boolean openIntroDialog,
            int jingleId,
            boolean strongBump,
            int screenShakeCountdown,
            int linkDamage,
            int ignoreLinkCollisionsCountdown) {}

    static final int[] ARROW_X_OFFSETS = {0x08, -0x08};
    static final int[] ARROW_Y_OFFSETS = {-0x04, -0x04};
    static final int[] ARROW_SPEED_X = {0x20, -0x20};
    static final int[] CHARGE_SPEED_X = {0x28, -0x28};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] xAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] yAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] zAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bodyVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] weaponFrame = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] weaponVisible = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public Update advance(MoblinKingFrame inputs) {
        return moblinKingStep.run(this, inputs);
    }

    Update applyLinkImpact(
            RoomEntity entity, int x, int y, int z, int variant, int transitionCountdown, int slowTransitionCountdown) {
        int slot = entity.slot();
        state[slot] = 9;
        bodyVariant[slot] = variant;
        transitionCountdown = 0x60;
        return new Update(
                withPosition(entity, x, y, z),
                transitionCountdown,
                slowTransitionCountdown,
                null,
                false,
                0x0B,
                false,
                0,
                0x08,
                0x28);
    }

    public void clear(int slot) {
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
        bodyVariant[slot] = 0;
        weaponFrame[slot] = 0;
        weaponVisible[slot] = true;
    }

    public int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    public int weaponVariant(int slot) {
        return weaponVisible[slot] ? (direction[slot] * 2 + weaponFrame[slot]) & 0x03 : -1;
    }

    public int bodyVariant(int slot) {
        // The room loader supplies a one-variant placeholder definition. Keep
        // the handler's 14-state body index here until RoomEntityRuntime swaps
        // in forMoblinKingState's dynamic display list.
        return presentationVariant(slot, bodyVariant[slot]);
    }

    static int directionToLink(int x, int linkX) {
        return signedByte((linkX - x) & 0xFF) < 0 ? 1 : 0;
    }

    private int presentationVariant(int slot, int variant) {
        return direction[slot] == 0 && variant < 0x0C ? variant + 0x06 : variant;
    }

    static Vector vectorTowardsOffsetLink(int x, int y, int linkX, int linkY, int direction, int length) {
        int targetX = (linkX + (direction == 0 ? -0x30 : 0x30)) & 0xFF;
        int dx = signedByte((targetX - x) & 0xFF);
        int dy = signedByte((linkY - y) & 0xFF);
        // GetVectorTowardsLink ($03:7E45) keeps the dominant component at
        // the requested speed and divides only the smaller component.
        int dominant = Math.max(Math.abs(dx), Math.abs(dy));
        if (dominant == 0) return new Vector(length, length);
        return new Vector(dx * length / dominant, dy * length / dominant);
    }

    static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        int fixed = ((position & 0xFF) << 4) | (accumulator[slot] & 0x0F);
        fixed += signedByte(speed);
        accumulator[slot] = fixed & 0x0F;
        return (fixed >>> 4) & 0xFF;
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return EntitySnapshots.withMotion(
                entity, x & 0xFF, y & 0xFF, entity.spriteVariant(), entity.entityFlipAttribute(), z & 0xFF, -1, false);
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record Vector(int x, int y) {}

    final MoblinKingStep moblinKingStep = new MoblinKingStep();
}
