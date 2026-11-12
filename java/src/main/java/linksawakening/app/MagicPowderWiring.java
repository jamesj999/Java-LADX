package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicPowder;
import linksawakening.state.InventoryItem;

/** Registers the magicpowder and its room-facing adapter. */
final class MagicPowderWiring {
    private MagicPowderWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_POWDER.romValue(),
                new MagicPowder(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new MagicPowder.SprinkleTarget() {
                            @Override
                            public boolean sprinkleMagicPowder() {
                                if (c.link() == null || c.roomSession() == null) {
                                    return false;
                                }
                                int romDirection = c.link().applyRomItemDirectionFromInput();
                                return c.roomSession()
                                        .playerItems()
                                        .sprinkleMagicPowder(
                                                c.link().romEntityX(),
                                                c.link().romEntityY(),
                                                c.link().romEntityZ(),
                                                romDirection);
                            }

                            @Override
                            public void startMagicPowderAttackStep() {
                                if (c.link() != null) {
                                    c.link().startRomMagicPowderAttackStep();
                                }
                            }

                            @Override
                            public boolean magicPowderAttackStepActive() {
                                return c.link() != null && c.link().romAttackStepAnimationCountdown() != 0;
                            }

                            @Override
                            public int linkPositionZ() {
                                return c.link().romEntityZ();
                            }

                            @Override
                            public void presentToadstool() {
                                c.link().presentToadstool();
                            }
                        },
                        c.link()::canUseItems));
    }
}
