package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
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
        return ZolGelBehavior.advance(
                this, entity, linkEntityX, linkEntityY, linkEntityZ, randomByteSupplier, backgroundCollision);
    }

    public Update advance(ZolGelFrame inputs) {
        return ZolGelBehavior.advance(this, inputs);
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
        if (backgroundInteraction == null) {
            return;
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
        return ZolGelBehavior.move(this, entity, x, y, backgroundInteraction, ignoreHitsCountdown, frameCounter);
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
        return ZolGelBehavior.vectorTowardsLink(entityX, entityY, linkX, linkY, length);
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(RoomEntity entity, Split split, boolean clinging) {}

    public record Split(int originalX, int originalY, int originalZ) {}

    record Vector(int x, int y) {}
}
