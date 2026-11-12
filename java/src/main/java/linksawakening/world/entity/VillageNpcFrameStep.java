package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;

/** Reused, reentrancy-safe scratch for VillageNpcBehavior.advanceVillageKid; values are assigned in source order. */
final class VillageNpcFrameStep {
    private linksawakening.world.entity.EntityStoryController owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frame;
    private int linkX;
    private int linkY;
    private linksawakening.world.entity.EntityStoryControllerServices runtime;
    private int slot;
    private int z;
    private boolean landed;
    private int timer;

    VillageNpcFrameStep() {}

    void run() {
        if (!advanceKidMotion()) return;
        if (!advancePlayTimer()) return;
        if (!openKidDialog()) return;
    }

    private boolean advanceKidMotion() {

        runtime = owner.runtime;
        slot = entity.slot();
        if (owner.readKidBall(slot)) {
            if (owner.preKidPlayTimer(slot, -1) <= 0) {
                runtime.slots()[slot] = withStatus(entity, EntityStatus.DISABLED);
                owner.writeKidBall(slot, false);
                return false;
            }
            int x = addFallingSpeedToPosition(
                    entity.x(), owner.readKidPlayState(slot), owner.storageKidSpeedXAccumulator(), slot);
            int z = addFallingSpeedToPosition(
                    entity.z(), owner.postKidZSpeed(slot, -1) & 255, owner.storageKidSpeedYAccumulator(), slot);
            runtime.slots()[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
            return false;
        }
        if (runtime.motions().get(BowWowMotion.class, BowWowMotion::new).readBowWowState() == 0x80) {
            owner.advanceKidnapWarningKid(entity, frame, linkX, linkY);
            return false;
        }
        owner.pushLinkFromKid(entity, linkX, linkY);
        if (runtime.spriteHandlers() != null)
            entity = withDefinition(
                    entity,
                    runtime.spriteHandlers().forKidPlaying(),
                    (entity.type() == 0x71 ? 2 : 0) | (owner.readKidPlayState(slot) == 1 ? 1 : 0));
        z = addFallingSpeedToPosition(
                entity.z(), owner.postKidZSpeed(slot, -1) & 255, owner.storageKidSpeedYAccumulator(), slot);
        landed = (z & 128) != 0;
        if (landed) {
            z = 0;
            owner.writeKidZSpeed(slot, 0);
        }
        runtime.slots()[slot] = withZ(entity, z);
        timer = owner.readKidPlayTimer(slot);
        if (timer > 0) timer--;
        return true;
    }

    private boolean advancePlayTimer() {

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
        return true;
    }

    private boolean openKidDialog() {

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
        return true;
    }

    private boolean inUse;

    void run(
            linksawakening.world.entity.EntityStoryController owner,
            linksawakening.world.entity.RoomEntity entity,
            int frame,
            int linkX,
            int linkY) {
        if (inUse) {
            new VillageNpcFrameStep().run(owner, entity, frame, linkX, linkY);
            return;
        }
        inUse = true;
        runtime = null;
        slot = 0;
        z = 0;
        landed = false;
        timer = 0;
        this.owner = owner;
        this.entity = entity;
        this.frame = frame;
        this.linkX = linkX;
        this.linkY = linkY;
        try {
            run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.runtime = null;
        }
    }
}
