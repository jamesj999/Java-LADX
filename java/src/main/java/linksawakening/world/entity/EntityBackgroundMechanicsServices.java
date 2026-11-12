package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBackgroundMechanics; resolved lazily to preserve initialization order. */
interface EntityBackgroundMechanicsServices {
    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEnemyInspection enemyInspection();

    EntityEventChannels events();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    RoomEntity[] slots();

    EntitySpriteViews spriteViews();

    static EntityBackgroundMechanicsServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityBackgroundMechanicsServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntitySpriteViews spriteViews() {
            return room.get().slotProperties.spriteViews();
        }

        public int clearEntity(int slot) {
            return room.get().clearEntity(slot);
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
        }

        public EntityEnemyInspection enemyInspection() {
            return room.get().enemyInspection;
        }

        public EntityEventChannels events() {
            return room.get().entityState.events;
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

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }
    }
}
