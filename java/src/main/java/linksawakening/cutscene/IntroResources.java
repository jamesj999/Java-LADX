package linksawakening.cutscene;

import java.io.IOException;
import java.io.InputStream;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;

/** Bundled-ROM compatibility entry point for intro scene resources; runtime injection remains explicit. */
final class IntroResources {
    private IntroResources() {}

    static IntroSequence.DefaultResources defaultResources() {
        Rom romData = loadBundledRom();
        BackgroundSceneLoader loader = new BackgroundSceneLoader(romData);
        return new IntroSequence.DefaultResources(romData, sceneId -> {
            BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
            if (spec == null) {
                throw new IllegalArgumentException("No cutscene background spec for " + sceneId);
            }
            return loader.load(spec);
        });
    }

    static Rom loadBundledRom() {
        try (InputStream stream = IntroSequence.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return Rom.of(stream.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load ROM", exception);
        }
    }
}
