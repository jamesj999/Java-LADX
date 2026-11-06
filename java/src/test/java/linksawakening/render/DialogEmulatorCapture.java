package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryTilemapLoader;
import linksawakening.world.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.io.UncheckedIOException;

/** Settled wake-dialog pages; production rendering, with one initial NPC clock synchronization. */
public final class DialogEmulatorCapture {
    public static void main(String[] args) throws Exception {
        String scenario = args.length > 1 ? args[1] : "marin";
        Path out = "marin".equals(scenario) ? Path.of(args[0]) : Path.of(args[0]).resolve(scenario);
        Path folder = out.resolve("ours");
        Files.createDirectories(folder);
        capture(scenario, Files.readString(out.resolve("initial.tsv")),
            Files.readAllLines(out.resolve("frames.tsv")), (name, pixels) -> {
                BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < 144; y++) for (int x = 0; x < 160; x++) {
                    int i = (y * 160 + x) * 4;
                    image.setRGB(x, y, 0xff000000 | ((pixels[i] & 255) << 16)
                        | ((pixels[i + 1] & 255) << 8) | (pixels[i + 2] & 255));
                }
                try {
                    ImageIO.write(image, "png", folder.resolve(name).toFile());
                } catch (java.io.IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    static void capture(String initialClocks, List<String> frames,
                        BiConsumer<String, byte[]> consumer) throws Exception {
        capture("marin", initialClocks, frames, consumer);
    }

    static void capture(String scenario, String initialClocks, List<String> frames,
                        BiConsumer<String, byte[]> consumer) throws Exception {
        boolean timedDialog = "marin-timing".equals(scenario);
        boolean bedExit = "bed-exit".equals(scenario);
        boolean inBed = "marin".equals(scenario) || bedExit || timedDialog;
        boolean heldShield = "shield-pose".equals(scenario);
        boolean receivedShield = "tarin-shield".equals(scenario);
        int linkX = inBed ? 0x38 : 105;
        int linkY = inBed ? 0x34 : 72;
        byte[] rom;
        try (var in = DialogEmulatorCapture.class.getResourceAsStream("/rom/azle.gbc")) {
            rom = in.readAllBytes();
        }
        GPU gpu = new GPU();
        gpu.loadBaseTiles(rom);
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        var session = new RoomSession(rom, gpu, new RoomLoader(rom),
            new OverworldTilesetTable(rom), collision, null, null);
        var profile = NewGameStartProfile.romDefaults();
        session.loadIndoor(profile.mapId(), profile.roomId());
        var player = new PlayerState();
        profile.initializePlayerState(player);
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var link = new Link(input, config, tables, collision,
            LinkSpriteSheet.loadFromRom(rom), player, new ItemRegistry());
        link.setPixelPosition(linkX - 8, linkY - 16);
        link.setRomDirection(0);
        if (heldShield) link.showTarinShieldPresentation(
            linksawakening.rom.RomBank.loadPalettes(rom, 0x05, 0x4C94, 1)[0]);
        if (receivedShield) {
            player.applyChestReward(ChestContentsTable.CHEST_SHIELD);
            link.showStandingShieldDownPose();
        }
        var motion = new MarinWakeUpMotion();
        var marinSource = session.activeRoom().entities().slots().get(0);
        String[] initial = initialClocks.trim().split("\\t");
        int firstFrame = Integer.parseInt(initial[0]);
        int inertia = Integer.parseInt(initial[1]);
        // One read-only initial synchronization, then continuous NPC ticks.
        // As with captureHouse, displayed OAM trails the observed clocks by two frames.
        var marin = motion.tickMarinPresentation(1, marinSource.x(), marinSource.y(), linkX, linkY);
        for (int i = inertia - 2; i > 0; i--) {
            marin = motion.tickMarinPresentation((firstFrame - i + 1) & 255,
                marinSource.x(), marinSource.y(), linkX, linkY);
        }
        var dialog = DialogController.forGameplay();
        dialog.setPlayerName("A");
        if (!heldShield && !bedExit) dialog.openPreformattedForLinkY(DialogTextLoader.loadFromRom(rom)
            .load(new SignpostDialogRef(0, inBed ? 1 : receivedShield ? 0x91 : 0x54)), linkY);
        int page = -1;
        int elapsed = 0;
        session.updateHouseTarinPresentation(0, linkX, linkY);
        var inventory = new InventoryController(input, config,
            new InventoryMenu(InventoryTilemapLoader.loadFromRom(rom), player));
        for (String row : frames) {
            String[] fields = row.split("\\t");
            int nextPage = Integer.parseInt(fields[0]);
            int f = Integer.parseInt(fields[1]);
            int targetElapsed = Integer.parseInt(fields[2]);
            int frame = (firstFrame + targetElapsed) & 255;
            // Match settled pages, not their opening/typewriter/scroll timing.
            if (timedDialog && targetElapsed > 0) dialog.tick();
            if (!timedDialog && nextPage != page) {
                if (page >= 0) dialog.advance();
                for (int i = 0; i < 128; i++) dialog.tick();
                page = nextPage;
            }
            while (elapsed < targetElapsed) {
                elapsed++;
                session.updateHouseTarinPresentation((firstFrame + elapsed) & 255, linkX, linkY);
                marin = motion.tickMarinPresentation((firstFrame + elapsed) & 255,
                    marinSource.x(), marinSource.y(), linkX, linkY);
            }
            session.applyMarinWakeUpPresentation(marin.romX(), marin.romY(), marin.spriteVariant());
            session.updateHouseTarinPresentation(frame, linkX, linkY);
            if (bedExit) {
                if (targetElapsed < 4) link.showMarinWakeUpBed(3);
                else {
                    if (targetElapsed == 4) link.leaveMarinWakeUpBed();
                    link.update();
                }
            } else if (inBed) link.showMarinWakeUpBed(3);
            byte[] pixels = new byte[160 * 144 * 4];
            new GameFrameSceneBuilder().build(GameFrameState.empty().withScreen(RenderScreen.OVERWORLD)
                .withRoom(session.renderSnapshot(), new ScrollController(), new TransitionController())
                .withLink(link).withInventoryController(inventory).withDialogController(dialog).withFrameCounter((frame - 2) & 255)).drawTo(pixels, gpu);
            consumer.accept(String.format("%02d-%02d.png", nextPage, f), pixels);
        }
    }
}
