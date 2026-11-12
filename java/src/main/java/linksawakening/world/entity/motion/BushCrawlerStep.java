package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for BushCrawlerMotion.advance; values are assigned in source order. */
final class BushCrawlerStep {
    private BushCrawlerMotion owner;
    private linksawakening.world.entity.motion.BushCrawlerFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int transitionCountdown;
    private boolean linkCollision;
    private boolean actionButtonAHeld;
    private boolean actionButtonBHeld;
    private int linkItemA;
    private int linkItemB;
    private int slot;
    private boolean wasInitialized;
    private boolean crawlOverlayRendered;
    private int renderedVariant;
    private int renderedVisualYOffset;
    private int countdown;
    private int x;
    private int y;
    private boolean liftRequested;
    private linksawakening.world.entity.RoomEntity updated;

    BushCrawlerStep() {}

    linksawakening.world.entity.motion.BushCrawlerMotion.Update run() {
        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        transitionCountdown = inputs.transitionCountdown();
        linkCollision = inputs.linkCollision();
        actionButtonAHeld = inputs.actionButtonAHeld();
        actionButtonBHeld = inputs.actionButtonBHeld();
        linkItemA = inputs.linkItemA();
        linkItemB = inputs.linkItemB();
        if ((entity.type() & 0xFF) != BushCrawlerMotion.ENTITY_BUSH_CRAWLER) {
            throw new IllegalArgumentException(
                    "Unsupported Bush Crawler motion entity type: 0x" + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Bush Crawler random-byte supplier cannot be null");
        }

        slot = entity.slot();
        wasInitialized = owner.initialized[slot];
        if (!wasInitialized) {
            owner.initialize(slot);
        }
        if (owner.privateState1[slot] == 0) {
            owner.privateState1[slot] = 1;
            owner.privateState4[slot] = ((entity.x() & 0xFF) >>> 4) & 0x01;
        }

        crawlOverlayRendered = owner.privateState1[slot] != 2 && owner.privateState2[slot] == 4;
        renderedVariant = owner.privateState1[slot] == 2 || crawlOverlayRendered
                ? (wasInitialized ? owner.presentationVariant[slot] : entity.spriteVariant())
                : owner.privateState4[slot];
        renderedVisualYOffset = owner.privateState2[slot] == 4 ? -4 : 0;
        countdown = transitionCountdown & 0xFF;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        liftRequested = false;

        if (owner.privateState1[slot] == 2) {
            BushCrawlerMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            if (countdown == 0) {
                int randomByte = randomByteSupplier.getAsInt() & 0xFF;
                int random = randomByte & 0x07;
                countdown = 0x20 + (randomByte & 0x1F);
                owner.speedX[slot] = BushCrawlerMotion.SPECIAL_SPEED_X[random];
                owner.speedY[slot] = BushCrawlerMotion.SPECIAL_SPEED_Y[random];
            }
            owner.presentationVariant[slot] = (frameCounter >>> 3) & 0x01;
        } else {
            switch (owner.state[slot]) {
                case 0 -> {
                    if (!advanceState0()) return completedResult;
                }
                case 1 -> {
                    if (!advanceState1()) return completedResult;
                }
                default -> throw new IllegalStateException("Invalid Bush Crawler state: " + owner.state[slot]);
            }
        }

        updated = EntitySnapshots.withMotion(
                entity, x, y, renderedVariant, entity.entityFlipAttribute(), entity.z(), -1, false);
        return new BushCrawlerMotion.Update(
                updated,
                owner.state[slot],
                countdown,
                owner.privateState1[slot],
                owner.privateState4[slot],
                renderedVisualYOffset,
                liftRequested,
                owner.privateState1[slot] == 2,
                crawlOverlayRendered);
    }

    private linksawakening.world.entity.motion.BushCrawlerMotion.Update completedResult;

    private boolean advanceState0() {
        owner.privateState2[slot] = 0;
        if (countdown == 0 && BushCrawlerMotion.startsCrawl(x, y, linkEntityX, linkEntityY)) {
            owner.state[slot] = 1;
            int direction = BushCrawlerMotion.directionToLink(x, y, linkEntityX, linkEntityY);
            owner.speedX[slot] = BushCrawlerMotion.SPEED_X_BY_DIRECTION[direction];
            owner.speedY[slot] = BushCrawlerMotion.SPEED_Y_BY_DIRECTION[direction];
            countdown = 0x30;
        } else if (linkCollision
                && BushCrawlerMotion.powerBraceletHeld(actionButtonAHeld, actionButtonBHeld, linkItemA, linkItemB)) {
            liftRequested = true;
        }

        return true;
    }

    private boolean advanceState1() {
        owner.privateState2[slot] = 4;
        if (countdown == 0) {
            countdown = 0x20;
            owner.state[slot] = 0;
        } else {
            BushCrawlerMotion.Position moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            if (moved.collided()) {
                countdown = 0x20;
                owner.state[slot] = 0;
            } else {
                owner.presentationVariant[slot] = (frameCounter >>> 3) & 0x01;
            }
        }

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.BushCrawlerMotion.Update run(
            BushCrawlerMotion owner, linksawakening.world.entity.motion.BushCrawlerFrame inputs) {
        if (inUse) {
            return new BushCrawlerStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        randomByteSupplier = null;
        backgroundCollision = null;
        transitionCountdown = 0;
        linkCollision = false;
        actionButtonAHeld = false;
        actionButtonBHeld = false;
        linkItemA = 0;
        linkItemB = 0;
        slot = 0;
        wasInitialized = false;
        crawlOverlayRendered = false;
        renderedVariant = 0;
        renderedVisualYOffset = 0;
        countdown = 0;
        x = 0;
        y = 0;
        liftRequested = false;
        updated = null;
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
            this.updated = null;
            this.completedResult = null;
        }
    }
}
