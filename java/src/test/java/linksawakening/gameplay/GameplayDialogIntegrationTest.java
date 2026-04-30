package linksawakening.gameplay;

import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.input.InputConfig;
import linksawakening.world.Warp;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static linksawakening.world.RoomConstants.ROOM_OBJECTS_AREA_SIZE;
import static linksawakening.world.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.RoomConstants.ROOM_OBJECT_ROW_STRIDE;
import static linksawakening.rom.RomBank.romOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

final class GameplayDialogIntegrationTest {

    private static final int OBJECT_SIGNPOST = 0xD4;

    @Test
    void activeOverworldDialogConsumesAdvanceKeysBeforeOtherGameplayInput() {
        InputConfig inputConfig = new InputConfig(GLFW_KEY_ENTER, 1, 2, 3, 4, GLFW_KEY_Z, GLFW_KEY_X);

        assertAdvanceKeyConsumed(inputConfig, GLFW_KEY_Z);
        assertAdvanceKeyConsumed(inputConfig, GLFW_KEY_X);
        assertAdvanceKeyConsumed(inputConfig, GLFW_KEY_ENTER);
    }

    @Test
    void activeDialogBlocksLinkMovementAndActions() {
        DialogController dialog = new DialogController(16);

        assertFalse(GameplayDialogInput.blocksGameplay(dialog));
        assertFalse(OverworldDialogBlockers.none().pausesGameplay());

        dialog.open("READ THIS");

        assertTrue(GameplayDialogInput.blocksGameplay(dialog));
        assertTrue(new OverworldDialogBlockers(false, false, false, true, false).pausesGameplay());
    }

    @Test
    void consumedDialogAdvancePausesGameplayForRestOfFrameEvenAfterDialogCloses() {
        DialogController dialog = new DialogController(16);
        dialog.open("A");
        dialog.advance();
        assertTrue(dialog.isActive());

        boolean consumed = GameplayDialogInput.handleOverworldKeyPress(GLFW_KEY_Z, GLFW_PRESS,
            new InputConfig(GLFW_KEY_ENTER, 1, 2, 3, 4, GLFW_KEY_Z, GLFW_KEY_X), dialog);

        assertTrue(consumed);
        assertFalse(dialog.isActive());
        assertTrue(new OverworldDialogBlockers(false, false, false, false, consumed).pausesGameplay());
    }

    @Test
    void signpostOpeningUsesActionButtonsNotEnter() {
        InputConfig inputConfig = new InputConfig(GLFW_KEY_ENTER, 1, 2, 3, 4, GLFW_KEY_Z, GLFW_KEY_X);

        assertTrue(GameplayDialogInput.isActionButtonKey(GLFW_KEY_Z, inputConfig));
        assertTrue(GameplayDialogInput.isActionButtonKey(GLFW_KEY_X, inputConfig));
        assertFalse(GameplayDialogInput.isActionButtonKey(GLFW_KEY_ENTER, inputConfig));
    }

    @Test
    void signpostDialogTableIsLoadedFromRomAndSelectsDialogTableLikeBank0() throws Exception {
        SignpostDialogTable table = SignpostDialogTable.loadFromRom(loadRom());

        assertEquals(new SignpostDialogRef(2, 0x2D), table.resolve(0x1E));
        assertEquals(new SignpostDialogRef(0, 0x83), table.resolve(0xA4));
        assertEquals(new SignpostDialogRef(1, 0xB6), table.resolve(0x11));
    }

    @Test
    void dialogTextLoaderDecodesSignpostTextFromRomPointersAndBanks() throws Exception {
        DialogTextLoader loader = DialogTextLoader.loadFromRom(loadRom());

        assertEquals("Telephone Booth \u00F1 Signpost Maze \u00FF", loader.load(new SignpostDialogRef(0, 0x83)));
        assertEquals("You are near theEagle's Tower.  Beware of the   bird!\u00FF",
            loader.load(new SignpostDialogRef(2, 0x2D)));
        assertEquals("MUSIC, THE FISH STIRS IN THE EGGYOU ARE THERE...\u00FF",
            loader.load(new SignpostDialogRef(1, 0xB6)));
    }

    @Test
    void dialogTextLoaderPreservesLowerSixBankBitsWhenHighBitFlagsAreSet() {
        byte[] rom = new byte[romOffset(0x21, 0x4000) + 3];
        int pointerOffset = romOffset(0x1C, 0x4001);
        int bankOffset = romOffset(0x1C, 0x4741);
        rom[pointerOffset] = 0x00;
        rom[pointerOffset + 1] = 0x40;
        rom[bankOffset] = (byte) 0xA1;
        rom[romOffset(0x01, 0x4000)] = 'B';
        rom[romOffset(0x01, 0x4001)] = (byte) 0xFF;
        rom[romOffset(0x21, 0x4000)] = 'O';
        rom[romOffset(0x21, 0x4001)] = 'K';
        rom[romOffset(0x21, 0x4002)] = (byte) 0xFF;

        DialogTextLoader loader = DialogTextLoader.loadFromRom(rom);

        assertEquals("OK\u00FF", loader.load(new SignpostDialogRef(0, 0x00)));
    }

    @Test
    void pressingActionFacingUpTowardSignpostOpensResolvedDialog() throws Exception {
        SignpostDialogTable table = SignpostDialogTable.loadFromRom(loadRom());
        OverworldDialogInteraction interaction = new OverworldDialogInteraction(table, DialogTextLoader.loadFromRom(loadRom()));
        DialogController dialog = new DialogController(16);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        roomObjectsArea[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_SIGNPOST;

        boolean opened = interaction.tryOpenSignpostDialog(
            0xA4,
            Warp.CATEGORY_OVERWORLD,
            roomObjectsArea,
            4 * 16,
            3 * 16,
            Link.DIRECTION_UP,
            true,
            OverworldDialogBlockers.none(),
            dialog);

        assertTrue(opened);
        assertTrue(dialog.isActive());
        dialog.advance();
        assertFalse(dialog.visibleText().startsWith("DIALOG"));
        assertEquals("Telephone Booth \n\u00F1 Signpost Maze ", dialog.visibleText());
    }

    @Test
    void signpostDialogOpeningUsesPreformattedRomRows() throws Exception {
        OverworldDialogInteraction interaction = new OverworldDialogInteraction(
            SignpostDialogTable.loadFromRom(loadRom()), DialogTextLoader.loadFromRom(loadRom()));
        DialogController dialog = new DialogController(16);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        roomObjectsArea[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_SIGNPOST;

        boolean opened = interaction.tryOpenSignpostDialog(
            0x1E,
            Warp.CATEGORY_OVERWORLD,
            roomObjectsArea,
            4 * 16,
            3 * 16,
            Link.DIRECTION_UP,
            true,
            OverworldDialogBlockers.none(),
            dialog);

        assertTrue(opened);
        dialog.advance();
        assertEquals("You are near the\nEagle's Tower.  ", dialog.visibleText());
    }

    @Test
    void signpostDialogOpeningChoosesBoxPositionFromLinkY() throws Exception {
        OverworldDialogInteraction interaction = new OverworldDialogInteraction(
            SignpostDialogTable.loadFromRom(loadRom()), DialogTextLoader.loadFromRom(loadRom()));
        int[] roomObjectsArea = emptyRoomObjectsArea();
        roomObjectsArea[ROOM_OBJECTS_BASE + 3 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_SIGNPOST;

        DialogController upperLinkDialog = new DialogController(16);
        assertTrue(interaction.tryOpenSignpostDialog(0xA4, Warp.CATEGORY_OVERWORLD,
            roomObjectsArea, 4 * 16, 0x37, Link.DIRECTION_UP, true, OverworldDialogBlockers.none(),
            upperLinkDialog));
        assertEquals(DialogController.BoxPosition.BOTTOM, upperLinkDialog.boxPosition());

        DialogController lowerLinkDialog = new DialogController(16);
        assertTrue(interaction.tryOpenSignpostDialog(0xA4, Warp.CATEGORY_OVERWORLD,
            roomObjectsArea, 4 * 16, 0x38, Link.DIRECTION_UP, true, OverworldDialogBlockers.none(),
            lowerLinkDialog));
        assertEquals(DialogController.BoxPosition.TOP, lowerLinkDialog.boxPosition());
    }

    @Test
    void linkTopLeftYConvertsToHardwareDialogYAtSpriteBottom() {
        assertEquals(0x47, LinkDialogPosition.dialogYFromTopLeft(0x37));
        assertEquals(0x48, LinkDialogPosition.dialogYFromTopLeft(0x38));
    }

    @Test
    void dialogSoundRouterConsumesAndRoutesControllerEvents() {
        DialogController dialog = new DialogController(16);
        dialog.openPreformatted("AB");
        dialog.tick();
        dialog.tick();
        RecordingDialogSoundSink sink = new RecordingDialogSoundSink();

        DialogSoundRouter.routePending(dialog, sink);

        assertEquals(List.of(DialogController.SoundEvent.TEXT_PRINT), sink.events);
        assertEquals(List.of(), dialog.consumeSoundEvents());
    }

    @Test
    void sfxDialogSoundSinkRendersAudiblePcmToOutput() {
        RecordingPcmSoundOutput output = new RecordingPcmSoundOutput();
        SfxDialogSoundSink sink = new SfxDialogSoundSink(output);

        sink.play(DialogController.SoundEvent.TEXT_PRINT);

        assertEquals(1, output.buffers.size());
        assertEquals(44_100, output.sampleRates.getFirst());
        assertTrue(hasNonZeroSample(output.buffers.getFirst()));
    }

    @Test
    void signpostInteractionRequiresOverworldActionAndUpFacingSignpost() throws Exception {
        OverworldDialogInteraction interaction = new OverworldDialogInteraction(
            SignpostDialogTable.loadFromRom(loadRom()), DialogTextLoader.loadFromRom(loadRom()));
        int[] roomObjectsArea = emptyRoomObjectsArea();
        roomObjectsArea[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_SIGNPOST;

        assertFalse(interaction.tryOpenSignpostDialog(0xA4, Warp.CATEGORY_OVERWORLD,
            roomObjectsArea, 4 * 16, 3 * 16, Link.DIRECTION_UP, false, OverworldDialogBlockers.none(),
            new DialogController(16)));
        assertFalse(interaction.tryOpenSignpostDialog(0xA4, Warp.CATEGORY_OVERWORLD,
            roomObjectsArea, 4 * 16, 3 * 16, Link.DIRECTION_DOWN, true, OverworldDialogBlockers.none(),
            new DialogController(16)));
        assertFalse(interaction.tryOpenSignpostDialog(0xA4, Warp.CATEGORY_INDOOR,
            roomObjectsArea, 4 * 16, 3 * 16, Link.DIRECTION_UP, true, OverworldDialogBlockers.none(),
            new DialogController(16)));
    }

    @Test
    void signpostInteractionRespectsInventoryScrollTransitionAndDialogBlockers() throws Exception {
        OverworldDialogInteraction interaction = new OverworldDialogInteraction(
            SignpostDialogTable.loadFromRom(loadRom()), DialogTextLoader.loadFromRom(loadRom()));
        int[] roomObjectsArea = emptyRoomObjectsArea();
        roomObjectsArea[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_SIGNPOST;

        assertSignpostBlocked(interaction, roomObjectsArea, new OverworldDialogBlockers(true, false, false, false, false));
        assertSignpostBlocked(interaction, roomObjectsArea, new OverworldDialogBlockers(false, true, false, false, false));
        assertSignpostBlocked(interaction, roomObjectsArea, new OverworldDialogBlockers(false, false, true, false, false));
        assertSignpostBlocked(interaction, roomObjectsArea, new OverworldDialogBlockers(false, false, false, true, false));
    }

    private static void assertAdvanceKeyConsumed(InputConfig inputConfig, int key) {
        DialogController dialog = new DialogController(16);
        dialog.open("HELLO");

        boolean consumed = GameplayDialogInput.handleOverworldKeyPress(key, GLFW_PRESS, inputConfig, dialog);

        assertTrue(consumed);
        assertTrue(dialog.isActive());
        assertEquals("HELLO", dialog.visibleText());
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[ROOM_OBJECTS_AREA_SIZE];
        for (int i = 0; i < roomObjectsArea.length; i++) {
            roomObjectsArea[i] = 0xFF;
        }
        return roomObjectsArea;
    }

    private static void assertSignpostBlocked(OverworldDialogInteraction interaction,
                                              int[] roomObjectsArea,
                                              OverworldDialogBlockers blockers) {
        assertFalse(interaction.tryOpenSignpostDialog(0xA4, Warp.CATEGORY_OVERWORLD,
            roomObjectsArea, 4 * 16, 3 * 16, Link.DIRECTION_UP, true, blockers, new DialogController(16)));
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = GameplayDialogIntegrationTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }

    private static final class RecordingDialogSoundSink implements DialogSoundSink {
        private final List<DialogController.SoundEvent> events = new ArrayList<>();

        @Override
        public void play(DialogController.SoundEvent event) {
            events.add(event);
        }
    }

    private static final class RecordingPcmSoundOutput implements PcmSoundOutput {
        private final List<short[]> buffers = new ArrayList<>();
        private final List<Integer> sampleRates = new ArrayList<>();

        @Override
        public void play(short[] stereoPcm, int sampleRate) {
            buffers.add(stereoPcm);
            sampleRates.add(sampleRate);
        }
    }

    private static boolean hasNonZeroSample(short[] pcm) {
        for (short sample : pcm) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }
}
