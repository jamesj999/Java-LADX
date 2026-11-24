package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameplayFrameOrderTest {
    @Test
    void transitionsSimulationAndInputCompletionRunInThatOrder() {
        var calls = new ArrayList<String>();
        new GameplayFrame(() -> calls.add("transitions"), () -> calls.add("simulation"), () -> calls.add("input"))
                .tick();
        assertEquals(List.of("transitions", "simulation", "input"), calls);
    }

    @Test
    void earlySimulationReturnStillCompletesInput() {
        var calls = new ArrayList<String>();
        new GameplayFrame(() -> calls.add("transitions"), () -> {}, () -> calls.add("input")).tick();
        assertEquals(List.of("transitions", "input"), calls);
    }

    @Test
    void transferOnlyFrameCompletesInputWithoutSimulatingGameplay() {
        var calls = new ArrayList<String>();
        var transitions = new GameplayFrame.Transitions() {
            @Override
            public void run() {
                calls.add("transitions");
            }

            @Override
            public boolean tileTransferOnlyFrame() {
                return true;
            }
        };
        new GameplayFrame(transitions, () -> calls.add("simulation"), () -> calls.add("input")).tick();
        assertEquals(List.of("transitions", "input"), calls);
    }

    @Test
    void simulationFailureDoesNotAdvanceInput() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("simulation");
        var frame = new GameplayFrame(
                () -> calls.add("transitions"),
                () -> {
                    throw failure;
                },
                () -> calls.add("input"));
        assertSame(failure, assertThrows(IllegalStateException.class, frame::tick));
        assertEquals(List.of("transitions"), calls);
    }
}
