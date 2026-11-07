package linksawakening.world.entity;

import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.TransientVfxRequest;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import java.util.function.IntSupplier;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM Genie encounter phases, sharing the owning room slot table. */
public final class GenieEntityController {
    private final RoomEntityRuntime runtime;
    GenieEntityController(RoomEntityRuntime runtime) { this.runtime = runtime; }

    public void advanceGenieJar(RoomEntity jarEntity, int linkEntityY) {
        int slot = jarEntity.slot();
        int freeSlots = 0;
        for (RoomEntity candidate : runtime.slots) {
            if (!candidate.loaded()) {
                freeSlots++;
            }
        }
        boolean bodySpawnSucceeded = freeSlots >= 1;
        boolean rockSpawnSucceeded = freeSlots >= 2;
        GenieMotion.Update update = runtime.genieMotion.advanceState0(
            slot, jarEntity.x(), jarEntity.y(), jarEntity.z(), runtime.geniePrivateState4[slot],
            bodySpawnSucceeded, rockSpawnSucceeded);
        GenieMotion.JarState jar = update.jar();
        runtime.geniePrivateState1[slot] = jar.privateState1();
        runtime.enemyHealth[slot] = jar.health();
        runtime.enemyPhysicsFlags[slot] = jar.physicsFlags();
        runtime.enemyHitboxFlags[slot] = jar.hitboxFlags();

        RoomEntity fallingJar = runtime.slots[slot];
        int z = addFallingSpeedToPosition(
            fallingJar.z(), runtime.genieSpeedZ[slot], runtime.genieSpeedZAccumulator, slot);
        runtime.genieSpeedZ[slot] = (runtime.genieSpeedZ[slot] - 2) & 0xFF;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            runtime.genieSpeedZ[slot] = 0;
        }
        runtime.slots[slot] = withZ(fallingJar, z);

        boolean activatedThisTick = false;
        if (update.bodySpawn() == null
            && runtime.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
            && runtime.genieActiveState[slot] == 0
            && (linkEntityY & 0xFF) < 0x70) {
            runtime.genieActiveState[slot] = 1;
            runtime.enemyTransitionCountdown[slot] = 0xFF;
            activatedThisTick = true;
        }
        if (update.bodySpawn() == null
            && runtime.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
            && runtime.genieActiveState[slot] == 1
            && !activatedThisTick) {
            if (landed && runtime.geniePrivateState3[slot] == 0) {
                runtime.genieSpeedZ[slot] = 0x10;
                runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, runtime.ENTITY_GENIE, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x20));
            }
            RoomEntity movedJar = advanceGenieJarAttract(runtime.slots[slot]);
            runtime.slots[slot] = movedJar;
            if (runtime.enemyTransitionCountdown[slot] == 1) {
                spawnGenieEntity(movedJar.x(), movedJar.y() - 0x26,
                    2, 0x47, GenieMotion.INITIAL_HEALTH,
                    GenieMotion.INITIAL_PHYSICS_FLAGS,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, runtime.ENTITY_GENIE, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x06));
            }
        }

        if (update.bodySpawn() != null) {
            spawnGenieBody(update.bodySpawn());
        }
        if (update.rockSpawn() != null && update.rockSpawnSucceeded()) {
            GenieMotion.RockSpawnRequest rock = update.rockSpawn();
            runtime.entityPlayerSpawns.spawnLiftableRockAnimation(rock.x(), rock.y(), rock.spriteVariant(),
                rock.privateCountdown1(), rock.physicsFlags(), -1);
        }
        if (update.noiseSfx() >= 0) {
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                slot, runtime.ENTITY_GENIE, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, update.noiseSfx()));
        }
        if (update.sourceUnloaded()) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
        }

    RoomEntity advanceGenieJarAttract(RoomEntity jar) {
        int slot = jar.slot();
        if (runtime.geniePrivateState3[slot] == 0) {
            EnemyRecoilMotion.Vector towardCenter = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                jar.x(), jar.y(), jar.z(), 0x50, 0x48, 0x08);
            runtime.genieSpeedX[slot] = towardCenter.x() & 0xFF;
            runtime.genieSpeedY[slot] = towardCenter.y() & 0xFF;
            if ((((jar.x() - 0x50 + 0x02) & 0xFF) < 0x04)
                && (((jar.y() - 0x48 + 0x02) & 0xFF) < 0x04)) {
                runtime.geniePrivateState3[slot] = (runtime.geniePrivateState3[slot] + 1) & 0xFF;
            }
        }
        int x = addFallingSpeedToPosition(
            jar.x(), runtime.genieSpeedX[slot], runtime.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
            jar.y(), runtime.genieSpeedY[slot], runtime.genieSpeedYAccumulator, slot);
        return withPositionAndVariant(jar, x, y, jar.spriteVariant());
        }

    public void advanceGenieAppearance(RoomEntity appearance) {
        int slot = appearance.slot();
        int countdown = runtime.enemyTransitionCountdown[slot];
        if ((countdown & 0x80) != 0) {
            if ((countdown & 0x7F) == 0) {
                if (runtime.genieControllerSlot >= 0 && runtime.genieControllerSlot < runtime.slots.length
                    && runtime.slots[runtime.genieControllerSlot].loaded()
                    && runtime.slots[runtime.genieControllerSlot].type() == runtime.ENTITY_GENIE) {
                    runtime.genieActiveState[runtime.genieControllerSlot] = 3;
                }
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
            }
            return;
        }
        if (countdown == 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown == 0x06) {
            spawnGenieEntity(appearance.x(), Math.max(appearance.y(), 0x14),
                1, 0x40, GenieMotion.INITIAL_HEALTH, 0x01, 0x8C);
        }
        }

    public void advanceGenieBody(RoomEntity body, int frameCounter,
                                  int linkEntityX, int linkEntityY,
                                  IntSupplier randomByteSupplier) {
        int slot = body.slot();
        if (runtime.genieActiveState[slot] == 0) {
            if (runtime.enemyTransitionCountdown[slot] != 0) {
                return;
            }
            runtime.genieActiveState[slot] = 1;
            runtime.geniePrivateCountdown1[slot] = 0xFF;
            boolean jarExists = runtime.genieControllerSlot >= 0
                && runtime.genieControllerSlot < runtime.slots.length
                && runtime.slots[runtime.genieControllerSlot].loaded()
                && runtime.slots[runtime.genieControllerSlot].type() == runtime.ENTITY_GENIE;
            if (jarExists) {
                runtime.pendingDialogRequests.add(new DialogRequest(0, 0x52));
                return;
            }
            runtime.genieActiveState[slot] = 4;
            runtime.enemyHealth[slot] = 0x08;
            runtime.pendingDialogRequests.add(new DialogRequest(0, 0x53));
            return;
        }
        if (runtime.genieActiveState[slot] == 4) {
            runtime.enemyHitboxFlags[slot] = 0x0C;
            runtime.entityOptions1Override[slot] = 0x81;
            if (runtime.enemyFlashCountdown[slot] == 0x02) {
                runtime.genieActiveState[slot] = 5;
                runtime.enemyTransitionCountdown[slot] = 0x80;
                return;
            }
            int x = addFallingSpeedToPosition(
                body.x(), runtime.genieSpeedX[slot], runtime.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(
                body.y(), runtime.genieSpeedY[slot], runtime.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y,
                2 + ((frameCounter >>> 4) & 1));
            runtime.slots[slot] = moved;
            if ((frameCounter & 0x03) == 0) {
                EnemyRecoilMotion.Vector desired =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x(), moved.y(), moved.z(),
                        linkEntityX, linkEntityY, 0x10);
                runtime.genieSpeedX[slot] = sourceGenieSteerStep(
                    runtime.genieSpeedX[slot], desired.x());
                runtime.genieSpeedY[slot] = sourceGenieSteerStep(
                    runtime.genieSpeedY[slot], desired.y());
            }
            return;
        }
        if (runtime.genieActiveState[slot] == 5) {
            runtime.enemyPhysicsFlags[slot] = 0x41;
            if (runtime.enemyTransitionCountdown[slot] == 0) {
                runtime.genieActiveState[slot] = 4;
                runtime.enemyPhysicsFlags[slot] = 0x01;
                RoomEntity resetBody = body;
                if ((randomByteSupplier.getAsInt() & 1) == 0) {
                    resetBody = withPositionAndVariant(body,
                        runtime.geniePrivateState4[slot], runtime.geniePrivateState3[slot],
                        body.spriteVariant());
                    runtime.slots[slot] = resetBody;
                }
                runtime.genieSpeedX[slot] = 0;
                runtime.genieSpeedY[slot] = 0;
                runtime.enemyIgnoreHitsCountdown[slot] = 0;
                spawnGenieAimedFireball(resetBody, slot, linkEntityX, linkEntityY);
                return;
            }
            EnemyRecoilMotion.Vector centerVector =
                EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), 0x50, 0x48, 0x20);
            EnemyRecoilMotion.Vector linkVector =
                EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), linkEntityX, linkEntityY, 0x04);
            runtime.genieSpeedX[slot] = (-centerVector.x() + linkVector.x()) & 0xFF;
            runtime.genieSpeedY[slot] = (centerVector.y() + linkVector.y()) & 0xFF;
            int x = addFallingSpeedToPosition(
                body.x(), runtime.genieSpeedX[slot], runtime.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(
                body.y(), runtime.genieSpeedY[slot], runtime.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y,
                2 + ((frameCounter >>> 4) & 1));
            runtime.slots[slot] = moved;
            runtime.geniePrivateState3[slot] = (0x90 - moved.y()) & 0xFF;
            runtime.geniePrivateState4[slot] = (0xA0 - moved.x()) & 0xFF;
            return;
        }
        if (runtime.genieActiveState[slot] == 2) {
            if (runtime.genieControllerSlot < 0 || runtime.genieControllerSlot >= runtime.slots.length
                || !runtime.slots[runtime.genieControllerSlot].loaded()) {
                return;
            }
            RoomEntity jar = runtime.slots[runtime.genieControllerSlot];
            int targetY = (jar.y() - 0x20) & 0xFF;
            EnemyRecoilMotion.Vector towardJar = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x(), body.y(), body.z(), jar.x(), targetY, 0x10);
            runtime.genieSpeedX[slot] = towardJar.x() & 0xFF;
            runtime.genieSpeedY[slot] = towardJar.y() & 0xFF;
            int x = addFallingSpeedToPosition(
                body.x(), runtime.genieSpeedX[slot], runtime.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(
                body.y(), runtime.genieSpeedY[slot], runtime.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, body.spriteVariant());
            runtime.slots[slot] = moved;
            int visualY = (moved.y() - moved.z()) & 0xFF;
            if ((((moved.x() - jar.x() + 3) & 0xFF) < 6)
                && (((visualY - targetY + 3) & 0xFF) < 6)) {
                runtime.enemyTransitionCountdown[slot] = 0x10;
                runtime.genieActiveState[slot] = 3;
            }
            return;
        }
        if (runtime.genieActiveState[slot] == 3) {
            int countdown = runtime.enemyTransitionCountdown[slot];
            if (countdown == 0) {
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
                return;
            }
            if (countdown == 0x04) {
                spawnGenieEntity(body.x(), body.y(), 2, 0xC7,
                    GenieMotion.INITIAL_HEALTH, GenieMotion.INITIAL_PHYSICS_FLAGS,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, runtime.ENTITY_GENIE, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x1F));
            }
            return;
        }
        if (runtime.genieActiveState[slot] != 1) {
            return;
        }
        RoomEntity moved = withPositionAndVariant(body,
            addFallingSpeedToPosition(body.x(), runtime.genieSpeedX[slot],
                runtime.genieSpeedXAccumulator, slot),
            addFallingSpeedToPosition(body.y(), runtime.genieSpeedY[slot],
                runtime.genieSpeedYAccumulator, slot),
            (frameCounter >>> 4) & 1);
        runtime.slots[slot] = moved;

        int direction = runtime.genieDirection[slot] & 1;
        if ((frameCounter & 0x07) == 0) {
            int target = direction == 0 ? 0x08 : 0xF8;
            if (runtime.genieSpeedX[slot] != target) {
                runtime.genieSpeedX[slot] = (runtime.genieSpeedX[slot] + (direction == 0 ? 1 : -1)) & 0xFF;
            }
        }
        int boundary = direction == 0 ? 0x60 : 0x40;
        if (moved.x() == boundary) {
            runtime.genieDirection[slot] ^= 1;
        }
        if ((frameCounter & 1) == 0) {
            int verticalDirection = runtime.geniePrivateState2[slot] & 1;
            int target = verticalDirection == 0 ? 0x08 : 0xF8;
            runtime.genieSpeedY[slot] = (runtime.genieSpeedY[slot]
                + (verticalDirection == 0 ? 1 : -1)) & 0xFF;
            if (runtime.genieSpeedY[slot] == target) {
                runtime.geniePrivateState2[slot] ^= 1;
            }
        }

        if (runtime.enemyTransitionCountdown[slot] == 0) {
            int inertia = runtime.entityInertia[slot] & 0xFF;
            if (inertia >= 0x08) {
                runtime.genieActiveState[slot] = 2;
                runtime.enemyTransitionCountdown[slot] = 0x30;
                return;
            }
            int[] delays = {0x10, 0x14, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40};
            runtime.enemyTransitionCountdown[slot] = delays[inertia];
            int projectileSlot = spawnGenieEntity(moved.x() - 0x0C, moved.y() - 0x14,
                3, 0, GenieMotion.INITIAL_HEALTH, 0x42,
                GenieMotion.INITIAL_HITBOX_FLAGS);
            if (projectileSlot >= 0) {
                runtime.genieSpeedZ[projectileSlot] = 0x20;
                runtime.genieSpeedX[projectileSlot] = 0x0C;
            }
        }

        if (runtime.geniePrivateCountdown1[slot] == 0) {
            runtime.geniePrivateCountdown1[slot] = 0x30;
            int xOffset = (runtime.entityInertia[slot] & 1) == 0 ? -0x0C : 0x0C;
            int fireballSlot = spawnGenieEntity(moved.x() + xOffset, moved.y(),
                4, 0, GenieMotion.INITIAL_HEALTH, 0x12,
                GenieMotion.INITIAL_HITBOX_FLAGS);
            if (fireballSlot >= 0) {
                runtime.entityInertia[slot] = (runtime.entityInertia[slot] + 1) & 0xFF;
                runtime.genieSpeedZ[fireballSlot] = 0x24;
                EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    moved.x() + xOffset, moved.y(), 0, linkEntityX, linkEntityY, 0x1F);
                runtime.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
                runtime.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
                runtime.geniePrivateCountdown2[slot] = 0x10;
                runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, runtime.ENTITY_GENIE, 0, false,
                    EntityCombatEvent.SoundChannel.NOISE, 0x28));
            }
        }
        if (runtime.geniePrivateCountdown2[slot] != 0) {
            runtime.slots[slot] = withPositionAndVariant(runtime.slots[slot], runtime.slots[slot].x(),
                runtime.slots[slot].y(), 2 + (runtime.entityInertia[slot] & 1));
        }
        }

    int spawnGenieAimedFireball(RoomEntity body, int bodySlot,
                                         int linkEntityX, int linkEntityY) {
        int xOffset = (runtime.entityInertia[bodySlot] & 1) == 0 ? -0x0C : 0x0C;
        int fireballSlot = spawnGenieEntity(body.x() + xOffset, body.y(),
            4, 0, GenieMotion.INITIAL_HEALTH, 0x12,
            GenieMotion.INITIAL_HITBOX_FLAGS);
        if (fireballSlot < 0) {
            return -1;
        }
        runtime.entityInertia[bodySlot] = (runtime.entityInertia[bodySlot] + 1) & 0xFF;
        runtime.genieSpeedZ[fireballSlot] = 0x24;
        EnemyRecoilMotion.Vector towardLink =
            EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x() + xOffset, body.y(), 0,
                linkEntityX, linkEntityY, 0x1F);
        runtime.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
        runtime.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
        runtime.pendingEntityEvents.add(new EntityCombatEvent(
            bodySlot, runtime.ENTITY_GENIE, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x28));
        return fireballSlot;
        }

    public void advanceGenieDestruction(RoomEntity genie, int frameCounter,
                                          IntSupplier randomByteSupplier) {
        int slot = genie.slot();
        if (!runtime.genieDeathInitialized[slot]) {
            runtime.genieDeathInitialized[slot] = true;
            runtime.genieActiveState[slot] = 0;
            runtime.bossAgonySfxCountdown = 0x03;
            runtime.bossAgonySourceSlot = slot;
        }
        runtime.enemyFlashCountdown[slot] = frameCounter & 0xFF;
        switch (runtime.genieActiveState[slot]) {
            case 0 -> {
                runtime.enemyTransitionCountdown[slot] = 0x40;
                runtime.genieActiveState[slot] = 1;
            }
            case 1 -> {
                if (runtime.enemyTransitionCountdown[slot] == 0) {
                    runtime.enemyTransitionCountdown[slot] = 0xA0;
                    runtime.genieActiveState[slot] = 2;
                }
            }
            default -> {
                int countdown = runtime.enemyTransitionCountdown[slot];
                if (countdown == 0) {
                    runtime.bossRewards.spawnMoldormHeartContainer(genie);
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, runtime.ENTITY_GENIE, 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x1A));
                    return;
                }
                if ((countdown & 0x07) == 0) {
                    int x = (genie.x()
                        + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
                    int y = ((genie.y() - genie.z())
                        + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
                    runtime.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.POOF, x, y));
                    runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, runtime.ENTITY_GENIE, 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x13));
                }
            }
        }
        }

    public void advanceGenieSideProjectile(RoomEntity projectile, int frameCounter) {
        int slot = projectile.slot();
        int combinedSpeedX = (runtime.genieSpeedX[slot] + runtime.genieSharedBodySpeedX) & 0xFF;
        int combinedSpeedY = (runtime.genieSpeedY[slot] + runtime.genieSharedBodySpeedY) & 0xFF;
        int x = addFallingSpeedToPosition(
            projectile.x(), combinedSpeedX, runtime.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
            projectile.y(), combinedSpeedY, runtime.genieSpeedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(projectile, x, y,
            (frameCounter >>> 3) & 1);
        if (runtime.genieActiveState[slot] == 0) {
            int z = addFallingSpeedToPosition(
                moved.z(), runtime.genieSpeedZ[slot], runtime.genieSpeedZAccumulator, slot);
            runtime.genieSpeedZ[slot] = (runtime.genieSpeedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                runtime.genieActiveState[slot] = 1;
                runtime.enemyTransitionCountdown[slot] = 0x08;
                runtime.genieSpeedX[slot] = 0xE0;
            }
            runtime.slots[slot] = withZ(moved, z);
            return;
        }
        runtime.slots[slot] = moved;
        if (runtime.enemyTransitionCountdown[slot] == 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
        }

    public void advanceGenieFireball(RoomEntity fireball, int frameCounter) {
        int slot = fireball.slot();
        int x = addFallingSpeedToPosition(
            fireball.x(), runtime.genieSpeedX[slot], runtime.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
            fireball.y(), runtime.genieSpeedY[slot], runtime.genieSpeedYAccumulator, slot);
        int z = addFallingSpeedToPosition(
            fireball.z(), runtime.genieSpeedZ[slot], runtime.genieSpeedZAccumulator, slot);
        runtime.genieSpeedZ[slot] = (runtime.genieSpeedZ[slot] - 2) & 0xFF;
        if ((z & 0x80) != 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.slots[slot] = withZ(withPositionAndVariant(fireball, x, y,
            (frameCounter >>> 3) & 1), z);
        }

    int spawnGenieBody(GenieMotion.BodySpawnRequest request) {
        return spawnGenieEntity(request.x(), request.y(), request.privateState1(),
            request.transitionCountdown(), request.health(),
            GenieMotion.INITIAL_PHYSICS_FLAGS, GenieMotion.INITIAL_HITBOX_FLAGS);
        }

    int spawnGenieEntity(int x, int y, int privateState1,
                                 int transitionCountdown, int health,
                                 int physicsFlags, int hitboxFlags) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_GENIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(freeSlot, -1, runtime.ENTITY_GENIE,
            x & 0xFF, y & 0xFF, EntityStatus.ACTIVE, definition, variant,
            0, 0, 0, -1, false);
        runtime.genieActiveState[freeSlot] = 0;
        runtime.geniePrivateState1[freeSlot] = privateState1 & 0xFF;
        runtime.geniePrivateState2[freeSlot] = 0;
        runtime.geniePrivateState3[freeSlot] = 0;
        runtime.geniePrivateState4[freeSlot] = 0;
        runtime.geniePrivateCountdown1[freeSlot] = 0;
        runtime.geniePrivateCountdown2[freeSlot] = 0;
        runtime.genieDirection[freeSlot] = 0;
        runtime.genieDeathInitialized[freeSlot] = false;
        runtime.genieSpeedX[freeSlot] = 0;
        runtime.genieSpeedY[freeSlot] = 0;
        runtime.genieSpeedZ[freeSlot] = 0;
        runtime.genieSpeedXAccumulator[freeSlot] = 0;
        runtime.genieSpeedYAccumulator[freeSlot] = 0;
        runtime.genieSpeedZAccumulator[freeSlot] = 0;
        runtime.genieMotion.clear(freeSlot);
        runtime.enemyTransitionCountdown[freeSlot] = transitionCountdown & 0xFF;
        runtime.enemyHealth[freeSlot] = health & 0xFF;
        runtime.enemyPhysicsFlags[freeSlot] = physicsFlags & 0xFF;
        runtime.enemyHitboxFlags[freeSlot] = hitboxFlags & 0xFF;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityOptions1Override[freeSlot] = runtime.GENIE_OPTIONS1;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
        }

    RoomEntity registerGenieJarImpact(RoomEntity jar) {
        int slot = jar.slot();
        if (runtime.genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        runtime.genieImpactRegisteredThisFrame[slot] = true;
        runtime.geniePrivateState4[slot] = (runtime.geniePrivateState4[slot] + 1) & 0xFF;
        runtime.enemyFlashCountdown[slot] = 0x20;
        runtime.pendingEntityEvents.add(new EntityCombatEvent(
            slot, runtime.ENTITY_GENIE, 0, false,
            EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (runtime.geniePrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        runtime.genieActiveState[slot] = 1;
        runtime.geniePrivateState3[slot] = 0;
        runtime.enemyTransitionCountdown[slot] = 0x80;
        runtime.thrownEntityMotion.clear(slot);
        runtime.thrownMotionInitialized[slot] = false;
        return withStatus(jar, EntityStatus.ACTIVE);
        }
}
