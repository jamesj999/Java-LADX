package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$18 ZoraEntityHandler state machine and projectile launch. */
final class ZoraMotion {
    static final int ENTITY_TYPE = 0xCB;
    static final int INITIAL_PHYSICS_FLAGS = 0x02;
    static final int OPTIONS1 = 0x02;
    static final int PROJECTILE_NOCLIP_FLAG = 0x40;

    private static final int[] WATER_X = {
        0x18, 0x28, 0x38, 0x48, 0x58, 0x68, 0x78, 0x88
    };
    private static final int[] WATER_Y = {
        0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80
    };
    private static final int[] Z_OFFSETS = {0, 0, 1, 2, 2, 2, 1, 0};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** The request made by Zora's type-$7D SpawnNewEntity path. */
    record ProjectileSpawn(int x, int y, int speedX, int speedY) {
    }

    record Update(RoomEntity entity, int state, int transitionCountdown, int physicsFlags,
                  ProjectileSpawn projectileSpawn, boolean splash) {
    }

    void initialize(int slot) {
        state[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int transitionCountdown, int physicsFlags,
                   int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier,
                   RoomEntityObjectQuery objectQuery) {
        checkType(entity);
        Objects.requireNonNull(randomByteSupplier, "Zora random-byte supplier");
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int nextPhysicsFlags = physicsFlags & 0xFF;
        RoomEntity updated = entity;
        ProjectileSpawn projectileSpawn = null;
        boolean splash = false;

        switch (state[slot]) {
            case 0 -> {
                nextPhysicsFlags |= PROJECTILE_NOCLIP_FLAG;
                int randomIndex = randomByteSupplier.getAsInt() & 0x07;
                updated = withPosition(entity, WATER_X[randomIndex], WATER_Y[randomIndex]);
                RoomEntityObjectSample object = objectQuery == null
                    ? null : objectQuery.sample(updated);
                if (object != null && object.physicsFlag() == 0x07) {
                    countdown = (randomByteSupplier.getAsInt() & 0x7F) | 0x40;
                    state[slot] = 1;
                }
            }
            case 1 -> {
                if (countdown == 0) {
                    countdown = 0x60;
                    state[slot] = 2;
                }
            }
            case 2 -> {
                if (countdown == 0) {
                    countdown = 0x60;
                    nextPhysicsFlags &= ~PROJECTILE_NOCLIP_FLAG;
                    inertia[slot] = 0;
                    state[slot] = 3;
                } else {
                    int variant = (countdown & 0x04) != 0 ? 2 : 1;
                    updated = withVariant(updated, variant);
                }
            }
            case 3 -> {
                int currentInertia = inertia[slot] & 0xFF;
                int nextInertia = (currentInertia + 1) & 0xFF;
                inertia[slot] = nextInertia;
                int z = Z_OFFSETS[(currentInertia >>> 3) & 0x07];
                updated = withZ(updated, z);

                if (countdown == 0) {
                    state[slot] = 0;
                    countdown = 0;
                    updated = withVariant(updated, 0);
                    splash = true;
                } else {
                    if (countdown == 0x30) {
                        int visualY = (updated.y() - updated.z()) & 0xFF;
                        Vector vector = vectorTowardsLink(updated.x(), visualY,
                            linkEntityX, linkEntityY, 0x18);
                        projectileSpawn = new ProjectileSpawn(updated.x(), visualY,
                            vector.x(), vector.y());
                    }
                    int variant = (countdown >= 0x50 || countdown < 0x20) ? 3 : 4;
                    updated = withVariant(updated, variant);
                }
            }
            default -> throw new IllegalStateException("Invalid Zora state: " + state[slot]);
        }

        return new Update(updated, state[slot], countdown, nextPhysicsFlags,
            projectileSpawn, splash);
    }

    void clear(int slot) {
        state[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int inertia(int slot) {
        return inertia[slot];
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withZ(RoomEntity entity, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            z & 0xFF);
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                             int linkEntityX, int linkEntityY, int length) {
        int distanceX = signedByte(linkEntityX - entityX);
        int distanceY = signedByte(linkEntityY - entityY);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsDominant = absoluteX < absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum > 0xFF || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        int x = yIsDominant ? result : length;
        int y = yIsDominant ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Zora entity");
        }
    }

    private record Vector(int x, int y) {
    }
}
