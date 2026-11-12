package linksawakening.world.entity;

/** Reused view of ROM slot state, preserving array indexing and values. */
public final class EntitySlotView {
    private final EntityCombatTables combat;
    private final EntityPhysicsTables physics;
    private final linksawakening.world.entity.motion.UnmaskedIronMaskMotion ironMask;

    EntitySlotView(
            EntityCombatTables combat,
            EntityPhysicsTables physics,
            linksawakening.world.entity.motion.UnmaskedIronMaskMotion ironMask) {
        this.combat = combat;
        this.physics = physics;
        this.ironMask = ironMask;
    }

    public int transitionCountdown(int slot) {
        return physics.readEnemyTransitionCountdown(slot);
    }

    public void setTransitionCountdown(int slot, int value) {
        physics.writeEnemyTransitionCountdown(slot, value);
    }

    public int ignoreHits(int slot) {
        return combat.readEnemyIgnoreHitsCountdown(slot);
    }

    public void setIgnoreHits(int slot, int value) {
        combat.writeEnemyIgnoreHitsCountdown(slot, value);
    }

    public void decrementIgnoreHits(int slot) {
        combat.postEnemyIgnoreHitsCountdown(slot, -1);
    }

    public void shiftIgnoreHitsRight(int slot, int distance) {
        combat.writeEnemyIgnoreHitsCountdown(slot, combat.readEnemyIgnoreHitsCountdown(slot) >>> (distance));
    }

    public int physicsFlags(int slot) {
        return physics.readEnemyPhysicsFlags(slot);
    }

    public void setPhysicsFlags(int slot, int value) {
        physics.writeEnemyPhysicsFlags(slot, value);
    }

    public void andPhysicsFlags(int slot, int mask) {
        physics.writeEnemyPhysicsFlags(slot, physics.readEnemyPhysicsFlags(slot) & (mask));
    }

    public void orPhysicsFlags(int slot, int mask) {
        physics.writeEnemyPhysicsFlags(slot, physics.readEnemyPhysicsFlags(slot) | (mask));
    }

    public int flashCountdown(int slot) {
        return combat.readEnemyFlashCountdown(slot);
    }

    public void setFlashCountdown(int slot, int value) {
        combat.writeEnemyFlashCountdown(slot, value);
    }

    public int healthGroup(int slot) {
        return combat.readEnemyHealthGroup(slot);
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
        combat.writeEnemyHealthGroup(slot, value);
    }

    public int health(int slot) {
        return combat.readEnemyHealth(slot);
    }

    public void setHealth(int slot, int value) {
        combat.writeEnemyHealth(slot, value);
    }

    public int hitboxFlags(int slot) {
        return combat.readEnemyHitboxFlags(slot);
    }

    public void setHitboxFlags(int slot, int value) {
        combat.writeEnemyHitboxFlags(slot, value);
    }

    public int options1Override(int slot) {
        return physics.readEntityOptions1Override(slot);
    }

    public void setOptions1Override(int slot, int value) {
        physics.writeEntityOptions1Override(slot, value);
    }

    public int pokeyInertia(int slot) {
        return physics.readEntityInertia(slot);
    }

    public void setPokeyInertia(int slot, int value) {
        physics.writeEntityInertia(slot, value);
    }

    public int slowTransitionCountdown(int slot) {
        return physics.readSlowTransitionCountdown(slot);
    }

    public void setSlowTransitionCountdown(int slot, int value) {
        physics.writeSlowTransitionCountdown(slot, value);
    }

    public boolean slowTimerInitialized(int slot) {
        return physics.readSlowTimerInitialized(slot);
    }

    public void setSlowTimerInitialized(int slot, boolean value) {
        physics.writeSlowTimerInitialized(slot, value);
    }

    public int ironMaskPrivateState2(int slot) {
        return ironMask.maskRemovalState(slot);
    }
}
