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
        return new Binding(() -> r);
    }

    final class Binding implements EntityRoomInspectionServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public boolean bombFinalPresentationPending(int slot) {
            return room.get().bombInteractions.bombFinalPresentationPending(slot);
        }

        public EntityWarpController warpState() {
            return room.get().warpState;
        }

        public boolean indoorRoom() {
            return room.get().indoorRoom;
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public linksawakening.gpu.EntitySpriteTileSnapshot spriteTiles() {
            return room.get().spriteTiles;
        }

        public EntityBackgroundMechanics backgroundMechanics() {
            return room.get().backgroundMechanics;
        }

        public EntityBombInteractions bombInteractions() {
            return room.get().bombInteractions;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
        }

        public EntityEventChannels events() {
            return room.get().entityState.events;
        }

        public EntityLiftThrowController liftThrow() {
            return room.get().liftThrow;
        }

        public EntityLinkState linkState() {
            return room.get().entityState.linkState;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }

        public EntityPickupController pickups() {
            return room.get().pickups;
        }

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityProjectileController projectiles() {
            return room.get().projectiles;
        }

        public EntityRewardController rewards() {
            return room.get().rewards;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public EntityStoryController story() {
            return room.get().story;
        }

        public RomEnemyCombatTables enemyCombatTables() {
            return room.get().enemyCombatTables;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }
    }
}
