package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.SlimeEyeHalfMotion;

/** Per-invocation scratch for SlimeEyeBehavior.advanceHalf; values are assigned in source order. */
final class SlimeEyeHalfStep {
    private linksawakening.world.entity.SlimeEyeController owner;
    private linksawakening.world.entity.EntityFrameExecution f;
    private linksawakening.world.entity.SlimeEyeControllerServices runtime;
    private int s;
    private linksawakening.world.entity.RoomEntity e;

    SlimeEyeHalfStep(
            linksawakening.world.entity.SlimeEyeController owner, linksawakening.world.entity.EntityFrameExecution f) {
        this.owner = owner;
        this.f = f;
    }

    void run() {
        runtime = owner.runtime;
        s = f.slotFrame().entity().slot();
        e = runtime.slots()[s];
        if (runtime.combatTables().readEnemyIgnoreHitsCountdown(s) == 2) {
            if (owner.angry[s] == 0) owner.angry[s] = 1;
            else {
                owner.state[s] = 2;
                owner.speedX[s] = owner.speedY[s] = 0;
                owner.sound(s, 0x24);
            }
        }
        if (runtime.combatTables().readEnemyIgnoreHitsCountdown(s) != 0
                && !owner.hitThisFrame(s, f.inputs().frame())) {
            if (runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .isActive(s)) {
                e = runtime.backgroundMechanics()
                        .applyEnemyRecoilIfNeeded(e, f.inputs().backgroundCollision())
                        .entity();
                runtime.slots()[s] = e;
            } else runtime.combatTables().postEnemyIgnoreHitsCountdown(s, -1);
            // ApplyRecoilIfNeeded_04 discards the caller return address.
            // The half's state machine, including Z, does not run this frame.
            return;
        }
        switch (owner.state[s]) {
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
            default -> throw new IllegalStateException("Slime Eye half state " + owner.state[s]);
        }
    }

    private boolean advanceState0() {
        if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
            int index = f.inputs().randomByteSupplier().getAsInt() & 7;
            owner.speedX[s] = owner.read(RomSymbols.Data_004_4E94, index);
            owner.speedY[s] = owner.read(RomSymbols.Data_004_4E9C, index);
            if ((f.inputs().randomByteSupplier().getAsInt() & 3) == 0) {
                EntityMotionMath.Vector v = EntityMotionMath.vectorAwayFromLink(
                        e.x(), e.y(), f.inputs().linkEntityX(), f.inputs().linkEntityY(), 0x18);
                owner.speedX[s] = (-v.x()) & 255;
                owner.speedY[s] = (-v.y()) & 255;
            }
            owner.speedZ[s] = (f.inputs().randomByteSupplier().getAsInt() & 15) + 8;
            owner.state[s] = 1;
        }
        owner.variant[s] = 0;

        return true;
    }

    private boolean advanceState1() {
        if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
            e = owner.integrateXY(e, f.inputs().backgroundCollision());
            e = owner.integrateZ(e);
            owner.speedZ[s] = (owner.speedZ[s] - 2) & 255;
            if ((e.z() & 0x80) != 0) {
                e = withZ(e, 0);
                owner.dropping[s] = 0;
                if ((((owner.speedZ[s] - 0xE0) & 255) & 0x80) != 0) {
                    f.emit(new ScreenShakeRequest(s, 0x18, 0));
                    owner.sound(s, 0x0B);
                    if (!f.inputs().linkAirborne()) owner.stunLink(s, 0x0E);
                } else if (owner.speedZ[s] < 0xF2) owner.sound(s, 0x20);
                owner.speedZ[s] = 0;
                owner.state[s] = 0;
                runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x20);
            }
            runtime.slots()[s] = e;
        }
        owner.variant[s] = 1;

        return true;
    }

    private boolean advanceState2() {
        owner.speedZ[s] = 0x60;
        e = owner.integrateZ(e);
        runtime.slots()[s] = e;
        SlimeEyeHalfMotion.Update result = SlimeEyeHalfMotion.rise(
                e.z(),
                runtime.physicsTables().readEnemyTransitionCountdown(s),
                owner.variant[s],
                owner.dropping[s],
                e.x(),
                e.y());
        owner.state[s] = result.state();
        runtime.physicsTables().writeEnemyTransitionCountdown(s, result.countdown());

        return true;
    }

    private boolean advanceState3() {
        SlimeEyeHalfMotion.Update result = SlimeEyeHalfMotion.waitAbove(
                runtime.physicsTables().readEnemyTransitionCountdown(s),
                owner.speedZ[s],
                owner.dropping[s],
                e.x(),
                e.y(),
                f.inputs().linkEntityX(),
                f.inputs().linkEntityY());
        owner.state[s] = result.state();
        owner.speedZ[s] = result.speedZ();
        owner.variant[s] = result.spriteVariant();
        owner.dropping[s] = result.privateState4();
        runtime.physicsTables().writeEnemyTransitionCountdown(s, result.countdown());
        runtime.slots()[s] = withPositionAndVariant(e, result.x(), result.y(), e.spriteVariant());
        if (result.jingle() >= 0) owner.sound(s, result.jingle());

        return true;
    }
}
