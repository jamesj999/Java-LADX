package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityStoryController; resolved lazily to preserve initialization order. */
interface EntityStoryControllerServices {
    EntityMotionRegistry motions();

    int findFreeEntitySlot();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    RoomEntity[] slots();

    void blockLinkForOwl(int slot);

    void initializeHealthGroup(int slot);

    void presentWitchGotItem(int slot);

    static EntityStoryControllerServices bind(RoomEntityRuntime r) {
        return new EntityStoryControllerServices() {
            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
            }

            public EntityBossRewards bossRewards() {
                return r.bossRewards;
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

            public EntityPickupController pickups() {
                return r.pickups;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityRoomInspection inspection() {
                return r.inspection();
            }

            public EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public void blockLinkForOwl(int slot) {
                r.blockLinkForOwl(slot);
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }

            public void presentWitchGotItem(int slot) {
                r.presentWitchGotItem(slot);
            }
        };
    }
}
