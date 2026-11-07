package linksawakening.world.entity.event;

import linksawakening.world.RoomEntity;
import linksawakening.vfx.TransientVfxType;

/** Link-side HRAM effects emitted by Hinox's grab/throw handler. */
public record HinoxLinkEffectRequest(int sourceSlot, int heldX, int heldY, int heldZ,
                                     int speedX, int speedY, int velocityZ,
                                     int airborneState, int damage,
                                     boolean motionBlocked,
                                     boolean applyHeldLinkPose) implements EntityRequest {
    public HinoxLinkEffectRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Hinox source slot out of range: " + sourceSlot);
        }
        for (int value : new int[] {heldX, heldY, heldZ, speedX, speedY, velocityZ,
                                    airborneState, damage}) {
            if ((value & ~0xFF) != 0) {
                throw new IllegalArgumentException("Hinox Link effect must be byte-shaped");
            }
        }
    }

    public boolean throwing() {
        return speedX != 0 || speedY != 0 || velocityZ != 0 || damage != 0;
    }
}
