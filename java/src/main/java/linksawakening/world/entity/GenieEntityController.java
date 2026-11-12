package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** ROM Genie encounter phases, sharing the owning room slot table. */
public final class GenieEntityController {
    private final RoomEntityRuntime runtime;

    GenieEntityController(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

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
        GenieMotion.Update update = runtime.entityState.genieMotion.advanceState0(
                slot,
                jarEntity.x(),
                jarEntity.y(),
                jarEntity.z(),
                runtime.entityState.geniePrivateState4[slot],
                bodySpawnSucceeded,
                rockSpawnSucceeded);
        GenieMotion.JarState jar = update.jar();
        runtime.entityState.geniePrivateState1[slot] = jar.privateState1();
        runtime.entityState.enemyHealth[slot] = jar.health();
        runtime.entityState.enemyPhysicsFlags[slot] = jar.physicsFlags();
        runtime.entityState.enemyHitboxFlags[slot] = jar.hitboxFlags();

        RoomEntity fallingJar = runtime.slots[slot];
        int z = addFallingSpeedToPosition(
                fallingJar.z(),
                runtime.entityState.genieSpeedZ[slot],
                runtime.entityState.genieSpeedZAccumulator,
                slot);
        runtime.entityState.genieSpeedZ[slot] = (runtime.entityState.genieSpeedZ[slot] - 2) & 0xFF;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            runtime.entityState.genieSpeedZ[slot] = 0;
        }
        runtime.slots[slot] = withZ(fallingJar, z);

        boolean activatedThisTick = false;
        if (update.bodySpawn() == null
                && runtime.entityState.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && runtime.entityState.genieActiveState[slot] == 0
                && (linkEntityY & 0xFF) < 0x70) {
            runtime.entityState.genieActiveState[slot] = 1;
            runtime.entityState.enemyTransitionCountdown[slot] = 0xFF;
            activatedThisTick = true;
        }
        if (update.bodySpawn() == null
                && runtime.entityState.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && runtime.entityState.genieActiveState[slot] == 1
                && !activatedThisTick) {
            if (landed && runtime.entityState.geniePrivateState3[slot] == 0) {
                runtime.entityState.genieSpeedZ[slot] = 0x10;
                runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x20));
            }
            RoomEntity movedJar = advanceGenieJarAttract(runtime.slots[slot]);
            runtime.slots[slot] = movedJar;
            if (runtime.entityState.enemyTransitionCountdown[slot] == 1) {
                spawnGenieEntity(
                        movedJar.x(),
                        movedJar.y() - 0x26,
                        2,
                        0x47,
                        GenieMotion.INITIAL_HEALTH,
                        GenieMotion.INITIAL_PHYSICS_FLAGS,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x06));
            }
        }

        if (update.bodySpawn() != null) {
            spawnGenieBody(update.bodySpawn());
        }
        if (update.rockSpawn() != null && update.rockSpawnSucceeded()) {
            GenieMotion.RockSpawnRequest rock = update.rockSpawn();
            runtime.entityPlayerSpawns.spawnLiftableRockAnimation(
                    rock.x(), rock.y(), rock.spriteVariant(), rock.privateCountdown1(), rock.physicsFlags(), -1);
        }
        if (update.noiseSfx() >= 0) {
            runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                    slot,
                    EntityRomConstants.ENTITY_GENIE,
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.NOISE,
                    update.noiseSfx()));
        }
        if (update.sourceUnloaded()) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
    }

    RoomEntity advanceGenieJarAttract(RoomEntity jar) {
        int slot = jar.slot();
        if (runtime.entityState.geniePrivateState3[slot] == 0) {
            EnemyRecoilMotion.Vector towardCenter =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(jar.x(), jar.y(), jar.z(), 0x50, 0x48, 0x08);
            runtime.entityState.genieSpeedX[slot] = towardCenter.x() & 0xFF;
            runtime.entityState.genieSpeedY[slot] = towardCenter.y() & 0xFF;
            if ((((jar.x() - 0x50 + 0x02) & 0xFF) < 0x04) && (((jar.y() - 0x48 + 0x02) & 0xFF) < 0x04)) {
                runtime.entityState.geniePrivateState3[slot] =
                        (runtime.entityState.geniePrivateState3[slot] + 1) & 0xFF;
            }
        }
        int x = addFallingSpeedToPosition(
                jar.x(), runtime.entityState.genieSpeedX[slot], runtime.entityState.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
                jar.y(), runtime.entityState.genieSpeedY[slot], runtime.entityState.genieSpeedYAccumulator, slot);
        return withPositionAndVariant(jar, x, y, jar.spriteVariant());
    }

    public void advanceGenieAppearance(RoomEntity appearance) {
        int slot = appearance.slot();
        int countdown = runtime.entityState.enemyTransitionCountdown[slot];
        if ((countdown & 0x80) != 0) {
            if ((countdown & 0x7F) == 0) {
                if (runtime.entityState.genieControllerSlot >= 0
                        && runtime.entityState.genieControllerSlot < runtime.slots.length
                        && runtime.slots[runtime.entityState.genieControllerSlot].loaded()
                        && runtime.slots[runtime.entityState.genieControllerSlot].type()
                                == EntityRomConstants.ENTITY_GENIE) {
                    runtime.entityState.genieActiveState[runtime.entityState.genieControllerSlot] = 3;
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
            spawnGenieEntity(
                    appearance.x(), Math.max(appearance.y(), 0x14), 1, 0x40, GenieMotion.INITIAL_HEALTH, 0x01, 0x8C);
        }
    }

    public void advanceGenieBody(
            RoomEntity body, int frameCounter, int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier) {
        int slot = body.slot();
        if (runtime.entityState.genieActiveState[slot] == 0) {
            if (runtime.entityState.enemyTransitionCountdown[slot] != 0) {
                return;
            }
            runtime.entityState.genieActiveState[slot] = 1;
            runtime.entityState.geniePrivateCountdown1[slot] = 0xFF;
            boolean jarExists = runtime.entityState.genieControllerSlot >= 0
                    && runtime.entityState.genieControllerSlot < runtime.slots.length
                    && runtime.slots[runtime.entityState.genieControllerSlot].loaded()
                    && runtime.slots[runtime.entityState.genieControllerSlot].type() == EntityRomConstants.ENTITY_GENIE;
            if (jarExists) {
                runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(0, 0x52));
                return;
            }
            runtime.entityState.genieActiveState[slot] = 4;
            runtime.entityState.enemyHealth[slot] = 0x08;
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(0, 0x53));
            return;
        }
        if (runtime.entityState.genieActiveState[slot] == 4) {
            runtime.entityState.enemyHitboxFlags[slot] = 0x0C;
            runtime.entityState.entityOptions1Override[slot] = 0x81;
            if (runtime.entityState.enemyFlashCountdown[slot] == 0x02) {
                runtime.entityState.genieActiveState[slot] = 5;
                runtime.entityState.enemyTransitionCountdown[slot] = 0x80;
                return;
            }
            int x = addFallingSpeedToPosition(
                    body.x(), runtime.entityState.genieSpeedX[slot], runtime.entityState.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(
                    body.y(), runtime.entityState.genieSpeedY[slot], runtime.entityState.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
            runtime.slots[slot] = moved;
            if ((frameCounter & 0x03) == 0) {
                EnemyRecoilMotion.Vector desired = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x10);
                runtime.entityState.genieSpeedX[slot] =
                        sourceGenieSteerStep(runtime.entityState.genieSpeedX[slot], desired.x());
                runtime.entityState.genieSpeedY[slot] =
                        sourceGenieSteerStep(runtime.entityState.genieSpeedY[slot], desired.y());
            }
            return;
        }
        if (runtime.entityState.genieActiveState[slot] == 5) {
            runtime.entityState.enemyPhysicsFlags[slot] = 0x41;
            if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
                runtime.entityState.genieActiveState[slot] = 4;
                runtime.entityState.enemyPhysicsFlags[slot] = 0x01;
                RoomEntity resetBody = body;
                if ((randomByteSupplier.getAsInt() & 1) == 0) {
                    resetBody = withPositionAndVariant(
                            body,
                            runtime.entityState.geniePrivateState4[slot],
                            runtime.entityState.geniePrivateState3[slot],
                            body.spriteVariant());
                    runtime.slots[slot] = resetBody;
                }
                runtime.entityState.genieSpeedX[slot] = 0;
                runtime.entityState.genieSpeedY[slot] = 0;
                runtime.entityState.enemyIgnoreHitsCountdown[slot] = 0;
                spawnGenieAimedFireball(resetBody, slot, linkEntityX, linkEntityY);
                return;
            }
            EnemyRecoilMotion.Vector centerVector =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(body.x(), body.y(), body.z(), 0x50, 0x48, 0x20);
            EnemyRecoilMotion.Vector linkVector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), linkEntityX, linkEntityY, 0x04);
            runtime.entityState.genieSpeedX[slot] = (-centerVector.x() + linkVector.x()) & 0xFF;
            runtime.entityState.genieSpeedY[slot] = (centerVector.y() + linkVector.y()) & 0xFF;
            int x = addFallingSpeedToPosition(
                    body.x(), runtime.entityState.genieSpeedX[slot], runtime.entityState.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(
                    body.y(), runtime.entityState.genieSpeedY[slot], runtime.entityState.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
            runtime.slots[slot] = moved;
            runtime.entityState.geniePrivateState3[slot] = (0x90 - moved.y()) & 0xFF;
            runtime.entityState.geniePrivateState4[slot] = (0xA0 - moved.x()) & 0xFF;
            return;
        }
        if (runtime.entityState.genieActiveState[slot] == 2) {
            if (runtime.entityState.genieControllerSlot < 0
                    || runtime.entityState.genieControllerSlot >= runtime.slots.length
                    || !runtime.slots[runtime.entityState.genieControllerSlot].loaded()) {
                return;
            }
            RoomEntity jar = runtime.slots[runtime.entityState.genieControllerSlot];
            int targetY = (jar.y() - 0x20) & 0xFF;
            EnemyRecoilMotion.Vector towardJar = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), jar.x(), targetY, 0x10);
            runtime.entityState.genieSpeedX[slot] = towardJar.x() & 0xFF;
            runtime.entityState.genieSpeedY[slot] = towardJar.y() & 0xFF;
            int x = addFallingSpeedToPosition(
                    body.x(), runtime.entityState.genieSpeedX[slot], runtime.entityState.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(
                    body.y(), runtime.entityState.genieSpeedY[slot], runtime.entityState.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, body.spriteVariant());
            runtime.slots[slot] = moved;
            int visualY = (moved.y() - moved.z()) & 0xFF;
            if ((((moved.x() - jar.x() + 3) & 0xFF) < 6) && (((visualY - targetY + 3) & 0xFF) < 6)) {
                runtime.entityState.enemyTransitionCountdown[slot] = 0x10;
                runtime.entityState.genieActiveState[slot] = 3;
            }
            return;
        }
        if (runtime.entityState.genieActiveState[slot] == 3) {
            int countdown = runtime.entityState.enemyTransitionCountdown[slot];
            if (countdown == 0) {
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
                return;
            }
            if (countdown == 0x04) {
                spawnGenieEntity(
                        body.x(),
                        body.y(),
                        2,
                        0xC7,
                        GenieMotion.INITIAL_HEALTH,
                        GenieMotion.INITIAL_PHYSICS_FLAGS,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1F));
            }
            return;
        }
        if (runtime.entityState.genieActiveState[slot] != 1) {
            return;
        }
        RoomEntity moved = withPositionAndVariant(
                body,
                addFallingSpeedToPosition(
                        body.x(),
                        runtime.entityState.genieSpeedX[slot],
                        runtime.entityState.genieSpeedXAccumulator,
                        slot),
                addFallingSpeedToPosition(
                        body.y(),
                        runtime.entityState.genieSpeedY[slot],
                        runtime.entityState.genieSpeedYAccumulator,
                        slot),
                (frameCounter >>> 4) & 1);
        runtime.slots[slot] = moved;

        int direction = runtime.entityState.genieDirection[slot] & 1;
        if ((frameCounter & 0x07) == 0) {
            int target = direction == 0 ? 0x08 : 0xF8;
            if (runtime.entityState.genieSpeedX[slot] != target) {
                runtime.entityState.genieSpeedX[slot] =
                        (runtime.entityState.genieSpeedX[slot] + (direction == 0 ? 1 : -1)) & 0xFF;
            }
        }
        int boundary = direction == 0 ? 0x60 : 0x40;
        if (moved.x() == boundary) {
            runtime.entityState.genieDirection[slot] ^= 1;
        }
        if ((frameCounter & 1) == 0) {
            int verticalDirection = runtime.entityState.geniePrivateState2[slot] & 1;
            int target = verticalDirection == 0 ? 0x08 : 0xF8;
            runtime.entityState.genieSpeedY[slot] =
                    (runtime.entityState.genieSpeedY[slot] + (verticalDirection == 0 ? 1 : -1)) & 0xFF;
            if (runtime.entityState.genieSpeedY[slot] == target) {
                runtime.entityState.geniePrivateState2[slot] ^= 1;
            }
        }

        if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
            int inertia = runtime.entityState.entityInertia[slot] & 0xFF;
            if (inertia >= 0x08) {
                runtime.entityState.genieActiveState[slot] = 2;
                runtime.entityState.enemyTransitionCountdown[slot] = 0x30;
                return;
            }
            int[] delays = {0x10, 0x14, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40};
            runtime.entityState.enemyTransitionCountdown[slot] = delays[inertia];
            int projectileSlot = spawnGenieEntity(
                    moved.x() - 0x0C,
                    moved.y() - 0x14,
                    3,
                    0,
                    GenieMotion.INITIAL_HEALTH,
                    0x42,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
            if (projectileSlot >= 0) {
                runtime.entityState.genieSpeedZ[projectileSlot] = 0x20;
                runtime.entityState.genieSpeedX[projectileSlot] = 0x0C;
            }
        }

        if (runtime.entityState.geniePrivateCountdown1[slot] == 0) {
            runtime.entityState.geniePrivateCountdown1[slot] = 0x30;
            int xOffset = (runtime.entityState.entityInertia[slot] & 1) == 0 ? -0x0C : 0x0C;
            int fireballSlot = spawnGenieEntity(
                    moved.x() + xOffset,
                    moved.y(),
                    4,
                    0,
                    GenieMotion.INITIAL_HEALTH,
                    0x12,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
            if (fireballSlot >= 0) {
                runtime.entityState.entityInertia[slot] = (runtime.entityState.entityInertia[slot] + 1) & 0xFF;
                runtime.entityState.genieSpeedZ[fireballSlot] = 0x24;
                EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x() + xOffset, moved.y(), 0, linkEntityX, linkEntityY, 0x1F);
                runtime.entityState.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
                runtime.entityState.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
                runtime.entityState.geniePrivateCountdown2[slot] = 0x10;
                runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x28));
            }
        }
        if (runtime.entityState.geniePrivateCountdown2[slot] != 0) {
            runtime.slots[slot] = withPositionAndVariant(
                    runtime.slots[slot],
                    runtime.slots[slot].x(),
                    runtime.slots[slot].y(),
                    2 + (runtime.entityState.entityInertia[slot] & 1));
        }
    }

    int spawnGenieAimedFireball(RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        int xOffset = (runtime.entityState.entityInertia[bodySlot] & 1) == 0 ? -0x0C : 0x0C;
        int fireballSlot = spawnGenieEntity(
                body.x() + xOffset, body.y(), 4, 0, GenieMotion.INITIAL_HEALTH, 0x12, GenieMotion.INITIAL_HITBOX_FLAGS);
        if (fireballSlot < 0) {
            return -1;
        }
        runtime.entityState.entityInertia[bodySlot] = (runtime.entityState.entityInertia[bodySlot] + 1) & 0xFF;
        runtime.entityState.genieSpeedZ[fireballSlot] = 0x24;
        EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x() + xOffset, body.y(), 0, linkEntityX, linkEntityY, 0x1F);
        runtime.entityState.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
        runtime.entityState.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                bodySlot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x28));
        return fireballSlot;
    }

    public void advanceGenieDestruction(RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        int slot = genie.slot();
        if (!runtime.entityState.genieDeathInitialized[slot]) {
            runtime.entityState.genieDeathInitialized[slot] = true;
            runtime.entityState.genieActiveState[slot] = 0;
            runtime.entityState.bossAgonySfxCountdown = 0x03;
            runtime.entityState.bossAgonySourceSlot = slot;
        }
        runtime.entityState.enemyFlashCountdown[slot] = frameCounter & 0xFF;
        switch (runtime.entityState.genieActiveState[slot]) {
            case 0 -> {
                runtime.entityState.enemyTransitionCountdown[slot] = 0x40;
                runtime.entityState.genieActiveState[slot] = 1;
            }
            case 1 -> {
                if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
                    runtime.entityState.enemyTransitionCountdown[slot] = 0xA0;
                    runtime.entityState.genieActiveState[slot] = 2;
                }
            }
            default -> {
                int countdown = runtime.entityState.enemyTransitionCountdown[slot];
                if (countdown == 0) {
                    runtime.bossRewards.spawnMoldormHeartContainer(genie);
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
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
                    runtime.entityState.events.transientVfxRequests.add(
                            new TransientVfxRequest(TransientVfxType.POOF, x, y));
                    runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
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

    public void advanceGenieSideProjectile(RoomEntity projectile, int frameCounter) {
        int slot = projectile.slot();
        int combinedSpeedX = (runtime.entityState.genieSpeedX[slot] + runtime.entityState.genieSharedBodySpeedX) & 0xFF;
        int combinedSpeedY = (runtime.entityState.genieSpeedY[slot] + runtime.entityState.genieSharedBodySpeedY) & 0xFF;
        int x = addFallingSpeedToPosition(
                projectile.x(), combinedSpeedX, runtime.entityState.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
                projectile.y(), combinedSpeedY, runtime.entityState.genieSpeedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(projectile, x, y, (frameCounter >>> 3) & 1);
        if (runtime.entityState.genieActiveState[slot] == 0) {
            int z = addFallingSpeedToPosition(
                    moved.z(), runtime.entityState.genieSpeedZ[slot], runtime.entityState.genieSpeedZAccumulator, slot);
            runtime.entityState.genieSpeedZ[slot] = (runtime.entityState.genieSpeedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                runtime.entityState.genieActiveState[slot] = 1;
                runtime.entityState.enemyTransitionCountdown[slot] = 0x08;
                runtime.entityState.genieSpeedX[slot] = 0xE0;
            }
            runtime.slots[slot] = withZ(moved, z);
            return;
        }
        runtime.slots[slot] = moved;
        if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
    }

    public void advanceGenieFireball(RoomEntity fireball, int frameCounter) {
        int slot = fireball.slot();
        int x = addFallingSpeedToPosition(
                fireball.x(), runtime.entityState.genieSpeedX[slot], runtime.entityState.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
                fireball.y(), runtime.entityState.genieSpeedY[slot], runtime.entityState.genieSpeedYAccumulator, slot);
        int z = addFallingSpeedToPosition(
                fireball.z(), runtime.entityState.genieSpeedZ[slot], runtime.entityState.genieSpeedZAccumulator, slot);
        runtime.entityState.genieSpeedZ[slot] = (runtime.entityState.genieSpeedZ[slot] - 2) & 0xFF;
        if ((z & 0x80) != 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.slots[slot] = withZ(withPositionAndVariant(fireball, x, y, (frameCounter >>> 3) & 1), z);
    }

    int spawnGenieBody(GenieMotion.BodySpawnRequest request) {
        return spawnGenieEntity(
                request.x(),
                request.y(),
                request.privateState1(),
                request.transitionCountdown(),
                request.health(),
                GenieMotion.INITIAL_PHYSICS_FLAGS,
                GenieMotion.INITIAL_HITBOX_FLAGS);
    }

    int spawnGenieEntity(
            int x, int y, int privateState1, int transitionCountdown, int health, int physicsFlags, int hitboxFlags) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_GENIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(
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
        runtime.entityState.genieActiveState[freeSlot] = 0;
        runtime.entityState.geniePrivateState1[freeSlot] = privateState1 & 0xFF;
        runtime.entityState.geniePrivateState2[freeSlot] = 0;
        runtime.entityState.geniePrivateState3[freeSlot] = 0;
        runtime.entityState.geniePrivateState4[freeSlot] = 0;
        runtime.entityState.geniePrivateCountdown1[freeSlot] = 0;
        runtime.entityState.geniePrivateCountdown2[freeSlot] = 0;
        runtime.entityState.genieDirection[freeSlot] = 0;
        runtime.entityState.genieDeathInitialized[freeSlot] = false;
        runtime.entityState.genieSpeedX[freeSlot] = 0;
        runtime.entityState.genieSpeedY[freeSlot] = 0;
        runtime.entityState.genieSpeedZ[freeSlot] = 0;
        runtime.entityState.genieSpeedXAccumulator[freeSlot] = 0;
        runtime.entityState.genieSpeedYAccumulator[freeSlot] = 0;
        runtime.entityState.genieSpeedZAccumulator[freeSlot] = 0;
        runtime.entityState.genieMotion.clear(freeSlot);
        runtime.entityState.enemyTransitionCountdown[freeSlot] = transitionCountdown & 0xFF;
        runtime.entityState.enemyHealth[freeSlot] = health & 0xFF;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = physicsFlags & 0xFF;
        runtime.entityState.enemyHitboxFlags[freeSlot] = hitboxFlags & 0xFF;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.entityOptions1Override[freeSlot] = EntityRomConstants.GENIE_OPTIONS1;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    RoomEntity registerGenieJarImpact(RoomEntity jar) {
        int slot = jar.slot();
        if (runtime.entityState.genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        runtime.entityState.genieImpactRegisteredThisFrame[slot] = true;
        runtime.entityState.geniePrivateState4[slot] = (runtime.entityState.geniePrivateState4[slot] + 1) & 0xFF;
        runtime.entityState.enemyFlashCountdown[slot] = 0x20;
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (runtime.entityState.geniePrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        runtime.entityState.genieActiveState[slot] = 1;
        runtime.entityState.geniePrivateState3[slot] = 0;
        runtime.entityState.enemyTransitionCountdown[slot] = 0x80;
        runtime.entityState.thrownEntityMotion.clear(slot);
        runtime.entityState.thrownMotionInitialized[slot] = false;
        return withStatus(jar, EntityStatus.ACTIVE);
    }

    public int geniePrivateCountdown1(int slot) {
        return runtime.entityState.geniePrivateCountdown1[slot];
    }

    public void decrementGeniePrivateCountdown1(int slot) {
        runtime.entityState.geniePrivateCountdown1[slot]--;
    }

    public int geniePrivateCountdown2(int slot) {
        return runtime.entityState.geniePrivateCountdown2[slot];
    }

    public void decrementGeniePrivateCountdown2(int slot) {
        runtime.entityState.geniePrivateCountdown2[slot]--;
    }

    public int geniePrivateState1(int slot) {
        return runtime.entityState.geniePrivateState1[slot];
    }

    public int geniePrivateState4(int slot) {
        return runtime.entityState.geniePrivateState4[slot];
    }

    public void setGenieControllerSlot(int value) {
        runtime.entityState.genieControllerSlot = value;
    }

    public void setGenieSharedBodySpeedX(int value) {
        runtime.entityState.genieSharedBodySpeedX = value;
    }

    public int genieSpeedX(int slot) {
        return runtime.entityState.genieSpeedX[slot];
    }

    public void setGenieSharedBodySpeedY(int value) {
        runtime.entityState.genieSharedBodySpeedY = value;
    }

    public int genieSpeedY(int slot) {
        return runtime.entityState.genieSpeedY[slot];
    }

    public int genieActiveState(int slot) {
        return runtime.entityState.genieActiveState[slot];
    }
}
