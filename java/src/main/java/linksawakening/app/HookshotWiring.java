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
                                c.link().position().romEntityX(),
                                c.link().position().romEntityY(),
                                c.link().animation().romEntityZ(),
                                Direction.fromJava(c.link().animation().direction())
                                        .romIndex(),
                                c.link().airborne().isAirborne(),
                                c.link().equipment().isRomLinkPushing());
            }

            @Override
            public boolean hookshotActive() {
                return c.roomSession().playerItems().hookshotActive();
            }
        }));
    }
}
