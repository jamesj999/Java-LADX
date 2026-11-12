package linksawakening.app;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicRod;
import linksawakening.state.InventoryItem;
import linksawakening.state.PowerUp;

/** Registers the magicrod and its room-facing adapter. */
final class MagicRodWiring {
    private MagicRodWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_ROD.romValue(),
                new MagicRod(
                        c.romTables(),
                        c.graphics().linkSpriteSheet(),
                        c.graphics().linkTunicPalette(),
                        c.gameplaySoundSink(),
                        new MagicRod.LaunchTarget() {
                            @Override
                            public void startMagicRodAttackStep() {

                                c.link().equipment().startRomMagicRodAttackStep();
                            }

                            @Override
                            public boolean fireMagicRodFireball() {
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireMagicRodFireball(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection,
                                                c.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
