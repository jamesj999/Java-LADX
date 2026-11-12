package linksawakening.app.services;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.PlayerState;

/** Long-lived player services constructed before the first frame. */
public record PlayerServices(
        PlayerState playerState, Link link, ItemRegistry itemRegistry, EquipmentController equipmentController) {
    public PlayerServices {
        Objects.requireNonNull(playerState);
        Objects.requireNonNull(link);
        Objects.requireNonNull(itemRegistry);
        Objects.requireNonNull(equipmentController);
    }
}
