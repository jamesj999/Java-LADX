package linksawakening.app;

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
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                return c.roomSession()
                                        .playerItems()
                                        .sprinkleMagicPowder(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection);
                            }

                            @Override
                            public void startMagicPowderAttackStep() {

                                c.link().equipment().startRomMagicPowderAttackStep();
                            }

                            @Override
                            public boolean magicPowderAttackStepActive() {
                                return c.link().equipment().romAttackStepAnimationCountdown() != 0;
                            }

                            @Override
                            public int linkPositionZ() {
                                return c.link().animation().romEntityZ();
                            }

                            @Override
                            public void presentToadstool() {
                                c.link().animation().presentToadstool();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
