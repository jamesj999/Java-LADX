package linksawakening.app.wiring;

import java.util.Objects;
import linksawakening.app.services.AudioServices;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.RomServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.wiring.EquipmentWiring.EquipmentInputs;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.entity.LinkTunicPalette;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.SwordPalette;
import linksawakening.equipment.SwordSpriteSheet;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.room.RoomSession;

/**
 * Creates Link and registers equipment in source order. Stateless factory used during session
 * initialization.
 */
final class PlayerWiring {
    private PlayerWiring() {}

    record PlayerWorld(RoomSession roomSession, OverworldCollision overworldCollision) {}

    static PlayerGraphics graphics(Rom rom) {
        return new PlayerGraphics(
                LinkSpriteSheet.loadFromRom(rom),
                SwordSpriteSheet.loadFromRom(rom),
                SwordPalette.loadFromRom(rom),
                LinkTunicPalette.loadFromRom(rom));
    }

    static PlayerServices build(
            PlayerState playerState,
            InputServices input,
            RomServices rom,
            PlayerWorld world,
            UiServices ui,
            AudioServices audio,
            PlayerGraphics graphics,
            RomRandomByteSource random) {
        var inputState = input.inputState();
        var inputConfig = input.inputConfig();
        var romTables = rom.romTables();
        var overworldCollision = world.overworldCollision();
        var gameplaySoundSink = audio.gameplaySoundSink();
        var roomSession = world.roomSession();
        var dialogController = ui.dialogController();
        var itemRegistry = new ItemRegistry();
        LinkTunicPalette linkTunicPalette = graphics.linkTunicPalette();
        Link link = new Link(
                inputState,
                inputConfig,
                romTables,
                overworldCollision,
                graphics.linkSpriteSheet(),
                new Link.Services(playerState, itemRegistry, gameplaySoundSink, linkTunicPalette));
        link.equipment()
                .setShieldInputAllowed(() -> !GameplayDialogInput.blocksGameplay(dialogController)
                        && (!roomSession.entityState().powerUpPickupSequenceActive()
                                && !roomSession.rooms().heartPiecePickupActive()));

        var c = new EquipmentInputs(playerState, link, roomSession, romTables, graphics, gameplaySoundSink, random);
        register(c, itemRegistry);
        return new PlayerServices(
                playerState,
                link,
                itemRegistry,
                new EquipmentController(inputState, inputConfig, playerState, itemRegistry));
    }

    private static void register(EquipmentInputs c, ItemRegistry registry) {
        ToolEquipment.ocarina(c, registry);
        CombatEquipment.sword(c, registry);
        CombatEquipment.magicRod(c, registry);
        CombatEquipment.magicPowder(c, registry);
        EquipmentWiring.shield(c, registry);
        ToolEquipment.shovel(c, registry);
        EquipmentWiring.feather(c, registry);
        EquipmentWiring.bomb(c, registry);
        CombatEquipment.arrow(c, registry);
        ToolEquipment.hookshot(c, registry);
        ToolEquipment.boomerang(c, registry);
    }

    /** ROM-backed player and equipment graphics loaded during wiring. */
    record PlayerGraphics(
            LinkSpriteSheet linkSpriteSheet,
            SwordSpriteSheet swordSpriteSheet,
            SwordPalette swordPalette,
            LinkTunicPalette linkTunicPalette) {
        PlayerGraphics {
            Objects.requireNonNull(linkSpriteSheet);
            Objects.requireNonNull(swordSpriteSheet);
            Objects.requireNonNull(swordPalette);
            Objects.requireNonNull(linkTunicPalette);
        }
    }
}
