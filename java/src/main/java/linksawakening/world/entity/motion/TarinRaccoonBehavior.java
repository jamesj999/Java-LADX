package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.TarinRaccoonMotion.*;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Focused behavior using the state owned by TarinRaccoonMotion. */
final class TarinRaccoonBehavior {
    static TarinRaccoonMotion.Update advance(
            TarinRaccoonMotion owner, RoomEntity source, TarinRaccoonMotion.Input input) {
        return owner.advance(source, input, null);
    }

    static TarinRaccoonMotion.Update advance(
            TarinRaccoonMotion owner,
            RoomEntity source,
            TarinRaccoonMotion.Input input,
            RoomEntityBackgroundInteraction backgroundInteraction) {
        TarinRaccoonMotion.checkType(source);
        int slot = source.slot();
        int slowCountdown = input.slowTransitionCountdown();
        int transitionCountdown = input.transitionCountdown();
        int variant = source.spriteVariant();
        boolean shouldGetLost = false;
        int dialog = NO_EVENT;
        boolean blockLink = false;
        boolean clearAttack = false;
        int linkFacing = NO_EVENT;
        boolean pushLink = false;
        boolean roomChanged = false;
        boolean tarinFlag = false;
        int soundId = NO_EVENT;
        boolean spawnBomb = false;
        int collisionFlags = 0;
        RoomEntity entity = source;

        switch (owner.state[slot]) {
            case 0 -> {
                variant = (input.frameCounter() >>> 4) & 1;
                if ((input.linkY() & 0xFF) < 0x30) {
                    shouldGetLost = true;
                    variant = 2 + ((input.frameCounter() >>> 3) & 1);
                } else {
                    owner.warningShown[slot] = false;
                }
                entity = TarinRaccoonMotion.withVariant(source, variant);
                if (!input.dialogActive() && !input.inventoryAppearing()) {
                    if ((input.linkY() & 0xFF) < 0x20 && !owner.warningShown[slot]) {
                        owner.warningShown[slot] = true;
                        dialog = 0x021;
                    } else if (input.actionHeld()
                            && TarinRaccoonMotion.nearbyAndFacing(entity, input)
                            && input.linkAttackStepAnimationCountdown() == 0
                            && !input.linkAirborne()
                            && input.dialogCooldown() == 0
                            && input.windowY() == 0x80) {
                        dialog = 0x00D;
                    }
                }
            }
            case 1 -> {
                blockLink = true;
                clearAttack = true;
                linkFacing = TarinRaccoonMotion.directionToLink(source, input.linkX(), input.linkY()) ^ 0x01;

                int animationSum = owner.animationAccumulator[slot] + owner.animationInertia[slot];
                owner.animationAccumulator[slot] = animationSum & 0xFF;
                if (animationSum > 0xFF) {
                    owner.animationIndex[slot] = (owner.animationIndex[slot] + 1) % 6;
                }
                variant = TRANSFORMATION_VARIANTS[owner.animationIndex[slot]];
                entity = TarinRaccoonMotion.withVariant(source, variant);

                if (slowCountdown == 0) {
                    owner.speedZ[slot] = 0;
                    owner.state[slot] = 2;
                    variant = 9;
                    entity = TarinRaccoonMotion.withVariant(entity, variant);
                    spawnBomb = true;
                    roomChanged = true;
                    tarinFlag = true;
                    break;
                }

                if ((input.frameCounter() & 1) == 0 && owner.animationInertia[slot] < 0xF0) {
                    owner.animationInertia[slot]++;
                }
                TarinRaccoonMotion.Movement moved = owner.moveWithBackground(entity, backgroundInteraction, slot);
                entity = TarinRaccoonMotion.withPositionAndVariant(entity, moved.x(), moved.y(), variant);
                collisionFlags = moved.collisionFlags();

                if (slowCountdown < 6) {
                    if ((entity.y() & 0xFF) < 0x30) {
                        slowCountdown = 8;
                    } else {
                        owner.speedZ[slot] = (owner.speedZ[slot] + 1) & 0xFF;
                        owner.speedX[slot] = TarinRaccoonMotion.dampTowardsZero(owner.speedX[slot]);
                        owner.speedY[slot] = TarinRaccoonMotion.dampTowardsZero(owner.speedY[slot]);
                        entity = TarinRaccoonMotion.withZ(
                                entity,
                                TarinRaccoonMotion.addSpeedToPosition(
                                        entity.z(), owner.speedZ[slot], owner.accumulatorZ, slot));
                        break;
                    }
                }

                if ((collisionFlags & 0x03) != 0) {
                    owner.speedX[slot] = (-TarinRaccoonMotion.signedByte(owner.speedX[slot])) & 0xFF;
                    soundId = 0x09;
                }
                if ((collisionFlags & 0x0C) != 0) {
                    owner.speedY[slot] = (-TarinRaccoonMotion.signedByte(owner.speedY[slot])) & 0xFF;
                    soundId = 0x09;
                }
                if (slowCountdown < 0x60 && (input.frameCounter() & 3) == 0) {
                    owner.speedX[slot] = TarinRaccoonMotion.accelerateToLimit(owner.speedX[slot]);
                    owner.speedY[slot] = TarinRaccoonMotion.accelerateToLimit(owner.speedY[slot]);
                }
            }
            case 2 -> {
                blockLink = true;
                int z = TarinRaccoonMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.accumulatorZ, slot);
                owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
                entity = TarinRaccoonMotion.withZ(entity, z);
                if ((z & 0x80) != 0) {
                    entity = TarinRaccoonMotion.withZ(entity, 0);
                    soundId = 0x23;
                    transitionCountdown = 0x40;
                    variant = 8 + TarinRaccoonMotion.directionToLink(entity, input.linkX(), input.linkY());
                    entity = TarinRaccoonMotion.withVariant(entity, variant);
                    owner.nearLinkLatch[slot] = TarinRaccoonMotion.withinSignedWindow(input.linkX() - entity.x(), 0x12)
                            && TarinRaccoonMotion.withinSignedWindow(input.linkY() - entity.y(), 0x12);
                    owner.state[slot] = 3;
                }
            }
            case 3 -> {
                if (transitionCountdown == 1) {
                    dialog = 0x00A;
                    break;
                }
                if (transitionCountdown != 0) {
                    blockLink = true;
                    break;
                }
                if ((input.frameCounter() & 0x1F) == 0) {
                    variant = 8 + TarinRaccoonMotion.directionToLink(entity, input.linkX(), input.linkY());
                    entity = TarinRaccoonMotion.withVariant(entity, variant);
                }
                pushLink = !owner.nearLinkLatch[slot];
                if (!input.dialogActive()
                        && !input.inventoryAppearing()
                        && input.actionHeld()
                        && TarinRaccoonMotion.nearbyAndFacing(entity, input)
                        && !input.linkAirborne()
                        && input.dialogCooldown() == 0
                        && input.windowY() == 0x80) {
                    dialog = 0x00B;
                }
            }
            default -> throw new IllegalStateException("Invalid Tarin state: " + owner.state[slot]);
        }

        return new TarinRaccoonMotion.Update(
                entity,
                owner.state[slot],
                entity.spriteVariant(),
                shouldGetLost,
                dialog,
                blockLink,
                clearAttack,
                linkFacing,
                pushLink,
                roomChanged,
                tarinFlag,
                soundId < 0 ? NO_EVENT : 0,
                soundId,
                spawnBomb,
                slowCountdown,
                transitionCountdown,
                owner.speedX[slot] & 0xFF,
                owner.speedY[slot] & 0xFF,
                owner.speedZ[slot] & 0xFF,
                collisionFlags,
                owner.nearLinkLatch[slot]);
    }

    static TarinRaccoonMotion.Movement moveWithBackground(
            TarinRaccoonMotion owner, RoomEntity entity, RoomEntityBackgroundInteraction interaction, int slot) {
        int x = TarinRaccoonMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.accumulatorX, slot);
        int y = TarinRaccoonMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.accumulatorY, slot);
        int flags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = TarinRaccoonMotion.signedByte(owner.speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, entity.y());
            if (result.blocked()) {
                flags |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = TarinRaccoonMotion.signedByte(owner.speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                flags |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new TarinRaccoonMotion.Movement(x & 0xFF, y & 0xFF, flags & 0x0F);
    }
}
