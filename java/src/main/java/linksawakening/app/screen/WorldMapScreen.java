package linksawakening.app.screen;

import linksawakening.app.gameplay.interaction.DialogSoundRouting;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.state.GameplaySounds;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.render.WorldMapLayer;
import linksawakening.rom.Rom;
import linksawakening.world.room.RoomSession;

/** Modal Select map. Gameplay stays suspended while this screen owns the input edges. */
public final class WorldMapScreen implements Screen {
    private static final int INITIAL_REPEAT_DELAY = 24;
    private static final int REPEAT_INTERVAL = 3;

    private final Rom rom;
    private final RoomSession rooms;
    private final InputServices input;
    private final UiServices ui;
    private final GameplaySounds sounds;
    private final DialogController dialog;
    private final DialogSoundRouting dialogSounds;
    private final ScreenNavigator navigator;
    private final InputCompletion completion;
    private final linksawakening.app.state.ScreenState screens;
    private final SaveActions saves;
    private WorldMapLayer map;
    private int directionHeldFrames;

    public record Navigation(
            ScreenNavigator navigator,
            InputCompletion completion,
            linksawakening.app.state.ScreenState screens,
            SaveActions saves) {}

    public WorldMapScreen(
            Rom rom,
            RoomSession rooms,
            InputServices input,
            UiServices ui,
            GameplaySounds sounds,
            DialogController dialog,
            DialogSoundRouting dialogSounds,
            Navigation navigation) {
        this.rom = rom;
        this.rooms = rooms;
        this.input = input;
        this.ui = ui;
        this.sounds = sounds;
        this.dialog = dialog;
        this.dialogSounds = dialogSounds;
        this.navigator = navigation.navigator();
        this.completion = navigation.completion();
        this.screens = navigation.screens();
        this.saves = navigation.saves();
    }

    public void open() {
        if (map == null) map = new WorldMapLayer(rom);
        map.open(rooms.progress().worldMapRoom(), rooms.dungeon().overworldRoomStatusSnapshot());
        directionHeldFrames = 0;
        screens.menuTransition = new linksawakening.ui.MenuTransition(9);
        navigator.show(ScreenKind.WORLD_MAP);
        completion.complete();
    }

    @Override
    public void tick() {
        var transition = screens.menuTransition;
        if (!transition.transitionFinished()) {
            transition.tick();
            if (transition.transitionFinished()) {
                if (transition.returningToRoom()) {
                    screens.menuTransition = null;
                    navigator.show(ScreenKind.GAMEPLAY);
                } else {
                    sounds.sink().play(GameplaySoundEvent.MENU_VALIDATE);
                }
            }
            completion.complete();
            return;
        }
        map.tick();
        var keys = input.inputConfig();
        var buttons = input.inputState();
        boolean a = buttons.wasPressed(keys.aKey());
        boolean b = buttons.wasPressed(keys.bKey());
        if (dialog.isActive()) {
            if (a || b) dialog.advance();
            dialog.tick();
            dialogSounds.route();
        } else if (linksawakening.app.input.InputRouter.shouldEnterFileSave(
                keys, buttons, keys.selectKey(), linksawakening.input.KeyCodes.PRESS)) {
            saves.startFileSave();
        } else if (b || buttons.wasPressed(keys.selectKey())) {
            transition.close();
        } else if (a && map.locationDialogIndex() >= 0) {
            var ref = new SignpostDialogRef(map.locationDialogTable(), map.locationDialogIndex());
            var position =
                    map.cursorRoom() < 0x70 ? DialogController.BoxPosition.BOTTOM : DialogController.BoxPosition.TOP;
            dialog.openPreformatted(ui.dialogTextLoader().load(ref), position);
        } else if (map.isCursorMovementReady()) {
            moveCursor();
        }
        completion.complete();
    }

    private void moveCursor() {
        var keys = input.inputConfig();
        var buttons = input.inputState();
        boolean held = buttons.isDown(keys.rightKey())
                || buttons.isDown(keys.leftKey())
                || buttons.isDown(keys.downKey())
                || buttons.isDown(keys.upKey());
        directionHeldFrames = held ? directionHeldFrames + 1 : 0;
        boolean repeat = directionHeldFrames == INITIAL_REPEAT_DELAY;
        if (repeat) directionHeldFrames -= REPEAT_INTERVAL;
        int dx = direction(keys.rightKey(), keys.leftKey(), repeat);
        int dy = direction(keys.downKey(), keys.upKey(), repeat);
        if (dx != 0 || dy != 0) {
            sounds.sink().play(map.moveCursor(dx, dy) ? GameplaySoundEvent.MENU_MOVE : GameplaySoundEvent.ENEMY_BUMP);
        }
    }

    private int direction(int positive, int negative, boolean repeat) {
        var buttons = input.inputState();
        boolean plus = repeat ? buttons.isDown(positive) : buttons.wasPressed(positive);
        boolean minus = repeat ? buttons.isDown(negative) : buttons.wasPressed(negative);
        return (plus ? 1 : 0) - (minus ? 1 : 0);
    }

    @Override
    public void onKey(int key, int action) {}

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.WORLD_MAP;
    }

    @Override
    public GameFrameState renderState(GameFrameState state) {
        var transition = screens.menuTransition;
        map.configurePresentation(dialog, transition.transitionFinished() || transition.returningToRoom());
        return state.withWorldMap(map);
    }
}
