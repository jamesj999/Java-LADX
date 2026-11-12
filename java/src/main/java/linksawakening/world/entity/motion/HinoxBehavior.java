package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.HinoxMotion.*;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Focused behavior using the state owned by HinoxMotion. */
final class HinoxBehavior {
    static HinoxMotion.Update advance(HinoxMotion owner, HinoxFrame inputs) {
        RoomEntity entity = inputs.entity();
        int transitionCountdown = inputs.transitionCountdown();
        int frameCounter = inputs.frameCounter();
        int linkX = inputs.linkX();
        int linkY = inputs.linkY();
        int linkZ = inputs.linkZ();
        int linkMotionState = inputs.linkMotionState();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundInteraction backgroundInteraction = inputs.backgroundInteraction();
        int entityCollisionFlagsForFrame = inputs.entityCollisionFlagsForFrame();
        int flashCountdown = inputs.flashCountdown();
        HinoxMotion.validateSlot(entity.slot());
        if (!owner.initialized[entity.slot()]) {
            owner.initialize(entity.slot());
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Hinox random source cannot be null");
        }

        int slot = entity.slot();
        boolean linkMotionBlockedAtEntry = owner.privateCountdown1[slot] != 0;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int transition = transitionCountdown & 0xFF;
        int frame = frameCounter & 0xFF;
        int linkSpeedX = 0;
        int linkSpeedY = 0;
        int linkVelocityZ = 0;
        int airborne = 0;
        int linkDamage = 0;
        int jingle = -1;
        int wave = -1;
        boolean dust = false;
        HinoxMotion.BombSpawn bomb = null;
        int spriteVariant = entity.spriteVariant() < 0 ? 0 : entity.spriteVariant();

        // HinoxEntityHandler performs this branch before selecting a private
        // state. A sword flash at exactly $03 is the ROM's bomb transition.
        if (owner.state[slot] < STATE_GRAB && (flashCountdown & 0xFF) == 0x03) {
            owner.state[slot] = STATE_BOMB;
            transition = 0x20;
            return owner.update(new HinoxResult(
                    entity,
                    x,
                    y,
                    transition,
                    spriteVariant,
                    linkMotionBlockedAtEntry,
                    linkX,
                    linkY,
                    linkZ,
                    linkSpeedX,
                    linkSpeedY,
                    linkVelocityZ,
                    airborne,
                    linkDamage,
                    jingle,
                    wave,
                    dust,
                    false,
                    false,
                    bomb));
        }

        switch (owner.state[slot]) {
            case STATE_INITIAL -> {
                if (((owner.entityCollisionFlags[slot] | entityCollisionFlagsForFrame) & 0x0F) != 0
                        || transition == 0) {
                    transition = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
                    owner.state[slot] = STATE_WANDER;
                    owner.speedX[slot] = 0;
                    owner.speedY[slot] = 0;
                }
                HinoxMotion.Position next = owner.move(entity, x, y, backgroundInteraction, slot);
                x = next.x();
                y = next.y();
                spriteVariant = owner.tickInertia(slot);
            }
            case STATE_WANDER -> {
                if (transition == 0) {
                    owner.privateState3[slot] = (owner.privateState3[slot] + 1) & 0xFF;
                    if (owner.privateState3[slot] == 2) {
                        owner.privateState3[slot] = 0;
                        if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                            owner.state[slot] = STATE_CHARGE_WINDUP;
                            transition = 0x30;
                            owner.speedX[slot] = 0;
                            owner.speedY[slot] = 0;
                            break;
                        }
                    }
                    transition = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                    owner.state[slot] = STATE_WANDER;
                    owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0x03;
                    int direction = owner.privateState1[slot] == 0
                            ? HinoxMotion.directionToLink(x, y, linkX, linkY)
                            : randomByteSupplier.getAsInt() & 0x03;
                    owner.speedX[slot] = SPEED_X_BY_DIRECTION[direction];
                    owner.speedY[slot] = SPEED_Y_BY_DIRECTION[direction];
                }
                spriteVariant = entity.spriteVariant();
            }
            case STATE_CHARGE_WINDUP -> {
                if (transition == 0) {
                    transition = 0x20;
                    owner.state[slot] = STATE_CHARGE;
                    HinoxMotion.Vector vector = HinoxMotion.vectorTowardsLink(x, y, entity.z(), linkX, linkY, 0x18);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                }
                owner.tickInertia(slot);
                spriteVariant = owner.tickInertia(slot);
                if ((frame & 0x0F) == 0) {
                    jingle = 0x20;
                }
            }
            case STATE_CHARGE -> {
                if (transition == 0) {
                    owner.state[slot] = STATE_GRAB;
                    transition = 0x4F;
                    wave = 0x16;
                }
                HinoxMotion.Position next = owner.move(entity, x, y, backgroundInteraction, slot);
                x = next.x();
                y = next.y();
                if (owner.state[slot] == STATE_CHARGE
                        && HinoxMotion.withinDistance(x, linkX, 0x18)
                        && HinoxMotion.withinDistance(y, linkY, 0x18)
                        && (linkMotionState & 0xFF) == 0) {
                    owner.state[slot] = STATE_GRAB;
                    transition = 0x4F;
                    jingle = 0x16;
                }
                dust = (frame & 0x07) == 0;
                if (dust) {
                    // State 3 writes hMultiPurpose0/1 for the later bomb
                    // handler. Preserve those source registers per entity.
                    owner.bombOriginX[slot] = x & 0xFF;
                    owner.bombOriginY[slot] = (y + 0x0A) & 0xFF;
                    owner.bombOriginWritten[slot] = true;
                }
                owner.tickInertia(slot);
                spriteVariant = owner.tickInertia(slot);
                if ((frame & 0x0F) == 0) {
                    jingle = 0x20;
                }
            }
            case STATE_GRAB -> {
                if (transition == 0) {
                    owner.state[slot] = STATE_INITIAL;
                    transition = 0;
                    return owner.update(new HinoxResult(
                            entity,
                            x,
                            y,
                            transition,
                            spriteVariant,
                            linkMotionBlockedAtEntry,
                            linkX,
                            linkY,
                            linkZ,
                            linkSpeedX,
                            linkSpeedY,
                            linkVelocityZ,
                            airborne,
                            linkDamage,
                            jingle,
                            wave,
                            dust,
                            false,
                            false,
                            bomb));
                } else if (transition == 0x20) {
                    linkSpeedY = 0x20;
                    linkSpeedX = (linkX & 0xFF) >= 0x50 ? 0xE0 : 0x20;
                    linkVelocityZ = 0x10;
                    airborne = 0x02;
                    linkDamage = 0x08;
                    jingle = 0x08;
                    linkX = x;
                    linkY = y;
                    owner.privateCountdown1[slot] = 0x50;
                    return owner.update(new HinoxResult(
                            entity,
                            x,
                            y,
                            transition,
                            entity.spriteVariant(),
                            linkMotionBlockedAtEntry,
                            linkX,
                            linkY,
                            linkZ,
                            linkSpeedX,
                            linkSpeedY,
                            linkVelocityZ,
                            airborne,
                            linkDamage,
                            jingle,
                            wave,
                            dust,
                            true,
                            false,
                            bomb));
                }
                boolean blocked = linkMotionBlockedAtEntry;
                int heldX = linkX;
                int heldY = linkY;
                int heldZ = linkZ;
                boolean heldPositionWritten = false;
                boolean applyHeldLinkPose = false;
                if (transition >= 0x20 && transition <= 0x4F) {
                    int index = (transition - 0x20) >>> 2;
                    index = Math.min(index, GRAB_LINK_X_OFFSETS.length - 1);
                    heldX = (x + GRAB_LINK_X_OFFSETS[index]) & 0xFF;
                    heldY = y;
                    heldZ = GRAB_LINK_Z[index];
                    heldPositionWritten = true;
                    airborne = 0x02;
                    applyHeldLinkPose = true;
                }
                spriteVariant = GRAB_BODY_VARIANT[Math.min(transition >>> 3, GRAB_BODY_VARIANT.length - 1)];
                return owner.update(new HinoxResult(
                        entity,
                        x,
                        y,
                        transition,
                        spriteVariant,
                        blocked,
                        heldX,
                        heldY,
                        heldZ,
                        linkSpeedX,
                        linkSpeedY,
                        linkVelocityZ,
                        airborne,
                        linkDamage,
                        jingle,
                        wave,
                        dust,
                        heldPositionWritten,
                        applyHeldLinkPose,
                        bomb));
            }
            case STATE_BOMB -> {
                if (transition == 0) {
                    owner.state[slot] = STATE_INITIAL;
                } else if (transition == 0x10) {
                    int originX = owner.bombOriginWritten[slot] ? owner.bombOriginX[slot] : x;
                    int originY = owner.bombOriginWritten[slot] ? owner.bombOriginY[slot] : y;
                    HinoxMotion.Vector vector =
                            HinoxMotion.vectorTowardsLink((originX - 0x0C) & 0xFF, originY, 0x10, linkX, linkY, 0x10);
                    bomb = new HinoxMotion.BombSpawn(
                            ENTITY_BOMB, (originX - 0x0C) & 0xFF, originY, 0x10, vector.x(), vector.y(), 0x18, 0x40);
                    jingle = 0x08;
                }
                spriteVariant = transition >= 0x10 ? 1 : 0;
            }
            default -> {
                owner.state[slot] = STATE_INITIAL;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                spriteVariant = 0;
            }
        }
        return owner.update(new HinoxResult(
                entity,
                x,
                y,
                transition,
                spriteVariant,
                linkMotionBlockedAtEntry,
                linkX,
                linkY,
                0,
                linkSpeedX,
                linkSpeedY,
                linkVelocityZ,
                airborne,
                linkDamage,
                jingle,
                wave,
                dust,
                false,
                false,
                bomb));
    }

    static HinoxMotion.Position move(
            HinoxMotion owner, RoomEntity entity, int x, int y, RoomEntityBackgroundInteraction background, int slot) {
        owner.entityCollisionFlags[slot] = 0;
        int nextX = HinoxMotion.addSpeed(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        if (nextX != x && background != null) {
            EntityBackgroundCollisionResult result =
                    background.probe(entity, HinoxMotion.horizontalDirection(owner.speedX[slot]), nextX, y);
            if (result.blocked()) {
                owner.entityCollisionFlags[slot] |= result.collisionFlag();
                nextX = x;
            }
        }
        int nextY = HinoxMotion.addSpeed(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        if (nextY != y && background != null) {
            EntityBackgroundCollisionResult result =
                    background.probe(entity, HinoxMotion.verticalDirection(owner.speedY[slot]), nextX, nextY);
            if (result.blocked()) {
                owner.entityCollisionFlags[slot] |= result.collisionFlag();
                nextY = y;
            }
        }
        return new HinoxMotion.Position(nextX, nextY);
    }
}
