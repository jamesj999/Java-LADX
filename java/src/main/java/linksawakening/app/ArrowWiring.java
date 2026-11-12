package linksawakening.app;

import linksawakening.equipment.Arrow;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.InventoryItem;
import linksawakening.state.PowerUp;
import linksawakening.world.room.ArrowShotResult;

/** Registers the arrow and its room-facing adapter. */
final class ArrowWiring {
    private ArrowWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOW.romValue(),
                new Arrow(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new Arrow.ShootTarget() {
                            private boolean playWhooshForLastShot;

                            @Override
                            public boolean shootArrow() {
                                ArrowShotResult result = c.roomSession()
                                        .playerItems()
                                        .shootArrowResult(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                c.link().equipment().applyRomItemDirectionFromInput(),
                                                c.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                playWhooshForLastShot = result.playWhoosh();
                                if (result.spawned()) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return result.spawned();
                            }

                            @Override
                            public boolean playWhooshForLastShot() {
                                return playWhooshForLastShot;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
