package linksawakening.app.services;

import java.util.Objects;
import linksawakening.config.AppConfig;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.scene.BackgroundSceneLoader;

/**
 * Long-lived rom services constructed before the first frame. The immutable bundle is retained for the
 * owning session.
 */
public record RomServices(
        Rom romData, RomTables romTables, AppConfig appConfig, GPU gpu, BackgroundSceneLoader backgroundSceneLoader) {
    public RomServices {
        Objects.requireNonNull(romData);
        Objects.requireNonNull(romTables);
        Objects.requireNonNull(appConfig);
        Objects.requireNonNull(gpu);
        Objects.requireNonNull(backgroundSceneLoader);
    }
}
