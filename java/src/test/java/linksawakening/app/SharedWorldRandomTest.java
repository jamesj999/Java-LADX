package linksawakening.app;

import linksawakening.testing.RequiresRom;
import linksawakening.world.entity.RomRandomByteSource;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
final class SharedWorldRandomTest {
    @Test
    void equipmentAndRoomEntitiesConsumeTheSameRomSeedInCallOrder() {
        var random = new RomRandomByteSource(0x22);
        var draws = new ArrayList<RomRandomByteSource.Draw>();
        random.setObserver(draws::add);
        var game = new GameSession(random);
        game.initializeHeadlessNewGame();
        var rooms = game.diagnostics().roomSession();
        rooms.loadInitialOverworld(0xF1);
        draws.clear();
        random.beginFrame(10, 0);
        random.getAsInt(); // A world/equipment draw before AnimateEntities.
        rooms.tickEntities(10, 0x20, 0x60);
        rooms.tickEntities(11, 0x20, 0x60);
        assertTrue(draws.size() > 1, "The live room must consume the session's observed RNG");
        for (int i = 1; i < draws.size(); i++) {
            assertEquals(draws.get(i - 1).value(), draws.get(i).previousSeed(),
                "GetRandomByte $00:280D uses one wRandomSeed across callers");
        }
    }
}
