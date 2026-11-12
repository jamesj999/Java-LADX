package linksawakening.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class MagicPowderTest {

    @Test
    void successfulUseSpawnsFirstThenSpendsPowderStartsTheRomAttackStepAndPlaysJingle() {
        PlayerState player = playerWithPowder(2);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        MagicPowder powder = new MagicPowder(player, sounds, target);

        powder.onPress();

        assertEquals(1, target.sprinkleRequests);
        assertEquals(1, target.attackStepRequests);
        assertEquals(1, player.ammunition().magicPowderCount());
        assertEquals(List.of(GameplaySoundEvent.MAGIC_POWDER), sounds.events);
        assertTrue(powder.blocksMotion());
        assertTrue(powder.locksFacing());
    }

    @Test
    void emptyPowderUsesTheWrongAnswerJingleWithoutSpawning() {
        PlayerState player = playerWithPowder(0);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        MagicPowder powder = new MagicPowder(player, sounds, target);

        powder.onPress();

        assertEquals(0, target.sprinkleRequests);
        assertEquals(0, target.attackStepRequests);
        assertEquals(List.of(GameplaySoundEvent.WRONG_ANSWER), sounds.events);
    }

    @Test
    void toadstoolUseBypassesEmptyPowderErrorAndSprinkling() {
        for (int count : new int[] {0, 20}) {
            PlayerState player = playerWithPowder(count);
            player.ammunition().setHasToadstool(true);
            RecordingTarget target = new RecordingTarget();
            RecordingSoundSink sounds = new RecordingSoundSink();
            new MagicPowder(player, sounds, target).onPress();
            assertEquals(List.of(), sounds.events);
            assertEquals(0, target.sprinkleRequests);
            assertEquals(0, target.attackStepRequests);
            assertEquals(1, target.toadstoolPresentations);
            assertEquals(count, player.ammunition().magicPowderCount());
            assertTrue(player.ammunition().hasToadstool()); // the Witch handler owns the trade
        }
    }

    @Test
    void toadstoolUseAboveGroundReturnsBeforePresentationOrSound() {
        PlayerState player = playerWithPowder(0);
        player.ammunition().setHasToadstool(true);
        RecordingTarget target = new RecordingTarget();
        target.z = 1;
        RecordingSoundSink sounds = new RecordingSoundSink();
        new MagicPowder(player, sounds, target).onPress();
        assertEquals(0, target.toadstoolPresentations);
        assertEquals(List.of(), sounds.events);
        assertEquals(0, target.sprinkleRequests);
    }

    @Test
    void failedEntityAllocationDoesNotSpendPowderOrStartTheAttackStep() {
        PlayerState player = playerWithPowder(1);
        RecordingTarget target = new RecordingTarget();
        target.sprinkleResult = false;
        RecordingSoundSink sounds = new RecordingSoundSink();
        MagicPowder powder = new MagicPowder(player, sounds, target);

        powder.onPress();

        assertEquals(1, player.ammunition().magicPowderCount());
        assertEquals(0, target.attackStepRequests);
        assertEquals(List.of(), sounds.events);
    }

    @Test
    void consumingTheLastPowderClearsBothButtonSlotsThatStillHoldPowder() {
        PlayerState player = playerWithPowder(1);
        player.inventory().setItemB(PlayerState.INVENTORY_MAGIC_POWDER);
        RecordingTarget target = new RecordingTarget();

        new MagicPowder(player, GameplaySoundSink.none(), target).onPress();

        assertEquals(0, player.ammunition().magicPowderCount());
        assertEquals(PlayerState.INVENTORY_EMPTY, player.inventory().itemA());
        assertEquals(PlayerState.INVENTORY_EMPTY, player.inventory().itemB());
    }

    @Test
    void itemGateAndExistingAttackWindowAreCheckedBeforePowderCount() {
        PlayerState player = playerWithPowder(1);
        RecordingTarget target = new RecordingTarget();
        MagicPowder blocked = new MagicPowder(player, GameplaySoundSink.none(), target, () -> false);

        blocked.onPress();
        assertEquals(0, target.sprinkleRequests);
        assertEquals(1, player.ammunition().magicPowderCount());

        target.attackStepActive = true;
        MagicPowder alreadyAttacking = new MagicPowder(player, GameplaySoundSink.none(), target);
        alreadyAttacking.onPress();
        assertEquals(0, target.sprinkleRequests);
    }

    @Test
    void constructorRejectsNullDependencies() {
        PlayerState player = playerWithPowder(1);
        RecordingTarget target = new RecordingTarget();
        assertThrows(NullPointerException.class, () -> new MagicPowder(null, GameplaySoundSink.none(), target));
        assertThrows(NullPointerException.class, () -> new MagicPowder(player, null, target));
        assertThrows(NullPointerException.class, () -> new MagicPowder(player, GameplaySoundSink.none(), null));
    }

    private static PlayerState playerWithPowder(int count) {
        PlayerState player = new PlayerState();
        player.ammunition().setMaxMagicPowder(20);
        player.ammunition().setMagicPowderCount(count);
        player.inventory().setItemA(PlayerState.INVENTORY_MAGIC_POWDER);
        return player;
    }

    private static final class RecordingTarget implements MagicPowder.SprinkleTarget {
        private int z;
        private int toadstoolPresentations;

        @Override
        public int linkPositionZ() {
            return z;
        }

        @Override
        public void presentToadstool() {
            toadstoolPresentations++;
        }

        private int sprinkleRequests;
        private int attackStepRequests;
        private boolean sprinkleResult = true;
        private boolean attackStepActive;

        @Override
        public boolean sprinkleMagicPowder() {
            sprinkleRequests++;
            return sprinkleResult;
        }

        @Override
        public void startMagicPowderAttackStep() {
            attackStepRequests++;
            attackStepActive = true;
        }

        @Override
        public boolean magicPowderAttackStepActive() {
            return attackStepActive;
        }
    }

    private static final class RecordingSoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
