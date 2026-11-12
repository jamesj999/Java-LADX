package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityProjectileController; resolved lazily to preserve initialization order. */
interface EntityProjectileControllerServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPlayerSpawns entityPlayerSpawns();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotLifecycle lifecycle();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables();

    linksawakening.world.entity.RoomEntity[] slots();

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

            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityEventChannels events() {
                return r.entityState.events;
            }

            public linksawakening.world.entity.EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityPlayerSpawns entityPlayerSpawns() {
                return r.entityPlayerSpawns;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables() {
                return r.enemyCombatTables;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
