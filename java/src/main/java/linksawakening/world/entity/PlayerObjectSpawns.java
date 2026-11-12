package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** Source-ordered allocation and initialization for this spawn family. */
final class PlayerObjectSpawns {
    static int spawnPushedBlock(EntityPlayerSpawnsServices runtime, int x, int y) {
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

    static int spawnLiftedRoomObject(
            EntityPlayerSpawnsServices runtime, int x, int y, int romDirection, int spriteVariant) {
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

    static int spawnLiftableRockSmash(
            EntityPlayerSpawnsServices runtime, int entityX, int entityY, int sourceSpriteVariant) {
        if (sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK
                && sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_BUSH
                && sourceSpriteVariant != EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_GRASS) {
            throw new IllegalArgumentException("Liftable-rock source variant must be 0, 1, or FF");
        }
        boolean rock = sourceSpriteVariant == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK;
        int initialCountdown = rock ? 0x0F : 0x1F;
        return PlayerObjectSpawns.spawnLiftableRockAnimation(
                runtime,
                entityX,
                entityY,
                sourceSpriteVariant,
                initialCountdown,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS,
                rock ? 0x09 : 0x05);
    }

    static int spawnLiftableRockRubble(EntityPlayerSpawnsServices runtime, int entityX, int entityY) {
        return PlayerObjectSpawns.spawnLiftableRockAnimation(
                runtime,
                entityX,
                entityY,
                EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK,
                0x0F,
                EntityRomConstants.LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS,
                -1);
    }

    static int spawnLiftableRockAnimation(
            EntityPlayerSpawnsServices runtime,
            int entityX,
            int entityY,
            int sourceSpriteVariant,
            int initialCountdown,
            int physicsFlags,
            int soundId) {
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
}
