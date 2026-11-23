package linksawakening.world.entity;

import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;

/** Composes cross-cutting state; type-specific state belongs to its controller. */
final class EntityRuntimeState {
    private final EntityEventChannels events = new EntityEventChannels();
    private final EntityLinkState linkState = new EntityLinkState();
    private final EntityLinkView linkView = new EntityLinkView(linkState);

    private final EntityCombatTables combatTables;
    private final EntityPhysicsTables physicsTables;
    private final EntityRoomVariables roomVariables = new EntityRoomVariables();
    private final EntityPresentationState presentationState = new EntityPresentationState();
    private final EntityMotionRegistry motions = new EntityMotionRegistry();
    private final EntitySlotView slotView;

    EntityRuntimeState(EntitySlots slots) {
        combatTables = slots.combat();
        physicsTables = slots.physics();
        slotView = new EntitySlotView(
                combatTables, physicsTables, motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new));
    }

    EntityPresentationState presentationState() {
        return this.presentationState;
    }

    EntityPhysicsTables physicsTables() {
        return this.physicsTables;
    }

    EntityCombatTables combatTables() {
        return this.combatTables;
    }

    EntityMotionRegistry motions() {
        return this.motions;
    }

    EntityEventChannels events() {
        return this.events;
    }

    EntityRoomVariables roomVariables() {
        return this.roomVariables;
    }

    EntityLinkState linkState() {
        return this.linkState;
    }

    EntityLinkView linkView() {
        return this.linkView;
    }

    EntitySlotView slotView() {
        return this.slotView;
    }
}
