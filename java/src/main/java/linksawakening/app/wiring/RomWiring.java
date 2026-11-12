package linksawakening.app.wiring;

import java.io.InputStream;
import linksawakening.app.debug.DebugLog;
import linksawakening.app.state.SceneState;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;

/** Loads the ROM and initial title presentation. Stateless factory used during session initialization. */
final class RomWiring {
    private RomWiring() {}

    private static final int GAME_BOY_BG_MAP_TILES = 32 * 32;

    record Graphics(BackgroundSceneLoader backgrounds, SceneState scene, byte[] framebuffer) {}

    static Rom loadROM() {
        try (InputStream is = RomWiring.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (is == null) {
                throw new IllegalStateException("ROM not found in resources");
            }

            Rom romData = Rom.of(is.readAllBytes());
            DebugLog.log("ROM loaded: " + romData.size() + " bytes");
            return romData;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    static Graphics graphics(Rom romData, GPU gpu) {
        if (romData.size() < 0x8000) {
            throw new IllegalStateException("ROM data is not available");
        }

        BackgroundSceneLoader backgroundSceneLoader = new BackgroundSceneLoader(romData);
        gpu.loadTitleScreenTiles(romData);
        BackgroundScene scene = backgroundSceneLoader.load(BackgroundSceneCatalog.TITLE);
        int[] currentTilemap = scene.tilemap();
        int[] currentAttrmap = scene.attrmap();
        int[][] bgPalettes = scene.palettes();
        int[][] objPalettes = scene.objectPalettes();
        byte[] indexedDisplayBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        if (currentTilemap.length != GAME_BOY_BG_MAP_TILES) {
            throw new IllegalStateException("Decoded title tilemap has unexpected size: " + currentTilemap.length);
        }
        if (currentAttrmap.length != GAME_BOY_BG_MAP_TILES) {
            throw new IllegalStateException("Decoded title attrmap has unexpected size: " + currentAttrmap.length);
        }

        SceneState initial = new SceneState();
        initial.currentTilemap = currentTilemap;
        initial.currentAttrmap = currentAttrmap;
        initial.bgPalettes = bgPalettes;
        initial.objPalettes = objPalettes;
        return new Graphics(backgroundSceneLoader, initial, indexedDisplayBuffer);
    }
}
