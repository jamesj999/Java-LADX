package linksawakening.app;

import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;

/** Registers the hookshot and its room-facing adapter. */
final class HookshotWiring {
    private HookshotWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(InventoryItem.HOOKSHOT.romValue(), new Hookshot(new Hookshot.LaunchTarget() {
            @Override
            public boolean fireHookshot() {
                return c.roomSession()
                        .playerItems()
                        .fireHookshot(
                                c.link().romEntityX(),
                                c.link().romEntityY(),
                                c.link().romEntityZ(),
                                Direction.fromJava(c.link().direction()).romIndex(),
                                c.link().isAirborne(),
                                c.link().isRomLinkPushing());
            }

            @Override
            public boolean hookshotActive() {
                return c.roomSession().playerItems().hookshotActive();
            }
        }));
    }
}
