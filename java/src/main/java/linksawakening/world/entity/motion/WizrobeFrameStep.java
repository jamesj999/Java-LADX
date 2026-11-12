package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Per-invocation scratch for WizrobeMotion.advance; values are assigned in source order. */
final class WizrobeFrameStep {
    private final WizrobeMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private int transitionCountdown;
    private int currentPhysicsFlags;
    private int slot;
    private int renderedVariant;
    private int nextVariant;
    private int countdown;
    private int physicsFlags;
    private linksawakening.world.entity.motion.WizrobeMotion.ProjectileSpawn projectileSpawn;
    private linksawakening.world.entity.RoomEntity updated;

    WizrobeFrameStep(
            WizrobeMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            int transitionCountdown,
            int currentPhysicsFlags) {
        this.owner = owner;
        this.entity = entity;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.randomByteSupplier = randomByteSupplier;
        this.transitionCountdown = transitionCountdown;
        this.currentPhysicsFlags = currentPhysicsFlags;
    }

    linksawakening.world.entity.motion.WizrobeMotion.Update run() {
        if ((entity.type() & 0xFF) != WizrobeMotion.ENTITY_WIZROBE) {
            throw new IllegalArgumentException(
                    "Unsupported Wizrobe motion entity type: 0x" + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Wizrobe random-byte supplier cannot be null");
        }

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot, entity.spriteVariant());
        }
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }

        // RenderActiveEntitySpritesPair runs before the state dispatch. Keep
        // the state-table write separate so the snapshot remains the pixels
        // selected for this handler pass while the next pass sees the newly
        // written variant.
        renderedVariant = owner.currentSpriteVariant[slot];
        nextVariant = renderedVariant;
        countdown = transitionCountdown & 0xFF;
        // State 0..2 explicitly select projectile-noclip physics. State 3
        // writes normal physics on its >=2 countdown path, but its $01/$00
        // branches return without touching the table; preserve the previous
        // value for those two frames.
        physicsFlags = (owner.state[slot] & 0xFF) == 3 && owner.privateCountdown1[slot] >= 0x02
                ? 0x02
                : ((owner.state[slot] & 0xFF) == 3 ? currentPhysicsFlags & 0xFF : 0x42);
        projectileSpawn = null;

        switch (owner.state[slot] & 0xFF) {
            case 0 -> {
                if (!advanceState0()) return completedResult;
            }
            case 1 -> {
                if (!advanceState1()) return completedResult;
            }
            case 2 -> {
                if (!advanceState2()) return completedResult;
            }
            case 3 -> {
                if (!advanceState3()) return completedResult;
            }
            default -> throw new IllegalStateException("Invalid Wizrobe state: " + owner.state[slot]);
        }

        owner.currentSpriteVariant[slot] = WizrobeMotion.normalizeVariant(nextVariant);
        updated = EntitySnapshots.withMotion(
                entity, entity.x(), entity.y(), renderedVariant, entity.entityFlipAttribute(), entity.z(), -1, false);
        return new WizrobeMotion.Update(updated, countdown, physicsFlags, projectileSpawn);
    }

    private linksawakening.world.entity.motion.WizrobeMotion.Update completedResult;

    private boolean advanceState0() {
        if (countdown == 0) {
            owner.state[slot] = 1;
            owner.privateState1[slot] = 0x01;
            owner.privateCountdown1[slot] = 0x20;
        }
        nextVariant = 0xFF;

        return true;
    }

    private boolean advanceState1() {
        if (owner.privateCountdown1[slot] != 0) {
            nextVariant = (owner.privateState1[slot] & 0x02) != 0 ? 0 : 0xFF;
        } else {
            owner.state[slot] = (owner.state[slot] + owner.privateState1[slot]) & 0xFF;
            countdown = 0x30;
        }

        return true;
    }

    private boolean advanceState2() {
        nextVariant = 0;
        if (owner.privateCountdown1[slot] < 0x02) {
            if (owner.privateCountdown1[slot] == 0x01) {
                owner.state[slot] = (owner.state[slot] + owner.privateState1[slot]) & 0xFF;
                if (owner.state[slot] == 0x01) {
                    owner.privateCountdown1[slot] = 0x20;
                }
            } else {
                owner.privateCountdown1[slot] = 0x18;
            }
        }

        return true;
    }

    private boolean advanceState3() {
        if (owner.privateCountdown1[slot] == 0x28) {
            projectileSpawn = owner.projectileSpawn(entity, owner.direction[slot]);
            physicsFlags = 0x02;
        } else if (owner.privateCountdown1[slot] < 0x02) {
            if (owner.privateCountdown1[slot] == 0x01) {
                owner.privateState1[slot] = 0xFF;
                owner.state[slot] = (owner.state[slot] - 1) & 0xFF;
            } else {
                owner.privateCountdown1[slot] = 0x40;
                owner.direction[slot] = WizrobeMotion.directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
                nextVariant = owner.direction[slot] + 1;
            }
        }

        return true;
    }
}
