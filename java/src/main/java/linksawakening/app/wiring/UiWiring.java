package linksawakening.app.wiring;

import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.services.AudioServices;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.UiServices;
import linksawakening.audio.music.FrameAudioPlayer;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.SignpostDialogTable;
import linksawakening.rom.Rom;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryTilemapLoader;

/** Creates inventory, dialog, and cutscene presentation before world entry. */
final class UiWiring {
    private UiWiring() {}

    static UiServices build(
            Rom romData, PlayerState playerState, InputServices input, AudioServices audio, ScreenNavigator navigator) {
        var inputState = input.inputState();
        var inputConfig = input.inputConfig();
        var gameplaySoundSink = audio.gameplaySoundSink();
        var musicPlayer = audio.musicPlayer();
        InventoryTilemapLoader tilemapLoader = InventoryTilemapLoader.loadFromRom(romData);
        InventoryMenu inventoryMenu = new InventoryMenu(tilemapLoader, playerState, gameplaySoundSink, romData);
        InventoryController inventoryController = new InventoryController(inputState, inputConfig, inventoryMenu);
        DialogController dialogController = DialogController.forGameplay();
        DialogTextLoader dialogTextLoader = DialogTextLoader.loadFromRom(romData);
        OverworldDialogInteraction overworldDialogInteraction =
                new OverworldDialogInteraction(SignpostDialogTable.loadFromRom(romData), dialogTextLoader);
        CutsceneManager cutsceneManager = new CutsceneManager(dialogController, navigator::setCutsceneScene);
        var introCatalog = SoundEffectCatalog.fromRom(romData);
        cutsceneManager.setIntroNoiseSound(id -> introCatalog
                .find(SoundEffectNamespace.NOISE, id)
                .ifPresent(((FrameAudioPlayer) musicPlayer)::playSoundEffect));
        return new UiServices(
                inventoryMenu,
                inventoryController,
                dialogController,
                dialogTextLoader,
                overworldDialogInteraction,
                cutsceneManager);
    }
}
