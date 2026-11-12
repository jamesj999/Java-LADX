package linksawakening.scene;
/** Immutable background scene spec values. */
public record BackgroundSceneSpec(
        int tilemapBank, int tilemapAddr, int attrmapBank, int attrmapAddr, int palettesBank, int palettesAddr) {}
