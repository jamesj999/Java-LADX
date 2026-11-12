package linksawakening.app;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Shovel;
import linksawakening.state.InventoryItem;
import linksawakening.world.room.ShovelStartResult;

/** Registers the shovel and its room-facing adapter. */
final class ShovelWiring {
    private ShovelWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHOVEL.romValue(),
                new Shovel(
                        c.gameplaySoundSink(),
                        new Shovel.DigTarget() {
                            private int linkEntityX;
                            private int linkEntityY;
                            private int romDirection;

                            @Override
                            public Shovel.StartResult startShovel() {
                                romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                linkEntityX = c.link().position().romEntityX();
                                linkEntityY = c.link().position().romEntityY();
                                ShovelStartResult result = c.roomSession()
                                        .playerItems()
                                        .startShovel(
                                                linkEntityX,
                                                linkEntityY,
                                                romDirection,
                                                c.link().airborne().isAirborne());
                                return new Shovel.StartResult(result.started(), result.poking());
                            }

                            @Override
                            public void advanceShovel(int timer) {

                                c.roomSession()
                                        .playerItems()
                                        .advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
                            }

                            @Override
                            public int shovelAnimationState(int javaDirection, int timer) {
                                return c.roomSession().playerItems().shovelAnimationState(javaDirection, timer);
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
