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
  public int physicsFlags(int slot) { return state.enemyPhysicsFlags[slot]; }
  public void setPhysicsFlags(int slot, int value) { state.enemyPhysicsFlags[slot] = value; }
  public void andPhysicsFlags(int slot, int mask) { state.enemyPhysicsFlags[slot] &= mask; }
  public void orPhysicsFlags(int slot, int mask) { state.enemyPhysicsFlags[slot] |= mask; }
  public int flashCountdown(int slot) { return state.enemyFlashCountdown[slot]; }
  public void setFlashCountdown(int slot, int value) { state.enemyFlashCountdown[slot] = value; }
}
