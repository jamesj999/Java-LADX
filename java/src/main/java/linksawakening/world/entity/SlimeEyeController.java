package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.SlimeEyeHalfMotion;

/** Source-ordered Slime Eye handler (04:496F–504B), including child slot ownership. */
public final class SlimeEyeController {
    private final SlimeEyeControllerServices runtime;
    private final int[] state = new int[16],
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
    private final boolean[] dying = new boolean[16];
    private Rom rom;
    private int screenShakeCountdown;

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

    private int read(RomAddress table, int index) {
        if (rom == null) throw new IllegalStateException("Slime Eye requires ROM encounter tables");
        return rom.u8(table.plus(index));
    }

    public void advance(EntityFrameExecution frame) {
        try {
            advanceHandler(frame);
        } finally {
            swordHitFrame[frame.entity.slot()] = 0;
        }
    }

    private void advanceHandler(EntityFrameExecution frame) {
        RoomEntity entity = frame.entity;
        int slot = entity.slot();
        if (entity.status() == EntityStatus.INIT) {
            clear(slot);
            runtime.combatTables().writeEnemyHealth(slot, 0x50);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
            runtime.slots()[slot] = withZ(withStatus(entity, EntityStatus.ACTIVE), 0x7E);
            render(slot);
            return;
        }
        if (entity.status() != EntityStatus.ACTIVE && entity.status() != EntityStatus.DYING) return;
        // UpdateEntityTimers precedes the handler's interactivity guards.
        if (private1[slot] > 0) private1[slot]--;
        if (splitting[slot] > 0) splitting[slot]--;
        if (!frame.roomInspection().roomTransitionActive()
                && !frame.roomInspection().freeMovementMode()) {
            int dx = 0, dy = 0;
            if (((frame.linkEntityX - 0x11) & 255) >= 0x7E) dx = frame.linkEntityX < 0x50 ? 1 : -1;
            else if (((frame.linkEntityY - 0x16) & 255) >= 0x5E) dy = -1;
            if (dx != 0 || dy != 0) {
                frame.emit(new LinkMotionBlockRequest(slot, dx, dy, 1));
                frame.linkEntityX = (frame.linkEntityX + dx) & 255;
                frame.linkEntityY = (frame.linkEntityY + dy) & 255;
                frame.lifecycle().delayInteractions(2);
            }
        }
        runtime.lifecycle().decrementSlowTransitionCountdown(slot, frame.frame);
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
                render(slot);
                return;
            }
        }
        // Main's combat pass precedes tick. Restore the fresh write that the
        // common flash tick would otherwise age before the ROM collision site.
        if (hitThisFrame(slot, frame.frame)) runtime.combatTables().writeEnemyFlashCountdown(slot, 0x18);
        // RenderActiveEntitySpritesRect runs before this frame's state changes.
        render(slot);
        if (runtime.physicsTables().readSlowTransitionCountdown(slot) != 0) {
            frame.emit(new LinkMotionBlockRequest(slot));
            frame.emit(new LinkFallenPoseRequest(slot));
        }
        if (entity.status() == EntityStatus.DYING) {
            // The lethal collision occurs inside the active handler in ROM.
            // Its destruction handler starts on the following slot visit.
            if (hitThisFrame(slot, frame.frame)) return;
            if (!dying[slot]) {
                state[slot] = 0;
                dying[slot] = true;
            }
            advanceDeath(frame);
            return;
        }
        if (half[slot] == 0) runtime.combatTables().writeEnemyHealth(slot, 0x50);
        if ((half[slot] != 0 || state[slot] != 0) && (!frame.handlerInteractive || frame.countdownBlocksInteraction))
            return;
        if (half[slot] == 0) advanceBody(frame);
        else advanceHalf(frame);
    }

    private void advanceBody(EntityFrameExecution f) {
        int s = f.entity.slot();
        RoomEntity e = runtime.slots()[s];
        runtime.combatTables().writeEnemyHealth(s, 0x50);
        switch (state[s]) {
            case 0 -> {
                if (screenShakeCountdown == 5) {
                    state[s] = 1;
                    sound(s, 8);
                }
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x50);
                    long zols = runtime.slotEntities().stream()
                            .filter(x -> x.loaded() && x.type() == 0x1B)
                            .count();
                    if (zols < 2)
                        spawn(
                                0x1B,
                                (f.randomByteSupplier.getAsInt() & 0x3F) + 0x40,
                                (f.randomByteSupplier.getAsInt() & 0x3F) + 0x30,
                                0x70);
                }
            }
            case 1 -> {
                e = integrateZ(e);
                if (speedZ[s] != 0xA0) speedZ[s] = (speedZ[s] - 2) & 255;
                if ((e.z() & 0x80) != 0) {
                    e = withZ(e, 0);
                    f.emit(new ScreenShakeRequest(s, 0x50, 4));
                    runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x40);
                    noise(s, 0x0C);
                    if (!f.linkAirborne) stunLink(s, 0x14);
                    state[s] = 2;
                }
                runtime.slots()[s] = e;
            }
            case 2 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) state[s] = 3;
            }
            case 3 -> {
                if (runtime.combatTables().readEnemyIgnoreHitsCountdown(s) > 0 && !hitThisFrame(s, f.frame))
                    runtime.combatTables().postEnemyIgnoreHitsCountdown(s, -1);
                if (splitting[s] != 0) {
                    if (splitting[s] == 1) {
                        split(e);
                        return;
                    }
                    f.emit(new LinkMotionBlockRequest(s, 0, (f.frame & 1) == 0 ? -1 : 0, splitting[s]));
                    variant[s] = 6;
                    return;
                }
                wander(f);
                variant[s] = (f.frame >> 4) & 1;
                if (stretch[s] != 0) {
                    variant[s] = stretch[s] + 1;
                    if (private1[s] == 0) {
                        private1[s] = 0x28;
                        stretch[s]--;
                    }
                }
                if (runtime.combatTables().readEnemyFlashCountdown(s) != 0 && (f.frame & 0x1F) == 0) {
                    private1[s] = 0x50;
                    if (stretch[s] != 4) stretch[s]++;
                }
            }
            default -> throw new IllegalStateException("Slime Eye body state " + state[s]);
        }
    }

    private void wander(EntityFrameExecution f) {
        int s = f.entity.slot(), countdown = runtime.physicsTables().readEnemyTransitionCountdown(s);
        RoomEntity e = runtime.slots()[s];
        if (moving[s] == 0) {
            if (countdown != 0) return;
            runtime.physicsTables().writeEnemyTransitionCountdown(s, (f.randomByteSupplier.getAsInt() & 0x1F) + 0x10);
            moving[s] = 1;
            int quadrant = (e.x() >= 0x50 ? 1 : 0) | (((e.y() - e.z()) & 255) >= 0x48 ? 2 : 0);
            int index = quadrant * 4 | (f.randomByteSupplier.getAsInt() & 3);
            speedX[s] = read(RomSymbols.Data_004_4B5C, index);
            speedY[s] = read(RomSymbols.Data_004_4B6C, index);
        } else if (countdown == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x30);
            moving[s] = 0;
        } else if ((countdown & 0x0E) == 0) runtime.slots()[s] = integrateXY(e, f.backgroundCollision);
    }

    private void advanceHalf(EntityFrameExecution f) {
        int s = f.entity.slot();
        RoomEntity e = runtime.slots()[s];
        if (runtime.combatTables().readEnemyIgnoreHitsCountdown(s) == 2) {
            if (angry[s] == 0) angry[s] = 1;
            else {
                state[s] = 2;
                speedX[s] = speedY[s] = 0;
                sound(s, 0x24);
            }
        }
        if (runtime.combatTables().readEnemyIgnoreHitsCountdown(s) != 0 && !hitThisFrame(s, f.frame)) {
            if (runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .isActive(s)) {
                e = runtime.backgroundMechanics()
                        .applyEnemyRecoilIfNeeded(e, f.backgroundCollision)
                        .entity();
                runtime.slots()[s] = e;
            } else runtime.combatTables().postEnemyIgnoreHitsCountdown(s, -1);
            // ApplyRecoilIfNeeded_04 discards the caller return address.
            // The half's state machine, including Z, does not run this frame.
            return;
        }
        switch (state[s]) {
            case 0 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    int index = f.randomByteSupplier.getAsInt() & 7;
                    speedX[s] = read(RomSymbols.Data_004_4E94, index);
                    speedY[s] = read(RomSymbols.Data_004_4E9C, index);
                    if ((f.randomByteSupplier.getAsInt() & 3) == 0) {
                        EntityMotionMath.Vector v =
                                EntityMotionMath.vectorAwayFromLink(e.x(), e.y(), f.linkEntityX, f.linkEntityY, 0x18);
                        speedX[s] = (-v.x()) & 255;
                        speedY[s] = (-v.y()) & 255;
                    }
                    speedZ[s] = (f.randomByteSupplier.getAsInt() & 15) + 8;
                    state[s] = 1;
                }
                variant[s] = 0;
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    e = integrateXY(e, f.backgroundCollision);
                    e = integrateZ(e);
                    speedZ[s] = (speedZ[s] - 2) & 255;
                    if ((e.z() & 0x80) != 0) {
                        e = withZ(e, 0);
                        dropping[s] = 0;
                        if ((((speedZ[s] - 0xE0) & 255) & 0x80) != 0) {
                            f.emit(new ScreenShakeRequest(s, 0x18, 0));
                            sound(s, 0x0B);
                            if (!f.linkAirborne) stunLink(s, 0x0E);
                        } else if (speedZ[s] < 0xF2) sound(s, 0x20);
                        speedZ[s] = 0;
                        state[s] = 0;
                        runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x20);
                    }
                    runtime.slots()[s] = e;
                }
                variant[s] = 1;
            }
            case 2 -> {
                speedZ[s] = 0x60;
                e = integrateZ(e);
                runtime.slots()[s] = e;
                SlimeEyeHalfMotion.Update result = SlimeEyeHalfMotion.rise(
                        e.z(),
                        runtime.physicsTables().readEnemyTransitionCountdown(s),
                        variant[s],
                        dropping[s],
                        e.x(),
                        e.y());
                state[s] = result.state();
                runtime.physicsTables().writeEnemyTransitionCountdown(s, result.countdown());
            }
            case 3 -> {
                SlimeEyeHalfMotion.Update result = SlimeEyeHalfMotion.waitAbove(
                        runtime.physicsTables().readEnemyTransitionCountdown(s),
                        speedZ[s],
                        dropping[s],
                        e.x(),
                        e.y(),
                        f.linkEntityX,
                        f.linkEntityY);
                state[s] = result.state();
                speedZ[s] = result.speedZ();
                variant[s] = result.spriteVariant();
                dropping[s] = result.privateState4();
                runtime.physicsTables().writeEnemyTransitionCountdown(s, result.countdown());
                runtime.slots()[s] = withPositionAndVariant(e, result.x(), result.y(), e.spriteVariant());
                if (result.jingle() >= 0) sound(s, result.jingle());
            }
            default -> throw new IllegalStateException("Slime Eye half state " + state[s]);
        }
    }

    private boolean hitThisFrame(int slot, int frame) {
        return swordHitFrame[slot] == (frame & 255) + 1;
    }

    private void stunLink(int s, int countdown) {
        runtime.physicsTables().writeSlowTransitionCountdown(s, countdown);
        runtime.physicsTables().writeSlowTimerInitialized(s, true);
    }

    private RoomEntity integrateZ(RoomEntity e) {
        int s = e.slot(), value = accZ[s] + (byte) speedZ[s];
        accZ[s] = value & 15;
        return withZ(e, (e.z() + (value >> 4)) & 255);
    }

    private RoomEntity integrateXY(RoomEntity e, RoomEntityBackgroundCollision collision) {
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

    private int spawn(int type, int x, int y, int z) {
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

    private void split(RoomEntity e) {
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

    private void advanceDeath(EntityFrameExecution f) {
        int s = f.entity.slot();
        runtime.combatTables().writeEnemyFlashCountdown(s, f.frame & 255);
        switch (state[s]) {
            case 0 -> {
                runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x40);
                state[s] = 1;
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(s, 0xA0);
                    state[s] = 2;
                }
            }
            default -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    boolean another = runtime.slotEntities().stream()
                            .anyMatch(e -> e.loaded() && e.slot() != s && e.type() == 0x5B);
                    if (!another) {
                        runtime.bossRewards().spawnMoldormHeartContainer(f.entity);
                        noise(s, 0x1A);
                    }
                    runtime.lifecycle().disableEntityWithoutPersistence(s);
                } else if ((runtime.physicsTables().readEnemyTransitionCountdown(s) & 7) == 0) {
                    int x = (f.entity.x() + (f.randomByteSupplier.getAsInt() & 31) - 16) & 255;
                    int y = (f.entity.y() - f.entity.z() + (f.randomByteSupplier.getAsInt() & 31) - 16) & 255;
                    runtime.bossRewards().createMoldormPoof(f.entity, x, y);
                }
            }
        }
    }

    private void render(int s) {
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

    private void sound(int s, int id) {
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(s, 0x5B, 0, false, EntityCombatEvent.SoundChannel.JINGLE, id));
    }

    private void noise(int s, int id) {
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(s, 0x5B, 0, false, EntityCombatEvent.SoundChannel.NOISE, id));
    }

    void resolveCombat(
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
            EnemyAttackContext attack,
            List<EntityCombatEvent> events) {
        int s = e.slot();
        if (!interactive
                || e.status() != EntityStatus.ACTIVE
                || runtime.combatTables().readEnemyIgnoreHitsCountdown(s) != 0) return;
        boolean isHalf = half[s] != 0;
        if ((!isHalf && state[s] != 3)
                || (isHalf
                        && (state[s] >= 2
                                || (state[s] == 1 && runtime.physicsTables().readEnemyTransitionCountdown(s) != 0))))
            return;
        if (splitting[s] != 0
                || runtime.combatTables().readEnemyFlashCountdown(s) > 0
                        && runtime.combatTables().readEnemyFlashCountdown(s) < 0x18) return;
        int hitbox = isHalf ? 24 : 20;
        boolean hit = sword && overlaps(e, hitbox, sx, sw, sy, sh);
        if (hit) {
            if (!isHalf && stretch[s] == 4 && attack.pegasusBoots()) {
                splitting[s] = 0x0C;
                return;
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
            var damage = runtime.enemyCombatTables() == null
                    ? null
                    : runtime.enemyCombatTables()
                            .resolveSwordDamage(0x5B, runtime.combatTables().readEnemyHealthGroup(s), damageAttack);
            int amount = damage == null ? 1 : damage.numericDamage();
            if (amount == 0) return;
            swordHitFrame[s] = (frame & 255) + 1;
            runtime.combatTables()
                    .writeEnemyHealth(s, Math.max(0, runtime.combatTables().readEnemyHealth(s) - amount));
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(s, attack.powerRecoil() ? 0x20 : 0x0A);
            runtime.combatTables().writeEnemyFlashCountdown(s, 0x18);
            if (isHalf)
                runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .configure(s, e.x(), e.y(), e.z(), lx, ly, 0x30);
            if (isHalf && runtime.combatTables().readEnemyHealth(s) == 0) {
                state[s] = 0;
                runtime.slots()[s] = withStatus(e, EntityStatus.DYING);
            }
            // Bank 03:7081–70B6 applies the Slime Eye sword response to both halves
            // as well as the unsplit body. Pegasus hits call 6DDF (reset boots,
            // ignore collisions $10, push $14); normal unstretched hits push $10
            // and ignore collisions for four frames. A fully stretched normal
            // strike takes the ordinary damage branch without this push.
            EntityCombatEvent.LinkCollisionResponse response = EntityCombatEvent.LinkCollisionResponse.NONE;
            if (isHalf || stretch[s] != 4) {
                var vector = linksawakening.world.entity.motion.EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        e.x(), e.y(), e.z(), lx, ly, attack.pegasusBoots() ? 0x14 : 0x10);
                response = new EntityCombatEvent.LinkCollisionResponse(
                        vector.x() & 255, vector.y() & 255, attack.pegasusBoots() ? 0x10 : 4);
            }
            events.add(new EntityCombatEvent(
                    s,
                    0x5B,
                    0,
                    true,
                    amount,
                    -1,
                    EntityCombatEvent.SoundChannel.JINGLE,
                    3,
                    EntityCombatEvent.SoundChannel.NONE,
                    -1,
                    null,
                    response));
        } else if (!airborne
                && !(isHalf && dropping[s] != 0)
                && RoomEntityCombatRules.collisionCadenceMatches(frame, s)
                && overlaps(e, isHalf ? 24 : stretch[s] * 4, lx + 8, 4, ly + 8, 4)) {
            int damage = runtime.enemyCombatTables() == null
                    ? 4
                    : runtime.enemyCombatTables()
                            .contactDamageForGroup(runtime.combatTables().readEnemyHealthGroup(s));
            var vector = linksawakening.world.entity.motion.EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    e.x(), e.y(), e.z(), lx, ly, 0x14);
            events.add(new EntityCombatEvent(
                    s,
                    0x5B,
                    damage,
                    false,
                    0,
                    -1,
                    EntityCombatEvent.SoundChannel.NONE,
                    -1,
                    EntityCombatEvent.SoundChannel.NONE,
                    -1,
                    null,
                    new EntityCombatEvent.LinkCollisionResponse(vector.x() & 255, vector.y() & 255, 0x10)));
        }
    }

    private boolean overlaps(RoomEntity e, int box, int x, int w, int y, int h) {
        if (w <= 0 || h <= 0) return false;
        int dx = Math.abs((byte) (e.x() + read(RomSymbols.Data_004_4B12, box) - x));
        int dy = Math.abs((byte) (e.y() - e.z() + read(RomSymbols.Data_004_4B12, box + 2) - y));
        return dx < read(RomSymbols.Data_004_4B12, box + 1) + w && dy < read(RomSymbols.Data_004_4B12, box + 3) + h;
    }
}
