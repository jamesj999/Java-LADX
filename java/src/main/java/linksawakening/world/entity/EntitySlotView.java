package linksawakening.world.entity;

/** Reused view of ROM slot state, preserving array indexing and values. */
public final class EntitySlotView {
  private final EntityRuntimeState state;
  EntitySlotView(EntityRuntimeState state) { this.state = state; }
  public int transitionCountdown(int slot) { return state.enemyTransitionCountdown[slot]; }
  public void setTransitionCountdown(int slot, int value) {
    state.enemyTransitionCountdown[slot] = value;
  }
}
