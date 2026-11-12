package linksawakening.world.entity;
/** Ordered per-slot combat operations; false ends this slot without visiting later operations. */
final class EntitySlotCombat {
    static boolean resolve(EntityCombatSlot slot) {
        if (!EntityCombatGates.acceptInitialStatus(slot)) return false;
        if (!EntityCombatGates.acceptOrdinaryCollision(slot)) return false;
        if (!EntityCombatGates.acceptHandlerCollision(slot)) return false;
        if (!EntityCombatGates.acceptMovementState(slot)) return false;
        if (!EntityCombatGates.acceptDamageCountdowns(slot)) return false;
        if (!EntityCombatOverlap.probeBodyAndSword(slot)) return false;
        if (!EntityCombatOverlap.applyShieldContact(slot)) return false;
        if (!EntityCombatOverlap.acceptContactOrStomp(slot)) return false;
        if (!EntitySwordImpact.prepare(slot)) return false;
        if (!EntitySwordImpact.apply(slot)) return false;
        if (!EntityLinkContact.prepare(slot)) return false;
        if (!EntityLinkContact.applyRecoil(slot)) return false;
        if (!EntityLinkContact.emitOutcome(slot)) return false;
        return true;
    }
}
