package linksawakening.world.entity;

import linksawakening.world.entity.handler.EntityHandlerFamilies;

import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.LiftedEntityMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.WarpMotion;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import java.util.Arrays;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM presentation loading and initial room slot setup in source order. */
final class EntityRoomInitialization {
    private final RoomEntityRuntime runtime;
    EntityRoomInitialization(RoomEntityRuntime runtime) { this.runtime = runtime; }

    EntityHandlerRegistry createEntityHandlers() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        registry.register(runtime.ENTITY_KEESE, (entity, random) -> runtime.keeseMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_IRON_MASK, (entity, random) -> runtime.roamingEnemyMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_TEKTITE, (entity, random) -> runtime.tektiteMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_LEEVER, (entity, random) -> runtime.leeverMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_ANTI_FAIRY, (entity, random) -> runtime.antiFairyMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_HIDING_ZOL, (entity, random) -> runtime.hidingZolMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_STAR, (entity, random) -> runtime.starMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_BLOOPER, (entity, random) -> runtime.blooperMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_PINCER, (entity, random) -> runtime.pincerMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_WINGED_OCTOROK, (entity, random) -> runtime.wingedOctorokMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_SPIKE_TRAP, (entity, random) -> runtime.spikeTrapMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_PAIRODD, (entity, random) -> runtime.pairoddMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_PAIRODD_PROJECTILE, (entity, random) -> runtime.pairoddProjectileMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_WATER_TEKTITE, (entity, random) -> runtime.waterTektiteMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_FISH, (entity, random) -> runtime.fishMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_CROW, (entity, random) -> runtime.crowMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_CUCCO, (entity, random) -> runtime.cuccoMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_POKEY, (entity, random) -> runtime.pokeyMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_PIRANHA_PLANT, (entity, random) -> runtime.piranhaMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_ZORA, (entity, random) -> runtime.zoraMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_ZOMBIE, (entity, random) -> runtime.zombieMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_BUZZ_BLOB, (entity, random) -> runtime.buzzBlobMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_SAND_CRAB, (entity, random) -> runtime.sandCrabMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_DOG, (entity, random) -> runtime.dogMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_URCHIN, (entity, random) -> runtime.urchinMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_WITCH_RAT, (entity, random) -> runtime.witchRatMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_BOO_BUDDY, (entity, random) -> runtime.booBuddyMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_DROPPABLE_FAIRY, (entity, random) -> runtime.fairyMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_STALFOS_AGGRESSIVE, (entity, random) -> runtime.stalfosAggressiveMotion.initialize(entity.slot(), random));
        registry.register(runtime.ENTITY_STALFOS_EVASIVE, (entity, random) -> runtime.stalfosEvasiveMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_GIBDO, (entity, random) -> runtime.gibdoMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_LIKE_LIKE, (entity, random) -> runtime.likeLikeMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_POLS_VOICE, (entity, random) -> runtime.polsVoiceMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_GOOMBA, (entity, random) -> runtime.goombaMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_SNAKE, (entity, random) -> runtime.snakeMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_PEAHAT, (entity, random) -> runtime.peaHatMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_ARMOS_STATUE, (entity, random) -> runtime.armosMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_ARMOS_KNIGHT, (entity, random) -> runtime.armosKnightMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_HINOX, (entity, random) -> runtime.hinoxMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_HARDHAT_BEETLE, (entity, random) -> runtime.hardHatMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_SPIKED_BEETLE, (entity, random) -> runtime.spikedBeetleMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_BOMBER, (entity, random) -> runtime.bomberMotion.initialize(entity.slot()));
        registry.register(runtime.ENTITY_MAD_BOMBER, (entity, random) -> runtime.madBomberMotion.initialize(entity.slot()));
        registry.registerAdvance(runtime.ENTITY_BUTTERFLY, (entity, context) -> runtime.butterflyMotion.advance(entity, context.frameCounter(), context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier()));
        registry.registerAdvance(runtime.ENTITY_KEESE, (entity, context) -> runtime.keeseMotion.advance(entity, context.frameCounter(), context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier()));
        registry.registerAdvance(runtime.ENTITY_TEKTITE, (entity, context) -> runtime.tektiteMotion.advance(entity, context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier(), context.backgroundCollision()));
        registry.registerAdvance(runtime.ENTITY_LEEVER, (entity, context) -> runtime.leeverMotion.advance(entity, context.frameCounter(), context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier(), context.backgroundCollision()));
        registry.registerAdvance(runtime.ENTITY_ANTI_FAIRY, (entity, context) -> runtime.antiFairyMotion.advance(entity, context.frameCounter(), context.backgroundCollision(),
                    context.randomByteSupplier()));
        registry.registerAdvance(runtime.ENTITY_GIBDO, (entity, context) -> runtime.gibdoMotion.advance(entity, context.randomByteSupplier(), context.backgroundCollision()));
        EntityHandlerFamilies.install(registry);
        return registry;
    }

    void initializeRomPresentation(byte[] romData) {
if (romData != null) {
            for (int i = 0; i < runtime.heartPieceMeterPairs.length; i++) runtime.heartPieceMeterPairs[i] =
                romData[3 * 0x4000 + 0x1B17 + i] & 255;
            for (int i = 0; i < runtime.instrumentPaletteCycle.length; i++) {
                int offset = 0x36 * 0x4000 + 0x3096 + i * 2;
                int rgb555 = (romData[offset] & 255) | ((romData[offset + 1] & 255) << 8);
                runtime.instrumentPaletteCycle[i] = linksawakening.rom.RomUtil.decodeRgb555(rgb555);
            }
            for (int i = 0; i < runtime.instrumentWarpSprites.length; i++) {
                runtime.instrumentWarpSprites[i] = romData[6 * 0x4000 + 0x373C + i] & 255;
            }
            // Eighteen fixed repeat commands, two rows per controller frame.
            // Shipped ROM symbols: Data_007_6493=$6493, Data_007_64F6=$64DB.
            for (int row = 0; row < 18; row++) {
                int tile = romData[7 * 0x4000 + 0x2493 + row * 4 + 3] & 255;
                int attr = romData[7 * 0x4000 + 0x24DB + row * 4 + 3] & 255;
                java.util.Arrays.fill(runtime.instrumentHintTiles, row * 20, (row + 1) * 20, tile);
                java.util.Arrays.fill(runtime.instrumentHintAttributes, row * 20, (row + 1) * 20, attr);
            }
        }
    }

    void initializeRoomSlots(RoomEntitySnapshot initial) {

        runtime.hookshotChainOam = initial.hookshotChainOam();
        runtime.pincerBodyOam = initial.pincerBodyOam();
        runtime.wingedOctorokOam = initial.wingedOctorokOam();
        Arrays.fill(runtime.entityOptions1Override, -1);
        Arrays.fill(runtime.droppedItemBySlot, 0);
        Arrays.fill(runtime.bombDirection, 0xFF);
        Arrays.fill(runtime.bombFinalPresentationPending, false);
        Arrays.fill(runtime.thrownDirection, 0xFF);
        for (RoomEntity entity : runtime.slots) {
            runtime.baseEntityFlipAttribute[entity.slot()] = entity.entityFlipAttribute();
            runtime.fallingVisualYOffset[entity.slot()] = initial.visualYOffset(entity.slot());
            runtime.enemyHealth[entity.slot()] = entity.loaded() ? runtime.initialHealth(entity.type()) : 0;
            runtime.enemyPhysicsFlags[entity.slot()] = entity.loaded()
                ? initialPhysicsFlags(entity.type()) : 0;
            runtime.enemyHitboxFlags[entity.slot()] = entity.loaded()
                ? initialHitboxFlags(entity.type()) : 0;
            // Room-loaded indoor warps start at state 1 ($03:4F75).
            // State 0 is reserved for dynamically spawned warps.
            if (entity.loaded() && runtime.indoorRoom && entity.type() == WarpMotion.ENTITY_TYPE) {
                runtime.warpState[entity.slot()] = WarpMotion.STATE_APPROACH;
                runtime.slots[entity.slot()] = withPositionAndVariant(entity, entity.x(), entity.y(), -1);
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_GENIE) {
                GenieMotion.JarState jar = runtime.genieMotion.initialize(entity.slot());
                runtime.genieActiveState[entity.slot()] = 0;
                runtime.geniePrivateState1[entity.slot()] = jar.privateState1();
                runtime.geniePrivateState3[entity.slot()] = 0;
                runtime.enemyHealth[entity.slot()] = jar.health();
                runtime.enemyPhysicsFlags[entity.slot()] = jar.physicsFlags();
                runtime.enemyHitboxFlags[entity.slot()] = jar.hitboxFlags();
                runtime.entityOptions1Override[entity.slot()] = runtime.GENIE_OPTIONS1;
                if (entity.status() == EntityStatus.INIT) {
                    runtime.slots[entity.slot()] = withPositionAndVariant(
                        entity, entity.x() + 0x08, entity.y(), entity.spriteVariant());
                }
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_CHEST_WITH_ITEM) {
                runtime.chestItemBySlot[entity.slot()] = entity.spriteVariant();
            }
            if (entity.loaded() && isBombiteType(entity.type())) {
                runtime.bombiteMotion.initialize(entity.slot());
                // The bank-$04 handlers use the global slow-transition tick
                // for TimerBombite's fuse. Keep it live for either family;
                // BouncingBombite simply leaves its value at zero.
                runtime.slowTimerInitialized[entity.slot()] = true;
            }
            if (entity.loaded() && isGhiniType(entity.type())) {
                runtime.ghiniMotion.initialize(entity.slot(), entity.type());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_LIKE_LIKE) {
                runtime.likeLikeMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_POLS_VOICE) {
                runtime.polsVoiceMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_FISH) {
                runtime.fishMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_CROW) {
                runtime.crowMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_BOO_BUDDY) {
                runtime.booBuddyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_BUZZ_BLOB) {
                runtime.buzzBlobMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_SAND_CRAB) {
                runtime.sandCrabMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_DOG) {
                runtime.dogMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_URCHIN) {
                runtime.urchinMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_WITCH_RAT) {
                runtime.witchRatMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_DROPPABLE_FAIRY) {
                runtime.fairyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_SPIKED_BEETLE) {
                runtime.spikedBeetleMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_PINCER) {
                runtime.pincerMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_BUSH_CRAWLER) {
                runtime.bushCrawlerMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_MIMIC) {
                runtime.mimicMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_MASKED_MIMIC_GORIYA) {
                runtime.maskedMimicMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_MINI_MOLDORM) {
                runtime.miniMoldormMotion.initialize(entity);
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_MOLDORM) {
                runtime.moldormMotion.initialize(entity);
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_ROLLING_BONES) {
                runtime.rollingBonesMotion.initializeBoss(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_ROLLING_BONES_BAR) {
                runtime.rollingBonesMotion.initializeBar(entity.slot());
            }
            if (entity.loaded() && entity.type() == runtime.ENTITY_CUCCO) {
                runtime.cuccoMotion.initialize(entity.slot());
            }
            if (entity.status() == EntityStatus.DYING) {
                runtime.powerRecoilDeath[entity.slot()] = entity.powerRecoilDeath();
            }
            if (isLaserType(entity.type())) {
                runtime.laserMotion.initializeForEntity(entity);
            }
            if (isFollowingNpcType(entity.type())) {
                if (entity.type() == runtime.ENTITY_BOW_WOW) {
                    runtime.bowWowMotion.initialize(entity.slot());
                } else {
                    runtime.followingNpcMotion.initialize(entity.slot(), entity.type());
                }
            }
            if (entity.status() == EntityStatus.LIFTED) {
                runtime.liftedStateInitialized[entity.slot()] = true;
                runtime.liftedSourceDirection[entity.slot()] = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                runtime.liftedEffectiveDirection = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                runtime.enemyTransitionCountdown[entity.slot()] = 0x02;
                runtime.liftedEntitySlot = entity.slot();
            }
            if (entity.status() == EntityStatus.THROWN) {
                runtime.thrownDirection[entity.slot()] = ThrownEntityMotion.ROM_DIRECTION_DOWN;
            }
        }
    
    }
}
