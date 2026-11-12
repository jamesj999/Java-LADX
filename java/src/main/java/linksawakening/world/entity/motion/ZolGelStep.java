package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.ZolGelMotion.*;

import linksawakening.world.entity.*;
import linksawakening.world.entity.RoomEntity;

/** Per-invocation scratch for ZolGelBehavior.advance; values are assigned in source order. */
final class ZolGelStep {
    private linksawakening.world.entity.motion.ZolGelMotion owner;
    private linksawakening.world.entity.motion.ZolGelFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkEntityX;
    private int linkEntityY;
    private int linkEntityZ;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int frameCounter;
    private int ignoreHitsCountdown;
    private boolean joypadHeld;
    private int slot;
    private int z;
    private boolean hitGround;
    private int type;
    private linksawakening.world.entity.motion.ZolGelMotion.Split split;
    private int x;
    private int y;
    private boolean clinging;
    private linksawakening.world.entity.RoomEntity updated;

    ZolGelStep(
            linksawakening.world.entity.motion.ZolGelMotion owner,
            linksawakening.world.entity.motion.ZolGelFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.motion.ZolGelMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceClingingOrFree()) return completedResult;
        if (!buildSnapshot()) return completedResult;

        return new ZolGelMotion.Update(updated, split, clinging);
    }

    private linksawakening.world.entity.motion.ZolGelMotion.Update completedResult;

    private boolean prepareFrame() {

        entity = inputs.entity();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkEntityZ = inputs.linkEntityZ();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundInteraction = inputs.backgroundInteraction();
        frameCounter = inputs.frameCounter();
        ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        joypadHeld = inputs.joypadHeld();
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot, entity.type(), randomByteSupplier);
        }
        owner.decrementCountdowns(slot);

        z = ZolGelMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 3) & 0xFF;
        hitGround = (z & 0x80) != 0;
        if (hitGround) {
            z = 0;
            owner.speedZ[slot] = 0;
        }

        type = entity.type();
        split = null;
        if (type == ENTITY_ZOL && owner.flashCountdown[slot] == 8) {
            owner.flashCountdown[slot] = 0;
            int originalX = entity.x();
            int originalY = entity.y();
            int originalZ = z;
            owner.resetForGel(slot);
            owner.speedZ[slot] = 0x20;
            split = new ZolGelMotion.Split(originalX, originalY, originalZ);
            type = ENTITY_GEL;
        }

        if (owner.privateCountdown2[slot] == 0) {
            owner.spriteVariant[slot] = owner.state[slot] & 0x01;
            if (owner.spriteVariant[slot] == 1) {
                owner.privateCountdown2[slot] = 8;
            }
        }

        x = entity.x();
        y = entity.y();
        clinging = type == ENTITY_GEL && owner.state[slot] == 4 && owner.transitionCountdown[slot] != 0;
        return true;
    }

    private boolean advanceClingingOrFree() {

        if (clinging) {
            advanceClinging();
        } else {
            if (owner.state[slot] == 0) {
                int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                x = moved[0];
                y = moved[1];
                if (owner.transitionCountdown[slot] == 0) {
                    owner.transitionCountdown[slot] = 7;
                    owner.state[slot] = 1;
                    ZolGelMotion.Vector vector = ZolGelMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 4);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                }
            } else if (owner.state[slot] == 1) {
                if (owner.transitionCountdown[slot] == 0) {
                    owner.transitionCountdown[slot] = 0x10;
                    owner.speedX[slot] = 0;
                    owner.speedY[slot] = 0;
                    if ((randomByteSupplier.getAsInt() & 0x0F) != 0) {
                        // IncrementEntityState ($06:7D0B) returns HL pointing
                        // to wEntitiesStateTable, not the countdown table.
                        // LD [HL],B ($7D0E) cancels the leap: inch again in
                        // state zero after the retained $10-frame pause.
                        owner.state[slot] = 0;
                        int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                        x = moved[0];
                        y = moved[1];
                    } else {
                        owner.transitionCountdown[slot] = 0x50;
                        owner.state[slot] = 2;
                    }
                } else {
                    int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                    x = moved[0];
                    y = moved[1];
                }
            } else if (owner.state[slot] == 2) {
                if (owner.transitionCountdown[slot] == 0) {
                    owner.state[slot] = 3;
                    ZolGelMotion.Vector vector = ZolGelMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x10);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                    owner.speedZ[slot] = 0x20;
                } else {
                    owner.speedX[slot] = (owner.transitionCountdown[slot] & 0x04) == 0 ? 0x08 : 0xF8;
                    owner.speedY[slot] = 0;
                    int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                    x = moved[0];
                    y = moved[1];
                }
            } else if (owner.state[slot] == 3) {
                int[] moved = owner.physics(entity, x, y, backgroundInteraction, frameCounter);
                x = moved[0];
                y = moved[1];
                if (hitGround) {
                    owner.state[slot] = 0;
                }
            } else if (owner.state[slot] == 4) {
                if (owner.transitionCountdown[slot] == 0) {
                    owner.privateCountdown3[slot] = 0x30;
                    ZolGelMotion.Vector vector = ZolGelMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x10);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                    owner.speedZ[slot] = 0x20;
                    z = (z + 1) & 0xFF;
                    owner.state[slot] = 3;
                }
            }
        }
        return true;
    }

    private boolean buildSnapshot() {

        updated = new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                type,
                x,
                y,
                entity.status(),
                entity.spriteDefinition(),
                owner.spriteVariant[slot],
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                z,
                -1,
                false);
        return true;
    }

    private void advanceClinging() {
        int xOffset = ((owner.transitionCountdown[slot] >>> 1) & 0x07) - 4;
        int yOffset = ((owner.transitionCountdown[slot] >>> 2) & 0x07) - 4;
        x = (linkEntityX - xOffset) & 0xFF;
        y = (linkEntityY - yOffset) & 0xFF;
        z = linkEntityZ & 0xFF;
        owner.applyClingingBackgroundProbe(entity, x, y, backgroundInteraction, ignoreHitsCountdown, frameCounter);
        if (joypadHeld) {
            owner.decreaseTransitionCountdown(slot);
            owner.decreaseTransitionCountdown(slot);
            owner.decreaseTransitionCountdown(slot);
        }
    }
}
