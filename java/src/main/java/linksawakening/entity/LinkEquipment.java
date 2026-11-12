package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import linksawakening.equipment.EquipmentItem;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;

/** ROM-faithful equipment collaborator for Link. */
final class LinkEquipment {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkControls> linkControls;
    private final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    private final LinkComponentRef<LinkItemState> linkItemState;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkEquipment(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkControls = dependencies.linkControls;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkItemState = dependencies.linkItemState;
        linkServices = dependencies.linkServices;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    BooleanSupplier shieldInputAllowed = () -> true;

    /**
     * Mirrors the leading CheckItemsToUse gate used before an equipped item
     * spends inventory. Airborne motion remains eligible; PlaceBomb does not
     * reject it, while pit, carry, and interactive item motion do.
     */
    boolean canUseItems() {
        if (linkAirborne.get().accessRomMotionState() != 0
                || linkEntityEffects.get().accessIsCarryingLiftedObject()
                || linkItemState.get().accessIsRomLinkPushing()) {
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
        if (linkStateMachine.get().gelClinging || !shieldInputAllowed.getAsBoolean()) {
            return false;
        }
        if (linkServices.get().inputState == null
                || linkServices.get().inputConfig == null
                || linkServices.get().playerState == null) {
            return false;
        }
        boolean usingA = linkServices.get().playerState.inventory().itemA() == InventoryItem.SHIELD.romValue()
                && linkServices
                        .get()
                        .inputState
                        .isDown(linkServices.get().inputConfig.aKey());
        boolean usingB = linkServices.get().playerState.inventory().itemB() == InventoryItem.SHIELD.romValue()
                && linkServices
                        .get()
                        .inputState
                        .isDown(linkServices.get().inputConfig.bKey());
        return usingA || usingB;
    }

    /**
     * Mirrors {@code func_157C}: a single currently-held D-pad direction
     * immediately becomes Link's facing before an arrow is spawned; neutral
     * and diagonal masks preserve the existing facing.
     */
    int applyRomItemDirectionFromInput() {
        int newDirection = JOYPAD_TO_DIRECTION[linkControls.get().accessBuildJoypadMask()];
        if (newDirection != -1) {
            linkAnimationState.get().direction = Direction.fromJava(newDirection);
        }
        return linkAnimationState.get().direction.romIndex();
    }

    boolean itemsBlockMotion() {
        return itemsBlockMotion(false);
    }

    boolean itemsBlockMotion(boolean beforeTopDownAnimation) {
        if (linkEntityEffects.get().grabbingObject || linkEntityEffects.get().accessIsLiftTransitionBlockingMotion()) {
            return true;
        }
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return false;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemA());
        if (a != null && itemBlocksMotion(a, beforeTopDownAnimation)) {
            return true;
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemB());
        return b != null && itemBlocksMotion(b, beforeTopDownAnimation);
    }

    static boolean itemBlocksMotion(EquipmentItem item, boolean beforeTopDownAnimation) {
        return beforeTopDownAnimation && item instanceof linksawakening.equipment.Sword sword
                ? sword.blocksTopDownMotion()
                : item.blocksMotion();
    }

    boolean itemsLockFacing() {
        return itemsLockFacing(false);
    }

    boolean itemsLockFacing(boolean beforeTopDownAnimation) {
        if (linkEntityEffects.get().grabbingObject) return true;
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return false;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemA());
        if (a != null && itemLocksFacing(a, beforeTopDownAnimation)) {
            return true;
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemB());
        return b != null && itemLocksFacing(b, beforeTopDownAnimation);
    }

    static boolean itemLocksFacing(EquipmentItem item, boolean beforeTopDownAnimation) {
        return beforeTopDownAnimation && item instanceof linksawakening.equipment.Sword sword
                ? sword.locksTopDownFacing()
                : item.locksFacing();
    }

    EquipmentItem itemInSlotA() {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return null;
        }
        return linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemA());
    }

    EquipmentItem itemInSlotB() {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return null;
        }
        return linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemB());
    }

    void tickRomAttackStepAnimationCountdown() {
        if ((linkItemState.get().romAttackStepAnimationCountdown & ATTACK_STEP_DURATION_MASK) == 0) {
            linkItemState.get().romAttackStepAnimationCountdown = 0;
        } else {
            linkItemState.get().romAttackStepAnimationCountdown =
                    (linkItemState.get().romAttackStepAnimationCountdown - 1) & 0xFF;
        }
    }

    /** Starts the ROM's generic item attack-step animation window. */
    void startRomItemAttackStep() {
        linkItemState.get().romAttackStepAnimationCountdown = ROM_ITEM_ATTACK_STEP_COUNTDOWN;
    }

    /** Starts SprinkleMagicPowder's two-frame-longer generic item window ($0E). */
    void startRomMagicPowderAttackStep() {
        linkItemState.get().romAttackStepAnimationCountdown = 0x0E;
    }

    /** Starts UseMagicRod's high-bit attack-step animation window. */
    void startRomMagicRodAttackStep() {
        linkItemState.get().romAttackStepAnimationCountdown = 0x8E;
    }

    /** Mirrors TarinEntityHandler's direct clear of the item attack-step byte. */
    void clearRomAttackStepAnimationCountdown() {
        linkItemState.get().romAttackStepAnimationCountdown = 0;
    }

    void accessStartRomItemAttackStep() {
        this.startRomItemAttackStep();
    }

    void accessStartRomMagicPowderAttackStep() {
        this.startRomMagicPowderAttackStep();
    }

    void accessStartRomMagicRodAttackStep() {
        this.startRomMagicRodAttackStep();
    }

    void accessClearRomAttackStepAnimationCountdown() {
        this.clearRomAttackStepAnimationCountdown();
    }

    boolean accessCanUseItems() {
        return this.canUseItems();
    }

    void accessSetShieldInputAllowed(BooleanSupplier allowed) {
        this.setShieldInputAllowed(allowed);
    }

    boolean accessIsUsingShield() {
        return this.isUsingShield();
    }

    int accessApplyRomItemDirectionFromInput() {
        return this.applyRomItemDirectionFromInput();
    }
}
