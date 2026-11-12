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
        return new EntitySlotLifecycleServices() {
            public void setBombFinalPresentationPending(int slot, boolean value) {
                r.bombInteractions.setBombFinalPresentationPending(slot, value);
            }

            public EntityWarpController warpState() {
                return r.warpState;
            }

            public boolean beginLift(int slot, int romDirection) {
                return r.beginLift(slot, romDirection);
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
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

            public EntityEnemyInspection enemyInspection() {
                return r.enemyInspection;
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

            public GenieEntityController genie() {
                return r.genie;
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

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }

            public void startSwordPickup(int slot) {
                r.startSwordPickup(slot);
            }

            public void startToadstoolPickup(int slot) {
                r.startToadstoolPickup(slot);
            }
        };
    }
}
