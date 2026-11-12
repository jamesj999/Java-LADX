package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for SnakeMotion.advance; values are assigned in source order. */
final class SnakeFrameStep {
    private SnakeMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int transitionCountdown;
    private int slot;
    private int countdown;
    private int x;
    private int y;
    private int renderedVariant;
    private linksawakening.world.entity.motion.SnakeMotion.Position moved;
    private linksawakening.world.entity.RoomEntity updated;

    SnakeFrameStep() {}

    linksawakening.world.entity.motion.SnakeMotion.Update run() {
        if ((entity.type() & 0xFF) != SnakeMotion.ENTITY_SNAKE) {
            throw new IllegalArgumentException(
                    "Unsupported Snake motion entity type: 0x" + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Snake random-byte supplier cannot be null");
        }

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }

        countdown = transitionCountdown & 0xFF;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        // RenderActiveEntitySpritesPair runs before movement and state
        // dispatch. The handler adds the persistent direction offset to the
        // previously selected base animation variant, then writes the next
        // base variant only after the current frame has been drawn.
        renderedVariant = (owner.animationVariant[slot] + owner.direction[slot]) & 0xFF;

        // SnakeEntityHandler updates its position and applies background
        // interaction before dispatching the state table. A collision then
        // resets the state, transition byte, and private countdown; state 0
        // clears the speed in the same handler pass.
        moved = owner.move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        if (moved.collided()) {
            owner.state[slot] = 0;
            countdown = 0x08;
            owner.privateCountdown1[slot] = 0x20;
        }

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
            default -> throw new IllegalStateException("Invalid Snake state: " + owner.state[slot]);
        }

        updated = EntitySnapshots.withMotion(
                entity, x, y, renderedVariant, entity.entityFlipAttribute(), entity.z(), -1, false);
        return new SnakeMotion.Update(updated, countdown);
    }

    private linksawakening.world.entity.motion.SnakeMotion.Update completedResult;

    private boolean advanceState0() {
        if (countdown == 0) {
            owner.state[slot] = 1;
            int random = randomByteSupplier.getAsInt() & 0xFF;
            countdown = 0x30 + (random & 0x1F);
            owner.setSpeed(slot, random & 0x03);
            // The ROM returns immediately from this branch, leaving
            // the previous base animation variant in place.
        } else {
            owner.clearSpeed(slot);
            countdown = owner.startDashIfNeeded(slot, x, y, linkEntityX, linkEntityY, countdown);
            owner.setAnimationVariant(slot, SnakeMotion.crawlingAnimation(frameCounter));
        }

        return true;
    }

    private boolean advanceState1() {
        if (countdown == 0) {
            countdown = 0x18;
            owner.state[slot] = 2;
        }
        countdown = owner.startDashIfNeeded(slot, x, y, linkEntityX, linkEntityY, countdown);
        owner.setAnimationVariant(slot, SnakeMotion.crawlingAnimation(frameCounter));

        return true;
    }

    private boolean advanceState2() {
        if (countdown == 0) {
            countdown = 0x20;
            owner.state[slot] = 0;
            owner.privateCountdown1[slot] = 0x40;
        }
        owner.setAnimationVariant(slot, SnakeMotion.dashingAnimation(frameCounter));

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.SnakeMotion.Update run(
            SnakeMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown) {
        if (inUse) {
            return new SnakeFrameStep()
                    .run(
                            owner,
                            entity,
                            frameCounter,
                            linkEntityX,
                            linkEntityY,
                            randomByteSupplier,
                            backgroundCollision,
                            transitionCountdown);
        }
        inUse = true;
        slot = 0;
        countdown = 0;
        x = 0;
        y = 0;
        renderedVariant = 0;
        moved = null;
        updated = null;
        completedResult = null;
        this.owner = owner;
        this.entity = entity;
        this.frameCounter = frameCounter;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.randomByteSupplier = randomByteSupplier;
        this.backgroundCollision = backgroundCollision;
        this.transitionCountdown = transitionCountdown;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.randomByteSupplier = null;
            this.backgroundCollision = null;
            this.moved = null;
            this.updated = null;
            this.completedResult = null;
        }
    }
}
