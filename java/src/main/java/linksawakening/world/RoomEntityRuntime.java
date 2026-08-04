package linksawakening.world;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Mutable per-room entity state for the handler work that can be expressed by
 * the current room snapshot model. ROM display lists remain immutable; this
 * class only advances handler-owned status and sprite-variant fields.
 */
public final class RoomEntityRuntime {
    private static final int ENTITY_PIECE_OF_POWER = 0x33;
    private static final int ENTITY_BUTTERFLY = 0x6E;
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_GHOST = 0xD4;
    private static final int ENTITY_ROOSTER = 0xD5;

    private final RoomEntity[] slots;
    private EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;
    private final boolean indoorRoom;
    private final IntSupplier defaultRandomByteSupplier;
    private final RomRandomByteSource fallbackRomRandomByteSource;
    private FollowingNpcState followingNpcState = FollowingNpcState.none();
    private final ButterflyMotion butterflyMotion = new ButterflyMotion();
    private final KeeseMotion keeseMotion = new KeeseMotion();
    private final RoamingEnemyMotion roamingEnemyMotion = new RoamingEnemyMotion();
    private final FollowingNpcMotion followingNpcMotion = new FollowingNpcMotion();
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dyingCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    private RoomEntityRuntime(RoomEntitySnapshot initial, boolean indoorRoom,
                               IntSupplier defaultRandomByteSupplier) {
        this.slots = initial.slots().toArray(RoomEntity[]::new);
        this.spriteSelection = initial.spriteSelection();
        this.spriteTiles = initial.spriteTiles();
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;
        this.fallbackRomRandomByteSource = defaultRandomByteSupplier == null
            ? new RomRandomByteSource() : null;
        for (RoomEntity entity : slots) {
            if (entity.type() == ENTITY_GHOST || entity.type() == ENTITY_ROOSTER) {
                followingNpcMotion.initialize(entity.slot(), entity.type());
            }
        }
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial) {
        return from(initial, false);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier);
    }

    /** Advances the ROM handlers that have deterministic frame-only variants. */
    public void tick(int frameCounter) {
        IntSupplier randomByteSupplier = defaultRandomByteSupplier;
        if (randomByteSupplier == null) {
            fallbackRomRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
            randomByteSupplier = fallbackRomRandomByteSource;
        }
        tick(frameCounter, 0, 0, randomByteSupplier, null);
    }

    /**
     * Advances the active entity handlers with Link's ROM entity coordinates
     * (hLinkPositionX/Y), not Java's top-left sprite coordinates. The
     * random-byte supplier is deliberately injected so ROM handlers can be
     * tested independently from the host's entropy source.
     */
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier) {
        tick(frameCounter, linkEntityX, linkEntityY, randomByteSupplier, null);
    }

    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        Objects.requireNonNull(randomByteSupplier, "randomByteSupplier");
        int frame = frameCounter & 0xFF;
        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            if (!entity.loaded()) {
                continue;
            }
            if (entity.sourceLoadOrder() == -1 && isDisabledFollower(entity.type())) {
                clearEntity(entity.slot());
                continue;
            }

            EntityStatus status = entity.status();
            boolean wasInitializing = status == EntityStatus.INIT;
            if (status == EntityStatus.DYING) {
                if (dyingCountdown[entity.slot()] > 0) {
                    dyingCountdown[entity.slot()]--;
                }
                if (dyingCountdown[entity.slot()] == 0) {
                    disableEntityWithoutPersistence(entity.slot());
                }
                continue;
            } else if (status == EntityStatus.INIT) {
                status = EntityStatus.ACTIVE;
                initializeEntityTimers(entity);
                if (entity.type() == ENTITY_KEESE) {
                    keeseMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_OCTOROK) {
                    roamingEnemyMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_GHOST || entity.type() == ENTITY_ROOSTER) {
                    followingNpcMotion.initialize(entity.slot(), entity.type());
                }
            } else if (status == EntityStatus.ACTIVE) {
                decrementSlowTransitionCountdown(entity.slot(), frame);
                if (shouldDisappear(entity)) {
                    if (slowTransitionCountdown[entity.slot()] == 0) {
                        clearEntity(entity.slot());
                        continue;
                    }
                }
            }
            RoomEntity updated = entity;
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_BUTTERFLY) {
                updated = butterflyMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_KEESE) {
                updated = keeseMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_OCTOROK) {
                updated = roamingEnemyMotion.advance(entity, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && (entity.type() == ENTITY_GHOST || entity.type() == ENTITY_ROOSTER)) {
                updated = followingNpcMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    backgroundCollision);
            }
            int variant = variantFor(updated, frame);
            if (status == EntityStatus.ACTIVE && shouldDisappear(entity)) {
                variant = (slowTransitionCountdown[entity.slot()] & 0x01) != 0 ? 0 : -1;
            }
            if (status != entity.status() || variant != entity.spriteVariant()
                || updated.x() != entity.x() || updated.y() != entity.y()
                || updated.z() != entity.z()) {
                slots[index] = new RoomEntity(
                    updated.slot(), updated.sourceLoadOrder(), updated.type(), updated.x(), updated.y(),
                    status, entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
                    entity.spriteTileOffset(), updated.z());
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

        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
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

    /**
     * Runs the ported portion of DefaultEnemyDamageCollisionHandler for the
     * currently supported enemy families. The four sword arguments are the
     * ROM's wC140..wC143 rectangle; non-positive width/height disables sword
     * collision for this pass.
     */
    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight) {
        List<EntityCombatEvent> events = new ArrayList<>();
        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityCombatRules.supportsEnemyCollision(entity.type())) {
                continue;
            }

            boolean linkCollision = !linkAirborne && linkInteractive
                && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
            boolean swordHit = swordCollisionActive
                && RoomEntityCombatRules.overlapsSword(
                    entity, swordX, swordWidth, swordY, swordHeight);
            if (!linkCollision && !swordHit) {
                continue;
            }

            if (swordHit && RoomEntityCombatRules.canBeKilledByBasicSword(entity.type())) {
                dyingCountdown[entity.slot()] = 0x40;
                slots[entity.slot()] = withStatus(entity, EntityStatus.DYING);
            }
            events.add(new EntityCombatEvent(
                entity.slot(), entity.type(),
                linkCollision ? RoomEntityCombatRules.contactDamage(entity.type()) : 0,
                swordHit));
        }
        return List.copyOf(events);
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
        dyingCountdown[slot] = 0;
        butterflyMotion.clear(slot);
        keeseMotion.clear(slot);
        roamingEnemyMotion.clear(slot);
        followingNpcMotion.clear(slot);
        slots[slot] = RoomEntity.disabled(slot);
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8
            ? 1 << entity.sourceLoadOrder() : 0;
    }

    public RoomEntitySnapshot snapshot() {
        return new RoomEntitySnapshot(Arrays.asList(slots), spriteSelection, spriteTiles);
    }

    void setSpriteSelection(EntitySpriteSelection selection) {
        spriteSelection = selection;
    }

    void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        followingNpcState = state;
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return slowTransitionCountdown[slot];
    }

    int keeseState(int slot) {
        return keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        return keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return keeseMotion.speedY(slot);
    }

    int octorokState(int slot) {
        return roamingEnemyMotion.state(slot);
    }

    int octorokTransitionCountdown(int slot) {
        return roamingEnemyMotion.transitionCountdown(slot);
    }

    int octorokDirection(int slot) {
        return roamingEnemyMotion.direction(slot);
    }

    int octorokSpeedX(int slot) {
        return roamingEnemyMotion.speedX(slot);
    }

    int octorokSpeedY(int slot) {
        return roamingEnemyMotion.speedY(slot);
    }

    int butterflyPrivateStateX(int slot) {
        return butterflyMotion.privateStateX(slot);
    }

    int butterflyPrivateStateY(int slot) {
        return butterflyMotion.privateStateY(slot);
    }

    int dyingCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return dyingCountdown[slot];
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

    private boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !followingNpcState.roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST ->
                followingNpcState.ghostFollowingState() != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE ->
                !followingNpcState.marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW ->
                !followingNpcState.bowWowFollowing();
            default -> false;
        };
    }

    private static RoomEntity withStatus(RoomEntity entity, EntityStatus status) {
        return new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            status, entity.spriteDefinition(), entity.spriteVariant(), entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    private void disableEntityWithoutPersistence(int slot) {
        slowTransitionCountdown[slot] = 0;
        slowTimerInitialized[slot] = false;
        dyingCountdown[slot] = 0;
        butterflyMotion.clear(slot);
        keeseMotion.clear(slot);
        followingNpcMotion.clear(slot);
        slots[slot] = RoomEntity.disabled(slot);
    }
}
