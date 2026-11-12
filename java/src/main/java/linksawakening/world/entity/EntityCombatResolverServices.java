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
        return new Binding(() -> r);
    }

    final class Binding implements EntityCombatResolverServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public int contactDamage(int slot) {
            return room.get().contactDamage(slot);
        }

        public int options1(int slot) {
            return room.get().options1(slot);
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
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

        public EntityFrameContext frameContext(int frameCounter) {
            return room.get().frameContext(frameCounter);
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

        public EntityRoomInspection roomInspection() {
            return room.get().roomInspection;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public RomEnemyCombatTables enemyCombatTables() {
            return room.get().enemyCombatTables;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public SlimeEyeController slimeEye() {
            return room.get().slimeEye;
        }
    }
}
