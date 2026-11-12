package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntitySlotLifecycle; resolved lazily to preserve initialization order. */
interface EntitySlotLifecycleServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    EntityWarpController warpState();

    boolean beginLift(int slot, int romDirection);

    int findFreeEntitySlot();

    int initialHealth(int type);

    EntityBackgroundMechanics backgroundMechanics();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEnemyInspection enemyInspection();

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

    GenieEntityController genie();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    SlimeEyeController slimeEye();

    void initializeHealthGroup(int slot);

    void startSwordPickup(int slot);

    void startToadstoolPickup(int slot);

    static EntitySlotLifecycleServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntitySlotLifecycleServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public void setBombFinalPresentationPending(int slot, boolean value) {
            room.get().bombInteractions.setBombFinalPresentationPending(slot, value);
        }

        public EntityWarpController warpState() {
            return room.get().warpState;
        }

        public boolean beginLift(int slot, int romDirection) {
            return room.get().beginLift(slot, romDirection);
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
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

        public EntityEnemyInspection enemyInspection() {
            return room.get().enemyInspection;
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

        public GenieEntityController genie() {
            return room.get().genie;
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

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }

        public void startSwordPickup(int slot) {
            room.get().startSwordPickup(slot);
        }

        public void startToadstoolPickup(int slot) {
            room.get().startToadstoolPickup(slot);
        }
    }
}
