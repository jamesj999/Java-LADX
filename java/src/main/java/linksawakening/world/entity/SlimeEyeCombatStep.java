package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Per-invocation scratch for SlimeEyeCombat.resolveCombat; values are assigned in source order. */
final class SlimeEyeCombatStep {
    private linksawakening.world.entity.SlimeEyeController owner;
    private linksawakening.world.entity.SlimeEyeCombatFrame inputs;
    private linksawakening.world.entity.SlimeEyeControllerServices runtime;
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
    private linksawakening.world.entity.EnemyAttackContext attack;
    private java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events;
    private int s;
    private boolean isHalf;
    private int hitbox;
    private boolean hit;

    SlimeEyeCombatStep(
            linksawakening.world.entity.SlimeEyeController owner,
            linksawakening.world.entity.SlimeEyeCombatFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    void run() {
        if (!prepareCollision()) return;
        if (!applySwordHit()) return;
    }

    private boolean prepareCollision() {

        runtime = owner.runtime;
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
                || runtime.combatTables().readEnemyIgnoreHitsCountdown(s) != 0) return false;
        isHalf = owner.half[s] != 0;
        if ((!isHalf && owner.state[s] != 3)
                || (isHalf
                        && (owner.state[s] >= 2
                                || (owner.state[s] == 1
                                        && runtime.physicsTables().readEnemyTransitionCountdown(s) != 0))))
            return false;
        if (owner.splitting[s] != 0
                || runtime.combatTables().readEnemyFlashCountdown(s) > 0
                        && runtime.combatTables().readEnemyFlashCountdown(s) < 0x18) return false;
        hitbox = isHalf ? 24 : 20;
        hit = sword && owner.overlaps(e, hitbox, sx, sw, sy, sh);
        return true;
    }

    private boolean applySwordHit() {

        if (hit) {
            return applyDamage();
        } else if (!airborne
                && !(isHalf && owner.dropping[s] != 0)
                && RoomEntityCombatRules.collisionCadenceMatches(frame, s)
                && owner.overlaps(e, isHalf ? 24 : owner.stretch[s] * 4, lx + 8, 4, ly + 8, 4)) {
            int damage = runtime.enemyCombatTables() == null
                    ? 4
                    : runtime.enemyCombatTables()
                            .contactDamageForGroup(runtime.combatTables().readEnemyHealthGroup(s));
            var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(e.x(), e.y(), e.z(), lx, ly, 0x14);
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
        return true;
    }

    private boolean applyDamage() {
        if (!isHalf && owner.stretch[s] == 4 && attack.pegasusBoots()) {
            owner.splitting[s] = 0x0C;
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
        var damage = runtime.enemyCombatTables() == null
                ? null
                : runtime.enemyCombatTables()
                        .resolveSwordDamage(0x5B, runtime.combatTables().readEnemyHealthGroup(s), damageAttack);
        int amount = damage == null ? 1 : damage.numericDamage();
        if (amount == 0) return false;
        owner.swordHitFrame[s] = (frame & 255) + 1;
        runtime.combatTables()
                .writeEnemyHealth(s, Math.max(0, runtime.combatTables().readEnemyHealth(s) - amount));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(s, attack.powerRecoil() ? 0x20 : 0x0A);
        runtime.combatTables().writeEnemyFlashCountdown(s, 0x18);
        if (isHalf)
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .configure(s, e.x(), e.y(), e.z(), lx, ly, 0x30);
        if (isHalf && runtime.combatTables().readEnemyHealth(s) == 0) {
            owner.state[s] = 0;
            runtime.slots()[s] = withStatus(e, EntityStatus.DYING);
        }
        // Bank 03:7081–70B6 applies the Slime Eye sword response to both halves
        // as well as the unsplit body. Pegasus hits call 6DDF (reset boots,
        // ignore collisions $10, push $14); normal unstretched hits push $10
        // and ignore collisions for four frames. A fully stretched normal
        // strike takes the ordinary damage branch without this push.
        EntityCombatEvent.LinkCollisionResponse response = EntityCombatEvent.LinkCollisionResponse.NONE;
        if (isHalf || owner.stretch[s] != 4) {
            var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
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

        return true;
    }
}
