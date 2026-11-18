package linksawakening.app.gameplay.entity;

import java.util.List;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.event.EntityProjectileEvent;

/**
 * Defines world-tick and result-application order. Retained implementations also enable recording test
 * doubles.
 */
interface EntityFrameStages {
    List<EntityProjectileEvent> tickEntities(Sword.CollisionBox b);

    void positions();

    void motion();

    void attacks();

    void sword();

    void combat(List<EntityProjectileEvent> events);

    void chests();

    void progress();

    void witch();

    void dialogs();

    void projectiles(List<EntityProjectileEvent> events);

    void finalEffects();

    default void run(Sword.CollisionBox b) {
        var events = tickEntities(b);
        applyResults(events);
    }

    default void applyResults(List<EntityProjectileEvent> events) {
        positions();
        motion();
        attacks();
        sword();
        combat(events);
        chests();
        progress();
        witch();
        dialogs();
        projectiles(events);
        finalEffects();
    }
}
