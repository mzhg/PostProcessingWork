package jet.opengl.postprocessing.shader;

/**
 * Represents a piece of shader source and the shader type.<p>
 * Used with creation functions to pass in arrays of multiple shader source types.<p></p>
 * Created by mazhen'gui on 2017/4/18.
 */

public class ShaderSourceItem {
    /** The shader source */
    public CharSequence source;
    /** The shader type.e.g GL_VERTEX_SHADER*/
    public ShaderType type;

    /** The predefined macros for this shader.*/
    public Macro[] macros;
    /** Compile Version, when == 0, use the lastest GLSL version. */
    public int compileVersion;
    /** Attrib binders, only valid for Vertex Shader. */
    public AttribBinder[] attribs;

    public ShaderSourceItem(){}

    public ShaderSourceItem(CharSequence source, ShaderType type) {
        this.source = source;
        this.type = type;
    }
}
