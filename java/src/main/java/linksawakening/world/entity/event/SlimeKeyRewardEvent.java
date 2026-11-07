package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** Reward, room-completion, and dialog state emitted by HidingSlimeKeyEntityHandler. */
public record SlimeKeyRewardEvent(int slot, int goldenLeavesCount, int dialogLowId) implements EntityRequest {
    public SlimeKeyRewardEvent {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Slime Key entity slot out of range: " + slot);
        }
        if (goldenLeavesCount < 0 || goldenLeavesCount > 0x63) {
            throw new IllegalArgumentException("Golden leaves count must be between 0 and 99: "
                + goldenLeavesCount);
        }
        if ((dialogLowId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Slime Key dialog must be an unsigned byte: "
                + dialogLowId);
        }
    }
}
