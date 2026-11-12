package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Composes cross-cutting state; type-specific state belongs to its controller. */
final class EntityRuntimeState {
    final EntityEventChannels events = new EntityEventChannels();
    final EntityLinkState linkState = new EntityLinkState();
    final EntityLinkView linkView = new EntityLinkView(linkState);

    final EntityCombatTables combatTables = new EntityCombatTables();
    final EntityPhysicsTables physicsTables = new EntityPhysicsTables();
    final EntityRoomVariables roomVariables = new EntityRoomVariables();
    final EntityPresentationState presentationState = new EntityPresentationState();
    final EntityMotionRegistry motions = new EntityMotionRegistry();
    final EntitySlotView slotView = new EntitySlotView(
            combatTables, physicsTables, motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new));
}
