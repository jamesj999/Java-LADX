package linksawakening.app.gameplay.entity;

import java.util.List;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.event.EntityProjectileEvent;

interface EntityFrameStages {
    List<EntityProjectileEvent> tickEntities(GameplaySimulation c, Sword.CollisionBox b);

    void positions(GameplaySimulation c);

    void motion(GameplaySimulation c);

    void attacks(GameplaySimulation c);

    void sword(GameplaySimulation c);

    void combat(GameplaySimulation c, List<EntityProjectileEvent> events);

    void chests(GameplaySimulation c);

    void progress(GameplaySimulation c);

    void witch(GameplaySimulation c);

    void dialogs(GameplaySimulation c);

    void projectiles(GameplaySimulation c, List<EntityProjectileEvent> events);

    void finalEffects(GameplaySimulation c);

    default void run(GameplaySimulation c, Sword.CollisionBox b) {
        var events = tickEntities(c, b);
        positions(c);
        motion(c);
        attacks(c);
        sword(c);
        combat(c, events);
        chests(c);
        progress(c);
        witch(c);
        dialogs(c);
        projectiles(c, events);
        finalEffects(c);
    }
}
