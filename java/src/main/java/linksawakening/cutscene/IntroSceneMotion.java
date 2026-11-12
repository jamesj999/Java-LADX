package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.List;
import java.util.function.Function;
import linksawakening.rom.Rom;
import linksawakening.scene.BackgroundScene;

/** Intro sequencing, presentation or ROM data used by ROM {@code IntroHandler}. */
abstract class IntroSceneMotion extends IntroLifecycle {

    protected IntroSceneMotion() {
        super();
    }

    protected IntroSceneMotion(Rom romData, Function<String, BackgroundScene> backgroundProvider) {
        super(romData, backgroundProvider);
    }

    protected IntroSceneMotion(DefaultResources resources) {
        super(resources);
    }

    protected void tickSeaFade() {
        decrementLightning();
        seaFadeTimer++;
        if (seaFadeTimer >= 0x30) {
            fadeSteps++;
            fadeBg = brighten(bgPalettes, fadeSteps, 32);
            fadeObj = brighten(objPalettes, Math.max(0, seaFadeTimer - 0x47), 32);
            int[][] firstTwo = brighten(objPalettes, fadeSteps, 8);
            fadeObj[0] = firstTwo[0];
            fadeObj[1] = firstTwo[1];
        }
        if (seaFadeTimer >= 0x58) {
            beachFadeTimer = BEACH_FADE_FRAMES;
            transitionTo(Stage.BEACH_LOAD);
            loadDelay = 3;
        }
    }

    protected void tickBeachFade() {
        tickSeaWaveSound();
        beachFadeTimer--;
        // func_020_6AC1 restores all palettes one RGB555 step at a time.
        if (beachFadeTimer < 0x34) {
            if ((beachFadeTimer & 3) == 0) fadeSteps = Math.min(12, fadeSteps + 1);
            if (fadeSteps + (clock & 3) >= 6) {
                fadeBg = approachPalettes(fadeBg, bgPalettes);
                fadeObj = approachPalettes(fadeObj, objPalettes);
            }
        }
        if (beachFadeTimer == 0) {
            marinState = 0;
            marinX = BEACH_MARIN_START_X;
            marinInertia = 1;
            marinTimer = 0;
            linkVisible = false;
            transitionTo(Stage.BEACH);
        }
    }

    protected void tickBeach() {
        // Entity slot 1 (Link) executes before slot 0 (Marin).
        if (linkVisible) tickInertLink();
        if (stage != Stage.BEACH) return;
        tickSeaWaveSound();
        // Link's handler moves both entities vertically before Marin builds her OAM.
        List<IntroSprite> corrected = correctionSpriteScratch;
        corrected.clear();
        corrected.addAll(pendingSprites);
        if (corrected.size() >= 2) {
            corrected.subList(corrected.size() - 2, corrected.size()).clear();
            addPair(corrected, marinVariants.get(marinVariant), marinX, marinY);
            pendingSprites = List.copyOf(corrected);
        }
        tickMarin();
    }

    private void tickMarin() {
        switch (marinState) {
            case 0 -> {
                updateBeachOffsets(false);
                marinVariant = (clock >>> 3) & 1;
                if (marinX < 0x48) {
                    marinTimer = 0x40;
                    marinState = 1;
                }
                if (--marinInertia == 0) {
                    marinInertia = 4;
                    marinX--;
                }
            }
            case 1 -> {
                animateWaves(true);
                marinVariant = 1;
                if (marinTimer > 0) marinTimer--;
                else {
                    marinState = 2;
                    linkVisible = true;
                    linkX = BEACH_LINK_START_X;
                    linkState = 0;
                    linkTimer = 0;
                    clock = 0;
                }
            }
            case 2 -> {
                updateBeachOffsets(false);
                linkX = (linkX - 1) & 255;
                if ((clock & 1) == 0) {
                    beachScrollX++;
                    patchMountains();
                    if (beachScrollX == 0x30) {
                        marinTimer = 0x40;
                        marinState = 3;
                    }
                }
                if (marinState == 2) marinVariant = (clock >>> 2) & 1;
            }
            case 3 -> {
                if (marinTimer > 0) {
                    marinTimer--;
                    marinVariant = 1;
                    animateWaves(true);
                } else {
                    updateBeachOffsets(true);
                    marinVariant = (clock >>> 4) & 1;
                    if ((clock & 1) == 0) linkX = (linkX - 1) & 255;
                    if ((clock & 3) == 0) {
                        beachScrollX++;
                        patchMountains();
                        if (beachScrollX == 0x3A) marinTimer = 0x30;
                        if (beachScrollX == 0x40) marinTimer = 0x50;
                        if (beachScrollX == 0x56) {
                            beachScrollX = 0xA0;
                            marinTimer = 0xE0;
                            marinState = 4;
                            beachScrollStoppedThisFrame = true;
                        }
                    }
                }
            }
            case 4 -> {
                animateWaves(true);
                if ((clock & 1) == 0) {
                    int timer = marinTimer;
                    if (marinTimer > 0) marinTimer--;
                    boolean leaning = (timer < 0xA0 && timer >= 0x90)
                            || (timer < 0x50 && timer >= 0x4A)
                            || (timer < 0x3C && timer >= 0x36);
                    marinVariant = leaning ? 3 : 2;
                    linkVariant = leaning ? 1 : 0;
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    protected void tickInertLink() {
        switch (linkState) {
            case 0 -> {
                linkTimer = (linkTimer - 1) & 255;
                if (linkTimer == 0) {
                    linkTimer = 0x90;
                    linkState = 1;
                }
            }
            case 1 -> {
                if ((clock & 3) == 0 && --linkTimer == 0) linkState = 2;
            }
            case 2 -> {
                if (panRows == 19) {
                    linkState = 3;
                    linkTimer = 0x17;
                    return;
                }
                if ((++panCounter & 3) != 0) return;
                marinY = Math.min(0xA0, marinY + 1);
                linkY = Math.min(0xA0, linkY + 1);
                int oldY = scrollY;
                scrollY = (scrollY - 1) & 255;
                if ((oldY & 7) == 0) {
                    int target = 0x3F4 - panRows * 32;
                    int[] rows = rom.postBeachTilemap();
                    for (int i = 0; i < 20; i++)
                        tilemap[(target & 0x3E0) | ((target + i) & 31)] = rows[panRows * 20 + i];
                    for (int i = 0; i < 32; i++) attrmap[(target - 20 + i) & 1023] = 3;
                    panRows++;
                    drawCommandQueued = true;
                }
            }
            case 3 -> {
                if ((clock & 3) == 0 && --linkTimer == 0) transitionTo(Stage.TITLE_REVEAL);
            }
            default -> throw new IllegalStateException();
        }
    }

    protected void patchMountains() {
        if (beachScrollX == 0x20) rom.applyDrawCommands(tilemap, 0x79EC);
        if (beachScrollX == 0x22) rom.applyDrawCommands(tilemap, 0x79FD);
    }

    protected void updateBeachOffsets(boolean slow) {
        if ((clock & (slow ? 15 : 7)) == 0) sectionScrollOffsets[0]++;
        fraction4 += slow ? 0x28 : 0x50;
        fraction5 += slow ? 0x2C : 0x58;
        fractionD += slow ? 0x58 : 0xB0;
        sectionScrollOffsets[1] += fraction4 >>> 8;
        fraction4 &= 255;
        sectionScrollOffsets[2] += fraction5 >>> 8;
        fraction5 &= 255;
        sectionScrollOffsets[3] += fractionD >>> 8;
        fractionD &= 255;
        animateWaves(true);
    }

    protected void animateWaves(boolean beach) {
        if ((clock & 15) < 4 || drawCommandQueued) return;
        int chunk = (clock & 3) * 32;
        byte[] bytes = rom.tileBytes(rom.animationFrameAddress(beach, clock) + chunk, 32);
        System.arraycopy(bytes, 0, waveTiles, chunk, 32);
    }
}
