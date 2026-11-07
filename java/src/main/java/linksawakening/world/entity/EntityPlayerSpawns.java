package linksawakening.world.entity;

import linksawakening.world.room.ChestContentsTable;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteDefinition;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM slot allocation and player-created entity spawning. */
public final class EntityPlayerSpawns {
    private final RoomEntityRuntime runtime;
    EntityPlayerSpawns(RoomEntityRuntime runtime) { this.runtime = runtime; }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_IRON_MASKS_MASK);
        int selectedVariant = definition.supported()
            ? Math.min(variant & 0x01, definition.variantCount() - 1) : -1;
        RoomEntity mask = new RoomEntity(freeSlot, -1, runtime.ENTITY_IRON_MASKS_MASK,
            x & 0xFF, y & 0xFF, EntityStatus.ACTIVE, definition, selectedVariant,
            0, 0, 0, -1, false);
        runtime.slots[freeSlot] = mask;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_IRON_MASKS_MASK);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyPhysicsFlags[freeSlot] = runtime.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS;
        runtime.entityOptions1Override[freeSlot] = runtime.IRON_MASKS_MASK_OPTIONS1;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        if (runtime.indoorRoom) {
            runtime.slowTransitionCountdown[freeSlot] = 0x80;
            runtime.slowTimerInitialized[freeSlot] = true;
        }
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        runtime.ironMasksMaskSourceHookshotSlot[freeSlot] = (sourceHookshotSlot + 1) & 0xFF;
    }

    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ,
                           int romDirection) {
        if (runtime.hookshotActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_HOOKSHOT_CHAIN);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity chain = new RoomEntity(freeSlot, -1, runtime.ENTITY_HOOKSHOT_CHAIN,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF, -1, false);
        runtime.slots[freeSlot] = chain;
        runtime.hookshotChainMotion.initializeSpawn(freeSlot, linkEntityX & 0xFF,
            linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        runtime.enemyPhysicsFlags[freeSlot] = 2 | runtime.ENTITY_PHYSICS_HARMLESS
            | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION
            | runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        return freeSlot;
    }

    int spawnPushedBlock(int x, int y) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_PUSHED_BLOCK);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(freeSlot, -1, runtime.ENTITY_PUSHED_BLOCK,
            x & 0xFF, y & 0xFF, EntityStatus.ACTIVE, definition, variant, 0, 0, 0, -1, false);
        runtime.enemyPhysicsFlags[freeSlot] = 2 | runtime.ENTITY_PHYSICS_HARMLESS
            | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_LIFTABLE_ROCK);
        int variant = definition.supported() ? spriteVariant : -1;
        runtime.slots[freeSlot] = new RoomEntity(freeSlot, -1, runtime.ENTITY_LIFTABLE_ROCK,
            x & 0xFF, y & 0xFF, EntityStatus.ACTIVE, definition, variant, 0, 0, 0, -1, false);
        runtime.enemyPhysicsFlags[freeSlot] = 0x02 | runtime.ENTITY_PHYSICS_HARMLESS
            | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP | runtime.ENTITY_PHYSICS_SHADOW;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_LIFTABLE_ROCK);
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        if (!runtime.beginLift(freeSlot, romDirection)) {
            runtime.slots[freeSlot] = RoomEntity.disabled(freeSlot);
            return -1;
        }
        runtime.pendingEntityEvents.add(new EntityCombatEvent(
            freeSlot, runtime.ENTITY_LIFTABLE_ROCK, 0, false,
            EntityCombatEvent.SoundChannel.WAVE, 0x02));
        return freeSlot;
    }

    int spawnRoomEventKeyDrop() {
        int itemType = runtime.entityMapId >= 0x0A && runtime.entityMapId != 0xFF
            ? runtime.ENTITY_HIDING_SLIME_KEY : runtime.ENTITY_KEY_DROP_POINT;
        return runtime.pickups.spawnKeyDropAt(itemType, 0x28, 0x3C, 0, 0x70, 0, 0);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection,
                   boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.lastArrowShotPlayedWhoosh = false;
        if (runtime.activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_ARROW);
        int variant = romDirection;
        RoomEntity arrow = new RoomEntity(freeSlot, -1, runtime.ENTITY_ARROW,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF, -1, false);
        runtime.slots[freeSlot] = arrow;
        runtime.playerArrowMotion.clear(freeSlot);
        runtime.playerArrowMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION
            | runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.enemyPhysicsFlags[freeSlot] = 2 | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.latestShotArrowEntityIndex = freeSlot;
        if (runtime.bombArrowCooldown != 0) {
            int bombSlot = runtime.latestDroppedBombEntityIndex;
            runtime.bombArrowCooldown = 0;
            if (runtime.roomInspection.isLoadedEntityOfType(bombSlot, runtime.ENTITY_BOMB)) {
                runtime.lifecycle.disableEntityWithoutPersistence(bombSlot);
            }
            runtime.latestDroppedBombEntityIndex = -1;
            runtime.playerArrowBombArrow[freeSlot] = true;
            if (runtime.spriteHandlers != null) {
                runtime.slots[freeSlot] = withDefinition(runtime.slots[freeSlot],
                    runtime.spriteHandlers.forBombArrow(), romDirection);
            }
        } else {
            runtime.bombArrowCooldown = runtime.BOMB_ARROW_COOLDOWN;
            runtime.lastArrowShotPlayedWhoosh = true;
        }
        return freeSlot;
    }

    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ,
                       int romDirection, int pressedButtonsMask) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_BOOMERANG);
        RoomEntity boomerang = new RoomEntity(freeSlot, -1, runtime.ENTITY_BOOMERANG,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, romDirection, 0, 0, (linkEntityZ + 1) & 0xFF, -1, false);
        runtime.slots[freeSlot] = boomerang;
        runtime.boomerangMotion.clear(freeSlot);
        runtime.boomerangMotion.initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        runtime.thrownDirection[freeSlot] = romDirection;
        runtime.entityUnknownJ[freeSlot] = 1;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION
            | runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.enemyPhysicsFlags[freeSlot] = 2 | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        return freeSlot;
    }

    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ,
                       int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_SWORD_BEAM);
        int variant = definition.supported() ? romDirection : -1;
        RoomEntity beam = new RoomEntity(freeSlot, -1, runtime.ENTITY_SWORD_BEAM,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF, -1, false);
        runtime.slots[freeSlot] = beam;
        runtime.swordBeamMotion.clear(freeSlot);
        runtime.swordBeamMotion.initializeSpawn(freeSlot, romDirection);
        runtime.thrownDirection[freeSlot] = romDirection;
        runtime.entityUnknownJ[freeSlot] = 1;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION
            | runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.enemyPhysicsFlags[freeSlot] = 2 | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        return freeSlot;
    }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ,
                              int romDirection) {
        return spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ,
                              int romDirection, boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_MAGIC_ROD_FIREBALL);
        MagicRodFireballMotion.SpawnData spawn =
            MagicRodFireballMotion.spawnData(romDirection, pieceOfPower);
        int variant = definition.supported() ? spawn.initialVariant() : -1;
        RoomEntity fireball = new RoomEntity(freeSlot, -1, runtime.ENTITY_MAGIC_ROD_FIREBALL,
            (linkEntityX + spawn.offsetX()) & 0xFF,
            (linkEntityY + spawn.offsetY()) & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF, -1, false);
        runtime.slots[freeSlot] = fireball;
        runtime.magicRodFireballMotion.clear(freeSlot);
        runtime.magicRodFireballMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.thrownDirection[freeSlot] = romDirection;
        runtime.entityUnknownJ[freeSlot] = 1;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION
            | runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.enemyPhysicsFlags[freeSlot] = 2 | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        return freeSlot;
    }

    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ,
                                 int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(
            runtime.ENTITY_MAGIC_POWDER_SPRINKLE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity sprinkle = new RoomEntity(freeSlot, -1,
            runtime.ENTITY_MAGIC_POWDER_SPRINKLE,
            (linkEntityX + xOffsets[romDirection]) & 0xFF,
            (linkEntityY + yOffsets[romDirection]) & 0xFF,
            EntityStatus.ACTIVE, definition, variant, 0, 0, linkEntityZ & 0xFF, -1, false);
        runtime.slots[freeSlot] = sprinkle;
        runtime.magicPowderState[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = runtime.MAGIC_POWDER_TRANSITION_COUNTDOWN;
        runtime.slowTransitionCountdown[freeSlot] = 0;
        runtime.slowTimerInitialized[freeSlot] = false;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyPhysicsFlags[freeSlot] = 0x03 | runtime.ENTITY_PHYSICS_HARMLESS
            | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        int phase = animationPhase & 0x01;
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_MUSICAL_NOTE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        int x = linkEntityX + (phase == 0 ? 0x08 : -0x08);
        RoomEntity note = new RoomEntity(freeSlot, -1, runtime.ENTITY_MUSICAL_NOTE,
            x & 0xFF, (linkEntityY - 0x08) & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, 0, -1, false);
        runtime.slots[freeSlot] = note;
        runtime.musicalNoteInertia[freeSlot] = runtime.MUSICAL_NOTE_INITIAL_INERTIA;
        runtime.musicalNoteSpeedX[freeSlot] = phase == 0 ? 0x06 : 0x01;
        runtime.musicalNoteSpeedY[freeSlot] = runtime.MUSICAL_NOTE_SPEED_Y;
        runtime.enemyPhysicsFlags[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.lastBombPlacementPlayedBump = false;
        runtime.bombInteractions.finalizePendingBombPresentations();
        if (runtime.bombActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(freeSlot, -1, runtime.ENTITY_BOMB,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF, -1, false);
        runtime.slots[freeSlot] = bomb;
        runtime.bombDirection[freeSlot] = romDirection;
        runtime.thrownEntityMotion.startPlacedBomb(freeSlot, romDirection);
        runtime.placedBombMotionInitialized[freeSlot] = true;
        runtime.playerArrowBombArrow[freeSlot] = false;
        runtime.bombFinalPresentationPending[freeSlot] = false;
        runtime.bombPrivateState4[freeSlot] = 0;
        runtime.bombPrivateCountdown1[freeSlot] = 0x10;
        runtime.bombPrivateCountdown3[freeSlot] = 0;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = BombMotion.INITIAL_COUNTDOWN;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.enemyPhysicsFlags[freeSlot] = runtime.BOMB_INITIAL_PHYSICS_FLAGS;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.BOMB_OPTIONS1;
        if (runtime.bombArrowCooldown != 0) {
            runtime.bombArrowCooldown = 0;
            int arrowSlot = runtime.latestShotArrowEntityIndex;
            if (runtime.roomInspection.isLoadedEntityOfType(arrowSlot, runtime.ENTITY_ARROW)) {
                runtime.bombInteractions.markArrowAsBombArrow(arrowSlot);
            }
        } else {
            runtime.bombArrowCooldown = runtime.BOMB_ARROW_COOLDOWN;
            runtime.latestDroppedBombEntityIndex = freeSlot;
            runtime.bombInteractions.applyInitialBombArrowCandidateState(freeSlot, romDirection);
        }
        return freeSlot;
    }

    int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        if (sourceSpriteVariant != runtime.LIFTABLE_ROCK_SMASH_MODE_ROCK
            && sourceSpriteVariant != runtime.LIFTABLE_ROCK_SMASH_MODE_BUSH
            && sourceSpriteVariant != runtime.LIFTABLE_ROCK_SMASH_MODE_GRASS) {
            throw new IllegalArgumentException("Liftable-rock source variant must be 0, 1, or FF");
        }
        boolean rock = sourceSpriteVariant == runtime.LIFTABLE_ROCK_SMASH_MODE_ROCK;
        int initialCountdown = rock ? 0x0F : 0x1F;
        return spawnLiftableRockAnimation(entityX, entityY, sourceSpriteVariant,
            initialCountdown, runtime.LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS, rock ? 0x09 : 0x05);
    }

    int spawnLiftableRockRubble(int entityX, int entityY) {
        return spawnLiftableRockAnimation(entityX, entityY, runtime.LIFTABLE_ROCK_SMASH_MODE_ROCK,
            0x0F, runtime.LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS, -1);
    }

    int spawnLiftableRockAnimation(int entityX, int entityY, int sourceSpriteVariant,
                                           int initialCountdown, int physicsFlags, int soundId) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_LIFTABLE_ROCK);
        boolean swampLeaves = !runtime.indoorRoom && runtime.spriteSelection != null
            && runtime.spriteSelection.roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
            && runtime.spriteSelection.roomId() == 0x32;
        int variant = definition.supported()
            ? EntityMotionMath.liftableRockSmashVariant(sourceSpriteVariant, swampLeaves, initialCountdown) : -1;
        RoomEntity smash = new RoomEntity(freeSlot, -1, runtime.ENTITY_LIFTABLE_ROCK,
            entityX & 0xFF, entityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, 0, -1, false);
        runtime.slots[freeSlot] = smash;
        runtime.liftableRockSmashActive[freeSlot] = true;
        runtime.liftableRockSmashCountdown[freeSlot] = initialCountdown;
        runtime.liftableRockSmashSourceVariant[freeSlot] = sourceSpriteVariant;
        runtime.enemyPhysicsFlags[freeSlot] = physicsFlags;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_LIFTABLE_ROCK);
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.entityOptions1Override[freeSlot] = runtime.BOMB_OPTIONS1;
        if (soundId >= 0) {
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                freeSlot, runtime.ENTITY_LIFTABLE_ROCK, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, soundId));
        }
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(freeSlot, -1, runtime.ENTITY_BOMB,
            arrow.x(), arrow.y(), EntityStatus.ACTIVE, definition, variant,
            0, 0, arrow.z(), -1, false);
        runtime.slots[freeSlot] = bomb;
        runtime.bombDirection[freeSlot] = runtime.playerArrowMotion.direction(arrow.slot());
        runtime.bombFinalPresentationPending[freeSlot] = false;
        runtime.bombPrivateState4[freeSlot] = 0;
        runtime.bombPrivateCountdown1[freeSlot] = 0;
        runtime.bombPrivateCountdown3[freeSlot] = 0;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = runtime.BOMB_ARROW_EXPLOSION_COUNTDOWN;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.enemyPhysicsFlags[freeSlot] = runtime.BOMB_INITIAL_PHYSICS_FLAGS;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.BOMB_OPTIONS1;
        runtime.placedBombMotionInitialized[freeSlot] = true;
        runtime.thrownEntityMotion.clear(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction,
                                        int transitionCountdown) {
        if (!runtime.indoorRoom) {
            return false;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_HOOKSHOT_BRIDGE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bridge = new RoomEntity(freeSlot, -1, runtime.ENTITY_HOOKSHOT_BRIDGE,
            bridgeX & 0xFF, bridgeY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, 0, -1, false);
        runtime.slots[freeSlot] = bridge;
        runtime.hookshotBridgeMotion.initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        runtime.enemyPhysicsFlags[freeSlot] = runtime.ENTITY_PHYSICS_HARMLESS
            | runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = transitionCountdown & 0xFF;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION
            | runtime.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY,
                        boolean allowDrop) {
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
        RoomEntity drop = new RoomEntity(freeSlot, -1, itemType, x & 0xFF, y & 0xFF,
            EntityStatus.ACTIVE, definition, variant, 0, 0, 0, -1, false);
        runtime.slots[freeSlot] = drop;
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.slowTransitionCountdown[freeSlot] = 0x80;
        runtime.slowTimerInitialized[freeSlot] = true;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(itemType);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dropPrivateCountdown1[freeSlot] = 0x18;
        runtime.dropPrivateCountdown3[freeSlot] = 0x03;
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        runtime.enemyDropMotion.initialize(freeSlot, runtime.groundInteractionSideScrolling, 0x20);
        runtime.enemyDropMotion.initializeAwayFromLink(freeSlot, x & 0xFF, y & 0xFF,
            linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        runtime.enemyDropActive[freeSlot] = true;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
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
        RoomEntity chest = new RoomEntity(freeSlot, -1, runtime.ENTITY_CHEST_WITH_ITEM,
            x & 0xFF, y & 0xFF, EntityStatus.INIT, definition, spriteVariant,
            0, 0, 0, -1, false);
        runtime.slots[freeSlot] = chest;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyPhysicsFlags[freeSlot] = runtime.CHEST_INITIAL_PHYSICS_FLAGS;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.chestSpeedY[freeSlot] = 0;
        runtime.chestSpeedYAccumulator[freeSlot] = 0;
        runtime.chestInertia[freeSlot] = 0;
        runtime.chestItemBySlot[freeSlot] = itemType;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = false;
        return freeSlot;
    }
}
