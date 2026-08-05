package linksawakening.world;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
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
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_LEEVER = 0x0E;
    private static final int ENTITY_ANTI_FAIRY = 0x15;
    private static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    private static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    private static final int ENTITY_ZOL = 0x1B;
    private static final int ENTITY_GEL = 0x1C;
    private static final int ENTITY_HIDING_ZOL = 0x9B;
    private static final int ENTITY_SPIKE_TRAP = 0x27;
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int ENTITY_PAIRODD_PROJECTILE = 0x58;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
    private static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    private static final int ENTITY_GIBDO = 0x1F;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;
    private static final int ENTITY_GHOST = 0xD4;
    private static final int ENTITY_ROOSTER = 0xD5;
    private static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    private static final int ENTITY_BOW_WOW = 0x6D;

    private final RoomEntity[] slots;
    private EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;
    private final boolean indoorRoom;
    private final IntSupplier defaultRandomByteSupplier;
    private final RomRandomByteSource fallbackRomRandomByteSource;
    private final EntitySpriteHandlerCatalog spriteHandlers;
    private final RomEnemyCombatTables enemyCombatTables;
    private FollowingNpcState followingNpcState = FollowingNpcState.none();
    private LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();
    private int followingLinkZ;
    private int followingLinkDirection;
    private int followingEntityYOffset;
    private final ButterflyMotion butterflyMotion = new ButterflyMotion();
    private final KeeseMotion keeseMotion = new KeeseMotion();
    private final RoamingEnemyMotion roamingEnemyMotion = new RoamingEnemyMotion();
    private final EnemyProjectileMotion enemyProjectileMotion = new EnemyProjectileMotion();
    private final TektiteMotion tektiteMotion = new TektiteMotion();
    private final LeeverMotion leeverMotion = new LeeverMotion();
    private final AntiFairyMotion antiFairyMotion = new AntiFairyMotion();
    private final SparkMotion sparkMotion = new SparkMotion();
    private final ZolGelMotion zolGelMotion = new ZolGelMotion();
    private final HidingZolMotion hidingZolMotion = new HidingZolMotion();
    private final SpikeTrapMotion spikeTrapMotion = new SpikeTrapMotion();
    private final PairoddMotion pairoddMotion = new PairoddMotion();
    private final PairoddProjectileMotion pairoddProjectileMotion =
        new PairoddProjectileMotion();
    private final WaterTektiteMotion waterTektiteMotion = new WaterTektiteMotion();
    private final StalfosAggressiveMotion stalfosAggressiveMotion =
        new StalfosAggressiveMotion();
    private final GibdoMotion gibdoMotion = new GibdoMotion();
    private final PeaHatMotion peaHatMotion = new PeaHatMotion();
    private final ArmosMotion armosMotion = new ArmosMotion();
    private final GhiniMotion ghiniMotion = new GhiniMotion();
    private final HardHatMotion hardHatMotion = new HardHatMotion();
    private final EnemyRecoilMotion enemyRecoilMotion = new EnemyRecoilMotion();
    private final FollowingNpcMotion followingNpcMotion = new FollowingNpcMotion();
    private final BowWowMotion bowWowMotion = new BowWowMotion();
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dyingCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyHealth = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyFlashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyIgnoreHitsCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyProjectileSpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests =
        new ArrayList<>();

    private RoomEntityRuntime(RoomEntitySnapshot initial, boolean indoorRoom,
                               IntSupplier defaultRandomByteSupplier,
                               EntitySpriteHandlerCatalog spriteHandlers,
                               RomEnemyCombatTables enemyCombatTables) {
        this.slots = initial.slots().toArray(RoomEntity[]::new);
        this.spriteSelection = initial.spriteSelection();
        this.spriteTiles = initial.spriteTiles();
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;
        this.fallbackRomRandomByteSource = defaultRandomByteSupplier == null
            ? new RomRandomByteSource() : null;
        this.spriteHandlers = spriteHandlers;
        this.enemyCombatTables = enemyCombatTables;
        for (RoomEntity entity : slots) {
            enemyHealth[entity.slot()] = entity.loaded() ? initialHealth(entity.type()) : 0;
            if (isFollowingNpcType(entity.type())) {
                if (entity.type() == ENTITY_BOW_WOW) {
                    bowWowMotion.initialize(entity.slot());
                } else {
                    followingNpcMotion.initialize(entity.slot(), entity.type());
                }
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
        return new RoomEntityRuntime(initial, indoorRoom, null, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        if (enemyCombatTables == null) {
            throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables);
    }

    /** Advances the ROM handlers that have deterministic frame-only variants. */
    public void tick(int frameCounter) {
        IntSupplier randomByteSupplier = defaultRandomByteSupplier;
        if (randomByteSupplier == null) {
            fallbackRomRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
            randomByteSupplier = fallbackRomRandomByteSource;
        }
        tickInternal(frameCounter, 0, 0, randomByteSupplier, null,
            null, 0, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    /**
     * Advances the active entity handlers with Link's ROM entity coordinates
     * (hLinkPositionX/Y), not Java's top-left sprite coordinates. The
     * random-byte supplier is deliberately injected so ROM handlers can be
     * tested independently from the host's entropy source.
     */
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier, null,
            null, 0, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, null, 0, 0, 0,
            EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier, boolean creditsGameplay) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier, null,
            null, 0, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(),
            creditsGameplay);
    }

    void tick(int frameCounter, int linkEntityX, int linkEntityY,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, linkPositionHistory, linkZ, linkDirection,
            entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, null, projectileLinkState.z(), projectileLinkState.direction(),
            0, projectileLinkState, false);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, linkPositionHistory, linkZ, linkDirection, entityYOffset,
            projectileLinkState, false);
    }

    private List<EntityProjectileEvent> tickInternal(int frameCounter, int linkEntityX,
                      int linkEntityY, IntSupplier randomByteSupplier,
                      RoomEntityBackgroundCollision backgroundCollision,
                      LinkPositionHistory linkPositionHistory,
                      int linkZ, int linkDirection, int entityYOffset,
                      EnemyProjectileCollision.LinkState projectileLinkState,
                      boolean creditsGameplay) {
        Objects.requireNonNull(randomByteSupplier, "randomByteSupplier");
        Objects.requireNonNull(projectileLinkState, "projectileLinkState");
        projectileLaunchRequests.clear();
        Arrays.fill(enemyProjectileSpawnedThisFrame, false);
        List<EntityProjectileEvent> projectileEvents = new ArrayList<>();
        if (linkPositionHistory != null) {
            followingLinkPositionHistory = linkPositionHistory;
        }
        followingLinkZ = linkZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        followingEntityYOffset = entityYOffset & 0xFF;
        int frame = frameCounter & 0xFF;
        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            if (!entity.loaded()) {
                continue;
            }
            if (enemyProjectileSpawnedThisFrame[entity.slot()]) {
                continue;
            }
            RoomEntity originalEntity = entity;
            decrementEnemyCombatCountdowns(entity.slot());
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
                if (entity.type() == ENTITY_OCTOROK || entity.type() == ENTITY_MOBLIN) {
                    roamingEnemyMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_TEKTITE) {
                    tektiteMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_LEEVER) {
                    leeverMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_ANTI_FAIRY) {
                    antiFairyMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (isSparkType(entity.type())) {
                    sparkMotion.initialize(entity.slot(), entity.type());
                }
                if (isZolGelType(entity.type())) {
                    zolGelMotion.initialize(entity.slot(), entity.type(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_HIDING_ZOL) {
                    hidingZolMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_SPIKE_TRAP) {
                    spikeTrapMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_PAIRODD) {
                    pairoddMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_PAIRODD_PROJECTILE) {
                    pairoddProjectileMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_WATER_TEKTITE) {
                    waterTektiteMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_STALFOS_AGGRESSIVE) {
                    stalfosAggressiveMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_GIBDO) {
                    gibdoMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_PEAHAT) {
                    peaHatMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_ARMOS_STATUE) {
                    armosMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_GHINI) {
                    ghiniMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_HARDHAT_BEETLE) {
                    hardHatMotion.initialize(entity.slot());
                }
                if (isFollowingNpcType(entity.type())) {
                    if (entity.type() == ENTITY_BOW_WOW) {
                        bowWowMotion.initialize(entity.slot());
                    } else {
                        followingNpcMotion.initialize(entity.slot(), entity.type());
                    }
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
                && isRoamingEnemyType(entity.type())) {
                // AnimateRoamingEnemy applies the shared recoil before its
                // state-specific movement and collision handler.
                EnemyRecoilMotion.Update recoil = applyEnemyRecoilIfNeeded(
                    entity, backgroundCollision);
                entity = recoil.entity();
                updated = entity;
            }
            if (wasInitializing && entity.type() == ENTITY_LEEVER) {
                // EntityInitLeever calls SetEntitySpriteVariant($FF) before
                // the first active handler dispatch.
                updated = withVariant(entity, -1);
            }
            if (wasInitializing && isSparkType(entity.type())) {
                updated = sparkMotion.applyInitializationOffset(entity);
            }
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
                && isRoamingEnemyType(entity.type())) {
                RoamingEnemyMotion.Update roamingUpdate = roamingEnemyMotion.advance(
                    entity, linkEntityX, linkEntityY, randomByteSupplier,
                    backgroundCollision, creditsGameplay);
                updated = roamingUpdate.entity();
                if (roamingUpdate.launchRequest() != null) {
                    projectileLaunchRequests.add(roamingUpdate.launchRequest());
                    spawnEnemyProjectile(entity, roamingUpdate.launchRequest());
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_TEKTITE) {
                updated = tektiteMotion.advance(entity, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_LEEVER) {
                updated = leeverMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_ANTI_FAIRY) {
                updated = antiFairyMotion.advance(entity, frame, backgroundCollision,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing && isSparkType(entity.type())) {
                updated = sparkMotion.advance(entity, frame, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing && isZolGelType(entity.type())) {
                ZolGelMotion.Update zolGelUpdate = zolGelMotion.advance(entity, linkEntityX,
                    linkEntityY, linkZ, randomByteSupplier, backgroundCollision);
                updated = zolGelUpdate.entity();
                if (zolGelUpdate.split() != null) {
                    updated = applyZolSplit(entity, updated, zolGelUpdate.split());
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_HIDING_ZOL) {
                updated = hidingZolMotion.advance(entity, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision).entity();
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_SPIKE_TRAP) {
                updated = spikeTrapMotion.advance(entity, linkEntityX, linkEntityY,
                    backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_PAIRODD) {
                PairoddMotion.Update pairoddUpdate = pairoddMotion.advance(entity, frame,
                    linkEntityX, linkEntityY, randomByteSupplier,
                    enemyFlashCountdown[entity.slot()] > 0);
                updated = pairoddUpdate.entity();
                if (pairoddUpdate.spawnProjectile()) {
                    spawnPairoddProjectile(entity, linkEntityX, linkEntityY);
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_PAIRODD_PROJECTILE) {
                updated = pairoddProjectileMotion.advance(entity, frame);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isEnemyProjectileType(entity.type())) {
                EnemyProjectileMotion.Update projectileUpdate;
                boolean hasLinkCollision = enemyProjectileMotion.transitionCountdown(entity.slot()) == 0;
                if (hasLinkCollision) {
                    var collisionEvent = EnemyProjectileCollision.check(entity,
                        enemyProjectileMotion.direction(entity.slot()), projectileLinkState);
                    if (collisionEvent.isPresent()) {
                        EntityProjectileEvent event = collisionEvent.orElseThrow();
                        projectileEvents.add(event);
                        if (event.remove()) {
                            disableEntityWithoutPersistence(entity.slot());
                            continue;
                        }
                        projectileUpdate = enemyProjectileMotion.advance(entity, backgroundCollision,
                            event.startsWallTransition());
                    } else {
                        projectileUpdate = enemyProjectileMotion.advance(entity, backgroundCollision);
                    }
                } else {
                    projectileUpdate = enemyProjectileMotion.advance(entity, backgroundCollision);
                }
                if (projectileUpdate.unloaded()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                updated = projectileUpdate.entity();
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_WATER_TEKTITE) {
                updated = waterTektiteMotion.advance(entity, frame, randomByteSupplier,
                    backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_STALFOS_AGGRESSIVE) {
                updated = stalfosAggressiveMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_GIBDO) {
                updated = gibdoMotion.advance(entity, randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_PEAHAT) {
                updated = peaHatMotion.advance(entity, frame, randomByteSupplier,
                    backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_ARMOS_STATUE) {
                updated = armosMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_GHINI) {
                updated = ghiniMotion.advance(entity, frame, randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_HARDHAT_BEETLE) {
                updated = hardHatMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isDynamicFollowingNpc(entity)) {
                if (entity.type() == ENTITY_BOW_WOW) {
                    updated = bowWowMotion.advance(entity, frame, linkEntityX, linkEntityY,
                        followingLinkZ, randomByteSupplier, backgroundCollision);
                } else {
                    updated = followingNpcMotion.advance(entity, frame, linkEntityX, linkEntityY,
                        followingLinkZ, followingLinkDirection, followingEntityYOffset,
                        followingLinkPositionHistory, backgroundCollision);
                }
            }
            int variant = variantFor(updated, frame);
            if (status == EntityStatus.ACTIVE && shouldDisappear(entity)) {
                variant = (slowTransitionCountdown[entity.slot()] & 0x01) != 0 ? 0 : -1;
            }
            if (status != entity.status() || variant != entity.spriteVariant()
                || updated.type() != entity.type()
                || updated.spriteDefinition() != entity.spriteDefinition()
                || updated.x() != originalEntity.x() || updated.y() != originalEntity.y()
                || updated.z() != originalEntity.z()) {
                slots[index] = new RoomEntity(
                    updated.slot(), updated.sourceLoadOrder(), updated.type(), updated.x(), updated.y(),
                    status, updated.spriteDefinition(), variant, updated.entityFlipAttribute(),
                    updated.spriteTileOffset(), updated.z());
            }
        }
        return List.copyOf(projectileEvents);
    }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return List.copyOf(projectileLaunchRequests);
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
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            EnemyAttackContext.standard());
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight,
                                                 EnemyAttackContext attackContext) {
        if (attackContext == null) {
            throw new IllegalArgumentException("Enemy attack context cannot be null");
        }
        List<EntityCombatEvent> events = new ArrayList<>();
        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityCombatRules.supportsEnemyCollision(entity.type())) {
                continue;
            }
            if (entity.type() == ENTITY_LEEVER && !leeverMotion.isChasing(entity.slot())) {
                continue;
            }
            if (entity.type() == ENTITY_PEAHAT && !peaHatMotion.isGrounded(entity)) {
                continue;
            }
            if (entity.type() == ENTITY_PAIRODD
                && !pairoddMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (isZolGelType(entity.type()) && zolGelMotion.skipsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean hidingZolSwordCollision = entity.type() != ENTITY_HIDING_ZOL
                || hidingZolMotion.allowsSwordCollision(entity.slot());
            boolean hidingZolLinkCollision = entity.type() != ENTITY_HIDING_ZOL
                || hidingZolMotion.allowsLinkCollision(entity.slot());
            if (!hidingZolSwordCollision && !hidingZolLinkCollision) {
                continue;
            }
            if (enemyFlashCountdown[entity.slot()] > 0
                || enemyIgnoreHitsCountdown[entity.slot()] > 0) {
                continue;
            }

            boolean linkCollision = !linkAirborne && linkInteractive
                && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
                && hidingZolLinkCollision
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
            boolean swordHit = swordCollisionActive
                && hidingZolSwordCollision
                && RoomEntityCombatRules.overlapsSword(
                    entity, swordX, swordWidth, swordY, swordHeight);
            if (!linkCollision && !swordHit) {
                continue;
            }

            EntityCombatEvent.SoundChannel soundChannel = EntityCombatEvent.SoundChannel.NONE;
            int soundId = -1;
            int enemyDamage = 0;
            int enemySpecialAction = -1;
            if (swordHit) {
                RomEnemyCombatTables.SwordDamageResult swordResult =
                    enemyCombatTables == null ? null
                        : enemyCombatTables.resolveSwordDamage(entity.type(), attackContext);
                int swordDamage = swordResult == null
                    ? RoomEntityCombatRules.basicSwordDamage(entity.type())
                    : swordResult.numericDamage();
                enemyDamage = swordDamage;
                if (swordResult != null) {
                    enemySpecialAction = swordResult.specialAction();
                }
                boolean swordResultApplied = swordResult == null
                    ? swordDamage > 0
                    : !swordResult.ignored();
                if (isRoamingEnemyType(entity.type())) {
                    // EnemyCollidedWithSword applies the default `$30` recoil
                    // before ApplySwordDamagesToEnemy changes health.
                    enemyRecoilMotion.configure(
                        entity.slot(), entity.x(), entity.y(), entity.z(),
                        linkEntityX, linkEntityY, 0x30);
                }
                // ConfigureEntityRecoil reaches StartIgnoringHitsForEntity
                // before ApplySwordDamagesToEnemy, including lethal hits.
                enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                    ? 0x20 : 0x0A;
                if (swordDamage > 0) {
                    enemyHealth[entity.slot()] = Math.max(0,
                        enemyHealth[entity.slot()] - swordDamage);
                }
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = swordResultApplied ? 0x03 : 0x09;
                if (swordDamage > 0 && enemyHealth[entity.slot()] == 0) {
                    dyingCountdown[entity.slot()] = 0x40;
                    slots[entity.slot()] = withStatus(entity, EntityStatus.DYING);
                } else if (swordDamage > 0) {
                    // jr_003_73B6 and StartIgnoringHitsForEntity: a normal
                    // sword hit flashes for $18 frames and suppresses the
                    // next $0A collision passes.
                    enemyFlashCountdown[entity.slot()] = 0x18;
                    enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                        ? 0x20 : 0x0A;
                    if (isZolGelType(entity.type())) {
                        zolGelMotion.onSwordHit(entity.slot());
                    }
                    if (entity.type() == ENTITY_ZOL && spriteHandlers != null) {
                        entity = withDefinition(entity, spriteHandlers.forZolSlimeEye(),
                            entity.spriteVariant());
                        slots[entity.slot()] = entity;
                    }
                }
            }
            if (linkCollision && entity.type() == ENTITY_GEL) {
                zolGelMotion.onLinkCollision(entity.slot());
            }
            events.add(new EntityCombatEvent(
                entity.slot(), entity.type(),
                linkCollision ? contactDamage(entity.type()) : 0,
                swordHit, enemyDamage, enemySpecialAction, soundChannel, soundId,
                EntityCombatEvent.SoundChannel.NONE, -1));
        }
        return List.copyOf(events);
    }

    private int initialHealth(int type) {
        return enemyCombatTables == null
            ? RoomEntityCombatRules.initialHealth(type)
            : enemyCombatTables.initialHealth(type);
    }

    private int contactDamage(int type) {
        return enemyCombatTables == null
            ? RoomEntityCombatRules.contactDamage(type)
            : enemyCombatTables.contactDamage(type);
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
        enemyHealth[slot] = 0;
        enemyFlashCountdown[slot] = 0;
        enemyIgnoreHitsCountdown[slot] = 0;
        enemyRecoilMotion.clear(slot);
        butterflyMotion.clear(slot);
        keeseMotion.clear(slot);
        roamingEnemyMotion.clear(slot);
        tektiteMotion.clear(slot);
        leeverMotion.clear(slot);
        antiFairyMotion.clear(slot);
        sparkMotion.clear(slot);
        zolGelMotion.clear(slot);
        hidingZolMotion.clear(slot);
        spikeTrapMotion.clear(slot);
        pairoddMotion.clear(slot);
        pairoddProjectileMotion.clear(slot);
        enemyProjectileMotion.clear(slot);
        waterTektiteMotion.clear(slot);
        stalfosAggressiveMotion.clear(slot);
        gibdoMotion.clear(slot);
        peaHatMotion.clear(slot);
        armosMotion.clear(slot);
        ghiniMotion.clear(slot);
        hardHatMotion.clear(slot);
        followingNpcMotion.clear(slot);
        bowWowMotion.clear(slot);
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

    private static boolean isFollowingNpcType(int type) {
        return type == ENTITY_GHOST || type == ENTITY_ROOSTER
            || type == ENTITY_MARIN_AT_THE_SHORE || type == ENTITY_BOW_WOW;
    }

    private static boolean isSparkType(int type) {
        return type == ENTITY_SPARK_COUNTER_CLOCKWISE || type == ENTITY_SPARK_CLOCKWISE;
    }

    private static boolean isZolGelType(int type) {
        return type == ENTITY_ZOL || type == ENTITY_GEL;
    }

    private static boolean isRoamingEnemyType(int type) {
        return type == ENTITY_OCTOROK || type == ENTITY_MOBLIN;
    }

    private static boolean isEnemyProjectileType(int type) {
        return type == ENTITY_OCTOROK_ROCK || type == ENTITY_MOBLIN_ARROW;
    }

    private static boolean isDynamicFollowingNpc(RoomEntity entity) {
        return entity.sourceLoadOrder() == -1 && isFollowingNpcType(entity.type());
    }

    private RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated,
                                      ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition = spriteDefinitionFor(ENTITY_GEL);
        int gelVariant = gelDefinition.supported() ? 0 : -1;
        int slot = original.slot();
        RoomEntity gel = new RoomEntity(slot, original.sourceLoadOrder(), ENTITY_GEL,
            (split.originalX() - 4) & 0xFF, split.originalY(), EntityStatus.ACTIVE,
            gelDefinition, gelVariant, original.entityFlipAttribute(), original.spriteTileOffset(),
            split.originalZ());
        enemyHealth[slot] = initialHealth(ENTITY_GEL);
        enemyFlashCountdown[slot] = 0;
        enemyIgnoreHitsCountdown[slot] = 0;
        enemyRecoilMotion.clear(slot);

        int freeSlot = findFreeEntitySlot();
        if (freeSlot >= 0) {
            RoomEntity spawnedGel = new RoomEntity(freeSlot, original.sourceLoadOrder(), ENTITY_GEL,
                (split.originalX() + 8) & 0xFF, split.originalY(), EntityStatus.ACTIVE,
                gelDefinition, gelVariant, original.entityFlipAttribute(), original.spriteTileOffset(),
                split.originalZ());
            slots[freeSlot] = spawnedGel;
            dyingCountdown[freeSlot] = 0;
            enemyHealth[freeSlot] = initialHealth(ENTITY_GEL);
            enemyFlashCountdown[freeSlot] = 0;
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            enemyIgnoreHitsCountdown[freeSlot] = 1;
            enemyRecoilMotion.clear(freeSlot);
            zolGelMotion.prepareSpawnedGel(freeSlot);
        }
        return gel;
    }

    private int findFreeEntitySlot() {
        for (int slot = slots.length - 1; slot >= 0; slot--) {
            if (!slots[slot].loaded()) {
                return slot;
            }
        }
        return -1;
    }

    private void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
            spriteDefinitionFor(ENTITY_PAIRODD_PROJECTILE);
        int projectileVariant = projectileDefinition.supported()
            ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(freeSlot, -1, ENTITY_PAIRODD_PROJECTILE,
            source.x(), source.y(), EntityStatus.ACTIVE, projectileDefinition,
            projectileVariant, 0, 0, source.z());
        slots[freeSlot] = projectile;
        enemyHealth[freeSlot] = initialHealth(ENTITY_PAIRODD_PROJECTILE);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyRecoilMotion.clear(freeSlot);
        dyingCountdown[freeSlot] = 0;
        pairoddProjectileMotion.initializeSpawn(freeSlot, source, linkEntityX, linkEntityY);
    }

    private void spawnEnemyProjectile(RoomEntity source,
                                      RoamingEnemyMotion.LaunchRequest request) {
        // Dynamic projectiles must carry the ROM-decoded display definition;
        // a behavior-only runtime without a catalog can still expose the
        // launch request, but must not insert an unrenderable placeholder.
        if (spriteHandlers == null) {
            return;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        int direction = roamingEnemyMotion.direction(source.slot());
        EnemyProjectileMotion.SpawnData spawn = EnemyProjectileMotion.spawnData(
            request.projectileType(), direction);
        EntitySpriteDefinition projectileDefinition = spriteDefinitionFor(request.projectileType());
        int projectileVariant = projectileDefinition.supported()
            ? spawn.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(freeSlot, -1, request.projectileType(),
            byteValue(source.x() + signedByte(spawn.offsetX())),
            byteValue(source.y() + signedByte(spawn.offsetY())), EntityStatus.ACTIVE,
            projectileDefinition, projectileVariant, 0, 0, source.z());
        slots[freeSlot] = projectile;
        enemyHealth[freeSlot] = initialHealth(request.projectileType());
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyRecoilMotion.clear(freeSlot);
        dyingCountdown[freeSlot] = 0;
        enemyProjectileMotion.initializeSpawn(freeSlot, request.projectileType(), direction);
        enemyProjectileSpawnedThisFrame[freeSlot] = true;
    }

    private EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        if (spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(entityType);
        }
        EntityRoomLoader.RoomTable table = spriteSelection == null
            ? (indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A
                : EntityRoomLoader.RoomTable.OVERWORLD)
            : spriteSelection.roomTable();
        int mapId = spriteSelection == null ? -1 : spriteSelection.roomId();
        return spriteHandlers.forEntityType(entityType, table, mapId);
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

    int tektiteState(int slot) {
        return tektiteMotion.state(slot);
    }

    int tektiteTransitionCountdown(int slot) {
        return tektiteMotion.transitionCountdown(slot);
    }

    int tektiteSpeedX(int slot) {
        return tektiteMotion.speedX(slot);
    }

    int tektiteSpeedY(int slot) {
        return tektiteMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        return pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return pairoddProjectileMotion.speedY(slot);
    }

    int enemyProjectileSpeedX(int slot) {
        return enemyProjectileMotion.speedX(slot);
    }

    int enemyProjectileSpeedY(int slot) {
        return enemyProjectileMotion.speedY(slot);
    }

    int enemyProjectileTransitionCountdown(int slot) {
        return enemyProjectileMotion.transitionCountdown(slot);
    }

    int tektiteSpeedZ(int slot) {
        return tektiteMotion.speedZ(slot);
    }

    int leeverState(int slot) {
        return leeverMotion.state(slot);
    }

    int leeverTransitionCountdown(int slot) {
        return leeverMotion.transitionCountdown(slot);
    }

    int leeverSpeedX(int slot) {
        return leeverMotion.speedX(slot);
    }

    int leeverSpeedY(int slot) {
        return leeverMotion.speedY(slot);
    }

    int antiFairySpeedX(int slot) {
        return antiFairyMotion.speedX(slot);
    }

    int antiFairySpeedY(int slot) {
        return antiFairyMotion.speedY(slot);
    }

    int sparkPrivateState1(int slot) {
        return sparkMotion.privateState1(slot);
    }

    int sparkPrivateState2(int slot) {
        return sparkMotion.privateState2(slot);
    }

    int sparkTransitionCountdown(int slot) {
        return sparkMotion.transitionCountdown(slot);
    }

    int sparkSpeedX(int slot) {
        return sparkMotion.speedX(slot);
    }

    int sparkSpeedY(int slot) {
        return sparkMotion.speedY(slot);
    }

    int zolState(int slot) {
        return zolGelMotion.state(slot);
    }

    int zolTransitionCountdown(int slot) {
        return zolGelMotion.transitionCountdown(slot);
    }

    int zolSpeedX(int slot) {
        return zolGelMotion.speedX(slot);
    }

    int zolSpeedY(int slot) {
        return zolGelMotion.speedY(slot);
    }

    int zolSpeedZ(int slot) {
        return zolGelMotion.speedZ(slot);
    }

    int hidingZolState(int slot) {
        return hidingZolMotion.state(slot);
    }

    int hidingZolTransitionCountdown(int slot) {
        return hidingZolMotion.transitionCountdown(slot);
    }

    int hidingZolPrivateState1(int slot) {
        return hidingZolMotion.privateState1(slot);
    }

    int hidingZolSpeedX(int slot) {
        return hidingZolMotion.speedX(slot);
    }

    int hidingZolSpeedY(int slot) {
        return hidingZolMotion.speedY(slot);
    }

    int hidingZolSpeedZ(int slot) {
        return hidingZolMotion.speedZ(slot);
    }

    int spikeTrapState(int slot) {
        return spikeTrapMotion.state(slot);
    }

    int spikeTrapTransitionCountdown(int slot) {
        return spikeTrapMotion.transitionCountdown(slot);
    }

    int spikeTrapDirection(int slot) {
        return spikeTrapMotion.direction(slot);
    }

    int spikeTrapSpeedX(int slot) {
        return spikeTrapMotion.speedX(slot);
    }

    int spikeTrapSpeedY(int slot) {
        return spikeTrapMotion.speedY(slot);
    }

    int spikeTrapPrivateState1(int slot) {
        return spikeTrapMotion.privateState1(slot);
    }

    int spikeTrapPrivateState2(int slot) {
        return spikeTrapMotion.privateState2(slot);
    }

    int waterTektiteState(int slot) {
        return waterTektiteMotion.state(slot);
    }

    int waterTektiteTransitionCountdown(int slot) {
        return waterTektiteMotion.transitionCountdown(slot);
    }

    int waterTektiteSpeedX(int slot) {
        return waterTektiteMotion.speedX(slot);
    }

    int waterTektiteSpeedY(int slot) {
        return waterTektiteMotion.speedY(slot);
    }

    int waterTektitePrivateState1(int slot) {
        return waterTektiteMotion.privateState1(slot);
    }

    int waterTektitePrivateState2(int slot) {
        return waterTektiteMotion.privateState2(slot);
    }

    int stalfosState(int slot) {
        return stalfosAggressiveMotion.state(slot);
    }

    int stalfosTransitionCountdown(int slot) {
        return stalfosAggressiveMotion.transitionCountdown(slot);
    }

    int stalfosSpeedX(int slot) {
        return stalfosAggressiveMotion.speedX(slot);
    }

    int stalfosSpeedY(int slot) {
        return stalfosAggressiveMotion.speedY(slot);
    }

    int stalfosSpeedZ(int slot) {
        return stalfosAggressiveMotion.speedZ(slot);
    }

    int gibdoState(int slot) {
        return gibdoMotion.state(slot);
    }

    int gibdoSpeedX(int slot) {
        return gibdoMotion.speedX(slot);
    }

    int gibdoSpeedY(int slot) {
        return gibdoMotion.speedY(slot);
    }

    int peaHatState(int slot) {
        return peaHatMotion.state(slot);
    }

    int peaHatSlowTransitionCountdown(int slot) {
        return peaHatMotion.slowTransitionCountdown(slot);
    }

    int peaHatPrivateState1(int slot) {
        return peaHatMotion.privateState1(slot);
    }

    int peaHatPrivateState2(int slot) {
        return peaHatMotion.privateState2(slot);
    }

    int peaHatPrivateState3(int slot) {
        return peaHatMotion.privateState3(slot);
    }

    int peaHatPrivateState4(int slot) {
        return peaHatMotion.privateState4(slot);
    }

    int peaHatSpeedX(int slot) {
        return peaHatMotion.speedX(slot);
    }

    int peaHatSpeedY(int slot) {
        return peaHatMotion.speedY(slot);
    }

    int armosState(int slot) {
        return armosMotion.state(slot);
    }

    int armosTransitionCountdown(int slot) {
        return armosMotion.transitionCountdown(slot);
    }

    int armosSpeedX(int slot) {
        return armosMotion.speedX(slot);
    }

    int armosSpeedY(int slot) {
        return armosMotion.speedY(slot);
    }

    int ghiniTransitionCountdown(int slot) {
        return ghiniMotion.transitionCountdown(slot);
    }

    int ghiniPrivateCountdown1(int slot) {
        return ghiniMotion.privateCountdown1(slot);
    }

    int ghiniTargetXDirection(int slot) {
        return ghiniMotion.targetXDirection(slot);
    }

    int ghiniTargetYDirection(int slot) {
        return ghiniMotion.targetYDirection(slot);
    }

    int ghiniSpeedX(int slot) {
        return ghiniMotion.speedX(slot);
    }

    int ghiniSpeedY(int slot) {
        return ghiniMotion.speedY(slot);
    }

    int hardHatSpeedX(int slot) {
        return hardHatMotion.speedX(slot);
    }

    int hardHatSpeedY(int slot) {
        return hardHatMotion.speedY(slot);
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

    int enemyHealth(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyHealth[slot];
    }

    int enemyFlashCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyFlashCountdown[slot];
    }

    int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyIgnoreHitsCountdown[slot];
    }

    int enemyRecoilSpeedX(int slot) {
        return enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedY(int slot) {
        return enemyRecoilMotion.recoilSpeedY(slot);
    }

    boolean enemyRecoilActive(int slot) {
        return enemyRecoilMotion.isActive(slot);
    }

    private static int variantFor(RoomEntity entity, int frameCounter) {
        if (!entity.spriteDefinition().supported() || entity.spriteDefinition().variantCount() < 2) {
            return entity.spriteVariant();
        }
        return switch (entity.type()) {
            case ENTITY_PIECE_OF_POWER -> (frameCounter >>> 3) & 0x01;
            case ENTITY_BUTTERFLY -> ((frameCounter + entity.slot() * 8) >>> 3) & 0x01;
            case ENTITY_ARMOS_STATUE -> (frameCounter >>> 4) & 0x01;
            case ENTITY_GHINI -> ((frameCounter >>> 4) ^ entity.slot()) & 0x01;
            case ENTITY_HARDHAT_BEETLE -> (frameCounter >>> 3) & 0x01;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE ->
                (frameCounter >>> 1) & 0x01;
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

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withDefinition(RoomEntity entity,
                                              EntitySpriteDefinition definition, int variant) {
        int selectedVariant = definition.supported()
            ? Math.min(Math.max(variant, 0), definition.variantCount() - 1) : -1;
        return new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), definition, selectedVariant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    private void disableEntityWithoutPersistence(int slot) {
        slowTransitionCountdown[slot] = 0;
        slowTimerInitialized[slot] = false;
        dyingCountdown[slot] = 0;
        enemyHealth[slot] = 0;
        enemyFlashCountdown[slot] = 0;
        enemyIgnoreHitsCountdown[slot] = 0;
        enemyRecoilMotion.clear(slot);
        butterflyMotion.clear(slot);
        keeseMotion.clear(slot);
        roamingEnemyMotion.clear(slot);
        tektiteMotion.clear(slot);
        leeverMotion.clear(slot);
        antiFairyMotion.clear(slot);
        sparkMotion.clear(slot);
        zolGelMotion.clear(slot);
        hidingZolMotion.clear(slot);
        spikeTrapMotion.clear(slot);
        pairoddMotion.clear(slot);
        pairoddProjectileMotion.clear(slot);
        enemyProjectileMotion.clear(slot);
        waterTektiteMotion.clear(slot);
        stalfosAggressiveMotion.clear(slot);
        gibdoMotion.clear(slot);
        peaHatMotion.clear(slot);
        armosMotion.clear(slot);
        followingNpcMotion.clear(slot);
        ghiniMotion.clear(slot);
        hardHatMotion.clear(slot);
        bowWowMotion.clear(slot);
        slots[slot] = RoomEntity.disabled(slot);
    }

    private void decrementEnemyCombatCountdowns(int slot) {
        if (enemyFlashCountdown[slot] > 0) {
            enemyFlashCountdown[slot]--;
        }
        if (!enemyRecoilMotion.isActive(slot) && enemyIgnoreHitsCountdown[slot] > 0) {
            enemyIgnoreHitsCountdown[slot]--;
        }
    }

    private EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!enemyRecoilMotion.isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (enemyIgnoreHitsCountdown[slot] == 0) {
            enemyRecoilMotion.clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        roamingEnemyMotion.beginRecoil(slot);
        // ApplyRecoilIfNeeded_03 decrements the shared countdown immediately
        // before applying one fixed-point recoil step.
        enemyIgnoreHitsCountdown[slot]--;
        EnemyRecoilMotion.Update update = enemyRecoilMotion.advance(
            entity, backgroundCollision);
        if (update.blocked()) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            enemyIgnoreHitsCountdown[slot] = 0;
        }
        return update;
    }

    private static int byteValue(int value) {
        return value & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }
}
