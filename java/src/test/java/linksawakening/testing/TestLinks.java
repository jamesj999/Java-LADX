package linksawakening.testing;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.entity.LinkDependencies;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.entity.LinkTunicPalette;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.LinkMotionTables;
import linksawakening.rom.RomTables;
import linksawakening.rom.SwordTables;
import linksawakening.state.PlayerState;

/** Named test setup for Link, with real lightweight services and explicit inert defaults. */
public final class TestLinks {
    private TestLinks() {}

    public static TestLinks link() {
        return new TestLinks();
    }

    private InputState input = new InputState();
    private InputConfig config = new InputConfig(-1, -1, -1, -1, -1, -1, -1);
    private LinkMotionTables motionTables = LinkMotionTables.stationary();
    private SwordTables swordTables = SwordTables.animationOnly();
    private OverworldCollision collision = OverworldCollision.openWorld();
    private LinkSpriteSheet sprites = LinkSpriteSheet.invisible();
    private PlayerState player = new PlayerState();
    private ItemRegistry items = new ItemRegistry();
    private GameplaySoundSink sounds = GameplaySoundSink.none();
    private LinkTunicPalette palette = LinkTunicPalette.green();

    public TestLinks input(InputState value) {
        input = Objects.requireNonNull(value, "input");
        return this;
    }

    public TestLinks config(InputConfig value) {
        config = Objects.requireNonNull(value, "config");
        return this;
    }

    public TestLinks tables(RomTables value) {
        motionTables = value.linkMotionTables();
        swordTables = value.swordTables();
        return this;
    }

    public TestLinks collision(OverworldCollision value) {
        collision = Objects.requireNonNull(value, "collision");
        return this;
    }

    public TestLinks sprites(LinkSpriteSheet value) {
        sprites = Objects.requireNonNull(value, "sprites");
        return this;
    }

    public TestLinks player(PlayerState value) {
        player = Objects.requireNonNull(value, "player");
        return this;
    }

    public TestLinks items(ItemRegistry value) {
        items = Objects.requireNonNull(value, "items");
        return this;
    }

    public TestLinks sounds(GameplaySoundSink value) {
        sounds = Objects.requireNonNull(value, "sounds");
        return this;
    }

    public TestLinks palette(LinkTunicPalette value) {
        palette = Objects.requireNonNull(value, "palette");
        return this;
    }

    public Link build() {
        return new Link(new LinkDependencies(
                input, config, motionTables, swordTables, collision, sprites, player, items, sounds, palette));
    }
}
