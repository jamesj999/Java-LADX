package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.ZombieMotion;

/** Source-ordered allocation and initialization for this spawn family. */
final class EnemyChildSpawns {
    static void spawnZombieChild(EntityEnemySpawnsServices runtime, ZombieMotion.SpawnRequest spawn) {
        int freeSlot = runtime.findFreeEntitySlotAtMost(0x05);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_ZOMBIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity child = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_ZOMBIE,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = child;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        // ConfigureNewEntity starts from the static $52 row; the handler
        // clears projectile noclip before the child is first dispatched.
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot, ZombieMotion.INITIAL_PHYSICS_FLAGS & ~ZombieMotion.PROJECTILE_NOCLIP_FLAG);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_ZOMBIE));
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_ZOMBIE));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.motions().get(ZombieMotion.class, ZombieMotion::new).initializeChild(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static void spawnPokeySegment(EntityEnemySpawnsServices runtime, PokeyMotion.SegmentSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionForPokeySegment();
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity segment = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_POKEY,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                spawn.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = segment;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, spawn.transitionCountdown());
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_POKEY));
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_POKEY));
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_POKEY));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        // ConfigureNewEntity seeds one ignored-hit frame; the child handler
        // decrements it before its first interactive collision pass.
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeEntityInertia(freeSlot, 0);
        runtime.motions()
                .get(PokeyMotion.class, PokeyMotion::new)
                .initializeSegment(freeSlot, spawn.speedX(), spawn.speedY());
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static boolean spawnEvasiveClone(
            EntityEnemySpawnsServices runtime, RoomEntity source, StalfosEvasiveMotion.CloneRequest request) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition cloneDefinition =
                runtime.slotProperties().spriteDefinitionForEvasiveState(source.spriteDefinition(), 1);
        int cloneVariant = cloneDefinition.supported() ? cloneDefinition.initialVariant() : -1;
        RoomEntity clone = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_STALFOS_EVASIVE,
                request.x(),
                request.y(),
                EntityStatus.ACTIVE,
                cloneDefinition,
                cloneVariant,
                0,
                0,
                request.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = clone;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.EVASIVE_CLONE_OPTIONS);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.EVASIVE_CLONE_PHYSICS_FLAGS);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        // SpawnNewEntity writes one frame, but the Evasive handler immediately
        // stores register B over it. AnimateEntities keeps B at zero, so this
        // clone enters its fleeing handler with no ignore-hits countdown.
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .initializeClone(freeSlot, request.speedX(), request.speedY());
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0A));
        return true;
    }

    static boolean canSpawnCuccoAngry(EntityEnemySpawnsServices runtime) {
        for (int slot = 7; slot >= 0; slot--) {
            if (!runtime.slots()[slot].loaded()) return true;
        }
        return false;
    }

    static boolean spawnCuccoAngry(EntityEnemySpawnsServices runtime, CuccoMotion.SpawnRequest request) {
        int freeSlot = runtime.findFreeEntitySlotAtMost(7);
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_CUCCO);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity angry = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_CUCCO,
                request.x(),
                request.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                request.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = angry;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(CuccoMotion.class, CuccoMotion::new)
                .initializeAngry(freeSlot, request.speedX(), request.speedY());
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, CuccoMotion.ANGRY_OPTIONS1);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, CuccoMotion.ANGRY_PHYSICS_FLAGS);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, CuccoMotion.ANGRY_HITBOX_FLAGS);
        runtime.combatTables().writeEnemyHealth(freeSlot, CuccoMotion.HEALTH_OVERRIDE);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }
}
