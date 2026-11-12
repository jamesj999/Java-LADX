package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaFlowerMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;
import linksawakening.world.room.ChestContentsTable;

/** ROM per-slot options, sprite definitions and handler metadata. */
public final class EntitySlotProperties {
    private final RoomEntityRuntime runtime;

    EntitySlotProperties(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    void resolveThreeOfAKindPuzzle() {
        ThreeOfAKindMotion.PuzzleResult result = runtime.threeOfAKindMotion.resolvePuzzle(
                Arrays.asList(runtime.slots), runtime.enemyTransitionCountdown);
        if (result == ThreeOfAKindMotion.PuzzleResult.NONE) {
            return;
        }
        int coordinatorSlot = Arrays.stream(runtime.slots)
                .filter(entity -> entity.status() == EntityStatus.ACTIVE
                        && entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND)
                .mapToInt(RoomEntity::slot)
                .findFirst()
                .orElse(0);
        int jingle = result == ThreeOfAKindMotion.PuzzleResult.MATCH
                ? runtime.threeOfAKindMotion.matchedJingle(coordinatorSlot)
                : 0x1D;
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                coordinatorSlot,
                EntityRomConstants.ENTITY_THREE_OF_A_KIND,
                0,
                false,
                EntityCombatEvent.SoundChannel.JINGLE,
                jingle));
        if (result != ThreeOfAKindMotion.PuzzleResult.MATCH) {
            return;
        }
        for (int index = 0; index < runtime.slots.length; index++) {
            RoomEntity entity = runtime.slots[index];
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_THREE_OF_A_KIND) {
                continue;
            }
            int slot = entity.slot();
            runtime.droppedItemBySlot[slot] = runtime.threeOfAKindMotion.matchedDrop(slot);
            runtime.dyingCountdown[slot] = 0x1F;
            runtime.enemyHealth[slot] = 0;
            runtime.enemyPhysicsFlags[slot] = 0x04;
            runtime.slots[index] = withStatus(entity, EntityStatus.DYING);
        }
        // The source writes the shared noise register once per card in the
        // same handler pass; only the final register value is audible.
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                coordinatorSlot,
                EntityRomConstants.ENTITY_THREE_OF_A_KIND,
                0,
                false,
                EntityCombatEvent.SoundChannel.NOISE,
                0x13));
    }

    int initialHealth(int type) {
        return runtime.roomInspection.initialHealth(type);
    }

    int contactDamage(int slot) {
        return runtime.roomInspection.contactDamage(slot);
    }

    public RoomEntity advanceMusicalNoteEntity(RoomEntity entity) {
        int slot = entity.slot();
        int inertia = (runtime.musicalNoteInertia[slot] - 1) & 0xFF;
        runtime.musicalNoteInertia[slot] = inertia;
        if (inertia == 0) {
            runtime.clearEntity(slot);
            return null;
        }

        int speedXAdjustment = (inertia & 0x10) != 0 ? -1 : 1;
        if ((inertia & 0x01) == 0) {
            runtime.musicalNoteSpeedX[slot] = (runtime.musicalNoteSpeedX[slot] + speedXAdjustment) & 0xFF;
        }
        int x = (entity.x() + signedByte(runtime.musicalNoteSpeedX[slot])) & 0xFF;
        int y = (entity.y() + signedByte(runtime.musicalNoteSpeedY[slot])) & 0xFF;
        return withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    void updateHookshotChainOam(int linkEntityX, int linkEntityY, int frameCounter) {
        int slot = runtime.inspection().hookshotSlot();
        if (slot < 0 || !runtime.slots[slot].loaded()) {
            runtime.hookshotChainOam = List.of();
            return;
        }
        RoomEntity chain = runtime.slots[slot];
        runtime.hookshotChainOam =
                HookshotChainOam.entries(chain.x(), chain.y(), linkEntityX, linkEntityY, frameCounter);
    }

    void updateBowWowChain(int frame) {
        List<BowWowChain.Entry> entries = new ArrayList<>();
        // The handler's dialogue/inventory/transition branches still run the
        // chain. Its sequence check only returns early on the unpaused path.
        if (runtime.transitionSequenceCounter != 4
                && !runtime.roomTransitionActive
                && !runtime.linkState.dialogActive
                && !runtime.linkState.inventoryAppearing
                && !runtime.inspection().witchGotItemPresentationActive()) {
            runtime.bowWowChainOam = List.of();
            return;
        }
        boolean moving = (runtime.linkState.currentLinkSpeedX
                        | runtime.linkState.currentLinkSpeedY
                        | runtime.linkState.currentLinkVelocityZ)
                != 0;
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            RoomEntity entity = runtime.slots[slot];
            if (entity.type() != EntityRomConstants.ENTITY_BOW_WOW || entity.status() != EntityStatus.ACTIVE) continue;
            runtime.slots[slot] = runtime.bowWowMotion.advanceChain(entity, runtime.bowWowState, moving);
            entries.addAll(runtime.bowWowMotion.chainEntries(slot, runtime.bowWowState, frame));
        }
        runtime.bowWowChainOam = List.copyOf(entries);
    }

    void updatePincerBodyOam() {
        List<PincerBodyOam.Entry> entries = new ArrayList<>();
        for (RoomEntity entity : runtime.slots) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_PINCER) {
                continue;
            }
            entries.addAll(PincerBodyOam.entries(
                    entity.slot(),
                    runtime.pincerMotion.holeX(entity.slot()),
                    runtime.pincerMotion.holeY(entity.slot()),
                    entity.x(),
                    entity.y(),
                    runtime.pincerMotion.state(entity.slot())));
        }
        runtime.pincerBodyOam = List.copyOf(entries);
    }

    void updateWingedOctorokOam() {
        List<WingedOctorokOam.Entry> entries = new ArrayList<>();
        for (RoomEntity entity : runtime.slots) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_WINGED_OCTOROK) {
                continue;
            }
            entries.addAll(WingedOctorokOam.entries(
                    entity.slot(),
                    runtime.wingedOctorokMotion.privateState2(entity.slot()),
                    entity.x(),
                    entity.y(),
                    entity.z()));
        }
        runtime.wingedOctorokOam = List.copyOf(entries);
    }

    public boolean kanaletCrowTakeoffTriggered(RoomEntity crow) {
        for (RoomEntity candidate : runtime.slots) {
            if (!candidate.loaded()) {
                continue;
            }
            if (candidate.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                    && runtime.liftableRockSmashActive[candidate.slot()]
                    && runtime.liftableRockSmashSourceVariant[candidate.slot()]
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
            int countdown = runtime.enemyTransitionCountdown[candidate.slot()] & 0xFF;
            if (runtime.enemyFlashCountdown[candidate.slot()] == 0 || countdown >= 0x22) {
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

    public EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        return runtime.roomInspection.spriteDefinitionFor(entityType);
    }

    public EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) {
        return runtime.roomInspection.spriteDefinitionForBushCrawlerState(privateState1);
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return runtime.roomInspection.spriteDefinitionForPokeySegment();
    }

    public EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) {
        return runtime.roomInspection.spriteDefinitionForBushCrawlerCrawlState(privateState4);
    }

    EntitySpriteDefinition spriteDefinitionForChest(int itemType) {
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_CHEST_WITH_ITEM);
        }
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_CHEST_WITH_ITEM);
        }
        int roomId = runtime.spriteSelection == null ? -1 : runtime.spriteSelection.roomId();
        if (runtime.entityMapId >= 0 && roomId >= 0) {
            return runtime.spriteHandlers.forChestState(runtime.entityMapId, roomId, itemType);
        }
        return runtime.spriteHandlers.forEntityType(
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                runtime.spriteSelection == null
                        ? (runtime.indoorRoom
                                ? EntityRoomLoader.RoomTable.INDOORS_A
                                : EntityRoomLoader.RoomTable.OVERWORLD)
                        : runtime.spriteSelection.roomTable(),
                runtime.entityMapId);
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
        return runtime.roomInspection.spriteDefinitionForEvasiveState(fallback, privateState1);
    }

    public RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return runtime.roomInspection.refreshColorShellDisplay(entity, status);
    }

    public int options1(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (runtime.entityOptions1Override[slot] >= 0) {
            return runtime.entityOptions1Override[slot];
        }
        if (isGhiniType(runtime.slots[slot].type())) {
            return EntityRomConstants.GHINI_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_BOMB) {
            return EntityRomConstants.BOMB_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_BOMBER) {
            return EntityRomConstants.BOMBER_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_MAD_BOMBER) {
            return EntityRomConstants.MAD_BOMBER_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_WIZROBE_PROJECTILE) {
            return EntityRomConstants.WIZROBE_PROJECTILE_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                    | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_KEY_DROP_POINT) {
            return EntityRomConstants.KEY_DROP_POINT_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
            return EntityRomConstants.ARMOS_KNIGHT_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_HINOX) {
            return EntityRomConstants.HINOX_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_ROLLING_BONES) {
            return 0x84;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
            return 0x40;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_CHEST_WITH_ITEM) {
            return EntityRomConstants.CHEST_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_FISH) {
            return EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_CROW) {
            return EntityRomConstants.ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_CUCCO) {
            return EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_GOPONGA_FLOWER) {
            return GopongaFlowerMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER) {
            return GiantGopongaMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_POKEY) {
            return PokeyMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_ZORA) {
            return ZoraMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_ZOMBIE) {
            return ZombieMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_BUZZ_BLOB) {
            return BuzzBlobMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_SAND_CRAB) {
            return SandCrabMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_URCHIN) {
            return UrchinMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_WITCH_RAT) {
            return WitchRatMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return GopongaProjectileMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_BOO_BUDDY) {
            return BooBuddyMotion.OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            return EntityRomConstants.FAIRY_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            // The static table starts at splash-only. The handler writes the
            // sword-clink-off bit on its first normal active pass.
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_MINI_MOLDORM) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_MOLDORM) {
            return MoldormMotion.INITIAL_OPTIONS1;
        }
        if (runtime.slots[slot].type() == EntityRomConstants.ENTITY_MIMIC) {
            return EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (isBombiteType(runtime.slots[slot].type())) {
            return EntityRomConstants.BOMBITE_OPTIONS1;
        }
        return runtime.slots[slot].type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE
                        || runtime.slots[slot].type() == EntityRomConstants.ENTITY_LIKE_LIKE
                ? EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                : 0;
    }
}
