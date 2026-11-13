package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Arrays;
import linksawakening.world.entity.handler.EntityHandlerFamilies;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.WarpMotion;

/** ROM presentation loading and initial room slot setup in source order. */
final class EntityRoomInitialization {
    final EntityWarpController warpState;
    final boolean indoorRoom;
    final EntityBombState bombInteractions;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityLiftState liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityRewardState rewards;
    final EntitySlotProperties slotProperties;
    final EntityStoryState story;
    final GenieEntityController genie;
    final RoomEntity[] slots;
    final EntitySlotLifecycle lifecycle;

    EntityRoomInitialization(
            EntityWarpController warpState,
            boolean indoorRoom,
            EntityBombState bombInteractions,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityRewardState rewards,
            EntitySlotProperties slotProperties,
            EntityStoryState story,
            GenieEntityController genie,
            RoomEntity[] slots,
            EntitySlotLifecycle lifecycle) {
        this.warpState = java.util.Objects.requireNonNull(warpState, "warpState");
        this.indoorRoom = indoorRoom;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.rewards = java.util.Objects.requireNonNull(rewards, "rewards");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.story = java.util.Objects.requireNonNull(story, "story");
        this.genie = java.util.Objects.requireNonNull(genie, "genie");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    EntityHandlerRegistry createEntityHandlers() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry(this.motions);
        EntityHandlerFamilies.install(registry);
        return registry;
    }

    void initializeRomPresentation(linksawakening.rom.Rom romData) {
        EntityPresentationRom.initialize(this, romData);
    }

    void initializeRoomSlots(RoomEntitySnapshot initial) {

        this.slotProperties.state.writeHookshotChainOam(initial.hookshotChainOam());
        this.slotProperties.state.writePincerBodyOam(initial.pincerBodyOam());
        this.slotProperties.state.writeWingedOctorokOam(initial.wingedOctorokOam());
        Arrays.fill(this.story.storageKidBall(), false);
        Arrays.fill(this.story.storageKidPlayTimer(), 0);
        Arrays.fill(this.story.storageKidPlayState(), 0);
        Arrays.fill(this.story.storageKidZSpeed(), 0);
        Arrays.fill(this.story.storageKidKidnapState(), 0);
        Arrays.fill(this.story.storageKidKidnapDelay(), 0);
        Arrays.fill(this.story.storageKidSpeedXAccumulator(), 0);
        Arrays.fill(this.story.storageKidSpeedYAccumulator(), 0);
        this.physicsTables.resetOptionsOverrides();
        Arrays.fill(this.bossRewards.storageDroppedItemBySlot(), 0);
        Arrays.fill(this.bombInteractions.storageBombDirection(), 0xFF);
        this.bombInteractions.clearBombFinalPresentations();
        Arrays.fill(this.liftThrow.storageThrownDirection(), 0xFF);
        for (RoomEntity entity : this.slots) {
            initializeSlotTables(entity, initial);
            initializeSpecialEntities(entity, initial);
            initializeRoamingEntities(entity, initial);
            initializeBossAndFollowerEntities(entity, initial);
        }
    }

    private void initializeSlotTables(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == 0x71) {
            this.story.writeKidPlayState(entity.slot(), 1);
            this.story.writeKidPlayTimer(entity.slot(), 0x20);
        }
        this.physicsTables.writeBaseEntityFlipAttribute(entity.slot(), entity.entityFlipAttribute());
        this.physicsTables.writeFallingVisualYOffset(entity.slot(), initial.visualYOffset(entity.slot()));
        if (entity.loaded()) this.lifecycle.initializeHealthGroup(entity.slot());
        this.combatTables.writeEnemyHealth(
                entity.slot(), entity.loaded() ? this.slotProperties.initialHealth(entity.type()) : 0);
        this.physicsTables.writeEnemyPhysicsFlags(
                entity.slot(), entity.loaded() ? initialPhysicsFlags(entity.type()) : 0);
        this.combatTables.writeEnemyHitboxFlags(entity.slot(), entity.loaded() ? initialHitboxFlags(entity.type()) : 0);
        // Room-loaded indoor warps start at state 1 ($03:4F75).
        // State 0 is reserved for dynamically spawned warps.
        if (entity.loaded() && this.indoorRoom && entity.type() == WarpMotion.ENTITY_TYPE) {
            this.warpState.writeWarpState(entity.slot(), WarpMotion.STATE_APPROACH);
            this.slots[entity.slot()] = withPositionAndVariant(entity, entity.x(), entity.y(), -1);
        }
    }

    private void initializeSpecialEntities(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_GENIE) {
            GenieMotion.JarState jar =
                    this.motions.get(GenieMotion.class, GenieMotion::new).initialize(entity.slot());
            this.genie.setGenieActiveState(entity.slot(), 0);
            this.genie.setGeniePrivateState1(entity.slot(), jar.privateState1());
            this.genie.setGeniePrivateState3(entity.slot(), 0);
            this.combatTables.writeEnemyHealth(entity.slot(), jar.health());
            this.physicsTables.writeEnemyPhysicsFlags(entity.slot(), jar.physicsFlags());
            this.combatTables.writeEnemyHitboxFlags(entity.slot(), jar.hitboxFlags());
            this.physicsTables.writeEntityOptions1Override(entity.slot(), EntityRomConstants.GENIE_OPTIONS1);
            if (entity.status() == EntityStatus.INIT) {
                this.slots[entity.slot()] =
                        withPositionAndVariant(entity, entity.x() + 0x08, entity.y(), entity.spriteVariant());
            }
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CHEST_WITH_ITEM) {
            this.pickups.writeChestItemBySlot(entity.slot(), entity.spriteVariant());
        }
        if (entity.loaded() && isBombiteType(entity.type())) {
            this.motions.get(BombiteMotion.class, BombiteMotion::new).initialize(entity.slot());
            // The bank-$04 handlers use the global slow-transition tick
            // for TimerBombite's fuse. Keep it live for either family;
            // BouncingBombite simply leaves its value at zero.
            this.physicsTables.writeSlowTimerInitialized(entity.slot(), true);
        }
        if (entity.loaded() && isGhiniType(entity.type())) {
            this.motions.get(GhiniMotion.class, GhiniMotion::new).initialize(entity.slot(), entity.type());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE) {
            this.motions.get(LikeLikeMotion.class, LikeLikeMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            this.motions.get(PolsVoiceMotion.class, PolsVoiceMotion::new).initialize(entity.slot());
        }
    }

    private void initializeRoamingEntities(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_FISH) {
            this.motions.get(FishMotion.class, FishMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CROW) {
            this.motions.get(CrowMotion.class, CrowMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY) {
            this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUZZ_BLOB) {
            this.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SAND_CRAB) {
            this.motions.get(SandCrabMotion.class, SandCrabMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DOG) {
            this.motions.get(DogMotion.class, DogMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_URCHIN) {
            this.motions.get(UrchinMotion.class, UrchinMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_WITCH_RAT) {
            this.motions.get(WitchRatMotion.class, WitchRatMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            this.motions.get(FairyMotion.class, FairyMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            this.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_PINCER) {
            this.motions.get(PincerMotion.class, PincerMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            this.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MIMIC) {
            this.motions.get(MimicMotion.class, MimicMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA) {
            this.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new).initialize(entity.slot());
        }
    }

    private void initializeBossAndFollowerEntities(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MINI_MOLDORM) {
            this.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new).initialize(entity);
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
            this.motions.get(MoldormMotion.class, MoldormMotion::new).initialize(entity);
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES) {
            this.motions.get(RollingBonesMotion.class, RollingBonesMotion::new).initializeBoss(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
            this.motions.get(RollingBonesMotion.class, RollingBonesMotion::new).initializeBar(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CUCCO) {
            this.motions.get(CuccoMotion.class, CuccoMotion::new).initialize(entity.slot());
        }
        if (entity.status() == EntityStatus.DYING) {
            this.combatTables.writePowerRecoilDeath(entity.slot(), entity.powerRecoilDeath());
        }
        if (isLaserType(entity.type())) {
            this.motions.get(LaserMotion.class, LaserMotion::new).initializeForEntity(entity);
        }
        if (isFollowingNpcType(entity.type())) {
            if (entity.type() == EntityRomConstants.ENTITY_BOW_WOW) {
                this.motions.get(BowWowMotion.class, BowWowMotion::new).initialize(entity.slot());
            } else {
                this.motions
                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .initialize(entity.slot(), entity.type());
            }
        }
        if (entity.status() == EntityStatus.LIFTED) {
            this.liftThrow.writeLiftedStateInitialized(entity.slot(), true);
            this.liftThrow.writeLiftedSourceDirection(entity.slot(), LiftedEntityMotion.ROM_DIRECTION_DOWN);
            this.liftThrow.writeLiftedEffectiveDirection(LiftedEntityMotion.ROM_DIRECTION_DOWN);
            this.physicsTables.writeEnemyTransitionCountdown(entity.slot(), 0x02);
            this.liftThrow.writeLiftedEntitySlot(entity.slot());
        }
        if (entity.status() == EntityStatus.THROWN) {
            this.liftThrow.writeThrownDirection(entity.slot(), ThrownEntityMotion.ROM_DIRECTION_DOWN);
        }
    }
}
