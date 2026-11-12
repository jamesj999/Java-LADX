package linksawakening.world.entity.motion;

import linksawakening.world.entity.*;

/** Test-only writes to owned motion state. */
public final class MotionTestFixture {
    private MotionTestFixture() {}

    public static void setStateForTest(CrowMotion motion, int slot, int value) {
        motion.ensureInitialized(slot);
        motion.state[slot] = value & 0xFF;
    }

    public static void setSpeedForTest(CrowMotion motion, int slot, int x, int y, int z) {
        motion.ensureInitialized(slot);
        motion.speedX[slot] = x & 0xFF;
        motion.speedY[slot] = y & 0xFF;
        motion.speedZ[slot] = z & 0xFF;
    }

    public static void setPrivateCountdown1ForTest(StalfosEvasiveMotion motion, int slot, int value) {
        motion.initialized[slot] = true;
        motion.privateCountdown1[slot] = value & 0xFF;
    }

    public static void setFleeingForTest(StalfosEvasiveMotion motion, int slot, int newSpeedX, int newSpeedY) {
        motion.reset(slot);
        motion.initialized[slot] = true;
        motion.privateState1[slot] = 1;
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
    }

    public static void setPrivateCountdownsForTest(MoblinKingMotion motion, int slot, int countdown1, int countdown2) {
        motion.privateCountdown1[slot] = countdown1 & 0xFF;
        motion.privateCountdown2[slot] = countdown2 & 0xFF;
    }

    public static void setStateForTest(
            MoblinKingMotion motion,
            int slot,
            int nextState,
            int nextDirection,
            int nextSpeedX,
            int nextSpeedY,
            int nextSpeedZ) {
        motion.state[slot] = nextState;
        motion.direction[slot] = nextDirection & 1;
        motion.speedX[slot] = MoblinKingMotion.signedByte(nextSpeedX);
        motion.speedY[slot] = MoblinKingMotion.signedByte(nextSpeedY);
        motion.speedZ[slot] = MoblinKingMotion.signedByte(nextSpeedZ);
    }

    public static int stateForTest(WitchMotion motion, int slot) {
        return motion.state[slot];
    }

    public static void setStateForTest(WitchMotion motion, int slot, int value) {
        motion.state[slot] = value;
    }

    public static void setInertiaForTest(WitchMotion motion, int slot, int value) {
        motion.inertia[slot] = value & 0xFF;
    }

    public static void setForTest(
            WizrobeProjectileMotion motion, int slot, int newSpeedX, int newSpeedY, int newDirection) {
        if ((newSpeedX & ~0xFF) != 0 || (newSpeedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Wizrobe projectile speeds must be bytes");
        }
        WizrobeProjectileMotion.checkDirection(newDirection);
        motion.reset(slot);
        motion.speedX[slot] = newSpeedX;
        motion.speedY[slot] = newSpeedY;
        motion.direction[slot] = newDirection;
        motion.initialized[slot] = true;
    }

    public static void forceStateForTest(
            PincerMotion motion, int slot, int newState, int transitionCountdown, int physicsFlags) {
        motion.state[slot] = newState & 0xFF;
        motion.lungeVariant[slot] = 2;
        motion.speedX[slot] = 0;
        motion.speedY[slot] = 0;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setHoleForTest(PincerMotion motion, int slot, int x, int y) {
        motion.holeX[slot] = x & 0xFF;
        motion.holeY[slot] = y & 0xFF;
    }

    public static void setSpeedForTest(FairyMotion motion, int slot, int newSpeedX, int newSpeedY, int ignoredSpeedZ) {
        FairyMotion.validateByte(ignoredSpeedZ, "Fairy Z speed");
        motion.setSpeed(slot, newSpeedX, newSpeedY);
    }

    public static void setForTest(
            WizrobeMotion motion,
            int slot,
            int newState,
            int transitionCountdown,
            int newPrivateState1,
            int newPrivateCountdown1,
            int newDirection,
            int spriteVariant) {
        if (newState < 0 || newState > 3) {
            throw new IllegalArgumentException("Wizrobe state out of range: " + newState);
        }
        WizrobeMotion.checkByte(transitionCountdown, "Wizrobe transition countdown");
        WizrobeMotion.checkByte(newPrivateState1, "Wizrobe private state");
        WizrobeMotion.checkByte(newPrivateCountdown1, "Wizrobe private countdown");
        if (newDirection < 0 || newDirection > 3) {
            throw new IllegalArgumentException("Wizrobe direction out of range: " + newDirection);
        }
        if (spriteVariant < -1 || spriteVariant > 4) {
            throw new IllegalArgumentException("Wizrobe sprite variant out of range: " + spriteVariant);
        }
        motion.state[slot] = newState;
        motion.privateState1[slot] = newPrivateState1;
        motion.privateCountdown1[slot] = newPrivateCountdown1;
        motion.direction[slot] = newDirection;
        motion.currentSpriteVariant[slot] = spriteVariant;
        motion.initialized[slot] = true;
    }

    public static void forceStateForTest(
            WingedOctorokMotion motion,
            int slot,
            int newState,
            int transitionCountdown,
            int newPrivateCountdown2,
            int z,
            int newSpeedZ) {
        motion.state[slot] = newState & 0xFF;
        motion.direction[slot] = 0;
        motion.privateState1[slot] = 0;
        motion.privateState2[slot] = 0;
        motion.privateCountdown2[slot] = newPrivateCountdown2 & 0xFF;
        motion.speedX[slot] = 0;
        motion.speedY[slot] = 0;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.zPosition[slot] = z & 0xFF;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.speedZAccumulator[slot] = 0;
        motion.inertia[slot] = 0;
        motion.presentationVariant[slot] = 0;
        motion.initialized[slot] = true;
        motion.zInitialized[slot] = true;
    }

    public static void setStateForTest(
            CuccoMotion motion,
            int slot,
            int newState,
            int transitionCountdown,
            int newPrivateState1,
            int newSpeedX,
            int newSpeedY,
            int newSpeedZ) {
        motion.ensureInitialized(slot);
        motion.state[slot] = newState & 0xFF;
        motion.privateState1[slot] = newPrivateState1 & 0xFF;
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.speedZAccumulator[slot] = 0;
    }

    public static void setPrivateStateForTest(CuccoMotion motion, int slot, int value) {
        motion.ensureInitialized(slot);
        motion.privateState1[slot] = value & 0xFF;
    }

    public static void setSpeedForTest(MimicMotion motion, int slot, int newSpeedX, int newSpeedY) {
        MimicMotion.validateSlot(slot);
        MimicMotion.validateByte(newSpeedX, "Mimic X speed");
        MimicMotion.validateByte(newSpeedY, "Mimic Y speed");
        motion.speedX[slot] = newSpeedX;
        motion.speedY[slot] = newSpeedY;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
    }

    public static void setPrivateCountdown1ForTest(ZolGelMotion motion, int slot, int value) {
        motion.privateCountdown1[slot] = value & 0xFF;
    }

    public static void setStateForTest(
            HinoxMotion motion, int slot, int value, int privateState1Value, int privateState3Value) {
        HinoxMotion.validateSlot(slot);
        if (value < 0 || value > HinoxMotion.STATE_BOMB) throw new IllegalArgumentException("Invalid Hinox state");
        motion.state[slot] = value;
        motion.privateState1[slot] = privateState1Value & 0x03;
        motion.privateState3[slot] = privateState3Value & 0xFF;
        motion.initialized[slot] = true;
    }

    public static void setSpeedForTest(HinoxMotion motion, int slot, int x, int y) {
        HinoxMotion.validateSlot(slot);
        motion.speedX[slot] = x & 0xFF;
        motion.speedY[slot] = y & 0xFF;
    }

    public static void setInertiaForTest(HinoxMotion motion, int slot, int value) {
        HinoxMotion.validateSlot(slot);
        motion.inertia[slot] = value & 0xFF;
    }

    public static void setEntityCollisionForTest(HinoxMotion motion, int slot, int value) {
        HinoxMotion.validateSlot(slot);
        motion.entityCollisionFlags[slot] = value & 0xFF;
    }

    public static void setPrivateState3ForTest(HinoxMotion motion, int slot, int value) {
        HinoxMotion.validateSlot(slot);
        motion.privateState3[slot] = value & 0xFF;
    }

    public static void setPrivateCountdown1ForTest(HinoxMotion motion, int slot, int value) {
        HinoxMotion.validateSlot(slot);
        motion.privateCountdown1[slot] = value & 0xFF;
    }

    public static void setParentForTest(LaserMotion motion, int slot, int countdown, int newSpeedX, int newSpeedY) {
        motion.initializeParent(slot);
        motion.transitionCountdown[slot] = countdown & 0xFF;
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
    }

    public static void setBeamForTest(LaserMotion motion, int slot, int newSpeedX, int newSpeedY, int beamDirection) {
        motion.initializeBeam(slot, beamDirection, newSpeedX, newSpeedY);
    }

    public static void setPrivateStateForTest(BooBuddyMotion motion, int slot, int value) {
        motion.ensureInitialized(slot);
        motion.privateState1[slot] = value & 0xFF;
    }

    public static void setSpeedForTest(BooBuddyMotion motion, int slot, int x, int y, int z) {
        if ((z & 0xFF) != 0) {
            throw new IllegalArgumentException("Boo Buddy has no test Z speed");
        }
        motion.ensureInitialized(slot);
        motion.speedX[slot] = x & 0xFF;
        motion.speedY[slot] = y & 0xFF;
    }

    public static void setForTest(
            PairoddProjectileMotion motion, int slot, int newSpeedX, int newSpeedY, int newDirection) {
        if ((newSpeedX & ~0xFF) != 0 || (newSpeedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Pairodd projectile speeds must be bytes");
        }
        if (newDirection < 0 || newDirection > 3) {
            throw new IllegalArgumentException("Pairodd projectile direction out of range");
        }
        motion.speedX[slot] = (byte) newSpeedX;
        motion.speedY[slot] = (byte) newSpeedY;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.direction[slot] = newDirection;
        motion.initialized[slot] = true;
    }

    public static void forceStateForTest(
            BushCrawlerMotion motion,
            int slot,
            int newState,
            int transitionCountdown,
            int newPrivateState1,
            int newPrivateState4,
            int newSpeedX,
            int newSpeedY) {
        motion.state[slot] = newState & 0xFF;
        motion.privateState1[slot] = newPrivateState1 & 0xFF;
        motion.privateState2[slot] = newState == 1 ? 4 : 0;
        motion.privateState4[slot] = newPrivateState4 & 0x01;
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.presentationVariant[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void forcePrivateStateForTest(
            BushCrawlerMotion motion, int slot, int newPrivateState1, int transitionCountdown) {
        motion.state[slot] = 0;
        motion.privateState1[slot] = newPrivateState1 & 0xFF;
        motion.privateState2[slot] = 0;
        motion.privateState4[slot] = 0;
        motion.speedX[slot] = 0;
        motion.speedY[slot] = 0;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.presentationVariant[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setPrivateCountdown1ForTest(RoamingEnemyMotion motion, int slot, int value) {
        motion.privateCountdown1[slot] = value & 0xFF;
    }

    public static void setStateForTest(
            RoamingEnemyMotion motion,
            int slot,
            int newState,
            int newTransitionCountdown,
            int newInertia,
            int newPrivateState1,
            int newDirection) {
        motion.state[slot] = newState & 0xFF;
        motion.transitionCountdown[slot] = newTransitionCountdown & 0xFF;
        motion.inertia[slot] = newInertia & 0xFF;
        motion.privateState1[slot] = newPrivateState1 & 0x03;
        motion.direction[slot] = newDirection & 0x03;
        motion.speedX[slot] = RoamingEnemyMotion.SPEED_X_BY_DIRECTION[motion.direction[slot]];
        motion.speedY[slot] = RoamingEnemyMotion.SPEED_Y_BY_DIRECTION[motion.direction[slot]];
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.collisionsTable[slot] = 0;
        motion.horizontallyCollidedObject[slot] = 0;
        motion.verticallyCollidedObject[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setStateForTest(
            TarinRaccoonMotion motion,
            int slot,
            int newState,
            int newSpeedX,
            int newSpeedY,
            int newSpeedZ,
            int privateState2,
            int privateState3,
            boolean nearLatch) {
        if (newState < 0 || newState > 3) {
            throw new IllegalArgumentException("Tarin state must be 0..3");
        }
        motion.state[slot] = newState;
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.animationAccumulator[slot] = privateState2 & 0xFF;
        motion.animationInertia[slot] = privateState3 & 0xFF;
        motion.animationIndex[slot] = 0;
        motion.nearLinkLatch[slot] = nearLatch;
        motion.accumulatorX[slot] = 0;
        motion.accumulatorY[slot] = 0;
        motion.accumulatorZ[slot] = 0;
    }

    public static void setSpeedForTest(
            EnemyProjectileMotion motion, int slot, int newSpeedX, int newSpeedY, int newSpeedZ) {
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.speedZAccumulator[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setTransitionForTest(
            EnemyProjectileMotion motion, int slot, int countdown, int z, int newSpeedZ) {
        motion.transitionCountdown[slot] = countdown & 0xFF;
        motion.zPosition[slot] = z & 0xFF;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.speedZAccumulator[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setSpeedForTest(
            PlayerArrowMotion motion, int slot, int newSpeedX, int newSpeedY, int newSpeedZ) {
        PlayerArrowMotion.validateSlot(slot);
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
        motion.speedZAccumulator[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setTransitionForTest(PlayerArrowMotion motion, int slot, int countdown, int z, int newSpeedZ) {
        PlayerArrowMotion.validateSlot(slot);
        motion.transitionCountdown[slot] = countdown & 0xFF;
        motion.zPosition[slot] = z & 0xFF;
        motion.speedZ[slot] = newSpeedZ & 0xFF;
        motion.speedZAccumulator[slot] = 0;
        motion.initialized[slot] = true;
    }

    public static void setStateForTest(
            WitchRatMotion motion,
            int slot,
            int newState,
            int transitionCountdown,
            int newDirection,
            int newSpeedX,
            int newSpeedY,
            int newSpeedZ) {
        WitchRatMotion.validateSlot(slot);
        if (newState < 0 || newState > 1) {
            throw new IllegalArgumentException("Witch Rat state must be 0 or 1: " + newState);
        }
        WitchRatMotion.validateByte(transitionCountdown, "Witch Rat transition countdown");
        if (newDirection != 0 && newDirection != 0x04) {
            throw new IllegalArgumentException("Witch Rat direction must be 0 or 4: " + newDirection);
        }
        WitchRatMotion.validateByte(newSpeedX, "Witch Rat X speed");
        WitchRatMotion.validateByte(newSpeedY, "Witch Rat Y speed");
        WitchRatMotion.validateByte(newSpeedZ, "Witch Rat Z speed");
        motion.reset(slot);
        motion.state[slot] = newState;
        motion.direction[slot] = newDirection;
        motion.speedX[slot] = newSpeedX;
        motion.speedY[slot] = newSpeedY;
        motion.speedZ[slot] = newSpeedZ;
        motion.initialized[slot] = true;
    }

    public static void setStateForTest(
            DogMotion motion, int slot, int newState, int newDirection, int newSpeedX, int newSpeedY, int newSpeedZ) {
        if (newState < 0 || newState > 3) {
            throw new IllegalArgumentException("Dog state must be 0..3: " + newState);
        }
        DogMotion.validateByte(newDirection, "Dog direction");
        DogMotion.validateByte(newSpeedX, "Dog X speed");
        DogMotion.validateByte(newSpeedY, "Dog Y speed");
        DogMotion.validateByte(newSpeedZ, "Dog Z speed");
        motion.reset(slot);
        motion.state[slot] = newState;
        motion.direction[slot] = newDirection;
        motion.speedX[slot] = newSpeedX;
        motion.speedY[slot] = newSpeedY;
        motion.speedZ[slot] = newSpeedZ;
        motion.initialized[slot] = true;
    }

    public static void setStateForTest(
            ColorShellMotion motion,
            int slot,
            int state,
            int transitionCountdown,
            int direction,
            int speedX,
            int speedY) {
        motion.restoreState(slot, state, transitionCountdown, direction, speedX, speedY);
    }

    public static void setTransitionCountdownForTest(BowWowMotion motion, int slot, int value) {
        motion.setTransitionCountdown(slot, value);
    }

    public static void setStateForTest(
            BlooperMotion motion,
            int slot,
            int newState,
            int newCountdown,
            int newDirection,
            int newSpeedX,
            int newSpeedY) {
        if (!motion.initialized[slot]) {
            motion.initialize(slot);
        }
        motion.state[slot] = newState;
        motion.transitionCountdown[slot] = newCountdown & 0xFF;
        motion.direction[slot] = newDirection & 0xFF;
        motion.speedX[slot] = newSpeedX & 0xFF;
        motion.speedY[slot] = newSpeedY & 0xFF;
        motion.speedXAccumulator[slot] = 0;
        motion.speedYAccumulator[slot] = 0;
    }

    public static void setSettledForTest(ThreeOfAKindMotion motion, int slot, int face) {
        motion.state[slot] = 2;
        motion.direction[slot] = face & 3;
    }
}
