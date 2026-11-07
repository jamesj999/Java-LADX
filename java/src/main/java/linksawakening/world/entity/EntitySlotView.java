package linksawakening.world.entity;

/** Reused view of ROM slot state, preserving array indexing and values. */
public final class EntitySlotView {
  private final EntityRuntimeState state;
  EntitySlotView(EntityRuntimeState state) { this.state = state; }
  public int transitionCountdown(int slot) { return state.enemyTransitionCountdown[slot]; }
  public void setTransitionCountdown(int slot, int value) {
    state.enemyTransitionCountdown[slot] = value;
  }
  public int ignoreHits(int slot) { return state.enemyIgnoreHitsCountdown[slot]; }
  public void setIgnoreHits(int slot, int value) { state.enemyIgnoreHitsCountdown[slot] = value; }
  public void decrementIgnoreHits(int slot) { state.enemyIgnoreHitsCountdown[slot]--; }
  public void shiftIgnoreHitsRight(int slot, int distance) {
    state.enemyIgnoreHitsCountdown[slot] >>>= distance;
  }
}
