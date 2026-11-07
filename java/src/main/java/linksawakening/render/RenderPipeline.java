package linksawakening.render;

/** Indexed production composition. */
public enum RenderPipeline {
    HARDWARE,
    /** @deprecated Source-compatible alias for the default pipeline; no RGBA production backend remains. */
    @Deprecated LEGACY_RGBA
}
