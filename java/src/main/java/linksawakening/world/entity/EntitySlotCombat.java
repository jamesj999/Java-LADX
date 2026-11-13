package linksawakening.world.entity;

import linksawakening.world.entity.EntityCombatResolver.EntityCombatSlot;

/** Ordered per-slot combat operations; false ends this slot without visiting later operations. */
final class EntitySlotCombat {
    static boolean resolve(EntityCombatSlot slot) {
        if (!EntityCombatResolver.entityCombatGatesAcceptInitialStatus(slot)) return false;
        if (!EntityCombatResolver.entityCombatGatesAcceptOrdinaryCollision(slot)) return false;
        if (!EntityCombatResolver.entityCombatGatesAcceptHandlerCollision(slot)) return false;
        if (!EntityCombatResolver.entityCombatGatesAcceptMovementState(slot)) return false;
        if (!EntityCombatResolver.entityCombatGatesAcceptDamageCountdowns(slot)) return false;
        if (!EntityCombatResolver.entityCombatOverlapProbeBodyAndSword(slot)) return false;
        if (!EntityCombatResolver.entityCombatOverlapApplyShieldContact(slot)) return false;
        if (!EntityCombatResolver.entityCombatOverlapAcceptContactOrStomp(slot)) return false;
        if (!EntityCombatResolver.entitySwordImpactPrepare(slot)) return false;
        if (!EntityCombatResolver.entitySwordImpactApply(slot)) return false;
        if (!EntityLinkContact.prepare(slot)) return false;
        if (!EntityLinkContact.applyRecoil(slot)) return false;
        if (!EntityLinkContact.emitOutcome(slot)) return false;
        return true;
    }
}
