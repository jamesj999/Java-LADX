package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.function.Function;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/** Intro sequencing, presentation or ROM data used by ROM {@code IntroHandler}. */
abstract class IntroSceneRendering extends IntroSceneTransitions {
    protected IntroSceneRendering() {
        super();
    }

    protected IntroSceneRendering(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
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
