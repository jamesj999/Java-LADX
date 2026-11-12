package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.SlimeEyeHalfMotion;

/** Focused behavior using the state owned by SlimeEyeController. */
final class SlimeEyeBehavior {
    static void advanceHandler(SlimeEyeController owner, EntityFrameExecution frame) {
        var runtime = owner.runtime;
        RoomEntity entity = frame.slotFrame().entity();
        int slot = entity.slot();
        if (entity.status() == EntityStatus.INIT) {
            owner.clear(slot);
            runtime.combatTables().writeEnemyHealth(slot, 0x50);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
            runtime.slots()[slot] = withZ(withStatus(entity, EntityStatus.ACTIVE), 0x7E);
            owner.render(slot);
            return;
        }
        if (entity.status() != EntityStatus.ACTIVE && entity.status() != EntityStatus.DYING) return;
        // UpdateEntityTimers precedes the handler's interactivity guards.
        if (owner.private1[slot] > 0) owner.private1[slot]--;
        if (owner.splitting[slot] > 0) owner.splitting[slot]--;
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
        runtime.lifecycle()
                .decrementSlowTransitionCountdown(slot, frame.inputs().frame());
        if (!frame.link().inventoryAppearing() && !frame.roomInspection().roomTransitionActive()) {
            BossIntroMotion.Update intro = runtime.motions()
                    .get(BossIntroMotion.class, BossIntroMotion::new)
                    .advance(
                            runtime.slotProperties().options1(slot),
                            entity.type(),
                            runtime.roomVariables().readEntityMapId(),
                            runtime.lifecycle().transitionSequenceCounter());
            if (intro.musicTrack() >= 0) frame.requestMusic(intro.musicTrack());
            if (intro.dialogLowId() >= 0) {
                frame.emit(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                owner.render(slot);
                return;
            }
        }
        // Main's combat pass precedes tick. Restore the fresh write that the
        // common flash tick would otherwise age before the ROM collision site.
        if (owner.hitThisFrame(slot, frame.inputs().frame()))
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0x18);
        // RenderActiveEntitySpritesRect runs before this frame's state changes.
        owner.render(slot);
        if (runtime.physicsTables().readSlowTransitionCountdown(slot) != 0) {
            frame.emit(new LinkMotionBlockRequest(slot));
            frame.emit(new LinkFallenPoseRequest(slot));
        }
        if (entity.status() == EntityStatus.DYING) {
            // The lethal collision occurs inside the active handler in ROM.
            // Its destruction handler starts on the following slot visit.
            if (owner.hitThisFrame(slot, frame.inputs().frame())) return;
            if (!owner.dying[slot]) {
                owner.state[slot] = 0;
                owner.dying[slot] = true;
            }
            owner.advanceDeath(frame);
            return;
        }
        if (owner.half[slot] == 0) runtime.combatTables().writeEnemyHealth(slot, 0x50);
        if ((owner.half[slot] != 0 || owner.state[slot] != 0)
                && (!frame.gates().handlerInteractive() || frame.gates().countdownBlocksInteraction())) return;
        if (owner.half[slot] == 0) owner.advanceBody(frame);
        else owner.advanceHalf(frame);
    }

    static void advanceBody(SlimeEyeController owner, EntityFrameExecution f) {
        var runtime = owner.runtime;
        int s = f.slotFrame().entity().slot();
        RoomEntity e = runtime.slots()[s];
        runtime.combatTables().writeEnemyHealth(s, 0x50);
        switch (owner.state[s]) {
            case 0 -> {
                if (owner.screenShakeCountdown == 5) {
                    owner.state[s] = 1;
                    owner.sound(s, 8);
                }
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x50);
                    long zols = runtime.slotEntities().stream()
                            .filter(x -> x.loaded() && x.type() == 0x1B)
                            .count();
                    if (zols < 2)
                        owner.spawn(
                                0x1B,
                                (f.inputs().randomByteSupplier().getAsInt() & 0x3F) + 0x40,
                                (f.inputs().randomByteSupplier().getAsInt() & 0x3F) + 0x30,
                                0x70);
                }
            }
            case 1 -> {
                e = owner.integrateZ(e);
                if (owner.speedZ[s] != 0xA0) owner.speedZ[s] = (owner.speedZ[s] - 2) & 255;
                if ((e.z() & 0x80) != 0) {
                    e = withZ(e, 0);
                    f.emit(new ScreenShakeRequest(s, 0x50, 4));
                    runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x40);
                    owner.noise(s, 0x0C);
                    if (!f.inputs().linkAirborne()) owner.stunLink(s, 0x14);
                    owner.state[s] = 2;
                }
                runtime.slots()[s] = e;
            }
            case 2 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) owner.state[s] = 3;
            }
            case 3 -> {
                if (runtime.combatTables().readEnemyIgnoreHitsCountdown(s) > 0
                        && !owner.hitThisFrame(s, f.inputs().frame()))
                    runtime.combatTables().postEnemyIgnoreHitsCountdown(s, -1);
                if (owner.splitting[s] != 0) {
                    if (owner.splitting[s] == 1) {
                        owner.split(e);
                        return;
                    }
                    f.emit(new LinkMotionBlockRequest(
                            s, 0, (f.inputs().frame() & 1) == 0 ? -1 : 0, owner.splitting[s]));
                    owner.variant[s] = 6;
                    return;
                }
                owner.wander(f);
                owner.variant[s] = (f.inputs().frame() >> 4) & 1;
                if (owner.stretch[s] != 0) {
                    owner.variant[s] = owner.stretch[s] + 1;
                    if (owner.private1[s] == 0) {
                        owner.private1[s] = 0x28;
                        owner.stretch[s]--;
                    }
                }
                if (runtime.combatTables().readEnemyFlashCountdown(s) != 0
                        && (f.inputs().frame() & 0x1F) == 0) {
                    owner.private1[s] = 0x50;
                    if (owner.stretch[s] != 4) owner.stretch[s]++;
                }
            }
            default -> throw new IllegalStateException("Slime Eye body state " + owner.state[s]);
        }
    }

    static void advanceHalf(SlimeEyeController owner, EntityFrameExecution f) {
        var runtime = owner.runtime;
        int s = f.slotFrame().entity().slot();
        RoomEntity e = runtime.slots()[s];
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
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    int index = f.inputs().randomByteSupplier().getAsInt() & 7;
                    owner.speedX[s] = owner.read(RomSymbols.Data_004_4E94, index);
                    owner.speedY[s] = owner.read(RomSymbols.Data_004_4E9C, index);
                    if ((f.inputs().randomByteSupplier().getAsInt() & 3) == 0) {
                        EntityMotionMath.Vector v = EntityMotionMath.vectorAwayFromLink(
                                e.x(),
                                e.y(),
                                f.inputs().linkEntityX(),
                                f.inputs().linkEntityY(),
                                0x18);
                        owner.speedX[s] = (-v.x()) & 255;
                        owner.speedY[s] = (-v.y()) & 255;
                    }
                    owner.speedZ[s] = (f.inputs().randomByteSupplier().getAsInt() & 15) + 8;
                    owner.state[s] = 1;
                }
                owner.variant[s] = 0;
            }
            case 1 -> {
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
            }
            case 2 -> {
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
            }
            case 3 -> {
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
            }
            default -> throw new IllegalStateException("Slime Eye half state " + owner.state[s]);
        }
    }

    static int spawn(SlimeEyeController owner, int type, int x, int y, int z) {
        var runtime = owner.runtime;
        int s = runtime.findFreeEntitySlot();
        if (s < 0) return -1;
        runtime.lifecycle().disableEntityWithoutPersistence(s);
        var definition = type == 0x1B && runtime.spriteHandlers() != null
                ? runtime.spriteHandlers().forZolSlimeEye()
                : runtime.slotProperties().spriteDefinitionFor(type);
        runtime.slots()[s] = new RoomEntity(
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
        runtime.initializeHealthGroup(s);
        runtime.combatTables().writeEnemyHealth(s, runtime.initialHealth(type));
        runtime.physicsTables().writeEnemyPhysicsFlags(s, EntityTypeRules.initialPhysicsFlags(type));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(s, 1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(s, true);
        return s;
    }
}
