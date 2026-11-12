package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.function.Function;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/** Intro sequencing, presentation or ROM data used by ROM {@code IntroHandler}. */
abstract class IntroBeachMotion extends IntroSceneMotion {

    protected IntroBeachMotion() {
        super();
    }

    protected IntroBeachMotion(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
        super(romData, backgroundProvider);
    }

    protected IntroBeachMotion(DefaultResources resources) {
        super(resources);
    }

    protected void tickTitleReveal() {
        if (stageFrame % TITLE_REVEAL_STEP_FRAMES != 0) {
            return;
        }
        if (titleRowIndex < titleRows.size()) {
            applyTitleRow(titleRows.get(titleRowIndex));
            titleRowIndex++;
            titleRevealRows = titleRowIndex;
            if (titleRowIndex == titleRows.size()) transitionTo(Stage.TITLE_WAIT);
            return;
        }
        transitionTo(Stage.TITLE_WAIT);
    }

    protected void tickTitleDx() {
        titleHoldFrame++;
        if (titleHoldFrame >= TITLE_HOLD_FRAMES) {
            stage = Stage.COMPLETE;
        }
    }

    protected void transitionTo(Stage nextStage) {
        transitionFrame = snapshot;
        stage = nextStage;
        stageFrame = 0;
        switch (nextStage) {
            case LINK_FACE -> {
                linkFaceTimer = 0;
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_LINK_FACE);
                verticalWaveOffset = 0;
            }
            case SEA_FADE -> {
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_SEA);
                verticalWaveOffset = -verticalOffsets[((0x80 + 0xFC) >>> 4) & 7];
            }
            case BEACH_LOAD -> {
                scrollX = 0;
                setScene(IntroCutsceneScript.SCENE_BEACH);
                sectionScrollOffsets[0] = sectionScrollOffsets[2] = sectionScrollOffsets[3] = 0;
                sectionScrollOffsets[1] = 0x92;
                waveDestination = 0x1300;
                waveTiles = rom.tileBytes(0x4B00, 128);
                System.arraycopy(rom.tileBytes(0x5200, 32), 0, waveTiles, 0, 32);
                visibleWave = waveTiles.clone();
                fadeBg = brighten(bgPalettes, 31, 32);
                fadeObj = brighten(objPalettes, 31, 32);
                fadeSteps = 0;
                verticalWaveOffset = 0;
            }
            case BEACH_FADE, BEACH -> {}
            case TITLE_REVEAL -> {
                titleRowIndex = 0;
                titleRevealRows = 0;
                titleHoldFrame = 0;
                // Keep the streamed beach map and its 160,111 scroll origin.
                scrollX = beachScrollX;
                verticalWaveOffset = 0;
            }
            case TITLE_DX -> {
                rom.applyDrawCommands(tilemap, 0x7364);
                rom.applyDrawCommands(attrmap, 0x73AC);
            }
            case TITLE_WAIT, COMPLETE, SEA -> {}
        }
    }

    protected void decrementLightning() {
        lightningVisibleCountdown = Math.max(0, lightningVisibleCountdown - 1);
        paletteLightningCountdown = lightningVisibleCountdown;
        for (int index = lightning.size() - 1; index >= 0; index--) {
            Lightning current = lightning.get(index);
            if (current.remaining() <= 1) {
                lightning.remove(index);
            } else {
                lightning.set(
                        index, new Lightning(current.variant(), current.x(), current.y(), current.remaining() - 1));
            }
        }
    }

    protected void triggerSeaLightningIfNeeded() {
        for (int index = 0; index < SEA_LIGHTNING_SCROLLS.length; index++) {
            if (scrollX == SEA_LIGHTNING_SCROLLS[index]) {
                triggerLightning(index);
                return;
            }
        }
    }

    protected void triggerLightning(int index) {
        if (index < 0 || index >= lightningPositions.length) {
            return;
        }
        lightning.add(new Lightning(Math.max(0, lightningStatuses[index] - 1), lightningPositions[index], 0x30, 0x20));
        playThunder();
    }

    protected void playThunder() {
        lightningVisibleCountdown = 0x1C;
        // RenderLightning.playSfx -> PlayBombExplosionSfx (bank0.asm 00:0C4B).
        noiseSound.accept(0x0C);
    }

    protected void updateWaveOffsets() {
        if ((clock & 0x07) == 0) {
            sectionScrollOffsets[0]++;
        }
        if ((clock & 0x0F) == 0) {
            sectionScrollOffsets[1]++;
            sectionScrollOffsets[3]++;
        }
        if ((clock & 0x1F) == 0) {
            sectionScrollOffsets[2]++;
        }
        fraction4 += 0x28;
        sectionScrollOffsets[4] += fraction4 >>> 8;
        fraction4 &= 255;
        int waveIndex = ((clock + 0xFC) >>> 4) & 0x07;
        verticalWaveOffset = -verticalOffsets[waveIndex];
    }

    protected void refreshSnapshot() {
        if (snapshot == null) sprites = renderSprites();
        boolean previousScene =
                stageFrame == 0 && transitionFrame != null && (stage == Stage.LINK_FACE || stage == Stage.BEACH_LOAD);
        int blank = stage == Stage.SEA && frameCounter <= 3
                ? 0xFFFFFF
                : stage == Stage.LINK_FACE && stageFrame > 0 && stageFrame <= 3
                        ? 0
                        : stage == Stage.SEA_FADE && stageFrame <= 2
                                ? 0xFFFFFF
                                : stage == Stage.BEACH_LOAD && stageFrame > 0 ? 0xFFFFFF : -1;
        int[][] backgroundColors = visibleBg == null ? dynamicBgPalettes() : visibleBg;
        int[][] objectColors = visibleObj == null ? dynamicObjPalettes() : visibleObj;
        if (blank >= 0) {
            backgroundColors = solidPalettes(blank);
            objectColors = solidPalettes(blank);
        }
        snapshot = snapshotBuilder
                .sceneId(sceneId())
                .substate(substate())
                .frameCounter(frameCounter)
                .scrollX(
                        previousScene && stage == Stage.LINK_FACE
                                ? (transitionFrame.scrollX() + sectionScrollOffsets[0]) & 255
                                : previousScene ? transitionFrame.scrollX() : scrollX())
                .scrollY(previousScene ? transitionFrame.scrollY() : visibleScrollY)
                .lineScrollX(
                        previousScene && stage == Stage.LINK_FACE
                                ? null
                                : previousScene ? transitionFrame.lineScrollX() : lineScrollMap())
                .verticalWaveOffset(verticalWaveOffset)
                .tilemap(previousScene ? transitionFrame.tilemap() : visibleTilemap == null ? tilemap : visibleTilemap)
                .attrmap(previousScene ? transitionFrame.attrmap() : visibleAttrmap == null ? attrmap : visibleAttrmap)
                .bgPalettes(backgroundColors)
                .objPalettes(objectColors)
                .sprites(sprites)
                .titleRevealRows(titleRevealRows)
                .lineScrollY(
                        previousScene && stage == Stage.LINK_FACE
                                ? null
                                : previousScene ? transitionFrame.lineScrollY() : lineScrollYMap())
                .animatedTiles(
                        skipped
                                ? null
                                : previousScene
                                        ? transitionFrame.animatedTiles()
                                        : visibleWave == null ? waveTiles : visibleWave)
                .animatedTilesDestination(previousScene ? transitionFrame.animatedTilesDestination() : waveDestination)
                .build();
    }

    protected String substate() {
        return switch (stage) {
            case SEA -> "SEA";
            case LINK_FACE -> linkFaceTimer >= LINK_FACE_LIGHTNING_FRAME
                    ? "LINK_FACE_LIGHTNING"
                    : linkFaceTimer >= LINK_FACE_SCREAM_FRAME ? "LINK_FACE_SCREAM" : "LINK_FACE";
            case SEA_FADE -> "SEA_FADE";
            case BEACH_LOAD -> "BEACH_LOAD";
            case BEACH_FADE -> "BEACH_FADE";
            case BEACH -> "MARIN_STATE_" + marinState;
            case TITLE_REVEAL -> "TITLE_REVEAL";
            case TITLE_WAIT -> "TITLE_WAIT";
            case TITLE_DX -> "TITLE_DX";
            case COMPLETE -> "STABLE_TITLE";
        };
    }
}
