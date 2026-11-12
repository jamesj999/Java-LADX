package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.room.ChestContentsTable;

/** ROM slot allocation and player-created entity spawning. */
public final class EntityPlayerSpawns {
    private final RoomEntityRuntime runtime;

    EntityPlayerSpawns(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_IRON_MASKS_MASK);
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = EntityRomConstants.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.IRON_MASKS_MASK_OPTIONS1;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        if (runtime.indoorRoom) {
            runtime.entityState.slowTransitionCountdown[freeSlot] = 0x80;
            runtime.entityState.slowTimerInitialized[freeSlot] = true;
        }
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        runtime.entityState.ironMasksMaskSourceHookshotSlot[freeSlot] = (sourceHookshotSlot + 1) & 0xFF;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.hookshotChainMotion.initializeSpawn(
                freeSlot, linkEntityX & 0xFF, linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        runtime.entityState.enemyPhysicsFlags[freeSlot] =
                2 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        return freeSlot;
    }

    int spawnPushedBlock(int x, int y) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.enemyPhysicsFlags[freeSlot] =
                2 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.enemyPhysicsFlags[freeSlot] = 0x02
                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                | EntityRomConstants.ENTITY_PHYSICS_SHADOW;
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        if (!runtime.beginLift(freeSlot, romDirection)) {
            runtime.slots[freeSlot] = RoomEntity.disabled(freeSlot);
            return -1;
        }
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                freeSlot,
                EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                0,
                false,
                EntityCombatEvent.SoundChannel.WAVE,
                0x02));
        return freeSlot;
    }

    int spawnRoomEventKeyDrop() {
        int itemType = runtime.entityState.entityMapId >= 0x0A && runtime.entityState.entityMapId != 0xFF
                ? EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                : EntityRomConstants.ENTITY_KEY_DROP_POINT;
        return runtime.pickups.spawnKeyDropAt(itemType, 0x28, 0x3C, 0, 0x70, 0, 0);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.entityState.lastArrowShotPlayedWhoosh = false;
        if (runtime.inspection().activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.playerArrowMotion.clear(freeSlot);
        runtime.entityState.playerArrowMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.latestShotArrowEntityIndex = freeSlot;
        if (runtime.entityState.bombArrowCooldown != 0) {
            int bombSlot = runtime.entityState.latestDroppedBombEntityIndex;
            runtime.entityState.bombArrowCooldown = 0;
            if (runtime.roomInspection.isLoadedEntityOfType(bombSlot, EntityRomConstants.ENTITY_BOMB)) {
                runtime.lifecycle.disableEntityWithoutPersistence(bombSlot);
            }
            runtime.entityState.latestDroppedBombEntityIndex = -1;
            runtime.entityState.playerArrowBombArrow[freeSlot] = true;
            if (runtime.spriteHandlers != null) {
                runtime.slots[freeSlot] =
                        withDefinition(runtime.slots[freeSlot], runtime.spriteHandlers.forBombArrow(), romDirection);
            }
        } else {
            runtime.entityState.bombArrowCooldown = EntityRomConstants.BOMB_ARROW_COOLDOWN;
            runtime.entityState.lastArrowShotPlayedWhoosh = true;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.boomerangMotion.clear(freeSlot);
        runtime.entityState.boomerangMotion.initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        runtime.entityState.thrownDirection[freeSlot] = romDirection;
        runtime.entityState.entityUnknownJ[freeSlot] = 1;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.swordBeamMotion.clear(freeSlot);
        runtime.entityState.swordBeamMotion.initializeSpawn(freeSlot, romDirection);
        runtime.entityState.thrownDirection[freeSlot] = romDirection;
        runtime.entityState.entityUnknownJ[freeSlot] = 1;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.magicRodFireballMotion.clear(freeSlot);
        runtime.entityState.magicRodFireballMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.entityState.thrownDirection[freeSlot] = romDirection;
        runtime.entityState.entityUnknownJ[freeSlot] = 1;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.magicPowderState[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = EntityRomConstants.MAGIC_POWDER_TRANSITION_COUNTDOWN;
        runtime.entityState.slowTransitionCountdown[freeSlot] = 0;
        runtime.entityState.slowTimerInitialized[freeSlot] = false;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.enemyPhysicsFlags[freeSlot] =
                0x03 | EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.musicalNoteInertia[freeSlot] = EntityRomConstants.MUSICAL_NOTE_INITIAL_INERTIA;
        runtime.entityState.musicalNoteSpeedX[freeSlot] = phase == 0 ? 0x06 : 0x01;
        runtime.entityState.musicalNoteSpeedY[freeSlot] = EntityRomConstants.MUSICAL_NOTE_SPEED_Y;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.entityState.lastBombPlacementPlayedBump = false;
        runtime.bombInteractions.finalizePendingBombPresentations();
        if (runtime.inspection().bombActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.bombDirection[freeSlot] = romDirection;
        runtime.entityState.thrownEntityMotion.startPlacedBomb(freeSlot, romDirection);
        runtime.entityState.placedBombMotionInitialized[freeSlot] = true;
        runtime.entityState.playerArrowBombArrow[freeSlot] = false;
        runtime.bombFinalPresentationPending[freeSlot] = false;
        runtime.entityState.bombPrivateState4[freeSlot] = 0;
        runtime.entityState.bombPrivateCountdown1[freeSlot] = 0x10;
        runtime.entityState.bombPrivateCountdown3[freeSlot] = 0;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = BombMotion.INITIAL_COUNTDOWN;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS;
        runtime.entityState.entityGroundStatus[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.BOMB_OPTIONS1;
        if (runtime.entityState.bombArrowCooldown != 0) {
            runtime.entityState.bombArrowCooldown = 0;
            int arrowSlot = runtime.entityState.latestShotArrowEntityIndex;
            if (runtime.roomInspection.isLoadedEntityOfType(arrowSlot, EntityRomConstants.ENTITY_ARROW)) {
                runtime.bombInteractions.markArrowAsBombArrow(arrowSlot);
            }
        } else {
            runtime.entityState.bombArrowCooldown = EntityRomConstants.BOMB_ARROW_COOLDOWN;
            runtime.entityState.latestDroppedBombEntityIndex = freeSlot;
            runtime.bombInteractions.applyInitialBombArrowCandidateState(freeSlot, romDirection);
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
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        boolean swampLeaves = !runtime.indoorRoom
                && runtime.entityState.spriteSelection != null
                && runtime.entityState.spriteSelection.roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
                && runtime.entityState.spriteSelection.roomId() == 0x32;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.liftableRockSmashActive[freeSlot] = true;
        runtime.entityState.liftableRockSmashCountdown[freeSlot] = initialCountdown;
        runtime.entityState.liftableRockSmashSourceVariant[freeSlot] = sourceSpriteVariant;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = physicsFlags;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_LIFTABLE_ROCK);
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.BOMB_OPTIONS1;
        if (soundId >= 0) {
            runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                    freeSlot,
                    EntityRomConstants.ENTITY_LIFTABLE_ROCK,
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.NOISE,
                    soundId));
        }
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.bombDirection[freeSlot] = runtime.entityState.playerArrowMotion.direction(arrow.slot());
        runtime.bombFinalPresentationPending[freeSlot] = false;
        runtime.entityState.bombPrivateState4[freeSlot] = 0;
        runtime.entityState.bombPrivateCountdown1[freeSlot] = 0;
        runtime.entityState.bombPrivateCountdown3[freeSlot] = 0;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = EntityRomConstants.BOMB_ARROW_EXPLOSION_COUNTDOWN;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS;
        runtime.entityState.entityGroundStatus[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.BOMB_OPTIONS1;
        runtime.entityState.placedBombMotionInitialized[freeSlot] = true;
        runtime.entityState.thrownEntityMotion.clear(freeSlot);
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        if (!runtime.indoorRoom) {
            return false;
        }
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.hookshotBridgeMotion.initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        runtime.entityState.enemyPhysicsFlags[freeSlot] =
                EntityRomConstants.ENTITY_PHYSICS_HARMLESS | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = transitionCountdown & 0xFF;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        if (runtime.defaultRandomByteSupplier == null) {
            throw new IllegalStateException("Shovel drops require a ROM random-byte supplier");
        }
        if ((runtime.defaultRandomByteSupplier.getAsInt() & 0x07) != 0 || !allowDrop) {
            return -1;
        }

        int itemType = (runtime.defaultRandomByteSupplier.getAsInt() & 0x01) == 0 ? 0x2E : 0x2D;
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.entityState.slowTransitionCountdown[freeSlot] = 0x80;
        runtime.entityState.slowTimerInitialized[freeSlot] = true;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(itemType);
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.entityGroundStatus[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = -1;
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.dropPrivateCountdown1[freeSlot] = 0x18;
        runtime.entityState.dropPrivateCountdown3[freeSlot] = 0x03;
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        runtime.entityState.enemyDropMotion.initialize(
                freeSlot, runtime.entityState.groundInteractionSideScrolling, 0x20);
        runtime.entityState.enemyDropMotion.initializeAwayFromLink(
                freeSlot, x & 0xFF, y & 0xFF, linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        runtime.entityState.enemyDropActive[freeSlot] = true;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        if (runtime.chestContentsTable == null) {
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = EntityRomConstants.CHEST_INITIAL_PHYSICS_FLAGS;
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.enemyHealth[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.entityState.chestSpeedY[freeSlot] = 0;
        runtime.entityState.chestSpeedYAccumulator[freeSlot] = 0;
        runtime.entityState.chestInertia[freeSlot] = 0;
        runtime.entityState.chestItemBySlot[freeSlot] = itemType;
        runtime.entityState.entityOptions1Override[freeSlot] = -1;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = false;
        return freeSlot;
    }
}
