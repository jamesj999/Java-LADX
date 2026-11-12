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
    private final EntityPlayerSpawnsServices runtime;

    EntityPlayerSpawns(EntityPlayerSpawnsServices runtime) {
        this.runtime = runtime;
    }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_IRON_MASKS_MASK);
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
        runtime.slots()[freeSlot] = mask;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_IRON_MASKS_MASK));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.IRON_MASKS_MASK_OPTIONS1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        if (runtime.indoorRoom()) {
            runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0x80);
            runtime.physicsTables().writeSlowTimerInitialized(freeSlot, true);
        }
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(freeSlot, (sourceHookshotSlot + 1) & 0xFF);
    }

    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        if (runtime.inspection().hookshotActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_CHAIN);
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
        runtime.slots()[freeSlot] = chain;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .initializeSpawn(freeSlot, linkEntityX & 0xFF, linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        2
                                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        return freeSlot;
    }

    int spawnPushedBlock(int x, int y) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_PUSHED_BLOCK);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots()[freeSlot] = new RoomEntity(
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        2
                                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        int variant = definition.supported() ? spriteVariant : -1;
        runtime.slots()[freeSlot] = new RoomEntity(
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        0x02
                                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                                | EntityRomConstants.ENTITY_PHYSICS_SHADOW);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK));
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        if (!runtime.beginLift(freeSlot, romDirection)) {
            runtime.slots()[freeSlot] = RoomEntity.disabled(freeSlot);
            return -1;
        }
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        freeSlot,
                        EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        0x02));
        return freeSlot;
    }

    int spawnRoomEventKeyDrop() {
        int itemType = runtime.roomVariables().readEntityMapId() >= 0x0A
                        && runtime.roomVariables().readEntityMapId() != 0xFF
                ? EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                : EntityRomConstants.ENTITY_KEY_DROP_POINT;
        return runtime.pickups().spawnKeyDropAt(itemType, 0x28, 0x3C, 0, 0x70, 0, 0);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.projectiles().writeLastArrowShotPlayedWhoosh(false);
        if (runtime.inspection().activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_ARROW);
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
        runtime.slots()[freeSlot] = arrow;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(freeSlot);
        runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.projectiles().writeLatestShotArrowEntityIndex(freeSlot);
        if (runtime.bombInteractions().readBombArrowCooldown() != 0) {
            int bombSlot = runtime.bombInteractions().readLatestDroppedBombEntityIndex();
            runtime.bombInteractions().writeBombArrowCooldown(0);
            if (runtime.roomInspection().isLoadedEntityOfType(bombSlot, EntityRomConstants.ENTITY_BOMB)) {
                runtime.lifecycle().disableEntityWithoutPersistence(bombSlot);
            }
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
            runtime.bombInteractions().writePlayerArrowBombArrow(freeSlot, true);
            if (runtime.spriteHandlers() != null) {
                runtime.slots()[freeSlot] = withDefinition(
                        runtime.slots()[freeSlot], runtime.spriteHandlers().forBombArrow(), romDirection);
            }
        } else {
            runtime.bombInteractions().writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.projectiles().writeLastArrowShotPlayedWhoosh(true);
        }
        return freeSlot;
    }

    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection().activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOOMERANG);
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
        runtime.slots()[freeSlot] = boomerang;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).clear(freeSlot);
        runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        runtime.liftThrow().writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables().writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection().activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_SWORD_BEAM);
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
        runtime.slots()[freeSlot] = beam;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).clear(freeSlot);
        runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).initializeSpawn(freeSlot, romDirection);
        runtime.liftThrow().writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables().writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection().activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL);
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
        runtime.slots()[freeSlot] = fireball;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(freeSlot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.liftThrow().writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables().writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE);
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
        runtime.slots()[freeSlot] = sprinkle;
        runtime.initializeHealthGroup(freeSlot);
        runtime.projectiles().writeMagicPowderState(freeSlot, 0);
        runtime.physicsTables()
                .writeEnemyTransitionCountdown(freeSlot, EntityRomConstants.MAGIC_POWDER_TRANSITION_COUNTDOWN);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, false);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        0x03
                                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        int phase = animationPhase & 0x01;
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MUSICAL_NOTE);
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
        runtime.slots()[freeSlot] = note;
        runtime.initializeHealthGroup(freeSlot);
        runtime.projectiles().writeMusicalNoteInertia(freeSlot, EntityRomConstants.MUSICAL_NOTE_INITIAL_INERTIA);
        runtime.projectiles().writeMusicalNoteSpeedX(freeSlot, phase == 0 ? 0x06 : 0x01);
        runtime.projectiles().writeMusicalNoteSpeedY(freeSlot, EntityRomConstants.MUSICAL_NOTE_SPEED_Y);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.bombInteractions().writeLastBombPlacementPlayedBump(false);
        runtime.bombInteractions().finalizePendingBombPresentations();
        if (runtime.inspection().bombActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        runtime.slots()[freeSlot] = bomb;
        runtime.initializeHealthGroup(freeSlot);
        runtime.bombInteractions().writeBombDirection(freeSlot, romDirection);
        runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .startPlacedBomb(freeSlot, romDirection);
        runtime.bombInteractions().writePlacedBombMotionInitialized(freeSlot, true);
        runtime.bombInteractions().writePlayerArrowBombArrow(freeSlot, false);
        runtime.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions().writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(freeSlot, 0x10);
        runtime.bombInteractions().writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, BombMotion.INITIAL_COUNTDOWN);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (runtime.bombInteractions().readBombArrowCooldown() != 0) {
            runtime.bombInteractions().writeBombArrowCooldown(0);
            int arrowSlot = runtime.projectiles().readLatestShotArrowEntityIndex();
            if (runtime.roomInspection().isLoadedEntityOfType(arrowSlot, EntityRomConstants.ENTITY_ARROW)) {
                runtime.bombInteractions().markArrowAsBombArrow(arrowSlot);
            }
        } else {
            runtime.bombInteractions().writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(freeSlot);
            runtime.bombInteractions().applyInitialBombArrowCandidateState(freeSlot, romDirection);
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
        return spawnLiftableRockAnimation(
                entityX,
                entityY,
                sourceSpriteVariant,
                initialCountdown,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS,
                rock ? 0x09 : 0x05);
    }

    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return spawnLiftableRockAnimation(
                entityX,
                entityY,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK,
                0x0F,
                EntityRomConstants.LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS,
                -1);
    }

    int spawnLiftableRockAnimation(
            int entityX, int entityY, int sourceSpriteVariant, int initialCountdown, int physicsFlags, int soundId) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        boolean swampLeaves = !runtime.indoorRoom()
                && runtime.presentationState().readSpriteSelection() != null
                && runtime.presentationState().readSpriteSelection().roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.presentationState().readSpriteSelection().roomId() == 0x32;
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
        runtime.slots()[freeSlot] = smash;
        runtime.initializeHealthGroup(freeSlot);
        runtime.liftThrow().writeLiftableRockSmashActive(freeSlot, true);
        runtime.liftThrow().writeLiftableRockSmashCountdown(freeSlot, initialCountdown);
        runtime.liftThrow().writeLiftableRockSmashSourceVariant(freeSlot, sourceSpriteVariant);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, physicsFlags);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (soundId >= 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            freeSlot,
                            EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            soundId));
        }
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        runtime.slots()[freeSlot] = bomb;
        runtime.initializeHealthGroup(freeSlot);
        runtime.bombInteractions()
                .writeBombDirection(
                        freeSlot,
                        runtime.motions()
                                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                                .direction(arrow.slot()));
        runtime.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions().writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables()
                .writeEnemyTransitionCountdown(freeSlot, EntityRomConstants.BOMB_ARROW_EXPLOSION_COUNTDOWN);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        runtime.bombInteractions().writePlacedBombMotionInitialized(freeSlot, true);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        if (!runtime.indoorRoom()) {
            return false;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE);
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
        runtime.slots()[freeSlot] = bridge;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, transitionCountdown & 0xFF);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        if (runtime.defaultRandomByteSupplier() == null) {
            throw new IllegalStateException("Shovel drops require a ROM random-byte supplier");
        }
        if ((runtime.defaultRandomByteSupplier().getAsInt() & 0x07) != 0 || !allowDrop) {
            return -1;
        }

        int itemType = (runtime.defaultRandomByteSupplier().getAsInt() & 0x01) == 0 ? 0x2E : 0x2D;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(itemType);
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
        runtime.slots()[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        runtime.pickups().resetEnemyDropState(freeSlot);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0x80);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, true);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(itemType));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.bossRewards().writeDropPrivateCountdown1(freeSlot, 0x18);
        runtime.bossRewards().writeDropPrivateCountdown3(freeSlot, 0x03);
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling(), 0x20);
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initializeAwayFromLink(freeSlot, x & 0xFF, y & 0xFF, linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        runtime.bossRewards().writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        if (runtime.chestContentsTable() == null) {
            throw new IllegalStateException("ROM chest contents table is not configured");
        }
        EntitySlotValidation.validateByte(objectLeft, "Chest object left");
        EntitySlotValidation.validateByte(objectTop, "Chest object top");
        if (itemType < 0 || itemType > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item variant out of range: " + itemType);
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionForChest(itemType);
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
        runtime.slots()[freeSlot] = chest;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.CHEST_INITIAL_PHYSICS_FLAGS);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.pickups().writeChestSpeedY(freeSlot, 0);
        runtime.pickups().writeChestSpeedYAccumulator(freeSlot, 0);
        runtime.pickups().writeChestInertia(freeSlot, 0);
        runtime.pickups().writeChestItemBySlot(freeSlot, itemType);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, false);
        return freeSlot;
    }
}
