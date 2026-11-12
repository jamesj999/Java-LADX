package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.input.KeyCodes;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class LinkAllocationTest {
    @Test
    void walkingAndRepeatedSwordSwingsStayWithinAllocationBudget() {
        byte[] bytes = new byte[0x100000];
        Arrays.fill(
                bytes,
                RomSymbols.HorizontalIncrementForLinkPosition.offset(),
                RomSymbols.HorizontalIncrementForLinkPosition.offset() + 32,
                (byte) 16);
        Rom rom = Rom.of(bytes);
        RomTables tables = RomTables.load(rom);
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var player = new PlayerState();
        player.inventory().setItemA(InventoryItem.SWORD.romValue());
        var items = new ItemRegistry();
        var sword = new Sword(tables, null);
        items.register(InventoryItem.SWORD.romValue(), sword);
        var collision = new OverworldCollision(tables);
        collision.setRoom(new int[0x100]);
        Link link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(tables)
                .collision(collision)
                .sprites(LinkSpriteSheet.loadFromRom(rom))
                .player(player)
                .items(items)
                .build();
        input.onKeyEvent(config.rightKey(), KeyCodes.PRESS);
        int[] frames = {0};
        boolean[] observed = {false, false};
        double bytesPerFrame = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            link.position().setPixelPosition(32, 32);
            if ((frames[0]++ & 31) == 0) sword.onPress();
            sword.tick(false);
            observed[1] |= sword.state() != Sword.STATE_NONE;
            link.motion().update();
            observed[0] |= link.position().pixelX() != 32;
            input.tickEdges();
        });
        assertTrue(observed[0]);
        assertTrue(observed[1]);
        System.out.println("Link walking/sword bytes/frame=" + bytesPerFrame);
        assertTrue(bytesPerFrame <= 8, "bytes/frame=" + bytesPerFrame);
    }
}
