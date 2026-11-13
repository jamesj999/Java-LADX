package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
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
        return wizrobeFrameStep.run(
                entity, linkEntityX, linkEntityY, randomByteSupplier, transitionCountdown, currentPhysicsFlags);
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
            RoomEntity entity,
            int transitionCountdown,
            int physicsFlags, /* Optional emitted projectile; absent when this visit does not fire. */
            ProjectileSpawn projectileSpawn) {}

    final WizrobeFrameStep wizrobeFrameStep = new WizrobeFrameStep(this);

    static final class WizrobeFrameStep {
        private final WizrobeMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private int transitionCountdown;
        private int currentPhysicsFlags;
        private int slot;
        private int renderedVariant;
        private int nextVariant;
        private int countdown;
        private int physicsFlags;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional emitted projectile; absent when this visit does not fire. */ private linksawakening.world.entity
                        .motion.WizrobeMotion.ProjectileSpawn
                projectileSpawn;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        WizrobeFrameStep(WizrobeMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.WizrobeMotion.Update run() {
            if ((entity.type() & 0xFF) != WizrobeMotion.ENTITY_WIZROBE) {
                throw new IllegalArgumentException(
                        "Unsupported Wizrobe motion entity type: 0x" + Integer.toHexString(entity.type()));
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Wizrobe random-byte supplier cannot be null");
            }

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot, entity.spriteVariant());
            }
            if (owner.privateCountdown1[slot] > 0) {
                owner.privateCountdown1[slot]--;
            }

            // RenderActiveEntitySpritesPair runs before the state dispatch. Keep
            // the state-table write separate so the snapshot remains the pixels
            // selected for this handler pass while the next pass sees the newly
            // written variant.
            renderedVariant = owner.currentSpriteVariant[slot];
            nextVariant = renderedVariant;
            countdown = transitionCountdown & 0xFF;
            // State 0..2 explicitly select projectile-noclip physics. State 3
            // writes normal physics on its >=2 countdown path, but its $01/$00
            // branches return without touching the table; preserve the previous
            // value for those two frames.
            physicsFlags = (owner.state[slot] & 0xFF) == 3 && owner.privateCountdown1[slot] >= 0x02
                    ? 0x02
                    : ((owner.state[slot] & 0xFF) == 3 ? currentPhysicsFlags & 0xFF : 0x42);
            projectileSpawn = null;

            switch (owner.state[slot] & 0xFF) {
                case 0 -> {
                    if (!advanceState0()) return completedResult;
                }
                case 1 -> {
                    if (!advanceState1()) return completedResult;
                }
                case 2 -> {
                    if (!advanceState2()) return completedResult;
                }
                case 3 -> {
                    if (!advanceState3()) return completedResult;
                }
                default -> throw new IllegalStateException("Invalid Wizrobe state: " + owner.state[slot]);
            }

            owner.currentSpriteVariant[slot] = WizrobeMotion.normalizeVariant(nextVariant);
            updated = EntitySnapshots.withMotion(
                    entity,
                    entity.x(),
                    entity.y(),
                    renderedVariant,
                    entity.entityFlipAttribute(),
                    entity.z(),
                    -1,
                    false);
            return new WizrobeMotion.Update(updated, countdown, physicsFlags, projectileSpawn);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.WizrobeMotion.Update completedResult;

        private boolean advanceState0() {
            if (countdown == 0) {
                owner.state[slot] = 1;
                owner.privateState1[slot] = 0x01;
                owner.privateCountdown1[slot] = 0x20;
            }
            nextVariant = 0xFF;

            return true;
        }

        private boolean advanceState1() {
            if (owner.privateCountdown1[slot] != 0) {
                nextVariant = (owner.privateState1[slot] & 0x02) != 0 ? 0 : 0xFF;
            } else {
                owner.state[slot] = (owner.state[slot] + owner.privateState1[slot]) & 0xFF;
                countdown = 0x30;
            }

            return true;
        }

        private boolean advanceState2() {
            nextVariant = 0;
            if (owner.privateCountdown1[slot] < 0x02) {
                if (owner.privateCountdown1[slot] == 0x01) {
                    owner.state[slot] = (owner.state[slot] + owner.privateState1[slot]) & 0xFF;
                    if (owner.state[slot] == 0x01) {
                        owner.privateCountdown1[slot] = 0x20;
                    }
                } else {
                    owner.privateCountdown1[slot] = 0x18;
                }
            }

            return true;
        }

        private boolean advanceState3() {
            if (owner.privateCountdown1[slot] == 0x28) {
                projectileSpawn = owner.projectileSpawn(entity, owner.direction[slot]);
                physicsFlags = 0x02;
            } else if (owner.privateCountdown1[slot] < 0x02) {
                if (owner.privateCountdown1[slot] == 0x01) {
                    owner.privateState1[slot] = 0xFF;
                    owner.state[slot] = (owner.state[slot] - 1) & 0xFF;
                } else {
                    owner.privateCountdown1[slot] = 0x40;
                    owner.direction[slot] =
                            WizrobeMotion.directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
                    nextVariant = owner.direction[slot] + 1;
                }
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.WizrobeMotion.Update run(
                linksawakening.world.entity.RoomEntity entity,
                int linkEntityX,
                int linkEntityY,
                java.util.function.IntSupplier randomByteSupplier,
                int transitionCountdown,
                int currentPhysicsFlags) {
            if (inUse) {
                return new WizrobeFrameStep(owner)
                        .run(
                                entity,
                                linkEntityX,
                                linkEntityY,
                                randomByteSupplier,
                                transitionCountdown,
                                currentPhysicsFlags);
            }
            inUse = true;
            slot = 0;
            renderedVariant = 0;
            nextVariant = 0;
            countdown = 0;
            physicsFlags = 0;
            projectileSpawn = null;
            updated = null;
            completedResult = null;

            this.entity = entity;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.randomByteSupplier = randomByteSupplier;
            this.transitionCountdown = transitionCountdown;
            this.currentPhysicsFlags = currentPhysicsFlags;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;
                this.projectileSpawn = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
