package linksawakening.world.entity;

/** Dependencies used by EntityRoomInspection; resolved lazily to preserve initialization order. */
interface EntityRoomInspectionServices {
    boolean bombFinalPresentationPending(int slot);

    EntityWarpController warpState();

    boolean indoorRoom();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.gpu.EntitySpriteTileSnapshot spriteTiles();

    linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics();

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPickupController pickups();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityProjectileController projectiles();

    linksawakening.world.entity.EntityRewardController rewards();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.EntityStoryController story();

    linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables();

    linksawakening.world.entity.RoomEntity[] slots();

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

            public linksawakening.world.entity.EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public linksawakening.world.entity.EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
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

            public linksawakening.world.entity.EntityProjectileController projectiles() {
                return r.projectiles;
            }

            public linksawakening.world.entity.EntityRewardController rewards() {
                return r.rewards;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.EntityStoryController story() {
                return r.story;
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
