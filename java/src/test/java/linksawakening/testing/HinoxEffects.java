package linksawakening.testing;

import linksawakening.world.entity.event.HinoxLinkEffectRequest;

/** Named ROM-byte inputs for Hinox grab/throw boundary tests. */
public final class HinoxEffects {
    private int sourceSlot;
    private int heldX;
    private int heldY;
    private int heldZ;
    private int speedX;
    private int speedY;
    private int velocityZ;
    private int airborneState;
    private int damage;
    private boolean motionBlocked;
    private boolean applyHeldLinkPose;

    public HinoxEffects sourceSlot(int value) {
        sourceSlot = value;
        return this;
    }

    public HinoxEffects heldX(int value) {
        heldX = value;
        return this;
    }

    public HinoxEffects heldY(int value) {
        heldY = value;
        return this;
    }

    public HinoxEffects heldZ(int value) {
        heldZ = value;
        return this;
    }

    public HinoxEffects speedX(int value) {
        speedX = value;
        return this;
    }

    public HinoxEffects speedY(int value) {
        speedY = value;
        return this;
    }

    public HinoxEffects velocityZ(int value) {
        velocityZ = value;
        return this;
    }

    public HinoxEffects airborneState(int value) {
        airborneState = value;
        return this;
    }

    public HinoxEffects damage(int value) {
        damage = value;
        return this;
    }

    public HinoxEffects motionBlocked(boolean value) {
        motionBlocked = value;
        return this;
    }

    public HinoxEffects applyHeldLinkPose(boolean value) {
        applyHeldLinkPose = value;
        return this;
    }

    public HinoxLinkEffectRequest build() {
        return new HinoxLinkEffectRequest(
                sourceSlot,
                heldX,
                heldY,
                heldZ,
                speedX,
                speedY,
                velocityZ,
                airborneState,
                damage,
                motionBlocked,
                applyHeldLinkPose);
    }
}
