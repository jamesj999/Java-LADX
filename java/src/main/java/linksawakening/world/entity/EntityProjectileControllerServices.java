package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityProjectileController; resolved lazily to preserve initialization order. */
interface EntityProjectileControllerServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBombInteractions bombInteractions();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPlayerSpawns entityPlayerSpawns();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    static EntityProjectileControllerServices bind(RoomEntityRuntime r) {
        return new EntityProjectileControllerServices() {
            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
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

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public EntityPlayerSpawns entityPlayerSpawns() {
                return r.entityPlayerSpawns;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public RomEnemyCombatTables enemyCombatTables() {
                return r.enemyCombatTables;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
