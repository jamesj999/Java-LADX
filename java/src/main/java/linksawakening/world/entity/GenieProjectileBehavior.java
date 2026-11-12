package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GenieMotion;

/** Focused behavior using the state owned by GenieEntityController. */
final class GenieProjectileBehavior {
    static void advanceGenieDestruction(
            GenieEntityController owner, RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        var runtime = owner.runtime;
        int slot = genie.slot();
        if (!owner.genieDeathInitialized[slot]) {
            owner.genieDeathInitialized[slot] = true;
            owner.genieActiveState[slot] = 0;
            runtime.bossRewards().writeBossAgonySfxCountdown(0x03);
            runtime.bossRewards().writeBossAgonySourceSlot(slot);
        }
        runtime.combatTables().writeEnemyFlashCountdown(slot, frameCounter & 0xFF);
        switch (owner.genieActiveState[slot]) {
            case 0 -> {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x40);
                owner.genieActiveState[slot] = 1;
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
                    owner.genieActiveState[slot] = 2;
                }
            }
            default -> {
                int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
                if (countdown == 0) {
                    runtime.bossRewards().spawnMoldormHeartContainer(genie);
                    runtime.lifecycle().disableEntityWithoutPersistence(slot);
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot,
                                    EntityRomConstants.ENTITY_GENIE,
                                    0,
                                    false,
                                    EntityCombatEvent.SoundChannel.NOISE,
                                    0x1A));
                    return;
                }
                if ((countdown & 0x07) == 0) {
                    int x = (genie.x() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
                    int y = ((genie.y() - genie.z()) + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
                    runtime.events().transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot,
                                    EntityRomConstants.ENTITY_GENIE,
                                    0,
                                    false,
                                    EntityCombatEvent.SoundChannel.NOISE,
                                    0x13));
                }
            }
        }
    }

    static void advanceGenieSideProjectile(GenieEntityController owner, RoomEntity projectile, int frameCounter) {
        var runtime = owner.runtime;
        int slot = projectile.slot();
        int combinedSpeedX = (owner.genieSpeedX[slot] + owner.genieSharedBodySpeedX) & 0xFF;
        int combinedSpeedY = (owner.genieSpeedY[slot] + owner.genieSharedBodySpeedY) & 0xFF;
        int x = addFallingSpeedToPosition(projectile.x(), combinedSpeedX, owner.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(projectile.y(), combinedSpeedY, owner.genieSpeedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(projectile, x, y, (frameCounter >>> 3) & 1);
        if (owner.genieActiveState[slot] == 0) {
            int z = addFallingSpeedToPosition(moved.z(), owner.genieSpeedZ[slot], owner.genieSpeedZAccumulator, slot);
            owner.genieSpeedZ[slot] = (owner.genieSpeedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                owner.genieActiveState[slot] = 1;
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x08);
                owner.genieSpeedX[slot] = 0xE0;
            }
            runtime.slots()[slot] = withZ(moved, z);
            return;
        }
        runtime.slots()[slot] = moved;
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }

    static int spawnGenieEntity(
            GenieEntityController owner,
            int x,
            int y,
            int privateState1,
            int transitionCountdown,
            int health,
            int physicsFlags,
            int hitboxFlags) {
        var runtime = owner.runtime;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_GENIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots()[freeSlot] = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_GENIE,
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
        owner.genieActiveState[freeSlot] = 0;
        owner.geniePrivateState1[freeSlot] = privateState1 & 0xFF;
        owner.geniePrivateState2[freeSlot] = 0;
        owner.geniePrivateState3[freeSlot] = 0;
        owner.geniePrivateState4[freeSlot] = 0;
        owner.geniePrivateCountdown1[freeSlot] = 0;
        owner.geniePrivateCountdown2[freeSlot] = 0;
        owner.genieDirection[freeSlot] = 0;
        owner.genieDeathInitialized[freeSlot] = false;
        owner.genieSpeedX[freeSlot] = 0;
        owner.genieSpeedY[freeSlot] = 0;
        owner.genieSpeedZ[freeSlot] = 0;
        owner.genieSpeedXAccumulator[freeSlot] = 0;
        owner.genieSpeedYAccumulator[freeSlot] = 0;
        owner.genieSpeedZAccumulator[freeSlot] = 0;
        runtime.motions().get(GenieMotion.class, GenieMotion::new).clear(freeSlot);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, transitionCountdown & 0xFF);
        runtime.combatTables().writeEnemyHealth(freeSlot, health & 0xFF);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, physicsFlags & 0xFF);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, hitboxFlags & 0xFF);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.GENIE_OPTIONS1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }
}
