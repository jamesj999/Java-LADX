package linksawakening.entity;

import static linksawakening.entity.Link.*;

import java.util.function.BooleanSupplier;

/** Equipment operations from LinkMotionDefault and its related ROM routines. */
public interface LinkEquipmentRole {
    void markRomLinkPushing(int countdown);

    void tickRomLinkPushing();

    boolean isRomLinkPushing();

    int romAttackStepAnimationCountdown();

    void startRomItemAttackStep();

    void startRomMagicPowderAttackStep();

    void startRomMagicRodAttackStep();

    void clearRomAttackStepAnimationCountdown();

    boolean canUseItems();

    void setShieldInputAllowed(BooleanSupplier allowed);

    boolean isUsingShield();

    int applyRomItemDirectionFromInput();
}
