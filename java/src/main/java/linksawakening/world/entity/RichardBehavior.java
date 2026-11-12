package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityStoryController. */
final class RichardBehavior {
    static void advanceRichard(
            EntityStoryController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne) {
        new RichardStep(owner, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne).run();
    }

    static void enableBowWowFollowing(EntityStoryController owner) {
        var runtime = owner.runtime;
        runtime.motions()
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .writeFollowingNpcState(new FollowingNpcState(
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .roosterFollowing(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .ghostFollowingState(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .marinFollowing(),
                        true,
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .instrument4Flags(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .powerBraceletLevel(),
                        runtime.motions()
                                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                .readFollowingNpcState()
                                .db10()));
    }
}
