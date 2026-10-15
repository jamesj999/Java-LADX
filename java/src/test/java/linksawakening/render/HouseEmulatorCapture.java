package linksawakening.render;

import linksawakening.entity.Link;
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

/** Offline opening scene through production room, entity, bed and HUD rendering. */
public final class HouseEmulatorCapture {
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        byte[] rom;
        try (var in = HouseEmulatorCapture.class.getResourceAsStream("/rom/azle.gbc")) {
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
        var motion = new MarinWakeUpMotion();
        var marinSource = session.activeRoom().entities().slots().get(0);
        var clocks = Files.readAllLines(out.resolve("clocks.tsv"));
        String[] first = clocks.get(0).split("\t");
        int firstFrame = Integer.parseInt(first[0]);
        int slowCountdown = Integer.parseInt(first[1]);
        // The emulator screenshot trails the observed entity clocks by two frames
        // (OAM buffer -> DMA -> displayed frame). Apply this fixed pipeline offset once.
        int inertia = Integer.parseInt(first[2]);
        // Synchronize once after the ROM fade-in. Never copy VRAM/OAM or per-frame variants.
        for (int i = 127; i > slowCountdown; i--) motion.tick(0, false, false);
        var marin = motion.tickMarinPresentation(1, marinSource.x(), marinSource.y(), 0x38, 0x34);
        for (int i = inertia - 2; i > 0; i--) {
            marin = motion.tickMarinPresentation((firstFrame - i + 1) & 255,
                marinSource.x(), marinSource.y(), 0x38, 0x34);
        }
        session.updateHouseTarinPresentation(0, 0x38, 0x34);
        var inventory = new InventoryController(input, config,
            new InventoryMenu(InventoryTilemapLoader.loadFromRom(rom), player));
        Path folder = out.resolve("ours");
        Files.createDirectories(folder);
        for (int f = 0; f < clocks.size(); f++) {
            int frame = (firstFrame + f) & 255;
            var wake = motion.tick(f == 0 ? 1 : (frame - 2) & 255, false, false);
            if (f > 0) {
                marin = motion.tickMarinPresentation(frame, marinSource.x(), marinSource.y(), 0x38, 0x34);
            }
            session.applyMarinWakeUpPresentation(marin.romX(), marin.romY(), marin.spriteVariant());
            session.updateHouseTarinPresentation(frame, 0x38, 0x34);
            link.showMarinWakeUpBed(wake.bedSpriteVariant());
            byte[] pixels = new byte[160 * 144 * 4];
            new GameFrameSceneBuilder().build(GameFrameState.empty().withScreen(RenderScreen.OVERWORLD)
                .withRoom(session.renderSnapshot(), new ScrollController(), new TransitionController())
                .withLink(link).withInventoryController(inventory).withFrameCounter(frame)).drawTo(pixels, gpu);
            BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 144; y++) for (int x = 0; x < 160; x++) {
                int i = (y * 160 + x) * 4;
                image.setRGB(x, y, 0xff000000 | ((pixels[i] & 255) << 16)
                    | ((pixels[i + 1] & 255) << 8) | (pixels[i + 2] & 255));
            }
            ImageIO.write(image, "png", folder.resolve(String.format("%03d.png", f)).toFile());
        }
    }
}
