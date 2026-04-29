package linksawakening.cutscene;

import java.util.ArrayList;
import java.util.List;

public final class IntroSequence {

    public static final int SEA_SCROLL_FRAMES = (0xC0 - 0x50) * 8;
    public static final int LINK_FACE_FRAMES = 160;
    private static final int BEACH_MARIN_SOURCE_START_X = 0xB0;
    private static final int BEACH_MARIN_SOURCE_STOP_X = 0x47;
    private static final int BEACH_MARIN_MIRROR_OAM_BASE_X = 0xA0;
    private static final int BEACH_LINK_START_X = 0xFE;
    private static final int BEACH_LINK_Y = 0x6E;
    public static final int BEACH_MARIN_WALK_FRAMES =
        (BEACH_MARIN_SOURCE_START_X - BEACH_MARIN_SOURCE_STOP_X) * 4;
    public static final int BEACH_MARIN_WAIT_FRAMES = 0x40;
    private static final int BEACH_LINK_SCROLL_IN_FRAMES = 0x60;
    private static final int BEACH_MARIN_SECOND_WAIT_FRAMES = 0x40;
    private static final int BEACH_SCROLL_TO_PAUSE_3A_FRAMES = (0x3A - 0x30) * 4;
    private static final int BEACH_PAUSE_3A_FRAMES = 0x30;
    private static final int BEACH_SCROLL_TO_PAUSE_40_FRAMES = (0x40 - 0x3A) * 4;
    private static final int BEACH_PAUSE_40_FRAMES = 0x50;
    private static final int BEACH_SCROLL_TO_FINAL_HOLD_FRAMES = (0x56 - 0x40) * 4;
    private static final int BEACH_FINAL_HOLD_FRAMES = 0xE0;
    private static final int[] BEACH_SECTION_Y = { 0x30, 0x56, 0x68 };
    public static final int BEACH_FRAMES =
        BEACH_MARIN_WALK_FRAMES
            + BEACH_MARIN_WAIT_FRAMES
            + BEACH_LINK_SCROLL_IN_FRAMES
            + BEACH_MARIN_SECOND_WAIT_FRAMES
            + BEACH_SCROLL_TO_PAUSE_3A_FRAMES
            + BEACH_PAUSE_3A_FRAMES
            + BEACH_SCROLL_TO_PAUSE_40_FRAMES
            + BEACH_PAUSE_40_FRAMES
            + BEACH_SCROLL_TO_FINAL_HOLD_FRAMES
            + BEACH_FINAL_HOLD_FRAMES;
    public static final int TITLE_REVEAL_STEP_FRAMES = 1;
    public static final int TITLE_HOLD_FRAMES = 160;

    private enum Stage {
        SEA,
        LINK_FACE,
        BEACH,
        TITLE,
        COMPLETE
    }

    private static final int[][] SHIP_TILES = {
        { 0x00, 0x00, 0x1C, 0x02 },
        { 0x00, 0x08, 0x1E, 0x02 },
        { 0x10, -0x08, 0x20, 0x02 },
        { 0x10, 0x00, 0x22, 0x02 },
        { 0x10, 0x08, 0x24, 0x02 },
        { 0x10, 0x10, 0x26, 0x02 }
    };
    private static final int[] SHIP_HEAVE_TABLE = { 2, 1, 0, 0, 0, 1, 2, 2 };

    private Stage stage = Stage.SEA;
    private int stageFrame;
    private int frameCount;
    private int scrollX;
    private int shipX = 0xC0;
    private int shipY = 0x4E;

    public void tick() {
        if (stage == Stage.COMPLETE) {
            return;
        }

        frameCount++;
        stageFrame++;

        switch (stage) {
            case SEA -> tickSea();
            case LINK_FACE -> tickTimed(Stage.BEACH, LINK_FACE_FRAMES);
            case BEACH -> tickTimed(Stage.TITLE, BEACH_FRAMES);
            case TITLE -> tickTimed(Stage.COMPLETE, TITLE_HOLD_FRAMES);
            case COMPLETE -> { }
        }
    }

    private void tickSea() {
        if ((stageFrame % 8) == 0) {
            scrollX = (scrollX + 1) & 0xFF;
            shipX--;
        }
        if (stageFrame >= SEA_SCROLL_FRAMES) {
            transitionTo(Stage.LINK_FACE);
        }
    }

    private void tickTimed(Stage nextStage, int frames) {
        if (stageFrame >= frames) {
            transitionTo(nextStage);
        }
    }

    private void transitionTo(Stage nextStage) {
        stage = nextStage;
        stageFrame = 0;
        if (nextStage != Stage.SEA) {
            scrollX = 0;
        }
    }

    public boolean isActive() {
        return stage != Stage.COMPLETE;
    }

    public void skipToTitle() {
        stage = Stage.COMPLETE;
        stageFrame = 0;
        scrollX = 0;
    }

    public String sceneId() {
        return switch (stage) {
            case SEA -> IntroCutsceneScript.SCENE_SEA;
            case LINK_FACE -> IntroCutsceneScript.SCENE_LINK_FACE;
            case BEACH -> IntroCutsceneScript.SCENE_BEACH;
            case TITLE, COMPLETE -> IntroCutsceneScript.SCENE_TITLE;
        };
    }

    public int scrollX() {
        if (stage == Stage.BEACH) {
            return beachScrollX();
        }
        return scrollX;
    }

    public int scrollY() {
        return 0;
    }

    public int scrollXForLine(int screenY) {
        if (stage != Stage.BEACH) {
            return scrollX();
        }
        return (beachScrollX() + beachSectionScrollOffset(beachSectionIndex(screenY))) & 0xFF;
    }

    public int[] lineScrollX(int height) {
        if (stage != Stage.BEACH) {
            return null;
        }
        int[] scrolls = new int[height];
        for (int y = 0; y < height; y++) {
            scrolls[y] = scrollXForLine(y);
        }
        return scrolls;
    }

    public int titleRevealRows() {
        if (stage == Stage.COMPLETE) {
            return TitleReveal.ROW_COUNT;
        }
        if (stage != Stage.TITLE) {
            return 0;
        }
        return Math.min(TitleReveal.ROW_COUNT, stageFrame / TITLE_REVEAL_STEP_FRAMES);
    }

    int shipX() {
        return shipX;
    }

    public List<IntroSprite> sprites() {
        List<IntroSprite> sprites = new ArrayList<>();
        if (stage == Stage.SEA) {
            addRain(sprites, 0x10);
            addShip(sprites);
        } else if (stage == Stage.LINK_FACE) {
            addRain(sprites, 0x15);
            if (stageFrame >= 144 && stageFrame < 176) {
                addLightning(sprites, 0x58, 0x30);
            }
        } else if (stage == Stage.BEACH) {
            addBeachSprites(sprites);
        } else if (stage == Stage.TITLE) {
            addTitleSparkles(sprites);
        }
        return List.copyOf(sprites);
    }

    private void addRain(List<IntroSprite> sprites, int rows) {
        for (int i = 0; i < rows; i++) {
            int x = (0x10 + i * 0x1C + frameCount * 2) % 0xA0;
            int y = (0x10 + i * 0x25 + frameCount * 3) % 0x90;
            int tile = (i & 1) == 0 ? 0x28 : 0x70 + ((i & 3) * 2);
            sprites.add(new IntroSprite(tile, x, y, 0, false, false));
        }
    }

    private void addShip(List<IntroSprite> sprites) {
        int heave = SHIP_HEAVE_TABLE[((frameCount + 0xD0) >> 4) & 0x07];
        for (int[] entry : SHIP_TILES) {
            sprites.add(IntroSprite.fromOam8x16(
                entry[2],
                shipX + entry[1],
                shipY + heave + entry[0],
                entry[3] & 0x07,
                (entry[3] & 0x20) != 0,
                (entry[3] & 0x40) != 0
            ));
        }
    }

    private void addLightning(List<IntroSprite> sprites, int x, int y) {
        sprites.add(IntroSprite.fromOam8x16(0x34, x, y, 1, false, false));
        sprites.add(IntroSprite.fromOam8x16(0x36, x + 8, y, 1, false, false));
        sprites.add(IntroSprite.fromOam8x16(0x2C, x, y + 16, 1, false, false));
        sprites.add(IntroSprite.fromOam8x16(0x2E, x - 8, y + 32, 1, false, false));
    }

    private void addBeachSprites(List<IntroSprite> sprites) {
        int marinX = beachMarinX();
        int marinTileBase = beachMarinTileBase();
        sprites.add(IntroSprite.fromOam8x16(marinTileBase, marinX, 0x68, 3, false, false));
        sprites.add(IntroSprite.fromOam8x16(marinTileBase + 2, marinX + 8, 0x68, 3, false, false));

        int linkApproachFrame = beachLinkApproachFrame();
        if (linkApproachFrame < 0) {
            return;
        }

        int linkX = beachLinkX(linkApproachFrame);
        if (linkX >= 0xF0) {
            return;
        }
        sprites.add(IntroSprite.fromOam8x16(0x10, linkX, BEACH_LINK_Y, 0, false, false));
        sprites.add(IntroSprite.fromOam8x16(0x12, linkX + 8, BEACH_LINK_Y, 0, false, false));
    }

    private int beachMarinX() {
        int sourceX = stageFrame < BEACH_MARIN_WALK_FRAMES
            ? BEACH_MARIN_SOURCE_START_X - stageFrame / 4
            : BEACH_MARIN_SOURCE_STOP_X;
        // Preserve the disassembly's timing while matching the mirrored beach composition.
        return BEACH_MARIN_MIRROR_OAM_BASE_X - sourceX;
    }

    private int beachMarinTileBase() {
        int linkApproachFrame = beachLinkApproachFrame();
        if (stageFrame < BEACH_MARIN_WALK_FRAMES) {
            return ((frameCount >> 3) & 0x01) * 4;
        }
        if (linkApproachFrame >= 0 && linkApproachFrame < BEACH_LINK_SCROLL_IN_FRAMES) {
            return ((linkApproachFrame >> 2) & 0x01) * 4;
        }
        return 0x04;
    }

    private int beachLinkX(int linkApproachFrame) {
        if (linkApproachFrame < BEACH_LINK_SCROLL_IN_FRAMES) {
            return BEACH_LINK_START_X - linkApproachFrame;
        }
        return 0x9E - beachState3MovingFrames(linkApproachFrame) / 2;
    }

    private int beachState3MovingFrames(int linkApproachFrame) {
        int frame = linkApproachFrame - BEACH_LINK_SCROLL_IN_FRAMES - BEACH_MARIN_SECOND_WAIT_FRAMES;
        if (frame <= 0) {
            return 0;
        }

        int moving = Math.min(frame, BEACH_SCROLL_TO_PAUSE_3A_FRAMES);
        frame -= BEACH_SCROLL_TO_PAUSE_3A_FRAMES + BEACH_PAUSE_3A_FRAMES;
        if (frame <= 0) {
            return moving;
        }

        moving += Math.min(frame, BEACH_SCROLL_TO_PAUSE_40_FRAMES);
        frame -= BEACH_SCROLL_TO_PAUSE_40_FRAMES + BEACH_PAUSE_40_FRAMES;
        if (frame <= 0) {
            return moving;
        }

        moving += Math.min(frame, BEACH_SCROLL_TO_FINAL_HOLD_FRAMES);
        return moving;
    }

    private int beachScrollX() {
        int linkApproachFrame = beachLinkApproachFrame();
        if (linkApproachFrame < 0) {
            return 0;
        }
        if (linkApproachFrame < BEACH_LINK_SCROLL_IN_FRAMES) {
            return linkApproachFrame / 2;
        }

        int frame = linkApproachFrame - BEACH_LINK_SCROLL_IN_FRAMES;
        if (frame < BEACH_MARIN_SECOND_WAIT_FRAMES) {
            return 0x30;
        }
        frame -= BEACH_MARIN_SECOND_WAIT_FRAMES;

        if (frame < BEACH_SCROLL_TO_PAUSE_3A_FRAMES) {
            return 0x30 + frame / 4;
        }
        frame -= BEACH_SCROLL_TO_PAUSE_3A_FRAMES;
        if (frame < BEACH_PAUSE_3A_FRAMES) {
            return 0x3A;
        }
        frame -= BEACH_PAUSE_3A_FRAMES;

        if (frame < BEACH_SCROLL_TO_PAUSE_40_FRAMES) {
            return 0x3A + frame / 4;
        }
        frame -= BEACH_SCROLL_TO_PAUSE_40_FRAMES;
        if (frame < BEACH_PAUSE_40_FRAMES) {
            return 0x40;
        }
        frame -= BEACH_PAUSE_40_FRAMES;

        if (frame < BEACH_SCROLL_TO_FINAL_HOLD_FRAMES) {
            return 0x40 + frame / 4;
        }
        return 0x56;
    }

    private int beachSectionIndex(int screenY) {
        if (screenY < BEACH_SECTION_Y[0]) {
            return 0;
        }
        if (screenY < BEACH_SECTION_Y[1]) {
            return 1;
        }
        if (screenY < BEACH_SECTION_Y[2]) {
            return 2;
        }
        return 3;
    }

    private int beachSectionScrollOffset(int sectionIndex) {
        int fastWaveFrames = beachFastWaveScrollFrames();
        int slowWaveFrames = beachSlowWaveScrollFrames();
        return switch (sectionIndex) {
            case 0 -> (fastWaveFrames / 8 + slowWaveFrames / 16) & 0xFF;
            case 1 -> (0x92
                + fixedPointScrollOffset(fastWaveFrames, 0x50)
                + fixedPointScrollOffset(slowWaveFrames, 0x28)) & 0xFF;
            case 2 -> (fixedPointScrollOffset(fastWaveFrames, 0x58)
                + fixedPointScrollOffset(slowWaveFrames, 0x2C)) & 0xFF;
            default -> (fixedPointScrollOffset(fastWaveFrames, 0xB0)
                + fixedPointScrollOffset(slowWaveFrames, 0x58)) & 0xFF;
        };
    }

    private int beachFastWaveScrollFrames() {
        int linkApproachFrame = beachLinkApproachFrame();
        if (linkApproachFrame < 0) {
            return stageFrame;
        }
        return BEACH_MARIN_WALK_FRAMES + Math.min(linkApproachFrame, BEACH_LINK_SCROLL_IN_FRAMES);
    }

    private int beachSlowWaveScrollFrames() {
        int linkApproachFrame = beachLinkApproachFrame();
        if (linkApproachFrame <= BEACH_LINK_SCROLL_IN_FRAMES + BEACH_MARIN_SECOND_WAIT_FRAMES) {
            return 0;
        }
        return beachState3MovingFrames(linkApproachFrame);
    }

    private static int fixedPointScrollOffset(int frames, int increment) {
        return (frames * increment / 0x100) & 0xFF;
    }

    private int beachLinkApproachFrame() {
        int linkStartFrame = BEACH_MARIN_WALK_FRAMES + BEACH_MARIN_WAIT_FRAMES;
        return stageFrame - linkStartFrame;
    }

    private void addTitleSparkles(List<IntroSprite> sprites) {
        int phase = (frameCount / 8) & 0x07;
        int[][] variants = {
            { 0x38, 0x38, 1 },
            { 0x3A, 0x3A, 1 },
            { 0x3A, 0x3A, 1 },
            { 0x3C, 0x3E, 0 },
            { 0x3C, 0x3E, 0 },
            { 0x3A, 0x3A, 1 },
            { 0x3A, 0x3A, 1 },
            { 0x38, 0x38, 1 }
        };
        int[][] positions = {
            { 0x18, 0x20 }, { 0x38, 0x48 }, { 0x40, 0x44 }, { 0x58, 0x28 },
            { 0x60, 0x44 }, { 0x80, 0x28 }, { 0x88, 0x28 }, { 0x18, 0x40 }
        };
        int variant = (frameCount / 4) & 0x07;
        sprites.add(IntroSprite.fromOam8x16(variants[variant][0],
            positions[phase][0], positions[phase][1], 0, false, false));
        sprites.add(IntroSprite.fromOam8x16(variants[variant][1],
            positions[phase][0] + 8, positions[phase][1], 0, variants[variant][2] != 0, false));
    }
}
