package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;

/** ROM per-slot options, sprite definitions and handler metadata. */
public final class EntitySlotProperties {
    final EntitySlotPresentation state;
    final boolean indoorRoom;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
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
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
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
        this.spriteHandlers = spriteHandlers;
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
    }

    public EntitySpriteViews spriteViews() {
        return state.spriteViews();
    }

    void resolveThreeOfAKindPuzzle() {
        EntityCompositePresentation.resolveThreeOfAKindPuzzle(this);
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
        EntityCompositePresentation.updateBowWowChain(this, frame);
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
        return EntitySlotPropertyRules.kanaletCrowTakeoffTriggered(this, crow);
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
        return EntitySlotPropertyRules.spriteDefinitionForChest(this, itemType);
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
        return this.roomInspection.spriteDefinitionForEvasiveState(fallback, privateState1);
    }

    public RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return this.roomInspection.refreshColorShellDisplay(entity, status);
    }

    public int options1(int slot) {
        return EntitySlotPropertyRules.options1(this, slot);
    }

    final java.util.function.IntUnaryOperator puzzleCountdown;
    private java.util.List<RoomEntity> puzzleEntities;

    java.util.List<RoomEntity> puzzleEntities() {
        if (puzzleEntities == null) puzzleEntities = java.util.Arrays.asList(this.slots);
        return puzzleEntities;
    }
}
