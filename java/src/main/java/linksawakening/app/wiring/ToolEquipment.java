package linksawakening.app.wiring;

import linksawakening.app.wiring.EquipmentWiring.EquipmentInputs;
import linksawakening.equipment.Boomerang;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.Shovel;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.world.room.ShovelStartResult;

/** Registers tools and their room targets. Constructed only during session wiring. */
final class ToolEquipment {
    private ToolEquipment() {}

    static void ocarina(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.OCARINA.romValue(),
                new Ocarina(c.playerState(), c.gameplaySoundSink(), new Ocarina.PlaybackTarget() {
                    @Override
                    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                        return c.roomSession().transitions().startOcarina(countdown, songFlags, selectedSong);
                    }

                    @Override
                    public boolean ocarinaPlaying() {
                        return c.roomSession().transitions().ocarinaPlaying();
                    }

                    @Override
                    public boolean canStartOcarina() {
                        return c.link().equipment().canUseItems()
                                && !c.link().airborne().isAirborne()
                                && !c.roomSession().playerItems().hookshotActive();
                    }

                    @Override
                    public int ocarinaAnimationState() {
                        return c.roomSession().transitions().ocarinaAnimationState();
                    }
                }));
    }

    static void shovel(EquipmentInputs c, ItemRegistry registry) {
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

    static void hookshot(EquipmentInputs c, ItemRegistry registry) {
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

    static void boomerang(EquipmentInputs c, ItemRegistry registry) {
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
