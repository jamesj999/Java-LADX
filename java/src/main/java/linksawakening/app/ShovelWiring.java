package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

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
                                if (c.link() == null || c.roomSession() == null) {
                                    return new Shovel.StartResult(false, true);
                                }
                                romDirection = c.link().applyRomItemDirectionFromInput();
                                linkEntityX = c.link().romEntityX();
                                linkEntityY = c.link().romEntityY();
                                ShovelStartResult result = c.roomSession()
                                        .playerItems()
                                        .startShovel(
                                                linkEntityX,
                                                linkEntityY,
                                                romDirection,
                                                c.link().isAirborne());
                                return new Shovel.StartResult(result.started(), result.poking());
                            }

                            @Override
                            public void advanceShovel(int timer) {
                                if (c.roomSession() != null) {
                                    c.roomSession()
                                            .playerItems()
                                            .advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
                                }
                            }

                            @Override
                            public int shovelAnimationState(int javaDirection, int timer) {
                                return c.roomSession() == null
                                        ? -1
                                        : c.roomSession().playerItems().shovelAnimationState(javaDirection, timer);
                            }
                        },
                        c.link()::canUseItems));
    }
}
