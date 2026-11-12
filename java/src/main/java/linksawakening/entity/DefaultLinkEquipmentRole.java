package linksawakening.entity;

import static linksawakening.entity.Link.*;

import java.util.function.BooleanSupplier;

/** Delegates equipment operations to their state-owning ROM implementations. */
final class DefaultLinkEquipmentRole implements LinkEquipmentRole {
    private final LinkEquipment linkEquipment;
    private final LinkItemState linkItemState;

    DefaultLinkEquipmentRole(LinkAssembly assembly) {
        linkEquipment = assembly.linkEquipment.get();
        linkItemState = assembly.linkItemState.get();
    }

    public void markRomLinkPushing(int countdown) {
        linkItemState.accessMarkRomLinkPushing(countdown);
    }

    public void tickRomLinkPushing() {
        linkItemState.accessTickRomLinkPushing();
    }

    public boolean isRomLinkPushing() {
        return linkItemState.accessIsRomLinkPushing();
    }

    public int romAttackStepAnimationCountdown() {
        return linkItemState.accessRomAttackStepAnimationCountdown();
    }

    public void startRomItemAttackStep() {
        linkEquipment.accessStartRomItemAttackStep();
    }

    public void startRomMagicPowderAttackStep() {
        linkEquipment.accessStartRomMagicPowderAttackStep();
    }

    public void startRomMagicRodAttackStep() {
        linkEquipment.accessStartRomMagicRodAttackStep();
    }

    public void clearRomAttackStepAnimationCountdown() {
        linkEquipment.accessClearRomAttackStepAnimationCountdown();
    }

    public boolean canUseItems() {
        return linkEquipment.accessCanUseItems();
    }

    public void setShieldInputAllowed(BooleanSupplier allowed) {
        linkEquipment.accessSetShieldInputAllowed(allowed);
    }

    public boolean isUsingShield() {
        return linkEquipment.accessIsUsingShield();
    }

    public int applyRomItemDirectionFromInput() {
        return linkEquipment.accessApplyRomItemDirectionFromInput();
    }
}
