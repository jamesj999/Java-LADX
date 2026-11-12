package linksawakening.cutscene;

import java.util.function.Function;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/**
 * Runtime for the ROM {@code IntroHandler} opening sequence.
 *
 * <p>This follows the intro handlers' state and timer boundaries without
 * pretending to execute the Game Boy CPU. ROM tables provide all entity
 * records and animation data; this class only models the state that affects
 * the frame renderer.</p>
 */
public final class IntroSequence extends IntroSceneRendering {

    public static final int SEA_SCROLL_FRAMES = (0xC0 - 0x50) * 8 + 1;
    public static final int LINK_FACE_FRAMES = 0xA0;
    public static final int SEA_FADE_FRAMES = 0x58 - 1;
    public static final int BEACH_FADE_FRAMES = 0xFF;
    public static final int BEACH_MARIN_WALK_FRAMES = (0xB0 - 0x47) * 4;
    public static final int BEACH_MARIN_WAIT_FRAMES = 0x40;
    public static final int TITLE_REVEAL_STEP_FRAMES = 1;
    public static final int TITLE_HOLD_FRAMES = 160;

    public IntroSequence() {
        super();
    }

    public IntroSequence(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
        super(romData, backgroundProvider);
    }

    public IntroSequence(DefaultResources resources) {
        super(resources);
    }

    /** @deprecated Use the Rom overload; retains legacy partial-buffer validation. */
    @Deprecated(forRemoval = true)
    public IntroSequence(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {
        this(romData == null ? null : new Rom(romData), backgroundProvider);
    }
}
