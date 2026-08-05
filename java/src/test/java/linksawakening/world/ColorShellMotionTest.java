package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class ColorShellMotionTest {

    @Test
    void stateZeroConsumesRomRandomDirectionAndStartsTheWalkingCountdown() {
        ColorShellMotion motion = new ColorShellMotion();
        AtomicInteger randomCalls = new AtomicInteger();
        RoomEntity entity = entity(0x40, 0x40);
        motion.initialize(0);

        ColorShellMotion.Update update = motion.advance(entity, 0, 0x00, 0x00,
            () -> {
                randomCalls.incrementAndGet();
                return 0x06;
            }, null);

        assertEquals(1, randomCalls.get());
        assertEquals(1, motion.state(0));
        assertEquals(3, motion.direction(0));
        assertEquals(0x40, motion.transitionCountdown(0));
        assertEquals(0x40, update.entity().x());
        assertEquals(0x40, update.entity().y());
    }

    @Test
    void stateOneUsesRomDirectionSpeedsAndSignedFixedPointAccumulation() {
        ColorShellMotion motion = new ColorShellMotion();
        RoomEntity entity = entity(0x40, 0x40);
        motion.setStateForTest(0, 1, 0x20, 0, 0, 0);

        ColorShellMotion.Update update = new ColorShellMotion.Update(entity);
        for (int frame = 0; frame < 6; frame++) {
            update = motion.advance(update.entity(), frame, 0x00, 0x00, () -> 0, null);
        }

        assertEquals(0x03, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
        assertEquals(0x41, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(0x1A, motion.transitionCountdown(0));
    }

    @Test
    void stateThreeClampsBothAxesToTheBankThirtySixBounds() {
        ColorShellMotion motion = new ColorShellMotion();
        RoomEntity entity = entity(0x16, 0x1E);
        motion.setStateForTest(0, 3, 0x20, 0, 0xF0, 0xF0);

        ColorShellMotion.Update update = motion.advance(entity, 0, 0, 0, () -> 0, null);

        assertEquals(0x16, update.entity().x());
        assertEquals(0x1E, update.entity().y());

        motion.setStateForTest(0, 3, 0x20, 0, 0x10, 0x10);
        update = motion.advance(entity(0x89, 0x72), 1, 0, 0, () -> 0, null);

        assertEquals(0x89, update.entity().x());
        assertEquals(0x72, update.entity().y());
    }

    @Test
    void walkingStateChargesAtTheTighterLinkProximityWindow() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 1, 0, 0, 0, 0);

        ColorShellMotion.Update update = motion.advance(entity(0x40, 0x40), 0,
            0x4F, 0x4F, () -> 0, null);

        assertEquals(2, motion.state(0));
        assertEquals(0x20, motion.transitionCountdown(0));
        assertEquals(0x0E, Math.max(Math.abs(signed(motion.speedX(0))),
            Math.abs(signed(motion.speedY(0)))));
    }

    @Test
    void stateTwoStartsLandingLoopAndTogglesOnlyOnEvenFrames() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 2, 0, 2, 0, 0);

        ColorShellMotion.Update odd = motion.advance(entity(0x40, 0x40), 1,
            0, 0, () -> 0, null);
        assertEquals(3, motion.state(0));
        assertEquals(0x18, motion.transitionCountdown(0));
        assertEquals(0, odd.entity().spriteVariant());

        ColorShellMotion.Update even = motion.advance(odd.entity(), 2,
            0, 0, () -> 0, null);
        assertEquals(1, even.entity().spriteVariant());
    }

    @Test
    void stateThreeRestartsWalkingWithZeroSpeedAtCountdownBoundary() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 3, 0, 1, 0x10, 0xF0);

        ColorShellMotion.Update update = motion.advance(entity(0x40, 0x40), 0,
            0, 0, () -> 0, null);

        assertEquals(1, motion.state(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
    }

    @Test
    void statesFourAndFiveHoldHarmlessUntilTheRomStatusAllowsWalking() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 2, 1, 0, 0, 0);

        RoomEntity active = entity(0x40, 0x40);
        ColorShellMotion.Update update = motion.advance(active, 0, 0, 0,
            () -> 0, null, 1);
        assertEquals(4, motion.state(0));
        assertEquals(0x80, motion.physicsFlags(0) & 0x80);
        assertEquals(1, update.nextIgnoreHitsCountdown());

        update = motion.advance(update.entity(), 1, 0, 0, () -> 0, null, 0);
        assertEquals(5, motion.state(0));
        assertEquals(0, update.nextIgnoreHitsCountdown());

        update = motion.advance(update.entity(), 2, 0, 0, () -> 0, null, 0);
        assertEquals(1, motion.state(0));
        assertEquals(0, motion.physicsFlags(0) & 0x80);

        motion.setStateForTest(0, 5, 0, 0, 0, 0);
        RoomEntity stunned = new RoomEntity(0, 0, 0xE9, 0x40, 0x40,
            EntityStatus.STUNNED, EntitySpriteDefinition.unsupported(0xE9), -1);
        motion.advance(stunned, 3, 0, 0, () -> 0, null, 0);
        assertEquals(5, motion.state(0));
    }

    @Test
    void statesSixAndSevenUseTheRomCornerAndSignedZLanding() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 6, 0, 0, 0, 0);

        ColorShellMotion.Update update = motion.advance(entityWithZ(0x60, 0x50, 0),
            0, 0, 0, () -> 0, null, 0);
        assertEquals(0x78, update.entity().x());
        assertEquals(0x60, update.entity().y());
        assertEquals(0, update.entity().z());
        assertEquals(7, motion.state(0));
        assertEquals(0x10, motion.speedZ(0));

        for (int frame = 1; frame < 80 && motion.state(0) == 7; frame++) {
            update = motion.advance(update.entity(), frame, 0, 0, () -> 0, null, 0);
        }
        assertEquals(8, motion.state(0));
        assertEquals(0, update.entity().z());
        assertEquals(0, motion.speedZ(0));
    }

    @Test
    void stateEightWritesTheSolvedMarkerAndUnlockNoiseForTheMatchingColor() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 8, 0, 0, 0, 0);
        RecordingWorld world = new RecordingWorld(0x5E);

        ColorShellMotion.Update update = motion.advance(entity(0x40, 0x40), 0,
            0, 0, () -> 0, null, 0, world, List.of(entity(0x40, 0x40)));

        assertEquals(0x0C, motion.state(0));
        assertEquals(List.of(0x67), world.objectWrites);
        assertEquals(List.of(0x04), world.noises);
        assertEquals(List.of(), world.jingles);
        assertEquals(0x40, update.entity().x());
    }

    @Test
    void stateEightLaunchesTheFailedAnswerArcAndWrongJingle() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 8, 0, 0, 0, 0);
        RecordingWorld world = new RecordingWorld(0x00);

        motion.advance(entity(0x40, 0x40), 0, 0, 0, () -> 0, null, 0,
            world, List.of(entity(0x40, 0x40)));

        assertEquals(9, motion.state(0));
        assertEquals(0x18, motion.transitionCountdown(0));
        assertEquals(0x10, motion.speedX(0));
        assertEquals(0x10, motion.speedZ(0));
        assertEquals(List.of(0x1D), world.jingles);
    }

    @Test
    void stateCWaitsForEveryOtherColorShellBeforeStartingCompletion() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 0x0C, 0, 0, 0, 0);
        motion.setStateForTest(1, 1, 0, 0, 0, 0);
        RoomEntity first = entity(0x40, 0x40);
        RoomEntity other = new RoomEntity(1, 1, 0xEA, 0x50, 0x40,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(0xEA), -1);
        RecordingWorld world = new RecordingWorld(0x00);

        motion.advance(first, 0, 0, 0, () -> 0, null, 0, world, List.of(first, other));
        assertEquals(0x0C, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));

        motion.setStateForTest(1, 0x0C, 0, 0, 0, 0);
        motion.advance(first, 1, 0, 0, () -> 0, null, 0, world, List.of(first, other));
        assertEquals(0x0D, motion.state(0));
        assertEquals(0x18, motion.transitionCountdown(0));
        assertEquals(List.of(0x67), world.objectWrites);
    }

    @Test
    void stateDWritesFinalObjectSpawnsPoofAndRequestsUnload() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 0x0D, 0, 0, 0, 0);
        RecordingWorld world = new RecordingWorld(0x00);

        ColorShellMotion.Update update = motion.advance(entityWithZ(0x40, 0x48, 3), 0,
            0, 0, () -> 0, null, 0, world, List.of(entityWithZ(0x40, 0x48, 3)));

        assertEquals(List.of(0x5F), world.objectWrites);
        assertEquals(1, world.poofs.size());
        assertArrayEquals(new int[] {0x40, 0x45}, world.poofs.get(0),
            "poof uses entity Y minus Z like hMultiPurpose1");
        assertEquals(1, update.unloadRequested() ? 1 : 0);
    }

    @Test
    void clearResetsEveryStateOneField() {
        ColorShellMotion motion = new ColorShellMotion();
        motion.setStateForTest(0, 3, 0x20, 2, 0x10, 0xF0);
        motion.clear(0);

        assertEquals(0, motion.state(0));
        assertEquals(0, motion.transitionCountdown(0));
        assertEquals(0, motion.direction(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0, motion.speedZ(0));
        assertEquals(0, motion.spriteVariant(0));
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, 0xE9, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xE9), -1);
    }

    private static RoomEntity entityWithZ(int x, int y, int z) {
        return new RoomEntity(0, 0, 0xE9, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xE9), -1, 0, 0, z);
    }

    private static int signed(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }

    private static final class RecordingWorld implements ColorShellWorld {
        private final int objectBeforeShell;
        private final List<Integer> objectWrites = new ArrayList<>();
        private final List<Integer> jingles = new ArrayList<>();
        private final List<Integer> noises = new ArrayList<>();
        private final List<int[]> poofs = new ArrayList<>();

        private RecordingWorld(int objectBeforeShell) {
            this.objectBeforeShell = objectBeforeShell;
        }

        @Override
        public int objectAt(RoomEntity entity, int relativeOffset) {
            return relativeOffset == -1 ? objectBeforeShell : 0;
        }

        @Override
        public void writeObject(RoomEntity entity, int objectId) {
            objectWrites.add(objectId);
        }

        @Override
        public void playJingle(int id) {
            jingles.add(id);
        }

        @Override
        public void playNoise(int id) {
            noises.add(id);
        }

        @Override
        public void spawnPoof(int x, int y) {
            poofs.add(new int[] {x, y});
        }
    }
}
