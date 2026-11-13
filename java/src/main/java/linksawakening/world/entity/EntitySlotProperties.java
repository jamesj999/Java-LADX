package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.signedByte;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntityTypeRules.isBombiteType;
import static linksawakening.world.entity.EntityTypeRules.isGhiniType;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaFlowerMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WingedOctorokMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;
import linksawakening.world.room.ChestContentsTable;

/** ROM per-slot options, sprite definitions and handler metadata. */
public final class EntitySlotProperties {
    final EntitySlotPresentation state;
    final boolean indoorRoom;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPresentationState presentationState;
    final EntityProjectileState projectiles;
    final EntityRoomInspection inspection;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final RoomEntity[] slots;
    final EntitySlotLifecycle lifecycle;

    EntitySlotProperties(
            EntitySlotPresentation state,
            boolean indoorRoom,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ linksawakening.world
                            .entity.EntitySpriteViews
                    spriteHandlers,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRoomInspection inspection,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            RoomEntity[] slots,
            EntitySlotLifecycle lifecycle) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = java.util.Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.puzzleCountdown = physicsTables::readEnemyTransitionCountdown;
        this.puzzleEntities = java.util.Arrays.asList(slots);
    }

    public EntitySpriteViews spriteViews() {
        return state.spriteViews();
    }

    void resolveThreeOfAKindPuzzle() {
        var runtime = this;
        ThreeOfAKindMotion.PuzzleResult result = runtime.motions
                .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                .resolvePuzzle(this.puzzleEntities(), this.puzzleCountdown);
        if (result == ThreeOfAKindMotion.PuzzleResult.NONE) {
            return;
        }
        int coordinatorSlot = 0;
        for (RoomEntity entity : runtime.slots) {
            if (entity.status() == EntityStatus.ACTIVE && entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND) {
                coordinatorSlot = entity.slot();
                break;
            }
        }
        int jingle = result == ThreeOfAKindMotion.PuzzleResult.MATCH
                ? runtime.motions
                        .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                        .matchedJingle(coordinatorSlot)
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
            runtime.bossRewards.writeDroppedItemBySlot(
                    slot,
                    runtime.motions
                            .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                            .matchedDrop(slot));
            runtime.combatTables.writeDyingCountdown(slot, 0x1F);
            runtime.combatTables.writeEnemyHealth(slot, 0);
            runtime.physicsTables.writeEnemyPhysicsFlags(slot, 0x04);
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
        return this.roomInspection.initialHealth(type);
    }

    int contactDamage(int slot) {
        return this.roomInspection.contactDamage(slot);
    }

    public RoomEntity advanceMusicalNoteEntity(RoomEntity entity) {
        int slot = entity.slot();
        int inertia = (this.projectiles.readMusicalNoteInertia(slot) - 1) & 0xFF;
        this.projectiles.writeMusicalNoteInertia(slot, inertia);
        if (inertia == 0) {
            this.lifecycle.clearEntity(slot);
            return null;
        }

        int speedXAdjustment = (inertia & 0x10) != 0 ? -1 : 1;
        if ((inertia & 0x01) == 0) {
            this.projectiles.writeMusicalNoteSpeedX(
                    slot, (this.projectiles.readMusicalNoteSpeedX(slot) + speedXAdjustment) & 0xFF);
        }
        int x = (entity.x() + signedByte(this.projectiles.readMusicalNoteSpeedX(slot))) & 0xFF;
        int y = (entity.y() + signedByte(this.projectiles.readMusicalNoteSpeedY(slot))) & 0xFF;
        return withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    void updateHookshotChainOam(int linkEntityX, int linkEntityY, int frameCounter) {
        int slot = this.inspection.hookshotSlot();
        if (slot < 0 || !this.slots[slot].loaded()) {
            state.writeHookshotChainOam(List.of());
            return;
        }
        RoomEntity chain = this.slots[slot];
        state.writeHookshotChainOam(
                HookshotChainOam.entries(chain.x(), chain.y(), linkEntityX, linkEntityY, frameCounter));
    }

    void updateBowWowChain(int frame) {
        var runtime = this;
        List<BowWowChain.Entry> entries = null;
        // The handler's dialogue/inventory/transition branches still run the
        // chain. Its sequence check only returns early on the unpaused path.
        if (runtime.roomVariables.readTransitionSequenceCounter() != 4
                && !runtime.roomVariables.readRoomTransitionActive()
                && !runtime.linkState.dialogActive
                && !runtime.linkState.inventoryAppearing
                && !runtime.inspection.witchGotItemPresentationActive()) {
            this.state.writeBowWowChainOam(List.of());
            return;
        }
        boolean moving = (runtime.linkState.currentLinkSpeedX
                        | runtime.linkState.currentLinkSpeedY
                        | runtime.linkState.currentLinkVelocityZ)
                != 0;
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            RoomEntity entity = runtime.slots[slot];
            if (entity.type() != EntityRomConstants.ENTITY_BOW_WOW || entity.status() != EntityStatus.ACTIVE) continue;
            runtime.slots[slot] = runtime.motions
                    .get(BowWowMotion.class, BowWowMotion::new)
                    .advanceChain(
                            entity,
                            runtime.motions
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState(),
                            moving);
            if (entries == null) entries = new ArrayList<>();
            entries.addAll(runtime.motions
                    .get(BowWowMotion.class, BowWowMotion::new)
                    .chainEntries(
                            slot,
                            runtime.motions
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState(),
                            frame));
        }
        this.state.writeBowWowChainOam(entries == null ? List.of() : List.copyOf(entries));
    }

    void updatePincerBodyOam() {
        state.updatePincerBodyOam(slots, motions);
    }

    void updateWingedOctorokOam() {
        List<WingedOctorokOam.Entry> entries = null;
        for (RoomEntity entity : this.slots) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_WINGED_OCTOROK) {
                continue;
            }
            if (entries == null) entries = new ArrayList<>();
            entries.addAll(WingedOctorokOam.entries(
                    entity.slot(),
                    this.motions
                            .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                            .privateState2(entity.slot()),
                    entity.x(),
                    entity.y(),
                    entity.z()));
        }
        state.writeWingedOctorokOam(entries == null ? List.of() : List.copyOf(entries));
    }

    public boolean kanaletCrowTakeoffTriggered(RoomEntity crow) {
        for (RoomEntity candidate : this.slots) {
            if (!candidate.loaded()) {
                continue;
            }
            if (candidate.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                    && this.liftThrow.readLiftableRockSmashActive(candidate.slot())
                    && this.liftThrow.readLiftableRockSmashSourceVariant(candidate.slot())
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
            int countdown = this.physicsTables.readEnemyTransitionCountdown(candidate.slot()) & 0xFF;
            if (this.combatTables.readEnemyFlashCountdown(candidate.slot()) == 0 || countdown >= 0x22) {
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
        return this.roomInspection.spriteDefinitionFor(entityType);
    }

    public EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) {
        return this.roomInspection.spriteDefinitionForBushCrawlerState(privateState1);
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return this.roomInspection.spriteDefinitionForPokeySegment();
    }

    public EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) {
        return this.roomInspection.spriteDefinitionForBushCrawlerCrawlState(privateState4);
    }

    EntitySpriteDefinition spriteDefinitionForChest(int itemType) {
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_CHEST_WITH_ITEM);
        }
        if (!this.spriteHandlers.supportsPresentation()) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_CHEST_WITH_ITEM);
        }
        int roomId = this.presentationState.readSpriteSelection().roomId();
        if (this.roomVariables.readEntityMapId() >= 0 && roomId >= 0) {
            return this.spriteHandlers.forChestState(this.roomVariables.readEntityMapId(), roomId, itemType);
        }
        return this.spriteHandlers.forEntityType(
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                this.presentationState.readSpriteSelection().roomTable(),
                this.roomVariables.readEntityMapId());
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
        return this.roomInspection.spriteDefinitionForEvasiveState(fallback, privateState1);
    }

    public RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return this.roomInspection.refreshColorShellDisplay(entity, status);
    }

    public int options1(int slot) {
        if (slot < 0 || slot >= this.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (this.physicsTables.readEntityOptions1Override(slot) >= 0) {
            return this.physicsTables.readEntityOptions1Override(slot);
        }
        if (isGhiniType(this.slots[slot].type())) {
            return EntityRomConstants.GHINI_OPTIONS1;
        }
        return switch (this.slots[slot].type()) {
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
            default -> entitySlotPropertyRulesFallbackOptions(this.slots[slot].type());
        };
    }

    final java.util.function.IntUnaryOperator puzzleCountdown;
    private final java.util.List<RoomEntity> puzzleEntities;

    java.util.List<RoomEntity> puzzleEntities() {
        return puzzleEntities;
    }

    private static int entitySlotPropertyRulesFallbackOptions(int type) {
        if (isBombiteType(type)) {
            return EntityRomConstants.BOMBITE_OPTIONS1;
        }
        return type == EntityRomConstants.ENTITY_STALFOS_EVASIVE || type == EntityRomConstants.ENTITY_LIKE_LIKE
                ? EntityRomConstants.ENTITY_OPT1_SPLASH_IN_WATER
                : 0;
    }
}
