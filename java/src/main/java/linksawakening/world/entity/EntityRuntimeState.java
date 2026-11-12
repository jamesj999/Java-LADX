package linksawakening.world.entity;

/** Composes cross-cutting state; type-specific state belongs to its controller. */
final class EntityRuntimeState {
    final EntityEventChannels events = new EntityEventChannels();
    final EntityLinkState linkState = new EntityLinkState();
    final EntityLinkView linkView = new EntityLinkView(linkState);

    final EntityCombatTables combatTables = new EntityCombatTables();
    final EntityPhysicsTables physicsTables = new EntityPhysicsTables();
    final EntityRoomVariables roomVariables = new EntityRoomVariables();
    final EntityPresentationState presentationState = new EntityPresentationState();
    final EntityMotionSet motionSet = new EntityMotionSet();
    final EntitySlotView slotView = new EntitySlotView(combatTables, physicsTables, motionSet.unmaskedIronMaskMotion);
}
