package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$06 Zol/Gel inching, leaping, and split state machine. */
public final class ZolGelMotion {
    private boolean gelClingingToLink;

    public boolean readGelClingingToLink() {
        return gelClingingToLink;
    }

    public boolean writeGelClingingToLink(boolean value) {
        return gelClingingToLink = value;
    }

    static final int ENTITY_ZOL = 0x1B;
    static final int ENTITY_GEL = 0x1C;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] flashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitZol and EntityInitWithRandomDirection for Gel. */
    public void initialize(int slot, int entityType, IntSupplier randomByteSupplier) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        privateCountdown3[slot] = 0;
        direction[slot] = entityType == ENTITY_GEL ? randomByteSupplier.getAsInt() & 0x03 : 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        initialized[slot] = true;
    }

    public void onSwordHit(int slot) {
        flashCountdown[slot] = 0x18;
    }

    public void onLinkCollision(int slot) {
        initialized[slot] = true;
        if (state[slot] != 4) {
            transitionCountdown[slot] = 0x80;
            state[slot] = 4;
        }
    }

    public boolean skipsEnemyCollision(int slot) {
        return state[slot] == 4 || privateCountdown3[slot] != 0;
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        return ZolGelMotion.advanceBody(
                this, entity, linkEntityX, linkEntityY, linkEntityZ, randomByteSupplier, backgroundCollision);
    }

    public Update advance(ZolGelFrame inputs) {
        return ZolGelMotion.advanceBody(this, inputs);
    }

    /** Initializes a SpawnNewEntity Gel without consuming its init random byte. */
    public void prepareSpawnedGel(int slot) {
        resetForGel(slot);
        initialized[slot] = true;
        speedZ[slot] = 0x20;
    }

    /** Mirrors the special Zol state written by ChestWithItemEntityHandler. */
    public void prepareChestSpawn(int slot) {
        resetForGel(slot);
        initialized[slot] = true;
        state[slot] = 3;
        speedX[slot] = 0x08;
        speedZ[slot] = 0x18;
        privateCountdown1[slot] = 0x50;
    }

    public void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        privateCountdown3[slot] = 0;
        flashCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    void decrementCountdowns(int slot) {
        // GelState4Handler only decreases its transition countdown when the
        // ROM sees joypad input; state 4 is therefore intentionally excluded
        // from the ordinary per-frame countdown pass.
        if (state[slot] != 4 && transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
        if (privateCountdown3[slot] > 0) {
            privateCountdown3[slot]--;
        }
        if (flashCountdown[slot] > 0) {
            flashCountdown[slot]--;
        }
    }

    void decreaseTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    int[] physics(
            RoomEntity entity, int x, int y, RoomEntityBackgroundInteraction backgroundInteraction, int frameCounter) {
        // ZolGelPhysics still updates position while private countdown 1 is
        // active, but the ROM skips its background helper for that frame. When
        // it does call the helper, it temporarily exposes ignore-hits value
        // $02 to the rich room probe.
        RoomEntityBackgroundInteraction physicsInteraction =
                privateCountdown1[entity.slot()] == 0 ? backgroundInteraction : null;
        return move(entity, x, y, physicsInteraction, 0x02, frameCounter);
    }

    void applyClingingBackgroundProbe(
            RoomEntity entity,
            int x,
            int y,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter) {
        {
        }
        if (speedX[entity.slot()] != 0) {
            backgroundInteraction.probe(
                    entity, signedByte(speedX[entity.slot()]) < 0 ? 1 : 0, x, y, ignoreHitsCountdown, frameCounter);
        }
        if (speedY[entity.slot()] != 0) {
            backgroundInteraction.probe(
                    entity, signedByte(speedY[entity.slot()]) < 0 ? 2 : 3, x, y, ignoreHitsCountdown, frameCounter);
        }
    }

    void resetForGel(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        privateCountdown3[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZ[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        flashCountdown[slot] = 0;
    }

    private int[] move(
            RoomEntity entity,
            int x,
            int y,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter) {
        int slot = entity.slot();
        int movedX = ZolGelMotion.addSpeedToPosition(x, this.speedX[slot], this.speedXAccumulator, slot);
        int movedY = ZolGelMotion.addSpeedToPosition(y, this.speedY[slot], this.speedYAccumulator, slot);
        {
            if (movedX != x
                    && backgroundInteraction
                            .probe(
                                    entity,
                                    ZolGelMotion.signedByte(this.speedX[slot]) < 0 ? 1 : 0,
                                    movedX,
                                    y,
                                    ignoreHitsCountdown,
                                    frameCounter)
                            .blocked()) {
                movedX = x;
            }
            if (movedY != y
                    && backgroundInteraction
                            .probe(
                                    entity,
                                    ZolGelMotion.signedByte(this.speedY[slot]) < 0 ? 2 : 3,
                                    movedX,
                                    movedY,
                                    ignoreHitsCountdown,
                                    frameCounter)
                            .blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        return ZolGelMotion.vectorTowardsLinkBody(entityX, entityY, linkX, linkY, length);
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(
            RoomEntity entity, /* Optional split outcome; absent until the Zol transforms into Gel. */
            Split split,
            boolean clinging) {}

    public record Split(int originalX, int originalY, int originalZ) {}

    record Vector(int x, int y) {}

    final ZolGelStep zolGelStep = new ZolGelStep(this);

    static ZolGelMotion.Update advanceBody(
            ZolGelMotion owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return owner.advance(new ZolGelFrame(
                entity,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision),
                0,
                0,
                false));
    }

    static ZolGelMotion.Update advanceBody(ZolGelMotion owner, ZolGelFrame inputs) {
        return owner.zolGelStep.run(inputs);
    }

    static ZolGelMotion.Vector vectorTowardsLinkBody(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = ZolGelMotion.signedByte((linkX - entityX) & 0xFF);
        int distanceY = ZolGelMotion.signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new ZolGelMotion.Vector(x & 0xFF, y & 0xFF);
    }

    public record ZolGelFrame(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int frameCounter,
            int ignoreHitsCountdown,
            boolean joypadHeld) {
        public ZolGelFrame {
            backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
        }
    }

    static final class ZolGelStep {
        private final linksawakening.world.entity.motion.ZolGelMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ZolGelMotion.ZolGelFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        private int linkEntityZ;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int frameCounter;
        private int ignoreHitsCountdown;
        private boolean joypadHeld;
        private int slot;
        private int z;
        private boolean hitGround;
        private int type;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional split outcome; absent until the Zol transforms into Gel. */ private linksawakening.world.entity
                        .motion.ZolGelMotion.Split
                split;
        private int x;
        private int y;
        private boolean clinging;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        ZolGelStep(linksawakening.world.entity.motion.ZolGelMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.ZolGelMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceClingingOrFree()) return completedResult;
            if (!buildSnapshot()) return completedResult;

            return new ZolGelMotion.Update(updated, split, clinging);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ZolGelMotion.Update completedResult;

        private boolean prepareFrame() {

            entity = inputs.entity();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkEntityZ = inputs.linkEntityZ();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundInteraction = inputs.backgroundInteraction();
            frameCounter = inputs.frameCounter();
            ignoreHitsCountdown = inputs.ignoreHitsCountdown();
            joypadHeld = inputs.joypadHeld();
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot, entity.type(), randomByteSupplier);
            }
            owner.decrementCountdowns(slot);

            z = ZolGelMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 3) & 0xFF;
            hitGround = (z & 0x80) != 0;
            if (hitGround) {
                z = 0;
                owner.speedZ[slot] = 0;
            }

            type = entity.type();
            split = null;
            if (type == ENTITY_ZOL && owner.flashCountdown[slot] == 8) {
                owner.flashCountdown[slot] = 0;
                int originalX = entity.x();
                int originalY = entity.y();
                int originalZ = z;
                owner.resetForGel(slot);
                owner.speedZ[slot] = 0x20;
                split = new ZolGelMotion.Split(originalX, originalY, originalZ);
                type = ENTITY_GEL;
            }

            if (owner.privateCountdown2[slot] == 0) {
                owner.spriteVariant[slot] = owner.state[slot] & 0x01;
                if (owner.spriteVariant[slot] == 1) {
                    owner.privateCountdown2[slot] = 8;
                }
            }

            x = entity.x();
            y = entity.y();
            clinging = type == ENTITY_GEL && owner.state[slot] == 4 && owner.transitionCountdown[slot] != 0;
            return true;
        }

        private boolean advanceClingingOrFree() {

            if (clinging) {
                advanceClinging();
            } else {
                if (owner.state[slot] == 0) {
                    int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                    x = moved[0];
                    y = moved[1];
                    if (owner.transitionCountdown[slot] == 0) {
                        owner.transitionCountdown[slot] = 7;
                        owner.state[slot] = 1;
                        ZolGelMotion.Vector vector = ZolGelMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 4);
                        owner.speedX[slot] = vector.x();
                        owner.speedY[slot] = vector.y();
                    }
                } else if (owner.state[slot] == 1) {
                    if (owner.transitionCountdown[slot] == 0) {
                        owner.transitionCountdown[slot] = 0x10;
                        owner.speedX[slot] = 0;
                        owner.speedY[slot] = 0;
                        if ((randomByteSupplier.getAsInt() & 0x0F) != 0) {
                            // IncrementEntityState ($06:7D0B) returns HL pointing
                            // to wEntitiesStateTable, not the countdown table.
                            // LD [HL],B ($7D0E) cancels the leap: inch again in
                            // state zero after the retained $10-frame pause.
                            owner.state[slot] = 0;
                            int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                            x = moved[0];
                            y = moved[1];
                        } else {
                            owner.transitionCountdown[slot] = 0x50;
                            owner.state[slot] = 2;
                        }
                    } else {
                        int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                        x = moved[0];
                        y = moved[1];
                    }
                } else if (owner.state[slot] == 2) {
                    if (owner.transitionCountdown[slot] == 0) {
                        owner.state[slot] = 3;
                        ZolGelMotion.Vector vector =
                                ZolGelMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x10);
                        owner.speedX[slot] = vector.x();
                        owner.speedY[slot] = vector.y();
                        owner.speedZ[slot] = 0x20;
                    } else {
                        owner.speedX[slot] = (owner.transitionCountdown[slot] & 0x04) == 0 ? 0x08 : 0xF8;
                        owner.speedY[slot] = 0;
                        int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                        x = moved[0];
                        y = moved[1];
                    }
                } else if (owner.state[slot] == 3) {
                    int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                    x = moved[0];
                    y = moved[1];
                    if (hitGround) {
                        owner.state[slot] = 0;
                    }
                } else if (owner.state[slot] == 4) {
                    if (owner.transitionCountdown[slot] == 0) {
                        owner.privateCountdown3[slot] = 0x30;
                        ZolGelMotion.Vector vector =
                                ZolGelMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x10);
                        owner.speedX[slot] = vector.x();
                        owner.speedY[slot] = vector.y();
                        owner.speedZ[slot] = 0x20;
                        z = (z + 1) & 0xFF;
                        owner.state[slot] = 3;
                    }
                }
            }
            return true;
        }

        private boolean buildSnapshot() {

            updated = new RoomEntity(
                    entity.slot(),
                    entity.sourceLoadOrder(),
                    type,
                    x,
                    y,
                    entity.status(),
                    entity.spriteDefinition(),
                    owner.spriteVariant[slot],
                    entity.entityFlipAttribute(),
                    entity.spriteTileOffset(),
                    z,
                    -1,
                    false);
            return true;
        }

        private void advanceClinging() {
            int xOffset = ((owner.transitionCountdown[slot] >>> 1) & 0x07) - 4;
            int yOffset = ((owner.transitionCountdown[slot] >>> 2) & 0x07) - 4;
            x = (linkEntityX - xOffset) & 0xFF;
            y = (linkEntityY - yOffset) & 0xFF;
            z = linkEntityZ & 0xFF;
            owner.applyClingingBackgroundProbe(entity, x, y, backgroundInteraction, ignoreHitsCountdown, frameCounter);
            if (joypadHeld) {
                owner.decreaseTransitionCountdown(slot);
                owner.decreaseTransitionCountdown(slot);
                owner.decreaseTransitionCountdown(slot);
            }
        }

        private boolean inUse;

        linksawakening.world.entity.motion.ZolGelMotion.Update run(
                linksawakening.world.entity.motion.ZolGelMotion.ZolGelFrame inputs) {
            if (inUse) {
                return new ZolGelStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            linkEntityX = 0;
            linkEntityY = 0;
            linkEntityZ = 0;
            randomByteSupplier = null;
            backgroundInteraction = null;
            frameCounter = 0;
            ignoreHitsCountdown = 0;
            joypadHeld = false;
            slot = 0;
            z = 0;
            hitGround = false;
            type = 0;
            split = null;
            x = 0;
            y = 0;
            clinging = false;
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
                this.backgroundInteraction = null;
                this.split = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
