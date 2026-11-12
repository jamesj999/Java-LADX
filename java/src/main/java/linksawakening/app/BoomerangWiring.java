package linksawakening.app;

import linksawakening.equipment.Boomerang;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.InventoryItem;

/** Registers the boomerang and its room-facing adapter. */
final class BoomerangWiring {
    private BoomerangWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOOMERANG.romValue(),
                new Boomerang(
                        new Boomerang.LaunchTarget() {
                            @Override
                            public boolean fireBoomerang() {
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireBoomerang(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                c.link().equipment().applyRomItemDirectionFromInput(),
                                                c.link().controls().romPressedButtonsMask());
                                if (spawned) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return spawned;
                            }

                            @Override
                            public boolean boomerangActive() {
                                return c.roomSession().playerItems().boomerangActive();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
