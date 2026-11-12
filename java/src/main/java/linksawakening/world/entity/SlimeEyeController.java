package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;

/** Source-ordered Slime Eye handler (04:496F–504B), including child slot ownership. */
public final class SlimeEyeController {
    final SlimeEyeControllerServices runtime;
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
    private Rom rom;
    int screenShakeCountdown;

    SlimeEyeController(SlimeEyeControllerServices runtime) {
        this.runtime = runtime;
    }

    void loadRom(byte[] bytes) {
        if (bytes != null) rom = new Rom(bytes);
    }

    public void setScreenShakeCountdown(int value) {
        screenShakeCountdown = value & 255;
    }

    int state(int slot) {
        return state[slot];
    }

    boolean half(int slot) {
        return half[slot] != 0;
    }

    int stretch(int slot) {
        return stretch[slot];
    }

    int variant(int slot) {
        return variant[slot];
    }

    int dropping(int slot) {
        return dropping[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

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

    int read(RomAddress table, int index) {
        if (rom == null) throw new IllegalStateException("Slime Eye requires ROM encounter tables");
        return rom.u8(table.plus(index));
    }

    public void advance(EntityFrameExecution frame) {
        try {
            advanceHandler(frame);
        } finally {
            swordHitFrame[frame.slotFrame().entity().slot()] = 0;
        }
    }

    private void advanceHandler(EntityFrameExecution frame) {
        SlimeEyeBehavior.advanceHandler(this, frame);
    }

    void advanceBody(EntityFrameExecution f) {
        SlimeEyeBehavior.advanceBody(this, f);
    }

    void wander(EntityFrameExecution f) {
        int s = f.slotFrame().entity().slot(),
                countdown = runtime.physicsTables().readEnemyTransitionCountdown(s);
        RoomEntity e = runtime.slots()[s];
        if (moving[s] == 0) {
            if (countdown != 0) return;
            runtime.physicsTables()
                    .writeEnemyTransitionCountdown(
                            s, (f.inputs().randomByteSupplier().getAsInt() & 0x1F) + 0x10);
            moving[s] = 1;
            int quadrant = (e.x() >= 0x50 ? 1 : 0) | (((e.y() - e.z()) & 255) >= 0x48 ? 2 : 0);
            int index = quadrant * 4 | (f.inputs().randomByteSupplier().getAsInt() & 3);
            speedX[s] = read(RomSymbols.Data_004_4B5C, index);
            speedY[s] = read(RomSymbols.Data_004_4B6C, index);
        } else if (countdown == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x30);
            moving[s] = 0;
        } else if ((countdown & 0x0E) == 0)
            runtime.slots()[s] = integrateXY(e, f.inputs().backgroundCollision());
    }

    void advanceHalf(EntityFrameExecution f) {
        SlimeEyeBehavior.advanceHalf(this, f);
    }

    boolean hitThisFrame(int slot, int frame) {
        return swordHitFrame[slot] == (frame & 255) + 1;
    }

    void stunLink(int s, int countdown) {
        runtime.physicsTables().writeSlowTransitionCountdown(s, countdown);
        runtime.physicsTables().writeSlowTimerInitialized(s, true);
    }

    RoomEntity integrateZ(RoomEntity e) {
        int s = e.slot(), value = accZ[s] + (byte) speedZ[s];
        accZ[s] = value & 15;
        return withZ(e, (e.z() + (value >> 4)) & 255);
    }

    RoomEntity integrateXY(RoomEntity e, RoomEntityBackgroundCollision collision) {
        int s = e.slot(), x = accX[s] + (byte) speedX[s], y = accY[s] + (byte) speedY[s];
        accX[s] = x & 15;
        accY[s] = y & 15;
        int nx = (e.x() + (x >> 4)) & 255, ny = (e.y() + (y >> 4)) & 255;
        if (collision != null) {
            if (collision.blocks(e, (byte) speedX[s] < 0 ? 1 : 0, nx, e.y())) {
                nx = e.x();
                speedX[s] = (-speedX[s]) & 255;
            }
            if (collision.blocks(e, (byte) speedY[s] < 0 ? 2 : 3, nx, ny)) {
                ny = e.y();
                speedY[s] = (-speedY[s]) & 255;
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
            half[child] = 1;
            state[child] = 1;
            speedX[child] = read(RomSymbols.Data_004_496D, i);
            speedZ[child] = 0x10;
            runtime.combatTables().writeEnemyHealth(child, 5);
            render(child);
        }
        runtime.lifecycle().disableEntityWithoutPersistence(e.slot());
    }

    void advanceDeath(EntityFrameExecution f) {
        SlimeEyeCombat.advanceDeath(this, f);
    }

    void render(int s) {
        RoomEntity e = runtime.slots()[s];
        if (runtime.spriteHandlers() == null) return;
        if (half[s] == 0 && state[s] == 0) {
            runtime.slots()[s] = withVariant(e, -1);
            return;
        }
        if (variant[s] == 255) {
            runtime.slots()[s] = withVariant(e, -1);
            return;
        }
        var definition = runtime.spriteHandlers().forSlimeEyeState(half[s] != 0, variant[s], e.z());
        runtime.slots()[s] = withFlipAttribute(
                withDefinition(e, definition, 0),
                runtime.physicsTables().readBaseEntityFlipAttribute(s)
                        ^ ((runtime.combatTables().readEnemyFlashCountdown(s) << 2) & 0x10));
    }

    void sound(int s, int id) {
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(s, 0x5B, 0, false, EntityCombatEvent.SoundChannel.JINGLE, id));
    }

    void noise(int s, int id) {
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(s, 0x5B, 0, false, EntityCombatEvent.SoundChannel.NOISE, id));
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
}
