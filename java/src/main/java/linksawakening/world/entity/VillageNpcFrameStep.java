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
    private linksawakening.world.entity.EntityStoryController runtime;
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

        runtime = owner;
        slot = entity.slot();
        if (owner.state.readKidBall(slot)) {
            if (owner.state.preKidPlayTimer(slot, -1) <= 0) {
                runtime.slots[slot] = withStatus(entity, EntityStatus.DISABLED);
                owner.state.writeKidBall(slot, false);
                return false;
            }
            int x = addFallingSpeedToPosition(
                    entity.x(), owner.state.readKidPlayState(slot), owner.state.storageKidSpeedXAccumulator(), slot);
            int z = addFallingSpeedToPosition(
                    entity.z(),
                    owner.state.postKidZSpeed(slot, -1) & 255,
                    owner.state.storageKidSpeedYAccumulator(),
                    slot);
            runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, entity.y(), 0), z);
            return false;
        }
        if (runtime.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState() == 0x80) {
            owner.advanceKidnapWarningKid(entity, frame, linkX, linkY);
            return false;
        }
        owner.pushLinkFromKid(entity, linkX, linkY);
        if (runtime.spriteHandlers != null)
            entity = withDefinition(
                    entity,
                    runtime.spriteViews.forKidPlaying(),
                    (entity.type() == 0x71 ? 2 : 0) | (owner.state.readKidPlayState(slot) == 1 ? 1 : 0));
        z = addFallingSpeedToPosition(
                entity.z(), owner.state.postKidZSpeed(slot, -1) & 255, owner.state.storageKidSpeedYAccumulator(), slot);
        landed = (z & 128) != 0;
        if (landed) {
            z = 0;
            owner.state.writeKidZSpeed(slot, 0);
        }
        runtime.slots[slot] = withZ(entity, z);
        timer = owner.state.readKidPlayTimer(slot);
        if (timer > 0) timer--;
        return true;
    }

    private boolean advancePlayTimer() {

        if (timer == 0) {
            if (owner.state.readKidPlayState(slot) == 0) {
                owner.state.writeKidPlayState(slot, 1);
                timer = 0x80;
                int ball =
                        EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
                if (ball >= 0 && runtime.spriteHandlers != null) {
                    var definition = runtime.spriteViews.forKidBall();
                    runtime.slots[ball] = new RoomEntity(
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
                    runtime.lifecycle.initializeHealthGroup(ball);
                    runtime.physicsTables.writeEnemyPhysicsFlags(ball, ENTITY_PHYSICS_HARMLESS);
                    runtime.combatTables.writeEnemyHitboxFlags(ball, 0);
                    runtime.physicsTables.writeEntityOptions1Override(
                            ball, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
                    owner.state.writeKidBall(ball, true);
                    owner.state.writeKidPlayTimer(ball, 0x24);
                    owner.state.writeKidPlayState(ball, entity.type() == 0x71 ? 0x14 : 0xEC);
                    owner.state.writeKidZSpeed(ball, 0x10);
                    owner.state.writeKidSpeedXAccumulator(ball, 0);
                    owner.state.writeKidSpeedYAccumulator(ball, 0);
                    runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(ball, true);
                }
            } else {
                owner.state.writeKidPlayState(slot, 0);
                timer = 0x60;
            }
        } else if (owner.state.readKidPlayState(slot) == 1 && timer < 0x60) {
            runtime.slots[slot] = withVariant(runtime.slots[slot], entity.type() == 0x71 ? 2 : 0);
            if (timer < 0x40 && landed) owner.state.writeKidZSpeed(slot, 8);
        }
        owner.state.writeKidPlayTimer(slot, timer);
        return true;
    }

    private boolean openKidDialog() {

        if (runtime.linkState.actionButtonAHeld
                && !runtime.linkState.dialogActive
                && runtime.linkState.dialogCooldown == 0
                && !runtime.linkState.inventoryAppearing
                && runtime.linkState.windowY == 0x80
                && Math.abs(signedByte(linkX - entity.x())) < 0x10
                && Math.abs(signedByte(linkY - entity.y())) < 0x14) {
            int dialog = (runtime.roomVariables.readInstrument1Flags() & 2) == 0
                    ? 0x18 + (owner.state.postKidHintIndex(1) & 3)
                    : 0x1C + entity.type() - 0x70;
            runtime.events.pendingDialogRequests.add(new DialogRequest(1, dialog));
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
