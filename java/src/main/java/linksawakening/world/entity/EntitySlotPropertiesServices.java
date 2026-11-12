package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntitySlotProperties; resolved lazily to preserve initialization order. */
interface EntitySlotPropertiesServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    RoomEntity[] slots();

    static EntitySlotPropertiesServices bind(RoomEntityRuntime r) {
        return new EntitySlotPropertiesServices() {
            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
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

            public EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityProjectileController projectiles() {
                return r.projectiles;
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

            public RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
