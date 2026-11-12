package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

/** Marin and inert Link actor state machines from IntroHandler; owns actor positions, counters and animation variants. */
final class IntroBeachActors {

    private int marinVariant, linkVariant;
    private int marinY = 0x68, linkY = BEACH_LINK_Y;
    private int linkState, linkTimer, panCounter, panRows;
    private int marinState;
    private int marinX = BEACH_MARIN_START_X;
    private int marinInertia = 1;
    private int marinTimer;
    private int linkX = BEACH_LINK_START_X;
    private boolean linkVisible;
    private final IntroRomData rom;
    private final IntroVideoMemory video;
    private final IntroWater water;
    private final IntroClock clockState;

    IntroBeachActors(IntroRomData rom, IntroVideoMemory video, IntroWater water, IntroClock clockState) {
        this.rom = rom;
        this.video = video;
        this.water = water;
        this.clockState = clockState;
    }

    void tickMarin() {
        switch (marinState) {
            case 0 -> {
                water.updateBeachOffsets(false);
                this.marinVariant((clockState.clock() >>> 3) & 1);
                if (marinX < 0x48) {
                    marinTimer = 0x40;
                    marinState = 1;
                }
                if ((marinInertia = marinInertia - 1) == 0) {
                    marinInertia = 4;
                    marinX = marinX - 1;
                }
            }
            case 1 -> {
                water.animateWaves(true);
                this.marinVariant(1);
                if (marinTimer > 0) marinTimer = marinTimer - 1;
                else {
                    marinState = 2;
                    linkVisible = true;
                    linkX = BEACH_LINK_START_X;
                    this.linkState(0);
                    this.linkTimer(0);
                    clockState.clock(0);
                }
            }
            case 2 -> {
                water.updateBeachOffsets(false);
                linkX = (linkX - 1) & 255;
                if ((clockState.clock() & 1) == 0) {
                    video.beachScrollX(video.beachScrollX() + 1);
                    patchMountains();
                    if (video.beachScrollX() == 0x30) {
                        marinTimer = 0x40;
                        marinState = 3;
                    }
                }
                if (marinState == 2) this.marinVariant((clockState.clock() >>> 2) & 1);
            }
            case 3 -> {
                if (marinTimer > 0) {
                    marinTimer = marinTimer - 1;
                    this.marinVariant(1);
                    water.animateWaves(true);
                } else {
                    water.updateBeachOffsets(true);
                    this.marinVariant((clockState.clock() >>> 4) & 1);
                    if ((clockState.clock() & 1) == 0) linkX = (linkX - 1) & 255;
                    if ((clockState.clock() & 3) == 0) {
                        video.beachScrollX(video.beachScrollX() + 1);
                        patchMountains();
                        if (video.beachScrollX() == 0x3A) marinTimer = 0x30;
                        if (video.beachScrollX() == 0x40) marinTimer = 0x50;
                        if (video.beachScrollX() == 0x56) {
                            video.beachScrollX(0xA0);
                            marinTimer = 0xE0;
                            marinState = 4;
                            video.beachScrollStoppedThisFrame(true);
                        }
                    }
                }
            }
            case 4 -> {
                water.animateWaves(true);
                if ((clockState.clock() & 1) == 0) {
                    int timer = marinTimer;
                    if (marinTimer > 0) marinTimer = marinTimer - 1;
                    boolean leaning = (timer < 0xA0 && timer >= 0x90)
                            || (timer < 0x50 && timer >= 0x4A)
                            || (timer < 0x3C && timer >= 0x36);
                    this.marinVariant(leaning ? 3 : 2);
                    linkVariant = leaning ? 1 : 0;
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    boolean tickInertLink() {
        switch (this.linkState()) {
            case 0 -> {
                this.linkTimer((this.linkTimer() - 1) & 255);
                if (this.linkTimer() == 0) {
                    this.linkTimer(0x90);
                    this.linkState(1);
                }
            }
            case 1 -> {
                if ((clockState.clock() & 3) == 0 && this.linkTimer(this.linkTimer() - 1) == 0) this.linkState(2);
            }
            case 2 -> {
                if (panRows == 19) {
                    this.linkState(3);
                    this.linkTimer(0x17);
                    return false;
                }
                if ((this.panCounter(this.panCounter() + 1) & 3) != 0) return false;
                marinY = Math.min(0xA0, marinY + 1);
                this.linkY(Math.min(0xA0, this.linkY() + 1));
                int oldY = video.scrollY();
                video.scrollY((video.scrollY() - 1) & 255);
                if ((oldY & 7) == 0) {
                    int target = 0x3F4 - panRows * 32;
                    int[] rows = rom.postBeachTilemap();
                    for (int i = 0; i < 20; i++)
                        video.tilemap()[(target & 0x3E0) | ((target + i) & 31)] = rows[panRows * 20 + i];
                    for (int i = 0; i < 32; i++) video.attrmap()[(target - 20 + i) & 1023] = 3;
                    panRows = panRows + 1;
                    video.drawCommandQueued(true);
                }
            }
            case 3 -> {
                if ((clockState.clock() & 3) == 0 && this.linkTimer(this.linkTimer() - 1) == 0) return true;
            }
            default -> throw new IllegalStateException();
        }
        return false;
    }

    void patchMountains() {
        if (video.beachScrollX() == 0x20) rom.applyDrawCommands(video.tilemap(), 0x79EC);
        if (video.beachScrollX() == 0x22) rom.applyDrawCommands(video.tilemap(), 0x79FD);
    }

    int marinVariant() {
        return marinVariant;
    }

    int marinVariant(int value) {
        marinVariant = value;
        return value;
    }

    int linkVariant() {
        return linkVariant;
    }

    int linkVariant(int value) {
        linkVariant = value;
        return value;
    }

    int marinY() {
        return marinY;
    }

    int marinY(int value) {
        marinY = value;
        return value;
    }

    int linkY() {
        return linkY;
    }

    int linkY(int value) {
        linkY = value;
        return value;
    }

    int linkState() {
        return linkState;
    }

    int linkState(int value) {
        linkState = value;
        return value;
    }

    int linkTimer() {
        return linkTimer;
    }

    int linkTimer(int value) {
        linkTimer = value;
        return value;
    }

    int panCounter() {
        return panCounter;
    }

    int panCounter(int value) {
        panCounter = value;
        return value;
    }

    int panRows() {
        return panRows;
    }

    int panRows(int value) {
        panRows = value;
        return value;
    }

    int marinState() {
        return marinState;
    }

    int marinState(int value) {
        marinState = value;
        return value;
    }

    int marinX() {
        return marinX;
    }

    int marinX(int value) {
        marinX = value;
        return value;
    }

    int marinInertia() {
        return marinInertia;
    }

    int marinInertia(int value) {
        marinInertia = value;
        return value;
    }

    int marinTimer() {
        return marinTimer;
    }

    int marinTimer(int value) {
        marinTimer = value;
        return value;
    }

    int linkX() {
        return linkX;
    }

    int linkX(int value) {
        linkX = value;
        return value;
    }

    boolean linkVisible() {
        return linkVisible;
    }

    boolean linkVisible(boolean value) {
        linkVisible = value;
        return value;
    }
}
