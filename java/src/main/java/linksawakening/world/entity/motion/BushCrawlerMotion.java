package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 BushCrawlerEntityHandler movement and lift trigger. */
public final class BushCrawlerMotion {
    static final int ENTITY_BUSH_CRAWLER = 0xBB;
    static final int ENTITY_SWORD = 0x01;
    static final int ENTITY_POWER_BRACELET = 0x03;

    static final int[] SPEED_X_BY_DIRECTION = {0x10, 0xF0, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF0, 0x10};
    // Data_007_41E6 is immediately followed by Data_007_41E8 in bank $07.
    // The handler intentionally indexes across that label boundary for Y.
    static final int[] SPECIAL_SPEED_X = {0x00, 0x0C, 0x10, 0x0C, 0x00, 0xF4, 0xF0, 0xF4};
    static final int[] SPECIAL_SPEED_Y = {0xF0, 0xF4, 0x00, 0x0C, 0x10, 0x0C, 0x00, 0xF4};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] presentationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int privateState1,
            int privateState4,
            int visualYOffset,
            boolean liftRequested,
            boolean specialState,
            boolean crawlOverlayRendered) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public void initializeSpecial(int slot) {
        initialize(slot);
        privateState1[slot] = 2;
    }

    public Update advance(BushCrawlerFrame inputs) {
        return bushCrawlerStep.run(inputs);
    }

    public void clear(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateState4[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int privateState1(int slot) {
        return privateState1[slot];
    }

    public int privateState4(int slot) {
        return privateState4[slot];
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    static boolean startsCrawl(int x, int y, int linkX, int linkY) {
        int distanceY = signedByte(linkY - y);
        if (!inSignedWindow(distanceY, 8)) {
            return false;
        }
        int distanceX = signedByte(linkX - x);
        if (!inSignedWindow(distanceX, 8)) {
            return false;
        }
        return directionToLink(x, y, linkX, linkY) != 2;
    }

    static boolean powerBraceletHeld(
            boolean actionButtonAHeld, boolean actionButtonBHeld, int linkItemA, int linkItemB) {
        if ((linkItemB & 0xFF) == ENTITY_POWER_BRACELET) {
            return actionButtonBHeld;
        }
        return (linkItemA & 0xFF) == ENTITY_POWER_BRACELET && actionButtonAHeld;
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (nextX != x && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (nextY != y && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Position(int x, int y, boolean collided) {}

    final BushCrawlerStep bushCrawlerStep = new BushCrawlerStep(this);

    public record BushCrawlerFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown,
            boolean linkCollision,
            boolean actionButtonAHeld,
            boolean actionButtonBHeld,
            int linkItemA,
            int linkItemB) {
        public BushCrawlerFrame {
            backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        }
    }

    static final class BushCrawlerStep {
        private final BushCrawlerMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.BushCrawlerMotion.BushCrawlerFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
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
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        BushCrawlerStep(BushCrawlerMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

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

        // Absent before initialization or while this reusable execution scratch is inactive.
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
                    && BushCrawlerMotion.powerBraceletHeld(
                            actionButtonAHeld, actionButtonBHeld, linkItemA, linkItemB)) {
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
                linksawakening.world.entity.motion.BushCrawlerMotion.BushCrawlerFrame inputs) {
            if (inUse) {
                return new BushCrawlerStep(owner).run(inputs);
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

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
