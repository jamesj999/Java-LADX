package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.room.ChestContentsTable;

/** ROM slot allocation and player-created entity spawning. */
public final class EntityPlayerSpawns {
    final boolean indoorRoom;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final java.util.function.IntSupplier defaultRandomByteSupplier;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBombInteractions bombInteractions;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupController pickups;
    final EntityPresentationState presentationState;
    final EntityProjectileState projectiles;
    final EntityRoomInspection inspection;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    final RoomEntity[] slots;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final linksawakening.world.room.ChestContentsTable chestContentsTable;
    final EntityPlayerCommands playerCommands;

    EntityPlayerSpawns(
            boolean indoorRoom,
            java.util.function.IntSupplier defaultRandomByteSupplier,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBombInteractions bombInteractions,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupController pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRoomInspection inspection,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RoomEntity[] slots,
            linksawakening.world.room.ChestContentsTable chestContentsTable,
            EntityPlayerCommands playerCommands) {
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;
        this.spriteHandlers = spriteHandlers;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.chestContentsTable = chestContentsTable;
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
    }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        EntityPlayerSpawns.hookshotSpawnsSpawnIronMasksMask(this, x, y, variant, sourceHookshotSlot);
    }

    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.hookshotSpawnsSpawnHookshotChain(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnPushedBlock(int x, int y) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnPushedBlock(this, x, y);
    }

    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftedRoomObject(this, x, y, romDirection, spriteVariant);
    }

    int spawnRoomEventKeyDrop() {
        return EntityPlayerSpawns.playerRewardSpawnsSpawnRoomEventKeyDrop(this);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnArrow(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnArrow(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnBoomerang(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnSwordBeam(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnMagicRodFireball(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnMagicRodFireball(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnMagicPowderSprinkle(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        EntityPlayerSpawns.playerRewardSpawnsSpawnMusicalNote(this, linkEntityX, linkEntityY, animationPhase);
    }

    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerBombSpawnsSpawnBomb(this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockSmash(this, entityX, entityY, sourceSpriteVariant);
    }

    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockRubble(this, entityX, entityY);
    }

    int spawnLiftableRockAnimation(
            int entityX, int entityY, int sourceSpriteVariant, int initialCountdown, int physicsFlags, int soundId) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockAnimation(
                this, entityX, entityY, sourceSpriteVariant, initialCountdown, physicsFlags, soundId);
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        return EntityPlayerSpawns.playerBombSpawnsSpawnBombFromBombArrow(this, arrow);
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        return EntityPlayerSpawns.hookshotSpawnsSpawnHookshotBridge(
                this, bridgeX, bridgeY, direction, transitionCountdown);
    }

    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        return EntityPlayerSpawns.playerRewardSpawnsSpawnShovelDrop(
                this, objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop);
    }

    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        return EntityPlayerSpawns.playerRewardSpawnsSpawnChestWithItem(this, objectLeft, objectTop, itemType);
    }

    static void hookshotSpawnsSpawnIronMasksMask(
            EntityPlayerSpawns runtime, int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_IRON_MASKS_MASK);
        int selectedVariant = definition.supported() ? Math.min(variant & 0x01, definition.variantCount() - 1) : -1;
        RoomEntity mask = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_IRON_MASKS_MASK,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                selectedVariant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = mask;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_IRON_MASKS_MASK));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, EntityRomConstants.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.IRON_MASKS_MASK_OPTIONS1);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        if (runtime.indoorRoom) {
            runtime.physicsTables.writeSlowTransitionCountdown(freeSlot, 0x80);
            runtime.physicsTables.writeSlowTimerInitialized(freeSlot, true);
        }
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(freeSlot, (sourceHookshotSlot + 1) & 0xFF);
    }

    static int hookshotSpawnsSpawnHookshotChain(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (runtime.inspection.hookshotActive()) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_CHAIN);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity chain = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_HOOKSHOT_CHAIN,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = chain;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .initializeSpawn(freeSlot, linkEntityX & 0xFF, linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                2 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        return freeSlot;
    }

    static boolean hookshotSpawnsSpawnHookshotBridge(
            EntityPlayerSpawns runtime, int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        if (!runtime.indoorRoom) {
            return false;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bridge = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE,
                bridgeX & 0xFF,
                bridgeY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = bridge;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, transitionCountdown & 0xFF);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    static int playerBombSpawnsSpawnBomb(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.bombInteractions.state.writeLastBombPlacementPlayedBump(false);
        runtime.bombInteractions.finalizePendingBombPresentations();
        if (runtime.inspection.bombActive()) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BOMB,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = bomb;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.bombInteractions.state.writeBombDirection(freeSlot, romDirection);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).startPlacedBomb(freeSlot, romDirection);
        runtime.bombInteractions.state.writePlacedBombMotionInitialized(freeSlot, true);
        runtime.bombInteractions.state.writePlayerArrowBombArrow(freeSlot, false);
        runtime.bombInteractions.state.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions.state.writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions.state.writeBombPrivateCountdown1(freeSlot, 0x10);
        runtime.bombInteractions.state.writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, BombMotion.INITIAL_COUNTDOWN);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (runtime.bombInteractions.state.readBombArrowCooldown() != 0) {
            runtime.bombInteractions.state.writeBombArrowCooldown(0);
            int arrowSlot = runtime.projectiles.readLatestShotArrowEntityIndex();
            if (runtime.roomInspection.isLoadedEntityOfType(arrowSlot, EntityRomConstants.ENTITY_ARROW)) {
                runtime.bombInteractions.markArrowAsBombArrow(arrowSlot);
            }
        } else {
            runtime.bombInteractions.state.writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.bombInteractions.state.writeLatestDroppedBombEntityIndex(freeSlot);
            runtime.bombInteractions.applyInitialBombArrowCandidateState(freeSlot, romDirection);
        }
        return freeSlot;
    }

    static int playerBombSpawnsSpawnBombFromBombArrow(EntityPlayerSpawns runtime, RoomEntity arrow) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BOMB,
                arrow.x(),
                arrow.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                arrow.z(),
                -1,
                false);
        runtime.slots[freeSlot] = bomb;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.bombInteractions.state.writeBombDirection(
                freeSlot,
                runtime.motions
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .direction(arrow.slot()));
        runtime.bombInteractions.state.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions.state.writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions.state.writeBombPrivateCountdown1(freeSlot, 0);
        runtime.bombInteractions.state.writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(
                freeSlot, EntityRomConstants.BOMB_ARROW_EXPLOSION_COUNTDOWN);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        runtime.bombInteractions.state.writePlacedBombMotionInitialized(freeSlot, true);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int playerObjectSpawnsSpawnPushedBlock(EntityPlayerSpawns runtime, int x, int y) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_PUSHED_BLOCK);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_PUSHED_BLOCK,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                2 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int playerObjectSpawnsSpawnLiftedRoomObject(
            EntityPlayerSpawns runtime, int x, int y, int romDirection, int spriteVariant) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        int variant = definition.supported() ? spriteVariant : -1;
        runtime.slots[freeSlot] = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                0x02
                        | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                        | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                        | EntityRomConstants.ENTITY_PHYSICS_SHADOW);
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK));
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        if (!runtime.playerCommands.beginLift(freeSlot, romDirection)) {
            runtime.slots[freeSlot] = RoomEntity.disabled(freeSlot);
            return -1;
        }
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                freeSlot,
                EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                0,
                false,
                EntityCombatEvent.SoundChannel.WAVE,
                0x02));
        return freeSlot;
    }

    static int playerObjectSpawnsSpawnLiftableRockSmash(
            EntityPlayerSpawns runtime, int entityX, int entityY, int sourceSpriteVariant) {
        if (sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK
                && sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_BUSH
                && sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS) {
            throw new IllegalArgumentException("Liftable-rock source variant must be 0, 1, or FF");
        }
        boolean rock = sourceSpriteVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK;
        int initialCountdown = rock ? 0x0F : 0x1F;
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockAnimation(
                runtime,
                entityX,
                entityY,
                sourceSpriteVariant,
                initialCountdown,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS,
                rock ? 0x09 : 0x05);
    }

    static int playerObjectSpawnsSpawnLiftableRockRubble(EntityPlayerSpawns runtime, int entityX, int entityY) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockAnimation(
                runtime,
                entityX,
                entityY,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK,
                0x0F,
                EntityRomConstants.LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS,
                -1);
    }

    static int playerObjectSpawnsSpawnLiftableRockAnimation(
            EntityPlayerSpawns runtime,
            int entityX,
            int entityY,
            int sourceSpriteVariant,
            int initialCountdown,
            int physicsFlags,
            int soundId) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        boolean swampLeaves = !runtime.indoorRoom
                && runtime.presentationState.readSpriteSelection() != null
                && runtime.presentationState.readSpriteSelection().roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.presentationState.readSpriteSelection().roomId() == 0x32;
        int variant = definition.supported()
                ? EntityMotionMath.liftableRockSmashVariant(sourceSpriteVariant, swampLeaves, initialCountdown)
                : -1;
        RoomEntity smash = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                entityX & 0xFF,
                entityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = smash;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.liftThrow.writeLiftableRockSmashActive(freeSlot, true);
        runtime.liftThrow.writeLiftableRockSmashCountdown(freeSlot, initialCountdown);
        runtime.liftThrow.writeLiftableRockSmashSourceVariant(freeSlot, sourceSpriteVariant);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, physicsFlags);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK));
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (soundId >= 0) {
            runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                    freeSlot,
                    EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.NOISE,
                    soundId));
        }
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int playerProjectileSpawnsSpawnArrow(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnArrow(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    static int playerProjectileSpawnsSpawnArrow(
            EntityPlayerSpawns runtime,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.projectiles.writeLastArrowShotPlayedWhoosh(false);
        if (runtime.inspection.activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_ARROW);
        int variant = romDirection;
        RoomEntity arrow = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_ARROW,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = arrow;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(freeSlot);
        runtime.motions
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.projectiles.writeLatestShotArrowEntityIndex(freeSlot);
        if (runtime.bombInteractions.state.readBombArrowCooldown() != 0) {
            int bombSlot = runtime.bombInteractions.state.readLatestDroppedBombEntityIndex();
            runtime.bombInteractions.state.writeBombArrowCooldown(0);
            if (runtime.roomInspection.isLoadedEntityOfType(bombSlot, EntityRomConstants.ENTITY_BOMB)) {
                runtime.lifecycle.disableEntityWithoutPersistence(bombSlot);
            }
            runtime.bombInteractions.state.writeLatestDroppedBombEntityIndex(-1);
            runtime.bombInteractions.state.writePlayerArrowBombArrow(freeSlot, true);
            if (runtime.spriteHandlers != null) {
                runtime.slots[freeSlot] = withDefinition(
                        runtime.slots[freeSlot],
                        runtime.slotProperties.spriteViews().forBombArrow(),
                        romDirection);
            }
        } else {
            runtime.bombInteractions.state.writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.projectiles.writeLastArrowShotPlayedWhoosh(true);
        }
        return freeSlot;
    }

    static int playerProjectileSpawnsSpawnBoomerang(
            EntityPlayerSpawns runtime,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            int pressedButtonsMask) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection.activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOOMERANG);
        RoomEntity boomerang = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BOOMERANG,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                romDirection,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = boomerang;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions.get(BoomerangMotion.class, BoomerangMotion::new).clear(freeSlot);
        runtime.motions
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        runtime.liftThrow.writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables.writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    static int playerProjectileSpawnsSpawnSwordBeam(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection.activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_SWORD_BEAM);
        int variant = definition.supported() ? romDirection : -1;
        RoomEntity beam = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_SWORD_BEAM,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = beam;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions.get(SwordBeamMotion.class, SwordBeamMotion::new).clear(freeSlot);
        runtime.motions.get(SwordBeamMotion.class, SwordBeamMotion::new).initializeSpawn(freeSlot, romDirection);
        runtime.liftThrow.writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables.writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    static int playerProjectileSpawnsSpawnMagicRodFireball(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnMagicRodFireball(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    static int playerProjectileSpawnsSpawnMagicRodFireball(
            EntityPlayerSpawns runtime,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection.activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL);
        MagicRodFireballMotion.SpawnData spawn = MagicRodFireballMotion.spawnData(romDirection, pieceOfPower);
        int variant = definition.supported() ? spawn.initialVariant() : -1;
        RoomEntity fireball = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL,
                (linkEntityX + spawn.offsetX()) & 0xFF,
                (linkEntityY + spawn.offsetY()) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = fireball;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(freeSlot);
        runtime.motions
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.liftThrow.writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables.writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    static int playerProjectileSpawnsSpawnMagicPowderSprinkle(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity sprinkle = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                (linkEntityX + xOffsets[romDirection]) & 0xFF,
                (linkEntityY + yOffsets[romDirection]) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                linkEntityZ & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = sprinkle;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.projectiles.writeMagicPowderState(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(
                freeSlot, EntityRomConstants.MAGIC_POWDER_TRANSITION_COUNTDOWN);
        runtime.physicsTables.writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables.writeSlowTimerInitialized(freeSlot, false);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                0x03
                        | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                        | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int playerRewardSpawnsSpawnRoomEventKeyDrop(EntityPlayerSpawns runtime) {
        int itemType =
                runtime.roomVariables.readEntityMapId() >= 0x0A && runtime.roomVariables.readEntityMapId() != 0xFF
                        ? EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                        : EntityRomConstants.ENTITY_KEY_DROP_POINT;
        return runtime.pickups.spawnKeyDropAt(itemType, 0x28, 0x3C, 0, 0x70, 0, 0);
    }

    static void playerRewardSpawnsSpawnMusicalNote(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return;
        }

        int phase = animationPhase & 0x01;
        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_MUSICAL_NOTE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        int x = linkEntityX + (phase == 0 ? 0x08 : -0x08);
        RoomEntity note = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_MUSICAL_NOTE,
                x & 0xFF,
                (linkEntityY - 0x08) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = note;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.projectiles.writeMusicalNoteInertia(freeSlot, EntityRomConstants.MUSICAL_NOTE_INITIAL_INERTIA);
        runtime.projectiles.writeMusicalNoteSpeedX(freeSlot, phase == 0 ? 0x06 : 0x01);
        runtime.projectiles.writeMusicalNoteSpeedY(freeSlot, EntityRomConstants.MUSICAL_NOTE_SPEED_Y);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static int playerRewardSpawnsSpawnShovelDrop(
            EntityPlayerSpawns runtime,
            int objectLeft,
            int objectTop,
            int linkEntityX,
            int linkEntityY,
            boolean allowDrop) {
        if (runtime.defaultRandomByteSupplier == null) {
            throw new IllegalStateException("Shovel drops require a ROM random-byte supplier");
        }
        if ((runtime.defaultRandomByteSupplier.getAsInt() & 0x07) != 0 || !allowDrop) {
            return -1;
        }

        int itemType = (runtime.defaultRandomByteSupplier.getAsInt() & 0x01) == 0 ? 0x2E : 0x2D;
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity drop = new RoomEntity(
                freeSlot,
                -1,
                itemType,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = drop;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.physicsTables.writeSlowTransitionCountdown(freeSlot, 0x80);
        runtime.physicsTables.writeSlowTimerInitialized(freeSlot, true);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables.writeEnemyHealth(freeSlot, runtime.slotProperties.initialHealth(itemType));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.bossRewards.writeDropPrivateCountdown1(freeSlot, 0x18);
        runtime.bossRewards.writeDropPrivateCountdown3(freeSlot, 0x03);
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        runtime.motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables.readGroundInteractionSideScrolling(), 0x20);
        runtime.motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initializeAwayFromLink(freeSlot, x & 0xFF, y & 0xFF, linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        runtime.bossRewards.writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int playerRewardSpawnsSpawnChestWithItem(
            EntityPlayerSpawns runtime, int objectLeft, int objectTop, int itemType) {
        if (runtime.chestContentsTable == null) {
            throw new IllegalStateException("ROM chest contents table is not configured");
        }
        EntitySlotValidation.validateByte(objectLeft, "Chest object left");
        EntitySlotValidation.validateByte(objectTop, "Chest object top");
        if (itemType < 0 || itemType > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item variant out of range: " + itemType);
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionForChest(itemType);
        int spriteVariant = definition.supported() ? itemType : -1;
        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        RoomEntity chest = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.INIT,
                definition,
                spriteVariant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = chest;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.CHEST_INITIAL_PHYSICS_FLAGS);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.pickups.state.writeChestSpeedY(freeSlot, 0);
        runtime.pickups.state.writeChestSpeedYAccumulator(freeSlot, 0);
        runtime.pickups.state.writeChestInertia(freeSlot, 0);
        runtime.pickups.state.writeChestItemBySlot(freeSlot, itemType);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, false);
        return freeSlot;
    }
}
