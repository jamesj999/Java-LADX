package linksawakening.world.entity;

/** Read-only view of the live Link-relative inputs. */
public final class EntityLinkView {
  private final EntityLinkState state;
  EntityLinkView(EntityLinkState state) { this.state = state; }
  public boolean actionButtonAHeld() { return state.actionButtonAHeld; }
  public boolean actionButtonBHeld() { return state.actionButtonBHeld; }
  public boolean actionButtonsHeld() { return state.actionButtonsHeld; }
  public int currentLinkFinalPositionX() { return state.currentLinkFinalPositionX; }
  public boolean currentLinkFinalPositionXConfigured() { return state.currentLinkFinalPositionXConfigured; }
  public int currentLinkSpeedX() { return state.currentLinkSpeedX; }
  public int currentLinkSpeedY() { return state.currentLinkSpeedY; }
  public boolean dialogActive() { return state.dialogActive; }
  public int dialogCooldown() { return state.dialogCooldown; }
  public boolean inventoryAppearing() { return state.inventoryAppearing; }
  public boolean joypadHeld() { return state.joypadHeld; }
  public int linkAttackStepAnimationCountdown() { return state.linkAttackStepAnimationCountdown; }
  public int linkItemA() { return state.linkItemA; }
  public int linkItemB() { return state.linkItemB; }
  public int linkPlayingOcarinaCountdown() { return state.linkPlayingOcarinaCountdown; }
  public int linkPressedButtonsMask() { return state.linkPressedButtonsMask; }
  public boolean runningWithPegasusBoots() { return state.runningWithPegasusBoots; }
  public int windowY() { return state.windowY; }
}
