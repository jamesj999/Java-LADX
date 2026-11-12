package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Numeric sword damage and recoil preceding the status transition. */
final class EntitySwordDamage {
    static void apply(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.swordResult = runtime.enemyCombatTables() == null
                ? null
                : runtime.enemyCombatTables()
                        .resolveSwordDamage(
                                slot.entity.type(),
                                runtime.combatTables().readEnemyHealthGroup(slot.entity.slot()),
                                frame.attackContext());
        slot.swordDamage = slot.swordResult == null
                ? RoomEntityCombatRules.basicSwordDamage(slot.entity.type())
                : slot.swordResult.numericDamage();
        slot.enemyDamage = slot.swordDamage;
        if (slot.swordResult != null) {
            slot.enemySpecialAction = slot.swordResult.specialAction();
        }
        slot.swordResultApplied = slot.swordResult == null ? slot.swordDamage > 0 : !slot.swordResult.ignored();
        if (usesSharedRecoil(slot.entity.type()) || slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // EnemyCollidedWithSword applies the default `$30` recoil
            // before ApplySwordDamagesToEnemy changes health.
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .configure(
                            slot.entity.slot(),
                            slot.entity.x(),
                            slot.entity.y(),
                            slot.entity.z(),
                            frame.linkEntityX(),
                            frame.linkEntityY(),
                            0x30);
        }
        // ConfigureEntityRecoil reaches StartIgnoringHitsForEntity
        // before ApplySwordDamagesToEnemy, including lethal hits.
        runtime.combatTables()
                .writeEnemyIgnoreHitsCountdown(
                        slot.entity.slot(), frame.attackContext().powerRecoil() ? 0x20 : 0x0A);
        if (slot.swordDamage > 0) {
            // $06:6CEB checks existing recoil before $6CEE applies a new hit.
            // Main resolves combat before ticking handlers, so preserve that phase.
            if (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES)
                runtime.motions()
                        .get(RollingBonesMotion.class, RollingBonesMotion::new)
                        .setSwordHitFrame(slot.entity.slot(), (frame.frameCounter() & 0xFF) + 1);
            runtime.combatTables()
                    .writeEnemyHealth(
                            slot.entity.slot(),
                            Math.max(0, runtime.combatTables().readEnemyHealth(slot.entity.slot()) - slot.swordDamage));
            if (slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
                runtime.motions().get(MoldormMotion.class, MoldormMotion::new).onSwordHit(slot.entity.slot());
                runtime.combatTables().writeEnemyFlashCountdown(slot.entity.slot(), 0x28);
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0);
            }
        }
        slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
        slot.soundId = slot.swordResultApplied ? 0x03 : 0x09;
        EntitySwordStatus.apply(slot);
    }
}
