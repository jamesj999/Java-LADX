package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;

/** Source-ordered Slime Eye handler (04:496F–504B), including child slot ownership. */
public final class SlimeEyeController {
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
    private Rom rom;
    int screenShakeCountdown;

    SlimeEyeController(
            State encounter,
            java.util.List<RoomEntity> slotEntities,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBossRewards bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntity[] slots) {
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

    void loadRom(Rom source) {
        if (source != null) rom = source;
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
            encounter.private1,
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
        SlimeEyeBehavior.advanceHandler(this, frame);
    }

    void advanceBody(EntityFrameExecution f) {
        SlimeEyeBehavior.advanceBody(this, f);
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
            encounter.speedX[s] = read(RomSymbols.Data_004_4B5C, index);
            encounter.speedY[s] = read(RomSymbols.Data_004_4B6C, index);
        } else if (countdown == 0) {
            this.physicsTables.writeEnemyTransitionCountdown(s, 0x30);
            encounter.moving[s] = 0;
        } else if ((countdown & 0x0E) == 0)
            this.slots[s] = integrateXY(e, f.inputs().backgroundCollision());
    }

    void advanceHalf(EntityFrameExecution f) {
        SlimeEyeBehavior.advanceHalf(this, f);
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
        return SlimeEyeBehavior.spawn(this, type, x, y, z);
    }

    void split(RoomEntity e) {
        for (int i = 0; i < 2; i++) {
            int child = spawn(0x5B, e.x() + (byte) read(RomSymbols.Data_004_496B, i), e.y(), 0);
            if (child < 0) continue;
            encounter.half[child] = 1;
            encounter.state[child] = 1;
            encounter.speedX[child] = read(RomSymbols.Data_004_496D, i);
            encounter.speedZ[child] = 0x10;
            this.combatTables.writeEnemyHealth(child, 5);
            render(child);
        }
        this.lifecycle.disableEntityWithoutPersistence(e.slot());
    }

    void advanceDeath(EntityFrameExecution f) {
        SlimeEyeCombat.advanceDeath(this, f);
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
        this.events.pendingEntityEvents.add(
                new EntityCombatEvent(s, 0x5B, 0, false, EntityCombatEvent.SoundChannel.JINGLE, id));
    }

    void noise(int s, int id) {
        this.events.pendingEntityEvents.add(
                new EntityCombatEvent(s, 0x5B, 0, false, EntityCombatEvent.SoundChannel.NOISE, id));
    }

    void resolveCombat(SlimeEyeCombatFrame inputs) {
        SlimeEyeCombat.resolveCombat(this, inputs);
    }

    boolean overlaps(RoomEntity e, int box, int x, int w, int y, int h) {
        if (w <= 0 || h <= 0) return false;
        int dx = Math.abs((byte) (e.x() + read(RomSymbols.Data_004_4B12, box) - x));
        int dy = Math.abs((byte) (e.y() - e.z() + read(RomSymbols.Data_004_4B12, box + 2) - y));
        return dx < read(RomSymbols.Data_004_4B12, box + 1) + w && dy < read(RomSymbols.Data_004_4B12, box + 3) + h;
    }

    final SlimeEyeHalfStep slimeEyeHalfStep = new SlimeEyeHalfStep();
    final SlimeEyeCombatStep slimeEyeCombatStep = new SlimeEyeCombatStep();
    /** Encounter slot state shared with slot clearing, independent of behavior dispatch. */
    static final class State {
        final int[] state = new int[16],
                half = new int[16],
                stretch = new int[16],
                moving = new int[16],
                angry = new int[16],
                dropping = new int[16],
                private1 = new int[16],
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
                private1,
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
}
