package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** Per-invocation scratch for PlayerProjectileDamage.applyPlayerProjectileDamage; values are assigned in source order. */
final class PlayerProjectileDamageFrameStep {
    private linksawakening.world.entity.EntityProjectileController owner;
    private linksawakening.world.entity.RoomEntity target;
    private int damageType;
    private int speedX;
    private int speedY;
    private linksawakening.world.entity.EntityProjectileControllerServices runtime;
    private int targetSlot;
    private linksawakening.world.entity.RomEnemyCombatTables.SwordDamageResult damageResult;
    private int rawDamage;
    private int enemyDamage;
    private int specialAction;
    private linksawakening.world.entity.event.EntityCombatEvent.SoundChannel secondaryChannel;
    private int secondarySoundId;

    PlayerProjectileDamageFrameStep(
            linksawakening.world.entity.EntityProjectileController owner,
            linksawakening.world.entity.RoomEntity target,
            int damageType,
            int speedX,
            int speedY) {
        this.owner = owner;
        this.target = target;
        this.damageType = damageType;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    void run() {
        if (!prepareDamage()) return;
        if (!applyDamageOrStatus()) return;
        if (!emitDamageEvent()) return;
    }

    private boolean prepareDamage() {

        runtime = owner.runtime;
        targetSlot = target.slot();
        runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configureFromSpeed(targetSlot, speedX, speedY);

        damageResult = runtime.enemyCombatTables() == null
                ? null
                : runtime.enemyCombatTables()
                        .resolveAttackDamage(
                                target.type(), runtime.combatTables().readEnemyHealthGroup(target.slot()), damageType);
        rawDamage =
                damageResult == null ? RoomEntityCombatRules.basicSwordDamage(target.type()) : damageResult.rawValue();
        if (rawDamage == 0) {
            return false;
        }

        enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
        specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
        secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
        secondarySoundId = -1;
        return true;
    }

    private boolean applyDamageOrStatus() {

        if (rawDamage == 0xFE) {
            runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x60);
            runtime.combatTables().writeEnemyStunnedCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
            runtime.physicsTables()
                    .writeEnemyPhysicsFlags(
                            targetSlot, (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot) + 2) & 0xFF);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(targetSlot);
            runtime.slots()[targetSlot] = withStatus(target, EntityStatus.BURNING);
            secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
            secondarySoundId = 0x12;
        } else if (rawDamage == 0xFF) {
            runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyStunnedCountdown(targetSlot, 0xFF);
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(targetSlot);
            runtime.slots()[targetSlot] = withStatus(target, EntityStatus.STUNNED);
        } else if (rawDamage == 0xFD) {
            // The fairy-conversion branch is entity-specific in the ROM. The
            // generic table result is still surfaced so the pending special
            // action is not silently changed into numeric damage.
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(targetSlot);
        } else {
            runtime.combatTables()
                    .writeEnemyHealth(
                            targetSlot, Math.max(0, runtime.combatTables().readEnemyHealth(targetSlot) - enemyDamage));
            if (runtime.combatTables().readEnemyHealth(targetSlot) == 0) {
                runtime.combatTables().writeDyingCountdown(targetSlot, 0x2F);
                runtime.combatTables().writePowerRecoilDeath(targetSlot, false);
                runtime.slots()[targetSlot] = withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false);
            } else if (isZolGelType(target.type())) {
                runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).onSwordHit(targetSlot);
                if (target.type() == EntityRomConstants.ENTITY_ZOL && runtime.spriteHandlers() != null) {
                    runtime.slots()[targetSlot] =
                            withDefinition(target, runtime.spriteHandlers().forZolSlimeEye(), target.spriteVariant());
                }
            }
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x18);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
        }
        return true;
    }

    private boolean emitDamageEvent() {

        runtime.events()
                .pendingEntityEvents
                .add(linksawakening.world.entity.event.EntityCombatEvent.builder(targetSlot, target.type())
                        .linkDamage(0)
                        .swordHit(false)
                        .enemyDamage(enemyDamage)
                        .enemySpecialAction(specialAction)
                        .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                        .soundId(0x03)
                        .secondarySoundChannel(secondaryChannel)
                        .secondarySoundId(secondarySoundId)
                        .swordPokeVfx(null)
                        .build());
        return true;
    }
}
