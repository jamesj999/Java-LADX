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
    private final EntityRoomInitializationServices runtime;

    EntityRoomInitialization(EntityRoomInitializationServices runtime) {
        this.runtime = runtime;
    }

    EntityHandlerRegistry createEntityHandlers() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry(runtime.motions());
        EntityHandlerFamilies.install(registry);
        return registry;
    }

    void initializeRomPresentation(linksawakening.rom.Rom romData) {
        EntityPresentationRom.initialize(runtime, romData);
    }

    void initializeRoomSlots(RoomEntitySnapshot initial) {

        runtime.slotProperties().writeHookshotChainOam(initial.hookshotChainOam());
        runtime.slotProperties().writePincerBodyOam(initial.pincerBodyOam());
        runtime.slotProperties().writeWingedOctorokOam(initial.wingedOctorokOam());
        Arrays.fill(runtime.story().storageKidBall(), false);
        Arrays.fill(runtime.story().storageKidPlayTimer(), 0);
        Arrays.fill(runtime.story().storageKidPlayState(), 0);
        Arrays.fill(runtime.story().storageKidZSpeed(), 0);
        Arrays.fill(runtime.story().storageKidKidnapState(), 0);
        Arrays.fill(runtime.story().storageKidKidnapDelay(), 0);
        Arrays.fill(runtime.story().storageKidSpeedXAccumulator(), 0);
        Arrays.fill(runtime.story().storageKidSpeedYAccumulator(), 0);
        runtime.physicsTables().resetOptionsOverrides();
        Arrays.fill(runtime.bossRewards().storageDroppedItemBySlot(), 0);
        Arrays.fill(runtime.bombInteractions().storageBombDirection(), 0xFF);
        runtime.clearBombFinalPresentations();
        Arrays.fill(runtime.liftThrow().storageThrownDirection(), 0xFF);
        for (RoomEntity entity : runtime.slots()) {
            initializeSlotTables(entity, initial);
            initializeSpecialEntities(entity, initial);
            initializeRoamingEntities(entity, initial);
            initializeBossAndFollowerEntities(entity, initial);
        }
    }

    private void initializeSlotTables(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == 0x71) {
            runtime.story().writeKidPlayState(entity.slot(), 1);
            runtime.story().writeKidPlayTimer(entity.slot(), 0x20);
        }
        runtime.physicsTables().writeBaseEntityFlipAttribute(entity.slot(), entity.entityFlipAttribute());
        runtime.physicsTables().writeFallingVisualYOffset(entity.slot(), initial.visualYOffset(entity.slot()));
        if (entity.loaded()) runtime.initializeHealthGroup(entity.slot());
        runtime.combatTables()
                .writeEnemyHealth(entity.slot(), entity.loaded() ? runtime.initialHealth(entity.type()) : 0);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(entity.slot(), entity.loaded() ? initialPhysicsFlags(entity.type()) : 0);
        runtime.combatTables()
                .writeEnemyHitboxFlags(entity.slot(), entity.loaded() ? initialHitboxFlags(entity.type()) : 0);
        // Room-loaded indoor warps start at state 1 ($03:4F75).
        // State 0 is reserved for dynamically spawned warps.
        if (entity.loaded() && runtime.indoorRoom() && entity.type() == WarpMotion.ENTITY_TYPE) {
            runtime.warpState().writeWarpState(entity.slot(), WarpMotion.STATE_APPROACH);
            runtime.slots()[entity.slot()] = withPositionAndVariant(entity, entity.x(), entity.y(), -1);
        }
    }

    private void initializeSpecialEntities(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_GENIE) {
            GenieMotion.JarState jar =
                    runtime.motions().get(GenieMotion.class, GenieMotion::new).initialize(entity.slot());
            runtime.genie().setGenieActiveState(entity.slot(), 0);
            runtime.genie().setGeniePrivateState1(entity.slot(), jar.privateState1());
            runtime.genie().setGeniePrivateState3(entity.slot(), 0);
            runtime.combatTables().writeEnemyHealth(entity.slot(), jar.health());
            runtime.physicsTables().writeEnemyPhysicsFlags(entity.slot(), jar.physicsFlags());
            runtime.combatTables().writeEnemyHitboxFlags(entity.slot(), jar.hitboxFlags());
            runtime.physicsTables().writeEntityOptions1Override(entity.slot(), EntityRomConstants.GENIE_OPTIONS1);
            if (entity.status() == EntityStatus.INIT) {
                runtime.slots()[entity.slot()] =
                        withPositionAndVariant(entity, entity.x() + 0x08, entity.y(), entity.spriteVariant());
            }
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CHEST_WITH_ITEM) {
            runtime.pickups().writeChestItemBySlot(entity.slot(), entity.spriteVariant());
        }
        if (entity.loaded() && isBombiteType(entity.type())) {
            runtime.motions().get(BombiteMotion.class, BombiteMotion::new).initialize(entity.slot());
            // The bank-$04 handlers use the global slow-transition tick
            // for TimerBombite's fuse. Keep it live for either family;
            // BouncingBombite simply leaves its value at zero.
            runtime.physicsTables().writeSlowTimerInitialized(entity.slot(), true);
        }
        if (entity.loaded() && isGhiniType(entity.type())) {
            runtime.motions().get(GhiniMotion.class, GhiniMotion::new).initialize(entity.slot(), entity.type());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE) {
            runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            runtime.motions().get(PolsVoiceMotion.class, PolsVoiceMotion::new).initialize(entity.slot());
        }
    }

    private void initializeRoamingEntities(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_FISH) {
            runtime.motions().get(FishMotion.class, FishMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CROW) {
            runtime.motions().get(CrowMotion.class, CrowMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY) {
            runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUZZ_BLOB) {
            runtime.motions().get(BuzzBlobMotion.class, BuzzBlobMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SAND_CRAB) {
            runtime.motions().get(SandCrabMotion.class, SandCrabMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DOG) {
            runtime.motions().get(DogMotion.class, DogMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_URCHIN) {
            runtime.motions().get(UrchinMotion.class, UrchinMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_WITCH_RAT) {
            runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            runtime.motions().get(FairyMotion.class, FairyMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            runtime.motions()
                    .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                    .initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_PINCER) {
            runtime.motions().get(PincerMotion.class, PincerMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            runtime.motions()
                    .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                    .initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MIMIC) {
            runtime.motions().get(MimicMotion.class, MimicMotion::new).initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA) {
            runtime.motions()
                    .get(MaskedMimicMotion.class, MaskedMimicMotion::new)
                    .initialize(entity.slot());
        }
    }

    private void initializeBossAndFollowerEntities(RoomEntity entity, RoomEntitySnapshot initial) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MINI_MOLDORM) {
            runtime.motions()
                    .get(MiniMoldormMotion.class, MiniMoldormMotion::new)
                    .initialize(entity);
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
            runtime.motions().get(MoldormMotion.class, MoldormMotion::new).initialize(entity);
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES) {
            runtime.motions()
                    .get(RollingBonesMotion.class, RollingBonesMotion::new)
                    .initializeBoss(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
            runtime.motions()
                    .get(RollingBonesMotion.class, RollingBonesMotion::new)
                    .initializeBar(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CUCCO) {
            runtime.motions().get(CuccoMotion.class, CuccoMotion::new).initialize(entity.slot());
        }
        if (entity.status() == EntityStatus.DYING) {
            runtime.combatTables().writePowerRecoilDeath(entity.slot(), entity.powerRecoilDeath());
        }
        if (isLaserType(entity.type())) {
            runtime.motions().get(LaserMotion.class, LaserMotion::new).initializeForEntity(entity);
        }
        if (isFollowingNpcType(entity.type())) {
            if (entity.type() == EntityRomConstants.ENTITY_BOW_WOW) {
                runtime.motions().get(BowWowMotion.class, BowWowMotion::new).initialize(entity.slot());
            } else {
                runtime.motions()
                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .initialize(entity.slot(), entity.type());
            }
        }
        if (entity.status() == EntityStatus.LIFTED) {
            runtime.liftThrow().writeLiftedStateInitialized(entity.slot(), true);
            runtime.liftThrow().writeLiftedSourceDirection(entity.slot(), LiftedEntityMotion.ROM_DIRECTION_DOWN);
            runtime.liftThrow().writeLiftedEffectiveDirection(LiftedEntityMotion.ROM_DIRECTION_DOWN);
            runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0x02);
            runtime.liftThrow().writeLiftedEntitySlot(entity.slot());
        }
        if (entity.status() == EntityStatus.THROWN) {
            runtime.liftThrow().writeThrownDirection(entity.slot(), ThrownEntityMotion.ROM_DIRECTION_DOWN);
        }
    }
}
