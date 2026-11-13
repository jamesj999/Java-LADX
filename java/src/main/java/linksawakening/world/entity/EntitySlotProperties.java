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
    final EntitySlotPropertiesServices runtime;

    EntitySlotProperties(EntitySlotPresentation state, EntitySlotPropertiesServices runtime) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.runtime = runtime;
        this.puzzleCountdown = runtime.physicsTables()::readEnemyTransitionCountdown;
    }

    public EntitySpriteViews spriteViews() {
        return state.spriteViews();
    }

    void resolveThreeOfAKindPuzzle() {
        EntityCompositePresentation.resolveThreeOfAKindPuzzle(this);
    }

    int initialHealth(int type) {
        return runtime.roomInspection().initialHealth(type);
    }

    int contactDamage(int slot) {
        return runtime.roomInspection().contactDamage(slot);
    }

    public RoomEntity advanceMusicalNoteEntity(RoomEntity entity) {
        int slot = entity.slot();
        int inertia = (runtime.projectiles().state.readMusicalNoteInertia(slot) - 1) & 0xFF;
        runtime.projectiles().state.writeMusicalNoteInertia(slot, inertia);
        if (inertia == 0) {
            runtime.clearEntity(slot);
            return null;
        }

        int speedXAdjustment = (inertia & 0x10) != 0 ? -1 : 1;
        if ((inertia & 0x01) == 0) {
            runtime.projectiles()
                    .state
                    .writeMusicalNoteSpeedX(
                            slot, (runtime.projectiles().state.readMusicalNoteSpeedX(slot) + speedXAdjustment) & 0xFF);
        }
        int x = (entity.x() + signedByte(runtime.projectiles().state.readMusicalNoteSpeedX(slot))) & 0xFF;
        int y = (entity.y() + signedByte(runtime.projectiles().state.readMusicalNoteSpeedY(slot))) & 0xFF;
        return withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    void updateHookshotChainOam(int linkEntityX, int linkEntityY, int frameCounter) {
        int slot = runtime.inspection().hookshotSlot();
        if (slot < 0 || !runtime.slots()[slot].loaded()) {
            state.writeHookshotChainOam(List.of());
            return;
        }
        RoomEntity chain = runtime.slots()[slot];
        state.writeHookshotChainOam(
                HookshotChainOam.entries(chain.x(), chain.y(), linkEntityX, linkEntityY, frameCounter));
    }

    void updateBowWowChain(int frame) {
        EntityCompositePresentation.updateBowWowChain(this, frame);
    }

    void updatePincerBodyOam() {
        List<PincerBodyOam.Entry> entries = null;
        for (RoomEntity entity : runtime.slots()) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_PINCER) {
                continue;
            }
            if (entries == null) entries = new ArrayList<>();
            entries.addAll(PincerBodyOam.entries(
                    entity.slot(),
                    runtime.motions().get(PincerMotion.class, PincerMotion::new).holeX(entity.slot()),
                    runtime.motions().get(PincerMotion.class, PincerMotion::new).holeY(entity.slot()),
                    entity.x(),
                    entity.y(),
                    runtime.motions().get(PincerMotion.class, PincerMotion::new).state(entity.slot())));
        }
        state.writePincerBodyOam(entries == null ? List.of() : List.copyOf(entries));
    }

    void updateWingedOctorokOam() {
        List<WingedOctorokOam.Entry> entries = null;
        for (RoomEntity entity : runtime.slots()) {
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_WINGED_OCTOROK) {
                continue;
            }
            if (entries == null) entries = new ArrayList<>();
            entries.addAll(WingedOctorokOam.entries(
                    entity.slot(),
                    runtime.motions()
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
        return EntitySlotPropertyRules.spriteDefinitionForChest(this, itemType);
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
        return runtime.roomInspection().spriteDefinitionForEvasiveState(fallback, privateState1);
    }

    public RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return runtime.roomInspection().refreshColorShellDisplay(entity, status);
    }

    public int options1(int slot) {
        return EntitySlotPropertyRules.options1(this, slot);
    }

    final java.util.function.IntUnaryOperator puzzleCountdown;
    private java.util.List<RoomEntity> puzzleEntities;

    java.util.List<RoomEntity> puzzleEntities() {
        if (puzzleEntities == null) puzzleEntities = java.util.Arrays.asList(runtime.slots());
        return puzzleEntities;
    }
}
