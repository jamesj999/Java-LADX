package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityStoryController; resolved lazily to preserve initialization order. */
interface EntityStoryControllerServices {
    EntityMotionRegistry motions();

    int findFreeEntitySlot();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPickupController pickups();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityRoomInspection inspection();

    linksawakening.world.entity.EntityRoomInspection roomInspection();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotLifecycle lifecycle();

    linksawakening.world.entity.RoomEntity[] slots();

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

            public linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
            }

            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
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

            public linksawakening.world.entity.EntityPickupController pickups() {
                return r.pickups;
            }

            public linksawakening.world.entity.EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public linksawakening.world.entity.EntityRoomInspection inspection() {
                return r.inspection();
            }

            public linksawakening.world.entity.EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
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
