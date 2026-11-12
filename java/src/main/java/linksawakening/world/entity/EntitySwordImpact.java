package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Ordered swordimpact operations for a single slot. */
final class EntitySwordImpact {
    static boolean prepare(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.soundChannel = EntityCombatEvent.SoundChannel.NONE;
        slot.soundId = -1;
        slot.secondarySoundChannel = EntityCombatEvent.SoundChannel.NONE;
        slot.secondarySoundId = -1;
        slot.enemyDamage = 0;
        slot.enemySpecialAction = -1;
        slot.swordPokeVfx = null;
        slot.peaHatSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_PEAHAT && !slot.peaHatGrounded;
        slot.spikedBeetleSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                && (runtime.options1(slot.entity.slot()) & 0x40) != 0;
        slot.maskedMimicSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA
                && (runtime.options1(slot.entity.slot()) & 0x40) != 0;
        slot.rollingBonesBarSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR;
        slot.moblinKingSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING
                && runtime.motions()
                                .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                .state(slot.entity.slot())
                        != 5;
        return true;
    }

    static boolean apply(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.entity.type() == EntityRomConstants.ENTITY_CRYSTAL_SWITCH) {
            applyCrystalSwitch(slot);
        } else if (slot.swordHit
                && slot.entity.type() == EntityRomConstants.ENTITY_IRON_MASK
                && runtime.motions()
                                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                .maskRemovalState(slot.entity.slot())
                        == 0
                && runtime.linkState().lastRomLinkDirection
                        != runtime.motions()
                                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                .direction(slot.entity.slot())) {
            applyMaskedIronMask(slot);
        } else if (slot.swordHit
                && RoomEntityCombatRules.swordPokeForSwordCollision(
                        slot.entity.type(),
                        slot.peaHatSwordClink
                                || slot.spikedBeetleSwordClink
                                || slot.maskedMimicSwordClink
                                || slot.rollingBonesBarSwordClink
                                || slot.moblinKingSwordClink)
                && !slot.moldormTailSwordHit) {
            applySwordClink(slot);
        } else if (slot.swordHit && slot.entity.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE) {
            lightBombite(slot);
        } else if (slot.swordHit) {
            EntitySwordDamage.apply(slot);
        }
        return true;
    }

    private static void applyCrystalSwitch(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.swordHit) {
            // The crystal handler uses the normal enemy-hit path for
            // the flash/ignore timers, but its preceding health write
            // prevents the generic damage path from killing it.
            runtime.combatTables()
                    .writeEnemyIgnoreHitsCountdown(
                            slot.entity.slot(), frame.attackContext().powerRecoil() ? 0x20 : 0x0A);
            runtime.combatTables().writeEnemyFlashCountdown(slot.entity.slot(), 0x18);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(slot.entity.slot());
            slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
            slot.soundId = 0x03;
        }
    }

    private static void applyMaskedIronMask(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // DefaultEnemyDamageCollisionHandler's masked Iron Mask branch
        // only accepts a sword hit from the direction the mask faces.
        // A rear hit pushes Link and the mask apart, then uses the
        // shared sword-poke presentation without applying damage.
        slot.swordPokeVfx =
                new EntityCombatEvent.SwordPokeVfx(byteValue(frame.swordX() - 0x08), byteValue(frame.swordY() - 0x08));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x10);
        runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configure(
                        slot.entity.slot(),
                        slot.entity.x(),
                        slot.entity.y(),
                        slot.entity.z(),
                        frame.linkEntityX(),
                        frame.linkEntityY(),
                        0x10);
        slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
        slot.soundId = 0x07;
    }

    private static void applySwordClink(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // EnemyCollidedWithSword's ENTITY_OPT1_SWORD_CLINK_OFF path
        // calls label_D07/label_D15: no damage or normal recoil,
        // sixteen ignored-hit frames, then the sword-poke VFX and
        // jingle writes.
        slot.swordPokeVfx =
                new EntityCombatEvent.SwordPokeVfx(byteValue(frame.swordX() - 0x08), byteValue(frame.swordY() - 0x08));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x10);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot.entity.slot());
        slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
        slot.soundId = 0x07;
    }

    private static void lightBombite(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // EnemyCollidedWithSword's Bombite branch is not a damage hit:
        // it enters state-$02, reverses the vector returned by
        // GetVectorTowardsLink, and starts the private lit window.
        runtime.motions()
                .get(BombiteMotion.class, BombiteMotion::new)
                .enterBouncingLitFromSword(slot.entity.slot(), slot.entity, frame.linkEntityX(), frame.linkEntityY());
        runtime.physicsTables().writeEnemyTransitionCountdown(slot.entity.slot(), 0x40);
        runtime.bombInteractions().writeBombPrivateCountdown1(slot.entity.slot(), 0x08);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot.entity.slot());
    }
}
