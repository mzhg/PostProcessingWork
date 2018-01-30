package jet.opengl.postprocessing.shader;

/**
 * Created by mazhen'gui on 2017/7/14.
 */

public class UniformBlockProperties {
    public UniformBlockType type;
    public String name;
    public int binding;
    public int size;

    @Override
    public String toString() {
        return type.name() +  " [ name=" + name + ", binding=" + binding + ", size = " + size + "]";
    }
}
