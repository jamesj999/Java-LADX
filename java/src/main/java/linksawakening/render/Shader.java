package linksawakening.render;

/** Compatibility adapter for the presenter's GL shader implementation. */
@Deprecated
public class Shader {
    private final OpenGlFramePresenter.GlShader delegate = new OpenGlFramePresenter.GlShader();
    public Shader() {}
    public void use() { delegate.use(); }
    public void setUniform1i(String name, int value) { delegate.setUniform1i(name, value); }
    public void setUniform1fv(String name, float[] values) { delegate.setUniform1fv(name, values); }
    public int getPositionAttrib() { return delegate.getPositionAttrib(); }
    public int getTexCoordAttrib() { return delegate.getTexCoordAttrib(); }
    public void cleanup() { delegate.cleanup(); }
}
