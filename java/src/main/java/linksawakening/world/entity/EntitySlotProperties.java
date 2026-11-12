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
    private List<HookshotChainOam.Entry> hookshotChainOam = List.of();
    private List<PincerBodyOam.Entry> pincerBodyOam = List.of();
    private List<WingedOctorokOam.Entry> wingedOctorokOam = List.of();
    private final boolean[] wingedSwordAttackThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] wingedStateTwoThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private List<BowWowChain.Entry> bowWowChainOam = List.of();
    private final EntitySlotPropertiesServices runtime;

    EntitySlotProperties(EntitySlotPropertiesServices runtime) {
        this.runtime = runtime;
    }

    void resolveThreeOfAKindPuzzle() {
        ThreeOfAKindMotion.PuzzleResult result = runtime.motionSet()
                .threeOfAKindMotion
                .resolvePuzzle(Arrays.asList(runtime.slots()), runtime.physicsTables()::readEnemyTransitionCountdown);
        if (result == ThreeOfAKindMotion.PuzzleResult.NONE) {
            return;
        }
        int coordinatorSlot = Arrays.stream(runtime.slots())
                .filter(entity -> entity.status() == EntityStatus.ACTIVE
                        && entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND)
                .mapToInt(RoomEntity::slot)
                .findFirst()
                .orElse(0);
        int jingle = result == ThreeOfAKindMotion.PuzzleResult.MATCH
                ? runtime.motionSet().threeOfAKindMotion.matchedJingle(coordinatorSlot)
                : 0x1D;
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        coordinatorSlot,
                        EntityRomConstants.ENTITY_THREE_OF_A_KIND,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        jingle));
        if (result != ThreeOfAKindMotion.PuzzleResult.MATCH) {
            return;
        }
        for (int index = 0; index < runtime.slots().length; index++) {
            RoomEntity entity = runtime.slots()[index];
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_THREE_OF_A_KIND) {
                continue;
            }
            int slot = entity.slot();
            runtime.bossRewards()
                    .writeDroppedItemBySlot(
                            slot, runtime.motionSet().threeOfAKindMotion.matchedDrop(slot));
            runtime.combatTables().writeDyingCountdown(slot, 0x1F);
            runtime.combatTables().writeEnemyHealth(slot, 0);
            runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x04);
            runtime.slots()[index] = withStatus(entity, EntityStatus.DYING);
        }
        // The source writes the shared noise register once per card in the
        // same handler pass; only the final register value is audible.
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        coordinatorSlot,
                        EntityRomConstants.ENTITY_THREE_OF_A_KIND,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x13));
    }

    int initialHealth(int type) {
        return runtime.roomInspection().initialHealth(type);
    }

    int contactDamage(int slot) {
        return runtime.roomInspection().contactDamage(slot);
    }

    public RoomEntity advanceMusicalNoteEntity(RoomEntity entity) {
        int slot = entity.slot();
        int inertia = (runtime.projectiles().readMusicalNoteInertia(slot) - 1) & 0xFF;
        runtime.projectiles().writeMusicalNoteInertia(slot, inertia);
        if (inertia == 0) {
            runtime.clearEntity(slot);
            return null;
        }

        int speedXAdjustment = (inertia & 0x10) != 0 ? -1 : 1;
        if ((inertia & 0x01) == 0) {
            runtime.projectiles()
                    .writeMusicalNoteSpeedX(
                            slot, (runtime.projectiles().readMusicalNoteSpeedX(slot) + speedXAdjustment) & 0xFF);
        }
        int x = (entity.x() + signedByte(runtime.projectiles().readMusicalNoteSpeedX(slot))) & 0xFF;
        int y = (entity.y() + signedByte(runtime.projectiles().readMusicalNoteSpeedY(slot))) & 0xFF;
        return withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    void updateHookshotChainOam(int linkEntityX, int linkEntityY, int frameCounter) {
        int slot = runtime.inspection().hookshotSlot();
        if (slot < 0 || !runtime.slots()[slot].loaded()) {
            this.writeHookshotChainOam(List.of());
            return;
        }
        RoomEntity chain = runtime.slots()[slot];
        this.writeHookshotChainOam(
                HookshotChainOam.entries(chain.x(), chain.y(), linkEntityX, linkEntityY, frameCounter));
    }

    void updateBowWowChain(int frame) {
        List<BowWowChain.Entry> entries = new ArrayList<>();
        // The handler's dialogue/inventory/transition branches still run the
        // chain. Its sequence check only returns early on the unpaused path.
        if (runtime.roomVariables().readTransitionSequenceCounter() != 4
                && !runtime.roomVariables().readRoomTransitionActive()
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !runtime.inspection().witchGotItemPresentationActive()) {
            this.writeBowWowChainOam(List.of());
            return;
        }
        boolean moving = (runtime.linkState().currentLinkSpeedX
                        | runtime.linkState().currentLinkSpeedY
                        | runtime.linkState().currentLinkVelocityZ)
                != 0;
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            RoomEntity entity = runtime.slots()[slot];
            if (entity.type() != EntityRomConstants.ENTITY_BOW_WOW || entity.status() != EntityStatus.ACTIVE) continue;
            runtime.slots()[slot] = runtime.motionSet()
                    .bowWowMotion
                    .advanceChain(entity, runtime.motionSet().bowWowMotion.readBowWowState(), moving);
            entries.addAll(runtime.motionSet()
                    .bowWowMotion
                    .chainEntries(slot, runtime.motionSet().bowWowMotion.readBowWowState(), frame));
        }
        this.writeBowWowChainOam(List.copyOf(entries));
    }

    void updatePincerBodyOam() {
        List<PincerBodyOam.Entry> entries = new ArrayList<>();
        for (RoomEntity entity : runtime.slots()) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_PINCER) {
                continue;
            }
            entries.addAll(PincerBodyOam.entries(
                    entity.slot(),
                    runtime.motionSet().pincerMotion.holeX(entity.slot()),
                    runtime.motionSet().pincerMotion.holeY(entity.slot()),
                    entity.x(),
                    entity.y(),
                    runtime.motionSet().pincerMotion.state(entity.slot())));
        }
        this.writePincerBodyOam(List.copyOf(entries));
    }

    void updateWingedOctorokOam() {
        List<WingedOctorokOam.Entry> entries = new ArrayList<>();
        for (RoomEntity entity : runtime.slots()) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_WINGED_OCTOROK) {
                continue;
            }
            entries.addAll(WingedOctorokOam.entries(
                    entity.slot(),
                    runtime.motionSet().wingedOctorokMotion.privateState2(entity.slot()),
                    entity.x(),
                    entity.y(),
                    entity.z()));
        }
        this.writeWingedOctorokOam(List.copyOf(entries));
    }

    public boolean kanaletCrowTakeoffTriggered(RoomEntity crow) {
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

    public EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        return runtime.roomInspection().spriteDefinitionFor(entityType);
    }

    public EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) {
        return runtime.roomInspection().spriteDefinitionForBushCrawlerState(privateState1);
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return runtime.roomInspection().spriteDefinitionForPokeySegment();
    }

    public EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) {
        return runtime.roomInspection().spriteDefinitionForBushCrawlerCrawlState(privateState4);
    }

    EntitySpriteDefinition spriteDefinitionForChest(int itemType) {
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

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
        return runtime.roomInspection().spriteDefinitionForEvasiveState(fallback, privateState1);
    }

    public RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return runtime.roomInspection().refreshColorShellDisplay(entity, status);
    }

    public int options1(int slot) {
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

    List<HookshotChainOam.Entry> readHookshotChainOam() {
        return hookshotChainOam;
    }

    List<HookshotChainOam.Entry> writeHookshotChainOam(List<HookshotChainOam.Entry> value) {
        return hookshotChainOam = value;
    }

    List<PincerBodyOam.Entry> readPincerBodyOam() {
        return pincerBodyOam;
    }

    List<PincerBodyOam.Entry> writePincerBodyOam(List<PincerBodyOam.Entry> value) {
        return pincerBodyOam = value;
    }

    List<WingedOctorokOam.Entry> readWingedOctorokOam() {
        return wingedOctorokOam;
    }

    List<WingedOctorokOam.Entry> writeWingedOctorokOam(List<WingedOctorokOam.Entry> value) {
        return wingedOctorokOam = value;
    }

    boolean readWingedSwordAttackThisFrame(int slot) {
        return wingedSwordAttackThisFrame[slot];
    }

    boolean writeWingedSwordAttackThisFrame(int slot, boolean value) {
        return wingedSwordAttackThisFrame[slot] = value;
    }

    boolean[] storageWingedSwordAttackThisFrame() {
        return wingedSwordAttackThisFrame;
    }

    boolean readWingedStateTwoThisFrame(int slot) {
        return wingedStateTwoThisFrame[slot];
    }

    boolean writeWingedStateTwoThisFrame(int slot, boolean value) {
        return wingedStateTwoThisFrame[slot] = value;
    }

    boolean[] storageWingedStateTwoThisFrame() {
        return wingedStateTwoThisFrame;
    }

    List<BowWowChain.Entry> readBowWowChainOam() {
        return bowWowChainOam;
    }

    List<BowWowChain.Entry> writeBowWowChainOam(List<BowWowChain.Entry> value) {
        return bowWowChainOam = value;
    }
}
