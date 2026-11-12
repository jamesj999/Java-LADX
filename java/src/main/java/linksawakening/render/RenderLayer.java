package linksawakening.render;

@FunctionalInterface
/** Defines a layer that can draw into a supplied rendering context. */
interface RenderLayer {
    void render(RenderContext context);
}
