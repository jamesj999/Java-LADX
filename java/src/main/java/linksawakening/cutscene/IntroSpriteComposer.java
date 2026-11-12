package linksawakening.cutscene;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.RomRandomByteSource;

/** Intro OAM composition owns immutable sprite tables, scratch storage and the source rain RNG. */
final class IntroSpriteComposer {

    private final List<IntroSprite> renderSpriteScratch = new ArrayList<>();
    private final List<IntroSprite> correctionSpriteScratch = new ArrayList<>();

    private final IntroRomData rom;
    private final RomRandomByteSource random = new RomRandomByteSource(0xA2);
    private final List<IntroRomData.SpritePair> marinVariants;
    private final List<IntroRomData.SpritePair> inertLinkVariants;
    private final List<IntroRomData.SpritePair> sparkleVariants;
    private final List<List<IntroRomData.OamEntry>> lightningTiles;
    private final int[] shipHeave;
    private final List<IntroRomData.OamEntry> shipTiles;
    private final List<IntroRomData.OamEntry> additionalShipTiles;

    record Assets(
            List<IntroRomData.SpritePair> marinVariants,
            List<IntroRomData.SpritePair> inertLinkVariants,
            List<IntroRomData.SpritePair> sparkleVariants,
            List<List<IntroRomData.OamEntry>> lightningTiles,
            int[] shipHeave,
            List<IntroRomData.OamEntry> shipTiles,
            List<IntroRomData.OamEntry> additionalShipTiles) {}

    IntroSpriteComposer(IntroRomData rom, Assets assets) {
        this.rom = rom;
        this.marinVariants = assets.marinVariants();
        this.inertLinkVariants = assets.inertLinkVariants();
        this.sparkleVariants = assets.sparkleVariants();
        this.lightningTiles = assets.lightningTiles();
        this.shipHeave = assets.shipHeave();
        this.shipTiles = assets.shipTiles();
        this.additionalShipTiles = assets.additionalShipTiles();
    }

    void renderRain(List<IntroSprite> rendered, int rows) {
        int y = (random.getAsInt() & 0x18) + 0x10;
        int x = (random.getAsInt() & 0x18) + 0x10;
        for (int row = 0; row < rows; row++) {
            int tile = 0x28;
            if ((random.getAsInt() & 0x01) != 0) {
                tile = (random.getAsInt() & 0x06) + 0x70;
            }
            rendered.add(new IntroSprite(tile, x - 8, y - 16, 0, false, false, 16));
            x += 0x1C;
            if (x >= 0xA0) {
                x -= 0x98;
                y += 0x25;
            }
        }
    }

    void renderShip(List<IntroSprite> rendered, boolean fading, int clock, int shipX, int shipY, int seaFadeTimer) {
        int heaveIndex = fading ? 0 : (clock + 1 + 0xD0) >>> 4 & 0x07;
        int heave = shipHeave[heaveIndex];
        for (IntroRomData.OamEntry entry : shipTiles) {
            rendered.add(toSprite(entry, shipX, shipY + heave));
        }
        if (fading && seaFadeTimer >= 0x10) {
            for (IntroRomData.OamEntry entry : additionalShipTiles) {
                rendered.add(toSprite(entry, shipX, shipY + heave));
            }
        }
    }

    void renderLightning(List<IntroSprite> rendered, List<IntroSequence.Lightning> lightning) {
        for (IntroSequence.Lightning current : lightning) {
            if (current.variant() >= lightningTiles.size()) {
                continue;
            }
            for (IntroRomData.OamEntry entry : lightningTiles.get(current.variant())) {
                rendered.add(toSprite(entry, current.x(), current.y()));
            }
        }
    }

    void renderBeachEntities(
            List<IntroSprite> rendered,
            boolean linkVisible,
            int linkX,
            int linkY,
            int linkVariant,
            int marinVariant,
            int marinX,
            int marinY) {
        if (linkVisible && linkX < 0xF0) addPair(rendered, inertLinkVariants.get(linkVariant), linkX, linkY);
        addPair(rendered, marinVariants.get(marinVariant), marinX, marinY);
    }

    void renderTitleSparkle(List<IntroSprite> rendered, int clock, int titleHoldFrame) {
        // TitleScreenHandler allocates a sparkle every 64 frames. Its countdown
        // runs backwards through the ROM variants, not forwards from frame zero.
        int age = clock & 63;
        if (clock >= 64 && age > 0 && age < 63) {
            int position = ((clock >>> 6) - 1) & 7;
            addPair(
                    rendered,
                    sparkleVariants.get((63 - age) >>> 3),
                    rom.titleSparkleXPositions()[position],
                    rom.titleSparkleYPositions()[position]);
        }
        if (titleHoldFrame >= 60) {
            for (IntroRomData.OamEntry entry : rom.dxTiles(titleHoldFrame >= 124))
                rendered.add(toSprite(entry, 0x78, 0x59));
        }
    }

    void addPair(List<IntroSprite> rendered, IntroRomData.SpritePair pair, int oamX, int oamY) {
        rendered.add(new IntroSprite(
                pair.firstTileIndex(),
                oamX - 8,
                oamY - 16,
                pair.firstAttributes() & 0x07,
                (pair.firstAttributes() & 0x20) != 0,
                (pair.firstAttributes() & 0x40) != 0,
                16));
        rendered.add(new IntroSprite(
                pair.secondTileIndex(),
                oamX,
                oamY - 16,
                pair.secondAttributes() & 0x07,
                (pair.secondAttributes() & 0x20) != 0,
                (pair.secondAttributes() & 0x40) != 0,
                16));
    }

    IntroSprite toSprite(IntroRomData.OamEntry entry, int oamX, int oamY) {
        return new IntroSprite(
                entry.tileIndex(),
                oamX + entry.xOffset() - 8,
                oamY + entry.yOffset() - 16,
                entry.attributes() & 0x07,
                (entry.attributes() & 0x20) != 0,
                (entry.attributes() & 0x40) != 0,
                16);
    }

    List<IntroSprite> begin() {
        renderSpriteScratch.clear();
        return renderSpriteScratch;
    }

    List<IntroSprite> correction() {
        return correctionSpriteScratch;
    }

    IntroRomData.SpritePair marinPair(int variant) {
        return marinVariants.get(variant);
    }

    void beginFrame(int clock) {
        random.beginFrame(clock, 0);
    }
}
