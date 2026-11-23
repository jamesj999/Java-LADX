package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withFlipAttribute;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
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

/** Bank-$04 SlimeEyeEntityHandler and label_004_496F child spawning ($496F–$504B). */
public final class SlimeEyeController {
    private static final RomAddress CHILD_X_OFFSETS = RomSymbols.Data_004_496B; // ROM Data_004_496B
    private static final RomAddress CHILD_SPEED_X = RomSymbols.Data_004_496D; // ROM Data_004_496D
    private static final RomAddress CONTACT_HITBOXES = RomSymbols.Data_004_4B12; // ROM Data_004_4B12
    private static final RomAddress BODY_SPEED_X = RomSymbols.Data_004_4B5C; // ROM Data_004_4B5C
    private static final RomAddress BODY_SPEED_Y = RomSymbols.Data_004_4B6C; // ROM Data_004_4B6C
    private static final RomAddress HALF_SPEED_X = RomSymbols.Data_004_4E94; // ROM Data_004_4E94
    private static final RomAddress HALF_SPEED_Y = RomSymbols.Data_004_4E9C; // ROM Data_004_4E9C

    private final List<RoomEntity> slotEntities;
    private final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    private final EntityBackgroundMechanics backgroundMechanics;
    private final EntityBossRewards bossRewards;
    private final EntityCombatTables combatTables;
    private final EntityEventChannels events;
    private final EntityLinkView link;
    private final EntityPresentationState presentation;
    private final EntityRoomInspection roomInspection;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntitySlotLifecycle lifecycle;
    private final EntitySlotProperties slotProperties;
    private final RomEnemyCombatTables enemyCombatTables;
    private final EntitySlots slots;
    private final State encounter;
    // The ROM-free reader retains the original encounter-table precondition.
    private final ToIntFunction<RomAddress> readByte;
    private int screenShakeCountdown;

    SlimeEyeController(
            State encounter,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBossRewards bossRewards,
            EntityEventChannels events,
            EntityLinkView link,
            EntityPresentationState presentation,
            EntityRoomInspection roomInspection,
            EntityMotionRegistry motions,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            EntitySlots slots,
            Rom rom) {
        List<RoomEntity> slotEntities = slots.view();
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.readByte = rom == null
                ? address -> {
                    throw new IllegalStateException("Slime Eye requires ROM encounter tables");
                }
                : rom::u8;
        this.encounter = Objects.requireNonNull(encounter, "encounter");
        this.slotEntities = Objects.requireNonNull(slotEntities, "slotEntities");
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.backgroundMechanics = Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.link = Objects.requireNonNull(link, "link");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.roomInspection = Objects.requireNonNull(roomInspection, "roomInspection");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = Objects.requireNonNull(slots, "slots");
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
        return readByte.applyAsInt(table.plus(index));
    }

    public void advance(RoomEntity entity, EntityFrameInputLatch inputs, EntityTimerGates gates) {
        try {
            advanceHandler(entity, inputs, gates);
        } finally {
            encounter.swordHitFrame[entity.slot()] = 0;
        }
    }

    private void advanceHandler(RoomEntity entity, EntityFrameInputLatch inputs, EntityTimerGates gates) {
        int slot = entity.slot();
        if (entity.status() == EntityStatus.INIT) {
            this.clear(slot);
            this.combatTables.writeEnemyHealth(slot, 0x50);
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
            this.slots.replace(slot, withZ(withStatus(entity, EntityStatus.ACTIVE), 0x7E));
            this.render(slot);
            return;
        }
        if (entity.status() != EntityStatus.ACTIVE && entity.status() != EntityStatus.DYING) return;
        // UpdateEntityTimers precedes the handler's interactivity guards.
        if (this.encounter.privateCountdown1[slot] > 0) this.encounter.privateCountdown1[slot]--;
        if (this.encounter.splitting[slot] > 0) this.encounter.splitting[slot]--;
        if (!roomInspection.roomTransitionActive() && !roomInspection.freeMovementMode()) {
            int dx = 0, dy = 0;
            if (((inputs.linkEntityX() - 0x11) & 255) >= 0x7E) dx = inputs.linkEntityX() < 0x50 ? 1 : -1;
            else if (((inputs.linkEntityY() - 0x16) & 255) >= 0x5E) dy = -1;
            if (dx != 0 || dy != 0) {
                events.linkMotionBlock(new LinkMotionBlockRequest(slot, dx, dy, 1));
                inputs.linkEntityX((inputs.linkEntityX() + dx) & 255);
                inputs.linkEntityY((inputs.linkEntityY() + dy) & 255);
                lifecycle.delayInteractions(2);
            }
        }
        this.lifecycle.decrementSlowTransitionCountdown(slot, inputs.frame());
        if (!link.inventoryAppearing() && !roomInspection.roomTransitionActive()) {
            BossIntroMotion.Update intro = this.motions
                    .get(BossIntroMotion.class, BossIntroMotion::new)
                    .advance(
                            this.slotProperties.options1(slot),
                            entity.type(),
                            roomInspection.entityMapId(),
                            this.lifecycle.transitionSequenceCounter());
            if (intro.musicTrack() >= 0) presentation.writePendingMusicTrack(intro.musicTrack());
            if (intro.dialogLowId() >= 0) {
                events.dialog(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                this.render(slot);
                return;
            }
        }
        // Main's combat pass precedes tick. Restore the fresh write that the
        // common flash tick would otherwise age before the ROM collision site.
        if (this.hitThisFrame(slot, inputs.frame())) this.combatTables.writeEnemyFlashCountdown(slot, 0x18);
        // RenderActiveEntitySpritesRect runs before this frame's state changes.
        this.render(slot);
        if (this.physicsTables.readSlowTransitionCountdown(slot) != 0) {
            events.linkMotionBlock(new LinkMotionBlockRequest(slot));
            events.linkFallenPose(new LinkFallenPoseRequest(slot));
        }
        if (entity.status() == EntityStatus.DYING) {
            // The lethal collision occurs inside the active handler in ROM.
            // Its destruction handler starts on the following slot visit.
            if (this.hitThisFrame(slot, inputs.frame())) return;
            if (!this.encounter.dying[slot]) {
                this.encounter.state[slot] = 0;
                this.encounter.dying[slot] = true;
            }
            this.advanceDeath(entity, inputs);
            return;
        }
        if (this.encounter.half[slot] == 0) this.combatTables.writeEnemyHealth(slot, 0x50);
        if ((this.encounter.half[slot] != 0 || this.encounter.state[slot] != 0)
                && (!gates.handlerInteractive() || gates.countdownBlocksInteraction())) return;
        if (this.encounter.half[slot] == 0) this.advanceBody(slot, inputs);
        else this.slimeEyeHalfStep.run(slot, inputs);
    }

    private void advanceBody(int s, EntityFrameInputLatch inputs) {
        RoomEntity e = this.slots.get(s);
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
                                (inputs.randomByteSupplier().getAsInt() & 0x3F) + 0x40,
                                (inputs.randomByteSupplier().getAsInt() & 0x3F) + 0x30,
                                0x70);
                }
            }
            case 1 -> {
                e = this.integrateZ(e);
                if (this.encounter.speedZ[s] != 0xA0) this.encounter.speedZ[s] = (this.encounter.speedZ[s] - 2) & 255;
                if ((e.z() & 0x80) != 0) {
                    e = withZ(e, 0);
                    events.screenShake(new ScreenShakeRequest(s, 0x50, 4));
                    this.physicsTables.writeEnemyTransitionCountdown(s, 0x40);
                    this.noise(s, 0x0C);
                    if (!inputs.linkAirborne()) this.stunLink(s, 0x14);
                    this.encounter.state[s] = 2;
                }
                this.slots.replace(s, e);
            }
            case 2 -> {
                if (this.physicsTables.readEnemyTransitionCountdown(s) == 0) this.encounter.state[s] = 3;
            }
            case 3 -> {
                if (this.combatTables.readEnemyIgnoreHitsCountdown(s) > 0 && !this.hitThisFrame(s, inputs.frame()))
                    this.combatTables.postEnemyIgnoreHitsCountdown(s, -1);
                if (this.encounter.splitting[s] != 0) {
                    if (this.encounter.splitting[s] == 1) {
                        this.split(e);
                        return;
                    }
                    events.linkMotionBlock(new LinkMotionBlockRequest(
                            s, 0, (inputs.frame() & 1) == 0 ? -1 : 0, this.encounter.splitting[s]));
                    this.encounter.variant[s] = 6;
                    return;
                }
                this.wander(s, inputs);
                this.encounter.variant[s] = (inputs.frame() >> 4) & 1;
                if (this.encounter.stretch[s] != 0) {
                    this.encounter.variant[s] = this.encounter.stretch[s] + 1;
                    if (this.encounter.privateCountdown1[s] == 0) {
                        this.encounter.privateCountdown1[s] = 0x28;
                        this.encounter.stretch[s]--;
                    }
                }
                if (this.combatTables.readEnemyFlashCountdown(s) != 0 && (inputs.frame() & 0x1F) == 0) {
                    this.encounter.privateCountdown1[s] = 0x50;
                    if (this.encounter.stretch[s] != 4) this.encounter.stretch[s]++;
                }
            }
            default -> throw new IllegalStateException("Slime Eye body state " + this.encounter.state[s]);
        }
    }

    private void wander(int s, EntityFrameInputLatch inputs) {
        int countdown = this.physicsTables.readEnemyTransitionCountdown(s);
        RoomEntity e = this.slots.get(s);
        if (encounter.moving[s] == 0) {
            if (countdown != 0) return;
            this.physicsTables.writeEnemyTransitionCountdown(
                    s, (inputs.randomByteSupplier().getAsInt() & 0x1F) + 0x10);
            encounter.moving[s] = 1;
            int quadrant = (e.x() >= 0x50 ? 1 : 0) | (((e.y() - e.z()) & 255) >= 0x48 ? 2 : 0);
            int index = quadrant * 4 | (inputs.randomByteSupplier().getAsInt() & 3);
            encounter.speedX[s] = read(BODY_SPEED_X, index);
            encounter.speedY[s] = read(BODY_SPEED_Y, index);
        } else if (countdown == 0) {
            this.physicsTables.writeEnemyTransitionCountdown(s, 0x30);
            encounter.moving[s] = 0;
        } else if ((countdown & 0x0E) == 0) this.slots.replace(s, integrateXY(e, inputs.backgroundCollision()));
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

    RoomEntity integrateXY(RoomEntity e, RoomEntityBackgroundCollision collision) {
        int s = e.slot(),
                x = encounter.accX[s] + (byte) encounter.speedX[s],
                y = encounter.accY[s] + (byte) encounter.speedY[s];
        encounter.accX[s] = x & 15;
        encounter.accY[s] = y & 15;
        int nx = (e.x() + (x >> 4)) & 255, ny = (e.y() + (y >> 4)) & 255;
        {
            if (collision.blocks(e, (byte) encounter.speedX[s] < 0 ? 1 : 0, nx, e.y())) {
                nx = e.x();
                encounter.speedX[s] = -encounter.speedX[s] & 255;
            }
            if (collision.blocks(e, (byte) encounter.speedY[s] < 0 ? 2 : 3, nx, ny)) {
                ny = e.y();
                encounter.speedY[s] = -encounter.speedY[s] & 255;
            }
        }
        return withPositionAndVariant(e, nx, ny, e.spriteVariant());
    }

    int spawn(int type, int x, int y, int z) {
        int s = this.slots.findFree(this.slots.size() - 1);
        if (s < 0) return -1;
        this.lifecycle.disableEntityWithoutPersistence(s);
        var definition = type == 0x1B && this.spriteHandlers.supportsPresentation()
                ? this.slotProperties.spriteViews().forZolSlimeEye()
                : this.slotProperties.spriteDefinitionFor(type);
        this.slots.replace(
                s,
                new RoomEntity(
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
                        false));
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

    private void advanceDeath(RoomEntity entity, EntityFrameInputLatch inputs) {
        int s = entity.slot();
        this.combatTables.writeEnemyFlashCountdown(s, inputs.frame() & 255);
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
                        this.bossRewards.spawnMoldormHeartContainer(entity);
                        this.noise(s, 0x1A);
                    }
                    this.lifecycle.disableEntityWithoutPersistence(s);
                } else if ((this.physicsTables.readEnemyTransitionCountdown(s) & 7) == 0) {
                    int x = (entity.x() + (inputs.randomByteSupplier().getAsInt() & 31) - 16) & 255;
                    int y = (entity.y()
                                    - entity.z()
                                    + (inputs.randomByteSupplier().getAsInt() & 31)
                                    - 16)
                            & 255;
                    this.bossRewards.createMoldormPoof(entity, x, y);
                }
            }
        }
    }

    void render(int s) {
        RoomEntity e = this.slots.get(s);
        if (!this.spriteHandlers.supportsPresentation()) return;
        if (encounter.half[s] == 0 && encounter.state[s] == 0) {
            this.slots.replace(s, withVariant(e, -1));
            return;
        }
        if (encounter.variant[s] == 255) {
            this.slots.replace(s, withVariant(e, -1));
            return;
        }
        var definition = this.spriteHandlers.forSlimeEyeState(encounter.half[s] != 0, encounter.variant[s], e.z());
        this.slots.replace(
                s,
                withFlipAttribute(
                        withDefinition(e, definition, 0),
                        this.physicsTables.readBaseEntityFlipAttribute(s)
                                ^ ((this.combatTables.readEnemyFlashCountdown(s) << 2) & 0x10)));
    }

    void sound(int s, int id) {
        this.events.sound(s, EntityRomConstants.ENTITY_SLIME_EYE, EntityCombatEvent.SoundChannel.JINGLE, id);
    }

    void noise(int s, int id) {
        this.events.sound(s, EntityRomConstants.ENTITY_SLIME_EYE, EntityCombatEvent.SoundChannel.NOISE, id);
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

    private final SlimeEyeHalfStep slimeEyeHalfStep = new SlimeEyeHalfStep(this);
    private final SlimeEyeCombatStep slimeEyeCombatStep = new SlimeEyeCombatStep(this);
    /** Encounter slot state shared with slot clearing, independent of behavior dispatch. */
    static final class State {
        private final int[] state = new int[16],
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
        private final boolean[] dying = new boolean[16];

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

        int[] state() {
            return this.state;
        }

        int[] half() {
            return this.half;
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
            List<linksawakening.world.entity.event.EntityCombatEvent> events) {}

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
        private List<linksawakening.world.entity.event.EntityCombatEvent> events;
        private int s;
        private boolean isHalf;
        private int hitbox;
        private boolean hit;

        SlimeEyeCombatStep(linksawakening.world.entity.SlimeEyeController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
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
                    || (owner.combatTables.readEnemyFlashCountdown(s) > 0
                            && owner.combatTables.readEnemyFlashCountdown(s) < 0x18)) return false;
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
            var enemyRecoilMotion = owner.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
            if (isHalf) enemyRecoilMotion.configure(s, e.x(), e.y(), e.z(), lx, ly, 0x30);
            if (isHalf && owner.combatTables.readEnemyHealth(s) == 0) {
                owner.encounter.state[s] = 0;
                owner.slots.replace(s, withStatus(e, EntityStatus.DYING));
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
        private EntityFrameInputLatch inputs;
        private int s;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity e;

        SlimeEyeHalfStep(linksawakening.world.entity.SlimeEyeController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            e = owner.slots.get(s);
            if (owner.combatTables.readEnemyIgnoreHitsCountdown(s) == 2) {
                if (owner.encounter.angry[s] == 0) owner.encounter.angry[s] = 1;
                else {
                    owner.encounter.state[s] = 2;
                    owner.encounter.speedX[s] = owner.encounter.speedY[s] = 0;
                    owner.sound(s, 0x24);
                }
            }
            if (owner.combatTables.readEnemyIgnoreHitsCountdown(s) != 0 && !owner.hitThisFrame(s, inputs.frame())) {
                var enemyRecoilMotion = owner.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
                if (enemyRecoilMotion.isActive(s)) {
                    e = owner.backgroundMechanics
                            .applyEnemyRecoilIfNeeded(e, inputs.backgroundCollision())
                            .entity();
                    owner.slots.replace(s, e);
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
                int index = inputs.randomByteSupplier().getAsInt() & 7;
                owner.encounter.speedX[s] = owner.read(HALF_SPEED_X, index);
                owner.encounter.speedY[s] = owner.read(HALF_SPEED_Y, index);
                if ((inputs.randomByteSupplier().getAsInt() & 3) == 0) {
                    EntityMotionMath.Vector v = EntityMotionMath.vectorAwayFromLink(
                            e.x(), e.y(), inputs.linkEntityX(), inputs.linkEntityY(), 0x18);
                    owner.encounter.speedX[s] = -v.x() & 255;
                    owner.encounter.speedY[s] = -v.y() & 255;
                }
                owner.encounter.speedZ[s] = (inputs.randomByteSupplier().getAsInt() & 15) + 8;
                owner.encounter.state[s] = 1;
            }
            owner.encounter.variant[s] = 0;

            return true;
        }

        private boolean advanceState1() {
            if (owner.physicsTables.readEnemyTransitionCountdown(s) == 0) {
                e = owner.integrateXY(e, inputs.backgroundCollision());
                e = owner.integrateZ(e);
                owner.encounter.speedZ[s] = (owner.encounter.speedZ[s] - 2) & 255;
                if ((e.z() & 0x80) != 0) {
                    e = withZ(e, 0);
                    owner.encounter.dropping[s] = 0;
                    if ((((owner.encounter.speedZ[s] - 0xE0) & 255) & 0x80) != 0) {
                        owner.events.screenShake(new ScreenShakeRequest(s, 0x18, 0));
                        owner.sound(s, 0x0B);
                        if (!inputs.linkAirborne()) owner.stunLink(s, 0x0E);
                    } else if (owner.encounter.speedZ[s] < 0xF2) owner.sound(s, 0x20);
                    owner.encounter.speedZ[s] = 0;
                    owner.encounter.state[s] = 0;
                    owner.physicsTables.writeEnemyTransitionCountdown(s, 0x20);
                }
                owner.slots.replace(s, e);
            }
            owner.encounter.variant[s] = 1;

            return true;
        }

        private boolean advanceState2() {
            owner.encounter.speedZ[s] = 0x60;
            e = owner.integrateZ(e);
            owner.slots.replace(s, e);
            HalfUpdate result = riseHalf(
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
            HalfUpdate result = waitAboveHalf(
                    owner.physicsTables.readEnemyTransitionCountdown(s),
                    owner.encounter.speedZ[s],
                    owner.encounter.dropping[s],
                    e.x(),
                    e.y(),
                    inputs.linkEntityX(),
                    inputs.linkEntityY());
            owner.encounter.state[s] = result.state();
            owner.encounter.speedZ[s] = result.speedZ();
            owner.encounter.variant[s] = result.spriteVariant();
            owner.encounter.dropping[s] = result.dropping();
            owner.physicsTables.writeEnemyTransitionCountdown(s, result.countdown());
            owner.slots.replace(s, withPositionAndVariant(e, result.x(), result.y(), e.spriteVariant()));
            if (result.jingle() >= 0) owner.sound(s, result.jingle());

            return true;
        }

        private boolean inUse;

        void run(int slot, EntityFrameInputLatch inputs) {
            if (inUse) {
                new SlimeEyeHalfStep(owner).run(slot, inputs);
                return;
            }
            inUse = true;

            s = slot;
            e = null;

            this.inputs = inputs;
            try {
                run();
            } finally {
                inUse = false;

                this.inputs = null;

                this.e = null;
            }
        }
    }

    // Bank 04:4F65–4FAD. Countdown ticking and Z integration remain at the caller's original sites.
    public record HalfUpdate(
            int state, int countdown, int speedZ, int spriteVariant, int dropping, int x, int y, int jingle) {}

    /** Apply state 2 after AddEntityZSpeedToPos, using its resulting byte Z. */
    public static HalfUpdate riseHalf(int integratedZ, int countdown, int spriteVariant, int dropping, int x, int y) {
        return new HalfUpdate(
                (integratedZ & 0xFF) < 0x70 ? 2 : 3,
                (integratedZ & 0xFF) < 0x70 ? countdown : 0x30,
                0x60,
                spriteVariant,
                dropping,
                x,
                y,
                -1);
    }

    /** State 3 hides the half, then drops it at Link's current coordinates. */
    public static HalfUpdate waitAboveHalf(
            int countdown, int speedZ, int dropping, int x, int y, int linkX, int linkY) {
        if (countdown != 0) {
            return new HalfUpdate(3, countdown, speedZ, 0xFF, dropping, x, y, -1);
        }
        return new HalfUpdate(1, 0x18, 0xC0, 0xFF, 1, linkX & 0xFF, linkY & 0xFF, 0x08);
    }

    State encounter() {
        return this.encounter;
    }
}
