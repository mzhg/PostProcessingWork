package jet.opengl.postprocessing.texture;

/**
 * Created by mazhen'gui on 2017/4/15.
 */

public class TextureAttachDesc {
    public int index;
    public AttachType type = AttachType.TEXTURE_2D;
    public int layer;
    public int level;

    public TextureAttachDesc(int index, AttachType type, int layer, int level) {
        this.index = index;
        this.type = type;
        this.layer = layer;
        this.level = level;
    }

    public TextureAttachDesc(){}
}
