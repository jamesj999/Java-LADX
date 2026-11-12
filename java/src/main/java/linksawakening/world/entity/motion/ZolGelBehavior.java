package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.ZolGelMotion.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Focused behavior using the state owned by ZolGelMotion. */
final class ZolGelBehavior {
    static ZolGelMotion.Update advance(
            ZolGelMotion owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return owner.advance(new ZolGelFrame(
                entity,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                backgroundCollision == null ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision),
                0,
                0,
                false));
    }

    static ZolGelMotion.Update advance(ZolGelMotion owner, ZolGelFrame inputs) {
        RoomEntity entity = inputs.entity();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundInteraction backgroundInteraction = inputs.backgroundInteraction();
        int frameCounter = inputs.frameCounter();
        int ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        boolean joypadHeld = inputs.joypadHeld();
        int slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot, entity.type(), randomByteSupplier);
        }
        owner.decrementCountdowns(slot);

        int z = ZolGelMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 3) & 0xFF;
        boolean hitGround = (z & 0x80) != 0;
        if (hitGround) {
            z = 0;
            owner.speedZ[slot] = 0;
        }

        int type = entity.type();
        ZolGelMotion.Split split = null;
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

        int x = entity.x();
        int y = entity.y();
        boolean clinging = type == ENTITY_GEL && owner.state[slot] == 4 && owner.transitionCountdown[slot] != 0;
        if (clinging) {
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

        RoomEntity updated = new RoomEntity(
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
        return new ZolGelMotion.Update(updated, split, clinging);
    }

    static int[] move(
            ZolGelMotion owner,
            RoomEntity entity,
            int x,
            int y,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter) {
        int slot = entity.slot();
        int movedX = ZolGelMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        int movedY = ZolGelMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        if (backgroundInteraction != null) {
            if (movedX != x
                    && backgroundInteraction
                            .probe(
                                    entity,
                                    ZolGelMotion.signedByte(owner.speedX[slot]) < 0 ? 1 : 0,
                                    movedX,
                                    y,
                                    ignoreHitsCountdown,
                                    frameCounter)
                            .blocked()) {
                movedX = x;
            }
            if (movedY != y
                    && backgroundInteraction
                            .probe(
                                    entity,
                                    ZolGelMotion.signedByte(owner.speedY[slot]) < 0 ? 2 : 3,
                                    movedX,
                                    movedY,
                                    ignoreHitsCountdown,
                                    frameCounter)
                            .blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
    }

    static ZolGelMotion.Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = ZolGelMotion.signedByte((linkX - entityX) & 0xFF);
        int distanceY = ZolGelMotion.signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new ZolGelMotion.Vector(x & 0xFF, y & 0xFF);
    }
}
