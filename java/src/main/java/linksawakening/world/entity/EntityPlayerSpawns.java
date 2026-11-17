package linksawakening.world.entity;

import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.SwordBeamMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;
import linksawakening.world.room.ChestContentsTable;

/** SpawnNewEntity / SpawnNewEntityInRange allocation and player-weapon initialization paths.
 *
 * Player-created entities share free-slot selection, slot initialization and presentation publication as one spawn transaction; ongoing item behavior belongs to the motion owners.
 */
public final class EntityPlayerSpawns {
    private final boolean indoorRoom;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    private final IntSupplier defaultRandomByteSupplier;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    private final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    private final EntityBombInteractions bombInteractions;
    private final EntityBossState bossRewards;
    private final EntityCombatTables combatTables;
    private final EntityEventChannels events;
    private final EntityLiftState liftThrow;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntityPickupController pickups;
    private final EntityPresentationState presentationState;
    private final EntityProjectileState projectiles;
    private final EntityRoomInspection inspection;
    private final EntityRoomInspection roomInspection;
    private final EntityRoomVariables roomVariables;
    private final EntitySlotLifecycle lifecycle;
    private final EntitySlotProperties slotProperties;
    private final EntitySlots slots;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    private final EntityChestContents chestContentsTable;
    private final EntityPlayerCommands playerCommands;

    EntityPlayerSpawns(
            boolean indoorRoom,
            IntSupplier defaultRandomByteSupplier,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBombInteractions bombInteractions,
            EntityBossState bossRewards,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPickupController pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            EntitySlots slots,
            EntityChestContents chestContentsTable,
            EntityPlayerCommands playerCommands) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        EntityRoomInspection inspection = roomInspection;
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = Objects.requireNonNull(defaultRandomByteSupplier, "randomByteSupplier");
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = Objects.requireNonNull(projectiles, "projectiles");
        this.inspection = Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.chestContentsTable = Objects.requireNonNull(chestContentsTable, "chest contents");
        this.playerCommands = Objects.requireNonNull(playerCommands, "playerCommands");
    }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_IRON_MASKS_MASK);
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
        this.slots.replace(freeSlot, mask);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_IRON_MASKS_MASK));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.IRON_MASKS_MASK_OPTIONS1);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        if (this.indoorRoom) {
            this.physicsTables.writeSlowTransitionCountdown(freeSlot, 0x80);
            this.physicsTables.writeSlowTimerInitialized(freeSlot, true);
        }
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        var unmaskedIronMaskMotion = this.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        unmaskedIronMaskMotion.setMaskSourceHookshotSlot(freeSlot, (sourceHookshotSlot + 1) & 0xFF);
    }

    public int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (this.inspection.hookshotActive()) {
            return -1;
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_CHAIN);
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
        this.slots.replace(freeSlot, chain);
        this.lifecycle.initializeHealthGroup(freeSlot);
        var hookshotChainMotion = this.motions.get(HookshotChainMotion.class, HookshotChainMotion::new);
        hookshotChainMotion.initializeSpawn(
                freeSlot, linkEntityX & 0xFF, linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                2 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        return freeSlot;
    }

    public int spawnPushedBlock(int x, int y) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_PUSHED_BLOCK);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        this.slots.replace(
                freeSlot,
                new RoomEntity(
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
                        false));
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                2 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    public int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        int variant = definition.supported() ? spriteVariant : -1;
        this.slots.replace(
                freeSlot,
                new RoomEntity(
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
                        false));
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                0x02
                        | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                        | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                        | EntityRomConstants.ENTITY_PHYSICS_SHADOW);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK));
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        if (!this.playerCommands.beginLift(freeSlot, romDirection)) {
            this.slots.clear(freeSlot);
            return -1;
        }
        this.events.sound(freeSlot, EntityRomConstants.ENTITY_LIFTABLE_ROCK, EntityCombatEvent.SoundChannel.WAVE, 0x02);
        return freeSlot;
    }

    public int spawnRoomEventKeyDrop() {
        int itemType = this.roomVariables.readEntityMapId() >= 0x0A && this.roomVariables.readEntityMapId() != 0xFF
                ? EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                : EntityRomConstants.ENTITY_KEY_DROP_POINT;
        return this.pickups.spawnKeyDropAt(itemType, 0x28, 0x3C, 0, 0x70, 0, 0);
    }

    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnArrow(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnArrow(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    public int spawnBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (this.inspection.activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOOMERANG);
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
        this.slots.replace(freeSlot, boomerang);
        this.lifecycle.initializeHealthGroup(freeSlot);
        var boomerangMotion = this.motions.get(BoomerangMotion.class, BoomerangMotion::new);
        boomerangMotion.clear(freeSlot);
        boomerangMotion.initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        this.liftThrow.writeThrownDirection(freeSlot, romDirection);
        this.physicsTables.writeEntityUnknownJ(freeSlot, 1);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    public int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (this.inspection.activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_SWORD_BEAM);
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
        this.slots.replace(freeSlot, beam);
        this.lifecycle.initializeHealthGroup(freeSlot);
        var swordBeamMotion = this.motions.get(SwordBeamMotion.class, SwordBeamMotion::new);
        swordBeamMotion.clear(freeSlot);
        swordBeamMotion.initializeSpawn(freeSlot, romDirection);
        this.liftThrow.writeThrownDirection(freeSlot, romDirection);
        this.physicsTables.writeEntityUnknownJ(freeSlot, 1);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    public int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnMagicRodFireball(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return EntityPlayerSpawns.playerProjectileSpawnsSpawnMagicRodFireball(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    public int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE);
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
        this.slots.replace(freeSlot, sprinkle);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.projectiles.writeMagicPowderState(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(
                freeSlot, EntityRomConstants.MAGIC_POWDER_TRANSITION_COUNTDOWN);
        this.physicsTables.writeSlowTransitionCountdown(freeSlot, 0);
        this.physicsTables.writeSlowTimerInitialized(freeSlot, false);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                0x03
                        | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                        | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        int phase = animationPhase & 0x01;
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_MUSICAL_NOTE);
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
        this.slots.replace(freeSlot, note);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.projectiles.writeMusicalNoteInertia(freeSlot, EntityRomConstants.MUSICAL_NOTE_INITIAL_INERTIA);
        this.projectiles.writeMusicalNoteSpeedX(freeSlot, phase == 0 ? 0x06 : 0x01);
        this.projectiles.writeMusicalNoteSpeedY(freeSlot, EntityRomConstants.MUSICAL_NOTE_SPEED_Y);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        this.bombInteractions.writeLastBombPlacementPlayedBump(false);
        this.bombInteractions.finalizePendingBombPresentations();
        if (this.inspection.bombActive()) {
            return -1;
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        this.slots.replace(freeSlot, bomb);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.bombInteractions.writeBombDirection(freeSlot, romDirection);
        var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        thrownEntityMotion.startPlacedBomb(freeSlot, romDirection);
        this.bombInteractions.writePlacedBombMotionInitialized(freeSlot, true);
        this.bombInteractions.writePlayerArrowBombArrow(freeSlot, false);
        this.bombInteractions.setBombFinalPresentationPending(freeSlot, false);
        this.bombInteractions.writeBombOrigin(freeSlot, 0);
        this.bombInteractions.writeBombCollisionGraceCountdown(freeSlot, 0x10);
        this.bombInteractions.writeBombLiftDelay(freeSlot, 0);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, BombMotion.INITIAL_COUNTDOWN);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        this.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (this.bombInteractions.readBombArrowCooldown() != 0) {
            this.bombInteractions.writeBombArrowCooldown(0);
            int arrowSlot = this.projectiles.readLatestShotArrowEntityIndex();
            if (this.roomInspection.isLoadedEntityOfType(arrowSlot, EntityRomConstants.ENTITY_ARROW)) {
                this.bombInteractions.markArrowAsBombArrow(arrowSlot);
            }
        } else {
            this.bombInteractions.writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            this.bombInteractions.writeLatestDroppedBombEntityIndex(freeSlot);
            this.bombInteractions.applyInitialBombArrowCandidateState(freeSlot, romDirection);
        }
        return freeSlot;
    }

    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        if (sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK
                && sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_BUSH
                && sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS) {
            throw new IllegalArgumentException("Liftable-rock source variant must be 0, 1, or FF");
        }
        boolean rock = sourceSpriteVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK;
        int initialCountdown = rock ? 0x0F : 0x1F;
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockAnimation(
                this,
                entityX,
                entityY,
                sourceSpriteVariant,
                initialCountdown,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS,
                rock ? 0x09 : 0x05);
    }

    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockAnimation(
                this,
                entityX,
                entityY,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK,
                0x0F,
                EntityRomConstants.LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS,
                -1);
    }

    int spawnLiftableRockAnimation(
            int entityX, int entityY, int sourceSpriteVariant, int initialCountdown, int physicsFlags, int soundId) {
        return EntityPlayerSpawns.playerObjectSpawnsSpawnLiftableRockAnimation(
                this, entityX, entityY, sourceSpriteVariant, initialCountdown, physicsFlags, soundId);
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        this.slots.replace(freeSlot, bomb);
        this.lifecycle.initializeHealthGroup(freeSlot);
        var playerArrowMotion = this.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
        this.bombInteractions.writeBombDirection(freeSlot, playerArrowMotion.direction(arrow.slot()));
        this.bombInteractions.setBombFinalPresentationPending(freeSlot, false);
        this.bombInteractions.writeBombOrigin(freeSlot, 0);
        this.bombInteractions.writeBombCollisionGraceCountdown(freeSlot, 0);
        this.bombInteractions.writeBombLiftDelay(freeSlot, 0);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, EntityRomConstants.BOMB_ARROW_EXPLOSION_COUNTDOWN);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        this.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        this.bombInteractions.writePlacedBombMotionInitialized(freeSlot, true);
        var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        thrownEntityMotion.clear(freeSlot);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        if (!this.indoorRoom) {
            return false;
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE);
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
        this.slots.replace(freeSlot, bridge);
        this.lifecycle.initializeHealthGroup(freeSlot);
        var hookshotBridgeMotion = this.motions.get(HookshotBridgeMotion.class, HookshotBridgeMotion::new);
        hookshotBridgeMotion.initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot,
                EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, transitionCountdown & 0xFF);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(
                freeSlot,
                EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                        | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    public int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        if ((this.defaultRandomByteSupplier.getAsInt() & 0x07) != 0 || !allowDrop) {
            return -1;
        }

        int itemType = (this.defaultRandomByteSupplier.getAsInt() & 0x01) == 0 ? 0x2E : 0x2D;
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionFor(itemType);
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
        this.slots.replace(freeSlot, drop);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.pickups.resetEnemyDropState(freeSlot);
        this.physicsTables.writeSlowTransitionCountdown(freeSlot, 0x80);
        this.physicsTables.writeSlowTimerInitialized(freeSlot, true);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        this.combatTables.writeEnemyHealth(freeSlot, this.slotProperties.initialHealth(itemType));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.bossRewards.writeDropPrivateCountdown1(freeSlot, 0x18);
        this.bossRewards.writeDropPrivateCountdown3(freeSlot, 0x03);
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        var enemyDropMotion = this.motions.get(EnemyDropMotion.class, EnemyDropMotion::new);
        enemyDropMotion.initialize(freeSlot, this.physicsTables.readGroundInteractionSideScrolling(), 0x20);
        enemyDropMotion.initializeAwayFromLink(
                freeSlot, x & 0xFF, y & 0xFF, linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        this.bossRewards.writeEnemyDropActive(freeSlot, true);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    public int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        this.chestContentsTable.requireSpawnTable();
        EntitySlotValidation.validateByte(objectLeft, "Chest object left");
        EntitySlotValidation.validateByte(objectTop, "Chest object top");
        if (itemType < 0 || itemType > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item variant out of range: " + itemType);
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionForChest(itemType);
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
        this.slots.replace(freeSlot, chest);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.CHEST_INITIAL_PHYSICS_FLAGS);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.pickups.state().writeChestSpeedY(freeSlot, 0);
        this.pickups.state().writeChestSpeedYAccumulator(freeSlot, 0);
        this.pickups.state().writeChestInertia(freeSlot, 0);
        this.pickups.state().writeChestItemBySlot(freeSlot, itemType);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, false);
        return freeSlot;
    }

    static int playerObjectSpawnsSpawnLiftableRockAnimation(
            EntityPlayerSpawns runtime,
            int entityX,
            int entityY,
            int sourceSpriteVariant,
            int initialCountdown,
            int physicsFlags,
            int soundId) {
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        boolean swampLeaves = !runtime.indoorRoom
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
        runtime.slots.replace(freeSlot, smash);
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
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (soundId >= 0) {
            runtime.events.sound(
                    freeSlot, EntityRomConstants.ENTITY_LIFTABLE_ROCK, EntityCombatEvent.SoundChannel.NOISE, soundId);
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
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
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
        runtime.slots.replace(freeSlot, arrow);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        var playerArrowMotion = runtime.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
        playerArrowMotion.clear(freeSlot);
        playerArrowMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
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
        if (runtime.bombInteractions.readBombArrowCooldown() != 0) {
            int bombSlot = runtime.bombInteractions.readLatestDroppedBombEntityIndex();
            runtime.bombInteractions.writeBombArrowCooldown(0);
            if (runtime.roomInspection.isLoadedEntityOfType(bombSlot, EntityRomConstants.ENTITY_BOMB)) {
                runtime.lifecycle.disableEntityWithoutPersistence(bombSlot);
            }
            runtime.bombInteractions.writeLatestDroppedBombEntityIndex(-1);
            runtime.bombInteractions.writePlayerArrowBombArrow(freeSlot, true);
            {
                runtime.slots.replace(
                        freeSlot,
                        runtime.spriteHandlers.present(
                                runtime.slots.get(freeSlot),
                                runtime.slotProperties.spriteViews().forBombArrow(),
                                romDirection));
            }
        } else {
            runtime.bombInteractions.writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.projectiles.writeLastArrowShotPlayedWhoosh(true);
        }
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
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
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
        runtime.slots.replace(freeSlot, fireball);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        var magicRodFireballMotion = runtime.motions.get(MagicRodFireballMotion.class, MagicRodFireballMotion::new);
        magicRodFireballMotion.clear(freeSlot);
        magicRodFireballMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
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
}
