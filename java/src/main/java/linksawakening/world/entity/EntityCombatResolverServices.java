package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityCombatResolver; resolved lazily to preserve initialization order. */
interface EntityCombatResolverServices {
    int contactDamage(int slot);

    int options1(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityFrameContext frameContext(int frameCounter);

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomInspection roomInspection();

    EntitySlotProperties slotProperties();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    SlimeEyeController slimeEye();

    static EntityCombatResolverServices bind(RoomEntityRuntime r) {
        return new EntityCombatResolverServices() {
            public int contactDamage(int slot) {
                return r.contactDamage(slot);
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

            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
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

            public EntityFrameContext frameContext(int frameCounter) {
                return r.frameContext(frameCounter);
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

            public EntityRoomInspection roomInspection() {
                return r.roomInspection;
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

            public SlimeEyeController slimeEye() {
                return r.slimeEye;
            }
        };
    }
}
