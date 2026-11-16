package linksawakening.world.entity;

/** Read-only view of the live Link-relative inputs. */
public final class EntityLinkView {
    private final EntityLinkState state;

    EntityLinkView(EntityLinkState state) {
        this.state = java.util.Objects.requireNonNull(state, "state");
    }

    public boolean actionButtonAHeld() {
        return state.readActionButtonAHeld();
    }

    public boolean actionButtonBHeld() {
        return state.readActionButtonBHeld();
    }

    public boolean actionButtonsHeld() {
        return state.readActionButtonsHeld();
    }

    public int currentLinkFinalPositionX() {
        return state.readCurrentLinkFinalPositionX();
    }

    public boolean currentLinkFinalPositionXConfigured() {
        return state.readCurrentLinkFinalPositionXConfigured();
    }

    public int currentLinkSpeedX() {
        return state.readCurrentLinkSpeedX();
    }

    public int currentLinkSpeedY() {
        return state.readCurrentLinkSpeedY();
    }

    public boolean dialogActive() {
        return state.readDialogActive();
    }

    public int dialogCooldown() {
        return state.readDialogCooldown();
    }

    public boolean inventoryAppearing() {
        return state.readInventoryAppearing();
    }

    public boolean joypadHeld() {
        return state.readJoypadHeld();
    }

    public int linkAttackStepAnimationCountdown() {
        return state.readLinkAttackStepAnimationCountdown();
    }

    public int linkItemA() {
        return state.readLinkItemA();
    }

    public int linkItemB() {
        return state.readLinkItemB();
    }

    public int linkPlayingOcarinaCountdown() {
        return state.readLinkPlayingOcarinaCountdown();
    }

    public int linkPressedButtonsMask() {
        return state.readLinkPressedButtonsMask();
    }

    public boolean runningWithPegasusBoots() {
        return state.readRunningWithPegasusBoots();
    }

    public int windowY() {
        return state.readWindowY();
    }
}
