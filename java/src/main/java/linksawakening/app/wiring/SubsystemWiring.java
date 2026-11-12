package linksawakening.app.wiring;

import java.io.IOException;
import linksawakening.app.flow.SessionNavigator;
import linksawakening.app.host.Host;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.RomServices;
import linksawakening.app.services.SessionServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SaveState;
import linksawakening.app.state.SceneState;
import linksawakening.config.AppConfig;
import linksawakening.gpu.GPU;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Builds ROM, audio, player, world and UI bundles in dependency order. Called once before application
 * controllers are connected. Stateless factory used during session initialization.
 */
final class SubsystemWiring {
    private SubsystemWiring() {}

    /** Initial services, scene state, and save storage produced by subsystem wiring. */
    public record Built(SessionServices services, SceneState scene, SaveState saves) {}

    public static Built build(
            RomRandomByteSource random,
            GPU gpu,
            TransitionController transition,
            ScrollController scroll,
            SessionNavigator navigator,
            Host host,
            boolean headless) {
        Rom rom = RomWiring.loadROM();
        // Legacy world APIs still require the ROM backing array; keep this escape inside wiring.
        transition.setRomData(rom.unsafeBytes());
        AppConfig config = headless
                ? AppConfig.parse("{\"itemProfile\":\"NEW_GAME\",\"debugEnabled\":false}")
                : AppConfig.loadFromResources();
        var graphics = RomWiring.graphics(rom, gpu);
        SaveState saves = new SaveState(openSaves(headless));
        var input = new InputServices(config.inputConfig(), new InputState());
        var playerState = new PlayerState();
        var audio = AudioWiring.build(rom, host);
        var ui = UiWiring.build(rom, playerState, input, audio, navigator);
        var tables = RomTables.load(rom);
        var romServices = new RomServices(rom, tables, config, gpu, graphics.backgrounds());
        var collision = new OverworldCollision(tables);
        var bushes = new OverworldBushInteraction(rom.unsafeBytes(), tables);
        var playerGraphics = PlayerWiring.graphics(rom);
        var visual = WorldWiring.visuals(rom, random, graphics.framebuffer());
        var rooms = WorldWiring.rooms(romServices, visual, collision, random, navigator, audio.gameplaySoundSink());
        var player = PlayerWiring.build(
                playerState,
                input,
                romServices,
                new PlayerWiring.PlayerWorld(rooms, collision),
                ui,
                audio,
                playerGraphics,
                random);
        var transitions = WorldWiring.transitions(rooms, playerState, transition, scroll, audio.gameplaySoundSink());
        var world = new WorldServices(rooms, transitions, collision, bushes, scroll, transition);
        return new Built(
                new SessionServices(romServices, audio, input, world, player, ui, visual), graphics.scene(), saves);
    }

    private static SaveRamStore openSaves(boolean headless) {
        try {
            return headless ? SaveRamStore.inMemory() : SaveRamStore.open(SaveRamStore.defaultPath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open save image", e);
        }
    }
}
