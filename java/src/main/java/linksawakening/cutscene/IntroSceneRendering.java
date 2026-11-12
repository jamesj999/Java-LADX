package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.function.Function;
import linksawakening.scene.BackgroundScene;

/**
 * Domain-specific runtime for the shipped opening sequence.
 *
 * <p>This follows the intro handlers' state and timer boundaries without
 * pretending to execute the Game Boy CPU. ROM tables provide all entity
 * records and animation data; this class only models the state that affects
 * the frame renderer.</p>
 */

/** IntroSceneRendering behavior on the shared IntroSequence state. */
abstract class IntroSceneRendering extends IntroSceneTransitions {
    protected IntroSceneRendering() {
        super();
    }

    protected IntroSceneRendering(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {
        super(romData, backgroundProvider);
    }

    protected IntroSceneRendering(DefaultResources resources) {
        super(resources);
    }

    protected void applyTitleRow(IntroRomData.TitleRow row) {
        copyDrawRow(tilemap, row.tileTargetAddress(), row.tileBytes());
        copyDrawRow(attrmap, row.attributeTargetAddress(), row.attributeBytes());
    }
}
