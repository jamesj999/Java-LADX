package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBombInteractions; resolved lazily to preserve initialization order. */
interface EntityBombInteractionsServices {

    int clearEntity(int slot);

    int options1(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntitySlotProperties slotProperties();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    static EntityBombInteractionsServices bind(RoomEntityRuntime r) {
        return new EntityBombInteractionsServices() {

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public int options1(int slot) {
                return r.options1(slot);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
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
