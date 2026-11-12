package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;

/** Per-invocation scratch for MiniMoldormMotion.advanceAfterHistory; values are assigned in source order. */
final class MiniMoldormFrameStep {
    private final MiniMoldormMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int transitionCountdown;
    private int ignoreHitsCountdown;
    private int flashCountdown;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private java.util.function.IntSupplier randomByteSupplier;
    private int frameCounter;
    private int slot;
    private int nextTransitionCountdown;
    private int x;
    private int y;
    private int collisionFlags;
    private int key;
    private int segment1Index;
    private int segment2Index;
    private linksawakening.world.entity.RoomEntity moved;

    MiniMoldormFrameStep(
            MiniMoldormMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int transitionCountdown,
            int ignoreHitsCountdown,
            int flashCountdown,
            linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction,
            java.util.function.IntSupplier randomByteSupplier,
            int frameCounter) {
        this.owner = owner;
        this.entity = entity;
        this.transitionCountdown = transitionCountdown;
        this.ignoreHitsCountdown = ignoreHitsCountdown;
        this.flashCountdown = flashCountdown;
        this.backgroundInteraction = backgroundInteraction;
        this.randomByteSupplier = randomByteSupplier;
        this.frameCounter = frameCounter;
    }

    linksawakening.world.entity.motion.MiniMoldormMotion.Update run() {
        if (!prepareTimers()) return completedResult;
        if (!advanceCollision()) return completedResult;
        if (!advanceDirectionAndHistory()) return completedResult;

        return new MiniMoldormMotion.Update(
                moved,
                nextTransitionCountdown,
                owner.headSpriteVariant[slot],
                collisionFlags,
                owner.historyX[key][segment1Index],
                owner.historyY[key][segment1Index],
                owner.historyX[key][segment2Index],
                owner.historyY[key][segment2Index]);
    }

    private linksawakening.world.entity.motion.MiniMoldormMotion.Update completedResult;

    private boolean prepareTimers() {

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(entity);
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Mini Moldorm random source cannot be null");
        }
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }

        nextTransitionCountdown = transitionCountdown & 0xFF;
        if (nextTransitionCountdown > 0) {
            nextTransitionCountdown--;
        }
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        collisionFlags = 0;
        return true;
    }

    private boolean advanceCollision() {

        if ((flashCountdown & 0xFF) == 0) {
            int nextX = MiniMoldormMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            if (nextX != x && backgroundInteraction != null) {
                int direction = MiniMoldormMotion.signedByte(owner.speedX[slot]) < 0
                        ? EntityBackgroundCollisionResult.LEFT
                        : EntityBackgroundCollisionResult.RIGHT;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                        entity, direction, nextX, y, ignoreHitsCountdown & 0xFF, frameCounter);
                if (result.blocked()) {
                    collisionFlags |= result.collisionFlag();
                } else {
                    x = nextX;
                }
            } else {
                x = nextX;
            }

            int nextY = MiniMoldormMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            if (nextY != y && backgroundInteraction != null) {
                int direction = MiniMoldormMotion.signedByte(owner.speedY[slot]) < 0
                        ? EntityBackgroundCollisionResult.UP
                        : EntityBackgroundCollisionResult.DOWN;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                        entity, direction, x, nextY, ignoreHitsCountdown & 0xFF, frameCounter);
                if (result.blocked()) {
                    collisionFlags |= result.collisionFlag();
                } else {
                    y = nextY;
                }
            } else {
                y = nextY;
            }
        }

        if (collisionFlags != 0) {
            owner.privateState1[slot] = (collisionFlags & 0x01) != 0
                    ? 0x08
                    : (collisionFlags & 0x02) != 0 ? 0x00 : (collisionFlags & 0x04) != 0 ? 0x04 : 0x0C;
            if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                owner.privateState2[slot] = -owner.privateState2[slot];
            }
            nextTransitionCountdown = 0x10;
        }
        return true;
    }

    private boolean advanceDirectionAndHistory() {

        if (owner.privateCountdown1[slot] == 0) {
            owner.privateCountdown1[slot] = 0x04;
            owner.privateState1[slot] = (owner.privateState1[slot] + owner.privateState2[slot]) & 0x0F;
            owner.headSpriteVariant[slot] = MiniMoldormMotion.HEAD_SPRITE_VARIANTS[owner.privateState1[slot]];
            owner.speedY[slot] = MiniMoldormMotion.Y_SPEEDS[owner.privateState1[slot]];
            owner.speedX[slot] = MiniMoldormMotion.X_SPEEDS[owner.privateState1[slot]];
        }

        if (nextTransitionCountdown == 0) {
            nextTransitionCountdown = 0x10 + (randomByteSupplier.getAsInt() & 0x1F);
            owner.privateState2[slot] = (randomByteSupplier.getAsInt() & 0x02) - 1;
        }

        key = owner.historyKey[slot];
        segment1Index = (owner.inertia[slot] - 0x09) & MiniMoldormMotion.HISTORY_MASK;
        segment2Index = (owner.inertia[slot] - 0x10) & MiniMoldormMotion.HISTORY_MASK;
        moved = MiniMoldormMotion.withPosition(entity, x, y);
        return true;
    }
}
