package linksawakening.world.entity;

/** Reused view of ROM slot state, preserving array indexing and values. */
public final class EntitySlotView {
    private final EntityRuntimeState state;

    EntitySlotView(EntityRuntimeState state) {
        this.state = state;
    }

    public int transitionCountdown(int slot) {
        return state.enemyTransitionCountdown[slot];
    }

    public void setTransitionCountdown(int slot, int value) {
        state.enemyTransitionCountdown[slot] = value;
    }

    public int ignoreHits(int slot) {
        return state.enemyIgnoreHitsCountdown[slot];
    }

    public void setIgnoreHits(int slot, int value) {
        state.enemyIgnoreHitsCountdown[slot] = value;
    }

    public void decrementIgnoreHits(int slot) {
        state.enemyIgnoreHitsCountdown[slot]--;
    }

    public void shiftIgnoreHitsRight(int slot, int distance) {
        state.enemyIgnoreHitsCountdown[slot] >>>= distance;
    }

    public int physicsFlags(int slot) {
        return state.enemyPhysicsFlags[slot];
    }

    public void setPhysicsFlags(int slot, int value) {
        state.enemyPhysicsFlags[slot] = value;
    }

    public void andPhysicsFlags(int slot, int mask) {
        state.enemyPhysicsFlags[slot] &= mask;
    }

    public void orPhysicsFlags(int slot, int mask) {
        state.enemyPhysicsFlags[slot] |= mask;
    }

    public int flashCountdown(int slot) {
        return state.enemyFlashCountdown[slot];
    }

    public void setFlashCountdown(int slot, int value) {
        state.enemyFlashCountdown[slot] = value;
    }

    public int healthGroup(int slot) {
        return state.enemyHealthGroup[slot];
    }
    /**
     * Writes wEntitiesHealthGroup ($C4D0) for a handler's slot.
     * Outstanding handlers must wire these ROM writes when implemented:
     * Facade spawned entities: $04:52C6 = $02, $04:5327 = $1B;
     * flying tiles spawner: $07:7529 = $00;
     * Angler Fish spawned entity: $05:566F = $00;
     * Evil Eagle spawned entity: $05:5F2D = $02;
     * Master Stalfos transition: $36:6076 = $00;
     * Master Stalfos func_036_4F68 conditional __PATCH_0__ block = $09
     * (no address in the unpatched ROM).
     */
    public void setHealthGroup(int slot, int value) {
        EntitySlotValidation.validateByte(value, "Health group");
        state.enemyHealthGroup[slot] = value;
    }

    public int health(int slot) {
        return state.enemyHealth[slot];
    }

    public void setHealth(int slot, int value) {
        state.enemyHealth[slot] = value;
    }

    public int hitboxFlags(int slot) {
        return state.enemyHitboxFlags[slot];
    }

    public void setHitboxFlags(int slot, int value) {
        state.enemyHitboxFlags[slot] = value;
    }

    public int options1Override(int slot) {
        return state.entityOptions1Override[slot];
    }

    public void setOptions1Override(int slot, int value) {
        state.entityOptions1Override[slot] = value;
    }

    public int pokeyInertia(int slot) {
        return state.entityInertia[slot];
    }

    public void setPokeyInertia(int slot, int value) {
        state.entityInertia[slot] = value;
    }

    public int slowTransitionCountdown(int slot) {
        return state.slowTransitionCountdown[slot];
    }

    public void setSlowTransitionCountdown(int slot, int value) {
        state.slowTransitionCountdown[slot] = value;
    }

    public boolean slowTimerInitialized(int slot) {
        return state.slowTimerInitialized[slot];
    }

    public void setSlowTimerInitialized(int slot, boolean value) {
        state.slowTimerInitialized[slot] = value;
    }

    public int ironMaskPrivateState2(int slot) {
        return state.ironMaskPrivateState2[slot];
    }
}
