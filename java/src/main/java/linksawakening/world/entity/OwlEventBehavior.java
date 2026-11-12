package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityStoryController. */
final class OwlEventBehavior {
    static void advanceOwlEvent(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean handlerLinkInteractive) {
        new OwlEventStep(owner, entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive).run();
    }

    static void advanceDepartingOwl(EntityStoryController owner, RoomEntity entity, int slot, int frameCounter) {
        var runtime = owner.runtime;
        runtime.blockLinkForOwl(slot);
        RoomEntity flying = runtime.roomInspection().owlPresentation(entity, frameCounter);
        int x = addFallingSpeedToPosition(
                flying.x(),
                owner.owlState().readOwlSpeedX(slot),
                owner.owlState().storageOwlXAccumulator(),
                slot);
        int y = addFallingSpeedToPosition(
                flying.y(),
                owner.owlState().readOwlSpeedY(slot),
                owner.owlState().storageOwlYAccumulator(),
                slot);
        int z = addFallingSpeedToPosition(
                flying.z(),
                owner.owlState().readOwlSpeedZ(slot),
                owner.owlState().storageOwlZAccumulator(),
                slot);
        runtime.slots()[slot] = withZ(withPositionAndVariant(flying, x, y, 0), z);
        if ((frameCounter & 0x07) == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x05));
        }
        if (x < 0x08 || x > 0x98 || y < 0x10 || y > 0x88 || z >= 0x40) {
            runtime.presentationState()
                    .writePendingMusicTrack(
                            runtime.bossRewards().readEnemyDropActivePowerUp()
                                    ? 0x49
                                    : owner.owlState().readOwlPreviousMusicTrack(slot));
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }
}
