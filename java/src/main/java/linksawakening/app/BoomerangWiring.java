package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Boomerang;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.PlayerState;

/** Registers the boomerang and its room-facing adapter. */
final class BoomerangWiring {
    private BoomerangWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                PlayerState.INVENTORY_BOOMERANG,
                new Boomerang(
                        new Boomerang.LaunchTarget() {
                            @Override
                            public boolean fireBoomerang() {
                                if (c.link() == null || c.roomSession() == null) {
                                    return false;
                                }
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireBoomerang(
                                                c.link().romEntityX(),
                                                c.link().romEntityY(),
                                                c.link().romEntityZ(),
                                                c.link().applyRomItemDirectionFromInput(),
                                                c.link().romPressedButtonsMask());
                                if (spawned) {
                                    c.link().startRomItemAttackStep();
                                }
                                return spawned;
                            }

                            @Override
                            public boolean boomerangActive() {
                                return c.roomSession() != null
                                        && c.roomSession().playerItems().boomerangActive();
                            }
                        },
                        c.link()::canUseItems));
    }
}
