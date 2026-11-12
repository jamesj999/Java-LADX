package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.screen.SaveActions;
import linksawakening.equipment.EquipmentController;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;

/** Owns the source-ordered game-over resets around save/respawn selection. */
public final class GameOverFlow implements GameOverActions {
    private final EquipmentController equipment;
    private final PassOutController passOut;
    private final SaveActions saves;
    private final TransientVfxSystem effects;
    private final DroppableRupeeSystem rupees;

    public GameOverFlow(
            EquipmentController equipment,
            PassOutController passOut,
            SaveActions saves,
            TransientVfxSystem effects,
            DroppableRupeeSystem rupees) {
        this.equipment = Objects.requireNonNull(equipment);
        this.passOut = Objects.requireNonNull(passOut);
        this.saves = Objects.requireNonNull(saves);
        this.effects = Objects.requireNonNull(effects);
        this.rupees = Objects.requireNonNull(rupees);
    }

    public void finishGameOver(int option) {
        var sword = equipment.activeSword();
        if (sword != null) sword.resetAfterGameOver();
        passOut.clear();
        saves.finishGameOver(option);
        effects.clear();
        rupees.clear();
    }
}
