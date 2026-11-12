package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRewardController; resolved lazily to preserve initialization order. */
interface EntityRewardControllerServices {
    EntityMotionRegistry motions();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotLifecycle lifecycle();

    linksawakening.world.entity.EntityStoryController story();

    linksawakening.world.entity.RoomEntity[] slots();

    void holdSwordAboveLink(
            linksawakening.world.entity.RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            boolean finalPose);

    void initializeHealthGroup(int slot);

    static EntityRewardControllerServices bind(RoomEntityRuntime r) {
        return new EntityRewardControllerServices() {
            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityEventChannels events() {
                return r.entityState.events;
            }

            public linksawakening.world.entity.EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public linksawakening.world.entity.EntityStoryController story() {
                return r.story;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }

            public void holdSwordAboveLink(
                    linksawakening.world.entity.RoomEntity entity,
                    int linkEntityX,
                    int linkEntityY,
                    int linkZ,
                    boolean finalPose) {
                r.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose);
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
