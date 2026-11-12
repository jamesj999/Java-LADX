package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.function.Function;
import linksawakening.scene.BackgroundScene;

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
