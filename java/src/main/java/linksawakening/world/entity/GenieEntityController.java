package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.addFallingSpeedToPosition;
import static linksawakening.world.entity.EntityMotionMath.sourceGenieSteerStep;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** Bank-$04 GenieEntityHandler jar, body, projectile and destruction sequences. */
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
    final EntitySlots slots;

    GenieEntityController(
            State encounter,
            EntityBossRewards bossRewards,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPlayerSpawns entityPlayerSpawns,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            EntitySlots slots) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
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
        this.genieJarStep.run(jarEntity, linkEntityY);
    }

    RoomEntity advanceGenieJarAttract(RoomEntity jar) {
        int slot = jar.slot();
        if (encounter.romPrivateState3[slot] == 0) {
            EnemyRecoilMotion.Vector towardCenter =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(jar.x(), jar.y(), jar.z(), 0x50, 0x48, 0x08);
            encounter.genieSpeedX[slot] = towardCenter.x() & 0xFF;
            encounter.genieSpeedY[slot] = towardCenter.y() & 0xFF;
            if ((((jar.x() - 0x50 + 0x02) & 0xFF) < 0x04) && (((jar.y() - 0x48 + 0x02) & 0xFF) < 0x04)) {
                encounter.romPrivateState3[slot] = (encounter.romPrivateState3[slot] + 1) & 0xFF;
            }
        }
        int x = addFallingSpeedToPosition(jar.x(), encounter.genieSpeedX[slot], encounter.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(jar.y(), encounter.genieSpeedY[slot], encounter.genieSpeedYAccumulator, slot);
        return withPositionAndVariant(jar, x, y, jar.spriteVariant());
    }

    public void advanceGenieAppearance(RoomEntity appearance) {
        int slot = appearance.slot();
        int countdown = this.physicsTables.readEnemyTransitionCountdown(slot);
        if ((countdown & 0x80) != 0) {
            if ((countdown & 0x7F) == 0) {
                if (this.encounter.genieControllerSlot >= 0
                        && this.encounter.genieControllerSlot < this.slots.size()
                        && this.slots.get(this.encounter.genieControllerSlot).loaded()
                        && this.slots.get(this.encounter.genieControllerSlot).type()
                                == EntityRomConstants.ENTITY_GENIE) {
                    this.encounter.genieActiveState[this.encounter.genieControllerSlot] = 3;
                }
                this.lifecycle.disableEntityWithoutPersistence(slot);
            }
            return;
        }
        if (countdown == 0) {
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown == 0x06) {
            this.spawnGenieEntity(
                    appearance.x(), Math.max(appearance.y(), 0x14), 1, 0x40, GenieMotion.INITIAL_HEALTH, 0x01, 0x8C);
        }
    }

    public void advanceGenieBody(
            RoomEntity body, int frameCounter, int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier) {
        this.genieBodyStep.run(body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
    }

    int spawnGenieAimedFireball(RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        int xOffset = (this.physicsTables.readEntityInertia(bodySlot) & 1) == 0 ? -0x0C : 0x0C;
        int fireballSlot = this.spawnGenieEntity(
                body.x() + xOffset, body.y(), 4, 0, GenieMotion.INITIAL_HEALTH, 0x12, GenieMotion.INITIAL_HITBOX_FLAGS);
        if (fireballSlot < 0) {
            return -1;
        }
        this.physicsTables.writeEntityInertia(bodySlot, (this.physicsTables.readEntityInertia(bodySlot) + 1) & 0xFF);
        this.encounter.genieSpeedZ[fireballSlot] = 0x24;
        EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x() + xOffset, body.y(), 0, linkEntityX, linkEntityY, 0x1F);
        this.encounter.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
        this.encounter.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                bodySlot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x28));
        return fireballSlot;
    }

    public void advanceGenieDestruction(RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) {
        int slot = genie.slot();
        if (!this.encounter.genieDeathInitialized[slot]) {
            this.encounter.genieDeathInitialized[slot] = true;
            this.encounter.genieActiveState[slot] = 0;
            this.bossRewards.state.writeBossAgonySfxCountdown(0x03);
            this.bossRewards.state.writeBossAgonySourceSlot(slot);
        }
        this.combatTables.writeEnemyFlashCountdown(slot, frameCounter & 0xFF);
        switch (this.encounter.genieActiveState[slot]) {
            case 0 -> {
                this.physicsTables.writeEnemyTransitionCountdown(slot, 0x40);
                this.encounter.genieActiveState[slot] = 1;
            }
            case 1 -> {
                if (this.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                    this.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
                    this.encounter.genieActiveState[slot] = 2;
                }
            }
            default -> {
                int countdown = this.physicsTables.readEnemyTransitionCountdown(slot);
                if (countdown == 0) {
                    this.bossRewards.spawnMoldormHeartContainer(genie);
                    this.lifecycle.disableEntityWithoutPersistence(slot);
                    this.events.pendingEntityEvents.add(new EntityCombatEvent(
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
                    this.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
                    this.events.pendingEntityEvents.add(new EntityCombatEvent(
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
        int combinedSpeedX = (this.encounter.genieSpeedX[slot] + this.encounter.genieSharedBodySpeedX) & 0xFF;
        int combinedSpeedY = (this.encounter.genieSpeedY[slot] + this.encounter.genieSharedBodySpeedY) & 0xFF;
        int x = addFallingSpeedToPosition(projectile.x(), combinedSpeedX, this.encounter.genieSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(projectile.y(), combinedSpeedY, this.encounter.genieSpeedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(projectile, x, y, (frameCounter >>> 3) & 1);
        if (this.encounter.genieActiveState[slot] == 0) {
            int z = addFallingSpeedToPosition(
                    moved.z(), this.encounter.genieSpeedZ[slot], this.encounter.genieSpeedZAccumulator, slot);
            this.encounter.genieSpeedZ[slot] = (this.encounter.genieSpeedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                this.encounter.genieActiveState[slot] = 1;
                this.physicsTables.writeEnemyTransitionCountdown(slot, 0x08);
                this.encounter.genieSpeedX[slot] = 0xE0;
            }
            this.slots.replace(slot, withZ(moved, z));
            return;
        }
        this.slots.replace(slot, moved);
        if (this.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
            this.lifecycle.disableEntityWithoutPersistence(slot);
        }
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
        this.slots.replace(slot, withZ(withPositionAndVariant(fireball, x, y, (frameCounter >>> 3) & 1), z));
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
        var runtime = this;
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_GENIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots.replace(
                freeSlot,
                new RoomEntity(
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
                        false));
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        this.encounter.genieActiveState[freeSlot] = 0;
        this.encounter.handlerState[freeSlot] = privateState1 & 0xFF;
        this.encounter.romPrivateState2[freeSlot] = 0;
        this.encounter.romPrivateState3[freeSlot] = 0;
        this.encounter.romPrivateState4[freeSlot] = 0;
        this.encounter.geniePrivateCountdown1[freeSlot] = 0;
        this.encounter.geniePrivateCountdown2[freeSlot] = 0;
        this.encounter.genieDirection[freeSlot] = 0;
        this.encounter.genieDeathInitialized[freeSlot] = false;
        this.encounter.genieSpeedX[freeSlot] = 0;
        this.encounter.genieSpeedY[freeSlot] = 0;
        this.encounter.genieSpeedZ[freeSlot] = 0;
        this.encounter.genieSpeedXAccumulator[freeSlot] = 0;
        this.encounter.genieSpeedYAccumulator[freeSlot] = 0;
        this.encounter.genieSpeedZAccumulator[freeSlot] = 0;
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

    RoomEntity registerGenieJarImpact(RoomEntity jar) {
        var runtime = this;
        int slot = jar.slot();
        if (this.encounter.genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        this.encounter.genieImpactRegisteredThisFrame[slot] = true;
        this.encounter.romPrivateState4[slot] = (this.encounter.romPrivateState4[slot] + 1) & 0xFF;
        runtime.combatTables.writeEnemyFlashCountdown(slot, 0x20);
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (this.encounter.romPrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        this.encounter.genieActiveState[slot] = 1;
        this.encounter.romPrivateState3[slot] = 0;
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x80);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
        runtime.liftThrow.writeThrownMotionInitialized(slot, false);
        return withStatus(jar, EntityStatus.ACTIVE);
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
        return encounter.handlerState[slot];
    }

    public int geniePrivateState4(int slot) {
        return encounter.romPrivateState4[slot];
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
        encounter.handlerState[slot] = value;
    }

    void setGeniePrivateState2(int slot, int value) {
        encounter.romPrivateState2[slot] = value;
    }

    int geniePrivateState3(int slot) {
        return encounter.romPrivateState3[slot];
    }

    void setGeniePrivateState3(int slot, int value) {
        encounter.romPrivateState3[slot] = value;
    }

    void setGeniePrivateState4(int slot, int value) {
        encounter.romPrivateState4[slot] = value;
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

    final GenieJarStep genieJarStep = new GenieJarStep(this);
    final GenieBodyStep genieBodyStep = new GenieBodyStep(this);
    /** Encounter slot state shared with slot clearing, independent of behavior dispatch. */
    static final class State {
        final int[] genieActiveState = new int[EntityRoomLoader.MAX_ENTITIES];
        // ROM wEntitiesPrivateState1Table; selects GenieState0Handler through GenieState4Handler.
        final int[] handlerState = new int[EntityRoomLoader.MAX_ENTITIES];
        // ROM wEntitiesPrivateState2Table; interpretation follows the selected Genie handler.
        final int[] romPrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
        // ROM wEntitiesPrivateState3Table; interpretation follows the selected Genie handler.
        final int[] romPrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
        // ROM wEntitiesPrivateState4Table; interpretation follows the selected Genie handler.
        final int[] romPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
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
        private int genieControllerSlot = -1;
        private int genieSharedBodySpeedX;
        private int genieSharedBodySpeedY;

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
            return handlerState[slot];
        }

        public int geniePrivateState4(int slot) {
            return romPrivateState4[slot];
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
            handlerState[slot] = value;
        }

        void setGeniePrivateState2(int slot, int value) {
            romPrivateState2[slot] = value;
        }

        int geniePrivateState3(int slot) {
            return romPrivateState3[slot];
        }

        void setGeniePrivateState3(int slot, int value) {
            romPrivateState3[slot] = value;
        }

        void setGeniePrivateState4(int slot, int value) {
            romPrivateState4[slot] = value;
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

    static final class GenieBodyStep {
        private final linksawakening.world.entity.GenieEntityController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity body;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private int slot;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity moved;
        private int direction;
        private int boundary;

        GenieBodyStep(linksawakening.world.entity.GenieEntityController owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!prepareBody()) return;
            if (!advanceDeathState()) return;
            if (!advanceAppearanceState()) return;
            if (!advanceBodyAnimation()) return;
            if (!advanceAttackClock()) return;
            if (!spawnAttackEffects()) return;
        }

        private boolean prepareBody() {

            slot = body.slot();
            if (owner.encounter.genieActiveState[slot] == 0) {
                if (owner.physicsTables.readEnemyTransitionCountdown(slot) != 0) {
                    return false;
                }
                owner.encounter.genieActiveState[slot] = 1;
                owner.encounter.geniePrivateCountdown1[slot] = 0xFF;
                boolean jarExists = owner.encounter.genieControllerSlot >= 0
                        && owner.encounter.genieControllerSlot < owner.slots.size()
                        && owner.slots.get(owner.encounter.genieControllerSlot).loaded()
                        && owner.slots.get(owner.encounter.genieControllerSlot).type()
                                == EntityRomConstants.ENTITY_GENIE;
                if (jarExists) {
                    owner.events.pendingDialogRequests.add(new DialogRequest(0, 0x52));
                    return false;
                }
                owner.encounter.genieActiveState[slot] = 4;
                owner.combatTables.writeEnemyHealth(slot, 0x08);
                owner.events.pendingDialogRequests.add(new DialogRequest(0, 0x53));
                return false;
            }
            if (owner.encounter.genieActiveState[slot] == 4) {
                owner.combatTables.writeEnemyHitboxFlags(slot, 0x0C);
                owner.physicsTables.writeEntityOptions1Override(slot, 0x81);
                if (owner.combatTables.readEnemyFlashCountdown(slot) == 0x02) {
                    owner.encounter.genieActiveState[slot] = 5;
                    owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x80);
                    return false;
                }
                int x = addFallingSpeedToPosition(
                        body.x(), owner.encounter.genieSpeedX[slot], owner.encounter.genieSpeedXAccumulator, slot);
                int y = addFallingSpeedToPosition(
                        body.y(), owner.encounter.genieSpeedY[slot], owner.encounter.genieSpeedYAccumulator, slot);
                RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
                owner.slots.replace(slot, moved);
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
                owner.physicsTables.writeEnemyPhysicsFlags(slot, 0x41);
                if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                    owner.encounter.genieActiveState[slot] = 4;
                    owner.physicsTables.writeEnemyPhysicsFlags(slot, 0x01);
                    RoomEntity resetBody = body;
                    if ((randomByteSupplier.getAsInt() & 1) == 0) {
                        resetBody = withPositionAndVariant(
                                body,
                                owner.encounter.romPrivateState4[slot],
                                owner.encounter.romPrivateState3[slot],
                                body.spriteVariant());
                        owner.slots.replace(slot, resetBody);
                    }
                    owner.encounter.genieSpeedX[slot] = 0;
                    owner.encounter.genieSpeedY[slot] = 0;
                    owner.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
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
                owner.slots.replace(slot, moved);
                owner.encounter.romPrivateState3[slot] = (0x90 - moved.y()) & 0xFF;
                owner.encounter.romPrivateState4[slot] = (0xA0 - moved.x()) & 0xFF;
                return false;
            }
            return true;
        }

        private boolean advanceAppearanceState() {

            if (owner.encounter.genieActiveState[slot] == 2) {
                if (owner.encounter.genieControllerSlot < 0
                        || owner.encounter.genieControllerSlot >= owner.slots.size()
                        || !owner.slots.get(owner.encounter.genieControllerSlot).loaded()) {
                    return false;
                }
                RoomEntity jar = owner.slots.get(owner.encounter.genieControllerSlot);
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
                owner.slots.replace(slot, moved);
                int visualY = (moved.y() - moved.z()) & 0xFF;
                if ((((moved.x() - jar.x() + 3) & 0xFF) < 6) && (((visualY - targetY + 3) & 0xFF) < 6)) {
                    owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x10);
                    owner.encounter.genieActiveState[slot] = 3;
                }
                return false;
            }
            return true;
        }

        private boolean advanceBodyAnimation() {

            if (owner.encounter.genieActiveState[slot] == 3) {
                int countdown = owner.physicsTables.readEnemyTransitionCountdown(slot);
                if (countdown == 0) {
                    owner.lifecycle.disableEntityWithoutPersistence(slot);
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
                    owner.events.pendingEntityEvents.add(new EntityCombatEvent(
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
            owner.slots.replace(slot, moved);

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
                int verticalDirection = owner.encounter.romPrivateState2[slot] & 1;
                int target = verticalDirection == 0 ? 0x08 : 0xF8;
                owner.encounter.genieSpeedY[slot] =
                        (owner.encounter.genieSpeedY[slot] + (verticalDirection == 0 ? 1 : -1)) & 0xFF;
                if (owner.encounter.genieSpeedY[slot] == target) {
                    owner.encounter.romPrivateState2[slot] ^= 1;
                }
            }

            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                int inertia = owner.physicsTables.readEntityInertia(slot) & 0xFF;
                if (inertia >= 0x08) {
                    owner.encounter.genieActiveState[slot] = 2;
                    owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x30);
                    return false;
                }
                int[] delays = {0x10, 0x14, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40};
                owner.physicsTables.writeEnemyTransitionCountdown(slot, delays[inertia]);
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
                int xOffset = (owner.physicsTables.readEntityInertia(slot) & 1) == 0 ? -0x0C : 0x0C;
                int fireballSlot = owner.spawnGenieEntity(
                        moved.x() + xOffset,
                        moved.y(),
                        4,
                        0,
                        GenieMotion.INITIAL_HEALTH,
                        0x12,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                if (fireballSlot >= 0) {
                    owner.physicsTables.writeEntityInertia(
                            slot, (owner.physicsTables.readEntityInertia(slot) + 1) & 0xFF);
                    owner.encounter.genieSpeedZ[fireballSlot] = 0x24;
                    EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                            moved.x() + xOffset, moved.y(), 0, linkEntityX, linkEntityY, 0x1F);
                    owner.encounter.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
                    owner.encounter.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
                    owner.encounter.geniePrivateCountdown2[slot] = 0x10;
                    owner.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x28));
                }
            }
            if (owner.encounter.geniePrivateCountdown2[slot] != 0) {
                owner.slots.replace(
                        slot,
                        withPositionAndVariant(
                                owner.slots.get(slot),
                                owner.slots.get(slot).x(),
                                owner.slots.get(slot).y(),
                                2 + (owner.physicsTables.readEntityInertia(slot) & 1)));
            }
            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.RoomEntity body,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                java.util.function.IntSupplier randomByteSupplier) {
            if (inUse) {
                new GenieBodyStep(owner).run(body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
                return;
            }
            inUse = true;

            slot = 0;
            moved = null;
            direction = 0;
            boundary = 0;

            this.body = body;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.randomByteSupplier = randomByteSupplier;
            try {
                run();
            } finally {
                inUse = false;

                this.body = null;
                this.randomByteSupplier = null;

                this.moved = null;
            }
        }
    }

    static final class GenieJarStep {
        private final linksawakening.world.entity.GenieEntityController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity jarEntity;
        private int linkEntityY;
        private int slot;
        private int freeSlots;
        private boolean bodySpawnSucceeded;
        private boolean rockSpawnSucceeded;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.GenieMotion.Update update;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.GenieMotion.JarState jar;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity fallingJar;
        private int z;
        private boolean landed;
        private boolean activatedThisTick;

        GenieJarStep(linksawakening.world.entity.GenieEntityController owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!advanceJarPhysics()) return;
            if (!advanceJarAttract()) return;
            if (!emitSpawnEffects()) return;
        }

        private boolean advanceJarPhysics() {

            slot = jarEntity.slot();
            freeSlots = 0;
            for (int candidateSlotIndex = 0; candidateSlotIndex < owner.slots.size(); candidateSlotIndex++) {
                RoomEntity candidate = owner.slots.get(candidateSlotIndex);

                if (!candidate.loaded()) {
                    freeSlots++;
                }
            }
            bodySpawnSucceeded = freeSlots >= 1;
            rockSpawnSucceeded = freeSlots >= 2;
            update = owner.motions
                    .get(GenieMotion.class, GenieMotion::new)
                    .advanceState0(
                            slot,
                            jarEntity.x(),
                            jarEntity.y(),
                            jarEntity.z(),
                            owner.encounter.romPrivateState4[slot],
                            bodySpawnSucceeded,
                            rockSpawnSucceeded);
            jar = update.jar();
            owner.encounter.handlerState[slot] = jar.privateState1();
            owner.combatTables.writeEnemyHealth(slot, jar.health());
            owner.physicsTables.writeEnemyPhysicsFlags(slot, jar.physicsFlags());
            owner.combatTables.writeEnemyHitboxFlags(slot, jar.hitboxFlags());

            fallingJar = owner.slots.get(slot);
            z = addFallingSpeedToPosition(
                    fallingJar.z(), owner.encounter.genieSpeedZ[slot], owner.encounter.genieSpeedZAccumulator, slot);
            owner.encounter.genieSpeedZ[slot] = (owner.encounter.genieSpeedZ[slot] - 2) & 0xFF;
            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                owner.encounter.genieSpeedZ[slot] = 0;
            }
            owner.slots.replace(slot, withZ(fallingJar, z));

            activatedThisTick = false;
            if (update.bodySpawn() == null
                    && owner.encounter.romPrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                    && owner.encounter.genieActiveState[slot] == 0
                    && (linkEntityY & 0xFF) < 0x70) {
                owner.encounter.genieActiveState[slot] = 1;
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
                activatedThisTick = true;
            }
            return true;
        }

        private boolean advanceJarAttract() {

            if (update.bodySpawn() == null
                    && owner.encounter.romPrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                    && owner.encounter.genieActiveState[slot] == 1
                    && !activatedThisTick) {
                if (landed && owner.encounter.romPrivateState3[slot] == 0) {
                    owner.encounter.genieSpeedZ[slot] = 0x10;
                    owner.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x20));
                }
                RoomEntity movedJar = owner.advanceGenieJarAttract(owner.slots.get(slot));
                owner.slots.replace(slot, movedJar);
                if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 1) {
                    owner.spawnGenieEntity(
                            movedJar.x(),
                            movedJar.y() - 0x26,
                            2,
                            0x47,
                            GenieMotion.INITIAL_HEALTH,
                            GenieMotion.INITIAL_PHYSICS_FLAGS,
                            GenieMotion.INITIAL_HITBOX_FLAGS);
                    owner.events.pendingEntityEvents.add(new EntityCombatEvent(
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
                owner.entityPlayerSpawns.spawnLiftableRockAnimation(
                        rock.x(), rock.y(), rock.spriteVariant(), rock.privateCountdown1(), rock.physicsFlags(), -1);
            }
            if (update.noiseSfx() >= 0) {
                owner.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_GENIE,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        update.noiseSfx()));
            }
            if (update.sourceUnloaded()) {
                owner.lifecycle.disableEntityWithoutPersistence(slot);
            }
            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.RoomEntity jarEntity, int linkEntityY) {
            if (inUse) {
                new GenieJarStep(owner).run(jarEntity, linkEntityY);
                return;
            }
            inUse = true;

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

            this.jarEntity = jarEntity;
            this.linkEntityY = linkEntityY;
            try {
                run();
            } finally {
                inUse = false;

                this.jarEntity = null;

                this.update = null;
                this.jar = null;
                this.fallingJar = null;
            }
        }
    }
}
