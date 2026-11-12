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
    private final int[] genieActiveState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] geniePrivateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] geniePrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] geniePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] geniePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] geniePrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] geniePrivateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] genieDeathInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] genieSpeedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] genieImpactRegisteredThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int genieControllerSlot = -1;
    private int genieSharedBodySpeedX;
    private int genieSharedBodySpeedY;

    private final GenieEntityControllerServices runtime;

    GenieEntityController(GenieEntityControllerServices runtime) {
        this.runtime = runtime;
    }

    public void advanceGenieJar(RoomEntity jarEntity, int linkEntityY) {
        int slot = jarEntity.slot();
        int freeSlots = 0;
        for (RoomEntity candidate : runtime.slots()) {
            if (!candidate.loaded()) {
                freeSlots++;
            }
        }
        boolean bodySpawnSucceeded = freeSlots >= 1;
        boolean rockSpawnSucceeded = freeSlots >= 2;
        GenieMotion.Update update = runtime.motionSet()
                .genieMotion
                .advanceState0(
                        slot,
                        jarEntity.x(),
                        jarEntity.y(),
                        jarEntity.z(),
                        geniePrivateState4[slot],
                        bodySpawnSucceeded,
                        rockSpawnSucceeded);
        GenieMotion.JarState jar = update.jar();
        geniePrivateState1[slot] = jar.privateState1();
        runtime.combatTables().writeEnemyHealth(slot, jar.health());
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, jar.physicsFlags());
        runtime.combatTables().writeEnemyHitboxFlags(slot, jar.hitboxFlags());

        RoomEntity fallingJar = runtime.slots()[slot];
        int z = addFallingSpeedToPosition(fallingJar.z(), genieSpeedZ[slot], genieSpeedZAccumulator, slot);
        genieSpeedZ[slot] = (genieSpeedZ[slot] - 2) & 0xFF;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            genieSpeedZ[slot] = 0;
        }
        runtime.slots()[slot] = withZ(fallingJar, z);

        boolean activatedThisTick = false;
        if (update.bodySpawn() == null
                && geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && genieActiveState[slot] == 0
                && (linkEntityY & 0xFF) < 0x70) {
            genieActiveState[slot] = 1;
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            activatedThisTick = true;
        }
        if (update.bodySpawn() == null
                && geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && genieActiveState[slot] == 1
                && !activatedThisTick) {
            if (landed && geniePrivateState3[slot] == 0) {
                genieSpeedZ[slot] = 0x10;
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x20));
            }
            RoomEntity movedJar = advanceGenieJarAttract(runtime.slots()[slot]);
            runtime.slots()[slot] = movedJar;
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 1) {
                spawnGenieEntity(
                        movedJar.x(),
                        movedJar.y() - 0x26,
                        2,
                        0x47,
                        GenieMotion.INITIAL_HEALTH,
                        GenieMotion.INITIAL_PHYSICS_FLAGS,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x06));
            }
        }

        if (update.bodySpawn() != null) {
            spawnGenieBody(update.bodySpawn());
        }
        if (update.rockSpawn() != null && update.rockSpawnSucceeded()) {
            GenieMotion.RockSpawnRequest rock = update.rockSpawn();
            runtime.entityPlayerSpawns()
                    .spawnLiftableRockAnimation(
                            rock.x(),
                            rock.y(),
                            rock.spriteVariant(),
                            rock.privateCountdown1(),
                            rock.physicsFlags(),
                            -1);
        }
        if (update.noiseSfx() >= 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            update.noiseSfx()));
        }
        if (update.sourceUnloaded()) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }

    RoomEntity advanceGenieJarAttract(RoomEntity jar) {
        int slot = jar.slot();
        if (geniePrivateState3[slot] == 0) {
            EnemyRecoilMotion.Vector towardCenter =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(jar.x(), jar.y(), jar.z(), 0x50, 0x48, 0x08);
            genieSpeedX[slot] = towardCenter.x() & 0xFF;
            genieSpeedY[slot] = towardCenter.y() & 0xFF;
            if ((((jar.x() - 0x50 + 0x02) & 0xFF) < 0x04) && (((jar.y() - 0x48 + 0x02) & 0xFF) < 0x04)) {
                geniePrivateState3[slot] = (geniePrivateState3[slot] + 1) & 0xFF;
            }
        }
        int x = addFallingSpeedToPosition(jar.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(jar.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
        return withPositionAndVariant(jar, x, y, jar.spriteVariant());
    }

    public void advanceGenieAppearance(RoomEntity appearance) {
        int slot = appearance.slot();
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if ((countdown & 0x80) != 0) {
            if ((countdown & 0x7F) == 0) {
                if (genieControllerSlot >= 0
                        && genieControllerSlot < runtime.slots().length
                        && runtime.slots()[genieControllerSlot].loaded()
                        && runtime.slots()[genieControllerSlot].type() == EntityRomConstants.ENTITY_GENIE) {
                    genieActiveState[genieControllerSlot] = 3;
                }
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
            }
            return;
        }
        if (countdown == 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
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
        if (genieActiveState[slot] == 0) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) != 0) {
                return;
            }
            genieActiveState[slot] = 1;
            geniePrivateCountdown1[slot] = 0xFF;
            boolean jarExists = genieControllerSlot >= 0
                    && genieControllerSlot < runtime.slots().length
                    && runtime.slots()[genieControllerSlot].loaded()
                    && runtime.slots()[genieControllerSlot].type() == EntityRomConstants.ENTITY_GENIE;
            if (jarExists) {
                runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x52));
                return;
            }
            genieActiveState[slot] = 4;
            runtime.combatTables().writeEnemyHealth(slot, 0x08);
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x53));
            return;
        }
        if (genieActiveState[slot] == 4) {
            runtime.combatTables().writeEnemyHitboxFlags(slot, 0x0C);
            runtime.physicsTables().writeEntityOptions1Override(slot, 0x81);
            if (runtime.combatTables().readEnemyFlashCountdown(slot) == 0x02) {
                genieActiveState[slot] = 5;
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x80);
                return;
            }
            int x = addFallingSpeedToPosition(body.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(body.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
            runtime.slots()[slot] = moved;
            if ((frameCounter & 0x03) == 0) {
                EnemyRecoilMotion.Vector desired = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x10);
                genieSpeedX[slot] = sourceGenieSteerStep(genieSpeedX[slot], desired.x());
                genieSpeedY[slot] = sourceGenieSteerStep(genieSpeedY[slot], desired.y());
            }
            return;
        }
        if (genieActiveState[slot] == 5) {
            runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x41);
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                genieActiveState[slot] = 4;
                runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x01);
                RoomEntity resetBody = body;
                if ((randomByteSupplier.getAsInt() & 1) == 0) {
                    resetBody = withPositionAndVariant(
                            body, geniePrivateState4[slot], geniePrivateState3[slot], body.spriteVariant());
                    runtime.slots()[slot] = resetBody;
                }
                genieSpeedX[slot] = 0;
                genieSpeedY[slot] = 0;
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
                spawnGenieAimedFireball(resetBody, slot, linkEntityX, linkEntityY);
                return;
            }
            EnemyRecoilMotion.Vector centerVector =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(body.x(), body.y(), body.z(), 0x50, 0x48, 0x20);
            EnemyRecoilMotion.Vector linkVector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), linkEntityX, linkEntityY, 0x04);
            genieSpeedX[slot] = (-centerVector.x() + linkVector.x()) & 0xFF;
            genieSpeedY[slot] = (centerVector.y() + linkVector.y()) & 0xFF;
            int x = addFallingSpeedToPosition(body.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(body.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
            runtime.slots()[slot] = moved;
            geniePrivateState3[slot] = (0x90 - moved.y()) & 0xFF;
            geniePrivateState4[slot] = (0xA0 - moved.x()) & 0xFF;
            return;
        }
        if (genieActiveState[slot] == 2) {
            if (genieControllerSlot < 0
                    || genieControllerSlot >= runtime.slots().length
                    || !runtime.slots()[genieControllerSlot].loaded()) {
                return;
            }
            RoomEntity jar = runtime.slots()[genieControllerSlot];
            int targetY = (jar.y() - 0x20) & 0xFF;
            EnemyRecoilMotion.Vector towardJar = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), jar.x(), targetY, 0x10);
            genieSpeedX[slot] = towardJar.x() & 0xFF;
            genieSpeedY[slot] = towardJar.y() & 0xFF;
            int x = addFallingSpeedToPosition(body.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(body.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, body.spriteVariant());
            runtime.slots()[slot] = moved;
            int visualY = (moved.y() - moved.z()) & 0xFF;
            if ((((moved.x() - jar.x() + 3) & 0xFF) < 6) && (((visualY - targetY + 3) & 0xFF) < 6)) {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
                genieActiveState[slot] = 3;
            }
            return;
        }
        if (genieActiveState[slot] == 3) {
            int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
            if (countdown == 0) {
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
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
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x1F));
            }
            return;
        }
        if (genieActiveState[slot] != 1) {
            return;
        }
        RoomEntity moved = withPositionAndVariant(
                body,
                addFallingSpeedToPosition(body.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot),
                addFallingSpeedToPosition(body.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot),
                (frameCounter >>> 4) & 1);
        runtime.slots()[slot] = moved;

        int direction = genieDirection[slot] & 1;
        if ((frameCounter & 0x07) == 0) {
            int target = direction == 0 ? 0x08 : 0xF8;
            if (genieSpeedX[slot] != target) {
                genieSpeedX[slot] = (genieSpeedX[slot] + (direction == 0 ? 1 : -1)) & 0xFF;
            }
        }
        int boundary = direction == 0 ? 0x60 : 0x40;
        if (moved.x() == boundary) {
            genieDirection[slot] ^= 1;
        }
        if ((frameCounter & 1) == 0) {
            int verticalDirection = geniePrivateState2[slot] & 1;
            int target = verticalDirection == 0 ? 0x08 : 0xF8;
            genieSpeedY[slot] = (genieSpeedY[slot] + (verticalDirection == 0 ? 1 : -1)) & 0xFF;
            if (genieSpeedY[slot] == target) {
                geniePrivateState2[slot] ^= 1;
            }
        }

        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            int inertia = runtime.physicsTables().readEntityInertia(slot) & 0xFF;
            if (inertia >= 0x08) {
                genieActiveState[slot] = 2;
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x30);
                return;
            }
            int[] delays = {0x10, 0x14, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40};
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, delays[inertia]);
            int projectileSlot = spawnGenieEntity(
                    moved.x() - 0x0C,
                    moved.y() - 0x14,
                    3,
                    0,
                    GenieMotion.INITIAL_HEALTH,
                    0x42,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
            if (projectileSlot >= 0) {
                genieSpeedZ[projectileSlot] = 0x20;
                genieSpeedX[projectileSlot] = 0x0C;
            }
        }

        if (geniePrivateCountdown1[slot] == 0) {
            geniePrivateCountdown1[slot] = 0x30;
            int xOffset = (runtime.physicsTables().readEntityInertia(slot) & 1) == 0 ? -0x0C : 0x0C;
            int fireballSlot = spawnGenieEntity(
                    moved.x() + xOffset,
                    moved.y(),
                    4,
                    0,
                    GenieMotion.INITIAL_HEALTH,
                    0x12,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
            if (fireballSlot >= 0) {
                runtime.physicsTables()
                        .writeEntityInertia(slot, (runtime.physicsTables().readEntityInertia(slot) + 1) & 0xFF);
                genieSpeedZ[fireballSlot] = 0x24;
                EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x() + xOffset, moved.y(), 0, linkEntityX, linkEntityY, 0x1F);
                genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
                genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
                geniePrivateCountdown2[slot] = 0x10;
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.NOISE,
                                0x28));
            }
        }
        if (geniePrivateCountdown2[slot] != 0) {
            runtime.slots()[slot] = withPositionAndVariant(
                    runtime.slots()[slot],
                    runtime.slots()[slot].x(),
                    runtime.slots()[slot].y(),
                    2 + (runtime.physicsTables().readEntityInertia(slot) & 1));
        }
    }

    int spawnGenieAimedFireball(RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        int xOffset = (runtime.physicsTables().readEntityInertia(bodySlot) & 1) == 0 ? -0x0C : 0x0C;
        int fireballSlot = spawnGenieEntity(
                body.x() + xOffset, body.y(), 4, 0, GenieMotion.INITIAL_HEALTH, 0x12, GenieMotion.INITIAL_HITBOX_FLAGS);
        if (fireballSlot < 0) {
            return -1;
        }
        runtime.physicsTables()
                .writeEntityInertia(bodySlot, (runtime.physicsTables().readEntityInertia(bodySlot) + 1) & 0xFF);
        genieSpeedZ[fireballSlot] = 0x24;
        EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x() + xOffset, body.y(), 0, linkEntityX, linkEntityY, 0x1F);
        genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
        genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        bodySlot,
                        EntityRomConstants.ENTITY_GENIE,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x28));
        return fireballSlot;
    }

    public void advanceGenieDestruction(RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        int slot = genie.slot();
        if (!genieDeathInitialized[slot]) {
            genieDeathInitialized[slot] = true;
            genieActiveState[slot] = 0;
            runtime.bossRewards().writeBossAgonySfxCountdown(0x03);
            runtime.bossRewards().writeBossAgonySourceSlot(slot);
        }
        runtime.combatTables().writeEnemyFlashCountdown(slot, frameCounter & 0xFF);
        switch (genieActiveState[slot]) {
            case 0 -> {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x40);
                genieActiveState[slot] = 1;
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
                    genieActiveState[slot] = 2;
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

    public void advanceGenieSideProjectile(RoomEntity projectile, int frameCounter) {
        int slot = projectile.slot();
        int combinedSpeedX = (genieSpeedX[slot] + genieSharedBodySpeedX) & 0xFF;
        int combinedSpeedY = (genieSpeedY[slot] + genieSharedBodySpeedY) & 0xFF;
        int x = addFallingSpeedToPosition(projectile.x(), combinedSpeedX, genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(projectile.y(), combinedSpeedY, genieSpeedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(projectile, x, y, (frameCounter >>> 3) & 1);
        if (genieActiveState[slot] == 0) {
            int z = addFallingSpeedToPosition(moved.z(), genieSpeedZ[slot], genieSpeedZAccumulator, slot);
            genieSpeedZ[slot] = (genieSpeedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                genieActiveState[slot] = 1;
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x08);
                genieSpeedX[slot] = 0xE0;
            }
            runtime.slots()[slot] = withZ(moved, z);
            return;
        }
        runtime.slots()[slot] = moved;
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }

    public void advanceGenieFireball(RoomEntity fireball, int frameCounter) {
        int slot = fireball.slot();
        int x = addFallingSpeedToPosition(fireball.x(), genieSpeedX[slot], genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(fireball.y(), genieSpeedY[slot], genieSpeedYAccumulator, slot);
        int z = addFallingSpeedToPosition(fireball.z(), genieSpeedZ[slot], genieSpeedZAccumulator, slot);
        genieSpeedZ[slot] = (genieSpeedZ[slot] - 2) & 0xFF;
        if ((z & 0x80) != 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.slots()[slot] = withZ(withPositionAndVariant(fireball, x, y, (frameCounter >>> 3) & 1), z);
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
        genieActiveState[freeSlot] = 0;
        geniePrivateState1[freeSlot] = privateState1 & 0xFF;
        geniePrivateState2[freeSlot] = 0;
        geniePrivateState3[freeSlot] = 0;
        geniePrivateState4[freeSlot] = 0;
        geniePrivateCountdown1[freeSlot] = 0;
        geniePrivateCountdown2[freeSlot] = 0;
        genieDirection[freeSlot] = 0;
        genieDeathInitialized[freeSlot] = false;
        genieSpeedX[freeSlot] = 0;
        genieSpeedY[freeSlot] = 0;
        genieSpeedZ[freeSlot] = 0;
        genieSpeedXAccumulator[freeSlot] = 0;
        genieSpeedYAccumulator[freeSlot] = 0;
        genieSpeedZAccumulator[freeSlot] = 0;
        runtime.motionSet().genieMotion.clear(freeSlot);
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

    RoomEntity registerGenieJarImpact(RoomEntity jar) {
        int slot = jar.slot();
        if (genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        genieImpactRegisteredThisFrame[slot] = true;
        geniePrivateState4[slot] = (geniePrivateState4[slot] + 1) & 0xFF;
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0x20);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (geniePrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        genieActiveState[slot] = 1;
        geniePrivateState3[slot] = 0;
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x80);
        runtime.motionSet().thrownEntityMotion.clear(slot);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
        return withStatus(jar, EntityStatus.ACTIVE);
    }

    public int geniePrivateCountdown1(int slot) {
        return geniePrivateCountdown1[slot];
    }

    public void decrementGeniePrivateCountdown1(int slot) {
        geniePrivateCountdown1[slot]--;
    }

    public int geniePrivateCountdown2(int slot) {
        return geniePrivateCountdown2[slot];
    }

    public void decrementGeniePrivateCountdown2(int slot) {
        geniePrivateCountdown2[slot]--;
    }

    public int geniePrivateState1(int slot) {
        return geniePrivateState1[slot];
    }

    public int geniePrivateState4(int slot) {
        return geniePrivateState4[slot];
    }

    public void setGenieControllerSlot(int value) {
        genieControllerSlot = value;
    }

    public void setGenieSharedBodySpeedX(int value) {
        genieSharedBodySpeedX = value;
    }

    public int genieSpeedX(int slot) {
        return genieSpeedX[slot];
    }

    public void setGenieSharedBodySpeedY(int value) {
        genieSharedBodySpeedY = value;
    }

    public int genieSpeedY(int slot) {
        return genieSpeedY[slot];
    }

    public int genieActiveState(int slot) {
        return genieActiveState[slot];
    }

    void setGenieActiveState(int slot, int value) {
        genieActiveState[slot] = value;
    }

    void setGeniePrivateState1(int slot, int value) {
        geniePrivateState1[slot] = value;
    }

    void setGeniePrivateState2(int slot, int value) {
        geniePrivateState2[slot] = value;
    }

    int geniePrivateState3(int slot) {
        return geniePrivateState3[slot];
    }

    void setGeniePrivateState3(int slot, int value) {
        geniePrivateState3[slot] = value;
    }

    void setGeniePrivateState4(int slot, int value) {
        geniePrivateState4[slot] = value;
    }

    void setGeniePrivateCountdown1(int slot, int value) {
        geniePrivateCountdown1[slot] = value;
    }

    void setGeniePrivateCountdown2(int slot, int value) {
        geniePrivateCountdown2[slot] = value;
    }

    void setGenieDirection(int slot, int value) {
        genieDirection[slot] = value;
    }

    void setGenieDeathInitialized(int slot, boolean value) {
        genieDeathInitialized[slot] = value;
    }

    void setGenieSpeedX(int slot, int value) {
        genieSpeedX[slot] = value;
    }

    void setGenieSpeedY(int slot, int value) {
        genieSpeedY[slot] = value;
    }

    int genieSpeedZ(int slot) {
        return genieSpeedZ[slot];
    }

    void setGenieSpeedZ(int slot, int value) {
        genieSpeedZ[slot] = value;
    }

    void setGenieSpeedXAccumulator(int slot, int value) {
        genieSpeedXAccumulator[slot] = value;
    }

    void setGenieSpeedYAccumulator(int slot, int value) {
        genieSpeedYAccumulator[slot] = value;
    }

    void setGenieSpeedZAccumulator(int slot, int value) {
        genieSpeedZAccumulator[slot] = value;
    }

    int genieControllerSlot() {
        return genieControllerSlot;
    }

    void beginFrame() {
        java.util.Arrays.fill(genieImpactRegisteredThisFrame, false);
    }
}
