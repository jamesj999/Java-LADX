package linksawakening.world;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;

import java.util.Arrays;

/**
 * Mutable per-room entity state for the handler work that can be expressed by
 * the current room snapshot model. ROM display lists remain immutable; this
 * class only advances handler-owned status and sprite-variant fields.
 */
public final class RoomEntityRuntime {
    private static final int ENTITY_PIECE_OF_POWER = 0x33;
    private static final int ENTITY_BUTTERFLY = 0x6E;

    private final RoomEntity[] slots;
    private final EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;
    private final boolean indoorRoom;
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    private RoomEntityRuntime(RoomEntitySnapshot initial, boolean indoorRoom) {
        this.slots = initial.slots().toArray(RoomEntity[]::new);
        this.spriteSelection = initial.spriteSelection();
        this.spriteTiles = initial.spriteTiles();
        this.indoorRoom = indoorRoom;
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial) {
        return from(initial, false);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom);
    }

    /** Advances the ROM handlers that have deterministic frame-only variants. */
    public void tick(int frameCounter) {
        int frame = frameCounter & 0xFF;
        for (int index = 0; index < slots.length; index++) {
            RoomEntity entity = slots[index];
            if (!entity.loaded()) {
                continue;
            }

            EntityStatus status = entity.status();
            if (status == EntityStatus.INIT) {
                status = EntityStatus.ACTIVE;
                initializeEntityTimers(entity);
            } else if (status == EntityStatus.ACTIVE) {
                decrementSlowTransitionCountdown(entity.slot(), frame);
                if (shouldDisappear(entity)) {
                    if (slowTransitionCountdown[entity.slot()] == 0) {
                        clearEntity(entity.slot());
                        continue;
                    }
                }
            }
            int variant = variantFor(entity, frame);
            if (status == EntityStatus.ACTIVE && shouldDisappear(entity)) {
                variant = (slowTransitionCountdown[entity.slot()] & 0x01) != 0 ? 0 : -1;
            }
            if (status != entity.status() || variant != entity.spriteVariant()) {
                slots[index] = new RoomEntity(
                    entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
                    status, entity.spriteDefinition(), variant, entity.entityFlipAttribute());
            }
        }
    }

    /**
     * Runs the room-entity portion of PickableCollectIfNeeded. A non-null
     * event means the entity completed its pickup collision and its first-eight
     * persistence bit is ready for the room session to record.
     */
    public EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkPixelX,
                                             int linkPixelY,
                                             boolean linkAirborne,
                                             boolean linkInteractive) {
        if (linkAirborne || !linkInteractive) {
            return null;
        }

        for (RoomEntity entity : slots) {
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityPickupRules.isPickable(entity.type())
                || !RoomEntityPickupRules.collisionCadenceMatches(frameCounter, entity.slot())
                || !RoomEntityPickupRules.overlapsLink(entity, linkPixelX, linkPixelY)) {
                continue;
            }

            int persistentClearMask = persistentClearMask(entity);
            if (requiresHeldPickupTransition(entity.type())) {
                slots[entity.slot()] = withStatus(entity, EntityStatus.LIFTED);
            } else {
                clearEntity(entity.slot());
            }
            return new EntityPickupEvent(entity.slot(), entity.type(), persistentClearMask);
        }
        return null;
    }

    /** Unloads a slot and returns the persistent first-eight load-order bit. */
    public int clearEntity(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        RoomEntity entity = slots[slot];
        if (!entity.loaded()) {
            return 0;
        }
        slowTransitionCountdown[slot] = 0;
        slowTimerInitialized[slot] = false;
        slots[slot] = RoomEntity.disabled(slot);
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8
            ? 1 << entity.sourceLoadOrder() : 0;
    }

    public RoomEntitySnapshot snapshot() {
        return new RoomEntitySnapshot(Arrays.asList(slots), spriteSelection, spriteTiles);
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return slowTransitionCountdown[slot];
    }

    private static int variantFor(RoomEntity entity, int frameCounter) {
        if (!entity.spriteDefinition().supported() || entity.spriteDefinition().variantCount() < 2) {
            return entity.spriteVariant();
        }
        return switch (entity.type()) {
            case ENTITY_PIECE_OF_POWER -> (frameCounter >>> 3) & 0x01;
            case ENTITY_BUTTERFLY -> ((frameCounter + entity.slot() * 8) >>> 3) & 0x01;
            case 0x70, 0x73 -> (frameCounter >>> 4) & 0x01;
            default -> entity.spriteVariant();
        };
    }

    private void initializeEntityTimers(RoomEntity entity) {
        if (indoorRoom && RoomEntityPickupRules.usesIndoorDefaultSlowTimer(entity.type())) {
            slowTransitionCountdown[entity.slot()] = 0x80;
            slowTimerInitialized[entity.slot()] = true;
        }
    }

    private void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0 && slowTimerInitialized[slot]
            && slowTransitionCountdown[slot] > 0) {
            slowTransitionCountdown[slot]--;
        }
    }

    private boolean shouldDisappear(RoomEntity entity) {
        return slowTimerInitialized[entity.slot()]
            && switch (entity.type()) {
                case 0x2D, 0x2E, 0x2F, 0x37, 0x38, 0x3B -> true;
                default -> false;
            }
            && slowTransitionCountdown[entity.slot()] < 0x1C;
    }

    private static int persistentClearMask(RoomEntity entity) {
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8
            ? 1 << entity.sourceLoadOrder() : 0;
    }

    private static boolean requiresHeldPickupTransition(int type) {
        return switch (type) {
            case 0x30, 0x31, 0x33, 0x34, 0x35, 0x36, 0x39, 0x3A, 0x3C -> true;
            default -> false;
        };
    }

    private static RoomEntity withStatus(RoomEntity entity, EntityStatus status) {
        return new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            status, entity.spriteDefinition(), entity.spriteVariant(), entity.entityFlipAttribute());
    }
}
