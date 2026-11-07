package linksawakening.entity;

import linksawakening.equipment.EquipmentItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.state.LinkPlayerState;
import static linksawakening.entity.LinkRomConstants.*;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import static linksawakening.entity.Link.*;

/** ROM-faithful equipment collaborator for Link. */
final class LinkEquipment {
    private final Link link;
    BooleanSupplier shieldInputAllowed = () -> true;

    LinkEquipment(Link link) { this.link = link; }

    /**
     * Mirrors the leading CheckItemsToUse gate used before an equipped item
     * spends inventory. Airborne motion remains eligible; PlaceBomb does not
     * reject it, while pit, carry, and interactive item motion do.
     */
    boolean canUseItems() {
        if (link.romMotionState() != 0 || link.isCarryingLiftedObject() || link.isRomLinkPushing()) {
            return false;
        }
        return !itemsBlockMotion();
    }

    /** Live input gate, also consulted when rendering a paused dialogue frame. */
    void setShieldInputAllowed(BooleanSupplier allowed) {
        shieldInputAllowed = Objects.requireNonNull(allowed);
    }

    /** Mirrors wIsUsingShield: equipped, held, and permitted by gameplay input. */
    boolean isUsingShield() {
        if (link.linkStateMachine.gelClinging || !shieldInputAllowed.getAsBoolean()) {
            return false;
        }
        if (link.inputState == null || link.inputConfig == null || link.playerState == null) {
            return false;
        }
        boolean usingA = link.playerState.itemA() == PlayerState.INVENTORY_SHIELD
            && link.inputState.isDown(link.inputConfig.aKey());
        boolean usingB = link.playerState.itemB() == PlayerState.INVENTORY_SHIELD
            && link.inputState.isDown(link.inputConfig.bKey());
        return usingA || usingB;
    }

    /**
     * Mirrors {@code func_157C}: a single currently-held D-pad direction
     * immediately becomes Link's facing before an arrow is spawned; neutral
     * and diagonal masks preserve the existing facing.
     */
    int applyRomItemDirectionFromInput() {
        int newDirection = JOYPAD_TO_DIRECTION[link.buildJoypadMask()];
        if (newDirection != -1) {
            link.direction = newDirection;
        }
        return link.romDirectionForJavaDirection(link.direction);
    }

    boolean itemsBlockMotion() {
        return itemsBlockMotion(false);
    }

    boolean itemsBlockMotion(boolean beforeTopDownAnimation) {
        if (link.isLiftTransitionBlockingMotion()) {
            return true;
        }
        if (link.itemRegistry == null || link.playerState == null) {
            return false;
        }
        EquipmentItem a = link.itemRegistry.lookup(link.playerState.itemA());
        if (a != null && itemBlocksMotion(a, beforeTopDownAnimation)) {
            return true;
        }
        EquipmentItem b = link.itemRegistry.lookup(link.playerState.itemB());
        return b != null && itemBlocksMotion(b, beforeTopDownAnimation);
    }

    static boolean itemBlocksMotion(EquipmentItem item, boolean beforeTopDownAnimation) {
        return beforeTopDownAnimation && item instanceof linksawakening.equipment.Sword sword
            ? sword.blocksTopDownMotion() : item.blocksMotion();
    }

    boolean itemsLockFacing() {
        return itemsLockFacing(false);
    }

    boolean itemsLockFacing(boolean beforeTopDownAnimation) {
        if (link.itemRegistry == null || link.playerState == null) {
            return false;
        }
        EquipmentItem a = link.itemRegistry.lookup(link.playerState.itemA());
        if (a != null && itemLocksFacing(a, beforeTopDownAnimation)) {
            return true;
        }
        EquipmentItem b = link.itemRegistry.lookup(link.playerState.itemB());
        return b != null && itemLocksFacing(b, beforeTopDownAnimation);
    }

    static boolean itemLocksFacing(EquipmentItem item, boolean beforeTopDownAnimation) {
        return beforeTopDownAnimation && item instanceof linksawakening.equipment.Sword sword
            ? sword.locksTopDownFacing() : item.locksFacing();
    }

    EquipmentItem itemInSlotA() {
        if (link.itemRegistry == null || link.playerState == null) {
            return null;
        }
        return link.itemRegistry.lookup(link.playerState.itemA());
    }

    EquipmentItem itemInSlotB() {
        if (link.itemRegistry == null || link.playerState == null) {
            return null;
        }
        return link.itemRegistry.lookup(link.playerState.itemB());
    }

    void tickRomAttackStepAnimationCountdown() {
        if ((link.romAttackStepAnimationCountdown & ATTACK_STEP_DURATION_MASK) == 0) {
            link.romAttackStepAnimationCountdown = 0;
        } else {
            link.romAttackStepAnimationCountdown = (link.romAttackStepAnimationCountdown - 1) & 0xFF;
        }
    }

    /** Starts the ROM's generic item attack-step animation window. */
    void startRomItemAttackStep() {
        link.romAttackStepAnimationCountdown = ROM_ITEM_ATTACK_STEP_COUNTDOWN;
    }

    /** Starts SprinkleMagicPowder's two-frame-longer generic item window ($0E). */
    void startRomMagicPowderAttackStep() {
        link.romAttackStepAnimationCountdown = 0x0E;
    }

    /** Starts UseMagicRod's high-bit attack-step animation window. */
    void startRomMagicRodAttackStep() {
        link.romAttackStepAnimationCountdown = 0x8E;
    }

    /** Mirrors TarinEntityHandler's direct clear of the item attack-step byte. */
    void clearRomAttackStepAnimationCountdown() {
        link.romAttackStepAnimationCountdown = 0;
    }
}
