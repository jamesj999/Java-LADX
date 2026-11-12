package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Arrays;
import linksawakening.world.entity.handler.EntityHandlerFamilies;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.WarpMotion;

/** ROM presentation loading and initial room slot setup in source order. */
final class EntityRoomInitialization {
    private final RoomEntityRuntime runtime;

    EntityRoomInitialization(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    EntityHandlerRegistry createEntityHandlers() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        registry.register(
                EntityRomConstants.ENTITY_KEESE,
                (entity, random) -> runtime.entityState.keeseMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_IRON_MASK,
                (entity, random) -> runtime.entityState.roamingEnemyMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_TEKTITE,
                (entity, random) -> runtime.entityState.tektiteMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_LEEVER,
                (entity, random) -> runtime.entityState.leeverMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_ANTI_FAIRY,
                (entity, random) -> runtime.entityState.antiFairyMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_HIDING_ZOL,
                (entity, random) -> runtime.entityState.hidingZolMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_STAR,
                (entity, random) -> runtime.entityState.starMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_BLOOPER,
                (entity, random) -> runtime.entityState.blooperMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_PINCER,
                (entity, random) -> runtime.entityState.pincerMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_WINGED_OCTOROK,
                (entity, random) -> runtime.entityState.wingedOctorokMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_SPIKE_TRAP,
                (entity, random) -> runtime.entityState.spikeTrapMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_PAIRODD,
                (entity, random) -> runtime.entityState.pairoddMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_PAIRODD_PROJECTILE,
                (entity, random) -> runtime.entityState.pairoddProjectileMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_WATER_TEKTITE,
                (entity, random) -> runtime.entityState.waterTektiteMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_FISH,
                (entity, random) -> runtime.entityState.fishMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_CROW,
                (entity, random) -> runtime.entityState.crowMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_CUCCO,
                (entity, random) -> runtime.entityState.cuccoMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_POKEY,
                (entity, random) -> runtime.entityState.pokeyMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_PIRANHA_PLANT,
                (entity, random) -> runtime.entityState.piranhaMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_ZORA,
                (entity, random) -> runtime.entityState.zoraMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_ZOMBIE,
                (entity, random) -> runtime.entityState.zombieMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_BUZZ_BLOB,
                (entity, random) -> runtime.entityState.buzzBlobMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_SAND_CRAB,
                (entity, random) -> runtime.entityState.sandCrabMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_DOG,
                (entity, random) -> runtime.entityState.dogMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_URCHIN,
                (entity, random) -> runtime.entityState.urchinMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_WITCH_RAT,
                (entity, random) -> runtime.entityState.witchRatMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_BOO_BUDDY,
                (entity, random) -> runtime.entityState.booBuddyMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_DROPPABLE_FAIRY,
                (entity, random) -> runtime.entityState.fairyMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE,
                (entity, random) -> runtime.entityState.stalfosAggressiveMotion.initialize(entity.slot(), random));
        registry.register(
                EntityRomConstants.ENTITY_STALFOS_EVASIVE,
                (entity, random) -> runtime.entityState.stalfosEvasiveMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_GIBDO,
                (entity, random) -> runtime.entityState.gibdoMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_LIKE_LIKE,
                (entity, random) -> runtime.entityState.likeLikeMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_POLS_VOICE,
                (entity, random) -> runtime.entityState.polsVoiceMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_GOOMBA,
                (entity, random) -> runtime.entityState.goombaMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_SNAKE,
                (entity, random) -> runtime.entityState.snakeMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_PEAHAT,
                (entity, random) -> runtime.entityState.peaHatMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_ARMOS_STATUE,
                (entity, random) -> runtime.entityState.armosMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_ARMOS_KNIGHT,
                (entity, random) -> runtime.entityState.armosKnightMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_HINOX,
                (entity, random) -> runtime.entityState.hinoxMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_HARDHAT_BEETLE,
                (entity, random) -> runtime.entityState.hardHatMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_SPIKED_BEETLE,
                (entity, random) -> runtime.entityState.spikedBeetleMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_BOMBER,
                (entity, random) -> runtime.entityState.bomberMotion.initialize(entity.slot()));
        registry.register(
                EntityRomConstants.ENTITY_MAD_BOMBER,
                (entity, random) -> runtime.entityState.madBomberMotion.initialize(entity.slot()));
        registry.registerAdvance(
                EntityRomConstants.ENTITY_BUTTERFLY,
                (entity, context) -> runtime.entityState.butterflyMotion.advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier()));
        registry.registerAdvance(
                EntityRomConstants.ENTITY_KEESE,
                (entity, context) -> runtime.entityState.keeseMotion.advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier()));
        registry.registerAdvance(
                EntityRomConstants.ENTITY_TEKTITE,
                (entity, context) -> runtime.entityState.tektiteMotion.advance(
                        entity,
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier(),
                        context.backgroundCollision()));
        registry.registerAdvance(
                EntityRomConstants.ENTITY_LEEVER,
                (entity, context) -> runtime.entityState.leeverMotion.advance(
                        entity,
                        context.frameCounter(),
                        context.linkEntityX(),
                        context.linkEntityY(),
                        context.randomByteSupplier(),
                        context.backgroundCollision()));
        registry.registerAdvance(
                EntityRomConstants.ENTITY_ANTI_FAIRY,
                (entity, context) -> runtime.entityState.antiFairyMotion.advance(
                        entity, context.frameCounter(), context.backgroundCollision(), context.randomByteSupplier()));
        registry.registerAdvance(
                EntityRomConstants.ENTITY_GIBDO,
                (entity, context) -> runtime.entityState.gibdoMotion.advance(
                        entity, context.randomByteSupplier(), context.backgroundCollision()));
        EntityHandlerFamilies.install(registry);
        return registry;
    }

    void initializeRomPresentation(byte[] romData) {
        if (romData != null) {
            for (int i = 0; i < runtime.entityState.heartPieceMeterPairs.length; i++)
                runtime.entityState.heartPieceMeterPairs[i] = romData[3 * 0x4000 + 0x1B17 + i] & 255;
            for (int i = 0; i < runtime.entityState.instrumentPaletteCycle.length; i++) {
                int offset = 0x36 * 0x4000 + 0x3096 + i * 2;
                int rgb555 = (romData[offset] & 255) | ((romData[offset + 1] & 255) << 8);
                runtime.entityState.instrumentPaletteCycle[i] = linksawakening.rom.RomUtil.decodeRgb555(rgb555);
            }
            for (int i = 0; i < runtime.entityState.instrumentWarpSprites.length; i++) {
                runtime.entityState.instrumentWarpSprites[i] = romData[6 * 0x4000 + 0x373C + i] & 255;
            }
            // Eighteen fixed repeat commands, two rows per controller frame.
            // Shipped ROM symbols: Data_007_6493=$6493, Data_007_64F6=$64DB.
            for (int row = 0; row < 18; row++) {
                int tile = romData[7 * 0x4000 + 0x2493 + row * 4 + 3] & 255;
                int attr = romData[7 * 0x4000 + 0x24DB + row * 4 + 3] & 255;
                java.util.Arrays.fill(runtime.entityState.instrumentHintTiles, row * 20, (row + 1) * 20, tile);
                java.util.Arrays.fill(runtime.entityState.instrumentHintAttributes, row * 20, (row + 1) * 20, attr);
            }
        }
    }

    void initializeRoomSlots(RoomEntitySnapshot initial) {

        runtime.entityState.hookshotChainOam = initial.hookshotChainOam();
        runtime.entityState.pincerBodyOam = initial.pincerBodyOam();
        runtime.entityState.wingedOctorokOam = initial.wingedOctorokOam();
        Arrays.fill(runtime.entityState.kidBall, false);
        Arrays.fill(runtime.entityState.kidPlayTimer, 0);
        Arrays.fill(runtime.entityState.kidPlayState, 0);
        Arrays.fill(runtime.entityState.kidZSpeed, 0);
        Arrays.fill(runtime.entityState.kidKidnapState, 0);
        Arrays.fill(runtime.entityState.kidKidnapDelay, 0);
        Arrays.fill(runtime.entityState.kidSpeedXAccumulator, 0);
        Arrays.fill(runtime.entityState.kidSpeedYAccumulator, 0);
        Arrays.fill(runtime.entityState.entityOptions1Override, -1);
        Arrays.fill(runtime.entityState.droppedItemBySlot, 0);
        Arrays.fill(runtime.entityState.bombDirection, 0xFF);
        Arrays.fill(runtime.bombFinalPresentationPending, false);
        Arrays.fill(runtime.entityState.thrownDirection, 0xFF);
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == 0x71) {
                runtime.entityState.kidPlayState[entity.slot()] = 1;
                runtime.entityState.kidPlayTimer[entity.slot()] = 0x20;
            }
            runtime.entityState.baseEntityFlipAttribute[entity.slot()] = entity.entityFlipAttribute();
            runtime.entityState.fallingVisualYOffset[entity.slot()] = initial.visualYOffset(entity.slot());
            if (entity.loaded()) runtime.initializeHealthGroup(entity.slot());
            runtime.entityState.enemyHealth[entity.slot()] = entity.loaded() ? runtime.initialHealth(entity.type()) : 0;
            runtime.entityState.enemyPhysicsFlags[entity.slot()] =
                    entity.loaded() ? initialPhysicsFlags(entity.type()) : 0;
            runtime.entityState.enemyHitboxFlags[entity.slot()] =
                    entity.loaded() ? initialHitboxFlags(entity.type()) : 0;
            // Room-loaded indoor warps start at state 1 ($03:4F75).
            // State 0 is reserved for dynamically spawned warps.
            if (entity.loaded() && runtime.indoorRoom && entity.type() == WarpMotion.ENTITY_TYPE) {
                runtime.entityState.warpState[entity.slot()] = WarpMotion.STATE_APPROACH;
                runtime.slots[entity.slot()] = withPositionAndVariant(entity, entity.x(), entity.y(), -1);
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_GENIE) {
                GenieMotion.JarState jar = runtime.entityState.genieMotion.initialize(entity.slot());
                runtime.genie.setGenieActiveState(entity.slot(), 0);
                runtime.genie.setGeniePrivateState1(entity.slot(), jar.privateState1());
                runtime.genie.setGeniePrivateState3(entity.slot(), 0);
                runtime.entityState.enemyHealth[entity.slot()] = jar.health();
                runtime.entityState.enemyPhysicsFlags[entity.slot()] = jar.physicsFlags();
                runtime.entityState.enemyHitboxFlags[entity.slot()] = jar.hitboxFlags();
                runtime.entityState.entityOptions1Override[entity.slot()] = EntityRomConstants.GENIE_OPTIONS1;
                if (entity.status() == EntityStatus.INIT) {
                    runtime.slots[entity.slot()] =
                            withPositionAndVariant(entity, entity.x() + 0x08, entity.y(), entity.spriteVariant());
                }
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CHEST_WITH_ITEM) {
                runtime.entityState.chestItemBySlot[entity.slot()] = entity.spriteVariant();
            }
            if (entity.loaded() && isBombiteType(entity.type())) {
                runtime.entityState.bombiteMotion.initialize(entity.slot());
                // The bank-$04 handlers use the global slow-transition tick
                // for TimerBombite's fuse. Keep it live for either family;
                // BouncingBombite simply leaves its value at zero.
                runtime.entityState.slowTimerInitialized[entity.slot()] = true;
            }
            if (entity.loaded() && isGhiniType(entity.type())) {
                runtime.entityState.ghiniMotion.initialize(entity.slot(), entity.type());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE) {
                runtime.entityState.likeLikeMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
                runtime.entityState.polsVoiceMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_FISH) {
                runtime.entityState.fishMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CROW) {
                runtime.entityState.crowMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY) {
                runtime.entityState.booBuddyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUZZ_BLOB) {
                runtime.entityState.buzzBlobMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SAND_CRAB) {
                runtime.entityState.sandCrabMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DOG) {
                runtime.entityState.dogMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_URCHIN) {
                runtime.entityState.urchinMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_WITCH_RAT) {
                runtime.entityState.witchRatMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
                runtime.entityState.fairyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
                runtime.entityState.spikedBeetleMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_PINCER) {
                runtime.entityState.pincerMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BUSH_CRAWLER) {
                runtime.entityState.bushCrawlerMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MIMIC) {
                runtime.entityState.mimicMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA) {
                runtime.entityState.maskedMimicMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MINI_MOLDORM) {
                runtime.entityState.miniMoldormMotion.initialize(entity);
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
                runtime.entityState.moldormMotion.initialize(entity);
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES) {
                runtime.entityState.rollingBonesMotion.initializeBoss(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
                runtime.entityState.rollingBonesMotion.initializeBar(entity.slot());
            }
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_CUCCO) {
                runtime.entityState.cuccoMotion.initialize(entity.slot());
            }
            if (entity.status() == EntityStatus.DYING) {
                runtime.entityState.powerRecoilDeath[entity.slot()] = entity.powerRecoilDeath();
            }
            if (isLaserType(entity.type())) {
                runtime.entityState.laserMotion.initializeForEntity(entity);
            }
            if (isFollowingNpcType(entity.type())) {
                if (entity.type() == EntityRomConstants.ENTITY_BOW_WOW) {
                    runtime.entityState.bowWowMotion.initialize(entity.slot());
                } else {
                    runtime.entityState.followingNpcMotion.initialize(entity.slot(), entity.type());
                }
            }
            if (entity.status() == EntityStatus.LIFTED) {
                runtime.entityState.liftedStateInitialized[entity.slot()] = true;
                runtime.entityState.liftedSourceDirection[entity.slot()] = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                runtime.entityState.liftedEffectiveDirection = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                runtime.entityState.enemyTransitionCountdown[entity.slot()] = 0x02;
                runtime.entityState.liftedEntitySlot = entity.slot();
            }
            if (entity.status() == EntityStatus.THROWN) {
                runtime.entityState.thrownDirection[entity.slot()] = ThrownEntityMotion.ROM_DIRECTION_DOWN;
            }
        }
    }
}
