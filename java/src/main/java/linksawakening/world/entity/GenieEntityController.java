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
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** ROM Genie encounter phases, sharing the owning room slot table. */
public final class GenieEntityController {
    final State encounter;
    final EntityBossRewards bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPlayerSpawns entityPlayerSpawns;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    final RoomEntity[] slots;

    GenieEntityController(
            State encounter,
            EntityBossRewards bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPlayerSpawns entityPlayerSpawns,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RoomEntity[] slots) {
        this.encounter = java.util.Objects.requireNonNull(encounter, "encounter");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.entityPlayerSpawns = java.util.Objects.requireNonNull(entityPlayerSpawns, "entityPlayerSpawns");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
    }

    public void advanceGenieJar(RoomEntity jarEntity, int linkEntityY) {
        GenieEntityController.genieJarBehaviorAdvanceGenieJar(this, jarEntity, linkEntityY);
    }

    RoomEntity advanceGenieJarAttract(RoomEntity jar) {
        int slot = jar.slot();
        if (encounter.geniePrivateState3[slot] == 0) {
            EnemyRecoilMotion.Vector towardCenter =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(jar.x(), jar.y(), jar.z(), 0x50, 0x48, 0x08);
            encounter.genieSpeedX[slot] = towardCenter.x() & 0xFF;
            encounter.genieSpeedY[slot] = towardCenter.y() & 0xFF;
            if ((((jar.x() - 0x50 + 0x02) & 0xFF) < 0x04) && (((jar.y() - 0x48 + 0x02) & 0xFF) < 0x04)) {
                encounter.geniePrivateState3[slot] = (encounter.geniePrivateState3[slot] + 1) & 0xFF;
            }
        }
        int x = addFallingSpeedToPosition(jar.x(), encounter.genieSpeedX[slot], encounter.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(jar.y(), encounter.genieSpeedY[slot], encounter.genieSpeedYAccumulator, slot);
        return withPositionAndVariant(jar, x, y, jar.spriteVariant());
    }

    public void advanceGenieAppearance(RoomEntity appearance) {
        GenieEntityController.genieBodyBehaviorAdvanceGenieAppearance(this, appearance);
    }

    public void advanceGenieBody(
            RoomEntity body, int frameCounter, int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier) {
        GenieEntityController.genieBodyBehaviorAdvanceGenieBody(
                this, body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
    }

    int spawnGenieAimedFireball(RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        return GenieEntityController.genieBodyBehaviorSpawnGenieAimedFireball(
                this, body, bodySlot, linkEntityX, linkEntityY);
    }

    public void advanceGenieDestruction(RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        GenieEntityController.genieProjectileBehaviorAdvanceGenieDestruction(
                this, genie, frameCounter, randomByteSupplier);
    }

    public void advanceGenieSideProjectile(RoomEntity projectile, int frameCounter) {
        GenieEntityController.genieProjectileBehaviorAdvanceGenieSideProjectile(this, projectile, frameCounter);
    }

    public void advanceGenieFireball(RoomEntity fireball, int frameCounter) {
        int slot = fireball.slot();
        int x = addFallingSpeedToPosition(
                fireball.x(), encounter.genieSpeedX[slot], encounter.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(
                fireball.y(), encounter.genieSpeedY[slot], encounter.genieSpeedYAccumulator, slot);
        int z = addFallingSpeedToPosition(
                fireball.z(), encounter.genieSpeedZ[slot], encounter.genieSpeedZAccumulator, slot);
        encounter.genieSpeedZ[slot] = (encounter.genieSpeedZ[slot] - 2) & 0xFF;
        if ((z & 0x80) != 0) {
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        this.slots[slot] = withZ(withPositionAndVariant(fireball, x, y, (frameCounter >>> 3) & 1), z);
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
        return GenieEntityController.genieProjectileBehaviorSpawnGenieEntity(
                this, x, y, privateState1, transitionCountdown, health, physicsFlags, hitboxFlags);
    }

    RoomEntity registerGenieJarImpact(RoomEntity jar) {
        return GenieEntityController.genieJarBehaviorRegisterGenieJarImpact(this, jar);
    }

    public int geniePrivateCountdown1(int slot) {
        return encounter.geniePrivateCountdown1[slot];
    }

    public void decrementGeniePrivateCountdown1(int slot) {
        encounter.geniePrivateCountdown1[slot]--;
    }

    public int geniePrivateCountdown2(int slot) {
        return encounter.geniePrivateCountdown2[slot];
    }

    public void decrementGeniePrivateCountdown2(int slot) {
        encounter.geniePrivateCountdown2[slot]--;
    }

    public int geniePrivateState1(int slot) {
        return encounter.geniePrivateState1[slot];
    }

    public int geniePrivateState4(int slot) {
        return encounter.geniePrivateState4[slot];
    }

    public void setGenieControllerSlot(int value) {
        encounter.genieControllerSlot = value;
    }

    public void setGenieSharedBodySpeedX(int value) {
        encounter.genieSharedBodySpeedX = value;
    }

    public int genieSpeedX(int slot) {
        return encounter.genieSpeedX[slot];
    }

    public void setGenieSharedBodySpeedY(int value) {
        encounter.genieSharedBodySpeedY = value;
    }

    public int genieSpeedY(int slot) {
        return encounter.genieSpeedY[slot];
    }

    public int genieActiveState(int slot) {
        return encounter.genieActiveState[slot];
    }

    void setGenieActiveState(int slot, int value) {
        encounter.genieActiveState[slot] = value;
    }

    void setGeniePrivateState1(int slot, int value) {
        encounter.geniePrivateState1[slot] = value;
    }

    void setGeniePrivateState2(int slot, int value) {
        encounter.geniePrivateState2[slot] = value;
    }

    int geniePrivateState3(int slot) {
        return encounter.geniePrivateState3[slot];
    }

    void setGeniePrivateState3(int slot, int value) {
        encounter.geniePrivateState3[slot] = value;
    }

    void setGeniePrivateState4(int slot, int value) {
        encounter.geniePrivateState4[slot] = value;
    }

    void setGeniePrivateCountdown1(int slot, int value) {
        encounter.geniePrivateCountdown1[slot] = value;
    }

    void setGeniePrivateCountdown2(int slot, int value) {
        encounter.geniePrivateCountdown2[slot] = value;
    }

    void setGenieDirection(int slot, int value) {
        encounter.genieDirection[slot] = value;
    }

    void setGenieDeathInitialized(int slot, boolean value) {
        encounter.genieDeathInitialized[slot] = value;
    }

    void setGenieSpeedX(int slot, int value) {
        encounter.genieSpeedX[slot] = value;
    }

    void setGenieSpeedY(int slot, int value) {
        encounter.genieSpeedY[slot] = value;
    }

    int genieSpeedZ(int slot) {
        return encounter.genieSpeedZ[slot];
    }

    void setGenieSpeedZ(int slot, int value) {
        encounter.genieSpeedZ[slot] = value;
    }

    void setGenieSpeedXAccumulator(int slot, int value) {
        encounter.genieSpeedXAccumulator[slot] = value;
    }

    void setGenieSpeedYAccumulator(int slot, int value) {
        encounter.genieSpeedYAccumulator[slot] = value;
    }

    void setGenieSpeedZAccumulator(int slot, int value) {
        encounter.genieSpeedZAccumulator[slot] = value;
    }

    int genieControllerSlot() {
        return encounter.genieControllerSlot;
    }

    void beginFrame() {
        java.util.Arrays.fill(encounter.genieImpactRegisteredThisFrame, false);
    }

    final GenieJarStep genieJarStep = new GenieJarStep();
    final GenieBodyStep genieBodyStep = new GenieBodyStep();
    /** Encounter slot state shared with slot clearing, independent of behavior dispatch. */
    static final class State {
        final int[] genieActiveState = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] geniePrivateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] geniePrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] geniePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] geniePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] geniePrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] geniePrivateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieDirection = new int[EntityRoomLoader.MAX_ENTITIES];
        final boolean[] genieDeathInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
        final int[] genieSpeedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
        final boolean[] genieImpactRegisteredThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
        int genieControllerSlot = -1;
        int genieSharedBodySpeedX;
        int genieSharedBodySpeedY;

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

    static void genieBodyBehaviorAdvanceGenieAppearance(GenieEntityController owner, RoomEntity appearance) {
        var runtime = owner;
        int slot = appearance.slot();
        int countdown = runtime.physicsTables.readEnemyTransitionCountdown(slot);
        if ((countdown & 0x80) != 0) {
            if ((countdown & 0x7F) == 0) {
                if (owner.encounter.genieControllerSlot >= 0
                        && owner.encounter.genieControllerSlot < runtime.slots.length
                        && runtime.slots[owner.encounter.genieControllerSlot].loaded()
                        && runtime.slots[owner.encounter.genieControllerSlot].type()
                                == EntityRomConstants.ENTITY_GENIE) {
                    owner.encounter.genieActiveState[owner.encounter.genieControllerSlot] = 3;
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
            owner.spawnGenieEntity(
                    appearance.x(), Math.max(appearance.y(), 0x14), 1, 0x40, GenieMotion.INITIAL_HEALTH, 0x01, 0x8C);
        }
    }

    static void genieBodyBehaviorAdvanceGenieBody(
            GenieEntityController owner,
            RoomEntity body,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier) {
        owner.genieBodyStep.run(owner, body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
    }

    static int genieBodyBehaviorSpawnGenieAimedFireball(
            GenieEntityController owner, RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        var runtime = owner;
        int xOffset = (runtime.physicsTables.readEntityInertia(bodySlot) & 1) == 0 ? -0x0C : 0x0C;
        int fireballSlot = owner.spawnGenieEntity(
                body.x() + xOffset, body.y(), 4, 0, GenieMotion.INITIAL_HEALTH, 0x12, GenieMotion.INITIAL_HITBOX_FLAGS);
        if (fireballSlot < 0) {
            return -1;
        }
        runtime.physicsTables.writeEntityInertia(
                bodySlot, (runtime.physicsTables.readEntityInertia(bodySlot) + 1) & 0xFF);
        owner.encounter.genieSpeedZ[fireballSlot] = 0x24;
        EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x() + xOffset, body.y(), 0, linkEntityX, linkEntityY, 0x1F);
        owner.encounter.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
        owner.encounter.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                bodySlot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x28));
        return fireballSlot;
    }

    static void genieJarBehaviorAdvanceGenieJar(GenieEntityController owner, RoomEntity jarEntity, int linkEntityY) {
        owner.genieJarStep.run(owner, jarEntity, linkEntityY);
    }

    static RoomEntity genieJarBehaviorRegisterGenieJarImpact(GenieEntityController owner, RoomEntity jar) {
        var runtime = owner;
        int slot = jar.slot();
        if (owner.encounter.genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        owner.encounter.genieImpactRegisteredThisFrame[slot] = true;
        owner.encounter.geniePrivateState4[slot] = (owner.encounter.geniePrivateState4[slot] + 1) & 0xFF;
        runtime.combatTables.writeEnemyFlashCountdown(slot, 0x20);
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (owner.encounter.geniePrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        owner.encounter.genieActiveState[slot] = 1;
        owner.encounter.geniePrivateState3[slot] = 0;
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x80);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
        runtime.liftThrow.writeThrownMotionInitialized(slot, false);
        return withStatus(jar, EntityStatus.ACTIVE);
    }

    static void genieProjectileBehaviorAdvanceGenieDestruction(
            GenieEntityController owner, RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        var runtime = owner;
        int slot = genie.slot();
        if (!owner.encounter.genieDeathInitialized[slot]) {
            owner.encounter.genieDeathInitialized[slot] = true;
            owner.encounter.genieActiveState[slot] = 0;
            runtime.bossRewards.state.writeBossAgonySfxCountdown(0x03);
            runtime.bossRewards.state.writeBossAgonySourceSlot(slot);
        }
        runtime.combatTables.writeEnemyFlashCountdown(slot, frameCounter & 0xFF);
        switch (owner.encounter.genieActiveState[slot]) {
            case 0 -> {
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x40);
                owner.encounter.genieActiveState[slot] = 1;
            }
            case 1 -> {
                if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
                    owner.encounter.genieActiveState[slot] = 2;
                }
            }
            default -> {
                int countdown = runtime.physicsTables.readEnemyTransitionCountdown(slot);
                if (countdown == 0) {
                    runtime.bossRewards.spawnMoldormHeartContainer(genie);
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
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
                    runtime.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
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

    static void genieProjectileBehaviorAdvanceGenieSideProjectile(
            GenieEntityController owner, RoomEntity projectile, int frameCounter) {
        var runtime = owner;
        int slot = projectile.slot();
        int combinedSpeedX = (owner.encounter.genieSpeedX[slot] + owner.encounter.genieSharedBodySpeedX) & 0xFF;
        int combinedSpeedY = (owner.encounter.genieSpeedY[slot] + owner.encounter.genieSharedBodySpeedY) & 0xFF;
        int x = addFallingSpeedToPosition(projectile.x(), combinedSpeedX, owner.encounter.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(projectile.y(), combinedSpeedY, owner.encounter.genieSpeedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(projectile, x, y, (frameCounter >>> 3) & 1);
        if (owner.encounter.genieActiveState[slot] == 0) {
            int z = addFallingSpeedToPosition(
                    moved.z(), owner.encounter.genieSpeedZ[slot], owner.encounter.genieSpeedZAccumulator, slot);
            owner.encounter.genieSpeedZ[slot] = (owner.encounter.genieSpeedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                owner.encounter.genieActiveState[slot] = 1;
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x08);
                owner.encounter.genieSpeedX[slot] = 0xE0;
            }
            runtime.slots[slot] = withZ(moved, z);
            return;
        }
        runtime.slots[slot] = moved;
        if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
        }
    }

    static int genieProjectileBehaviorSpawnGenieEntity(
            GenieEntityController owner,
            int x,
            int y,
            int privateState1,
            int transitionCountdown,
            int health,
            int physicsFlags,
            int hitboxFlags) {
        var runtime = owner;
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
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
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        owner.encounter.genieActiveState[freeSlot] = 0;
        owner.encounter.geniePrivateState1[freeSlot] = privateState1 & 0xFF;
        owner.encounter.geniePrivateState2[freeSlot] = 0;
        owner.encounter.geniePrivateState3[freeSlot] = 0;
        owner.encounter.geniePrivateState4[freeSlot] = 0;
        owner.encounter.geniePrivateCountdown1[freeSlot] = 0;
        owner.encounter.geniePrivateCountdown2[freeSlot] = 0;
        owner.encounter.genieDirection[freeSlot] = 0;
        owner.encounter.genieDeathInitialized[freeSlot] = false;
        owner.encounter.genieSpeedX[freeSlot] = 0;
        owner.encounter.genieSpeedY[freeSlot] = 0;
        owner.encounter.genieSpeedZ[freeSlot] = 0;
        owner.encounter.genieSpeedXAccumulator[freeSlot] = 0;
        owner.encounter.genieSpeedYAccumulator[freeSlot] = 0;
        owner.encounter.genieSpeedZAccumulator[freeSlot] = 0;
        runtime.motions.get(GenieMotion.class, GenieMotion::new).clear(freeSlot);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, transitionCountdown & 0xFF);
        runtime.combatTables.writeEnemyHealth(freeSlot, health & 0xFF);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, physicsFlags & 0xFF);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, hitboxFlags & 0xFF);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.GENIE_OPTIONS1);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static final class GenieBodyStep {
        private linksawakening.world.entity.GenieEntityController owner;
        private linksawakening.world.entity.RoomEntity body;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.GenieEntityController runtime;
        private int slot;
        private linksawakening.world.entity.RoomEntity moved;
        private int direction;
        private int boundary;

        GenieBodyStep() {}

        void run() {
            if (!prepareBody()) return;
            if (!advanceDeathState()) return;
            if (!advanceAppearanceState()) return;
            if (!advanceBodyAnimation()) return;
            if (!advanceAttackClock()) return;
            if (!spawnAttackEffects()) return;
        }

        private boolean prepareBody() {

            runtime = owner;
            slot = body.slot();
            if (owner.encounter.genieActiveState[slot] == 0) {
                if (runtime.physicsTables.readEnemyTransitionCountdown(slot) != 0) {
                    return false;
                }
                owner.encounter.genieActiveState[slot] = 1;
                owner.encounter.geniePrivateCountdown1[slot] = 0xFF;
                boolean jarExists = owner.encounter.genieControllerSlot >= 0
                        && owner.encounter.genieControllerSlot < runtime.slots.length
                        && runtime.slots[owner.encounter.genieControllerSlot].loaded()
                        && runtime.slots[owner.encounter.genieControllerSlot].type() == EntityRomConstants.ENTITY_GENIE;
                if (jarExists) {
                    runtime.events.pendingDialogRequests.add(new DialogRequest(0, 0x52));
                    return false;
                }
                owner.encounter.genieActiveState[slot] = 4;
                runtime.combatTables.writeEnemyHealth(slot, 0x08);
                runtime.events.pendingDialogRequests.add(new DialogRequest(0, 0x53));
                return false;
            }
            if (owner.encounter.genieActiveState[slot] == 4) {
                runtime.combatTables.writeEnemyHitboxFlags(slot, 0x0C);
                runtime.physicsTables.writeEntityOptions1Override(slot, 0x81);
                if (runtime.combatTables.readEnemyFlashCountdown(slot) == 0x02) {
                    owner.encounter.genieActiveState[slot] = 5;
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x80);
                    return false;
                }
                int x = addFallingSpeedToPosition(
                        body.x(), owner.encounter.genieSpeedX[slot], owner.encounter.genieSpeedXAccumulator, slot);
                int y = addFallingSpeedToPosition(
                        body.y(), owner.encounter.genieSpeedY[slot], owner.encounter.genieSpeedYAccumulator, slot);
                RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
                runtime.slots[slot] = moved;
                if ((frameCounter & 0x03) == 0) {
                    EnemyRecoilMotion.Vector desired = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                            moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x10);
                    owner.encounter.genieSpeedX[slot] =
                            sourceGenieSteerStep(owner.encounter.genieSpeedX[slot], desired.x());
                    owner.encounter.genieSpeedY[slot] =
                            sourceGenieSteerStep(owner.encounter.genieSpeedY[slot], desired.y());
                }
                return false;
            }
            return true;
        }

        private boolean advanceDeathState() {

            if (owner.encounter.genieActiveState[slot] == 5) {
                runtime.physicsTables.writeEnemyPhysicsFlags(slot, 0x41);
                if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                    owner.encounter.genieActiveState[slot] = 4;
                    runtime.physicsTables.writeEnemyPhysicsFlags(slot, 0x01);
                    RoomEntity resetBody = body;
                    if ((randomByteSupplier.getAsInt() & 1) == 0) {
                        resetBody = withPositionAndVariant(
                                body,
                                owner.encounter.geniePrivateState4[slot],
                                owner.encounter.geniePrivateState3[slot],
                                body.spriteVariant());
                        runtime.slots[slot] = resetBody;
                    }
                    owner.encounter.genieSpeedX[slot] = 0;
                    owner.encounter.genieSpeedY[slot] = 0;
                    runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
                    owner.spawnGenieAimedFireball(resetBody, slot, linkEntityX, linkEntityY);
                    return false;
                }
                EnemyRecoilMotion.Vector centerVector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        body.x(), body.y(), body.z(), 0x50, 0x48, 0x20);
                EnemyRecoilMotion.Vector linkVector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        body.x(), body.y(), body.z(), linkEntityX, linkEntityY, 0x04);
                owner.encounter.genieSpeedX[slot] = (-centerVector.x() + linkVector.x()) & 0xFF;
                owner.encounter.genieSpeedY[slot] = (centerVector.y() + linkVector.y()) & 0xFF;
                int x = addFallingSpeedToPosition(
                        body.x(), owner.encounter.genieSpeedX[slot], owner.encounter.genieSpeedXAccumulator, slot);
                int y = addFallingSpeedToPosition(
                        body.y(), owner.encounter.genieSpeedY[slot], owner.encounter.genieSpeedYAccumulator, slot);
                RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
                runtime.slots[slot] = moved;
                owner.encounter.geniePrivateState3[slot] = (0x90 - moved.y()) & 0xFF;
                owner.encounter.geniePrivateState4[slot] = (0xA0 - moved.x()) & 0xFF;
                return false;
            }
            return true;
        }

        private boolean advanceAppearanceState() {

            if (owner.encounter.genieActiveState[slot] == 2) {
                if (owner.encounter.genieControllerSlot < 0
                        || owner.encounter.genieControllerSlot >= runtime.slots.length
                        || !runtime.slots[owner.encounter.genieControllerSlot].loaded()) {
                    return false;
                }
                RoomEntity jar = runtime.slots[owner.encounter.genieControllerSlot];
                int targetY = (jar.y() - 0x20) & 0xFF;
                EnemyRecoilMotion.Vector towardJar = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        body.x(), body.y(), body.z(), jar.x(), targetY, 0x10);
                owner.encounter.genieSpeedX[slot] = towardJar.x() & 0xFF;
                owner.encounter.genieSpeedY[slot] = towardJar.y() & 0xFF;
                int x = addFallingSpeedToPosition(
                        body.x(), owner.encounter.genieSpeedX[slot], owner.encounter.genieSpeedXAccumulator, slot);
                int y = addFallingSpeedToPosition(
                        body.y(), owner.encounter.genieSpeedY[slot], owner.encounter.genieSpeedYAccumulator, slot);
                RoomEntity moved = withPositionAndVariant(body, x, y, body.spriteVariant());
                runtime.slots[slot] = moved;
                int visualY = (moved.y() - moved.z()) & 0xFF;
                if ((((moved.x() - jar.x() + 3) & 0xFF) < 6) && (((visualY - targetY + 3) & 0xFF) < 6)) {
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
                    owner.encounter.genieActiveState[slot] = 3;
                }
                return false;
            }
            return true;
        }

        private boolean advanceBodyAnimation() {

            if (owner.encounter.genieActiveState[slot] == 3) {
                int countdown = runtime.physicsTables.readEnemyTransitionCountdown(slot);
                if (countdown == 0) {
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                if (countdown == 0x04) {
                    owner.spawnGenieEntity(
                            body.x(),
                            body.y(),
                            2,
                            0xC7,
                            GenieMotion.INITIAL_HEALTH,
                            GenieMotion.INITIAL_PHYSICS_FLAGS,
                            GenieMotion.INITIAL_HITBOX_FLAGS);
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x1F));
                }
                return false;
            }
            if (owner.encounter.genieActiveState[slot] != 1) {
                return false;
            }
            moved = withPositionAndVariant(
                    body,
                    addFallingSpeedToPosition(
                            body.x(), owner.encounter.genieSpeedX[slot], owner.encounter.genieSpeedXAccumulator, slot),
                    addFallingSpeedToPosition(
                            body.y(), owner.encounter.genieSpeedY[slot], owner.encounter.genieSpeedYAccumulator, slot),
                    (frameCounter >>> 4) & 1);
            runtime.slots[slot] = moved;

            direction = owner.encounter.genieDirection[slot] & 1;
            if ((frameCounter & 0x07) == 0) {
                int target = direction == 0 ? 0x08 : 0xF8;
                if (owner.encounter.genieSpeedX[slot] != target) {
                    owner.encounter.genieSpeedX[slot] =
                            (owner.encounter.genieSpeedX[slot] + (direction == 0 ? 1 : -1)) & 0xFF;
                }
            }
            boundary = direction == 0 ? 0x60 : 0x40;
            if (moved.x() == boundary) {
                owner.encounter.genieDirection[slot] ^= 1;
            }
            return true;
        }

        private boolean advanceAttackClock() {

            if ((frameCounter & 1) == 0) {
                int verticalDirection = owner.encounter.geniePrivateState2[slot] & 1;
                int target = verticalDirection == 0 ? 0x08 : 0xF8;
                owner.encounter.genieSpeedY[slot] =
                        (owner.encounter.genieSpeedY[slot] + (verticalDirection == 0 ? 1 : -1)) & 0xFF;
                if (owner.encounter.genieSpeedY[slot] == target) {
                    owner.encounter.geniePrivateState2[slot] ^= 1;
                }
            }

            if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                int inertia = runtime.physicsTables.readEntityInertia(slot) & 0xFF;
                if (inertia >= 0x08) {
                    owner.encounter.genieActiveState[slot] = 2;
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x30);
                    return false;
                }
                int[] delays = {0x10, 0x14, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40};
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, delays[inertia]);
                int projectileSlot = owner.spawnGenieEntity(
                        moved.x() - 0x0C,
                        moved.y() - 0x14,
                        3,
                        0,
                        GenieMotion.INITIAL_HEALTH,
                        0x42,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                if (projectileSlot >= 0) {
                    owner.encounter.genieSpeedZ[projectileSlot] = 0x20;
                    owner.encounter.genieSpeedX[projectileSlot] = 0x0C;
                }
            }
            return true;
        }

        private boolean spawnAttackEffects() {

            if (owner.encounter.geniePrivateCountdown1[slot] == 0) {
                owner.encounter.geniePrivateCountdown1[slot] = 0x30;
                int xOffset = (runtime.physicsTables.readEntityInertia(slot) & 1) == 0 ? -0x0C : 0x0C;
                int fireballSlot = owner.spawnGenieEntity(
                        moved.x() + xOffset,
                        moved.y(),
                        4,
                        0,
                        GenieMotion.INITIAL_HEALTH,
                        0x12,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                if (fireballSlot >= 0) {
                    runtime.physicsTables.writeEntityInertia(
                            slot, (runtime.physicsTables.readEntityInertia(slot) + 1) & 0xFF);
                    owner.encounter.genieSpeedZ[fireballSlot] = 0x24;
                    EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                            moved.x() + xOffset, moved.y(), 0, linkEntityX, linkEntityY, 0x1F);
                    owner.encounter.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
                    owner.encounter.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
                    owner.encounter.geniePrivateCountdown2[slot] = 0x10;
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x28));
                }
            }
            if (owner.encounter.geniePrivateCountdown2[slot] != 0) {
                runtime.slots[slot] = withPositionAndVariant(
                        runtime.slots[slot],
                        runtime.slots[slot].x(),
                        runtime.slots[slot].y(),
                        2 + (runtime.physicsTables.readEntityInertia(slot) & 1));
            }
            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.GenieEntityController owner,
                linksawakening.world.entity.RoomEntity body,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                java.util.function.IntSupplier randomByteSupplier) {
            if (inUse) {
                new GenieBodyStep().run(owner, body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
                return;
            }
            inUse = true;
            runtime = null;
            slot = 0;
            moved = null;
            direction = 0;
            boundary = 0;
            this.owner = owner;
            this.body = body;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.randomByteSupplier = randomByteSupplier;
            try {
                run();
            } finally {
                inUse = false;
                this.owner = null;
                this.body = null;
                this.randomByteSupplier = null;
                this.runtime = null;
                this.moved = null;
            }
        }
    }

    static final class GenieJarStep {
        private linksawakening.world.entity.GenieEntityController owner;
        private linksawakening.world.entity.RoomEntity jarEntity;
        private int linkEntityY;
        private linksawakening.world.entity.GenieEntityController runtime;
        private int slot;
        private int freeSlots;
        private boolean bodySpawnSucceeded;
        private boolean rockSpawnSucceeded;
        private linksawakening.world.entity.motion.GenieMotion.Update update;
        private linksawakening.world.entity.motion.GenieMotion.JarState jar;
        private linksawakening.world.entity.RoomEntity fallingJar;
        private int z;
        private boolean landed;
        private boolean activatedThisTick;

        GenieJarStep() {}

        void run() {
            if (!advanceJarPhysics()) return;
            if (!advanceJarAttract()) return;
            if (!emitSpawnEffects()) return;
        }

        private boolean advanceJarPhysics() {

            runtime = owner;
            slot = jarEntity.slot();
            freeSlots = 0;
            for (RoomEntity candidate : runtime.slots) {
                if (!candidate.loaded()) {
                    freeSlots++;
                }
            }
            bodySpawnSucceeded = freeSlots >= 1;
            rockSpawnSucceeded = freeSlots >= 2;
            update = runtime.motions
                    .get(GenieMotion.class, GenieMotion::new)
                    .advanceState0(
                            slot,
                            jarEntity.x(),
                            jarEntity.y(),
                            jarEntity.z(),
                            owner.encounter.geniePrivateState4[slot],
                            bodySpawnSucceeded,
                            rockSpawnSucceeded);
            jar = update.jar();
            owner.encounter.geniePrivateState1[slot] = jar.privateState1();
            runtime.combatTables.writeEnemyHealth(slot, jar.health());
            runtime.physicsTables.writeEnemyPhysicsFlags(slot, jar.physicsFlags());
            runtime.combatTables.writeEnemyHitboxFlags(slot, jar.hitboxFlags());

            fallingJar = runtime.slots[slot];
            z = addFallingSpeedToPosition(
                    fallingJar.z(), owner.encounter.genieSpeedZ[slot], owner.encounter.genieSpeedZAccumulator, slot);
            owner.encounter.genieSpeedZ[slot] = (owner.encounter.genieSpeedZ[slot] - 2) & 0xFF;
            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                owner.encounter.genieSpeedZ[slot] = 0;
            }
            runtime.slots[slot] = withZ(fallingJar, z);

            activatedThisTick = false;
            if (update.bodySpawn() == null
                    && owner.encounter.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                    && owner.encounter.genieActiveState[slot] == 0
                    && (linkEntityY & 0xFF) < 0x70) {
                owner.encounter.genieActiveState[slot] = 1;
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
                activatedThisTick = true;
            }
            return true;
        }

        private boolean advanceJarAttract() {

            if (update.bodySpawn() == null
                    && owner.encounter.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                    && owner.encounter.genieActiveState[slot] == 1
                    && !activatedThisTick) {
                if (landed && owner.encounter.geniePrivateState3[slot] == 0) {
                    owner.encounter.genieSpeedZ[slot] = 0x10;
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x20));
                }
                RoomEntity movedJar = owner.advanceGenieJarAttract(runtime.slots[slot]);
                runtime.slots[slot] = movedJar;
                if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 1) {
                    owner.spawnGenieEntity(
                            movedJar.x(),
                            movedJar.y() - 0x26,
                            2,
                            0x47,
                            GenieMotion.INITIAL_HEALTH,
                            GenieMotion.INITIAL_PHYSICS_FLAGS,
                            GenieMotion.INITIAL_HITBOX_FLAGS);
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x06));
                }
            }

            if (update.bodySpawn() != null) {
                owner.spawnGenieBody(update.bodySpawn());
            }
            return true;
        }

        private boolean emitSpawnEffects() {

            if (update.rockSpawn() != null && update.rockSpawnSucceeded()) {
                GenieMotion.RockSpawnRequest rock = update.rockSpawn();
                runtime.entityPlayerSpawns.spawnLiftableRockAnimation(
                        rock.x(), rock.y(), rock.spriteVariant(), rock.privateCountdown1(), rock.physicsFlags(), -1);
            }
            if (update.noiseSfx() >= 0) {
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
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
            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.GenieEntityController owner,
                linksawakening.world.entity.RoomEntity jarEntity,
                int linkEntityY) {
            if (inUse) {
                new GenieJarStep().run(owner, jarEntity, linkEntityY);
                return;
            }
            inUse = true;
            runtime = null;
            slot = 0;
            freeSlots = 0;
            bodySpawnSucceeded = false;
            rockSpawnSucceeded = false;
            update = null;
            jar = null;
            fallingJar = null;
            z = 0;
            landed = false;
            activatedThisTick = false;
            this.owner = owner;
            this.jarEntity = jarEntity;
            this.linkEntityY = linkEntityY;
            try {
                run();
            } finally {
                inUse = false;
                this.owner = null;
                this.jarEntity = null;
                this.runtime = null;
                this.update = null;
                this.jar = null;
                this.fallingJar = null;
            }
        }
    }
}
