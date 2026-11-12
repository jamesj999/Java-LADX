package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** Per-invocation scratch for GenieBodyBehavior.advanceGenieBody; values are assigned in source order. */
final class GenieBodyStep {
    private linksawakening.world.entity.GenieEntityController owner;
    private linksawakening.world.entity.RoomEntity body;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.GenieEntityControllerServices runtime;
    private int slot;
    private linksawakening.world.entity.RoomEntity moved;
    private int direction;
    private int boundary;

    GenieBodyStep(
            linksawakening.world.entity.GenieEntityController owner,
            linksawakening.world.entity.RoomEntity body,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier) {
        this.owner = owner;
        this.body = body;
        this.frameCounter = frameCounter;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.randomByteSupplier = randomByteSupplier;
    }

    void run() {
        if (!prepareBody()) return;
        if (!advanceDeathState()) return;
        if (!advanceAppearanceState()) return;
        if (!advanceBodyAnimation()) return;
        if (!advanceAttackClock()) return;
        if (!spawnAttackEffects()) return;
    }

    private boolean prepareBody() {

        runtime = owner.runtime;
        slot = body.slot();
        if (owner.genieActiveState[slot] == 0) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) != 0) {
                return false;
            }
            owner.genieActiveState[slot] = 1;
            owner.geniePrivateCountdown1[slot] = 0xFF;
            boolean jarExists = owner.genieControllerSlot >= 0
                    && owner.genieControllerSlot < runtime.slots().length
                    && runtime.slots()[owner.genieControllerSlot].loaded()
                    && runtime.slots()[owner.genieControllerSlot].type() == EntityRomConstants.ENTITY_GENIE;
            if (jarExists) {
                runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x52));
                return false;
            }
            owner.genieActiveState[slot] = 4;
            runtime.combatTables().writeEnemyHealth(slot, 0x08);
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x53));
            return false;
        }
        if (owner.genieActiveState[slot] == 4) {
            runtime.combatTables().writeEnemyHitboxFlags(slot, 0x0C);
            runtime.physicsTables().writeEntityOptions1Override(slot, 0x81);
            if (runtime.combatTables().readEnemyFlashCountdown(slot) == 0x02) {
                owner.genieActiveState[slot] = 5;
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x80);
                return false;
            }
            int x = addFallingSpeedToPosition(body.x(), owner.genieSpeedX[slot], owner.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(body.y(), owner.genieSpeedY[slot], owner.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
            runtime.slots()[slot] = moved;
            if ((frameCounter & 0x03) == 0) {
                EnemyRecoilMotion.Vector desired = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x10);
                owner.genieSpeedX[slot] = sourceGenieSteerStep(owner.genieSpeedX[slot], desired.x());
                owner.genieSpeedY[slot] = sourceGenieSteerStep(owner.genieSpeedY[slot], desired.y());
            }
            return false;
        }
        return true;
    }

    private boolean advanceDeathState() {

        if (owner.genieActiveState[slot] == 5) {
            runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x41);
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                owner.genieActiveState[slot] = 4;
                runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x01);
                RoomEntity resetBody = body;
                if ((randomByteSupplier.getAsInt() & 1) == 0) {
                    resetBody = withPositionAndVariant(
                            body, owner.geniePrivateState4[slot], owner.geniePrivateState3[slot], body.spriteVariant());
                    runtime.slots()[slot] = resetBody;
                }
                owner.genieSpeedX[slot] = 0;
                owner.genieSpeedY[slot] = 0;
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
                owner.spawnGenieAimedFireball(resetBody, slot, linkEntityX, linkEntityY);
                return false;
            }
            EnemyRecoilMotion.Vector centerVector =
                    EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(body.x(), body.y(), body.z(), 0x50, 0x48, 0x20);
            EnemyRecoilMotion.Vector linkVector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), linkEntityX, linkEntityY, 0x04);
            owner.genieSpeedX[slot] = (-centerVector.x() + linkVector.x()) & 0xFF;
            owner.genieSpeedY[slot] = (centerVector.y() + linkVector.y()) & 0xFF;
            int x = addFallingSpeedToPosition(body.x(), owner.genieSpeedX[slot], owner.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(body.y(), owner.genieSpeedY[slot], owner.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, 2 + ((frameCounter >>> 4) & 1));
            runtime.slots()[slot] = moved;
            owner.geniePrivateState3[slot] = (0x90 - moved.y()) & 0xFF;
            owner.geniePrivateState4[slot] = (0xA0 - moved.x()) & 0xFF;
            return false;
        }
        return true;
    }

    private boolean advanceAppearanceState() {

        if (owner.genieActiveState[slot] == 2) {
            if (owner.genieControllerSlot < 0
                    || owner.genieControllerSlot >= runtime.slots().length
                    || !runtime.slots()[owner.genieControllerSlot].loaded()) {
                return false;
            }
            RoomEntity jar = runtime.slots()[owner.genieControllerSlot];
            int targetY = (jar.y() - 0x20) & 0xFF;
            EnemyRecoilMotion.Vector towardJar = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    body.x(), body.y(), body.z(), jar.x(), targetY, 0x10);
            owner.genieSpeedX[slot] = towardJar.x() & 0xFF;
            owner.genieSpeedY[slot] = towardJar.y() & 0xFF;
            int x = addFallingSpeedToPosition(body.x(), owner.genieSpeedX[slot], owner.genieSpeedXAccumulator, slot);
            int y = addFallingSpeedToPosition(body.y(), owner.genieSpeedY[slot], owner.genieSpeedYAccumulator, slot);
            RoomEntity moved = withPositionAndVariant(body, x, y, body.spriteVariant());
            runtime.slots()[slot] = moved;
            int visualY = (moved.y() - moved.z()) & 0xFF;
            if ((((moved.x() - jar.x() + 3) & 0xFF) < 6) && (((visualY - targetY + 3) & 0xFF) < 6)) {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
                owner.genieActiveState[slot] = 3;
            }
            return false;
        }
        return true;
    }

    private boolean advanceBodyAnimation() {

        if (owner.genieActiveState[slot] == 3) {
            int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
            if (countdown == 0) {
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
            }
            if (countdown == 0x04) {
                owner.spawnGenieEntity(
                        body.x(),
                        body.y(),
                        2,
                        0xC7,
                        GenieMotion.INITIAL_HEALTH,
                        GenieMotion.INITIAL_PHYSICS_FLAGS,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x1F));
            }
            return false;
        }
        if (owner.genieActiveState[slot] != 1) {
            return false;
        }
        moved = withPositionAndVariant(
                body,
                addFallingSpeedToPosition(body.x(), owner.genieSpeedX[slot], owner.genieSpeedXAccumulator, slot),
                addFallingSpeedToPosition(body.y(), owner.genieSpeedY[slot], owner.genieSpeedYAccumulator, slot),
                (frameCounter >>> 4) & 1);
        runtime.slots()[slot] = moved;

        direction = owner.genieDirection[slot] & 1;
        if ((frameCounter & 0x07) == 0) {
            int target = direction == 0 ? 0x08 : 0xF8;
            if (owner.genieSpeedX[slot] != target) {
                owner.genieSpeedX[slot] = (owner.genieSpeedX[slot] + (direction == 0 ? 1 : -1)) & 0xFF;
            }
        }
        boundary = direction == 0 ? 0x60 : 0x40;
        if (moved.x() == boundary) {
            owner.genieDirection[slot] ^= 1;
        }
        return true;
    }

    private boolean advanceAttackClock() {

        if ((frameCounter & 1) == 0) {
            int verticalDirection = owner.geniePrivateState2[slot] & 1;
            int target = verticalDirection == 0 ? 0x08 : 0xF8;
            owner.genieSpeedY[slot] = (owner.genieSpeedY[slot] + (verticalDirection == 0 ? 1 : -1)) & 0xFF;
            if (owner.genieSpeedY[slot] == target) {
                owner.geniePrivateState2[slot] ^= 1;
            }
        }

        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            int inertia = runtime.physicsTables().readEntityInertia(slot) & 0xFF;
            if (inertia >= 0x08) {
                owner.genieActiveState[slot] = 2;
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x30);
                return false;
            }
            int[] delays = {0x10, 0x14, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40};
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, delays[inertia]);
            int projectileSlot = owner.spawnGenieEntity(
                    moved.x() - 0x0C,
                    moved.y() - 0x14,
                    3,
                    0,
                    GenieMotion.INITIAL_HEALTH,
                    0x42,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
            if (projectileSlot >= 0) {
                owner.genieSpeedZ[projectileSlot] = 0x20;
                owner.genieSpeedX[projectileSlot] = 0x0C;
            }
        }
        return true;
    }

    private boolean spawnAttackEffects() {

        if (owner.geniePrivateCountdown1[slot] == 0) {
            owner.geniePrivateCountdown1[slot] = 0x30;
            int xOffset = (runtime.physicsTables().readEntityInertia(slot) & 1) == 0 ? -0x0C : 0x0C;
            int fireballSlot = owner.spawnGenieEntity(
                    moved.x() + xOffset,
                    moved.y(),
                    4,
                    0,
                    GenieMotion.INITIAL_HEALTH,
                    0x12,
                    GenieMotion.INITIAL_HITBOX_FLAGS);
            if (fireballSlot >= 0) {
                runtime.physicsTables()
                        .writeEntityInertia(slot, (runtime.physicsTables().readEntityInertia(slot) + 1) & 0xFF);
                owner.genieSpeedZ[fireballSlot] = 0x24;
                EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        moved.x() + xOffset, moved.y(), 0, linkEntityX, linkEntityY, 0x1F);
                owner.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
                owner.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
                owner.geniePrivateCountdown2[slot] = 0x10;
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.NOISE,
                                0x28));
            }
        }
        if (owner.geniePrivateCountdown2[slot] != 0) {
            runtime.slots()[slot] = withPositionAndVariant(
                    runtime.slots()[slot],
                    runtime.slots()[slot].x(),
                    runtime.slots()[slot].y(),
                    2 + (runtime.physicsTables().readEntityInertia(slot) & 1));
        }
        return true;
    }
}
