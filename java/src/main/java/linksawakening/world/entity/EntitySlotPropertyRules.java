package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaFlowerMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;
import linksawakening.world.room.ChestContentsTable;

/** Focused behavior using the state owned by EntitySlotProperties. */
final class EntitySlotPropertyRules {
    static boolean kanaletCrowTakeoffTriggered(EntitySlotProperties owner, RoomEntity crow) {
        var runtime = owner.runtime;
        for (RoomEntity candidate : runtime.slots()) {
            if (!candidate.loaded()) {
                continue;
            }
            if (candidate.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                    && runtime.liftThrow().readLiftableRockSmashActive(candidate.slot())
                    && runtime.liftThrow().readLiftableRockSmashSourceVariant(candidate.slot())
                            == EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK) {
                int distanceX = Math.abs(signedByte((crow.x() - candidate.x()) & 0xFF));
                int distanceY = signedByte(((crow.y() - crow.z()) - candidate.y()) & 0xFF);
                if (distanceX < 0x10 && distanceY >= -0x28 && distanceY < 0x28) {
                    return true;
                }
                continue;
            }
            if (candidate.type() != EntityRomConstants.ENTITY_BOMB) {
                continue;
            }
            int countdown = runtime.physicsTables().readEnemyTransitionCountdown(candidate.slot()) & 0xFF;
            if (runtime.combatTables().readEnemyFlashCountdown(candidate.slot()) == 0 || countdown >= 0x22) {
                continue;
            }
            int distanceX = Math.abs(signedByte((crow.x() - candidate.x()) & 0xFF));
            int distanceY = Math.abs(signedByte(((crow.y() - crow.z()) - candidate.y()) & 0xFF));
            if (distanceX < 0x20 && distanceY < 0x20) {
                return true;
            }
        }
        return false;
    }

    static EntitySpriteDefinition spriteDefinitionForChest(EntitySlotProperties owner, int itemType) {
        var runtime = owner.runtime;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_CHEST_WITH_ITEM);
        }
        if (runtime.spriteHandlers() == null) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_CHEST_WITH_ITEM);
        }
        int roomId = runtime.presentationState().readSpriteSelection() == null
                ? -1
                : runtime.presentationState().readSpriteSelection().roomId();
        if (runtime.roomVariables().readEntityMapId() >= 0 && roomId >= 0) {
            return runtime.spriteHandlers()
                    .forChestState(runtime.roomVariables().readEntityMapId(), roomId, itemType);
        }
        return runtime.spriteHandlers()
                .forEntityType(
                        EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                        runtime.presentationState().readSpriteSelection() == null
                                ? (runtime.indoorRoom()
                                        ? EntityRoomLoader.RoomTable.INDOORS_A
                                        : EntityRoomLoader.RoomTable.OVERWORLD)
                                : runtime.presentationState()
                                        .readSpriteSelection()
                                        .roomTable(),
                        runtime.roomVariables().readEntityMapId());
    }

    static int options1(EntitySlotProperties owner, int slot) {
        var runtime = owner.runtime;
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (runtime.physicsTables().readEntityOptions1Override(slot) >= 0) {
            return runtime.physicsTables().readEntityOptions1Override(slot);
        }
        if (isGhiniType(runtime.slots()[slot].type())) {
            return EntityRomConstants.GHINI_OPTIONS1;
        }
        return switch (runtime.slots()[slot].type()) {
            case EntityRomConstants.ENTITY_BOMB -> EntityRomConstants.BOMB_OPTIONS1;
            case EntityRomConstants.ENTITY_BOMBER -> EntityRomConstants.BOMBER_OPTIONS1;
            case EntityRomConstants.ENTITY_MAD_BOMBER -> EntityRomConstants.MAD_BOMBER_OPTIONS1;
            case EntityRomConstants.ENTITY_WIZROBE_PROJECTILE -> EntityRomConstants.WIZROBE_PROJECTILE_OPTIONS1;
            case EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP -> EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                    | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
            case EntityRomConstants.ENTITY_KEY_DROP_POINT -> EntityRomConstants.KEY_DROP_POINT_OPTIONS1;
            case EntityRomConstants.ENTITY_ARMOS_KNIGHT -> EntityRomConstants.ARMOS_KNIGHT_OPTIONS1;
            case EntityRomConstants.ENTITY_HINOX -> EntityRomConstants.HINOX_OPTIONS1;
            case EntityRomConstants.ENTITY_ROLLING_BONES -> 0x84;
            case EntityRomConstants.ENTITY_ROLLING_BONES_BAR -> 0x40;
            case EntityRomConstants.ENTITY_CHEST_WITH_ITEM -> EntityRomConstants.CHEST_OPTIONS1;
            case EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL -> EntityRomConstants
                    .SECRET_SEASHELL_HIDDEN_OPTIONS1;
            case EntityRomConstants.ENTITY_FISH -> EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
            case EntityRomConstants.ENTITY_CROW -> EntityRomConstants.ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS;
            case EntityRomConstants.ENTITY_CUCCO -> EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
            case EntityRomConstants.ENTITY_GOPONGA_FLOWER -> GopongaFlowerMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER -> GiantGopongaMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_POKEY -> PokeyMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_ZORA -> ZoraMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_ZOMBIE -> ZombieMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_BUZZ_BLOB -> BuzzBlobMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_SAND_CRAB -> SandCrabMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_URCHIN -> UrchinMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_WITCH_RAT -> WitchRatMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE -> GopongaProjectileMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_BOO_BUDDY -> BooBuddyMotion.OPTIONS1;
            case EntityRomConstants.ENTITY_DROPPABLE_FAIRY -> EntityRomConstants.FAIRY_OPTIONS1;
            case EntityRomConstants.ENTITY_POLS_VOICE -> EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
            case EntityRomConstants.ENTITY_MINI_MOLDORM -> EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
            case EntityRomConstants.ENTITY_MOLDORM -> MoldormMotion.INITIAL_OPTIONS1;
            case EntityRomConstants.ENTITY_MIMIC -> EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
            default -> fallbackOptions(runtime.slots()[slot].type());
        };
    }

    private static int fallbackOptions(int type) {
        if (isBombiteType(type)) {
            return EntityRomConstants.BOMBITE_OPTIONS1;
        }
        return type == EntityRomConstants.ENTITY_STALFOS_EVASIVE || type == EntityRomConstants.ENTITY_LIKE_LIKE
                ? EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                : 0;
    }
}
