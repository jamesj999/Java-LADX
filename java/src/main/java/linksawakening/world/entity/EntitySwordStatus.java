package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Status effects after numeric sword damage, in ROM branch order. */
final class EntitySwordStatus {
    static void apply(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;
        if (slot.enemySpecialAction == 0xFE) {
            burn(slot);
        } else if (slot.enemySpecialAction == 0xFF) {
            stun(slot);
        } else if (slot.swordDamage > 0 && runtime.combatTables().readEnemyHealth(slot.entity.slot()) == 0) {
            die(slot);
        } else if (slot.swordDamage > 0) {
            flashSurvivor(slot);
        }
    }

    private static void burn(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // ApplySwordDamagesToEnemy's burning branch starts the
        // shared $60 transition and emits NOISE_SFX_BURSTING_FLAME.
        runtime.physicsTables().writeEnemyTransitionCountdown(slot.entity.slot(), 0x60);
        runtime.combatTables().writeEnemyStunnedCountdown(slot.entity.slot(), 0);
        runtime.combatTables().writeEnemyFlashCountdown(slot.entity.slot(), 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x0A);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot.entity.slot());
        runtime.slots()[slot.entity.slot()] = withStatus(slot.entity, EntityStatus.BURNING);
        slot.secondarySoundChannel = EntityCombatEvent.SoundChannel.NOISE;
        slot.secondarySoundId = 0x12;
    }

    private static void stun(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // EntityBecomeStunned uses private countdown 2 and clears
        // vertical speed; the runtime has no separate Z velocity
        // for this status, so the countdown is the authoritative
        // stun state here.
        runtime.physicsTables().writeEnemyTransitionCountdown(slot.entity.slot(), 0);
        runtime.combatTables().writeEnemyStunnedCountdown(slot.entity.slot(), 0xFF);
        runtime.combatTables().writeEnemyFlashCountdown(slot.entity.slot(), 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x0A);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot.entity.slot());
        runtime.slots()[slot.entity.slot()] = withStatus(slot.entity, EntityStatus.STUNNED);
    }

    private static void die(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // ApplySwordDamagesToEnemy falls through on death to
        // $03:73B6-73E4. Moldorm already received $28/$C8 above;
        // ordinary lethal hits also flash for $18. The recoil
        // caller retains its $20 ignore/$40 death override.
        // Final Nightmare ($E6), form 3, also uses $28/$C8
        // ($03:73BF-73C8); wire this when its form handler exists.
        if (slot.entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            runtime.combatTables().writeEnemyFlashCountdown(slot.entity.slot(), 0x18);
        }
        runtime.combatTables()
                .writeDyingCountdown(
                        slot.entity.slot(),
                        slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM
                                ? 0
                                : (frame.attackContext().powerRecoil() ? 0x40 : 0x2F));
        if (slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            runtime.bossRewards().writeBossDeathProducerState(slot.entity.slot(), 0);
            runtime.motions()
                    .get(MoblinKingMotion.class, MoblinKingMotion::new)
                    .setLethalHitFrame(slot.entity.slot(), (frame.frameCounter() & 255) + 1);
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
            runtime.bossRewards().writeBossDeathProducerState(slot.entity.slot(), 0);
            runtime.motions()
                    .get(MoldormMotion.class, MoldormMotion::new)
                    .setDestructionTailState(slot.entity.slot(), 0);
        }
        if (slot.entity.type() == 0x12) {
            runtime.bossRewards()
                    .cascadeGhiniDeath(
                            slot.entity,
                            runtime.frameContext(frame.frameCounter()).randomByteSupplier());
        }
        runtime.combatTables()
                .writePowerRecoilDeath(slot.entity.slot(), frame.attackContext().powerRecoil());
        runtime.slots()[slot.entity.slot()] = withDeathPresentation(
                withStatus(slot.entity, EntityStatus.DYING),
                -1,
                runtime.combatTables().readPowerRecoilDeath(slot.entity.slot()));
    }

    private static void flashSurvivor(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // jr_003_73B6 and StartIgnoringHitsForEntity: a normal
        // sword hit flashes for $18 frames and suppresses the
        // next $0A collision passes.
        if (slot.entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            runtime.combatTables().writeEnemyFlashCountdown(slot.entity.slot(), 0x18);
        }
        runtime.combatTables()
                .writeEnemyIgnoreHitsCountdown(
                        slot.entity.slot(),
                        slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM
                                ? 0
                                : frame.attackContext().powerRecoil() ? 0x20 : 0x0A);
        if (isZolGelType(slot.entity.type())) {
            runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).onSwordHit(slot.entity.slot());
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_ZOL && runtime.spriteHandlers() != null) {
            slot.entity = withDefinition(
                    slot.entity, runtime.slotProperties().spriteViews().forZolSlimeEye(), slot.entity.spriteVariant());
            runtime.slots()[slot.entity.slot()] = slot.entity;
        }
    }
}
