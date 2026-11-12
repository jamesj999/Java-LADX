package linksawakening.world.entity;

/** Dependencies used by EntitySlotLifecycle; resolved lazily to preserve initialization order. */
interface EntitySlotLifecycleServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    EntityWarpController warpState();

    boolean beginLift(int slot, int romDirection);

    int findFreeEntitySlot();

    int initialHealth(int type);

    linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics();

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEnemyInspection enemyInspection();

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

    linksawakening.world.entity.GenieEntityController genie();

    linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables();

    linksawakening.world.entity.RoomEntity[] slots();

    linksawakening.world.entity.SlimeEyeController slimeEye();

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

            public linksawakening.world.entity.EntityEnemyInspection enemyInspection() {
                return r.enemyInspection;
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

            public linksawakening.world.entity.GenieEntityController genie() {
                return r.genie;
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
