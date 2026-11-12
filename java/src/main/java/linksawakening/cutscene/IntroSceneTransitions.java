package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/** IntroSceneTransitions behavior on the shared IntroSequence state. */
abstract class IntroSceneTransitions extends IntroBeachMotion {

    protected IntroSceneTransitions() {
        super();
    }

    protected IntroSceneTransitions(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
        super(romData, backgroundProvider);
    }

    protected IntroSceneTransitions(DefaultResources resources) {
        super(resources);
    }

    protected List<IntroSprite> renderSprites() {
        List<IntroSprite> rendered = renderSpriteScratch;
        rendered.clear();
        if (stage == Stage.SEA || stage == Stage.SEA_FADE) {
            renderShip(rendered);
            renderLightning(rendered);
            renderRain(rendered, 0x10);
        } else if (stage == Stage.LINK_FACE) {
            renderRain(rendered, 0x15);
            renderLightning(rendered);
        } else if (stage == Stage.BEACH) {
            renderBeachEntities(rendered);
        } else if (stage == Stage.TITLE_DX || (stage == Stage.COMPLETE && !skipped)) {
            renderTitleSparkle(rendered);
        }
        return List.copyOf(rendered);
    }

    protected void renderRain(List<IntroSprite> rendered, int rows) {
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

    protected void renderShip(List<IntroSprite> rendered) {
        int heaveIndex = stage == Stage.SEA_FADE ? 0 : (clock + 1 + 0xD0) >>> 4 & 0x07;
        int heave = shipHeave[heaveIndex];
        for (IntroRomData.OamEntry entry : shipTiles) {
            rendered.add(toSprite(entry, shipX, shipY + heave));
        }
        if (stage == Stage.SEA_FADE && seaFadeTimer >= 0x10) {
            for (IntroRomData.OamEntry entry : additionalShipTiles) {
                rendered.add(toSprite(entry, shipX, shipY + heave));
            }
        }
    }

    protected void renderLightning(List<IntroSprite> rendered) {
        for (Lightning current : lightning) {
            if (current.variant() >= lightningTiles.size()) {
                continue;
            }
            for (IntroRomData.OamEntry entry : lightningTiles.get(current.variant())) {
                rendered.add(toSprite(entry, current.x(), current.y()));
            }
        }
    }

    protected void renderBeachEntities(List<IntroSprite> rendered) {
        if (linkVisible && linkX < 0xF0) addPair(rendered, inertLinkVariants.get(linkVariant), linkX, linkY);
        addPair(rendered, marinVariants.get(marinVariant), marinX, marinY);
    }

    protected void renderTitleSparkle(List<IntroSprite> rendered) {
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

    protected void addPair(List<IntroSprite> rendered, IntroRomData.SpritePair pair, int oamX, int oamY) {
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

    protected IntroSprite toSprite(IntroRomData.OamEntry entry, int oamX, int oamY) {
        return new IntroSprite(
                entry.tileIndex(),
                oamX + entry.xOffset() - 8,
                oamY + entry.yOffset() - 16,
                entry.attributes() & 0x07,
                (entry.attributes() & 0x20) != 0,
                (entry.attributes() & 0x40) != 0,
                16);
    }

    protected int[] lineScrollMap() {
        if (beachScrollStoppedThisFrame) {
            int[] lines = new int[144];
            Arrays.fill(lines, beachScrollX);
            // The CGB handler's direct SCX write (01:774E) reaches the LCD on
            // scanline 9; the preceding scanlines retain the first section's SCX.
            Arrays.fill(lines, 0, 9, topLineScroll);
            return lines;
        }
        boolean sea = stage == Stage.SEA || stage == Stage.SEA_FADE;
        boolean beach = stage == Stage.BEACH_FADE || (stage == Stage.BEACH && marinState < 4);
        if (stage == Stage.TITLE_REVEAL || stage == Stage.TITLE_WAIT) {
            int[] lines = new int[144];
            Arrays.fill(lines, scrollX());
            return lines;
        }
        if (!sea && !beach) return null;
        int[] lines = new int[144];
        for (int y = 0; y < 144; y++) {
            int section = sea
                    ? (y < 32 ? 0 : y < 48 ? 1 : y < 64 ? 2 : y < 96 - verticalWaveOffset ? 3 : 4)
                    : (y < 48 ? 0 : y < 86 ? 1 : y < 104 ? 2 : 3);
            lines[y] = beach && y < 48 ? topLineScroll : (scrollX() + sectionScrollOffsets[section]) & 255;
        }
        return lines;
    }

    protected int[] lineScrollYMap() {
        if (stage == Stage.TITLE_REVEAL || stage == Stage.TITLE_WAIT)
            return rom.titleLineScroll(titleEffectFrame == 0 ? 16 : titleEffectFrame / 2, visibleScrollY);
        if (stage != Stage.SEA && stage != Stage.SEA_FADE) return null;
        int[] lines = new int[144];
        for (int y = 64; y < 144; y++) lines[y] = verticalWaveOffset;
        return lines;
    }

    protected void setScene(String sceneId) {
        BackgroundScene scene = backgroundProvider.apply(sceneId);
        if (scene == null) {
            throw new IllegalArgumentException("No background scene for " + sceneId);
        }
        tilemap = scene.tilemap().clone();
        attrmap = scene.attrmap().clone();
        visibleTilemap = visibleAttrmap = null;
        bgPalettes = copyPalettes(scene.palettes());
        objPalettes = copyPalettes(scene.objectPalettes());
    }

    protected int[][] dynamicBgPalettes() {
        if (stage == Stage.BEACH_LOAD || stage == Stage.BEACH_FADE || (stage == Stage.SEA_FADE && fadeBg != null))
            return copyPalettes(fadeBg);
        int[][] palettes = copyPalettes(bgPalettes);
        if (paletteLightningCountdown == 0
                || (stage != Stage.SEA && stage != Stage.LINK_FACE && stage != Stage.SEA_FADE)) {
            return palettes;
        }
        int modifierIndex = (paletteLightningCountdown >>> 1) & 0x03;
        int modifier = colorModifiers[modifierIndex * 2] | (colorModifiers[modifierIndex * 2 + 1] << 8);
        for (int palette = 0; palette < palettes.length; palette++) {
            for (int color = 0; color < Math.min(3, palettes[palette].length); color++) {
                palettes[palette][color] = applyRgb555Modifier(palettes[palette][color], modifier);
            }
        }
        return palettes;
    }

    protected int[][] dynamicObjPalettes() {
        if (stage == Stage.BEACH_LOAD || stage == Stage.BEACH_FADE || (stage == Stage.SEA_FADE && fadeObj != null))
            return copyPalettes(fadeObj);
        int[][] palettes = copyPalettes(objPalettes);
        if (stage != Stage.TITLE_DX && stage != Stage.COMPLETE) {
            return palettes;
        }
        int pair = Math.min(7, Math.max(0, titleHoldFrame - 60) / 8);
        int[][] fadeRows = dxFadeInPalette.rows();
        if (palettes.length > 6 && fadeRows.length >= pair * 2 + 2) {
            palettes[5] = fadeRows[pair * 2].clone();
            palettes[6] = fadeRows[pair * 2 + 1].clone();
        }
        return palettes;
    }
}
