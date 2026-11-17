package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntityTypeRules.initialHitboxFlags;
import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;
import static linksawakening.world.entity.EntityTypeRules.isBombiteType;
import static linksawakening.world.entity.EntityTypeRules.isFollowingNpcType;
import static linksawakening.world.entity.EntityTypeRules.isGhiniType;
import static linksawakening.world.entity.EntityTypeRules.isLaserType;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.rom.RomAddress;
import linksawakening.world.entity.handler.EntityHandlerFamilies;
import linksawakening.world.entity.motion.BombiteMotion;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.BushCrawlerMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.DogMotion;
import linksawakening.world.entity.motion.FairyMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.GhiniMotion;
import linksawakening.world.entity.motion.LaserMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.LikeLikeMotion;
import linksawakening.world.entity.motion.MaskedMimicMotion;
import linksawakening.world.entity.motion.MimicMotion;
import linksawakening.world.entity.motion.MiniMoldormMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PincerMotion;
import linksawakening.world.entity.motion.PolsVoiceMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.SpikedBeetleMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WarpMotion;
import linksawakening.world.entity.motion.WitchRatMotion;

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
    final EntitySlots slots;
    final EntitySlotLifecycle lifecycle;

    EntityRoomInitialization(
            EntityWarpController warpState,
            boolean indoorRoom,
            EntityBombState bombInteractions,
            EntityBossState bossRewards,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPickupState pickups,
            EntityRewardState rewards,
            EntitySlotProperties slotProperties,
            EntityStoryState story,
            GenieEntityController genie,
            EntitySlots slots,
            EntitySlotLifecycle lifecycle) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.warpState = Objects.requireNonNull(warpState, "warpState");
        this.indoorRoom = indoorRoom;
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.rewards = Objects.requireNonNull(rewards, "rewards");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.story = Objects.requireNonNull(story, "story");
        this.genie = Objects.requireNonNull(genie, "genie");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    EntityHandlerRegistry createEntityHandlers() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry(this.motions);
        EntityHandlerFamilies.install(registry);
        return registry;
    }

    void initializeRomPresentation(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ linksawakening.rom
                            .Rom
                    romData) {
        {
            for (int i = 0; i < this.pickups.storageHeartPieceMeterPairs().length; i++)
                this.pickups.writeHeartPieceMeterPairs(
                        i, romData.u8(ENTITYPRESENTATIONROM_HEART_PIECE_VARIANTS.plus(i)));
            for (int i = 0; i < this.rewards.storageInstrumentPaletteCycle().length; i++) {
                int rgb555 = romData.u16le(ENTITYPRESENTATIONROM_INSTRUMENT_PALETTE.plus(i * 2));
                this.rewards.writeInstrumentPaletteCycle(i, linksawakening.rom.RomBank.decodeRgb555(rgb555));
            }
            for (int i = 0; i < this.rewards.storageInstrumentWarpSprites().length; i++) {
                this.rewards.writeInstrumentWarpSprites(
                        i, romData.u8(ENTITYPRESENTATIONROM_INSTRUMENT_WARP_SPRITES.plus(i)));
            }
            // Eighteen fixed repeat commands, two rows per controller frame.
            // Shipped ROM symbols: Data_007_6493=$6493, Data_007_64F6=$64DB.
            for (int row = 0; row < 18; row++) {
                int tile = romData.u8(ENTITYPRESENTATIONROM_HINT_TILES.plus(row * 4 + 3));
                int attr = romData.u8(ENTITYPRESENTATIONROM_HINT_ATTRIBUTES.plus(row * 4 + 3));
                Arrays.fill(this.rewards.storageInstrumentHintTiles(), row * 20, (row + 1) * 20, tile);
                Arrays.fill(this.rewards.storageInstrumentHintAttributes(), row * 20, (row + 1) * 20, attr);
            }
        }
    }

    void initializeRoomSlots(RoomEntitySnapshot initial) {

        slotProperties.initializeOam(initial);
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
        for (int entitySlotIndex = 0; entitySlotIndex < this.slots.size(); entitySlotIndex++) {
            RoomEntity entity = this.slots.get(entitySlotIndex);

            initializeSlotTables(entity, initial);
            initializeSpecialEntities(entity);
            initializeRoamingEntities(entity);
            initializeBossAndFollowerEntities(entity);
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
            this.slots.replace(entity.slot(), withPositionAndVariant(entity, entity.x(), entity.y(), -1));
        }
    }

    private void initializeSpecialEntities(RoomEntity entity) {
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
                this.slots.replace(
                        entity.slot(),
                        withPositionAndVariant(entity, entity.x() + 0x08, entity.y(), entity.spriteVariant()));
            }
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CHEST_WITH_ITEM) {
            this.pickups.writeChestItemBySlot(entity.slot(), entity.spriteVariant());
        }
        if (entity.loaded() && isBombiteType(entity.type())) {
            var bombiteMotion = this.motions.get(BombiteMotion.class, BombiteMotion::new);
            bombiteMotion.initialize(entity.slot());
            // The bank-$04 handlers use the global slow-transition tick
            // for TimerBombite's fuse. Keep it live for either family;
            // BouncingBombite simply leaves its value at zero.
            this.physicsTables.writeSlowTimerInitialized(entity.slot(), true);
        }
        if (entity.loaded() && isGhiniType(entity.type())) {
            var ghiniMotion = this.motions.get(GhiniMotion.class, GhiniMotion::new);
            ghiniMotion.initialize(entity.slot(), entity.type());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE) {
            var likeLikeMotion = this.motions.get(LikeLikeMotion.class, LikeLikeMotion::new);
            likeLikeMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            var polsVoiceMotion = this.motions.get(PolsVoiceMotion.class, PolsVoiceMotion::new);
            polsVoiceMotion.initialize(entity.slot());
        }
    }

    private void initializeRoamingEntities(RoomEntity entity) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_FISH) {
            var fishMotion = this.motions.get(FishMotion.class, FishMotion::new);
            fishMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CROW) {
            var crowMotion = this.motions.get(CrowMotion.class, CrowMotion::new);
            crowMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY) {
            var booBuddyMotion = this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
            booBuddyMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUZZ_BLOB) {
            var buzzBlobMotion = this.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new);
            buzzBlobMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SAND_CRAB) {
            var sandCrabMotion = this.motions.get(SandCrabMotion.class, SandCrabMotion::new);
            sandCrabMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DOG) {
            var dogMotion = this.motions.get(DogMotion.class, DogMotion::new);
            dogMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_URCHIN) {
            var urchinMotion = this.motions.get(UrchinMotion.class, UrchinMotion::new);
            urchinMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_WITCH_RAT) {
            var witchRatMotion = this.motions.get(WitchRatMotion.class, WitchRatMotion::new);
            witchRatMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            var fairyMotion = this.motions.get(FairyMotion.class, FairyMotion::new);
            fairyMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            var spikedBeetleMotion = this.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new);
            spikedBeetleMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_PINCER) {
            var pincerMotion = this.motions.get(PincerMotion.class, PincerMotion::new);
            pincerMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUSH_CRAWLER) {
            var bushCrawlerMotion = this.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new);
            bushCrawlerMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MIMIC) {
            var mimicMotion = this.motions.get(MimicMotion.class, MimicMotion::new);
            mimicMotion.initialize(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA) {
            var maskedMimicMotion = this.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new);
            maskedMimicMotion.initialize(entity.slot());
        }
    }

    private void initializeBossAndFollowerEntities(RoomEntity entity) {
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MINI_MOLDORM) {
            var miniMoldormMotion = this.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new);
            miniMoldormMotion.initialize(entity);
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
            var moldormMotion = this.motions.get(MoldormMotion.class, MoldormMotion::new);
            moldormMotion.initialize(entity);
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES) {
            var rollingBonesMotion = this.motions.get(RollingBonesMotion.class, RollingBonesMotion::new);
            rollingBonesMotion.initializeBoss(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
            var rollingBonesMotion = this.motions.get(RollingBonesMotion.class, RollingBonesMotion::new);
            rollingBonesMotion.initializeBar(entity.slot());
        }
        if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CUCCO) {
            var cuccoMotion = this.motions.get(CuccoMotion.class, CuccoMotion::new);
            cuccoMotion.initialize(entity.slot());
        }
        if (entity.status() == EntityStatus.DYING) {
            this.combatTables.writePowerRecoilDeath(entity.slot(), entity.powerRecoilDeath());
        }
        if (isLaserType(entity.type())) {
            var laserMotion = this.motions.get(LaserMotion.class, LaserMotion::new);
            laserMotion.initializeForEntity(entity);
        }
        if (isFollowingNpcType(entity.type())) {
            if (entity.type() == EntityRomConstants.ENTITY_BOW_WOW) {
                var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
                bowWowMotion.initialize(entity.slot());
            } else {
                var followingNpcMotion = this.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
                followingNpcMotion.initialize(entity.slot(), entity.type());
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

    // These symbols are not yet in the shared generated RomSymbols allowlist.
    private static final RomAddress ENTITYPRESENTATIONROM_HEART_PIECE_VARIANTS =
            new RomAddress(0x03, 0x5B17); // HeartPieceSpriteVariants
    private static final RomAddress ENTITYPRESENTATIONROM_INSTRUMENT_PALETTE =
            new RomAddress(0x36, 0x7096); // InstrumentPalette
    private static final RomAddress ENTITYPRESENTATIONROM_INSTRUMENT_WARP_SPRITES =
            new RomAddress(0x06, 0x773C); // Data_006_773C
    private static final RomAddress ENTITYPRESENTATIONROM_HINT_TILES = new RomAddress(0x07, 0x6493); // Data_007_6493
    private static final RomAddress ENTITYPRESENTATIONROM_HINT_ATTRIBUTES =
            new RomAddress(0x07, 0x64DB); // Data_007_64F6, unpatched ROM
}
