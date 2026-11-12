package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.HookshotChainMotion.*;

/** Focused behavior using the state owned by HookshotChainMotion. */
final class HookshotChainBehavior {
    static HookshotChainMotion.Step advance(HookshotChainMotion.State state, int linkX, int linkY, boolean blocked) {
        return HookshotChainMotion.advance(state, linkX, linkY, blocked, true);
    }

    static HookshotChainMotion.Step advance(
            HookshotChainMotion.State state, int linkX, int linkY, boolean blocked, boolean tickCountdown) {
        if (state == null) {
            throw new IllegalArgumentException("Hookshot state cannot be null");
        }
        HookshotChainMotion.validateByte(linkX, "linkX");
        HookshotChainMotion.validateByte(linkY, "linkY");
        if (blocked) {
            return new HookshotChainMotion.Step(state, true, false, state.transitionCountdown() == 0);
        }

        HookshotChainMotion.SpeedStep nextX =
                HookshotChainMotion.addSpeed(state.x(), state.speedX(), state.speedXAccumulator());
        HookshotChainMotion.SpeedStep nextY =
                HookshotChainMotion.addSpeed(state.y(), state.speedY(), state.speedYAccumulator());
        int nextCountdown = tickCountdown ? Math.max(0, state.transitionCountdown() - 1) : state.transitionCountdown();
        boolean returning = nextCountdown == 0;
        int nextSpeedX = state.speedX();
        int nextSpeedY = state.speedY();
        if (returning) {
            HookshotChainMotion.Vector vector = HookshotChainMotion.vectorTowardsLink(
                    nextX.position(), nextY.position(), state.z(), linkX, linkY, HOOKSHOT_SPEED);
            nextSpeedX = vector.x();
            nextSpeedY = vector.y();
        }
        HookshotChainMotion.State next = new HookshotChainMotion.State(
                nextX.position(),
                nextY.position(),
                state.z(),
                state.direction(),
                nextSpeedX,
                nextSpeedY,
                nextCountdown,
                nextX.accumulator(),
                nextY.accumulator(),
                state.entityState(),
                state.wallCollisionPending());
        // The chain is spawned on Link's collision box. The ROM only treats
        // that overlap as the unload condition after the transition countdown
        // has switched the entity to its return path; otherwise the outbound
        // launch would immediately collide with its owner.
        return new HookshotChainMotion.Step(
                next, false, returning && HookshotChainMotion.overlapsLink(state, linkX, linkY), returning);
    }

    static HookshotChainMotion.Step advance(
            HookshotChainMotion owner, int slot, int linkX, int linkY, boolean blocked) {
        HookshotChainMotion.validateSlot(slot);
        HookshotChainMotion.State state = owner.requireState(slot);
        HookshotChainMotion.Step step = HookshotChainMotion.advance(state, linkX, linkY, blocked);
        owner.states[slot] = step.state();
        return step;
    }

    static HookshotChainMotion.State copy(HookshotChainSnapshot inputs) {
        HookshotChainMotion.State state = inputs.state();
        int x = inputs.x();
        int y = inputs.y();
        int z = inputs.z();
        int direction = inputs.direction();
        int speedX = inputs.speedX();
        int speedY = inputs.speedY();
        int transitionCountdown = inputs.transitionCountdown();
        int speedXAccumulator = inputs.speedXAccumulator();
        int speedYAccumulator = inputs.speedYAccumulator();
        int entityState = inputs.entityState();
        boolean wallCollisionPending = inputs.wallCollisionPending();
        return new HookshotChainMotion.State(
                x,
                y,
                z,
                direction,
                speedX,
                speedY,
                transitionCountdown,
                speedXAccumulator,
                speedYAccumulator,
                entityState,
                wallCollisionPending);
    }
}
