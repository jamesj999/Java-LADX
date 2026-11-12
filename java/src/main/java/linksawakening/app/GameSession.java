package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.story.OpeningStory;
import linksawakening.config.AppConfig;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.startup.StartupCoordinator;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.Warp;

public class GameSession implements AutoCloseable {
    private final FrameClock clock = new FrameClock();
    private final OpeningStoryState opening = new OpeningStoryState();
    private final GameplaySounds sounds = new GameplaySounds();

    public FrameClock frameClock() {
        return clock;
    }

    public OpeningStoryState openingState() {
        return opening;
    }

    public GameplaySounds gameplaySounds() {
        return sounds;
    }

    @Override
    public void close() {
        cleanup();
    }

    PcmSoundOutput headlessPcm;

    final RomRandomByteSource random;

    final long frameDurationNs = 1_000_000_000L / 60L;

    SessionContext context;

    public SessionState state() {
        return context.state();
    }

    public SessionServices services() {
        return context.services();
    }

    public SessionFlows flows() {
        return context.runtime().flows();
    }

    public SaveState saveState() {
        return context.runtime().saves();
    }

    public RunState lifecycle() {
        return context.lifecycle();
    }

    public byte[] headlessFrame() {
        update();
        context.runtime().presentation().composeFrameBuffer();
        return context.services().visual().indexedDisplayBuffer().clone();
    }

    void stepWithoutRender() {
        update();
    }

    public void run(String[] args) {
        initialize(false);
        if (StartupCoordinator.shouldStartIntroCutscene(
                context.state().options().config())) {
            context.runtime().flows().router().startIntroCutscene();
        } else if (context.state().options().config().startMode() == AppConfig.StartMode.NORMAL
                || !context.state().options().config().showTitleScreen()) {
            context.runtime().flows().router().startConfiguredGameplay();
        } else {
            context.runtime().flows().router().startTitleScreenWithoutIntro();
        }

        context.host().open(context.runtime().input()::onKeyEvent);

        long nextFrameNs = System.nanoTime();

        while (!context.host().shouldClose() && context.lifecycle().running()) {
            context.host().pollEvents();
            update();
            context.runtime().presentation().composeFrameBuffer();
            context.host().present(context.services().visual().indexedDisplayBuffer());

            nextFrameNs += frameDurationNs;
            long sleepNs = nextFrameNs - System.nanoTime();
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // We're behind — drop the backlog so we don't spiral.
                nextFrameNs = System.nanoTime();
            }
        }

        cleanup();
    }

    public HeadlessGameDriver.Diagnostics diagnostics() {
        return new HeadlessGameDriver.Diagnostics(context);
    }

    public void headlessButton(String button, boolean pressed) {
        context.runtime().flows().inputRouter().headlessButton(button, pressed);
    }

    public GameSession() {
        this(new RomRandomByteSource());
    }

    public GameSession(RomRandomByteSource random) {
        this.random = Objects.requireNonNull(random);
    }

    public GameSession(PcmSoundOutput output) {
        this();
        headlessPcm = Objects.requireNonNull(output);
    }

    public void initializeHeadlessIntro() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.runtime().flows().router().startIntroCutscene();
    }

    void update() {
        if (context.state().debug().debugMenu != null
                && context.state().debug().debugMenu.isOpen()) return;
        clock.advance();
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(clock.value(), 0);

        context.runtime().flows().music().tick();

        context.state().screens().currentScreen.tick();
    }

    void cleanup() {
        context.runtime().shutdown().close();
    }

    public void initializeHeadlessNewGame() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.services().ui().dialogController().setPlayerName("A");
        context.runtime().flows().saves().startNewGame();
    }

    private void initialize(boolean headless) {
        context = SessionAssembly.build(this, headless);
    }

    void onKeyEvent(int key, int action) {
        context.runtime().input().onKeyEvent(key, action);
    }

    public void debugTeleport(Warp destination) {
        context.runtime().flows().teleporter().teleport(destination);
    }

    SaveCoordinator saves() {
        return context.runtime().flows().saves();
    }

    ScreenRouter router() {
        return context.runtime().flows().router();
    }

    RenderStateAssembler renderer() {
        return context.runtime().flows().renderer();
    }

    InputRouter inputRouter() {
        return context.runtime().flows().inputRouter();
    }

    OpeningStory openingStory() {
        return context.runtime().flows().interactions().story();
    }

    GameplaySimulation simulation() {
        return context.runtime().flows().simulation();
    }

    BushInteraction bushes() {
        return context.runtime().flows().interactions().bush();
    }

    DialogInteractions dialogs() {
        return context.runtime().flows().interactions().dialogs();
    }

    LiftInteraction lifting() {
        return context.runtime().flows().interactions().lift();
    }
}
