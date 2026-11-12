package linksawakening.app;

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

/** Creates Link and registers equipment in source order. */
final class PlayerWiring {
    private PlayerWiring() {}

    record PlayerWorld(RoomSession roomSession, OverworldCollision overworldCollision) {}

    static PlayerGraphics graphics(Rom rom) {
        return new PlayerGraphics(
                LinkSpriteSheet.loadFromRom(rom.unsafeBytes()),
                SwordSpriteSheet.loadFromRom(rom.unsafeBytes()),
                SwordPalette.loadFromRom(rom.unsafeBytes()),
                LinkTunicPalette.loadFromRom(rom.unsafeBytes()));
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
                playerState,
                itemRegistry,
                gameplaySoundSink,
                linkTunicPalette);
        link.setShieldInputAllowed(() -> !GameplayDialogInput.blocksGameplay(dialogController)
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
        OcarinaWiring.register(c, registry);
        SwordWiring.register(c, registry);
        MagicRodWiring.register(c, registry);
        MagicPowderWiring.register(c, registry);
        ShieldWiring.register(c, registry);
        ShovelWiring.register(c, registry);
        FeatherWiring.register(c, registry);
        BombWiring.register(c, registry);
        ArrowWiring.register(c, registry);
        HookshotWiring.register(c, registry);
        BoomerangWiring.register(c, registry);
    }
}
