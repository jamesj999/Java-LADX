package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Focused behavior using the state owned by EntityStoryController. */
final class VillageNpcBehavior {
    static void advanceVillageKid(EntityStoryController owner, RoomEntity entity, int frame, int linkX, int linkY) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (owner.readKidBall(slot)) {
            if (owner.preKidPlayTimer(slot, -1) <= 0) {
                runtime.slots()[slot] = withStatus(entity, EntityStatus.DISABLED);
                owner.writeKidBall(slot, false);
                return;
            }
            int x = addFallingSpeedToPosition(
                    entity.x(), owner.readKidPlayState(slot), owner.storageKidSpeedXAccumulator(), slot);
            int z = addFallingSpeedToPosition(
                    entity.z(), owner.postKidZSpeed(slot, -1) & 255, owner.storageKidSpeedYAccumulator(), slot);
            runtime.slots()[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
            return;
        }
        if (runtime.motions().get(BowWowMotion.class, BowWowMotion::new).readBowWowState() == 0x80) {
            owner.advanceKidnapWarningKid(entity, frame, linkX, linkY);
            return;
        }
        owner.pushLinkFromKid(entity, linkX, linkY);
        if (runtime.spriteHandlers() != null)
            entity = withDefinition(
                    entity,
                    runtime.spriteHandlers().forKidPlaying(),
                    (entity.type() == 0x71 ? 2 : 0) | (owner.readKidPlayState(slot) == 1 ? 1 : 0));
        int z = addFallingSpeedToPosition(
                entity.z(), owner.postKidZSpeed(slot, -1) & 255, owner.storageKidSpeedYAccumulator(), slot);
        boolean landed = (z & 128) != 0;
        if (landed) {
            z = 0;
            owner.writeKidZSpeed(slot, 0);
        }
        runtime.slots()[slot] = withZ(entity, z);
        int timer = owner.readKidPlayTimer(slot);
        if (timer > 0) timer--;
        if (timer == 0) {
            if (owner.readKidPlayState(slot) == 0) {
                owner.writeKidPlayState(slot, 1);
                timer = 0x80;
                int ball = runtime.findFreeEntitySlot();
                if (ball >= 0 && runtime.spriteHandlers() != null) {
                    var definition = runtime.spriteHandlers().forKidBall();
                    runtime.slots()[ball] = new RoomEntity(
                            ball,
                            -1,
                            0x71,
                            entity.x(),
                            entity.y(),
                            EntityStatus.ACTIVE,
                            definition,
                            0,
                            0,
                            0,
                            0,
                            -1,
                            false);
                    runtime.initializeHealthGroup(ball);
                    runtime.physicsTables().writeEnemyPhysicsFlags(ball, ENTITY_PHYSICS_HARMLESS);
                    runtime.combatTables().writeEnemyHitboxFlags(ball, 0);
                    runtime.physicsTables()
                            .writeEntityOptions1Override(ball, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
                    owner.writeKidBall(ball, true);
                    owner.writeKidPlayTimer(ball, 0x24);
                    owner.writeKidPlayState(ball, entity.type() == 0x71 ? 0x14 : 0xEC);
                    owner.writeKidZSpeed(ball, 0x10);
                    owner.writeKidSpeedXAccumulator(ball, 0);
                    owner.writeKidSpeedYAccumulator(ball, 0);
                    runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(ball, true);
                }
            } else {
                owner.writeKidPlayState(slot, 0);
                timer = 0x60;
            }
        } else if (owner.readKidPlayState(slot) == 1 && timer < 0x60) {
            runtime.slots()[slot] = withVariant(runtime.slots()[slot], entity.type() == 0x71 ? 2 : 0);
            if (timer < 0x40 && landed) owner.writeKidZSpeed(slot, 8);
        }
        owner.writeKidPlayTimer(slot, timer);
        if (runtime.linkState().actionButtonAHeld
                && !runtime.linkState().dialogActive
                && runtime.linkState().dialogCooldown == 0
                && !runtime.linkState().inventoryAppearing
                && runtime.linkState().windowY == 0x80
                && Math.abs(signedByte(linkX - entity.x())) < 0x10
                && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
            int dialog = (runtime.roomVariables().readInstrument1Flags() & 2) == 0
                    ? 0x18 + (owner.postKidHintIndex(1) & 3)
                    : 0x1C + entity.type() - 0x70;
            runtime.events().pendingDialogRequests.add(new DialogRequest(1, dialog));
        }
    }

    static void advanceKidnapWarningKid(
            EntityStoryController owner, RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int facingVariant = ((yDistance < 0 ? 0 : 2) ^ 2) + ((frameCounter >>> 3) & 0x01);
        runtime.slots()[slot] = withVariant(entity, facingVariant);
        switch (owner.readKidKidnapState(slot)) {
            case 0 -> {
                owner.writeKidKidnapDelay(slot, 0x30);
                runtime.presentationState().writePendingMusicTrack(0x0E);
                owner.writeKidKidnapState(slot, 1);
            }
            case 1 -> {
                if ((yDistance >= -0x20 && yDistance < 0x20 || owner.preKidKidnapDelay(slot, -1) == 0)
                        && runtime.roomVariables().readTransitionSequenceCounter() == 0x04) {
                    if (entity.type() == EntityRomConstants.ENTITY_KID_71) {
                        runtime.events().pendingDialogRequests.add(new DialogRequest(2, 0x20));
                    }
                    owner.writeKidKidnapState(slot, 2);
                } else {
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLink(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x08);
                    runtime.slots()[slot] = withPositionAndVariant(
                            entity,
                            addFallingSpeedToPosition(
                                    entity.x(), vector.x() & 0xFF, owner.storageKidSpeedXAccumulator(), slot),
                            addFallingSpeedToPosition(
                                    entity.y(), vector.y() & 0xFF, owner.storageKidSpeedYAccumulator(), slot),
                            facingVariant);
                    runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
                }
            }
            default -> {
                owner.pushLinkFromKid(entity, linkEntityX, linkEntityY);
                int z = addFallingSpeedToPosition(
                        entity.z(), owner.readKidZSpeed(slot) & 255, owner.storageKidSpeedYAccumulator(), slot);
                owner.writeKidZSpeed(slot, owner.readKidZSpeed(slot) - (2));
                if (z == 0 || (z & 128) != 0) {
                    z = 0;
                    owner.writeKidZSpeed(slot, (frameCounter & 31) == 0 ? 0x10 : 0);
                }
                runtime.slots()[slot] = withZ(runtime.slots()[slot], z);
                int xDistance = Math.abs(signedByte((linkEntityX - entity.x()) & 0xFF));
                if (runtime.linkState().actionButtonsHeld
                        && !runtime.linkState().dialogActive
                        && xDistance < 0x18
                        && Math.abs(yDistance) < 0x18) {
                    runtime.events().pendingDialogRequests.add(new DialogRequest(2, 0x20));
                }
            }
        }
    }

    static void advanceMadamMeowMeow(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 1) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
            runtime.events().pendingMadamMeowMeowFullHealRequests.add(new MadamMeowMeowFullHealRequest(slot));
        }
        int xDistance = signedByte((linkEntityX - entity.x()) & 0xFF);
        int yDistance = signedByte((linkEntityY - entity.y()) & 0xFF);
        int direction = Math.abs(xDistance) >= Math.abs(yDistance) ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
        int variant = direction * 2 + ((frameCounter >>> 4) & 0x01);
        runtime.slots()[entity.slot()] = withVariant(entity, variant);
        boolean linkFacingMadam = (runtime.linkState().lastRomLinkDirection ^ 0x01) == direction;
        boolean interactionWindow = ((yDistance + 0x14) & 0xFF) < 0x28 && ((xDistance + 0x10) & 0xFF) < 0x20;
        if (runtime.linkState().actionButtonAHeld
                && interactionWindow
                && linkFacingMadam
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !linkAirborne
                && runtime.linkState().dialogCooldown == 0
                && runtime.linkState().windowY == 0x80) {
            if ((runtime.roomVariables().readInstrument2Flags() & 0x02) != 0
                    && runtime.motions()
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState()
                            == 0x01) {
                runtime.motions().get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(0);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x10);
                runtime.events().pendingDialogRequests.add(new DialogRequest(1, 0x2F));
                return;
            }
            int dialogLowId =
                    switch (runtime.motions()
                            .get(BowWowMotion.class, BowWowMotion::new)
                            .readBowWowState()) {
                        case 0 -> 0x30;
                        case 1 -> 0x32;
                        default -> 0x31;
                    };
            runtime.events().pendingDialogRequests.add(new DialogRequest(1, dialogLowId));
        }
    }

    static void advanceOwlStatue(
            EntityStoryController owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int linkMotionState,
            boolean linkAirborne) {
        var runtime = owner.runtime;
        // The shipped handler pushes before ReturnIfNonInteractive_18.
        // CheckLinkCollisionWithEnemy uses the already incremented visual Y;
        // translating the Link probe keeps the statue's world position unchanged.
        if (linkZ == 0
                && linkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY - 1)) {
            runtime.backgroundMechanics().requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
        if (!runtime.roomVariables().readGameplayWorld()
                || runtime.roomVariables().readTransitionSequenceCounter() != 0x04
                || runtime.linkState().dialogActive
                || runtime.linkState().inventoryAppearing
                || runtime.roomVariables().readRoomTransitionActive()
                || runtime.inspection().witchGotItemPresentationActive()
                || owner.readOwlStatueDialogId() < 0) {
            return;
        }
        int dx = signedByte((linkEntityX - entity.x()) & 0xFF);
        int dy = signedByte((linkEntityY - entity.y()) & 0xFF);
        if (runtime.roomInspection().sourceNpcInteractionAllowed(dx, dy, linkAirborne, directionToLink(dx, dy))) {
            runtime.events()
                    .pendingDialogRequests
                    .add(new DialogRequest(owner.readOwlStatueDialogId() >>> 8, owner.readOwlStatueDialogId() & 0xFF));
        }
    }
}
