package linksawakening.world.entity.motion;

/** Per-invocation scratch for BombiteMotion.advance; values are assigned in source order. */
final class BombiteStep {
    private final BombiteMotion owner;
    private linksawakening.world.entity.motion.BombiteFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int transitionCountdown;
    private int slowTransitionCountdown;
    private int privateCountdown1;
    private int enemyIgnoreHitsCountdown;
    private boolean runningWithPegasusBoots;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int nextTransition;
    private int nextSlowTransition;
    private int nextPrivateCountdown1;
    private int spriteVariant;
    private boolean explode;
    private boolean bumpJingle;
    private int x;
    private int y;
    private boolean blockedX;
    private boolean blockedY;
    private linksawakening.world.entity.RoomEntity moved;

    BombiteStep(BombiteMotion owner, linksawakening.world.entity.motion.BombiteFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.motion.BombiteMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceBombite()) return completedResult;

        return new BombiteMotion.Update(
                moved, nextTransition, nextSlowTransition, nextPrivateCountdown1, spriteVariant, explode, bumpJingle);
    }

    private linksawakening.world.entity.motion.BombiteMotion.Update completedResult;

    private boolean advanceState0() {
        if (nextTransition == 0) {
            owner.state[slot] = 1;
        }
        spriteVariant = (frameCounter >>> 4) & 0x01;

        return true;
    }

    private boolean advanceState1() {
        int directionChoice = randomByteSupplier.getAsInt() & 0x03;
        int direction = directionChoice == 0
                ? BombiteMotion.directionToLink(moved.x(), moved.y(), linkEntityX, linkEntityY)
                : randomByteSupplier.getAsInt() & 0x03;
        owner.speedX[slot] = BombiteMotion.BOUNCING_SPEED_X[direction];
        owner.speedY[slot] = BombiteMotion.BOUNCING_SPEED_Y[direction];
        nextTransition = 0x20 + (randomByteSupplier.getAsInt() & 0x0F);
        owner.state[slot] = 0;

        return true;
    }

    private boolean advanceState2() {
        if (nextTransition == 0) {
            explode = true;
        } else {
            if (blockedX) {
                owner.speedX[slot] = BombiteMotion.negate(owner.speedX[slot]);
                bumpJingle = true;
            } else if (blockedY) {
                owner.speedY[slot] = BombiteMotion.negate(owner.speedY[slot]);
                bumpJingle = true;
            }
            spriteVariant = (frameCounter >>> 1) & 0x01;
        }

        return true;
    }

    private boolean prepareFrame() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        transitionCountdown = inputs.transitionCountdown();
        slowTransitionCountdown = inputs.slowTransitionCountdown();
        privateCountdown1 = inputs.privateCountdown1();
        enemyIgnoreHitsCountdown = inputs.enemyIgnoreHitsCountdown();
        runningWithPegasusBoots = inputs.runningWithPegasusBoots();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        nextTransition = transitionCountdown & 0xFF;
        nextSlowTransition = slowTransitionCountdown & 0xFF;
        nextPrivateCountdown1 = privateCountdown1 & 0xFF;
        spriteVariant = entity.spriteVariant();
        explode = false;
        bumpJingle = false;

        if (entity.type() == BombiteMotion.ENTITY_TIMER_BOMBITE
                && enemyIgnoreHitsCountdown == 0x08
                && owner.state[slot] == 0) {
            // TimerBombiteEntityHandler's pre-movement hit-countdown trigger.
            owner.state[slot] = 1;
            nextSlowTransition = 0x6F;
        }

        x = BombiteMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        y = BombiteMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
        blockedX = false;
        blockedY = false;
        if (backgroundCollision != null) {
            if (x != entity.x()
                    && backgroundCollision.blocks(
                            entity, BombiteMotion.horizontalDirection(owner.speedX[slot]), x, entity.y())) {
                x = entity.x();
                blockedX = true;
            }
            if (y != entity.y()
                    && backgroundCollision.blocks(entity, BombiteMotion.verticalDirection(owner.speedY[slot]), x, y)) {
                y = entity.y();
                blockedY = true;
            }
        }

        moved = BombiteMotion.withPosition(entity, x, y);
        return true;
    }

    private boolean advanceBombite() {

        if (entity.type() == BombiteMotion.ENTITY_TIMER_BOMBITE) {
            if (owner.state[slot] == 0) {
                if (nextTransition == 0) {
                    int random = randomByteSupplier.getAsInt() & 0xFF;
                    nextTransition = 0x30 + (random & 0x1F);
                    int direction = random & 0x03;
                    owner.speedX[slot] = BombiteMotion.TIMER_SPEED_X[direction];
                    owner.speedY[slot] = BombiteMotion.TIMER_SPEED_Y[direction];
                }
                spriteVariant = (frameCounter >>> 4) & 0x01;
            } else {
                if (runningWithPegasusBoots) {
                    owner.state[slot] = 0;
                } else {
                    int xDistance = BombiteMotion.unsignedByteAbs(linkEntityX - moved.x());
                    int yDistance = BombiteMotion.unsignedByteAbs(linkEntityY - moved.y());
                    if (xDistance >= 0x12 || yDistance >= 0x12) {
                        if (((frameCounter ^ slot) & 0x03) == 0) {
                            BombiteMotion.Vector vector = BombiteMotion.vectorTowardsLink(
                                    moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x0E);
                            owner.speedX[slot] = vector.x();
                            owner.speedY[slot] = vector.y();
                        }
                    } else {
                        owner.speedX[slot] = 0;
                        owner.speedY[slot] = 0;
                    }

                    int originalSlowTransition = nextSlowTransition;
                    if (originalSlowTransition == 0) {
                        explode = true;
                    } else {
                        if (originalSlowTransition == 0x18) {
                            nextSlowTransition = 0x0A;
                            nextPrivateCountdown1 = 0x30;
                        }
                        spriteVariant = BombiteMotion.TIMER_COUNTDOWN_VARIANTS[
                                Math.min(
                                        originalSlowTransition >>> 4,
                                        BombiteMotion.TIMER_COUNTDOWN_VARIANTS.length - 1)];
                    }
                }
            }
        } else if (entity.type() == BombiteMotion.ENTITY_BOUNCING_BOMBITE) {
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
                default -> throw new IllegalStateException("Invalid Bombite state: " + owner.state[slot]);
            }
        }
        return true;
    }
}
