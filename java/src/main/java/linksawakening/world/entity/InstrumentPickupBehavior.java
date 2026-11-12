package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityRewardController. */
final class InstrumentPickupBehavior {
    static void advanceInstrumentPickup(
            EntityRewardController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            IntSupplier randomByteSupplier) {
        new InstrumentPickupStep(owner, entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier)
                .run();
    }

    static void spawnInstrumentParticle(EntityRewardController owner, RoomEntity source, int offsetIndex, int variant) {
        var runtime = owner.runtime;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0 || runtime.spriteHandlers() == null) {
            return;
        }
        int[] xOffsets = {0x0A, -0x06};
        int[] speedX = {0x04, 0xFC};
        EntitySpriteDefinition definition = runtime.spriteHandlers().forInstrumentShard();
        runtime.slots()[freeSlot] = new RoomEntity(
                freeSlot,
                -1,
                source.type(),
                (source.x() + xOffsets[offsetIndex]) & 0xFF,
                (source.y() - 0x08) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant & 0x01,
                0,
                0,
                0,
                -1,
                false);
        runtime.initializeHealthGroup(freeSlot);
        owner.writeInstrumentParticleKind(freeSlot, 1);
        owner.writeInstrumentParticleSpeedX(freeSlot, speedX[offsetIndex]);
        owner.writeInstrumentParticleSpeedY(freeSlot, 0xFD);
        owner.writeInstrumentParticleXAccumulator(freeSlot, 0);
        owner.writeInstrumentParticleYAccumulator(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0x38);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, ENTITY_PHYSICS_HARMLESS);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }
}
