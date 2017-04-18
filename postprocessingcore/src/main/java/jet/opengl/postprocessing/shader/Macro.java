package jet.opengl.postprocessing.shader;

/**
 * Represents a pre-defined macro for the shader source building.<p></p>
 * Created by mazhen'gui on 2017-04-18 17:08:55.
 */

public class Macro {
    public String key;
    public Object value;

    public Macro(){}

    public Macro(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
