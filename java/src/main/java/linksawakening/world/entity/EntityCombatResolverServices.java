package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityCombatResolver; resolved lazily to preserve initialization order. */
interface EntityCombatResolverServices {
    int contactDamage(int slot);

    int options1(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics();

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityFrameContext frameContext(int frameCounter);

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityRoomInspection roomInspection();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables();

    linksawakening.world.entity.RoomEntity[] slots();

    linksawakening.world.entity.SlimeEyeController slimeEye();

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

            public linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
            }

            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
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

            public linksawakening.world.entity.EntityFrameContext frameContext(int frameCounter) {
                return r.frameContext(frameCounter);
            }

            public linksawakening.world.entity.EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityRoomInspection roomInspection() {
                return r.roomInspection;
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

            public linksawakening.world.entity.SlimeEyeController slimeEye() {
                return r.slimeEye;
            }
        };
    }
}
