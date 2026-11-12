package linksawakening.world.entity.motion;

/** Reused, reentrancy-safe scratch for BomberMotion.advance; values are assigned in source order. */
final class BomberFrameStep {
    private BomberMotion owner;
    private linksawakening.world.entity.motion.BomberFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int transitionCountdown;
    private int linkEntityX;
    private int linkEntityY;
    private int romLinkDirection;
    private boolean swordAnimationActive;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int x;
    private int y;
    private int z;
    private linksawakening.world.entity.RoomEntity moved;
    private int nextTransitionCountdown;
    private int oldInertia;
    private linksawakening.world.entity.motion.BomberMotion.BombSpawn bombSpawn;
    private boolean playJingle;

    BomberFrameStep() {}

    linksawakening.world.entity.motion.BomberMotion.Update run() {
        entity = inputs.entity();
        transitionCountdown = inputs.transitionCountdown();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        romLinkDirection = inputs.romLinkDirection();
        swordAnimationActive = inputs.swordAnimationActive();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        x = BomberMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        y = BomberMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
        if (backgroundCollision != null) {
            if (x != entity.x()
                    && backgroundCollision.blocks(
                            entity, BomberMotion.horizontalDirection(owner.speedX[slot]), x, entity.y())) {
                x = entity.x();
                owner.speedX[slot] = 0;
            }
            if (y != entity.y()
                    && backgroundCollision.blocks(entity, BomberMotion.verticalDirection(owner.speedY[slot]), x, y)) {
                y = entity.y();
                owner.speedY[slot] = 0;
            }
        }

        z = BomberMotion.Z_BY_INERTIA[(owner.inertia[slot] >>> 3) & 0x03];
        moved = BomberMotion.withPosition(entity, x, y, z);
        nextTransitionCountdown = transitionCountdown & 0xFF;

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
            default -> owner.state[slot] = 0;
        }

        oldInertia = owner.inertia[slot];
        owner.inertia[slot] = (oldInertia + 1) & 0xFF;
        bombSpawn = null;
        playJingle = false;
        if ((oldInertia & 0x7F) == 0) {
            BomberMotion.Vector vector =
                    BomberMotion.vectorTowardsLink(moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x10);
            bombSpawn = new BomberMotion.BombSpawn(moved.x(), moved.y(), moved.z(), vector.x(), vector.y(), 0x08, 0x40);
            playJingle = true;
        }

        if (BomberMotion.matchesSwordAttackWindow(
                moved, linkEntityX, linkEntityY, romLinkDirection, swordAnimationActive)) {
            owner.state[slot] = 2;
            nextTransitionCountdown = 0x12;
            BomberMotion.Vector vector =
                    BomberMotion.vectorTowardsLink(moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x20);
            owner.speedX[slot] = (-BomberMotion.signedByte(vector.x())) & 0xFF;
            owner.speedY[slot] = (-BomberMotion.signedByte(vector.y())) & 0xFF;
        }

        return new BomberMotion.Update(moved, nextTransitionCountdown, bombSpawn, playJingle);
    }

    private linksawakening.world.entity.motion.BomberMotion.Update completedResult;

    private boolean advanceState0() {
        if (nextTransitionCountdown == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            nextTransitionCountdown = 0x20 + (random & 0x1F);
            int direction = random & 0x07;
            owner.speedX[slot] = BomberMotion.SPEED_X_BY_RANDOM[direction];
            owner.speedY[slot] = BomberMotion.SPEED_Y_BY_RANDOM[direction];
            owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0xFF;
            if ((owner.privateState1[slot] & 0x07) == 0) {
                BomberMotion.Vector vector =
                        BomberMotion.vectorTowardsLink(moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x0A);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
            }
            owner.state[slot] = 1;
        }

        return true;
    }

    private boolean advanceState1() {
        if (nextTransitionCountdown == 0) {
            nextTransitionCountdown = 0x20;
            owner.state[slot] = 0;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
        }

        return true;
    }

    private boolean advanceState2() {
        if (nextTransitionCountdown == 0) {
            owner.state[slot] = 0;
        }

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.BomberMotion.Update run(
            BomberMotion owner, linksawakening.world.entity.motion.BomberFrame inputs) {
        if (inUse) {
            return new BomberFrameStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        transitionCountdown = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        romLinkDirection = 0;
        swordAnimationActive = false;
        randomByteSupplier = null;
        backgroundCollision = null;
        slot = 0;
        x = 0;
        y = 0;
        z = 0;
        moved = null;
        nextTransitionCountdown = 0;
        oldInertia = 0;
        bombSpawn = null;
        playJingle = false;
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
            this.backgroundCollision = null;
            this.moved = null;
            this.bombSpawn = null;
            this.completedResult = null;
        }
    }
}
