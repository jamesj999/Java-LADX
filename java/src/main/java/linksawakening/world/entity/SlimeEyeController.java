package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withFlipAttribute;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.SlimeEyeHalfMotion;

/** Bank-$04 SlimeEyeEntityHandler and label_004_496F child spawning ($496F–$504B). */
public final class SlimeEyeController {
    private static final RomAddress CHILD_X_OFFSETS = RomSymbols.Data_004_496B; // ROM Data_004_496B
    private static final RomAddress CHILD_SPEED_X = RomSymbols.Data_004_496D; // ROM Data_004_496D
    private static final RomAddress CONTACT_HITBOXES = RomSymbols.Data_004_4B12; // ROM Data_004_4B12
    private static final RomAddress BODY_SPEED_X = RomSymbols.Data_004_4B5C; // ROM Data_004_4B5C
    private static final RomAddress BODY_SPEED_Y = RomSymbols.Data_004_4B6C; // ROM Data_004_4B6C
    private static final RomAddress HALF_SPEED_X = RomSymbols.Data_004_4E94; // ROM Data_004_4E94
    private static final RomAddress HALF_SPEED_Y = RomSymbols.Data_004_4E9C; // ROM Data_004_4E9C

    final java.util.List<RoomEntity> slotEntities;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityBossRewards bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;
    final State encounter;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private final Rom rom;
    int screenShakeCountdown;

    SlimeEyeController(
            State encounter,
            java.util.List<RoomEntity> slotEntities,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ linksawakening.entity
                            .EntitySpriteHandlerCatalog
                    spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBossRewards bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ RomEnemyCombatTables
                    enemyCombatTables,
            RoomEntity[] slots,
            /* Optional encounter tables; absent in ROM-free runtimes. */ Rom rom) {
        this.rom = rom;
        this.encounter = java.util.Objects.requireNonNull(encounter, "encounter");
        this.slotEntities = java.util.Objects.requireNonNull(slotEntities, "slotEntities");
        this.spriteHandlers = spriteHandlers;
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = enemyCombatTables;
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
    }

    public void setScreenShakeCountdown(int value) {
        screenShakeCountdown = value & 255;
    }

    int state(int slot) {
        return encounter.state[slot];
    }

    boolean half(int slot) {
        return encounter.half[slot] != 0;
    }

    int stretch(int slot) {
        return encounter.stretch[slot];
    }

    int variant(int slot) {
        return encounter.variant[slot];
    }

    int dropping(int slot) {
        return encounter.dropping[slot];
    }

    int speedZ(int slot) {
        return encounter.speedZ[slot];
    }

    void clear(int slot) {
        for (int[] table : new int[][] {
            encounter.state,
            encounter.half,
            encounter.stretch,
            encounter.moving,
            encounter.angry,
            encounter.dropping,
            encounter.privateCountdown1,
            encounter.splitting,
            encounter.variant,
            encounter.speedX,
            encounter.speedY,
            encounter.speedZ,
            encounter.accX,
            encounter.accY,
            encounter.accZ,
            encounter.swordHitFrame
        }) table[slot] = 0;
        encounter.dying[slot] = false;
    }

    int read(RomAddress table, int index) {
        if (rom == null) throw new IllegalStateException("Slime Eye requires ROM encounter tables");
        return rom.u8(table.plus(index));
    }

    public void advance(EntityFrameExecution frame) {
        try {
            advanceHandler(frame);
        } finally {
            encounter.swordHitFrame[frame.slotFrame().entity().slot()] = 0;
        }
    }

    private void advanceHandler(EntityFrameExecution frame) {
        var runtime = this;
        RoomEntity entity = frame.slotFrame().entity();
        int slot = entity.slot();
        if (entity.status() == EntityStatus.INIT) {
            this.clear(slot);
            runtime.combatTables.writeEnemyHealth(slot, 0x50);
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
            runtime.slots[slot] = withZ(withStatus(entity, EntityStatus.ACTIVE), 0x7E);
            this.render(slot);
            return;
        }
        if (entity.status() != EntityStatus.ACTIVE && entity.status() != EntityStatus.DYING) return;
        // UpdateEntityTimers precedes the handler's interactivity guards.
        if (this.encounter.privateCountdown1[slot] > 0) this.encounter.privateCountdown1[slot]--;
        if (this.encounter.splitting[slot] > 0) this.encounter.splitting[slot]--;
        if (!frame.roomInspection().roomTransitionActive()
                && !frame.roomInspection().freeMovementMode()) {
            int dx = 0, dy = 0;
            if (((frame.inputs().linkEntityX() - 0x11) & 255) >= 0x7E)
                dx = frame.inputs().linkEntityX() < 0x50 ? 1 : -1;
            else if (((frame.inputs().linkEntityY() - 0x16) & 255) >= 0x5E) dy = -1;
            if (dx != 0 || dy != 0) {
                frame.emit(new LinkMotionBlockRequest(slot, dx, dy, 1));
                frame.inputs().linkEntityX((frame.inputs().linkEntityX() + dx) & 255);
                frame.inputs().linkEntityY((frame.inputs().linkEntityY() + dy) & 255);
                frame.lifecycle().delayInteractions(2);
            }
        }
        runtime.lifecycle.decrementSlowTransitionCountdown(slot, frame.inputs().frame());
        if (!frame.link().inventoryAppearing() && !frame.roomInspection().roomTransitionActive()) {
            BossIntroMotion.Update intro = runtime.motions
                    .get(BossIntroMotion.class, BossIntroMotion::new)
                    .advance(
                            runtime.slotProperties.options1(slot),
                            entity.type(),
                            runtime.roomVariables.readEntityMapId(),
                            runtime.lifecycle.transitionSequenceCounter());
            if (intro.musicTrack() >= 0) frame.requestMusic(intro.musicTrack());
            if (intro.dialogLowId() >= 0) {
                frame.emit(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                this.render(slot);
                return;
            }
        }
        // Main's combat pass precedes tick. Restore the fresh write that the
        // common flash tick would otherwise age before the ROM collision site.
        if (this.hitThisFrame(slot, frame.inputs().frame())) runtime.combatTables.writeEnemyFlashCountdown(slot, 0x18);
        // RenderActiveEntitySpritesRect runs before this frame's state changes.
        this.render(slot);
        if (runtime.physicsTables.readSlowTransitionCountdown(slot) != 0) {
            frame.emit(new LinkMotionBlockRequest(slot));
            frame.emit(new LinkFallenPoseRequest(slot));
        }
        if (entity.status() == EntityStatus.DYING) {
            // The lethal collision occurs inside the active handler in ROM.
            // Its destruction handler starts on the following slot visit.
            if (this.hitThisFrame(slot, frame.inputs().frame())) return;
            if (!this.encounter.dying[slot]) {
                this.encounter.state[slot] = 0;
                this.encounter.dying[slot] = true;
            }
            this.advanceDeath(frame);
            return;
        }
        if (this.encounter.half[slot] == 0) runtime.combatTables.writeEnemyHealth(slot, 0x50);
        if ((this.encounter.half[slot] != 0 || this.encounter.state[slot] != 0)
                && (!frame.gates().handlerInteractive() || frame.gates().countdownBlocksInteraction())) return;
        if (this.encounter.half[slot] == 0) this.advanceBody(frame);
        else this.advanceHalf(frame);
    }

    void advanceBody(EntityFrameExecution f) {
        int s = f.slotFrame().entity().slot();
        RoomEntity e = this.slots[s];
        this.combatTables.writeEnemyHealth(s, 0x50);
        switch (this.encounter.state[s]) {
            case 0 -> {
                if (this.screenShakeCountdown == 5) {
                    this.encounter.state[s] = 1;
                    this.sound(s, 8);
                }
                if (this.physicsTables.readEnemyTransitionCountdown(s) == 0) {
                    this.physicsTables.writeEnemyTransitionCountdown(s, 0x50);
                    long zols = this.slotEntities.stream()
                            .filter(x -> x.loaded() && x.type() == 0x1B)
                            .count();
                    if (zols < 2)
                        this.spawn(
                                0x1B,
                                (f.inputs().randomByteSupplier().getAsInt() & 0x3F) + 0x40,
                                (f.inputs().randomByteSupplier().getAsInt() & 0x3F) + 0x30,
                                0x70);
                }
            }
            case 1 -> {
                e = this.integrateZ(e);
                if (this.encounter.speedZ[s] != 0xA0) this.encounter.speedZ[s] = (this.encounter.speedZ[s] - 2) & 255;
                if ((e.z() & 0x80) != 0) {
                    e = withZ(e, 0);
                    f.emit(new ScreenShakeRequest(s, 0x50, 4));
                    this.physicsTables.writeEnemyTransitionCountdown(s, 0x40);
                    this.noise(s, 0x0C);
                    if (!f.inputs().linkAirborne()) this.stunLink(s, 0x14);
                    this.encounter.state[s] = 2;
                }
                this.slots[s] = e;
            }
            case 2 -> {
                if (this.physicsTables.readEnemyTransitionCountdown(s) == 0) this.encounter.state[s] = 3;
            }
            case 3 -> {
                if (this.combatTables.readEnemyIgnoreHitsCountdown(s) > 0
                        && !this.hitThisFrame(s, f.inputs().frame()))
                    this.combatTables.postEnemyIgnoreHitsCountdown(s, -1);
                if (this.encounter.splitting[s] != 0) {
                    if (this.encounter.splitting[s] == 1) {
                        this.split(e);
                        return;
                    }
                    f.emit(new LinkMotionBlockRequest(
                            s, 0, (f.inputs().frame() & 1) == 0 ? -1 : 0, this.encounter.splitting[s]));
                    this.encounter.variant[s] = 6;
                    return;
                }
                this.wander(f);
                this.encounter.variant[s] = (f.inputs().frame() >> 4) & 1;
                if (this.encounter.stretch[s] != 0) {
                    this.encounter.variant[s] = this.encounter.stretch[s] + 1;
                    if (this.encounter.privateCountdown1[s] == 0) {
                        this.encounter.privateCountdown1[s] = 0x28;
                        this.encounter.stretch[s]--;
                    }
                }
                if (this.combatTables.readEnemyFlashCountdown(s) != 0
                        && (f.inputs().frame() & 0x1F) == 0) {
                    this.encounter.privateCountdown1[s] = 0x50;
                    if (this.encounter.stretch[s] != 4) this.encounter.stretch[s]++;
                }
            }
            default -> throw new IllegalStateException("Slime Eye body state " + this.encounter.state[s]);
        }
    }

    void wander(EntityFrameExecution f) {
        int s = f.slotFrame().entity().slot(), countdown = this.physicsTables.readEnemyTransitionCountdown(s);
        RoomEntity e = this.slots[s];
        if (encounter.moving[s] == 0) {
            if (countdown != 0) return;
            this.physicsTables.writeEnemyTransitionCountdown(
                    s, (f.inputs().randomByteSupplier().getAsInt() & 0x1F) + 0x10);
            encounter.moving[s] = 1;
            int quadrant = (e.x() >= 0x50 ? 1 : 0) | (((e.y() - e.z()) & 255) >= 0x48 ? 2 : 0);
            int index = quadrant * 4 | (f.inputs().randomByteSupplier().getAsInt() & 3);
            encounter.speedX[s] = read(BODY_SPEED_X, index);
            encounter.speedY[s] = read(BODY_SPEED_Y, index);
        } else if (countdown == 0) {
            this.physicsTables.writeEnemyTransitionCountdown(s, 0x30);
            encounter.moving[s] = 0;
        } else if ((countdown & 0x0E) == 0)
            this.slots[s] = integrateXY(e, f.inputs().backgroundCollision());
    }

    void advanceHalf(EntityFrameExecution f) {
        this.slimeEyeHalfStep.run(f);
    }

    boolean hitThisFrame(int slot, int frame) {
        return encounter.swordHitFrame[slot] == (frame & 255) + 1;
    }

    void stunLink(int s, int countdown) {
        this.physicsTables.writeSlowTransitionCountdown(s, countdown);
        this.physicsTables.writeSlowTimerInitialized(s, true);
    }

    RoomEntity integrateZ(RoomEntity e) {
        int s = e.slot(), value = encounter.accZ[s] + (byte) encounter.speedZ[s];
        encounter.accZ[s] = value & 15;
        return withZ(e, (e.z() + (value >> 4)) & 255);
    }

    RoomEntity integrateXY(
            RoomEntity e, /* Optional room geometry/history; null preserves the path without that room input. */
            RoomEntityBackgroundCollision collision) {
        int s = e.slot(),
                x = encounter.accX[s] + (byte) encounter.speedX[s],
                y = encounter.accY[s] + (byte) encounter.speedY[s];
        encounter.accX[s] = x & 15;
        encounter.accY[s] = y & 15;
        int nx = (e.x() + (x >> 4)) & 255, ny = (e.y() + (y >> 4)) & 255;
        if (collision != null) {
            if (collision.blocks(e, (byte) encounter.speedX[s] < 0 ? 1 : 0, nx, e.y())) {
                nx = e.x();
                encounter.speedX[s] = (-encounter.speedX[s]) & 255;
            }
            if (collision.blocks(e, (byte) encounter.speedY[s] < 0 ? 2 : 3, nx, ny)) {
                ny = e.y();
                encounter.speedY[s] = (-encounter.speedY[s]) & 255;
            }
        }
        return withPositionAndVariant(e, nx, ny, e.spriteVariant());
    }

    int spawn(int type, int x, int y, int z) {
        int s = EntitySlotAllocator.findFreeSlot(this.slots, this.combatTables, this.slots.length - 1);
        if (s < 0) return -1;
        this.lifecycle.disableEntityWithoutPersistence(s);
        var definition = type == 0x1B && this.spriteHandlers != null
                ? this.slotProperties.spriteViews().forZolSlimeEye()
                : this.slotProperties.spriteDefinitionFor(type);
        this.slots[s] = new RoomEntity(
                s,
                -1,
                type,
                x & 255,
                y & 255,
                EntityStatus.ACTIVE,
                definition,
                definition.initialVariant(),
                0,
                0,
                z,
                -1,
                false);
        this.lifecycle.initializeHealthGroup(s);
        this.combatTables.writeEnemyHealth(s, this.slotProperties.initialHealth(type));
        this.physicsTables.writeEnemyPhysicsFlags(s, EntityTypeRules.initialPhysicsFlags(type));
        this.combatTables.writeEnemyIgnoreHitsCountdown(s, 1);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(s, true);
        return s;
    }

    void split(RoomEntity e) {
        for (int i = 0; i < 2; i++) {
            int child = spawn(EntityRomConstants.ENTITY_SLIME_EYE, e.x() + (byte) read(CHILD_X_OFFSETS, i), e.y(), 0);
            if (child < 0) continue;
            encounter.half[child] = 1;
            encounter.state[child] = 1;
            encounter.speedX[child] = read(CHILD_SPEED_X, i);
            encounter.speedZ[child] = 0x10;
            this.combatTables.writeEnemyHealth(child, 5);
            render(child);
        }
        this.lifecycle.disableEntityWithoutPersistence(e.slot());
    }

    void advanceDeath(EntityFrameExecution f) {
        int s = f.slotFrame().entity().slot();
        this.combatTables.writeEnemyFlashCountdown(s, f.inputs().frame() & 255);
        switch (this.encounter.state[s]) {
            case 0 -> {
                this.physicsTables.writeEnemyTransitionCountdown(s, 0x40);
                this.encounter.state[s] = 1;
            }
            case 1 -> {
                if (this.physicsTables.readEnemyTransitionCountdown(s) == 0) {
                    this.physicsTables.writeEnemyTransitionCountdown(s, 0xA0);
                    this.encounter.state[s] = 2;
                }
            }
            default -> {
                if (this.physicsTables.readEnemyTransitionCountdown(s) == 0) {
                    boolean another = this.slotEntities.stream()
                            .anyMatch(e ->
                                    e.loaded() && e.slot() != s && e.type() == EntityRomConstants.ENTITY_SLIME_EYE);
                    if (!another) {
                        this.bossRewards.spawnMoldormHeartContainer(
                                f.slotFrame().entity());
                        this.noise(s, 0x1A);
                    }
                    this.lifecycle.disableEntityWithoutPersistence(s);
                } else if ((this.physicsTables.readEnemyTransitionCountdown(s) & 7) == 0) {
                    int x = (f.slotFrame().entity().x()
                                    + (f.inputs().randomByteSupplier().getAsInt() & 31)
                                    - 16)
                            & 255;
                    int y = (f.slotFrame().entity().y()
                                    - f.slotFrame().entity().z()
                                    + (f.inputs().randomByteSupplier().getAsInt() & 31)
                                    - 16)
                            & 255;
                    this.bossRewards.createMoldormPoof(f.slotFrame().entity(), x, y);
                }
            }
        }
    }

    void render(int s) {
        RoomEntity e = this.slots[s];
        if (this.spriteHandlers == null) return;
        if (encounter.half[s] == 0 && encounter.state[s] == 0) {
            this.slots[s] = withVariant(e, -1);
            return;
        }
        if (encounter.variant[s] == 255) {
            this.slots[s] = withVariant(e, -1);
            return;
        }
        var definition = this.spriteHandlers.forSlimeEyeState(encounter.half[s] != 0, encounter.variant[s], e.z());
        this.slots[s] = withFlipAttribute(
                withDefinition(e, definition, 0),
                this.physicsTables.readBaseEntityFlipAttribute(s)
                        ^ ((this.combatTables.readEnemyFlashCountdown(s) << 2) & 0x10));
    }

    void sound(int s, int id) {
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                s, EntityRomConstants.ENTITY_SLIME_EYE, 0, false, EntityCombatEvent.SoundChannel.JINGLE, id));
    }

    void noise(int s, int id) {
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                s, EntityRomConstants.ENTITY_SLIME_EYE, 0, false, EntityCombatEvent.SoundChannel.NOISE, id));
    }

    void resolveCombat(SlimeEyeCombatFrame inputs) {
        this.slimeEyeCombatStep.run(inputs);
    }

    boolean overlaps(RoomEntity e, int box, int x, int w, int y, int h) {
        if (w <= 0 || h <= 0) return false;
        int dx = Math.abs((byte) (e.x() + read(CONTACT_HITBOXES, box) - x));
        int dy = Math.abs((byte) (e.y() - e.z() + read(CONTACT_HITBOXES, box + 2) - y));
        return dx < read(CONTACT_HITBOXES, box + 1) + w && dy < read(CONTACT_HITBOXES, box + 3) + h;
    }

    final SlimeEyeHalfStep slimeEyeHalfStep = new SlimeEyeHalfStep(this);
    final SlimeEyeCombatStep slimeEyeCombatStep = new SlimeEyeCombatStep(this);
    /** Encounter slot state shared with slot clearing, independent of behavior dispatch. */
    static final class State {
        final int[] state = new int[16],
                half = new int[16],
                stretch = new int[16],
                moving = new int[16],
                angry = new int[16],
                dropping = new int[16],
                // ROM GetEntityPrivateCountdown1 / wEntitiesPrivateCountdown1Table.
                privateCountdown1 = new int[16],
                splitting = new int[16],
                variant = new int[16],
                speedX = new int[16],
                speedY = new int[16],
                speedZ = new int[16],
                accX = new int[16],
                accY = new int[16],
                accZ = new int[16],
                swordHitFrame = new int[16];
        final boolean[] dying = new boolean[16];

        void clear(int slot) {
            for (int[] table : new int[][] {
                state,
                half,
                stretch,
                moving,
                angry,
                dropping,
                privateCountdown1,
                splitting,
                variant,
                speedX,
                speedY,
                speedZ,
                accX,
                accY,
                accZ,
                swordHitFrame
            }) table[slot] = 0;
            dying[slot] = false;
        }
    }

    record SlimeEyeCombatFrame(
            RoomEntity e,
            int frame,
            int lx,
            int ly,
            boolean airborne,
            boolean interactive,
            boolean sword,
            int sx,
            int sw,
            int sy,
            int sh,
            // Optional attack details; null selects the existing default attack context.
            EnemyAttackContext attack,
            java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events) {}

    static final class SlimeEyeCombatStep {
        private final linksawakening.world.entity.SlimeEyeController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.SlimeEyeController.SlimeEyeCombatFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity e;
        private int frame;
        private int lx;
        private int ly;
        private boolean airborne;
        private boolean interactive;
        private boolean sword;
        private int sx;
        private int sw;
        private int sy;
        private int sh;
        // Optional attack details; null selects the existing default attack context.
        private linksawakening.world.entity.EnemyAttackContext attack;
        private java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events;
        private int s;
        private boolean isHalf;
        private int hitbox;
        private boolean hit;

        SlimeEyeCombatStep(linksawakening.world.entity.SlimeEyeController owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!prepareCollision()) return;
            if (!applySwordHit()) return;
        }

        private boolean prepareCollision() {

            e = inputs.e();
            frame = inputs.frame();
            lx = inputs.lx();
            ly = inputs.ly();
            airborne = inputs.airborne();
            interactive = inputs.interactive();
            sword = inputs.sword();
            sx = inputs.sx();
            sw = inputs.sw();
            sy = inputs.sy();
            sh = inputs.sh();
            attack = inputs.attack();
            events = inputs.events();
            s = e.slot();
            if (!interactive
                    || e.status() != EntityStatus.ACTIVE
                    || owner.combatTables.readEnemyIgnoreHitsCountdown(s) != 0) return false;
            isHalf = owner.encounter.half[s] != 0;
            if ((!isHalf && owner.encounter.state[s] != 3)
                    || (isHalf
                            && (owner.encounter.state[s] >= 2
                                    || (owner.encounter.state[s] == 1
                                            && owner.physicsTables.readEnemyTransitionCountdown(s) != 0))))
                return false;
            if (owner.encounter.splitting[s] != 0
                    || owner.combatTables.readEnemyFlashCountdown(s) > 0
                            && owner.combatTables.readEnemyFlashCountdown(s) < 0x18) return false;
            hitbox = isHalf ? 24 : 20;
            hit = sword && owner.overlaps(e, hitbox, sx, sw, sy, sh);
            return true;
        }

        private boolean applySwordHit() {

            if (hit) {
                return applyDamage();
            } else if (!airborne
                    && !(isHalf && owner.encounter.dropping[s] != 0)
                    && RoomEntityCombatRules.collisionCadenceMatches(frame, s)
                    && owner.overlaps(e, isHalf ? 24 : owner.encounter.stretch[s] * 4, lx + 8, 4, ly + 8, 4)) {
                int damage = owner.enemyCombatTables.contactDamage(
                        EntityRomConstants.ENTITY_SLIME_EYE, owner.combatTables.readEnemyHealthGroup(s));
                var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(e.x(), e.y(), e.z(), lx, ly, 0x14);
                events.add(linksawakening.world.entity.event.EntityCombatEvent.builder(
                                s, EntityRomConstants.ENTITY_SLIME_EYE)
                        .linkDamage(damage)
                        .swordHit(false)
                        .enemyDamage(0)
                        .enemySpecialAction(-1)
                        .soundChannel(EntityCombatEvent.SoundChannel.NONE)
                        .soundId(-1)
                        .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                        .secondarySoundId(-1)
                        .swordPokeVfx(null)
                        .linkCollisionResponse(
                                new EntityCombatEvent.LinkCollisionResponse(vector.x() & 255, vector.y() & 255, 0x10))
                        .build());
            }
            return true;
        }

        private boolean applyDamage() {
            if (!isHalf && owner.encounter.stretch[s] == 4 && attack.pegasusBoots()) {
                owner.encounter.splitting[s] = 0x0C;
                return false;
            }
            // 6DDF clears wIsRunningWithPegasusBoots before ApplySwordDamagesToEnemy
            // selects its damage type. Retaining the pre-collision context doubles
            // the first half hit incorrectly (the ROM takes five HP to four).
            EnemyAttackContext damageAttack = attack.pegasusBoots()
                    ? new EnemyAttackContext(
                            attack.swordLevel(),
                            attack.redTunic(),
                            attack.pieceOfPower(),
                            attack.spinAttack(),
                            false,
                            attack.usingShield(),
                            attack.romDirection(),
                            attack.linkZ())
                    : attack;
            var damage = owner.enemyCombatTables.resolveSwordDamage(
                    EntityRomConstants.ENTITY_SLIME_EYE, owner.combatTables.readEnemyHealthGroup(s), damageAttack);
            int amount = damage.numericDamage();
            if (amount == 0) return false;
            owner.encounter.swordHitFrame[s] = (frame & 255) + 1;
            owner.combatTables.writeEnemyHealth(s, Math.max(0, owner.combatTables.readEnemyHealth(s) - amount));
            owner.combatTables.writeEnemyIgnoreHitsCountdown(s, attack.powerRecoil() ? 0x20 : 0x0A);
            owner.combatTables.writeEnemyFlashCountdown(s, 0x18);
            if (isHalf)
                owner.motions
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .configure(s, e.x(), e.y(), e.z(), lx, ly, 0x30);
            if (isHalf && owner.combatTables.readEnemyHealth(s) == 0) {
                owner.encounter.state[s] = 0;
                owner.slots[s] = withStatus(e, EntityStatus.DYING);
            }
            // Bank 03:7081–70B6 applies the Slime Eye sword response to both halves
            // as well as the unsplit body. Pegasus hits call 6DDF (reset boots,
            // ignore collisions $10, push $14); normal unstretched hits push $10
            // and ignore collisions for four frames. A fully stretched normal
            // strike takes the ordinary damage branch without this push.
            EntityCombatEvent.LinkCollisionResponse response = EntityCombatEvent.LinkCollisionResponse.NONE;
            if (isHalf || owner.encounter.stretch[s] != 4) {
                var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        e.x(), e.y(), e.z(), lx, ly, attack.pegasusBoots() ? 0x14 : 0x10);
                response = new EntityCombatEvent.LinkCollisionResponse(
                        vector.x() & 255, vector.y() & 255, attack.pegasusBoots() ? 0x10 : 4);
            }
            events.add(
                    linksawakening.world.entity.event.EntityCombatEvent.builder(s, EntityRomConstants.ENTITY_SLIME_EYE)
                            .linkDamage(0)
                            .swordHit(true)
                            .enemyDamage(amount)
                            .enemySpecialAction(-1)
                            .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                            .soundId(3)
                            .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                            .secondarySoundId(-1)
                            .swordPokeVfx(null)
                            .linkCollisionResponse(response)
                            .build());

            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.SlimeEyeController.SlimeEyeCombatFrame inputs) {
            if (inUse) {
                new SlimeEyeCombatStep(owner).run(inputs);
                return;
            }
            inUse = true;

            e = null;
            frame = 0;
            lx = 0;
            ly = 0;
            airborne = false;
            interactive = false;
            sword = false;
            sx = 0;
            sw = 0;
            sy = 0;
            sh = 0;
            attack = null;
            events = null;
            s = 0;
            isHalf = false;
            hitbox = 0;
            hit = false;

            this.inputs = inputs;
            try {
                run();
            } finally {
                inUse = false;

                this.inputs = null;

                this.e = null;
                this.attack = null;
                this.events = null;
            }
        }
    }

    static final class SlimeEyeHalfStep {
        private final linksawakening.world.entity.SlimeEyeController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityFrameExecution f;
        private int s;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity e;

        SlimeEyeHalfStep(linksawakening.world.entity.SlimeEyeController owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {

            s = f.slotFrame().entity().slot();
            e = owner.slots[s];
            if (owner.combatTables.readEnemyIgnoreHitsCountdown(s) == 2) {
                if (owner.encounter.angry[s] == 0) owner.encounter.angry[s] = 1;
                else {
                    owner.encounter.state[s] = 2;
                    owner.encounter.speedX[s] = owner.encounter.speedY[s] = 0;
                    owner.sound(s, 0x24);
                }
            }
            if (owner.combatTables.readEnemyIgnoreHitsCountdown(s) != 0
                    && !owner.hitThisFrame(s, f.inputs().frame())) {
                if (owner.motions
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .isActive(s)) {
                    e = owner.backgroundMechanics
                            .applyEnemyRecoilIfNeeded(e, f.inputs().backgroundCollision())
                            .entity();
                    owner.slots[s] = e;
                } else owner.combatTables.postEnemyIgnoreHitsCountdown(s, -1);
                // ApplyRecoilIfNeeded_04 discards the caller return address.
                // The half's state machine, including Z, does not run this frame.
                return;
            }
            switch (owner.encounter.state[s]) {
                case 0 -> {
                    if (!advanceState0()) return;
                }
                case 1 -> {
                    if (!advanceState1()) return;
                }
                case 2 -> {
                    if (!advanceState2()) return;
                }
                case 3 -> {
                    if (!advanceState3()) return;
                }
                default -> throw new IllegalStateException("Slime Eye half state " + owner.encounter.state[s]);
            }
        }

        private boolean advanceState0() {
            if (owner.physicsTables.readEnemyTransitionCountdown(s) == 0) {
                int index = f.inputs().randomByteSupplier().getAsInt() & 7;
                owner.encounter.speedX[s] = owner.read(HALF_SPEED_X, index);
                owner.encounter.speedY[s] = owner.read(HALF_SPEED_Y, index);
                if ((f.inputs().randomByteSupplier().getAsInt() & 3) == 0) {
                    EntityMotionMath.Vector v = EntityMotionMath.vectorAwayFromLink(
                            e.x(), e.y(), f.inputs().linkEntityX(), f.inputs().linkEntityY(), 0x18);
                    owner.encounter.speedX[s] = (-v.x()) & 255;
                    owner.encounter.speedY[s] = (-v.y()) & 255;
                }
                owner.encounter.speedZ[s] = (f.inputs().randomByteSupplier().getAsInt() & 15) + 8;
                owner.encounter.state[s] = 1;
            }
            owner.encounter.variant[s] = 0;

            return true;
        }

        private boolean advanceState1() {
            if (owner.physicsTables.readEnemyTransitionCountdown(s) == 0) {
                e = owner.integrateXY(e, f.inputs().backgroundCollision());
                e = owner.integrateZ(e);
                owner.encounter.speedZ[s] = (owner.encounter.speedZ[s] - 2) & 255;
                if ((e.z() & 0x80) != 0) {
                    e = withZ(e, 0);
                    owner.encounter.dropping[s] = 0;
                    if ((((owner.encounter.speedZ[s] - 0xE0) & 255) & 0x80) != 0) {
                        f.emit(new ScreenShakeRequest(s, 0x18, 0));
                        owner.sound(s, 0x0B);
                        if (!f.inputs().linkAirborne()) owner.stunLink(s, 0x0E);
                    } else if (owner.encounter.speedZ[s] < 0xF2) owner.sound(s, 0x20);
                    owner.encounter.speedZ[s] = 0;
                    owner.encounter.state[s] = 0;
                    owner.physicsTables.writeEnemyTransitionCountdown(s, 0x20);
                }
                owner.slots[s] = e;
            }
            owner.encounter.variant[s] = 1;

            return true;
        }

        private boolean advanceState2() {
            owner.encounter.speedZ[s] = 0x60;
            e = owner.integrateZ(e);
            owner.slots[s] = e;
            SlimeEyeHalfMotion.Update result = SlimeEyeHalfMotion.rise(
                    e.z(),
                    owner.physicsTables.readEnemyTransitionCountdown(s),
                    owner.encounter.variant[s],
                    owner.encounter.dropping[s],
                    e.x(),
                    e.y());
            owner.encounter.state[s] = result.state();
            owner.physicsTables.writeEnemyTransitionCountdown(s, result.countdown());

            return true;
        }

        private boolean advanceState3() {
            SlimeEyeHalfMotion.Update result = SlimeEyeHalfMotion.waitAbove(
                    owner.physicsTables.readEnemyTransitionCountdown(s),
                    owner.encounter.speedZ[s],
                    owner.encounter.dropping[s],
                    e.x(),
                    e.y(),
                    f.inputs().linkEntityX(),
                    f.inputs().linkEntityY());
            owner.encounter.state[s] = result.state();
            owner.encounter.speedZ[s] = result.speedZ();
            owner.encounter.variant[s] = result.spriteVariant();
            owner.encounter.dropping[s] = result.privateState4();
            owner.physicsTables.writeEnemyTransitionCountdown(s, result.countdown());
            owner.slots[s] = withPositionAndVariant(e, result.x(), result.y(), e.spriteVariant());
            if (result.jingle() >= 0) owner.sound(s, result.jingle());

            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.EntityFrameExecution f) {
            if (inUse) {
                new SlimeEyeHalfStep(owner).run(f);
                return;
            }
            inUse = true;

            s = 0;
            e = null;

            this.f = f;
            try {
                run();
            } finally {
                inUse = false;

                this.f = null;

                this.e = null;
            }
        }
    }
}
