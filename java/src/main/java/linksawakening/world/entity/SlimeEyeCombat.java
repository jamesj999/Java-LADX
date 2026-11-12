package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.*;

import linksawakening.world.entity.event.*;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by SlimeEyeController. */
final class SlimeEyeCombat {
    static void advanceDeath(SlimeEyeController owner, EntityFrameExecution f) {
        var runtime = owner.runtime;
        int s = f.slotFrame().entity().slot();
        runtime.combatTables().writeEnemyFlashCountdown(s, f.inputs().frame() & 255);
        switch (owner.state[s]) {
            case 0 -> {
                runtime.physicsTables().writeEnemyTransitionCountdown(s, 0x40);
                owner.state[s] = 1;
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(s, 0xA0);
                    owner.state[s] = 2;
                }
            }
            default -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(s) == 0) {
                    boolean another = runtime.slotEntities().stream()
                            .anyMatch(e -> e.loaded() && e.slot() != s && e.type() == 0x5B);
                    if (!another) {
                        runtime.bossRewards()
                                .spawnMoldormHeartContainer(f.slotFrame().entity());
                        owner.noise(s, 0x1A);
                    }
                    runtime.lifecycle().disableEntityWithoutPersistence(s);
                } else if ((runtime.physicsTables().readEnemyTransitionCountdown(s) & 7) == 0) {
                    int x = (f.slotFrame().entity().x()
                                    + (f.inputs().randomByteSupplier().getAsInt() & 31)
                                    - 16)
                            & 255;
                    int y = (f.slotFrame().entity().y()
                                    - f.slotFrame().entity().z()
                                    + (f.inputs().randomByteSupplier().getAsInt() & 31)
                                    - 16)
                            & 255;
                    runtime.bossRewards().createMoldormPoof(f.slotFrame().entity(), x, y);
                }
            }
        }
    }

    static void resolveCombat(SlimeEyeController owner, SlimeEyeCombatFrame inputs) {
        new SlimeEyeCombatStep(owner, inputs).run();
    }
}
