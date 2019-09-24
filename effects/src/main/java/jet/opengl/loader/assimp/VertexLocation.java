package jet.opengl.loader.assimp;

public class VertexLocation{
    public int position = 0;
    public int normal = 1;
    public int texcoord = 2;
    public int color = 3;
    public int tangent = 4;
    public int boneWeight0 = 5;
    public int boneWeight1 = 6;
    public int blendShape = 7;

    public void set(VertexLocation ohs){
        position = ohs.position;
        normal = ohs.normal;
        texcoord = ohs.texcoord;
        color = ohs.color;
        tangent = ohs.tangent;
        boneWeight0 = ohs.boneWeight0;
        boneWeight1 = ohs.boneWeight1;
        blendShape = ohs.blendShape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VertexLocation that = (VertexLocation) o;

        if (position != that.position) return false;
        if (normal != that.normal) return false;
        if (texcoord != that.texcoord) return false;
        if (color != that.color) return false;
        if (tangent != that.tangent) return false;
        if (boneWeight0 != that.boneWeight0) return false;
        if (boneWeight1 != that.boneWeight1) return false;
        return blendShape == that.blendShape;
    }

    @Override
    public int hashCode() {
        int result = position;
        result = 31 * result + normal;
        result = 31 * result + texcoord;
        result = 31 * result + color;
        result = 31 * result + tangent;
        result = 31 * result + boneWeight0;
        result = 31 * result + boneWeight1;
        result = 31 * result + blendShape;
        return result;
    }
}
