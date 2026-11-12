package linksawakening.render;

@FunctionalInterface
/** Defines a layer that can draw into a supplied rendering context. */
public interface RenderLayer {
    void render(RenderContext context);
}
