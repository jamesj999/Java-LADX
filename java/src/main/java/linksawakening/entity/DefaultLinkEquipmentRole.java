package linksawakening.entity;

import java.util.function.BooleanSupplier;

/** Delegates equipment operations to state owners used by ROM {@code CheckItemsToUse}. */
final class DefaultLinkEquipmentRole implements LinkEquipmentRole {
    private final LinkEquipment linkEquipment;
    private final LinkItemState linkItemState;

    DefaultLinkEquipmentRole(LinkAssembly assembly) {
        linkEquipment = assembly.linkEquipment.get();
        linkItemState = assembly.linkItemState.get();
    }

    @Override
    public void markRomLinkPushing(int countdown) {
        linkItemState.accessMarkRomLinkPushing(countdown);
    }

    @Override
    public void tickRomLinkPushing() {
        linkItemState.accessTickRomLinkPushing();
    }

    @Override
    public boolean isRomLinkPushing() {
        return linkItemState.accessIsRomLinkPushing();
    }

    @Override
    public int romAttackStepAnimationCountdown() {
        return linkItemState.accessRomAttackStepAnimationCountdown();
    }

    @Override
    public void startRomItemAttackStep() {
        linkEquipment.accessStartRomItemAttackStep();
    }

    @Override
    public void startRomMagicPowderAttackStep() {
        linkEquipment.accessStartRomMagicPowderAttackStep();
    }

    @Override
    public void startRomMagicRodAttackStep() {
        linkEquipment.accessStartRomMagicRodAttackStep();
    }

    @Override
    public void clearRomAttackStepAnimationCountdown() {
        linkEquipment.accessClearRomAttackStepAnimationCountdown();
    }

    @Override
    public boolean canUseItems() {
        return linkEquipment.accessCanUseItems();
    }

    @Override
    public void setShieldInputAllowed(BooleanSupplier allowed) {
        linkEquipment.accessSetShieldInputAllowed(allowed);
    }

    @Override
    public boolean isUsingShield() {
        return linkEquipment.accessIsUsingShield();
    }

    @Override
    public int applyRomItemDirectionFromInput() {
        return linkEquipment.accessApplyRomItemDirectionFromInput();
    }
}
