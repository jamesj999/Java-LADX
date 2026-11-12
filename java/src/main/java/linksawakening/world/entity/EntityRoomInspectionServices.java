package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRoomInspection; resolved lazily to preserve initialization order. */
interface EntityRoomInspectionServices {
    boolean bombFinalPresentationPending(int slot);

    EntityWarpController warpState();

    boolean indoorRoom();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.gpu.EntitySpriteTileSnapshot spriteTiles();

    EntityBackgroundMechanics backgroundMechanics();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntityRewardController rewards();

    EntityRoomVariables roomVariables();

    EntitySlotProperties slotProperties();

    EntityStoryController story();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    static EntityRoomInspectionServices bind(RoomEntityRuntime r) {
        return new EntityRoomInspectionServices() {
            public boolean bombFinalPresentationPending(int slot) {
                return r.bombInteractions.bombFinalPresentationPending(slot);
            }

            public EntityWarpController warpState() {
                return r.warpState;
            }

            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public linksawakening.gpu.EntitySpriteTileSnapshot spriteTiles() {
                return r.spriteTiles;
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

            public EntityPickupController pickups() {
                return r.pickups;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityProjectileController projectiles() {
                return r.projectiles;
            }

            public EntityRewardController rewards() {
                return r.rewards;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public EntityStoryController story() {
                return r.story;
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
