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
    private List<HookshotChainOam.Entry> hookshotChainOam = List.of();
    private List<PincerBodyOam.Entry> pincerBodyOam = List.of();
    private List<WingedOctorokOam.Entry> wingedOctorokOam = List.of();
    private final boolean[] wingedSwordAttackThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] wingedStateTwoThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private List<BowWowChain.Entry> bowWowChainOam = List.of();
    final EntitySlotPropertiesServices runtime;
    private EntitySpriteViews spriteViews;

    EntitySlotProperties(EntitySlotPropertiesServices runtime) {
        this.runtime = runtime;
        this.puzzleCountdown = runtime.physicsTables()::readEnemyTransitionCountdown;
    }

    public EntitySpriteViews spriteViews() {
        if (spriteViews == null) spriteViews = new EntitySpriteViews(runtime.spriteHandlers());
        return spriteViews;
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
        this.writePincerBodyOam(entries == null ? List.of() : List.copyOf(entries));
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
        this.writeWingedOctorokOam(entries == null ? List.of() : List.copyOf(entries));
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

    final java.util.function.IntUnaryOperator puzzleCountdown;
    private java.util.List<RoomEntity> puzzleEntities;

    java.util.List<RoomEntity> puzzleEntities() {
        if (puzzleEntities == null) puzzleEntities = java.util.Arrays.asList(runtime.slots());
        return puzzleEntities;
    }
}
