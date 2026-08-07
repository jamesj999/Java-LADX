package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 WizrobeEntityHandler state machine and launch timing. */
final class WizrobeMotion {
    static final int ENTITY_WIZROBE = 0x21;
    static final int ENTITY_WIZROBE_PROJECTILE = 0x22;

    private static final int[] PROJECTILE_OFFSET_X = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] PROJECTILE_OFFSET_Y = {0x00, 0x00, 0xF8, 0x08};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] currentSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, int spriteVariant) {
        reset(slot);
        currentSpriteVariant[slot] = spriteVariant;
        initialized[slot] = true;
    }

    /** Mirrors EntityInitWizrobe: transition $80 and sprite variant decremented. */
    void initializeFromRoom(int slot, int spriteVariant) {
        reset(slot);
        int decremented = (spriteVariant - 1) & 0xFF;
        currentSpriteVariant[slot] = decremented == 0xFF ? -1 : decremented;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, int transitionCountdown,
                   int currentPhysicsFlags) {
        if ((entity.type() & 0xFF) != ENTITY_WIZROBE) {
            throw new IllegalArgumentException("Unsupported Wizrobe motion entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Wizrobe random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, entity.spriteVariant());
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }

        // RenderActiveEntitySpritesPair runs before the state dispatch. Keep
        // the state-table write separate so the snapshot remains the pixels
        // selected for this handler pass while the next pass sees the newly
        // written variant.
        int renderedVariant = currentSpriteVariant[slot];
        int nextVariant = renderedVariant;
        int countdown = transitionCountdown & 0xFF;
        // State 0..2 explicitly select projectile-noclip physics. State 3
        // writes normal physics on its >=2 countdown path, but its $01/$00
        // branches return without touching the table; preserve the previous
        // value for those two frames.
        int physicsFlags = (state[slot] & 0xFF) == 3
            && privateCountdown1[slot] >= 0x02
            ? 0x02 : ((state[slot] & 0xFF) == 3
                ? currentPhysicsFlags & 0xFF : 0x42);
        ProjectileSpawn projectileSpawn = null;

        switch (state[slot] & 0xFF) {
            case 0 -> {
                if (countdown == 0) {
                    state[slot] = 1;
                    privateState1[slot] = 0x01;
                    privateCountdown1[slot] = 0x20;
                }
                nextVariant = 0xFF;
            }
            case 1 -> {
                if (privateCountdown1[slot] != 0) {
                    nextVariant = (privateState1[slot] & 0x02) != 0 ? 0 : 0xFF;
                } else {
                    state[slot] = (state[slot] + privateState1[slot]) & 0xFF;
                    countdown = 0x30;
                }
            }
            case 2 -> {
                nextVariant = 0;
                if (privateCountdown1[slot] < 0x02) {
                    if (privateCountdown1[slot] == 0x01) {
                        state[slot] = (state[slot] + privateState1[slot]) & 0xFF;
                        if (state[slot] == 0x01) {
                            privateCountdown1[slot] = 0x20;
                        }
                    } else {
                        privateCountdown1[slot] = 0x18;
                    }
                }
            }
            case 3 -> {
                if (privateCountdown1[slot] == 0x28) {
                    projectileSpawn = projectileSpawn(entity, direction[slot]);
                    physicsFlags = 0x02;
                } else if (privateCountdown1[slot] < 0x02) {
                    if (privateCountdown1[slot] == 0x01) {
                        privateState1[slot] = 0xFF;
                        state[slot] = (state[slot] - 1) & 0xFF;
                    } else {
                        privateCountdown1[slot] = 0x40;
                        direction[slot] = directionToLink(
                            entity.x(), entity.y(), linkEntityX, linkEntityY);
                        nextVariant = direction[slot] + 1;
                    }
                }
            }
            default -> throw new IllegalStateException("Invalid Wizrobe state: "
                + state[slot]);
        }

        currentSpriteVariant[slot] = normalizeVariant(nextVariant);
        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), renderedVariant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, countdown, physicsFlags, projectileSpawn);
    }

    void setForTest(int slot, int newState, int transitionCountdown,
                    int newPrivateState1, int newPrivateCountdown1,
                    int newDirection, int spriteVariant) {
        if (newState < 0 || newState > 3) {
            throw new IllegalArgumentException("Wizrobe state out of range: " + newState);
        }
        checkByte(transitionCountdown, "Wizrobe transition countdown");
        checkByte(newPrivateState1, "Wizrobe private state");
        checkByte(newPrivateCountdown1, "Wizrobe private countdown");
        if (newDirection < 0 || newDirection > 3) {
            throw new IllegalArgumentException("Wizrobe direction out of range: " + newDirection);
        }
        if (spriteVariant < -1 || spriteVariant > 4) {
            throw new IllegalArgumentException("Wizrobe sprite variant out of range: "
                + spriteVariant);
        }
        state[slot] = newState;
        privateState1[slot] = newPrivateState1;
        privateCountdown1[slot] = newPrivateCountdown1;
        direction[slot] = newDirection;
        currentSpriteVariant[slot] = spriteVariant;
        initialized[slot] = true;
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot] & 0xFF;
    }

    int direction(int slot) {
        return direction[slot] & 0x03;
    }

    int privateState1(int slot) {
        return privateState1[slot] & 0xFF;
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot] & 0xFF;
    }

    int nextSpriteVariant(int slot) {
        return currentSpriteVariant[slot];
    }

    private ProjectileSpawn projectileSpawn(RoomEntity source, int projectileDirection) {
        int directionIndex = projectileDirection & 0x03;
        return new ProjectileSpawn(
            (source.x() + signedByte(PROJECTILE_OFFSET_X[directionIndex])) & 0xFF,
            (source.y() + signedByte(PROJECTILE_OFFSET_Y[directionIndex])) & 0xFF,
            directionIndex);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
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

    private static int normalizeVariant(int variant) {
        return variant == 0xFF ? -1 : variant & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void checkByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    record ProjectileSpawn(int x, int y, int direction) {
    }

    record Update(RoomEntity entity, int transitionCountdown, int physicsFlags,
                  ProjectileSpawn projectileSpawn) {
    }
}
