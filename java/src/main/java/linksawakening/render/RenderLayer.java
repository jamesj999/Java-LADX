package linksawakening.render;

/** Defines a layer that can draw into a supplied rendering context.  Hardware analogue: indexed BG/window planes and ordered OBJ entries. */
@FunctionalInterface
interface RenderLayer {
    void render(RenderContext context);
}
