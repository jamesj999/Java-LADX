package linksawakening.render;

import org.lwjgl.opengl.GL20;

public class Shader {
    
    private int program;
    
    public Shader() {
        createShaders();
    }
    
    private void createShaders() {
        String vertexSource = 
            "#version 330 core\n" +
            "layout(location = 0) in vec2 aPosition;\n" +
            "layout(location = 1) in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";
        
        String fragmentSource = 
            "#version 330 core\n" +
            "in vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    fragColor = texture(uTexture, vTexCoord);\n" +
            "}\n";
        
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSource);
        GL20.glCompileShader(vertexShader);
        
        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            System.err.println("Vertex shader error: " + GL20.glGetShaderInfoLog(vertexShader));
        }
        
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);
        
        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            System.err.println("Fragment shader error: " + GL20.glGetShaderInfoLog(fragmentShader));
        }
        
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            System.err.println("Program link error: " + GL20.glGetProgramInfoLog(program));
        }
        
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        
        System.out.println("Shader compiled successfully");
    }
    
    public void use() {
        GL20.glUseProgram(program);
    }
    
    public void setUniform1i(String name, int value) {
        int loc = GL20.glGetUniformLocation(program, name);
        GL20.glUniform1i(loc, value);
    }
    
    public void setUniform1fv(String name, float[] values) {
        int loc = GL20.glGetUniformLocation(program, name);
        GL20.glUniform1fv(loc, values);
    }
    
    public int getPositionAttrib() {
        return GL20.glGetAttribLocation(program, "aPosition");
    }
    
    public int getTexCoordAttrib() {
        return GL20.glGetAttribLocation(program, "aTexCoord");
    }
    
    public void cleanup() {
        GL20.glDeleteProgram(program);
    }
}