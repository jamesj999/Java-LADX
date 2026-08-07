package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

final class BombTest {

    @Test
    void constructorRejectsNullPlacementDependencies() {
        PlayerState player = playerWithBombs(1);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();

        assertThrows(NullPointerException.class, () -> new Bomb(null, sounds, target));
        assertThrows(NullPointerException.class, () -> new Bomb(player, null, target));
        assertThrows(NullPointerException.class, () -> new Bomb(player, sounds, null));
    }

    @Test
    void registeredBombUsesOnlyTheNewButtonEdge() {
        InputState input = new InputState();
        InputConfig inputConfig = inputConfig();
        PlayerState player = playerWithBombs(2);
        player.setItemA(PlayerState.INVENTORY_BOMBS);
        RecordingTarget target = new RecordingTarget();
        Bomb bomb = new Bomb(player, GameplaySoundSink.none(), target);
        ItemRegistry registry = new ItemRegistry();
        registry.register(PlayerState.INVENTORY_BOMBS, bomb);
        EquipmentController controller = new EquipmentController(input, inputConfig, player, registry);

        input.onKeyEvent(GLFW_KEY_A, GLFW_PRESS);
        controller.dispatchButtonEdges();
        input.tickEdges();
        controller.dispatchButtonEdges();

        assertEquals(1, target.placementRequests);
        assertEquals(1, player.bombCount());
    }

    @Test
    void zeroBombsPlayWrongAnswerWithoutCallingPlacementOrChangingCount() {
        PlayerState player = playerWithBombs(0);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        Bomb bomb = new Bomb(player, sounds, target);

        bomb.onPress();

        assertEquals(0, player.bombCount());
        assertEquals(0, target.placementRequests);
        assertEquals(List.of(GameplaySoundEvent.WRONG_ANSWER), sounds.events);
    }

    @Test
    void blockedItemUseDoesNotSpendInventoryPlayWrongAnswerOrAllocate() {
        PlayerState player = playerWithBombs(2);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        Bomb bomb = new Bomb(player, sounds, target, () -> false);

        bomb.onPress();

        assertEquals(2, player.bombCount());
        assertEquals(0, target.placementRequests);
        assertEquals(List.of(), sounds.events);
    }

    @Test
    void activeBombRejectsPlacementBeforeInventoryCheck() {
        PlayerState player = playerWithBombs(0);
        RecordingTarget target = new RecordingTarget();
        target.active = true;
        RecordingSoundSink sounds = new RecordingSoundSink();
        Bomb bomb = new Bomb(player, sounds, target);

        bomb.onPress();

        assertEquals(0, player.bombCount());
        assertEquals(0, target.placementRequests);
        assertEquals(List.of(), sounds.events);
    }

    @Test
    void successfulPlacementDecrementsInventoryOnceBeforeCallingTarget() {
        PlayerState player = playerWithBombs(2);
        RecordingTarget target = new RecordingTarget(player);
        Bomb bomb = new Bomb(player, GameplaySoundSink.none(), target);

        bomb.onPress();

        assertEquals(1, player.bombCount());
        assertEquals(1, target.placementRequests);
        assertEquals(1, target.countObservedAtPlacement);
    }

    @Test
    void convertedTopViewPlacementPlaysTheSourceBumpJingle() {
        PlayerState player = playerWithBombs(1);
        RecordingTarget target = new RecordingTarget(player);
        target.playBump = true;
        RecordingSoundSink sounds = new RecordingSoundSink();
        Bomb bomb = new Bomb(player, sounds, target);

        bomb.onPress();

        assertEquals(List.of(GameplaySoundEvent.ENEMY_BUMP), sounds.events);
    }

    @Test
    void failedPlacementStillConsumesInventoryAfterTheItemUseGate() {
        PlayerState player = playerWithBombs(2);
        RecordingTarget target = new RecordingTarget(player);
        target.placementSucceeds = false;
        RecordingSoundSink sounds = new RecordingSoundSink();
        Bomb bomb = new Bomb(player, sounds, target);

        bomb.onPress();

        assertEquals(1, player.bombCount());
        assertEquals(1, target.placementRequests);
        assertEquals(1, target.countObservedAtPlacement);
        assertEquals(List.of(), sounds.events);
    }

    private static PlayerState playerWithBombs(int count) {
        PlayerState player = new PlayerState();
        player.setMaxBombs(9);
        player.setBombCount(count);
        return player;
    }

    private static InputConfig inputConfig() {
        return new InputConfig(GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN,
            GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
    }

    private static final class RecordingTarget implements Bomb.PlacementTarget {
        private final PlayerState player;
        private int placementRequests;
        private int countObservedAtPlacement = -1;
        private boolean active;
        private boolean placementSucceeds = true;
        private boolean playBump;

        private RecordingTarget() {
            this(null);
        }

        private RecordingTarget(PlayerState player) {
            this.player = player;
        }

        @Override
        public boolean placeBomb() {
            placementRequests++;
            countObservedAtPlacement = player == null ? -1 : player.bombCount();
            return placementSucceeds;
        }

        @Override
        public boolean bombActive() {
            return active;
        }

        @Override
        public boolean playBumpForLastPlacement() {
            return playBump;
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
