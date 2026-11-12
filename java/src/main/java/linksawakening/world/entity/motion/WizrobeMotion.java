package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;

/** Bank-$06 WizrobeEntityHandler state machine and launch timing. */
public final class WizrobeMotion {
    static final int ENTITY_WIZROBE = 0x21;
    static final int ENTITY_WIZROBE_PROJECTILE = 0x22;

    private static final int[] PROJECTILE_OFFSET_X = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] PROJECTILE_OFFSET_Y = {0x00, 0x00, 0xF8, 0x08};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] currentSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, int spriteVariant) {
        reset(slot);
        currentSpriteVariant[slot] = spriteVariant;
        initialized[slot] = true;
    }

    /** Mirrors EntityInitWizrobe: transition $80 and sprite variant decremented. */
    public void initializeFromRoom(int slot, int spriteVariant) {
        reset(slot);
        int decremented = (spriteVariant - 1) & 0xFF;
        currentSpriteVariant[slot] = decremented == 0xFF ? -1 : decremented;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            int transitionCountdown,
            int currentPhysicsFlags) {
        return new WizrobeFrameStep(
                        this,
                        entity,
                        linkEntityX,
                        linkEntityY,
                        randomByteSupplier,
                        transitionCountdown,
                        currentPhysicsFlags)
                .run();
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot] & 0xFF;
    }

    public int direction(int slot) {
        return direction[slot] & 0x03;
    }

    public int privateState1(int slot) {
        return privateState1[slot] & 0xFF;
    }

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot] & 0xFF;
    }

    public int nextSpriteVariant(int slot) {
        return currentSpriteVariant[slot];
    }

    ProjectileSpawn projectileSpawn(RoomEntity source, int projectileDirection) {
        int directionIndex = projectileDirection & 0x03;
        return new ProjectileSpawn(
                (source.x() + signedByte(PROJECTILE_OFFSET_X[directionIndex])) & 0xFF,
                (source.y() + signedByte(PROJECTILE_OFFSET_Y[directionIndex])) & 0xFF,
                directionIndex);
    }

    static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private void reset(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        currentSpriteVariant[slot] = 0;
    }

    static int normalizeVariant(int variant) {
        return variant == 0xFF ? -1 : variant & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    static void checkByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    public record ProjectileSpawn(int x, int y, int direction) {}

    public record Update(
            RoomEntity entity, int transitionCountdown, int physicsFlags, ProjectileSpawn projectileSpawn) {}
}
