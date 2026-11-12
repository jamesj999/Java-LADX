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
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_BOMB) {
            return EntityRomConstants.BOMB_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_BOMBER) {
            return EntityRomConstants.BOMBER_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_MAD_BOMBER) {
            return EntityRomConstants.MAD_BOMBER_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_WIZROBE_PROJECTILE) {
            return EntityRomConstants.WIZROBE_PROJECTILE_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                    | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_KEY_DROP_POINT) {
            return EntityRomConstants.KEY_DROP_POINT_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
            return EntityRomConstants.ARMOS_KNIGHT_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_HINOX) {
            return EntityRomConstants.HINOX_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_ROLLING_BONES) {
            return 0x84;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
            return 0x40;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_CHEST_WITH_ITEM) {
            return EntityRomConstants.CHEST_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_FISH) {
            return EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_CROW) {
            return EntityRomConstants.ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_CUCCO) {
            return EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_GOPONGA_FLOWER) {
            return GopongaFlowerMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER) {
            return GiantGopongaMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_POKEY) {
            return PokeyMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_ZORA) {
            return ZoraMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_ZOMBIE) {
            return ZombieMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_BUZZ_BLOB) {
            return BuzzBlobMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_SAND_CRAB) {
            return SandCrabMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_URCHIN) {
            return UrchinMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_WITCH_RAT) {
            return WitchRatMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return GopongaProjectileMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_BOO_BUDDY) {
            return BooBuddyMotion.OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            return EntityRomConstants.FAIRY_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            // The static table starts at splash-only. The handler writes the
            // sword-clink-off bit on its first normal active pass.
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_MINI_MOLDORM) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_MOLDORM) {
            return MoldormMotion.INITIAL_OPTIONS1;
        }
        if (runtime.slots()[slot].type() == EntityRomConstants.ENTITY_MIMIC) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (isBombiteType(runtime.slots()[slot].type())) {
            return EntityRomConstants.BOMBITE_OPTIONS1;
        }
        return runtime.slots()[slot].type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE
                        || runtime.slots()[slot].type() == EntityRomConstants.ENTITY_LIKE_LIKE
                ? EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                : 0;
    }
}
