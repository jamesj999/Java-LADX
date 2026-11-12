package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.LikeLikeEvent;

/** Bank-$06 Like LikeEntityHandler state and its shared Gibdo walk. */
public final class LikeLikeMotion {
    static final int INVENTORY_SHIELD = 0x04;
    static final int LINK_MOTION_DEFAULT = 0x00;
    static final int LINK_MOTION_NON_INTERACTIVE = 0x02;

    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final GibdoMotion walkMotion = new GibdoMotion();

    public record Update(RoomEntity entity, int slowTransitionCountdown, LikeLikeEvent event, int spriteVariant) {}

    public void initialize(int slot) {
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = true;
        walkMotion.initialize(slot);
    }

    public void clear(int slot) {
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = false;
        walkMotion.clear(slot);
    }

    public Update advance(LikeLikeFrame inputs) {
        return new LikeLikeFrameStep(this, inputs).run();
    }

    public int state(int slot) {
        return privateState3[slot];
    }

    public int privateState1(int slot) {
        return privateState1[slot];
    }

    public int inertia(int slot) {
        return inertia[slot];
    }

    public int walkState(int slot) {
        return walkMotion.state(slot);
    }

    public int walkSpeedY(int slot) {
        return walkMotion.speedY(slot);
    }
}
