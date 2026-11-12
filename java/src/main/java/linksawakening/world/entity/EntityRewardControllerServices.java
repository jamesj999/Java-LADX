package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRewardController; resolved lazily to preserve initialization order. */
interface EntityRewardControllerServices {
    EntityMotionRegistry motions();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntityStoryController story();

    RoomEntity[] slots();

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose);

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

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEventChannels events() {
                return r.entityState.events;
            }

            public EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public EntityStoryController story() {
                return r.story;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public void holdSwordAboveLink(
                    RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) {
                r.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose);
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
