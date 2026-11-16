package linksawakening.world.entity;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.motion.PincerMotion;

/** Shared slotProperties slot state, independent of behavior dispatch and construction order. */
final class EntitySlotPresentation {
    private final List<PincerBodyOam.Entry> pincerEntries = new ArrayList<>();
    private final EntitySpriteViews spriteViews;

    EntitySlotPresentation(EntitySpriteViews spriteViews) {
        this.spriteViews = java.util.Objects.requireNonNull(spriteViews, "spriteViews");
    }

    EntitySpriteViews spriteViews() {
        return spriteViews;
    }

    private List<HookshotChainOam.Entry> hookshotChainOam = List.of();
    private List<PincerBodyOam.Entry> pincerBodyOam = List.of();
    private List<WingedOctorokOam.Entry> wingedOctorokOam = List.of();
    private final boolean[] wingedSwordAttackThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] wingedStateTwoThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private List<BowWowChain.Entry> bowWowChainOam = List.of();

    List<HookshotChainOam.Entry> readHookshotChainOam() {
        return hookshotChainOam;
    }

    List<HookshotChainOam.Entry> writeHookshotChainOam(List<HookshotChainOam.Entry> value) {
        hookshotChainOam = value;
        return hookshotChainOam;
    }

    List<PincerBodyOam.Entry> readPincerBodyOam() {
        return pincerBodyOam;
    }

    List<PincerBodyOam.Entry> writePincerBodyOam(List<PincerBodyOam.Entry> value) {
        pincerBodyOam = value;
        return pincerBodyOam;
    }

    List<WingedOctorokOam.Entry> readWingedOctorokOam() {
        return wingedOctorokOam;
    }

    List<WingedOctorokOam.Entry> writeWingedOctorokOam(List<WingedOctorokOam.Entry> value) {
        wingedOctorokOam = value;
        return wingedOctorokOam;
    }

    boolean readWingedSwordAttackThisFrame(int slot) {
        return wingedSwordAttackThisFrame[slot];
    }

    boolean writeWingedSwordAttackThisFrame(int slot, boolean value) {
        wingedSwordAttackThisFrame[slot] = value;
        return wingedSwordAttackThisFrame[slot];
    }

    boolean[] storageWingedSwordAttackThisFrame() {
        return wingedSwordAttackThisFrame;
    }

    boolean readWingedStateTwoThisFrame(int slot) {
        return wingedStateTwoThisFrame[slot];
    }

    boolean writeWingedStateTwoThisFrame(int slot, boolean value) {
        wingedStateTwoThisFrame[slot] = value;
        return wingedStateTwoThisFrame[slot];
    }

    boolean[] storageWingedStateTwoThisFrame() {
        return wingedStateTwoThisFrame;
    }

    List<BowWowChain.Entry> readBowWowChainOam() {
        return bowWowChainOam;
    }

    List<BowWowChain.Entry> writeBowWowChainOam(List<BowWowChain.Entry> value) {
        bowWowChainOam = value;
        return bowWowChainOam;
    }

    void updatePincerBodyOam(EntitySlots slots, EntityMotionRegistry motions) {
        List<PincerBodyOam.Entry> entries = pincerEntries;
        entries.clear();
        for (int entitySlotIndex = 0; entitySlotIndex < slots.size(); entitySlotIndex++) {
            RoomEntity entity = slots.get(entitySlotIndex);

            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_PINCER) {
                continue;
            }
            entries.addAll(PincerBodyOam.entries(
                    entity.slot(),
                    motions.get(PincerMotion.class, PincerMotion::new).holeX(entity.slot()),
                    motions.get(PincerMotion.class, PincerMotion::new).holeY(entity.slot()),
                    entity.x(),
                    entity.y(),
                    motions.get(PincerMotion.class, PincerMotion::new).state(entity.slot())));
        }
        writePincerBodyOam(List.copyOf(entries));
    }
}
