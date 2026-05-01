package linksawakening.equipment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class SwordTest {

    private static final int STATE_DRAW = 1; // SWORD_ANIMATION_STATE_DRAW in the ROM

    @Test
    void startingSlashPlaysOneOfTheRomSwordSwingNoiseEffects() {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        SequenceIntSupplier randomBytes = new SequenceIntSupplier(0, 1, 2, 3);
        Sword sword = new Sword(null, null, soundSink, randomBytes);

        sword.onPress();

        assertEquals(List.of(GameplaySoundEvent.SWORD_SWING_A), soundSink.events);

        while (sword.state() != Sword.STATE_NONE) {
            sword.tick(false);
        }
        sword.onPress();
        while (sword.state() != Sword.STATE_NONE) {
            sword.tick(false);
        }
        sword.onPress();
        while (sword.state() != Sword.STATE_NONE) {
            sword.tick(false);
        }
        sword.onPress();

        assertEquals(List.of(
                GameplaySoundEvent.SWORD_SWING_A,
                GameplaySoundEvent.SWORD_SWING_B,
                GameplaySoundEvent.SWORD_SWING_C,
                GameplaySoundEvent.SWORD_SWING_D), soundSink.events);
    }

    @Test
    void pressingSwordWhileSwingingDoesNotReplaySwingSound() {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Sword sword = new Sword(null, null, soundSink, new SequenceIntSupplier(0));

        sword.onPress();
        sword.onPress();

        assertEquals(List.of(GameplaySoundEvent.SWORD_SWING_A), soundSink.events);
    }

    @Test
    void reachingMaximumChargePlaysChargingSwordJingleOnce() {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Sword sword = new Sword(null, null, soundSink, new SequenceIntSupplier(0));

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }
        sword.tick(true);

        assertEquals(List.of(
                GameplaySoundEvent.SWORD_SWING_A,
                GameplaySoundEvent.SWORD_FULLY_CHARGED), soundSink.events);
    }

    @Test
    void releasingFullyChargedSwordPlaysSpinAttackNoise() {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Sword sword = new Sword(null, null, soundSink, new SequenceIntSupplier(0));

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }

        sword.onRelease();

        assertEquals(List.of(
                GameplaySoundEvent.SWORD_SWING_A,
                GameplaySoundEvent.SWORD_FULLY_CHARGED,
                GameplaySoundEvent.SPIN_ATTACK), soundSink.events);
    }

    @Test
    void tapSlashDoesNotExposeAnExtraSwingEndFrame() {
        Sword sword = new Sword(null, null);

        sword.onPress();

        List<Integer> observed = new ArrayList<>();
        do {
            sword.tick(false);
            observed.add(sword.state());
        } while (sword.state() != Sword.STATE_NONE);

        assertEquals(
            List.of(
                STATE_DRAW, STATE_DRAW, STATE_DRAW,
                Sword.STATE_SWING_START, Sword.STATE_SWING_START, Sword.STATE_SWING_START,
                Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE,
                Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE,
                Sword.STATE_NONE
            ),
            observed
        );
    }

    @Test
    void heldSlashTransitionsStraightIntoHolding() {
        Sword sword = new Sword(null, null);

        sword.onPress();

        List<Integer> observed = new ArrayList<>();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
            observed.add(sword.state());
        }

        assertEquals(
            List.of(
                STATE_DRAW, STATE_DRAW, STATE_DRAW,
                Sword.STATE_SWING_START, Sword.STATE_SWING_START, Sword.STATE_SWING_START,
                Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE,
                Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE, Sword.STATE_SWING_MIDDLE,
                Sword.STATE_HOLDING
            ),
            observed
        );
    }

    @Test
    void holdingSwordDoesNotEnableStaticCollision() {
        Sword sword = new Sword(null, null);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        assertEquals(Sword.STATE_HOLDING, sword.state());
        assertEquals(false, sword.staticCollisionActive());
    }

    @Test
    void releasingFullyChargedSwordStartsSpinAttackInsteadOfEndingImmediately() {
        Sword sword = new Sword(null, null);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }

        sword.onRelease();
        sword.tick(false);

        assertNotEquals(Sword.STATE_NONE, sword.state());

        sword.tick(false);

        assertEquals(true, sword.staticCollisionActive());
        assertEquals(true, sword.spinAttackActive());
    }

    @Test
    void fullyChargedReleaseKeepsSwordExtendedForReleaseFrame() {
        Sword sword = new Sword(null, null);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }

        sword.onRelease();
        sword.tick(false);

        assertEquals(Sword.STATE_HOLDING, sword.state());
    }

    @Test
    void fullyChargedReleaseFrameRendersSameSwordPoseAsHolding() throws Exception {
        byte[] romData = loadRom();
        Sword sword = new Sword(RomTables.loadFromRom(romData), SwordSpriteSheet.loadFromRom(romData));

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }

        byte[] holdingBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        sword.render(holdingBuffer, 40, 48, Link.DIRECTION_RIGHT, 0, 0);

        sword.onRelease();
        sword.tick(false);

        byte[] releaseBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        sword.render(releaseBuffer, 40, 48, Link.DIRECTION_RIGHT, 0, 0);

        assertEquals(Arrays.toString(alphaMask(holdingBuffer)), Arrays.toString(alphaMask(releaseBuffer)));
    }

    @Test
    void holdingSwordRendersUsingRomHoldingEntry() throws Exception {
        byte[] romData = loadRom();
        RomTables romTables = RomTables.loadFromRom(romData);
        SwordSpriteSheet spriteSheet = SwordSpriteSheet.loadFromRom(romData);
        Sword sword = new Sword(romTables, spriteSheet);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        byte[] actual = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        sword.render(actual, 40, 48, Link.DIRECTION_RIGHT, 0, 0);

        byte[] expected = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        drawExpectedSwordPose(expected, romTables, spriteSheet, 40, 48, Link.DIRECTION_RIGHT, Sword.STATE_HOLDING);

        assertEquals(Arrays.toString(alphaMask(expected)), Arrays.toString(alphaMask(actual)));
    }

    @Test
    void fullyChargedReleaseStartsOnRomFirstSpinFrameOnFollowingFrame() {
        Sword sword = new Sword(null, null);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }

        sword.onRelease();
        sword.tick(false);
        sword.tick(false);

        assertEquals(Sword.STATE_SWING_END, sword.state());
    }

    @Test
    void spinAttackCollisionWalksEveryRomSwordDirection() throws Exception {
        Sword sword = new Sword(RomTables.loadFromRom(loadRom()), null);
        Set<Integer> collisionIndices = new HashSet<>();

        sword.onPress();
        sword.attackDirection(Link.DIRECTION_RIGHT);
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }

        sword.onRelease();
        while (!sword.spinAttackActive()) {
            sword.tick(false);
        }
        while (sword.spinAttackActive()) {
            collisionIndices.add(sword.staticCollisionMapIndex(Link.DIRECTION_UP));
            sword.tick(false);
        }

        assertEquals(Set.of(4, 5, 6, 7, 8, 9, 10, 11), collisionIndices);
    }

    @Test
    void downFacingSpinAttackUsesRomSectorOrder() {
        Sword sword = fullyChargedSwordFacing(Link.DIRECTION_DOWN);

        sword.onRelease();
        while (!sword.spinAttackActive()) {
            sword.tick(false);
        }

        List<Integer> states = new ArrayList<>();
        List<Integer> directions = new ArrayList<>();
        int lastState = -1;
        int lastDirection = -1;
        while (sword.spinAttackActive()) {
            int state = sword.state();
            int direction = sword.attackDirection(Link.DIRECTION_DOWN);
            if (state != lastState || direction != lastDirection) {
                states.add(state);
                directions.add(direction);
                lastState = state;
                lastDirection = direction;
            }
            sword.tick(false);
        }

        assertEquals(
            List.of(
                Sword.STATE_SWING_END,
                Sword.STATE_SWING_MIDDLE,
                Sword.STATE_SWING_START,
                Sword.STATE_SWING_MIDDLE,
                Sword.STATE_SWING_END,
                Sword.STATE_SWING_MIDDLE,
                Sword.STATE_SWING_END,
                Sword.STATE_SWING_MIDDLE
            ),
            states
        );
        assertEquals(
            List.of(
                Link.DIRECTION_DOWN,
                Link.DIRECTION_RIGHT,
                Link.DIRECTION_RIGHT,
                Link.DIRECTION_UP,
                Link.DIRECTION_UP,
                Link.DIRECTION_LEFT,
                Link.DIRECTION_LEFT,
                Link.DIRECTION_DOWN
            ),
            directions
        );
    }

    @Test
    void spinAttackDurationMatchesRomCountdownCadence() {
        Sword sword = fullyChargedSwordFacing(Link.DIRECTION_RIGHT);

        sword.onRelease();
        while (!sword.spinAttackActive()) {
            sword.tick(false);
        }

        int activeFrames = 0;
        while (sword.spinAttackActive()) {
            activeFrames++;
            sword.tick(false);
        }

        assertEquals(25, activeFrames);
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream in = SwordTest.class.getResourceAsStream("/rom/azle.gbc")) {
            if (in == null) {
                throw new IOException("Missing ROM resource /rom/azle.gbc");
            }
            return in.readAllBytes();
        }
    }

    private static byte[] alphaMask(byte[] rgbaBuffer) {
        byte[] out = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT];
        for (int i = 0; i < out.length; i++) {
            out[i] = rgbaBuffer[i * 4 + 3];
        }
        return out;
    }

    private static void drawExpectedSwordPose(byte[] buffer, RomTables romTables, SwordSpriteSheet spriteSheet,
                                              int linkPixelX, int linkPixelY, int javaDirection, int swordState) {
        int romDirection = toRomDirection(javaDirection);
        int swordDir = romTables.swordDirectionFor(romDirection, swordState);
        int bladeX = linkPixelX + romTables.swordBladeXOffset(romDirection, swordState);
        int bladeY = linkPixelY + romTables.swordBladeYOffset(romDirection, swordState);
        int tile0 = romTables.swordSpriteTile(swordDir, 0);
        int tile1 = romTables.swordSpriteTile(swordDir, 1);
        int attr0 = romTables.swordSpriteAttr(swordDir, 0);
        int attr1 = romTables.swordSpriteAttr(swordDir, 1);
        if (tile0 != 0xFF) {
            draw8x16Alpha(buffer, spriteSheet, tile0, bladeX, bladeY, attr0);
        }
        if (tile1 != 0xFF) {
            draw8x16Alpha(buffer, spriteSheet, tile1, bladeX + 8, bladeY, attr1);
        }
    }

    private static void draw8x16Alpha(byte[] buffer, SwordSpriteSheet spriteSheet,
                                      int tileIndex, int screenX, int screenY, int attrs) {
        boolean flipX = (attrs & 0x20) != 0;
        boolean flipY = (attrs & 0x40) != 0;
        drawTileAlpha(buffer, spriteSheet.tile(flipY ? tileIndex + 1 : tileIndex), screenX, screenY, flipX, flipY);
        drawTileAlpha(buffer, spriteSheet.tile(flipY ? tileIndex : tileIndex + 1), screenX, screenY + 8, flipX, flipY);
    }

    private static void drawTileAlpha(byte[] buffer, linksawakening.gpu.Tile tile,
                                      int screenX, int screenY, boolean flipX, boolean flipY) {
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sx = flipX ? 7 - tx : tx;
                int sy = flipY ? 7 - ty : ty;
                if (tile.getPixel(sx, sy) == 0) {
                    continue;
                }
                buffer[(py * Framebuffer.WIDTH + px) * 4 + 3] = (byte) 0xFF;
            }
        }
    }

    private static int toRomDirection(int javaDirection) {
        switch (javaDirection) {
            case Link.DIRECTION_RIGHT: return 0;
            case Link.DIRECTION_LEFT:  return 1;
            case Link.DIRECTION_UP:    return 2;
            case Link.DIRECTION_DOWN:  return 3;
            default: return 3;
        }
    }

    private static Sword fullyChargedSwordFacing(int direction) {
        Sword sword = new Sword(null, null);
        sword.onPress();
        sword.attackDirection(direction);
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }
        while (sword.charge() < Sword.MAX_CHARGE) {
            sword.tick(true);
        }
        sword.attackDirection(direction);
        return sword;
    }

    private static final class RecordingGameplaySoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }

    private static final class SequenceIntSupplier implements java.util.function.IntSupplier {
        private final int[] values;
        private int index;

        private SequenceIntSupplier(int... values) {
            this.values = values;
        }

        @Override
        public int getAsInt() {
            return values[Math.min(index++, values.length - 1)];
        }
    }
}
